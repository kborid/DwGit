package com.kborid.smart.widget.letterIndex;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import com.kborid.smart.R;

import java.util.ArrayList;
import java.util.List;

public class LetterIndexView extends View {

    private OnTouchingLetterChangedListener listener;

    private static final int STYLE_NORMAL = 0;
    private static final int STYLE_BOLD = 1;
    /**
     * head icon array
     */
    private Drawable[] mHeadArray;
    /**
     * top space between head and LetterIndexView top
     */
    private int mHeadMarginTop;
    /**
     * bottom space between head and letter content
     */
    private int mHeadMarginBottom;
    /**
     * space for each head icon
     */
    private int mHeadSpace;
    /**
     * head icon width
     */
    private int mHeadWidth;
    /**
     * head icon height
     */
    private int mHeadHeight;
    /**
     * letter array
     */
    private String[] mLetterArray;
    /**
     * letter width
     */
    private int mLetterWidth;
    /**
     * letter height
     */
    private int mLetterHeight;
    /**
     * letter size
     */
    private int mLetterSize;
    /**
     * letter color
     */
    private int mLetterColor;
    /**
     * letter style
     */
    private int mLetterStyle;
    /**
     * space between letter and head
     */
    private int mLetterMarginTop;
    /**
     * space between letter and LetterIndexView bottom
     */
    private int mLetterMarginBottom;

    private Drawable mHitDrawable;
    private int mHitIndex;

    private Paint mPaint;

    private float offset;

    private boolean hit;

    public LetterIndexView(Context paramContext) {
        this(paramContext, null);
    }

    public LetterIndexView(Context paramContext, AttributeSet paramAttributeSet) {
        this(paramContext, paramAttributeSet, 0);
    }

    public LetterIndexView(Context paramContext, AttributeSet paramAttributeSet, int paramInt) {
        super(paramContext, paramAttributeSet, paramInt);
        parseAttr(paramContext, paramAttributeSet);
        this.mPaint = new Paint();
        this.offset = 0.0F;
        this.hit = false;
        mPaint.setAntiAlias(true);
        mPaint.setTextAlign(Paint.Align.CENTER);
    }

    private void parseAttr(Context paramContext, AttributeSet paramAttributeSet) {
        TypedArray array = paramContext.obtainStyledAttributes(paramAttributeSet, R.styleable.LetterIndexView);

        mHeadMarginTop = array.getDimensionPixelSize(R.styleable.LetterIndexView_letterHeadMarginTop, 0);
        mHeadMarginBottom = array.getDimensionPixelSize(R.styleable.LetterIndexView_letterHeadMarginBottom, 0);
        mHeadSpace = array.getDimensionPixelSize(R.styleable.LetterIndexView_letterHeadSpace, 0);
        mHeadWidth = array.getDimensionPixelSize(R.styleable.LetterIndexView_letterHeadWidth, -1);
        mHeadHeight = array.getDimensionPixelSize(R.styleable.LetterIndexView_letterHeadHeight, -1);

        mLetterWidth = array.getDimensionPixelSize(R.styleable.LetterIndexView_letterWidth, -1);
        mLetterHeight = array.getDimensionPixelSize(R.styleable.LetterIndexView_letterHeadHeight, -1);
        mLetterSize = array.getDimensionPixelSize(R.styleable.LetterIndexView_letterSize, -1);
        mLetterColor = array.getColor(R.styleable.LetterIndexView_letterColor, Color.GRAY);
        mLetterStyle = array.getInt(R.styleable.LetterIndexView_letterStyle, STYLE_NORMAL);
        mLetterMarginTop = array.getDimensionPixelSize(R.styleable.LetterIndexView_letterMarginTop, 0);
        mLetterMarginBottom = array.getDimensionPixelSize(R.styleable.LetterIndexView_letterMarginBottom, 0);

        mHitDrawable = array.getDrawable(R.styleable.LetterIndexView_letterTouchBackground);
        if(mHitDrawable == null) {
            mHitDrawable = getResources().getDrawable(R.drawable.select_contact_right_pressed);
        }
        mHeadArray = new Drawable[0];
        mLetterArray = new String[0];
        int headArrayId = array.getResourceId(R.styleable.LetterIndexView_letterHeadArray, -1);
        int letterArrayId = array.getResourceId(R.styleable.LetterIndexView_letterArray, -1);
        array.recycle();

        if (headArrayId > 0) {
            TypedArray ar = getContext().getResources().obtainTypedArray(headArrayId);
            List<Drawable> drawables = new ArrayList<>();
            int length = ar.length();
            if (length > 0) {
                for (int i = 0; i < length; i++) {
                    int resId = ar.getResourceId(i, -1);
                    if(resId > 0) {
                        drawables.add(getResources().getDrawable(resId));
                    }
                }
            }
            ar.recycle();
            if(drawables.size() > 0) {
                mHeadArray = new Drawable[drawables.size()];
                drawables.toArray(mHeadArray);
            }
        }
        int letterId = letterArrayId > 0 ? letterArrayId : R.array.letter_list;
        mLetterArray = getResources().getStringArray(letterId);
    }

