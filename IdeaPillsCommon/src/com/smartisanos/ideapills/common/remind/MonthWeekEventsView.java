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

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.SystemClock;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityEvent;
import android.view.MotionEvent;
import android.view.accessibility.AccessibilityManager;
import android.app.Service;

import com.smartisanos.ideapills.common.R;
import com.smartisanos.ideapills.common.remind.util.CalendarUtils;
import com.smartisanos.ideapills.common.remind.view.DayCellViewDrawer;
import com.smartisanos.ideapills.common.remind.view.MonthByWeekDayViewDrawer;

import java.security.InvalidParameterException;
import java.util.HashMap;
import java.util.List;

public class MonthWeekEventsView extends View {

    private static final String TAG = "MonthView";

    private static final boolean DEBUG_LAYOUT = false;

    private int DEFAULT_EDGE_SPACING;
    private float DAY_SEPARATOR_INNER_WIDTH = 0.73f;

    private int TODAY_HIGHLIGHT_WIDTH = 2;
    private float sWeekCellTotalInterval;

    private int SPACING_WEEK_NUMBER = 24;

    private float mCellWidth;
    protected boolean mHasToday = false;
    protected int mTodayIndex = -1;
    // Contains user events and holidays
    private List<Boolean> mHasEvents;
    // This is for drawing the outlines around event chips and supports up to 10
    // events being drawn on each day. The code will expand this if necessary.

    protected static StringBuilder mStringBuilder = new StringBuilder(50);

    protected Drawable mTodayDrawable;

    // private Drawable mMonthBGTodayDrawable; // instead by
    // remind_month_by_week_listek_list.xml
    // private Drawable mMonthBGSelectedDrawable; // instead by
    // remind_month_by_week_list.xmlist.xml
    // private Drawable mMonthBGSelectedTodayDrawable; // instead by
    // remind_month_by_week_list.xmlist.xml
    private Drawable mMonthBGNotFocusDrawable;
    private Drawable mTodayFullMonthBG;
    private Drawable mUnfocusTodayFullMonthBG;
    private Drawable mSelectedFullMonthBG;
    // private Drawable mEventsDotNormal; // nobody 20130815
    // private Drawable mEventsDotWhite;
    private Drawable EventDrawableWapper;
    private int mWeekEventWithLunarTop;
    private int mWeekEvenTop;
    private int mMonthEventTop;

    private int mClickedDayIndex = -1;
    private static final int mClickedAlpha = 128;
    protected int mTodayAnimateColor;

    private boolean mAnimateToday;
    private int mAnimateTodayAlpha = 0;
    private ObjectAnimator mTodayAnimator = null;
    protected Time mTimeToday = new Time();

    /**
     * These params can be passed into the view to control how it appears. {@link #VIEW_PARAMS_WEEK} is the only
     * required field, though the default values are unlikely to fit most layouts correctly.
     */
    /**
     * This sets the height of this week in pixels
     */
    public static final String VIEW_PARAMS_HEIGHT = "height";
    /**
     * This specifies the position (or weeks since the epoch) of this week, calculated using
     */
    public static final String VIEW_PARAMS_WEEK = "week";
    /**
     * This sets one of the days in this view as selected {@link Time#SUNDAY} through {@link Time#SATURDAY}.
     */
    public static final String VIEW_PARAMS_SELECTED_WEEKDAY = "selected_day";
    /**
     * Which day the week should start on. {@link Time#SUNDAY} through {@link Time#SATURDAY}.
     */
    public static final String VIEW_PARAMS_WEEK_START = "week_start";
    /**
     * How many days to display at a time. Days will be displayed starting with {@link #mWeekStart}.
     */
    public static final String VIEW_PARAMS_NUM_DAYS = "num_days";
    /**
     * Which month is currently in focus, as defined by {@link Time#month} [0-11].
     */
    public static final String VIEW_PARAMS_FOCUS_MONTH = "focus_month";
    /**
     * If this month should display week numbers. false if 0, true otherwise.
     */
    public static final String VIEW_PARAMS_SHOW_WK_NUM = "show_wk_num";
    /**
     * If this is single week.false if 0, true otherwise.
     */
    public static final String VIEW_PARAMS_SINGLE_WEEK = "is_single_week";

    protected int DEFAULT_HEIGHT = 32;
    protected static final int DEFAULT_SELECTED_DAY = -1;
    protected static final int DEFAULT_WEEK_START = Time.SUNDAY;
    protected static final int DEFAULT_NUM_DAYS = 7;
    protected static final int DEFAULT_SHOW_WK_NUM = 0;
    protected static final int DEFAULT_FOCUS_MONTH = -1;

    protected int MINI_DAY_NUMBER_TEXT_SIZE;

    // used for scaling to the device density
    protected float mScale = 0;

    // affects the padding on the sides of this view
    protected int mPadding = 0;

    protected Rect r = new Rect();
    protected Paint p = new Paint();
    protected Drawable mSelectedDayLine;

    // separator count
    private final static int SEPARATOR_COUNT = (DEFAULT_NUM_DAYS + 2) * 4;
    // paint for day separators
    private final Paint mSeparatorPaint = new Paint();
    // position info of day separators
    private final float mSeparatorLines[] = new float[SEPARATOR_COUNT];
    // color of day separators
    private int mSeparatorColor;

