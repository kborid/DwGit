package com.smartisanos.voice.util;

import android.app.ActivityManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.media.AudioSystem;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.provider.Settings;
import android.text.TextUtils;

import com.smartisanos.voice.engine.XunfeiRecognizerEngine;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import smartisanos.api.SettingsSmt;
import smartisanos.app.voiceassistant.ParcelableMap;
public class VoiceUtils {
    static final LogUtils log = LogUtils.getInstance(VoiceUtils.class);
    private static final HandlerThread sWorkerThread = new HandlerThread("sara-update-database");
    static {
        sWorkerThread.start();
    }
    private static final Handler sDataBaseWorkerHandler = new Handler(sWorkerThread.getLooper());
    private static final int EOF = -1;
    private static String mCountryIso;

    public static SharedPreferences getSharePref(Context context){
        return context.getSharedPreferences(context.getPackageName(), Context.MODE_PRIVATE);
    }

    public static boolean isChineseLocale(){
        Locale currentLocale = Locale.getDefault();
        return Locale.SIMPLIFIED_CHINESE.equals(currentLocale) || Locale.TRADITIONAL_CHINESE.equals(currentLocale);
    }

    public static boolean isMobileConnectedOrConnecting(Context context) {
        ConnectivityManager connMgr = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo mobile = connMgr.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
        if (mobile.isAvailable() && mobile.isConnectedOrConnecting()) {
            return true;
        } else {
            return false;
        }
    }

    public static boolean isNetworkAvailable(Context context) {
        ConnectivityManager connMgr = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo net = connMgr.getActiveNetworkInfo();
        if (net != null && net.isConnectedOrConnecting()) {
            return true;
        } else {
            return false;
        }
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
    public static String getSelectedLanguage(Context context) {
        String language =  Settings.Global.getString(context.getContentResolver(), SettingsSmt.Global.VOICE_LANGUAGE);
        return TextUtils.isEmpty(language) ? VoiceConstant.DEFAULT_SELECT_LANGUAGE : language;
    }

    public static boolean isPackageExist(Context context, String pkgName) {
        boolean exist = false;
        if (context != null && !TextUtils.isEmpty(pkgName)) {
            try {
                if (context.getPackageManager().getPackageInfo(pkgName, 0) != null) {
                    exist = true;
                }
            } catch (Exception e) {
                log.w("package not exist: " + pkgName);
            }
        }

        return exist;
    }

    public static ParcelableMap getCurrentAllPackageLaunchCounts(Context context) {
        ParcelableMap parcelableMap = new ParcelableMap();
        if (Build.VERSION.SDK_INT >= VoiceConstant.LOLLIPOP_VERSION){
            try {

                  getUsageStatusWithHigherApiLevel(context,parcelableMap);
            } catch (Exception e) {
                 log.e("SDK INT bigger than 21");
            }
        } else {
              getUsageStatusWithLowerApiLevel(context,parcelableMap);
        }
        return parcelableMap;
    }
    private  static void getUsageStatusWithLowerApiLevel(Context context,ParcelableMap parcelableMap) {
        try {
            ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
            Class clazz = activityManager.getClass();
            Method m1 = clazz.getDeclaredMethod("getAllPackageLaunchCounts");
            parcelableMap.setMap((Map<String, Integer>) m1.invoke(activityManager));
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public static final int INTERVAL_YEARLY  = 0x00000003;
    private  static void getUsageStatusWithHigherApiLevel(Context context ,ParcelableMap parcelableMap) {
           Object  usageObject = context.getSystemService("usagestats");
        if (usageObject != null) {
            Calendar beginTime = Calendar.getInstance();
            beginTime.set(Calendar.YEAR, 2000);
            Calendar endTime = Calendar.getInstance();
            long begin = beginTime.getTimeInMillis();
            long end = endTime.getTimeInMillis();
            try {
                Class usageStatClass = usageObject.getClass();
                Method queryMethod = usageStatClass.getDeclaredMethod("queryUsageStats", int.class, long.class, long.class);
                List queryResult = (List) queryMethod.invoke(usageObject, INTERVAL_YEARLY, new Long(begin), new Long(end));
                Map<String, Integer> mUsageCountsTemp = new HashMap<String, Integer>();
                if (queryResult != null) {
                    List list = (List) queryResult;
                    for (Object result : list) {
                        if (result == null) {
                            continue;
                        }
                        Object usageStatsObj = result;
                        Class usageStatsClass = usageStatsObj.getClass();
                        Method getPackageNameM = usageStatsClass.getDeclaredMethod("getPackageName");
                        String pkg = (String) getPackageNameM.invoke(usageStatsObj);
                        Field mLaunchCountF = usageStatsClass.getDeclaredField("mLaunchCount");
                        int launchCount = mLaunchCountF.getInt(usageStatsObj);
                        mUsageCountsTemp.put(pkg, launchCount);
                    }
                    parcelableMap.setMap(mUsageCountsTemp);
                }
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    public static void buildGrammar(String lexicon){
        if (!VoiceConstant.CTA_ENABLE) {
            XunfeiRecognizerEngine mEngine = XunfeiRecognizerEngine.getInstance();
            mEngine.updateLexicon(lexicon);
        }
    }
}
