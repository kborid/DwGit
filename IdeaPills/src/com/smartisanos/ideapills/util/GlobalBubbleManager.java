package com.smartisanos.ideapills.util;

import android.annotation.NonNull;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.provider.Settings;
import android.service.onestep.GlobalBubble;
import android.service.onestep.GlobalBubbleAttach;
import android.text.TextUtils;
import android.widget.Toast;

import com.smartisanos.ideapills.BubbleController;
import com.smartisanos.ideapills.Constants;
import com.smartisanos.ideapills.common.util.PackageUtils;
import com.smartisanos.ideapills.common.util.TaskHandler;
import com.smartisanos.ideapills.common.model.ShareItem;
import com.smartisanos.ideapills.IdeaPillsApp;
import com.smartisanos.ideapills.R;
import com.smartisanos.ideapills.data.BUBBLE;
import com.smartisanos.ideapills.data.DataHandler;
import com.smartisanos.ideapills.remind.util.AlarmUtils;
import com.smartisanos.ideapills.sync.SyncManager;
import com.smartisanos.ideapills.sync.share.SyncShareManager;
import com.smartisanos.ideapills.sync.share.SyncShareUtils;
import com.smartisanos.ideapills.entity.AttachMentItem;
import com.smartisanos.ideapills.entity.BubbleItem;
import com.smartisanos.ideapills.common.util.UIHandler;
import com.smartisanos.ideapills.common.util.CommonUtils;
import com.smartisanos.ideapills.view.FiltrateSetting;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

import smartisanos.api.SettingsSmt;

public class GlobalBubbleManager {
    private static final LOG log = LOG.getInstance(GlobalBubbleManager.class);
    public static final int BUBBLE_TEXT_MAX  = 5000;
    public static final int ERROR_NO_BUBBLE_ITEM_ADD = 0;
    public static final int BUBBLE_ITEM_ADD_SUCCED = 1;
    private static final long TODO_OVER_CHECK_INTERVAL = 30 * 60 * 1000;

    private volatile static GlobalBubbleManager sInstance;

    public synchronized static GlobalBubbleManager getInstance() {
        if (sInstance == null) {
            synchronized (GlobalBubbleManager.class) {
                if (sInstance == null) {
                    sInstance = new GlobalBubbleManager();
                }
            }
        }
        return sInstance;
    }

    @NonNull
    private Map<Integer, BubbleItem> mBubbleMap = new ConcurrentHashMap<Integer, BubbleItem>();
    private List<BubbleItem> mVisibleBubbles = new ArrayList<BubbleItem>();
    private final Object mVisibleBubblesLock = new Object();

    private Context mContext;

    private List<OnUpdateListener> mListeners = new ArrayList<OnUpdateListener>();

    private GlobalBubbleManager() {
        mContext = IdeaPillsApp.getInstance();
    }

    public Map<Integer, BubbleItem> getBubbleMap() {
        return mBubbleMap;
    }

    public List<BubbleItem> getBubbles() {
        return getBubbles(null, false);
    }

    public List<BubbleItem> getBubbles(String filter) {
        return getBubbles(filter, false);
    }

    public List<BubbleItem> getBubbles(int colorFilter, boolean hidedone) {
        return getBubbles(colorFilter, null, hidedone);
    }

    public List<BubbleItem> getBubbles(String filter, boolean hidedone) {
        return getBubbles(FiltrateSetting.FILTRATE_ALL, null, hidedone);
    }

