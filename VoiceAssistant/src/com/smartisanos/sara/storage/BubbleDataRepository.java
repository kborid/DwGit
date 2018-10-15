package com.smartisanos.sara.storage;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcelable;
import android.service.onestep.GlobalBubble;
import android.service.onestep.GlobalBubbleAttach;
import android.text.TextUtils;

import com.smartisanos.ideapills.common.data.BubbleColumn;
import com.smartisanos.ideapills.common.sync.SyncUtil;
import com.smartisanos.ideapills.common.util.FileUtils;
import com.smartisanos.ideapills.common.util.MutiTaskHandler;
import com.smartisanos.sara.SaraApplication;
import com.smartisanos.sara.bubble.OffLineRecognizeService;
import com.smartisanos.sara.setting.recycle.RecycleItem;
import com.smartisanos.sara.util.BubbleSpeechPlayer;
import com.smartisanos.sara.util.LogUtils;
import com.smartisanos.sara.util.SaraConstant;
import com.smartisanos.sara.util.SaraUtils;
import com.smartisanos.sara.util.StringUtils;
import com.smartisanos.sara.util.WaveFileGenerator;
import android.support.v4.content.LocalBroadcastManager;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;

import smartisanos.util.IdeaPillsUtils;

public class BubbleDataRepository {

    private static boolean sPermitPptimize = true;

    public static Bundle addGlobalBubbles(Context context, List<GlobalBubble> globalBubbles,
                                          List<GlobalBubbleAttach> globalBubbleAttachs, Bundle extra) {
        return IdeaPillsUtils.addGlobalBubbles(context, globalBubbles, globalBubbleAttachs, extra);
    }

    public static ArrayList<Parcelable> getGlobleBubbleList(Bundle bdl) {
        Bundle bundle = IdeaPillsUtils.callIdeaPills(null, SaraConstant.METHOD_LIST_BUBBLES, bdl);
        return bundle != null ? bundle.getParcelableArrayList(SaraConstant.KEY_BUBBLES) : null;
    }

    public static List<Uri> getEarliestVoiceBubbleUri(int limit) {
        final ContentResolver cr = SaraApplication.getInstance().getContentResolver();
        String where = BubbleColumn.TYPE + " in (" + GlobalBubble.TYPE_VOICE + ", " + GlobalBubble.TYPE_VOICE_OFFLINE + ")";
        Cursor c = null;
        try {
            List<Uri> uris = new ArrayList<Uri>();
            c = cr.query(BubbleColumn.CONTENT_URI, new String[]{BubbleColumn.URI},
                    where, null, BubbleColumn._ID + " ASC LIMIT " + limit);
            if (c != null && c.moveToFirst()) {
                do {
                    uris.add(Uri.parse(c.getString(0)));
                } while (c.moveToNext());
            }
            return uris;
        } catch (Exception e) {
            LogUtils.e(e);
        } finally {
            if (c != null) {
                c.close();
            }
        }
        return null;
    }

    public static ArrayList<RecycleItem> getRecycleBubbleList(Context context, String type) {
        final ContentResolver cr = context.getContentResolver();
        ArrayList<RecycleItem> itemList = new ArrayList<RecycleItem>();
        String where = null;
        if (SaraConstant.LIST_TYPE_USED.equals(type)) {
            where = BubbleColumn.USED_TIME + ">0 AND " + BubbleColumn.LEGACY_USED_TIME + "=0 AND " + BubbleColumn.MARK_DELETE + "=0";
        } else if (SaraConstant.LIST_TYPE_LEGACY_USED.equals(type)) {
            where = BubbleColumn.LEGACY_USED_TIME + ">0 AND " + BubbleColumn.REMOVED_TIME + "=0 AND " + BubbleColumn.MARK_DELETE + "=0";
        } else if (SaraConstant.LIST_TYPE_REMOVED.equals(type)) {
            where = BubbleColumn.REMOVED_TIME + ">0" + " AND " + BubbleColumn.MARK_DELETE + "=0";
        }
        Cursor c = cr.query(BubbleColumn.CONTENT_URI, BubbleColumn.COLUMNS, where, null, BubbleColumn.DEFAULT_SORT_ORDER);
        try {
            if (c != null && c.moveToFirst()) {
                do {
                    RecycleItem item = bindRecycleItem(context, c, type);
                    if (item != null) {
                        itemList.add(item);
                    }
                } while (c.moveToNext());
            }
        } catch (Exception e) {
            return null;
        } finally {
            if (c != null) {
                c.close();
            }
        }
        return itemList;
    }

