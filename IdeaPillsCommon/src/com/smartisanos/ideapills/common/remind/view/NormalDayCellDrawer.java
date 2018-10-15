package com.smartisanos.ideapills.common.remind.view;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Paint;

import com.smartisanos.ideapills.common.R;
import com.smartisanos.ideapills.common.remind.MonthWeekEventsView;
import com.smartisanos.ideapills.common.remind.util.PaintFactory;
import com.smartisanos.ideapills.common.util.CommonUtils;

public abstract class NormalDayCellDrawer extends DayCellViewDrawer {

    private boolean mDrawFocus;
    protected Context mContext;
    //color
    protected int mNormalNumColor;
    protected int mNotFocusColor;
    protected int mTodayColor;
//    protected String mTodayText;
    //paint
    protected float mCellWidth;
    public PaintFactory mPaintFactory;
//    private boolean isZhLocale;
//    private int mTodaySize;

    public NormalDayCellDrawer(Context context, boolean mDrawFocus, float cellWidth) {
        this.mDrawFocus = mDrawFocus;
        this.mContext = context;
        this.mCellWidth = cellWidth;
        mPaintFactory = new PaintFactory();
        initColor();
//        mTodayText = context.getResources().getString(R.string.today_text);
//        isZhLocale = CommonUtils.isLanguageZhCN(context);
//        mTodaySize = context.getResources().getDimensionPixelSize(R.dimen.today_text_size_month_number);
    }

    private void initColor() {
        Resources res = mContext.getResources();
        mNormalNumColor = res.getColor(R.color.black_60);
        mNotFocusColor = res.getColor(R.color.month_day_number_other);
        mTodayColor = res.getColor(R.color.month_today_number);
    }

    @Override
    public void drawView(int startX, String dayNum, String lunar, int eventType, int viewType, Canvas canvas, float switchAnimProgress) {

        prepareDrawView(startX, dayNum, lunar, eventType, viewType);

        Paint dayNumPaint = getDayNumPaint();

        boolean isToday = (viewType & VIEW_TYPE_TODAY) == VIEW_TYPE_TODAY;
        boolean isSelected = (viewType & VIEW_TYPE_SELECTED) == VIEW_TYPE_SELECTED;

        boolean isBold = isSelected || isToday;
        boolean notFocus = (viewType & VIEW_TYPE_NOT_FOCUS_MONTH) == VIEW_TYPE_NOT_FOCUS_MONTH;
        boolean isDayOutOfRange = (viewType & VIEW_TYPE_DAY_OUT_OF_RANGE) == VIEW_TYPE_DAY_OUT_OF_RANGE;
        boolean isTodayBefore = (viewType & VIEW_TYPE_TODAY_BEFORE) == VIEW_TYPE_TODAY_BEFORE;

        //set color
        if (mDrawFocus) {
            if (isToday || isSelected) {
                setTodayColor(dayNumPaint);
            } else if (notFocus) {
                if (isDayOutOfRange || isTodayBefore) {
                    setFocusColor(dayNumPaint, 0);
                } else {
                    setFocusColor(dayNumPaint, switchAnimProgress);
                }
            } else {
                if (isTodayBefore) {
                    setFocusColor(dayNumPaint, 0);
                } else {
                    setNormalColor(dayNumPaint);
                }
            }
        } else {
            if (isTodayBefore) {
                setFocusColor(dayNumPaint, 0);
            } else {
                setNormalColor(dayNumPaint);
            }
        }
        dayNumPaint.setFakeBoldText(isBold);

        //draw day num ,example 1990/7/10  draw 10
//        if (isZhLocale && isToday) {
//            dayNum = mTodayText;
//        }
        drawDayNum(startX, dayNum, lunar, canvas, switchAnimProgress);
    }


    public void setHasFocus(boolean hasFocus) {
        mDrawFocus = hasFocus;
    }

    protected void setNormalColor(Paint dayNumPaint) {
        dayNumPaint.setColor(mNormalNumColor);
    }

    protected void setFocusColor(Paint dayNumPaint, float switchAnimProgress) {
        int dayColor;
        if (switchAnimProgress < MonthWeekEventsView.SWTICH_ANIM_CHANGE_POINT) {
            dayColor = mNotFocusColor;
        } else {
            dayColor = mNormalNumColor;
        }
        dayNumPaint.setColor(dayColor);
    }

    protected void setTodayColor(Paint dayNumPaint) {
        dayNumPaint.setColor(mTodayColor);
    }

    protected void prepareDrawView(int startX, String dayNum, String lunar, int eventType, int viewType) {

    }

    protected abstract Paint getDayNumPaint();

    protected float getCenterPosition(int startX, String dayNum, Paint paint) {
        return startX - (mCellWidth - getStringWidth(dayNum, paint)) / 2;
    }

    protected void drawDayNum(float xa, String dayNum, String lunar, Canvas canvas, float precents) {

    }

    protected float getStringWidth(String text, Paint paint) {
        float[] widths = new float[text.length()];
        paint.getTextWidths(text, widths);
        float sum = 0f;
        for (float f : widths) {
            sum += f;
        }
        return sum;
    }
}
