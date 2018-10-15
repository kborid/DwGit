package com.smartisanos.voice.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;

public class SharePrefUtil {
    private static String CURRENT_INDEX_KEY ="currentIndex";
    public static final void putString(Context context, String key, String value) {
        SharedPreferences sp = context.getSharedPreferences(context.getPackageName(), Context.MODE_PRIVATE);
        Editor editor = sp.edit();
        editor.putString(key, value);
        editor.apply();
    }
    public static final String getString(Context context, String key, String def) {
        SharedPreferences sp = context.getSharedPreferences(context.getPackageName(), Context.MODE_PRIVATE);
        return sp.getString(key, def);
    }
    
    public static final void putBoolean(Context context, String key, boolean value) {
        SharedPreferences sp = context.getSharedPreferences(context.getPackageName(), Context.MODE_PRIVATE);
        Editor editor = sp.edit();
        editor.putBoolean(key, value);
        editor.apply();
    }

    public static final boolean getBoolean(Context context, String key, boolean def) {
        SharedPreferences sp = context.getSharedPreferences(context.getPackageName(), Context.MODE_PRIVATE);
        return sp.getBoolean(key, def);
    }
    public static final void putInt(Context context, String key, int value) {
        SharedPreferences sp = context.getSharedPreferences(context.getPackageName(), Context.MODE_PRIVATE);
        Editor editor = sp.edit();
        editor.putInt(key, value);
        editor.apply();
    }
    public static final int getInt(Context context, String key, int value) {
        SharedPreferences sp = context.getSharedPreferences(context.getPackageName(), Context.MODE_PRIVATE);
        return sp.getInt(key, value);
    }
    
    public static final long getLong(Context context, String key, long def) {
        SharedPreferences sp = context.getSharedPreferences(context.getPackageName(), Context.MODE_PRIVATE);
        return sp.getLong(key, def);
    }
    public static final void putLong(Context context, String key, long value) {
        SharedPreferences sp = context.getSharedPreferences(context.getPackageName(), Context.MODE_PRIVATE);
        Editor editor = sp.edit();
        editor.putLong(key, value);
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
}
