package com.smartisanos.ideapills.common.remind;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.SmtPCUtils;
import android.content.Context;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.format.DateFormat;
import android.text.format.DateUtils;
import android.text.style.CharacterStyle;
import android.text.style.RelativeSizeSpan;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.Interpolator;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.ViewSwitcher;

import com.smartisanos.ideapills.common.R;
import com.smartisanos.ideapills.common.remind.util.CalendarUtils;
import com.smartisanos.ideapills.common.remind.util.DateAndTimePickUtil;
import com.smartisanos.ideapills.common.remind.util.SequenceAnimUtils;
import com.smartisanos.ideapills.common.remind.view.DragViewSwitcher;
import com.smartisanos.ideapills.common.remind.view.SmartisanTimePicker1Day;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Formatter;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

import smartisanos.util.DeviceType;
import smartisanos.widget.SettingItemSwitch;

public abstract class AbstractRemindAlarmSettingFragment extends DialogFragment implements
        DragViewSwitcher.DragViewSwitcherListener, View.OnClickListener {

    private final static String TAG = "AbstractRemindAlarmSettingFragment";

    private static final int MESID_JMP = 1;
    private static final int[] SEEK_HOUR_ARRAY = {
            8, 10, 12, 14, 16, 18, 20
    };

    private String mClockFormatString;
    private SimpleDateFormat mClockFormat;
    private SimpleDateFormat mContentDescriptionFormat;

    private static final int AM_PM_STYLE_NORMAL = 0;
    private static final int AM_PM_STYLE_SMALL = 1;
    private static final int AM_PM_STYLE_GONE = 2;
    private final int mAmPmStyle = AM_PM_STYLE_NORMAL;
    private boolean mIs24HourTime;
    protected DragViewSwitcher mViewSwitcher;
    private ViewSwitcher.ViewFactory mViewFactory = new ViewSwitcher.ViewFactory() {

        @Override
        public View makeView() {
            View v = LayoutInflater.from(getActivity()).inflate(
                    R.layout.remind_month_by_week_list, null);
            return v;
        }
    };
    protected ViewGroup mDayNamesHeader;
    protected String[] mDayLabels;
    protected TextView mDayTitle;
    private ImageView mAllPreviousIv;
    private ImageView mAllNextIv;
    private Button mCancelButton = null;
    private Button mConfirmButton = null;
    private SmartisanTimePicker1Day mTimePicker = null;
    private View mRemindDetailView;
    private TextView mNotifyItemRightText;
    private PopupWindow mPopupWindow;
    private long mSelectBeforeTime = 0L;
    private int mSelectPosition = 0;
    private List<RemindData.BeforeTime> mList;
    private int mRemindDetailHeight = -1;
    protected Time mSelectedDay = new Time();
    private final Time mDesiredDay = new Time();
    long mMilliTime;
    protected MonthByWeekAdapter mAdapter;
    private RelativeLayout mWeeksLayout;
    protected int mFirstDayOfWeek;
    protected boolean mFirstJumpCalendar = true;
    public boolean mIsWeeksLayoutInAnimation;
    protected LinkedList<Calendar> needToRunCals = new LinkedList<Calendar>();
    MonthByWeekAdapter.CellEventListener mCellEventListener = new MonthByWeekAdapter.CellEventListener() {

        @Override
        public void onCellClicked(Time day) {
            onDayTapped(day);
        }

        @Override
        public void onCellLongClicked(Time day) {
        }
    };
    protected Time mTempTime = new Time();
    protected Time mFirstDayOfMonth = new Time();
    protected int mCurrentMonthDisplayed;
    private StringBuilder mStringBuilder;
    private Formatter mFormatter;
    protected int mAddTimes;
    private final Time mTodayTime = new Time();
    private final Calendar mTodayCalendar = Calendar.getInstance();
    protected SettingItemSwitch mSwitchFullDay;
    protected Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            int msgId = msg.what;
            if (MESID_JMP == msgId) {
                Calendar targetCal = Calendar.getInstance();
                targetCal.setTimeInMillis(mMilliTime);
                changeCurrentTime(0 - mAddTimes);

                targetCal.set(Calendar.DAY_OF_MONTH, getAndCorrectDay(targetCal));
                jumpToDayForMonth(targetCal, false);
                mAddTimes = 0;
            }
        }
    };

    private Handler mDateHandler = new Handler() {

        public void handleMessage(Message msg) {
            switch (msg.what) {
                case DateAndTimePickUtil.KEY_OK:
                    long startMills = (Long) msg.obj;
                    if (startMills != 0) {
                        Time time = new Time();
                        time.set(startMills);
                        //init first
                        if (mWeeksLayout == null) {
                            mDesiredDay.set(time);
                            mDesiredDay.normalize(true);
                            mMilliTime = time.toMillis(true);
                            updateDateInTitle();
                            goTo(mMilliTime, true);
                            return;
                        }
                        Calendar targetCal = Calendar.getInstance();
                        targetCal.setTimeInMillis(time.toMillis(true));
                        jumpToDayForMonth(targetCal, false);
                    }
                    break;
            }
        }
    };
    private Dialog mTimeDialog;
    private ViewGroup mContent;
    private View.OnTouchListener mTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            int action = event.getAction();
            switch (action) {
                case MotionEvent.ACTION_DOWN:
                    if (mContent == null) {
                        return false;
                    }
                    View view = mContent.getChildAt(0);
                    Rect rect = new Rect();
                    view.getHitRect(rect);
                    if (!rect.contains((int) event.getX(), (int) event.getY())) {
                        onTouchOutside();
                    }
                    break;
            }
            return false;
        }
    };

    protected abstract void onClickCancel();

    protected abstract void onClickConfirm();

    protected abstract void onTouchOutside();

    protected abstract boolean hasBubbleItem();

    protected abstract long getInitDueDate();

    protected abstract long getInitRemindTime();

    protected abstract boolean isSetBubbleWindowType();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mContent = (ViewGroup) inflater.inflate(R.layout.remind_fragment_alarm_setting, container, false);
        mViewSwitcher = (DragViewSwitcher) mContent.findViewById(R.id.main_switcher);
        mViewSwitcher.setFactory(mViewFactory);
        mViewSwitcher.addDragViewSwitcherActor(this);

        mDayNamesHeader = (ViewGroup) mContent.findViewById(R.id.day_names);
        mDayTitle = (TextView) mContent.findViewById(R.id.date_title);
        mDayTitle.setOnClickListener(this);
        mAllPreviousIv = (ImageView) mContent.findViewById(R.id.allinone_image_previous);
        mAllPreviousIv.setOnClickListener(this);
        mAllNextIv = (ImageView) mContent.findViewById(R.id.allinone_image_next);
        mAllNextIv.setOnClickListener(this);

        mCancelButton = (Button) mContent.findViewById(R.id.cancel_action);
        mCancelButton.setOnClickListener(this);
        mConfirmButton = (Button) mContent.findViewById(R.id.confirm_action);
        mConfirmButton.setOnClickListener(this);
        mTimePicker = (SmartisanTimePicker1Day) mContent.findViewById(R.id.time_picker);
        mRemindDetailView = mContent.findViewById(R.id.remind_detail_content);
        mNotifyItemRightText = (TextView) mContent.findViewById(R.id.set_notify_right_text);
        mNotifyItemRightText.setOnClickListener(this);
        mSwitchFullDay = (SettingItemSwitch) mContent.findViewById(R.id.set_time_switch);

        mIs24HourTime = android.text.format.DateFormat.is24HourFormat(getActivity());

        mList = new RemindData().getBeforeData(getActivity());

        long initDueDate = getInitDueDate();
        long notifyTime = getInitRemindTime();
        boolean hasBubble = hasBubbleItem();
        refreshByTime(initDueDate, notifyTime, hasBubble);

        mSwitchFullDay.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                Log.e(TAG, "onCheckedChanged isChecked:" + isChecked);
                switchFullDayView(isChecked);
                if (!isChecked) {
                    setSelectTime();
                }
            }
        });
        mContent.setOnTouchListener(mTouchListener);
        adaptHeightLackScreenWhenCreate(mContent);
        return mContent;
    }

    private void adaptHeightLackScreenWhenCreate(View root) {
        if (DeviceType.isOneOf(DeviceType.ODIN, DeviceType.M1, DeviceType.T2, DeviceType.U1)) {
            ViewGroup.MarginLayoutParams lpPicker = (ViewGroup.MarginLayoutParams) mTimePicker.getLayoutParams();
            lpPicker.topMargin = lpPicker.topMargin / 2;
            lpPicker.bottomMargin = lpPicker.bottomMargin / 2;

            ViewGroup.MarginLayoutParams lpDiv = (ViewGroup.MarginLayoutParams)
                    root.findViewById(R.id.divider_set_time_top_line).getLayoutParams();
            lpDiv.topMargin = lpDiv.topMargin / 2;

            ViewGroup.MarginLayoutParams lpSetT = (ViewGroup.MarginLayoutParams)
                    root.findViewById(R.id.set_time_view).getLayoutParams();
            lpSetT.height = lpSetT.height * 3 / 4;

            ViewGroup.MarginLayoutParams lpSetN = (ViewGroup.MarginLayoutParams)
                    root.findViewById(R.id.set_notify_layout).getLayoutParams();
            lpSetN.height = lpSetN.height * 3 / 4;

            ViewGroup.MarginLayoutParams lpTitle = (ViewGroup.MarginLayoutParams)
                    root.findViewById(R.id.allinone_titilebar).getLayoutParams();
            lpTitle.height = lpTitle.height * 3 / 4;
        }
    }

    public void refreshByTime(long refreshDueDate, long notifyTime, boolean hasBubble) {
        long currentMil = System.currentTimeMillis();
        mTodayTime.set(currentMil);
        mTodayCalendar.setTimeInMillis(currentMil);
        mMilliTime = refreshDueDate;
        if (mMilliTime > 0) {
            mCancelButton.setText(R.string.remind_remove);
        } else {
            mCancelButton.setText(R.string.remind_cancel);
        }

        mMilliTime = mMilliTime < currentMil ? currentMil : mMilliTime;
        final boolean remindHasSet = !hasBubble ? notifyTime > 0 : notifyTime > 0 && refreshDueDate > 0L;

        if (remindHasSet) { // need notify
            long dueDate = !hasBubble ? mMilliTime : refreshDueDate;
            mSelectBeforeTime = dueDate - notifyTime;
            setRemindBeforeTime();
        }

        if (mMilliTime > 0) {
            mSelectedDay.set(mMilliTime);
            mTimePicker.setCurrentHour(mSelectedDay.hour);
            mTimePicker.setCurrentMinute(mSelectedDay.minute);
            mSwitchFullDay.setChecked(!remindHasSet); // set remind follow previous settings
        } else {
            mSwitchFullDay.setChecked(true);    // default set to true
            mMilliTime = System.currentTimeMillis();
            mSelectedDay.set(mMilliTime);
        }
        mRemindDetailView.setVisibility(mSwitchFullDay.isChecked() ? View.GONE : View.VISIBLE);
        mStringBuilder = new StringBuilder(50);
        mFormatter = new Formatter(mStringBuilder, Locale.getDefault());

        setUpHeader();
        setupAdapter();
        goTo(mMilliTime, true);
        setupWeeksLayout(true);
        updateDayNamesHeader();
        mAdapter.setSelectedDay(mSelectedDay);
    }

    protected long getSelectBeforeTime() {
        return mSelectBeforeTime;
    }

    protected String getSelectBeforeTip() {
        return getString(R.string.remind_notify_time_expire);
    }

    @Override
    public void onStart() {
        super.onStart();
        updateDateInTitle();
    }

    @Override
    public void onStop() {
        super.onStop();
        if (mTimeDialog != null) {
            mTimeDialog.dismiss();
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.cancel_action:
                onClickCancel();
                break;
            case R.id.confirm_action:
                onClickConfirm();
                break;
            case R.id.date_title:
                Time time = new Time();
                time.set(mMilliTime);
                Activity activity = getActivity();
                boolean setBubbleWindowType = isSetBubbleWindowType();
                if (SmtPCUtils.isValidExtDisplayId(activity)) {
                    mTimeDialog = DateAndTimePickUtil.createDatePickerExDialog(activity, mDateHandler, time);
                } else {
                    mTimeDialog = DateAndTimePickUtil.createDatePickerDialog(activity, mDateHandler,
                            time, setBubbleWindowType);
                }
                break;
            case R.id.allinone_image_previous:
                if (!needToRunCals.isEmpty()) {
                    // the jump animation has not ended, just return for this click.
                    // BugId:0028445
                    return;
                }

                if (isBeforeCurrentTime()) {
                    // must not go to current time before
                    return;
                }

                // 1.update time and title; 2. send jump msg to animate;
                if (changeCurrentTime(-1)) {
                    mAddTimes--;
                    updateDateInTitle();
                }
                mHandler.removeMessages(MESID_JMP);
                Message preMsg = mHandler.obtainMessage();
                preMsg.what = MESID_JMP;
                mHandler.sendMessageDelayed(preMsg, 200); // Delay execute animate 200ms
                break;
            case R.id.allinone_image_next:
                if (!needToRunCals.isEmpty()) {
                    // the jump animation has not ended, just return for this click.
                    // BugId:0028445
                    return;
                }
                if (changeCurrentTime(1)) {
                    mAddTimes++;
                    updateDateInTitle();
                }
                mHandler.removeMessages(MESID_JMP);
                Message nextMsg = mHandler.obtainMessage();
                nextMsg.what = MESID_JMP;
                mHandler.sendMessageDelayed(nextMsg, 200); // Delay execute animate 200ms
                break;
            case R.id.set_notify_right_text:
                showBeforeTimeView();
                break;
            default:
                break;
        }
    }

    private void setupAdapter() {
        mFirstDayOfWeek = CalendarUtils.getFirstDayOfWeekInTime(getActivity());

        HashMap<String, Integer> weekParams = new HashMap<String, Integer>();
        weekParams.put(MonthByWeekAdapter.WEEK_PARAMS_NUM_WEEKS, 6);
        weekParams.put(MonthByWeekAdapter.WEEK_PARAMS_SINGLE_WEEK, 0);

        weekParams.put(MonthByWeekAdapter.WEEK_PARAMS_WEEK_START,
                mFirstDayOfWeek);
        weekParams.put(MonthByWeekAdapter.WEEK_PARAMS_JULIAN_DAY,
                Time.getJulianDay(mSelectedDay.toMillis(true), mSelectedDay.gmtoff));
        weekParams.put(MonthByWeekAdapter.WEEK_PARAMS_DAYS_PER_WEEK, 7);
        weekParams.put(MonthByWeekAdapter.WEEK_PARAMS_FOCUS_MONTH, mSelectedDay.month);
        if (mAdapter == null) {
            mAdapter = new MonthByWeekAdapter(getActivity(), weekParams, mCellEventListener);
        } else {
            mAdapter.updateParams(weekParams);
        }
    }

    protected void setupWeeksLayout(boolean useCurrentView) {
        // Configure the listview
        View v = useCurrentView ? mViewSwitcher.getCurrentView() : mViewSwitcher.getNextView();
        mWeeksLayout = (RelativeLayout) v.findViewById(R.id.linearlayout_list);
        mAdapter.setWeeksLayout(mWeeksLayout);
    }

    protected void setUpHeader() {
        mDayLabels = new String[7];
        for (int i = Calendar.SUNDAY; i <= Calendar.SATURDAY; i++) {
            mDayLabels[i - Calendar.SUNDAY] = DateUtils.getDayOfWeekString(i,
                    DateUtils.LENGTH_MEDIUM).toUpperCase();
        }
    }

    private void onDayTapped(Time tappedDay) {
        if (tappedDay == null || !CalendarUtils.isValidDay(tappedDay.toMillis(true))) {
            return;
        }

        if (tappedDay.year < mTodayTime.year || (tappedDay.year == mTodayTime.year && tappedDay.yearDay < mTodayTime.yearDay)) {
            // must not select current time before
            return;
        }

        if (!CalendarUtils.isTheSameDay(tappedDay, mSelectedDay)) {
            mSelectedDay = tappedDay;
            mMilliTime = mSelectedDay.toMillis(true);
            mAdapter.onDayTapped(mSelectedDay);
        }
    }

    @Override
    public boolean prepareNextView(int changeKind) {
        Calendar linkCal = getMonthCalendarByOffset(1);
        if (linkCal == null) {
            return false;
        }
        return prepareFollowingView(changeKind, true, linkCal);
    }

    @Override
    public boolean preparePreviouseView(int changeKind) {
        if (isBeforeCurrentTime()) {
            return false;
        }

        Calendar linkCal = getMonthCalendarByOffset(-1);
        if (linkCal == null) {
            return false;
        }
        return prepareFollowingView(changeKind, false, linkCal);
    }

    protected boolean prepareFollowingView(int changeKind, boolean isNext, Calendar c) {
        mFirstJumpCalendar = true;
        if (changeKind == DragViewSwitcher.X_CHANGE) {
            return prepareJumpNextView(isNext, c);
        }
        return false;
    }

    protected boolean prepareJumpNextView(boolean next, Calendar... c) {
        if (!CalendarUtils.isValidDay(c[0].getTimeInMillis())) {
            return false;
        }
        if (mIsWeeksLayoutInAnimation) {
            needToRunCals.add(c[0]);
            return false;
        }
        // set up weeks layout for animation, use viewSwitcher's next view
        setupWeeksLayout(false);
        mMilliTime = c[0].getTimeInMillis();
        goTo(mMilliTime, c.length == 1);

        for (int i = 1; i < c.length; i++) {
            needToRunCals.add(c[i]);
        }

        int size = needToRunCals.size();
        setupScrollAnim(next, getAniminationListener(next, c[0], size),
                SequenceAnimUtils.getInterpolator(size, size),
                SequenceAnimUtils.getDuration(size, size));
        return true;
    }

    protected Animation.AnimationListener getAniminationListener(final boolean next, final Calendar c, final int allCount) {
        return new Animation.AnimationListener() {

            @Override
            public void onAnimationRepeat(Animation animation) {
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                if (!needToRunCals.isEmpty()) {
                    updateDateInTitle();
                    Calendar calendar = needToRunCals.getFirst();
                    setupWeeksLayout(false);
                    mAdapter.setSelectedDay(getTime(calendar), needToRunCals.size() == 1);
                    needToRunCals.removeFirst();
                    Interpolator interpolator = SequenceAnimUtils
                            .getInterpolator(allCount, needToRunCals.size());
                    int duration = SequenceAnimUtils.getDuration(allCount, needToRunCals.size());
                    mMilliTime = calendar.getTimeInMillis();
                    goTo(mMilliTime, true);
                    setupScrollAnim(next, getAniminationListener(next, calendar, needToRunCals.size() + 1), interpolator, duration);
                    mViewSwitcher.showNext();
                } else {
                    animationEnd(/*curCal,*/c);
                    mIsWeeksLayoutInAnimation = false;
                }
            }

            @Override
            public void onAnimationStart(Animation animation) {
                // put it in onAnimationEnd to make sure animation runs smoothly, BugId:0047940
                mIsWeeksLayoutInAnimation = true;
            }
        };
    }

    protected void animationEnd(/*Calendar curCal,*/Calendar targetC) {
        Time end = new Time();
        end.set(targetC.getTimeInMillis());
        mSelectedDay = end;
        updateDateInTitle();
    }

    protected void setupScrollAnim(boolean next, Animation.AnimationListener listener,
                                   Interpolator interpolator, int duration) {
        Animation monthIn = null;
        Animation monthOut = null;
        if (next) {
            monthIn = AnimationUtils.loadAnimation(getActivity(),
                    R.anim.remind_week_left_in);
            monthOut = AnimationUtils.loadAnimation(
                    getActivity(), R.anim.remind_week_left_out);
        } else {
            monthIn = AnimationUtils.loadAnimation(getActivity(),
                    R.anim.remind_week_right_in);
            monthOut = AnimationUtils.loadAnimation(
                    getActivity(), R.anim.remind_week_right_out);
        }

        monthIn.setDuration(duration);
        mViewSwitcher.setInAnimation(monthIn);
        monthOut.setDuration(duration);
        mViewSwitcher.setOutAnimation(monthOut);
        monthIn.setAnimationListener(listener);
        if (interpolator != null) {
            monthIn.setInterpolator(interpolator);
            monthOut.setInterpolator(interpolator);
        }
    }

    protected Calendar getMonthCalendarByOffset(int offset) {
        // ignore the focusMonth the first time when continuously quick click jump.
        Calendar c = getCalendarBeforeJump(!mFirstJumpCalendar);
        mFirstJumpCalendar = false;
        c.add(Calendar.MONTH, offset);
        if (mSelectedDay.month == c.get(Calendar.MONTH)
                && mSelectedDay.year == c.get(Calendar.YEAR)) {
            // keep the monthDay unchanged if in same month
            c.set(Calendar.DAY_OF_MONTH, mSelectedDay.monthDay);
        } else {
            c.set(Calendar.DAY_OF_MONTH, getAndCorrectDay(c));
        }
        if (c.get(Calendar.YEAR) > CalendarUtils.MAX_CALENDAR_YEAR
                || c.get(Calendar.YEAR) < CalendarUtils.MIN_CALENDAR_YEAR) {
            return null;
        }
        return c;
    }

    private boolean changeCurrentTime(int var) {
        Calendar c = getMonthCalendarByOffset(var);
        if (c == null) {
            return false;
        }
        mMilliTime = c.getTimeInMillis();
        return true;
    }

    private boolean isBeforeCurrentTime() {
        Calendar calendar = getMonthCalendarByOffset(-1);
        if (calendar == null) {
            return true;
        }

        int preYear = calendar.get(Calendar.YEAR);
        int preMonth = calendar.get(Calendar.MONTH);
        if (preYear > mTodayTime.year) {
            return false;
        }

        if (preYear == mTodayTime.year) {
            return preMonth < mTodayTime.month;
        }
        return true;
    }

    /**
     * Jump should be based on the focus month, and happens in three ways:
     * <br>1, press the pre/next button in tittle.
     * <br>2, scroll up and down in month view
     * <br>3, select a day to jump when tap on title.
     * <br> this method will called in all three ways to make sure the jump right.
     *
     * @param ignoreFoucsMonth this param handles the case continuously quick click jump in case 1.
     */
    private Calendar getCalendarBeforeJump(boolean ignoreFoucsMonth) {
        Calendar start = Calendar.getInstance();
        int focusMonth = mAdapter.getFocusMonth();
        if (ignoreFoucsMonth || focusMonth == mSelectedDay.month) {
            if (ignoreFoucsMonth) {
                // ignoreFoucsMonth means continuously quick click jump in case 1.
                // the mMilliTime will change with each one click. bugId:0050738
                start.setTimeInMillis(mMilliTime);
            } else {
                // when timezone changed, the date may changed. bugId:0050045
                start.setTimeInMillis(mSelectedDay.toMillis(true));
            }
        } else {
            // BugId:0042825. The selected day is not in focus month,
            // new a calendar in focus month to make the jump animation right.
            // caused by feature:0039879. Do it once every jump(mAddTimes==1)
            Time temp = getTimeByFocusMonth(focusMonth);
            start.setTimeInMillis(temp.normalize(true));
        }
        return start;
    }

    protected Time getTimeByFocusMonth(int focusMonth) {
        Time temp = new Time(mSelectedDay);
        // change the year!!!
        if (focusMonth == 11 && mSelectedDay.month == 0) {
            temp.year -= 1;
        } else if (focusMonth == 0 && mSelectedDay.month == 11) {
            temp.year += 1;
        }
        temp.month = mAdapter.getFocusMonth();
        // set monthDay to the middle of the month.
        // so that normalize will not change the month(29/2->1/3)
        temp.monthDay = 15;
        return temp;
    }

    protected int jumpToDayForMonth(Calendar targetCal, boolean isDropEnd) {
        mFirstJumpCalendar = true;
        Calendar startCal = getCalendarBeforeJump(false);

        if (mSelectedDay.month == targetCal.get(Calendar.MONTH)
                && mSelectedDay.year == targetCal.get(Calendar.YEAR)) {
            // keep the monthDay unchanged if in same month
            // Math.abs(mAddTimes) == 1 means the case press pre/next button.
            if (Math.abs(mAddTimes) == 1) {
                targetCal.set(Calendar.DAY_OF_MONTH, mSelectedDay.monthDay);
            }
        }
        boolean sameMonth = isSameCalendarByFields(startCal, targetCal, Calendar.YEAR, Calendar.MONTH);
        if (sameMonth) {
            mMilliTime = targetCal.getTimeInMillis();
            goTo(mMilliTime, true);
            animationEnd(targetCal);
            return 0;
        }
        boolean next = startCal.before(targetCal);
        Calendar[] needToJumpCalendars = getNeedToJumpCalendar(startCal, targetCal,
                Calendar.MONTH, 1, Calendar.YEAR, Calendar.MONTH);
        if (prepareJumpNextView(next, needToJumpCalendars)) {
            mViewSwitcher.showNext();
        }
        return 0;//SequenceAnimUtils.getDuration(0, needToJumpCalendars.length);
    }

    /**
     * for simplicity, we use the same day(targetTime) during the animations.
     * this method only help us to calculate how many animations need be run(3 at most).
     */
    protected Calendar[] getNeedToJumpCalendar(Calendar curSelectTime, Calendar targetTime, int field, int diffy, int... compareFiled) {

        ArrayList<Calendar> cals = new ArrayList<Calendar>();

        final boolean after = curSelectTime.after(targetTime);
        Calendar start = after ? targetTime : curSelectTime;
        Calendar end = after ? curSelectTime : targetTime;

        // the first
        cals.add(targetTime);

        Calendar theOneAfterStart = getCalendarByOffsetInField(start, field, diffy);
        if (!isSameCalendarByFields(theOneAfterStart, end, compareFiled) && end.after(theOneAfterStart)) {
            // the second
            cals.add(targetTime);
            Calendar theOneBefore = getCalendarByOffsetInField(end, field, 0 - diffy);
            if (!isSameCalendarByFields(theOneBefore, theOneAfterStart, compareFiled)) {
                // the third
                cals.add(targetTime);
            }
        }
        return cals.toArray(new Calendar[0]);
    }

    protected boolean isSameCalendarByFields(Calendar a, Calendar b, int... compareFiled) {
        for (int i = 0; i < compareFiled.length; i++) {
            if (compareFiled[i] == Calendar.WEEK_OF_YEAR) {
                CalendarUtils.setFisrtDayOfWeek(getActivity(), new Calendar[]{a, b});
            }
            if (a.get(compareFiled[i]) != b.get(compareFiled[i])) {
                return false;
            }
        }
        return true;
    }

    /**
     * This moves to the specified time in the view. If the time is not already
     * in range it will move the list so that the first of the month containing
     * the time is at the top of the view. If the new time is already in view
     * the list will not be scrolled unless forceScroll is true. This time may
     * optionally be highlighted as selected as well.
     *
     * @param time     The time to move to
     * @param hasFocus Whether this view has focus--to draw the backgrounds.
     * @return Whether or not the view animated to the new location
     */
    public boolean goTo(long time, boolean hasFocus) {
        if (time == -1) {
            Log.e(TAG, "time is invalid");
            return false;
        }

        // Set the selected day
        mSelectedDay.set(time);
        mSelectedDay.normalize(true);

        mTempTime.set(time);
        long millis = mTempTime.normalize(true);
        // Get the week position we're going to
        int position;
        // Check if the selected day is now outside of our visible range
        // and if so scroll to the month that contains it
        mFirstDayOfMonth.set(mTempTime);

        mFirstDayOfMonth.monthDay = 1;
        millis = mFirstDayOfMonth.normalize(true);
        setMonthDisplayed(mFirstDayOfMonth, true);

        position = CalendarUtils.getWeeksSinceEpochFromJulianDay(
                Time.getJulianDay(millis, mFirstDayOfMonth.gmtoff), mFirstDayOfWeek);

        mAdapter.setPosition(position);
        mAdapter.setSelectedDay(mSelectedDay, hasFocus);
        return false;
    }

    protected void setMonthDisplayed(Time time, boolean updateHighlight) {
        mCurrentMonthDisplayed = time.month;
        if (updateHighlight) {
            mAdapter.updateFocusMonth(mCurrentMonthDisplayed);
        }
        if (mSelectedDay.minute >= 30) {
            mSelectedDay.minute = 30;
        } else {
            mSelectedDay.minute = 0;
        }
    }

    protected String buildMonthYearDate() {
        mStringBuilder.setLength(0);
        long timeMillis = mMilliTime;
        if (mFirstJumpCalendar) {
            // mainly for onResume, the mMilliTime/selectedDay may not in the focus month.
            Time t = new Time();
            t.set(mMilliTime);
            if (t.month != mAdapter.getFocusMonth()) {
                Time temp = getTimeByFocusMonth(mAdapter.getFocusMonth());
                timeMillis = temp.toMillis(true);
            }
        }
        String date = DateUtils.formatDateRange(
                getActivity(),
                mFormatter,
                timeMillis,
                timeMillis,
                DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_NO_MONTH_DAY
                        | DateUtils.FORMAT_SHOW_YEAR, mAdapter.getHomeTimeZone()).toString();
        return date;
    }

    protected void updateDateInTitle() {
        String dateStr;
        dateStr = buildMonthYearDate();
        mDayTitle.setText(dateStr);
        mAllNextIv.setVisibility(CalendarUtils.turningMonthIsValid(mSelectedDay, true)
                ? View.VISIBLE : View.INVISIBLE);
        mAllPreviousIv.setVisibility(CalendarUtils.turningMonthIsValid(mSelectedDay, false)
                ? View.VISIBLE : View.INVISIBLE);
    }

    /**
     * Fixes the day names header to provide correct spacing and updates the
     * label text. Override this to set up a custom header.
     */
    protected void updateDayNamesHeader() {
        TextView label;
        int offset = mFirstDayOfWeek;
        for (int i = 0; i < 7; i++) {
            label = (TextView) mDayNamesHeader.getChildAt(i);
//            if (i < 7 + 1) {
                int position = (offset + i) % 7;
                label.setText(mDayLabels[position]);
                label.setVisibility(View.VISIBLE);
//            } else {
//                label.setVisibility(View.GONE);
//            }
        }
    }

    protected boolean isSwitchFullDayChecked() {
        return mSwitchFullDay.isChecked();
    }

    protected final long getRemindTime() {
        if (mTimePicker.getCurrentHour() < 0) { // select the whole day.
            mSelectedDay.hour = 0;
            mSelectedDay.minute = 0;
            mSelectedDay.second = 0;
        } else {
            mSelectedDay.hour = mTimePicker.getCurrentHour();
            mSelectedDay.minute = mTimePicker.getCurrentMinute();
            mSelectedDay.second = 0;
        }
        mMilliTime = mSelectedDay.toMillis(true);
        return mMilliTime;
    }

    protected final CharSequence getSmallTime(long time) {
        Context context = getActivity();
        boolean is24 = DateFormat.is24HourFormat(context);

        final char MAGIC1 = '\uEF00';
        final char MAGIC2 = '\uEF01';

        SimpleDateFormat sdf;
        String format = is24 ? "H:mm" : "h:mm a";
        if (!format.equals(mClockFormatString)) {
            mContentDescriptionFormat = new SimpleDateFormat(format);
            /*
             * Search for an unquoted "a" in the format string, so we can
             * add dummy characters around it to let us find it again after
             * formatting and change its size.
             */
            if (mAmPmStyle != AM_PM_STYLE_NORMAL) {
                int a = -1;
                boolean quoted = false;
                for (int i = 0; i < format.length(); i++) {
                    char c = format.charAt(i);

                    if (c == '\'') {
                        quoted = !quoted;
                    }
                    if (!quoted && c == 'a') {
                        a = i;
                        break;
                    }
                }

                if (a >= 0) {
                    // Move a back so any whitespace before AM/PM is also in the alternate size.
                    final int b = a;
                    while (a > 0 && Character.isWhitespace(format.charAt(a - 1))) {
                        a--;
                    }
                    format = format.substring(0, a) + MAGIC1 + format.substring(a, b)
                            + "a" + MAGIC2 + format.substring(b + 1);
                }
            }
            mClockFormat = sdf = new SimpleDateFormat(format);
            mClockFormatString = format;
        } else {
            sdf = mClockFormat;
        }
        String result = sdf.format(time);

        if (mAmPmStyle != AM_PM_STYLE_NORMAL) {
            int magic1 = result.indexOf(MAGIC1);
            int magic2 = result.indexOf(MAGIC2);
            if (magic1 >= 0 && magic2 > magic1) {
                SpannableStringBuilder formatted = new SpannableStringBuilder(result);
                if (mAmPmStyle == AM_PM_STYLE_GONE) {
                    formatted.delete(magic1, magic2 + 1);
                } else {
                    if (mAmPmStyle == AM_PM_STYLE_SMALL) {
                        CharacterStyle style = new RelativeSizeSpan(0.7f);
                        formatted.setSpan(style, magic1, magic2,
                                Spannable.SPAN_EXCLUSIVE_INCLUSIVE);
                    }
                    formatted.delete(magic2, magic2 + 1);
                    formatted.delete(magic1, magic1 + 1);
                }
                return formatted;
            }
        }
        return result;
    }

    private Calendar getCalendarByOffsetInField(Calendar refCalendar, int field, int offset) {
        Calendar tempCal = Calendar.getInstance();
        tempCal.setTimeInMillis(refCalendar.getTimeInMillis());
        tempCal.add(field, offset);
        return tempCal;
    }

    protected Time getTime(Calendar calendar) {
        Time time = new Time();
        time.set(calendar.getTime().getTime());
        return time;
    }

    private int getDetailContentHeight() {
        int w = View.MeasureSpec.makeMeasureSpec(0,
                View.MeasureSpec.UNSPECIFIED);
        int h = View.MeasureSpec.makeMeasureSpec(0,
                View.MeasureSpec.UNSPECIFIED);
        mRemindDetailView.measure(w, h);
        return mRemindDetailView.getMeasuredHeight();
    }

    private void switchFullDayView(final boolean isFullDay) {
        if (mRemindDetailHeight < 0) {
            mRemindDetailHeight = getDetailContentHeight();
        }
        int start = isFullDay ? mRemindDetailHeight : 0;
        int end = isFullDay ? 0 : mRemindDetailHeight;
        ValueAnimator animator = ValueAnimator.ofInt(start, end);
        animator.setDuration(300);
        animator.setInterpolator(new AccelerateInterpolator() {
            @Override
            public float getInterpolation(float input) {
                float t = input;
                if ((t *= 2) < 1) return 0.5f * t * t * t;
                return 0.5f * ((t -= 2) * t * t + 2);
            }
        });
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                int value = (int) animation.getAnimatedValue();
                ViewGroup.LayoutParams layoutParams = mRemindDetailView.getLayoutParams();
                layoutParams.height = value;
                mRemindDetailView.setLayoutParams(layoutParams);
            }
        });
        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                if (!isFullDay) {
                    mRemindDetailView.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                mRemindDetailView.setVisibility(isFullDay ? View.GONE : View.VISIBLE);
            }
        });
        animator.start();
    }

    private int getAndCorrectDay(Calendar calendar) {
        int dayOfMonth = 1;
        if (calendar.get(Calendar.YEAR) == mTodayTime.year && calendar.get(Calendar.MONTH) <= mTodayTime.month) {
            dayOfMonth = mTodayTime.monthDay;
        }
        return dayOfMonth;
    }

    private void setSelectTime() {
        if (mSelectedDay.year == mTodayTime.year && mSelectedDay.month == mTodayTime.month && mSelectedDay.yearDay == mTodayTime.yearDay) {
            // pm require set next hour
            int tempHour = mTodayCalendar.get(Calendar.HOUR_OF_DAY);
            int tempMin;

            if (tempHour == 23) {
                Log.d("setTime", "mIs24HourTime:" + mIs24HourTime);
                tempMin = 59;
            } else {
                tempHour++;
                tempMin = 0;
            }

            mTimePicker.setCurrentHour(tempHour);
            mTimePicker.setCurrentMinute(tempMin);
            return;
        }

        // pm require default set 8 hour
        mTimePicker.setCurrentHour(8);
        mTimePicker.setCurrentMinute(0);
    }

    private void showBeforeTimeView() {
        Context context = getActivity();
        if (context == null) {
            return;
        }
        View contentView = LayoutInflater.from(context).inflate(R.layout.show_before_time_layout, null);
        ListView listView = (ListView) contentView.findViewById(R.id.time_sort_type_list);

        final BeforeTimeAdapter adapter = new BeforeTimeAdapter(context, mList);
        listView.setAdapter(adapter);
        listView.setEnabled(false);
        adapter.setItemClickListener(new BeforeTimeAdapter.ItemClickListener() {
            @Override
            public void onItemClick(int position, boolean select) {
                if (select) {
                    handleSelectBeforeTime((RemindData.BeforeTime) adapter.getItem(position));
                } else {
                    if (mPopupWindow != null && mPopupWindow.isShowing()) {
                        mPopupWindow.dismiss();
                    }
                }
            }
        });
        adapter.setSelectPosition(mSelectPosition);

        mPopupWindow = new PopupWindow(context);
        mPopupWindow.setWindowLayoutMode(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        mPopupWindow.setContentView(contentView);
        mPopupWindow.setOutsideTouchable(true);
        mPopupWindow.setBackgroundDrawable(null);
        mPopupWindow.setAnimationStyle(R.style.RemindPopupAnim);
        // Set it focusable so that it can receive back key press event
        mPopupWindow.setFocusable(true);
        mPopupWindow.setClippingEnabled(false);
        // imag src in view offset 10
        int location[] = new int[2];
        mNotifyItemRightText.getLocationInWindow(location);
        int xOffset = context.getResources().getDimensionPixelOffset(R.dimen.reminder_margin_right);
        int yOffset = location[1] - context.getResources().getDimensionPixelOffset(R.dimen.remind_picker_height) - mNotifyItemRightText.getHeight();
        mPopupWindow.showAtLocation(mNotifyItemRightText, Gravity.RIGHT | Gravity.TOP, -xOffset, yOffset);
    }

    private void handleSelectBeforeTime(RemindData.BeforeTime item) {
        if (mPopupWindow != null && mPopupWindow.isShowing()) {
            mPopupWindow.dismiss();
        }

        mSelectBeforeTime = item.time;
        mSelectPosition = item.position;
        mNotifyItemRightText.setText(item.text);
    }

    protected boolean isExpireTime(long remindTime) {
        return isExpireTime(remindTime, false);
    }
    
    protected boolean isExpireTime(long remindTime, boolean isJustCalcToDay) {
        Time temp = new Time();
        temp.set(remindTime);

        if (temp.year < mTodayTime.year) {
            // expire
            return true;
        }

        if (temp.year == mTodayTime.year) {
            if (temp.yearDay < mTodayTime.yearDay) {
                return true;
            }

            if (temp.yearDay == mTodayTime.yearDay && !isJustCalcToDay) {
                if (temp.hour < mTodayTime.hour) {
                    return true; // expire
                }

                if (temp.hour == mTodayTime.hour) {
                    return temp.minute <= mTodayTime.minute;
                }
            }
        }
        return false;
    }

    private void setRemindBeforeTime() {
        if (mSelectBeforeTime <= 0L || mList == null || mList.isEmpty()) {
            return;
        }

        for (RemindData.BeforeTime item : mList) {
            if (mSelectBeforeTime == item.time) {
                mSelectPosition = item.position;
                mNotifyItemRightText.setText(item.text);
                break;
            }
        }
    }
}
