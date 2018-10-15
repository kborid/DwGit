package com.smartisanos.sara.bubble;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;

public class BubbleRootView extends FrameLayout {
    private ITouchDownListener mTouchDownListener;

    public BubbleRootView(Context context) {
        this(context, null);
    }

    public BubbleRootView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public void notifySubtreeAccessibilityStateChanged(View child, View source, int changeType) {
        return;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        int action = ev.getAction();
        if (mTouchDownListener != null && action == MotionEvent.ACTION_DOWN) {
            mTouchDownListener.onTouchDown(ev);
        }
        return super.onInterceptTouchEvent(ev);
    }

    public void setTouchDownListener(ITouchDownListener l) {
        mTouchDownListener = l;
    }

    public interface ITouchDownListener {
        void onTouchDown(MotionEvent ev);
    }
}
