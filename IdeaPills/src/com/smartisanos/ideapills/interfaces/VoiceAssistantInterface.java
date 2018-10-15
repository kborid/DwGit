package com.smartisanos.ideapills.interfaces;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.service.onestep.GlobalBubble;
import android.service.onestep.GlobalBubbleAttach;
import android.text.TextUtils;

import com.smartisanos.ideapills.IdeaPillsApp;
import com.smartisanos.ideapills.data.ATTACHMENT;
import com.smartisanos.ideapills.data.BUBBLE;
import com.smartisanos.ideapills.data.BubbleDB;
import com.smartisanos.ideapills.data.DataHandler;
import com.smartisanos.ideapills.data.Table;
import com.smartisanos.ideapills.remind.util.AlarmUtils;
import com.smartisanos.ideapills.util.GlobalBubbleManager;
import com.smartisanos.ideapills.util.LOG;
import com.smartisanos.ideapills.util.Utils;
import com.smartisanos.ideapills.entity.AttachMentItem;
import com.smartisanos.ideapills.entity.BubbleItem;
import com.smartisanos.ideapills.common.util.UIHandler;

import java.util.ArrayList;
import java.util.List;

public class VoiceAssistantInterface {
    private static final LOG log = LOG.getInstance(VoiceAssistantInterface.class);

    private static final String METHOD_QUERY_GLOBAL_BUBBLE     = "METHOD_QUERY_GLOBAL_BUBBLE";
    private static final String METHOD_DROP_GLOBAL_BUBBLE      = "METHOD_DROP_GLOBAL_BUBBLE";
    private static final String METHOD_NOTIFY_BUBBLE_REMOVED   = "METHOD_NOTIFY_BUBBLE_REMOVED";
    private static final String METHOD_COUNT_GLOBAL_BUBBLE     = "METHOD_COUNT_GLOBAL_BUBBLE";
    private static final String METHOD_DATA_LOADED             = "METHOD_DATA_LOADED";
    private static final String METHOD_HIDE_SARA               = "METHOD_HIDE_SARA";
    private static final String METHOD_UPDATE_VOICE_WAVE_DATA  = "METHOD_UPDATE_VOICE_WAVE_DATA";
    private static final String METHOD_CHECK_OFFLINE = "METHOD_CHECK_OFFLINE";
    private static final String METHOD_NOTIFY_BUBBLE_TODO_CHANGE = "METHOD_NOTIFY_BUBBLE_TODO_CHANGE";

    private static final String DESTROY_TYPE_REMOVED = "destroy_removed";
    private static final String DESTROY_TYPE_USED    = "destroy_used";
    private static final String DESTROY_TYPE_LEGACY_USED    = "destroy_legacy_used";

    private static final Uri SARA_CALL = Uri.parse("content://com.smartisanos.sara/globalBubble");

