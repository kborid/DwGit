package com.smartisanos.ideapills.common.data;

import android.net.Uri;
import android.provider.BaseColumns;

public class AttachmentColumn implements BaseColumns {
    public static final Uri CONTENT_URI = Uri.parse("content://" + BubbleColumn.AUTHORITY + "/attachment");
    public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.google.attachment";
    public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.google.attachment";
    public static final String DEFAULT_SORT_ORDER = "create_at asc";
    public static final String MINETYPE       = "minetype";
    public static final String URI            = "uri";
    public static final String FILENAME       = "filename";
    public static final String TIME_STAMP     = "time_stamp";
    public static final String STATUS         = "status";
    public static final String MARK_DELETE    = "mark_delete";
    public static final String SYNC_ID        = "sync_id";
    public static final String BUBBLE_ID      = "bubble_id";
    public static final String ORIGINALURI    = "originalUri";
    public static final String CREATE_AT      = "create_at";
    public static final String SIZE           = "size";
    public static final String DOWNLOAD_STATUS = "download_status";
    public static final String UPLOAD_STATUS   = "upload_status";
    public static final String VERSION         = "version";
    public static final String SYNC_ENCRYPT_KEY = "sync_encrypt_key";
    public static final String USER_ID = "user_id";
    public static final String BUBBLE_SYNC_ID = "bubble_sync_id";

    public static String[] COLUMNS = new String[]{_ID, MINETYPE, URI, FILENAME, TIME_STAMP, STATUS, MARK_DELETE, SYNC_ID,
            BUBBLE_ID, ORIGINALURI, CREATE_AT, SIZE, DOWNLOAD_STATUS, UPLOAD_STATUS, VERSION,
            SYNC_ENCRYPT_KEY, USER_ID, BUBBLE_SYNC_ID};
}
