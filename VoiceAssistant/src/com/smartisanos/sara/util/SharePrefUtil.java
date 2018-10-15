package com.smartisanos.sara.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;

import com.smartisanos.sara.SaraApplication;

public class SharePrefUtil {
    public static String KEY_USAGE_VIDEO_PLAYED = "key_usage_video_played";
    public static String KEY_OSCAR_LOWMEMORY_WARNED = "key_oscar_lowmemory_warned";

    public static final void savePref(Context context, String key, String value) {
        SharedPreferences sp = context.getSharedPreferences(context.getPackageName(), Context.MODE_PRIVATE);
        Editor editor = sp.edit();
        editor.putString(key, value);
        editor.apply();
    }

    public static final void savePref(Context context, String key, boolean value) {
        SharedPreferences sp = context.getSharedPreferences(context.getPackageName(), Context.MODE_PRIVATE);
        Editor editor = sp.edit();
        editor.putBoolean(key, value);
        editor.apply();
    }

    public static final void savePref(Context context, String key, long value) {
        SharedPreferences sp = context.getSharedPreferences(context.getPackageName(), Context.MODE_PRIVATE);
        Editor editor = sp.edit();
        editor.putLong(key, value);
        editor.apply();
    }

    public static final String getString(Context context, String key, String def) {
        SharedPreferences sp = context.getSharedPreferences(context.getPackageName(), Context.MODE_PRIVATE);
        return sp.getString(key, def);
    }

    public static final boolean getBoolean(Context context, String key, boolean def) {
        SharedPreferences sp = context.getSharedPreferences(context.getPackageName(), Context.MODE_PRIVATE);
        return sp.getBoolean(key, def);
    }

    public static final long getLong(Context context, String key, long def) {
        SharedPreferences sp = context.getSharedPreferences(context.getPackageName(), Context.MODE_PRIVATE);
        return sp.getLong(key, def);
    }

    public static final String getVoiceNameValue(Context context) {
        SharedPreferences sp = context.getSharedPreferences(context.getPackageName(), Context.MODE_PRIVATE);
        return sp.getString(SaraConstant.VOICE_NAME_KEY, "");
    }
    public static final void setVoiceNameValue(Context context,String fileName) {
        SharedPreferences sp = context.getSharedPreferences(context.getPackageName(), Context.MODE_PRIVATE);
        Editor editor = sp.edit();
        editor.putString(SaraConstant.VOICE_NAME_KEY, fileName);
        editor.apply();
    }

    public static final String getHomeAddr(Context context) {
        SharedPreferences sp = context.getSharedPreferences(context.getPackageName(), Context.MODE_PRIVATE);
        return sp.getString(SaraConstant.HOME_ADDRESS_KEY, "");
    }
    public static final void setHomeAddr(Context context,String fileName) {
        SharedPreferences sp = context.getSharedPreferences(context.getPackageName(), Context.MODE_PRIVATE);
        Editor editor = sp.edit();
        editor.putString(SaraConstant.HOME_ADDRESS_KEY, fileName);
        editor.apply();
    }

    public static final int getSearchInfoValue(Context context,String key, int defValue) {
        SharedPreferences sp = context.getSharedPreferences(context.getPackageName(), Context.MODE_PRIVATE);
        return sp.getInt(key, defValue);
    }
    public static final void setSearchInfoValue(Context context,String key, int defValue) {
        SharedPreferences sp = context.getSharedPreferences(context.getPackageName(), Context.MODE_PRIVATE);
        Editor editor = sp.edit();
        editor.putInt(key, defValue);
        editor.apply();
    }

    public static final void updateSharePrefByTime(int newDay){
        SaraApplication mSaraApplication = SaraApplication.getInstance();
        SharedPreferences sp = mSaraApplication.getSharedPreferences(mSaraApplication.getPackageName(), Context.MODE_PRIVATE);
        int mCurrentDisplayDay = sp.getInt(SaraConstant.PREF_KEY_CURRENT_DAY, -1);
        if (newDay == mCurrentDisplayDay) {
            return;
        }

        Editor editor = sp.edit();
        editor.putInt(SaraConstant.PREF_KEY_CURRENT_DAY, newDay);
        if (sp.getLong(SaraConstant.PREF_KEY_OLD_TIME, 0) != 0) {
            editor.putLong(SaraConstant.PREF_KEY_OLD_TIME, 0);
        }
        editor.putInt(SaraConstant.PREF_KEY_RECOGNIZE_NUM, 0);
        editor.apply();
    }
}
