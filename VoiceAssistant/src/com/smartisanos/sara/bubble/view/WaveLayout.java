package com.smartisanos.sara.bubble.view;

import android.content.Context;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewStub;
import android.view.animation.AnimationSet;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;

import com.smartisanos.sara.R;
import com.smartisanos.sara.bubble.manager.BubbleThemeManager;
import com.smartisanos.sara.util.AnimManager;
import com.smartisanos.sara.util.LogUtils;
import com.smartisanos.sara.util.SaraUtils;
import com.smartisanos.sara.widget.WaveView;

public class WaveLayout implements IWaveLayout {

    public static final int UI_MODE_NONE = 0;
    public static final int UI_MODE_LEFT = 1;
    public static final int UI_MODE_CENTER = 2;

    private View mParentContainer;
    private ViewStub mWaveStub;
    private ViewStub mWaveLeftStub;

    private View mWaveLayout;
    private View mWaveLayoutLeft;
    private View mTextPopup;
    private WaveView mWaveForm;
    private View mPopupBottomArrow;

    private int mWaveResultMarginLeft;
    private int mWaveResultMarginBottom;

    private int mUiMode = UI_MODE_NONE;
    private Context mContext;
    private WaveView.AnimationListener mWaveAnimListener;

    public WaveLayout(Context context, ViewStub waveStub, ViewStub waveLeftStub,
                      WaveView.AnimationListener waveAnimListener) {
        mContext = context;
        mWaveStub = waveStub;
        mWaveLeftStub = waveLeftStub;
        if (mWaveStub != null) {
            mParentContainer = (View) mWaveStub.getParent();
        } else {
            mParentContainer = (View) mWaveLeftStub.getParent();
        }
        mWaveAnimListener = waveAnimListener;
        mWaveResultMarginLeft = mContext.getResources().getDimensionPixelSize(R.dimen.wave_result_margin_left);
    }

    public WaveLayout(Context context, View parentContainer, WaveView.AnimationListener waveAnimListener) {
        mContext = context;
        mParentContainer = parentContainer;
        mWaveAnimListener = waveAnimListener;
        mWaveResultMarginLeft = mContext.getResources().getDimensionPixelSize(R.dimen.wave_result_margin_left);
    }

    @Override
    public void init(boolean isLeftMode) {
        init(isLeftMode ? UI_MODE_LEFT : UI_MODE_CENTER);
    }

    private void init(int newUiMode) {
        mUiMode = newUiMode;
        if (mUiMode == UI_MODE_LEFT) {
            initWaveLeft();
        } else {
            initWave();
        }
        if (mUiMode == UI_MODE_LEFT) {
            mWaveResultMarginBottom = SaraUtils.getLeftBubbleBottom(mContext);
        } else {
            if (SaraUtils.isNavigationBarMode(mContext)) {
                mWaveResultMarginBottom = mContext.getResources().getDimensionPixelSize(R.dimen.navigation_bar_wave_result_margin_bottom);
            } else {
                mWaveResultMarginBottom = mContext.getResources().getDimensionPixelSize(R.dimen.wave_result_margin_bottom);
            }
        }
        mWaveForm.updateWidthPrama(mUiMode == UI_MODE_LEFT, SaraUtils.isBlindMode());
    }

    @Override
    public void changeUiMode(boolean isLeftMode) {
        int newUiMode = isLeftMode ? UI_MODE_LEFT : UI_MODE_CENTER;
        if (newUiMode == mUiMode) {
            return;
        }
        if (newUiMode == UI_MODE_LEFT) {
            if (mWaveLayout != null) mWaveLayout.setVisibility(View.GONE);
            if (mWaveLayoutLeft != null) mWaveLayoutLeft.setVisibility(View.VISIBLE);
        } else if (newUiMode == UI_MODE_CENTER) {
            if (mWaveLayout != null) mWaveLayout.setVisibility(View.VISIBLE);
            if (mWaveLayoutLeft != null) mWaveLayoutLeft.setVisibility(View.GONE);
        }
        init(newUiMode);
    }

