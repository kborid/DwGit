package com.smartisanos.ideapills.data;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.smartisanos.ideapills.entity.AttachMentItem;
import com.smartisanos.ideapills.common.data.AttachmentColumn;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ATTACHMENT extends Table {
    public static final String NAME = "attachment";

    public static final String ID             = AttachmentColumn._ID;
    public static final String MINETYPE       = AttachmentColumn.MINETYPE;
    public static final String URI            = AttachmentColumn.URI;
    public static final String FILENAME       = AttachmentColumn.FILENAME;
    public static final String TIME_STAMP     = AttachmentColumn.TIME_STAMP;
    public static final String STATUS         = AttachmentColumn.STATUS;
    public static final String MARK_DELETE    = AttachmentColumn.MARK_DELETE;
    public static final String SYNC_ID        = AttachmentColumn.SYNC_ID;
    public static final String BUBBLE_ID      = AttachmentColumn.BUBBLE_ID;
    public static final String ORIGINALURI    = AttachmentColumn.ORIGINALURI;
    public static final String CREATE_AT      = AttachmentColumn.CREATE_AT;
    public static final String SIZE           = AttachmentColumn.SIZE;
    public static final String DOWNLOAD_STATUS = AttachmentColumn.DOWNLOAD_STATUS;
    public static final String UPLOAD_STATUS   = AttachmentColumn.UPLOAD_STATUS;
    public static final String VERSION         = AttachmentColumn.VERSION;
    public static final String SYNC_ENCRYPT_KEY = AttachmentColumn.SYNC_ENCRYPT_KEY;
    public static final String USER_ID = AttachmentColumn.USER_ID;
    public static final String BUBBLE_SYNC_ID = AttachmentColumn.BUBBLE_SYNC_ID;

    private static final Map<String, String> columnProps = new HashMap<String, String>();

    static {
        columnProps.put(ID,          "INTEGER PRIMARY KEY");
        columnProps.put(MINETYPE,    "TEXT");
        columnProps.put(URI,         "TEXT");
        columnProps.put(FILENAME,    "TEXT");
        columnProps.put(TIME_STAMP,  "LONG default 0");
        columnProps.put(STATUS,      "INTEGER");
        columnProps.put(MARK_DELETE, "INTEGER default 0");
        columnProps.put(SYNC_ID,     "TEXT");
        columnProps.put(BUBBLE_ID,   "INTEGER");
        columnProps.put(ORIGINALURI, "TEXT");
        columnProps.put(CREATE_AT,   "LONG default 0");
        columnProps.put(SIZE,   "LONG default 0");
        columnProps.put(DOWNLOAD_STATUS,   "INTEGER default " + AttachMentItem.DOWNLOAD_STATUS_DOWNLOAD_SUCCESS);
        columnProps.put(UPLOAD_STATUS,   "INTEGER default " + AttachMentItem.UPLOAD_STATUS_NOT_UPLOAD);
        columnProps.put(VERSION,    "INTEGER default -1");
        columnProps.put(SYNC_ENCRYPT_KEY, "TEXT");
        columnProps.put(USER_ID, "LONG default -1");
        columnProps.put(BUBBLE_SYNC_ID, "TEXT");
    }

    @Override
    public String tableName() {
        return NAME;
    }

    @Override
    public String[] getColumns() {
        return AttachmentColumn.COLUMNS;
    }

    @Override
    public String createSQL() {
        return generateCreateSQL(NAME, getColumns(), columnProps);
    }

    @Override
    public boolean updateTo(int oldVersion, int newVersion, SQLiteDatabase db) {
        if (oldVersion > newVersion) {
            //downgrade
            resetTable(db);
            return false;
        }
        if (oldVersion < 6) {
            resetTable(db);
            return false;
        } else if (oldVersion < newVersion) {
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
                }
            }.execute();
        }
        return false;
    }

    public static final String WHERE_SYNC_ID_IS_NULL = ATTACHMENT.SYNC_ID + " IS NULL";
    public static final String WHERE_SYNC_ID_NOT_NULL = ATTACHMENT.SYNC_ID + " IS NOT NULL";
    public static final String WHERE_SYNC_BUBBLE_TEXT_NULL_HAVE_ATTACHMENT_BUBBLE_ID = ATTACHMENT.BUBBLE_ID + " IN ( " + BUBBLE.WHERE_SYNC_SELECT_ID_TEXT_NULL + " )" + " AND " + ATTACHMENT.MARK_DELETE + "=0";

    public static final String WHERE_CLEAR_ATTACHMENT_TABLE = ATTACHMENT.MARK_DELETE + "=1 AND " + ATTACHMENT.WHERE_SYNC_ID_IS_NULL;
}
