package com.smartisanos.ideapills.common.anim;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;

import java.util.ArrayList;
import java.util.List;

public class AnimTimeLine {

    private List<Anim> mAnimList = new ArrayList<Anim>();
    private AnimatorSet mAnimationSet = new AnimatorSet();
    private AnimListener mListener;
    private int mStartDelay = 0;

    public void addAnim(Anim anim) {
        if (anim == null) {
            return;
        }
        anim.setAnimCallbackListener();
        mAnimList.add(anim);
    }

    public void addTimeLine(AnimTimeLine timeLine) {
        if (timeLine == null) {
            return;
        }
        List<Anim> animList = timeLine.getAnimList();
        if (animList != null) {
            timeLine.setAnimCallbackListener();
            mAnimList.addAll(animList);
        }
    }

    public List<Anim> getAnimList() {
        return mAnimList;
    }

    public void setAnimCallbackListener() {
        if (mAnimList == null || mAnimList.size() == 0) {
            return;
        }
        if (mListener == null) {
            return;
        }
        AnimatorCallbackListener listener = new AnimatorCallbackListener(mListener);
        AnimatorUpdateListener upatelistener = new AnimatorUpdateListener(mListener);

        ObjectAnimator lastAnim = null;
        long totalTime = 0;
        for (Anim anim : mAnimList) {
            List<ObjectAnimator> list = anim.getAnimatorList();
            if (list == null) {
                continue;
            }
            for (ObjectAnimator animator : list) {
                long delta = animator.getDuration() + animator.getStartDelay();
                if (delta >= totalTime) {
                    totalTime = delta;
                    lastAnim = animator;
                }
            }
        }
        if (lastAnim != null) {
            lastAnim.addListener(listener);
            lastAnim.addUpdateListener(upatelistener);
        } else {
        }
    }

    public void setDelay(int delay) {
        mStartDelay = delay;
    }

    public boolean start() {
        if (isEmpty()) {
            return false;
        }
        List<Animator> animators = new ArrayList<Animator>();
        for (Anim anim : mAnimList) {
            List<ObjectAnimator> list = anim.getAnimatorList();
            if (list != null && list.size() > 0) {
                animators.addAll(list);
            }
        }
        setAnimCallbackListener();
        mAnimationSet.playTogether(animators);
        if (mStartDelay != 0) {
            mAnimationSet.setStartDelay(mStartDelay);
        }
        mAnimationSet.start();
        return true;
    }

    public void stop() {
        mAnimationSet.end();
    }

    public void cancel() {
        mAnimationSet.cancel();
    }

    public boolean started() {
        return mAnimationSet.isStarted();
    }

    public boolean isRunning() {
        return mAnimationSet.isRunning();
    }

    public boolean isEmpty() {
        if (mAnimList == null || mAnimList.size() == 0) {
            return true;
        }

        boolean empty = true;
        for (Anim anim : mAnimList) {
            List<ObjectAnimator> list = anim.getAnimatorList();
            if (list != null && list.size() > 0) {
                empty = false;
                break;
            }
        }

        return empty;
    }

    public void setAnimListener(AnimListener listener) {
        if (listener == null) {
            return;
        }
        mListener = listener;
    }


    private class AnimatorUpdateListener implements  ValueAnimator.AnimatorUpdateListener{
        private AnimListener mAnimListener;

        public AnimatorUpdateListener(AnimListener listener) {
            mAnimListener = listener;
        }
        @Override
        public void onAnimationUpdate(ValueAnimator animation) {
            if (mAnimListener != null) {
                mAnimListener.onAnimationUpdate(animation);
            }
        }
    }


    private class AnimatorCallbackListener implements Animator.AnimatorListener {
        private AnimListener mAnimListener;

        public AnimatorCallbackListener(AnimListener listener) {
            mAnimListener = listener;
        }

        @Override
        public void onAnimationStart(Animator animator) {
            if (mAnimListener != null) {
                mAnimListener.onStart();
            }
        }

        @Override
        public void onAnimationEnd(Animator animator) {
            if (mAnimListener != null) {
                mAnimListener.onComplete(Anim.ANIM_FINISH_TYPE_COMPLETE);
            }
        }

        @Override
        public void onAnimationCancel(Animator animator) {
            if (mAnimListener != null) {
                mAnimListener.onComplete(Anim.ANIM_FINISH_TYPE_CANCELED);
            }
        }

        @Override
        public void onAnimationRepeat(Animator animator) {

        }
    }
}