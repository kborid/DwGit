package com.smartisanos.sara.widget;
import java.util.ArrayList;
import android.animation.Animator;
import android.animation.Animator.AnimatorListener;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.res.Resources;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import com.smartisanos.sara.R;
import com.smartisanos.sara.util.SaraUtils;
import smartisanos.api.SettingsSmt;
public class LeftSlideBubbleAnimView extends RelativeLayout implements IGuideAnimView {

    private int HAND_START_X;
    private int HAND_START_Y;
    private int HAND_MIDDLE_X;
    private int HAND_MIDDLE_Y;
    private int HAND_END_X;
    private int HAND_END_Y;
    private int CIRCLE_START_X;
    private int CIRCLE_START_Y;
    private static final DecelerateInterpolator mInterpolator = new DecelerateInterpolator(1.5f);

    private ImageView mBgShadowView;
    private View mListView;
    private ImageView mCircleView;
    private ImageView mHandView;

    private AnimatorSet mAnimSet;

    public LeftSlideBubbleAnimView(Context context) {
        this(context, null);
    }

    public LeftSlideBubbleAnimView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public LeftSlideBubbleAnimView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        Resources res = getResources();
        HAND_START_X = res.getDimensionPixelSize(R.dimen.bubble_anim_hand_start_x);
        HAND_START_Y = res.getDimensionPixelSize(R.dimen.bubble_anim_hand_start_y);
        HAND_END_X = res.getDimensionPixelSize(R.dimen.bubble_anim_hand_end_x);
        HAND_END_Y = res.getDimensionPixelSize(R.dimen.bubble_anim_hand_end_y);
        HAND_MIDDLE_X = res.getDimensionPixelSize(R.dimen.bubble_anim_hand_middle_x);
        HAND_MIDDLE_Y = res.getDimensionPixelSize(R.dimen.bubble_anim_hand_middle_y);
        CIRCLE_START_X = res.getDimensionPixelSize(R.dimen.bubble_anim_circle_start_x);
        CIRCLE_START_Y = res.getDimensionPixelSize(R.dimen.bubble_anim_circle_start_y);
    }

    @Override
    protected void onFinishInflate() {
        mBgShadowView = (ImageView) findViewById(R.id.bg_shadow);
        mListView = findViewById(R.id.list);
        mCircleView = (ImageView) findViewById(R.id.circle);
        mHandView = (ImageView) findViewById(R.id.hand);
        init();
    }

    @Override
    public void setShowHandAnim(boolean isShowHand) {
        mHandView.setVisibility(isShowHand ? View.VISIBLE : View.GONE);
    }

    private void init() {
        mHandView.setX(HAND_START_X);
        mHandView.setY(HAND_START_Y);
        mHandView.setAlpha(0.0f);
        mListView.setTranslationX(getViewWidth(mListView));
        mBgShadowView.setAlpha(0.0f);
        mCircleView.setAlpha(0.0f);
        mCircleView.setX(CIRCLE_START_X);
        mCircleView.setY(CIRCLE_START_Y);
        mCircleView.setScaleX(0.2f);
        mCircleView.setScaleY(0.2f);
    }

    @Override
    public void setBackgroundByLauncherMode() {
        ImageView bgView = (ImageView) findViewById(R.id.bg);
        int launcherMode = SaraUtils.getCurrentLauncherMode(getContext());
        if (launcherMode == SettingsSmt.LAUNCHER_MODE_VALUE.LAUNCHER_MODE_GRIDS_9) {
            bgView.setBackgroundResource(R.drawable.launcher_bg);
            mBgShadowView.setImageResource(R.drawable.launcher_bg_press);
        } else if (launcherMode == SettingsSmt.LAUNCHER_MODE_VALUE.LAUNCHER_MODE_GRIDS_12) {
            bgView.setBackgroundResource(R.drawable.launcher_12_bg);
            mBgShadowView.setImageResource(R.drawable.launcher_20_bg_press);
        } else if (launcherMode == SettingsSmt.LAUNCHER_MODE_VALUE.LAUNCHER_MODE_GRIDS_16
                || (SaraUtils.is16OGridsOrigLauncher() && launcherMode == SettingsSmt.LAUNCHER_MODE_VALUE.LAUNCHER_MODE_ORIGINAL)){
            bgView.setBackgroundResource(R.drawable.launcher_16_bg);
            mBgShadowView.setImageResource(R.drawable.launcher_bg_press);
        } else {
            bgView.setBackgroundResource(R.drawable.launcher_20_bg);
            mBgShadowView.setImageResource(R.drawable.launcher_20_bg_press);
        }
    }

    @Override
    public void show() {
        if (getVisibility() != View.VISIBLE) {
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
        cancelAnim();
        init();
        final ArrayList<Animator> animatorList = new ArrayList<Animator>();
        animatorList.add(createHandInAnim());
        animatorList.add(createListAndSettingInAnim());
        animatorList.add(createCircleAnim());
        animatorList.add(createHandOutAnim());
        animatorList.add(createListAndSettingOutAnim());

        mAnimSet = new AnimatorSet();
        mAnimSet.playTogether(animatorList);
        mAnimSet.setStartDelay(2000l);
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

    private Animator createHandInAnim() {
        final ArrayList<Animator> animatorList = new ArrayList<Animator>();
        animatorList.add(createAlphaAnim(mHandView, 0.0f, 1.0f, 200l));

        ObjectAnimator XAnim = ObjectAnimator.ofFloat(mHandView, "X", HAND_START_X, HAND_END_X);
        XAnim.setDuration(400l);
        XAnim.setStartDelay(SaraUtils.isDeltaStartDelay() ? 200l: 400l);
        XAnim.setInterpolator(mInterpolator);
        animatorList.add(XAnim);

        ObjectAnimator YAnim = ObjectAnimator.ofFloat(mHandView, "Y", HAND_START_Y, HAND_END_Y);
        YAnim.setDuration(200l);
        YAnim.setStartDelay(SaraUtils.isDeltaStartDelay() ? 600: 800l);
        YAnim.setInterpolator(mInterpolator);
        animatorList.add(YAnim);

        animatorList.add(createAlphaAnim(mHandView, 1.0f, 0.0f, 200l, SaraUtils.isDeltaStartDelay() ? 600: 800));
        final AnimatorSet animSet = new AnimatorSet();
        animSet.playTogether(animatorList);
        return animSet;
    }

    private Animator createHandOutAnim() {
        final ArrayList<Animator> animatorList = new ArrayList<Animator>();
        Animator alpha = createAlphaAnim(mHandView, 0.0f, 1.0f, 200l, 2000l);
        animatorList.add(alpha);
        alpha.addListener(new AnimatorListener() {

            @Override
            public void onAnimationStart(Animator animation) {
                mHandView.setX(HAND_MIDDLE_X);
            }

            @Override
            public void onAnimationRepeat(Animator animation) {
            }

            @Override
            public void onAnimationEnd(Animator animation) {
            }

            @Override
            public void onAnimationCancel(Animator animation) {
            }
        });
        ObjectAnimator YAnim = ObjectAnimator.ofFloat(mHandView, "Y", HAND_END_Y, HAND_MIDDLE_Y);
        YAnim.setDuration(400l);
        YAnim.setStartDelay(SaraUtils.isDeltaStartDelay() ? 0 : 2000);
        YAnim.setInterpolator(mInterpolator);
        animatorList.add(YAnim);

        ObjectAnimator YSecondAnim = ObjectAnimator.ofFloat(mHandView, "Y", HAND_MIDDLE_Y, HAND_END_Y);
        YSecondAnim.setDuration(400l);
        YSecondAnim.setStartDelay(SaraUtils.isDeltaStartDelay() ? 600l : 2600l);
        YSecondAnim.setInterpolator(mInterpolator);
        animatorList.add(YSecondAnim);

        animatorList.add(createAlphaAnim(mHandView, 1.0f, 0.0f, 400l, SaraUtils.isDeltaStartDelay() ? 600l : 2600l));

        final AnimatorSet animSet = new AnimatorSet();
        animSet.playTogether(animatorList);

        return animSet;
    }

    private Animator createListAndSettingInAnim() {
        final ArrayList<Animator> animatorList = new ArrayList<Animator>();

        ObjectAnimator listXAnim = ObjectAnimator.ofFloat(mListView, "translationX", getViewWidth(mListView), 0);
        listXAnim.setDuration(400l);
        listXAnim.setInterpolator(mInterpolator);
        animatorList.add(listXAnim);

        animatorList.add(createAlphaAnim(mBgShadowView, 0.0f, 1.0f, 400l));

        final AnimatorSet animSet = new AnimatorSet();
        animSet.playTogether(animatorList);
        animSet.setStartDelay(400l);
        return animSet;
    }

    private Animator createListAndSettingOutAnim() {
        final ArrayList<Animator> animatorList = new ArrayList<Animator>();

        ObjectAnimator listXAnim = ObjectAnimator.ofFloat(mListView, "translationX", 0, getViewWidth(mListView));
        listXAnim.setDuration(400l);
        listXAnim.setInterpolator(mInterpolator);
        animatorList.add(listXAnim);

        animatorList.add(createAlphaAnim(mBgShadowView, 1.0f, 0.0f, 400l));

        final AnimatorSet animSet = new AnimatorSet();
        animSet.playTogether(animatorList);
        animSet.setStartDelay(2600l);
        return animSet;
    }

    private Animator createCircleAnim() {
        final ArrayList<Animator> animatorList = new ArrayList<Animator>();
        animatorList.add(createAlphaAnim(mCircleView, 0.0f, 1.0f, 200l, 2400l));
        animatorList.add(createScaleAnim(mCircleView, 0.2f, 1.0f, 200l, SaraUtils.isDeltaStartDelay() ? 0 : 2400));

        animatorList.add(createAlphaAnim(mCircleView, 1.0f, 0.0f, 200l, SaraUtils.isDeltaStartDelay() ? 200l : 2600l));
        animatorList.add(createScaleAnim(mCircleView, 1.0f, 0.2f, 200l, SaraUtils.isDeltaStartDelay() ? 200l : 2600l));
        final AnimatorSet animSet = new AnimatorSet();
        animSet.playTogether(animatorList);
        return animSet;
    }

    private Animator createScaleAnim(View targetView, float from, float to, long duration, long delay) {
        final ArrayList<Animator> animatorList = new ArrayList<Animator>();
        ObjectAnimator scaleXAnim = ObjectAnimator.ofFloat(targetView, "scaleX", from, to);
        scaleXAnim.setDuration(duration);
        scaleXAnim.setInterpolator(mInterpolator);
        animatorList.add(scaleXAnim);

        ObjectAnimator scaleYAnim = ObjectAnimator.ofFloat(targetView, "scaleY", from, to);
        scaleYAnim.setDuration(duration);
        scaleYAnim.setInterpolator(mInterpolator);
        animatorList.add(scaleYAnim);
        final AnimatorSet animSet = new AnimatorSet();
        animSet.playTogether(animatorList);
        animSet.setStartDelay(delay);
        return animSet;
    }

    private Animator createAlphaAnim(View targetView, float from, float to, long duration) {
        return createAlphaAnim(targetView, from, to, duration, 0);
    }

    private Animator createAlphaAnim(View targetView, float from, float to, long duration,
            long delay) {
        ObjectAnimator alphaAnim = ObjectAnimator.ofFloat(targetView, "alpha", from, to);
        alphaAnim.setDuration(duration);
        alphaAnim.setStartDelay(delay);
        alphaAnim.setInterpolator(mInterpolator);
        return alphaAnim;
    }

    private int getViewWidth(View view) {
        int width = view.getWidth();
        if (width == 0) {
            int measureSpec = View.MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);
            view.measure(measureSpec, measureSpec);
            width = view.getMeasuredWidth();
        }
        return width;
    }
}
