package com.smartisanos.sara.lock.util;

import android.app.KeyguardManager;
import android.telephony.SignalStrength;
import android.widget.TextView;

import com.android.internal.widget.LockPatternUtils;

import smartisanos.api.KeyguardManagerSmt;
import smartisanos.api.LockPatternUtilsSmt;
import smartisanos.api.SettingsSmt;
import smartisanos.api.SignalStrengthSmt;
import smartisanos.api.TextViewSmt;

public class SmartisanApi {
    //Settings.Global
    public static final String FACE_DATA_LOCK_SCREEN_ENABLE = SettingsSmt.Global.FACE_DATA_LOCK_SCREEN_ENABLE;
    public static final String USE_FINGERPRINT_IN_LOCKSCREEN = SettingsSmt.Global.USE_FINGERPRINT_IN_LOCKSCREEN;

    //TextView
    public static void setMaxTextSize(TextView target, float size) {
        TextViewSmt.getInstance().setMaxTextSize(target, size);
    }

    public static void setHiddenContextMenuItem(TextView target, int flag) {
        TextViewSmt.getInstance().setHiddenContextMenuItem(target, flag);
    }

    //SignalStrength
    public static int getSmartisanLevel(SignalStrength target) {
        return SignalStrengthSmt.getInstance().getSmartisanLevel(target);
    }

    public static int getSmartisanCdmaLevel(SignalStrength target) {
        return SignalStrengthSmt.getInstance().getSmartisanCdmaLevel(target);
    }

    public static int getSmartisanEvdoLevel(SignalStrength target) {
        return SignalStrengthSmt.getInstance().getSmartisanEvdoLevel(target);
    }

    //LockPatternUtils
    public static int getKeyguardStoredPasswordQualityWrapWifiAuth(LockPatternUtils target) {
        return LockPatternUtilsSmt.getInstance().getKeyguardStoredPasswordQualityWrapWifiAuth(target);
    }

    //KeyguardManager
    public static boolean isKeyguardVerified(KeyguardManager target) {
        return KeyguardManagerSmt.getInstance().isKeyguardVerified(target);
    }

    public static void verifyKeyguardSecurely(KeyguardManager target, final smartisanos.api.OnKeyguardVerifyResultSmto callback, String pwd) {
        KeyguardManagerSmt.getInstance().verifyKeyguardSecurely(target, callback, pwd);
    }

    public static interface OnKeyguardVerifyResultSmto extends smartisanos.api.OnKeyguardVerifyResultSmto {
    }


}
