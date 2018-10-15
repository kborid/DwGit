package com.smartisanos.sara.bubble.view;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.service.onestep.GlobalBubble;
import android.service.onestep.GlobalBubbleAttach;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewStub;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.CheckBox;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.smartisanos.ideapills.common.anim.Anim;
import com.smartisanos.ideapills.common.anim.AnimInterpolator;
import com.smartisanos.sara.R;
import com.smartisanos.sara.bubble.manager.BubbleThemeManager;
import com.smartisanos.sara.util.SaraUtils;

import java.util.List;

public class FakeBubbleView implements IFakeBubbleView {

    private View mParentContainer;
    private ViewStub mFakeBubbleStub;

    private RelativeLayout mFakBubbleView;
    private ImageView mFakTodo;
    private CheckBox mFakeCheck;
    private ImageView mLittleNotify;
    private LinearLayout mFakBackgroud;
    private TextView mFakBubbleText;
    private ImageView mFakPlay;

    private int mDisplayWidth;
    private final int mTranslateAnimDuration;

    private Context mContext;

    public FakeBubbleView(Context context, ViewStub fakeBubbleStub) {
        this(context, fakeBubbleStub, 150);
    }

    public FakeBubbleView(Context context, ViewStub fakeBubbleStub, int translateAnimDuration) {
        mContext = context;
        mFakeBubbleStub = fakeBubbleStub;
        mParentContainer = (View) mFakeBubbleStub.getParent();
        mTranslateAnimDuration = translateAnimDuration;
    }

    @Override
    public void initFakeAnim(GlobalBubble globalBubble, List<GlobalBubbleAttach> attachmentList, int displayWidth) {
        initFakBubble();
        mDisplayWidth = displayWidth;
        if (globalBubble == null) {
            return;
        }
        LinearLayout.LayoutParams textParam = (LinearLayout.LayoutParams) mFakBubbleText.getLayoutParams();
        int color = globalBubble.getColor();
        if (color == GlobalBubble.COLOR_SHARE) {
            mFakBackgroud.setBackgroundResource(R.drawable.text_popup_share);
            textParam.leftMargin = mContext.getResources().getDimensionPixelSize(R.dimen.text_right_margin_share);
            mFakBubbleText.setLayoutParams(textParam);
            mLittleNotify.setImageResource(R.drawable.little_remind_icon_share);
        } else {
            mFakBackgroud.setBackgroundResource(BubbleThemeManager.getBackgroudRes(color, BubbleThemeManager.BACKGROUND_BUBBLE_NORMAL));
            textParam.leftMargin = mContext.getResources().getDimensionPixelSize(R.dimen.text_right_full_margin_share);
            mFakBubbleText.setLayoutParams(textParam);
            mLittleNotify.setImageResource(R.drawable.little_remind_icon);
        }
        mFakBubbleText.setTextColor(mContext.getResources().getColor(BubbleThemeManager.getBackgroudRes(color, BubbleThemeManager.BACKGROUND_BUBBLE_TEXT_COLOR)));
        mFakPlay.setImageResource(BubbleThemeManager.getBackgroudRes(color, BubbleThemeManager.BACKGROUND_BUBBLE_PLAY_ICON));
        if (globalBubble.getType() == GlobalBubble.TYPE_TEXT) {
            mFakPlay.setVisibility(View.GONE);
        }

        mLittleNotify.setVisibility(globalBubble.getDueDate() > 0 ? View.VISIBLE : View.GONE);
        mFakTodo.setVisibility(View.GONE);
        mFakBubbleText.setVisibility(View.VISIBLE);

        boolean isChecked = globalBubble.getToDo() == GlobalBubble.TODO_OVER;
        SaraUtils.toDoOver(mFakBubbleText, isChecked);
        mFakeCheck.setChecked(isChecked);
        if (TextUtils.isEmpty(globalBubble.getText()) && attachmentList != null && attachmentList.size() > 0) {
            mFakBubbleText.setText(String.format(mContext.getResources().getQuantityString(
                    R.plurals.bubble_count_attach, attachmentList.size()),
                    attachmentList.size()));
            mFakBubbleText.setAlpha(0.4f);
        } else {
            mFakBubbleText.setText(globalBubble.getText());
            mFakBubbleText.setAlpha(1.0f);
        }

        mFakBubbleView.measure(View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
        mFakBubbleView.setAlpha(0);
        mFakBubbleView.setVisibility(View.VISIBLE);
    }

    @Override
    public void setFakeAnimOffset(int left, int top, int w, int h) {
        FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) mFakBubbleView.getLayoutParams();
        params.leftMargin = left + (w - mFakBubbleView.getMeasuredWidth()) / 2;
        params.topMargin = top + (h - mFakBubbleView.getMeasuredHeight()) / 2;
        mFakBubbleView.setLayoutParams(params);
    }

    @Override
    public int getFakeBubbleBgWidth() {
        return mFakBackgroud.getMeasuredWidth();
    }

    @Override
    public int getFakeBubbleBgHeight() {
        return mFakBackgroud.getMeasuredHeight();
    }