    public List<BubbleItem> getBubbles(int colorFilter, String filter, boolean hidedone) {
        Pattern pattern = null;
        if (!TextUtils.isEmpty(filter)) {
            pattern = Pattern.compile(".*" + Pattern.quote(filter) + ".*", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
        }
        List<BubbleItem> ret = new ArrayList<BubbleItem>();
        synchronized (mVisibleBubblesLock) {
            int itemColor = GlobalBubble.COLOR_BLUE;
            for (BubbleItem item : mVisibleBubbles) {
                if (hidedone && item.getUsedTime() > 0) {
                    continue;
                }
                itemColor = item.getColor();
                if (item.getShareStatus() > GlobalBubble.SHARE_STATUS_NOT_SHARED) {
                    itemColor = GlobalBubble.COLOR_SHARE;
                }
                if (colorFilter > FiltrateSetting.FILTRATE_ALL && itemColor != colorFilter) {
                    continue;
                }
                if (pattern == null || item.matches(pattern)) {
                    ret.add(item);
                }
            }
        }
        return ret;
    }

    public int indexOfPptBubble(BubbleItem bubbleItem) {
        if (bubbleItem == null) {
            return -1;
        }
        synchronized (mVisibleBubblesLock) {
            if (mVisibleBubbles != null) {
                int index = 0;
                for (BubbleItem bubble : mVisibleBubbles) {
                    if (bubble == null || GlobalBubble.COLOR_NAVY_BLUE != bubble.getColor()) {
                        continue;
                    }
                    if (bubble.getId() == bubbleItem.getId()) {
                        return index;
                    }
                    index++;
                }
            }
        }
        return -1;
    }

    public void clearAllBubblesCache() {
        synchronized (mVisibleBubblesLock) {
            for (BubbleItem item : mVisibleBubbles) {
                item.invalidateNormalWidth();
                item.setHeightInLarge(-1);
            }
            BubbleItem.sBubbleItemFake.invalidateNormalWidth();
            BubbleItem.sBubbleItemFake.setHeightInLarge(-1);
        }
    }

    public boolean hasTodoOverBubble(int color) {
        synchronized (mVisibleBubblesLock) {
            for (BubbleItem item : mVisibleBubbles) {
                if (item.getUsedTime() > 0) {
                    if ((color != GlobalBubble.COLOR_SHARE && item.getColor() == color && item.getShareStatus() <= GlobalBubble.SHARE_STATUS_NOT_SHARED)
                            || color <= FiltrateSetting.FILTRATE_ALL
                            || (color == GlobalBubble.COLOR_SHARE && item.getShareStatus() > GlobalBubble.SHARE_STATUS_NOT_SHARED)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public BubbleItem getBubbleItemById(int id) {
        BubbleItem item = null;
        synchronized (mVisibleBubblesLock) {
            if (mVisibleBubbles != null) {
                for (BubbleItem bubble : mVisibleBubbles) {
                    if (bubble == null) {
                        continue;
                    }
                    if (bubble.getId() == id) {
                        item = bubble;
                        break;
                    }
                }
            }
        }
        return item;
    }

    public BubbleItem getBubbleBySyncId(String bubbleSyncId) {
        BubbleItem item = null;
        synchronized (mVisibleBubblesLock) {
            if (mVisibleBubbles != null) {
                for (BubbleItem bubble : mVisibleBubbles) {
                    if (bubble == null) {
                        continue;
                    }
                    if (bubbleSyncId != null && bubbleSyncId.equals(bubble.getSyncId())) {
                        item = bubble;
                        break;
                    }
                }
            }
        }
        return item;
    }

    public BubbleItem getBubbleByAttachmentId(int attachmentId) {
        synchronized (mVisibleBubblesLock) {
            if (mVisibleBubbles != null) {
                for (BubbleItem bubble : mVisibleBubbles) {
                    if (bubble == null) {
                        continue;
                    }
                    for (AttachMentItem attachMentItem : bubble.getAttachments()) {
                        if (attachMentItem.getId() == attachmentId) {
                            return bubble;
                        }
                    }
                }
            }
        }
        return null;
    }

    public AttachMentItem getAttachmentItemById(int id) {
        synchronized (mVisibleBubblesLock) {
            if (mVisibleBubbles != null) {
                for (BubbleItem bubble : mVisibleBubbles) {
                    if (bubble == null) {
                        continue;
                    }
                    if (bubble.getAttachments() != null) {
                        for (AttachMentItem attachMentItem : bubble.getAttachments()) {
                            if (attachMentItem.getId() == id) {
                                return attachMentItem;
                            }
                        }
                    }
                }
            }
        }
        return null;
    }

    public List<AttachMentItem> getAttachmentItemsNotDownload() {
        List<AttachMentItem> attachMentItems = new ArrayList<AttachMentItem>();
        synchronized (mVisibleBubblesLock) {
            if (mVisibleBubbles != null) {
                for (BubbleItem bubble : mVisibleBubbles) {
                    if (bubble == null) {
                        continue;
                    }
                    if (bubble.getAttachments() != null) {
                        for (AttachMentItem attachMentItem : bubble.getAttachments()) {
                            if (attachMentItem.getDownloadStatus() == AttachMentItem.DOWNLOAD_STATUS_NOT_DOWNLOAD) {
                                attachMentItems.add(attachMentItem);
                            }
                        }
                    }
                }
            }
        }
        return attachMentItems;
    }

    public AttachMentItem getAttachmentItemBySyncId(String syncId) {
        if (syncId == null) {
            return null;
        }
        synchronized (mVisibleBubblesLock) {
            if (mVisibleBubbles != null) {
                for (BubbleItem bubble : mVisibleBubbles) {
                    if (bubble == null) {
                        continue;
                    }
                    if (bubble.getAttachments() != null) {
                        for (AttachMentItem attachMentItem : bubble.getAttachments()) {
                            if (syncId.equals(attachMentItem.getSyncId())) {
                                return attachMentItem;
                            }
                        }
                    }
                }
            }
        }
        return null;
    }

    public BubbleItem getNextAlarmBubble() {
        BubbleItem nexAlarmItem = null;
        long minTime = Long.MAX_VALUE;
        long nowTime = System.currentTimeMillis();
        List<BubbleItem> updateItems = new ArrayList<BubbleItem>();
        synchronized (mVisibleBubblesLock) {
            if (mVisibleBubbles != null) {
                for (BubbleItem bubble : mVisibleBubbles) {
                    if (bubble == null) {
                        continue;
                    }
                    if (bubble.getRemindTime() > 0 && bubble.getRemindTime() >= nowTime
                            && bubble.getRemindTime() < minTime) {
                        minTime = bubble.getRemindTime();
                        nexAlarmItem = bubble;
                    }
                }
            }
        }
        if (updateItems.size() > 0) {
            updateBubbleItems(updateItems);
        }
        return nexAlarmItem;
    }

    @NonNull
    public List<BubbleItem> getAlertBubbles(long time) {
        List<BubbleItem> alertBubbles = new ArrayList<BubbleItem>();
        // 10s内的可以提醒
        long minAlertTime = time - 10 * 1000;
        synchronized (mVisibleBubblesLock) {
            if (mVisibleBubbles != null) {
                for (BubbleItem bubble : mVisibleBubbles) {
                    if (bubble == null) {
                        continue;
                    }
                    if (bubble.getRemovedTime() > 0) {
                        continue;
                    }
                    if (bubble.getRemindTime() > 0 && bubble.getRemindTime() > minAlertTime
                            && bubble.getRemindTime() <= time) {
                        alertBubbles.add(bubble);
                        LOG.d(String.format("getAlertBubbles: add bubble: %1$s, time=%2$d", bubble.getText(), bubble.getRemindTime()));
                    }
                }
            }
        }
        return alertBubbles;
    }

    public boolean registerListener(OnUpdateListener li) {
        if (!mListeners.contains(li)) {
            mListeners.add(li);
            return true;
        }
        return false;
    }

    public void unregisterListener(OnUpdateListener li) {
        mListeners.remove(li);
    }

    public List<BubbleItem> addGlobalBubbles(List<GlobalBubble> bubbles) {
        List<BubbleItem> bubbleItems = getBubbleItemsFrom(bubbles);
        if (GlobalBubble.COLOR_SHARE == Constants.DEFAULT_BUBBLE_COLOR) {
            handleShareItems(bubbleItems);
        }
        return addBubbleItems(bubbleItems);
    }

    public int addGlobalBubblesByDragEvent(List<GlobalBubble> bubbles) {
        int result = ERROR_NO_BUBBLE_ITEM_ADD;
        List<BubbleItem> bubbleItems = getBubbleItemsFrom(bubbles);
        if (GlobalBubble.COLOR_SHARE == Constants.getDefaultBubbleColor()) {
            handleShareItems(bubbleItems);
        }
        if (bubbleItems == null || bubbleItems.size() <= 0) {
            return result;
        }
        boolean needScheduRemindAlarm = false;
        List<BubbleItem> added = new ArrayList<BubbleItem>();
        long now = System.currentTimeMillis();
        synchronized (mVisibleBubblesLock) {
            int size = bubbleItems.size();
            int maxWeight = getMaxWeight();
            for (int i = size - 1; i >= 0; i--) {
                BubbleItem item = bubbleItems.get(i);
                if (!checkIsTextLengthInLimit(item)) {
                    log.error("the bubble contains too much words");
                    continue;
                }
                if (!mVisibleBubbles.contains(item)) {
                    mVisibleBubbles.add(0, item);
                    added.add(0, item);
                    item.setWeight(++maxWeight);
                    if (item.getTimeStamp() == 0) {
                        item.setTimeStamp(System.currentTimeMillis());
                    }
                    result = BUBBLE_ITEM_ADD_SUCCED;
                }
                if (item.getRemindTime() >= now) {
                    needScheduRemindAlarm = true;
                }
            }
        }
        saveBubbleItems(added);
        if (needScheduRemindAlarm) {   // for set remind alarm.
            AlarmUtils.scheduleNextAlarm(mContext, null);
        }
        notifyBubbleAddedInner(added);
        return result;
    }

    public List<BubbleItem> addGlobalBubblesFromSara(List<GlobalBubble> bubbles,
                                                        List<GlobalBubbleAttach> attaches) {
        List<BubbleItem> bubbleItems = getBubbleItemsFrom(bubbles, attaches);
        final List<BubbleItem> sharedItems = new ArrayList<>();
        for (BubbleItem bubbleItem : bubbleItems) {
            if (bubbleItem.isShareColor()) {
                sharedItems.add(bubbleItem);
            }
        }
        if (!sharedItems.isEmpty()) {
            handleShareItems(sharedItems);
        }
        final List<BubbleItem> added = addBubbleItems(bubbleItems);
        DataHandler.post(new Runnable() {
            @Override
            public void run() {
                if (added != null && added.size() > 0) {
                    for (BubbleItem item : added) {
                        if (item != null && item.getCreateAt() > 0 && item.getDueDate() > 0) {
                            AlarmUtils.replaceAlarmToCalendar(IdeaPillsApp.getInstance(), item);
                        }
                    }
                }
            }
        });
        return bubbleItems;
    }

    public void handleShareItems(List<BubbleItem> bubbleItems) {
        final long userId = SyncManager.getCloudAccountUserId(IdeaPillsApp.getInstance());
        List<Long> addParticipantIds = null;
        List<Long> sendInvitationParticipantIds = null;
        if (userId > 0 && SyncManager.syncEnable(IdeaPillsApp.getInstance())) {
            addParticipantIds = SyncShareUtils.findParticipantIdsForAdd(userId, SyncShareManager.INSTANCE.getCachedInvitationList());
            sendInvitationParticipantIds = SyncShareUtils.findParticipantIdsForSendInvitation(userId,
                    SyncShareManager.INSTANCE.getCachedInvitationList());
        }
        if (addParticipantIds != null && !addParticipantIds.isEmpty()) {
            for (BubbleItem shareItem : bubbleItems) {
                shareItem.changeShareStatusToAdd(addParticipantIds);
            }
        } else if (sendInvitationParticipantIds != null && !sendInvitationParticipantIds.isEmpty()) {
            for (BubbleItem shareItem : bubbleItems) {
                shareItem.changeShareStatusToWaitInvitation(sendInvitationParticipantIds.get(0));
            }
        } else {
            for (BubbleItem shareItem : bubbleItems) {
                shareItem.setShareStatus(GlobalBubble.SHARE_STATUS_NOT_SHARED);
                shareItem.clearPendingShareStatus();
            }
        }
    }

    public void showTextLimitToast() {
        UIHandler.post(new Runnable() {
            public void run() {
                GlobalBubbleUtils.showSystemToast(R.string.bubble_add_string_limit, Toast.LENGTH_SHORT);
            }
        });
    }

    public boolean checkIsTextLengthInLimit(String txt) {
        if (!TextUtils.isEmpty(txt) && CommonUtils.getStringLength(txt) > BUBBLE_TEXT_MAX) {
            showTextLimitToast();
            return false;
        }
        return true;
    }

    public boolean checkIsTextLengthInLimit(BubbleItem item) {
        return checkIsTextLengthInLimit(item.getText());
    }

    public void addBubbleItemsOnly(List<BubbleItem> bubbleItems, int pos) {
        synchronized (mVisibleBubblesLock) {
            mVisibleBubbles.addAll(pos, bubbleItems);
        }
        saveBubbleItems(bubbleItems);
    }

    public void addBubbleItemsToVisible(List<BubbleItem> bubbleItems, int pos) {
        synchronized (mVisibleBubblesLock) {
            mVisibleBubbles.addAll(pos, bubbleItems);
        }
    }

    public int getMaxWeight() {
        synchronized (mVisibleBubblesLock) {
            if (mVisibleBubbles.size() == 0) {
                return -1;
            }
            for (BubbleItem item : mVisibleBubbles) {
                if (item.getWeight() > 0) {
                    return item.getWeight();
                }
            }
            return mVisibleBubbles.get(0).getWeight();
        }
    }

    public List<BubbleItem> addBubbleItems(List<BubbleItem> bubbleItems) {
        if (bubbleItems == null || bubbleItems.size() <= 0) {
            return null;
        }
        boolean needScheduRemindAlarm = false;
        List<BubbleItem> added = new ArrayList<BubbleItem>();
        long now = System.currentTimeMillis();
        synchronized (mVisibleBubblesLock) {
            int size = bubbleItems.size();
            int maxWeight = getMaxWeight();
            for (int i = size - 1; i >= 0; i--) {
                BubbleItem item = bubbleItems.get(i);
                if (!checkIsTextLengthInLimit(item)) {
                    log.error("the bubble contains too much words");
                    continue;
                }
                if (!mVisibleBubbles.contains(item)) {
                    mVisibleBubbles.add(0, item);
                    added.add(0, item);
                    item.setWeight(++maxWeight);
                    if (item.getTimeStamp() == 0) {
                        item.setTimeStamp(System.currentTimeMillis());
                    }
                }

                if (item.getRemindTime() >= now) {
                    needScheduRemindAlarm = true;
                }
            }
        }
        saveBubbleItems(added);
        if (needScheduRemindAlarm) {   // for set remind alarm.
            AlarmUtils.scheduleNextAlarm(mContext, null);
        }
        notifyBubbleAddedInner(added);
        return added;
    }

    public void notifyBubbleAdded(List<BubbleItem> bubbles) {
        if (bubbles == null || bubbles.size() <= 0) {
            return;
        }
        List<BubbleItem> added = new ArrayList<BubbleItem>();
        List<BubbleItem> update = new ArrayList<BubbleItem>();
        synchronized (mVisibleBubblesLock) {
            int size = bubbles.size();
            int maxWeight = getMaxWeight();
            BubbleItem item = null;
            BubbleItem exist = null;
            int index = 0;
            for (int i = size - 1; i >= 0; i--) {
                item = bubbles.get(i);
                if (item == null || !checkIsTextLengthInLimit(item)) {
                    log.error("the bubble contains too much words");
                    continue;
                }
                if (mBubbleMap.containsKey(item.getId())) {
                    exist = mBubbleMap.get(item.getId());
                    index = mVisibleBubbles.indexOf(exist);
                    if(index >=0 && index < mVisibleBubbles.size()) {
                        mVisibleBubbles.remove(index);
                        mVisibleBubbles.add(index, item);
                        update.add(item);
                    }else{
                        item.setWeight(++maxWeight);
                        added.add(0, item);
                    }
                    mBubbleMap.put(item.getId(),item);
                } else {
                    mBubbleMap.put(item.getId(), item);
                    if (!mVisibleBubbles.contains(item)) {
                        item.setWeight(++maxWeight);
                        mVisibleBubbles.add(0, item);
                        added.add(0, item);
                    } else {
                        update.add(item);
                    }
                }
                if (item.getTimeStamp() == 0) {
                    item.setTimeStamp(System.currentTimeMillis());
                }
            }
        }
        notifyBubbleAddedInner(added);
        if (update.size() > 0) {
            notifyBubbleUpdateInner();
        }
    }

    public void notifyBubbleDeleted(List<BubbleItem> bubbles) {
        if (bubbles == null || bubbles.size() <= 0) {
            return;
        }
        synchronized (mVisibleBubblesLock) {
            int size = bubbles.size();
            int index = 0;
            BubbleItem item = null;
            BubbleItem exist;
            for (int i = size - 1; i >= 0; i--) {
                item = bubbles.get(i);
                if (item != null && mBubbleMap.containsKey(item.getId())) {
                    exist = mBubbleMap.get(item.getId());
                    mBubbleMap.remove(exist.getId());
                    if (mVisibleBubbles.contains(exist)) {
                        mVisibleBubbles.remove(exist);
                    }
                }
            }
        }
        notifyBubbleUpdateInner();
    }

    public void removeBubbleItem(BubbleItem bubble) {
        List<BubbleItem> bubbles = new ArrayList<BubbleItem>();
        bubbles.add(bubble);
        removeBubbleItems(bubbles);
    }

    public void removeShareItems() {
        List<BubbleItem> toRemoved = new ArrayList<>();
        boolean changedShareColor = false;
        synchronized (mVisibleBubblesLock) {
            for (BubbleItem item : mBubbleMap.values()) {
                if (item == null) {
                    continue;
                }
                if (item.isShareFromOthers()) {
                    toRemoved.add(item);
                }
                if (item.isShareColor()) {
                    item.setShareStatusSilent(GlobalBubble.SHARE_STATUS_NOT_SHARED);
                    item.clearPendingShareStatus();
                    changedShareColor = true;
                }
            }
        }
        if (!toRemoved.isEmpty() || changedShareColor) {
            synchronized (mVisibleBubblesLock) {
                for (BubbleItem removedItem : toRemoved) {
                    mBubbleMap.remove(removedItem.getId());
                    mVisibleBubbles.remove(removedItem);
                }
            }
            notifyBubbleUpdateInner();
        }
        DataHandler.handleTask(DataHandler.TASK_CLEAR_SHARE_BUBBLE_DATA);
    }

    public void removeBubbleItems(List<BubbleItem> bubbles) {
        if (bubbles == null || bubbles.size() <= 0) {
            return;
        }

        boolean needReScheduleAlarm = false;
        final List<BubbleItem> removed = new ArrayList<BubbleItem>();
        long now = System.currentTimeMillis();
        synchronized (mVisibleBubblesLock) {
            Map<Integer, BubbleItem> bubbleItemMap = getBubbleMap();
            for (BubbleItem bubble : bubbles) {
                if (bubble == null) {
                    continue;
                }
                int id = bubble.getId();
                bubbleItemMap.remove(id);
                if (mVisibleBubbles.remove(bubble)) {
                    handleConflict(bubble);
                    onDismiss(bubble);
                    if (id != 0) {
                        removed.add(bubble);
                    }
                    if (bubble.getRemindTime() >= now) {
                        needReScheduleAlarm = true;
                    }
                }
            }
        }
        if (removed.size() > 0) {
            notifyUpdate();
            BubbleItem firstItem = (BubbleItem) removed.get(0);
            if (firstItem.needDele()) {
                List params = new ArrayList();
                params.add(BUBBLE.ID + "=" + firstItem.getId());
                DataHandler.handleTask(DataHandler.TASK_MARK_DELETE, params);
            } else if (firstItem.needRemove()) {
                List params = new ArrayList();
                params.add(BUBBLE.ID + "=" + firstItem.getId());
                DataHandler.handleTask(DataHandler.TASK_REMOVE_FOREVER, params);
            } else {
                long time = System.currentTimeMillis();
                List<ContentValues> valuesList = new ArrayList<>();
                for (BubbleItem removedItem : removed) {
                    ContentValues value = new ContentValues();
                    removedItem.setRemovedTime(time);
                    value.put(BUBBLE.REMOVED_TIME, removedItem.getRemovedTime());
                    value.put(BUBBLE.MODIFY_FLAG, removedItem.getModificationFlag());
                    value.put(BUBBLE.SECONDARY_MODIFY_FLAG, removedItem.getSecondaryModifyFlag());
                    valuesList.add(value);
                }
                List params = new ArrayList();
                params.add(removed);
                params.add(valuesList);
                DataHandler.handleTask(DataHandler.TASK_REMOVE_BUBBLE, params);
            }
        }
        if (needReScheduleAlarm) {
            AlarmUtils.scheduleNextAlarm(mContext, null);
        }
    }

    private void onDismiss(BubbleItem item) {
        if (item.isNeedInput()) {
            item.setNeedInput(false);
            item.setInLargeMode(false);
            IdeaPillsApp.getInstance().getBubbleObserverManager().notifyBubbleObserver(item, BubbleItem.MSG_INPUT_OVER);
        } else {
            IdeaPillsApp.getInstance().getBubbleObserverManager().notifyBubbleObserver(item, BubbleItem.MSG_BUBBLE_DELETE);
        }
    }

    public boolean isEmpty() {
        synchronized (mVisibleBubblesLock) {
            return mVisibleBubbles.isEmpty();
        }
    }

    public void updateOrder() {
        synchronized (mVisibleBubblesLock) {
            Collections.sort(mVisibleBubbles);
            updateBubbleItems(mVisibleBubbles);
        }
    }

    private void orderChanged() {
        synchronized (mVisibleBubblesLock) {
            int size = mVisibleBubbles.size();
            List<Integer> weightList = new ArrayList<Integer>();
            for (int i = 0; i < size; i++) {
                weightList.add(mVisibleBubbles.get(i).getWeight());
            }
            Collections.sort(weightList);
            for (int i = 0; i < size; i++) {
                mVisibleBubbles.get(i).setWeight(weightList.get(size - i - 1));
            }
        }
        updateOrder();
    }

    public void saveBubbleItems(List<BubbleItem> bubbleItems) {
        saveBubbleItems(bubbleItems, false);
    }

    public void saveBubbleItems(List<BubbleItem> bubbleItems, final boolean justSave) {
        if (bubbleItems != null && bubbleItems.size() > 0) {
            List params = new ArrayList();
            final List<BubbleItem> add = new ArrayList<BubbleItem>();
            for (BubbleItem item : bubbleItems) {
//                if (item.getId() == 0 && !item.isTextAvailable()) {
//                    log.info("empty bubble will not goto trash");
//                    continue;
//                }
                add.add(item);
            }
            if (add.size() > 0) {
                params.add(add);
                Runnable callback = new Runnable() {
                    public void run() {
                        UIHandler.post(new Runnable() {
                            public void run() {
                                for (BubbleItem item : add) {
                                    if (item != null) {
                                        item.mWaitingUpdateId = false;
                                        if (!justSave) {
                                            if (item.getId() > 0) {
                                                synchronized (mVisibleBubblesLock) {
                                                    if (item.isVisibleItem() && !mVisibleBubbles.contains(item)
                                                            && !item.needDele() && !item.needTrash()) {
                                                        mBubbleMap.put(item.getId(), item);
                                                        mVisibleBubbles.add(0, item);
                                                        item.setWeight(getMaxWeight() + 1);
                                                    } else {
                                                        mBubbleMap.put(item.getId(), item);
                                                    }
                                                }
                                            } else {
                                                log.assertIfDebug("this bubble should not be " + item.getId());
                                            }
                                        }
                                    }
                                }
                            }
                        });
                    }
                };
                params.add(callback);
                DataHandler.handleTask(DataHandler.TASK_ADD_BUBBLE, params);
            }
        }
    }

    public void insertBubbleItem(BubbleItem bubbleItem) {
        if (bubbleItem == null) {
            return;
        }
        if (bubbleItem.mWaitingUpdateId) {
            log.error("insertBubbleItem return by mWaitingUpdateId true");
            return;
        }
        bubbleItem.mWaitingUpdateId = true;
        List<BubbleItem> list = new ArrayList<BubbleItem>();
        list.add(bubbleItem);
        saveBubbleItems(list);
    }

    public void updateBubbleItem(BubbleItem bubbleItem)  {
        List<BubbleItem> list = new ArrayList<BubbleItem>();
        list.add(bubbleItem);
        updateBubbleItems(list);
    }

    public void updateBubbleItems(List<BubbleItem> bubbleItems) {
        if (bubbleItems != null && bubbleItems.size() > 0) {
            boolean needSync = false;
            List<ContentValues> values = new ArrayList<ContentValues>();
            boolean colorChanged = false;
            for (BubbleItem item : bubbleItems) {
                if (item == null) {
                    continue;
                }
                if (!colorChanged && item.isBubbleColorChanged()) {
                    colorChanged = true;
                }
                if (item.hasModificationFlagChanged()) {
                    values.add(item.toContentValues());
                    if (!needSync && !item.isAddingAttachment()) {
                        needSync = true;
                    }
                }
                if (!needSync && item.isHasChangedBubbleWithoutSync()) {
                    needSync = true;
                }
            }
            if (needSync) {
                for (BubbleItem item : bubbleItems) {
                    if (item == null) {
                        continue;
                    }
                    item.setHasChangedBubbleWithoutSync(false);
                }
            }
            List params = new ArrayList();
            params.add(values);
            if (needSync) {
                DataHandler.handleTask(DataHandler.TASK_UPDATE_BUBBLE_BY_ID_AND_SYNC, params);
            } else {
                DataHandler.handleTask(DataHandler.TASK_UPDATE_BUBBLE_BY_ID, params);
            }
            if (colorChanged) {
                notifyUpdate();
            }
        }
    }

    public void updateAttachmentItem(AttachMentItem attachMentItem)  {
        List<AttachMentItem> list = new ArrayList<AttachMentItem>();
        list.add(attachMentItem);
        updateAttachmentItems(list);
    }

    public void updateAttachmentItems(List<AttachMentItem> attachMentItems) {
        if (attachMentItems != null && attachMentItems.size() > 0) {
            boolean needSync = false;
            List<ContentValues> values = new ArrayList<ContentValues>();
            for (AttachMentItem item : attachMentItems) {
                if (item == null) {
                    continue;
                }
                values.add(item.toContentValues());
            }
            List params = new ArrayList();
            params.add(values);
            DataHandler.handleTask(DataHandler.TASK_UPDATE_ATTACHMENT_BY_ID, params);
        }
    }

    public List<BubbleItem> sortAndRebuildWeightBubbles(List<BubbleItem> changedBubbles, final List<BubbleItem> addedBubbles,
                                                        final List<Long> sortArray, boolean canUseServerOrder) {
        List<BubbleItem> allBubbles = new ArrayList<BubbleItem>(mBubbleMap.values());
        //sort by order
        StringBuffer buffer = new StringBuffer();
        for (BubbleItem item : allBubbles) {
            buffer.append(item.getWeight() + ", ");
        }
        log.error("before sort " + buffer.toString());
        if (sortArray != null && canUseServerOrder) {
            Collections.sort(allBubbles, new Comparator<BubbleItem>() {
                @Override
                public int compare(BubbleItem o1, BubbleItem o2) {
                    try {
                        int indexO1 = sortArray.indexOf(Long.parseLong(o1.getSyncId()));
                        int indexO2 = sortArray.indexOf(Long.parseLong(o2.getSyncId()));
                        if (indexO1 < indexO2) {
                            return -1;
                        } else {
                            return 1;
                        }
                    } catch (Exception e) {
                        return o1.compareTo(o2);
                    }
                }
            });
        } else {
            Collections.sort(allBubbles, new Comparator<BubbleItem>() {
                @Override
                public int compare(BubbleItem o1, BubbleItem o2) {
                    boolean isContainsO1 = addedBubbles.contains(o1);
                    boolean isContainsO2 = addedBubbles.contains(o2);
                    if (isContainsO1 && !isContainsO2) {
                        return -1;
                    } else if (!isContainsO1 && isContainsO2) {
                        return 1;
                    } else {
                        if (sortArray != null
                                && isContainsO1 && isContainsO2
                                && !o1.getModificationFlag(BubbleItem.MF_WEIGHT)
                                && !o2.getModificationFlag(BubbleItem.MF_WEIGHT)) {
                            int indexO1 = sortArray.indexOf(Long.parseLong(o1.getSyncId()));
                            int indexO2 = sortArray.indexOf(Long.parseLong(o2.getSyncId()));
                            if (indexO1 < indexO2) {
                                return -1;
                            } else {
                                return 1;
                            }
                        } else {
                            return o1.compareTo(o2);
                        }
                    }
                }
            });
        }

        // change weight
        int lastWeight = Integer.MIN_VALUE;
        for (int j = allBubbles.size() - 1; j >= 0; j--) {
            BubbleItem bubbleItem = allBubbles.get(j);
            int nowWeight;
            if (bubbleItem.getWeight() > lastWeight) {
                nowWeight = bubbleItem.getWeight();
            } else {
                nowWeight = lastWeight + 1;
                bubbleItem.setWeightSilent(nowWeight);
                if (!changedBubbles.contains(bubbleItem)) {
                    changedBubbles.add(bubbleItem);
                }
            }
            lastWeight = nowWeight;
        }

        buffer = new StringBuffer();
        for (BubbleItem item : allBubbles) {
            buffer.append(item.getWeight() + ", ");
        }
        log.error("after sort " + buffer.toString());
        log.error("updateBubbleList, item size ["+allBubbles.size()+"]");
        return allBubbles;
    }

    //just for init
    public void updateBubbleList(List<BubbleItem> bubbles) {
        if (bubbles != null && bubbles.size() > 0) {
            synchronized (mVisibleBubblesLock) {
                mVisibleBubbles.clear();
                mBubbleMap.clear();
                mVisibleBubbles.addAll(bubbles);
                for (BubbleItem item : bubbles) {
                    mBubbleMap.put(item.getId(), item);
                }
                log.info("readBubbles over, bubble.size -> " + mVisibleBubbles.size());
            }
            if (mVisibleBubbles.size() > 0) {
                notifyUpdate();
            }
        } else {
            log.error("updateBubbleList by empty bubbles");
        }
    }

    public void handleActionKeyguardOn() {
        LOG.d("hide all bubbles while keyguard on");
        if (Utils.isKeyguardLocked() && !BubbleController.getInstance().isExtDisplay()) {
            BubbleController.getInstance().dismissConfirmDialog();
        }
    }

    public void handleActionKeyguardDismiss() {
        LOG.d("show all bubbles while keyguard dismiss with voice state: " + Constants.IS_IDEA_PILLS_ENABLE);
    }

    public void handleActionHome() {
        List<BubbleItem> addingAttachItems = new ArrayList<>();
        synchronized (mVisibleBubblesLock) {
            for (BubbleItem bubbleItem : mVisibleBubbles) {
                if (bubbleItem.isAddingAttachment()) {
                    addingAttachItems.add(bubbleItem);
                }
            }
        }

        if (StatusManager.getStatus(StatusManager.FORCE_HIDE_WINDOW)
                && Utils.isSetupCompelete(mContext)) {
            BubbleController.getInstance().clearForceHideWindow();
        }
        if (StatusManager.getStatus(StatusManager.ADDING_ATTACHMENT)) {
            if (!BubbleController.getInstance().isExtDisplay()) {
                StatusManager.setStatus(StatusManager.ADDING_ATTACHMENT, false);
            }
        }
    }

    public void notifyUpdate() {
        for (OnUpdateListener lis : mListeners) {
            lis.onUpdate();
        }
    }
    private void notifyBubbleUpdateInner() {
        BubbleController.getInstance().refreshPullViewAlpha();
        for (OnUpdateListener lis : mListeners) {
            lis.onUpdate();
        }
    }
    private void notifyBubbleAddedInner(List<BubbleItem> items) {
        BubbleController.getInstance().refreshPullViewAlpha();
        for (OnUpdateListener lis : mListeners) {
            if (!lis.onBubblesAdd(items)) {
                lis.onUpdate();
            }
        }
    }

    public interface OnUpdateListener {
        boolean onBubblesAdd(List<BubbleItem> bubbles);

        // this method only be invoked if the onBubblesAdd method return false !
        void onUpdate();
    }

    public void reportBubbleNum() {
        TaskHandler.post(new Runnable() {
            @Override
            public void run() {
                int total = 0;
                int note_num = 0;
                int import_num = 0;
                int todo_num = 0;
                int tosend_num = 0;
                int idear_num = 0;
                int share_num = 0;
                int add_num = 0;
                int day_num = 0;
                int time_num = 0;
                int open_num = 0;
                int close_num = 0;
                if (mVisibleBubbles != null && mVisibleBubbles.size() > 0) {
                    total = mVisibleBubbles.size();
                    for (BubbleItem item : mVisibleBubbles) {
                        switch (item.getColor()) {
                            case 4:
                                note_num++;
                                break;
                            case 1:
                                import_num++;
                                break;
                            case 2:
                                todo_num++;
                                break;
                            case 3:
                                tosend_num++;
                                break;
                            case 5:
                                idear_num++;
                                break;
                        }
                        if (item.haveAttachments()) {
                            add_num++;
                        }
                        if (item.isInLargeMode()) {
                            open_num++;
                        } else {
                            close_num++;
                        }
                        if (item.getRemindTime() > 0) {
                            time_num++;
                        } else if (item.getDueDate() > 0) {
                            day_num++;
                        }
                        if (!TextUtils.isEmpty(item.getSyncId())) {
                            share_num++;
                        }
                    }
                }
                LinkedHashMap<String, Object> dataMap = new LinkedHashMap<>();
                final int num = mVisibleBubbles.size();

                dataMap.put("num", total);
                dataMap.put("note", note_num);
                dataMap.put("import", import_num);
                dataMap.put("todo", todo_num);
                dataMap.put("tosend", tosend_num);
                dataMap.put("idear", idear_num);
                dataMap.put("share", share_num);
                dataMap.put("add", add_num);
                dataMap.put("set_day", day_num);
                dataMap.put("set_time", time_num);
                dataMap.put("open_pill", open_num);
                dataMap.put("close_pill", close_num);
                LOG.d("A420018 reportBubbleNum =" + dataMap);
                Tracker.onEvent(BubbleTrackerID.BUBBLE_DAILY_MAX_NUMBER, dataMap);
            }
        });
    }

    public void trackSettingsStatus() {
        TaskHandler.post(new Runnable() {
            @Override
            public void run() {
                ContentResolver cr = mContext.getContentResolver();
                LinkedHashMap<String, Object> dataMap = new LinkedHashMap<>();
                List<ShareItem> mSaveShareList = PackageUtils.getShareItemAvailiableList(mContext);
                StringBuilder packageName = new StringBuilder();
                if (mSaveShareList != null) {
                    for (ShareItem item : mSaveShareList) {
                        packageName.append(item.getPackageName());
                        packageName.append(",");
                    }
                }
                dataMap.put("def_type", Settings.Global.getInt(cr, SettingsSmt.Global.DEFAULT_BUBBLE_TYPE,
                        GlobalBubble.COLOR_BLUE));
                dataMap.put("app_drawer_pkg", packageName);
                dataMap.put("hive_completed", Settings.Global.getInt(cr, "voice_todo_over_cycle_type", 1));
                dataMap.put("app_drawer", Settings.Global.getInt(cr, "app_drawer_enable", 1));
                LOG.d("A131026 reportBubbleNum =" + dataMap);
                Tracker.onStatus("A131026", dataMap);
            }
        });
    }

    public void removeAll() {
        synchronized (mVisibleBubblesLock) {
            mVisibleBubbles.clear();
        }
        notifyUpdate();
    }

    public void mergeVoiceRestoreData(List<BubbleItem> items) {
        if (items == null || items.size() == 0) {
            return;
        }
        log.error("dump restore item");
        for (BubbleItem item : items) {
            log.error(item.getSyncId() + ", " + item.getText());
        }
        for (BubbleItem item : items) {
            if (item == null) {
                continue;
            }
            int id = item.getId();
            BubbleItem oldItem = mBubbleMap.get(id);
            if (oldItem != null) {
                //merge item
                oldItem.setUri(item.getUri());
                oldItem.setType(item.getType());
                oldItem.setVoiceSyncId(item.getVoiceSyncId());
                oldItem.setVoiceVersion(item.getVoiceVersion());
                oldItem.setVoiceEncryptKey(item.getVoiceEncryptKey());
            }
        }
    }

    public void mergeRestoreRemovedData(List<BubbleItem> bubbles) {
        if (bubbles == null || bubbles.size() <= 0) {
            return;
        }
        Map<Integer, BubbleItem> bubbleItemMap = getBubbleMap();
        synchronized (mVisibleBubblesLock) {
            for (BubbleItem bubble : bubbles) {
                if (bubble == null) {
                    continue;
                }
                int id = bubble.getId();
                bubbleItemMap.remove(id);
                BubbleItem memoryRelateBubble = getBubbleItemById(id);
                if (memoryRelateBubble != null) {
                    mVisibleBubbles.remove(memoryRelateBubble);
                    onDismiss(memoryRelateBubble);
                }
            }
        }
    }

    public void mergePendingShareHandleData(List<Integer> bubbleIds) {
        if (bubbleIds == null || bubbleIds.size() <= 0) {
            return;
        }
        Map<Integer, BubbleItem> bubbleItemMap = getBubbleMap();
        synchronized (mVisibleBubblesLock) {
            for (Integer bubbleId : bubbleIds) {
                BubbleItem relateItem = bubbleItemMap.get(bubbleId);
                if (relateItem != null) {
                    relateItem.clearPendingShareStatus();
                }
            }
        }
    }

    public void mergePendingShareInvitationData(List<BubbleItem> pendingShareInvitationBubbles) {
        if (pendingShareInvitationBubbles == null || pendingShareInvitationBubbles.size() <= 0) {
            return;
        }
        Map<Integer, BubbleItem> bubbleItemMap = getBubbleMap();
        synchronized (mVisibleBubblesLock) {
            for (BubbleItem pendingShareInvitationBubble : pendingShareInvitationBubbles) {
                BubbleItem relateItem = bubbleItemMap.get(pendingShareInvitationBubble.getId());
                if (relateItem != null) {
                    relateItem.mergePendingShareStatus(pendingShareInvitationBubble);
                }
            }
        }
        notifyUpdate();
    }

    public void notifySaraCheckOffline() {
        boolean needCheckOffline = false;
        synchronized (mVisibleBubblesLock) {
            if (mVisibleBubbles != null) {
                for (BubbleItem bubble : mVisibleBubbles) {
                    if (bubble == null) {
                        continue;
                    }
                    if (bubble.getType() == GlobalBubble.TYPE_VOICE_OFFLINE) {
                        needCheckOffline = true;
                        break;
                    }
                }
            }
        }
        if (needCheckOffline) {
            DataHandler.handleTask(DataHandler.TASK_CHECK_OFFLINE);
        }
    }

    public void mergeFirstTimeMatchData(List<BubbleItem> matchItems) {
        for (BubbleItem item : matchItems) {
            if (item == null) {
                continue;
            }
            int id = item.getId();
            if (id <= 0) {
                continue;
            }
            BubbleItem oldItem = mBubbleMap.get(id);
            if (oldItem != null) {
                oldItem.setSyncId(item.getSyncId());
                oldItem.setVersion(item.getVersion());
                oldItem.setVoiceBubbleSyncId(item.getVoiceBubbleSyncId());
                oldItem.setUserId(item.getUserId());
            }
        }
    }

    public List<BubbleItem> mergeRestoreData(List<BubbleItem> items, List<Integer> delConflictBubbleIds,
                                             List<String> delConflictAttachmentIds, List<Integer> changeToRemoveBubbleIds,
                                             List<Long> sortList) {
        if (items == null) {
            return null;
        }
        log.error("dump restore item");
        boolean canUseServerOrder = true;
        if (sortList == null || sortList.isEmpty()) {
            canUseServerOrder = false;
        }
        if (canUseServerOrder) {
            for (BubbleItem item : items) {
                if (TextUtils.isEmpty(item.getSyncId())) {
                    canUseServerOrder = false;
                    break;
                } else if (!TextUtils.isEmpty(item.getSyncId())) {
                    try {
                        long syncId = Long.parseLong(item.getSyncId());
                        if (!sortList.contains(syncId)) {
                            canUseServerOrder = false;
                            break;
                        }
                    } catch (Exception e) {
                        canUseServerOrder = false;
                        break;
                    }
                }
            }
        }
        if (canUseServerOrder) {
            for (BubbleItem bubble : mBubbleMap.values()) {
                if (bubble.getModificationFlag(BubbleItem.MF_WEIGHT)) {
                    canUseServerOrder = false;
                    break;
                } else if (TextUtils.isEmpty(bubble.getSyncId())) {
                    canUseServerOrder = false;
                    break;
                } else if (!TextUtils.isEmpty(bubble.getSyncId())) {
                    try {
                        long syncId = Long.parseLong(bubble.getSyncId());
                        if (!sortList.contains(syncId)) {
                            canUseServerOrder = false;
                            break;
                        }
                    } catch (Exception e) {
                        canUseServerOrder = false;
                        break;
                    }
                }
            }
        }

        List<BubbleItem> changedBubbles = new ArrayList<BubbleItem>();
        List<BubbleItem> addedBubbles = new ArrayList<BubbleItem>();
        List<BubbleItem> addedConflictBubbles = new ArrayList<BubbleItem>();
        List<BubbleItem> removedList = new ArrayList<BubbleItem>();
        for (BubbleItem item : items) {
            if (item == null) {
                continue;
            }
            int id = item.getId();
            BubbleItem oldItem = mBubbleMap.get(id);
            if (oldItem == null) {
                //new added
                if (item.isVisibleItem()) {
                    item.setSyncLock(true);
                    mBubbleMap.put(id, item);
                    if (TextUtils.isEmpty(item.getConflictSyncId())
                            || BubbleItem.CONFLICT_HANDLED_TAG.equals(item.getConflictSyncId())) {
                        addedBubbles.add(item);
                    } else {
                        addedConflictBubbles.add(item);
                    }
                }
                if (item.getId() > 0) {
                    changedBubbles.add(item);
                }
            } else {
                //merge item
                oldItem.mergeItem(item);
                if (!oldItem.isVisibleItem()) {
                    removedList.add(oldItem);
                } else if (changeToRemoveBubbleIds != null && changeToRemoveBubbleIds.contains(oldItem.getId())) {
                    removedList.add(oldItem);
                } else {
                    oldItem.setSyncLock(true);
                    if (oldItem.getAttachments() != null) {
                        if (delConflictBubbleIds != null && delConflictBubbleIds.contains(oldItem.getId())) {
                            List<AttachMentItem> delAttachs = new ArrayList<>();
                            for (AttachMentItem attachMentItem : oldItem.getAttachments()) {
                                if (delConflictAttachmentIds.contains(String.valueOf(attachMentItem.getId()))) {
                                    delAttachs.add(attachMentItem);
                                }
                            }
                            oldItem.getAttachments().removeAll(delAttachs);
                        }
                    }
                    changedBubbles.add(oldItem);
                }
            }
        }
        if (removedList.size() > 0) {
            final List<BubbleItem> removed = new ArrayList<BubbleItem>();
            synchronized (mVisibleBubblesLock) {
                Map<Integer, BubbleItem> bubbleItemMap = getBubbleMap();
                for (BubbleItem bubble : removedList) {
                    if (bubble == null) {
                        continue;
                    }
                    int id = bubble.getId();
                    bubbleItemMap.remove(id);
                    if (mVisibleBubbles.remove(bubble)) {
                        onDismiss(bubble);
                        if (id != 0 && (changeToRemoveBubbleIds == null
                                || !changeToRemoveBubbleIds.contains(id))) {
                            removed.add(bubble);
                        }
                    }
                }
            }
            for (BubbleItem removedItem : removed) {
                if (!changedBubbles.contains(removedItem)) {
                    changedBubbles.add(removedItem);
                }
            }
        }

        updateBubbleList(sortAndRebuildWeightBubbles(changedBubbles, addedBubbles, sortList, canUseServerOrder));
        AlarmUtils.scheduleNextAlarm(mContext, null);
        return changedBubbles;
    }

    public void mergeFirstTimeMatchAttachmentData(List<AttachMentItem> matchItems, List<AttachMentItem> delItems) {
        for (AttachMentItem item : matchItems) {
            if (item == null) {
                continue;
            }
            int id = item.getId();
            if (id <= 0 || item.getBubbleId() <= 0) {
                continue;
            }
            BubbleItem oldItem = mBubbleMap.get(item.getBubbleId());
            if (oldItem != null && oldItem.getAttachments() != null) {
                for (AttachMentItem memoryAttachment : oldItem.getAttachments()) {
                    if (memoryAttachment.getId() > 0 && memoryAttachment.getId() == id) {
                        memoryAttachment.setBubbleSyncId(item.getBubbleSyncId());
                        memoryAttachment.setVersion(item.getVersion());
                        memoryAttachment.setSyncId(item.getSyncId());
                        memoryAttachment.setUserId(item.getUserId());
                        memoryAttachment.setSyncEncryptKey(item.getSyncEncryptKey());
                        break;
                    }
                }
            }
        }

        for (AttachMentItem item : delItems) {
            if (item == null) {
                continue;
            }
            int id = item.getId();
            if (id <= 0 || item.getBubbleId() <= 0) {
                continue;
            }
            BubbleItem oldItem = mBubbleMap.get(item.getBubbleId());
            List<AttachMentItem> attachMentItems;
            if (oldItem != null && (attachMentItems = oldItem.getAttachments()) != null) {
                AttachMentItem delMemoryItem = null;
                for (AttachMentItem memoryAttachment : attachMentItems) {
                    if (memoryAttachment.getId() > 0 && memoryAttachment.getId() == id) {
                        delMemoryItem = memoryAttachment;
                        break;
                    }
                }
                if (delMemoryItem != null) {
                    attachMentItems.remove(delMemoryItem);
                }
            }
        }
    }

    public void mergeRestoreAttachmentData(List<AttachMentItem> addItems) {
        if (addItems == null || addItems.size() == 0) {
            return;
        }
        log.error("dump restore item");
        for (AttachMentItem item : addItems) {
            log.error(item.getSyncId() + "");
        }
        List<BubbleItem> changedBubbles = new ArrayList<>();
        for (AttachMentItem item : addItems) {
            if (item == null) {
                continue;
            }
            int id = item.getBubbleId();
            BubbleItem bubbleItem = getBubbleItemById(id);
            if (bubbleItem != null) {
                bubbleItem.addAttachment(item);
                bubbleItem.setHeightInLarge(-1);
                bubbleItem.setNormalWidth(-1);
                bubbleItem.setRefreshAttachment(true);
                if (!changedBubbles.contains(bubbleItem)) {
                    changedBubbles.add(bubbleItem);
                }
            }
        }
        for (BubbleItem changedBubble : changedBubbles) {
            Collections.sort(changedBubble.getAttachments());
        }
        notifyUpdate();
    }

    public void updateBubbleItemSyncTime(List<Integer> idList, long syncTime) {
        if (idList == null || idList.size() == 0) {
            return;
        }
        for (Integer id : idList) {
            BubbleItem item = mBubbleMap.get(id);
            if (item == null) {
                continue;
            }
            item.setRequestSyncTime(syncTime);
        }
    }

    public void syncBubbleLockFinish() {
        for (BubbleItem item : mBubbleMap.values()) {
            if (item == null) {
                continue;
            }
            item.setRequestSyncTime(0);
            item.setSyncLock(false);
        }
    }

    public void clearDirtySyncInfo(long userId) {
        for (BubbleItem item : mBubbleMap.values()) {
            if (item == null || item.isShareFromOthers()) {
                continue;
            }
            if (item.getUserId() != userId) {
                item.setSyncId(null);
                item.setVoiceBubbleSyncId(null);
                item.setVoiceSyncId(null);
                item.setVoiceEncryptKey(null);
                item.setVersion(0);
                item.setUserId(-1);

                if (item.getAttachments() != null) {
                    for (AttachMentItem attachMentItem : item.getAttachments()) {
                        attachMentItem.setSyncId(null);
                        attachMentItem.setSyncEncryptKey(null);
                        attachMentItem.setBubbleSyncId(null);
                        attachMentItem.setUserId(-1);
                    }
                }
            }
        }
    }

    public void syncLogout() {
        List<BubbleItem> toRemoved = new ArrayList<>();
        boolean changedShareColor = false;
        for (BubbleItem item : mBubbleMap.values()) {
            if (item == null) {
                continue;
            }
            if (item.isShareFromOthers()) {
                toRemoved.add(item);
                continue;
            }
            if (item.isShareColor()) {
                item.setShareStatusSilent(GlobalBubble.SHARE_STATUS_NOT_SHARED);
                item.clearPendingShareStatus();
                changedShareColor = true;
            }
            item.setSyncId(null);
            item.setVoiceBubbleSyncId(null);
            item.setVoiceSyncId(null);
            item.setVoiceEncryptKey(null);
            item.setVersion(0);
            item.setUserId(-1);

            if (item.getAttachments() != null) {
                for (AttachMentItem attachMentItem : item.getAttachments()) {
                    attachMentItem.setSyncId(null);
                    attachMentItem.setSyncEncryptKey(null);
                    attachMentItem.setBubbleSyncId(null);
                    attachMentItem.setUserId(-1);
                }
            }
            if (item.isEmptyBubble()) {
                toRemoved.add(item);
            }
        }
        if (!toRemoved.isEmpty() || changedShareColor) {
            synchronized (mVisibleBubblesLock) {
                for (BubbleItem removedItem : toRemoved) {
                    mBubbleMap.remove(removedItem.getId());
                    mVisibleBubbles.remove(removedItem);
                }
            }
            notifyBubbleUpdateInner();
        }
    }

    public boolean linkAttachMentsToBubble(List<AttachMentItem> list, int bubbleId) {
        BubbleItem bubbleItem = getBubbleItemById(bubbleId);
        if (bubbleItem != null) {
            bubbleItem.addAttachments(list);
            bubbleItem.setIsAddingAttachmentLock(false);
            Collections.sort(bubbleItem.getAttachments());
            handleConflict(bubbleItem, true);
            bubbleItem.setHeightInLarge(-1);
            bubbleItem.setRefreshAttachment(true);
            bubbleItem.setHasChangedBubbleWithoutSync(bubbleItem.isNeedInput());
            notifyUpdate();
            return !bubbleItem.isNeedInput();
        }
        return false;
    }

    public void removeAttachmentImgNoIn(int bubbleid, ArrayList<Uri> list) {
        BubbleItem bubbleItem = getBubbleItemById(bubbleid);
        if (bubbleItem != null) {
            boolean changed = false;
            List<AttachMentItem> list1 = bubbleItem.getAttachments();
            List<AttachMentItem> toRemove = new ArrayList<AttachMentItem>();
            for (int i = list1.size() - 1; i >= 0; i--) {
                AttachMentItem item = list1.get(i);
                if (item.getType() == AttachMentItem.TYPE_IMAGE) {
                    if (!list.contains(item.getUri())
                            && item.getType() != AttachMentItem.DOWNLOAD_STATUS_DOWNLOAD_SUCCESS) {
                        list1.remove(i);
                        changed = true;
                        toRemove.add(item);
                    }
                }
            }
            if (changed) {
                handleConflict(bubbleItem, true);
                bubbleItem.setHeightInLarge(-1);
                notifyUpdate();
                List params = new ArrayList();
                params.add(toRemove);
                if (bubbleItem.isNeedInput()) {
                    bubbleItem.setHasChangedBubbleWithoutSync(true);
                    DataHandler.handleTask(DataHandler.TASK_REMOVE_ATTACHMENTS, params);
                } else {
                    bubbleItem.setHasChangedBubbleWithoutSync(false);
                    DataHandler.handleTask(DataHandler.TASK_REMOVE_ATTACHMENTS_AND_SYNC, params);
                }
            }
        }
    }

    public void removeAttachment(AttachMentItem item) {
        BubbleItem bubbleItem = getBubbleItemById(item.getBubbleId());
        if (bubbleItem != null) {
            List<AttachMentItem> list1 = bubbleItem.getAttachments();
            List<AttachMentItem> toRemove = new ArrayList<AttachMentItem>();
            if (list1.remove(item)) {
                toRemove.add(item);
                handleConflict(bubbleItem, true);
                bubbleItem.setHeightInLarge(-1);
                List params = new ArrayList();
                params.add(toRemove);
                if (bubbleItem.isNeedInput()) {
                    bubbleItem.setHasChangedBubbleWithoutSync(true);
                    DataHandler.handleTask(DataHandler.TASK_REMOVE_ATTACHMENTS, params);
                } else {
                    bubbleItem.setHasChangedBubbleWithoutSync(false);
                    DataHandler.handleTask(DataHandler.TASK_REMOVE_ATTACHMENTS_AND_SYNC, params);
                }
            }
        }
    }

    public void mergeRestoreRemoveAttachments(List<AttachMentItem> attachMentItems) {
        if (attachMentItems == null || attachMentItems.size() <= 0) {
            return;
        }
        List<AttachMentItem> toRemove = new ArrayList<AttachMentItem>();
        for (AttachMentItem attachMentItem : attachMentItems) {
            if (attachMentItem == null) {
                continue;
            }
            BubbleItem bubbleItem = getBubbleItemById(attachMentItem.getBubbleId());
            if (bubbleItem != null) {
                List<AttachMentItem> list1 = bubbleItem.getAttachments();
                AttachMentItem toDelItem = null;
                for (AttachMentItem memoryAttachmentItem : list1) {
                    if (memoryAttachmentItem.getId() == attachMentItem.getId()) {
                        toDelItem = memoryAttachmentItem;
                        toRemove.add(attachMentItem);
                        break;
                    }
                }
                if (toDelItem != null) {
                    list1.remove(toDelItem);
                    bubbleItem.setHeightInLarge(-1);
                }
            }
        }
        if (toRemove.size() > 0) {
            notifyUpdate();
            List params = new ArrayList();
            params.add(toRemove);
            DataHandler.handleTask(DataHandler.TASK_REMOVE_ATTACHMENTS, params);
        }
    }


    public List<BubbleItem> getBubbleItemsFrom(List<GlobalBubble> bubbles) {
        List<BubbleItem> result = new ArrayList<BubbleItem>();
        if (bubbles != null && bubbles.size() > 0) {
            for (GlobalBubble bubble : bubbles) {
                result.add(new BubbleItem(bubble));
            }
        }
        return result;
    }

    public List<BubbleItem> getBubbleItemsFrom(List<GlobalBubble> bubbles,
                                               List<GlobalBubbleAttach> attaches) {
        List<BubbleItem> bubbleItems = new ArrayList<BubbleItem>();
        if (bubbles != null && bubbles.size() > 0) {
            for (int i = 0; i < bubbles.size(); i++) {
                GlobalBubble bubble = bubbles.get(i);
                BubbleItem bubbleItem = new BubbleItem(bubble);
                // because when attachs is not empty, bubbles count always be 1(sara add).it is dirty now
                if (i == 0 && attaches != null && attaches.size() > 0) {
                    int attachmentCount = attaches.size();
                    if (attachmentCount > AttachmentUtils.ATTACHMENT_QUANTITY_LIMIT) {
                        attachmentCount = AttachmentUtils.ATTACHMENT_QUANTITY_LIMIT;
                    }
                    List<AttachMentItem> attachMentItems = new ArrayList<AttachMentItem>();
                    for (int j = 0; j < attachmentCount; j++) {
                        AttachMentItem attachMentItem = AttachMentItem.fromGlobalBubbleAttach(attaches.get(j));
                        attachMentItems.add(attachMentItem);
                    }
                    bubbleItem.addAttachments(attachMentItems);
                    Collections.sort(bubbleItem.getAttachments());
                }
                bubbleItems.add(bubbleItem);
            }
        }
        return bubbleItems;
    }

    public BubbleItem getBubbleByConflictSyncId(String bubbleConflictSyncId) {
        BubbleItem item = null;
        synchronized (mVisibleBubblesLock) {
            if (mVisibleBubbles != null) {
                for (BubbleItem bubble : mVisibleBubbles) {
                    if (bubble == null) {
                        continue;
                    }
                    if (!TextUtils.isEmpty(bubbleConflictSyncId) && !BubbleItem.CONFLICT_HANDLED_TAG.equals(bubbleConflictSyncId)
                            && bubbleConflictSyncId.equals(bubble.getConflictSyncId())) {
                        item = bubble;
                        break;
                    }
                }
            }
        }
        return item;
    }

    public boolean handleConflict(BubbleItem bubbleItem) {
        return handleConflict(bubbleItem, false);
    }

    public boolean handleConflict(BubbleItem bubbleItem, boolean updateItemToDb) {
        BubbleItem conflictItem = null;
        boolean isSelfConflict = false;
        if (!TextUtils.isEmpty(bubbleItem.getConflictSyncId())) {
            conflictItem = bubbleItem;
            isSelfConflict = true;
        } else if (!TextUtils.isEmpty(bubbleItem.getSyncId())) {
            conflictItem = getBubbleByConflictSyncId(bubbleItem.getSyncId());
            isSelfConflict = false;
        }
        if (conflictItem != null) {
            conflictItem.setConflictSyncId(BubbleItem.CONFLICT_HANDLED_TAG);
            if ((!isSelfConflict || updateItemToDb) && conflictItem.getId() > 0) {
                List<ContentValues> values = new ArrayList<ContentValues>();
                ContentValues cv = new ContentValues();
                cv.put(BUBBLE.ID, conflictItem.getId());
                cv.put(BUBBLE.CONFLICT_SYNC_ID, conflictItem.getConflictSyncId());
                cv.put(BUBBLE.MODIFY_FLAG, conflictItem.getModificationFlag());
                cv.put(BUBBLE.SECONDARY_MODIFY_FLAG, conflictItem.getSecondaryModifyFlag());
                values.add(cv);
                List params = new ArrayList();
                params.add(values);
                DataHandler.handleTask(DataHandler.TASK_UPDATE_BUBBLE_BY_ID, params);
            }
            if (!isSelfConflict) {
                notifyUpdate();
            }
        }
        return conflictItem != null;
    }

    public void handleCancelShare(long userId) {
        List<BubbleItem> toRemoved = new ArrayList<>();
        for (BubbleItem item : mBubbleMap.values()) {
            if (item == null) {
                continue;
            }
            if (item.isShareFromOthers() && item.getUserId() == userId) {
                toRemoved.add(item);
            }
        }
        if (!toRemoved.isEmpty()) {
            synchronized (mVisibleBubblesLock) {
                for (BubbleItem removedItem : toRemoved) {
                    mBubbleMap.remove(removedItem.getId());
                    mVisibleBubbles.remove(removedItem);
                }
            }
            notifyBubbleUpdateInner();
        }
        List params = new ArrayList();
        params.add(userId);
        DataHandler.handleTask(DataHandler.TASK_CANCEL_SHARE_RELATION, params);
    }
}