    public void setOnTouchingLetterChangedListener(OnTouchingLetterChangedListener onTouchingLetterChangedListener) {
        this.listener = onTouchingLetterChangedListener;
    }

    public void setLetters(String[] letters) {
        if(letters != null) {
            this.mLetterArray = letters;
        }
    }

    public void setNormalColor(int color) {
        mLetterColor = color;
    }

    public boolean dispatchTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                hit = true;
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
        int halfWidth = getWidth() / 2;
        int headLength = mHeadArray.length;
        Drawable drawable;
        int height = mHeadMarginTop;
        if(headLength > 0) {
            for(int i = 0; i < headLength; i++) {
                drawable = mHeadArray[i];
                int drawWidth = mHeadWidth > 0 ? mHeadWidth : drawable.getIntrinsicWidth();
                int drawHeight = mHeadHeight > 0 ? mHeadHeight : drawable.getIntrinsicHeight();
                int left = halfWidth - drawWidth / 2;
                int top = height;
                int right = halfWidth + drawWidth / 2;
                int bottom = height + drawHeight;
                drawable.setBounds(left, top, right, bottom);
                drawable.draw(canvas);
                if(hit && mHitIndex == i) {
                    int hitWidth = mHitDrawable.getIntrinsicWidth();
                    int hitHeight = mHitDrawable.getIntrinsicHeight();
                    int centX = halfWidth;
                    int centY = height + drawHeight / 2;
                    mHitDrawable.setBounds(centX - hitWidth / 2, centY - hitHeight / 2, centX + hitWidth / 2, centY + hitHeight / 2);
                    mHitDrawable.draw(canvas);
                }
                height = height + drawHeight + (i == headLength - 1 ? mHeadMarginBottom : mHeadSpace);
            }
        }
        height = height + mLetterMarginTop;
        int letterLength = mLetterArray.length;
        if(letterLength > 0) {
            float letterH = (getHeight() - height) / letterLength;
            float letterSize = mLetterSize > 0 ? mLetterSize : letterH * 5 / 6;
            mPaint.setTextSize(letterSize);
            mPaint.setColor(mLetterColor);
            if(mLetterStyle == STYLE_BOLD) {
                mPaint.setTypeface(Typeface.DEFAULT_BOLD);
            } else {
                mPaint.setTypeface(Typeface.DEFAULT);
            }
            Paint.FontMetrics mat = mPaint.getFontMetrics();
            for(int i = 0; i < letterLength; i++) {
                float letterY = height + letterH * i + letterSize;
                canvas.drawText(mLetterArray[i], halfWidth, letterY, mPaint);
                if(hit && mHitIndex == mHeadArray.length + i) {
                    int hitWidth = mHitDrawable.getIntrinsicWidth();
                    int hitHeight = mHitDrawable.getIntrinsicHeight();
                    int centX = halfWidth;
                    int centY = (int) (letterY + (mat.top + mat.bottom) / 2);
                    mHitDrawable.setBounds(centX - hitWidth / 2, centY - hitHeight / 2, centX + hitWidth / 2, centY + hitHeight / 2);
                    mHitDrawable.draw(canvas);
                }
            }
        }
    }

    private void onHit(float offset) {
        this.offset = offset;
        if (hit && listener != null) {
            int length = mHeadArray.length;
            int height = mHeadMarginTop;
            for (int i = 0; i < length; i++) {
                int headHeight = mHeadHeight > 0 ? mHeadHeight : mHeadArray[i].getIntrinsicHeight();
                height = height + headHeight + (i == length - 1 ? mHeadMarginBottom : mHeadSpace);
                if (offset < height) {
                    mHitIndex = i;
                    listener.onHit(mHeadArray[i], mHitIndex);
                    return;
                }
            }
            height = height + mLetterMarginTop;
            length = mLetterArray.length;
            int letterH = (getHeight() - height) / length;
            for(int i = 0; i < length; i++) {
                if(offset < height + letterH) {
                    mHitIndex = mHeadArray.length + i;
                    listener.onHit(mLetterArray[i], mHitIndex);
                    return;
                }
                height = height + letterH;
            }
        }
    }

    private void onCancel() {
        hit = false;
        refreshDrawableState();

        if (listener != null) {
            listener.onCancel();
        }
    }

    public interface OnTouchingLetterChangedListener {
        void onHit(String letter, int index);
        void onHit(Drawable drawable, int index);
        void onCancel();
    }

}
