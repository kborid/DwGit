package com.smartisanos.ideapills.common.anim;


import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.view.View;
import java.util.ArrayList;
import java.util.List;

public class Anim {

    public final static int QUAD_IN      = AnimInterpolator.QUAD_IN;
    public final static int QUAD_OUT     = AnimInterpolator.QUAD_OUT;
    public final static int QUAD_IN_OUT  = AnimInterpolator.QUAD_IN_OUT;

    public final static int CIRC_IN      = AnimInterpolator.CIRC_IN;
    public final static int CIRC_OUT     = AnimInterpolator.CIRC_OUT;
    public final static int CIRC_IN_OUT  = AnimInterpolator.CIRC_IN_OUT;

    public final static int CUBIC_IN     = AnimInterpolator.CUBIC_IN;
    public final static int CUBIC_OUT    = AnimInterpolator.CUBIC_OUT;
    public final static int CUBIC_IN_OUT = AnimInterpolator.CUBIC_IN_OUT;

    public final static int QUART_IN     = AnimInterpolator.QUART_IN;
    public final static int QUART_OUT    = AnimInterpolator.QUART_OUT;
    public final static int QUART_IN_OUT = AnimInterpolator.QUART_IN_OUT;

    public final static int QUINT_IN     = AnimInterpolator.QUINT_IN;
    public final static int QUINT_OUT    = AnimInterpolator.QUINT_OUT;
    public final static int QUINT_IN_OUT = AnimInterpolator.QUINT_IN_OUT;

    public final static int SINE_IN      = AnimInterpolator.SINE_IN;
    public final static int SINE_OUT     = AnimInterpolator.SINE_OUT;
    public final static int SINE_IN_OUT  = AnimInterpolator.SINE_IN_OUT;

    public final static int BACK_IN      = AnimInterpolator.BACK_IN;
    public final static int BACK_OUT     = AnimInterpolator.BACK_OUT;
    public final static int BACK_IN_OUT  = AnimInterpolator.BACK_IN_OUT;

    public final static int EASE_OUT  = AnimInterpolator.EASE_OUT;

    public final static int DEFAULT = AnimInterpolator.DEFAULT;

    //Android anim name
    public static final String X           = "x";
    public static final String Y           = "y";
    public static final String TRANSLATE_X = "translationX";
    public static final String TRANSLATE_Y = "translationY";
    public static final String ROTATION    = "rotation";
    public static final String ROTATION_X  = "rotationX";
    public static final String ROTATION_Y  = "rotationY";
    public static final String SCALE_X     = "scaleX";
    public static final String SCALE_Y     = "scaleY";
    public static final String ALPHA       = "alpha";

    public static final int XY          = 1001;
    public static final int ROTATE      = 1002;
    public static final int SCALE       = 1003;
    public static final int TRANSPARENT = 1004;
    public static final int TRANSLATE   = 1005;

    public static final int ANIM_FINISH_TYPE_COMPLETE = 1;
    public static final int ANIM_FINISH_TYPE_CANCELED = 2;

    public static final Vector3f ZERO = new Vector3f(0f, 0f, 0f);
    public static final Vector3f VISIBLE = new Vector3f(0f, 0f, 1f);
    public static final Vector3f INVISIBLE = ZERO;

    private View mView;
    private int animType;
    private int duration;
    private int mDelay;
    private int mInOut;
    private Vector3f mFrom;
    private Vector3f mTo;
    private boolean mTolerateSameValue;

    private AnimListener mListener;
    private AnimatorSet mAnimationSet;

    private List<Animator> mAnimList;

    public Anim(View view, int type, int time, int easeInOut, Vector3f from, Vector3f to) {
        this(view, type, time, 0, easeInOut, from, to, false);
    }

    public Anim(View view, int type, int time, int delay, int easeInOut, Vector3f from, Vector3f to,
                boolean tolerateSameValue) {
        if (type != XY
                && type != ROTATE
                && type != SCALE
                && type != TRANSPARENT
                && type != TRANSLATE) {
            throw new IllegalArgumentException("error anim type ["+type+"]");
        }
        if (from == null || to == null) {
            throw new IllegalArgumentException("lose from or to");
        }
        mView = view;
        animType = type;
        duration = time;
        mDelay = delay;
        mInOut = easeInOut;
        mFrom = from;
        mTo = to;
        if (from == null || to == null) {
            throw new IllegalArgumentException("something is null ["+from+"]["+to+"]");
        }
        mTolerateSameValue = tolerateSameValue;
        buildAnim(from, to);
    }

    public Anim(View view, int type, int time, int easeInOut, Vector3f from, Vector3f to, boolean tolerateSameValue) {
        this(view, type, time, 0, easeInOut, from, to, tolerateSameValue);
    }

