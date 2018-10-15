package com.smartisanos.ideapills.common.util;

import smartisanos.util.LogTag;

public class DDLog {
    public static final String TAG = "VoiceAss.dw.test";

    public static void d(String msg) {
        LogTag.d(TAG, msg);
    }

    public static void i(String msg) {
        LogTag.i(TAG, msg);
    }

    public static void e(String msg) {
        LogTag.e(TAG, msg);
    }
}
