package com.smartisanos.ideapills.common.remind.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;

import com.smartisanos.ideapills.common.R;

public class MonthByWeekDayViewDrawer extends NormalDayCellDrawer {

    private int mWeekNumTop;
    private int mMonthNumTop;

    public MonthByWeekDayViewDrawer(Context context, boolean mDrawFocus, float cellWidth) {
        super(context, mDrawFocus, cellWidth);
        mWeekNumTop = (int) context.getResources().getDimension(R.dimen.monthweek_relative_week_en_num);
        mMonthNumTop = (int) context.getResources().getDimension(R.dimen.monthweek_relative_month_num);
    }

    @Override
    protected void drawDayNum(float xa, String dayNum,String lunnar, Canvas canvas, float switchAnimProgress) {
        int y = mMonthNumTop + (int)(switchAnimProgress * (mWeekNumTop - mMonthNumTop));
        canvas.drawText(dayNum, getCenterPosition((int)xa, dayNum, getDayNumPaint()), y, getDayNumPaint());
    }

    @Override
    protected Paint getDayNumPaint() {
        return mPaintFactory.getWeekNumPaint(mContext);
    }
}