    @Override
    public int getFakeAnimTargetX() {
        int visibleTodoWidth = mFakTodo.getVisibility() == View.VISIBLE ? (mFakTodo
                .getMeasuredWidth() + ((RelativeLayout.LayoutParams) mFakTodo.getLayoutParams()).rightMargin) / 2 : 0;
        int targetX = (mDisplayWidth - mFakBackgroud.getMeasuredWidth()) / 2 + visibleTodoWidth;
        return targetX;
    }

    @Override
    public int getFakeAnimTargetY(int endBoxHeight) {
        return (endBoxHeight - mFakBubbleView.getMeasuredHeight()) / 2;
    }

    @Override
    public void startFakeAnim(int translationY, final Animator.AnimatorListener animatorListener) {
        mFakBubbleView.setTranslationY(translationY);
        showFakBubbleViewWitAnim(animatorListener);
    }

    private void initFakBubble() {
        if (mFakBubbleView == null) {
            if (mFakeBubbleStub != null && mFakeBubbleStub.getParent() != null) {
                mFakBubbleView = (RelativeLayout) mFakeBubbleStub.inflate();
            } else {
                mFakBubbleView = (RelativeLayout) mParentContainer.findViewById(R.id.fak_bubble);
            }
            mFakTodo = (ImageView) mParentContainer.findViewById(R.id.fak_todo);
            mFakBackgroud = (LinearLayout) mParentContainer.findViewById(R.id.fak_llbackground);
            mFakBubbleText = (TextView) mParentContainer.findViewById(R.id.fak_bubble_text);
            mFakPlay = (ImageView) mParentContainer.findViewById(R.id.fak_bubble_play);
            mFakeCheck = (CheckBox) mParentContainer.findViewById(R.id.fake_todo);
            mLittleNotify = (ImageView) mParentContainer.findViewById(R.id.v_little_notify);
        }
    }

    private void showFakBubbleViewWitAnim(final Animator.AnimatorListener animatorListener) {
        AnimatorSet set = new AnimatorSet();
        ObjectAnimator fakBubbleAlphaAnimator = ObjectAnimator.ofFloat(mFakBubbleView, "alpha", 0f, 1f);
        fakBubbleAlphaAnimator.setStartDelay(100);
        fakBubbleAlphaAnimator.setDuration(100);
        fakBubbleAlphaAnimator.setInterpolator(new DecelerateInterpolator(1.5f));
        ObjectAnimator translateAnimator = null;
        translateAnimator = ObjectAnimator.ofFloat(mFakBubbleView, "translationX", 0, mFakBubbleView.getMeasuredWidth());
        translateAnimator.setStartDelay(300);
        translateAnimator.setDuration(mTranslateAnimDuration);
        translateAnimator.setInterpolator(new AccelerateInterpolator(1.0f));
        set.playTogether(fakBubbleAlphaAnimator, translateAnimator);
        set.addListener(new Animator.AnimatorListener() {

            @Override
            public void onAnimationStart(Animator animation) {
                if (animatorListener != null) {
                    animatorListener.onAnimationStart(animation);
                }
            }

            @Override
            public void onAnimationRepeat(Animator animation) {
                if (animatorListener != null) {
                    animatorListener.onAnimationRepeat(animation);
                }
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                mFakBubbleView.setVisibility(View.GONE);
                mFakBubbleView.setAlpha(0);
                mFakBubbleView.setTranslationX(0);
                mFakBubbleView.setTranslationY(0);
                if (animatorListener != null) {
                    animatorListener.onAnimationEnd(animation);
                }
            }

            @Override
            public void onAnimationCancel(Animator animation) {
                mFakBubbleView.setVisibility(View.GONE);
                mFakBubbleView.setAlpha(0);
                mFakBubbleView.setTranslationX(0);
                mFakBubbleView.setTranslationY(0);
                if (animatorListener != null) {
                    animatorListener.onAnimationCancel(animation);
                }
            }
        });
        set.start();
    }

    @Override
    public void startFakeBubbleSendFlyAnim(final int translationY, final int[] bubbleLoc, final int[] selectLoc) {
        mFakBubbleView.setTranslationY(translationY);
        mFakBubbleView.setAlpha(1f);
        ValueAnimator xAnim = ValueAnimator.ofInt(bubbleLoc[0], selectLoc[0]);
        ValueAnimator yAnim = ValueAnimator.ofInt(bubbleLoc[1], selectLoc[1]);

        xAnim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                Integer value = (Integer) animation.getAnimatedValue();
                mFakBubbleView.setTranslationX(value - bubbleLoc[0]);
                mFakBubbleView.setScaleX((float) 0.9 * (1.0f - (value - bubbleLoc[0]) * 1.0f / (selectLoc[0] - bubbleLoc[0])));
            }
        });
        yAnim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                Integer value = (Integer) animation.getAnimatedValue();
                mFakBubbleView.setTranslationY(translationY + (value - bubbleLoc[1]));
                mFakBubbleView.setScaleY((float) 0.9 * (1.0f - (value - bubbleLoc[1]) * 1.0f / (selectLoc[1] - bubbleLoc[1])));
            }
        });

        AnimatorSet set = new AnimatorSet();
        set.setDuration(300);
        set.setInterpolator(new AnimInterpolator.Interpolator(Anim.CUBIC_OUT));
        set.playTogether(xAnim, yAnim);
        set.start();
    }
}
