package com.smartisanos.sara.bullet.util;

import android.content.Context;
import android.util.DisplayMetrics;

import com.smartisanos.sara.SaraApplication;

public class DisplayUtils {

    public static Context mContext;
    public static int mScreenWidth;
    public static int mScreenHeight;

    public static float mDensity;
    public static float mScaleDensity;
    public static float mXdpi;
    public static float mYdpi;
    public static int mDensityDpi;

    static {
        init(SaraApplication.getInstance());
    }

    public static void init(Context context) {
        if (null == context) {
            return;
        }
        mContext = context.getApplicationContext();
        DisplayMetrics dm = context.getResources().getDisplayMetrics();
        mScreenWidth = dm.widthPixels;
        mScreenHeight = dm.heightPixels;
        mDensity = dm.density;
        mScaleDensity = dm.scaledDensity;
        mXdpi = dm.xdpi;
        mYdpi = dm.ydpi;
        mDensityDpi = dm.densityDpi;
    }

    public static int dp2px(float dipValue) {
        return (int) (dipValue * mDensity + 0.5f);
    }

    public static int dp2px(Context context, float dipValue) {
        DisplayMetrics dm = context.getResources().getDisplayMetrics();
        return (int) (dipValue * dm.density + 0.5f);
    }

    public static int px2dip(float pxValue) {
        return (int) (pxValue / mDensity + 0.5f);
    }

    public static int sp2px(float spValue) {
        return (int) (spValue * mScaleDensity + 0.5f);
    }
}
