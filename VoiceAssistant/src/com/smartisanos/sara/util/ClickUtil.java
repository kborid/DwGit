package com.smartisanos.sara.util;

/**
 * util for click detect
 *
 */
public class ClickUtil {

    private static long mLastClick = 0l;

    public static int CLICK_GAP = 500;

    public static int CLICK_GAP_LONG = 800;

    private static boolean isFastClick(int click_gap){
        long curTime = System.currentTimeMillis();
        long gap = curTime - mLastClick;
        if(gap >0 && gap < click_gap){
            return true;
        }
        mLastClick = curTime;
        return false;
    }

    public static boolean isFastClick(){
        return isFastClick(CLICK_GAP);
    }

    public static boolean isFastClickLong(){
        return isFastClick(CLICK_GAP_LONG);
    }

}