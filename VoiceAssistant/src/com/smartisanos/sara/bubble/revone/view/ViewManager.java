package com.smartisanos.sara.bubble.revone.view;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.view.View;

import com.smartisanos.sara.bubble.revone.utils.Cubic;

public abstract class ViewManager {
    protected Context mContext;
    protected View mView;
    protected View mRootView;
    private boolean mShowAnimationEnabled = true;
    private int mFadeInAnimDelay;

    public ViewManager(Context context, View view) {
        mContext = context;
        mRootView = view;
    }

    public void show() {
        if (mView == null) {
            mView = getView();
        }
        if (mView.getVisibility() != View.VISIBLE) {
            fadeIn(mFadeInAnimDelay);
        }
    }

    public void hide() {
        if (mView != null) {
            mView.setVisibility(View.GONE);
        }
    }

    protected void fadeIn(int delay) {
        if (mShowAnimationEnabled) {
            ObjectAnimator alphaAnim = ObjectAnimator.ofFloat(mView, View.ALPHA, 0f, 1f);
            alphaAnim.setDuration(300);
            alphaAnim.setStartDelay(delay);
            alphaAnim.setInterpolator(Cubic.easeOut);
            alphaAnim.addListener(new Animator.AnimatorListener() {

                @Override
                public void onAnimationCancel(Animator arg0) {
                }

                @Override
                public void onAnimationEnd(Animator arg0) {
                }

                @Override
                public void onAnimationRepeat(Animator arg0) {
                }

                @Override
                public void onAnimationStart(Animator arg0) {
                    mView.setVisibility(View.VISIBLE);
                }
            });
            alphaAnim.start();
        }
    }

    public void setAnimationEnabled(boolean showAnimationEnabled) {
        mShowAnimationEnabled = showAnimationEnabled;
    }

    public void setFadeInAnimDelay(int delay) {
        mFadeInAnimDelay = delay;
    }

    protected abstract View getView();
}
