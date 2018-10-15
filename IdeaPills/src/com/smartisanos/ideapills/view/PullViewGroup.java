package com.smartisanos.ideapills.view;

import android.animation.ValueAnimator;
import android.content.Context;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.DragEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.smartisanos.ideapills.BubbleController;
import com.smartisanos.ideapills.R;
import com.smartisanos.ideapills.common.anim.Anim;
import com.smartisanos.ideapills.common.anim.AnimCancelableListener;
import com.smartisanos.ideapills.common.anim.AnimListener;
import com.smartisanos.ideapills.common.anim.AnimTimeLine;
import com.smartisanos.ideapills.common.anim.SimpleAnimListener;
import com.smartisanos.ideapills.common.anim.Vector3f;
import com.smartisanos.ideapills.util.LOG;
import com.smartisanos.ideapills.util.StatusManager;
import com.smartisanos.ideapills.util.Tracker;
import com.smartisanos.ideapills.util.Utils;
import com.smartisanos.ideapills.util.ViewUtils;
import com.smartisanos.ideapills.common.util.UIHandler;

import smartisanos.util.SidebarUtils;


public class PullViewGroup extends FrameLayout {
    private static LOG log = LOG.getInstance(PullViewGroup.class);

    private int mMarginTopOrg = 0;
    private int mStableWidth;
    private int mWidth;
    private int mWidthDetail;
    ViewGroup mllHeadExpand;
    ViewGroup mll_headexpand_detail;
    private Anim mDelayAnim = null;

    private float mTranslateYinHideState = 0;
    private BubbleListView mListView = null;
    private Anim mAnimShow = null;
    private AnimTimeLine mAnimHideLine = null;
    private ImageView mIvFiltrate;
    private ImageView mIvTodoOver;
    private View mTodoOverDivider;
    private int mStableTopMargin;

    private ImageView mIvShare;
    private View mIvShareDivider;
    private ImageView mIvHeadHide;
    private View mIvHeadHideDivider;

    private Runnable mDelayShowAction = new Runnable() {
        public void run() {
            if (mListView != null) {
                mListView.playShowAnimationAfterPull(null);
                Tracker.onEvent("A420019","type",2);
            }
        }
    };

    public PullViewGroup(Context context) {
        this(context, null);
    }

    public PullViewGroup(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public PullViewGroup(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mllHeadExpand = (ViewGroup) findViewById(R.id.ll_headexpand);
        mll_headexpand_detail = (ViewGroup) findViewById(R.id.ll_headexpand_detail);
        getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            public void onGlobalLayout() {
                getViewTreeObserver().removeOnGlobalLayoutListener(this);
                LayoutParams lp = (LayoutParams) getLayoutParams();
                lp.topMargin = mMarginTopOrg + mListView.getHeadSpaceHeight();
                mStableTopMargin = lp.topMargin;
                lp.rightMargin = getStableWidth() - getFullWidth();
                setLayoutParams(lp);
            }
        });
        //todo : hard code
        mIvTodoOver = (ImageView) mllHeadExpand.findViewById(R.id.iv_todo_over);
        mTodoOverDivider = mllHeadExpand.findViewById(R.id.todo_over_divider);
        mIvShare = (ImageView) mll_headexpand_detail.findViewById(R.id.iv_bubble_share);
        mIvShareDivider = mll_headexpand_detail.findViewById(R.id.iv_bubble_share_divider);
        mIvHeadHide = (ImageView) mllHeadExpand.findViewById(R.id.iv_head_hide);
        mIvHeadHideDivider = mllHeadExpand.findViewById(R.id.iv_hide_divider);

