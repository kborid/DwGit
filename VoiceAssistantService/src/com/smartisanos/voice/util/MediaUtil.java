package com.smartisanos.voice.util;

import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import com.smartisanos.voice.engine.GrammarManager;
import com.smartisanos.voice.providers.VoiceSettings;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PaintFlagsDrawFilter;
import android.graphics.RectF;
import android.net.Uri;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.provider.BaseColumns;
import android.provider.MediaStore;
import android.provider.MediaStore.MediaColumns;
import android.provider.MediaStore.Audio.AudioColumns;
import android.text.TextUtils;
import android.util.LongSparseArray;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import smartisanos.app.voiceassistant.MediaStruct;

public class MediaUtil {
    public static final int DISPLAY_AS_ALBUM = 1;
    public static final int DISPLAY_AS_ARTIST = 2;
    public static final int DISPLAY_AS_TITLE = 3;

    public static final int REPEAT_ALL = 2;
    static final LogUtils log = LogUtils.getInstance(MediaUtil.class);

    public static final String META_CHANGED = "com.smartisanos.music.metachanged";

    public static final String PLAYSTATE_CHANGED = "com.smartisanos.music.playstatechanged";
    public static final String MUSIC_STATE = "playing";
    public static final String MUSIC_ID = "id";
    public static final String ALBUM_ID = "album_id";

    private final static String CLOUD_MUSIC_PATH = Environment.getExternalStorageDirectory()
            + "/smartisan/music/cloud/";

    private static final String AUDIO_CONDITION = "(" + AudioColumns.IS_MUSIC + " = 1 or "
            + AudioColumns.IS_PODCAST + " = 1 )" + " AND (" + AudioColumns.DATA + " LIKE " + "'"
            + CLOUD_MUSIC_PATH + "%' OR " + AudioColumns.SIZE + " > " + 800000 + ") AND "
            + AudioColumns.DATA + " NOT LIKE " + "'%.ogg' AND " + AudioColumns.DATA + " NOT LIKE "
            + "'%.3gp' AND " + AudioColumns.DATA + " NOT LIKE " + "'%.wav' ";

    private static final String MUSIC_PACKAGE_NAME="com.smartisanos.music";

    static ArrayList<String> sLastMusicNameList = new ArrayList<String>();
    static boolean isUpdating =false;
    static int SQL_BATCH_COUNT = 100;

