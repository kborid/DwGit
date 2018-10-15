package com.smartisanos.ideapills.common.data;

import android.net.Uri;
import android.provider.BaseColumns;

public class BubbleColumn implements BaseColumns {
    public static final String AUTHORITY = "com.smartisanos.ideapills.provider";
    public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/bubble");
    public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.google.bubble";
    public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.google.bubble";
    public static final String DEFAULT_SORT_ORDER = BubbleColumn.WEIGHT + " DESC";
    public static final String TYPE              = "type";
    public static final String COLOR             = "color";
    public static final String TODO_TYPE         = "todo_type";
    public static final String STATUS            = "status";
    public static final String URI               = "uri";
    public static final String TEXT              = "text";
    public static final String TIME_STAMP        = "time_stamp";
    public static final String SAMPLE_RATE       = "sampling_rate";
    public static final String MODIFIED          = "modified";
    public static final String WEIGHT            = "weight";
    public static final String VOICE_DURATION    = "voice_duration";
    public static final String REMOVED_TIME      = "removed_time";
    public static final String USED_TIME         = "used_time";
    public static final String RECEIVER          = "receiver";
    public static final String SYNC_ID           = "sync_id";
    @Deprecated
    public static final String REQUEST_SYNC_TIME = "request_sync_time";
    @Deprecated
    public static final String VOICE_RES_ID      = "voice_res_id";
    public static final String MODIFY_FLAG       = "modify_flag";
    public static final String MARK_DELETE       = "mark_delete";
    public static final String REMIND_TIME       = "remind_time";
    public static final String DUE_DATE          = "due_date";
    public static final String CREATE_AT         = "create_at";
    public static final String SECONDARY_MODIFY_FLAG = "secondary_modify_flag";
    public static final String VERSION           = "version";
    public static final String VOICE_SYNC_ID     = "voice_sync_id";
    public static final String VOICE_VERSION     = "voice_version";
    @Deprecated
    public static final String VOICE_WAVE_SYNC_ID     = "voice_wave_sync_id";
    @Deprecated
    public static final String VOICE_WAVE_VERSION     = "voice_wave_version";
    @Deprecated
    public static final String TODO_OVER_TIME         = "to_over_time";
    public static final String VOICE_SYNC_ENCRYPT_KEY = "voice_sync_encrypt_key";
    public static final String USER_ID = "user_id";
    public static final String LAST_CLOUD_TEXT = "last_cloud_text";
    public static final String CONFLICT_SYNC_ID = "conflict_sync_id";
    public static final String VOICE_BUBBLE_SYNC_ID = "voice_bubble_sync_id";
    public static final String LEGACY_USED_TIME = "legacy_used_time";
    public static final String SHARE_STATUS = "share_status";
    public static final String IS_SHARE_FROM_OTHERS = "is_share_from_others";
    public static final String SHARE_PENDING_STATUS = "share_pending_status";
    public static final String SHARE_PENDING_PARTICIPANTS = "share_pending_participants";

    public static String[] COLUMNS = new String[]{_ID, TYPE, COLOR, TODO_TYPE, STATUS, URI, TEXT, TIME_STAMP,
            SAMPLE_RATE, WEIGHT, VOICE_DURATION, REMOVED_TIME, MODIFIED,
            USED_TIME, RECEIVER, SYNC_ID, REQUEST_SYNC_TIME, VOICE_RES_ID, MODIFY_FLAG,
            MARK_DELETE, REMIND_TIME, DUE_DATE, CREATE_AT,
            SECONDARY_MODIFY_FLAG, VERSION, VOICE_SYNC_ID, VOICE_VERSION, VOICE_WAVE_SYNC_ID, VOICE_WAVE_VERSION,
            TODO_OVER_TIME, VOICE_SYNC_ENCRYPT_KEY, USER_ID, LAST_CLOUD_TEXT, CONFLICT_SYNC_ID, VOICE_BUBBLE_SYNC_ID,
            LEGACY_USED_TIME, SHARE_STATUS, IS_SHARE_FROM_OTHERS, SHARE_PENDING_STATUS, SHARE_PENDING_PARTICIPANTS};
}
