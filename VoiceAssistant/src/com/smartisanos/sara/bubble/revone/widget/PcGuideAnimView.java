package com.smartisanos.sara.bubble.revone.widget;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.RelativeLayout;

import com.smartisanos.sara.R;
import com.smartisanos.sara.widget.IGuideAnimView;

import java.util.ArrayList;

public class PcGuideAnimView extends RelativeLayout implements IGuideAnimView {
    private static final int ANIM_DURATION_SHORT = 200;
    private static final int ANIM_DURATION_LONG = 400;
    private static final int ANIM_STEP_DELAY = 1200;
    private AnimatorSet mAnimSet;
    private View mGuideMask;
    private View mHandView;
    private View mDotCenter;
    private View mListView;
    private View mButtonView;
    private View mDotRight;
    private int mListTranslationX;
    private int mHandTranslationY;
    private DecelerateInterpolator mInterpolator = new DecelerateInterpolator(1.5f);

    public PcGuideAnimView(Context context) {
        this(context, null);
    }

    public PcGuideAnimView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public PcGuideAnimView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onFinishInflate() {
        mGuideMask = findViewById(R.id.guide_mask);
        mHandView = findViewById(R.id.guide_hand);
        mDotCenter = findViewById(R.id.guide_center_dot);
        mListView = findViewById(R.id.guide_list);
        mButtonView = findViewById(R.id.guide_button);
        mDotRight = findViewById(R.id.guide_right_dot);
        mListTranslationX = mContext.getResources().getDimensionPixelSize(R.dimen.revone_guide_list_translation_x);
        mHandTranslationY = mContext.getResources().getDimensionPixelSize(R.dimen.revone_guide_hand_translation_y);
        reset();
    }

    private void reset() {
        mGuideMask.setAlpha(0);
        mDotRight.setAlpha(0);
        mDotCenter.setAlpha(0);
        mHandView.setAlpha(0);
        mButtonView.setScaleX(0f);
        mButtonView.setScaleY(0f);
        mListView.setTranslationX(mListTranslationX);
    }

    @Override
    public void setShowHandAnim(boolean isShowHand) {
    }

    @Override
    public void setBackgroundByLauncherMode() {
    }

    @Override
    public void show() {
        if (getVisibility() != View.VISIBLE) {
            reset();
            setVisibility(View.VISIBLE);
            startAnim();
        }
    }

    @Override
    public void hide() {
        setVisibility(View.GONE);
        cancelAnim();
    }

    public void cancelAnim() {
        if (mAnimSet != null) {
            mAnimSet.removeAllListeners();
            mAnimSet.end();
            mAnimSet.cancel();
            mAnimSet = null;
        }
    }

    public void startAnim() {
        final ArrayList<Animator> animatorList = new ArrayList<Animator>();
        animatorList.add(getButtonZoomInAnim());
        animatorList.add(getDotClickAnim());
        animatorList.add(getListSlideInAnim());
        animatorList.add(getHandClickAnim());
        animatorList.add(getListSlideOutAnim());
        mAnimSet = new AnimatorSet();
        mAnimSet.playSequentially(animatorList);
        mAnimSet.setStartDelay(ANIM_STEP_DELAY);
        mAnimSet.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                startAnim();
            }

            @Override
            public void onAnimationCancel(Animator animation) {
            }