    public static RecycleItem bindRecycleItem(Context context, Cursor cursor, String type) {
        RecycleItem item = new RecycleItem();
        try {
            int idIdx = cursor.getColumnIndex(BubbleColumn._ID);
            int typeIdx = cursor.getColumnIndex(BubbleColumn.TYPE);
            int colorIdx = cursor.getColumnIndex(BubbleColumn.COLOR);
            int todoTypeIdx = cursor.getColumnIndex(BubbleColumn.TODO_TYPE);
            int uriIdx = cursor.getColumnIndex(BubbleColumn.URI);
            int textIdx = cursor.getColumnIndex(BubbleColumn.TEXT);
            int removedTimeIdx = cursor.getColumnIndex(BubbleColumn.REMOVED_TIME);
            int usedTimeIdx = cursor.getColumnIndex(BubbleColumn.USED_TIME);
            int remindTimeIdx = cursor.getColumnIndex(BubbleColumn.REMIND_TIME);
            int dueDateIdx = cursor.getColumnIndex(BubbleColumn.DUE_DATE);
            int legacyUsedTimeIdx = cursor.getColumnIndex(BubbleColumn.LEGACY_USED_TIME);
            int shareStatusIdx = cursor.getColumnIndex(BubbleColumn.SHARE_STATUS);

            item.bubbleId = cursor.getInt(idIdx);
            item.bubbleText = cursor.getString(textIdx);
            item.bubbleType = cursor.getInt(typeIdx);
            if (item.bubbleType != GlobalBubble.TYPE_TEXT) {
                item.bubblePath = SaraUtils.formatContent2FilePath(context, Uri.parse(cursor.getString(uriIdx)));
            }
            int sharestatus = cursor.getInt(shareStatusIdx);
            if (sharestatus == GlobalBubble.SHARE_STATUS_ONE_TO_ONE
                    || sharestatus == GlobalBubble.SHARE_STATUS_MANY_TO_MANY) {
                item.bubbleColor = GlobalBubble.COLOR_SHARE;
            } else {
                item.bubbleColor = cursor.getInt(colorIdx);
            }
            int todoInDb = cursor.getInt(todoTypeIdx);
            todoInDb = (todoInDb == 0 ? GlobalBubble.TODO : todoInDb);
            item.bubbleTodo = todoInDb;
            if (SaraConstant.LIST_TYPE_USED.equals(type)) {
                item.setRecycleDate(context, cursor.getLong(usedTimeIdx));
            } else if (SaraConstant.LIST_TYPE_LEGACY_USED.equals(type)) {
                item.setRecycleDate(context, cursor.getLong(legacyUsedTimeIdx));
            } else if (SaraConstant.LIST_TYPE_REMOVED.equals(type)) {
                item.setRecycleDate(context, cursor.getLong(removedTimeIdx));
            }
            item.remindTime = cursor.getLong(remindTimeIdx);
            item.dueDate = cursor.getLong(dueDateIdx);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        return item;
    }

    public static void restoreGlobleBubble(Context context, Bundle bunble) {
        IdeaPillsUtils.callIdeaPills(context, SaraConstant.METHOD_RESTORE_BUBBLES, bunble);
    }

    public static void restoreLegacyGlobleBubble(Context context, Bundle bunble) {
        IdeaPillsUtils.callIdeaPills(context, SaraConstant.METHOD_RESTORE_LEGACY_BUBBLES, bunble);
    }

    public static void restoreDeleteGlobleBubble(Context context, Bundle bunble) {
        IdeaPillsUtils.callIdeaPills(context, SaraConstant.METHOD_RESTORE_DELETE_BUBBLES, bunble);
    }

    public static ArrayList<Parcelable> getGlobleBubbleAttachList(Context context, Bundle bdl) {
        Bundle bundle = IdeaPillsUtils.callIdeaPills(context, SaraConstant.METHOD_LIST_BUBBLE_ATTACHMENTS, bdl);
        return bundle != null ? bundle.getParcelableArrayList(SaraConstant.KEY_BUBBLE_ATTACHMENTS) : null;
    }

    public static ArrayList<Parcelable> getGlobleBubblesAttachList(Context context, Bundle bdl) {
        Bundle bundle = IdeaPillsUtils.callIdeaPills(context, SaraConstant.METHOD_LIST_BUBBLES_ATTACHMENTS, bdl);
        return bundle != null ? bundle.getParcelableArrayList(SaraConstant.KEY_BUBBLES_ATTACHMENTS) : null;
    }

    public static int getCanInsertGlobalBubbleNum(Context context, int selectNum) {
        Bundle visibleBundle = IdeaPillsUtils.callIdeaPills(context, SaraConstant.METHOD_VISIBLE_BUBBLE_COUNT, null);
        int count = 0;
        if (visibleBundle != null) {
            count = visibleBundle.getInt("count");
        }
        int maxInsertNum = (SaraConstant.MAX_VISIBLE_BUBBLE_COUNT - count);
        return selectNum <= maxInsertNum ? selectNum : maxInsertNum;
    }

    public static void destroyGlobleBubble(Context context, Bundle bunble) {
        IdeaPillsUtils.callIdeaPills(context, SaraConstant.METHOD_DESTROY_BUBBLES, bunble);
    }

    public static void hideGlobleBubble(Context context) {
        IdeaPillsUtils.callIdeaPills(context, SaraConstant.METHOD_HIDE_BUBBLE_LIST, null);
    }

    public static void updateGlobleBubble(final Context context, final int bubbleId, final String result, final String fileName) {
        MutiTaskHandler.post(new Runnable() {
            @Override
            public void run() {
                Bundle bunble = new Bundle();
                ArrayList<GlobalBubble> updateList = new ArrayList<GlobalBubble>();
                GlobalBubble globalBubble = new GlobalBubble();
                globalBubble.setId(bubbleId);
                if (!TextUtils.isEmpty(result)) {
                    globalBubble.setText(result);
                    globalBubble.setUri(Uri.parse(SaraUtils.formatFilePath2Content(context, fileName)));
                }
                globalBubble.setType(GlobalBubble.TYPE_VOICE);
                globalBubble.setTimeStamp(System.currentTimeMillis());
                updateList.add(globalBubble);
                bunble.putParcelableArrayList("bubbles", updateList);
                bunble.putBoolean(SaraConstant.BUBBLE_OFF_TO_ONLINE, true);
                IdeaPillsUtils.callIdeaPills(context, SaraConstant.METHOD_UPDATE_VOICE_BUBBLE_URI, bunble);
            }
        });
    }

    public static void sendShareInvitation(Context context, String phoneNum) {
        Bundle params = new Bundle();
        params.putString(SyncUtil.PARAM_KEY_PHONE_NUM, phoneNum);
        IdeaPillsUtils.callIdeaPills(context, SaraConstant.METHOD_SEND_SHARE_INVITE, params);
    }

    public static boolean isBubbleCanShare(Context context, boolean isShowToast) {
        Bundle params = new Bundle();
        params.putBoolean(SyncUtil.PARAM_KEY_SHOW_TOAST, isShowToast);
        Bundle bundle = IdeaPillsUtils.callIdeaPills(context, SaraConstant.METHOD_CAN_BUBBLE_SHARE, params);
        return bundle != null && bundle.getBoolean("canShare");
    }

    public static Bundle getBubbleCanShare(Context context, boolean isShowToast) {
        Bundle params = new Bundle();
        params.putBoolean(SyncUtil.PARAM_KEY_SHOW_TOAST, isShowToast);
        return IdeaPillsUtils.callIdeaPills(context, SaraConstant.METHOD_CAN_BUBBLE_SHARE, params);
    }

    public static void generateBubbleWaves(final Context context, final ArrayList<GlobalBubble> bubbles) {
        MutiTaskHandler.post(new Runnable() {
            @Override
            public void run() {
                if (bubbles != null && bubbles.size() > 0) {
                    WaveFileGenerator waveFileGenerator = new WaveFileGenerator();
                    ArrayList<GlobalBubble> updateList = new ArrayList<GlobalBubble>();

                    for (int i = 0; i < bubbles.size(); i++) {
                        GlobalBubble globalBubble = bubbles.get(i);
                        Uri uri = globalBubble.getUri();
                        if (uri == null) {
                            continue;
                        }
                        String mp3FilePath = SaraUtils.formatContent2FilePath(context, uri);
                        File waveFile = new File(mp3FilePath + SaraConstant.WAVE_FILE_SUFFIX);
                        String tempWavPath = mp3FilePath + SaraConstant.WAV_FILE_SUFFIX;
                        if (!waveFile.exists() && mp3FilePath != null && mp3FilePath.endsWith("mp3")) {
                            FileInputStream fis = null;
                            try {
                                fis = new FileInputStream(mp3FilePath);
                                boolean wavGenerateSuccess = waveFileGenerator.generateWav(fis.getFD(), tempWavPath);
                                if (!wavGenerateSuccess) {
                                    continue;
                                }
                                BubbleSpeechPlayer.getInstance(context).generateWaveFile(tempWavPath,
                                        20, waveFile.getAbsolutePath());
                                updateList.add(globalBubble);
                            } catch (Exception e) {
                                LogUtils.e(e);
                            } finally {
                                FileUtils.closeSilently(fis);
                                try {
                                    new File(tempWavPath).delete();
                                } catch (Exception e) {
                                    //ignore
                                }
                            }
                        } else if (waveFile.exists()) {
                            updateList.add(globalBubble);
                        }
                    }

                    Bundle updateBunble = new Bundle();
                    updateBunble.putParcelableArrayList("bubbles", updateList);
                    IdeaPillsUtils.callIdeaPills(context, SaraConstant.METHOD_MERGE_VOICE_WAVE_DATA, updateBunble);
                }
            }
        });
    }

    public static void updateBubblesPathWithRandomCharacter(final Context context) {
        MutiTaskHandler.post(new Runnable() {
            @Override
            public void run() {
                Bundle tmp = new Bundle();
                tmp.putString("type", SaraConstant.LIST_TYPE_ALL_VOICE);
                ArrayList<Parcelable> list = getGlobleBubbleList(tmp);

                if (list != null && list.size() > 0) {
                    sPermitPptimize = false;
                    for (int i = 0; i < list.size(); i++) {
                        Parcelable parcelable = list.get(i);
                        if (parcelable instanceof GlobalBubble) {
                            GlobalBubble globalBubble = (GlobalBubble) parcelable;

                            Uri uri = globalBubble.getUri();
                            if (uri == null) {
                                continue;
                            }
                            String path = uri.getLastPathSegment();
                            String prematch = path.substring(0, path.lastIndexOf("_"));
                            boolean update = false;
                            String oldPath = SaraUtils.formatContent2FilePath(context, uri);
                            File waveFile = new File(oldPath + SaraConstant.WAVE_FILE_SUFFIX);
                            if (!waveFile.exists()) {
                                BubbleSpeechPlayer.getInstance(context).generateWaveFile(oldPath);
                                update = true;
                            }
                            if (!TextUtils.isEmpty(path) && !prematch.matches(SaraConstant.SPECIAL_PREFIX_MATCH)) {
                                int startIndex = path.indexOf(SaraConstant.IDEA_PILL_SUFFIX) + SaraConstant.IDEA_PILL_SUFFIX.length();
                                String preffix = path.substring(startIndex, startIndex + 3);
                                String newPath = path.replaceFirst(preffix, preffix + "_" + StringUtils.getRandomCharacter());
                                SaraUtils.renameFile(context, oldPath, oldPath.replace(path, newPath));
                                SaraUtils.renameFile(context, oldPath + SaraConstant.WAVE_FILE_SUFFIX, oldPath.replace(path, newPath) + SaraConstant.WAVE_FILE_SUFFIX);
                                String pcmPath = oldPath.replace(SaraConstant.WAV_FILE_SUFFIX, SaraConstant.PCM_FILE_SUFFIX);
                                SaraUtils.renameFile(context, pcmPath, oldPath.replace(path, newPath).replace(SaraConstant.WAV_FILE_SUFFIX, SaraConstant.PCM_FILE_SUFFIX));
                                globalBubble.setUri(Uri.parse(uri.toString().replace(path, newPath)));
                                update = true;
                            }
                            if (path.endsWith(SaraConstant.WAV_FILE_SUFFIX)) {
                                String wavNewPath = SaraUtils.formatContent2FilePath(context, globalBubble.getUri());
                                String mp3NewPath = SaraUtils.pcmOrWav2Mp3(wavNewPath);
                                globalBubble.setUri(Uri.parse(SaraUtils.formatFilePath2Content(context, mp3NewPath)));
                                SaraUtils.renameFile(context, wavNewPath + SaraConstant.WAVE_FILE_SUFFIX, mp3NewPath + SaraConstant.WAVE_FILE_SUFFIX);
                                update = true;
                            }
                            if (update) {
                                ArrayList<GlobalBubble> updateList = new ArrayList<GlobalBubble>();
                                updateList.add(globalBubble);
                                Bundle updateBunble = new Bundle();
                                updateBunble.putParcelableArrayList("bubbles", updateList);
                                IdeaPillsUtils.callIdeaPills(context, SaraConstant.METHOD_UPDATE_VOICE_BUBBLE_URI, updateBunble);
                                Intent updateIntent = new Intent(SaraConstant.ACTION_DELETE_GLOBAL_BUBBLE);
                                LocalBroadcastManager.getInstance(context).sendBroadcast(updateIntent);
                            }
                        }
                    }
                    sPermitPptimize = true;
                }
            }
        });
    }

    public static void checkOffLineVoiceInBackground(final Context context) {
        MutiTaskHandler.post(new Runnable() {
            @Override
            public void run() {
                if (context != null && SaraUtils.isNetworkConnected()
                        && getOfflineGlobleBubbleNum() != 0 && sPermitPptimize) {
                    Intent intent = new Intent(SaraConstant.ACTION_PCM_RERECOGNIZE_ONLINE);
                    intent.setClass(context, OffLineRecognizeService.class);
                    context.startService(intent);
                }
            }
        });
    }

    public static int getOfflineGlobleBubbleNum() {
        final ContentResolver cr = SaraApplication.getInstance().getContentResolver();
        String where = BubbleColumn.TYPE + "=" + GlobalBubble.TYPE_VOICE_OFFLINE +
                " AND " + BubbleColumn.REMOVED_TIME + "=0 AND " + BubbleColumn.MARK_DELETE + "=0";
        int count = 0;
        Cursor c = null;
        try {
            c = cr.query(BubbleColumn.CONTENT_URI, new String[]{BubbleColumn._ID},
                    where, null, null);
            if (c != null) {
                count = c.getCount();
            }
        } catch (Exception e) {
            LogUtils.e(e);
        } finally {
            if (c != null) {
                c.close();
            }
        }
        return count;
    }
}
