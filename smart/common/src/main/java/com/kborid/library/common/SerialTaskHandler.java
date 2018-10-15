package com.kborid.library.common;

import android.os.Handler;
import android.os.HandlerThread;

public class SerialTaskHandler {

    private static final HandlerThread sWorkerThread = new HandlerThread("SerialTaskHandler");
    static {
        sWorkerThread.start();
    }

    private static final Handler mWorker = new Handler(sWorkerThread.getLooper());

    public static boolean post(Runnable r) {
        return mWorker.post(r);
    }

    public static boolean postDelayed(Runnable r, long delayMillis) {
        return mWorker.postDelayed(r, delayMillis);
    }

    public static void removeCallbacks(Runnable r) {
        mWorker.removeCallbacks(r);
    }
}