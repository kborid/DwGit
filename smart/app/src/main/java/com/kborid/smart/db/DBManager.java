package com.kborid.smart.db;

import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;

import com.kborid.smart.PRJApplication;

public class DBManager {
    private DBHelper mDb;

    private DBManager() {
        mDb = new DBHelper(PRJApplication.getInstance());
    }

    private static DBManager mInstance = null;

    public static DBManager getInstance() {
        if (null == mInstance) {
            mInstance = new DBManager();
        }
        return mInstance;
    }

    public void insert(ContentValues contentValues) {
        SQLiteDatabase sqLiteDatabase = mDb.getWritableDatabase();
        sqLiteDatabase.execSQL("insert dd");
        sqLiteDatabase.close();
    }
}
