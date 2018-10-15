package com.smartisanos.ideapills.util;

import android.os.Looper;
import android.os.Process;

public class ThreadVerify {
    private static final LOG log = LOG.getInstance(ThreadVerify.class);

    public static void enforceUiThread() {
        if (Looper.getMainLooper() != Looper.myLooper()) {
            throw new IllegalArgumentException("verifyThread failed");
        }
    }
}