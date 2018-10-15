package com.kborid.library.common;

import android.os.Handler;
import android.os.Looper;

public class UIHandler {
    private static final Handler sHandler;

    static {
        sHandler = new Handler(Looper.getMainLooper());
    }

    public static Handler getHandler() {
        return sHandler;
    }

    public static boolean post(Runnable r) {
        return sHandler.post(r);
    }

    public static boolean postDelayed(Runnable r, long delayMillis) {
        return sHandler.postDelayed(r, delayMillis);
    }

    public static void removeCallbacks(Runnable r) {
        sHandler.removeCallbacks(r);
    }
}
