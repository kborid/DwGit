package com.smartisanos.ideapills.data;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.text.TextUtils;

import com.smartisanos.ideapills.Constants;
import com.smartisanos.ideapills.IdeaPillsApp;
import com.smartisanos.ideapills.BubbleController;
import com.smartisanos.ideapills.interfaces.VoiceAssistantInterface;
import com.smartisanos.ideapills.remind.util.AlarmUtils;
import com.smartisanos.ideapills.sync.SyncManager;
import com.smartisanos.ideapills.sync.SyncProcessor;
import com.smartisanos.ideapills.sync.share.SyncShareManager;
import com.smartisanos.ideapills.util.Demonstration;
import com.smartisanos.ideapills.util.GlobalBubbleManager;
import com.smartisanos.ideapills.util.LOG;
import com.smartisanos.ideapills.common.util.TaskHandler;
import com.smartisanos.ideapills.util.Utils;
import com.smartisanos.ideapills.entity.AttachMentItem;
import com.smartisanos.ideapills.entity.BubbleItem;
import com.smartisanos.ideapills.common.util.UIHandler;
import com.smartisanos.ideapills.common.util.AttachmentUtil;

import android.service.onestep.GlobalBubble;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

public class DataHandler {
    private static final LOG log = LOG.getInstance(DataHandler.class);

    private static final HandlerThread sWorkerThread = new HandlerThread("DataHandler");
    static {
        sWorkerThread.start();
    }

    private static final int TASK_NUM_BASE           = 20000;
    public static final int TASK_DATA_INIT           = TASK_NUM_BASE + 1;
    public static final int TASK_UPDATE_WEIGHT       = TASK_NUM_BASE + 2;
    public static final int TASK_ADD_BUBBLE          = TASK_NUM_BASE + 3;
    public static final int TASK_UPDATE_BUBBLE_BY_ID = TASK_NUM_BASE + 4;
    public static final int TASK_REMOVE_BUBBLE       = TASK_NUM_BASE + 5;
    public static final int TASK_UPDATE_URI          = TASK_NUM_BASE + 6;
    public static final int TASK_MARK_DELETE         = TASK_NUM_BASE + 7;
    public static final int TASK_ADD_ATTACHMENTS     = TASK_NUM_BASE + 8;
    public static final int TASK_REMOVE_ATTACHMENTS  = TASK_NUM_BASE + 9;

    public static final int TASK_UPDATE_BUBBLE_BY_ID_AND_SYNC = TASK_NUM_BASE + 10;
    public static final int TASK_UPDATE_ATTACHMENT_BY_ID = TASK_NUM_BASE + 12;
    public static final int TASK_UPDATE_VOICE_WAVE_DATA = TASK_NUM_BASE + 13;
    public static final int TASK_MERGE_VOICE_WAVE_DATA = TASK_NUM_BASE + 14;
    public static final int TASK_REMOVE_ATTACHMENTS_AND_SYNC = TASK_NUM_BASE + 15;
    public static final int TASK_CHECK_OFFLINE = TASK_NUM_BASE + 16;

    public static final int TASK_UPDATE_BUBBLE_TO_NOT_SHARED = TASK_NUM_BASE + 17;
    public static final int TASK_UPDATE_BUBBLE_TO_NOT_SHARED_AND_SYNC = TASK_NUM_BASE + 18;
    public static final int TASK_CANCEL_SHARE_RELATION = TASK_NUM_BASE + 19;
    public static final int TASK_REMOVE_FOREVER = TASK_NUM_BASE + 20;
    public static final int TASK_CLEAR_SHARE_BUBBLE_DATA = TASK_NUM_BASE + 21;
    public static final int TASK_TODO_OVER_CHANGE = TASK_NUM_BASE + 22;

    public static void handleTask(int task) {
        handleTask(task, null);
    }

    public static void handleTask(int task, List params) {
        Message msg = mWorker.obtainMessage();
        msg.what = task;
        msg.obj = params;
        mWorker.sendMessage(msg);
    }

    public static boolean post(Runnable r) {
        return mWorker.post(r);
    }

    public static boolean postDelayed(Runnable r, long delayMillis) {
        return mWorker.postDelayed(r, delayMillis);
    }

