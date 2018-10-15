package com.smartisanos.ideapills.view;

import android.animation.ValueAnimator;
import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.ImageView;

import com.smartisanos.ideapills.common.anim.Anim;
import com.smartisanos.ideapills.common.anim.AnimListener;
import com.smartisanos.ideapills.common.anim.Vector3f;

public class BubbleColorChooserView extends ImageView implements AnimListener{
    private static final int ANIM_DURA = 250;
    private Anim mScalAnim = null;

    public BubbleColorChooserView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public BubbleColorChooserView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    private void startScalAnim(Vector3f target) {
        if (mScalAnim != null) {
            mScalAnim.cancel();
        }
        mScalAnim = new Anim(this, Anim.SCALE, ANIM_DURA, Anim.CUBIC_OUT, new Vector3f(getScaleX(), getScaleY()), target);
        mScalAnim.setListener(this);
        mScalAnim.start();
    }

    public boolean onTouchEvent(MotionEvent event) {
        if (isEnabled()) {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN: {
                    startScalAnim(new Vector3f(1.4f, 1.4f));
                }
                break;
                case MotionEvent.ACTION_CANCEL:
                case MotionEvent.ACTION_UP: {
                    startScalAnim(new Vector3f(1.0f, 1.0f));
                }
                break;
            }
        }
        return super.onTouchEvent(event);
    }

    public void onStart() {

    }

    public void onComplete(int type) {
        mScalAnim = null;
    }

    @Override
    public void onAnimationUpdate(ValueAnimator valueAnimator) {

    }
}

