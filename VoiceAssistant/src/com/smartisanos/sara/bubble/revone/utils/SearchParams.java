package com.smartisanos.sara.bubble.revone.utils;

import android.graphics.Point;

public class SearchParams {

    static int ACTIVITY_DISPLAY_GAP_X = 36;
    static int ACTIVITY_DISPLAY_GAP_Y = 36;
    static int ACTIVITY_DISPLAY_PADDING_X = 84;
    static int ACTIVITY_DISPLAY_PADDING_TOP = 90;
    static int ACTIVITY_DISPLAY_PADDING_BOTTOM = 80;

    private static final int DEF_COLUMN_IN_SCREEN = 7;
    private static final int DEF_LOCAL_SEARCH_SPAN = 1;
    private static final int DEF_WEB_SEARCH_SPAN = 2;

    // display as 16:9
    static final float DISPLAY_SCALE = 1.778f;

    private static int LOCAL_SEARCH_SPAN = DEF_LOCAL_SEARCH_SPAN;
    private static int WEB_SEARCH_SPAN = DEF_WEB_SEARCH_SPAN;

    private static int COLUMN_IN_SCREEN = DEF_COLUMN_IN_SCREEN;
    private static int ROW_IN_SCREEN = 2;


    public static void init(Point screenSize) {
        if (screenSize.x <= 1920 && screenSize.y <= 1080) {
            COLUMN_IN_SCREEN = DEF_COLUMN_IN_SCREEN - 1;
            LOCAL_SEARCH_SPAN = 2;
            ACTIVITY_DISPLAY_PADDING_TOP = 85;
        } else {
            COLUMN_IN_SCREEN = DEF_COLUMN_IN_SCREEN;
        }
    }

    public static int getColumnInScreen() {
        return COLUMN_IN_SCREEN;
    }

    public static int getWebSearchRowInScreen() {
        return ROW_IN_SCREEN / WEB_SEARCH_SPAN;
    }

    public static int getLocalSearchRowInScreen() {
        return ROW_IN_SCREEN / LOCAL_SEARCH_SPAN;
    }

    public static int getMaxWebSearchItems() {
        return ROW_IN_SCREEN / WEB_SEARCH_SPAN * COLUMN_IN_SCREEN;
    }

    public static int getMaxLocalSearchItems() {
        return ROW_IN_SCREEN / LOCAL_SEARCH_SPAN * COLUMN_IN_SCREEN;
    }

    public static boolean isAvailableForMoreWebSearchItems(int localCount, int webCount, boolean isOnlyForWeb) {
        if (isOnlyForWeb && localCount == 0) {
            return (ROW_IN_SCREEN * DEF_COLUMN_IN_SCREEN
                    - webCount * DEF_WEB_SEARCH_SPAN) >= DEF_WEB_SEARCH_SPAN;
        } else {
            return (ROW_IN_SCREEN * COLUMN_IN_SCREEN
                    - localCount * LOCAL_SEARCH_SPAN - webCount * WEB_SEARCH_SPAN) >= WEB_SEARCH_SPAN;
        }
    }

    public static boolean isAvailableForMoreLocalSearchItems(int localCount, int webCount, boolean isOnlyForLocal) {
        if (isOnlyForLocal && webCount == 0) {
            return (ROW_IN_SCREEN * DEF_COLUMN_IN_SCREEN
                    - localCount * DEF_LOCAL_SEARCH_SPAN) >= DEF_LOCAL_SEARCH_SPAN;
        } else {
            return (ROW_IN_SCREEN * COLUMN_IN_SCREEN
                    - localCount * LOCAL_SEARCH_SPAN - webCount * WEB_SEARCH_SPAN) >= LOCAL_SEARCH_SPAN;
        }
    }

    public static boolean isMoreThanMaxSearchItems(int localCount, int webCount) {
        return (ROW_IN_SCREEN * COLUMN_IN_SCREEN
                - localCount * LOCAL_SEARCH_SPAN - webCount * WEB_SEARCH_SPAN) < 0;
    }
}