    // Cache the number strings so we don't have to recompute them each time
    protected String[] mDayNumbers;
    protected String[] mDayTexts;
    private boolean mIsSingleWeek = false;
    // Quick lookup for checking which days are in the focus month
    protected boolean[] mFocusDay;
    protected boolean[] mIsDayOutOfRange;
    protected boolean[] mIsTodayBefore;
    // The Julian day of the first day displayed by this item
    protected int mFirstJulianDay = -1;
    // The month of the first day in this week
    protected int mFirstMonth = -1;
    // The month of the last day in this week
    protected int mLastMonth = -1;
    // The position of this week, equivalent to weeks since the week of Jan 1st,
    // 1970
    protected int mWeek = -1;
    // Quick reference to the width of this view, matches parent
    protected int mWidth;
    // The height this view should draw at in pixels, set by height param
    protected int mHeight = DEFAULT_HEIGHT;
    // The height this view should draw at in pixels, set by height param
    // Whether the week number should be shown
    /* protected boolean mShowWeekNum = false; */
    // If this view contains the selected day
    protected boolean mHasSelectedDay = false;
    // Which day is selected [0-6] or -1 if no day is selected
    protected int mSelectedWeekDay = DEFAULT_SELECTED_DAY;
    protected int mFakeSelectedDay = DEFAULT_SELECTED_DAY;
    // Which day of the week to start on [0-6]
    protected int mWeekStart = DEFAULT_WEEK_START;
    // How many days to display
    protected int mNumDays = DEFAULT_NUM_DAYS;
    // The number of days + a spot for week number if it is displayed
    protected int mNumCells = mNumDays;
    // The left edge of the selected day
    protected int mSelectedLeft = -1;
    // The right edge of the selected day
    protected int mSelectedRight = -1;
    // The timezone to display times/dates in (used for determining when Today
    // is)
    protected String mTimeZone = Time.getCurrentTimezone();

    private Context mContext;
    protected int mToday = DEFAULT_SELECTED_DAY;

    private Resources mRes;

    private boolean hasFocus;

    //1.0f means 100 %   SingleWeek
    //0 mean 0%  NotSingleWeek
    private float mSwitchAnimProgress = 0f;

    public static final float SWTICH_ANIM_CHANGE_POINT = 0.5f;

    private MonthByWeekDayViewDrawer mCellDrawer;

    private int getEventTop() {
        int top = 0;
        if (!mIsSingleWeek) {
            top = mMonthEventTop;
        } else {
            top = mWeekEvenTop;
        }
        return top;
    }

    WeekViewAccessibilityHelper mAccessibilityHelper;
    MonthByWeekAdapter.CellEventListener mCellEventListener;

    /**
     * Shows up as an error if we don't include this.
     */
    public MonthWeekEventsView(Context context, MonthByWeekAdapter.CellEventListener listener) {
        super(context);
        mContext = context;
        mRes = context.getResources();

        float scale = context.getResources().getDisplayMetrics().density;

        // Sets up any standard paints that will be used
        initView(scale);
        mCellEventListener = listener;
        mAccessibilityHelper = new WeekViewAccessibilityHelper(this);
        mAccessibilityHelper.setAccessibilityDelegateForView();
        setOnTouchListener(new OnTouchListener() {
            boolean mHandleLongPressEvent = false;
            // Check for long press
            GestureDetector detector = new GestureDetector(mContext,
                    new GestureDetector.OnGestureListener() {
                        public boolean onDown(MotionEvent e) {
                            mHandleLongPressEvent = false;
                            return true;
                        }

                        public void onShowPress(MotionEvent e) {
                        }

                        public boolean onSingleTapUp(MotionEvent e) {
                            Time day = getDayFromLocation(e.getX());
                            mCellEventListener.onCellClicked(day);
                            sendAccessibilityEvent(day);
                            return true;
                        }

                        public boolean onScroll(MotionEvent e1, MotionEvent e2,
                                                float distanceX, float distanceY) {
                            return false;
                        }

                        public void onLongPress(MotionEvent e) {
                            mHandleLongPressEvent = true;
                            Time day = getDayFromLocation(e.getX());
                            mCellEventListener.onCellLongClicked(day);
                        }

                        public boolean onFling(MotionEvent e1, MotionEvent e2,
                                               float velocityX, float velocityY) {
                            return false;
                        }
                    });

            @Override
            public boolean onTouch(View v, MotionEvent event) {

                // Here using system up time to replace the down
                // time of the motion event to avoid taking the
                // first event's down time of ACTION_DOWN as the
                // next one's, this will cause taking the second
                // ACTION_DOWN as long press event. This may cause
                // an about 50ms delay time for ACTION_DOWN's down time.
                // See bug 17204
                if ((event.getAction() & MotionEvent.ACTION_MASK) == MotionEvent.ACTION_DOWN) {
                    MotionEvent newEvent = MotionEvent.obtain(event);
                    newEvent.setDownTime(SystemClock.uptimeMillis());
                    return detector.onTouchEvent(newEvent);
                } else {
                    if (mHandleLongPressEvent) {
                        return true;
                    }
                    return detector.onTouchEvent(event);
                }
            }

        });
    }


    Rect mChildBounds;

