package com.smartisanos.sara.util;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.text.SimpleDateFormat;

import android.animation.ValueAnimator;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManagerNative;
import android.app.ActivityOptions;
import android.app.KeyguardManager;
import android.app.PendingIntent;
import android.app.SmtPCUtils;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.ClipDescription;
import android.content.ClipboardManager;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.IPackageManager;
import android.content.pm.LabeledIntent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PaintFlagsDrawFilter;
import android.graphics.PointF;
import android.graphics.RectF;
import android.media.AudioManager;
import android.media.AudioSystem;
import android.net.IConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.os.UserHandle;
import android.provider.Settings;
import android.provider.ContactsContract.Contacts;
import android.support.v4.app.ActivityOptionsCompat;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;
import android.service.onestep.GlobalBubble;
import android.service.onestep.GlobalBubbleAttach;

import smartisanos.util.DeviceType;
import smartisanos.util.MultiSimAdapter;
import smartisanos.util.SidebarUtils;
import smartisanos.app.tracker.Agent;
import smartisanos.app.MenuDialog;
import smartisanos.app.MenuDialogListAdapter;

import com.smartisanos.ideapills.common.util.CommonUtils;
import com.smartisanos.ideapills.common.util.MutiTaskHandler;
import com.smartisanos.ideapills.common.util.PackageUtils;

import com.smartisanos.ideapills.common.util.TaskHandler;
import com.smartisanos.sara.SaraApplication;
import com.smartisanos.sara.R;
import com.smartisanos.sara.bubble.BubbleActivity;
import com.smartisanos.sara.storage.BubbleDataRepository;
import com.smartisanos.sara.voicecommand.VoiceCommondActivity;
import com.smartisanos.ideapills.common.util.PackageManagerCompat;

import smartisanos.app.voiceassistant.IVoiceAssistantCallback;
import smartisanos.api.IntentSmt;
import smartisanos.api.SettingsSmt;
import smartisanos.api.TextViewSmt;
import smartisanos.api.ActivityManagerSmt;

public class SaraUtils {
    private static final String TAG = "VoiceAss.SaraHelper";

    public static final boolean SUPPORT_MAX_BUBBLE_COUNT = false;
    private static final String ALARM_ALERT_VISIBLE = "alarm_alert_visible";

    public enum WaveType {
        START_WAVE, RESULT_WAVE
    }
    public enum BUBBLE_TYPE {
        PHONE_BUBBLE, SHELL_BUBBLE, SHELL_SEARCH, NONE
    }

    public static interface BaseViewListener {
        public void hideView(int from, PointF point, boolean finish, boolean needSleep);

        Activity getActivityContext();
    }


    public static interface BubbleViewChangeListener extends BaseViewListener {
        public void deleteVoice(GlobalBubble globalBubble);
        public void loadResultForKeyboard(GlobalBubble bubble, boolean isSmallEdit, boolean needScaleAnim);
        public void editView(boolean keyInvisible, boolean isSmallEdit);

        public int getBubbleSmallTranslation(int bubbleHeight);

        public int getBubbleLargeTranslation(int bubbleHeight);

        public int getBubbleKeyboardTranslation(int keyboardHeight);
    }

    public static interface ShowViewChangeListener {
        public void onShowBulletViewForDefaultSetting();
        public void onShowBulletView();
        public void onShowSearchView();
    }

    public interface BulletViewChangeListener {
        boolean isCurrentBulletShow();
        void refreshBulletContactViewHeight(int resultHeight);
    }

    public static interface DialogListener {
        public void onBubbleDelete();
        public void onBubbleRestore();
    }

    public static boolean checkFinish(View view, MotionEvent event) {
        if (view != null && view.getVisibility() == View.VISIBLE) {
            int[] l = {0, 0
            };
            view.getLocationInWindow(l);
            int left = l[0], top = l[1], bottom = top + view.getHeight(), right = left + view.getWidth();
            if (event.getX() > left && event.getX() < right
                    && event.getY() > top && event.getY() < bottom) {
                return false;
            } else {
                return true;
            }
        } else {
            return true;
        }
    }
    public static void goToSleepIfNoKeyguard(Context context, boolean needSleep, boolean force) {
        int alarmVisible = Settings.Global.getInt(context.getContentResolver(), ALARM_ALERT_VISIBLE, 0);
        if (needSleep && alarmVisible != 1) {
            KeyguardManager km = (KeyguardManager) context.getSystemService(Context.KEYGUARD_SERVICE);
            final boolean isKeyGuardLocked = km.isKeyguardLocked();
            final boolean isKeyGuardSecure = km.isKeyguardSecure();
            if (!isKeyGuardSecure || isKeyGuardLocked || force) {
                PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
                pm.goToSleep(SystemClock.uptimeMillis());
            }
        }
    }

    public static void copyText(Context context, CharSequence cs) {
        ClipboardManager cm = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData.Item dataItem = new ClipData.Item(cs);
        ClipData data = new ClipData(cs,new String[] {ClipDescription.MIMETYPE_TEXT_URILIST}, dataItem);
        cm.setPrimaryClip(data);
    }
    public static boolean isTopSelfApp(Context context) {
        ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        ComponentName component =  ActivityManagerSmt.getInstance().getTopActivity(am);
        if (component != null){
            return context.getPackageName().equals(component.getPackageName());
        }
        return false;
    }

