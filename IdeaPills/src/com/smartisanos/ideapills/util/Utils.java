package com.smartisanos.ideapills.util;

import android.app.ActivityManagerNative;
import android.app.KeyguardManager;
import android.app.SmtPCUtils;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.ClipDescription;
import android.content.ClipboardManager;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.content.res.Configuration;
import android.media.MediaPlayer;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.UserHandle;
import android.provider.Settings;
import android.service.onestep.GlobalBubble;
import android.text.Editable;
import android.text.TextUtils;
import android.view.DragEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;
import android.widget.Toast;

import com.android.internal.sidebar.ISidebarService;
import com.smartisanos.ideapills.BubbleController;
import com.smartisanos.ideapills.Constants;
import com.smartisanos.ideapills.IdeaPillsApp;
import com.smartisanos.ideapills.InterfaceDefine;
import com.smartisanos.ideapills.R;
import com.smartisanos.ideapills.common.util.FileUtils;
import com.smartisanos.ideapills.common.util.MutiTaskHandler;
import com.smartisanos.ideapills.entity.AttachMentItem;
import com.smartisanos.ideapills.entity.BubbleItem;
import com.smartisanos.ideapills.common.util.UIHandler;
import com.smartisanos.ideapills.common.util.CommonUtils;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Callable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

import smartisanos.api.SettingsSmt;
import smartisanos.os.RemoteCallback;
import smartisanos.util.DeviceType;

import static android.text.format.DateUtils.SECOND_IN_MILLIS;
import static java.net.HttpURLConnection.HTTP_OK;

public class Utils {
    private static final LOG log = LOG.getInstance(Utils.class);

    public static enum STRING_TYPE {
        NONE, ENGLISH, CHINESE, NUMBER
    }

    public static enum DIALOG_TYPE {
        NONE, OPEN_IDEA
    }

    static void sendCloseSystemWindows(Context context, String reason) {
        if (ActivityManagerNative.isSystemReady()) {
            try {
                ActivityManagerNative.getDefault().closeSystemDialogs(reason);
            } catch (RemoteException e) {
            }
        }
    }

    public static boolean isSetupCompelete(Context context) {
        return Settings.Secure.getInt(context.getContentResolver(), Settings.Secure.USER_SETUP_COMPLETE, 0) == 1;
    }

