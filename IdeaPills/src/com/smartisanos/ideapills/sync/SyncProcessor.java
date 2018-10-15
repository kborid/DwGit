package com.smartisanos.ideapills.sync;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.ResultReceiver;
import android.service.onestep.GlobalBubble;
import android.text.TextUtils;
import android.util.Pair;
import android.widget.Toast;

import com.smartisanos.ideapills.BubbleController;
import com.smartisanos.ideapills.Constants;
import com.smartisanos.ideapills.IdeaPillsApp;
import com.smartisanos.ideapills.R;
import com.smartisanos.ideapills.common.sync.SyncUtil;
import com.smartisanos.ideapills.data.ATTACHMENT;
import com.smartisanos.ideapills.data.BUBBLE;
import com.smartisanos.ideapills.data.BubbleDB;
import com.smartisanos.ideapills.data.DBHelper;
import com.smartisanos.ideapills.data.DataHandler;
import com.smartisanos.ideapills.remind.util.AlarmUtils;
import com.smartisanos.ideapills.sync.entity.SyncPushItem;
import com.smartisanos.ideapills.sync.entity.SyncShareInvitation;
import com.smartisanos.ideapills.sync.entity.SyncShareUser;
import com.smartisanos.ideapills.sync.share.GlobalInvitationAction;
import com.smartisanos.ideapills.sync.share.SyncShareManager;
import com.smartisanos.ideapills.sync.share.SyncShareRepository;
import com.smartisanos.ideapills.sync.share.SyncShareUtils;
import com.smartisanos.ideapills.util.GlobalBubbleManager;
import com.smartisanos.ideapills.util.GlobalBubbleUtils;
import com.smartisanos.ideapills.util.LOG;
import com.smartisanos.ideapills.util.Utils;
import com.smartisanos.ideapills.entity.AttachMentItem;
import com.smartisanos.ideapills.entity.BubbleItem;
import com.smartisanos.ideapills.common.util.UIHandler;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * All sync data logic in IdeaPills handled at here
 */
public class SyncProcessor {
    private static final LOG log = LOG.getInstance(SyncProcessor.class);

    public static final String BUNDLE_DELETE = "delete";
    public static final String BUNDLE_REPLACE = "replace";
    public static final String BUNDLE_ADDED = "added";
    public static final String BUNDLE_IS_FIRST_LOGIN = "isFirstLogin";
    public static final String BUNDLE_SORT = "pillsort";

    private static final String BATCH_BUNDLE_IS_DONE = "isdone";
    private static final String BATCH_BUNDLE_SEQUENCE = "sequence";

    private static final int BATCH_BUNDLE_LIMIT = 400;

    private static volatile boolean sInSyncing = false;
    private static Bundle sTempRestoreBundle = null;
    private static Bundle sTempPrepareBundle = null;
    private static int sTempPrepareBundleSeq = 0;
    private static int sTempPrepareBundleLastFrom = -1;

    private static List<AttachMentItem> sTempLastTimeLoseAttachments = new ArrayList<>();
    private static List<AttachMentItem> sTempLastTimeLoseShareAttachments = new ArrayList<>();

    private static SyncFileLogger sSyncFileLogger;

    static {
        try {
            sSyncFileLogger = new SyncFileLogger();
        } catch (IOException e) {
            log.error(e.toString());
        }
    }

    public static boolean isInSyncing() {
        return sInSyncing;
    }

    private static Bundle mergeToLocalBundleRetFinal(Bundle bundle) {
        if (bundle.getBoolean(BATCH_BUNDLE_IS_DONE, true)) {
            mergeToLocalBundle(bundle);
            bundle = sTempRestoreBundle;
            sTempRestoreBundle = null;
            return bundle;
        } else {
            mergeToLocalBundle(bundle);
            return null;
        }
    }

    private static void mergeToLocalBundle(Bundle bundle) {
        if (bundle.getInt(BATCH_BUNDLE_SEQUENCE, 0) == 0) {
            sTempRestoreBundle = bundle;
        } else {
            if (sTempRestoreBundle == null) {
                sTempRestoreBundle = bundle;
            } else {
                List<ContentValues> deleteListOrigin = sTempRestoreBundle.getParcelableArrayList(BUNDLE_DELETE);
                ArrayList<ContentValues> replaceListOrigin = sTempRestoreBundle.getParcelableArrayList(BUNDLE_REPLACE);
                ArrayList<ContentValues> deleteListNew = bundle.getParcelableArrayList(BUNDLE_DELETE);
                ArrayList<ContentValues> replaceListNew = bundle.getParcelableArrayList(BUNDLE_REPLACE);
                if (deleteListNew != null) {
                    if (deleteListOrigin == null) {
                        sTempRestoreBundle.putParcelableArrayList(BUNDLE_DELETE, deleteListNew);
                    } else {
                        deleteListOrigin.addAll(deleteListNew);
                    }
                }
                if (replaceListNew != null) {
                    if (replaceListOrigin == null) {
                        sTempRestoreBundle.putParcelableArrayList(BUNDLE_REPLACE, replaceListNew);
                    } else {
                        replaceListOrigin.addAll(replaceListNew);
                    }
                }
            }
        }
    }

    private static boolean needGenPrepareBundle(int prepareBundleFrom) {
        if (prepareBundleFrom != sTempPrepareBundleLastFrom) {
            sTempPrepareBundle = null;
            sTempPrepareBundleSeq = 0;
        }
        sTempPrepareBundleLastFrom = prepareBundleFrom;
        return sTempPrepareBundleSeq == 0;
    }

    private static void setPrepareBundle(Bundle bundle) {
        sTempPrepareBundle = bundle;
    }

    private static Bundle batchGetPrepareBundle() {
        if (sTempPrepareBundle == null) {
            sTempPrepareBundleSeq = 0;
            return new Bundle();
        }
        final ArrayList<ContentValues> addList = sTempPrepareBundle.getParcelableArrayList(BUNDLE_ADDED);
        final ArrayList<ContentValues> replaceList = sTempPrepareBundle.getParcelableArrayList(BUNDLE_REPLACE);
        final ArrayList<ContentValues> delList = sTempPrepareBundle.getParcelableArrayList(BUNDLE_DELETE);

        int addListLength = addList == null ? 0 : addList.size();
        int replaceListLength = replaceList == null ? 0 : replaceList.size();
        int delListLength = delList == null ? 0 : delList.size();
        int totalLength = addListLength + replaceListLength + delListLength;
        Bundle retBundle;
        if (totalLength <= BATCH_BUNDLE_LIMIT) {
            retBundle = sTempPrepareBundle;
            retBundle.putInt(BATCH_BUNDLE_SEQUENCE, 0);
            retBundle.putBoolean(BATCH_BUNDLE_IS_DONE, true);
            sTempPrepareBundle = null;
            sTempPrepareBundleSeq = 0;
            return retBundle;
        }

        retBundle = new Bundle();
        int beginIndex = sTempPrepareBundleSeq * BATCH_BUNDLE_LIMIT;
        int endIndex = (sTempPrepareBundleSeq + 1) * BATCH_BUNDLE_LIMIT - 1;
        if (endIndex >= totalLength - 1) {
            retBundle.putInt(BATCH_BUNDLE_SEQUENCE, sTempPrepareBundleSeq);
            retBundle.putBoolean(BATCH_BUNDLE_IS_DONE, true);
            sTempPrepareBundle = null;
            sTempPrepareBundleSeq = 0;
        } else {
            retBundle.putInt(BATCH_BUNDLE_SEQUENCE, sTempPrepareBundleSeq);
            retBundle.putBoolean(BATCH_BUNDLE_IS_DONE, false);
            sTempPrepareBundleSeq++;
        }

        if (beginIndex < addListLength) {
            ArrayList<ContentValues> batchAddList = new ArrayList<>();
            if (endIndex > addListLength - 1) {
                batchAddList.addAll(addList.subList(beginIndex, addListLength));
                retBundle.putParcelableArrayList(BUNDLE_ADDED, batchAddList);
                beginIndex = 0;
                endIndex = endIndex - addListLength;
            } else {
                batchAddList.addAll(addList.subList(beginIndex, endIndex + 1));
                retBundle.putParcelableArrayList(BUNDLE_ADDED, batchAddList);
                return retBundle;
            }
        } else {
            beginIndex = beginIndex - addListLength;
            endIndex = endIndex - addListLength;
        }
        if (beginIndex < replaceListLength) {
            ArrayList<ContentValues> batchReplaceList = new ArrayList<>();
            if (endIndex > replaceListLength - 1) {
                batchReplaceList.addAll(replaceList.subList(beginIndex, replaceListLength));
                retBundle.putParcelableArrayList(BUNDLE_REPLACE, batchReplaceList);
                beginIndex = 0;
                endIndex = endIndex - replaceListLength;
            } else {
                batchReplaceList.addAll(replaceList.subList(beginIndex, endIndex + 1));
                retBundle.putParcelableArrayList(BUNDLE_REPLACE, batchReplaceList);
                return retBundle;
            }
        } else {
            beginIndex = beginIndex - replaceListLength;
            endIndex = endIndex - replaceListLength;
        }
        if (beginIndex < delListLength) {
            ArrayList<ContentValues> batchDelList = new ArrayList<>();
            if (endIndex > delListLength - 1) {
                batchDelList.addAll(delList.subList(beginIndex, delListLength));
                retBundle.putParcelableArrayList(BUNDLE_DELETE, batchDelList);
                return retBundle;
            } else {
                batchDelList.addAll(delList.subList(beginIndex, endIndex + 1));
                retBundle.putParcelableArrayList(BUNDLE_DELETE, batchDelList);
                return retBundle;
            }
        } else {
            return retBundle;
        }
    }

    private static void clearPrepareBundle() {
        sTempPrepareBundle = null;
        sTempPrepareBundleSeq = 0;
    }

    public static Bundle syncRestore(Bundle bundle) {
        sInSyncing = true;
        if (bundle == null) {
            log.error("syncRestore return by bundle is null");
            return new Bundle();
        }
        bundle = mergeToLocalBundleRetFinal(bundle);
        if (bundle == null) {
            return new Bundle();
        }
        final long userId = SyncManager.getCloudAccountUserId(IdeaPillsApp.getInstance());
        boolean isFirstLogin = bundle.getBoolean(BUNDLE_IS_FIRST_LOGIN);
        if (isFirstLogin) {
            firstLoginClearDirtySyncInfo(userId);
            UIHandler.post(new Runnable() {
                @Override
                public void run() {
                    GlobalBubbleManager.getInstance().clearDirtySyncInfo(userId);
                }
            });
            firstLoginMatchBubble(bundle, userId);
        }

        restoreDeleteBubbleInternal(bundle, false);
        restoreReplaceBubbleInternal(bundle, userId, false);
        SyncShareManager.INSTANCE.clearToNotShareFromOtherItems();
        return new Bundle();
    }

    public static Bundle getPreparedData() {
        if (needGenPrepareBundle(1)) {
            setPrepareBundle(getPreparedDataInternal(false));
        }
        return batchGetPrepareBundle();
    }

    public static Bundle handleSyncResult(Bundle bundle) {
        clearPrepareBundle();
        return handleSyncResultInternal(bundle, false);
    }

    public static Bundle syncRestoreAttachment(Bundle bundle) {
        if (bundle == null) {
            log.error("syncRestore return by bundle is null");
            return new Bundle();
        }
        bundle = mergeToLocalBundleRetFinal(bundle);
        if (bundle == null) {
            return new Bundle();
        }
        boolean isFirstLogin = bundle.getBoolean(BUNDLE_IS_FIRST_LOGIN);
        long userId = SyncManager.getCloudAccountUserId(IdeaPillsApp.getInstance());
        if (isFirstLogin) {
            firstLoginMatchAttachment(bundle, userId);
        }

        return syncRestoreAttachmentInternal(bundle, userId, false);
    }

    public static Bundle getPreparedAttachmentData() {
        if (needGenPrepareBundle(0)) {
            setPrepareBundle(getPreparedAttachmentDataInternal(false));
        }
        return batchGetPrepareBundle();
    }

    public static void handleSyncAttachmentResult(Bundle bundle) {
        clearPrepareBundle();
        handleSyncAttachmentResultInternal(bundle, false);
    }

    public static Bundle shareSyncRestore(Bundle bundle) {
        if (bundle == null) {
            log.error("shareSyncRestore return by bundle is null");
            return new Bundle();
        }
        bundle = mergeToLocalBundleRetFinal(bundle);
        if (bundle == null) {
            return new Bundle();
        }
        long userId = SyncManager.getCloudAccountUserId(IdeaPillsApp.getInstance());
        restoreDeleteBubbleInternal(bundle, true);
        restoreReplaceBubbleInternal(bundle, userId, true);
        return new Bundle();
    }

    public static Bundle getSharePreparedData() {
        if (needGenPrepareBundle(3)) {
            setPrepareBundle(getPreparedDataInternal(true));
        }
        return batchGetPrepareBundle();
    }

    public static Bundle handleShareSyncResult(Bundle bundle) {
        clearPrepareBundle();
        return handleSyncResultInternal(bundle, true);
    }

    public static Bundle shareSyncRestoreAttachment(Bundle bundle) {
        if (bundle == null) {
            log.error("shareSyncRestoreAttachment return by bundle is null");
            return new Bundle();
        }
        bundle = mergeToLocalBundleRetFinal(bundle);
        if (bundle == null) {
            return new Bundle();
        }
        long userId = SyncManager.getCloudAccountUserId(IdeaPillsApp.getInstance());
        return syncRestoreAttachmentInternal(bundle, userId, true);
    }

    public static Bundle getSharePreparedAttachmentData() {
        if (needGenPrepareBundle(2)) {
            setPrepareBundle(getPreparedAttachmentDataInternal(true));
        }
        return batchGetPrepareBundle();
    }

    public static void handleShareSyncAttachmentResult(Bundle bundle) {
        clearPrepareBundle();
        handleSyncAttachmentResultInternal(bundle, true);
    }

    private static void firstLoginClearDirtySyncInfo(long userId) {
        BubbleDB.clearBubbleSyncInfo(BUBBLE.USER_ID + " != -1 AND "
                + BUBBLE.USER_ID + " != " + userId
                + " AND " + BUBBLE.WHERE_SELF_DATA);
    }

