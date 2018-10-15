package com.smartisanos.ideapills.common.remind.view;

import android.graphics.Canvas;

/**
 * <li>Work with {@link com.android.calendar.month.MonthWeekEventsView}
 * <li>Draw the day cell view on the MonthWeekEventsView
 * @author zhouxiaoxi
 */
public abstract class DayCellViewDrawer {

    public static int VIEW_TYPE_NORMAL = 1<<1;
    public static int VIEW_TYPE_SELECTED = 1<<2;
    public static int VIEW_TYPE_TODAY = 1<<3;
    public static int VIEW_TYPE_NOT_FOCUS_MONTH = 1<<4;
    public static int VIEW_TYPE_DAY_OUT_OF_RANGE = 1<<5;
    public static int VIEW_TYPE_TODAY_BEFORE = 1<<6;

    public abstract void drawView(int index, String dayNum, String lunar, int eventType,int viewType, Canvas canvas, float percent);

    public abstract void setHasFocus(boolean focus);

}