    public static boolean isMainProcess(Context context) {
        int pid = android.os.Process.myPid();
        String processName = "";
        ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningAppProcessInfo process : am.getRunningAppProcesses()) {
            if (process.pid == pid) {
                processName = process.processName;
                break;
            }
        }
        return context.getPackageName().equals(processName);
    }

    public static void setMaxTextSize(TextView target, float size) {
        TextViewSmt textViewSmt = TextViewSmt.getInstance();
        textViewSmt.setMaxTextSize(target, size);
    }
    public static int caculateTextWidth(TextView tv) {
        Paint paint = new Paint();
        paint.setTextSize(tv.getTextSize());
        CharSequence text = tv.getText();
        int ret = 0;
        if (text != null) {
            ret = (int) paint.measureText(tv.getText().toString());
        }
        return ret;
    }

    public static int getMaxWidth(int widthExtra, TextView tv) {
        return widthExtra - caculateTextWidth(tv);
    }

    public static void setAnimatorScale(Context context) {
        ValueAnimator.setDurationScale(Settings.Global.getFloat(context.getContentResolver(),
          Settings.Global.ANIMATOR_DURATION_SCALE, 1.0f));
    }
    public static void showSimSelector(final Context context,
            final String number, final boolean isCall) {
        ArrayList<String> strList = new ArrayList<String>(2);
        strList.add(DSUtils.getSimName(context, DSUtils.SIM_1));
        strList.add(DSUtils.getSimName(context, DSUtils.SIM_2));
        ArrayList<View.OnClickListener> listenerList = new ArrayList<View.OnClickListener>(2);
        listenerList.add(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Agent.getInstance().onClick(context.getResources().getString(R.string.data_tracker_clipboard_count), "");
                callContact(context, number, DSUtils.SIM_1, isCall);
            }
        });
        listenerList.add(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                callContact(context, number, DSUtils.SIM_2, isCall);
            }
        });

        final MenuDialog dialog = new MenuDialog(context);
        dialog.setTitle(number);
        dialog.setAdapter(new MenuDialogListAdapter(context, strList, listenerList));
        dialog.show();
    }

    public static void dial(Context context, String number, boolean isClickBtn) {
        dial(context, number, isClickBtn, true);
    }

    public static void dial(Context context, String number, boolean isClickBtn, boolean isCall) {
        if (isClickBtn) {
            Agent.getInstance().onClick(context.getResources().getString(R.string.data_tracker_click_phone_count), "");
        } else {
            Agent.getInstance().onClick(context.getResources().getString(R.string.data_tracker_click_contact_name_count), "");
        }
        if (DSUtils.useDSFeature()) {
            showSimSelector(context, number, isCall);
        } else {
            callContact(context, number, DSUtils.SIM_NO, isCall);
        }
    }

    public static void callContact(Context context, String number, int simId, boolean isCall) {
        number = number.replace(" ", "").replace("-", "");
        Uri uri = Uri.fromParts(SaraConstant.SCHEME_TEL, number, null);
        final Intent intent = new Intent(isCall ? Intent.ACTION_CALL_PRIVILEGED : Intent.ACTION_DIAL, uri);
        if (isCall) {
            intent.setPackage(SaraConstant.PHONE_PACKAGE_NAME);
        } else {
            intent.setPackage(SaraConstant.PACKAGE_NAME_CONTACT);
        }
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        if (simId >= 0) {
            intent.putExtra(MultiSimAdapter.SLOT_KEY, simId);
        }
        startActivity(context, intent);
    }

    public static boolean isKeyguardSecureLocked() {
        KeyguardManager keyguardManager = (KeyguardManager) SaraApplication.getInstance().getApplicationContext()
                .getSystemService(Context.KEYGUARD_SERVICE);
        return keyguardManager.isKeyguardLocked() && keyguardManager.isKeyguardSecure();
    }

    public static boolean isKeyguardLocked() {
        KeyguardManager keyguardManager = (KeyguardManager) SaraApplication.getInstance().getApplicationContext()
                .getSystemService(Context.KEYGUARD_SERVICE);
        return keyguardManager.isKeyguardLocked();
    }

    public static void dismissKeyguardOnNextActivity() {
        if (isKeyguardLocked()) {
            CommonUtils.keyguardWaitingForActivityDrawn();
        }
    }

    public static boolean getLeftSlideLunchGloblePillEnabled(Context context) {
        return Settings.Global.getInt(context.getContentResolver(), SettingsSmt.Global.LEFT_SLIDE_LUNCH_GLOBAL_PILLS, 0) == 1;
    }
    public static void setLeftSlideLunchGloblePillEnabled(Context context, int value) {
        Settings.Global.putInt(context.getContentResolver(), SettingsSmt.Global.LEFT_SLIDE_LUNCH_GLOBAL_PILLS, value);
    }

    public static boolean leftSlideGuideEnable(Context context) {
        return isSettingEnable(context) && !getLeftSlideLunchGloblePillEnabled(context);
    }
    public static boolean getWebInputEnabled(Context context) {
        return Settings.Global.getInt(context.getContentResolver(), SettingsSmt.Global.VOICE_INPUT_WEB, 1) == 1;
    }

    public static boolean getLocalInputEnabled(Context context) {
        return Settings.Global.getInt(context.getContentResolver(), SettingsSmt.Global.VOICE_INPUT_LOCAL, 1) == 1;
    }
    public static String getSelectedLanguage(Context context) {
        String language =  Settings.Global.getString(context.getContentResolver(), SettingsSmt.Global.VOICE_LANGUAGE);
        return TextUtils.isEmpty(language) ? SaraConstant.DEFAULT_SELECT_LANGUAGE : language;
    }
    public static boolean isManualBackupTodo(Context context) {
        return SaraConstant.MAUNAL_BACKUP_TODO == Settings.Global.getInt(
                context.getContentResolver(), SettingsSmt.Global.VOICE_INPUT_TODO,
                SaraConstant.MAUNAL_BACKUP_TODO);
    }
    public static void setManualBackupTodo(Context context) {
        Settings.Global.putInt(context.getContentResolver(), SettingsSmt.Global.DEFAULT_BUBBLE_TYPE, GlobalBubble.COLOR_ORANGE);
        Settings.Global.putInt(context.getContentResolver(), SettingsSmt.Global.VOICE_INPUT_TODO, SaraConstant.MAUNAL_BACKUP_TODO);
    }
    public static int getDefaultBubbleColor(Context context) {
        int color = Settings.Global.getInt(context.getContentResolver(),
                        SettingsSmt.Global.DEFAULT_BUBBLE_TYPE, GlobalBubble.COLOR_BLUE);
        if (color != GlobalBubble.COLOR_SHARE) {
            return color;
        }
        if (BubbleDataRepository.isBubbleCanShare(context, false)) {
            return color;
        } else {
            return GlobalBubble.COLOR_BLUE;
        }
    }

    public static void setVoiceInputMode(Context context,int value){
        Settings.Global.putInt(context.getContentResolver(),
                SettingsSmt.Global.VOICE_INPUT_MODE, value);
    }
    public static int getCurrentLauncherMode(Context context) {
        return Settings.Global.getInt(context.getContentResolver(), SettingsSmt.Global.LAUNCHER_MODE,
                SettingsSmt.LAUNCHER_MODE_VALUE.LAUNCHER_MODE_GRIDS_9);
    }

    public static void buildWavRootPathAsync(final Context context) {
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                buildWavRootPath(context);
            }
        });
    }

    public static boolean buildWavRootPath(Context context) {
        String dataPath = context.getFilesDir().getAbsolutePath();
        File f = new File(dataPath + File.separator + "wav");
        return buildPath(f);
    }

    public static boolean isWavRootPathExist(Context context) {
        String dataPath = context.getFilesDir().getAbsolutePath();
        File f = new File(dataPath + File.separator + "wav");
        return f.exists() && f.isDirectory();
    }

    private static boolean buildPath(File rootFolder) {
        boolean result = false;
        if (rootFolder.exists() && rootFolder.isDirectory())
            return true;
        if (!rootFolder.exists()) {
            result = rootFolder.mkdirs();
            return result;
        }

        if (!rootFolder.isDirectory()) {
            if (rootFolder.delete()) {
                result = rootFolder.mkdirs();
                return result;
            } else {
                return false;
            }
        }
        return false;
    }

    public static void copyFileFrom2Data(String newPath, String oldPath) {
        InputStream inStream = null;
        FileOutputStream fs = null;
        try {
            int bytesum = 0;
            int byteread = 0;
            File oldfile = new File(oldPath);
            if (oldfile.exists()) {
                inStream = new FileInputStream(oldfile);
                if (!isWavRootPathExist(SaraApplication.getInstance())) {
                    LogUtils.d(TAG,"wav dirctiory is not exist");
                    buildWavRootPath(SaraApplication.getInstance());
                }
                fs = new FileOutputStream(newPath);
                byte[] buffer = new byte[1444];
                int length;
                while ((byteread = inStream.read(buffer)) != -1) {
                    bytesum += byteread;
                    fs.write(buffer, 0, byteread);
                }

            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (fs != null) {
                    fs.close();
                }
                if (inStream != null) {
                    inStream.close();
                }
            } catch (IOException e2) {
                e2.printStackTrace();
            }

        }
    }

    public static void pcm2Wav(String path) {
        WaveHeader mWaveHeader = new WaveHeader(WaveHeader.FORMAT_PCM, (short) 1, 16000, (short) 16);
        try {
            mWaveHeader.openRW(path);
            mWaveHeader.writeHeaderRm();
        } catch (IOException e) {
            LogUtils.e("switch pcm to wav fail "+ e);
        } finally {
            if (mWaveHeader != null){
                mWaveHeader.closeRm();
            }
        }
    }

    public static String pcmOrWav2Mp3(String path){
        LogUtils.d("path:" + path + ", ex:" + new File(path).exists());
        String newFilePath = path.replace(path.substring(path.lastIndexOf(".")), SaraConstant.MP3_FILE_SUFFIX);
        try {
            AudioEncoder mp3Encoder = new AudioEncoder(newFilePath);
            mp3Encoder.setFormat(16000, 1, 1);
            mp3Encoder.doEncode(path);
        } catch (Exception e) {
            e.printStackTrace();
        }
        new File(path).delete();
        return newFilePath;
    }
    public static final boolean renameFile(String originalFilePath,
            String finalFilePath) {
        boolean result = false;
        File originalFile = new File(originalFilePath);
        if (originalFile.isFile()) {
            File finalFile = new File(finalFilePath);
            result = originalFile.renameTo(finalFile);
        }
        return result;
    }

    public static void renameFile(Context context, String oldPath, String newPath) {
        File newFile = null;
        try {
            newFile = new File(newPath);
            File file = new File(oldPath);
            if (file.exists()){
                boolean ret = file.renameTo(newFile);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static String getWaveTmpPath(Context context) {
        return context.getFilesDir() + File.separator + "wav";
    }

    public static String formatContent2FilePath(Context context, Uri uri) {
        if (uri != null) {
            String contentPath = uri.getPath();
            return getWaveTmpPath(context) + contentPath.substring(contentPath.lastIndexOf("/"));
        }
        return null;
    }
    public static String formatFilePath2Content(Context context , String path) {
        return SaraConstant.CONTENT_FILE_PATH + path.substring(path.lastIndexOf("/")+1);
    }

    public static boolean isNetworkConnected() {
        try {
            IBinder b = ServiceManager.getService(Context.CONNECTIVITY_SERVICE);
            IConnectivityManager service = IConnectivityManager.Stub.asInterface(b);
            NetworkInfo activeNetwork = service.getActiveNetworkInfo();
            return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static void startActivity(Context context, Intent intent) {
        startActivity(context, intent, false);
    }

    public static void startActivity(Context context, Intent intent, boolean hold) {
        startActivity(context, intent, null, hold);
    }

    public static void startActivity(Context context, Intent intent, Bundle options, boolean hold) {
        if (!Intent.ACTION_CHOOSER.equals(intent.getAction())) {
            SaraUtils.dismissKeyguardOnNextActivity();
        }
        if (!(context instanceof Activity)) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            //fixed aMap can't re-search, need clear top
            if (null != intent.getComponent() && PackageUtils.COM_AUTONAVI_MINIMAP.equals(intent.getComponent().getPackageName())) {
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            }
        }
        if (!hold) {
            Intent i = new Intent(SaraConstant.ACTION_CHOOSE_RESULT);
            ComponentName componentName = intent.getComponent();
            if (null != componentName && PackageUtils.WECHAT_PACKAGE.equals(componentName.getPackageName())) {
                i.putExtra("delay", true);
            }
            context.sendBroadcast(i);
        }
        intent.putExtra(SaraConstant.HOLD_CURRENT_ACTIVITY, hold);
        try {
            context.startActivity(intent, options);
        } catch (Exception e) {
            LogUtils.e(TAG, "Can not start activity " + e);
        }
    }

    public static void startActivityForResult(Activity activity, Intent intent, int requestCode) {
        startActivityForResult(activity, intent, requestCode, null);
    }

    public static void startActivityForResult(Activity activity, Intent intent,
                                              int requestCode, Bundle options) {
        if (!Intent.ACTION_CHOOSER.equals(intent.getAction())) {
            SaraUtils.dismissKeyguardOnNextActivity();
        }
        try {
            activity.startActivityForResult(intent, requestCode, options);
        } catch (ActivityNotFoundException e) {
            LogUtils.e(TAG, "Can not start activity " + e);
            activity.finish();
        }
    }

    public static void startAttachementChoose(Activity activity) {
        try {
            Intent resultIntent = null;
            int[] anims = {0, smartisanos.R.anim.slide_down_out, smartisanos.R.anim.pop_up_in, 0};
            Intent i;
            if (SmtPCUtils.isValidExtDisplayId(activity)) {
                i = new Intent(Intent.ACTION_GET_CONTENT);
                i.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
                i.addCategory(Intent.CATEGORY_OPENABLE);
                i.addFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                i.setType("*/*");
                i.setComponent(new ComponentName("com.smartisanos.filemanager", "com.smartisan.filemanager.tablet.TabletActivity"));
                resultIntent = i;
            } else {
                i = new Intent(Intent.ACTION_GET_CONTENT);
                i.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
                i.addCategory(Intent.CATEGORY_OPENABLE);
                i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
                i.setType("*/*");
                i.putExtra(IntentSmt.EXTRA_SMARTISAN_ANIM_RESOURCE_ID, anims);
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP_MR1) {
                    resultIntent = Intent.createChooser(i, activity.getText(R.string.select_attachment_type), getAttachPickIntentSender(activity));
                } else {
                    resultIntent = Intent.createChooser(i, activity.getText(R.string.select_attachment_type));
                }
            }
            if (resultIntent != null) {
                resultIntent.putExtra("window_type", SaraConstant.BUBBLE_WINDOW_TYPE);
                resultIntent.putExtra("FLAG_SHOW_WHEN_LOCKED", true);
                resultIntent.putExtra(IntentSmt.EXTRA_SMARTISAN_ANIM_RESOURCE_ID, anims);
                startActivityForResult(activity, resultIntent,
                        SaraConstant.RESULT_PICK_ATTACHMENT);
            }
        } catch (ActivityNotFoundException e) {
            LogUtils.e(TAG, "Can not start activity " + e);
            activity.finish();
        }
    }

    public static IntentSender getAttachPickIntentSender(Activity activity) {
        Intent intent = new Intent(SaraConstant.ACTION_ATTACH_PICK_RESULT);
        PendingIntent broadcast = PendingIntent.getBroadcast(activity, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        return broadcast.getIntentSender();
    }

    private static final String PREVIEW_IMAGE_ATTACHMENT_APP_PACKAGE_NAME = "com.android.gallery3d";
    private static final String PREVIEW_IMAGE_ATTACHMENT_APP_ACTIVITY
            = PREVIEW_IMAGE_ATTACHMENT_APP_PACKAGE_NAME + ".app.Gallery";
    private static final String IMAGE_ATTACHMENT_URI_KEY = "ImageItemList";
    private static final String IMAGE_ATTACHMENT_VIEW_EDIT_KEY = "action_view_edit";
    private static final String PREVIEW_IMAGE_ATTACHMENT_DIALOG_TITLE = "delete_dialog_title_res";
    private static final String PREVIEW_IMAGE_ATTACHMENT_DIALOG_CONFIRM = "delete_dialog_confirm_res";
    private static final String PREVIEW_IMAGE_ATTACHMENT_PACKAGE_NAME = "package_name";

    public static void startImagePreview(Activity activity, GlobalBubbleAttach globalBubbleAttach, ArrayList<Uri> localUris) {
        if (globalBubbleAttach != null) {
            Uri localUri = globalBubbleAttach.getLocalUri();
            String contentType = globalBubbleAttach.getContentType();
            if (localUri != null && contentType != null && localUris != null) {
                Intent i = new Intent(Intent.ACTION_VIEW);
                i.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                ComponentName cn = new ComponentName(PREVIEW_IMAGE_ATTACHMENT_APP_PACKAGE_NAME, PREVIEW_IMAGE_ATTACHMENT_APP_ACTIVITY);
                i.setComponent(cn);
                i.putParcelableArrayListExtra(IMAGE_ATTACHMENT_URI_KEY, localUris);
                i.putExtra(IMAGE_ATTACHMENT_VIEW_EDIT_KEY, true);
                i.putExtra(PREVIEW_IMAGE_ATTACHMENT_DIALOG_TITLE,
                        R.string.preview_image_attachment_dialog_title);
                i.putExtra(PREVIEW_IMAGE_ATTACHMENT_DIALOG_CONFIRM,
                        R.string.preview_image_attachment_dialog_confirm);
                i.putExtra(PREVIEW_IMAGE_ATTACHMENT_PACKAGE_NAME,
                        activity.getPackageName());
                Bundle bundleOption = ActivityOptionsCompat.makeCustomAnimation(activity,
                        smartisanos.R.anim.pop_up_in, 0).toBundle();
                int[] anims = {0, smartisanos.R.anim.slide_down_out,smartisanos.R.anim.pop_up_in, 0};
                i.putExtra(IntentSmt.get_EXTRA_SMARTISAN_ANIM_RESOURCE_ID(), anims);
                AttachmentUtils.setIntentDataAndTypeAndNormalize(
                        i, localUri, contentType);
                SaraUtils.startActivityForResult(activity, i, SaraConstant.RESULT_IMAGE_PREVIEW, bundleOption);
            }
        } else {
            activity.finish();
        }
    }

    public static void startFilePreview(Activity activity, GlobalBubbleAttach globalBubbleAttach) {
        if (globalBubbleAttach != null) {
            Uri localUri = globalBubbleAttach.getLocalUri();
            String contentType = globalBubbleAttach.getContentType();
            if (contentType == null) {
                ToastUtil.showToast(activity, activity.getString(R.string.bubble_open_attachment_unknown_type));
                return;
            }
            if (localUri != null && contentType != null) {
                Intent i = new Intent(Intent.ACTION_VIEW);
                i.setDataAndType(globalBubbleAttach.getUri(), contentType);
                i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                Bundle bundleOption = ActivityOptionsCompat.makeCustomAnimation(activity,
                        smartisanos.R.anim.pop_up_in, 0).toBundle();
                int[] anims = {0, smartisanos.R.anim.slide_down_out, smartisanos.R.anim.pop_up_in, 0};
                i.putExtra(IntentSmt.get_EXTRA_SMARTISAN_ANIM_RESOURCE_ID(), anims);
                AttachmentUtils.setIntentDataAndTypeAndNormalize(
                        i, localUri, contentType);
                SaraUtils.startActivityForResult(activity, i, SaraConstant.RESULT_FILE_PREVIEW, bundleOption);
            }
        } else {
            activity.finish();
        }
    }

    public static void overridePendingTransition(Activity activity) {
        overridePendingTransition(activity, false);
    }

    public static void overridePendingTransition(Activity activity, boolean defaultNoneAnim) {
        Intent intent = activity.getIntent();
        if (intent != null) {
            int[] anims = intent.getIntArrayExtra(IntentSmt.get_EXTRA_SMARTISAN_ANIM_RESOURCE_ID());
            if (anims != null) {
                activity.overridePendingTransition(anims[0], anims[1]);
            } else if (defaultNoneAnim) {
                activity.overridePendingTransition(0, 0);
            }
        }
    }
    public static View inflateListTransparentHeader(Context context) {
        return LayoutInflater.from(context).inflate(R.layout.list_header_footer_view, null);
    }

    public static void recordFile(final Context ctx, final boolean offline, final String result) {
        String value = SharePrefUtil.getVoiceNameValue(ctx);
        int index = -1;
        if (!TextUtils.isEmpty(value)) {
            index = Integer.parseInt(value.split("_")[1]);
        }

        String context = StringUtils.trimPunctuation(result);
        String prefix = "";

        if (!TextUtils.isEmpty(context)) {
            int length = context.length() > 8 ? 8 : context.length();
            prefix = context.subSequence(0, length).toString();
        }
        String newFileName = SaraConstant.IDEA_PILL_SUFFIX + String.format("%03d", ++index) + "_" + StringUtils.getRandomCharacter() + "_" + prefix;
        final String path = SaraUtils.getWaveTmpPath(ctx) + File.separator + newFileName;

        TaskHandler.post(new Runnable() {
            @Override
            public void run() {
                copyFileFrom2Data(path + SaraConstant.PCM_FILE_SUFFIX, Environment.getExternalStorageDirectory() + "/sara/record.pcm");
                if (offline) {
                    copyFileFrom2Data(path + SaraConstant.PCM_FILE_SUFFIX + SaraConstant.BAK_FILE_SUFFIX, path + SaraConstant.PCM_FILE_SUFFIX);
                }
                pcmOrWav2Mp3(path + SaraConstant.PCM_FILE_SUFFIX);
                renameFile(ctx, path + SaraConstant.PCM_FILE_SUFFIX + SaraConstant.BAK_FILE_SUFFIX, path + SaraConstant.PCM_FILE_SUFFIX);
                String waveFile = SaraUtils.getWaveTmpPath(ctx) + SaraConstant.WAVE_FILE;
                SaraUtils.renameFile(waveFile, path + SaraConstant.MP3_FILE_SUFFIX + SaraConstant.WAVE_FILE_SUFFIX);
            }
        });
        SharePrefUtil.setVoiceNameValue(ctx, newFileName + SaraConstant.MP3_FILE_SUFFIX);
    }

    public static GlobalBubble toGlobalBubble(Context context, String result, int type, Uri uri, int color, long remindTime, long dueTime) {
        GlobalBubble bubble = new GlobalBubble();
        bubble.setText(result);
        bubble.setType(type);
        bubble.setUri(uri);
        if (uri == null) {
            bubble.setTimeStamp(System.currentTimeMillis());
        } else {
            File f = new File(formatContent2FilePath(context, uri));
            bubble.setTimeStamp(f.lastModified());
        }
        bubble.setSamplineRate(16000);
        bubble.setColor(color);
        bubble.setRemindTime(remindTime);
        bubble.setDueDate(dueTime);
        return bubble;
    }

    public static GlobalBubble toGlobalBubbleText(Context context,  int color) {
        GlobalBubble bubble = new GlobalBubble();
        bubble.setType(GlobalBubble.TYPE_TEXT);
        bubble.setColor(color);
        bubble.setTimeStamp(System.currentTimeMillis());
        return bubble;
    }

    public static Uri getUri(Context context) {
        String fileName = SharePrefUtil.getVoiceNameValue(context);
        if (!TextUtils.isEmpty(fileName)) {
            return Uri.parse(SaraConstant.CONTENT_FILE_PATH + fileName);
        }
        return null;
    }

    public static boolean isSettingEnable(Context context) {
        ContentResolver cr = context.getContentResolver();
        return Settings.Global.getInt(cr,
                SettingsSmt.Global.VOICE_INPUT, 1) == 1;
    }

    public static int getTodoOverType(Context context) {
        ContentResolver cr = context.getContentResolver();
        return Settings.Global.getInt(cr, SaraConstant.VOICE_TODO_OVER_CYCLE_TYPE, SaraConstant.VOICE_TODO_OVER_DAYLY);
    }

    public static void setTodoOverType(Context context, int value) {
        ContentResolver cr = context.getContentResolver();
        Settings.Global.putInt(cr, SaraConstant.VOICE_TODO_OVER_CYCLE_TYPE, value);
    }

    public static void finishActivity(Activity activity) {
        activity.finish();
        overridePendingTransition(activity);
        if (!isSettingEnable(activity)){
            activity.sendBroadcast(new Intent(SaraConstant.ACTION_FINISH_BUBBLE_ACTIVITY));
        }
    }

    private static boolean isAppInDoppelgangerStatus(Context context, String pkg) {
        if (pkg != null && context != null) {
            try {
                IPackageManager ipm = IPackageManager.Stub.asInterface(ServiceManager.getService("package"));
                PackageInfo info = ipm.getPackageInfo(pkg, 0, UserHandle.USER_DOPPELGANGER);
                if (info != null) {
                    return true;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    private static void jumpShareIntent(Context context, Intent intent) {
        if (intent == null) {
            // no response share intent
            ToastUtil.showToast(R.string.activity_not_found);
            return;
        }
        ActivityOptions bundleOption = ActivityOptions.makeCustomAnimation(context,
                smartisanos.R.anim.pop_up_in, 0);
        int[] anims = {0, smartisanos.R.anim.slide_down_out};
        boolean hold = false;
        ComponentName componentName = intent.getComponent();
        if (componentName != null && isAppInDoppelgangerStatus(context, componentName.getPackageName())) {
            hold = true;
            intent = Intent.createChooser(intent, null, getChooseIntentSender(context, null, true));
            intent.putExtra("FLAG_SHOW_WHEN_LOCKED", true);
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
        intent.putExtra(IntentSmt.EXTRA_SMARTISAN_ANIM_RESOURCE_ID, anims);
        startActivity(context, intent, bundleOption.toBundle(), hold);
    }

    public static void shareGlobal(Context context, GlobalBubble bubble, ComponentName componentName) {
        Intent intent = createShareIntent(context, bubble, componentName);
        if (componentName != null){
            jumpShareIntent(context, intent);
        } else {
            if (intent == null) {
                // no response share intent
                ToastUtil.showToast(R.string.activity_not_found);
                return;
            }
            Bundle bundleOption = ActivityOptionsCompat.makeCustomAnimation(context,
                    smartisanos.R.anim.pop_up_in, 0).toBundle();
            startActivity(context, intent, bundleOption, true);
        }
    }

    private static Intent chooserIntent(Intent srcIntent) {
        String shareType = srcIntent.getType();
        Intent targetedShare = new Intent(srcIntent.getAction());
        targetedShare.setType(shareType);
        String shareTextContent = srcIntent.getStringExtra(Intent.EXTRA_TEXT);
        if (!TextUtils.isEmpty(shareTextContent)) {
            targetedShare.putExtra(Intent.EXTRA_TEXT, shareTextContent);
        }

        if (srcIntent.getAction().equals(Intent.ACTION_SEND_MULTIPLE)) {
            ArrayList<Uri> multiUrls = srcIntent.getParcelableArrayListExtra(Intent.EXTRA_STREAM);
            if (multiUrls != null && !multiUrls.isEmpty()) {
                targetedShare.putParcelableArrayListExtra(Intent.EXTRA_STREAM, multiUrls);
            }
        } else {
            Bundle bundle = srcIntent.getExtras();
            if (bundle != null) {
                Uri shareExtraStream = bundle.getParcelable(Intent.EXTRA_STREAM);
                if (shareExtraStream != null) {
                    targetedShare.putExtra(Intent.EXTRA_STREAM, shareExtraStream);
                }
            }
        }

        targetedShare.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        return targetedShare;
    }

    public static Bundle bubble2Bundle(Context context, GlobalBubble bubble) {
        Bundle bundle = new Bundle();
        Uri uri = bubble.getUri();
        long createTime = BubbleSpeechPlayer.getInstance(context).getCreatedTime(bubble);
        if (uri != null) {
            String path = uri.getPath();
            bundle.putString("path", uri.toString());
            String name = path.substring(path.lastIndexOf("/") + 1, path.lastIndexOf("."));
            if (createTime > 0) {
                Date date = new Date(createTime);
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd", Locale.getDefault());
                String suffix = "_" + dateFormat.format(date);
                name = name + suffix;
            }
            bundle.putString("name", name);
        }
        bundle.putInt("samplingRate", 16000);
        bundle.putLong("createTime", createTime);
        bundle.putLong("duration", BubbleSpeechPlayer.getInstance(context).getDuration(bubble));
        return bundle;
    }

    public static Intent createShareIntent(Context context, GlobalBubble bubble, String packageName) {
        Intent targetIntent = new Intent(
                android.content.Intent.ACTION_SEND);
        targetIntent.setPackage(packageName);
        targetIntent.setType("text/plain");
        List<ResolveInfo> infos = context.getPackageManager().queryIntentActivitiesAsUser(targetIntent,
                PackageManagerCompat.DEFAULT_QUERY_FLAG, UserHandle.USER_OWNER);
        if (infos != null && !infos.isEmpty()) {
            ResolveInfo ri = infos.get(0);
            ComponentName cn = new ComponentName(ri.activityInfo.packageName, ri.activityInfo.name);
            return createShareIntent(context, bubble, cn);
        }

        targetIntent = new Intent(Intent.ACTION_MAIN);
        targetIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        targetIntent.setPackage(packageName);
        infos = context.getPackageManager().queryIntentActivities(targetIntent,
                PackageManagerCompat.DEFAULT_QUERY_FLAG);
        if (infos != null && infos.size() > 0) {
            ResolveInfo ri = infos.get(0);
            ComponentName cn = new ComponentName(ri.activityInfo.packageName, ri.activityInfo.name);
            return createShareIntent(context, bubble, cn);
        }

        return null;
    }

    public static Intent createShareIntent(Context context, Uri uri, String packageName) {
        Intent targetIntent = new Intent(Intent.ACTION_VIEW, uri);
        targetIntent.setPackage(packageName);
        targetIntent.putExtra("calling_package", SaraConstant.PILLS_PACKAGE_NAME);
        Intent resultIntent = null;
        try {
            List<ResolveInfo> list = context.getPackageManager().queryIntentActivities(targetIntent, 0);
            if (list == null || list.isEmpty()) {
                return null;
            }
        } catch (Exception e) {
            //ignore
        }
        resultIntent = Intent.createChooser(targetIntent, null);
        resultIntent.putExtra("FLAG_SHOW_WHEN_LOCKED", true);
        resultIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        return resultIntent;
    }

    public static Intent createShareIntent(Context context, GlobalBubble bubble, ComponentName componentName) {
        List<Intent> targetedShareIntents = new ArrayList<Intent>();
        Intent targetIntent = new Intent(Intent.ACTION_SEND);
        targetIntent.setType("text/plain");
        targetIntent.putExtra(Intent.EXTRA_TEXT,  bubble.getText());
        targetIntent.putExtra("calling_package", SaraConstant.PILLS_PACKAGE_NAME);
        Intent resultIntent = null;
        if (componentName == null) {
            targetedShareIntents.add(targetIntent);

            Intent copyIntent = new Intent("com.smartisanos.sara.intent.action.COPY_TEXT");
            List<ResolveInfo> copyInfo = context.getPackageManager().queryIntentActivities(
                    copyIntent, PackageManager.MATCH_DEFAULT_ONLY);
            if (!copyInfo.isEmpty()) {
                copyIntent.putExtra(Intent.EXTRA_TEXT, bubble.getText());
                ResolveInfo info = (ResolveInfo) copyInfo.get(0);
                copyIntent.setComponent(new ComponentName(info.activityInfo.packageName,
                        info.activityInfo.name));
                copyIntent.setPackage(info.activityInfo.packageName);
                targetedShareIntents.add(new LabeledIntent(copyIntent, info.activityInfo.packageName,
                        info.loadLabel(context.getPackageManager()), info.icon));
            }

            if (bubble.getType() != GlobalBubble.TYPE_TEXT && bubble.getUri() != null) {
                Intent targetIntent2 = new Intent(SaraConstant.ACTION_SHARE_RECORD);
                targetIntent2.putExtra(SaraConstant.SHARE_AUDIO_KEY, bubble2Bundle(context, bubble));
                targetIntent2.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                targetIntent2.setPackage(SaraConstant.RECORDER_PACKAGE_NAME);
                List<ResolveInfo> resInfo2 = context.getPackageManager().queryIntentActivities(targetIntent2, PackageManager.MATCH_DEFAULT_ONLY);
                if (!resInfo2.isEmpty()) {
                    ResolveInfo info = (ResolveInfo) resInfo2.get(0);
                    targetIntent2.setComponent(new ComponentName(info.activityInfo.packageName, info.activityInfo.name));
                    targetIntent2.setPackage(info.activityInfo.packageName);
                    targetedShareIntents.add(new LabeledIntent(targetIntent2,
                            info.activityInfo.packageName, info.loadLabel(context.getPackageManager()), info.icon));
                }
            }

            if (targetedShareIntents.size() > 0) {
                // no need to mark to used when share now
                resultIntent = Intent.createChooser(targetedShareIntents.remove(0), null, getChooseIntentSender(context, bubble, true));
                LabeledIntent[] li = targetedShareIntents.toArray(new LabeledIntent[targetedShareIntents.size()]);
                resultIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, li);
                resultIntent.putExtra("FLAG_SHOW_WHEN_LOCKED", true);
                resultIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                if (SmtPCUtils.isValidExtDisplayId(context)) {
                    resultIntent.setPackage("smartisanos");
                }
            }
        } else {
            targetIntent.setComponent(componentName);
            try {
                List<ResolveInfo> list = context.getPackageManager().queryIntentActivities(targetIntent, 0);
                if (list == null || list.isEmpty()) {
                    return null;
                }
            } catch (Exception e) {
                //ignore
            }
            resultIntent = targetIntent;
            resultIntent.putExtra("FLAG_SHOW_WHEN_LOCKED", true);
        }
        resultIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        int[] anims = {0, smartisanos.R.anim.slide_down_out,smartisanos.R.anim.pop_up_in, 0};
        resultIntent.putExtra(IntentSmt.EXTRA_SMARTISAN_ANIM_RESOURCE_ID, anims);
        return resultIntent;
    }

    public static IntentSender getChooseIntentSender(Context context, GlobalBubble bubble, boolean delayFinish) {
        Intent intent = new Intent(SaraConstant.ACTION_CHOOSE_RESULT);
        if (bubble != null) {
            intent.putExtra("usedBubble",bubble);
            intent.putExtra("delay", delayFinish);
        }
        PendingIntent broadcast = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        return broadcast.getIntentSender();
    }

    public static boolean hasNoPowerKey() {
        return DeviceType.isOneOf(DeviceType.T2, DeviceType.M1, DeviceType.M1L);
    }

    public static boolean isVirtualHomeKey() {
        return DeviceType.is(DeviceType.U1) || DeviceType.isSmartKeyProduct();
    }

    public static boolean isLowConfigDevice() {
        return DeviceType.isOneOf(DeviceType.T1, DeviceType.T2, DeviceType.U1);
    }

    public static boolean isDeltaStartDelay(){
        return DeviceType.isOneOf(DeviceType.M1L, DeviceType.M1);
    }
    public static int getPillsWindowLayer() {
        if (DeviceType.isOneOf(DeviceType.T2, DeviceType.U1)) {
            return 60000;
        } else if (DeviceType.is(DeviceType.T1)) {
            return 50000;
        } else {
            return 70000;
        }
    }

    public static int getSmartKeyCode() {
        return DeviceType.isOneOf(DeviceType.TRIDENT, DeviceType.OCEAN) ? 286 : 284;
    }

    public static boolean is16OGridsOrigLauncher() {
        return !DeviceType.isSmartKeyProduct();
    }
    public static boolean isLeftPopBubble(){
        return DeviceType.isSmartKeyProduct() && !BubbleActivity.sIsHeadSetOrBluetooth;
    }

    public static boolean isChineseLocale(){
        Locale currentLocale = Locale.getDefault();
        return Locale.SIMPLIFIED_CHINESE.equals(currentLocale) || Locale.TRADITIONAL_CHINESE.equals(currentLocale);
    }

    //this function is used to check is there any recorder running
    public static boolean checkRecorder() {
        for (int i = 0; i < AudioSystem.STREAM_TTS; i++) {
            if (AudioSystem.isSourceActive(i)) {
                return true;
            }
        }
        return false;
    }
    public static String getLookupKey(Context context, long contactId) {
        Uri contactUri = ContentUris.withAppendedId(Contacts.CONTENT_URI, contactId);
        final Cursor c = context.getContentResolver().query(contactUri,
                new String[] { Contacts.LOOKUP_KEY }, null, null, null);
        if (c == null) {
            return "";
        }
        String lookupKey = "";
        try {
            if (c.moveToFirst()) {
                lookupKey = c.getString(0);
            }
        } finally {
            c.close();
        }
        return lookupKey;
    }
    public static boolean checkAPP(Context context,String packageName) {
        try {
            context.getPackageManager()
                    .getApplicationInfo(packageName,
                            PackageManager.GET_UNINSTALLED_PACKAGES);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
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

    public static void killSelf(final Context context) {
        MutiTaskHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
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
        }, 200);
    }
    public static boolean sdkBeforeMarshmallow() {
        if (Build.VERSION.SDK_INT < 23) {
            return true;
        }
        return false;
    }
    public static boolean isKillSelf(Context context){
        if (sdkBeforeMarshmallow() && !isSettingEnable(context)) {
            return true;
        }
        return false;
    }
    public static void stopAudioPlay (final Context context){
        MutiTaskHandler.post(new Runnable() {
            @Override
            public void run() {
                AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
                if (audioManager.isMusicActive()) {
                    audioManager.requestAudioFocus(null, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT);
                 }
                if (BubbleSpeechPlayer.getInstance(context).isPlaying()) {
                    BubbleSpeechPlayer.getInstance(context).stop();
                }
            }
        });
    }

    public static boolean isBlindMode() {
        return isBlindModeOpen() && VoiceCommondActivity.isActivityExist;
    }

    public static boolean isBlindModeOpen() {
        ContentResolver cr = SaraApplication.getInstance().getContentResolver();
        return Settings.Global.getInt(cr,
                SettingsSmt.Global.VOICE_COMMAND_STATE, 0) == 1;
    }

    public static String formatResultString(String str) {
        if(str.isEmpty()) {
            return str;
        }
        return str.replaceAll("[\\p{Punct}\\p{Space}]+", "");
    }

    public static boolean isGlobalVibrateOn(Context context) {
        return Settings.Global.getInt(context.getContentResolver(),
                SettingsSmt.Global.GLOBAL_VIBRATION_ENABLED, 1) == 1;
    }

    public static boolean isSquareLagMode(Context context) {
        int mode = Settings.Global.getInt(context.getContentResolver(), "navigation_bar_mode", -1);
        return mode == 1;
    }

    public static boolean isSquareLagModeBottomPop(Context context) {
        int fixed = Settings.Global.getInt(context.getContentResolver(), "spacial_nav_bar_state", 0);
        return fixed == 1;
    }

    public static boolean isOneStepMode(Context context) {
        return SidebarUtils.isSidebarShowing(context);
    }

    public static boolean isWindowInthumbMode(Context context) {
        WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        return windowManager.isWindowInthumbMode();
    }

    public static boolean isNavigationBarMode(Context context) {
        int barMode = Settings.Global.getInt(context.getContentResolver(), "nav_fixed_mode", 0);
        return barMode == 0;
    }
    /**
     * use bluetooth start sara, modify the setting value
     * @param context
     * @param value 0 is end record, 1 is start record
     */
    public static void setBluetoothValue(Context context, int value) {
        LogUtils.d(BubbleActivity.TAG, "setBluetoothValue value = " + value);
        Settings.System.putInt(context.getContentResolver(), "capsule_state", value);
    }

    public static void toDoOver(TextView view, boolean over) {
        if (over) {
            view.setPaintFlags(view.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            view.setAlpha(0.4f);
        } else {
            view.setPaintFlags(view.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
            view.setAlpha(1.0f);
        }
        view.invalidate();
    }

    public static long getFirstDayOfYear() {
        Calendar current = Calendar.getInstance();
        current.setTimeInMillis(System.currentTimeMillis());
        current.setTimeZone(TimeZone.getTimeZone("GMT+8"));
        Calendar cal = Calendar.getInstance();
        cal.clear();
        cal.set(current.get(Calendar.YEAR), 0, 0, 0, 0, 0);
        return cal.getTimeInMillis();
    }

    public static boolean allAttachIsImage(List<GlobalBubbleAttach> mAttachments) {
        if(mAttachments != null || mAttachments.size() > 0) {
            for (GlobalBubbleAttach item : mAttachments) {
                if (item.getType() != GlobalBubbleAttach.TYPE_IMAGE) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    public static String getHintText(Context context, List<GlobalBubbleAttach> mAttachments) {
        if (mAttachments == null)
            return "";
        final int count = mAttachments.size();
        if (allAttachIsImage(mAttachments)) {
            return String.format(context.getResources().getQuantityString(
                    R.plurals.bubble_image_count_attach, count),
                    count);
        } else {
            return String.format(context.getResources().getQuantityString(
                    R.plurals.bubble_count_attach, count),
                    count);
        }
    }

    public static boolean isDrawerEnable(Context context) {
        return Settings.Global.getInt(context.getContentResolver(), SaraConstant.APP_DRAWER_ENABLE, 1) == 1;
    }

    public static int getLeftBubbleBottom(Context context) {
        Resources res = context.getResources();
        int bottomMargin = 0;
        if (isWindowInthumbMode(context)) {
            if (isSquareLagModeBottomPop(context)) {
                bottomMargin = res.getDimensionPixelSize(R.dimen.window_thumb_square_mode_left_wave_result_margin_bottom);
            } else {
                switch (DeviceType.THIS) {
                    case OSCAR:
                        bottomMargin = res.getDimensionPixelSize(R.dimen.window_thumb_mode_left_wave_result_margin_bottom_oscar);
                        break;
                    case TRIDENT:
                        bottomMargin = res.getDimensionPixelSize(R.dimen.window_thumb_mode_left_wave_result_margin_bottom_trident);
                        break;
                    default:
                        bottomMargin = res.getDimensionPixelSize(R.dimen.window_thumb_mode_left_wave_result_margin_bottom);
                        break;
                }
            }
        } else {
            if (isSquareLagMode(context)) {
                if (isSquareLagModeBottomPop(context)) {
                    bottomMargin = res.getDimensionPixelSize(R.dimen.square_mode_bottom_pop_left_wave_result_margin_bottom);
                } else {
                    bottomMargin = res.getDimensionPixelSize(R.dimen.square_mode_left_wave_result_margin_bottom);
                }
            } else {
                switch (DeviceType.THIS) {
                    case OSCAR:
                        bottomMargin = res.getDimensionPixelSize(R.dimen.left_wave_result_margin_bottom_oscar);
                        break;
                    case TRIDENT:
                        bottomMargin = res.getDimensionPixelSize(R.dimen.left_wave_result_margin_bottom_trident);
                        break;
                    default:
                        bottomMargin = res.getDimensionPixelSize(R.dimen.left_wave_result_margin_bottom);
                        break;
                }
                if (isOneStepMode(context)) {
                    bottomMargin += res.getDimensionPixelSize(R.dimen.onestep_mode_wave_result_margin_bottom_revise);
                }
            }
        }
        return bottomMargin;
    }

    public static BUBBLE_TYPE getBubbleType(Context context) {
        String type = SharePrefUtil.getString(context, SaraConstant.SAVE_SEARCH_TYPE, "");
        if (!TextUtils.isEmpty(type)) {
            if (type.equals(BUBBLE_TYPE.SHELL_SEARCH.name())) {
                return BUBBLE_TYPE.SHELL_SEARCH;
            }
            if (type.equals(BUBBLE_TYPE.SHELL_BUBBLE.name())) {
                return BUBBLE_TYPE.SHELL_BUBBLE;
            }

        }
        return BUBBLE_TYPE.NONE;
    }

    public static void setSearchType(Context context, String type) {
        SharePrefUtil.savePref(context, SaraConstant.SAVE_SEARCH_TYPE, type);
    }

    public static void startInstallApp(Context context, String packageName) {
        SaraUtils.dismissKeyguardOnNextActivity();
        try {
            Uri uri = Uri.parse("content://com.smartisanos.appstore.theme_download");
            Bundle extra = new Bundle();
            extra.putString("pkg", packageName);
            context.getContentResolver().call(uri, "openApp", null, extra);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void jumpToAppStore(Context context, String packageName) {
        SaraUtils.dismissKeyguardOnNextActivity();
        Intent intent = new Intent(Intent.ACTION_VIEW);
        String str = "smartisan://smartisan.com/details?id=" + packageName;
        intent.setData(Uri.parse(str));
        try {
            intent.putExtra("from_package",context.getPackageName());
            context.startActivity(intent);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void closeSystemDialog() {
        try {
            ActivityManagerNative.getDefault().closeSystemDialogs("shortcuts");
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

}