    private static void firstLoginMatchBubble(Bundle bundle, long userId) {
        // local data merge
        List<BubbleItem> allBubblesLocal = BubbleDB.listBubble(BUBBLE.WHERE_CASE_NOT_DELETE
                + " AND " + BUBBLE.WHERE_NOT_LEGACY_USED);
        Map<Long, List<BubbleItem>> localItemMap = new HashMap<Long, List<BubbleItem>>();
        if (allBubblesLocal != null && allBubblesLocal.size() > 0) {
            for (BubbleItem item : allBubblesLocal) {
                long modifyTextDate = item.getTimeStamp();
                if (modifyTextDate > 0) {
                    List<BubbleItem> relateItems = localItemMap.get(modifyTextDate);
                    if (relateItems == null) {
                        relateItems = new ArrayList<>();
                        relateItems.add(item);
                        localItemMap.put(modifyTextDate, relateItems);
                    } else {
                        relateItems.add(item);
                    }
                }
            }
        }

        final ArrayList<ContentValues> replaceList = bundle.getParcelableArrayList(BUNDLE_REPLACE);
        List<ContentValues> matchBubbleCvList = new ArrayList<ContentValues>();
        final List<BubbleItem> matchBubbleList = new ArrayList<BubbleItem>();
        if (replaceList != null && replaceList.size() > 0) {
            for (ContentValues value : replaceList) {
                if (value == null) {
                    continue;
                }
                Long modifyTextDate = value.getAsLong(SyncDataConverter.BUBBLE_TIME_STAMP);
                if (modifyTextDate == null) {
                    continue;
                }
                List<BubbleItem> relateItems = localItemMap.get(modifyTextDate);
                if (relateItems != null) {
                    for (BubbleItem relateItem : relateItems) {
                        if (!TextUtils.isEmpty(relateItem.getSyncId())) {
                            continue;
                        }

                        String syncId = value.getAsString(SyncDataConverter.BUBBLE_SYNC_ID);
                        Integer version = value.getAsInteger(SyncDataConverter.BUBBLE_VERSION);
                        if (!TextUtils.isEmpty(syncId)) {
                            String contentText = value.getAsString(SyncDataConverter.BUBBLE_TEXT);
                            boolean findMatchedBubble = false;
                            if (contentText == null && relateItem.getText() == null) {
                                findMatchedBubble = true;
                            } else if (contentText != null && relateItem.getText() != null) {
                                if (contentText.length() == relateItem.getText().length()) {
                                    if (contentText.equals(relateItem.getText())) {
                                        findMatchedBubble = true;
                                    }
                                }
                            }

                            if (findMatchedBubble) {
                                ContentValues cv = new ContentValues();
                                cv.put(BUBBLE.ID, relateItem.getId());
                                relateItem.setSyncId(syncId);
                                relateItem.setVoiceBubbleSyncId(syncId);
                                relateItem.setUserId(userId);
                                cv.put(BUBBLE.SYNC_ID, syncId);
                                cv.put(BUBBLE.VOICE_BUBBLE_SYNC_ID, syncId);
                                cv.put(BUBBLE.USER_ID, userId);
                                if (version != null) {
                                    relateItem.setVersion(version);
                                    cv.put(BUBBLE.VERSION, version);
                                }
                                matchBubbleList.add(relateItem);
                                matchBubbleCvList.add(cv);
                                break;
                            }
                        }
                    }
                }
            }

            if (!matchBubbleCvList.isEmpty()) {
                updateBubbles(matchBubbleCvList, true);
                UIHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        GlobalBubbleManager.getInstance().mergeFirstTimeMatchData(matchBubbleList);
                    }
                });
            }
        }
    }

    private static void firstLoginMatchAttachment(Bundle bundle, long userId) {
        final ArrayList<ContentValues> replaceList = bundle.getParcelableArrayList(BUNDLE_REPLACE);
        if (replaceList != null && replaceList.size() > 0) {
            Map<Integer, String> idMap = BubbleDB.listBubbleIdsAndSyncIds(BUBBLE.WHERE_CASE_NOT_DELETE);
            List<AttachMentItem> allAttachmentsLocal = BubbleDB.listAttachments(ATTACHMENT.MARK_DELETE + "=0");
            Map<String, List<AttachMentItem>> localItemMap = new HashMap<String, List<AttachMentItem>>();
            if (allAttachmentsLocal != null && allAttachmentsLocal.size() > 0) {
                for (AttachMentItem item : allAttachmentsLocal) {
                    int bubbleId = item.getBubbleId();
                    String bubbleSyncId = idMap.get(bubbleId);
                    if (!TextUtils.isEmpty(bubbleSyncId)) {
                        List<AttachMentItem> relateItems = localItemMap.get(bubbleSyncId);
                        if (relateItems == null) {
                            relateItems = new ArrayList<>();
                            relateItems.add(item);
                            localItemMap.put(bubbleSyncId, relateItems);
                        } else {
                            relateItems.add(item);
                        }
                    }
                }
            }

            List<ContentValues> matchAttachmentCvList = new ArrayList<ContentValues>();
            final List<AttachMentItem> matchAttachmentList = new ArrayList<AttachMentItem>();
            Set<String> handledSyncIds = new HashSet<>();
            for (ContentValues value : replaceList) {
                if (value == null) {
                    continue;
                }
                String bubbleSyncId = value.getAsString(SyncDataConverter.ATTACHMENT_PILL_ID);
                Integer type = value.getAsInteger(SyncDataConverter.ATTACHMENT_TYPE);
                String syncId = value.getAsString(SyncDataConverter.ATTACHMENT_SYNC_ID);
                Integer version = value.getAsInteger(SyncDataConverter.ATTACHMENT_VERSION);
                byte[] encryptKeyArray = value.getAsByteArray(SyncDataConverter.ATTACHMENT_ENCRYPT_KEY);
                if (type == null || TextUtils.isEmpty(bubbleSyncId)|| TextUtils.isEmpty(syncId)
                        || encryptKeyArray == null) {
                    continue;
                }
                if (version == null) {
                    version = 0;
                }
                if (type != SyncMediaTypeHelper.ATTACHMENT_TYPE_VOICE_RECORD) {
                    List<AttachMentItem> relateItems = localItemMap.get(bubbleSyncId);
                    if (relateItems != null) {
                        for (AttachMentItem relateItem : relateItems) {
                            if (!TextUtils.isEmpty(relateItem.getSyncId())) {
                                continue;
                            }

                            Long createdAt = value.getAsLong(SyncDataConverter.ATTACHMENT_CREATE_AT);
                            boolean findMatchedAttachment = false;
                            if (createdAt != null && createdAt > 0 && relateItem.getCreateAt() > 0 && createdAt == relateItem.getCreateAt()) {
                                findMatchedAttachment = true;
                            } else if (createdAt == null || relateItem.getCreateAt() == 0) {
                                String fileName = value.getAsString(SyncDataConverter.ATTACHMENT_FILENAME);
                                if (relateItem.getFilename() != null && fileName != null &&
                                        relateItem.getFilename().length() == fileName.length()) {
                                    if (fileName.equals(relateItem.getFilename())) {
                                        findMatchedAttachment = true;
                                    }
                                }
                            }

                            if (findMatchedAttachment) {
                                handledSyncIds.add(bubbleSyncId);
                                String encryptKey = SyncDataConverter.byteArrayToHexStr(encryptKeyArray);
                                ContentValues cv = new ContentValues();
                                cv.put(ATTACHMENT.ID, relateItem.getId());
                                relateItem.setSyncId(syncId);
                                relateItem.setUserId(userId);
                                relateItem.setSyncEncryptKey(encryptKey);
                                relateItem.setBubbleSyncId(bubbleSyncId);
                                relateItem.setVersion(version);
                                cv.put(ATTACHMENT.SYNC_ID, syncId);
                                cv.put(ATTACHMENT.USER_ID, userId);
                                cv.put(ATTACHMENT.BUBBLE_SYNC_ID, bubbleSyncId);
                                cv.put(ATTACHMENT.VERSION, version);
                                cv.put(ATTACHMENT.SYNC_ENCRYPT_KEY, encryptKey);
                                matchAttachmentCvList.add(cv);
                                matchAttachmentList.add(relateItem);
                                break;
                            }
                        }
                    }
                }
            }
            if (!matchAttachmentCvList.isEmpty()) {
                updateAttachments(matchAttachmentCvList);
            }
            final List<String> delAttachmentIds = new ArrayList<>();
            final List<AttachMentItem> delAttachmentList = new ArrayList<AttachMentItem>();
            for (String syncId : handledSyncIds) {
                List<AttachMentItem> relateItems = localItemMap.get(syncId);
                if (relateItems != null) {
                    for (AttachMentItem attachMentItem : relateItems) {
                        if (TextUtils.isEmpty(attachMentItem.getSyncId())) {
                            delAttachmentIds.add(String.valueOf(attachMentItem.getId()));
                            delAttachmentList.add(attachMentItem);
                        }
                    }
                }
            }
            BubbleDB.clearAttachments(ATTACHMENT.inSql(ATTACHMENT.ID, delAttachmentIds));
            UIHandler.post(new Runnable() {
                @Override
                public void run() {
                    GlobalBubbleManager.getInstance().mergeFirstTimeMatchAttachmentData(matchAttachmentList, delAttachmentList);
                }
            });
        }
    }

    private static void restoreDeleteBubbleInternal(Bundle bundle, boolean isHandleShareData) {
        String tag = isHandleShareData ? "shareSyncRestore" : "syncRestore";
        //sync delete status, just remove it
        List<ContentValues> deleteList = bundle.getParcelableArrayList(BUNDLE_DELETE);
        dumpContentValuesStatus(tag + " delete bundle", deleteList);
        if (deleteList != null && deleteList.size() > 0) {
            List<String> idList = new ArrayList<String>();
            for (ContentValues cv : deleteList) {
                if (cv == null) {
                    continue;
                }
                String syncId = cv.getAsString(SyncDataConverter.BUBBLE_SYNC_ID);
                if (syncId != null) {
                    idList.add(syncId);
                }
            }
            dumpStringListStatus(tag + " delete", idList);
            if (idList.size() >= 0) {
                String where = BUBBLE.inSql(BUBBLE.SYNC_ID, idList);
                final List<BubbleItem> list = BubbleDB.listBubble(where);
                //remove from db.
                BubbleDB.deleteBubbleBy(where);
                //update ui
                if (list != null && list.size() > 0) {
                    List<String> createDates = new ArrayList<>();
                    for (BubbleItem delItem : list) {
                        if (delItem.getDueDate() > 0 && delItem.getCreateAt() > 0) {
                            createDates.add(String.valueOf(delItem.getCreateAt()));
                        }
                    }
                    if (!createDates.isEmpty()) {
                        try {
                            AlarmUtils.deleteAlarmFromCalendar(IdeaPillsApp.getInstance(), createDates);
                        } catch (Exception e) {
                            //ignore
                        }
                    }
                    UIHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            GlobalBubbleManager.getInstance().mergeRestoreRemovedData(list);
                        }
                    });
                }
            }
        }
    }

    private static void restoreReplaceBubbleInternal(Bundle bundle, long userId, final boolean isHandleShareData) {
        String tag = isHandleShareData ? "shareSyncRestore" : "syncRestore";
        final ArrayList<ContentValues> replaceList = bundle.getParcelableArrayList(BUNDLE_REPLACE);
        dumpContentValuesStatus(tag + " replace bundle", replaceList);
        final List<BubbleItem> mergeItems = new ArrayList<BubbleItem>();
        final List<Integer> delConflictAttachmentBubbleIds = new ArrayList<>();
        final List<String> delConflictAttachmentIds = new ArrayList<>();
        final List<BubbleItem> changedToShowVoiceBubbles = new ArrayList<>();
        final List<Integer> changeToRemoveBubbleIds = new ArrayList<>();
        if (replaceList != null && replaceList.size() > 0) {
            List<BubbleItem> syncedList = BubbleDB.listBubble(BUBBLE.WHERE_SYNC_ID_NOT_NULL);
            Map<String, BubbleItem> itemMap = new HashMap<String, BubbleItem>();
            if (syncedList != null && syncedList.size() > 0) {
                for (BubbleItem item : syncedList) {
                    String syncId = item.getSyncId();
                    if (syncId != null) {
                        itemMap.put(syncId, item);
                    }
                }
            }
            List<BubbleItem> addedList = new ArrayList<BubbleItem>();
            List<BubbleItem> changedList = new ArrayList<BubbleItem>();
            List<BubbleItem> changedRemindList = new ArrayList<BubbleItem>();

            Map<Integer, BubbleItem> addedConflictMap = new HashMap<>();
            List<String> addedConflictBubbleIds = new ArrayList<String>();
            List<BubbleItem> addedConflictBubbles = new ArrayList<BubbleItem>();
            List<BubbleItem> tempContainerForConflict = new ArrayList<BubbleItem>();

            Map<Integer, BubbleItem> changedToShowMap = new HashMap<>();
            List<BubbleItem> changedToShowBubbles = new ArrayList<BubbleItem>();
            List<String> changedToShowBubbleIds = new ArrayList<String>();

            long now = System.currentTimeMillis();
            for (ContentValues value : replaceList) {
                if (value == null) {
                    continue;
                }
                if (value.containsKey(SyncDataConverter.BUBBLE_DELETE_STATUS)) {
                    int deleteStatus = value.getAsInteger(SyncDataConverter.BUBBLE_DELETE_STATUS);
                    if (deleteStatus == SyncDataConverter.DELETE_STATUS_PERMANENT_DELETED) {
                        continue;
                    }
                }
                String syncId = value.getAsString(SyncDataConverter.BUBBLE_SYNC_ID);
                BubbleItem item = itemMap.get(syncId);
                long bubbleUserId = getBubbleUserId(value, userId, isHandleShareData);
                if (item == null) {
                    item = SyncDataConverter.newBubbleItem(value);
                    if (isHandleShareData && !item.isShareColor()) {
                        // share from others, but no local data, just ignore
                        continue;
                    }
                    if (bubbleUserId <= 0) {
                        continue;
                    }
                    if (isHandleShareData) {
                        if (item.getShareStatus() == GlobalBubble.SHARE_STATUS_ONE_TO_ONE) {
                            item.setUserId(bubbleUserId);
                            item.setShareFromOthers(true);
                            addedList.add(item);
                        }
                    } else {
                        int relateColor = SyncShareManager.INSTANCE.getChangeToNotShareFromOtherItemColor(item.getCreateAt());
                        if (relateColor != -1) {
                            item.setColor(relateColor);
                        }
                        item.setUserId(bubbleUserId);
                        item.setShareFromOthers(false);
                        addedList.add(item);
                    }

                } else {
                    if (item.needDele()) {
                        continue;
                    }
                    int changeFlag;
                    if (isHandleShareData || item.isShareColor()) {
                        changeFlag = SyncDataConverter.replaceBubbleItem(item, value, null);
                    } else {
                        tempContainerForConflict.clear();
                        changeFlag = SyncDataConverter.replaceBubbleItem(item, value, tempContainerForConflict);
                    }
                    if (changeFlag != SyncDataConverter.BUBBLE_ITEM_NONE_CHANGED) {
                        changedList.add(item);
                        if (isHandleShareData) {
                            if ((changeFlag & SyncDataConverter.BUBBLE_ITEM_CHANGED_SHARE_STATUS_TO_NO) ==
                                    SyncDataConverter.BUBBLE_ITEM_CHANGED_SHARE_STATUS_TO_NO) {
                                // share canceled, convert to local item
                                changeToRemoveBubbleIds.add(item.getId());
                            }
                        } else if ((changeFlag & SyncDataConverter.BUBBLE_ITEM_CHANGED_CONFLICT_TEXT) ==
                                SyncDataConverter.BUBBLE_ITEM_CHANGED_CONFLICT_TEXT && tempContainerForConflict.size() == 1) {
                            BubbleItem conflictItem = tempContainerForConflict.get(0);
                            conflictItem.setUserId(userId);
                            conflictItem.setTimeStampSilent(now);
                            conflictItem.setCreateAtSilent(now);
                            now += 100;
                            addedConflictMap.put(item.getId(), conflictItem);
                            addedConflictBubbles.add(conflictItem);
                            addedConflictBubbleIds.add(String.valueOf(item.getId()));
                        }

                        if ((changeFlag & SyncDataConverter.BUBBLE_ITEM_CHANGED_DUE_DATE) ==
                                SyncDataConverter.BUBBLE_ITEM_CHANGED_DUE_DATE) {
                            changedRemindList.add(item);
                        }
                        if ((changeFlag & SyncDataConverter.BUBBLE_ITEM_CHANGED_TO_SHOW) ==
                                SyncDataConverter.BUBBLE_ITEM_CHANGED_TO_SHOW) {
                            changedToShowBubbles.add(item);
                            changedToShowMap.put(item.getId(), item);
                            changedToShowBubbleIds.add(String.valueOf(item.getId()));
                            if (item.getDueDate() > 0) {
                                if (!changedRemindList.contains(item)) {
                                    changedRemindList.add(item);
                                }
                            }
                        } else if ((changeFlag & SyncDataConverter.BUBBLE_ITEM_CHANGED_TO_HIDE) ==
                                SyncDataConverter.BUBBLE_ITEM_CHANGED_TO_HIDE) {
                            if (!changedRemindList.contains(item)) {
                                changedRemindList.add(item);
                            }
                        }
                    }
//                    if (changeFlag == SyncDataConverter.BUBBLE_ITEM_CHANGED_TODO) {
//                        notifyContent(item.getText());
//                    }
                }
            }
            //merge restore data
            if (changeToRemoveBubbleIds.size() > 0) {
                List<String> clearBubbleIds = new ArrayList<>();
                for (Integer changedToRemoveBubbleId : changeToRemoveBubbleIds) {
                    clearBubbleIds.add(String.valueOf(changedToRemoveBubbleId));
                }
                BubbleDB.deleteBubbleAndAttachment(BUBBLE.inSql(BUBBLE.ID, clearBubbleIds),
                        ATTACHMENT.inSql(ATTACHMENT.BUBBLE_ID, clearBubbleIds));
            }

            if (addedConflictBubbles.size() > 0) {
                insertBubbles(addedConflictBubbles);
                final List<AttachMentItem> conflictAttachments = new ArrayList<>();
                conflictAttachments.addAll(BubbleDB.listAttachments(
                        ATTACHMENT.DOWNLOAD_STATUS + "=" + AttachMentItem.DOWNLOAD_STATUS_DOWNLOAD_SUCCESS
                                + " AND " + ATTACHMENT.inSql(ATTACHMENT.BUBBLE_ID, addedConflictBubbleIds)
                                + " AND " + ATTACHMENT.MARK_DELETE + "=0"));
                for (AttachMentItem attachMentItem : conflictAttachments) {
                    BubbleItem relateItem = addedConflictMap.get(attachMentItem.getBubbleId());
                    if (relateItem != null) {
                        if (TextUtils.isEmpty(attachMentItem.getSyncId())) {
                            delConflictAttachmentIds.add(String.valueOf(attachMentItem.getId()));
                            delConflictAttachmentBubbleIds.add(attachMentItem.getBubbleId());
                        }
                        attachMentItem.changeToConflict(relateItem.getId(), now);
                        if (!Utils.isFileUriExists(IdeaPillsApp.getInstance(), attachMentItem.getUri())) {
                            continue;
                        }
                        relateItem.addAttachment(attachMentItem);
                        now += 100;
                    }
                }
                if (!delConflictAttachmentIds.isEmpty()) {
                    BubbleDB.clearAttachments(ATTACHMENT.inSql(ATTACHMENT.ID, delConflictAttachmentIds));
                }
                insertAttachments(conflictAttachments, userId);
                mergeItems.addAll(addedConflictBubbles);
                if (!conflictAttachments.isEmpty()) {
                    for (AttachMentItem attachMentItem : conflictAttachments) {
                        attachMentItem.copyCauseConflict(IdeaPillsApp.getInstance());
                    }
                }
            }

            if (changedToShowBubbles.size() > 0) {
                final List<AttachMentItem> changedToShowAttachments = new ArrayList<>();
                changedToShowAttachments.addAll(BubbleDB.listAttachments(
                        ATTACHMENT.inSql(ATTACHMENT.BUBBLE_ID, changedToShowBubbleIds)
                                + " AND " + ATTACHMENT.MARK_DELETE + "=0"));
                for (AttachMentItem attachMentItem : changedToShowAttachments) {
                    BubbleItem relateItem = changedToShowMap.get(attachMentItem.getBubbleId());
                    if (relateItem != null) {
                        relateItem.addAttachment(attachMentItem);
                    }
                }
                for (BubbleItem bubbleItem : changedToShowBubbles) {
                    if (bubbleItem.isVoiceBubble()) {
                        changedToShowVoiceBubbles.add(bubbleItem);
                    }
                }
            }

            if (addedList.size() > 0) {
                insertBubbles(addedList);
                List<AttachMentItem> loseAttachments;
                if (isHandleShareData) {
                    loseAttachments = sTempLastTimeLoseShareAttachments;
                } else {
                    loseAttachments = sTempLastTimeLoseAttachments;
                }
                if (loseAttachments != null && !loseAttachments.isEmpty()) {
                    Map<String, BubbleItem> addBubbleItemMap = new HashMap<>();
                    for (BubbleItem addItem : addedList) {
                        addBubbleItemMap.put(addItem.getSyncId(), addItem);
                    }
                    List<AttachMentItem> relateAddLoseAttachments = new ArrayList<>();
                    for (AttachMentItem loseAttachment : loseAttachments) {
                        BubbleItem relateBubbleItem = addBubbleItemMap.get(loseAttachment.getBubbleSyncId());
                        if (relateBubbleItem != null) {
                            loseAttachment.setBubbleId(relateBubbleItem.getId());
                            relateAddLoseAttachments.add(loseAttachment);
                            relateBubbleItem.addAttachment(loseAttachment);
                        }
                    }
                    insertAttachments(relateAddLoseAttachments);
                }

                mergeItems.addAll(addedList);

                try {
                    List<BubbleItem> addAlarmItems = new ArrayList<BubbleItem>();
                    for (BubbleItem addedItem : addedList) {
                        if (addedItem.getDueDate() > 0) {
                            addAlarmItems.add(addedItem);
                        }
                    }
                    AlarmUtils.replaceAlarmToCalendar(IdeaPillsApp.getInstance(), addAlarmItems);
                } catch (Exception e) {
                    //ignore
                }
            }
            if (isHandleShareData) {
                sTempLastTimeLoseShareAttachments.clear();
            } else {
                sTempLastTimeLoseAttachments.clear();
            }

            if (changedList.size() > 0) {
                mergeItems.addAll(changedList);

                if (!changedRemindList.isEmpty()) {
                    try {
                        List<String> delAlarmCreateDates = new ArrayList<String>();
                        List<BubbleItem> changedAlarmItems = new ArrayList<BubbleItem>();
                        for (BubbleItem changedItem : changedRemindList) {
                            if (changedItem.getRemovedTime() == 0) {
                                if (changedItem.getDueDate() > 0) {
                                    changedAlarmItems.add(changedItem);
                                } else {
                                    delAlarmCreateDates.add(String.valueOf(changedItem.getCreateAt()));
                                }
                            } else {
                                delAlarmCreateDates.add(String.valueOf(changedItem.getCreateAt()));
                            }
                        }
                        AlarmUtils.replaceAlarmToCalendar(IdeaPillsApp.getInstance(), changedAlarmItems);
                        AlarmUtils.deleteAlarmFromCalendar(IdeaPillsApp.getInstance(), delAlarmCreateDates);
                    } catch (Exception e) {
                        //ignore
                    }
                }
            }
            dumpBubblesStatus(tag + " added", addedList);
            dumpBubblesStatus(tag + " changed", changedList);
        }

        final List<Long> sortList = new ArrayList<>();
        final long[] sortArray = bundle.getLongArray(BUNDLE_SORT);
        if (sortArray != null && sortArray.length > 0) {
            for (long sortItem : sortArray) {
                sortList.add(sortItem);
            }
        }

        UIHandler.post(new Runnable() {
            @Override
            public void run() {
                final List<BubbleItem> changedItems;
                if (mergeItems.size() > 0 || !sortList.isEmpty()) {
                    changedItems = GlobalBubbleManager.getInstance().mergeRestoreData(mergeItems,
                            delConflictAttachmentBubbleIds, delConflictAttachmentIds, changeToRemoveBubbleIds, sortList);
                } else {
                    changedItems = null;
                }

                if (!changedToShowVoiceBubbles.isEmpty()) {
                    DataHandler.handleTask(DataHandler.TASK_UPDATE_VOICE_WAVE_DATA, changedToShowVoiceBubbles);
                }
                if ((changedItems != null && !changedItems.isEmpty())) {
                    final List<ContentValues> list = new ArrayList<ContentValues>();
                    for (BubbleItem bubbleItem : changedItems) {
                        list.add(bubbleItem.toContentValues());
                    }
                    if (isHandleShareData) {
                        SyncManager.noticeShareRestoreFinish(new Runnable() {
                            @Override
                            public void run() {
                                if (!list.isEmpty()) {
                                    updateBubbles(list, false);
                                }
                            }
                        });
                    } else {
                        SyncManager.noticeRestoreFinish(new Runnable() {
                            @Override
                            public void run() {
                                if (!list.isEmpty()) {
                                    updateBubbles(list, false);
                                }
                            }
                        });
                    }
                } else {
                    if (isHandleShareData) {
                        SyncManager.noticeShareRestoreFinish(null);
                    } else {
                        SyncManager.noticeRestoreFinish(null);
                    }
                }
            }
        });
    }

    private static Bundle getPreparedDataInternal(final boolean isHandleShareData) {
        //collect data for cloud sync
        String tag = isHandleShareData ? "getSharePreparedData" : "getPreparedData";
        final List<Integer> syncItemIds = new ArrayList<Integer>();
        long userId = SyncManager.getCloudAccountUserId(IdeaPillsApp.getInstance());
        Bundle bundle = new Bundle();
        if (userId < 0) {
            return bundle;
        }
        boolean hasPreparedData = false;
        String addedWhere = BUBBLE.WHERE_SYNC_ID_IS_NULL + " AND " + BUBBLE.WHERE_CASE_NOT_DELETE
                + " AND " + BUBBLE.WHERE_NOT_LEGACY_USED;
        if (isHandleShareData) {
            addedWhere += (" AND " + BUBBLE.WHERE_SHARE_DATA);
        } else {
            addedWhere += (" AND " + BUBBLE.WHERE_SELF_DATA);
        }
        List<BubbleItem> list = BubbleDB.listBubble(addedWhere);
        if (list != null && list.size() > 0) {
            hasPreparedData = true;
            ArrayList<ContentValues> values = new ArrayList<ContentValues>();
            int maxWeight = GlobalBubbleManager.getInstance().getMaxWeight();
            for (BubbleItem item : list) {
                BubbleItem relateItemInMemory = GlobalBubbleManager.getInstance().getBubbleItemById(item.getId());
                if (relateItemInMemory != null) {
                    if (!relateItemInMemory.isNeedInput() && !relateItemInMemory.isEmptyBubble()
                            && !relateItemInMemory.needRemove()) {
                        values.add(SyncDataConverter.bubbleToContentValues(item, userId));
                        syncItemIds.add(item.getId());
                    }
                } else {
                    if (item.isEmptyBubble() && item.getWeight() >= maxWeight) {
                        // maybe a new bubble in interaction, sync next time
                        continue;
                    }
                    values.add(SyncDataConverter.bubbleToContentValues(item, userId));
                    syncItemIds.add(item.getId());
                }
            }
            dumpContentValuesStatus(tag + " added", values);
            bundle.putParcelableArrayList(BUNDLE_ADDED, values);
        }

        String replaceWhere = BUBBLE.WHERE_SYNC_ID_NOT_NULL + " AND " + BUBBLE.MODIFY_FLAG + ">0 AND " +
                BUBBLE.MODIFY_FLAG + "!=" + BubbleItem.MF_WEIGHT + " AND " + BUBBLE.WHERE_CASE_NOT_DELETE;
        if (isHandleShareData) {
            replaceWhere += (" AND " + BUBBLE.WHERE_SHARE_DATA);
        } else {
            replaceWhere += (" AND " + BUBBLE.WHERE_SELF_DATA);
        }
        list = BubbleDB.listBubble(replaceWhere);
        if (list != null && list.size() > 0) {
            hasPreparedData = true;
            ArrayList<ContentValues> values = new ArrayList<>();
            for (BubbleItem item : list) {
                if (item == null) {
                    continue;
                }
                ContentValues cv = SyncDataConverter.incrementBubbleToContentValues(item);
                values.add(cv);
                syncItemIds.add(item.getId());
            }
            dumpContentValuesStatus(tag + " replace", values);
            bundle.putParcelableArrayList(BUNDLE_REPLACE, values);
        }

        String deleteWhere = BUBBLE.WHERE_SYNC_ID_NOT_NULL + " AND ( " + BUBBLE.WHERE_CASE_DELETE + " )";
        if (isHandleShareData) {
            deleteWhere += (" AND " + BUBBLE.WHERE_SHARE_DATA);
        } else {
            deleteWhere += (" AND " + BUBBLE.WHERE_SELF_DATA);
        }
        list = BubbleDB.listBubble(deleteWhere);
        if (list != null && list.size() > 0) {
            hasPreparedData = true;
            ArrayList<ContentValues> values = new ArrayList<ContentValues>();
            for (int i = 0; i < list.size(); i++) {
                BubbleItem item = list.get(i);
                ContentValues cv = new ContentValues();
                cv.put(SyncDataConverter.BUBBLE_USER_ID, item.getUserId());
                cv.put(SyncDataConverter.BUBBLE_SYNC_ID, item.getSyncId());
                cv.put(SyncDataConverter.BUBBLE_DELETE_STATUS, SyncDataConverter.DELETE_STATUS_PERMANENT_DELETED);
                values.add(cv);
            }
            dumpContentValuesStatus(tag + " delete", values);
            bundle.putParcelableArrayList(BUNDLE_DELETE, values);
        }

        if (!isHandleShareData && !hasPreparedData) {
            // trigger handle sync result for order change
            boolean needTrigger = false;
            BubbleItem triggerItem = null;
            List<BubbleItem> items = GlobalBubbleManager.getInstance().getBubbles();
            for (BubbleItem item : items) {
                if (triggerItem == null && !item.isNeedInput() && !item.isEmptyBubble()
                        && !item.needRemove() && !item.isShareColor()
                        && !TextUtils.isEmpty(item.getSyncId()) && item.getUserId() > 0) {
                    triggerItem = item;
                }
                if (item.getModificationFlag() == BubbleItem.MF_WEIGHT) {
                    needTrigger = true;
                }
                if (needTrigger && triggerItem != null) {
                    break;
                }
            }
            if (needTrigger && triggerItem != null) {
                ArrayList<ContentValues> values = new ArrayList<>();
                ContentValues cv = SyncDataConverter.triggerWeightBubbleToContentValues(triggerItem);
                values.add(cv);
                bundle.putParcelableArrayList(BUNDLE_REPLACE, values);
            }
        }
        UIHandler.post(new Runnable() {
            @Override
            public void run() {
                long syncTime = System.currentTimeMillis();
                GlobalBubbleManager.getInstance().updateBubbleItemSyncTime(syncItemIds, syncTime);
            }
        });
        return bundle;
    }

    private static Bundle handleSyncResultInternal(Bundle bundle, final boolean isHandleShareData) {
        String tag = isHandleShareData ? "handleShareSyncResult" : "handleSyncResult";
        if (bundle == null) {
            log.error(tag + " return by bundle is null");
            return new Bundle();
        }
        //need check sync failed
        int resultCode = bundle.getInt("result");
        if (resultCode != 0) {
            log.error(tag + " return by resultCode is [" + resultCode + "]");
            return new Bundle();
        }
        boolean isChangedOrder = false;
        final ArrayList<ContentValues> addedList = bundle.getParcelableArrayList(BUNDLE_ADDED);
        dumpContentValuesStatus(tag + " added bundle", addedList);
        if (addedList != null && addedList.size() > 0) {
            List<ContentValues> loseItemList = new ArrayList<ContentValues>();
            Map<Integer, BubbleItem> itemMap = GlobalBubbleManager.getInstance().getBubbleMap();
            List<ContentValues> list = new ArrayList<ContentValues>();

            long userId = SyncManager.getCloudAccountUserId(IdeaPillsApp.getInstance());
            for (ContentValues syncIdValue : addedList) {
                if (syncIdValue == null) {
                    continue;
                }
                Integer id = syncIdValue.getAsInteger(SyncDataConverter.BUBBLE_ID);
                if (id == null) {
                    continue;
                }
                BubbleItem item = itemMap.get(id);
                long bubbleUserId = getBubbleUserId(syncIdValue, userId, isHandleShareData);
                if (item != null) {
                    String syncId = syncIdValue.getAsString(SyncDataConverter.BUBBLE_SYNC_ID);
                    int version = syncIdValue.getAsInteger(SyncDataConverter.BUBBLE_VERSION);
                    item.setSyncId(syncId);
                    item.setVoiceBubbleSyncId(syncId);
                    item.setVersion(version);
                    item.resetModifyFlag();
                    if (bubbleUserId > 0) {
                        item.setUserId(bubbleUserId);
                    }
                    item.setLastCloudText(item.getText());
                    if (item.getAttachments() != null) {
                        for (AttachMentItem attachMentItem : item.getAttachments()) {
                            attachMentItem.setBubbleSyncId(syncId);
                        }
                    }
                    list.add(item.toContentValues());
                    isChangedOrder = true;
                } else {
                    ContentValues loseCv = SyncDataConverter.convertToItemValues(syncIdValue);
                    if (bubbleUserId > 0) {
                        loseCv.put(BUBBLE.USER_ID, bubbleUserId);
                    }
                    loseCv.put(BUBBLE.MODIFY_FLAG, 0);
                    loseCv.put(BUBBLE.SECONDARY_MODIFY_FLAG, 0);
                    loseItemList.add(loseCv);
                }
            }
            if (loseItemList.size() > 0) {
                list.addAll(loseItemList);
            }
            updateBubbles(list, true);
        }

        final List<ContentValues> updateList = bundle.getParcelableArrayList(BUNDLE_REPLACE);
        dumpContentValuesStatus(tag + " replace bundle", updateList);
        Map<Integer, BubbleItem> itemMap = GlobalBubbleManager.getInstance().getBubbleMap();
        List<ContentValues> changedList = new ArrayList<ContentValues>();
        if (updateList != null && updateList.size() > 0) {
            List<ContentValues> loseItemList = new ArrayList<ContentValues>();
            for (ContentValues cv : updateList) {
                Integer id = cv.getAsInteger(SyncDataConverter.BUBBLE_ID);
                if (id == null) {
                    continue;
                }
                BubbleItem item = itemMap.get(id);
                if (item != null) {
                    int version = cv.getAsInteger(SyncDataConverter.BUBBLE_VERSION);
                    item.setVersion(version);
                    if (item.getModificationFlag(BubbleItem.MF_WEIGHT)) {
                        isChangedOrder = true;
                    }
                    item.resetModifyFlag();
                    if (BubbleItem.CONFLICT_HANDLED_TAG.equals(item.getConflictSyncId())) {
                        item.setConflictSyncIdSilent(null);
                    }
                    item.setLastCloudText(item.getText());
                    changedList.add(item.toContentValues());
                } else {
                    ContentValues loseCv = SyncDataConverter.convertToItemValues(cv);
                    loseCv.put(BUBBLE.MODIFY_FLAG, 0);
                    loseCv.put(BUBBLE.SECONDARY_MODIFY_FLAG, 0);
                    if (BubbleItem.CONFLICT_HANDLED_TAG.equals(loseCv.getAsString(BUBBLE.CONFLICT_SYNC_ID))) {
                        loseCv.put(BUBBLE.CONFLICT_SYNC_ID, "");
                    }
                    loseItemList.add(loseCv);
                }
            }
            if (loseItemList.size() > 0) {
                changedList.addAll(loseItemList);
            }
        }
        if (!isHandleShareData) {
            for (BubbleItem item : itemMap.values()) {
                if (item.getModificationFlag() == BubbleItem.MF_WEIGHT) {
                    isChangedOrder = true;
                    item.resetModifyFlag();
                    ContentValues cv = new ContentValues();
                    cv.put(BUBBLE.ID, item.getId());
                    cv.put(BUBBLE.MODIFY_FLAG, 0);
                    changedList.add(cv);
                }
            }
        } else {
            isChangedOrder = false;
        }

        if (!changedList.isEmpty()) {
            updateBubbles(changedList, false);
        }

        // clear db
        String clearDbSql = BUBBLE.WHERE_CLEAR_BUBBLE_TABLE;
        if (isHandleShareData) {
            clearDbSql += (" AND " + BUBBLE.WHERE_SHARE_DATA);
        } else {
            clearDbSql += (" AND " + BUBBLE.WHERE_SELF_DATA);
        }
        BubbleDB.deleteBubbleBy(clearDbSql);
        List<String> deleteBubbleIds = BubbleDB.listBubbleIds(BUBBLE.MARK_DELETE + "=1");
        if (!deleteBubbleIds.isEmpty()) {
            ContentValues markAttachmentDelete = new ContentValues();
            markAttachmentDelete.put(ATTACHMENT.MARK_DELETE, 1);
            BubbleDB.updateAttachement(markAttachmentDelete, ATTACHMENT.inSql(ATTACHMENT.BUBBLE_ID, deleteBubbleIds));
        }

        //sync delete status, just remove it
        List<ContentValues> deleteList = bundle.getParcelableArrayList(BUNDLE_DELETE);
        dumpContentValuesStatus(tag + " delete bundle", deleteList);
        if (deleteList != null && deleteList.size() > 0) {
            List<String> idList = new ArrayList<String>();
            for (ContentValues cv : deleteList) {
                if (cv == null) {
                    continue;
                }
                String syncId = cv.getAsString(SyncDataConverter.BUBBLE_SYNC_ID);
                if (syncId != null) {
                    idList.add(syncId);
                }
            }
            if (idList.size() > 0) {
                String where = BUBBLE.inSql(BUBBLE.SYNC_ID, idList);
                BubbleDB.deleteBubbleBy(where + " AND " + BUBBLE.MARK_DELETE + "=1 AND " + BUBBLE.VOICE_SYNC_ID + " IS NULL ");

                // just set sync id to null here, delete when attachment sync finish
                BubbleDB.updateBubbleColumnToNull(BUBBLE.SYNC_ID, where + " AND (" + BUBBLE.WHERE_CASE_DELETE + ")");
            }
        }

        // pending share items
        if (!isHandleShareData) {
            handlePendingShareItems();
        }

        Bundle sorRetBundle = new Bundle();
        if (isChangedOrder) {
            sorRetBundle.putLongArray(BUNDLE_SORT, BubbleDB.listBubbleSyncIdsByWeight());
        }
        return sorRetBundle;
    }

    private static void handlePendingShareItems() {
        final List<BubbleItem> pendingShareItems = BubbleDB.listBubble(BUBBLE.WHERE_PENDING_SHARE + " AND "
                + BUBBLE.WHERE_SYNC_ID_NOT_NULL + " AND " + BUBBLE.WHERE_SELF_DATA + " AND " + BUBBLE.WHERE_CASE_NOT_DELETE);
        if (pendingShareItems != null && !pendingShareItems.isEmpty()) {
            long userId = SyncManager.getCloudAccountUserId(IdeaPillsApp.getInstance());
            if (userId >= 0 && SyncShareManager.INSTANCE.hasInvitationListCached()) {
                List<SyncShareInvitation> invitationList = SyncShareManager.INSTANCE.getCachedInvitationList();
                List<Long> allParticipantIds = SyncShareUtils.findAllParticipantIdsCanAdd(userId, invitationList);

                final HashSet<Long> failInviteeIds = new HashSet<>();
                if (invitationList != null) {
                    for (SyncShareInvitation invitation : invitationList) {
                        if (invitation.inviteStatus == SyncShareInvitation.INVITE_CANCEL
                                || invitation.inviteStatus == SyncShareInvitation.INVITE_DECLINE) {
                            failInviteeIds.add(invitation.invitee.id);
                        } else if (invitation.inviteStatus == SyncShareInvitation.INVITE_ACCEPT) {
                            failInviteeIds.remove(invitation.invitee.id);
                        }
                    }
                }
                final Map<String, BubbleItem> pendingAddShareItems = new HashMap<>();
                final Map<String, BubbleItem> pendingRemoveShareItems = new HashMap<>();
                final List<BubbleItem> pendingShareInvitationItems = new ArrayList<>();
                for (BubbleItem item : pendingShareItems) {
                    if (item.getSharePendingStatus() == BubbleItem.SHARE_PENDING_INVITATION) {
                        boolean isItemInFailInvitation;
                        List<Long> toAddParticipants = new ArrayList<>();
                        if (!item.getSharePendingParticipants().isEmpty()) {
                            long participantId = item.getSharePendingParticipants().get(0);
                            if (allParticipantIds.contains(participantId)) {
                                toAddParticipants.add(participantId);
                                isItemInFailInvitation = false;
                            } else {
                                isItemInFailInvitation = failInviteeIds.contains(participantId);
                            }
                        } else {
                            if (!allParticipantIds.isEmpty()) {
                                toAddParticipants.add(allParticipantIds.get(0));
                            }
                            isItemInFailInvitation = false;
                        }
                        if (!allParticipantIds.isEmpty()) {
                            if (!toAddParticipants.isEmpty()) {
                                item.changeShareStatusToAdd(toAddParticipants);
                                pendingShareInvitationItems.add(item);
                                pendingAddShareItems.put(item.getSyncId(), item);
                            } else {
                                item.setShareStatus(GlobalBubble.SHARE_STATUS_NOT_SHARED);
                                item.clearPendingShareStatus();
                                pendingShareInvitationItems.add(item);
                            }
                        } else if (isItemInFailInvitation || invitationList == null || invitationList.isEmpty()) {
                            item.setShareStatus(GlobalBubble.SHARE_STATUS_NOT_SHARED);
                            item.clearPendingShareStatus();
                            pendingShareInvitationItems.add(item);
                        }
                    } else if (item.getSharePendingStatus() == BubbleItem.SHARE_PENDING_ADD_PARTICIPANTS) {
                        boolean isNeedAdd = false;
                        if (!allParticipantIds.isEmpty()) {
                            List<Long> itemParticipantIds = item.getSharePendingParticipants();
                            if (itemParticipantIds != null) {
                                List<Long> newItemParticipantIds = new ArrayList<>();
                                for (Long itemParticipantId : itemParticipantIds) {
                                    if (allParticipantIds.contains(itemParticipantId)) {
                                        newItemParticipantIds.add(itemParticipantId);
                                    }
                                }
                                if (!newItemParticipantIds.isEmpty()) {
                                    isNeedAdd = true;
                                    item.modifyPendingParticipants(newItemParticipantIds);
                                    pendingAddShareItems.put(item.getSyncId(), item);
                                }
                            }
                        }
                        if (!isNeedAdd) {
                            item.setShareStatus(GlobalBubble.SHARE_STATUS_NOT_SHARED);
                            item.clearPendingShareStatus();
                            pendingShareInvitationItems.add(item);
                        }
                    } else if (item.getSharePendingStatus() == BubbleItem.SHARE_PENDING_REMOVE_PARTICIPANTS) {
                        boolean isNeedRemove = false;
                        if (!allParticipantIds.isEmpty()) {
                            List<Long> itemParticipantIds = item.getSharePendingParticipants();
                            if (itemParticipantIds != null) {
                                List<Long> newItemParticipantIds = new ArrayList<>();
                                for (Long itemParticipantId : itemParticipantIds) {
                                    if (allParticipantIds.contains(itemParticipantId)) {
                                        newItemParticipantIds.add(itemParticipantId);
                                    }
                                }
                                if (!newItemParticipantIds.isEmpty()) {
                                    isNeedRemove = true;
                                    item.modifyPendingParticipants(newItemParticipantIds);
                                    pendingRemoveShareItems.put(item.getSyncId(), item);
                                }
                            }
                        }
                        if (!isNeedRemove) {
                            item.setShareStatus(GlobalBubble.SHARE_STATUS_NOT_SHARED);
                            item.clearPendingShareStatus();
                            pendingShareInvitationItems.add(item);
                        }
                    }
                }
                if (!pendingShareInvitationItems.isEmpty()) {
                    List<ContentValues> updateCvs = new ArrayList<>();
                    for (BubbleItem pendingShareInvitationItem : pendingShareInvitationItems) {
                        if (pendingShareInvitationItem != null && pendingShareInvitationItem.getId() > 0) {
                            ContentValues cv = new ContentValues();
                            cv.put(BUBBLE.ID, pendingShareInvitationItem.getId());
                            cv.put(BUBBLE.SHARE_STATUS, pendingShareInvitationItem.getShareStatus());
                            cv.put(BUBBLE.SHARE_PENDING_STATUS, pendingShareInvitationItem.getSharePendingStatus());
                            cv.put(BUBBLE.SHARE_PENDING_PARTICIPANTS, pendingShareInvitationItem.getSharePendingParticipantsString());
                            cv.put(BUBBLE.MODIFY_FLAG, pendingShareInvitationItem.getModificationFlag());
                            cv.put(BUBBLE.SECONDARY_MODIFY_FLAG, pendingShareInvitationItem.getSecondaryModifyFlag());
                            updateCvs.add(cv);
                        }
                    }
                    updateBubbles(updateCvs, false);
                    UIHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            GlobalBubbleManager.getInstance().mergePendingShareInvitationData(pendingShareInvitationItems);
                        }
                    });
                }
                if (!pendingAddShareItems.isEmpty()) {
                    SyncShareRepository.addParticipants(userId, pendingAddShareItems.values(), new SyncBundleRepository.RequestListener<List<Long>>() {
                        @Override
                        public void onRequestStart() {

                        }

                        @Override
                        public void onResponse(List<Long> response) {
                            List<ContentValues> updateCvs = new ArrayList<>();
                            List<Integer> bubbleIds = new ArrayList<>();
                            for (Long successSyncId : response) {
                                BubbleItem relateItem = pendingAddShareItems.get(String.valueOf(successSyncId));
                                if (relateItem != null && relateItem.getId() > 0) {
                                    bubbleIds.add(relateItem.getId());
                                    ContentValues cv = new ContentValues();
                                    cv.put(BUBBLE.ID, relateItem.getId());
                                    log.info(relateItem.getSyncId() + " addParticipants:" + relateItem.getSharePendingParticipantsString());
                                    cv.put(BUBBLE.SHARE_PENDING_STATUS, BubbleItem.SHARE_PENDING_NONE);
                                    cv.put(BUBBLE.SHARE_PENDING_PARTICIPANTS, "");
                                    updateCvs.add(cv);
                                }
                            }
                            updateBubbles(updateCvs, false);
                            GlobalBubbleManager.getInstance().mergePendingShareHandleData(bubbleIds);
                        }

                        @Override
                        public void onError(SyncBundleRepository.DataException e) {
                            if (e.status == SyncShareRepository.ERROR_SHARE_PARTICIPANT_NOT_ALLOWED
                                    || e.status == SyncShareRepository.ERROR_SHARE_NOT_EXISTS) {
                                SyncShareManager.INSTANCE.refreshInvitationList();
                            }
                        }
                    });
                }
                if (!pendingRemoveShareItems.isEmpty()) {
                    SyncShareRepository.removeParticipants(userId, pendingRemoveShareItems.values(), new SyncBundleRepository.RequestListener<List<Long>>() {
                        @Override
                        public void onRequestStart() {

                        }

                        @Override
                        public void onResponse(List<Long> response) {
                            List<ContentValues> updateCvs = new ArrayList<>();
                            List<Integer> bubbleIds = new ArrayList<>();
                            for (Long successSyncId : response) {
                                BubbleItem relateItem = pendingRemoveShareItems.get(String.valueOf(successSyncId));
                                if (relateItem != null && relateItem.getId() > 0) {
                                    bubbleIds.add(relateItem.getId());
                                    ContentValues cv = new ContentValues();
                                    cv.put(BUBBLE.ID, relateItem.getId());
                                    log.info(relateItem.getSyncId() + " removeParticipants:" + relateItem.getSharePendingParticipantsString());
                                    cv.put(BUBBLE.SHARE_PENDING_STATUS, BubbleItem.SHARE_PENDING_NONE);
                                    cv.put(BUBBLE.SHARE_PENDING_PARTICIPANTS, "");
                                    updateCvs.add(cv);
                                }
                            }
                            updateBubbles(updateCvs, false);
                            GlobalBubbleManager.getInstance().mergePendingShareHandleData(bubbleIds);
                        }

                        @Override
                        public void onError(SyncBundleRepository.DataException e) {
                            if (pendingAddShareItems.isEmpty()) {
                                // refresh only no has add share items
                                if (e.status == SyncShareRepository.ERROR_SHARE_PARTICIPANT_NOT_ALLOWED
                                        || e.status == SyncShareRepository.ERROR_SHARE_NOT_EXISTS) {
                                    SyncShareManager.INSTANCE.refreshInvitationList();
                                }
                            }
                        }
                    });
                }
            }
        }
    }

    private static Bundle syncRestoreAttachmentInternal(Bundle bundle, long userId, final boolean isHandleShareData) {
        String tag = isHandleShareData ? "shareSyncRestoreAttachment" : "syncRestoreAttachment";

        // clear temp save lose attachments
        if (isHandleShareData) {
            sTempLastTimeLoseShareAttachments.clear();
        } else {
            sTempLastTimeLoseAttachments.clear();
        }

        //sync delete status, just remove it
        List<ContentValues> deleteList = bundle.getParcelableArrayList(BUNDLE_DELETE);
        dumpContentValuesStatus(tag + " delete bundle", deleteList);
        if (deleteList != null && deleteList.size() > 0) {
            List<String> idList = new ArrayList<String>();
            for (ContentValues cv : deleteList) {
                if (cv == null) {
                    continue;
                }
                String syncId = cv.getAsString(SyncDataConverter.ATTACHMENT_SYNC_ID);
                if (syncId != null) {
                    idList.add(syncId);
                }
            }
            dumpStringListStatus(tag + " delete", idList);
            if (idList.size() >= 0) {
                String where = ATTACHMENT.inSql(ATTACHMENT.SYNC_ID, idList);
                final List<AttachMentItem> list = BubbleDB.listAttachments(where);
                //remove from db.
                BubbleDB.clearAttachments(where);
                //update ui
                if (list != null && list.size() > 0) {
                    UIHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            GlobalBubbleManager.getInstance().mergeRestoreRemoveAttachments(list);
                        }
                    });
                }
            }
        }

        final ArrayList<ContentValues> replaceList = bundle.getParcelableArrayList(BUNDLE_REPLACE);
        dumpContentValuesStatus(tag + "  replace bundle", replaceList);
        final List<AttachMentItem> addedList = new ArrayList<AttachMentItem>();
        final List<BubbleItem> bubbleChangedList = new ArrayList<BubbleItem>();
        if (replaceList != null && replaceList.size() > 0) {
            List<AttachMentItem> syncedList = BubbleDB.listAttachments(ATTACHMENT.WHERE_SYNC_ID_NOT_NULL);
            Map<String, AttachMentItem> itemMap = new HashMap<String, AttachMentItem>();
            if (syncedList != null && syncedList.size() > 0) {
                for (AttachMentItem item : syncedList) {
                    String syncId = item.getSyncId();
                    if (syncId != null) {
                        itemMap.put(syncId, item);
                    }
                }
            }
            List<BubbleItem> syncedVoiceList = BubbleDB.listBubble(BUBBLE.WHERE_VOICE_SYNC_ID_NOT_NULL);
            Map<String, BubbleItem> voiceItemMap = new HashMap<String, BubbleItem>();
            if (syncedVoiceList != null && syncedVoiceList.size() > 0) {
                for (BubbleItem item : syncedVoiceList) {
                    String voiceSyncId = item.getVoiceSyncId();
                    if (voiceSyncId != null) {
                        voiceItemMap.put(voiceSyncId, item);
                    }
                }
            }

            List<AttachMentItem> loseAddedAttachmentItems = new ArrayList<>();
            List<String> loseAddedAttachmentBubbleSyncIds = new ArrayList<>();

            List<ContentValues> loseAddedVoiceBubbleCvs = new ArrayList<>();
            List<String> loseAddedVoiceBubbleSyncIds = new ArrayList<>();
            for (ContentValues value : replaceList) {
                if (value == null) {
                    continue;
                }
                if (value.containsKey(SyncDataConverter.ATTACHMENT_DELETE_STATUS)) {
                    int deleteStatus = value.getAsInteger(SyncDataConverter.ATTACHMENT_DELETE_STATUS);
                    if (deleteStatus == SyncDataConverter.DELETE_STATUS_PERMANENT_DELETED) {
                        continue;
                    }
                }
                String syncId = value.getAsString(SyncDataConverter.ATTACHMENT_SYNC_ID);
                int type = value.getAsInteger(SyncDataConverter.ATTACHMENT_TYPE);
                if (type == SyncMediaTypeHelper.ATTACHMENT_TYPE_VOICE_RECORD) {
                    BubbleItem relateVoiceBubbleItem = voiceItemMap.get(syncId);
                    if (relateVoiceBubbleItem == null) {
                        String bubbleSyncId = value.getAsString(SyncDataConverter.ATTACHMENT_PILL_ID);
                        if (TextUtils.isEmpty(bubbleSyncId)) {
                            continue;
                        }
                        relateVoiceBubbleItem = GlobalBubbleManager.getInstance().getBubbleBySyncId(bubbleSyncId);
                        if (relateVoiceBubbleItem == null) {
                            loseAddedVoiceBubbleSyncIds.add(bubbleSyncId);
                            loseAddedVoiceBubbleCvs.add(value);
                            continue;
                        }
                    }
                    SyncDataConverter.replaceAttachmentInBubble(relateVoiceBubbleItem, value);
                    if (!bubbleChangedList.contains(relateVoiceBubbleItem)) {
                        bubbleChangedList.add(relateVoiceBubbleItem);
                    }
                } else {
                    AttachMentItem item = itemMap.get(syncId);
                    String bubbleSyncId = value.getAsString(SyncDataConverter.ATTACHMENT_PILL_ID);
                    long attachmentUserId = getAttachmentUserId(value, userId, isHandleShareData);
                    if (item == null) {
                        if (!TextUtils.isEmpty(bubbleSyncId)) {
                            BubbleItem relateBubbleItem = GlobalBubbleManager.getInstance().getBubbleBySyncId(
                                    bubbleSyncId);
                            if (relateBubbleItem != null) {
                                int bubbleLocalId = relateBubbleItem.getId();
                                if (bubbleLocalId > 0) {
                                    item = SyncDataConverter.newAttachmentItem(value, bubbleLocalId);
                                    item.setBubbleSyncId(bubbleSyncId);
                                    if (attachmentUserId > 0) {
                                        item.setUserId(attachmentUserId);
                                        addedList.add(item);
                                    } else if (relateBubbleItem.getUserId() > 0) {
                                        item.setUserId(relateBubbleItem.getUserId());
                                        addedList.add(item);
                                    }
                                }
                            } else {
                                loseAddedAttachmentBubbleSyncIds.add(bubbleSyncId);
                                item = SyncDataConverter.newAttachmentItem(value, -1);
                                item.setBubbleSyncId(bubbleSyncId);
                                if (attachmentUserId > 0) {
                                    item.setUserId(attachmentUserId);
                                    loseAddedAttachmentItems.add(item);
                                }
                            }
                        }
                    }
                }
            }

            // merge restore data
            if (!loseAddedAttachmentItems.isEmpty()) {
                Map<String, Integer> idMap = BubbleDB.listBubbleSyncIdsAndIds(
                        BUBBLE.inSql(BUBBLE.SYNC_ID, loseAddedAttachmentBubbleSyncIds));
                for (AttachMentItem loseItem : loseAddedAttachmentItems) {
                    Integer relateBubbleId = idMap.get(loseItem.getBubbleSyncId());
                    if (relateBubbleId != null && relateBubbleId > 0) {
                        loseItem.setBubbleId(relateBubbleId);
                        addedList.add(loseItem);
                    } else {
                        if (isHandleShareData) {
                            sTempLastTimeLoseShareAttachments.add(loseItem);
                        } else {
                            sTempLastTimeLoseAttachments.add(loseItem);
                        }
                    }
                }
            }
            if (addedList.size() > 0) {
                insertAttachments(addedList);
            }

            final List<ContentValues> updateBubbleList = new ArrayList<ContentValues>();
            if (!loseAddedVoiceBubbleSyncIds.isEmpty()) {
                List<BubbleItem> loseBubbles = BubbleDB.listBubble(
                        BUBBLE.inSql(BUBBLE.SYNC_ID, loseAddedVoiceBubbleSyncIds));
                Map<String, BubbleItem> loseBubbleMap = new HashMap<String, BubbleItem>();
                if (loseBubbles != null) {
                    for (BubbleItem loseBubble: loseBubbles) {
                        loseBubbleMap.put(loseBubble.getSyncId(), loseBubble);
                    }
                }
                for (ContentValues value : loseAddedVoiceBubbleCvs) {
                    String bubbleSyncId = value.getAsString(SyncDataConverter.ATTACHMENT_PILL_ID);
                    if (!TextUtils.isEmpty(bubbleSyncId)) {
                        BubbleItem bubbleItem = loseBubbleMap.get(bubbleSyncId);
                        if (bubbleItem != null) {
                            SyncDataConverter.replaceAttachmentInBubble(bubbleItem, value);
                            bubbleChangedList.add(bubbleItem);
                        }
                    }
                }
            }
            if (bubbleChangedList.size() > 0) {
                for (final BubbleItem bubbleItem : bubbleChangedList) {
                    if (bubbleItem.getId() > 0) {
                        ContentValues cv = new ContentValues();
                        cv.put(BUBBLE.ID, bubbleItem.getId());
                        cv.put(BUBBLE.URI, bubbleItem.getUri() == null ? "" : bubbleItem.getUri().toString());
                        cv.put(BUBBLE.TYPE, bubbleItem.getType());
                        cv.put(BUBBLE.VOICE_SYNC_ID, bubbleItem.getVoiceSyncId());
                        cv.put(BUBBLE.VOICE_VERSION, bubbleItem.getVoiceVersion());
                        cv.put(BUBBLE.VOICE_SYNC_ENCRYPT_KEY, bubbleItem.getVoiceEncryptKey());
                        updateBubbleList.add(cv);
                    }
                }
            }
            if (!updateBubbleList.isEmpty()) {
                updateBubbles(updateBubbleList, false);
            }
            dumpAttachmentsStatus(tag + " added", addedList);
            dumpBubblesStatus(tag + " bubbleChanged", bubbleChangedList);
        }
        // call ui change
        UIHandler.post(new Runnable() {
            @Override
            public void run() {
                GlobalBubbleManager.getInstance().mergeRestoreAttachmentData(addedList);
                GlobalBubbleManager.getInstance().mergeVoiceRestoreData(bubbleChangedList);
                if (!bubbleChangedList.isEmpty()) {
                    DataHandler.handleTask(DataHandler.TASK_UPDATE_VOICE_WAVE_DATA, bubbleChangedList);
                }
                GlobalBubbleManager.getInstance().syncBubbleLockFinish();
                if (isHandleShareData) {
                    SyncManager.noticeShareRestoreAttachmentFinish(null);
                } else {
                    SyncManager.noticeRestoreAttachmentFinish(null);
                }
            }
        });
        return new Bundle();
    }

    private static Bundle getPreparedAttachmentDataInternal(final boolean isHandleShareData) {
        String tag = isHandleShareData ? "getSharePreparedAttachmentData" : "getPreparedAttachmentData";
        //collect data for cloud sync
        long userId = SyncManager.getCloudAccountUserId(IdeaPillsApp.getInstance());
        Bundle bundle = new Bundle();
        if (userId < 0) {
            return bundle;
        }
        String addedWhere = ATTACHMENT.WHERE_SYNC_ID_IS_NULL + " AND " + ATTACHMENT.MARK_DELETE + "=0";
        if (isHandleShareData) {
            List<String> shareBubbleIds = BubbleDB.listBubbleIds( " ("
                    + BUBBLE.WHERE_CASE_NOT_DELETE + ") " + " AND " + BUBBLE.WHERE_SHARE_DATA);
            if (!shareBubbleIds.isEmpty()) {
                addedWhere += " AND " + ATTACHMENT.inSql(ATTACHMENT.BUBBLE_ID, shareBubbleIds);
            } else {
                addedWhere += " AND 0=1";
            }
        } else {
            String notInBubblesWhere = " (" + BUBBLE.WHERE_CASE_DELETE + ") " + " OR " + BUBBLE.WHERE_LEGACY_USED
                    + " OR " + BUBBLE.WHERE_SHARE_DATA;
            List<String> notInBubbleIds = BubbleDB.listBubbleIds(notInBubblesWhere);
            if (!notInBubbleIds.isEmpty()) {
                addedWhere += " AND " + ATTACHMENT.notinSql(ATTACHMENT.BUBBLE_ID, notInBubbleIds);
            }
        }
        List<AttachMentItem> list = BubbleDB.listAttachments(addedWhere);
        ArrayList<ContentValues> values = new ArrayList<ContentValues>();
        if (list != null && list.size() > 0) {
            List<AttachMentItem> loseAttachments = new ArrayList<>();
            List<String> loseAttachmentBubbleIds = new ArrayList<>();
            Map<Integer, String> updateAttachmentIdMap = new HashMap<Integer, String>();
            for (AttachMentItem item : list) {
                if (!Utils.isFileUriVaild(IdeaPillsApp.getInstance(), item.getUri())) {
                    continue;
                }
                BubbleItem relateBubbleItem = GlobalBubbleManager.getInstance().getBubbleItemById(item.getBubbleId());
                if (relateBubbleItem != null) {
                    String bubbleSyncId = TextUtils.isEmpty(relateBubbleItem.getSyncId()) ?
                            relateBubbleItem.getVoiceBubbleSyncId() : relateBubbleItem.getSyncId();
                    if (!TextUtils.isEmpty(bubbleSyncId) && relateBubbleItem.getUserId() > 0) {
                        values.add(SyncDataConverter.attachmentToContentValues(item, bubbleSyncId,
                                relateBubbleItem.getId(), relateBubbleItem.getUserId()));
                        if (TextUtils.isEmpty(item.getBubbleSyncId())) {
                            updateAttachmentIdMap.put(relateBubbleItem.getId(), bubbleSyncId);
                        }
                    }
                } else if (!TextUtils.isEmpty(item.getBubbleSyncId()) && item.getUserId() > 0) {
                    values.add(SyncDataConverter.attachmentToContentValues(item,
                            item.getBubbleSyncId(), item.getBubbleId(), item.getUserId()));
                } else {
                    loseAttachments.add(item);
                    loseAttachmentBubbleIds.add(String.valueOf(item.getBubbleId()));
                }
            }
            if (!loseAttachments.isEmpty()) {
                Map<Integer, Pair<String, Long>> idMap = BubbleDB.listBubbleIdsAndSyncIdUserIds(
                        BUBBLE.inSql(BUBBLE.ID, loseAttachmentBubbleIds));
                for (AttachMentItem loseItem : loseAttachments) {
                    Pair<String, Long> relateBubbleSyncId = idMap.get(loseItem.getBubbleId());
                    if (relateBubbleSyncId != null && !TextUtils.isEmpty(relateBubbleSyncId.first)) {
                        if (isHandleShareData) {
                            if (relateBubbleSyncId.second > 0) {
                                values.add(SyncDataConverter.attachmentToContentValues(loseItem,
                                        relateBubbleSyncId.first, loseItem.getBubbleId(), relateBubbleSyncId.second));
                                updateAttachmentIdMap.put(loseItem.getBubbleId(), relateBubbleSyncId.first);
                            }
                        } else {
                            values.add(SyncDataConverter.attachmentToContentValues(loseItem,
                                    relateBubbleSyncId.first, loseItem.getBubbleId(), userId));
                            updateAttachmentIdMap.put(loseItem.getBubbleId(), relateBubbleSyncId.first);
                        }
                    }
                }
            }
            if (!updateAttachmentIdMap.isEmpty()) {
                updateAttachmentsSyncIds(updateAttachmentIdMap);
            }
        }
        String addedAttachmentVoiceWhere = BUBBLE.WHERE_VOICE_SYNC_ID_IS_NULL + " AND " + BUBBLE.WHERE_CASE_NOT_DELETE
                + " AND ( " + BUBBLE.TYPE + " = " + BubbleItem.TYPE_VOICE
                + " OR " + BUBBLE.TYPE + " = " + BubbleItem.TYPE_VOICE_OFFLINE + " )"
                + " AND " + BUBBLE.WHERE_NOT_LEGACY_USED;
        if (isHandleShareData) {
            addedAttachmentVoiceWhere += (" AND " + BUBBLE.WHERE_SHARE_DATA);
        } else {
            addedAttachmentVoiceWhere += (" AND " + BUBBLE.WHERE_SELF_DATA);
        }
        List<BubbleItem> voiceBubbleList = BubbleDB.listBubble(addedAttachmentVoiceWhere);
        if (voiceBubbleList != null && voiceBubbleList.size() > 0) {
            for (BubbleItem bubbleItem : voiceBubbleList) {
                if (Utils.isFileUriExists(IdeaPillsApp.getInstance(), bubbleItem.getUri())) {
                    ContentValues voiceCv = SyncDataConverter.voiceAttachmentToContentValues(bubbleItem);
                    if (voiceCv.containsKey(SyncDataConverter.ATTACHMENT_PILL_ID)) {
                        if (isHandleShareData) {
                            if (bubbleItem.getUserId() > 0) {
                                voiceCv.put(ATTACHMENT.USER_ID, bubbleItem.getUserId());
                                values.add(voiceCv);
                            }
                        } else {
                            voiceCv.put(ATTACHMENT.USER_ID, bubbleItem.getUserId() <= 0 ? userId : bubbleItem.getUserId());
                            values.add(voiceCv);
                        }
                    }
                }
            }
        }
        dumpContentValuesStatus(tag + " attachment added", values);
        bundle.putParcelableArrayList(BUNDLE_ADDED, values);

        String deleteWhere = ATTACHMENT.MARK_DELETE + "=1 AND " + ATTACHMENT.WHERE_SYNC_ID_NOT_NULL;
        List<String> shareBubbleIds = BubbleDB.listBubbleIds(BUBBLE.WHERE_SHARE_DATA);
        if (isHandleShareData) {
            if (!shareBubbleIds.isEmpty()) {
                deleteWhere += " AND " + ATTACHMENT.inSql(ATTACHMENT.BUBBLE_ID, shareBubbleIds);
                list = BubbleDB.listAttachments(deleteWhere);
            } else {
                list = new ArrayList<>();
            }
        } else {
            if (!shareBubbleIds.isEmpty()) {
                deleteWhere += " AND " + ATTACHMENT.notinSql(ATTACHMENT.BUBBLE_ID, shareBubbleIds);
            }
            list = BubbleDB.listAttachments(deleteWhere);
        }

        String deleteAttachmentVoiceWhere = BUBBLE.WHERE_VOICE_SYNC_ID_NOT_NULL + " AND ( " + BUBBLE.WHERE_CASE_DELETE + " )";
        if (isHandleShareData) {
            deleteAttachmentVoiceWhere += (" AND " + BUBBLE.WHERE_SHARE_DATA);
        } else {
            deleteAttachmentVoiceWhere += (" AND " + BUBBLE.WHERE_SELF_DATA);
        }
        voiceBubbleList = BubbleDB.listBubble(deleteAttachmentVoiceWhere);
        ArrayList<ContentValues> delValues = new ArrayList<ContentValues>();
        if (list != null) {
            for (int i = 0; i < list.size(); i++) {
                AttachMentItem item = list.get(i);
                if (!TextUtils.isEmpty(item.getBubbleSyncId()))  {
                    if (isHandleShareData && item.getUserId() <= 0) {
                        continue;
                    }
                    ContentValues cv = SyncDataConverter.attachmentToContentValues(item,
                            item.getBubbleSyncId(), item.getBubbleId(), item.getUserId()<= 0 ? userId : item.getUserId());
                    cv.put(SyncDataConverter.ATTACHMENT_DELETE_STATUS, SyncDataConverter.DELETE_STATUS_PERMANENT_DELETED);
                    if (cv.containsKey(SyncDataConverter.ATTACHMENT_PILL_ID)
                            && cv.containsKey(SyncDataConverter.ATTACHMENT_ENCRYPT_KEY)) {
                        delValues.add(cv);
                    }
                }
            }
        }
        if (voiceBubbleList != null) {
            for (int i = 0; i < voiceBubbleList.size(); i++) {
                BubbleItem item = voiceBubbleList.get(i);
                if (isHandleShareData && item.getUserId() <= 0) {
                    continue;
                }
                ContentValues cv = SyncDataConverter.voiceAttachmentToContentValues(item);
                cv.put(SyncDataConverter.ATTACHMENT_USER_ID, item.getUserId() <= 0 ? userId : item.getUserId());
                cv.put(SyncDataConverter.ATTACHMENT_DELETE_STATUS, SyncDataConverter.DELETE_STATUS_PERMANENT_DELETED);
                if (cv.containsKey(SyncDataConverter.ATTACHMENT_PILL_ID)
                        && cv.containsKey(SyncDataConverter.ATTACHMENT_ENCRYPT_KEY)) {
                    delValues.add(cv);
                }
            }
        }
        if (delValues.size() > 0) {
            dumpContentValuesStatus(tag + " delete", delValues);
            bundle.putParcelableArrayList(BUNDLE_DELETE, delValues);
        }
        return bundle;
    }

    private static void handleSyncAttachmentResultInternal(Bundle bundle, final boolean isHandleShareData) {
        String tag = isHandleShareData ? "handleSyncAttachmentResult" : "handleShareSyncAttachmentResult";
        if (bundle == null) {
            log.error(tag + " return by bundle is null");
            return;
        }
        //need check sync failed
        int resultCode = bundle.getInt("result");
        if (resultCode != 0) {
            log.error(tag + " return by resultCode is [" + resultCode + "]");
            return;
        }
        final ArrayList<ContentValues> addedList = bundle.getParcelableArrayList(BUNDLE_ADDED);
        dumpContentValuesStatus(tag + " bundle addedList", addedList);
        if (addedList != null && addedList.size() > 0) {
            long userId = SyncManager.getCloudAccountUserId(IdeaPillsApp.getInstance());
            List<BubbleItem> voiceAttachmentList = new ArrayList<BubbleItem>();
            List<ContentValues> loseVoiceAttachmentList = new ArrayList<ContentValues>();
            List<ContentValues> attachmentList = new ArrayList<ContentValues>();
            for (ContentValues syncIdValue : addedList) {
                if (syncIdValue == null) {
                    continue;
                }

                if (syncIdValue.getAsInteger(SyncDataConverter.ATTACHMENT_ID) == null) {
                    continue;
                }
                int id = syncIdValue.getAsInteger(SyncDataConverter.ATTACHMENT_ID);
                int type = syncIdValue.getAsInteger(SyncDataConverter.ATTACHMENT_TYPE);
                String syncId = syncIdValue.getAsString(SyncDataConverter.ATTACHMENT_SYNC_ID);
                int version = syncIdValue.getAsInteger(SyncDataConverter.ATTACHMENT_VERSION);
                String encryptKey;
                byte[] encryptKeyBytes = syncIdValue.getAsByteArray(SyncDataConverter.ATTACHMENT_ENCRYPT_KEY);
                if (encryptKeyBytes != null) {
                    encryptKey = SyncDataConverter.byteArrayToHexStr(encryptKeyBytes);
                } else {
                    encryptKey = null;
                }
                long attachmentUserId = getAttachmentUserId(syncIdValue, userId, isHandleShareData);
                if (type == SyncMediaTypeHelper.ATTACHMENT_TYPE_VOICE_RECORD) {
                    // for attachment we pass bubble id in ATTACHMENT_ID
                    BubbleItem bubbleItem = GlobalBubbleManager.getInstance().getBubbleItemById(id);
                    if (bubbleItem != null) {
                        bubbleItem.setVoiceSyncId(syncId);
                        bubbleItem.setVoiceVersion(version);
                        bubbleItem.setVoiceEncryptKey(encryptKey);
                        if (!voiceAttachmentList.contains(bubbleItem)) {
                            voiceAttachmentList.add(bubbleItem);
                        }
                    } else {
                        ContentValues cv = new ContentValues();
                        cv.put(BUBBLE.ID, id);
                        cv.put(BUBBLE.VOICE_SYNC_ID, syncId);
                        cv.put(BUBBLE.VOICE_VERSION, version);
                        cv.put(BUBBLE.VOICE_SYNC_ENCRYPT_KEY, encryptKey);
                        loseVoiceAttachmentList.add(cv);
                    }
                } else {
                    AttachMentItem attachMentItem = GlobalBubbleManager.getInstance().getAttachmentItemById(id);
                    if (attachMentItem != null) {
                        attachMentItem.setSyncId(syncId);
                        attachMentItem.setVersion(version);
                        attachMentItem.setSyncEncryptKey(encryptKey);
                        if (attachmentUserId > 0) {
                            attachMentItem.setUserId(attachmentUserId);
                        }
                        if (syncIdValue.containsKey(SyncDataConverter.ATTACHMENT_DOWNLOAD_STATUS)) {
                            attachMentItem.setDownloadStatus(
                                    syncIdValue.getAsInteger(SyncDataConverter.ATTACHMENT_DOWNLOAD_STATUS));
                        }
                        if (syncIdValue.containsKey(SyncDataConverter.ATTACHMENT_UPLOAD_STATUS)) {
                            attachMentItem.setUploadStatus(
                                    syncIdValue.getAsInteger(SyncDataConverter.ATTACHMENT_UPLOAD_STATUS));
                        }
                        attachmentList.add(attachMentItem.toContentValues());
                    } else {
                        ContentValues cv = new ContentValues();
                        cv.put(ATTACHMENT.ID, id);
                        cv.put(ATTACHMENT.SYNC_ID, syncId);
                        cv.put(ATTACHMENT.VERSION, version);
                        cv.put(ATTACHMENT.SYNC_ENCRYPT_KEY, encryptKey);
                        if (attachmentUserId > 0) {
                            cv.put(ATTACHMENT.USER_ID, attachmentUserId);
                        }
                        if (syncIdValue.containsKey(SyncDataConverter.ATTACHMENT_DOWNLOAD_STATUS)) {
                            cv.put(ATTACHMENT.DOWNLOAD_STATUS,
                                    syncIdValue.getAsInteger(SyncDataConverter.ATTACHMENT_DOWNLOAD_STATUS));
                        }
                        if (syncIdValue.containsKey(SyncDataConverter.ATTACHMENT_UPLOAD_STATUS)) {
                            cv.put(ATTACHMENT.UPLOAD_STATUS,
                                    syncIdValue.getAsInteger(SyncDataConverter.ATTACHMENT_UPLOAD_STATUS));
                        }
                        attachmentList.add(cv);
                    }
                }
            }
            ArrayList<ContentValues> voiceUpdateList = new ArrayList<ContentValues>();
            if (voiceAttachmentList.size() > 0) {
                for (BubbleItem bubbleItem : voiceAttachmentList) {
                    voiceUpdateList.add(bubbleItem.toContentValues());
                }
            }
            if (loseVoiceAttachmentList.size() > 0) {
                voiceUpdateList.addAll(loseVoiceAttachmentList);
            }

            if (voiceUpdateList.size() > 0) {
                updateBubbles(voiceUpdateList, false);
            }
            if (attachmentList.size() > 0) {
                updateAttachments(attachmentList);
            }
        }

        // clear db
        BubbleDB.clearAttachments(ATTACHMENT.WHERE_CLEAR_ATTACHMENT_TABLE);

        //sync delete status, just remove it
        List<ContentValues> deleteList = bundle.getParcelableArrayList(BUNDLE_DELETE);
        dumpContentValuesStatus(tag + " bundle deleteList", deleteList);
        if (deleteList != null && deleteList.size() > 0) {
            List<String> attachmentIdList = new ArrayList<String>();
            List<String> voiceAttachmentIdList = new ArrayList<String>();
            for (ContentValues cv : deleteList) {
                if (cv == null) {
                    continue;
                }
                String syncId = cv.getAsString(SyncDataConverter.ATTACHMENT_SYNC_ID);
                int type = cv.getAsInteger(SyncDataConverter.ATTACHMENT_TYPE);
                if (type == SyncMediaTypeHelper.ATTACHMENT_TYPE_VOICE_RECORD) {
                    if (syncId != null) {
                        voiceAttachmentIdList.add(syncId);
                    }
                } else {
                    if (syncId != null) {
                        attachmentIdList.add(syncId);
                    }
                }
            }
            if (attachmentIdList.size() > 0) {
                String where = ATTACHMENT.inSql(ATTACHMENT.SYNC_ID, attachmentIdList);
                where = where + " AND " + ATTACHMENT.MARK_DELETE + "=1";
                BubbleDB.clearAttachments(where);

                String updateWhere = where;
                BubbleDB.updateAttachmentColumnToNull(ATTACHMENT.SYNC_ID, updateWhere);
            }
            if (voiceAttachmentIdList.size() > 0) {
                String where = BUBBLE.inSql(BUBBLE.VOICE_SYNC_ID, voiceAttachmentIdList);
                String delWhere = where + " AND " + BUBBLE.MARK_DELETE + "=1";
                BubbleDB.deleteBubbleBy(delWhere);
            }
        }
    }

    public static void syncFinish() {
        sInSyncing = false;
        log.info("=========syncFinish========");
        if (sSyncFileLogger != null) {
            sSyncFileLogger.info("=========syncFinish========");
        }
        SyncShareManager.INSTANCE.popAllIdeapillsShareNotify();
    }

    public static void syncLogin() {
        SyncShareRepository.initInviterSwitch();
        SyncShareManager.INSTANCE.refreshInvitationList();
    }

    public static void syncLogout(boolean isDataInitReady) {
        log.info("=========syncLogout========");
        if (!isDataInitReady) {
            DBHelper.init();
        }
        SyncShareManager.INSTANCE.clearAll();
        BubbleDB.clearBubbleSyncInfo(null, null);

        BubbleDB.clearBubbleShareData();
        BubbleDB.clearEmptyBubble();
        if (isDataInitReady) {
            UIHandler.post(new Runnable() {
                @Override
                public void run() {
                    GlobalBubbleManager.getInstance().syncLogout();
                }
            });
        }
    }

    public static void downloadAttachmentResult(Bundle bundle) {
        String syncId = bundle.getString("file_sync_id");
        final AttachMentItem attachMentItem = GlobalBubbleManager.getInstance().getAttachmentItemBySyncId(syncId);
        if (attachMentItem != null) {
            final BubbleItem bubbleItem = GlobalBubbleManager.getInstance().getBubbleByAttachmentId(attachMentItem.getId());
            final int downloadResult = bundle.getInt("result", -1);
            final String filePath = bundle.getString("file_dir");
            final ResultReceiver resultReceiver = (ResultReceiver)bundle.getParcelable("load_result_receiver");
            log.info(syncId + " downloadAttachmentResult:" + downloadResult);
            UIHandler.post(new Runnable() {
                @Override
                public void run() {
                    attachMentItem.setDownloadStatus(downloadResult == 0 ? AttachMentItem.DOWNLOAD_STATUS_DOWNLOAD_SUCCESS :
                            AttachMentItem.DOWNLOAD_STATUS_DOWNLOAD_FAIL);
                    attachMentItem.setUri(Uri.fromFile(new File(filePath)));
                    attachMentItem.setStatus(AttachMentItem.STATUS_SUCCESS);
                    if (bubbleItem != null) {
                        bubbleItem.setRefreshAttachment(true);
                    }
                    GlobalBubbleManager.getInstance().updateAttachmentItem(attachMentItem);
                    if (resultReceiver != null) {
                        resultReceiver.send(0, null);
                    } else {
                        GlobalBubbleManager.getInstance().notifyUpdate();
                    }
                }
            });
        }
    }

    public static void sendShareInvite(Bundle params) {
        final String phoneNum;
        if (params != null) {
            phoneNum = params.getString(SyncUtil.PARAM_KEY_PHONE_NUM);
        } else {
            phoneNum = null;
        }
        if (TextUtils.isEmpty(phoneNum)) {
            return;
        }
        UIHandler.post(new Runnable() {
            @Override
            public void run() {
                GlobalInvitationAction.getInstance().sendInvitation(phoneNum, new GlobalInvitationAction.InvitationSuccessListener() {
                    @Override
                    public void onInvitationSendSuccess(SyncShareInvitation syncShareInvitation) {
                        IdeaPillsApp.getInstance().sendBroadcast(new Intent(Constants.BROADCAST_ACTION_INVITATION_SEND));
                    }
                });
            }
        });
    }

    public static boolean canShare(boolean showToast) {
        Bundle bundle = canShareInternal(showToast, false);
        return bundle != null && bundle.getBoolean(SyncUtil.RESULT_KEY_CAN_SHARE);
    }

    public static Bundle canShareFromCrossProcess(Bundle params) {
        boolean showToast = false;
        if (params != null) {
            showToast = params.getBoolean(SyncUtil.PARAM_KEY_SHOW_TOAST);
        }
        return canShareInternal(showToast, true);
    }

    private static Bundle canShareInternal(boolean showToast, boolean isFromCrossProcess) {
        Bundle bundle = new Bundle();
        final long userId = SyncManager.getCloudAccountUserId(IdeaPillsApp.getInstance());
        if (userId < 0) {
            if (showToast) {
                UIHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        GlobalInvitationAction.getInstance().showLoginDialog(true);
                    }
                });
            } else if (isFromCrossProcess) {
                bundle.putBoolean(SyncUtil.SHOW_LOGIN_DIALOG_FLAG, true);
            }
            bundle.putBoolean(SyncUtil.RESULT_KEY_CAN_SHARE, false);
            return bundle;
        }
        if (!SyncManager.syncEnable(IdeaPillsApp.getInstance())) {
            if (showToast) {
                UIHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        GlobalInvitationAction.getInstance().showLoginDialog(false);
                    }
                });
            } else if (isFromCrossProcess) {
                bundle.putBoolean(SyncUtil.SHOW_LOGIN_SYNC_DIALOG_FLAG, true);
            }
            bundle.putBoolean(SyncUtil.RESULT_KEY_CAN_SHARE, false);
            return bundle;
        }
        List<Long> addParticipantIds = SyncShareUtils.findParticipantIdsForAdd(userId, SyncShareManager.INSTANCE.getCachedInvitationList());
        if (addParticipantIds.isEmpty()) {
            SyncShareInvitation invitation = SyncShareManager.INSTANCE.getInvitation(userId);
            if (invitation != null && (invitation.inviteStatus == SyncShareInvitation.INVITE_ACCEPT ||
                    invitation.inviteStatus == SyncShareInvitation.INVITE_START)) {
                if (showToast) {
//                    GlobalBubbleUtils.showSystemToast(IdeaPillsApp.getInstance(), R.string.sync_invitation_sended_tip, Toast.LENGTH_SHORT);
                }
                bundle.putBoolean(SyncUtil.RESULT_KEY_CAN_SHARE, true);
            } else {
                if (showToast) {
                    UIHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            GlobalInvitationAction.getInstance().showInvitationDialog(new GlobalInvitationAction.InvitationSuccessListener() {
                                @Override
                                public void onInvitationSendSuccess(SyncShareInvitation syncShareInvitation) {
                                    IdeaPillsApp.getInstance().sendBroadcast(new Intent(Constants.BROADCAST_ACTION_INVITATION_SEND));
                                }
                            });
                        }
                    });
                } else if (isFromCrossProcess) {
                    bundle.putBoolean(SyncUtil.SHOW_INVITATION_DIALOG_FLAG, true);
                }
                bundle.putBoolean(SyncUtil.RESULT_KEY_CAN_SHARE, false);
            }
            return bundle;
        } else {
            bundle.putBoolean(SyncUtil.RESULT_KEY_CAN_SHARE, true);
            return bundle;
        }
    }

    public static Bundle handleSyncPush(Bundle params) {
        if (params == null) {
            return new Bundle();
        }
        ContentValues cv = params.getParcelable("pushdata");
        if (cv == null) {
            return new Bundle();
        }
        SyncPushItem syncPushItem = new SyncPushItem();
        syncPushItem.fromContentValues(cv);
        Context context = IdeaPillsApp.getInstance();
        long userId = SyncManager.getCloudAccountUserId(context);
        // TODO notify
        if (SyncPushItem.SHARE_ACTION_START_INVITE == syncPushItem.action) {
            if(syncPushItem.tid == userId){
                SyncShareManager.INSTANCE.refreshInvitationList();
                SyncShareInvitation invitation = new SyncShareInvitation();
                invitation.inviteStatus = syncPushItem.inviteStatus;
                invitation.inviter = new SyncShareUser();
                invitation.inviter.id = syncPushItem.fid;
                invitation.inviter.remark = "";
                invitation.inviter.nickname = syncPushItem.fName;
                invitation.invitee = new SyncShareUser();
                invitation.invitee.id = syncPushItem.tid;
                invitation.invitee.remark = "";
                invitation.createdAt = System.currentTimeMillis();
                SyncShareManager.INSTANCE.sendInvitationNotify(context, syncPushItem.fid, invitation);
            }
        } else if (SyncPushItem.SHARE_ACTION_HANDLE_INVITE == syncPushItem.action) {
            if(syncPushItem.tid == userId){
                SyncShareManager.INSTANCE.refreshInvitationList();
                SyncShareInvitation invitation = new SyncShareInvitation();
                invitation.inviteStatus = syncPushItem.inviteStatus;
                invitation.inviter = new SyncShareUser();
                invitation.inviter.id = syncPushItem.tid;
                invitation.inviter.remark = "";
                invitation.invitee = new SyncShareUser();
                invitation.invitee.id = syncPushItem.fid;
                invitation.invitee.remark = "";
                invitation.invitee.nickname = syncPushItem.fName;
                invitation.createdAt = System.currentTimeMillis();
                SyncShareManager.INSTANCE.sendInvitationNotify(context, syncPushItem.fid, invitation);
            }

        } else if (SyncPushItem.SHARE_ACTION_REMOVE_INVITE == syncPushItem.action) {
            if (syncPushItem.tid == userId) {
                SyncShareManager.INSTANCE.refreshInvitationList();
                SyncShareManager.INSTANCE.sendRemoveShareNotify(context, (int) syncPushItem.fid, syncPushItem.fName);
            }
        } else if (SyncPushItem.SHARE_ACTION_START_SHARE == syncPushItem.action) {
            if (syncPushItem.tid == userId) {
                SyncShareManager.INSTANCE.sendIdeapillsShareNotify(context, syncPushItem);
            }
        } else if (SyncPushItem.SHARE_ACTION_CANCEL_SHARE == syncPushItem.action) {
            if (syncPushItem.tid == userId) {
                SyncShareManager.INSTANCE.sendIdeapillsShareNotify(context, syncPushItem);
            }
        } else if (SyncPushItem.SHARE_ACTION_EDIT_SHARED_PILL == syncPushItem.action) {
            if (syncPushItem.tid == userId) {
                SyncShareManager.INSTANCE.sendIdeapillsEditNotify(context, syncPushItem);
            }
        }
        return new Bundle();
    }

    private static void insertBubbles(List<BubbleItem> addedList) {
        BubbleDB.bulkInsert(addedList);
    }

    private static void insertAttachments(List<AttachMentItem> addedList, long userId) {
        for (final AttachMentItem item : addedList) {
            if (item == null) {
                continue;
            }
            item.setUserId(userId);
        }
        BubbleDB.bulkInsert(addedList);
    }

    private static void insertAttachments(List<AttachMentItem> addedList) {
        BubbleDB.bulkInsert(addedList);
    }

    private static void updateBubbles(List<ContentValues> list, boolean alsoSyncBubbleSyncIdsToAttachment) {
        List<ContentValues> valuesList = new ArrayList<>();
        List<String> selectionList = new ArrayList<>();
        List<ContentValues> attachmentValuesList = new ArrayList<>();
        List<String> attachmentSelectionList = new ArrayList<>();
        for (ContentValues value : list) {
            if (value == null) {
                continue;
            }
            if (value.containsKey(BUBBLE.ID)) {
                int id = value.getAsInteger(BUBBLE.ID);
                if (id > 0) {
                    String where = BUBBLE.ID + "=" + id;
                    selectionList.add(where);
                    valuesList.add(value);

                    if (value.containsKey(BUBBLE.SYNC_ID) && alsoSyncBubbleSyncIdsToAttachment) {
                        attachmentSelectionList.add(ATTACHMENT.BUBBLE_ID + "=" + id);
                        ContentValues attachmentCv = new ContentValues();
                        String syncId = value.getAsString(BUBBLE.SYNC_ID);
                        attachmentCv.put(ATTACHMENT.BUBBLE_SYNC_ID, syncId);
                        attachmentValuesList.add(attachmentCv);
                    }
                }
                value.remove(BUBBLE.ID);
            }
        }
        BubbleDB.updateBubbles(valuesList, selectionList);
        if (!attachmentValuesList.isEmpty() && !attachmentSelectionList.isEmpty()) {
            BubbleDB.updateAttachments(attachmentValuesList, attachmentSelectionList);
        }
    }

    private static void updateAttachmentsSyncIds(Map<Integer, String> idMap) {
        List<ContentValues> valuesList = new ArrayList<>();
        List<String> selectionList = new ArrayList<>();
        for (Integer id : idMap.keySet()) {
            if (id == null || id <= 0) {
                continue;
            }
            String syncId = idMap.get(id);
            if (!TextUtils.isEmpty(syncId)) {
                selectionList.add(ATTACHMENT.BUBBLE_ID + "=" + id);
                ContentValues attachmentCv = new ContentValues();
                attachmentCv.put(ATTACHMENT.BUBBLE_SYNC_ID, syncId);
                valuesList.add(attachmentCv);
            }
        }
        if (!valuesList.isEmpty() && !selectionList.isEmpty()) {
            BubbleDB.updateAttachments(valuesList, selectionList);
        }
    }

    private static void updateAttachments(List<ContentValues> list) {
        List<ContentValues> valuesList = new ArrayList<>();
        List<String> selectionList = new ArrayList<>();
        for (ContentValues value : list) {
            if (value == null) {
                continue;
            }
            if (value.containsKey(ATTACHMENT.ID)) {
                int id = value.getAsInteger(ATTACHMENT.ID);
                if (id > 0) {
                    String where = ATTACHMENT.ID + "=" + id;
                    valuesList.add(value);
                    selectionList.add(where);
                }
                value.remove(ATTACHMENT.ID);
            }
        }
        BubbleDB.updateAttachments(valuesList, selectionList);
    }

    private static long getBubbleUserId(ContentValues cv, long loginUserId, boolean isHandleShareData) {
        if (isHandleShareData) {
            Long ownerUserId = cv.getAsLong(SyncDataConverter.BUBBLE_USER_ID);
            if (ownerUserId != null && ownerUserId > 0) {
                return ownerUserId;
            }
        } else {
            return loginUserId;
        }
        return -1;
    }

    private static long getAttachmentUserId(ContentValues cv, long loginUserId, boolean isHandleShareData) {
        if (isHandleShareData) {
            Long ownerUserId = cv.getAsLong(SyncDataConverter.ATTACHMENT_USER_ID);
            if (ownerUserId != null && ownerUserId > 0) {
                return ownerUserId;
            }
        } else {
            return loginUserId;
        }
        return -1;
    }

    static void notifyContent(String content) {
        Context context = IdeaPillsApp.getInstance();
        if (context == null) {
            return;
        }
        NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        Intent todoIntent = new Intent();
        todoIntent.setAction("com.smartisanos.ideapills.todo");
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, todoIntent, 0);
        Notification.Builder builder = new Notification.Builder(context)
                .setSmallIcon(R.drawable.sara)
                .setTicker(context.getString(R.string.notify_todo_title, content))
                .setContentTitle(context.getString(R.string.notify_todo_title, content))
                .addAction(0, "", pendingIntent)
                .setContentText(context.getString(R.string.notify_todo_title, content));
        Notification notification = builder.build();
        nm.notify(241, notification);
    }

    private static void dumpStringListStatus(String dumpTitle, List<String> stringList) {
        if (stringList == null) {
            return;
        }
        StringBuilder logBuilder = new StringBuilder();
        logBuilder.append("==").append(dumpTitle).append("  size:").append(stringList.size()).append(" begin==");
        for (String stringItem : stringList) {
            logBuilder.append("\n").append(stringItem);
        }
        logBuilder.append("\n==").append(dumpTitle).append(" end==");
        String logInfo = logBuilder.toString();
        if (LOG.DBG) {
            log.info(logInfo);
        }
        if (sSyncFileLogger != null) {
            sSyncFileLogger.info(logInfo);
        }
    }

    private static void dumpContentValuesStatus(String dumpTitle, List<ContentValues> contentValuesList) {
        if (contentValuesList == null) {
            return;
        }
        StringBuilder logBuilder = new StringBuilder();
        logBuilder.append("==").append(dumpTitle).append("  size:").append(contentValuesList.size()).append(" begin==");
        for (ContentValues contentValues : contentValuesList) {
            logBuilder.append("\n");
            for (String name : contentValues.keySet()) {
                String value = contentValues.getAsString(name);
                if (value != null && SyncDataConverter.BUBBLE_TEXT.equals(name)) {
                    value = value.length() > 5 ? value.substring(0, 5) : value;
                }
                logBuilder.append(" ").append(name).append("=").append(value);
            }
        }
        logBuilder.append("\n==").append(dumpTitle).append(" end==");
        String logInfo = logBuilder.toString();
        if (LOG.DBG) {
            log.info(logInfo);
        }
        if (sSyncFileLogger != null) {
            sSyncFileLogger.info(logInfo);
        }
    }

    private static void dumpBubblesStatus(String dumpTitle, List<BubbleItem> bubbleItems) {
        if (bubbleItems == null) {
            return;
        }
        StringBuilder logBuilder = new StringBuilder();
        logBuilder.append("\n==").append(dumpTitle).append("  size:").append(bubbleItems.size()).append(" begin==");
        for (BubbleItem item : bubbleItems) {
            logBuilder.append("\nsyncId:").append(item.getSyncId()).append(", Id:").
                    append(item.getId()).append(", toDo:").append(item.getToDo()).append(", txt:");
            if (item.getText() != null) {
                logBuilder.append(item.getText().length() > 5 ? item.getText().substring(0, 5) : item.getText());
            }
        }
        logBuilder.append("\n==").append(dumpTitle).append(" end==");
        String logInfo = logBuilder.toString();
        if (LOG.DBG) {
            log.info(logInfo);
        }
        if (sSyncFileLogger != null) {
            sSyncFileLogger.info(logInfo);
        }
    }

    private static void dumpAttachmentsStatus(String dumpTitle, List<AttachMentItem> attachMentItems) {
        if (attachMentItems == null) {
            return;
        }
        StringBuilder logBuilder = new StringBuilder();
        logBuilder.append("==").append(dumpTitle).append("  size:").append(attachMentItems.size()).append(" begin==");
        for (AttachMentItem item : attachMentItems) {
            logBuilder.append("\nsyncId:").append(item.getSyncId()).append(", Id:").append(item.getId()).append(", ").append(item.getFilename());
        }
        logBuilder.append("\n==").append(dumpTitle).append(" end==");
        String logInfo = logBuilder.toString();
        if (LOG.DBG) {
            log.info(logInfo);
        }
        if (sSyncFileLogger != null) {
            sSyncFileLogger.info(logInfo);
        }
    }

    /**
     * nullTextBubbleCount = "select count(*)  from  bubble where text = \"\" and removed_time > 0  and mark_delete = 0";
     * nullTextAndHasAttachMentCount = "select count(distinct bubble_id) from attachment where bubble_id  IN  (select  _id  from
     * bubble where text = \"\" and removed_time >0  and mark_delete =0) and mark_delete =0";
     *
     * @return
     */
    public static Bundle getRealBubbleCountData() {

        int allCount = BubbleDB.count(BUBBLE.WHERE_CASE_NOT_DELETE + " AND " + BUBBLE.WHERE_SYNC_ID_NOT_NULL);
        int nullTextBubbleCount = BubbleDB.count(BUBBLE.NAME, BUBBLE.WHERE_SYNC_DELETE_COUNT_TEXT_NULL, false, null);
        int realData;
        if (nullTextBubbleCount > 0) {// need excude dirty data
            int tempNullTextAndHasAttachMentCount = BubbleDB.count(ATTACHMENT.NAME, ATTACHMENT.WHERE_SYNC_BUBBLE_TEXT_NULL_HAVE_ATTACHMENT_BUBBLE_ID, true, ATTACHMENT.BUBBLE_ID);
            int nullTextAndHasAttachMentCount = tempNullTextAndHasAttachMentCount >= 0 ? tempNullTextAndHasAttachMentCount : 0;
            int nullTextAndNoAttachMentCount = nullTextBubbleCount - nullTextAndHasAttachMentCount;
            int tempRealData = allCount - nullTextAndNoAttachMentCount;
            realData = tempRealData >= 0 ? tempRealData : 0;
        } else {
            realData = allCount;
        }
        Bundle bundle = new Bundle();
        bundle.putInt("count", realData);
        return bundle;
    }
}