    public Rect getBoundsForIndex(int index) {
        if (mChildBounds == null) {
            mChildBounds = new Rect();
        }
        mChildBounds.set(Math.round(index * mCellWidth), 0, Math.round((index + 1) * mCellWidth), mHeight);
        return mChildBounds;
    }

    public CharSequence getContentDescriptionForIndex(int index) {
        int flags = DateUtils.FORMAT_SHOW_WEEKDAY | DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_NO_YEAR;
        Time selectDay = new Time(mTimeZone);
        selectDay.setJulianDay(mFirstJulianDay + index);
        return DateUtils.formatDateTime(mContext, selectDay.toMillis(true), flags);
    }

    public boolean isSelectedForIndex(int index) {
        return mSelectedWeekDay - mWeekStart == index;
    }

    public void setFakeSelectedDay(int selectDay) {
        if (mFakeSelectedDay != selectDay) {
            mFakeSelectedDay = selectDay;
            /* this.invalidate(); */
        }
    }

    public boolean hasFakeSelectedDay() {
        return mFakeSelectedDay > -1 ? true : false;
    }

    public void setSelectedDay(int selectDay) {
        if (mSelectedWeekDay != selectDay) {
            mSelectedWeekDay = selectDay;
            /* this.invalidate(); */
        }
    }

    public boolean hasSelectedDay() {
        return mHasSelectedDay;
    }

    void setHasEvents(List<Boolean> hasEvents) {
        mHasEvents = hasEvents;
    }

    protected void loadColors(Context context) {
        mSeparatorColor = mRes.getColor(R.color.month_grid_lines);
        mTodayAnimateColor = mRes.getColor(R.color.today_highlight_color);
        mMonthBGNotFocusDrawable = mRes.getDrawable(R.drawable.remind_month_view_grey_day_item);
        mTodayFullMonthBG = mRes.getDrawable(R.drawable.remind_calendar_month_view_today_focused);
        mUnfocusTodayFullMonthBG = mRes.getDrawable(R.drawable.remind_calendar_month_view_day_unfocused);
        mSelectedFullMonthBG = mRes.getDrawable(R.drawable.remind_calendar_month_view_day_focused);
        mTodayDrawable = mRes.getDrawable(R.drawable.remind_today_blue_week_holo_light);

        mWeekEventWithLunarTop = (int) mRes.getDimension(R.dimen.monthweek_relative_week_event);
        mMonthEventTop = (int) mRes.getDimension(R.dimen.monthweek_relative_month_event);
        mWeekEvenTop = mRes.getDimensionPixelOffset(R.dimen.monthweek_relative_week_event_no_lunnar);
    }

    /**
     * Sets up the text and style properties for painting. Override this if you want to use a different paint.
     */
    protected void initView(float scale) {

        sBgMarginV = mRes.getDimensionPixelOffset(R.dimen.monthbyweek_v);
        sBgMarginH = mRes.getDimensionPixelOffset(R.dimen.monthbyweek_h);
        mCellWidth = mRes.getDimension(R.dimen.monthweek_item_width);
        sWeekCellTotalInterval = mRes.getDimension(R.dimen.monthweek_grid_bg_interval_width);
        MINI_DAY_NUMBER_TEXT_SIZE = mRes.getDimensionPixelSize(R.dimen.text_size_month_number);

        DEFAULT_HEIGHT *= scale;
        EXPANDED_HEIGHT *= scale;
        EXPANDED_WIDTH *= scale;
        SPACING_WEEK_NUMBER *= scale;
        DAY_SEPARATOR_INNER_WIDTH *= scale;
        DEFAULT_EDGE_SPACING *= scale;
        TODAY_HIGHLIGHT_WIDTH *= scale;

        if (DAY_SEPARATOR_INNER_WIDTH > 0 && DAY_SEPARATOR_INNER_WIDTH < 1.0f) {
            DAY_SEPARATOR_INNER_WIDTH = 1.0f;
        }
        p.setFakeBoldText(false);
        p.setAntiAlias(true);
        p.setTextSize(MINI_DAY_NUMBER_TEXT_SIZE);
        p.setStyle(Style.FILL);
        mPadding = DEFAULT_EDGE_SPACING;
        loadColors(getContext());
        // TODO modify paint properties depending on isMini
        if (DEBUG_LAYOUT) {
            Log.d("EXTRA", "scale=" + scale);
        }
    }

