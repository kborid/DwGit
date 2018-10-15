package com.smartisanos.sara.voicecommand;

import android.app.Activity;
import android.app.ActivityOptions;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.media.AudioManager;
import android.net.Uri;
import android.speech.tts.TextToSpeech;
import android.text.TextUtils;

import com.smartisanos.sara.R;
import com.smartisanos.sara.util.LogUtils;
import com.smartisanos.sara.util.PinyinUtil;
import com.smartisanos.ideapills.common.util.UIHandler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

public class VoiceCommandUtils {

    public static final String TAG = "VoiceCommandUtils";
    public static final String PACKAGE_GAODE    = "com.autonavi.minimap";
    public static final String PACKAGE_WECHAT   = "com.tencent.mm";
    public static final String PACKAGE_ALIPAY   = "com.eg.android.AlipayGphone";
    public static final String PACKAGE_BIGBANG  = "com.smartisanos.textboom";

    public static final String ACTION_TRANSLATE  = "smartisanos.intent.action.BOOM_TEXT_TRANSLATE";

    public static void goWeb(Context ctx, String url) {
        if (!url.startsWith("http")) {
            url = "http://" + url;
        }
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse(url));
        startActivityCommon(ctx, intent);
    }

    public static void goTranslate(Context ctx, String content) {
        Intent intent = new Intent(ACTION_TRANSLATE);
        intent.putExtra(Intent.EXTRA_TEXT, content.toString());
        intent.putExtra("caller_pkg", ctx.getPackageName());
        intent.putExtra("show_all_text", true);
        intent.setFlags(Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
                | Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivityWithApp(ctx, intent, VoiceCommandUtils.PACKAGE_BIGBANG);
    }

    public static void goNavigateHome(Context context, AddressInfo address) {
        Uri uri;
        if (address.point != null) {
            uri = Uri.parse(
                    String.format("amapuri://route/plan/?dlat=%1$.10f&dlon=%2$.10f&dname=%3$s&dev=0&t=1",
                            address.point.getLatitude(),
                            address.point.getLongitude(),
                            address.name));
        } else {
            uri = Uri.parse(String.format("androidamap://keywordNavi?sourceApplication=ideapills&keyword=%s&style=2",
                    address.name));
        }
        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
        intent.setPackage(PACKAGE_GAODE);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivityWithApp(context, intent, VoiceCommandUtils.PACKAGE_GAODE);
    }

    public static boolean startActivityCommon(Context ctx, Intent intent) {
        try {
            ActivityOptions compat = ActivityOptions.makeCustomAnimation(ctx, smartisanos.R.anim.pop_up_in, 0);
            if (!(ctx instanceof Activity)) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            }
            ctx.startActivity(intent, compat.toBundle());
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public static void startActivityWithApp(final Context ctx, Intent intent, final String pkg) {
        if (checkAppExist(ctx, pkg)) {
            startActivityCommon(ctx, intent);
        } else {
            showInstallDialog(ctx.getApplicationContext(), pkg);
        }
    }

    public static boolean checkAppExist(final Context ctx, String pkg) {
        PackageInfo packageInfo;
        try {
            packageInfo = ctx.getPackageManager().getPackageInfo(pkg, 0);
        }catch (PackageManager.NameNotFoundException e) {
            packageInfo = null;
            e.printStackTrace();
        }

        return packageInfo != null;
    }

    public static void showInstallDialogDelay(final Context ctx, final String pkg) {
        UIHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                showInstallDialog(ctx, pkg);
            }
        }, 500);
    }

    public static void showInstallDialog(final Context ctx, final String pkg) {
        String appName = "";
        if (PACKAGE_GAODE.equals(pkg)) {
            appName = ctx.getString(R.string.app_name_gaode);
        } else if (PACKAGE_WECHAT.equals(pkg)) {
            appName = ctx.getString(R.string.app_name_wechat);
        } else if (PACKAGE_ALIPAY.equals(pkg)) {
            appName = ctx.getString(R.string.app_name_alipay);
        } else if (PACKAGE_BIGBANG.equals(pkg)) {
            appName = ctx.getString(R.string.app_name_bigbang);
        }

        String title = ctx.getString(R.string.bubble_notice);
        String message = String.format(ctx.getString(R.string.app_not_install_message), appName);
        String ok = ctx.getString(R.string.install);
        String cancel = ctx.getString(R.string.cancel);
        Intent target = new Intent(Intent.ACTION_VIEW);
        target.setData(Uri.parse("smartisan://smartisan.com/details?id=" + pkg));

        DialogProxyActivity.showDialog(ctx, title, message, ok, cancel, target);
    }

    public static CharSequence matchCommand(CharSequence command, Iterable collections) {
        if (command != null && collections != null) {
            String cmd = command.toString();
            String pinyin = PinyinUtil.getPurePinYin(cmd);

            Iterator it = collections.iterator();
            while (it.hasNext()) {
                String s = it.next().toString();
                if (cmd.equalsIgnoreCase(s)) {
                    LogUtils.d(TAG, String.format("matchCommand ignore case: %1$s == %2$s", cmd, s));
                    return s;
                }

                if (pinyin.equalsIgnoreCase(PinyinUtil.getPurePinYin(s))) {
                    LogUtils.d(TAG, String.format("matchCommand pinyin: %1$s == %2$s", cmd, s));
                    return s;
                }
            }
        }

        LogUtils.d(TAG, String.format("matchCommand failed: " + command));
        return null;
    }

    public static boolean matchCommand(CharSequence command, CharSequence s) {
        if (TextUtils.isEmpty(command) || TextUtils.isEmpty(s)) {
            return TextUtils.equals(command, s);
        }

        String cmd = getStringOrEmpty(command);
        String target = getStringOrEmpty(s);

        if (cmd.equalsIgnoreCase(target)) {
            LogUtils.d(TAG, String.format("matchCommand ignore case: %1$s == %2$s", cmd, target));
            return true;
        }

        if (PinyinUtil.getPurePinYin(cmd).equalsIgnoreCase(PinyinUtil.getPurePinYin(target))) {
            LogUtils.d(TAG, String.format("matchCommand pinyin: %1$s == %2$s", cmd, target));
            return true;
        }

        LogUtils.d(TAG, String.format("matchCommand match failed: %1$s == %2$s", cmd, target));
        return false;
    }

    public static String getStringOrEmpty(CharSequence s) {
        if (s == null) {
            return "";
        }

        return s.toString();
    }

    public static CharSequence getActivityLabel(Context context, Intent intent) {
        List<CharSequence> results = getActivityLabels(context, intent);
        if (results.size() > 0) {
            return results.get(0);
        } else {
            return null;
        }
    }

    public static List<CharSequence> getActivityLabels(Context context, Intent intent) {
        List<CharSequence> results = new ArrayList<>();
        PackageManager pm = context.getPackageManager();
        List<ResolveInfo> infos = pm.queryIntentActivities(intent, 0);
        if (infos != null && infos.size() > 0) {
            for (ResolveInfo info : infos) {
                results.add(info.loadLabel(pm));
            }
        }

        return results;
    }

    public static HashMap<String, Intent> getAllLaunchIntents(Context context) {
        PackageManager pm = context.getPackageManager();
        HashMap<String, Intent> intents = new HashMap<>();

        // try to find a main launcher activity.
        Intent intentToResolve = new Intent(Intent.ACTION_MAIN);
        intentToResolve.addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> ris = pm.queryIntentActivities(intentToResolve, 0);

        if (ris != null) {
            for (ResolveInfo info : ris) {
                Intent intent = new Intent(intentToResolve);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.setClassName(info.activityInfo.packageName,
                        info.activityInfo.name);
                String label = info.loadLabel(pm).toString();
                intents.put(label, intent);
                LogUtils.d(TAG, "put launch intent : " + label + " - " + info.activityInfo.name);
            }
        }

        return intents;
    }

    public static void speak(Context ctx, String text, OnSpeakListener listener) {
        new Speaker(ctx, listener, true).speak(text);
    }

    public interface OnSpeakListener {
        void onSpeakDone(String utteranceId, boolean success);
    }

    public static class Speaker implements Runnable, TextToSpeech.OnInitListener {
        private Context mContext;
        private TextToSpeech mTts;
        private String mWords;

        private boolean mInited = false;
        private boolean mOnce = false;

        private AudioManager mAudioManager;

        private OnSpeakListener mListener;

        public Speaker(Context context, OnSpeakListener listener) {
            this(context, listener, false);
        }

        public Speaker(Context context, OnSpeakListener listener, boolean once) {
            mContext = context;
            mListener = listener;
            mOnce = once;
            mTts = new TextToSpeech(mContext, this);
            mAudioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        }

        /**
         * speak words and use the words as the utteranceId.
         */
        public void speak(String words) {
            speak(words, words);
        }

        /**
         * speak the words with the utteranceId.
         */
        public void speak(String words, String utteranceId) {
            this.mWords = words;

            if (mInited) {
                LogUtils.d(TAG, "speak : " + words);
                mTts.speak(words, TextToSpeech.QUEUE_ADD, null, utteranceId);
                mAudioManager.requestAudioFocus(null,
                        AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT);
            } else {
                LogUtils.d(TAG, "delay speak...");
                UIHandler.postDelayed(this, 500);
            }
        }

        public void shutdown() {
            mAudioManager.abandonAudioFocus(null);
            mTts.shutdown();
        }

        @Override
        public void onInit(int status) {
            if (status == TextToSpeech.SUCCESS
                    && mTts.isLanguageAvailable(Locale.getDefault()) == TextToSpeech.LANG_AVAILABLE) {
                mInited = true;
                mTts.setLanguage(Locale.getDefault());
                mTts.setOnUtteranceCompletedListener(new TextToSpeech.OnUtteranceCompletedListener() {
                    @Override
                    public void onUtteranceCompleted(String utteranceId) {
                        LogUtils.d(TAG, "onUtteranceCompleted");
                        if (mOnce) {
                            shutdown();
                        }
                        if (mListener != null) {
                            mListener.onSpeakDone(utteranceId, true);
                        }
                    }
                });
            } else {
                LogUtils.e(TAG, "onInit: FAILED!");
                shutdown();
                if (mListener != null) {
                    mListener.onSpeakDone("init", false);
                }
            }
        }

        @Override
        public void run() {
            speak(mWords);
        }
    }
}
