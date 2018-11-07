package com.kborid.smart.util;

import android.content.Context;
import android.util.DisplayMetrics;
import android.view.WindowManager;

import com.kborid.smart.PRJApplication;

public class ScreenUtils {

    public static int mScreenWidth;
    public static int mScreenHeight;
    public static float mDensity;

    public static void init() {
        DisplayMetrics dm = new DisplayMetrics();
        WindowManager wm = (WindowManager) PRJApplication.getInstance().getSystemService(Context.WINDOW_SERVICE);
        if (null != wm) {
            wm.getDefaultDisplay().getMetrics(dm);
            mScreenWidth = dm.widthPixels;
            mScreenHeight = dm.heightPixels;
            mDensity = dm.density;
        }
    }
}