    public void setWeekParams(HashMap<String, Integer> params, String tz) {
        //note:it for testing,using the generate month lunars,don't delete it.
        /* Thread thead = new Thread(){
         @Override
             public void run() {
                 TestLunarGenerator.generateMonthLunar(mContext);
             }
         };
         thead.start();*/

        if (!params.containsKey(VIEW_PARAMS_WEEK)) {
            throw new InvalidParameterException("You must specify the week number for this view");
        }
        setTag(params);
        mTimeZone = tz;
        // We keep the current value for any params not present
        if (params.containsKey(VIEW_PARAMS_SINGLE_WEEK) && params.get(VIEW_PARAMS_SINGLE_WEEK) != 0) {
            mIsSingleWeek = true;
        } else {
            mIsSingleWeek = false;
        }
        if (mIsSingleWeek) {
            mHeight = MonthByWeekAdapter.mItemSingleHeight;
        } else {
            mHeight = MonthByWeekAdapter.mItemHeight;
        }

        if (params.containsKey(VIEW_PARAMS_SELECTED_WEEKDAY)) {
            mSelectedWeekDay = params.get(VIEW_PARAMS_SELECTED_WEEKDAY);
        }
        mHasSelectedDay = mSelectedWeekDay != -1;
        if (params.containsKey(VIEW_PARAMS_NUM_DAYS)) {
            mNumDays = params.get(VIEW_PARAMS_NUM_DAYS);
        }
        /*
         * if (params.containsKey(VIEW_PARAMS_SHOW_WK_NUM)) { if (params.get(VIEW_PARAMS_SHOW_WK_NUM) != 0) {
         * mShowWeekNum = true; } else { mShowWeekNum = false; } }
         */
        mNumCells = /* mShowWeekNum ? mNumDays + 1 : */mNumDays;
        // Allocate space for caching the day numbers and focus values
        mDayNumbers = new String[mNumCells];
        mFocusDay = new boolean[mNumCells];
        mWeek = params.get(VIEW_PARAMS_WEEK);
        int julianMonday = CalendarUtils.getJulianMondayFromWeeksSinceEpoch(mWeek);
        // the month view should not change with timeZone, use CST timezone by default.
        Time solar = new Time("Asia/Shanghai");
        solar.setJulianDay(julianMonday);

        // If we're showing the week number calculate it based on Monday
        int i = 0;
        /*
         * if (mShowWeekNum) { mDayNumbers[0] = Integer.toString(time.getWeekNumber()); int[] temp =
         * SolarToLunar.getLunar(time.year, time.month + 1, time.monthDay); StringBuilder lunar = new StringBuilder();
         * int[] solar = new int[3]; solar[0] = time.year; solar[1] = time.month + 1; solar[2] = time.monthDay;
         * getDay(lunar, temp, solar); mDayTexts[0] = lunar.toString(); i++; }
         */

        if (params.containsKey(VIEW_PARAMS_WEEK_START)) {
            mWeekStart = params.get(VIEW_PARAMS_WEEK_START);
        }

        // Now adjust our starting day based on the start day of the week
        // If the week is set to start on a Saturday the first week will be
        // Dec 27th 1969 -Jan 2nd, 1970
        CalendarUtils.adjustToBeginningOfWeek(solar, mWeekStart);

        mFirstJulianDay = Time.getJulianDay(solar.toMillis(true), solar.gmtoff);
        mFirstMonth = solar.month;

        // Figure out what day today is
        Time today = new Time(tz);
        today.setToNow();
        mHasToday = false;
        mToday = -1;
        int focusMonth = params.containsKey(VIEW_PARAMS_FOCUS_MONTH) ? params.get(VIEW_PARAMS_FOCUS_MONTH)
                : DEFAULT_FOCUS_MONTH;
        mIsDayOutOfRange = null;
        mIsTodayBefore = null;
        for (; i < mNumCells; i++) {
            if (solar.monthDay == 1) {
                mFirstMonth = solar.month;
            }
            if (solar.month == focusMonth) {
                mFocusDay[i] = true;
            } else {
                mFocusDay[i] = false;
            }
            if (0 > focusMonth) {
                mFocusDay[i] = true;
            }
            if (solar.year == today.year && solar.month == today.month && solar.monthDay == today.monthDay) {
                mHasToday = true;
                mToday = i;
            }

            if (isBeforeDay(solar, today)) {
                if (mIsTodayBefore == null) {
                    mIsTodayBefore = new boolean[mNumCells];
                }
                mIsTodayBefore[i] = true;
            }

            if (solar.year < CalendarUtils.MIN_CALENDAR_YEAR || solar.year > CalendarUtils.
                    MAX_CALENDAR_YEAR) {
                mFocusDay[i] = false;
                if (mIsDayOutOfRange == null) {
                    mIsDayOutOfRange = new boolean[mNumCells];
                }
                mIsDayOutOfRange[i] = true;
            }
            mDayNumbers[i] = Integer.toString(solar.monthDay);
            /*
             * else { mDayTexts[i] = MonthLunars.getMonthLunar(mContext,
             * mFirstJulianDay + i); }
             */
            solar.monthDay++;
            // There are at least 28 day in a month.
            // So we don't need to normalize every time (to optimize
            // performance)
            if (solar.monthDay > 28) {
                solar.normalize(true);
            }
        }
        // We do one extra add at the end of the loop, if that pushed us to a
        // new month undo it
        if (solar.monthDay == 1) {
            solar.monthDay--;
            solar.normalize(true);
        }
        mLastMonth = solar.month;
        updateSelectionPositions();

        updateToday(tz);
        mNumCells = mNumDays + 1;
        updatePercent();
        /*
         * if (params.containsKey(VIEW_PARAMS_ANIMATE_TODAY) && mHasToday) { synchronized (mAnimatorListener) { if
         * (mTodayAnimator != null) { mTodayAnimator.removeAllListeners(); mTodayAnimator.cancel(); } mTodayAnimator =
         * ObjectAnimator.ofInt(this, "animateTodayAlpha", Math.max(mAnimateTodayAlpha, 80), 255);
         * mTodayAnimator.setDuration(150); mAnimatorListener.setAnimator(mTodayAnimator);
         * mAnimatorListener.setFadingIn(true); mTodayAnimator.addListener(mAnimatorListener); mAnimateToday = true;
         * mTodayAnimator.start(); } }
         */
    }

