package com.smartisanos.ideapills.common.remind;

import android.text.format.DateUtils;

import java.util.Calendar;
import java.util.TimeZone;

/**
 * This is an replace of {@link android.text.format.Time}.
 * For fixing Smartisan bug/1997
 */
public class Time extends android.text.format.Time {
    private Calendar mCalendarCopy;

    public Time(String timezone) {
        super(timezone);
    }

    public Time() {
    }

    public Time(android.text.format.Time other) {
        super(other);
    }

    @Override
    // Don't remove this method although it looks like the same as the same
    // as android.text.format.Time.setJulianDay.
    // Because it references the re-wrote static method getJulianDay.
    public long setJulianDay(int julianDay) {
        // Don't bother with the GMT offset since we don't know the correct
        // value for the given Julian day.  Just get close and then adjust
        // the day.
        long millis = (julianDay - EPOCH_JULIAN_DAY) * DateUtils.DAY_IN_MILLIS;
        set(millis);

        // Figure out how close we are to the requested Julian day.
        // We can't be off by more than a day.
        int approximateDay = getJulianDay(millis, gmtoff);
        int diff = julianDay - approximateDay;
        monthDay += diff;

        // Set the time to 12am and re-normalize.
        hour = 0;
        minute = 0;
        second = 0;
        millis = normalize(true);
        return millis;
    }

    public static int getJulianDay(long millis, long gmtoff) {
        long offsetMillis = gmtoff * 1000;
        long localMillis = (millis + offsetMillis);
        long julianDay = localMillis / DateUtils.DAY_IN_MILLIS;
        // For fixing Smartisan bug/1997
        // If localMillis is a positive number, ex. 28800000,
        // which represent 1970/1/1 8:00 UTC,
        // 28800000 / DateUtils.DAY_IN_MILLIS will get -0.333333...
        // which will be truncated to 0 in integer division.
        // Then we will get julian day 2440588.

        // If localMillis is a negative number, ex. -57600000,
        // which represent 1969/12/31 8:00 UTC,
        // -57600000 / DateUtils.DAY_IN_MILLIS will get -0.666666...
        // which will be truncated to 0 too.
        // We will get julian day 2440588, the same as above.
        // So we have to make it to be -1.
        if (localMillis < 0 && (localMillis % DateUtils.DAY_IN_MILLIS != 0)) {
            julianDay--;
        }
        return (int) julianDay + EPOCH_JULIAN_DAY;
    }

    @Override
    public long normalize(boolean ignoreDst) {
        long normalize = super.normalize(ignoreDst);
        //For fixing Smartisan bug/42389
        //The Time on Dst Time Will Caculate Error
        if(isDst < 0){
            //it means on the DST Begin time
            return getCalendarCopy().getTimeInMillis();
        }
        return normalize;
    }

    @Override
    public long toMillis(boolean ignoreDst) {
        if(isDst < 0){
            return getCalendarCopy().getTimeInMillis();
        }
        return super.toMillis(ignoreDst);
    }

    private Calendar getCalendarCopy(){
        TimeZone currentTimeZone = TimeZone.getTimeZone(timezone);
        if(mCalendarCopy == null){
            mCalendarCopy = Calendar.getInstance(currentTimeZone);
        }else if(!mCalendarCopy.getTimeZone().equals(currentTimeZone)){
            mCalendarCopy.setTimeZone(currentTimeZone);
        }
        mCalendarCopy.set(year, month, monthDay, hour, minute, second);
        return mCalendarCopy;
    }
}

