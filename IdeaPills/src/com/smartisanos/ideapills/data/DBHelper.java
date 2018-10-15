package com.smartisanos.ideapills.data;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;

import com.smartisanos.ideapills.IdeaPillsApp;
import com.smartisanos.ideapills.util.LOG;

public class DBHelper extends SQLiteOpenHelper {
    private static final LOG log = LOG.getInstance(DBHelper.class);

    private static final int DB_VERSION = 19;
    private static final String DB_NAME = "ideapills.db";

    private static volatile DBHelper mOpenHelper;

    public static DBHelper getInstance() {
        if (mOpenHelper == null) {
            synchronized (DBHelper.class) {
                if (mOpenHelper == null) {
                    mOpenHelper = new DBHelper(IdeaPillsApp.getInstance());
                }
            }
        }
        return mOpenHelper;
    }

    private DBHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
        mOpenHelper = this;
    }

    public static void init() {
        getInstance().getWritableDatabase();
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        log.error("DBHelper onCreate !");
        for (Table table : Table.TABLES) {
            if (table == null) {
                continue;
            }
            try {
                String sql = table.createSQL();
                log.error("create sql ["+sql+"]");
                db.execSQL(sql);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        log.error("onUpgrade from ["+oldVersion+"], to ["+newVersion+"]");
        for (Table table : Table.TABLES) {
            if (table == null) {
                continue;
            }
            try {
                table.updateTo(oldVersion, newVersion, db);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        log.error("onDowngrade from ["+oldVersion+"], to ["+newVersion+"]");
        for (Table table : Table.TABLES) {
            if (table == null) {
                continue;
            }
            try {
                table.updateTo(oldVersion, newVersion, db);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public boolean onCreate(Context context) {
        mOpenHelper = new DBHelper(context);
        return true;
    }

    public Cursor query(String tableName, String[] projection, String selection, String[] selectionArgs,
                        String sortOrder) {
        SQLiteDatabase db = mOpenHelper.getReadableDatabase();
        try {
            Cursor c = db.query(tableName, projection, selection, selectionArgs, null, null, sortOrder);
            return c;
        }  catch (SQLiteException e) {
            e.printStackTrace();
            return null;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}