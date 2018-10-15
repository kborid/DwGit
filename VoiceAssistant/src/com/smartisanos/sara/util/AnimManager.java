package com.smartisanos.sara.util;
import android.animation.Animator;
import android.animation.Animator.AnimatorListener;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.graphics.PointF;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.TranslateAnimation;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.smartisanos.ideapills.common.util.TaskHandler;

public class AnimManager {

    public static final int ANIM_DURATION = 10;
    public static final int TYPE_RESULT = 0;
    public static final int TYPE_EMPTY = 1;
    public static final int TYPE_FORBIDDEN = 2;

    private static final int ANIM_MESSAGE_UPDATE = 1;
    private static final int ANIM_MESSAGE_END = 0;

    public static final int HIDE_BUBBLE_TEXT_POPUP_DURATION  = 80;
    public static final int SHOW_BUBBLE_TEXT_CONTENT_DURATION  = 140;
    public static final int SHOW_BUBBLE_RESULT_DURATION = 250;
    public static final int FLY_BUBBLE_TO_GLOBLE = 300;
    public static final int SHOW_VOICE_DURATION = 250;
    public static final int BREATH_SCALE_TRANSLATE_DURATION = 1000;
    public static void setViewVisibilityWithAnim(View view, int visibility) {
        switch (visibility) {
            case View.VISIBLE:
                showViewWithAlphaAnim(view, ANIM_DURATION);
                break;
            case View.INVISIBLE:
            case View.GONE:
                HideViewWithAlphaAnim(view, ANIM_DURATION, 0);
                break;
        }
    }

    public static void HideViewWithAlphaAnim(final View view, int duration, int startDelay, final int state) {
        if (view != null && view.getVisibility() == View.VISIBLE) {
            Animation anim = new AlphaAnimation(1, 0);
            anim.setDuration(duration);
            anim.setStartOffset(startDelay);
            anim.setInterpolator(new DecelerateInterpolator(1.5f));
            anim.setAnimationListener(new AnimationListener() {

                @Override
                public void onAnimationStart(Animation animation) {

                }

                @Override
                public void onAnimationEnd(Animation animation) {
                    view.setVisibility(state);
                    view.clearAnimation();
                    view.setAlpha(1);
                }

                @Override
                public void onAnimationRepeat(Animation animation) {

                }

            });
            view.startAnimation(anim);
        }
    }

    public static void HideViewWithAlphaAnim(final View view, int duration, int startDelay) {
        HideViewWithAlphaAnim(view, duration, startDelay, View.GONE);
    }

    public static void showViewWithAlphaAnim(final View view, int duration) {
        if (view != null) {
            view.setVisibility(View.VISIBLE);
            Animation anim = new AlphaAnimation(0, 1);
            anim.setDuration(duration);
            anim.setAnimationListener(new AnimationListener() {

                @Override
                public void onAnimationStart(Animation animation) {

                }

                @Override
                public void onAnimationEnd(Animation animation) {
                    view.clearAnimation();
                }

                @Override
                public void onAnimationRepeat(Animation animation) {

                }

            });
            view.startAnimation(anim);
        }
    }