    public static void removeCallbacks(Runnable r) {
        mWorker.removeCallbacks(r);
    }

    private static final Handler mWorker = new Handler(sWorkerThread.getLooper()) {
        @Override
        public void handleMessage(Message msg) {
            int action = msg.what;
            List params = (List) msg.obj;
            try {
                switch (action) {
                    case TASK_DATA_INIT : {
                        handleTASK_DATA_INIT();
                        break;
                    }
                    case TASK_UPDATE_WEIGHT : {
                        handleTASK_UPDATE_WEIGHT(params);
                        break;
                    }
                    case TASK_ADD_BUBBLE : {
                        handleTASK_ADD_BUBBLE(params);
                        break;
                    }
                    case TASK_UPDATE_BUBBLE_BY_ID : {
                        handleTASK_UPDATE_BUBBLE_BY_ID(params, false);
                        break;
                    }
                    case TASK_UPDATE_BUBBLE_BY_ID_AND_SYNC : {
                        handleTASK_UPDATE_BUBBLE_BY_ID(params, true);
                        break;
                    }
                    case TASK_REMOVE_BUBBLE : {
                        handleTASK_REMOVE_BUBBLE(params);
                        break;
                    }
                    case TASK_UPDATE_URI : {
                        handleTASK_UPDATE_URI(params);
                        break;
                    }
                    case TASK_MARK_DELETE : {
                        handleTASK_MARK_DELETE(params);
                        break;
                    }
                    case TASK_ADD_ATTACHMENTS : {
                        handleTASK_ADD_ATTACHMENTS(params);
                        break;
                    }
                    case TASK_REMOVE_ATTACHMENTS: {
                        handleTASK_REMOVE_ATTACHMENTS(params, false);
                        break;
                    }
                    case TASK_REMOVE_ATTACHMENTS_AND_SYNC: {
                        handleTASK_REMOVE_ATTACHMENTS(params, true);
                        break;
                    }
                    case TASK_CHECK_OFFLINE: {
                        handleTASK_CHECK_OFFLINE();
                        break;
                    }
                    case TASK_UPDATE_ATTACHMENT_BY_ID: {
                        handleTASK_SYNC_UPDATE_ATTACHMENT_BY_ID(params);
                        break;
                    }
                    case TASK_UPDATE_VOICE_WAVE_DATA: {
                        handleTASK_UPDATE_VOICE_WAVE_DATA(params);
                        break;
                    }
                    case TASK_MERGE_VOICE_WAVE_DATA: {
                        handleTASK_MERGE_WAVE_DATA(params);
                        break;
                    }
                    case TASK_UPDATE_BUBBLE_TO_NOT_SHARED : {
                        handleTASK_UPDATE_BUBBLE_TO_NOT_SHARED(params, false);
                        break;
                    }
                    case TASK_UPDATE_BUBBLE_TO_NOT_SHARED_AND_SYNC : {
                        handleTASK_UPDATE_BUBBLE_TO_NOT_SHARED(params, true);
                        break;
                    }
                    case TASK_CANCEL_SHARE_RELATION : {
                        handleTASK_CANCEL_SHARE_RELATION(params);
                        break;
                    }
                    case TASK_REMOVE_FOREVER : {
                        handleTASK_REMOVE_FOREVER(params);
                        break;
                    }
                    case TASK_CLEAR_SHARE_BUBBLE_DATA : {
                        handleTASK_CLEAR_SHARE_BUBBLE_DATA();
                        break;
                    }
                    case TASK_TODO_OVER_CHANGE: {
                        handleTASK_TODO_OVER_CHANGE();
                        break;
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    };

    private static void handleTASK_DATA_INIT() {
        IdeaPillsApp app = IdeaPillsApp.getInstance();
        int count = BubbleDB.count(null);
        if (count == 0) {
            if (Demonstration.ENABLE) {
                Demonstration.initDemoDataIfNeeded(app);
            } else {
                //load data from sidebar
                loadDataFromSidebar(app);
            }
        }
        loadDataFromVoiceAssistant(app);
        BubbleDB.deleteBubbleBy(BUBBLE.WHERE_CLEAR_BUBBLE_TABLE);
        BubbleDB.clearAttachments(ATTACHMENT.WHERE_CLEAR_ATTACHMENT_TABLE);
        ContentValues updateValues = new ContentValues();
        updateValues.put(BUBBLE.STATUS, 0);
        BubbleDB.updateBubble(updateValues, BUBBLE.STATUS + " is null");
        BubbleDB.clearEmptyBubble();
        //check BUBBLE.VOICE_DURATION
        String where = BUBBLE.VOICE_DURATION + "=0 AND " + BUBBLE.TYPE + " in ("+
                BubbleItem.TYPE_VOICE+", "+BubbleItem.TYPE_VOICE_OFFLINE+")";
        List<BubbleItem> list = BubbleDB.listBubble(where);
        if (list != null && list.size() > 0) {
            //generate voice duration
            Utils.generateVoiceDuration(app, list);
            for (BubbleItem item : list) {
                String whereCause = BUBBLE.ID + "=" + item.getId();
                ContentValues values = new ContentValues();
                values.put(BUBBLE.VOICE_DURATION, item.getVoiceDuration());
                BubbleDB.updateBubble(values, whereCause);
            }
        }
        final List<BubbleItem> items = BubbleDB.listVisibleBubbles();
        if (items != null) {
            List<BubbleItem> voiceItems = new ArrayList<BubbleItem>();
            for (BubbleItem item : items) {
                if (item == null) {
                    continue;
                }
                if (item.isVoiceBubble()) {
                    voiceItems.add(item);
                }
                String whereCause = ATTACHMENT.BUBBLE_ID + "=" + item.getId() + " AND " + ATTACHMENT.MARK_DELETE + "=0 ";
                List<AttachMentItem> attachments = BubbleDB.listAttachments(whereCause);
                item.setAttachments(attachments);
            }
            if (voiceItems.size() > 0) {
                Utils.loadWaveData(app, voiceItems);
            }
        }
        Constants.DATA_INIT_READY = true;
        UIHandler.post(new Runnable() {
            @Override
            public void run() {
                BubbleController.getInstance().addIdeaPillsWindow();
                GlobalBubbleManager.getInstance().updateBubbleList(items);
                SyncManager.requestSync();
                SyncShareManager.INSTANCE.refreshInvitationList();
                new AsyncTask<Object, Object, Object>() {

                    @Override
                    protected Object doInBackground(Object... params) {
                        if (items == null || items.isEmpty()) {
                            try {
                                AlarmUtils.deleteAllAlarmFromCalendar(IdeaPillsApp.getInstance());
                            } catch (Exception e) {
                                // ignore
                            }
                        }
                        return null;
                    }
                }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                AlarmUtils.scheduleNextAlarm(IdeaPillsApp.getInstance(), null);
            }
        });
    }

    private static void loadDataFromVoiceAssistant(Context context) {
        int recordCount = VoiceAssistantInterface.getRecordCountFromVoiceAssistant(context);
        if (recordCount <= 0) {
            return;
        }
        try {
            ArrayList<GlobalBubble> removedBubbles = VoiceAssistantInterface.loadOldCachedBubbleFromVoiceAssistant(context);
            if (removedBubbles != null && removedBubbles.size() > 0) {
                List<BubbleItem> items = new ArrayList<BubbleItem>();
                for (GlobalBubble bubble : removedBubbles) {
                    if (bubble == null) {
                        continue;
                    }
                    BubbleItem item = new BubbleItem(bubble);
                    item.setId(0);
                    if (item.getRemovedTime() == 0) {
                        item.setRemovedTime(System.currentTimeMillis());
                    }
                    items.add(item);
                }
                BubbleDB.bulkInsert(items);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            VoiceAssistantInterface.clearOldCache(context);
        }
    }

    private static void loadDataFromSidebar(Context context) {
        //old data
        List<BubbleItem> cachedItems = new ArrayList<BubbleItem>();
        Cursor cursor = null;
        try {
            Uri uri = Uri.parse("content://com.smartisanos.sidebar.idea_pills_sync");
            ContentResolver resolver = context.getContentResolver();
            cursor = resolver.query(uri, null, null, null, null);
            if (cursor != null && cursor.getCount() > 0) {
                if (cursor.moveToFirst()) {
                    int idIdx = cursor.getColumnIndex("_id");
                    int typeIdx = cursor.getColumnIndex("type");
                    int colorIdx = cursor.getColumnIndex("color");
                    int todoTypeIdx = cursor.getColumnIndex("todo_type");
                    int uriIdx = cursor.getColumnIndex("uri");
                    int textIdx = cursor.getColumnIndex("text");
                    int timeIdx = cursor.getColumnIndex("time_stamp");
                    int rateIdx = cursor.getColumnIndex("sampling_rate");
                    int weightIdx = cursor.getColumnIndex("weight");
                    do {
                        GlobalBubble bubble = new GlobalBubble();
                        bubble.setId(cursor.getInt(idIdx));
                        bubble.setColor(cursor.getInt(colorIdx));
                        bubble.setType(cursor.getInt(typeIdx));
                        bubble.setToDo(cursor.getInt(todoTypeIdx));
                        bubble.setText(cursor.getString(textIdx));
                        bubble.setTimeStamp(cursor.getLong(timeIdx));
                        bubble.setSamplineRate(cursor.getInt(rateIdx));
                        String bubbleUri = cursor.getString(uriIdx);
                        if (!TextUtils.isEmpty(bubbleUri)) {
                            bubble.setUri(Uri.parse(bubbleUri));
                        }
                        BubbleItem item = new BubbleItem(bubble, cursor.getInt(weightIdx));
                        cachedItems.add(item);
                    } while (cursor.moveToNext());
                }
            }
            resolver.delete(uri, null, null);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (cursor != null) {
                try {
                    cursor.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        BubbleDB.bulkInsert(cachedItems);

        //generate voice duration, sara will changed uri. just for load old data from 3.6.X
        String where = BUBBLE.VOICE_DURATION + "=0 AND " + BUBBLE.TYPE + " in ("+
                BubbleItem.TYPE_VOICE+", "+BubbleItem.TYPE_VOICE_OFFLINE+")";
        List<BubbleItem> list = BubbleDB.listBubble(where);
        if (list != null && list.size() > 0) {
            //generate voice duration
            Utils.generateVoiceDuration(context, list);
            for (BubbleItem item : list) {
                String whereCause = BUBBLE.ID + "=" + item.getId();
                ContentValues values = new ContentValues();
                values.put(BUBBLE.VOICE_DURATION, item.getVoiceDuration());
                BubbleDB.updateBubble(values, whereCause);
            }
        }
    }

    private static void handleTASK_UPDATE_WEIGHT(List params) {
        if (params == null || params.size() == 0) {
            return;
        }
        List<BubbleItem> items = (List<BubbleItem>) params.get(0);
        if (items == null || items.size() == 0) {
            return;
        }
        BubbleDB.update(items);
        SyncManager.requestSync();
    }

    private static void handleTASK_ADD_BUBBLE(List params) {
        if (params == null || params.size() == 0) {
            return;
        }
        List<BubbleItem> items = (List<BubbleItem>) params.get(0);
        if (items == null || items.size() == 0) {
            return;
        }
        boolean needSync = false;
        for (final BubbleItem item : items) {
            if (item == null) {
                continue;
            }
            if (item.getId() <= 0) {
                BubbleDB.insert(item);
                if (item.getId() > 0) {
                    if (item.haveAttachments()) {
                        List params1 = new ArrayList();
                        List<AttachMentItem> attachMentItemList = item.getAttachments();
                        for (AttachMentItem attachMentItem : attachMentItemList) {
                            attachMentItem.setBubbleId(item.getId());
                            Uri saveUri = AttachmentUtil.copyFileToInnerDir(IdeaPillsApp.getInstance(), attachMentItem.getOriginalUri(), attachMentItem.getFilename());
                            if (saveUri != null) {
                                attachMentItem.setUri(saveUri);
                                attachMentItem.setStatus(AttachMentItem.STATUS_SUCCESS);
                            } else {
                                attachMentItem.setStatus(AttachMentItem.STATUS_FAIL);
                            }
                        }
                        params1.add(attachMentItemList);
                        handleTASK_ADD_ATTACHMENTS(params1);
                    }
                    if (item.isVoiceBubble()) {
                        final byte[] wave;
                        IdeaPillsApp app = IdeaPillsApp.getInstance();
                        wave = Utils.getWaveData(app, item.getUri());
                        item.setWaveData(wave);
                    }
                    if (!item.isEmptyBubble()) {
                        needSync = true;
                    }
                }
            }
        }
        if (params.size() > 1) {
            Runnable callback = (Runnable) params.get(1);
            if (callback != null) {
                callback.run();
            }
        }
        if (needSync) {
            SyncManager.requestSync();
        }
    }

    private static void handleTASK_UPDATE_BUBBLE_BY_ID(List params, boolean needSync) {
        if (params == null || params.size() == 0) {
            return;
        }
        List<ContentValues> values = (List<ContentValues>) params.get(0);
        if (values == null || values.size() == 0) {
            return;
        }
        List<String> selectionList = new ArrayList<>();
        List<ContentValues> valuesList = new ArrayList<>();
        for (ContentValues value : values) {
            if (value == null) {
                continue;
            }
            if (value.containsKey(BUBBLE.ID)) {
                int id = value.getAsInteger(BUBBLE.ID);
                if (id > 0) {
                    String where = BUBBLE.ID + "=" + id;
                    valuesList.add(value);
                    selectionList.add(where);
                }
                value.remove(BUBBLE.ID);
            }
        }
        BubbleDB.updateBubbles(valuesList, selectionList);
        if (needSync) {
            SyncManager.requestSync();
        }
    }

    private static void handleTASK_UPDATE_BUBBLE_TO_NOT_SHARED(List params, boolean needSync) {
        if (params == null || params.size() == 0) {
            return;
        }
        List<Integer> ids = (List<Integer>) params.get(0);
        if (ids == null || ids.size() == 0) {
            return;
        }
        for (Integer id : ids) {
            if (id == null || id < 0) {
                continue;
            }
            BubbleDB.clearBubbleSyncInfo(BUBBLE.ID + "=" + id, ATTACHMENT.BUBBLE_ID + "=" + id, true);
        }
        if (needSync) {
            SyncManager.requestSync();
        }
    }

    private static void handleTASK_CANCEL_SHARE_RELATION(List params) {
        if (params == null || params.size() == 0) {
            return;
        }
        long userId = (Long) params.get(0);
        BubbleDB.clearBubbleOfShareRelation(userId);
        SyncManager.requestSync();
    }

    private static void handleTASK_REMOVE_BUBBLE(List params) {
        if (params == null || params.size() == 0) {
            return;
        }
        List<BubbleItem> items = (List<BubbleItem>) params.get(0);
        if (items == null || items.size() == 0) {
            return;
        }
        List<ContentValues> values = (List<ContentValues>) params.get(1);
        if (values == null || values.size() == 0 || values.size() != items.size()) {
            return;
        }
        List<String> selectionList = new ArrayList<>();
        StringBuffer buffer = new StringBuffer();
        int count = items.size();
        buffer.append(BUBBLE.ID);
        buffer.append(" in (");
        for (int i = 0; i < count; i++) {
            BubbleItem item = items.get(i);
            buffer.append(item.getId());
            if (i != (count - 1)) {
                buffer.append(", ");
            }
            String where = BUBBLE.ID + "=" + item.getId();
            selectionList.add(where);
        }
        buffer.append(")");
        String where = buffer.toString();
        BubbleDB.updateBubbles(values, selectionList);
        //notify
        IdeaPillsApp app = IdeaPillsApp.getInstance();
        List<String> createDates = BubbleDB.listBubbleCreateDate(where);
        AlarmUtils.deleteAlarmFromCalendar(app, createDates);
        VoiceAssistantInterface.notifyBubbleRemoved(app);
        SyncManager.requestSync();
    }

    private static void handleTASK_UPDATE_URI(List params) {
        if (params == null || params.size() == 0) {
            return;
        }
        ArrayList<GlobalBubble> list = (ArrayList<GlobalBubble>) params.get(0);
        if (list == null || list.size() == 0) {
            return;
        }
        final boolean offlineToOnline = (Boolean) params.get(1);
        //update db
        List<Integer> updateIds = new ArrayList<Integer>();
        List<ContentValues> valueList = new ArrayList<ContentValues>();
        for (GlobalBubble bubble : list) {
            int id = bubble.getId();
            Uri uri = bubble.getUri();
            if (id > 0) {
                updateIds.add(id);
                ContentValues value = new ContentValues();
                value.put(BUBBLE.ID, id);
                if (uri != null) {
                    value.put(BUBBLE.URI, uri.toString());
                }
                if (offlineToOnline) {
                    value.put(BUBBLE.TYPE, bubble.getType());
                    value.put(BUBBLE.TEXT, bubble.getText());
                }
                valueList.add(value);
            }
        }
        int updateCount = BubbleDB.updateBubbleById(valueList);
        if (updateCount <= 0) {
            return;
        }
        //load wave data
        final List<BubbleItem> voiceItems = new ArrayList<BubbleItem>();
        List<BubbleItem> items = BubbleDB.listVisibleBubbles();
        for (BubbleItem item : items) {
            if (item == null) {
                continue;
            }
            if (item.isVoiceBubble()) {
                int id = item.getId();
                if (updateIds.contains(id)) {
                    voiceItems.add(item);
                }
            }
        }
        if (voiceItems.size() == 0) {
            return;
        }

        IdeaPillsApp app = IdeaPillsApp.getInstance();
        Utils.loadWaveData(app, voiceItems);
        UIHandler.post(new Runnable() {
            @Override
            public void run() {
                for (BubbleItem item : voiceItems) {
                    if (item == null) {
                        continue;
                    }
                    int id = item.getId();
                    BubbleItem bubble = GlobalBubbleManager.getInstance().getBubbleItemById(id);
                    if (bubble != null) {
                        bubble.setWaveData(item.getWaveData());
                        bubble.setUri(item.getUri());
                    }
                }
            }
        });
    }

    private static void handleTASK_MARK_DELETE(List params) {
        if (params == null || params.size() == 0) {
            return;
        }
        String where = (String) params.get(0);
        if (where == null) {
            return;
        }

        ContentValues value = new ContentValues();
        value.put(BUBBLE.MARK_DELETE, 1);
        BubbleDB.updateBubble(value, where);
        Map<Integer, String> idMap = BubbleDB.listBubbleIdsAndSyncIds(where);
        List<String> markDelBubbleIds = new ArrayList<String>();
        List<String> delForeverBubbleIds = new ArrayList<String>();
        boolean isSyncEnable = SyncManager.syncEnable(IdeaPillsApp.getInstance());
        for (Map.Entry<Integer, String> entry : idMap.entrySet()) {
            if (TextUtils.isEmpty(entry.getValue()) && !isSyncEnable) {
                delForeverBubbleIds.add(String.valueOf(entry.getKey()));
            } else {
                markDelBubbleIds.add(String.valueOf(entry.getKey()));
            }
        }

        IdeaPillsApp app = IdeaPillsApp.getInstance();
        List<String> createDates = BubbleDB.listBubbleCreateDate(where);
        AlarmUtils.deleteAlarmFromCalendar(app, createDates);

        if (!markDelBubbleIds.isEmpty()) {
            ContentValues attachmentValue = new ContentValues();
            attachmentValue.put(ATTACHMENT.MARK_DELETE, 1);
            attachmentValue.put(ATTACHMENT.BUBBLE_ID, 0);
            BubbleDB.updateAttachement(attachmentValue,
                    ATTACHMENT.inSql(ATTACHMENT.BUBBLE_ID, markDelBubbleIds));
        }
        if (!delForeverBubbleIds.isEmpty()) {
            BubbleDB.deleteBubbleAndAttachment(BUBBLE.inSql(BUBBLE.ID, delForeverBubbleIds),
                    ATTACHMENT.inSql(ATTACHMENT.BUBBLE_ID, delForeverBubbleIds));
        }

        //notify
        VoiceAssistantInterface.notifyBubbleRemoved(app);
        SyncManager.requestSync();
    }

    private static void handleTASK_REMOVE_FOREVER(List params) {
        if (params == null || params.size() == 0) {
            return;
        }
        String where = (String) params.get(0);
        if (where == null) {
            return;
        }

        List<String> delBubbleIds = BubbleDB.listBubbleIds(where);
        List<String> createDates = BubbleDB.listBubbleCreateDate(where);
        if (!delBubbleIds.isEmpty()) {
            BubbleDB.deleteBubbleAndAttachment(BUBBLE.inSql(BUBBLE.ID, delBubbleIds),
                    ATTACHMENT.inSql(ATTACHMENT.BUBBLE_ID, delBubbleIds));
        }
        //notify
        IdeaPillsApp app = IdeaPillsApp.getInstance();
        AlarmUtils.deleteAlarmFromCalendar(app, createDates);

        SyncManager.requestSync();
    }

    public static void handleTASK_CLEAR_SHARE_BUBBLE_DATA() {
        List<String> createDates = BubbleDB.listBubbleCreateDate(BUBBLE.WHERE_SHARE_DATA);
        BubbleDB.clearBubbleShareData();
        //notify
        IdeaPillsApp app = IdeaPillsApp.getInstance();
        AlarmUtils.deleteAlarmFromCalendar(app, createDates);
    }

    private static void handleTASK_SYNC_UPDATE_ATTACHMENT_BY_ID(List params) {
        if (params == null || params.size() == 0) {
            return;
        }
        List<ContentValues> values = (List<ContentValues>) params.get(0);
        if (values == null || values.size() == 0) {
            return;
        }
        for (ContentValues value : values) {
            if (value == null) {
                continue;
            }
            if (value.containsKey(ATTACHMENT.ID)) {
                int id = value.getAsInteger(ATTACHMENT.ID);
                if (id > 0) {
                    String where = ATTACHMENT.ID + "=" + id;
                    BubbleDB.updateAttachement(value, where);
                }
                value.remove(ATTACHMENT.ID);
            }
        }
    }

    private static void handleTASK_ADD_ATTACHMENTS(List params) {
        if (params == null) {
            log.assertIfDebug("");
            return;
        }
        final List<AttachMentItem> items = (List<AttachMentItem>) params.get(0);
        if (items == null || items.size() == 0) {
            return;
        }
        BubbleDB.bulkInsert(items);
        if (params.size() > 1) {
            final Integer bubbleId = (Integer) params.get(1);
            BubbleItem item = GlobalBubbleManager.getInstance().getBubbleItemById(bubbleId);
            if (item != null) {
                UIHandler.post(new Runnable() {
                    public void run() {
                        boolean needSync = GlobalBubbleManager.getInstance().linkAttachMentsToBubble(items, bubbleId);
                        if (needSync) {
                            SyncManager.requestSync();
                        }
                    }
                });
            }
        }
    }

    private static void handleTASK_REMOVE_ATTACHMENTS(List params, boolean needSync) {
        if (params == null || params.size() != 1) {
            log.assertIfDebug("wrong params size");
            return;
        }
        final List<AttachMentItem> items = (List<AttachMentItem>) params.get(0);
        if (items == null || items.size() == 0) {
            log.assertIfDebug("attachmentlist is empty");
            return;
        }
        StringBuilder sb = new StringBuilder();
        for (AttachMentItem item : items) {
            sb.append(item.getId()).append(',');
        }
        if (sb.length() > 0) {
            sb.setLength(sb.length() - 1);
            class AttachmentPart extends BubbleDB.BaseContentValueItem{
                private String mSyncId;
                private String mBubbleSyncId;

                public boolean bindCursor(Cursor cursor) {
                    try {
                        int idIdx              = cursor.getColumnIndex(ATTACHMENT.ID);
                        int syncIdIdx          = cursor.getColumnIndex(ATTACHMENT.SYNC_ID);
                        int bubbleSyncIdIdx    = cursor.getColumnIndex(ATTACHMENT.BUBBLE_SYNC_ID);
                        setId(cursor.getInt(idIdx));
                        setSyncId(cursor.getString(syncIdIdx));
                        setBubbleSyncId(cursor.getString(bubbleSyncIdIdx));
                    } catch (Exception e) {
                        e.printStackTrace();
                        return false;
                    }
                    return true;
                }

                public String getBubbleSyncId() {
                    return mBubbleSyncId;
                }
                public void setBubbleSyncId(String bubbleSyncId) {
                    this.mBubbleSyncId = bubbleSyncId;
                }
                public String getSyncId() {
                    return mSyncId;
                }
                public void setSyncId(String syncId) {
                    mSyncId = syncId;
                }
            }
            List<AttachmentPart> result = new ArrayList<AttachmentPart>();
            BubbleDB.list(ATTACHMENT.NAME, ATTACHMENT.ID+" in ("+sb.toString()+")",
                    new String[]{ATTACHMENT.ID, ATTACHMENT.SYNC_ID, ATTACHMENT.BUBBLE_SYNC_ID}, result, AttachmentPart.class);
            StringBuilder whereToDel = new StringBuilder();
            StringBuilder whereToUpdate = new StringBuilder();
            final HashSet<Integer> remain = new HashSet<Integer>();
            for (AttachmentPart part : result) {
                if (TextUtils.isEmpty(part.getSyncId()) && TextUtils.isEmpty(part.getBubbleSyncId())) {
                    whereToDel.append(part.getId()).append(',');
                } else {
                    whereToUpdate.append(part.getId()).append(',');
                    remain.add(part.getId());
                }
            }
            if (whereToDel.length() > 0) {
                whereToDel.setLength(whereToDel.length() - 1);
                BubbleDB.clearAttachments(ATTACHMENT.ID+" in ("+whereToDel.toString()+")");
            }
            if (whereToUpdate.length() > 0) {
                whereToUpdate.setLength(whereToUpdate.length() - 1);
                ContentValues contentValues = new ContentValues();
                contentValues.put(ATTACHMENT.MARK_DELETE, 1);
                BubbleDB.updateAttachement(contentValues, ATTACHMENT.ID+" in ("+whereToUpdate.toString()+")");
            }
            if (needSync) {
                SyncManager.requestSync();
            }
        }
    }

    private static void handleTASK_UPDATE_VOICE_WAVE_DATA(List params) {
        List<BubbleItem> changedBubbleItems = params;
        if (changedBubbleItems == null || changedBubbleItems.isEmpty()) {
            return;
        }
        ArrayList<GlobalBubble> globalBubbles = new ArrayList<>();
        for (BubbleItem bubbleItem : changedBubbleItems) {
            globalBubbles.add(bubbleItem.getBubble());
        }
        VoiceAssistantInterface.updateVoiceWaveData(IdeaPillsApp.getInstance(), globalBubbles);
    }

    private static void handleTASK_CHECK_OFFLINE() {
        VoiceAssistantInterface.checkOffline(IdeaPillsApp.getInstance());
    }

    private static void handleTASK_MERGE_WAVE_DATA(List params) {
        if (params == null || params.size() == 0) {
            return;
        }
        ArrayList<GlobalBubble> list = (ArrayList<GlobalBubble>) params.get(0);
        if (list == null || list.size() == 0) {
            return;
        }
        List<Integer> updateIds = new ArrayList<Integer>();
        for (GlobalBubble bubble : list) {
            int id = bubble.getId();
            if (id > 0) {
                updateIds.add(id);
            }
        }

        //load wave data
        final List<BubbleItem> voiceItems = new ArrayList<BubbleItem>();
        List<BubbleItem> items = BubbleDB.listVisibleBubbles();
        for (BubbleItem item : items) {
            if (item == null) {
                continue;
            }
            if (item.isVoiceBubble()) {
                int id = item.getId();
                if (updateIds.contains(id)) {
                    BubbleItem bubble = GlobalBubbleManager.getInstance().getBubbleItemById(id);
                    voiceItems.add(bubble);
                }
            }
        }
        if (voiceItems.size() == 0) {
            return;
        }

        IdeaPillsApp app = IdeaPillsApp.getInstance();
        Utils.loadWaveDataAndGenerateVoiceDuration(app, voiceItems);
        UIHandler.post(new Runnable() {
            @Override
            public void run() {
                GlobalBubbleManager.getInstance().notifyUpdate();
            }
        });
    }

    private static void handleTASK_TODO_OVER_CHANGE() {
        VoiceAssistantInterface.notifyBubbleTodoChange(IdeaPillsApp.getInstance());
    }
}
