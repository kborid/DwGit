package com.smartisanos.ideapills.common.remind.util;
import java.util.Calendar;

public class CalendarFieldTodayJudge implements ITodayJudge {

    private int mTimeField;
    private int mAddValue;
    private Calendar mCal;

    public CalendarFieldTodayJudge(int timeFied, int addValue) {
         mCal = Calendar.getInstance();
         mTimeField = timeFied;
         mAddValue = addValue;
    }

    @Override
    public boolean isToday(int index, String value) {
        return mCal.get(mTimeField) == (index + mAddValue);
    }

}