    private void buildAnim(Vector3f from, Vector3f to) {
        mAnimList = new ArrayList<Animator>();
        switch (animType) {
            case XY : {
                if (from.getX() != to.getX() || mTolerateSameValue) {
                    ObjectAnimator animator = ObjectAnimator.ofFloat(mView, X, from.getX(), to.getX());
                    mAnimList.add(animator);
                }
                if (from.getY() != to.getY() || mTolerateSameValue) {
                    ObjectAnimator animator = ObjectAnimator.ofFloat(mView, Y, from.getY(), to.getY());
                    mAnimList.add(animator);
                }
                break;
            }
            case ROTATE : {
                if (from.getX() != to.getX() || mTolerateSameValue) {
                    ObjectAnimator animator = ObjectAnimator.ofFloat(mView, ROTATION_X, from.getX(), to.getX());
                    mAnimList.add(animator);
                }
                if (from.getY() != to.getY() || mTolerateSameValue) {
                    ObjectAnimator animator = ObjectAnimator.ofFloat(mView, ROTATION_Y, from.getY(), to.getY());
                    mAnimList.add(animator);
                }
                if (from.getZ() != to.getZ() || mTolerateSameValue) {
                    ObjectAnimator animator = ObjectAnimator.ofFloat(mView, ROTATION, from.getZ(), to.getZ());
                    mAnimList.add(animator);
                }
                break;
            }
            case SCALE : {
                if (from.getX() != to.getX() || mTolerateSameValue) {
                    ObjectAnimator animator = ObjectAnimator.ofFloat(mView, SCALE_X, from.getX(), to.getX());
                    mAnimList.add(animator);
                }
                if (from.getY() != to.getY() || mTolerateSameValue) {
                    ObjectAnimator animator = ObjectAnimator.ofFloat(mView, SCALE_Y, from.getY(), to.getY());
                    mAnimList.add(animator);
                }
                break;
            }
            case TRANSPARENT : {
                if (mFrom.getZ() != mTo.getZ() || mTolerateSameValue) {
                    ObjectAnimator animator = ObjectAnimator.ofFloat(mView, ALPHA, mFrom.getZ(), mTo.getZ());
                    //?
                    if(mFrom.getX() == 1) {
                        // do nothing with layer type
                    } else {
                        mView.setLayerType(View.LAYER_TYPE_HARDWARE, null);
                    }
                    mAnimList.add(animator);
                }
                break;
            }
            case TRANSLATE : {
                if (from.getX() != to.getX() || mTolerateSameValue) {
                    ObjectAnimator animator = ObjectAnimator.ofFloat(mView, TRANSLATE_X, from.getX(), to.getX());
                    mAnimList.add(animator);
                }
                if (from.getY() != to.getY() || mTolerateSameValue) {
                    ObjectAnimator animator = ObjectAnimator.ofFloat(mView, TRANSLATE_Y, from.getY(), to.getY());
                    mAnimList.add(animator);
                }
                break;
            }
        }
        AnimInterpolator.Interpolator interpolator = null;
        if (mInOut != 0) {
            interpolator = new AnimInterpolator.Interpolator(mInOut);
        }
        for (Animator animator : mAnimList) {
            animator.setDuration(duration);
            if (interpolator != null) {
                animator.setInterpolator(interpolator);
            }
        }
    }

    public Vector3f getFrom() {
        return mFrom;
    }

    public Vector3f getTo() {
        return mTo;
    }

    public int getAnimType() {
        return animType;
    }

    public View getView() {
        return mView;
    }

    public void setDelay(long delay) {
        if (mAnimList == null) {
            return;
        }
        if (delay == 0) {
            return;
        }
        for (Animator animator : mAnimList) {
            animator.setStartDelay(delay);
        }
    }

    public void setInterpolator(int easeInOut, float param) {
        AnimInterpolator.Interpolator interpolator = null;
        if (easeInOut != 0) {
            interpolator = new AnimInterpolator.Interpolator(easeInOut, param);
        }
        for (Animator animator : mAnimList) {
            animator.setDuration(duration);
            if (interpolator != null) {
                animator.setInterpolator(interpolator);
            }
        }
    }

    public void setAnimCallbackListener() {
        if (mAnimList == null || mAnimList.size() == 0) {
            return;
        }
        if (mListener != null) {
            //need do some callback
            long totalTime = 0;
            Animator lastAnim = null;
            for (Animator animator : mAnimList) {
                long delta = animator.getDuration() + animator.getStartDelay();
                if (delta >= totalTime) {
                    totalTime = delta;
                    lastAnim = animator;
                }
            }
            if (lastAnim != null) {
                AnimatorCallbackListener listener = new AnimatorCallbackListener(mListener);
                AnimatorUpdateListener upatelistener = new AnimatorUpdateListener(mListener);
                lastAnim.addListener(listener);
                ((ObjectAnimator)lastAnim).addUpdateListener(upatelistener);
            } else {
                throw new IllegalArgumentException("set anim listener err !");
            }
        }
    }

    public void setFrom(Vector3f from) {
        if (from == null) {
            return;
        }
        mFrom = from;
        buildAnim(mFrom, mTo);
    }

    public void setTo(Vector3f to) {
        if (to == null) {
            return;
        }
        mTo = to;
        buildAnim(mFrom, mTo);
    }

    public boolean isEmpty() {
        if (mAnimList == null || mAnimList.size() == 0) {
            return true;
        }
        return false;
    }

    public boolean start() {
        if (isEmpty()) {
            return false;
        }
        setAnimCallbackListener();
        mAnimationSet = new AnimatorSet();
        mAnimationSet.playTogether(mAnimList);
        mAnimationSet.start();
        return true;
    }

    public boolean isRunning() {
        return mAnimationSet.isRunning();
    }

    public List<ObjectAnimator> getAnimatorList() {
        if (mAnimList == null || mAnimList.size() == 0) {
            return null;
        }
        List<ObjectAnimator> list = new ArrayList<ObjectAnimator>();
        int size = mAnimList.size();
        for (int i = 0; i < size; i++) {
            ObjectAnimator anim = (ObjectAnimator) mAnimList.get(i);
            if (anim != null) {
                list.add(anim);
            }
        }
        return list;
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
                mAnimListener.onComplete(ANIM_FINISH_TYPE_COMPLETE);
            }
        }

        @Override
        public void onAnimationCancel(Animator animator) {
            if (mAnimListener != null) {
                mAnimListener.onComplete(ANIM_FINISH_TYPE_CANCELED);
            }
        }

        @Override
        public void onAnimationRepeat(Animator animator) {

        }
    }

    public void cancel() {
        if (mAnimationSet == null) {
            return;
        }
        mAnimationSet.cancel();
    }

    public void setListener(AnimListener l) {
        mListener = l;
    }
}