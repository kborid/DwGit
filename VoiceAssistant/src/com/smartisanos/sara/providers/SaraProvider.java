package com.smartisanos.sara.providers;

/*
 * Copyright (C) 2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import android.content.ContentProvider;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;
import android.service.onestep.GlobalBubble;

import com.smartisanos.sara.SaraApplication;
import com.smartisanos.sara.providers.SaraSettings.GlobleBubbleColumns;
import com.smartisanos.sara.storage.BubbleDataRepository;
import com.smartisanos.sara.util.LogUtils;
import com.smartisanos.sara.util.SaraConstant;
import com.smartisanos.sara.util.SaraUtils;
import android.content.Intent;

import smartisanos.util.LogTag;

public class SaraProvider extends ContentProvider {
    private static final String TAG = "SaraProvider";
    private static final String DATABASE_NAME = "sara.db";
    private static final int DATABASE_VERSION = 8;
    public static final String AUTHORITY = "com.smartisanos.sara";
    public static final Uri CONTENT_URI_CONTACT = Uri.parse("content://" + AUTHORITY + "/sara_contact");
    public static final Uri CONTENT_URI_MUSIC = Uri.parse("content://" + AUTHORITY + "/sara_music");
    public static final Uri CONTENT_URI_APPLICATION = Uri.parse("content://" + AUTHORITY + "/sara_application");
    public static final Uri CONTENT_URI_GLOBLE_BUBBLE = Uri.parse("content://" + AUTHORITY + "/globleBubble");
    public static final String TABLE_SARA_CONTACT = "sara_contact";
    public static final String TABLE_SARA_MUSIC = "sara_music";
    public static final String TABLE_SARA_APPLICATION = "sara_application";
    public static final String TABLE_CONTACT_VERSION = "contact_version";
    public static final String TABLE_MUSIC_VERSION = "music_version";
    public static final String TABLE_GLOBLE_BUBBLE = "globleBubble";
    public static final Uri CONTENT_URI_CONTACT_VERSION = Uri.parse("content://" + AUTHORITY + "/contact_version");
    static final String PARAMETER_NOTIFY = "notify";
    private DatabaseHelper mOpenHelper;
    private static final String METHOD_DELETE_GLOBAL_BUBBLE = "METHOD_DELETE_GLOBAL_BUBBLE";
    private static final String METHOD_UPDATE_GLOBAL_BUBBLE = "METHOD_UPDATE_GLOBAL_BUBBLE";
    private static final String METHOD_INSERT_GLOBAL_BUBBLE = "METHOD_INSERT_GLOBAL_BUBBLE";
    private static final String METHOD_QUERY_GLOBAL_BUBBLE = "METHOD_QUERY_GLOBAL_BUBBLE";
    private static final String METHOD_DROP_GLOBAL_BUBBLE = "METHOD_DROP_GLOBAL_BUBBLE";
    private static final String METHOD_NOTIFY_BUBBLE_REMOVED = "METHOD_NOTIFY_BUBBLE_REMOVED";
    private static final String METHOD_COUNT_GLOBAL_BUBBLE = "METHOD_COUNT_GLOBAL_BUBBLE";
    private static final String METHOD_UPDATE_VOICE_WAVE_DATA = "METHOD_UPDATE_VOICE_WAVE_DATA";
    private static final String METHOD_CHECK_OFFLINE = "METHOD_CHECK_OFFLINE";
    private static final String METHOD_NOTIFY_BUBBLE_TODO_CHANGE = "METHOD_NOTIFY_BUBBLE_TODO_CHANGE";

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
    public Cursor query(Uri uri, String[] projection, String selection,
            String[] selectionArgs, String sortOrder) {
        checkDbExists(getContext());

        SqlArguments args = new SqlArguments(uri, selection, selectionArgs);
        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
        qb.setTables(args.table);

        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        Cursor result = qb.query(db, projection, args.where, args.args, null,
                null, sortOrder);
        result.setNotificationUri(getContext().getContentResolver(), uri);
        return result;
    }

    @Override
    public Uri insert(Uri uri, ContentValues initialValues) {
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
    public int bulkInsert(Uri uri, ContentValues[] values) {
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
            LogUtils.e(TAG, " bulk insert fail , the reason is "+e.getMessage());
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
        return db.insert(table, nullColumnHack, values);
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        checkDbExists(getContext());

        SqlArguments args = new SqlArguments(uri, selection, selectionArgs);

        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        int count = db.delete(args.table, args.where, args.args);
        if (count > 0)
            sendNotify(uri);
        return count;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection,
            String[] selectionArgs) {
        checkDbExists(getContext());

        SqlArguments args = new SqlArguments(uri, selection, selectionArgs);

        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        int count = db.update(args.table, values, args.where, args.args);
        if (count > 0)
            sendNotify(uri);
        return count;
    }

    @Override
    public Bundle call(String method, String arg, Bundle extras) {
        if (METHOD_DELETE_GLOBAL_BUBBLE.equals(method)) {
            int[] bubbleIdArr = extras.getIntArray(SaraConstant.KEY_BUBBLE_IDS);
            if (bubbleIdArr != null) {
                for(int i = 0; i < bubbleIdArr.length; i++) {
                    ContentValues values = new ContentValues();
                    String where = GlobleBubbleColumns.BUBBLE_ID + " = ? ";
                    String[] selection = new String[]{"" + bubbleIdArr[i]};
                    values.put(GlobleBubbleColumns.BUBBLE_DELETE_TIME, System.currentTimeMillis());
                    update(CONTENT_URI_GLOBLE_BUBBLE, values, where, selection);
                }
            }
            Bundle result = new Bundle();
            result.putBoolean(METHOD_DELETE_GLOBAL_BUBBLE, true);
            return result;
        } else if (METHOD_UPDATE_GLOBAL_BUBBLE.equals(method)) {
            ArrayList<GlobalBubble> bubbles = extras.getParcelableArrayList("bubbles");
            if (bubbles == null) {
                return null;
            }
            for (GlobalBubble bubble : bubbles) {
                if (bubble == null) {
                    continue;
                }
                ContentValues value = new ContentValues();
                value.put(GlobleBubbleColumns.BUBBLE_COLOR, bubble.getColor());
                value.put(GlobleBubbleColumns.BUBBLE_TODO, bubble.getToDo());
                value.put(GlobleBubbleColumns.BUBBLE_TEXT, bubble.getText());
                value.put(GlobleBubbleColumns.BUBBLE_TIMESTAMP, bubble.getTimeStamp());
                update(CONTENT_URI_GLOBLE_BUBBLE, value, GlobleBubbleColumns.BUBBLE_ID + "= ?", new String[] {bubble.getId() + ""});
            }
            Bundle result = new Bundle();
            result.putBoolean(METHOD_UPDATE_GLOBAL_BUBBLE, true);
            return result;
        } else if (METHOD_INSERT_GLOBAL_BUBBLE.equals(method)) {
            ArrayList<GlobalBubble> bubbles = extras.getParcelableArrayList("bubbles");
            if (bubbles == null) {
                return null;
            }
            for (GlobalBubble bubble : bubbles) {
                if (bubble == null) {
                    continue;
                }
                ContentValues value = new ContentValues();
                value.put(GlobleBubbleColumns.BUBBLE_ID, bubble.getId());
                value.put(GlobleBubbleColumns.BUBBLE_TEXT, bubble.getText());
                value.put(GlobleBubbleColumns.BUBBLE_TODO, bubble.getToDo());
                value.put(GlobleBubbleColumns.BUBBLE_COLOR, bubble.getColor());
                value.put(GlobleBubbleColumns.BUBBLE_TIMESTAMP, bubble.getTimeStamp());
                value.put(GlobleBubbleColumns.BUBBLE_TYPE, bubble.getType());
                value.put(GlobleBubbleColumns.BUBBLE_ON_LINE, 1);
                insert(CONTENT_URI_GLOBLE_BUBBLE, value);
            }
            Bundle result = new Bundle();
            result.putBoolean(METHOD_INSERT_GLOBAL_BUBBLE, true);
            return result;
        } else if (METHOD_QUERY_GLOBAL_BUBBLE.equals(method)){
            String[] projection = new String[] {
                    GlobleBubbleColumns.BUBBLE_ID, GlobleBubbleColumns.BUBBLE_TEXT,
                    GlobleBubbleColumns.BUBBLE_TODO, GlobleBubbleColumns.BUBBLE_COLOR,
                    GlobleBubbleColumns.BUBBLE_TIMESTAMP, GlobleBubbleColumns.BUBBLE_TYPE
                    , GlobleBubbleColumns.BUBBLE_DELETE_TIME, GlobleBubbleColumns.BUBBLE_PATH
            };

            Cursor cursor = query(CONTENT_URI_GLOBLE_BUBBLE, projection,
                    GlobleBubbleColumns.BUBBLE_DELETE_TIME + " != ?", new String[] {"0"}, GlobleBubbleColumns.BUBBLE_ID);
            ArrayList<GlobalBubble> bubbles = new ArrayList<GlobalBubble>();
            if (cursor != null && cursor.getCount() > 0) {
                cursor.moveToFirst();
                do {
                    GlobalBubble bubble = new GlobalBubble();
                    bubble.setId((int)cursor.getLong(cursor.getColumnIndex(GlobleBubbleColumns.BUBBLE_ID)));
                    bubble.setText(cursor.getString(cursor.getColumnIndex(GlobleBubbleColumns.BUBBLE_TEXT)));
                    bubble.setToDo(cursor.getInt(cursor.getColumnIndex(GlobleBubbleColumns.BUBBLE_TODO)));
                    bubble.setColor(cursor.getInt(cursor.getColumnIndex(GlobleBubbleColumns.BUBBLE_COLOR)));
                    bubble.setTimeStamp(cursor.getLong(cursor.getColumnIndex(GlobleBubbleColumns.BUBBLE_TIMESTAMP)));
                    bubble.setType(cursor.getInt(cursor.getColumnIndex(GlobleBubbleColumns.BUBBLE_TYPE)));
                    bubble.setRemovedTime(cursor.getLong(cursor.getColumnIndex(GlobleBubbleColumns.BUBBLE_DELETE_TIME)));
                    String path = cursor.getString(cursor.getColumnIndex(GlobleBubbleColumns.BUBBLE_PATH));
                    if (!TextUtils.isEmpty(path)){
                        bubble.setUri(Uri.parse(SaraUtils.formatFilePath2Content(getContext(), path)));
                    }
                    bubble.setSamplineRate(16000);
                    bubbles.add(bubble);
                } while (cursor.moveToNext());
            }
            if (cursor != null) {
                cursor.close();
            }
            Bundle result = new Bundle();
            result.putParcelableArrayList(SaraConstant.KEY_BUBBLES, bubbles);
            result.putBoolean(METHOD_QUERY_GLOBAL_BUBBLE, true);
            return result;
        } else if (METHOD_DROP_GLOBAL_BUBBLE.equals(method)) {
            delete(CONTENT_URI_GLOBLE_BUBBLE, null, null);
            Bundle result = new Bundle();
            result.putBoolean(METHOD_DROP_GLOBAL_BUBBLE, true);
            BubbleDataRepository.updateBubblesPathWithRandomCharacter(getContext());
            return result;
        } else if (METHOD_NOTIFY_BUBBLE_REMOVED.equals(method)){
            Intent intent = new Intent(SaraConstant.ACTION_DELETE_GLOBAL_BUBBLE);
            LocalBroadcastManager.getInstance(getContext()).sendBroadcast(intent);
            Bundle result = new Bundle();
            result.putBoolean(METHOD_NOTIFY_BUBBLE_REMOVED, true);
            return result;
        } else if (METHOD_COUNT_GLOBAL_BUBBLE.equals(method)){
            Cursor cursor = null;
            int count = 0;
            try {
                String sql = "select count(*) from " + TABLE_GLOBLE_BUBBLE;
                SQLiteDatabase db = mOpenHelper.getWritableDatabase();
                cursor = db.rawQuery(sql, null);

                if (cursor != null && cursor.moveToFirst()) {
                    int index = cursor.getColumnIndex("count(*)");
                    count = cursor.getInt(index);
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }
            Bundle result = new Bundle();
            result.putInt(SaraConstant.KEY_BUBBLE_COUNT, count);
            result.putBoolean(METHOD_COUNT_GLOBAL_BUBBLE, true);
            return result;
        } else if (METHOD_UPDATE_VOICE_WAVE_DATA.equals(method)) {
            ArrayList<GlobalBubble> bubbles = extras.getParcelableArrayList("bubbles");
            if (bubbles == null) {
                return null;
            }
            BubbleDataRepository.generateBubbleWaves(getContext().getApplicationContext(), bubbles);
            return null;
        } else if (METHOD_CHECK_OFFLINE.equals(method)) {
            BubbleDataRepository.checkOffLineVoiceInBackground(SaraApplication.getInstance());
            return null;
        } else if (METHOD_NOTIFY_BUBBLE_TODO_CHANGE.equals(method)) {
            Intent intent = new Intent(SaraConstant.ACTION_TODO_CHANGE_GLOBAL_BUBBLE);
            LocalBroadcastManager.getInstance(getContext()).sendBroadcast(intent);
            Bundle result = new Bundle();
            result.putBoolean(METHOD_NOTIFY_BUBBLE_TODO_CHANGE, true);
            return result;
        } else {
            return null;
        }
    }

    private void checkDbExists(Context context) {
        File dbFile = context.getDatabasePath(DATABASE_NAME);
        boolean ret = dbFile.exists();
        if (!ret && mOpenHelper != null) {
            LogTag.w(TAG, "db file not exits, recreate it");
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
            LogUtils.d(TAG, "creating new sara database");
            db.execSQL("CREATE TABLE sara_contact (" + "_id INTEGER PRIMARY KEY,"
                    + "titlePinyin TEXT,"
                    + "name TEXT," + "contactId LONG,"
                    + "photoId LONG," + "number TEXT," + "label TEXT,"
                    + "dataId LONG," + "mimeType TEXT,"
                    + "numberLocationInfo TEXT)");
            db.execSQL("CREATE TABLE sara_application (" + "_id INTEGER PRIMARY KEY," + "titlePinyin TEXT,"
                    + "realName TEXT," + "icon TEXT," + "uri TEXT,"
                    + "applicationType INTEGER," + "name TEXT,"
                    + "appIndex INTEGER)");
            db.execSQL("CREATE TABLE sara_music (" + "_id INTEGER PRIMARY KEY,"
                    + "media_id LONG," + "titlePinyin TEXT,"
                    + "titleName TEXT," + "albumPinyin TEXT," + "albumName TEXT,"
                    + "artistPinyin TEXT," + "artistName TEXT)");
            db.execSQL("CREATE TABLE contact_version (" + "_id INTEGER PRIMARY KEY,"
                    + "contactId LONG," + "version LONG)");
            db.execSQL("CREATE TABLE music_version (" + "_id INTEGER PRIMARY KEY,"
                    + "media_id LONG," + "version LONG)");
            //recycle 1:true,0 false; onLine :1 true,0 false
            db.execSQL("CREATE TABLE globleBubble (" + "_id INTEGER PRIMARY KEY,"
                    + "bubbleId  LONG DEFAULT -1," + "bubbleText TEXT," + "bubblePath TEXT,"
                    + " onLine int," + " deleteTime LONG DEFAULT 0,"
                    + "color INTEGER NOT NULL DEFAULT 4 ," + "todo INTEGER NOT NULL DEFAULT 0,"
                    + "bubbleType INTEGER ," + "timeStamp LONG NOT NULL DEFAULT 0"
                    + ")");
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            LogUtils.d(TAG, "onUpgrade triggered: " + oldVersion);
            int version = oldVersion;
            if(version == 1){
                db.execSQL("DROP TABLE IF EXISTS sara");
                db.execSQL("DROP TABLE IF EXISTS contact_version");
            }
            if (version == 2) {
                   db.execSQL("DROP TABLE IF EXISTS sara_contact");
                   db.execSQL("DROP TABLE IF EXISTS sara_application");
                   db.execSQL("DROP TABLE IF EXISTS sara_music");
                   db.execSQL("DROP TABLE IF EXISTS contact_version");
            }
            if (version < 3 ) {
                onCreate(db);
            }
            if (version <= 4) {
                db.execSQL("CREATE TABLE globleBubble (" + "_id INTEGER PRIMARY KEY,"
                        + "bubbleId  LONG DEFAULT -1," + "bubbleText TEXT," + "bubblePath TEXT,"
                        + " onLine int," + " deleteTime LONG DEFAULT 0" + ")");
            }
            if (version <= 5) {
                db.beginTransaction();
                try {
                    db.execSQL("ALTER TABLE globleBubble " +
                            "ADD COLUMN color INTEGER NOT NULL DEFAULT 4;");
                    db.execSQL("ALTER TABLE globleBubble " +
                            "ADD COLUMN todo INTEGER NOT NULL DEFAULT 0;");
                    db.execSQL("ALTER TABLE globleBubble " +
                            "ADD COLUMN bubbleType INTEGER;");
                    db.setTransactionSuccessful();
                    version = 6;
                } catch (Exception ex) {
                    // Old version remains, which means we wipe old data
                    LogUtils.e(TAG, ex.getMessage());
                } finally {
                    db.endTransaction();
                }
            }
            if (version == 6){
                db.beginTransaction();
                try {
                    db.execSQL("ALTER TABLE globleBubble " +
                            "ADD COLUMN timeStamp LONG NOT NULL DEFAULT 0;");
                    db.setTransactionSuccessful();
                    version = 7;
                } catch (Exception ex) {
                    // Old version remains, which means we wipe old data
                    LogUtils.e(TAG, ex.getMessage());
                } finally {
                    db.endTransaction();
                }
            }
            if (version ==7) {
                try {
                    LogTag.w(TAG, "delete");
                    db.beginTransaction();
                    db.execSQL("DROP TABLE IF EXISTS sara_contact");
                    db.execSQL("DROP TABLE IF EXISTS sara_application");
                    db.execSQL("DROP TABLE IF EXISTS sara_music");
                    db.execSQL("DROP TABLE IF EXISTS contact_version");
                    db.execSQL("DROP TABLE IF EXISTS music_version");
                    db.setTransactionSuccessful();
                    version = 8;
                } catch (Exception ex) {
                    LogUtils.e(TAG, ex.getMessage());
                } finally {
                    db.endTransaction();
                }
            }
        }

        public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            LogUtils.d(TAG, "onDowngrade triggered: " + oldVersion);
        }
    }


    @Override
    public ParcelFileDescriptor openFile(Uri uri, String mode) throws FileNotFoundException {
        ParcelFileDescriptor pfd = null;
        try {
            //check Illegal uri
            String path =  uri.getEncodedPath();
            if(!isSafeUri(path)){
                LogUtils.e(TAG, " uri is not safe uri =" + uri);
                return null;
            }
            String fileStr = getContext().getFilesDir() + path;
            File file = new File(fileStr);
            int modeFlags = ParcelFileDescriptor.MODE_READ_ONLY;
            if ("w".equals(mode)) {
                modeFlags = ParcelFileDescriptor.MODE_WRITE_ONLY;
                if (!file.exists()) {
                    boolean rootExist = SaraUtils.buildWavRootPath(getContext());
                    if (rootExist) {
                        boolean sucess = file.createNewFile();
                        if (!sucess) {
                            LogUtils.e(TAG, "create file fail");
                        }
                    }
                }
            } else if ("r".equals(mode)) {
                if (!file.exists()) {
                    LogUtils.e(TAG, " file : " + file.getPath() + "does not exist!");
                    return null;
                }
            }

            pfd = ParcelFileDescriptor.open(file, modeFlags);
        } catch (Exception ex) {
            LogUtils.e(TAG, " error creating pfd for " + uri.getPath());
        }
        if (pfd == null) {
            throw new FileNotFoundException();
        }
        return pfd;
    }

    private boolean isSafeUri(String path) {
        if(path.contains("../")){
            return false;
        }
        return true;
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
