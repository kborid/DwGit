package com.kborid.smart;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;

import com.kborid.library.common.LogUtils;

public class PRJApplication extends Application {
    private static final String TAG = "PRJApplication";
    private static PRJApplication instance = null;
    public static PRJApplication getInstance(){
        return instance;
    }
    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
    }

    private void initFunc1() {
        LogUtils.i(TAG, "initFunc1: ");
        try {
            LogUtils.i(TAG, "initFunc1: threadName = " + Thread.currentThread().getName());
            Thread.sleep(300);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void initFunc2() {
        LogUtils.i(TAG, "initFunc2: ");
        try {
            LogUtils.i(TAG, "initFunc2: threadName = " + Thread.currentThread().getName());
            Thread.sleep(200);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void initFunc3() {
        LogUtils.i(TAG, "initFunc3: ");
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                LogUtils.i(TAG, "initFunc3: run: start---");
                LogUtils.i(TAG, "initFunc3: threadName = " + Thread.currentThread().getName());
                try {
                    Thread.sleep(150);
                } catch (InterruptedException e){
                    e.printStackTrace();
                }
                LogUtils.i(TAG, "initFunc3: run: end---");
            }
        });
    }

    private static void reflectMethod(int params) {
        LogUtils.i(TAG, "reflectMethod() params = " + params);
    }
}
