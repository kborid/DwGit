package com.smartisanos.ideapills.common.anim;

import android.animation.ValueAnimator;

public interface AnimListener {
    public void onStart();
    public void onComplete(int type);
    public void onAnimationUpdate(ValueAnimator valueAnimator);
}