    private boolean isBeforeDay(Time solar, Time today) {
        if (solar.year < today.year) {
            return true;
        }

        if (solar.year == today.year && solar.month < today.month) {
            return true;
        }

        if (solar.year == today.year && solar.month == today.month) {
            if (solar.monthDay < today.monthDay) {
                return true;
            }
        }

        return false;
    }

    /**
     * @param dayOffset day offset from the first julian day
     */
    public boolean isFocusDay(int dayOffset) {
        if (mFocusDay != null && dayOffset >= 0 && dayOffset < mFocusDay.length) {
            return mFocusDay[dayOffset];
        } else {
            return false;
        }
    }

    public boolean isOutOfRangeDay(int dayOffset) {
        if (mIsDayOutOfRange != null && dayOffset >= 0 && dayOffset < mFocusDay.length) {
            return mIsDayOutOfRange[dayOffset];
        }
        return false;
    }

    /**
     * check today before
     *
     * @param dayOffset
     * @return
     */
    public boolean isBeforeToday(int dayOffset) {
        if (mIsTodayBefore != null && dayOffset >= 0 && dayOffset < mIsTodayBefore.length) {
            return mIsTodayBefore[dayOffset];
        }
        return false;
    }

    /**
     * Returns the month of the first day in this week
     * * @return The month the first day of this view is in
     */
    public int getFirstMonth() {
        return mFirstMonth;
    }

    /**
     * Returns the month of the last day in this week
     * * @return The month the last day of this view is in
     */
    public int getLastMonth() {
        return mLastMonth;
    }

    /**
     * Returns the julian day of the first day in this view.
     * * @return The julian day of the first day in the view.
     */
    public int getFirstJulianDay() {
        return mFirstJulianDay;
    }

    /**
     * @param tz
     */
    public boolean updateToday(String tz) {
        mTimeToday.timezone = tz;
        mTimeToday.setToNow();
        mTimeToday.normalize(true);
        int julianToday = Time.getJulianDay(mTimeToday.toMillis(false), mTimeToday.gmtoff);
        if (julianToday >= mFirstJulianDay && julianToday < mFirstJulianDay + mNumDays) {
            mHasToday = true;
            mTodayIndex = julianToday - mFirstJulianDay;
        } else {
            mHasToday = false;
            mTodayIndex = -1;
        }
        return mHasToday;
    }

    public boolean hasToday() {
        return mHasToday;
    }

    public void setHasFocus(boolean hasFocus) {
        this.hasFocus = hasFocus;
        if (!hasFocus) {
            for (int i = 0; i < mFocusDay.length; i++) {
                mFocusDay[i] = true;
            }
        }
        setAllDrawerFocusState();
    }

    private void setAllDrawerFocusState() {
        setDrawerFocusState(mCellDrawer);
    }

    private void setDrawerFocusState(DayCellViewDrawer drawer) {
        if (drawer != null) {
            drawer.setHasFocus(hasFocus);
        }
    }

    public boolean isHasFocus() {
        return hasFocus;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (mIsInShakeAnimation) {
            // the animation view are expanded in width/height, need translate here.
            canvas.translate(EXPANDED_WIDTH, EXPANDED_HEIGHT);
        }
        drawBackground(canvas);
        drawWeekNums(canvas);
        if (hasFocus && mHasToday && mAnimateToday) {
            drawToday(canvas);
        }
    }

    private int mUndrawBackgoundIndex = -1;

    public int getmUndrawBackgoundIndex() {
        return mUndrawBackgoundIndex;
    }

    public void setmUndrawBackgoundIndex(int mUndrawBackgoundIndex) {
        this.mUndrawBackgoundIndex = mUndrawBackgoundIndex;
        invalidate();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        if (mIsInShakeAnimation) {
            mWidth = w - EXPANDED_WIDTH * 2;
        } else {
            mWidth = w;
        }
        updateSelectionPositions();
    }

    protected void drawToday(Canvas canvas) {
        r.top = Math.round(DAY_SEPARATOR_INNER_WIDTH) + (TODAY_HIGHLIGHT_WIDTH / 2);
        r.bottom = mHeight - (int) Math.ceil(TODAY_HIGHLIGHT_WIDTH / 2.0f);
        p.setStyle(Style.STROKE);
        p.setStrokeWidth(TODAY_HIGHLIGHT_WIDTH);
        r.left = computeDayLeftPosition(mTodayIndex) + (TODAY_HIGHLIGHT_WIDTH / 2);
        r.right = computeDayLeftPosition(mTodayIndex + 1) - (int) Math.ceil(TODAY_HIGHLIGHT_WIDTH / 2.0f);
        p.setColor(mTodayAnimateColor | (mAnimateTodayAlpha << 24));
        canvas.drawRect(r, p);
        p.setStyle(Style.FILL);
    }

    // TODO move into SimpleWeekView
    // Computes the x position for the left side of the given day
    private int computeDayLeftPosition(int day) {
        int effectiveWidth = mWidth;
        float x = 0;
        int xOffset = 0;
        /*
         * if (mShowWeekNum) { xOffset = SPACING_WEEK_NUMBER + mPadding; effectiveWidth -= xOffset; }
         */
        x = day * mCellWidth + xOffset;
        return Math.round(x);
    }

