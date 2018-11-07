package com.kborid.smart.interpolator;

import android.annotation.TargetApi;
import android.os.Build;
import android.os.Looper;

import com.kborid.library.common.LogUtils;

@TargetApi(Build.VERSION_CODES.KITKAT)
public class CustomThread extends Thread {
    private int i = 0;
    @Override
    public void run() {
        super.run();
        Looper.prepare();
        LogUtils.d("CustomThread = " + i++);
        Looper.loop();
    }
}
