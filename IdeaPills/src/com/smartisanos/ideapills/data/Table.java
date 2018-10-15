package com.smartisanos.ideapills.data;

import android.database.sqlite.SQLiteDatabase;

import com.smartisanos.ideapills.util.LOG;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by lipeng on 15-4-24.
 */
public abstract class Table {
    static final LOG log = LOG.getInstance(Table.class);

    public static Table[] TABLES = new Table[] {
            new BUBBLE(), new ATTACHMENT()
    };

    public abstract String tableName();
    public abstract String[] getColumns();
    public abstract String createSQL();
    public abstract boolean updateTo(int oldVersion, int newVersion, SQLiteDatabase db);

    protected static String generateCreateSQL(String table, String[] columns, Map<String, String> props) {
        StringBuffer buffer = new StringBuffer();
        buffer.append("CREATE TABLE IF NOT EXISTS " + table + " (");
        for (int i = 0; i < columns.length; i++) {
            String column = columns[i];
            String prop = props.get(column);
            buffer.append(column);
            buffer.append(" ");
            buffer.append(prop);
            if (i != (columns.length - 1)) {
                buffer.append(", ");
            }
        }
        buffer.append(");");
        return buffer.toString();
    }

    private static final String DROP_TABLE_SQL_PREFIX = "DROP TABLE IF EXISTS ";

    public static String dropTableSql(String tableName) {
        return DROP_TABLE_SQL_PREFIX + tableName;
    }

    private static final String ALTER_TABLE_SQL_PREFIX = "ALTER TABLE ";

    public static String addColumnSql(String tableName, String columnName, String columnType) {
        if (tableName == null || columnName == null || columnType == null) {
            return null;
        }
        StringBuffer buffer = new StringBuffer();
        buffer.append(ALTER_TABLE_SQL_PREFIX);
        buffer.append(tableName);
        buffer.append(" ADD COLUMN ");
        buffer.append(columnName);
        buffer.append(" ");
        buffer.append(columnType);
        return buffer.toString();
    }

    /**
     * merge data from old table to new table.
     * make sure table name won't change and column name & type is same with old table
     * @param tableName
     * @param columns name of columns
     * @param createSql sql for create table
     */
    public static boolean formatTable(SQLiteDatabase db, final String tableName, final String[] columns, String createSql) {
        boolean success = true;
        db.beginTransaction();
        try {
            formatTableImpl(db, tableName, columns, createSql);
            db.setTransactionSuccessful();
        } catch (Exception e) {
            success = false;
            e.printStackTrace();
        } finally {
            try {
                db.endTransaction();
            } catch (Exception e) {
                success = false;
                e.printStackTrace();
            }
        }
        return success;
    }

    /**
     * merge data from old table to new table.
     * make sure table name won't change and column name & type is same with old table
     * @param tableName
     * @param columns backup data columns
     * @param createSql sql for create table
     */
    private static void formatTableImpl(SQLiteDatabase db, final String tableName, final String[] columns, final String createSql) {
        if (tableName == null) {
            log.error("mergeTable return by tableName is null");
            return;
        }
        if (columns == null || columns.length == 0) {
            log.error("mergeTable return by columns is empty");
            return;
        }
        new TransactionTask(db) {
            @Override
            public void run() {
                // rename old table
                String oldTableName = tableName + "_old";
                String renameTableSql = "ALTER TABLE " + tableName + " RENAME TO " + oldTableName;
                db.execSQL(renameTableSql);
                // create table with format
                db.execSQL(createSql);
                // merge data to new table
                StringBuffer buffer = new StringBuffer();
                for (int i = 0; i < columns.length; i++) {
                    buffer.append(columns[i]);
                    if (i != (columns.length - 1)) {
                        buffer.append(", ");
                    }
                }
                String mergeColumns = buffer.toString();
                String mergeSql = "INSERT INTO " + tableName + " (" + mergeColumns +
                        ") SELECT " + mergeColumns + " FROM " + oldTableName;
                db.execSQL(mergeSql);
                // drop tmp table
                db.execSQL(dropTableSql(oldTableName));
            }
        }.execute();
    }

    public static String inSql(String columnName, int[] ids) {
        if (ids == null || ids.length == 0) {
            return null;
        }
        StringBuffer buffer = new StringBuffer();
        buffer.append(columnName);
        buffer.append(" in (");
        for (int i = 0; i < ids.length; i++) {
            buffer.append(ids[i]);
            if (i != (ids.length - 1)) {
                buffer.append(", ");
            }
        }
        buffer.append(")");
        return buffer.toString();
    }

    public static String inSql(String columnName, long[] ids) {
        if (ids == null || ids.length == 0) {
            return null;
        }
        StringBuffer buffer = new StringBuffer();
        buffer.append(columnName);
        buffer.append(" in (");
        for (int i = 0; i < ids.length; i++) {
            buffer.append(ids[i]);
            if (i != (ids.length - 1)) {
                buffer.append(", ");
            }
        }
        buffer.append(")");
        return buffer.toString();
    }

    public static String inSql(String columnName, List<String> list) {
        if (list == null || list.size() == 0) {
            return null;
        }
        StringBuffer buffer = new StringBuffer();
        buffer.append(columnName);
        buffer.append(" in (");
        int count = list.size();
        for (int i = 0; i < count; i++) {
            buffer.append("'");
            buffer.append(list.get(i));
            buffer.append("'");
            if (i != (count - 1)) {
                buffer.append(", ");
            }
        }
        buffer.append(")");
        return buffer.toString();
    }

    public static String notinSql(String columnName, List<String> list) {
        if (list == null || list.size() == 0) {
            return null;
        }
        StringBuffer buffer = new StringBuffer();
        buffer.append(columnName);
        buffer.append(" not in (");
        int count = list.size();
        for (int i = 0; i < count; i++) {
            buffer.append("'");
            buffer.append(list.get(i));
            buffer.append("'");
            if (i != (count - 1)) {
                buffer.append(", ");
            }
        }
        buffer.append(")");
        return buffer.toString();
    }

    protected void resetTable(SQLiteDatabase db) {
        new TransactionTask(db) {
            @Override
            public void run() {
                String dropSql = Table.dropTableSql(tableName());
                db.execSQL(dropSql);
                String createSql = createSQL();
                db.execSQL(createSql);
            }
        }.execute();
    }
}
