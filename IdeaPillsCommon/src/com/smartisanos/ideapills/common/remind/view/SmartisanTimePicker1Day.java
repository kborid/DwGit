
package com.smartisanos.ideapills.common.remind.view;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.format.DateUtils;
import android.text.format.Time;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.Button;
import android.widget.FrameLayout;

import com.smartisanos.ideapills.common.R;

import java.text.DateFormatSymbols;
import java.util.Calendar;
import java.util.Locale;

/**
 * A view for selecting the time of day, in either 24 hour or AM/PM mode. The
 * hour, each minute digit, and AM/PM (if applicable) can be conrolled by
 * vertical spinners. The hour can be entered by keyboard input. Entering in two
 * digit hours can be accomplished by hitting two digits within a timeout of
 * about a second (e.g. '1' then '2' to select 12). The minutes can be entered
 * by entering single digits. Under AM/PM mode, the user can hit 'a', 'A", 'p'
 * or 'P' to pick. For a dialog using this view, see
 * {@link android.app.TimePickerDialog}.
 * <p>
 * See the <a href="{@docRoot}guide/topics/ui/controls/pickers.html">Pickers</a>
 * guide.
 * </p>
 */
public class SmartisanTimePicker1Day extends FrameLayout {

    private static final boolean DEFAULT_ENABLED_STATE = true;

    private static final int HOURS_IN_HALF_DAY = 12;

    // state
    private boolean mIs24HourView;

    private boolean mIsAm;

    // ui components
    public SmartisanNumberPicker1Day mHourSpinner;

    public SmartisanNumberPicker1Day mMinuteSpinner;

    public SmartisanNumberPicker1Day mAmPmSpinner;

    public View mAmPmDividerView;
    // Note that the legacy implementation of the TimePicker is
    // using a button for toggling between AM/PM while the new
    // version uses a NumberPicker spinner. Therefore the code
    // accommodates these two cases to be backwards compatible.
    private Button mAmPmButton;

    private String[] mAmPmStrings;

    private boolean mIsEnabled = DEFAULT_ENABLED_STATE;

    private Calendar mTempCalendar;

    private Locale mCurrentLocale;

    public SmartisanTimePicker1Day(Context context) {
        this(context, null);

    }

