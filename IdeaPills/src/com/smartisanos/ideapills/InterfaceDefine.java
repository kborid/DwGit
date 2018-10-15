package com.smartisanos.ideapills;

import android.provider.Settings;

import smartisanos.api.SettingsSmt;

public class InterfaceDefine {
    //settings
    public static final String SETTINGS_USER_SETUP_COMPLETE = Settings.Secure.USER_SETUP_COMPLETE;
    public static final String SETTINGS_VOICE_INPUT = SettingsSmt.Global.VOICE_INPUT;
    public static final String SETTINGS_FEATURE_PHONE_MODE = SettingsSmt.Global.FEATURE_PHONE_MODE;
    public static final String SETTINGS_DEFAULT_BUBBLE_COLOR = SettingsSmt.Global.DEFAULT_BUBBLE_TYPE;
    public static final String SETTINGS_TODO_OVER_CLEANING_CYCLE = SettingsSmt.Global.VOICE_TODO_OVER_CYCLE;
}
