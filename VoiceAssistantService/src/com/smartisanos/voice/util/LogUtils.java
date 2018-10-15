package com.smartisanos.voice.util;

import android.util.Log;
import android.os.SystemProperties;

public class LogUtils {
    public static final boolean DEBUG = (SystemProperties.getInt("ro.debuggable", 0) == 1);
    public static final String TAG = "VoiceAssistantService";

    private String mClassName = null;

    public static LogUtils getInstance(Class owner) {
        return new LogUtils(owner);
    }

    private LogUtils(Class owner) {
        if (owner == null) {
            throw new IllegalArgumentException("LOG must be init by class object");
        }
        mClassName = owner.getSimpleName();
    }

    public static void trace() {
        Exception ex = new Exception();
        Log.e(TAG, "########## trace begin  ##########");
        Log.e(TAG, Log.getStackTraceString(ex));
        Log.e(TAG, "########## trace finish ##########");
    }

    public void d(String info) {
        if (DEBUG)
            Log.d(TAG, getLogString(info));
    }

    public void i(String info) {
        if (DEBUG)
            Log.i(TAG, getLogString(info));
    }

    public void infoRelease(String info) {
        Log.i(TAG, getLogString(info));
    }

    public void e(String info) {
        Log.e(TAG, getLogString(info));
    }

    public void e(String info, Throwable throwable) {
        Log.e(TAG, getLogString(info), throwable);
    }

    public void w(String info) {
        Log.w(TAG, getLogString(info));
    }
    private String getLogString(String info) {
        if (mClassName != null) {
            return mClassName + " : " + info;
        }
        return info;
    }

    public String getReleaseString(String content) {
        if (DEBUG) {
            return StringUtils.getStringOrEmpty(content);
        }

        return "";
    }
}