    public SmartisanTimePicker1Day(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SmartisanTimePicker1Day(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        // initialization based on locale
        setCurrentLocale(Locale.getDefault());

        LayoutInflater inflater = (LayoutInflater) context.getSystemService(
                Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.remind_time_picker_1_day, this, true);

        Resources res = getResources();
        final int normalColor = res.getColor(R.color.date_pick_normal_day_color);
        final int selectColor = res.getColor(R.color.calendar_date_pick_today_color);

        // hour
        mHourSpinner = (SmartisanNumberPicker1Day) findViewById(R.id.hour);
        mHourSpinner.setTextSize(getResources().getDimensionPixelSize(R.dimen.no_year_picker_text_size),
                getResources().getDimensionPixelSize(R.dimen.no_year_picker_text_size_highlight));
        mHourSpinner.setTextColor(normalColor, selectColor);
        mHourSpinner.setOnValueChangedListener(new SmartisanNumberPicker1Day.OnValueChangeListener() {
            public void onValueChange(SmartisanNumberPicker1Day spinner, int oldVal, int newVal) {
                if (!is24HourView()) {
                    if ((oldVal == HOURS_IN_HALF_DAY - 1 && newVal == HOURS_IN_HALF_DAY)
                            || (oldVal == HOURS_IN_HALF_DAY && newVal == HOURS_IN_HALF_DAY - 1)) {
                        mIsAm = !mIsAm;
                        updateAmPmControl();
                    }
                } else {
                    if (newVal == 0 || oldVal == 0) {
                        onHourChanged(newVal);
                    }
                }
            }
        });
        mHourSpinner.setFormatter(new SmartisanNumberPicker1Day.Formatter() {
            @Override
            public String format(int value) {
                return SmartisanNumberPicker1Day.getTwoDigitFormatter().format(value);
            }
        });


        mMinuteSpinner = (SmartisanNumberPicker1Day) findViewById(R.id.minute);
        mMinuteSpinner.setMinValue(0);
        mMinuteSpinner.setMaxValue(60);
        mMinuteSpinner.setWrapSelectorWheel(true);
        // mMinuteSpinner.setOnLongPressUpdateInterval(100);
        mMinuteSpinner.setFormatter(new SmartisanNumberPicker1Day.Formatter() {
            @Override
            public String format(int value) {
                if (value == 0) {
                    return "";
                } else {
                    return SmartisanNumberPicker1Day.getTwoDigitFormatter().format(value - 1);
                }
            }
        });
        mMinuteSpinner.setTextSize(getResources().getDimensionPixelSize(R.dimen.no_year_picker_text_size),
                getResources().getDimensionPixelSize(R.dimen.no_year_picker_text_size_highlight));
        mMinuteSpinner.setTextColor(normalColor, selectColor);

        /* Get the localized am/pm strings and use them in the spinner */
        mAmPmStrings = new DateFormatSymbols().getAmPmStrings();
        mAmPmDividerView = findViewById(R.id.am_pm_divider);
        // am/pm
        View amPmView = findViewById(R.id.amPm);
        if (amPmView instanceof Button) {
            mAmPmSpinner = null;
            mAmPmButton = (Button) amPmView;
            mAmPmButton.setOnClickListener(new OnClickListener() {
                public void onClick(View button) {
                    button.requestFocus();
                    mIsAm = !mIsAm;
                    updateAmPmControl();
                }
            });
        } else {
            mAmPmButton = null;
            mAmPmSpinner = (SmartisanNumberPicker1Day) amPmView;
            mAmPmSpinner.setMinValue(0);
            mAmPmSpinner.setMaxValue(1);
            mAmPmSpinner.setDisplayedValues(mAmPmStrings);
            mAmPmSpinner
                    .setOnValueChangedListener(new SmartisanNumberPicker1Day.OnValueChangeListener() {
                        public void onValueChange(SmartisanNumberPicker1Day picker, int oldVal,
                                                  int newVal) {
                            picker.requestFocus();
                            mIsAm = !mIsAm;
                            updateAmPmControl();
                        }
                    });
            mAmPmSpinner.setTextSize(getResources().getDimensionPixelSize(R.dimen.no_year_picker_text_size), getResources().getDimensionPixelSize(R.dimen.no_year_picker_text_size_highlight));
            mAmPmSpinner.setTextColor(normalColor, selectColor);
        }

//        mRemindSpinner = (SmartisanNumberPicker1Day) findViewById(R.id.remind);
//        mRemindSpinner.setTextSize(getResources().getDimensionPixelSize(R.dimen.no_year_picker_text_size),
//                getResources().getDimensionPixelSize(R.dimen.no_year_picker_text_size_highlight));
//        mRemindSpinner.setTextColor(normalColor, selectColor);
//        mRemindSpinner.setMinValue(0);
//        mRemindSpinner.setMaxValue(1);
//        mRemindSpinner.setDisplayedValues(getResources().getStringArray(R.array.one_day_for_12_arr));
        onHourChanged(mHourSpinner.getValue());

        setIs24HourView(android.text.format.DateFormat.is24HourFormat(context));

        // update controls to initial state
        updateHourControl();
        updateAmPmControl();

        if (!isEnabled()) {
            setEnabled(false);
        }

        if (getResources().getInteger(R.integer.time_picker_wheel_item_count) == 5) {
            setBackgroundResource(R.drawable.remind_time_picker_widget_5_bg);
        } else {
            setBackgroundResource(R.drawable.remind_time_picker_widget_bg);
        }
    }

    private void onHourChanged(int hour) {
        mMinuteSpinner.setMinValue(1);
        mMinuteSpinner.setWrapSelectorWheel(true);
        mMinuteSpinner.setEnabled(true);

        mHourSpinner.setWrapSelectorWheel(true);
    }

    public String[] getDisplayDate(Context mcContext,int julian,Time time){
        int[] julianl = {julian-2,julian-1,julian,julian +1,julian+2};
        String[] ss = new String[5];
        for (int i = 0; i <julianl.length; i++) {
            long millims = time.setJulianDay(julianl[i]);
            ss[i] = DateUtils.formatDateRange(mcContext, millims,
                    millims, DateUtils.FORMAT_SHOW_DATE);
        }
        return ss;
    }
    @Override
    public void setEnabled(boolean enabled) {
        if (mIsEnabled == enabled) {
            return;
        }
        super.setEnabled(enabled);
        mMinuteSpinner.setEnabled(enabled);
        mHourSpinner.setEnabled(enabled);
        if (mAmPmSpinner != null) {
            mAmPmSpinner.setEnabled(enabled);
        } else {
            mAmPmButton.setEnabled(enabled);
        }
        mIsEnabled = enabled;
    }

    @Override
    public boolean isEnabled() {
        return mIsEnabled;
    }

    @Override
    protected void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        setCurrentLocale(newConfig.locale);
    }