    public static void showViewWithBottomInAnim(final View view, final int type) {
        if (view != null) {
            view.setVisibility(View.VISIBLE);
            Animation transAnim = null;
            if (type == TYPE_RESULT) {
                transAnim = new TranslateAnimation(0, 0, 1713, 0);
            } else {
                transAnim = new TranslateAnimation(0, 0, 1616, 0);
            }
            transAnim.setDuration(ANIM_DURATION);
            view.startAnimation(transAnim);
            transAnim.setAnimationListener(new AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {
                }

                @Override
                public void onAnimationEnd(Animation animation) {
                    view.clearAnimation();
                }

                @Override
                public void onAnimationRepeat(Animation animation) {
                }
            });
        }
    }
    public static void hideViewWithTopOutAnim(final View view, int type) {
        if (view != null && view.getVisibility() == View.VISIBLE) {
            Animation transAnim = null;
            if (type == TYPE_RESULT) {
                transAnim = new TranslateAnimation(0, 0, 0, -1557);
            } else if (type == TYPE_EMPTY) {
                transAnim = new TranslateAnimation(0, 0, 0, -1467);
            }

            transAnim.setAnimationListener(new AnimationListener() {

                @Override
                public void onAnimationStart(Animation animation) {
                }

                @Override
                public void onAnimationEnd(Animation animation) {
                    view.setVisibility(View.GONE);
                    view.clearAnimation();
                }

                @Override
                public void onAnimationRepeat(Animation animation) {
                }

            });

            transAnim.setDuration(ANIM_DURATION);
            view.startAnimation(transAnim);
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
        set.addListener(new AnimatorListener() {

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

    private static Handler mHandler =  new Handler(Looper.getMainLooper(), new Handler.Callback(){
        @Override
        public boolean handleMessage(Message msg) {
            switch (msg.what) {
                case ANIM_MESSAGE_END:
                    ((View) msg.obj).clearAnimation();
                case ANIM_MESSAGE_UPDATE:
                    View view = (View) msg.obj;
                    Bundle data = msg.getData();
                    int height = data.getInt("height");
                    float translationY = data.getFloat("translationY");
                    view.setTranslationY(translationY);
                    FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) view.getLayoutParams();
                    if (Math.rint(height - params.height) == 0) {
                        break;
                    }
                    params.height = height;
                    view.setLayoutParams(params);
                    break;
                default:
                    return false;
            }
            return true;
        }
    });

    public static void showViewWithTranslateAndHeight(final View view,
            final int duration, final int startHeight, final int targetHeight, final int targetTranslate, final int delayTime) {
        if (view != null) {
            final float startTranslate = view.getTranslationY();
            final float diffTranslate = targetTranslate - startTranslate;
            TaskHandler.post(new Runnable() {
                @Override
                public void run() {
                    ValueAnimator timerAnimator = ValueAnimator.ofInt(startHeight, targetHeight);
                    timerAnimator.addUpdateListener(new AnimatorUpdateListener() {
                        @Override
                        public void onAnimationUpdate(ValueAnimator animation) {
                            final int value = (int) animation.getAnimatedValue();
                            float translationY = (float) animation.getAnimatedFraction() * diffTranslate + startTranslate;
                            Message message = new Message();
                            message.what = ANIM_MESSAGE_UPDATE;
                            message.obj = view;
                            Bundle bundle = new Bundle();
                            bundle.putInt("height",value);
                            bundle.putFloat("translationY",translationY);
                            message.setData(bundle);
                            mHandler.sendMessage(message);
                        }
                    });
                    timerAnimator.setInterpolator(new DecelerateInterpolator(1.5f));
                    timerAnimator.setDuration(duration);
                    timerAnimator.setStartDelay(delayTime);
                    timerAnimator.addListener(new AnimatorListener() {
                        @Override
                        public void onAnimationStart(Animator animation) {
                        }

                        @Override
                        public void onAnimationRepeat(Animator animation) {
                        }

                        @Override
                        public void onAnimationEnd(Animator animation) {
                            Message message = new Message();
                            message.what = ANIM_MESSAGE_END;
                            message.obj = view;
                            Bundle bundle = new Bundle();
                            bundle.putInt("height",targetHeight);
                            bundle.putFloat("translationY",targetTranslate);
                            message.setData(bundle);
                            mHandler.sendMessage(message);
                        }

                        @Override
                        public void onAnimationCancel(Animator animation) {
                        }
                    });
                    timerAnimator.start();
                }
            });
        }
    }

    public static void hideViewWithScaleAnim(final View view, final View childView, PointF point) {
        final float initX = view.getX();
        final float initY = view.getY();
        ObjectAnimator scaleXAnim = ObjectAnimator.ofFloat(view, View.SCALE_X, 0.0f);

        ObjectAnimator scaleYAnim = ObjectAnimator.ofFloat(view, View.SCALE_Y, 0.0f);
        ObjectAnimator xAnim = ObjectAnimator.ofFloat(view, View.X, point.x);
        ObjectAnimator yAnim = ObjectAnimator.ofFloat(view, View.Y, point.y);

        ObjectAnimator alphaAnim = ObjectAnimator.ofFloat(view, View.ALPHA, 0f);
        ObjectAnimator childAlphaAnim = ObjectAnimator.ofFloat(childView, View.ALPHA, 0f);
        childAlphaAnim.setDuration(150);
        AnimatorSet set = new AnimatorSet();
        set.setDuration(FLY_BUBBLE_TO_GLOBLE);
        set.playTogether(scaleXAnim, scaleYAnim, xAnim, yAnim, alphaAnim, childAlphaAnim);
        set.setInterpolator(new DecelerateInterpolator(1.5f));
        set.addListener(new AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {

            }

            @Override
            public void onAnimationRepeat(Animator animation) {
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                view.setVisibility(View.GONE);
                view.setAlpha(1);
                childView.setAlpha(1);
                view.setScaleX(1);
                view.setScaleY(1);
                view.setX(initX);
                view.setY(initY);
                childView.clearAnimation();
                view.clearAnimation();
            }

            @Override
            public void onAnimationCancel(Animator animation) {
            }
        });
        set.start();
    }

    public static void hideViewWithAlphaAndTranslate(final View view, long delayTime,
            long duration, final int translateOffsetY, final int state) {
        if (view == null || view.getVisibility() != View.VISIBLE){
            return;
        }
        AnimatorSet set = new AnimatorSet();
        ObjectAnimator alphaAnimator = ObjectAnimator.ofFloat(view, "alpha", 1f, 0f);
        ObjectAnimator translateAnimator = ObjectAnimator.ofFloat(view, "translationY", view.getTranslationY(), translateOffsetY);
        set.setStartDelay(delayTime);
        set.setDuration(duration);
        set.setInterpolator(new DecelerateInterpolator(1.5f));
        set.playTogether(alphaAnimator, translateAnimator);
        set.addListener(new AnimatorListener() {

            @Override
            public void onAnimationStart(Animator animation) {
            }

            @Override
            public void onAnimationRepeat(Animator animation) {

            }

            @Override
            public void onAnimationEnd(Animator animation) {
                view.clearAnimation();
                view.setVisibility(state);
                view.setAlpha(1);
                view.setTranslationY(0);
            }

            @Override
            public void onAnimationCancel(Animator animation) {

            }
        });
        set.start();
    }

    public static void hideViewWithAlphaAndTranslate(final View view, long delayTime, long duration, final int translateOffsetY) {
        hideViewWithAlphaAndTranslate(view, delayTime, duration, translateOffsetY, View.GONE);
    }

    public static void showViewWithTranslate(final View view, long delayTime,
            long duration, int targetTranslateY, final View childView, int targetHeight) {
        AnimatorSet set = new AnimatorSet();
        ObjectAnimator translateAnimator = ObjectAnimator.ofFloat(view, "translationY", view.getTranslationY(), targetTranslateY);
        ValueAnimator heightAnimator = ValueAnimator.ofFloat(view.getMeasuredHeight(), targetHeight);
        heightAnimator.addUpdateListener(new AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float value = (Float) animation.getAnimatedValue();
                FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) view.getLayoutParams();
                params.height = (int) value;
                view.setLayoutParams(params);
            }
        });
        heightAnimator.setTarget(view);

        set.setStartDelay(delayTime);
        set.setDuration(duration);
        set.setInterpolator(new DecelerateInterpolator(1.5f));
        set.playTogether(translateAnimator, heightAnimator);
        set.addListener(new AnimatorListener() {

            @Override
            public void onAnimationStart(Animator animation) {
            }

            @Override
            public void onAnimationRepeat(Animator animation) {
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                view.clearAnimation();
                if (childView!= null){
                    childView.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onAnimationCancel(Animator animation) {

            }
        });
        set.start();
    }
    public static void showVoiceWaveAnim(final View view,final View arrowView, final View iconView,
            final View showView, int startDelay, int mDisplayWidth, int targetWidth) {
        AnimatorSet set = new AnimatorSet();

        ObjectAnimator viewalphaAnimator = ObjectAnimator.ofFloat(view, "alpha", 1f, 0f);
        viewalphaAnimator.setStartDelay(startDelay);
        viewalphaAnimator.setDuration(SHOW_VOICE_DURATION);

        ObjectAnimator showViewalphaAnimator = ObjectAnimator.ofFloat(showView, "alpha", 0f, 1f);
        showViewalphaAnimator.setStartDelay(startDelay);
        showViewalphaAnimator.setDuration(SHOW_VOICE_DURATION);

        ObjectAnimator arrowAlphaAnimator = ObjectAnimator.ofFloat(arrowView, "alpha", 0f, 1f);
        arrowAlphaAnimator.setStartDelay(startDelay);
        arrowAlphaAnimator.setDuration(SHOW_VOICE_DURATION - 30);

        FrameLayout.LayoutParams arrowParams = (FrameLayout.LayoutParams) arrowView.getLayoutParams();
        //22.7f * 3
        ObjectAnimator arrowTranslateAnimator = ObjectAnimator.ofFloat(arrowView,"translationX", 0,(mDisplayWidth - arrowParams.width) / 2 - arrowParams.leftMargin);
        arrowTranslateAnimator.setStartDelay(startDelay);
        arrowTranslateAnimator.setDuration(SHOW_VOICE_DURATION);
        ObjectAnimator iconAlphaAnimator = ObjectAnimator.ofFloat(iconView, "alpha", 0f, 1f);
        iconAlphaAnimator.setStartDelay(startDelay);
        iconAlphaAnimator.setDuration(SHOW_VOICE_DURATION - 30);

        FrameLayout.LayoutParams iconParams = (FrameLayout.LayoutParams) iconView.getLayoutParams();
        ObjectAnimator iconTranslateAnimator = ObjectAnimator.ofFloat(iconView, "translationX", 0,
                (mDisplayWidth - iconView.getMeasuredWidth()) / 2 + (iconView.getMeasuredWidth() - targetWidth) / 2  - iconParams.leftMargin);
        iconTranslateAnimator.setStartDelay(startDelay);
        iconTranslateAnimator.setDuration(SHOW_VOICE_DURATION);
        ValueAnimator popopWidthAnimator = ValueAnimator.ofFloat(iconParams.width, targetWidth);
        popopWidthAnimator.addUpdateListener(new AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float value = (Float) animation.getAnimatedValue();
                FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) iconView.getLayoutParams();
                params.width = (int) value;
                iconView.setLayoutParams(params);
            }
        });
        popopWidthAnimator.setTarget(iconView);
        popopWidthAnimator.setStartDelay(startDelay);
        popopWidthAnimator.setDuration(SHOW_VOICE_DURATION);
        ObjectAnimator iconAlphaAnimator2 = ObjectAnimator.ofFloat(iconView, "alpha", 1f, 0f);
        iconAlphaAnimator2.setStartDelay(startDelay + SHOW_VOICE_DURATION - 30);
        iconAlphaAnimator2.setDuration(30);
        ObjectAnimator arrowAlphaAnimator2 = ObjectAnimator.ofFloat(arrowView, "alpha", 1f, 0f);
        arrowAlphaAnimator2.setStartDelay(startDelay + SHOW_VOICE_DURATION - 30);
        arrowAlphaAnimator2.setDuration(30);
        set.setInterpolator(new DecelerateInterpolator(1.5f));
        set.playTogether(viewalphaAnimator,
                showViewalphaAnimator, arrowAlphaAnimator, iconAlphaAnimator,
                arrowTranslateAnimator,
                iconTranslateAnimator, popopWidthAnimator, iconAlphaAnimator2, arrowAlphaAnimator2);

        set.addListener(new AnimatorListener() {

            @Override
            public void onAnimationStart(Animator animation) {
                arrowView.setAlpha(0);
                arrowView.setVisibility(View.VISIBLE);
                iconView.setAlpha(0);
                iconView.setVisibility(View.VISIBLE);
            }

            @Override
            public void onAnimationRepeat(Animator animation) {
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                showView.setVisibility(View.VISIBLE);
                view.setVisibility(View.GONE);
                view.setAlpha(1);
                view.clearAnimation();
                iconView.clearAnimation();
                arrowView.clearAnimation();
                showView.clearAnimation();
                arrowView.setTranslationX(0);
                iconView.setTranslationX(0);
                arrowView.setVisibility(View.GONE);
                iconView.setVisibility(View.GONE);
                arrowView.setAlpha(1);
                iconView.setAlpha(1);
            }

            @Override
            public void onAnimationCancel(Animator animation) {
            }
        });
        set.start();
    }

    public static void replaceViewWithTranslateAndAlpha(final View pupopView, final View arrowView,
            final View formView,int delayTime, int displayWidth, final ImageView btnKeyView,final int targetWidth, int finalPopupWidth) {

        AnimatorSet set = new AnimatorSet();
        ObjectAnimator arrowAlphaAnimator = ObjectAnimator.ofFloat(arrowView, "alpha", 1f, 0f);

        ObjectAnimator arrowTranslateAnimator = ObjectAnimator.ofFloat(arrowView, "translationX",
                0, (displayWidth - formView.getMeasuredWidth()) / 2);

        ObjectAnimator formTranslateAnimator = ObjectAnimator.ofFloat(formView, "translationX", 0,
                (displayWidth - formView.getMeasuredWidth()) / 2);

        ObjectAnimator formAlphaAnimator = ObjectAnimator.ofFloat(formView, "alpha", 1f, 0f);

        ObjectAnimator popopTranslateAnimator = ObjectAnimator.ofFloat(pupopView, "translationX", 0,
                (displayWidth - formView.getMeasuredWidth()) / 2);

        ObjectAnimator popopAlphaAnimator = ObjectAnimator.ofFloat(pupopView, "alpha", 1f, 0f);

        ValueAnimator popopWidthAnimator = ValueAnimator.ofFloat(pupopView.getMeasuredWidth(), 192);
        popopWidthAnimator.addUpdateListener(new AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float value = (Float) animation.getAnimatedValue();
                FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) pupopView.getLayoutParams();
                params.width = (int) value;
                pupopView.setLayoutParams(params);
            }
        });
        popopWidthAnimator.setTarget(pupopView);

        ObjectAnimator btnKeyAlphaAnimator = ObjectAnimator.ofFloat(btnKeyView, "alpha", 0f, 1f);

        set.playTogether(btnKeyAlphaAnimator, arrowAlphaAnimator, arrowTranslateAnimator,
                formTranslateAnimator, popopTranslateAnimator, popopWidthAnimator,
                formAlphaAnimator, popopAlphaAnimator);
         set.setStartDelay(delayTime);
         set.setDuration(SHOW_VOICE_DURATION);
        set.setInterpolator(new DecelerateInterpolator(1.5f));
        set.addListener(new AnimatorListener() {

            @Override
            public void onAnimationStart(Animator animation) {
                btnKeyView.setAlpha(0);
                formView.setVisibility(View.VISIBLE);
                pupopView.setVisibility(View.VISIBLE);
                arrowView.setVisibility(View.VISIBLE);
            }

            @Override
            public void onAnimationRepeat(Animator animation) {

            }

            @Override
            public void onAnimationEnd(Animator animation) {
                arrowView.setVisibility(View.GONE);
                formView.setVisibility(View.GONE);
                pupopView.setVisibility(View.GONE);
                FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) pupopView.getLayoutParams();
                params.width = targetWidth;
                pupopView.setLayoutParams(params);
                arrowView.setAlpha(1);
                pupopView.setAlpha(1);
                formView.setAlpha(1);
                pupopView.clearAnimation();
                arrowView.clearAnimation();
                formView.clearAnimation();
                pupopView.setTranslationX(0);
                arrowView.setTranslationX(0);
                formView.setTranslationX(0);
                btnKeyView.setVisibility(View.VISIBLE);
            }

            @Override
            public void onAnimationCancel(Animator animation) {
            }
        });
        set.start();
    }

    public static void scaleViewAnim(View view, float scaleTarget){
        AnimatorSet set = new AnimatorSet();
        ObjectAnimator viewScaleXAnimator = ObjectAnimator.ofFloat(view, "scaleX", view.getScaleX(), scaleTarget);
        ObjectAnimator viewScaleYAnimator = ObjectAnimator.ofFloat(view, "scaleY", view.getScaleY(), scaleTarget);
        set.setDuration(250);
        set.setInterpolator(new DecelerateInterpolator(1.5f));
        set.playTogether(viewScaleXAnimator, viewScaleYAnimator);
        set.start();
    }

    public static AnimatorSet scaleViewAndTranslate(final View popup, View arrow, View keyboard) {
        AnimatorSet set = new AnimatorSet();
        final int width = popup.getLayoutParams().width;
        ValueAnimator popopWidthAnimator = ValueAnimator.ofFloat(width,
                width - 30);
        popopWidthAnimator.addUpdateListener(new AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float value = (Float) animation.getAnimatedValue();
                popup.setScaleX(value / (float) width);
            }
        });

        ObjectAnimator popAnimator = ObjectAnimator.ofFloat(popup, "translationY",
                popup.getTranslationY(), popup.getTranslationY() + 15);
        ObjectAnimator kTYAnimator = ObjectAnimator.ofFloat(keyboard, "translationY",
                keyboard.getTranslationY(), keyboard.getTranslationY() + 15);
        ObjectAnimator arrowYAnimator = ObjectAnimator.ofFloat(arrow, "translationY",
                keyboard.getTranslationY(), keyboard.getTranslationY() + 15);
        set.setDuration(250);
        set.setInterpolator(new DecelerateInterpolator(1.5f));
        set.setStartDelay(200);
        set.playTogether(popopWidthAnimator, popAnimator, kTYAnimator, arrowYAnimator);
        set.start();
        return set;
    }

}
