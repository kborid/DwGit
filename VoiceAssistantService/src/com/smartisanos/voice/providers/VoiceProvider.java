package com.smartisanos.voice.providers;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteCantOpenDatabaseException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.text.TextUtils;

import com.smartisanos.voice.providers.VoiceSettings.GlobleBubbleColumns;
import com.smartisanos.voice.util.LogUtils;

import java.io.File;

public class VoiceProvider extends ContentProvider {
    static final LogUtils log = LogUtils.getInstance(VoiceProvider.class);
    private static final String DATABASE_NAME = "voice.db";
    private static final int DATABASE_VERSION = 1;
    public static final String AUTHORITY = "com.smartisanos.voice";
    public static final Uri CONTENT_URI_CONTACT = Uri.parse("content://" + AUTHORITY + "/voice_contact");
    public static final Uri CONTENT_URI_MUSIC = Uri.parse("content://" + AUTHORITY + "/voice_music");
    public static final Uri CONTENT_URI_APPLICATION = Uri.parse("content://" + AUTHORITY + "/voice_application");
    public static final String TABLE_VOICE_CONTACT = "voice_contact";
    public static final String TABLE_VOICE_MUSIC = "voice_music";
    public static final String TABLE_VOICE_APPLICATION = "voice_application";
    public static final String TABLE_CONTACT_VERSION = "contact_version";
    public static final String TABLE_MUSIC_VERSION = "music_version";
    public static final String TABLE_GLOBLE_BUBBLE = "globleBubble";
    public static final Uri CONTENT_URI_CONTACT_VERSION = Uri.parse("content://" + AUTHORITY + "/contact_version");
    static final String PARAMETER_NOTIFY = "notify";
    private DatabaseHelper mOpenHelper;
    @Override
    public boolean onCreate() {
        final Context context = getContext();
        mOpenHelper = new DatabaseHelper(context);
        return true;
    }

    @Override
    public String getType(Uri uri) {
        return "";
    }

    @Override
    public synchronized Cursor query(Uri uri, String[] projection, String selection,
            String[] selectionArgs, String sortOrder) {
        checkDbExists(getContext());

        SqlArguments args = new SqlArguments(uri, selection, selectionArgs);
        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
        qb.setTables(args.table);

        SQLiteDatabase db = mOpenHelper.getReadableDatabase();

        Cursor result = null;
        try {
            result = qb.query(db, projection, args.where, args.args, null,
                    null, sortOrder);
            result.setNotificationUri(getContext().getContentResolver(), uri);
        } catch (SQLiteCantOpenDatabaseException e) {
            log.e("query SQLiteCantOpenDatabaseException e:" + e);
        }

        return result;
    }

    @Override
    public synchronized Uri insert(Uri uri, ContentValues initialValues) {
        checkDbExists(getContext());

        SqlArguments args = new SqlArguments(uri);
        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        final long rowId = dbInsertAndCheck(mOpenHelper, db, args.table, null,
                initialValues);
        if (rowId <= 0)
            return null;

        uri = ContentUris.withAppendedId(uri, rowId);
        sendNotify(uri);
        return uri;
    }

    @Override
    public synchronized int bulkInsert(Uri uri, ContentValues[] values) {
        SqlArguments args = new SqlArguments(uri);

        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        try {
            db.beginTransaction();
            int numValues = values.length;
            for (int i = 0; i < numValues; i++) {
                if (dbInsertAndCheck(mOpenHelper, db, args.table, null,
                        values[i]) < 0) {
                    return 0;
                }
            }
            db.setTransactionSuccessful();
        } catch(Exception e){
            log.e(" bulk insert fail , the reason is "+e.getMessage());
        } finally {
            db.endTransaction();
        }
        sendNotify(uri);
        return values.length;
    }

    private void sendNotify(Uri uri) {
        String notify = uri.getQueryParameter(PARAMETER_NOTIFY);
        if (notify == null || "true".equals(notify)) {
            getContext().getContentResolver().notifyChange(uri, null);
        }

    }

    private static long dbInsertAndCheck(DatabaseHelper helper,
            SQLiteDatabase db, String table, String nullColumnHack,
            ContentValues values) {
        long result = -1;
        try {
            result = db.insert(table, nullColumnHack, values);
        } catch (SQLiteCantOpenDatabaseException e) {
            log.e("dbInsertAndCheck SQLiteCantOpenDatabaseException e:" + e);
        }
        return result;
    }

