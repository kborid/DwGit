package com.smartisanos.ideapills.common.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.widget.TextView;

public class CrossLineTextView extends TextView {
    private static final int DEFAULT_STROKE_WIDTH = 1;
    private Paint mCrossLinePaint;
    private int mStyleFlag = 0;

    public CrossLineTextView(Context context) {
        this(context, null);
    }

    public CrossLineTextView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CrossLineTextView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mCrossLinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        final float scale = context.getResources().getDisplayMetrics().density;
        float strokeWidth = DEFAULT_STROKE_WIDTH * scale;
        mCrossLinePaint.setStrokeWidth(strokeWidth);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if ((mStyleFlag & (Paint.STRIKE_THRU_TEXT_FLAG | Paint.UNDERLINE_TEXT_FLAG)) > 0) {
            Paint paint = getPaint();
            mCrossLinePaint.setColor(paint.getColor());
            mCrossLinePaint.setTextSize(paint.getTextSize());
            Rect rect = new Rect();
            int count = getLineCount();
            final float strokeWidth = mCrossLinePaint.getStrokeWidth();
            for (int i = 0; i < count; i++) {
                getLineBounds(i, rect);
                if ((mStyleFlag & Paint.STRIKE_THRU_TEXT_FLAG) > 0) {
                    canvas.drawLine(rect.left, rect.centerY() + strokeWidth, rect.right, rect.centerY() + strokeWidth, mCrossLinePaint);
                } else {
                    canvas.drawLine(rect.left, rect.bottom, rect.right, rect.bottom, mCrossLinePaint);
                }
            }
        }
    }

    public void setPaintFlags(int flag) {
        mStyleFlag = flag;
        flag = flag & ~(Paint.STRIKE_THRU_TEXT_FLAG | Paint.UNDERLINE_TEXT_FLAG);
        super.setPaintFlags(flag);
    }
}
