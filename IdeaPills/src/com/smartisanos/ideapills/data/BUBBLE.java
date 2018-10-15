package com.smartisanos.ideapills.data;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.service.onestep.GlobalBubble;

import com.smartisanos.ideapills.entity.BubbleItem;
import com.smartisanos.ideapills.common.data.BubbleColumn;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BUBBLE extends Table {

    public static final String NAME = "bubble";

    public static final String ID                = BubbleColumn._ID;
    public static final String TYPE              = BubbleColumn.TYPE;
    public static final String COLOR             = BubbleColumn.COLOR;
    public static final String TODO_TYPE         = BubbleColumn.TODO_TYPE;
    public static final String STATUS            = BubbleColumn.STATUS;
    public static final String URI               = BubbleColumn.URI;
    public static final String TEXT              = BubbleColumn.TEXT;
    public static final String TIME_STAMP        = BubbleColumn.TIME_STAMP;
    public static final String SAMPLE_RATE       = BubbleColumn.SAMPLE_RATE;
    public static final String MODIFIED          = BubbleColumn.MODIFIED;
    public static final String WEIGHT            = BubbleColumn.WEIGHT;
    public static final String VOICE_DURATION    = BubbleColumn.VOICE_DURATION;
    public static final String REMOVED_TIME      = BubbleColumn.REMOVED_TIME;
    public static final String USED_TIME         = BubbleColumn.USED_TIME;
    public static final String RECEIVER          = BubbleColumn.RECEIVER;
    public static final String SYNC_ID           = BubbleColumn.SYNC_ID;
    @Deprecated
    public static final String REQUEST_SYNC_TIME = BubbleColumn.REQUEST_SYNC_TIME;
    @Deprecated
    public static final String VOICE_RES_ID      = BubbleColumn.VOICE_RES_ID;
    public static final String MODIFY_FLAG       = BubbleColumn.MODIFY_FLAG;
    public static final String MARK_DELETE       = BubbleColumn.MARK_DELETE;
    public static final String REMIND_TIME       = BubbleColumn.REMIND_TIME;
    public static final String DUE_DATE          = BubbleColumn.DUE_DATE;
    public static final String CREATE_AT         = BubbleColumn.CREATE_AT;
    public static final String SECONDARY_MODIFY_FLAG = BubbleColumn.SECONDARY_MODIFY_FLAG;
    public static final String VERSION           = BubbleColumn.VERSION;
    public static final String VOICE_SYNC_ID     = BubbleColumn.VOICE_SYNC_ID;
    public static final String VOICE_VERSION     = BubbleColumn.VOICE_VERSION;
    @Deprecated
    public static final String VOICE_WAVE_SYNC_ID     = BubbleColumn.VOICE_WAVE_SYNC_ID;
    @Deprecated
    public static final String VOICE_WAVE_VERSION     = BubbleColumn.VOICE_WAVE_VERSION;
    @Deprecated
    public static final String TODO_OVER_TIME         = BubbleColumn.TODO_OVER_TIME;
    public static final String VOICE_SYNC_ENCRYPT_KEY = BubbleColumn.VOICE_SYNC_ENCRYPT_KEY;
    public static final String USER_ID = BubbleColumn.USER_ID;
    public static final String LAST_CLOUD_TEXT = BubbleColumn.LAST_CLOUD_TEXT;
    public static final String CONFLICT_SYNC_ID = BubbleColumn.CONFLICT_SYNC_ID;
    public static final String VOICE_BUBBLE_SYNC_ID = BubbleColumn.VOICE_BUBBLE_SYNC_ID;
    public static final String LEGACY_USED_TIME = BubbleColumn.LEGACY_USED_TIME;
    public static final String SHARE_STATUS = BubbleColumn.SHARE_STATUS;
    public static final String IS_SHARE_FROM_OTHERS = BubbleColumn.IS_SHARE_FROM_OTHERS;
    public static final String SHARE_PENDING_STATUS = BubbleColumn.SHARE_PENDING_STATUS;
    public static final String SHARE_PENDING_PARTICIPANTS = BubbleColumn.SHARE_PENDING_PARTICIPANTS;

    private static final Map<String, String> columnProps = new HashMap<String, String>();

    static {
        columnProps.put(ID,                "INTEGER PRIMARY KEY");
        columnProps.put(TYPE,              "INTEGER");
        columnProps.put(COLOR,             "INTEGER");
        columnProps.put(TODO_TYPE,         "INTEGER");
        columnProps.put(STATUS,            "INTEGER");
        columnProps.put(URI,               "TEXT");
        columnProps.put(TEXT,              "TEXT");
        columnProps.put(TIME_STAMP,        "INTEGER");
        columnProps.put(SAMPLE_RATE,       "INTEGER");
        columnProps.put(WEIGHT,            "INTEGER");
        columnProps.put(VOICE_DURATION,    "LONG default 0");
        columnProps.put(MODIFIED,          "LONG default 0");
        columnProps.put(REMOVED_TIME,      "LONG default 0");
        columnProps.put(USED_TIME,         "LONG default 0");
        columnProps.put(RECEIVER,          "TEXT");
        columnProps.put(SYNC_ID,           "TEXT default NULL");
        columnProps.put(REQUEST_SYNC_TIME, "LONG default 0");
        columnProps.put(VOICE_RES_ID,      "TEXT default NULL");
        columnProps.put(MODIFY_FLAG,       "INTEGER default 0");
        columnProps.put(MARK_DELETE,       "INTEGER default 0");
        columnProps.put(REMIND_TIME,       "LONG default 0");
        columnProps.put(DUE_DATE,          "LONG default 0");
        columnProps.put(CREATE_AT,       "LONG default 0");
        columnProps.put(SECONDARY_MODIFY_FLAG, "INTEGER default 0");
        columnProps.put(VERSION,           "INTEGER default -1");
        columnProps.put(VOICE_SYNC_ID,     "TEXT default NULL");
        columnProps.put(VOICE_VERSION,     "INTEGER default -1");
        columnProps.put(VOICE_WAVE_SYNC_ID,   "TEXT default NULL");
        columnProps.put(VOICE_WAVE_VERSION,   "INTEGER default -1");
        columnProps.put(TODO_OVER_TIME,       "LONG default 0");
        columnProps.put(VOICE_SYNC_ENCRYPT_KEY, "TEXT");
        columnProps.put(USER_ID, "LONG default -1");
        columnProps.put(LAST_CLOUD_TEXT, "TEXT");
        columnProps.put(CONFLICT_SYNC_ID, "TEXT default NULL");
        columnProps.put(VOICE_BUBBLE_SYNC_ID, "TEXT default NULL");
        columnProps.put(LEGACY_USED_TIME, "LONG default 0");
        columnProps.put(SHARE_STATUS, "INTEGER default 0");
        columnProps.put(IS_SHARE_FROM_OTHERS, "INTEGER default 0");
        columnProps.put(SHARE_PENDING_STATUS, "INTEGER default 0");
        columnProps.put(SHARE_PENDING_PARTICIPANTS, "TEXT");
    }

    @Override
    public String tableName() {
        return NAME;
    }

    @Override
    public String[] getColumns() {
        return BubbleColumn.COLUMNS;
    }

    @Override
    public String createSQL() {
        return generateCreateSQL(NAME, getColumns(), columnProps);
    }

    @Override
    public boolean updateTo(final int oldVersion, final int newVersion, SQLiteDatabase db) {
        if (oldVersion > newVersion) {
            //downgrade
            resetTable(db);
            return false;
        }
        if (oldVersion < newVersion) {
            new TransactionTask(db) {
                @Override
                public void run() {
                    List<String> columnNames = new ArrayList<String>();
                    String queryColumnSql = "select * from " + NAME;
                    Cursor cursor = null;
                    try {
                        cursor = db.rawQuery(queryColumnSql, null);
                        if (cursor != null && cursor.moveToFirst()) {
                            int count = cursor.getColumnCount();
                            for (int i = 0; i < count; i++) {
                                String name = cursor.getColumnName(i);
                                columnNames.add(name);
                                log.error("db update, column name ["+name+"]");
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    } finally {
                        try {
                            if (cursor != null) {
                                cursor.close();
                            }
                        } catch (Exception e) {}
                    }

                    String[] columns = getColumns();
                    for (String column : columns) {
                        if (columnNames.contains(column)) {
                            continue;
                        }
                        try {
                            String sql = Table.addColumnSql(NAME, column, columnProps.get(column));
                            db.execSQL(sql);
                        } catch (Exception e) {}
                    }
                    if (newVersion >= 14 && oldVersion < 14) {
                        updateDbDataTo14(db);
                    }
                    if (newVersion >= 17 && oldVersion < 17) {
                        updateDbDataTo17(db, oldVersion, newVersion);
                    }
                }
            }.execute();
        }
        return false;
    }

    private void updateDbDataTo14(SQLiteDatabase db) {
        try {
            db.execSQL("UPDATE " + NAME + " SET" +
                    CREATE_AT + " = " + TIME_STAMP + " WHERE" + CREATE_AT + "= 0");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void updateDbDataTo17(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 16) {
            try {
                db.execSQL("UPDATE " + NAME + " SET " +
                        LEGACY_USED_TIME + " = " + USED_TIME + "," + USED_TIME + " = 0" +
                        " WHERE " + USED_TIME + " > 0");

                ContentValues cv = new ContentValues();
                cv.put(USED_TIME, System.currentTimeMillis());
                cv.put(TODO_OVER_TIME, System.currentTimeMillis());
                db.update(NAME, cv, BUBBLE.TODO_TYPE + "=" + GlobalBubble.TODO_OVER
                        + " AND " + BUBBLE.USED_TIME + "=0"
                        + " AND " + BUBBLE.LEGACY_USED_TIME + "=0", null);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else if (oldVersion == 16) {
            try {
                db.execSQL("UPDATE " + NAME + " SET " +
                        LEGACY_USED_TIME + " = " + USED_TIME + "," + USED_TIME + " = 0" +
                        " WHERE " + USED_TIME + " > 0 AND " + BUBBLE.TODO_OVER_TIME + " = 0 AND " + BUBBLE.WHERE_SYNC_ID_IS_NULL);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }


    public static final String WHERE_CASE_NOT_DELETE = MARK_DELETE + "=0";
    public static final String WHERE_CASE_DELETE = MARK_DELETE + "=1";

    public static final String WHERE_CASE_VISIBLE = REMOVED_TIME + "=0 AND "
            + LEGACY_USED_TIME + "=0 AND " + WHERE_CASE_NOT_DELETE;
    public static final String WHERE_CASE_USED = USED_TIME + ">0 AND "
            + LEGACY_USED_TIME + "=0 AND " + WHERE_CASE_NOT_DELETE;
    public static final String WHERE_CASE_LEGACY_USED = LEGACY_USED_TIME + ">0 AND " + BUBBLE.REMOVED_TIME
            + "=0 AND " + WHERE_CASE_NOT_DELETE;

    public static final String WHERE_CASE_VOICE_ALL = BUBBLE.TYPE + " in (" + BubbleItem.TYPE_VOICE + ", " + BubbleItem.TYPE_VOICE_OFFLINE + ")";
    public static final String WHERE_CASE_VOICE_OFFLINE = BUBBLE.TYPE + "=" + BubbleItem.TYPE_VOICE_OFFLINE +
            " AND " + BUBBLE.REMOVED_TIME + "=0 AND " + MARK_DELETE + "=0";

    public static final String WHERE_SYNC_ID_IS_NULL = BUBBLE.SYNC_ID + " IS NULL";
    public static final String WHERE_SYNC_ID_NOT_NULL = BUBBLE.SYNC_ID + " IS NOT NULL";
    public static final String WHERE_VOICE_SYNC_ID_IS_NULL = BUBBLE.VOICE_SYNC_ID + " IS NULL";
    public static final String WHERE_VOICE_SYNC_ID_NOT_NULL = BUBBLE.VOICE_SYNC_ID + " IS NOT NULL";
    public static final String WHERE_SYNC_DELETE_COUNT_TEXT_NULL = BUBBLE.TEXT + " =\"\" AND " + BUBBLE.REMOVED_TIME + ">0 AND " + BUBBLE.MARK_DELETE + "=0";
    public static final String WHERE_SYNC_SELECT_ID_TEXT_NULL = "SELECT " + BUBBLE.ID + " FROM " + BUBBLE.NAME + " where " + BUBBLE.TEXT + " =\"\" AND " + BUBBLE.REMOVED_TIME + ">0 AND " + BUBBLE.MARK_DELETE + "=0";

    public static final String WHERE_NOT_LEGACY_USED = LEGACY_USED_TIME + "=0";
    public static final String WHERE_LEGACY_USED = LEGACY_USED_TIME + ">0";

    public static final String WHERE_CLEAR_BUBBLE_TABLE = BUBBLE.MARK_DELETE + "=1 AND " + BUBBLE.WHERE_SYNC_ID_IS_NULL + " AND " +
            BUBBLE.VOICE_SYNC_ID + " IS NULL ";

    public static final String WHERE_SELF_DATA = BUBBLE.IS_SHARE_FROM_OTHERS + " = 0";
    public static final String WHERE_SHARE_DATA = BUBBLE.IS_SHARE_FROM_OTHERS + " = 1";

    public static final String WHERE_PENDING_SHARE = "("
            + BUBBLE.SHARE_PENDING_STATUS + " = " + BubbleItem.SHARE_PENDING_ADD_PARTICIPANTS
            + " OR " + BUBBLE.SHARE_PENDING_STATUS + " = " + BubbleItem.SHARE_PENDING_REMOVE_PARTICIPANTS
            + " OR " + BUBBLE.SHARE_PENDING_STATUS + " = " + BubbleItem.SHARE_PENDING_INVITATION
            + ") AND " + WHERE_CASE_NOT_DELETE;
}