    @Override
    public synchronized int delete(Uri uri, String selection, String[] selectionArgs) {
        checkDbExists(getContext());

        SqlArguments args = new SqlArguments(uri, selection, selectionArgs);

        SQLiteDatabase db = mOpenHelper.getWritableDatabase();

        int count = -1;
        try {
            count = db.delete(args.table, args.where, args.args);
        } catch (SQLiteCantOpenDatabaseException e) {
            log.e("delete SQLiteCantOpenDatabaseException e:" + e);
        }

        if (count > 0)
            sendNotify(uri);
        return count;
    }

    @Override
    public synchronized int update(Uri uri, ContentValues values, String selection,
            String[] selectionArgs) {
        checkDbExists(getContext());

        SqlArguments args = new SqlArguments(uri, selection, selectionArgs);

        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        int count = -1;
        try {
            count = db.update(args.table, values, args.where, args.args);
        } catch (SQLiteCantOpenDatabaseException e) {
            log.e("update SQLiteCantOpenDatabaseException e:" + e);
        }

        if (count > 0)
            sendNotify(uri);
        return count;
    }
    private void checkDbExists(Context context) {
        File dbFile = context.getDatabasePath(DATABASE_NAME);
        boolean ret = dbFile.exists();
        if (!ret && mOpenHelper != null) {
        	log.w("db file not exits, recreate it");
            mOpenHelper.getWritableDatabase();
        }
    }

    private static class DatabaseHelper extends SQLiteOpenHelper {
        Context mContext = null;
        DatabaseHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
            mContext = context;
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            log.d("creating new voice database");
            db.execSQL("CREATE TABLE voice_contact (" + "_id INTEGER PRIMARY KEY,"
                    + "titlePinyin TEXT,"
                    + "name TEXT," + "contactId LONG,"
                    + "photoId LONG," + "number TEXT," + "label TEXT,"
                    + "dataId LONG," + "mimeType TEXT,"
                    + "numberLocationInfo TEXT)");
            db.execSQL("CREATE TABLE voice_application (" + "_id INTEGER PRIMARY KEY," + "titlePinyin TEXT,"
                    + "realName TEXT," + "icon TEXT," + "uri TEXT,"
                    + "applicationType INTEGER," + "name TEXT,"
                    + "appIndex INTEGER)");
            db.execSQL("CREATE TABLE voice_music (" + "_id INTEGER PRIMARY KEY,"
                    + "media_id LONG," + "titlePinyin TEXT,"
                    + "titleName TEXT," + "albumPinyin TEXT," + "albumName TEXT,"
                    + "artistPinyin TEXT," + "artistName TEXT)");
            db.execSQL("CREATE TABLE contact_version (" + "_id INTEGER PRIMARY KEY,"
                    + "contactId LONG," + "version LONG)");
            db.execSQL("CREATE TABLE music_version (" + "_id INTEGER PRIMARY KEY,"
                    + "media_id LONG," + "version LONG)");
        }

		@Override
		public void onUpgrade(SQLiteDatabase arg0, int arg1, int arg2) {
		}
    }

    static class SqlArguments {
        public final String table;
        public final String where;
        public final String[] args;

        SqlArguments(Uri url, String where, String[] args) {
            if (url.getPathSegments().size() == 1) {
                this.table = url.getPathSegments().get(0);
                this.where = where;
                this.args = args;
            } else if (url.getPathSegments().size() != 2) {
                throw new IllegalArgumentException("Invalid URI: " + url);
            } else if (!TextUtils.isEmpty(where)) {
                throw new UnsupportedOperationException(
                        "WHERE clause not supported: " + url);
            } else {
                this.table = url.getPathSegments().get(0);
                if (this.table.equals(TABLE_GLOBLE_BUBBLE)) {
                    this.where = GlobleBubbleColumns.BUBBLE_ID + "=" + ContentUris.parseId(url);
                } else {
                    this.where = "_id=" + ContentUris.parseId(url);
                }
                this.args = null;
            }
        }

        SqlArguments(Uri url) {
            if (url.getPathSegments().size() == 1) {
                table = url.getPathSegments().get(0);
                where = null;
                args = null;
            } else {
                throw new IllegalArgumentException("Invalid URI: " + url);
            }
        }
    }
}
