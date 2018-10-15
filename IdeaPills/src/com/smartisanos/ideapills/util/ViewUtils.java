package com.smartisanos.ideapills.util;

import android.content.Context;
import android.content.res.Resources;
import android.util.DisplayMetrics;
import android.view.View;

public class ViewUtils {
    private static long Last_Click_Time = 0;
    private static final long MAX_CLICK_TIME = 350;

    public static void justBindOnClickListener(View view, View.OnClickListener listener, int[] ids) {
        for (int i = 0; i < ids.length; i++) {
            View target = view.findViewById(ids[i]);
            if (target != null) {
                target.setOnClickListener(listener);
            }
        }
    }

    public static boolean isClickAvailable() {
        boolean result = System.currentTimeMillis() - Last_Click_Time >= MAX_CLICK_TIME;
        Last_Click_Time = System.currentTimeMillis();
        return result;
    }

    public static int getSupposeHeight(View view) {
        int w = View.MeasureSpec.makeMeasureSpec(view.getLayoutParams().width, View.MeasureSpec.AT_MOST);
        int h = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
        view.measure(w, h);
        return view.getMeasuredHeight();
    }

    public static int getSupposeWidth(View view) {
        int w = View.MeasureSpec.makeMeasureSpec(view.getLayoutParams().width, View.MeasureSpec.AT_MOST);
        int h = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
        view.measure(w, h);
        return view.getMeasuredWidth();
    }

    public static int getSupposeWidthNoFixWidth(View view) {
        int w = View.MeasureSpec.makeMeasureSpec(view.getLayoutParams().width, View.MeasureSpec.UNSPECIFIED);
        int h = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
        view.measure(w, h);
        return view.getMeasuredWidth();
    }

    public static void setViewEnable(View view, boolean enable) {
        if (enable) {
            view.setEnabled(true);
            view.setAlpha(1.0f);
        } else {
            view.setEnabled(false);
            view.setAlpha(0.15f);
        }
    }

    public static int dp2px(float value) {
        final float scale = Resources.getSystem().getDisplayMetrics().density;
        return (int) (value * scale + 0.5f);
    }

    public static int dp2px(Context context, float value) {
        if (context == null) {
            return dp2px(value);
        }
        final DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
        return (int) (displayMetrics.density * value + 0.5f);
    }
}