    /**
     * Sets the current locale.
     *
     * @param locale The current locale.
     */
    private void setCurrentLocale(Locale locale) {
        if (locale.equals(mCurrentLocale)) {
            return;
        }
        mCurrentLocale = locale;
        mTempCalendar = Calendar.getInstance(locale);
    }

    /**
     * Used to save / restore state of time picker
     */
    private static class SavedState extends BaseSavedState {

        private final int mHour;

        private final int mMinute;

        private SavedState(Parcelable superState, int hour, int minute) {
            super(superState);
            mHour = hour;
            mMinute = minute;
        }

        private SavedState(Parcel in) {
            super(in);
            mHour = in.readInt();
            mMinute = in.readInt();
        }

        public int getHour() {
            return mHour;
        }

        public int getMinute() {
            return mMinute;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            super.writeToParcel(dest, flags);
            dest.writeInt(mHour);
            dest.writeInt(mMinute);
        }

        public static final Creator<SavedState> CREATOR = new Creator<SmartisanTimePicker1Day.SavedState>() {
            public SmartisanTimePicker1Day.SavedState createFromParcel(Parcel in) {
                return new SmartisanTimePicker1Day.SavedState(in);
            }

            public SmartisanTimePicker1Day.SavedState[] newArray(int size) {
                return new SmartisanTimePicker1Day.SavedState[size];
            }
        };
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        Parcelable superState = super.onSaveInstanceState();
        return new SavedState(superState, getCurrentHour(), getCurrentMinute());
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        SavedState ss = (SavedState) state;
        super.onRestoreInstanceState(ss.getSuperState());
        setCurrentHour(ss.getHour());
        setCurrentMinute(ss.getMinute());
    }

    /**
     * @return The current hour in the range (0-23).
     */
    public Integer getCurrentHour() {
        int currentHour = mHourSpinner.getValue();
        if (is24HourView()) {
            return currentHour;
        } else if (mIsAm) {
            return currentHour % HOURS_IN_HALF_DAY;
        } else {
            return (currentHour % HOURS_IN_HALF_DAY) + HOURS_IN_HALF_DAY;
        }
    }

    /**
     * Set the current hour.
     */
    public void setCurrentHour(Integer currentHour) {
        Log.d("setTime", "setCurrentHour currentHour:" + currentHour);

        // why was Integer used in the first place?
        if (currentHour == null || currentHour.equals(getCurrentHour())) {
            return;
        }
        if (!is24HourView()) {
            // convert [0,23] ordinal to wall clock display
            if (currentHour >= HOURS_IN_HALF_DAY) {
                mIsAm = false;
                if (currentHour > HOURS_IN_HALF_DAY) {
                    currentHour = currentHour - HOURS_IN_HALF_DAY;
                }
            } else {
                mIsAm = true;
                if (currentHour == 0) {
                    currentHour = HOURS_IN_HALF_DAY;
                }
            }
            updateAmPmControl();
        }
        mHourSpinner.setValue(currentHour);
        onHourChanged(mHourSpinner.getValue());
    }

//    /**
//     * @return return true if remind is selected, otherwise false.
//     */
//    public boolean getRemind() {
//        return true; // TODO wait pm decide is notify deault true
//    }

//    public void setRemind(boolean remind) {
//        mRemindSpinner.setValue(remind ? 0 : 1);
//    }

