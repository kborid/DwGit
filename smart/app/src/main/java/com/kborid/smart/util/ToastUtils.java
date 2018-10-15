package com.kborid.smart.util;

import android.widget.Toast;

import com.kborid.smart.PRJApplication;


public class ToastUtils {

    private static Toast mToast;

    public static void showToast(String msg, int duration) {
        if (null != mToast) {
            mToast.cancel();
            mToast = null;
        }

        mToast = Toast.makeText(PRJApplication.getInstance(), msg, duration);
        mToast.show();
    }

    public static void showToast(int stringId, int duration) {
        showToast(PRJApplication.getInstance().getString(stringId), duration);
    }

    public static void showToast(String msg) {
        showToast(msg, Toast.LENGTH_SHORT);
    }

    public static void showToast(int stringId) {
        showToast(stringId, Toast.LENGTH_SHORT);
    }
}
