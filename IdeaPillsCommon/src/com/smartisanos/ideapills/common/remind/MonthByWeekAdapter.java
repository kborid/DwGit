/*
 * Copyright (C) 2010 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.smartisanos.ideapills.common.remind;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Rect;
import android.util.Log;
import android.view.ViewGroup;
import android.widget.AbsListView.LayoutParams;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;

import com.smartisanos.ideapills.common.R;
import com.smartisanos.ideapills.common.remind.util.CalendarUtils;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

public class MonthByWeekAdapter {
    private static final String TAG = "MonthByWeek";

    protected String mHomeTimeZone;
    protected int mQueryDays;
    protected int mOrientation = Configuration.ORIENTATION_LANDSCAPE;

    protected List<Boolean> mHasEvents = null;

    protected int mFirstJulianDay;
    /**
     * The number of weeks to display at a time.
     */
    public static final String WEEK_PARAMS_NUM_WEEKS = "num_weeks";
    /**
     * Which month should be in focus currently.
     */
    public static final String WEEK_PARAMS_FOCUS_MONTH = "focus_month";
    /**
     * Which day the week should start on. {@link Time#SUNDAY} through
     * {@link Time#SATURDAY}.
     */
    public static final String WEEK_PARAMS_WEEK_START = "week_start";
    /**
     * The Julian day to highlight as selected.
     */
    public static final String WEEK_PARAMS_JULIAN_DAY = "selected_day";
    /**
     * How many days of the week to display [1-7].
     */
    public static final String WEEK_PARAMS_DAYS_PER_WEEK = "days_per_week";
    public static final String WEEK_PARAMS_SINGLE_WEEK = "single_week";

    protected static final int WEEK_COUNT_L = 6;
    protected static final int WEEK_COUNT_S = 5;
    protected static int DEFAULT_NUM_WEEKS = 6;
    protected static int DEFAULT_MONTH_FOCUS = 0;
    protected static int DEFAULT_DAYS_PER_WEEK = 7;
    protected static int WEEK_7_OVERHANG_HEIGHT = 7;

    protected static float mScale = 0;
    protected Context mContext;
    // The day to highlight as selected
    protected Time mSelectedDay;
    // The week since 1970 that the selected day is in
    protected int mSelectedWeek;

    protected int mSelectedInMonthPosition = -1;
    // When the week starts; numbered like Time.<WEEKDAY> (e.g. SUNDAY=0).
    protected int mFirstDayOfWeek;
    /* protected boolean mShowWeekNumber = false; */
    protected int mNumWeeks = DEFAULT_NUM_WEEKS;
    protected int mDaysPerWeek = DEFAULT_DAYS_PER_WEEK;
    protected int mFocusMonth = DEFAULT_MONTH_FOCUS;
    protected boolean mIsSingleWeek = false;

    private boolean mHasFocus;

    public static int mItemHeight;
    public static int mItemSingleHeight;
    public static final int ANIMATION_DURATION_MOVE = 0;//disable the anim
    private ArrayList<MonthWeekEventsView> mSortedWeeksViewList = new ArrayList<MonthWeekEventsView>();
    ArrayList<Integer> mSortedIndexList = new ArrayList<Integer>();

    CellEventListener mCellEventListener;
    public interface CellEventListener {
        void onCellClicked(Time day);
        void onCellLongClicked(Time day);
    }

    public MonthByWeekAdapter(Context context, HashMap<String, Integer> params, CellEventListener listener) {
        mContext = context;
        // Get default week start based on locale, subtracting one for use with
        // android Time.
        Calendar cal = Calendar.getInstance(Locale.getDefault());
        mFirstDayOfWeek = cal.getFirstDayOfWeek() - 1;

        Resources res = context.getResources();
        mItemHeight=(int)res.getDimension(R.dimen.monthweek_item_height);
        mItemSingleHeight=(int)res.getDimension(R.dimen.monthweek_item_single_height);

        if (mScale == 0) {
            mScale = res.getDisplayMetrics().density;
            if (mScale != 1) {
                WEEK_7_OVERHANG_HEIGHT *= mScale;
            }
        }
        mCellEventListener = listener;
        init();
        updateParams(params);
    }

    /**
     * Parse the parameters and set any necessary fields. See
     * {@link #WEEK_PARAMS_NUM_WEEKS} for parameter details.
     *
     * @param params
     *            A list of parameters for this adapter
     */
    public void updateParams(HashMap<String, Integer> params) {
        if (params == null) {
            Log.e(TAG, "WeekParameters are null! Cannot update adapter.");
            return;
        }
        if (params.containsKey(WEEK_PARAMS_FOCUS_MONTH)) {
            mFocusMonth = params.get(WEEK_PARAMS_FOCUS_MONTH);
        }
        if (params.containsKey(WEEK_PARAMS_FOCUS_MONTH)) {
            mNumWeeks = params.get(WEEK_PARAMS_NUM_WEEKS);
        }
        if (params.containsKey(WEEK_PARAMS_SINGLE_WEEK)) {
            mIsSingleWeek = params.get(WEEK_PARAMS_SINGLE_WEEK) == 1;
        }
        if (params.containsKey(WEEK_PARAMS_WEEK_START)) {
            mFirstDayOfWeek = params.get(WEEK_PARAMS_WEEK_START);
        }
        if (params.containsKey(WEEK_PARAMS_JULIAN_DAY)) {
            int julianDay = params.get(WEEK_PARAMS_JULIAN_DAY);
//            mSelectedDay.setJulianDay(julianDay);
            mSelectedWeek = CalendarUtils.getWeeksSinceEpochFromJulianDay(julianDay,
                    mFirstDayOfWeek);
        }
        if (params.containsKey(WEEK_PARAMS_DAYS_PER_WEEK)) {
            mDaysPerWeek = params.get(WEEK_PARAMS_DAYS_PER_WEEK);
        }
        refresh();
    }

    /**
     * Returns the currently highlighted day
     *
     * @return
     */
    public Time getSelectedDay() {
        return mSelectedDay;
    }

    public boolean hasFocus() {
        return mHasFocus;
    }

    public void setFocus(boolean hasFocus) {
        this.mHasFocus = hasFocus;
    }

    protected void init() {
        mHomeTimeZone = CalendarUtils.getTimeZone(mContext, null);
        mSelectedDay = new Time(mHomeTimeZone);
        mSelectedDay.setToNow();
    }

    public String getHomeTimeZone() {
        return mHomeTimeZone;
    }

    private void updateTimeZones() {
        mSelectedDay.timezone = mHomeTimeZone;
        mSelectedDay.normalize(true);
    }

    public void setSelectedDay(Time selectedTime) {
        setSelectedDay(selectedTime, true);
    }

    public void setSelectedDay(Time selectedTime, boolean hasFoucus){
        this.mHasFocus = hasFoucus;
        mSelectedDay.set(selectedTime);
        long millis = mSelectedDay.normalize(true);
        mFirstDayOfWeek = CalendarUtils.getFirstDayOfWeekInTime(mContext);
        mSelectedWeek = CalendarUtils
                .getWeeksSinceEpochFromJulianDay(
                        Time.getJulianDay(millis, mSelectedDay.gmtoff),
                        mFirstDayOfWeek);
        notifyDataSetChanged();

    }

    public MonthWeekEventsView updateViewAndSorted(MonthWeekEventsView[] children, int i) {
        MonthWeekEventsView view;
        if (children == null || i >= children.length || children[i] == null) {
            view = new MonthWeekEventsView(mContext, mCellEventListener);
        } else {
            view = children[i];
            if(view.hasFocus()){
                view.clearFocus();
            }
        }

        int position = mSortedIndexList.get(i);
        HashMap<String, Integer> drawingParams = null;

        if (drawingParams == null) {
            drawingParams = new HashMap<String, Integer>();
        }
        drawingParams.clear();

        LayoutParams params = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
        view.setLayoutParams(params);
//        view.setOnTouchListener(this);

        int selectedDay = -1;
        if (mSelectedWeek == (mPosition + position)) {
            selectedDay = mSelectedDay.weekDay;
        }

        drawingParams.put(MonthWeekEventsView.VIEW_PARAMS_FOCUS_MONTH,
                mFocusMonth);
        drawingParams.put(MonthWeekEventsView.VIEW_PARAMS_SELECTED_WEEKDAY,
                selectedDay);

        drawingParams.put(MonthWeekEventsView.VIEW_PARAMS_WEEK_START,
                mFirstDayOfWeek);
        drawingParams.put(MonthWeekEventsView.VIEW_PARAMS_NUM_DAYS,
                mDaysPerWeek);
        drawingParams.put(MonthWeekEventsView.VIEW_PARAMS_WEEK,
                (mPosition + position));
        if(position == mSelectedInMonthPosition && mIsSingleWeek){
            drawingParams.put(MonthWeekEventsView.VIEW_PARAMS_SINGLE_WEEK, 1);
        }

        view.setWeekParams(drawingParams, mSelectedDay.timezone);
        if(mHasFocus){
            setHasEvents(view);
        }
        view.setHasFocus(mHasFocus);
        return view;
    }

    /**
     * Changes which month is in focus and updates the view.
     *
     * @param month
     *            The month to show as in focus [0-11]
     */
    public void updateFocusMonth(int month) {
        mFocusMonth = month;
    }

    public int getFocusMonth() {
        return mFocusMonth;
    }

    public void notifyDataSetChanged() {
        if (null != mWeeksLayout) {
            int count = getCount();
            if (count > 0 && isInitialized()) {
                sortWeeksLayout(mIsSingleWeek, count);
            }
        }
    }

    public ArrayList<Integer> sortViewIndex(int select, int count) {
        ArrayList<Integer> result = new ArrayList<Integer>();
        if(count == 1) {
            result.add(select);
        } else {
            for(int i = 0; i < select; i++){
                result.add(i);
            }
            for(int i = count -1; i > select; i--){
                result.add(i);
            }
            result.add(select);
        }

        return result;
    }

    public void sortWeeksLayout(boolean isOverlap, int count) {
        // recycle used MonthWeekEventsView
        MonthWeekEventsView[] children =
                new MonthWeekEventsView[count];
        // save then clear layout
        for (int i = 0; i < children.length; i++) {
            children[i] = (MonthWeekEventsView) mWeeksLayout.getChildAt(i);
        }
        mWeeksLayout.removeAllViews();
        mSortedWeeksViewList.clear();
        mSelectedInMonthPosition = mSelectedWeek - mPosition;
        mSortedIndexList = sortViewIndex(mSelectedInMonthPosition, count);
        for (int i = 0; i < count; i++) {
            int index = mSortedIndexList.get(i);
            MonthWeekEventsView view = updateViewAndSorted(children, i);
            RelativeLayout.LayoutParams rl = new RelativeLayout.LayoutParams(
                    LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
            rl.topMargin = (isOverlap ? 0 : mItemHeight) * index;
            mWeeksLayout.addView(view, rl);
            mSortedWeeksViewList.add(view);
        }
    }

    // when do today shake animation, we add an extra MonthWeekEventsView to DragSupportView
    public MonthWeekEventsView getTodayMonthWeekEventsView(ViewGroup dragSupportView) {
        for (int i = 0; i < mSortedWeeksViewList.size(); i++) {
            MonthWeekEventsView child = mSortedWeeksViewList.get(i);
            if (child.hasToday()) {
                Rect rect = new Rect();
                dragSupportView.offsetDescendantRectToMyCoords(child, rect);
                MonthWeekEventsView view = updateViewAndSorted(null,i);
                view.setHasFocus(mHasFocus);
                view.setOriginalView(child);
                FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                        child.getWidth() + view.EXPANDED_WIDTH * 2,
                        FrameLayout.LayoutParams.WRAP_CONTENT);
                params.leftMargin = rect.left - view.EXPANDED_WIDTH;
                params.topMargin = rect.top - view.EXPANDED_HEIGHT;
                dragSupportView.addView(view, params);
                return view;
            }
        }
        return null;
    }

    private void setHasEvents(MonthWeekEventsView v) {
        if (mHasEvents == null) {
            v.setHasEvents(null);
           return;
        }
        int viewJulianDay = v.getFirstJulianDay();
        int start = viewJulianDay - mFirstJulianDay;
        int end = start + v.mNumDays;
        if (start < 0 || end > mHasEvents.size()) {
            v.setHasEvents(null);
            return;
        }
        v.setHasEvents(mHasEvents.subList(start, end));
    }

    protected void refresh() {
        mFirstDayOfWeek = CalendarUtils.getFirstDayOfWeekInTime(mContext);
        mHomeTimeZone = CalendarUtils.getTimeZone(mContext, null);
        updateTimeZones();
        notifyDataSetChanged();
    }

    protected void onDayTapped(Time day) {
        if (mSelectedDay == day) {
            return;
        }
        setSelectedDay(day);
    }

    /**
     * the position of weeks since the epoch
     */
    private int mPosition = -1;

    public void setPosition(int position) {
        if (isInitialized() && mPosition == position) {
            return;
        } else {
            mPosition = position;
        }
    }

    public boolean isInitialized() {
        return mPosition >= 0;
    }

    public void setWeeksLayout(RelativeLayout weeksLayout) {
        mWeeksLayout = weeksLayout;
    }

    private RelativeLayout mWeeksLayout;

    public int getCount() {
        if(mIsSingleWeek) {
            return 1;
        } else {
            return getThisMonthWeekCount();
        }
    }

    public int getThisMonthWeekCount() {
        int julianMonday = CalendarUtils
                .getJulianMondayFromWeeksSinceEpoch(mPosition + 4);
        Time time = new Time(mSelectedDay.timezone);
        time.setJulianDay(julianMonday
                - (Time.MONDAY - CalendarUtils.getFirstDayOfWeekInTime(mContext)));
        if (time.month == mFocusMonth) {
            julianMonday = CalendarUtils
                    .getJulianMondayFromWeeksSinceEpoch(mPosition + 5);
            time = new Time(mSelectedDay.timezone);
            time.setJulianDay(julianMonday
                    - (Time.MONDAY - CalendarUtils.getFirstDayOfWeekInTime(mContext)));
            if (time.month == mFocusMonth) {
                return WEEK_COUNT_L;
            }
            return WEEK_COUNT_S;
        }
        return WEEK_COUNT_S - 1;
    }


    public int getSelectedInMonthPosition() {
        return mSelectedInMonthPosition;
    }

    public void reAddChildWithMarginTop(int margTop) {
        if(mSortedIndexList.size() == mSortedWeeksViewList.size() && mSortedIndexList.size() > 0){
            mWeeksLayout.removeAllViews();
            for(int i = 0; i < mSortedIndexList.size(); i++){
                MonthWeekEventsView view = mSortedWeeksViewList.get(i);
                RelativeLayout.LayoutParams rl = new RelativeLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
                rl.topMargin = (int) (margTop * (view.mWeek - mPosition));
                if (view.getParent() != null && view.getParent() instanceof ViewGroup) {
                    ((ViewGroup)view.getParent()).removeView(view);
                }
                mWeeksLayout.addView(view, rl);
            }
        }
    }

    public MonthWeekEventsView getTouchView(float touchY) {
        if(touchY > mWeeksLayout.getHeight() || touchY < 0){
            return null;
        }
        if(mIsSingleWeek){
            return mSortedWeeksViewList.get(mSortedWeeksViewList.size() - 1);
        } else {
            int index = (int) (mWeeksLayout.getChildCount() * touchY / mWeeksLayout.getHeight());
            return getIndexOfView(index);
        }
    }

    public MonthWeekEventsView getIndexOfView(int index){
        if (mSortedIndexList.isEmpty() || index >= mSortedIndexList.size()) {
            return null;
        } else if (mIsSingleWeek) {
            return mSortedWeeksViewList.get(mSortedWeeksViewList.size() - 1);
        } else {
            return mSortedWeeksViewList.get(mSortedIndexList.get(index));
        }
    }
}