    @Override
    public void show(int bubbleColor) {
        View waveLayout = getWaveLayoutView();
        if (waveLayout != null) {
            waveLayout.setVisibility(View.VISIBLE);
            if (mUiMode == UI_MODE_LEFT && mWaveLayoutLeft != null) {
                startWaveLayoutAnim(0);
            }
            if (mPopupBottomArrow != null) {
                mPopupBottomArrow.setBackgroundResource(BubbleThemeManager.getBackgroudRes(bubbleColor, BubbleThemeManager.BACKGROUND_BUBBLE_ARROW));
            }
            mTextPopup.setBackgroundResource(BubbleThemeManager.getBackgroudRes(bubbleColor, BubbleThemeManager.BACKGROUND_BUBBLE_NORMAL));
            waveLayout.setTranslationY(0);
            FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT);
            if (mUiMode == UI_MODE_LEFT) {
                params.gravity = Gravity.LEFT | Gravity.BOTTOM;
                params.leftMargin = mWaveResultMarginLeft;
            } else {
                params.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
            }
            params.bottomMargin = mWaveResultMarginBottom;
            waveLayout.setLayoutParams(params);
            mWaveForm.reset();
            mWaveForm.setAlpha(1);
            waveLayout.setAlpha(1);
            if (mPopupBottomArrow != null) {
                mPopupBottomArrow.setAlpha(1);
                mPopupBottomArrow.setVisibility(View.VISIBLE);
            }
            mTextPopup.setAlpha(1);
            mTextPopup.setVisibility(View.VISIBLE);
            mWaveForm.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void setContentWidth(int width) {
        if (getWaveLayoutView() != null) {
            FrameLayout.LayoutParams waveParams = (FrameLayout.LayoutParams) mWaveForm.getLayoutParams();
            if (waveParams.width != width) {
                waveParams.width = width;
                mWaveForm.setLayoutParams(waveParams);
            }
            FrameLayout.LayoutParams textPopupParams = (FrameLayout.LayoutParams) mTextPopup.getLayoutParams();
            if (textPopupParams.width != width) {
                textPopupParams.width = width;
                mTextPopup.setLayoutParams(textPopupParams);
            }
        }
    }

    @Override
    public void waveChanged(byte[] waveFormData, int pointNum) {
        mWaveForm.waveChanged(waveFormData, pointNum);
    }

    @Override
    public void waveMax(int bubbleColor) {
        if (mUiMode == UI_MODE_LEFT) {
            if (mPopupBottomArrow != null) {
                mPopupBottomArrow.setBackgroundResource(BubbleThemeManager.getBackgroudRes(bubbleColor, BubbleThemeManager.BACKGROUND_BUBBLE_ARROW_lARGE));
            }
        }
    }

    @Override
    public void stopWaveAnim(boolean isCancelWithoutCallback) {
        mWaveForm.stopAnimation(isCancelWithoutCallback);
    }

    @Override
    public boolean clearAnim() {
        boolean isClearAnimation = false;
        View waveLayout = getWaveLayoutView();
        if (waveLayout != null) {
            if (waveLayout.getAnimation() != null && !waveLayout.getAnimation().hasEnded()) {
                isClearAnimation = true;
            }
            waveLayout.clearAnimation();
            if (mWaveForm.getAnimation() != null && !mWaveForm.getAnimation().hasEnded()) {
                isClearAnimation = true;
            }
            mWaveForm.clearAnimation();
            mWaveForm.stopAnimation(true);
        }
        return isClearAnimation;
    }

    @Override
    public void hide() {
        View waveLayout = getWaveLayoutView();
        if (waveLayout != null) {
            if (waveLayout.getVisibility() != View.GONE) {
                clearAnim();
                waveLayout.setVisibility(View.GONE);
            }
            mTextPopup.setVisibility(View.GONE);
            mWaveForm.setVisibility(View.GONE);
            if (mPopupBottomArrow != null) {
                mPopupBottomArrow.setVisibility(View.GONE);
            }
        }
    }

    @Override
    public void hideTextPopup(boolean withAnimation) {
        if (withAnimation) {
            if (mTextPopup.getVisibility() != View.GONE) {
                AnimManager.HideViewWithAlphaAnim(mTextPopup, AnimManager.HIDE_BUBBLE_TEXT_POPUP_DURATION, 0);
                mTextPopup.setVisibility(View.GONE);
            }
        } else {
            mTextPopup.setVisibility(View.GONE);
        }
    }

    @Override
    public boolean checkFinish(MotionEvent ev) {
        return SaraUtils.checkFinish(getWaveLayoutView(), ev);
    }

    @Override
    public int getWaveLayoutHeight() {
        return getWaveLayoutView().getHeight();
    }

    private void initWave() {
        if (mWaveStub != null && mWaveStub.getParent() != null) {
            mWaveLayout = mWaveStub.inflate();
        } else {
            mWaveLayout = mParentContainer.findViewById(R.id.wave);
        }
        initWaveChild(mWaveLayout);
    }

    private void initWaveLeft() {
        if (mWaveLeftStub != null && mWaveLeftStub.getParent() != null) {
            mWaveLayoutLeft = mWaveLeftStub.inflate();
        } else {
            mWaveLayoutLeft = mParentContainer.findViewById(R.id.wave_left);
        }
        initWaveChild(mWaveLayoutLeft);
    }

    private void initWaveChild(View waveLayout) {
        mTextPopup = waveLayout.findViewById(R.id.text_popup);
        mWaveForm = (WaveView) waveLayout.findViewById(R.id.waveform);
        mWaveForm.setWaveType(SaraUtils.WaveType.START_WAVE);
        mWaveForm.setAnimationListener(mWaveAnimListener);
        mPopupBottomArrow = waveLayout.findViewById(R.id.popup_bottom_arrow);
    }

    private View getWaveLayoutView() {
        if (mUiMode == UI_MODE_LEFT) {
            if (mWaveLayoutLeft == null) {
                LogUtils.e("view mustn't be null");
                initWaveLeft();
            }
            return mWaveLayoutLeft;
        } else {
            if (mWaveLayout == null) {
                LogUtils.e("view mustn't be null");
                initWave();
            }
            return mWaveLayout;
        }
    }

    private void startWaveLayoutAnim(long delay) {
        AnimationSet shrink = (AnimationSet) AnimationUtils.loadAnimation(mContext, R.anim.bubble_left);
        shrink.setStartOffset(delay);
        if (mWaveLayoutLeft != null) mWaveLayoutLeft.startAnimation(shrink);
    }
}