    public static void checkOffline(Context context) {
        if (context != null) {
            try {
                ContentResolver resolver = context.getContentResolver();
                Bundle bundle = new Bundle();
                resolver.call(SARA_CALL, METHOD_CHECK_OFFLINE, null, bundle);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static void updateVoiceWaveData(Context context, ArrayList<GlobalBubble> globalBubbles) {
        if (context != null) {
            try {
                ContentResolver resolver = context.getContentResolver();
                Bundle bundle = new Bundle();
                bundle.putParcelableArrayList("bubbles", globalBubbles);
                resolver.call(SARA_CALL, METHOD_UPDATE_VOICE_WAVE_DATA, null, bundle);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static void hideSara(Context context) {
        if (context != null) {
            try {
                ContentResolver resolver = context.getContentResolver();
                resolver.call(SARA_CALL, METHOD_HIDE_SARA, null, null);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static int getRecordCountFromVoiceAssistant(Context context) {
        if (context != null) {
            try {
                ContentResolver resolver = context.getContentResolver();
                Bundle bundle = resolver.call(SARA_CALL, METHOD_COUNT_GLOBAL_BUBBLE, null, null);
                if (bundle != null) {
                    return bundle.getInt("count");
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return -1;
    }

    public static ArrayList<GlobalBubble> loadOldCachedBubbleFromVoiceAssistant(Context context) {
        if (context == null) {
            return null;
        }
        try {
            ContentResolver resolver = context.getContentResolver();
            Bundle bundle = resolver.call(SARA_CALL, METHOD_QUERY_GLOBAL_BUBBLE, null, null);
            if (bundle != null) {
                ArrayList<GlobalBubble> bubbles = bundle.getParcelableArrayList("bubbles");
                return bubbles;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static void clearOldCache(Context context) {
        if (context == null) {
            return;
        }
        try {
            ContentResolver resolver = context.getContentResolver();
            resolver.call(SARA_CALL, METHOD_DROP_GLOBAL_BUBBLE, null, null);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static final String LIST_TYPE_REMOVED   = "removed";
    private static final String LIST_TYPE_OFFLINE   = "offline";
    private static final String LIST_TYPE_USED      = "used";
    private static final String LIST_TYPE_LEGACY_USED = "legacy_used";
    private static final String LIST_TYPE_ALL_VOICE = "all_voice";

    public static ArrayList<GlobalBubble> listBubble(Bundle extras) {
        if (extras == null) {
            return null;
        }
        String type = extras.getString("type");
        String where = null;
        if (LIST_TYPE_USED.equals(type)) {
            where = BUBBLE.WHERE_CASE_USED;
        } else if (LIST_TYPE_LEGACY_USED.equals(type)) {
            where = BUBBLE.WHERE_CASE_LEGACY_USED;
        } else if (LIST_TYPE_OFFLINE.equals(type)) {
            where = BUBBLE.WHERE_CASE_VOICE_OFFLINE;
        } else if (LIST_TYPE_REMOVED.equals(type)) {
            where = BUBBLE.REMOVED_TIME + ">0" + " AND " + BUBBLE.MARK_DELETE + "=0";
            long time = extras.getLong("before", -1);
            if (time > 0) {
                where = where + " AND " + BUBBLE.REMOVED_TIME + "<" + time;
            }
        } else if (LIST_TYPE_ALL_VOICE.equals(type)) {
            where = BUBBLE.WHERE_CASE_VOICE_ALL;
        }
        if (where == null) {
            return null;
        }
        log.info("listBubble by ["+where+"]");
        List<BubbleItem> items = BubbleDB.listBubble(where);
        if (items != null) {
            ArrayList<GlobalBubble> bubbles = new ArrayList<GlobalBubble>();
            for (BubbleItem item : items) {
                bubbles.add(item.getBubble());
            }
            return bubbles;
        }
        return null;
    }

    public static ArrayList<GlobalBubbleAttach> listAttachments(Bundle extras) {
        if (extras == null) {
            return null;
        }
        String type = extras.getString("type");
        int bubbleId = extras.getInt("bubbleId");
        String where = null;
        if (LIST_TYPE_REMOVED.equals(type)) {
            where = ATTACHMENT.BUBBLE_ID + "=" + bubbleId + " AND " + ATTACHMENT.MARK_DELETE + "=0 ";
        }
        ArrayList<GlobalBubbleAttach> attaches = new ArrayList<>();
        List<AttachMentItem> attachMentItems = BubbleDB.listAttachments(where);
        if (attachMentItems != null) {
            for (AttachMentItem item : attachMentItems) {
                GlobalBubbleAttach attach = item.toGlobalBubbleAttach();
                attaches.add(attach);
            }
            return attaches;
        }
        return null;
    }

    public static ArrayList<GlobalBubbleAttach> listAttachmentsCount(Bundle extras) {
        if (extras == null) {
            return null;
        }
        String type = extras.getString("type");
        String bubbleId = extras.getString("bubbleIds");
        String where = null;
        if (LIST_TYPE_REMOVED.equals(type)) {
            where = ATTACHMENT.BUBBLE_ID + " IN " + bubbleId + " AND " + ATTACHMENT.MARK_DELETE + "=0 ";
        }
        ArrayList<GlobalBubbleAttach> attachesCount = new ArrayList<>();
        List<AttachMentItem> attachMentItems = BubbleDB.listAttachments(where);
        if (attachMentItems != null) {
            for (AttachMentItem item : attachMentItems) {
                GlobalBubbleAttach attach = item.toGlobalBubbleAttach();
                attachesCount.add(attach);
            }
            return attachesCount;
        }
        return null;
    }

    public static void restoreDeleteBubble(final Context context, int[] ids){
        if (ids == null || ids.length == 0) {
            return;
        }
        String where = Table.inSql(BUBBLE.ID, ids);
        if (where != null) {
            ContentValues values = new ContentValues();
            values.put(BUBBLE.REMOVED_TIME, 0);
            values.put(BUBBLE.MODIFY_FLAG, BubbleItem.MF_REMOVED_TIME);
            BubbleDB.updateBubble(values, where);
        }
        final List<BubbleItem> bubbles = BubbleDB.listById(ids);
        if (bubbles == null || bubbles.size() == 0) {
            return;
        }
        String whereAttach;
        for (BubbleItem item : bubbles) {
            whereAttach = ATTACHMENT.BUBBLE_ID + "=" + item.getId() + " AND " + ATTACHMENT.MARK_DELETE + "=0 ";
            item.setAttachments(BubbleDB.listAttachments(whereAttach));
        }
        List<BubbleItem> voiceItems = new ArrayList<BubbleItem>();
        final List<BubbleItem> changedAlarmItems = new ArrayList<BubbleItem>();
        for (BubbleItem item : bubbles) {
            if (item.isVoiceBubble()) {
                voiceItems.add(item);
            }
            if (item.getDueDate() > 0) {
                changedAlarmItems.add(item);
            }
        }
        AlarmUtils.replaceAlarmToCalendar(IdeaPillsApp.getInstance(), changedAlarmItems);
        Utils.loadWaveData(context, voiceItems);
        UIHandler.post(new Runnable() {
            @Override
            public void run() {
                GlobalBubbleManager.getInstance().notifyBubbleAdded(bubbles);
                if (!changedAlarmItems.isEmpty()) {
                    AlarmUtils.scheduleNextAlarm(IdeaPillsApp.getInstance(), null);
                }
                //update weight to db
                List params = new ArrayList();
                params.add(bubbles);
                DataHandler.handleTask(DataHandler.TASK_UPDATE_WEIGHT, params);
            }
        });
    }

    public static void restoreBubble(final Context context, int[] ids) {
        if (ids == null || ids.length == 0) {
            return;
        }
        String where = Table.inSql(BUBBLE.ID, ids);
        if (where != null) {
            ContentValues values = new ContentValues();
            values.put(BUBBLE.REMOVED_TIME, 0);
            values.put(BUBBLE.USED_TIME, 0);
            values.put(BUBBLE.LEGACY_USED_TIME, 0);
            values.put(BUBBLE.TODO_TYPE,GlobalBubble.TODO);
            values.put(BUBBLE.MODIFY_FLAG,
                    BubbleItem.MF_USED_TIME | BubbleItem.MF_REMOVED_TIME | BubbleItem.MF_TODO);
            BubbleDB.updateBubble(values, where);
        }
        final List<BubbleItem> bubbles = BubbleDB.listById(ids);
        if (bubbles == null || bubbles.size() == 0) {
            return;
        }
        String whereAttach;
        for (BubbleItem item : bubbles) {
            whereAttach = ATTACHMENT.BUBBLE_ID + "=" + item.getId() + " AND " + ATTACHMENT.MARK_DELETE + "=0 ";
            item.setAttachments(BubbleDB.listAttachments(whereAttach));
        }
        List<BubbleItem> voiceItems = new ArrayList<BubbleItem>();
        for (BubbleItem item : bubbles) {
            if (item.isVoiceBubble()) {
                voiceItems.add(item);
            }
        }
        Utils.loadWaveData(context, voiceItems);
        UIHandler.post(new Runnable() {
            @Override
            public void run() {
                GlobalBubbleManager.getInstance().notifyBubbleAdded(bubbles);
                //update weight to db
                List params = new ArrayList();
                params.add(bubbles);
                DataHandler.handleTask(DataHandler.TASK_UPDATE_WEIGHT, params);
            }
        });
    }

    public static void destroyBubble(int[] ids, String destroyType) {
        if (ids == null || ids.length == 0 || destroyType == null) {
            return;
        }
        String where = Table.inSql(BUBBLE.ID, ids);
        if (DESTROY_TYPE_REMOVED.equals(destroyType)) {
            where = where + " AND " + BUBBLE.REMOVED_TIME + ">0";
        } else if (DESTROY_TYPE_USED.equals(destroyType)) {
            where = where + " AND " + BUBBLE.USED_TIME + ">0";
        } else if (DESTROY_TYPE_LEGACY_USED.equals(destroyType)) {
            where = where + " AND " + BUBBLE.LEGACY_USED_TIME + ">0";
        } else {
            where = null;
        }
        if (where == null) {
            return;
        }
        final List<BubbleItem> bubbles = BubbleDB.listById(ids);
        log.error("destroyBubble by ["+where+"]");
        //mark remove
        UIHandler.post(new Runnable() {
            @Override
            public void run() {
                if (bubbles != null && bubbles.size() != 0) {
                    GlobalBubbleManager.getInstance().notifyBubbleDeleted(bubbles);
                }
            }
        });
        List params = new ArrayList();
        params.add(where);
        DataHandler.handleTask(DataHandler.TASK_MARK_DELETE, params);
    }

    public static void notifyBubbleRemoved(Context context) {
        try {
            ContentResolver resolver = context.getContentResolver();
            resolver.call(SARA_CALL, METHOD_NOTIFY_BUBBLE_REMOVED, null, null);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void notifyBubbleTodoChange(Context context) {
        try {
            ContentResolver resolver = context.getContentResolver();
            resolver.call(SARA_CALL, METHOD_NOTIFY_BUBBLE_TODO_CHANGE, null, null);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static int visibleBubbleCount() {
        return BubbleDB.count(BUBBLE.WHERE_CASE_VISIBLE);
    }

    public static void updateVoiceBubbleUri(final ArrayList<GlobalBubble> bubbles, final boolean offlineToOnline) {
        if (bubbles == null) {
            return;
        }
        UIHandler.post(new Runnable() {
            @Override
            public void run() {
                for (GlobalBubble bubble : bubbles) {
                    int id = bubble.getId();
                    Uri uri = bubble.getUri();
                    if (id <= 0) {
                        continue;
                    }
                    BubbleItem item = GlobalBubbleManager.getInstance().getBubbleItemById(id);
                    if (item != null) {
                        if (uri != null) {
                            item.setUri(uri);
                        }
                        if (uri == null && item.getUri() == null) {
                            item.setType(BubbleItem.TYPE_TEXT);
                        } else if (offlineToOnline
                                && item.getType() == BubbleItem.TYPE_VOICE_OFFLINE) {
                            item.setType(BubbleItem.TYPE_VOICE);
                            if (!TextUtils.isEmpty(bubble.getText())) {
                                item.setText(bubble.getText());
                            }
                        }
                    }
                }
                GlobalBubbleManager.getInstance().notifyUpdate();

                List params = new ArrayList();
                params.add(bubbles);
                params.add(offlineToOnline);
                DataHandler.handleTask(DataHandler.TASK_UPDATE_URI, params);
            }
        });
    }

    public static void mergeVoiceBubbleWave(final ArrayList<GlobalBubble> bubbles) {
        if (bubbles == null) {
            return;
        }
        UIHandler.post(new Runnable() {
            @Override
            public void run() {
                List params = new ArrayList();
                params.add(bubbles);
                DataHandler.handleTask(DataHandler.TASK_MERGE_VOICE_WAVE_DATA, params);
            }
        });
    }
}