        if (BubbleController.getInstance().isInPptContext(getContext())) {
            setBackgroundResource(R.drawable.ppt_popup_setting_bg);
            mllHeadExpand.setPadding(mllHeadExpand.getPaddingLeft(), 0, mllHeadExpand.getPaddingLeft(), 0);
            mIvShare.setVisibility(View.GONE);
            mIvShareDivider.setVisibility(View.GONE);
            mIvHeadHide.setVisibility(View.GONE);
            mIvHeadHideDivider.setVisibility(View.GONE);
        }
        mStableWidth = getResources().getDimensionPixelOffset(R.dimen.bubble_slidebar_stable_width);
        mWidth = ViewUtils.getSupposeWidthNoFixWidth(mllHeadExpand);
        mWidthDetail = ViewUtils.getSupposeWidthNoFixWidth(mll_headexpand_detail);
        mMarginTopOrg = Utils.getHeadMarginTop(mContext);
        setPadding(0, 0, 0, 0);
        mIvFiltrate = (ImageView) mllHeadExpand.findViewById(R.id.iv_head_filtrate);
        mIvTodoOver = (ImageView) mllHeadExpand.findViewById(R.id.iv_todo_over);
        mTodoOverDivider = mllHeadExpand.findViewById(R.id.todo_over_divider);
    }

    public void updateTodoOverVisible(int visible, int width) {
        mIvTodoOver.setVisibility(visible);
        mTodoOverDivider.setVisibility(visible);
        mWidth = width;
        LayoutParams lpExpand = (LayoutParams) mllHeadExpand.getLayoutParams();
        lpExpand.width = width;
        updateLayoutWidth();
    }

    public void updateTodoOverStatus(int drawableId) {
        mIvTodoOver.setImageResource(drawableId);
    }

    private void updateLayoutWidth() {
        LayoutParams plLp = (LayoutParams) getLayoutParams();
        if (plLp != null) {
            if (plLp.width != ViewGroup.LayoutParams.WRAP_CONTENT) {
                plLp.width = getFullWidth();
            }
            plLp.rightMargin = getStableWidth() - getFullWidth();
        }
        requestLayout();
    }

    public void updateFiltrateStatus(int resId) {
        mIvFiltrate.setImageResource(resId);
    }

    public void setTranslateYinHideState(float y) {
        mTranslateYinHideState = y;
    }

    public void setListView(BubbleListView bubbleListView) {
        mListView = bubbleListView;
    }

    public int getStableWidth() {
        return mStableWidth;
    }

    public void setVisibleGone() {
        setVisibility(GONE);
        setAnimation(null);
        updateLayoutWidth();
        setTranslationX(0);
        setTranslationY(0);
    }

    public void clearStatusThenToTransparent() {
        clearAllStatus();
        setAlpha(0);
    }

    private void clearAnimShowHide() {
        if (mAnimShow != null) {
            mAnimShow.cancel();
        }
        if (mAnimHideLine != null) {
            mAnimHideLine.cancel();
        }
    }

    public boolean hasAnimRuning() {
        if ((mAnimShow != null && mAnimShow.isRunning()) || (mAnimHideLine != null && mAnimHideLine.isRunning())) {
            return true;
        }
        return false;
    }

    public void checkToSwitch() {
        if (mListView.checkShowHeadDetail()) {
            mll_headexpand_detail.setVisibility(VISIBLE);
            mllHeadExpand.setVisibility(GONE);
        } else {
            mll_headexpand_detail.setVisibility(GONE);
            mllHeadExpand.setVisibility(VISIBLE);
        }
    }

    public void moveTo(final AnimListener listener) {
        if (isLayoutRequested() && getVisibility() != GONE || getWidth() <= 0) {
            getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                public void onGlobalLayout() {
                    getViewTreeObserver().removeOnGlobalLayoutListener(this);
                    moveToInternal(getFullWidth(), listener);
                }
            });
        } else {
            moveToInternal(getFullWidth(), listener);
        }
    }

    private void moveToInternal(final int headNormalWidth, final AnimListener listener) {
        clearAnimShowHide();
        final Vector3f from = new Vector3f(getTranslationX(), 0);
        Vector3f to = new Vector3f(getStableWidth() - headNormalWidth, 0);
        mAnimShow = new Anim(this, Anim.TRANSLATE, 250, Anim.CUBIC_OUT, from, to, true);
        final ValueAnimator animator = ValueAnimator.ofFloat(from.getX(), to.getX());
        final float targetTranX = getStableWidth() - getFullWidth();
        mAnimShow.setListener(new SimpleAnimListener() {
            public void onStart() {
                animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                    public void onAnimationUpdate(ValueAnimator animation) {
                        if (mListView != null && mListView.getVisibility() == View.GONE) {
                            return;
                        }
                        Float value = (Float) animation.getAnimatedValue();
                        BubbleController.getInstance().dimBackgroundByMove(value / targetTranX);
                    }
                });
                animator.start();
            }

            public void onComplete(int type) {
                animator.cancel();
                if (listener != null) {
                    listener.onComplete(type);
                }
            }
        });

        if (!mAnimShow.start() && listener != null) {
            listener.onComplete(0);
        }
    }

    public void showStableState(int fromTopMargin, int toTopMargin, boolean withAnim) {
        if (LOG.DBG) {
            LOG.d("showStableState(" + fromTopMargin + "," + toTopMargin + ")...");
        }
        clearAnimShowHide();
        toTopMargin += mTranslateYinHideState;
        int translation = fromTopMargin - toTopMargin;
        LayoutParams lp = (LayoutParams) getLayoutParams();
        lp.topMargin = toTopMargin + mMarginTopOrg;
        lp.rightMargin = getStableWidth() - getFullWidth();
        lp.width = getFullWidth();
        setLayoutParams(lp);
        setVisibility(VISIBLE);
        mAnimHideLine = new AnimTimeLine();
        if (withAnim) {
            Anim anim = new Anim(this, Anim.TRANSLATE, 100, Anim.CUBIC_OUT, new Vector3f(60, 0), Anim.ZERO);
            mAnimHideLine.addAnim(anim);
            if (translation != 0) {
                Anim animtran = new Anim(this, Anim.TRANSLATE, Math.abs(translation / 3), Anim.CUBIC_OUT, new Vector3f(0, translation), new Vector3f(0, 0));
                mAnimHideLine.addAnim(animtran);
            }
        } else {
            setTranslationX(0);
            setTranslationY(0);
        }
        AnimListener listener = new SimpleAnimListener() {
            public void onStart() {

            }

            public void onComplete(int type) {
                checkToSwitch();
                delayToTransparent();
            }
        };
        mAnimHideLine.setAnimListener(listener);
        if (mAnimHideLine.isEmpty() || !mAnimHideLine.start()) {
            listener.onComplete(0);
        }
    }

    public void resetTopMargin() {
        LayoutParams lp = (LayoutParams) getLayoutParams();
        lp.topMargin = mMarginTopOrg;
        setLayoutParams(lp);
        mTranslateYinHideState = 0;
    }

    public int getListRelateTopMargin() {
        LayoutParams lp = (LayoutParams) getLayoutParams();
        if (lp.topMargin < 0 || lp.topMargin == mStableTopMargin
                || mStableTopMargin == 0) {
            return 0;
        }
        return Math.max(lp.topMargin - mStableTopMargin, 0);
    }

    public void dragPullViewVertical(float moveY) {
        LayoutParams lp = (LayoutParams) getLayoutParams();
        lp.topMargin += moveY;
        lp.topMargin = Math.min(mListView.getHeight() - mListView.getNormalHeight() + mMarginTopOrg, Math.max(mMarginTopOrg, lp.topMargin));
        setLayoutParams(lp);
    }

    public int getPullTop() {
        return getTop() - mMarginTopOrg;
    }

    public int getFullWidth() {
        if (mListView != null && mListView.checkShowHeadDetail()) {
            return mWidthDetail;
        } else {
            return mWidth;
        }
    }

    public float getLimitTranX(float tranx) {
        if (tranx < getStableWidth() - getFullWidth()) {
            tranx = getStableWidth() - getFullWidth();
        }
        if (tranx > 0) {
            tranx = 0;
        }
        return tranx;
    }

    @Override
    public boolean onDragEvent(DragEvent event) {
        switch (event.getAction()) {
            case DragEvent.ACTION_DRAG_STARTED:
                boolean accept = (event != null && event.getClipDescription() != null && event.getClipDescription().getMimeTypeCount() > 0);
                if (accept) {
                    StatusManager.setStatus(StatusManager.GLOBAL_DRAGGING, true);
                    if (event.getClipDescription().getExtras() == null
                            || !TextUtils.equals(event.getClipDescription().getExtras().getString(SidebarUtils.GLOBAL_DRAG_TYPE),
                            (SidebarUtils.FORCE_TOUCH_SECTOR_MENU_DRAG))) {
                        clearTransparent();
                    }
                }
                return accept;
            case DragEvent.ACTION_DRAG_ENTERED:
                UIHandler.removeCallbacks(mDelayShowAction);
                UIHandler.postDelayed(mDelayShowAction, 400);
                break;
            case DragEvent.ACTION_DRAG_LOCATION:
                break;
            case DragEvent.ACTION_DRAG_ENDED:
                delayToTransparent();
                break;
            default:
                UIHandler.removeCallbacks(mDelayShowAction);
                break;
        }
        return super.onDragEvent(event);
    }

    public void clearAllStatus() {
        if (mDelayAnim != null) {
            mDelayAnim.cancel();
            mDelayAnim = null;
        }
        clearAnimShowHide();
        setAnimation(null);
        setAlpha(1.0f);
        updateLayoutWidth();
        setTranslationX(0);
        setTranslationY(0);
    }

    public void clearTransparent() {
        if (mDelayAnim != null) {
            mDelayAnim.cancel();
            mDelayAnim = null;
        }
        if (!BubbleController.getInstance().isWindowTouchEnable()) {
            BubbleController.getInstance().setWindowTouchEnable(true);
            BubbleController.getInstance().setBubbleWindowEnable(true);
        }
        setAlpha(1.0f);
    }

    public void delayToTransparent() {
        if (mListView.getVisibility() != GONE) {
            return;
        }
        if (!BubbleController.getInstance().isBubbleWindowEnable()) {
            BubbleController.getInstance().setWindowTouchEnable(false);
            return;
        }
        log.info("delayToTransparent");
        clearTransparent();
        if (!StatusManager.getStatus(StatusManager.GLOBAL_DRAGGING)) {
            mDelayAnim = new Anim(this, Anim.TRANSPARENT, 500, Anim.CUBIC_OUT, Anim.VISIBLE, new Vector3f(0, 0, 0.3f));
            mDelayAnim.setDelay(3500);
            mDelayAnim.setListener(new AnimCancelableListener() {
                public void onAnimCompleted() {
                    mDelayAnim = new Anim(PullViewGroup.this, Anim.TRANSPARENT, 500, Anim.CUBIC_OUT, new Vector3f(0, 0, getAlpha()), Anim.INVISIBLE);
                    mDelayAnim.setDelay(3500);
                    mDelayAnim.setListener(new AnimCancelableListener() {
                        public void onAnimCompleted() {
                            BubbleController.getInstance().setWindowTouchEnable(false);
                            BubbleController.getInstance().setBubbleWindowEnable(false);
                        }
                    });
                    mDelayAnim.start();
                }
            });
            mDelayAnim.start();
        }
    }
}