            @Override
            public void onAnimationRepeat(Animator animation) {
            }
        });
        mAnimSet.start();
    }

    private Animator getButtonZoomInAnim() {
        PropertyValuesHolder scaleInX = PropertyValuesHolder.ofFloat("scaleX", 0f, 1f);
        PropertyValuesHolder scaleInY = PropertyValuesHolder.ofFloat("scaleY", 0f, 1f);
        Animator buttonIn = ObjectAnimator.ofPropertyValuesHolder(mButtonView, scaleInX, scaleInY);
        buttonIn.setInterpolator(mInterpolator);
        buttonIn.setDuration(ANIM_DURATION_LONG);
        return buttonIn;
    }

    private Animator getDotClickAnim() {
        PropertyValuesHolder alphaIn = PropertyValuesHolder.ofFloat("alpha", 0f, 1f);
        PropertyValuesHolder alphaOut = PropertyValuesHolder.ofFloat("alpha", 1f, 0f);
        PropertyValuesHolder scaleDotInX = PropertyValuesHolder.ofFloat("scaleX", 0.5f, 1.5f);
        PropertyValuesHolder scaleDotInY = PropertyValuesHolder.ofFloat("scaleY", 0.5f, 1.5f);
        Animator rightDotIn = ObjectAnimator.ofPropertyValuesHolder(mDotRight, scaleDotInX, scaleDotInY, alphaIn);
        rightDotIn.setInterpolator(mInterpolator);
        rightDotIn.setDuration(ANIM_DURATION_SHORT);

        PropertyValuesHolder scaleDotOutX = PropertyValuesHolder.ofFloat("scaleX", 1.5f, 0.5f);
        PropertyValuesHolder scaleDotOutY = PropertyValuesHolder.ofFloat("scaleY", 1.5f, 0.5f);
        Animator rightDotOut = ObjectAnimator.ofPropertyValuesHolder(mDotRight, scaleDotOutX, scaleDotOutY, alphaOut);
        rightDotOut.setInterpolator(mInterpolator);
        rightDotOut.setDuration(ANIM_DURATION_SHORT);

        PropertyValuesHolder scaleOutX = PropertyValuesHolder.ofFloat("scaleX", 1f, 0f);
        PropertyValuesHolder scaleOutY = PropertyValuesHolder.ofFloat("scaleY", 1f, 0f);
        Animator buttonOut = ObjectAnimator.ofPropertyValuesHolder(mButtonView, scaleOutX, scaleOutY);
        buttonOut.setInterpolator(mInterpolator);
        buttonOut.setDuration(ANIM_DURATION_LONG);

        AnimatorSet animSet = new AnimatorSet();
        animSet.setStartDelay(ANIM_STEP_DELAY);
        animSet.playSequentially(rightDotIn, rightDotOut, buttonOut);
        return animSet;
    }

    private Animator getListSlideInAnim() {
        PropertyValuesHolder alphaIn = PropertyValuesHolder.ofFloat("alpha", 0f, 1f);
        PropertyValuesHolder transX = PropertyValuesHolder.ofFloat("translationX", mListTranslationX, 0);
        Animator listIn = ObjectAnimator.ofPropertyValuesHolder(mListView, transX);
        listIn.setInterpolator(mInterpolator);
        Animator mMaskIn = ObjectAnimator.ofPropertyValuesHolder(mGuideMask, alphaIn);
        mMaskIn.setInterpolator(mInterpolator);
        AnimatorSet animSet = new AnimatorSet();
        animSet.playTogether(listIn, mMaskIn);
        animSet.setDuration(ANIM_DURATION_LONG);
        return animSet;
    }

    private Animator getHandClickAnim() {
        PropertyValuesHolder alphaIn = PropertyValuesHolder.ofFloat("alpha", 0f, 1f);
        PropertyValuesHolder alphaOut = PropertyValuesHolder.ofFloat("alpha", 1f, 0f);
        PropertyValuesHolder scaleDotInX = PropertyValuesHolder.ofFloat("scaleX", 0.5f, 1.5f);
        PropertyValuesHolder scaleDotInY = PropertyValuesHolder.ofFloat("scaleY", 0.5f, 1.5f);
        PropertyValuesHolder scaleDotOutX = PropertyValuesHolder.ofFloat("scaleX", 1.5f, 0.5f);
        PropertyValuesHolder scaleDotOutY = PropertyValuesHolder.ofFloat("scaleY", 1.5f, 0.5f);
        PropertyValuesHolder transY = PropertyValuesHolder.ofFloat("translationY", mHandTranslationY, 0);
        Animator handIn = ObjectAnimator.ofPropertyValuesHolder(mHandView, transY, alphaIn);
        handIn.setInterpolator(mInterpolator);
        handIn.setDuration(ANIM_DURATION_LONG);
        Animator centerDotIn = ObjectAnimator.ofPropertyValuesHolder(mDotCenter, scaleDotInX, scaleDotInY, alphaIn);
        centerDotIn.setInterpolator(mInterpolator);
        centerDotIn.setDuration(ANIM_DURATION_SHORT);
        Animator centerDotOut = ObjectAnimator.ofPropertyValuesHolder(mDotCenter, scaleDotOutX, scaleDotOutY, alphaOut);
        centerDotOut.setInterpolator(mInterpolator);
        centerDotOut.setDuration(ANIM_DURATION_SHORT);
        transY = PropertyValuesHolder.ofFloat("translationY", 0, mHandTranslationY);
        Animator handOut = ObjectAnimator.ofPropertyValuesHolder(mHandView, transY, alphaOut);
        handOut.setInterpolator(mInterpolator);
        handOut.setDuration(ANIM_DURATION_LONG);
        AnimatorSet animSet = new AnimatorSet();
        animSet.setStartDelay(ANIM_STEP_DELAY);
        animSet.playSequentially(handIn, centerDotIn, centerDotOut);
        animSet.playTogether(centerDotOut, handOut);
        return animSet;
    }

    private Animator getListSlideOutAnim() {
        PropertyValuesHolder alphaOut = PropertyValuesHolder.ofFloat("alpha", 1f, 0f);
        PropertyValuesHolder transX = PropertyValuesHolder.ofFloat("translationX", 0, mListTranslationX);
        Animator listOut = ObjectAnimator.ofPropertyValuesHolder(mListView, transX);
        listOut.setInterpolator(mInterpolator);
        Animator mMaskOut = ObjectAnimator.ofPropertyValuesHolder(mGuideMask, alphaOut);
        mMaskOut.setInterpolator(mInterpolator);
        AnimatorSet animSet = new AnimatorSet();
        animSet.setDuration(ANIM_DURATION_LONG);
        animSet.playTogether(listOut, mMaskOut);
        return animSet;
    }
}