    protected void drawBackground(Canvas canvas) {
        // int i = 0;
        drawBasicBackgound(canvas);
        // draw not focus background.

        drawDaySeparators(canvas);

        if (hasFocus) {
            // this is the case jump more than one months, the month between
            // the first and last has no focus, just use basic background.
            drawSpecificBackground(canvas);
        }
    }

    private float sBgMarginV = 4;//4px, the focus image is bigger than the day grid.
    private int sBgMarginH = 3; //3px

    /**
     * Including today, selected day and the days not in focused month.
     *
     * @param canvas
     */
    private void drawSpecificBackground(Canvas canvas) {

        int selectedPosition = mSelectedWeekDay - mWeekStart;
        if (selectedPosition < 0) {
            selectedPosition += 7;
        }

        Rect bgRect = new Rect();
        bgRect.top = (int) -sBgMarginV;
        bgRect.bottom = (int) (mHeight + sBgMarginV);

        for (int i = 0; i < mNumDays; i++) {
            if (i == mUndrawBackgoundIndex) {
                continue;
            }
            if ((mFocusDay != null && !mFocusDay[i])) {
                r.left = computeDayLeftPosition(i);
                r.right = computeDayLeftPosition(i + 1);
                mMonthBGNotFocusDrawable.setBounds(r);
                if (!isOutOfRangeDay(i)) {
                    mMonthBGNotFocusDrawable.setAlpha((int) ((1 - mSwitchAnimProgress) * 255));
                }
                mMonthBGNotFocusDrawable.draw(canvas);
            }

            if (mIsTodayBefore != null && mIsTodayBefore[i]) {
                r.left = computeDayLeftPosition(i);
                r.right = computeDayLeftPosition(i + 1);
                mMonthBGNotFocusDrawable.setBounds(r);
                mMonthBGNotFocusDrawable.draw(canvas);
            }
        }
        //draw foucs background at the end of drawing. To make sure the today shake animation complete
        if (mHasToday && mTodayIndex != mUndrawBackgoundIndex) {
            // set today background
            bgRect.left = computeDayLeftPosition(mTodayIndex) - sBgMarginH;
            bgRect.right = computeDayLeftPosition(mTodayIndex + 1) + sBgMarginH;
            if (mHasSelectedDay && selectedPosition == mTodayIndex) {
                if (mIsInShakeAnimation) {
                    Rect animationBgRect = new Rect(bgRect);
                    adjustAnimationBGRect(animationBgRect);
                    mTodayFullMonthBG.setBounds(animationBgRect);
                } else {
                    mTodayFullMonthBG.setBounds(bgRect);
                }
                mTodayFullMonthBG.draw(canvas);
            } else {
                mUnfocusTodayFullMonthBG.setBounds(bgRect);
                mUnfocusTodayFullMonthBG.draw(canvas);
            }
        }
        if (mHasSelectedDay && selectedPosition != mUndrawBackgoundIndex && selectedPosition != mTodayIndex) {
            // set selected background
            bgRect.left = computeDayLeftPosition(selectedPosition) - sBgMarginH;
            bgRect.right = computeDayLeftPosition(selectedPosition + 1) + sBgMarginH;
            mSelectedFullMonthBG.setBounds(bgRect);
            mSelectedFullMonthBG.draw(canvas);
        }
    }

    // should be deprecated, use drawDaySeparators instead
    private void drawBasicBackgound(Canvas canvas) {
        r.left = 0;
        r.right = mWidth;
        r.top = 0;
        r.bottom = mHeight + 1;
    }

    protected void drawDaySeparators(Canvas canvas) {
        final float strokeWidth = DAY_SEPARATOR_INNER_WIDTH;
        final int totalWidth = mWidth;
        final int color = mSeparatorColor;
        int y0 = 0;
        int y1 = mHeight;
        int i = 0;

        //draw day separator line (vertical)
        while (i < DEFAULT_NUM_DAYS * 4) {
            int x = computeDayLeftPosition(i / 4);
            mSeparatorLines[i++] = x;
            mSeparatorLines[i++] = y0;
            mSeparatorLines[i++] = x;
            mSeparatorLines[i++] = y1;
        }

        //draw one more day separator line at right edge (vertical)
        mSeparatorLines[i++] = strokeWidth <= 1.0f ? totalWidth - 1 : totalWidth;
        mSeparatorLines[i++] = y0;
        mSeparatorLines[i++] = strokeWidth <= 1.0f ? totalWidth - 1 : totalWidth;
        mSeparatorLines[i++] = y1;

        //draw week separator line (horizontal)
        mSeparatorLines[i++] = 0;
        mSeparatorLines[i++] = strokeWidth <= 1.0f ? 0 : 1;
        mSeparatorLines[i++] = totalWidth;
        mSeparatorLines[i++] = strokeWidth <= 1.0f ? 0 : 1;

        mSeparatorPaint.setColor(color);
        mSeparatorPaint.setStrokeWidth(strokeWidth);
        canvas.drawLines(mSeparatorLines, 0, SEPARATOR_COUNT, mSeparatorPaint);
    }

