package com.smartisanos.ideapills.data;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.service.onestep.GlobalBubble;
import android.text.TextUtils;
import android.util.Pair;

import com.smartisanos.ideapills.IdeaPillsApp;
import com.smartisanos.ideapills.common.util.MutiTaskHandler;
import com.smartisanos.ideapills.remind.util.AlarmUtils;
import com.smartisanos.ideapills.util.LOG;
import com.smartisanos.ideapills.util.Utils;
import com.smartisanos.ideapills.entity.AttachMentItem;
import com.smartisanos.ideapills.entity.BubbleItem;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class BubbleDB {
    private static final LOG log = LOG.getInstance(BubbleDB.class);

    public interface ContentValueItem {
        String getTableName();
        ContentValues toContentValues();
        boolean bindCursor(Cursor cursor);
        void setId(int id);
        int getId();
    }

    public static class BaseContentValueItem implements ContentValueItem{
        private int mId;

        public void setId(int id) {
            mId = id;
        }

        public int getId() {
            return mId;
        }

        public String getTableName() {
            return null;
        }

        public ContentValues toContentValues() {
            return null;
        }

        public boolean bindCursor(Cursor cursor) {
            return false;
        }
    };

    public static List<BubbleItem> listVisibleBubbles() {
        return listBubble(BUBBLE.WHERE_CASE_VISIBLE);
    }

    public static List<BubbleItem> listById(int[] ids) {
        if (ids == null || ids.length == 0) {
            return null;
        }
        String where = Table.inSql(BUBBLE.ID, ids);
        if (where == null) {
            return null;
        }
        return listBubble(where);
    }

    public static  <T extends ContentValueItem>  void list(String table, String where, String[] columns, List<T> list, Class<T> type) {
        list(table, where, columns, null, list, type);
    }

    private static  <T extends ContentValueItem>  void list(String table, String where, String order, List<T> list, Class<T> type) {
        list(table, where, null, order, list, type);
    }

    private static  <T extends ContentValueItem>  void list(String table, String where, String[] columns, String order, List<T> list, Class<T> type) {
        Cursor cursor = null;
        try {
            cursor = query(table, where, null, columns, order);
            if (cursor.moveToFirst()) {
                do {
                    ContentValueItem item = (ContentValueItem) type.newInstance();
                    if (item.bindCursor(cursor)) {
                        list.add((T) item);
                    }
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    public static List<BubbleItem> listBubble(String where) {
        List<BubbleItem> list = new ArrayList<BubbleItem>();
        list(BUBBLE.NAME, where, BUBBLE.WEIGHT + " DESC", list, BubbleItem.class);
        return list;
    }

    public static List<AttachMentItem> listAttachments(String where) {
        List<AttachMentItem> list = new ArrayList<AttachMentItem>();
        list(ATTACHMENT.NAME, where, null, ATTACHMENT.CREATE_AT + " ASC", list, AttachMentItem.class);
        return list;
    }

    public static List<Uri> listAttachmentPaths(String where, String[] selectionArgs) {
        List<Uri> pathList = new ArrayList<Uri>();
        Cursor cursor = null;
        try {
            SQLiteDatabase db = DBHelper.getInstance().getReadableDatabase();
            String sql = "select " + ATTACHMENT.URI + " from " + ATTACHMENT.NAME;
            if (where != null) {
                sql = sql + " where " + where;
            }
            cursor = db.rawQuery(sql, selectionArgs);
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    String uri = cursor.getString(0);
                    if (!TextUtils.isEmpty(uri)) {
                        try {
                            pathList.add(Uri.parse(uri));
                        } catch (Exception e) {
                            //ignore
                        }
                    }
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return pathList;
    }

    public static String listBubbleText(String where) {
        Cursor cursor = null;
        try {
            SQLiteDatabase db = DBHelper.getInstance().getReadableDatabase();
            String sql = "select " + BUBBLE.TEXT + " from " + BUBBLE.NAME;
            if (where != null) {
                sql = sql + " where " + where;
            }
            cursor = db.rawQuery(sql, null);
            if (cursor != null && cursor.moveToFirst()) {
                return cursor.getString(0);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return null;
    }

    public static List<String> listBubbleIds(String where) {
        List<String> idList = new ArrayList<String>();
        Cursor cursor = null;
        try {
            SQLiteDatabase db = DBHelper.getInstance().getReadableDatabase();
            String sql = "select " + BUBBLE.ID + " from " + BUBBLE.NAME;
            if (where != null) {
                sql = sql + " where " + where;
            }
            cursor = db.rawQuery(sql, null);
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    idList.add(String.valueOf(cursor.getInt(0)));
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return idList;
    }

    public static List<String> listBubbleCreateDate(String where) {
        List<String> createDateList = new ArrayList<String>();
        Cursor cursor = null;
        try {
            SQLiteDatabase db = DBHelper.getInstance().getReadableDatabase();
            String sql = "select " + BUBBLE.CREATE_AT + " from " + BUBBLE.NAME;
            if (where != null) {
                sql = sql + " where " + where;
            }
            cursor = db.rawQuery(sql, null);
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    createDateList.add(String.valueOf(cursor.getLong(0)));
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return createDateList;
    }

    public static long[] listBubbleSyncIdsByWeight() {
        long[] syncIdSortArray = null;
        Cursor cursor = null;
        try {
            SQLiteDatabase db = DBHelper.getInstance().getReadableDatabase();
            String sql = "select " + BUBBLE.SYNC_ID + " from " + BUBBLE.NAME;
            sql = sql + " where " + BUBBLE.WHERE_SYNC_ID_NOT_NULL + " and " + BUBBLE.MARK_DELETE + " = 0"
                    + " order by " + BUBBLE.WEIGHT + " desc";
            cursor = db.rawQuery(sql, null);
            if (cursor != null && cursor.moveToFirst()) {
                syncIdSortArray = new long[cursor.getCount()];
                int i = 0;
                do {
                    syncIdSortArray[i] = Long.parseLong(cursor.getString(0));
                    i++;
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            e.printStackTrace();
            syncIdSortArray = null;
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return syncIdSortArray;
    }

    public static Map<String, Integer> listBubbleSyncIdsAndIds(String where) {
        Map<String, Integer> idMap = new HashMap<>();
        Cursor cursor = null;
        try {
            SQLiteDatabase db = DBHelper.getInstance().getReadableDatabase();
            String sql = "select " + BUBBLE.ID + ", " + BUBBLE.SYNC_ID + " from " + BUBBLE.NAME;
            if (where != null) {
                sql = sql + " where " + where;
            }
            cursor = db.rawQuery(sql, null);
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    idMap.put(cursor.getString(1), cursor.getInt(0));
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return idMap;
    }

    public static Map<Integer, String> listBubbleIdsAndSyncIds(String where) {
        Map<Integer, String> idMap = new HashMap<>();
        Cursor cursor = null;
        try {
            SQLiteDatabase db = DBHelper.getInstance().getReadableDatabase();
            String sql = "select " + BUBBLE.ID + ", " + BUBBLE.SYNC_ID + " from " + BUBBLE.NAME;
            if (where != null) {
                sql = sql + " where " + where;
            }
            cursor = db.rawQuery(sql, null);
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    idMap.put(cursor.getInt(0), cursor.getString(1));
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return idMap;
    }

    public static Map<Integer, Pair<String, Long>> listBubbleIdsAndSyncIdUserIds(String where) {
        Map<Integer, Pair<String, Long>> idMap = new HashMap<>();
        Cursor cursor = null;
        try {
            SQLiteDatabase db = DBHelper.getInstance().getReadableDatabase();
            String sql = "select " + BUBBLE.ID + ", " + BUBBLE.SYNC_ID + ", " + BUBBLE.USER_ID + " from " + BUBBLE.NAME;
            if (where != null) {
                sql = sql + " where " + where;
            }
            cursor = db.rawQuery(sql, null);
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    idMap.put(cursor.getInt(0), new Pair<>(cursor.getString(1), cursor.getLong(2)));
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return idMap;
    }

    private static Cursor query(String table, String where, String[] selectArgs, String[] columns, String order) {
        SQLiteDatabase db = DBHelper.getInstance().getReadableDatabase();
        Cursor cursor = null;
        try {
            cursor = db.query(table, columns, where, selectArgs, null, null, order);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return cursor;
    }

    public static int count(String where) {
        return count(BUBBLE.NAME, where, false, null);
    }

    public static int count(String table, String where, boolean distinct, String distinctcColumn) {
        int count = -1;
        Cursor cursor = null;
        String columnCount = null;
        if (distinct) {
            columnCount = "count(distinct " + distinctcColumn + ")";
        } else {
            columnCount = "count(*)";
        }
        try {
            SQLiteDatabase db = DBHelper.getInstance().getReadableDatabase();
            String sql = "select " + columnCount + " from " + table;
            if (where != null) {
                sql = sql + " where " + where;
            }
            cursor = db.rawQuery(sql, null);
            if (cursor != null && cursor.moveToFirst()) {
                int index = cursor.getColumnIndex(columnCount);
                count = cursor.getInt(index);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return count;
    }

//    public static boolean checkExist(String whereCase) {
//        return count(whereCase) > 0;
//    }
//
//    public static boolean delete(int[] ids) {
//        if (ids == null || ids.length == 0) {
//            return false;
//        }
//        String where = Table.inSql(BUBBLE.ID, ids);
//        if (where == null) {
//            return false;
//        }
//        return deleteBy(where);
//    }

    public static boolean deleteBubbleBy(final String where) {
        log.error("deleteBubbleBy ["+where+"]");
        return deleteBy(BUBBLE.NAME, where);
    }

    static boolean deleteAttachmentBy(final String where) {
        log.error("deleteAttachmentBy ["+where+"]");
        return deleteBy(ATTACHMENT.NAME, where);
    }

    private static boolean deleteBy(final String table, final String where) {
        if (where == null) {
            return false;
        }
        SQLiteDatabase db = DBHelper.getInstance().getWritableDatabase();
        int count = new TransactionTask(db) {
            @Override
            public void run() {
                result.i = db.delete(table, where, null);
            }
        }.execute().i;
        return count > 0;
    }

    public static void insert(final ContentValueItem item) {
        if (item == null) {
            return;
        }
        ContentValues value = item.toContentValues();
        long id = insert(value, item.getTableName());
        if (id > 0) {
            item.setId((int) id);
        }
    }

    private static long insert(final ContentValues value, final String tablename) {
        if (value == null) {
            return -1;
        }
        SQLiteDatabase db = DBHelper.getInstance().getWritableDatabase();
        long id = new TransactionTask(db) {
            @Override
            public void run() {
                result.l = -1;
                result.l = db.insert(tablename, null, value);
            }
        }.execute().l;
        return id;
    }

    public static void bulkInsert(final List<? extends ContentValueItem> items) {
        if (items == null || items.size() == 0) {
            return;
        }
        SQLiteDatabase db = DBHelper.getInstance().getWritableDatabase();
        new TransactionTask(db) {
            @Override
            public void run() {
                int size = items.size();
                for (int i = 0; i < size; i++) {
                    ContentValueItem item = items.get(i);
                    if (item == null) {
                        continue;
                    }
                    ContentValues value = item.toContentValues();
                    if (value == null) {
                        continue;
                    }
                    long id = db.insert(item.getTableName(), null, value);
                    if (id <= 0) {
                        continue;
                    }
                    item.setId((int) id);
                }
            }
        }.execute();
    }

    public static int update(ContentValueItem item) {
        if (item == null) {
            return -1;
        }
        String where = BUBBLE.ID + "=" + item.getId();
        ContentValues values = item.toContentValues();
        return update(values, where, item.getTableName());
    }

    public static void update(final List<? extends ContentValueItem> items) {
        if (items == null || items.size() == 0) {
            return;
        }
        SQLiteDatabase db = DBHelper.getInstance().getWritableDatabase();
        new TransactionTask(db) {
            @Override
            public void run() {
                for (ContentValueItem item : items) {
                    update(item);
                }
            }
        }.execute();
    }

    public static int updateBubble(final ContentValues values, final String selection) {
        return update(values, selection, BUBBLE.NAME);
    }

    public static int updateAttachments(final ContentValues values, final String selection) {
        return update(values, selection, ATTACHMENT.NAME);
    }

    public static void updateBubbles(final List<ContentValues> valuesList, final List<String> selectionList) {
        updateValues(valuesList, selectionList, BUBBLE.NAME);
    }

    public static void updateAttachments(final List<ContentValues> valuesList, final List<String> selectionList) {
        updateValues(valuesList, selectionList, ATTACHMENT.NAME);
    }

    public static void updateValues(final List<ContentValues> valuesList, final List<String> selectionList,
                                    final String tableName) {
        if (valuesList == null || valuesList.size() == 0
                || selectionList == null || selectionList.size() == 0
                || valuesList.size() != selectionList.size()) {
            return;
        }
        SQLiteDatabase db = DBHelper.getInstance().getWritableDatabase();
        new TransactionTask(db) {
            @Override
            public void run() {
                for (int i = 0; i < valuesList.size(); i++) {
                    update(valuesList.get(i), selectionList.get(i), tableName);
                }
            }
        }.execute();
    }

    public static void updateAttachmentColumnToNull(final String column, final String selection) {
        List<String> columns = new ArrayList<>();
        columns.add(column);
        updateColumnToNull(ATTACHMENT.NAME, columns, selection);
    }

    public static void updateAttachmentColumnToNull(final List<String> columns, final String selection) {
        updateColumnToNull(ATTACHMENT.NAME, columns, selection);
    }

    public static void updateBubbleColumnToNull(final String column, final String selection) {
        List<String> columns = new ArrayList<>();
        columns.add(column);
        updateColumnToNull(BUBBLE.NAME, columns, selection);
    }

    public static void updateBubbleColumnToNull(final List<String> columns, final String selection) {
        updateColumnToNull(BUBBLE.NAME, columns, selection);
    }

    public static void updateColumnToNull(final String table, final List<String> columns, final String selection) {
        SQLiteDatabase db = DBHelper.getInstance().getWritableDatabase();
        new TransactionTask(db) {
            @Override
            public void run() {
                updateColumnToNullNoTransaction(db, table, columns, selection);
            }
        }.execute();
    }

    private static void updateColumnToNullNoTransaction(SQLiteDatabase db,
                                                        final String table, final List<String> columns, final String selection) {
        StringBuilder sqlBuilder = new StringBuilder();
        sqlBuilder.append("update ").append(table).append(" set");
        for (String column : columns) {
            sqlBuilder.append(" ").append(column).append(" = NULL,");
        }
        sqlBuilder.deleteCharAt(sqlBuilder.length() - 1);
        if (!TextUtils.isEmpty(selection)) {
            sqlBuilder.append(" where ").append(selection);
        }
        db.execSQL(sqlBuilder.toString());
    }

    public static void deleteBubbleAndAttachment(final String bubbleWhere, final String attachmentWhere) {
        SQLiteDatabase db = DBHelper.getInstance().getWritableDatabase();
        new TransactionTask(db) {
            @Override
            public void run() {
                db.delete(BUBBLE.NAME, bubbleWhere, null);
                clearAttachments(attachmentWhere);
            }
        }.execute();
    }

    public static void clearBubbleShareData() {
        List<String> shareBubbleIds = BubbleDB.listBubbleIds(BUBBLE.WHERE_SHARE_DATA);
        if (!shareBubbleIds.isEmpty()) {
            deleteBubbleAndAttachment(BUBBLE.inSql(BUBBLE.ID, shareBubbleIds),
                    ATTACHMENT.inSql(ATTACHMENT.BUBBLE_ID, shareBubbleIds));
        }
        ContentValues updateValues = new ContentValues();
        updateValues.put(BUBBLE.SHARE_STATUS, GlobalBubble.SHARE_STATUS_NOT_SHARED);
        updateValues.put(BUBBLE.SHARE_PENDING_STATUS, BubbleItem.SHARE_PENDING_NONE);
        updateValues.put(BUBBLE.SHARE_PENDING_PARTICIPANTS, "");
        updateBubble(updateValues, BUBBLE.SHARE_STATUS
                + " = " + GlobalBubble.SHARE_STATUS_ONE_TO_ONE + " OR "
                + BUBBLE.SHARE_STATUS + " = " + GlobalBubble.SHARE_STATUS_MANY_TO_MANY);
    }

    public static void clearEmptyBubble() {
        String filterSql = "select " + ATTACHMENT.BUBBLE_ID + " from " + ATTACHMENT.NAME + " group by " + ATTACHMENT.BUBBLE_ID;
        BubbleDB.deleteBubbleBy("(" + BUBBLE.TEXT + " is null OR " + BUBBLE.TEXT + "='') and " + BUBBLE.ID + " not in (" + filterSql + ")"
                + " and " + BUBBLE.WHERE_SYNC_ID_IS_NULL + " and " + BUBBLE.WHERE_VOICE_SYNC_ID_IS_NULL);
    }

    public static void clearBubbleSyncInfo(final String bubbleWhere, final String attachmentWhere) {
        clearBubbleSyncInfo(bubbleWhere, attachmentWhere, false);
    }

    public static void clearBubbleSyncInfo(final String bubbleWhere) {
        SQLiteDatabase db = DBHelper.getInstance().getWritableDatabase();
        new TransactionTask(db) {
            @Override
            public void run() {
                List<String> bubbleIds = new ArrayList<String>();
                Cursor cursor = null;
                try {
                    String sql = "select " + BUBBLE.ID + " from " + BUBBLE.NAME;
                    sql = sql + " where " + bubbleWhere;
                    cursor = db.rawQuery(sql, null);
                    if (cursor != null && cursor.moveToFirst()) {
                        do {
                            bubbleIds.add(String.valueOf(cursor.getInt(0)));
                        } while (cursor.moveToNext());
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    if (cursor != null) {
                        cursor.close();
                    }
                }
                if (!bubbleIds.isEmpty()) {
                    List<String> updateBubbleColumns = new ArrayList<>();
                    updateBubbleColumns.add(BUBBLE.SYNC_ID);
                    updateBubbleColumns.add(BUBBLE.VOICE_SYNC_ID);
                    updateBubbleColumns.add(BUBBLE.VOICE_SYNC_ENCRYPT_KEY);
                    updateBubbleColumns.add(BUBBLE.VOICE_BUBBLE_SYNC_ID);
                    updateColumnToNullNoTransaction(db, BUBBLE.NAME, updateBubbleColumns, bubbleWhere);
                    ContentValues cv = new ContentValues();
                    cv.put(BUBBLE.USER_ID, -1);
                    cv.put(BUBBLE.VOICE_VERSION, 0);
                    db.update(BUBBLE.NAME, cv, bubbleWhere, null);

                    String attachmentWhere = ATTACHMENT.inSql(ATTACHMENT.BUBBLE_ID, bubbleIds);
                    List<String> updateAttachmentColumns = new ArrayList<>();
                    updateAttachmentColumns.add(ATTACHMENT.SYNC_ID);
                    updateAttachmentColumns.add(ATTACHMENT.BUBBLE_SYNC_ID);
                    updateAttachmentColumns.add(ATTACHMENT.SYNC_ENCRYPT_KEY);
                    updateColumnToNullNoTransaction(db, ATTACHMENT.NAME, updateAttachmentColumns,
                            attachmentWhere);
                    cv = new ContentValues();
                    cv.put(ATTACHMENT.USER_ID, -1);
                    cv.put(ATTACHMENT.VERSION, 0);
                    db.update(ATTACHMENT.NAME, cv, attachmentWhere, null);
                }

                db.delete(BUBBLE.NAME, BUBBLE.WHERE_CLEAR_BUBBLE_TABLE, null);
                clearAttachments(ATTACHMENT.WHERE_CLEAR_ATTACHMENT_TABLE);
            }
        }.execute();
    }

    public static void clearBubbleSyncInfo(final String bubbleWhere, final String attachmentWhere,
                                           final boolean alsoClearShareStatus) {
        SQLiteDatabase db = DBHelper.getInstance().getWritableDatabase();
        new TransactionTask(db) {
            @Override
            public void run() {
                List<String> updateBubbleColumns = new ArrayList<>();
                updateBubbleColumns.add(BUBBLE.SYNC_ID);
                updateBubbleColumns.add(BUBBLE.VOICE_SYNC_ID);
                updateBubbleColumns.add(BUBBLE.VOICE_SYNC_ENCRYPT_KEY);
                updateBubbleColumns.add(BUBBLE.VOICE_BUBBLE_SYNC_ID);
                updateColumnToNullNoTransaction(db, BUBBLE.NAME, updateBubbleColumns, bubbleWhere);
                ContentValues cv = new ContentValues();
                cv.put(BUBBLE.USER_ID, -1);
                cv.put(BUBBLE.VOICE_VERSION, 0);
                if (alsoClearShareStatus) {
                    cv.put(BUBBLE.IS_SHARE_FROM_OTHERS, 0);
                    cv.put(BUBBLE.SHARE_STATUS, GlobalBubble.SHARE_STATUS_NOT_SHARED);
                }
                db.update(BUBBLE.NAME, cv, bubbleWhere, null);

                List<String> updateAttachmentColumns = new ArrayList<>();
                updateAttachmentColumns.add(ATTACHMENT.SYNC_ID);
                updateAttachmentColumns.add(ATTACHMENT.BUBBLE_SYNC_ID);
                updateAttachmentColumns.add(ATTACHMENT.SYNC_ENCRYPT_KEY);
                updateColumnToNullNoTransaction(db, ATTACHMENT.NAME, updateAttachmentColumns, attachmentWhere);
                cv = new ContentValues();
                cv.put(ATTACHMENT.USER_ID, -1);
                cv.put(ATTACHMENT.VERSION, 0);
                db.update(ATTACHMENT.NAME, cv, attachmentWhere, null);
            }
        }.execute();
    }

    public static void clearBubbleOfShareRelation(final long userId) {
        SQLiteDatabase db = DBHelper.getInstance().getWritableDatabase();
        new TransactionTask(db) {
            @Override
            public void run() {
                SQLiteDatabase db = DBHelper.getInstance().getReadableDatabase();
                String sql = "SELECT " + BUBBLE.ID + " FROM " + BUBBLE.NAME + " WHERE "
                        + BUBBLE.WHERE_SHARE_DATA + " AND " + BUBBLE.USER_ID + "=" + userId;
                Cursor cursor = null;
                int bubbleId = -1;
                try {
                    cursor = db.rawQuery(sql, null);
                    if (cursor != null && cursor.moveToFirst()) {
                        bubbleId = cursor.getInt(0);
                    }
                } finally {
                    Utils.closeSilently(cursor);
                }
                if (bubbleId >= 0) {
                    db.delete(BUBBLE.NAME, BUBBLE.ID + "= ?", new String[]{String.valueOf(bubbleId)});
                    db.delete(ATTACHMENT.NAME, ATTACHMENT.BUBBLE_ID + "= ?", new String[]{String.valueOf(bubbleId)});
                }
            }
        }.execute();
    }

    public static void clearAttachments(final String selection) {
        final List<Uri> paths = listAttachmentPaths(selection, null);
        deleteAttachmentBy(selection);
        MutiTaskHandler.post(new Runnable() {
            @Override
            public void run() {
                Set<File> parentPaths = new HashSet<File>();
                for (Uri uri : paths) {
                    if (uri != null) {
                        File file = new File(uri.getPath());
                        try {
                            file.delete();
                            File parentFile = file.getParentFile();
                            if (parentFile != null && parentFile.isDirectory()) {
                                parentPaths.add(parentFile);
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
                for (File parent : parentPaths) {
                    try {
                        String[] childList = parent.list();
                        if (childList.length == 0) {
                            parent.delete();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }

    public static int updateAttachement(final ContentValues values, final String selection) {
        return update(values, selection, ATTACHMENT.NAME);
    }

    private static int update(final ContentValues values, final String selection, final String tablename) {
        SQLiteDatabase db = DBHelper.getInstance().getWritableDatabase();
        int count = new TransactionTask(db) {
            @Override
            public void run() {
                result.i = -1;
                result.i = db.update(tablename, values, selection, null);
            }
        }.execute().i;
        return count;
    }

    public static int updateBubbleById(final List<ContentValues> values) {
        return updateById(values, BUBBLE.NAME);
    }

    private static int updateById(final List<ContentValues> values, final String tablename) {
        SQLiteDatabase db = DBHelper.getInstance().getWritableDatabase();
        int count = new TransactionTask(db) {
            @Override
            public void run() {
                result.i = 0;
                if (values != null && values.size() > 0) {
                    for (ContentValues value : values) {
                        int id = value.getAsInteger(BUBBLE.ID);
                        if (id > 0) {
                            value.remove(BUBBLE.ID);
                            String where = BUBBLE.ID + "=" + id;
                            result.i = db.update(tablename, value, where, null);
                            result.i++;
                        }
                    }
                }

            }
        }.execute().i;
        return count;
    }

    public static boolean deleteAll() {
        log.error("deleteAll");
        SQLiteDatabase db = DBHelper.getInstance().getWritableDatabase();
        int count = new TransactionTask(db) {
            @Override
            public void run() {
                ContentValues value = new ContentValues();
                value.put(BUBBLE.MARK_DELETE, 1);
                result.i = db.update(BUBBLE.NAME, value, null, null);
                ContentValues attachmentValue = new ContentValues();
                attachmentValue.put(ATTACHMENT.MARK_DELETE, 1);
                db.update(ATTACHMENT.NAME, attachmentValue, null, null);
            }
        }.execute().i;
        AlarmUtils.deleteAllAlarmFromCalendar(IdeaPillsApp.getInstance());
        return count > 0;
    }
}