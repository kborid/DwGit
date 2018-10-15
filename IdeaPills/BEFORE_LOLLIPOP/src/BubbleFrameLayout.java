package com.smartisanos.ideapills.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.smartisanos.ideapills.util.LOG;
import com.smartisanos.ideapills.util.Utils;

import smartisanos.view.PressGestureDetector;

public class BubbleFrameLayout extends FrameLayout{
    private static final LOG log = LOG.getInstance(BubbleFrameLayout.class);

    PressGestureDetector mPressGestureDetector;
    private boolean mDiscardNextActionUp;
    private OnTouchListener mTouchEventRecevier = null;
    public BubbleFrameLayout(Context context) {
        this(context, null);
    }

    public BubbleFrameLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public BubbleFrameLayout(Context context, AttributeSet attrs,
                           int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mPressGestureDetector = new PressGestureDetector(context, this);
        mPressGestureDetector.setBoomDelegate(new PressGestureDetector.BoomDelegate() {
            @Override
            public boolean onTextBoom(View touchView) {
                String text = touchView instanceof BubbleEditText
                        ? ((BubbleEditText) touchView).getShowText() : ((TextView) touchView).getText().toString();
                View view = (View) touchView.getParent();
                for (; view != null; ) {
                    if (view instanceof BubbleItemView) {
                        BubbleItemView itemView = (BubbleItemView) view;
                        Utils.startBoomActivity(getContext(), itemView, text, itemView.getBubbleItem(),Utils.isEditable(touchView));
                        break;
                    }
                    view = (View) view.getParent();
                }
                mDiscardNextActionUp = true;
                return true;
            }
        });
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (mTouchEventRecevier != null) {
            boolean handle = mTouchEventRecevier.onTouch(this, ev);
            switch (ev.getAction()) {
                case MotionEvent.ACTION_CANCEL:
                case MotionEvent.ACTION_UP: {
                    mTouchEventRecevier = null;
                }
            }
            return handle;
        }
        final int oldAction = ev.getAction();
        if (oldAction == MotionEvent.ACTION_DOWN) {
            mDiscardNextActionUp = false;
        }
        mPressGestureDetector.dispatchTouchEvent(ev, isHandlingTouchEvent());
        if (mDiscardNextActionUp) {
            ev.setAction(MotionEvent.ACTION_CANCEL);
            log.info("action=" + ev.getAction());
        }
        setHandlingTouchEvent(true);
        boolean ret = super.dispatchTouchEvent(ev);
        setHandlingTouchEvent(false);
        ev.setAction(oldAction);
        return ret;
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (event.getKeyCode() == KeyEvent.KEYCODE_BACK
                && event.getAction() == KeyEvent.ACTION_UP) {
            mPressGestureDetector.handleBackKey();
        }
        return super.dispatchKeyEvent(event);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        mPressGestureDetector.onAttached(null);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        mPressGestureDetector.onDetached();
    }

    /** @hide */
    @Override
    public boolean isLongPressSwipe() {
        return mPressGestureDetector.isLongPressSwipe();
    }

    public void setTouchEventReceiver(OnTouchListener listener) {
        mTouchEventRecevier = listener;
    }
}
