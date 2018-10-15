package com.smartisanos.sara.bubble.revone;

import android.app.SmtPCUtils;
import android.content.Context;

public class ExtScreenConstant {
    public static final boolean CONTAINS_STATUS_NAVI_BAR = true;
    public static int STATUS_BAR_HEIGHT;
    public static int NAVIGATION_BAR_HEIGHT;
    public static final String LAUNCH_BUTTON_POS = "center_pos";
    public static final String LAUNCH_TYPE = "launch_type";
    public static final String DRAG_STATE = "drag_state";
    public static int LAUNCH_FROM_LONG_PRESS = 0;
    public static int LAUNCH_FROM_CLICK = 1;
    public static int GENERAL_ANIM_TIME = 250;
    public static int DROP_ANIM_TIME = 150;
    public static float SCALE_OUT_VALUE = 1.12f;
    public static String ACTION_CLOSE_BUBBLE = "android.intent.action.CLOSE_BUBBLE";

    public static void init(Context context) {
        STATUS_BAR_HEIGHT = SmtPCUtils.getSmtStatusBarPixelHeight(context);
        NAVIGATION_BAR_HEIGHT = SmtPCUtils.getSmtNavigationBarPixelHeight(context);
    }
}