    public static void copyText(Context context, CharSequence cs, boolean inHistory){
        ClipboardManager cm  = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        try {
            Method method = cm.getClass().getMethod("setPrimaryClip", ClipData.class, boolean.class);
            try {
                method.invoke(cm, ClipData.newPlainText(null, cs), inHistory);
                return;
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            }
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
        cm.setPrimaryClip(ClipData.newPlainText(null, cs));
    }

    public static String formatRemindTime(long time, Context context) {
        String timeFormat = "";
        Date remindDate = new Date(time);
        Date now = new Date();
        if (remindDate.after(now)) {
            SimpleDateFormat dateFormat;
            int year = remindDate.getYear();
            int month = remindDate.getMonth();
            int day = remindDate.getDate();
            int hour = remindDate.getHours();
            int min = remindDate.getMinutes();
            if (year != now.getYear()) {
                dateFormat = new SimpleDateFormat(context.getResources().getString(R.string.bubble_datetime_format));
                timeFormat = dateFormat.format(remindDate);
            } else {
                if (month != now.getMonth()) {
                    dateFormat = new SimpleDateFormat(context.getResources().getString(R.string.bubble_datetime_format_month));
                    timeFormat = dateFormat.format(remindDate);
                } else {
                    if (day != now.getDate()) {
                        dateFormat = new SimpleDateFormat(context.getResources().getString(R.string.bubble_datetime_format_day));
                        timeFormat = dateFormat.format(remindDate);
                    } else {
                        dateFormat = new SimpleDateFormat(context.getResources().getString(R.string.bubble_datetime_format_time));
                        timeFormat = dateFormat.format(remindDate);
                    }
                }
            }
        }
        return timeFormat;
    }

    public static boolean isPackageInstalled(Context context, String packageName){
        try {
            context.getPackageManager().getPackageInfo(packageName, 0);
            return true;
        } catch (NameNotFoundException e) {
            // NA
        }
        return false;
    }

    private static final String LAUNCHER_NAME = "com.smartisanos.launcher.Launcher";

    public static boolean inArea(float rawX, float rawY, View view) {
        int[] loc = new int[2];
        view.getLocationOnScreen(loc);
        int viewWidth = view.getWidth();
        int viewHeight = view.getHeight();
        int left = loc[0];
        int top = loc[1];
        int right = left + viewWidth;
        int bottom = top + viewHeight;
        if (left < rawX && rawX < right) {
            if (top < rawY && rawY < bottom) {
                return true;
            }
        }
        return false;
    }

    public static boolean isNetworkConnected(Context context) {
        if (context == null) {
            return false;
        }
        boolean connected = false;
        try {
            ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo networkInfo = cm.getActiveNetworkInfo();
            if (networkInfo != null) {
                connected = networkInfo.isConnected();
            }
        } catch (Exception e) {}
        return connected;
    }

    public static boolean isWifiConnected(Context context) {
        if (context == null) {
            return false;
        }
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo wifi = cm.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        return wifi != null && wifi.isConnected();
    }

    public static ApplicationInfo getTopApplication(Context context) {
        try {
            ComponentName name = ActivityManagerNative.getDefault().getTopActivity();
            if (name != null) {
                String pkg = name.getPackageName();
                PackageManager pm = context.getPackageManager();
                return pm.getApplicationInfo(pkg, 0);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static ComponentName getTopActivityName() {
        try {
            return ActivityManagerNative.getDefault().getTopActivity();
        } catch (RemoteException e) {
            // NA
        }
        return null;
    }

    static final int TYPE_LAYER_OFFSET = 1000;
    static final int TYPE_LAYER_MULTIPLIER = 10000;

    public static void quietClose(Closeable io) {
        if (io != null) {
            try {
                io.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static int getDragGlobalBubbleNumber(DragEvent event) {
        ClipDescription clipDescription = event.getClipDescription();
        return GlobalBubble.countFromClipDescription(clipDescription);
    }

    public static List<GlobalBubble> convertToBubbleItems(ClipData clipData) {
        if (isEmptyTextClipData(clipData)) {
            return null;
        }
        int color = Constants.getNewBubbleColor();
        List<GlobalBubble> list = new ArrayList<GlobalBubble>();
        ClipDescription clipDescription = clipData.getDescription();
        for (int i = clipDescription.getMimeTypeCount() - 1; i >= 0; i--) {
            if (ClipDescription.MIMETYPE_TEXT_PLAIN.equals(clipDescription.getMimeType(i))) {
                CharSequence text = clipData.getItemAt(0).getText();
                if (!TextUtils.isEmpty(text) && GlobalBubbleManager.getInstance().checkIsTextLengthInLimit(text.toString())) {
                    GlobalBubble item = new GlobalBubble();
                    item.setColor(color);
                    item.setType(GlobalBubble.TYPE_TEXT);
                    item.setText(text.toString());
                    list.add(item);
                }
                return list;
            }
        }
        return null;
    }

    public static boolean isEmptyTextClipData(ClipData clipData) {
        if (clipData == null) {
            return true;
        }
        if (clipData.getItemCount() <= 0
                || clipData.getDescription() == null
                || clipData.getDescription().getMimeTypeCount() <= 0) {
            return true;
        }

        boolean hasText = false;
        ClipDescription clipDescription = clipData.getDescription();
        for (int i = clipDescription.getMimeTypeCount() - 1; i >= 0; i--) {
            if (ClipDescription.MIMETYPE_TEXT_PLAIN.equals(clipDescription.getMimeType(i))) {
                CharSequence text = clipData.getItemAt(0).getText();
                if (!TextUtils.isEmpty(text)) {
                    hasText = true;
                }
            }
        }
        return !hasText;
    }

    public static boolean isClipDataContainsAttachment(ClipData clipData) {
        if (clipData == null) {
            return false;
        }
        if (clipData.getItemCount() <= 0
                || clipData.getDescription() == null
                || clipData.getDescription().getMimeTypeCount() <= 0) {
            return false;
        }

        boolean hasAttach = false;
        ClipDescription clipDescription = clipData.getDescription();
        for (int i = clipDescription.getMimeTypeCount() - 1; i >= 0; i--) {
            if (!ClipDescription.MIMETYPE_TEXT_PLAIN.equals(clipDescription.getMimeType(i))) {
                hasAttach = true;
                break;
            }
        }
        return hasAttach;
    }

    public static boolean isUseFingerPrint(Context context) {
        int useFingerPrint = Settings.Global.getInt(context.getContentResolver(), SettingsSmt.Global.USE_FINGERPRINT_IN_LAUNCHER, 0);
        return useFingerPrint == 1;
    }

    private static boolean isChinese(char c) {
        Character.UnicodeBlock ub = Character.UnicodeBlock.of(c);
        if (ub == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS || ub == Character.UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS
                || ub == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A || ub == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_B
                || ub == Character.UnicodeBlock.CJK_SYMBOLS_AND_PUNCTUATION || ub == Character.UnicodeBlock.HALFWIDTH_AND_FULLWIDTH_FORMS
                || ub == Character.UnicodeBlock.GENERAL_PUNCTUATION) {
            return true;
        }
        return false;
    }

    public static boolean isChinese(CharSequence strName) {
        for (int i = 0; i < strName.length(); i++) {
            if (isChinese(strName.charAt(i))) {
                return true;
            }
        }
        return false;
    }

    public static boolean isEnglish(String text) {
        if (text == null) {
            return false;
        }
        String str = text.trim();
        Pattern pattern = Pattern.compile("^[a-zA-Z]*$");
        String[] strs = pattern.split(str);
        if (strs != null && strs.length == 0) {
            return true;
        }
        return false;
    }

    private static final int DEFAULT_TIMEOUT = (int) (10 * SECOND_IN_MILLIS);

    public static byte[] httpRequest(String request) {
        return httpRequest(request, DEFAULT_TIMEOUT);
    }

    public static byte[] httpRequest(String request, int timeout) {
        byte[] data = null;
        try {
            URL url = new URL(request);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(timeout);
            conn.setReadTimeout(timeout);
            data = readHttpConnectionResponse(conn);
        } catch (Exception e) {
            data = null;
            e.printStackTrace();
        }
        return data;
    }

    public static byte[] httpRequestPost(String request, String post) {
        if (post == null) {
            return null;
        }
        try {
            URL url = new URL(request);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setDoOutput(true);
            conn.setDoInput(true);
            BufferedOutputStream bos = new BufferedOutputStream(conn.getOutputStream());
            bos.write(post.getBytes());
            bos.flush();
            return readHttpConnectionResponse(conn);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private static byte[] readHttpConnectionResponse(HttpURLConnection conn) {
        byte[] data = null;
        try {
            final int responseCode = conn.getResponseCode();
            InputStream is = null;
            if (responseCode == HTTP_OK) {
                String encoding = conn.getContentEncoding();
                if ("gzip".equals(encoding)) {
                    is = new GZIPInputStream(conn.getInputStream());
                } else {
                    is = conn.getInputStream();
                }
            } else {
                log.error("httpRequest failed by response code ["+responseCode+"]");
            }
            if (is != null) {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                byte[] buf = new byte[1024];
                int len = 0;
                while ((len = is.read(buf)) > 0) {
                    baos.write(buf, 0, len);
                }
                data = baos.toByteArray();
                try {
                    is.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } catch (Exception e) {
            data = null;
            e.printStackTrace();
        }
        return data;
    }

    public static boolean isSetupComplete(Context context) {
        return Settings.Secure.getInt(context.getContentResolver(), InterfaceDefine.SETTINGS_USER_SETUP_COMPLETE, 0) == 1;
    }

    public static boolean checkIsVoiceAvailable(Context context, Uri uri) {
        if (uri == null) {
            return false;
        }
        ContentResolver contentResolver = context.getContentResolver();
        ParcelFileDescriptor fileDescriptor = null;
        try {
            fileDescriptor = contentResolver.openFileDescriptor(uri, "r");
            if (fileDescriptor != null) {
                return true;
            } else {
                log.error("file is null:" + uri);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            log.error("file not exist:" + uri);
        } finally {
            quietClose(fileDescriptor);
        }
        return false;
    }

    public static int generateVoiceDuration(Context context, Uri uri) {
        if (context == null || uri == null || !checkIsVoiceAvailable(context, uri)) {
            return 0;
        }
        int duration = 0;
        MediaPlayer mediaPlayer = new MediaPlayer();
        try {
            mediaPlayer.setDataSource(context, uri);
            mediaPlayer.prepare();
            duration = mediaPlayer.getDuration();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (IllegalStateException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            mediaPlayer.release();
        }
        return duration;
    }

    public static void loadWaveData(Context context, List<BubbleItem> items) {
        if (context == null || items == null || items.size() == 0) {
            return;
        }
        List<LoadWaveData> tasks = new ArrayList<LoadWaveData>();
        for (BubbleItem item : items) {
            tasks.add(new LoadWaveData(context, item));
        }
        try {
            MutiTaskHandler.invokeAll(tasks);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static void loadWaveDataAndGenerateVoiceDuration(Context context, List<BubbleItem> items) {
        if (context == null || items == null || items.size() == 0) {
            return;
        }
        List<LoadWaveDataAndDuration> tasks = new ArrayList<LoadWaveDataAndDuration>();
        for (BubbleItem item : items) {
            tasks.add(new LoadWaveDataAndDuration(context, item));
        }
        try {
            MutiTaskHandler.invokeAll(tasks);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private static class LoadWaveData implements Callable<Void> {
        private Context mContext;
        final private BubbleItem mItem;

        public LoadWaveData(Context context, BubbleItem item) {
            mContext = context;
            mItem = item;
        }

        @Override
        public Void call() throws Exception {
            byte[] wave = getWaveData(mContext, mItem.getUri());
            mItem.setWaveData(wave);
            return null;
        }
    }

    private static class LoadWaveDataAndDuration implements Callable<Void> {
        private Context mContext;
        final private BubbleItem mItem;

        public LoadWaveDataAndDuration(Context context, BubbleItem item) {
            mContext = context;
            mItem = item;
        }

        @Override
        public Void call() throws Exception {
            byte[] wave = getWaveData(mContext, mItem.getUri());
            mItem.setWaveData(wave);
            if (mItem.getVoiceDuration() <= 0) {
                int duration = Utils.generateVoiceDuration(mContext, mItem.getUri());
                mItem.setVoiceDuration(duration);
            }
            return null;
        }
    }

    public static void generateVoiceDuration(Context context, List<BubbleItem> items) {
        if (context == null || items == null || items.size() == 0) {
            return;
        }
        List<GenerateVoiceDuration> tasks = new ArrayList<GenerateVoiceDuration>();
        for (BubbleItem item : items) {
            tasks.add(new GenerateVoiceDuration(context, item));
        }
        try {
            MutiTaskHandler.invokeAll(tasks);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private static class GenerateVoiceDuration implements Callable<Void> {
        private Context mContext;
        final private BubbleItem mItem;

        public GenerateVoiceDuration(Context context, BubbleItem item) {
            mContext = context;
            mItem = item;
        }

        @Override
        public Void call() throws Exception {
            int duration = Utils.generateVoiceDuration(mContext, mItem.getUri());
            mItem.setVoiceDuration(duration);
            return null;
        }
    }

    public static int getBackgroudRes(boolean inlarge, BubbleItem item) {
        if (item.isShareColor()) {
            if (inlarge) {
                return item.isShareFromOthers() ? R.drawable.pop_expansion_bg_share_t : R.drawable.pop_expansion_bg_share;
            } else {
                return item.isShareFromOthers() ? R.drawable.text_popup_share_t : R.drawable.text_popup_share;
            }
        }
        switch (item.getColor()) {
            case GlobalBubble.COLOR_RED: {
                if (inlarge) {
                    return R.drawable.pop_expansion_bg_red;
                } else {
                    return R.drawable.text_popup_red;
                }
            }
            case GlobalBubble.COLOR_ORANGE: {
                if (inlarge) {
                    return R.drawable.pop_expansion_bg_orange;
                } else {
                    return R.drawable.text_popup_orange;
                }
            }
            case GlobalBubble.COLOR_GREEN: {
                if (inlarge) {
                    return R.drawable.pop_expansion_bg_green;
                } else {
                    return R.drawable.text_popup_green;
                }
            }
            case GlobalBubble.COLOR_PURPLE: {
                if (inlarge) {
                    return R.drawable.pop_expansion_bg_purple;
                } else {
                    return R.drawable.text_popup_purple;
                }
            }
            case GlobalBubble.COLOR_NAVY_BLUE: {
                if (inlarge) {
                    return R.drawable.ppt_pop_expansion_bg;
                } else {
                    return R.drawable.ppt_text_popup;
                }
            }
            default: {
                if (inlarge) {
                    return R.drawable.pop_expansion_bg;
                } else {
                    return R.drawable.text_popup;
                }
            }
        }
    }

    public static boolean isFileUriExists(Context context, Uri fileUri) {
        if (fileUri == null) {
            return false;
        }
        ParcelFileDescriptor fileDescriptor = null;
        try {

            fileDescriptor = context.getContentResolver().openFileDescriptor(fileUri, "r");
            return fileDescriptor != null;
        } catch (Exception e) {
            e.printStackTrace();
            log.error("the scheme of uri is not file");
        } finally {
            Utils.quietClose(fileDescriptor);
        }
        return false;
    }

    public static boolean isFileUriVaild(Context context, Uri fileUri) {
        if (fileUri == null) {
            return false;
        }
        ParcelFileDescriptor fileDescriptor = null;
        try {

            fileDescriptor = context.getContentResolver().openFileDescriptor(fileUri, "r");
            return fileDescriptor != null && (fileDescriptor.getStatSize() <= AttachmentUtils.ATTACHMENT_SIZE_LIMIT);
        } catch (Exception e) {
            e.printStackTrace();
            log.error("the scheme of uri is not file");
        } finally {
            Utils.quietClose(fileDescriptor);
        }
        return false;
    }

    public static byte[] getWaveData(Context context, Uri uri) {
        return getWaveDataInternal(context, uri, true);
    }

    public static byte[] getWaveDataByWaveUri(Context context, Uri waveUri) {
        return getWaveDataInternal(context, waveUri, false);
    }

    private static byte[] getWaveDataInternal(Context context, Uri uri, boolean addWaveSuffix) {
        if (uri == null) {
            return null;
        }
        FileInputStream inputStream = null;
        ParcelFileDescriptor fileDescriptor = null;
        try {
            Uri waveUri;
            if (addWaveSuffix) {
                waveUri = Uri.parse(uri.toString() + BubbleItem.WAVE_SUFFIX);
            } else {
                waveUri = uri;
            }
            fileDescriptor = context.getContentResolver().openFileDescriptor(waveUri, "r");
            if (fileDescriptor == null) {
                log.error(uri.toString() + ".wave is not exists");
                return null;
            }
            inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
            return FileUtils.toByteArray(inputStream);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            log.error("the scheme of uri is not file");
        } catch (IOException e) {
            e.printStackTrace();
            log.error("the scheme of uri is not file");
        } finally {
            Utils.quietClose(fileDescriptor);
            Utils.quietClose(inputStream);
        }
        return null;
    }

    private static final String ACTION_BOOM_TEXT = "smartisanos.intent.action.BOOM_TEXT";
    private static final String ACTION_BOOM_TEXT_SECOND = "smartisanos.intent.action.BOOM_BUBBLE";
    private static final String FIRST_BOOM_ACTIVITY_NAME = "com.smartisanos.textboom.BoomActivity";
    private static final String SECOND_BOOM_ACTIVITY_NAME = "com.smartisanos.textboom.DoubleBoomActivity";
    public static void startBoomActivity(Context context, View view,final String text, final BubbleItem item,final boolean isEditable) {
        Intent intent = new Intent(ACTION_BOOM_TEXT);
        int boomed = 0;
        if (!SmtPCUtils.isValidExtDisplayId(context)) {
            boomed = getBoomHappened();
        }
        if (boomed == 0) {
            BubbleController.getInstance().setSecondBoom(false);
            intent.setFlags(Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        } else if (boomed == 1) {
            BubbleController.getInstance().setSecondBoom(true);
            intent.setAction(ACTION_BOOM_TEXT_SECOND);
            intent.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        } else {
            GlobalBubbleUtils.showSystemToast(context, R.string.bubble_boom_twice, Toast.LENGTH_SHORT);
            log.error("not support boom more than 2 times");
            return;
        }
        if (BubbleController.getInstance().isExtDisplay()) {
            intent.addFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
        }
        intent.putExtra(Intent.EXTRA_TEXT, text);
        int[] pos = new int[2];
        view.getLocationOnScreen(pos);
        intent.putExtra("boom_startx", pos[0]);
        intent.putExtra("boom_starty", pos[1]);
        intent.putExtra("caller_pkg", context.getPackageName());
        RemoteCallback callback = new RemoteCallback(new RemoteCallback.OnResultListener() {
            @Override
            public void onResult(Bundle result) {
                String newText = result.getString(Intent.EXTRA_RETURN_RESULT, "");
                if (LOG.DBG) {
                    log.info("newText = " + newText);
                }
                if (!newText.equals(item.getText()) && isEditable) {
                    if (CommonUtils.getStringLength(newText) > GlobalBubbleManager.BUBBLE_TEXT_MAX) {
                        GlobalBubbleManager.getInstance().showTextLimitToast();
                        newText = text;
                    }
                    item.setText(newText);
                    if (!item.isEmptyBubble()) {
                        item.setTimeStamp(System.currentTimeMillis());
                        item.setEdited();
                        GlobalBubbleManager.getInstance().updateBubbleItem(item);
                        IdeaPillsApp.getInstance().getBubbleObserverManager().notifyBubbleObserver(item, BubbleItem.MSG_ONEDIT);
                    } else {
                        item.dele();
                        GlobalBubbleManager.getInstance().removeBubbleItem(item);
                    }
                }
                if (isEditable) {
                    IdeaPillsApp.getInstance().getBubbleObserverManager().notifyBubbleObserver(item, BubbleItem.MSG_AFTEREDIT);
                } else {
                    IdeaPillsApp.getInstance().getBubbleObserverManager().notifyBubbleObserver(item, BubbleItem.MSG_AFTEREDIT_TIME);
                }
                BubbleController.getInstance().setSecondBoom(false);
            }
        }, UIHandler.getHandler());
        RemoteCallback callbackStarted = new RemoteCallback(new RemoteCallback.OnResultListener() {
            @Override
            public void onResult(Bundle result) {
                log.info("callbackStarted onResult");
                IdeaPillsApp.getInstance().getBubbleObserverManager().notifyBubbleObserver(item, BubbleItem.MSG_BEFOREEDIT);
            }
        }, new Handler());
        intent.putExtra(isEditable ? "ideapills_content" : "is_idea_time", true);
        intent.putExtra("editable_resource", true);
        intent.putExtra("smartisanos.textboom.REMOTE_CALLBACK", callback);
        intent.putExtra("smartisanos.textboom.REMOTE_CALLBACK_STARTED", callbackStarted);
        intent.putExtra("show_all_text", true);
        intent.putExtra("force_callback", true);
        intent.putExtra("enter_edit_mode", true);
        intent.setPreferenceIconPackage("com.smartisanos.sara");
        GlobalBubbleUtils.startActivityMayInExtDisplay(context, intent);
    }

    public static boolean isEditable(View touchView) {
        final boolean isTextView = touchView instanceof TextView;
        if (isTextView) {
            TextView textView = ((TextView) touchView);
            return textView.getText() instanceof Editable && textView.onCheckIsTextEditor() && textView.isEnabled();
        }
        return false;
    }

    private static int getBoomHappened() {
        ComponentName componentName = Utils.getTopActivityName();
        String topActivity = (componentName == null) ? null : componentName.getClassName();
        if (FIRST_BOOM_ACTIVITY_NAME.equals(topActivity)) {
            return 1;
        } else if (SECOND_BOOM_ACTIVITY_NAME.equals(topActivity)) {
            return 2;
        } else {
            return 0;
        }
    }

    private static final String[] SEND_ACTIONS = new String[] { Intent.ACTION_SEND,
            Intent.ACTION_SEND_MULTIPLE };
    private static final int MATCH_DIRECT_BOOT_UNAWARE = 0x00040000;
    private static final int MATCH_DIRECT_BOOT_AWARE = 0x00080000;
    private static final int DEFAULT_QUERY_FLAG = MATCH_DIRECT_BOOT_AWARE | MATCH_DIRECT_BOOT_UNAWARE;

    public static List<ResolveInfo> getShareComponentNameByPackage(Context context, String pkg, int userId) {
        List<ResolveInfo> result = new ArrayList<ResolveInfo>();
        for (String action : SEND_ACTIONS) {
            Intent intent = new Intent(action);
            intent.addCategory(Intent.CATEGORY_DEFAULT);
            intent.setType(ClipDescription.MIMETYPE_TEXT_PLAIN);
            intent.setPackage(pkg);
            List<ResolveInfo> list = context.getPackageManager().queryIntentActivitiesAsUser(intent, DEFAULT_QUERY_FLAG, userId);
            result.addAll(list);
        }
        return result;
    }

    public static ComponentName getWechatShareComponent(Context context) {
        ComponentName componentName = null;
        try {
            List<ResolveInfo> list = getShareComponentNameByPackage(context, Constants.WECHAT, UserHandle.USER_OWNER);
            if (list != null && list.size() > 0) {
                for (ResolveInfo info : list) {
                    String pkg = info.activityInfo.packageName;
                    String cmp = info.activityInfo.name;
                    if (Constants.WECHAT.equals(pkg) && Constants.WECHAT_SHARE_COMPONENT.equals(cmp)) {
                        componentName = new ComponentName(Constants.WECHAT, Constants.WECHAT_SHARE_COMPONENT);
                        break;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return componentName;
    }

    public static boolean isIdeaPillsEnable(Context context) {
        return Constants.settingGlobalGetInt(context, InterfaceDefine.SETTINGS_VOICE_INPUT, 1) == 1;
    }

    public static void killSelf(Context context) {
        try {
            String pkg = context.getPackageName();
            Object object = context.getSystemService(Context.ACTIVITY_SERVICE);
            Class clazz = object.getClass();
            Method method = clazz.getMethod("killPackageDependentsSmt", String.class, int.class);
            if (method != null) {
                method.invoke(object, pkg, 0);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void unregisterIdeaPills() {
        ISidebarService sidebarService = ISidebarService.Stub.asInterface(ServiceManager.getService(Context.SIDEBAR_SERVICE));
        if (sidebarService != null) {
            try {
                sidebarService.registerIdeaPills(null);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    public static boolean requestViewTouchFocus(View view){
        // NA
        try {
            Method requestTouchFocus = view.getClass().getMethod("requestTouchFocus");
            boolean result = (boolean) requestTouchFocus.invoke(view);
            return result;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }


    /**
     * (copied from {@link Intent#normalizeMimeType(String)} for pre-J)
     *
     * Normalize a MIME data type.
     *
     * <p>A normalized MIME type has white-space trimmed,
     * content-type parameters removed, and is lower-case.
     * This aligns the type with Android best practices for
     * intent filtering.
     *
     * <p>For example, "text/plain; charset=utf-8" becomes "text/plain".
     * "text/x-vCard" becomes "text/x-vcard".
     *
     * <p>All MIME types received from outside Android (such as user input,
     * or external sources like Bluetooth, NFC, or the Internet) should
     * be normalized before they are used to create an Intent.
     *
     * @param type MIME data type to normalize
     * @return normalized MIME data type, or null if the input was null
     * @see {@link #setType}
     * @see {@link #setTypeAndNormalize}
     */
    public static String normalizeMimeType(String type) {
        if (type == null) {
            return null;
        }

        type = type.trim().toLowerCase(Locale.US);

        final int semicolonIndex = type.indexOf(';');
        if (semicolonIndex != -1) {
            type = type.substring(0, semicolonIndex);
        }
        return type;
    }

    /**
     * (copied from {@link Uri#normalize()} for pre-J)
     *
     * Return a normalized representation of this Uri.
     *
     * <p>A normalized Uri has a lowercase scheme component.
     * This aligns the Uri with Android best practices for
     * intent filtering.
     *
     * <p>For example, "HTTP://www.android.com" becomes
     * "http://www.android.com"
     *
     * <p>All URIs received from outside Android (such as user input,
     * or external sources like Bluetooth, NFC, or the Internet) should
     * be normalized before they are used to create an Intent.
     *
     * <p class="note">This method does <em>not</em> validate bad URI's,
     * or 'fix' poorly formatted URI's - so do not use it for input validation.
     * A Uri will always be returned, even if the Uri is badly formatted to
     * begin with and a scheme component cannot be found.
     *
     * @return normalized Uri (never null)
     * @see {@link android.content.Intent#setData}
     * @see {@link #setNormalizedData}
     */
    public static Uri normalizeUri(Uri uri) {
        String scheme = uri.getScheme();
        if (scheme == null) return uri;  // give up
        String lowerScheme = scheme.toLowerCase(Locale.US);
        if (scheme.equals(lowerScheme)) return uri;  // no change

        return uri.buildUpon().scheme(lowerScheme).build();
    }

    public static Intent setIntentDataAndTypeAndNormalize(Intent intent, Uri data, String type) {
        Uri uri = normalizeUri(data);
        return intent.setDataAndType(uri, normalizeMimeType(type));
    }

    public static void callInputMethod(TextView view) {
        final InputMethodManager imm = InputMethodManager.peekInstance();
        imm.focusIn(view);
        imm.viewClicked(view);
        if (!imm.showSoftInput(view, 0)) {
            log.error("showSoftInputFailed");
        }
    }

    public static void hideInputMethod(View view) {
        final InputMethodManager imm = InputMethodManager.peekInstance();
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

    public static void sortAttachments(List<AttachMentItem> list) {
        int size = list.size();
        if (list != null && size > 1) {
            for (int i = 1; i < size; i++) {
                boolean isImageType = (list.get(i - 1).getType() == AttachMentItem.TYPE_IMAGE);
                AttachMentItem item = list.get(i);
                if (!isImageType) {
                    if (item.getType() == AttachMentItem.TYPE_IMAGE) {
                        moveAttachmentForward(list, i);
                    }
                }
            }
        }
    }

    public static void moveAttachmentForward(List<AttachMentItem> list, int index) {
        AttachMentItem item = list.remove(index);
        list.add(index - 1, item);
        if (index - 1 > 0 && list.get(index - 2).getType() != AttachMentItem.TYPE_IMAGE) {
            moveAttachmentForward(list, index - 1);
        }
    }

    public static void launchAppStoreForPackage(Context context, String packageName) {
        Uri uri = Uri.parse("market://details?id=" + packageName);
        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
        intent.setPackage("com.smartisanos.appstore");
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            context.startActivity(intent);
        } catch (ActivityNotFoundException e) {
            //ignore
        }
    }

    public static void closeSilently(Closeable c) {
        if (c == null) return;
        try {
            c.close();
        } catch (IOException t) {
            //ignore
        }
    }

    public static boolean isEnglishChar(String c) {
        Pattern p = Pattern.compile("[a-zA-Z]");
        Matcher m = p.matcher(c);
        return m.matches();
    }

    public static boolean isNumberChar(String c) {
        Pattern p = Pattern.compile("[0-9]*");
        Matcher m = p.matcher(c);
        return m.matches();
    }

    public static boolean isChineseChar(String c) {
        Pattern p = Pattern.compile("[\u4e00-\u9fa5]");
        Matcher m = p.matcher(c);
        return m.matches();
    }

    public static STRING_TYPE getStringType(String c) {
        if (isEnglishChar(c)) {
            return STRING_TYPE.ENGLISH;
        }
        if (isNumberChar(c)) {
            return STRING_TYPE.NUMBER;
        }
        if (isChineseChar(c)) {
            return STRING_TYPE.CHINESE;
        }

        return STRING_TYPE.NONE;
    }

    public static String formatString(String resultStr) {
        if (TextUtils.isEmpty(resultStr)) {
            return "";
        }
        STRING_TYPE currentStrType = STRING_TYPE.NONE;
        STRING_TYPE lastStrType = STRING_TYPE.NONE;
        StringBuffer sb = new StringBuffer();
        for(int i = 0; i < resultStr.length(); i++) {
            String c = String.valueOf(resultStr.charAt(i));
            currentStrType = getStringType(c);
            if (currentStrType != lastStrType && lastStrType != STRING_TYPE.NONE && currentStrType != STRING_TYPE.NONE) {
                c = " " + c;
            }
            lastStrType = currentStrType;
            sb.append(c);
        }
        return sb.toString();
    }

    public static int getInt(ContentValues contentValues, String key, int defaultValue) {
        Integer integer = contentValues.getAsInteger(key);
        return integer == null ? defaultValue : integer;
    }

    public static long getLong(ContentValues contentValues, String key, long defaultValue) {
        Long longValue = contentValues.getAsLong(key);
        return longValue == null ? defaultValue : longValue;
    }

    public static boolean getBoolean(ContentValues contentValues, String key, boolean defaultValue) {
        Boolean booleanValue = contentValues.getAsBoolean(key);
        return booleanValue == null ? defaultValue : booleanValue;
    }

    public static boolean isKeyguardLocked() {
        try {
            KeyguardManager km = (KeyguardManager) IdeaPillsApp.getInstance().getSystemService(
                    Context.KEYGUARD_SERVICE);
            return km.isKeyguardLocked();
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean isShieldSlidingShowIdeapills(Context context) {
        return DeviceType.isOneOf(DeviceType.OSCAR, DeviceType.OCEAN) &&
                context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;
    }

    public static boolean isTrident() {
        return DeviceType.is(DeviceType.TRIDENT);
    }

    public static int getHeadMarginTop(Context context) {
        int margin = 0;
        if (context != null) {
            if (isTrident()) {
                margin = context.getResources().getDimensionPixelSize(R.dimen.bubble_item_head_base_margin_top);
            } else {
                margin = context.getResources().getDimensionPixelSize(R.dimen.bubble_item_base_margin_top);
            }
        }
        return margin;
    }
}
