package com.smartisanos.sara.bubble.revone.widget;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import com.smartisanos.sara.R;
import com.smartisanos.sara.util.SaraUtils;

public class LetterIndexView extends View {
    private static final int DEFAULT_NORMAL_COLOR = 0x99ffffff;
    private static final int DEFAULT_TOUCH_COLOR = Color.WHITE;

    private OnTouchingLetterChangedListener mListener;

    private String[] mLetters = null;

    private Paint mPaint;

    private boolean mHit;

    private int mNormalColor;

    private int mTouchColor;

    private int mCurrentIndex = -1;

    private static int sStringArrayId = R.array.letter_list;

    private int mTextSize = 12;
    private int mTextGap = 0;
    private int mCircleRadius = 6;

    public LetterIndexView(Context paramContext) {
        this(paramContext, null);
    }

    public LetterIndexView(Context paramContext, AttributeSet paramAttributeSet) {
        this(paramContext, paramAttributeSet, 0);
    }

    public LetterIndexView(Context paramContext, AttributeSet paramAttributeSet, int paramInt) {
        super(paramContext, paramAttributeSet, paramInt);
        mPaint = new Paint(Paint.FAKE_BOLD_TEXT_FLAG | Paint.ANTI_ALIAS_FLAG);
        mHit = false;
        mNormalColor = DEFAULT_NORMAL_COLOR;
        mTouchColor = DEFAULT_TOUCH_COLOR;
        Resources res = paramContext.getResources();
        mLetters = res.getStringArray(sStringArrayId);
        mTextSize = res.getDimensionPixelSize(R.dimen.flash_im_contact_index_letter_size);
        mTextGap = res.getDimensionPixelSize(R.dimen.flash_im_contact_index_letter_gap);
        mCircleRadius = res.getDimensionPixelSize(R.dimen.flash_im_contact_circle_radius);
        mPaint.setTextAlign(Paint.Align.CENTER);
        mPaint.setColor(mNormalColor);
        mPaint.setTextSize(mTextSize);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int count = mLetters.length;
        int width = (int) mPaint.measureText(mLetters[0]) + getPaddingLeft() + getPaddingRight();
        int height = getPaddingTop() + mTextSize * count + mTextGap * (count - 1) + getPaddingBottom();

        setMeasuredDimension(width, height);
    }

    public void setOnTouchingLetterChangedListener(OnTouchingLetterChangedListener onTouchingLetterChangedListener) {
        mListener = onTouchingLetterChangedListener;
    }

    public void setLetters(String[] letters) {
        mLetters = letters;
    }

    public void setNormalColor(int color) {
        mNormalColor = color;
    }

    public boolean dispatchTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                mHit = true;
                onHit(event.getY());
                break;
            case MotionEvent.ACTION_MOVE:
                onHit(event.getY());
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                onCancel();
                break;
        }
        invalidate();
        return true;
    }

    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int paddingTop = getPaddingTop();
        float halfWidth = getWidth() / 2;
        Rect rect = new Rect();
        for (int i = 0; i < mLetters.length; ++i) {
            float letterPosY = paddingTop + (mTextSize + mTextGap) * i + mTextSize;
            if (mCurrentIndex == i) {
                mPaint.getTextBounds(mLetters[i], 0, mLetters[i].length(), rect);
                canvas.drawCircle(halfWidth, letterPosY - rect.height() / 2, mCircleRadius, mPaint);
                mPaint.setColor(mTouchColor);
            } else {
                mPaint.setColor(mNormalColor);
            }
            canvas.drawText(mLetters[i], halfWidth, letterPosY, mPaint);
        }
    }

    private void onHit(float offset) {
        if (mHit && mListener != null) {
            int index = (int) (offset / (mTextSize + mTextGap));
            index = Math.max(index, 0);
            index = Math.min(index, mLetters.length - 1);
            mCurrentIndex = index;
            String str = mLetters[index];
            mListener.onHit(str);
        }
    }

    private void onCancel() {
        mHit = false;
        mCurrentIndex = -1;
        refreshDrawableState();

        if (mListener != null) {
            mListener.onCancel();
        }
    }

    public interface OnTouchingLetterChangedListener {
        void onHit(String letter);

        void onCancel();
    }

}
