package com.smartisanos.ideapills.common.remind.util;

import android.content.Context;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.Paint.Style;
import android.graphics.Typeface;

import com.smartisanos.ideapills.common.R;

public class PaintFactory {

    private Paint mWeekNumPaint;
    private Paint mMonthNumPaint;

    public Paint getWeekNumPaint(Context context){
        if (mWeekNumPaint == null) {
            mWeekNumPaint = new Paint();
            mWeekNumPaint.setFakeBoldText(false);
            mWeekNumPaint.setAntiAlias(true);
            mWeekNumPaint.setStyle(Style.FILL);
            mWeekNumPaint.setTextAlign(Align.RIGHT);
            float monthNumSize = context.getResources().getDimension(
                    R.dimen.text_size_month_number);
            mWeekNumPaint.setTextSize(monthNumSize);
        }
        return mWeekNumPaint;
    }

    public Paint getMonthNumWithoutLunarPaint(Context context){
        if (mMonthNumPaint == null) {
            mMonthNumPaint = new Paint();
            mMonthNumPaint.setFakeBoldText(false);
            mMonthNumPaint.setAntiAlias(true);
            mMonthNumPaint.setStyle(Style.FILL);
            mMonthNumPaint.setTextAlign(Align.RIGHT);
            mMonthNumPaint.setTypeface(Typeface.DEFAULT);
            float monthNumSize = context.getResources().getDimension(
                    R.dimen.text_size_month_number);
            mMonthNumPaint.setTextSize(monthNumSize);

        }
        return mMonthNumPaint;
    }
}