    protected void drawWeekNums(Canvas canvas) {

        //boolean isEventsDotNormal;
        int selectedPosition = mSelectedWeekDay - mWeekStart;
        if (selectedPosition < 0) {
            selectedPosition += 7;
        }

        DayCellViewDrawer dayCellViewDrawer = getDayCellViewDrawer();
        dayCellViewDrawer.setHasFocus(hasFocus);
        for (int i = 0; i < mNumDays; i++) {
            //the mNumDays is set to 7 by default but mDayNumbers may not be initialized
            if (mDayNumbers == null || mDayNumbers[i] == null) {
                continue;
            }
            int startX = computeDayLeftPosition(i + 1);

            int viewType = DayCellViewDrawer.VIEW_TYPE_NORMAL;
            //is not the focus month day
            if (!mFocusDay[i]) {
                viewType |= DayCellViewDrawer.VIEW_TYPE_NOT_FOCUS_MONTH;
            }
            if (isOutOfRangeDay(i)) {
                viewType |= DayCellViewDrawer.VIEW_TYPE_DAY_OUT_OF_RANGE;
            }

            //is the today type
            if (mHasToday && mTodayIndex == i) {
                viewType |= DayCellViewDrawer.VIEW_TYPE_TODAY;
            }
            //is the selected  type
            if (mHasSelectedDay && selectedPosition == i) {
                viewType |= DayCellViewDrawer.VIEW_TYPE_SELECTED;
            }

            if (isBeforeToday(i)) {
                viewType |= DayCellViewDrawer.VIEW_TYPE_TODAY_BEFORE;
            }

            String lunar = mDayTexts == null ? "" : mDayTexts[i];
            dayCellViewDrawer.drawView(startX, mDayNumbers[i], lunar, CalendarUtils.EVENT_TYPE_NORMAL, viewType, canvas, mSwitchAnimProgress);
        }
    }

    public DayCellViewDrawer getDayCellViewDrawer() {
        if (mCellDrawer == null) {
            mCellDrawer = new MonthByWeekDayViewDrawer(mContext, hasFocus, mCellWidth);
        }
        return mCellDrawer;
    }


    private float getStringWidth(String text) {
        float[] widths = new float[text.length()];
        p.getTextWidths(text, widths);
        float sum = 0f;
        for (float f : widths) {
            sum += f;
        }
        return sum;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (mIsInShakeAnimation) {
            setMeasuredDimension(MeasureSpec.getSize(widthMeasureSpec), mHeight + EXPANDED_HEIGHT * 2);
        } else {
            setMeasuredDimension(MeasureSpec.getSize(widthMeasureSpec), mHeight);
        }
        mCellWidth = (MeasureSpec.getSize(widthMeasureSpec) - sWeekCellTotalInterval) / 7f
                + sWeekCellTotalInterval / 6f;
    }

    protected void updateSelectionPositions() {
        if (mHasSelectedDay) {
            int selectedPosition = mSelectedWeekDay - mWeekStart;
            if (selectedPosition < 0) {
                selectedPosition += 7;
            }
            int effectiveWidth = mWidth - mPadding * 2;
            effectiveWidth -= SPACING_WEEK_NUMBER;
            mSelectedLeft = selectedPosition * effectiveWidth / mNumDays + mPadding;
            mSelectedRight = (selectedPosition + 1) * effectiveWidth / mNumDays + mPadding;
            mSelectedLeft += SPACING_WEEK_NUMBER;
            mSelectedRight += SPACING_WEEK_NUMBER;
        }
    }

    public int getCellPosFromLocation(float x) {
        int dayStart = /*
                        * mShowWeekNum ? (mWidth - mPadding * 2) / mNumCells + mPadding :
                        */mPadding;
        if (x < dayStart || x > mWidth - mPadding) {
            return -1;
        }
        // Selection is (x - start) / (pixels/day) == (x -s) * day / pixels
        return (int) ((x - dayStart) / mCellWidth);
    }

    public Time getDayFromLocation(float x) {
        int dayPosition = getCellPosFromLocation(x);
        if (dayPosition == -1) {
            return null;
        }
        int day = mFirstJulianDay + dayPosition;

        Time time = new Time(mTimeZone);
        if (mWeek == 0) {
            // This week is weird...
//            if (day < Time.EPOCH_JULIAN_DAY) {
//                day++;
//            } else
            if (day == Time.EPOCH_JULIAN_DAY) {
                time.set(1, 0, 1970);
                time.normalize(true);
                return time;
            }
        }

        time.setJulianDay(day);
        // set an hour value to avoid DST affect the weekday, bugId:0051299
        // time of 2015-3-29:00(Atlantic/Azores), weekday is changed from 0 to 6 because of DST.
        time.hour = 4;
        return time;
    }

    @Override
    public boolean dispatchHoverEvent(MotionEvent event) {
        if (mAccessibilityHelper != null && mAccessibilityHelper.dispatchHoverEvent(event)) {
            return true;
        }
        return super.dispatchHoverEvent(event);
    }

    private static AccessibilityManager sAccessibilityManager;
    private static boolean isAccessibilityEnabled(Context context) {
        if (sAccessibilityManager == null) {
            sAccessibilityManager = (AccessibilityManager) context.getSystemService(Service.ACCESSIBILITY_SERVICE);
        }
        return sAccessibilityManager!= null && sAccessibilityManager.isEnabled();
    }

