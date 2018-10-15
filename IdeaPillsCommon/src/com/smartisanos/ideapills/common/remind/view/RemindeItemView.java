package com.smartisanos.ideapills.common.remind.view;

import android.content.Context;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.RelativeLayout;

public class RemindeItemView extends RelativeLayout {

    public RemindeItemView(Context context) {
        this(context, null);
    }

    public RemindeItemView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public RemindeItemView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        final int action = event.getAction();
        final float x = event.getX();
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                View view = getChildAt(0);
                Rect rect = new Rect();
                view.getHitRect(rect);
                if (rect.contains((int) event.getX(), (int) event.getY())) {
                    setPressed(true);
                } else if (mClickListener != null) {
                    mClickListener.onClick(this, false);
                }
                break;
            case MotionEvent.ACTION_MOVE:
                break;
            case MotionEvent.ACTION_UP:
                if (isPressed()) {
                    setPressed(false);
                    mClickListener.onClick(this, true);
                }
                break;
            case MotionEvent.ACTION_CANCEL:
                if (isPressed()) {
                    setPressed(false);
                }
                break;
        }
        return true;
    }

    private ClickListener mClickListener;

    public void setClickListener(ClickListener l) {
        mClickListener = l;
    }

    public interface ClickListener {
        void onClick(View v, boolean select);
    }
}