    public static void getAllMediaUriByMusicName(Context context,
                                                 ArrayList<MediaStruct> musicList) {
        ContentResolver cr = context.getContentResolver();
        Cursor c = null;
        String[] projectionArgs = new String[]{BaseColumns._ID,
                AudioColumns.ARTIST, AudioColumns.ALBUM, MediaColumns.TITLE,
                MediaColumns.DATA, MediaStore.Audio.Media.DURATION};
        if (checkAPP(context)) {
            try {
                c = cr.query(Uri.parse(VoiceConstant.SMARTISAN_MUSIC_ALL_URI), projectionArgs, null, null, AudioColumns.TITLE_KEY);
            } catch (Exception e) {
                log.e("getAllMediaUriByMusicName: exception is "+e.getMessage());
                c = cr.query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                        projectionArgs, AUDIO_CONDITION, null, AudioColumns.TITLE_KEY);
            }
        } else {
            c = cr.query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    projectionArgs, AUDIO_CONDITION, null, AudioColumns.TITLE_KEY);
        }
        try {
            if (c != null && c.getCount() > 0) {
                c.moveToFirst();
                do {
                    final String title = c.getString(c
                            .getColumnIndex(MediaStore.Audio.Media.TITLE));
                    if (!TextUtils.isEmpty(title) && title.length() < 64) {
                        final long id = c.getLong(c
                                .getColumnIndex(MediaStore.Audio.Media._ID));
                        final String data = c.getString(c
                                .getColumnIndex(MediaStore.Audio.Media.DATA));
                        final String artist = c.getString(c
                                .getColumnIndex(MediaStore.Audio.Media.ARTIST));
                        final String album = c.getString(c
                                .getColumnIndex(MediaStore.Audio.Media.ALBUM));
                        final long time = c.getLong(c
                                        .getColumnIndex(MediaStore.Audio.Media.DURATION));
                        MediaStruct music = new MediaStruct();
                        if (!TextUtils.isEmpty(data)) {
                            boolean isExistSameTitle = false;
                            for (int i = 0; i < musicList.size(); i++) {
                                MediaStruct musicStruct = musicList.get(i);
                                if (musicStruct.mFlagType == DISPLAY_AS_TITLE
                                        && musicStruct.mTitle.contains(title)
                                    && musicStruct.mAlbum
                                    .equals(album)
                                    && musicStruct.mArtist
                                    .equals(artist)) {
                                    isExistSameTitle = true;
                                    break;
                                }
                            }
                            if (!isExistSameTitle) {
                                log.e("getMediaUriByMusicName  titleCursor "+ data);
                                music.mFlagType = DISPLAY_AS_TITLE;
                                music.mTitle.add(title);
                                music.mAlbum = album;
                                music.mArtist = artist;
                                music.mTime.add(time);
                                music.mPath = data;
                                music.mId.add(id);
                                musicList.add(music);
                            }
                        }
                    }
                } while (c.moveToNext());
            }
        } finally {
            if (c != null) {
                c.close();
            }
        }
    }

    public static void getMediaUriByMusicName(Context context, ArrayList<String> nameList, ArrayList<MediaStruct> musicList) {
        if (nameList.size() == 0) {
            return;
        }
        //we query music data form database with 100 names in one loop
        //we build query condition for title , album , artist in the same loop
        StringBuffer conditionTitleSearch = new StringBuffer();
        StringBuffer conditionAlbumSearch = new StringBuffer();
        StringBuffer conditionArtistSearch = new StringBuffer();
        int listLen = nameList.size();
        // looptime is used for record how many time loops with 100 step has been executed.
        int looptime = 0;
        //flag of whether continue loop
        boolean isLoop = true;
        //searchlist is used for record music name in one loop
        ArrayList<String> SearchList = new ArrayList<String>();
        //album and artist list to save the id of album and artist has query before
        ArrayList<Long> albumIdList = new ArrayList<Long>();
        ArrayList<Long> artistList = new ArrayList<Long>();
        ContentResolver cr = context.getContentResolver();
        while (isLoop) {
            SearchList.clear();
            for (int i = 0; i < SQL_BATCH_COUNT; i++) {
                if (i == 0) {
                    conditionTitleSearch.append(MediaStore.Audio.Media.TITLE + " in ( ?,");
                    conditionAlbumSearch.append(MediaStore.Audio.Media.ALBUM + " in ( ?,");
                    conditionArtistSearch.append(MediaStore.Audio.Media.ARTIST + " in ( ?,");
                } else if (((i + SQL_BATCH_COUNT * looptime) < listLen)) {
                    conditionTitleSearch.append(" ?,");
                    conditionAlbumSearch.append(" ?,");
                    conditionArtistSearch.append(" ?,");
                }
                //if the the position is the last item of one loop or the last item of all the names,
                //finish build the condition string
                if ((i == SQL_BATCH_COUNT - 1)
                        || (i + SQL_BATCH_COUNT * looptime) == listLen - 1) {
                    conditionTitleSearch.deleteCharAt(conditionTitleSearch.length() - 1);
                    conditionTitleSearch.append(" )");
                    conditionAlbumSearch.deleteCharAt(conditionAlbumSearch.length() - 1);
                    conditionAlbumSearch.append(" )");
                    conditionArtistSearch.deleteCharAt(conditionArtistSearch.length() - 1);
                    conditionArtistSearch.append(" )");
                }
                //if the position is out of range, break to avoid list index error
                //and isloop will be set false later to will make sure the loop finish after this time
                if ((i + SQL_BATCH_COUNT * looptime) == listLen) {
                    break;
                }
                SearchList.add(nameList.get(looptime * SQL_BATCH_COUNT + i));
            }
            //build the selection section in SQL query
            String conditionTitleSearchStr = conditionTitleSearch.toString();
            String conditionAlbumSearchStr = conditionAlbumSearch.toString();
            String conditionArtistSearchStr = conditionArtistSearch.toString();
            //build the selectionArgs section in SQL query
            String[] SearchStr = new String[SearchList.size()];
            for (int index = 0; index < SearchList.size(); index++) {
                SearchStr[index] = SearchList.get(index);
            }
            //clear the StringBuffer and add loop time 1 for next loop.
            conditionTitleSearch.delete(0, conditionTitleSearch.length());
            conditionAlbumSearch.delete(0, conditionAlbumSearch.length());
            conditionArtistSearch.delete(0, conditionArtistSearch.length());
            looptime++;

            if (SQL_BATCH_COUNT * looptime >= listLen) {
                isLoop = false;
            }

            Cursor titleCursor = null;
            String[] projectionArgsTitle = new String[] { BaseColumns._ID,MediaStore.Audio.Media.DATA,
               AudioColumns.ARTIST,AudioColumns.ALBUM, MediaColumns.TITLE,MediaStore.Audio.Media.DURATION };
            if (checkAPP(context)) {
                try {
                    titleCursor = cr.query(Uri.parse(VoiceConstant.SMARTISAN_MUSIC_ALL_URI),
                            projectionArgsTitle, conditionTitleSearchStr, SearchStr, null);
                } catch (Exception e) {
                    log.e("getMediaUriByMusicName titleCursor : exception is " + e.getMessage());
                    titleCursor = cr.query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                            projectionArgsTitle, conditionTitleSearchStr, SearchStr, null);
                }
            } else {
                titleCursor = cr.query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                        projectionArgsTitle, conditionTitleSearchStr, SearchStr, null);
            }
            try{
                if (titleCursor != null && titleCursor.getCount() > 0) {
                    titleCursor.moveToFirst();
                    do {
                        final long id = titleCursor.getLong(titleCursor.getColumnIndex(MediaStore.Audio.Media._ID));
                        final String data = titleCursor.getString(titleCursor.getColumnIndex(MediaStore.Audio.Media.DATA));
                        final String artist = titleCursor.getString(titleCursor.getColumnIndex(MediaStore.Audio.Media.ARTIST));
                        final String album = titleCursor.getString(titleCursor.getColumnIndex(MediaStore.Audio.Media.ALBUM));
                        final String title = titleCursor.getString(titleCursor.getColumnIndex(MediaStore.Audio.Media.TITLE));
                        final long time = titleCursor.getLong(titleCursor.getColumnIndex(MediaStore.Audio.Media.DURATION));
                        MediaStruct music = new MediaStruct();
                        if (!TextUtils.isEmpty(data)) {
                            boolean isExistSameTitle = false;
                            for (int i = 0; i < musicList.size(); i++) {
                                MediaStruct musicStruct = musicList.get(i);
                                if (musicStruct.mFlagType == DISPLAY_AS_TITLE
                                        && musicStruct.mTitle.contains(title)
                                        && musicStruct.mAlbum.equals(album)
                                        && musicStruct.mArtist.equals(artist)) {
                                    isExistSameTitle = true;
                                    break;
                                }
                            }
                            if (!isExistSameTitle) {
                                music.mFlagType = DISPLAY_AS_TITLE;
                                music.mTitle.add(title);
                                music.mAlbum = album;
                                music.mArtist = artist;
                                music.mTime.add(time);
                                music.mPath = data;
                                music.mId.add(id);
                                musicList.add(music);
                            }
                        }
                    } while (titleCursor.moveToNext());
                }
            } finally {
                if (titleCursor != null) {
                    titleCursor.close();
                }
            }

            Cursor albumCursor = null;
            String[] projectionArgsAlbum = new String[]{MediaStore.Audio.Media.ALBUM_ID, MediaStore.Audio.Media.DATA,
                   AudioColumns.ARTIST, AudioColumns.ALBUM};
            if (checkAPP(context)) {
                try {
                    albumCursor = cr.query(Uri.parse(VoiceConstant.SMARTISAN_MUSIC_ALL_URI),
                            projectionArgsAlbum, conditionAlbumSearchStr, SearchStr, null);
                } catch (Exception e) {
                    log.e("getMediaUriByMusicName albumCursor : exception is " + e.getMessage());
                    albumCursor = cr.query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                            projectionArgsAlbum, conditionAlbumSearchStr, SearchStr, null);
                }
            } else {
                albumCursor = cr.query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                        projectionArgsAlbum, conditionAlbumSearchStr, SearchStr, null);
            }
            try {
                if (albumCursor != null && albumCursor.getCount() > 0) {
                    albumCursor.moveToFirst();
                    do {
                        final long albumId = albumCursor.getLong(albumCursor.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID));
                        //avoid the get the redundant data from same album
                        if(albumIdList.contains(albumId)){
                            continue;
                        }
                        albumIdList.add(albumId);
                    } while (albumCursor.moveToNext());
                }
            } finally {
                if (albumCursor != null) {
                    albumCursor.close();
                }
            }

            Cursor artistCursor = null;
            String[] projectionArgsArtist = new String[]{MediaStore.Audio.Media.ARTIST_ID, MediaStore.Audio.Media.DATA,
                  MediaStore.Audio.Media.ARTIST, MediaStore.Audio.Media.ALBUM};
            if (checkAPP(context)) {
                try{
                    artistCursor = cr.query(Uri.parse(VoiceConstant.SMARTISAN_MUSIC_ALL_URI),
                        projectionArgsArtist, conditionArtistSearchStr, SearchStr, null);
                } catch(Exception e){
                    log.e("getMediaUriByMusicName artistCursor : exception is "+ e.getMessage());
                    artistCursor = cr.query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, projectionArgsArtist,
                            conditionArtistSearchStr, SearchStr, null);
                }
            } else {
                artistCursor = cr.query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                     projectionArgsArtist, conditionArtistSearchStr, SearchStr, null);
            }
            try{
                if (artistCursor != null && artistCursor.getCount() > 0) {
                    artistCursor.moveToFirst();
                    do {
                        final long artistId = artistCursor.getLong(artistCursor.getColumnIndex(MediaStore.Audio.Media.ARTIST_ID));
                        //avoid the get the redundant data from same artist
                        if(artistList.contains(artistId)){
                            continue;
                        }
                        artistList.add(artistId);
                    }while (artistCursor.moveToNext());
                }
            } finally {
                if (artistCursor != null) {
                    artistCursor.close();
                }
            }
        }
        getAblumOrArtistByMusicName(context,albumIdList,musicList,true);
        getAblumOrArtistByMusicName(context,artistList,musicList,false);
        albumIdList.clear();
        artistList.clear();
        SearchList.clear();
    }

    private static void getAblumOrArtistByMusicName(Context context,ArrayList<Long> idList,
            ArrayList<MediaStruct> musicList, boolean isAlbum) {
        if (idList.size() == 0) {
            return;
        }
        // we query music data form database with 100 names in one loop

        int listLen = idList.size();
        // looptime is used for record how many time loops with 100 step has been executed.
        int looptime = 0;
        // flag of whether continue loop
        boolean isLoop = true;
        // searchlist is used for record music name in one loop
        ArrayList<Long> SearchList = new ArrayList<Long>();
        ContentResolver cr = context.getContentResolver();
        Cursor cursor = null;
        //we use isAlbum flag to build different selection ,selectionArg, sortOrder, Uri
        String sortOrder = null;
        Uri uri = null;
        while (isLoop) {
            SearchList.clear();
            StringBuilder conditionSearch = new StringBuilder();
            for (int i = 0; i < SQL_BATCH_COUNT; i++) {
                if (i == 0) {
                    if (isAlbum) {
                        conditionSearch.append(MediaStore.Audio.Media.ALBUM_ID + " in ( ?,");
                    } else {
                        conditionSearch.append(MediaStore.Audio.Media.ARTIST_ID + " in ( ?,");
                    }
                } else if (((i + SQL_BATCH_COUNT * looptime) < listLen)) {
                    conditionSearch.append(" ?,");
                }
                // if the the position is the last item of one loop or the last item of all the names,
                // finish build the condition string
                if ((i == SQL_BATCH_COUNT - 1)
                        || (i + SQL_BATCH_COUNT * looptime) == listLen - 1) {
                    conditionSearch.deleteCharAt(conditionSearch.length() - 1);
                    conditionSearch.append(" )");
                }
                // if the position is out of range, break to avoid list index error
                // and isloop will be set false later to will make sure the loop finish after this time
                if ((i + SQL_BATCH_COUNT * looptime) == listLen) {
                    break;
                }
                SearchList.add(idList.get(looptime * SQL_BATCH_COUNT + i));
            }
            // build the selection section in SQL query
            String conditionSearchStr = conditionSearch.toString();
            // build the selectionArgs section in SQL query
            String[] SearchStr = new String[SearchList.size()];
            for (int index = 0; index < SearchList.size(); index++) {
                SearchStr[index] = String.valueOf(SearchList.get(index));
            }
            looptime++;

            if (SQL_BATCH_COUNT * looptime >= listLen) {
                isLoop = false;
            }
            //build different sort and uri by isAlbum flag
            if (isAlbum) {
                sortOrder = MediaStore.Audio.Media.ALBUM_ID;
                uri = Uri.parse(VoiceConstant.SMARTISAN_MUSIC_ALBUM_URI);
            } else {
                sortOrder = MediaStore.Audio.Media.ARTIST_ID + ","
                        + MediaStore.Audio.Media.ALBUM_KEY + ","
                        + MediaStore.Audio.Media.TRACK;
                uri = Uri.parse(VoiceConstant.SMARTISAN_MUSIC_ARTIST_URI);
            }

            String[] projectionArgs = new String[] { MediaStore.Audio.Media._ID, MediaStore.Audio.Media.ARTIST_ID,
                    MediaStore.Audio.Media.ALBUM_ID, MediaStore.Audio.Media.DATA, MediaStore.Audio.Media.TITLE,
                    AudioColumns.ARTIST, AudioColumns.ALBUM, MediaStore.Audio.Media.DURATION };
            if (checkAPP(context)) {
                try {
                    cursor = cr.query(uri, projectionArgs, conditionSearchStr, SearchStr, sortOrder);
                } catch (Exception e) {
                    log.e("getMediaUriByMusicName ALBUM_ID : exception is " + e.getMessage());
                    cursor = cr.query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, projectionArgs,
                            conditionSearchStr, SearchStr, sortOrder);
                }
            } else {
                cursor = cr.query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, projectionArgs,
                        conditionSearchStr, SearchStr, sortOrder);
            }
            try {
                if (cursor != null && cursor.getCount() > 0) {
                    cursor.moveToFirst();
                    long lastId = -1;
                    //used for the first search result, and avoid complex error not init variable
                    MediaStruct musicStruct = new MediaStruct();
                    int cursorCount = cursor.getCount();
                    do {
                        final String data = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.DATA));
                        long position = cursor.getPosition();
                        if (!TextUtils.isEmpty(data)) {
                            long id;
                            if (isAlbum) {
                                id = cursor.getLong(cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID));
                            } else {
                                id = cursor.getLong(cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST_ID));
                            }
                            //create MediaStruct and voluation only in the frist loop when ARTIST_ID or ALBUM_ID is changed
                            //we has sort the result from SQL query by ARTIST_ID or ALBUM_ID before
                            if (lastId != id) {
                                //we do not add musicStruct in the first loop because the addlist operate
                                //should only be do when the last item of same ARTIST_ID or ALBUM_ID is visit
                                if (position != 0) {
                                    musicList.add(musicStruct);
                                    musicStruct = new MediaStruct();
                                }
                                lastId = id;
                                if (isAlbum) {
                                    musicStruct.mFlagType = DISPLAY_AS_ALBUM;
                                    musicStruct.mAlbumId = id;
                                } else {
                                    musicStruct.mFlagType = DISPLAY_AS_ARTIST;
                                    musicStruct.mArtistId = id;
                                }
                                musicStruct.mAlbum = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM));
                                musicStruct.mArtist = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST));
                                musicStruct.mPath = data;
                            }
                            //add current music item to same artist or album in each loop
                            final long Id = cursor.getLong(cursor.getColumnIndex(MediaStore.Audio.Media._ID));
                            final String title = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.TITLE));
                            final long time = cursor.getLong(cursor.getColumnIndex(MediaStore.Audio.Media.DURATION));
                            musicStruct.mId.add(Id);
                            musicStruct.mTitle.add(title);
                            musicStruct.mTime.add(time);
                        }
                        //add the last musicStruct to list when the last loop is executed,otherwise the last musicStruct
                        //can not be add to list
                        if (position == cursorCount - 1) {
                            musicList.add(musicStruct);
                        }
                    } while (cursor.moveToNext());
                }
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }
        }
        SearchList.clear();
    }

    public static boolean buildMediaNameList(Context context,
            boolean checkUpdate, boolean isLastNameList) {
        if (context == null) {
            return false;
        }
        ContentResolver cr = context.getContentResolver();
        ArrayList<String> musicNameList = new ArrayList<String>();

        if (checkUpdate && !isUpdating) {
             SharePrefUtil.putBoolean(context, VoiceConstant.KEY_MUSIC, false);
            isUpdating = true;
            ArrayList<Long> delList = new ArrayList<Long>();
            ArrayList<Long> addList = new ArrayList<Long>();
            ArrayList<ContentValues> contentValueList = new ArrayList<ContentValues>();
            ArrayList<ContentValues> versionValueList = getMediaChangeList(context, delList,
                    addList , musicNameList);
            //add added new music data to contentValueList, save it to db later
            if(addList.size() != 0) {
                addMediaToDatabase(addList, context, contentValueList);
            }
            //delete the music data in sara db which has been deleted in media db
            int listLen = delList.size();
            ArrayList<Long> deleteList = new ArrayList<Long>();
            boolean isLoop = listLen != 0;
            int time = 0;
            while(isLoop){
                deleteList.clear();
                StringBuilder condition = new StringBuilder();
                for(int i = 0; i < SQL_BATCH_COUNT ; i++){
                    if(i == 0){
                        condition.append(VoiceSettings.MusicColumns.MEDIA_ID + " in ( ?,");
                    } else if( ((i + SQL_BATCH_COUNT * time) < listLen) && i < SQL_BATCH_COUNT){
                        condition.append(" ?,");
                    }
                    if((i == SQL_BATCH_COUNT - 1) || (i + SQL_BATCH_COUNT * time) == listLen - 1){
                        condition.deleteCharAt(condition.length()-1);
                        condition.append(" )");
                    }
                    if((i + SQL_BATCH_COUNT * time) == listLen){
                        break;
                    }
                    deleteList.add(delList.get(time * SQL_BATCH_COUNT + i));
                }
                String conditionStr = condition.toString();
                //deleteList.add(0, (long)SaraSettings.MusicColumns.ITEM_TYPE_MUSIC);
                String[] deteteIdStr = new String[deleteList.size()];
                for(int index = 0; index < deleteList.size() ; index ++){
                    deteteIdStr[index] = String.valueOf(deleteList.get(index));
                }
                context.getContentResolver()
                            .delete(VoiceSettings.MusicColumns.CONTENT_URI_MUSIC,
                                    conditionStr, deteteIdStr);

                time ++;
                if( SQL_BATCH_COUNT * time >= listLen){
                   isLoop = false;
                }
            }
            //save the new data to sara table of sara db
            if (contentValueList != null && contentValueList.size() > 0) {
                ContentValues[] values = new ContentValues[contentValueList.size()];
                context.getContentResolver().bulkInsert(
                        VoiceSettings.MusicColumns.CONTENT_URI_MUSIC,
                        contentValueList.toArray(values));
                SharePrefUtil.putBoolean(context, VoiceConstant.KEY_MUSIC, true);
            }
            context.getContentResolver().delete(VoiceSettings.MusicColumns.CONTENT_URI_MUSIC_VERSION, null, null);
            if(versionValueList != null && versionValueList.size() != 0) {
                ContentValues[] values = new ContentValues[versionValueList.size()];
                context.getContentResolver().bulkInsert(
                        VoiceSettings.MusicColumns.CONTENT_URI_MUSIC_VERSION, versionValueList.toArray(values));
            }
            log.d("buildMediaNameList update database finish");
            contentValueList.clear();
            versionValueList.clear();

            synchronized (sLastMusicNameList) {
                boolean isNeedUpdate = false;
                if (sLastMusicNameList.size() != musicNameList.size()) {
                    isNeedUpdate = true;
                } else {
                    List<String> tempList = new ArrayList<String>(sLastMusicNameList);
                    for (String nameItem : tempList) {
                        if (musicNameList.indexOf(nameItem) < 0) {
                            isNeedUpdate = true;
                            break;
                        }
                    }
                }
                if (isNeedUpdate) {
                    sLastMusicNameList.clear();
                    sLastMusicNameList.addAll(musicNameList);
                    if (!isLastNameList){
                        VoiceUtils.buildGrammar(GrammarManager.LEXICON_MUSIC);
                    }
                    isUpdating = false;
                    return isNeedUpdate;
                }
            }
        }
        isUpdating = false;
        return false;
    }

    public static ArrayList<ContentValues> getMediaChangeList(Context context,ArrayList<Long> delList ,
        ArrayList<Long> addList , ArrayList<String> nameList ) {
        LongSparseArray<String[]> mapSara = new LongSparseArray<String[]>();
        LongSparseArray<String[]> mapMusic = new LongSparseArray<String[]>();
        ArrayList<ContentValues> contentValueList = new ArrayList<ContentValues>();
        ArrayList<Long> oldList = new ArrayList<Long>();
        ArrayList<Long> newList = new ArrayList<Long>();
        ContentResolver cr = context.getContentResolver();
        Cursor c = null;

        //query the old version form music table in music_version db
        try {
             c = context.getContentResolver()
                    .query(VoiceSettings.MusicColumns.CONTENT_URI_MUSIC,
                            new String[] {VoiceSettings.MusicColumns.MEDIA_ID, VoiceSettings.MusicColumns.TITLE_NAME,VoiceSettings.MusicColumns.ALBUM_NAME,VoiceSettings.MusicColumns.ARTIST_NAME},
                            null, null, null);
        } catch (Exception ex){
            log.e("buildMediaNameList : query error is " + ex.getMessage());
        }
        try {
            if (c != null &&  c.getCount() > 0) {
                c.moveToFirst();
                do {
                    long MusicId = c.getLong(c.getColumnIndex(VoiceSettings.MusicColumns.MEDIA_ID));
                    String name = c.getString(c.getColumnIndex(VoiceSettings.MusicColumns.TITLE_NAME));
                    String album = c.getString(c.getColumnIndex(VoiceSettings.MusicColumns.ALBUM_NAME));
                    String artist = c.getString(c.getColumnIndex(VoiceSettings.MusicColumns.ARTIST_NAME));
                    mapSara.put(MusicId, new String[] { name, album, artist });
                    if (!oldList.contains(MusicId)) {
                        oldList.add(MusicId);
                    }
                } while (c.moveToNext());
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (c != null){
                c.close();
            }
        }
        //query the data in media  db as new data
        String[] projectionArgs = new String[] { BaseColumns._ID,
                AudioColumns.ARTIST, AudioColumns.ALBUM,
                MediaColumns.TITLE, MediaColumns.DATA,
                AudioColumns.ALBUM_ID, AudioColumns.ARTIST_ID};
        if (checkAPP(context)) {
            try {
                c = cr.query(Uri.parse(VoiceConstant.SMARTISAN_MUSIC_ALL_URI),
                        projectionArgs, null, null, null);
            } catch (Exception e) {
                log.e("buildMediaNameList : exception is " + e.getMessage());
                c = cr.query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, projectionArgs, AUDIO_CONDITION, null,
                        AudioColumns.TITLE_KEY);
            }
        } else {
            c = cr.query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, projectionArgs, AUDIO_CONDITION, null,
                    AudioColumns.TITLE_KEY);
        }
        // build MusicName List
        try {
            if (c != null && c.getCount() > 0) {
                c.moveToFirst();
                do {
                    long musicId = c.getLong(c.getColumnIndex(BaseColumns._ID));
                    if (!newList.contains(musicId)) {
                        newList.add(musicId);
                    }
                    String name = c.getString(c.getColumnIndex(MediaStore.Audio.Media.TITLE));
                    if (!TextUtils.isEmpty(name) && name.length() < 64&& !nameList.contains(name)) {
                        nameList.add(name);
                    }
                    String album = c.getString(c.getColumnIndex(MediaStore.Audio.Media.ALBUM));
                    if (!TextUtils.isEmpty(album) && album.length() < 64 && !nameList.contains(album)) {
                        nameList.add(album);
                    }
                    String aritst = c.getString(c.getColumnIndex(MediaStore.Audio.Media.ARTIST));
                    if (!TextUtils.isEmpty(aritst) && aritst.length() < 64 && !nameList.contains(aritst)) {
                        nameList.add(aritst);
                    }
                    mapMusic.put(musicId, new String[] { name, album, aritst });
                } while (c.moveToNext());
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (c != null){
                c.close();
            }
        }

        if (oldList.isEmpty() && newList.isEmpty()) {
            return contentValueList;
        }

        Set<Long> saraSet = new HashSet<Long>(oldList);
        Set<Long> mediaSet = new HashSet<Long>(newList);
        Set<Long> result = new HashSet<Long>();
        oldList.clear();
        newList.clear();
        // since the music data is read-only, we just to check
        // delete or add, no need to check update
        // form sara db data remove all data in meida db ,
        // so we can find which media_ID is deleted
        result.clear();
        result.addAll(saraSet);
        result.removeAll(mediaSet);
        ArrayList<Long> deletedList = new ArrayList(result);
        // form meida db data  remove all data in sara db ,
        // so we can find which media_ID is added
        result.clear();
        result.addAll(mediaSet);
        result.removeAll(saraSet);
        ArrayList<Long> addedList = new ArrayList(result);

        result.clear();
        result.addAll(saraSet);
        result.retainAll(mediaSet);

        for(long contactID :result ){
            String[] saraName = mapSara.get(contactID);
            String[] dbName = mapMusic.get(contactID);
            // if we the version count value is not different,this contact is updated,
            // we should delete it first and add the lastest date later.
            for (int i = 0; i < saraName.length; i++) {
                if (!TextUtils.isEmpty(saraName[i])) {
                    if (!saraName[i].equals(dbName[i])) {
                        deletedList.add(contactID);
                        addedList.add(contactID);
                        break;
                    }
                } else if (!TextUtils.isEmpty(dbName[i])) {
                    deletedList.add(contactID);
                    addedList.add(contactID);
                    break;
                }
            }
        }
        log.e("getMediaChangeList : deletedList is " + deletedList +"  addedList is "+addedList);
        delList.addAll(deletedList);
        addList.addAll(addedList);
        saraSet.clear();
        mediaSet.clear();
        result.clear();
        deletedList.clear();
        addedList.clear();
        return contentValueList;
    }

    private static void addMediaToDatabase(ArrayList<Long> idList,
            Context context, ArrayList<ContentValues> contentValuesList) {
        ContentResolver cr = context.getContentResolver();
        Cursor c = null;
        String[] projectionArgs = new String[] { BaseColumns._ID,
                AudioColumns.ARTIST, AudioColumns.ALBUM,
                MediaColumns.TITLE, MediaColumns.DATA,
                AudioColumns.ALBUM_ID, AudioColumns.ARTIST_ID };

        int listLen = idList.size();
        ArrayList<Long> addList = new ArrayList<Long>();
        boolean isLoop = listLen != 0;
        int time = 0;

        while(isLoop){
            addList.clear();
            StringBuilder condition = new StringBuilder();
            for(int i = 0; i < SQL_BATCH_COUNT ; i++){
                if(i == 0){
                    condition.append(BaseColumns._ID + " in ( ?,");
                } else if( ((i + SQL_BATCH_COUNT * time) < listLen) && i < SQL_BATCH_COUNT){
                    condition.append(" ?,");
                }
                if((i == SQL_BATCH_COUNT - 1) || (i + SQL_BATCH_COUNT * time) == listLen - 1){
                    condition.deleteCharAt(condition.length()-1);
                    condition.append(" )");
                }
                if((i + SQL_BATCH_COUNT * time) == listLen){
                    break;
                }
                addList.add(idList.get(time * SQL_BATCH_COUNT + i));
            }
            String conditionStr = condition.toString();
            String[] addIdStr = new String[addList.size()];
            for(int index = 0; index < addList.size() ; index ++){
                addIdStr[index] = String.valueOf(addList.get(index));
            }

            if (checkAPP(context)) {
                try {
                    c = cr.query(Uri.parse(VoiceConstant.SMARTISAN_MUSIC_ALL_URI),
                            projectionArgs, conditionStr , addIdStr, null);
                } catch (Exception e) {
                    log.e("buildMediaNameList : exception is " + e.getMessage());
                    c = cr.query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, projectionArgs, AUDIO_CONDITION+" AND "+conditionStr, addIdStr,
                            AudioColumns.TITLE_KEY);
                }
            } else {
                c = cr.query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, projectionArgs, AUDIO_CONDITION+" AND "+conditionStr, addIdStr,
                        AudioColumns.TITLE_KEY);
            }

            Pattern p = Pattern.compile(VoiceConstant.REGEX_SPECIAL);
            Matcher m;

            try {
                if (c != null && c.getCount() > 0) {
                    c.moveToFirst();
                    do {
                        final String title = c.getString(c.getColumnIndex(MediaStore.Audio.Media.TITLE));
                        long musicID = c.getLong(c.getColumnIndex(MediaStore.Audio.Media._ID));
                        ContentValues value = new ContentValues();
                        value.put(VoiceSettings.MusicColumns.MEDIA_ID, musicID);
                        if (!TextUtils.isEmpty(title) && title.length() < 64) {
                            String titlePinyin = null;
                            m = p.matcher(title);
                            String temp = m.replaceAll("")/*.replaceAll(VoiceConstant.REGEX_NOT_NORMAL, "")*/;
                            if (!TextUtils.isEmpty(temp)) {
                                titlePinyin = PinYinUtil.getPinYin(temp,true);
                            } else {
                                titlePinyin = "unknow";
                            }
                            value.put(VoiceSettings.MusicColumns.TITLE_NAME, title);
                            value.put(VoiceSettings.MusicColumns.TITLE_PINYIN,
                                    titlePinyin);
                            log.e("addMediaToDatabase title "+title);
                        }
                        final String album = c.getString(c.getColumnIndex(MediaStore.Audio.Media.ALBUM));
                        if (!TextUtils.isEmpty(album) && album.length() < 64) {
                                final String albumPinyin = PinYinUtil.getPinYin(album,true);
                                value.put(VoiceSettings.MusicColumns.ALBUM_NAME, album);
                                value.put(VoiceSettings.MusicColumns.ALBUM_PINYIN,
                                        albumPinyin);
                            log.e("addMediaToDatabase album "+ album);
                        }
                        final String artist = c.getString(c.getColumnIndex(MediaStore.Audio.Media.ARTIST));
                        if (!TextUtils.isEmpty(artist) && artist.length() < 64) {
                                final String artistPinyin = PinYinUtil.getPinYin(artist,true);
                                value.put(VoiceSettings.MusicColumns.ARTIST_NAME, artist);
                                value.put(VoiceSettings.MusicColumns.ARTIST_PINYIN,
                                        artistPinyin);
                            log.e("addMediaToDatabase artist "+ artist);
                        }
                        contentValuesList.add(value);
                    } while (c.moveToNext());
                }
            } finally {
                if (c != null) {
                    c.close();
                }
            }

            PinYinUtil.clearCache();

            time ++;
            if( SQL_BATCH_COUNT * time >= listLen){
               isLoop = false;
            }
        }
    }

    public static ArrayList<String> getMediaNameList(Context context, boolean isLoaded) {
        if (context == null) {
            return null;
        }
        if (sLastMusicNameList.size() == 0) {
            // select name from sara where type = 2
            Cursor c = context.getContentResolver()
                    .query(VoiceSettings.MusicColumns.CONTENT_URI_MUSIC,
                            VoiceSettings.MusicColumns.PROJECTION_MUSIC_LAST_LIST,
                            null, null, null);
            synchronized (sLastMusicNameList) {
                try {
                    if (c != null && c.getCount() > 0) {
                        while (c.moveToNext()) {
                            String name = c.getString(c.getColumnIndex(VoiceSettings.MusicColumns.TITLE_NAME));
                            if (!TextUtils.isEmpty(name) && !sLastMusicNameList.contains(name)) {
                                sLastMusicNameList.add(name);
                            }
                            name = c.getString(c.getColumnIndex(VoiceSettings.MusicColumns.ALBUM_NAME));
                            if (!TextUtils.isEmpty(name) && !sLastMusicNameList.contains(name)) {
                                sLastMusicNameList.add(name);
                            }
                            name = c.getString(c.getColumnIndex(VoiceSettings.MusicColumns.ARTIST_NAME));
                            if (!TextUtils.isEmpty(name) && !sLastMusicNameList.contains(name)) {
                                sLastMusicNameList.add(name);
                            }
                        }
                    }
                } catch (Exception e) {
                    sLastMusicNameList.clear();
                    log.e("it has no musics in last list " + e.getMessage());
                } finally {
                    if (c != null) {
                        c.close();
                    }
                }
            }
        }
        if (sLastMusicNameList.size() == 0) {
            buildMediaNameList(context, true, true);
        }
        return new ArrayList<String>(sLastMusicNameList);
    }

    public static ArrayList<String> getMediaList(Context context) {
        ArrayList<String> musicNameList = new ArrayList<String>();
        ContentResolver cr = context.getContentResolver();
        Cursor c = null;
        String[] projectionArgs = new String[] { MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.ARTIST, MediaStore.Audio.Media.ALBUM };
        if (checkAPP(context)) {
            try {
                c = cr.query(Uri.parse(VoiceConstant.SMARTISAN_MUSIC_ALL_URI),
                        projectionArgs, null, null, null);
            } catch (Exception e) {
                log.e("getMediaList : exception is " + e.getMessage());
                c = cr.query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                        projectionArgs, null, null, MediaStore.Audio.Media.TITLE + " ASC");
            }
        } else {
            c = cr.query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    projectionArgs, null, null, MediaStore.Audio.Media.TITLE
                            + " ASC");
        }
        try {
            if (c != null && c.getCount() > 0) {
                c.moveToFirst();
                do {
                    StringBuilder sb = new StringBuilder();
                    final String title = c.getString(c.getColumnIndex(MediaStore.Audio.Media.TITLE));
                    if (!TextUtils.isEmpty(title) && title.length() < 64) {
                        sb.append(title);
                    }
                    sb.append(", ");
                    final String artist = c.getString(c.getColumnIndex(MediaStore.Audio.Media.ARTIST));
                    if (!TextUtils.isEmpty(artist) && artist.length() < 64) {
                        sb.append(artist);
                    }
                    sb.append(", ");
                    final String album = c.getString(c.getColumnIndex(MediaStore.Audio.Media.ALBUM));
                    if (!TextUtils.isEmpty(album) && album.length() < 64) {
                        sb.append(album);
                    }
                    musicNameList.add(sb.toString());
                } while (c.moveToNext());
            }
        } finally {
            if (c != null) {
                c.close();
            }
        }
        return musicNameList;
    }

    public static void getMediaStructList(Context context, String name, ArrayList<MediaStruct> musicList, String realName) {
        if (context == null) {
            return;
        }
        //the name in the sara db may be duplicate now, so we need to avoid duplicate query by list
        ArrayList<String> nameList = new ArrayList<String>();
        ArrayList<String> noList = new ArrayList<String>();
        ArrayList<String> containsList = new ArrayList<String>();
        Cursor c = context.getContentResolver()
                .query(VoiceSettings.MusicColumns.CONTENT_URI_MUSIC,
                        VoiceSettings.MusicColumns.PROJECTION_MUSIC_NAME_MAP,
                        VoiceSettings.MusicColumns.TITLE_PINYIN + " LIKE ? OR "
                                + VoiceSettings.MusicColumns.TITLE_NAME + " LIKE ? OR "
                                + VoiceSettings.MusicColumns.ALBUM_PINYIN + " LIKE ? OR "
                                + VoiceSettings.MusicColumns.ALBUM_NAME + " LIKE ? OR "
                                + VoiceSettings.MusicColumns.ARTIST_PINYIN + " LIKE ? OR "
                                + VoiceSettings.MusicColumns.ARTIST_NAME + " LIKE ? ",
                        new String[]{"%" + name + "%", "%" + realName + "%", "%" + name + "%", "%" + realName + "%", "%" + name + "%", "%" + realName + "%"},
                        null);
            try {
                if (c != null && c.getCount() > 0) {
                    c.moveToFirst();
                    do {
                        String musicName = c.getString(c.getColumnIndex(VoiceSettings.MusicColumns.TITLE_NAME));
                        String musicPinyin = c.getString(c.getColumnIndex(VoiceSettings.MusicColumns.TITLE_PINYIN));
                        if (!TextUtils.isEmpty(musicName) && !TextUtils.isEmpty(musicPinyin) && musicPinyin.contains(name)){
                             orderMusicNameByAccuracy(realName,musicName,nameList,containsList,noList);
                        }
                        musicName = c.getString(c.getColumnIndex(VoiceSettings.MusicColumns.ALBUM_NAME));
                        musicPinyin = c.getString(c.getColumnIndex(VoiceSettings.MusicColumns.ALBUM_PINYIN));
                         if (!TextUtils.isEmpty(musicName) && !TextUtils.isEmpty(musicPinyin) && musicPinyin.contains(name)){
                             orderMusicNameByAccuracy(realName,musicName,nameList,containsList,noList);
                        }
                        musicName = c.getString(c.getColumnIndex(VoiceSettings.MusicColumns.ARTIST_NAME));
                        musicPinyin = c.getString(c.getColumnIndex(VoiceSettings.MusicColumns.ARTIST_PINYIN));
                         if (!TextUtils.isEmpty(musicName) && !TextUtils.isEmpty(musicPinyin) && musicPinyin.contains(name)){
                             orderMusicNameByAccuracy(realName,musicName,nameList,containsList,noList);
                        }
                    } while (c.moveToNext());
                }
            } finally {
                if (c != null){
                    c.close();
                }
            }
            nameList.addAll(containsList);
            nameList.addAll(noList);
            containsList.clear();
            noList.clear();
            MediaUtil.getMediaUriByMusicName(context, nameList, musicList);
            nameList.clear();
    }

    public static void onCheckSaraDBUpgrade(Context context){
        log.d("onCheckSaraDBUpgrade is enter");
        //just query the media data in sara db to trigger the upgrade check in db, do nothing to the feedback
        Cursor c = null;
        try {
            c = context.getContentResolver()
                    .query(VoiceSettings.MusicColumns.CONTENT_URI_MUSIC,
                            VoiceSettings.MusicColumns.PROJECTION_MEDIAID_VERSION_SARA,
                             null, null, null);
        } catch (Exception ex){
        } finally {
            if (c != null) {
                c.close();
            }
        }
    }

    public static void getRandomMediaStructList(Context context, ArrayList<MediaStruct> musicList) {
        if (context == null) {
            return;
        }
        ArrayList<String> nameList = new ArrayList<String>();
        // select name ,titlePinyin from sara where type = 2
        Cursor c = context.getContentResolver()
                .query(VoiceSettings.MusicColumns.CONTENT_URI_MUSIC,
                        VoiceSettings.MusicColumns.PROJECTION_MUSIC_NAME_MAP,
                        null, null, null);
        try {
            if (c != null && c.getCount() > 0) {
                Random r = new Random();
                int index = r.nextInt(c.getCount())+1;
                c.move(index);
                String searchStr = c.getString(c.getColumnIndex(VoiceSettings.MusicColumns.TITLE_NAME));
                if(TextUtils.isEmpty(searchStr)){
                    searchStr = c.getString(c.getColumnIndex(VoiceSettings.MusicColumns.ALBUM_NAME));
                }
                if(TextUtils.isEmpty(searchStr)){
                    searchStr = c.getString(c.getColumnIndex(VoiceSettings.MusicColumns.ARTIST_NAME));
                }
                nameList.add(searchStr);
                MediaUtil.getMediaUriByMusicName(
                        context,nameList,musicList);
                nameList.clear();
            }
        } finally {
            if (c != null){
                c.close();
            }
        }
    }

    public static String formatTime(long time) {
        String min = time / (1000 * 60) + "";
        String sec = time % (1000 * 60) + "";
        if (min.length() < 2) {
            min = "0" + time / (1000 * 60) + "";
        } else {
            min = time / (1000 * 60) + "";
        }
        if (sec.length() == 4) {
            sec = "0" + (time % (1000 * 60)) + "";
        } else if (sec.length() == 3) {
            sec = "00" + (time % (1000 * 60)) + "";
        } else if (sec.length() == 2) {
            sec = "000" + (time % (1000 * 60)) + "";
        } else if (sec.length() == 1) {
            sec = "0000" + (time % (1000 * 60)) + "";
        }
        return min + ":" + sec.trim().substring(0, 2);
    }

    //get the music picture
    public static Bitmap getArtworkFromFile(Context context,long songid) {
        Bitmap bm = null;
        if (songid < 0) {
            return bm;
        }
        try {
            if (songid > 0) {
                Uri uri = Uri.parse("content://media/external/audio/media/" + songid + "/albumart");
                ParcelFileDescriptor pfd =
                        context.getContentResolver().openFileDescriptor(uri, "r");
                if (pfd != null) {
                    FileDescriptor fd = pfd.getFileDescriptor();
                    BitmapFactory.Options options = new BitmapFactory.Options();
                    options.inSampleSize = 1;
                    options.inJustDecodeBounds = true;
                    BitmapFactory.decodeFileDescriptor(fd, null, options);
                    options.inSampleSize = computeSampleSize(options, 800);
                    options.inJustDecodeBounds = false;
                    options.inDither = false;
                    options.inPreferredConfig = Bitmap.Config.ARGB_8888;
                    bm = BitmapFactory.decodeFileDescriptor(fd, null, options);
                }
            }
        } catch (FileNotFoundException ex) {

        }
        return bm;
    }

    public static int computeSampleSize(BitmapFactory.Options options, int target){
        int w = options.outWidth;
        int h = options.outHeight;
        int candidateW = w / target;
        int candidateH = h / target;
        int candidate = Math.max(candidateW, candidateH);
        if (candidate == 0)
            return 1;
        if (candidate > 1){
            if ((w > target) && (w / candidate) < target)
                candidate -= 1;
        }
        if (candidate > 1){
            if ((h > target) && (h / candidate) < target)
                candidate -= 1;
        }
        return candidate;
    }

    public static int getPosition(ArrayList<long[]> list, long audioId) {
        if (list == null || list.size() == 0 || audioId == 0) {
            return -1;
        }
        for (int i = 0; i < list.size(); i++) {
            long[] audioList = list.get(i);
            for (int j = 0; j < audioList.length; j++) {
                if (audioId == audioList[j]) {
                    return i;
                }
            }
        }
        return -1;
    }

    public static int isContain(ArrayList<long[]> list, long[] queue) {
        if (list == null || list.size() == 0 || queue == null || queue.length == 0) {
            return -1;
        }
        int containsIndex = -1;

        out: for (int m = 0;m < list.size() ;m++) {
            long[] ids = list.get(m);
            if (queue.length > 1) {
                if (ids.length == queue.length) {
                    boolean equals = false;
                    for (int i = 0; i < ids.length; i++) {
                        boolean isContains = false;
                        for (int j = 0; j < queue.length; j++) {
                            if (ids[i] == queue[j]) {
                                isContains = true;
                                break;
                            }
                        }
                        if (!isContains) {
                            equals = false;
                            break;
                        } else {
                            equals = true;
                        }
                    }
                    if (equals) {
                        containsIndex = m;
                        break;
                    } else {
                        containsIndex = -1;
                        continue out;
                    }
                }
            } else {
                for (int i = 0; i < ids.length; i++) {
                    if (ids[i] == queue[0]) {
                        containsIndex = m;
                        break;
                    }
                }
            }
        }
        return containsIndex;
    }
    public static  Comparator<MediaStruct> comparator = new Comparator<MediaStruct>() {
        @Override
        public int compare(MediaStruct s1, MediaStruct s2) {
            if (s1.mFlagType > s2.mFlagType) {
                return -1;
            } else if (s1.mFlagType < s2.mFlagType) {
                return 1;
            } else {
                return 0;
            }
        }
    };

    public static boolean checkAPP(Context context) {
        try {
            context.getPackageManager()
                    .getApplicationInfo(MUSIC_PACKAGE_NAME,
                            PackageManager.GET_UNINSTALLED_PACKAGES);
            return true;
        } catch (NameNotFoundException e) {
            return false;
        }
    }

    public static Bitmap scaleCenterCrop(Bitmap source, int newWidth, int newHeight) {
        int sourceWidth = source.getWidth();
        int sourceHeight = source.getHeight();

        // Compute the scaling factors to fit the new height and width,
        // respectively.
        // To cover the final image, the final scaling will be the bigger
        // of these two.
        float xScale = (float) newWidth / sourceWidth;
        float yScale = (float) newHeight / sourceHeight;
        float scale = Math.max(xScale, yScale);

        // Now get the size of the source bitmap when scaled
        float scaledWidth = scale * sourceWidth;
        float scaledHeight = scale * sourceHeight;

        // Let's find out the upper left coordinates if the scaled bitmap
        // should be centered in the new size give by the parameters
        float left = (newWidth - scaledWidth) / 2;
        float top = (newHeight - scaledHeight) / 2;

        // The target rectangle for the new, scaled version of the source bitmap
        // will now
        // be
        RectF targetRect = new RectF(left, top, left + scaledWidth, top + scaledHeight);

        // Finally, we create a new bitmap of the specified size and draw our
        // new,
        // scaled bitmap onto it.
        Bitmap dest = Bitmap.createBitmap(newWidth, newHeight, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(dest);
        PaintFlagsDrawFilter pfd = new PaintFlagsDrawFilter(0,
                Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
        canvas.setDrawFilter(pfd);
        canvas.drawBitmap(source, null, targetRect, null);

        return dest;
    }

    public static int orderMusic(MediaStruct s1, MediaStruct s2, String name) {
        int ms1Match = getOrderValue(s1, name);
        int ms2Match = getOrderValue(s2, name);
        if (ms1Match == ms2Match) {
            return 0;
        } else if (ms1Match > ms2Match) {
            return 1;
        } else {
            return -1;
        }
    }

    public static int getOrderValue(MediaStruct ms, String name) {
        if (ms.mFlagType == MediaUtil.DISPLAY_AS_TITLE) {
            return getOrderValue(ms.mTitle.get(0), name);
        } else if (ms.mFlagType == MediaUtil.DISPLAY_AS_ALBUM) {
            return getOrderValue(ms.mAlbum, name);
        } else if (ms.mFlagType == MediaUtil.DISPLAY_AS_ALBUM) {
            return getOrderValue(ms.mArtist, name);
        }
        return -1;
    }

    public static int getOrderValue(String value, String name) {
        if (value.equals(name)) {
            return 3;
        } else if (value.startsWith(name)) {
            return 2;
        } else if (value.contains(name)) {
            return 1;
        } else {
            return 0;
        }
    }
    public static boolean hasMusic(Context context) {
        boolean existMusic = true;
        Cursor c = null;
        try {
            if (checkAPP(context)) {
                try {
                    c = context.getContentResolver().query(Uri.parse(VoiceConstant.SMARTISAN_MUSIC_ALL_URI), null, null, null, null);
                } catch (Exception e) {
                    c = context.getContentResolver().query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, null, AUDIO_CONDITION, null, null);
                }
            } else {
                c = context.getContentResolver().query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, null, AUDIO_CONDITION, null, null);
            }
            if (c == null || c.getCount() <= 0) {
                existMusic = false;
            }
        } catch (Exception e) {
            existMusic = false;
            log.e("exception is " + e.getMessage());
        } finally {
            if (c != null) {
                c.close();
            }
        }
        return existMusic;
    }

    private static void orderMusicNameByAccuracy(String realName,
            String musicName, ArrayList<String> nameList,
            ArrayList<String> containsList, ArrayList<String> noList) {
        if (realName.equals(musicName) && !nameList.contains(musicName)) {
            nameList.add(musicName);
        } else if (musicName.contains(realName) && !containsList.contains(musicName)) {
            containsList.add(musicName);
        } else if (!noList.contains(musicName)) {
            noList.add(musicName);
        }
    }
}
