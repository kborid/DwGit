package com.smartisanos.sara.bubble.view;

import android.content.Context;
import android.service.onestep.GlobalBubble;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.LinearLayout;

import com.smartisanos.sara.R;
import com.smartisanos.sara.util.AnimManager;

public class BubbleColorChooserLayout extends LinearLayout implements View.OnClickListener {

    private OnTouchListener mBubbleColorTouchListener = new OnTouchListener() {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    AnimManager.scaleViewAnim(v, 1.4f);
                    break;
                case MotionEvent.ACTION_CANCEL:
                case MotionEvent.ACTION_UP:
                    AnimManager.scaleViewAnim(v, 1.0f);
                    break;
            }
            return false;
        }
    };
    private OnColorClickListener mOnColorClickListener;

    public BubbleColorChooserLayout(Context context) {
        super(context);
    }

    public BubbleColorChooserLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public BubbleColorChooserLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        init();
    }

    public void setOnColorClickListener(OnColorClickListener onColorClickListener) {
        mOnColorClickListener = onColorClickListener;
    }

    private void init() {
        View redColor = findViewById(R.id.iv_color_red);
        View orangeColor = findViewById(R.id.iv_color_orange);
        View greenColor = findViewById(R.id.iv_color_green);
        View blueColor = findViewById(R.id.iv_color_blue);
        View purpleColor = findViewById(R.id.iv_color_purple);
        View shareColor = findViewById(R.id.iv_color_share);
        View navyBlueColor = findViewById(R.id.iv_color_navy_blue);

        redColor.setOnTouchListener(mBubbleColorTouchListener);
        orangeColor.setOnTouchListener(mBubbleColorTouchListener);
        greenColor.setOnTouchListener(mBubbleColorTouchListener);
        blueColor.setOnTouchListener(mBubbleColorTouchListener);
        purpleColor.setOnTouchListener(mBubbleColorTouchListener);
        shareColor.setOnTouchListener(mBubbleColorTouchListener);
        navyBlueColor.setOnTouchListener(mBubbleColorTouchListener);

        redColor.setOnClickListener(this);
        orangeColor.setOnClickListener(this);
        greenColor.setOnClickListener(this);
        blueColor.setOnClickListener(this);
        purpleColor.setOnClickListener(this);
        shareColor.setOnClickListener(this);
        navyBlueColor.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.iv_color_red: {
                if (mOnColorClickListener != null) {
                    mOnColorClickListener.onColorClick(GlobalBubble.COLOR_RED);
                }
            }
            break;
            case R.id.iv_color_orange: {
                if (mOnColorClickListener != null) {
                    mOnColorClickListener.onColorClick(GlobalBubble.COLOR_ORANGE);
                }
            }
            break;
            case R.id.iv_color_green: {
                if (mOnColorClickListener != null) {
                    mOnColorClickListener.onColorClick(GlobalBubble.COLOR_GREEN);
                }
            }
            break;
            case R.id.iv_color_blue: {
                if (mOnColorClickListener != null) {
                    mOnColorClickListener.onColorClick(GlobalBubble.COLOR_BLUE);
                }
            }
            break;
            case R.id.iv_color_purple: {
                if (mOnColorClickListener != null) {
                    mOnColorClickListener.onColorClick(GlobalBubble.COLOR_PURPLE);
                }
            }
            break;
            case R.id.iv_color_share: {
                if (mOnColorClickListener != null) {
                    mOnColorClickListener.onColorClick(GlobalBubble.COLOR_SHARE);
                }
            }
            break;
            case R.id.iv_color_navy_blue: {
                if (mOnColorClickListener != null) {
                    mOnColorClickListener.onColorClick(GlobalBubble.COLOR_NAVY_BLUE);
                }
            }
            break;
        }
    }

    public interface OnColorClickListener {
        void onColorClick(int color);
    }
}
