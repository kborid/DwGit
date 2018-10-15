package com.smartisanos.ideapills.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.FrameLayout;

import com.smartisanos.ideapills.R;
import com.smartisanos.ideapills.util.LOG;


public class BubbleContainerLayout extends FrameLayout{
    private static final LOG log = LOG.getInstance(BubbleContainerLayout.class);
    private int mleftIgnore;
    private OnTouchListener mOnTouchListener;
    private boolean mInterceptTouchEvent;

    public BubbleContainerLayout(Context context) {
        this(context, null);
    }

    public BubbleContainerLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public BubbleContainerLayout(Context context, AttributeSet attrs,
                                 int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onFinishInflate() {
        mleftIgnore = getResources().getDimensionPixelSize(R.dimen.bubble_item_left_ignore);
        super.onFinishInflate();
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (ev.getAction() == MotionEvent.ACTION_DOWN && ev.getX() < mleftIgnore) {
            return false;
        }
        return super.dispatchTouchEvent(ev);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (mOnTouchListener != null) {
            mInterceptTouchEvent = mOnTouchListener.onTouch(this, ev);
            return mInterceptTouchEvent;
        } else {
            mInterceptTouchEvent = false;
            return super.onInterceptTouchEvent(ev);
        }
    }

    @Override
    public void requestDisallowInterceptTouchEvent(boolean disallowIntercept) {
        if (disallowIntercept) {
            if (mOnTouchListener != null) {
                mOnTouchListener.onTouch(this, MotionEvent.obtain(System.currentTimeMillis(),
                        System.currentTimeMillis(), MotionEvent.ACTION_CANCEL, 0, 0, 0));
            }
        }
        super.requestDisallowInterceptTouchEvent(disallowIntercept);
    }

    public void setOnTouchListener(OnTouchListener onTouchListener) {
        mOnTouchListener = onTouchListener;
    }

    public boolean onTouchEvent(MotionEvent event) {
        boolean lastInterceptTouchEvent = mInterceptTouchEvent;
        if (mOnTouchListener != null) {
            //always deliver event to bubble adapter to record bubble state and do animation.
            mInterceptTouchEvent = mOnTouchListener.onTouch(this, event);
        } else {
            mInterceptTouchEvent = false;
        }
        if (!mInterceptTouchEvent) {
            super.onTouchEvent(event);
        } else if (!lastInterceptTouchEvent) {
            event.setAction(MotionEvent.ACTION_CANCEL);
            super.onTouchEvent(event);
        }
        return true;
    }
}