    /**
     * sendAccessibilityEvent when day selected.
     */
    protected void sendAccessibilityEvent(Time selectDay) {
        if (isAccessibilityEnabled(mContext) && selectDay != null) {
            AccessibilityEvent event = AccessibilityEvent.obtain(AccessibilityEvent.TYPE_ANNOUNCEMENT);
            event.setPackageName(mContext.getPackageName());
            event.setClassName(getClass().getName());
            event.setSource(this);
            int flags = DateUtils.FORMAT_SHOW_WEEKDAY | DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_NO_YEAR;
            event.getText().add(mContext.getString(R.string.des_selected)
                    + DateUtils.formatDateTime(mContext, selectDay.toMillis(true), flags));
            getParent().requestSendAccessibilityEvent(this, event);
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
    }

    public void testSetClickedDay(float xLocation) {
        mClickedDayIndex = getCellPosFromLocation(xLocation);
        /* invalidate(); */
    }

    public void testClearClickedDay() {
        mClickedDayIndex = -1;
        /* invalidate(); */
    }

    private View mOriginalView;
    private float mAnimationValue = 0;
    private boolean mIsInShakeAnimation = false;
    private boolean mIsInScaleAnimation = false;
    // expand the view by 30px * 2 in height and width. enough to do the animation
    public int EXPANDED_HEIGHT = 10;
    public int EXPANDED_WIDTH = 10;
    private static final long SHAKE_ANIMATION_DURATION = 200;
    private AnimatorUpdateListener mAnimatorUpdateListener = new AnimatorUpdateListener() {

        @Override
        public void onAnimationUpdate(ValueAnimator animation) {
            mAnimationValue = (Float) animation.getAnimatedValue();
            invalidate();
        }
    };

    private AnimatorUpdateListener mHeightUpdateListener = new AnimatorUpdateListener() {

        @Override
        public void onAnimationUpdate(ValueAnimator animation) {
            mHeight = (Integer) animation.getAnimatedValue();
            updatePercent();
            invalidate();
        }
    };

    private void updatePercent() {
        if (mIsInScaleAnimation) {
            mSwitchAnimProgress = (float) (mHeight - MonthByWeekAdapter.mItemHeight) / (MonthByWeekAdapter.mItemSingleHeight - MonthByWeekAdapter.mItemHeight);
        } else if (mIsSingleWeek) {
            mSwitchAnimProgress = 1f;
        } else {
            mSwitchAnimProgress = 0f;
        }

    }

    public void doSwtichStateAnimation(final boolean isToSingleWeek) {
        mIsInScaleAnimation = true;
        ValueAnimator itemAnimator;
        if (isToSingleWeek) {
            itemAnimator = ValueAnimator.ofInt(MonthByWeekAdapter.mItemHeight, MonthByWeekAdapter.mItemSingleHeight);
        } else {
            itemAnimator = ValueAnimator.ofInt(MonthByWeekAdapter.mItemSingleHeight, MonthByWeekAdapter.mItemHeight);
        }
        itemAnimator.addUpdateListener(mHeightUpdateListener);
        itemAnimator.setDuration(MonthByWeekAdapter.ANIMATION_DURATION_MOVE);
        itemAnimator.addListener(new AnimatorListenerAdapter() {

            @Override
            public void onAnimationCancel(Animator animation) {
                mIsInScaleAnimation = false;
                mIsSingleWeek = isToSingleWeek;
                if (isToSingleWeek) {
                    mSwitchAnimProgress = 1f;// mean single week
                } else {
                    mSwitchAnimProgress = 0f;// mean mutli-weeks
                }
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                mIsInScaleAnimation = false;
                mIsSingleWeek = isToSingleWeek;
                if (isToSingleWeek) {
                    mSwitchAnimProgress = 1f;
                } else {
                    mSwitchAnimProgress = 0f;
                }
            }
        });
        itemAnimator.start();
    }

    public void startShakeAnimation(final ViewGroup dragSupportView) {
        mIsInShakeAnimation = true;
        ValueAnimator zoomIn = ValueAnimator.ofFloat(0f, 0.05f);
        zoomIn.addUpdateListener(mAnimatorUpdateListener);
        ValueAnimator zoomOut = ValueAnimator.ofFloat(0.05f, 0f);
        zoomOut.addUpdateListener(mAnimatorUpdateListener);
        zoomOut.addListener(new AnimatorListenerAdapter() {

            @Override
            public void onAnimationEnd(Animator animation) {
                dragSupportView.removeView(MonthWeekEventsView.this);
                mOriginalView.setVisibility(View.VISIBLE);
                mIsInShakeAnimation = false;
            }
        });
        zoomIn.setDuration(SHAKE_ANIMATION_DURATION);
        zoomOut.setDuration(SHAKE_ANIMATION_DURATION);
        AnimatorSet as = new AnimatorSet();
        as.playSequentially(zoomIn, zoomOut);
        as.start();
        mOriginalView.setVisibility(View.INVISIBLE);
    }

    public void setOriginalView(View view) {
        mOriginalView = view;
    }

    public boolean isInAnimation() {
        return mIsInShakeAnimation;
    }

    private void adjustAnimationBGRect(Rect rect) {
        int dx = (int) (rect.width() * mAnimationValue);
        int dy = (int) (rect.height() * mAnimationValue);
        rect.left -= dx;
        rect.right += dx;
        rect.top -= dy;
        rect.bottom += dy;
    }
}
