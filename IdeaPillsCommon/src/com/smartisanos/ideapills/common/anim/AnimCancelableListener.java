package com.smartisanos.ideapills.common.anim;

import android.animation.ValueAnimator;

public abstract class AnimCancelableListener implements AnimListener {
    boolean isCanceled = false;

    public void onStart() {
        isCanceled = false;
    }

    public void onComplete(int type) {
        if (type == Anim.ANIM_FINISH_TYPE_CANCELED) {
            isCanceled = true;
        } else if(type == Anim.ANIM_FINISH_TYPE_COMPLETE){
            if(!isCanceled) {
                onAnimCompleted();
            }
        }
    }

    @Override
    public void onAnimationUpdate(ValueAnimator valueAnimator) {

    }

    public abstract void onAnimCompleted();
}
