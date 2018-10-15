package com.kborid.library.util;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.LinearInterpolator;
import android.view.animation.ScaleAnimation;
import android.view.animation.TranslateAnimation;
import android.widget.TextView;

public class AnimationUtil {

    private static final int ANIM_DURATION = 200;

    public static void hideResultDialogWithAnim(final View view) {
        if (view != null && view.getVisibility() == View.VISIBLE) {
            Animation transAnim = new TranslateAnimation(0, 0, 0, 100);
            view.setVisibility(View.GONE);
            AnimationSet set = new AnimationSet(true);
            Animation alphaAnim = new AlphaAnimation(1.0f, 0.0f);
            set.setInterpolator(new DecelerateInterpolator(1.0f));
            set.addAnimation(transAnim);
            set.addAnimation(alphaAnim);
            set.setDuration(ANIM_DURATION);
            view.startAnimation(set);
        }
    }

    public static void stopRecordWithAnimation(final View view) {
        if (view != null && view.getVisibility() != View.GONE) {
            Animation anim = new ScaleAnimation(1.0f, 0.8f, 1.0f, 0.8f, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
            anim.setDuration(ANIM_DURATION);
            anim.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {
                }

                @Override
                public void onAnimationRepeat(Animation animation) {
                }

                @Override
                public void onAnimationEnd(Animation animation) {
                    view.setVisibility(View.GONE);
                }
            });
            view.startAnimation(anim);
        }
    }

    public static void cancelAnimation(View view) {
        if (view != null) {
            Animation anim = view.getAnimation();
            if (anim != null && anim instanceof AnimationSet) {
                AnimationSet as = (AnimationSet) anim;
                for (Animation a : as.getAnimations()) {
                    a.cancel();
                }
                as.cancel();
            } else if (anim != null) {
                anim.cancel();
            }
        }
    }

    public static void showCoverWithAnimation(final View view , final Animation.AnimationListener l) {
        if (view != null && view.getVisibility() != View.VISIBLE) {
            ((TextView)view).setTextSize(13);
            Animation anim = new AlphaAnimation(0.0f, 1.0f);
            anim.setDuration(ANIM_DURATION);
            if(l != null){
                anim.setAnimationListener(l);
            } else {
                view.setVisibility(View.VISIBLE);
            }
            view.clearAnimation();
            view.startAnimation(anim);
        }
    }

    public static void startRecordWithAnimation(final View view) {
        AnimationSet set = new AnimationSet(true);
        float random = (float) (1.0 + 0.08 * Math.random());
        Animation anim = new ScaleAnimation(0.95f, random, 0.95f, random, Animation.RELATIVE_TO_SELF,0.5f, Animation.RELATIVE_TO_SELF,0.5f);
        anim.setDuration(80);

        Animation anim1 = new ScaleAnimation(random, 0.95f, random, 0.95f, Animation.RELATIVE_TO_SELF,0.5f, Animation.RELATIVE_TO_SELF,0.5f);
        anim1.setStartOffset(80);
        anim1.setDuration(100);
        anim1.setInterpolator(new LinearInterpolator());

        set.addAnimation(anim);
        set.addAnimation(anim1);
        set.setAnimationListener(new Animation.AnimationListener() {

            @Override
            public void onAnimationEnd(Animation animation) {
                startRecordWithAnimation(view);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }

            @Override
            public void onAnimationStart(Animation animation) {
            }
        });
        view.setPadding(1, 1, 1, 1);
        view.setVisibility(View.VISIBLE);
        view.startAnimation(set);
    }

    public static void showResultDialogWithAnim(final View view) {
        if (view != null && view.getVisibility() != View.VISIBLE) {
            view.setVisibility(View.VISIBLE);
            Animation transAnim = new TranslateAnimation(0, 0, 100, 0);
            AnimationSet set = new AnimationSet(true);
            Animation alphaAnim = new AlphaAnimation(0.0f, 1.0f);
            set.setInterpolator(new DecelerateInterpolator(1.0f));
            set.addAnimation(transAnim);
            set.addAnimation(alphaAnim);
            set.setDuration(ANIM_DURATION);
            view.startAnimation(set);
        }
    }

    public static void hideCoverWithAnimation(final View view) {
        if (view != null && view.getVisibility() == View.VISIBLE) {
            view.setVisibility(View.GONE);
            ((TextView)view).setTextSize(0);
            Animation anim = new AlphaAnimation(1.0f, 0.0f);
            anim.setDuration(ANIM_DURATION);
            view.clearAnimation();
            view.startAnimation(anim);
        }
    }

    public static void showViewWithAlphaAndTranslate(final View view, long delayTime,
                                                     long duration, final int translateOffsetY) {
        AnimatorSet set = new AnimatorSet();
        ObjectAnimator alphaAnimator = ObjectAnimator.ofFloat(view, "alpha", 0f, 1f);
        ObjectAnimator translateAnimator = ObjectAnimator.ofFloat(view, "translationY", translateOffsetY, 0);
        set.setStartDelay(delayTime);
        set.setDuration(duration);
        set.setInterpolator(new DecelerateInterpolator(1.5f));
        set.playTogether(alphaAnimator, translateAnimator);
        set.addListener(new Animator.AnimatorListener() {

            @Override
            public void onAnimationStart(Animator animation) {
                view.setAlpha(0);
                view.setTranslationY(translateOffsetY);
            }

            @Override
            public void onAnimationRepeat(Animator animation) {

            }

            @Override
            public void onAnimationEnd(Animator animation) {
                view.clearAnimation();
            }

            @Override
            public void onAnimationCancel(Animator animation) {

            }
        });
        set.start();
    }
}