    /**
     * Set whether in 24 hour or AM/PM mode.
     *
     * @param is24HourView True = 24 hour mode. False = AM/PM.
     */
    public void setIs24HourView(Boolean is24HourView) {
        if (mIs24HourView == is24HourView) {
            return;
        }
        mIs24HourView = is24HourView;
        // cache the current hour since spinner range changes
        int currentHour = getCurrentHour();
        updateHourControl();
        // set value after spinner range is updated
        setCurrentHour(currentHour);
        updateAmPmControl();
    }

    /**
     * @return true if this is in 24 hour view else false.
     */
    public boolean is24HourView() {
        return mIs24HourView;
    }

    /**
     * @return The current minute.
     */
    public Integer getCurrentMinute() {
        return mMinuteSpinner.getValue() - 1;
    }

    /**
     * Set the current minute (0-59).
     */
    public void setCurrentMinute(Integer currentMinute) {
        Log.d("setTime", "setCurrentMinute currentMinute:" + currentMinute);

        if (currentMinute == null || currentMinute.equals(getCurrentMinute())) {
            return;
        }
        mMinuteSpinner.setValue(currentMinute + 1);
    }

    @Override
    public int getBaseline() {
        return mHourSpinner.getBaseline();
    }

    @Override
    public boolean dispatchPopulateAccessibilityEvent(AccessibilityEvent event) {
        onPopulateAccessibilityEvent(event);
        return true;
    }

    @Override
    public void onPopulateAccessibilityEvent(AccessibilityEvent event) {
        super.onPopulateAccessibilityEvent(event);

        int flags = DateUtils.FORMAT_SHOW_TIME;
        if (mIs24HourView) {
            flags |= DateUtils.FORMAT_24HOUR;
        } else {
            flags |= DateUtils.FORMAT_12HOUR;
        }
        mTempCalendar.set(Calendar.HOUR_OF_DAY, getCurrentHour());
        mTempCalendar.set(Calendar.MINUTE, getCurrentMinute());
        String selectedDateUtterance = DateUtils.formatDateTime(getContext(),
                mTempCalendar.getTimeInMillis(), flags);
        event.getText().add(selectedDateUtterance);
    }

    @Override
    public void onInitializeAccessibilityEvent(AccessibilityEvent event) {
        super.onInitializeAccessibilityEvent(event);
        event.setClassName(SmartisanTimePicker1Day.class.getName());
    }

    @Override
    public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo info) {
        super.onInitializeAccessibilityNodeInfo(info);
        info.setClassName(SmartisanTimePicker1Day.class.getName());
    }

    private void updateHourControl() {
        if (is24HourView()) {
            mHourSpinner.setWrapSelectorWheel(true);
            mHourSpinner.setMinValue(0);
            mHourSpinner.setMaxValue(23);
//            mHourSpinner.setFormatter(SmartisanNumberPicker1Day.getTwoDigitFormatter());
//            mHourSpinner.setFormatter(new SmartisanNumberPicker1Day.Formatter() {
//                @Override
//                public String format(int value) {
//                    return value == 0 ? String.valueOf(0) : String.valueOf(value-1);
//                }
//            });
        } else {
            mHourSpinner.setWrapSelectorWheel(true);
            mHourSpinner.setMinValue(1);
            mHourSpinner.setMaxValue(12);
//            mHourSpinner.setFormatter(null);
        }
    }

    private void updateAmPmControl() {
        if (is24HourView()) {
            mAmPmDividerView.setVisibility(View.GONE);
            if (mAmPmSpinner != null) {
                mAmPmSpinner.setVisibility(View.GONE);
            } else {
                mAmPmButton.setVisibility(View.GONE);
            }
        } else {
            int index = mIsAm ? Calendar.AM : Calendar.PM;
            mAmPmDividerView.setVisibility(View.VISIBLE);
            if (mAmPmSpinner != null) {
                mAmPmSpinner.setValue(index);
                mAmPmSpinner.setVisibility(View.VISIBLE);
            } else {
                mAmPmButton.setText(mAmPmStrings[index]);
                mAmPmButton.setVisibility(View.VISIBLE);
            }
        }
        sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_SELECTED);
    }
}
