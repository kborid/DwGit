package com.smartisanos.voice.expandscreen;

import android.annotation.NonNull;
import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;

// For [Rev One]
// Create a provider that use to save package information,
// include package name, activity name and app name.
public class ExpandVoiceProvider extends ContentProvider {
    private static final String AUTHORITY = "com.smartisanos.voice.ExpandVoiceProvider";
    public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY);
    public static final String APP_TABLE_NAME = "app";
    public static final String APP_COLUMN_PKGNAME = "packageName";
    public static final String APP_COLUMN_ACTIVITYNAME = "activityName";
    public static final String APP_COLUMN_APPNAME = "appName";
    public static final String APP_COLUMN_APPNAMEPY = "appNamePinyin";
    private static final String PARAMETER_NOTIFY = "notify";
    private DatabaseHelper mDatabaseHelper;

    @Override
    public boolean onCreate() {
        mDatabaseHelper = new DatabaseHelper(getContext());
        return true;
    }

    @Override
    public int delete(Uri arg0, String arg1, String[] arg2) {
        SQLiteDatabase db = mDatabaseHelper.getWritableDatabase();
        int count = db.delete(APP_TABLE_NAME, null, null);
        return count;
    }

    @Override
    public String getType(Uri arg0) {
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        SQLiteDatabase db = mDatabaseHelper.getWritableDatabase();
        long rowId = db.insert(APP_TABLE_NAME, APP_COLUMN_PKGNAME, values);
        if (rowId <= 0) {
            return null;
        }

        sendNotify(uri);
        return uri;
    }

    @Override
    public int bulkInsert(@NonNull Uri uri, @NonNull ContentValues[] values) {
        SQLiteDatabase db = mDatabaseHelper.getWritableDatabase();

        int writedRows = 0;
        long id = -1;
        db.beginTransaction();
        try {
            for (ContentValues value : values) {
                id = db.insert(APP_TABLE_NAME, APP_COLUMN_PKGNAME, value);
                if (-1 != id) {
                    writedRows++;
                }
            }

            db.setTransactionSuccessful();
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            db.endTransaction();
        }

        if (0 < writedRows){
            sendNotify(uri);
        }

        return writedRows;
    }

    @Override
    public Cursor query(Uri arg0, String[] arg1, String arg2, String[] arg3,
                        String arg4) {
        SQLiteDatabase db = mDatabaseHelper.getReadableDatabase();
        return db.query(APP_TABLE_NAME, null, arg2, arg3, arg4, null, null);
    }

    @Override
    public int update(Uri uri, ContentValues values, String arg2, String[] arg3) {
        SQLiteDatabase db = mDatabaseHelper.getWritableDatabase();
        int count = -1;
        count = db.update(APP_TABLE_NAME, values, arg2, arg3);

        sendNotify(uri);
        return count;
    }

    private void sendNotify(Uri uri) {
        String notify = uri.getQueryParameter(PARAMETER_NOTIFY);
        if (notify == null || "true".equals(notify)) {
            getContext().getContentResolver().notifyChange(uri, null);
        }
    }

    private class DatabaseHelper extends SQLiteOpenHelper {
        private static final String DATABASE_NAME = "expand_voice.db";
        private static final int DATABASE_VERSION = 1;

        public DatabaseHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL("CREATE TABLE " + APP_TABLE_NAME + " ( " + "_id INTEGER PRIMARY KEY, "
                    + APP_COLUMN_PKGNAME + " TEXT,"
                    + APP_COLUMN_ACTIVITYNAME + " TEXT,"
                    + APP_COLUMN_APPNAME + " TEXT,"
                    + APP_COLUMN_APPNAMEPY + " Text )");
        }

        @Override
        public void onUpgrade(SQLiteDatabase arg0, int arg1, int arg2) {

        }
    }
}
