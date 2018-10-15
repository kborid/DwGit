package com.smartisanos.ideapills.view;

import android.animation.Animator;
import android.animation.Animator.AnimatorListener;
import android.animation.TypeEvaluator;
import android.animation.ValueAnimator;
import android.app.SmtPCUtils;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.service.onestep.GlobalBubble;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.smartisanos.ideapills.BubbleController;
import com.smartisanos.ideapills.Constants;
import com.smartisanos.ideapills.common.anim.SimpleAnimListener;
import com.smartisanos.ideapills.common.util.CommonUtils;
import com.smartisanos.ideapills.IdeaPillsApp;
import com.smartisanos.ideapills.R;
import com.smartisanos.ideapills.common.anim.Anim;
import com.smartisanos.ideapills.common.anim.AnimListener;
import com.smartisanos.ideapills.common.anim.AnimTimeLine;
import com.smartisanos.ideapills.common.anim.Vector3f;
import com.smartisanos.ideapills.data.DataHandler;
import com.smartisanos.ideapills.entity.BubbleItem;
import com.smartisanos.ideapills.sync.SyncBundleRepository;
import com.smartisanos.ideapills.sync.SyncManager;
import com.smartisanos.ideapills.sync.entity.SyncShareInvitation;
import com.smartisanos.ideapills.sync.share.GlobalInvitationAction;
import com.smartisanos.ideapills.sync.share.SyncShareManager;
import com.smartisanos.ideapills.sync.share.SyncShareRepository;
import com.smartisanos.ideapills.sync.share.SyncShareUtils;
import com.smartisanos.ideapills.util.BubbleTrackerID;
import com.smartisanos.ideapills.util.GlobalBubbleManager;
import com.smartisanos.ideapills.util.GlobalBubbleUtils;
import com.smartisanos.ideapills.util.LOG;
import com.smartisanos.ideapills.util.StatusManager;
import com.smartisanos.ideapills.util.Tracker;
import com.smartisanos.ideapills.util.Utils;
import com.smartisanos.ideapills.util.ViewUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import smartisanos.api.VibEffectSmt;
import smartisanos.api.ViewSmt;
import smartisanos.util.SidebarUtils;

import static com.smartisanos.ideapills.view.BubbleToDoCheckBox.BUBBLE_TODO_CHECKBOX;
import static com.smartisanos.ideapills.view.BubbleToDoCheckBox.BUBBLE_TODO_CHECKBOX_SHARE;

public class BubbleItemView extends RelativeLayout implements View.OnClickListener, CompoundButton.OnCheckedChangeListener,BubbleToDoCheckBox.OnCheckedChangeListener {
    private static final LOG log = LOG.getInstance(BubbleItemView.class);

    private static final ColorMatrixColorFilter sDarkerColorFilter = new ColorMatrixColorFilter(new float[]{0.7f,0,0,0,0, 0,0.7f,0,0,0, 0,0,0.7f,0,0, 0,0,0,1.0f,0});

    protected static final int DELETE_FLAG_NOTHING = 0 ;
    protected static final int DELETE_FLAG_NEED_CONFIRM = 0x01 ;
    // cal from two TextView marginTop diff and bg show content diff
    private static final int TOP_MARGIN_DIFF_WHEN_TRANSFORM_FROM_NORMAL_TO_LARGE = 4;

    private static final int ToLargeAnimDura = 250;
    private static final int ToDelAnimDura = 350;
    FrameLayout mFllayout = null;
    ImageView mIvBubbleBg = null;
    ImageView mIvBubbleFakeBg = null;
    ImageView mIvBubbleRightShadow = null;
    LinearLayout mllbackground = null;
    TextView mTextView = null;
    ImageView mAttachmentTag = null;
    CheckBox mCbCover = null;
    BubbleToDoCheckBox mCbToDo = null;
    ImageView mIvArrowImg = null;
    LinearLayout mLLColorChooser = null;
    BubbleItemDetailView mBubbleItemDetailView = null;
    private boolean mAbort = false;
    ImageView mVLittleNotify;

    private BubbleItem mBubbleItem = null;
    private int mMode;
    private ValueAnimator mValueAnimator = null;
    private int mEditExpandW;
    private TypeEvaluator<Point> mDelBubbleItemTypeEvaluator;
    private Anim mAnimTranslateY = null;
    private boolean needLargeCallback = false;
    private float mDownX, mDownY;
    private ViewTreeObserver.OnGlobalLayoutListener mOnGlobalLayoutListener;

    // for refresh when changed
    private int mLastBubbleItemColor = -1;
    private int mLastShareStatus = -1;
    private TypeEvaluator<Point> mToLargeTypeEvaluator = new TypeEvaluator<Point>() {
        private Point mPoint = new Point();

        public Point evaluate(float fraction, Point startValue, Point endValue) {
            int sx = startValue.x;
            int sy = startValue.y;
            int cx = (int) (sx + fraction * (endValue.x - sx));
            int cy = (int) (sy + fraction * (endValue.y - sy));
            mPoint.set(cx, cy);
            return mPoint;
        }
    };

    private AnimatorListener mToNormalListener = new AnimatorListener() {
        private boolean mCanceled = false;

        public void onAnimationStart(Animator animation) {
            mCanceled = false;
        }

        public void onAnimationEnd(Animator animation) {
            if (!mCanceled) {
                toNormal(mBubbleItem, false);
            }
        }

        public void onAnimationCancel(Animator animation) {
            mCanceled = true;
        }

        public void onAnimationRepeat(Animator animation) {

        }
    };

    public BubbleItemView(Context context) {
        this(context, null);
    }

    public BubbleItemView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public BubbleItemView(Context context, AttributeSet attrs,
                          int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public BubbleItemView(Context context, AttributeSet attrs,
                          int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    protected void onFinishInflate() {
        super.onFinishInflate();
        mIvBubbleBg = (ImageView) findViewById(R.id.iv_bubble_bg);
        mIvBubbleFakeBg = (ImageView) findViewById(R.id.iv_bubble_fake_bg);
        mIvBubbleRightShadow = (ImageView) findViewById(R.id.iv_bubble_right);
        mTextView = (TextView) findViewById(R.id.drag_item);
        mllbackground = (LinearLayout) findViewById(R.id.llbackground);
        mCbCover = (CheckBox) findViewById(R.id.cb_item_cover);
        mAttachmentTag = (ImageView) findViewById(R.id.v_speech_tag);
        mAttachmentTag.setOnClickListener(this);
        mVLittleNotify = (ImageView) findViewById(R.id.v_little_notify);
        mFllayout = (FrameLayout) findViewById(R.id.fl_layout);
        mFllayout.setOnClickListener(this);
        ViewSmt.getInstance().setOnForceTouchListener(mFllayout, new ViewSmt.OnForceTouchListener() {
            @Override
            public void onForceTouch() {
                if (mBubbleItem != null && !TextUtils.isEmpty(mBubbleItem.getText()) && isInLargeMode()) {
                    SidebarUtils.dragText(mFllayout, mContext, mBubbleItem.getText(), true);
                }
            }
        });
        mCbCover.setOnCheckedChangeListener(this);
        mIvArrowImg = (ImageView) findViewById(R.id.iv_arrow);
        mIvArrowImg.setOnClickListener(this);
        mEditExpandW = getResources().getDimensionPixelSize(R.dimen.bubble_item_edit_expand);
        mLLColorChooser = (LinearLayout) findViewById(R.id.ll_colorchooser);
        ViewUtils.justBindOnClickListener(mLLColorChooser, this,
                new int[]{R.id.iv_color_red, R.id.iv_color_orange, R.id.iv_color_green,
                        R.id.iv_color_blue, R.id.iv_color_purple, R.id.iv_color_share, R.id.iv_color_navy_blue});

        mCbToDo = (BubbleToDoCheckBox) findViewById(R.id.cb_todo);
        mCbToDo.setOnCheckedChangeListener(this);
        mCbToDo.setVisibility(View.GONE);
        removeView(mCbToDo);
        addView(mCbToDo, 1);

        mBubbleItemDetailView = (BubbleItemDetailView) findViewById(R.id.bubbledetailview);
        mBubbleItemDetailView.setBubbleItemView(this);
        mBubbleItemDetailView.setVisibility(GONE);
    }

    public boolean isAbort() {
        return mAbort;
    }

    public boolean isTemp() {
        return mBubbleItem != null && mBubbleItem.isTemp();
    }

    public void abort() {
        mAbort = true;
    }

    boolean isModeEdit() {
        return mMode == ViewMode.BUBBLE_EDIT;
    }

    private boolean needForbidTouch() {
        return (isModeSearch() && !StatusManager.getStatus(StatusManager.HAS_FILTER_STRING));
    }

    private boolean isModeSearch() {
        return ViewMode.BUBBLE_SEARCH == mMode;
    }

    public void setMode(int mode) {
        mMode = mode;
    }

    private int getAttachmentIconRes(BubbleItem item) {
        if (item.isShareColor()) {
            return item.isShareFromOthers() ? R.drawable.little_accessory_icon : R.drawable.little_accessory_icon_share;
        } else {
            return R.drawable.little_accessory_icon;
        }
    }

    private void refreshColorStyle() {
        if (mLastBubbleItemColor == mBubbleItem.getColor() && mLastShareStatus == mBubbleItem.getShareStatus()) {
            return;
        }
        mLastBubbleItemColor = mBubbleItem.getColor();
        if (mBubbleItem.isShareColor()) {
            mllbackground.setPadding(getResources().getDimensionPixelSize(R.dimen.drag_padding_left_share)
                    , mllbackground.getPaddingTop(), mllbackground.getPaddingRight(), mllbackground.getPaddingBottom());
            mCbToDo.setBackgroundResources(mBubbleItem.isShareFromOthers() ? BUBBLE_TODO_CHECKBOX_SHARE : BUBBLE_TODO_CHECKBOX);
            mTextView.setTextColor(getResources().getColor(mBubbleItem.isShareFromOthers() ? R.color.bubble_text_color : R.color.bubble_text_color_share));
            mIvArrowImg.setImageResource(mBubbleItem.isShareFromOthers() ? R.drawable.collapse_icon : R.drawable.collapse_icon_share);
            mVLittleNotify.setImageResource(mBubbleItem.isShareFromOthers() ? R.drawable.little_remind_icon : R.drawable.little_remind_icon_share);
        } else {
            mCbToDo.setBackgroundResources(BUBBLE_TODO_CHECKBOX);
            mllbackground.setPadding(getResources().getDimensionPixelSize(R.dimen.drag_padding_left_full)
                    , mllbackground.getPaddingTop(), mllbackground.getPaddingRight(), mllbackground.getPaddingBottom());
            mTextView.setTextColor(getResources().getColor(R.color.bubble_text_color));
            mIvArrowImg.setImageResource(R.drawable.collapse_icon);
            mVLittleNotify.setImageResource(R.drawable.little_remind_icon);
        }

        mBubbleItemDetailView.refreshColorStyle();
        mAttachmentTag.setImageDrawable(getResources().getDrawable(getAttachmentIconRes(mBubbleItem)));
    }

    private void setImageBg(BubbleItem item) {
        if (!item.isTemp()) {
            mIvBubbleBg.setBackgroundResource(Utils.getBackgroudRes(item.isInLargeMode(), item));
            updateBgDarkStatus(mIvBubbleBg);
            mBubbleItemDetailView.setShadeColor();
        }
    }

    private void setPopShadowBg(BubbleItem item, boolean round) {
        if (!item.isTemp() && mIvBubbleRightShadow != null) {
            final int color = item.getColor();
            int res = 0;
            if (item.isShareColor()) {
                if (round) {
                    res = item.isShareFromOthers() ? R.drawable.text_popup_shade_share_t : R.drawable.text_popup_shade_share;
                } else {
                    res = item.isShareFromOthers() ? R.drawable.pop_expansion_bg_two_shade_share_t : R.drawable.pop_expansion_bg_two_shade_share;
                }
                mIvBubbleRightShadow.setBackgroundResource(res);
                return;
            }
            switch (color) {
                case GlobalBubble.COLOR_RED: {
                    if (round) {
                        res = R.drawable.text_popup_red_shade;
                    } else {
                        res = R.drawable.pop_expansion_bg_red_two_shade;
                    }
                    break;
                }
                case GlobalBubble.COLOR_ORANGE: {
                    if (round) {
                        res = R.drawable.text_popup_orange_shade;
                    } else {
                        res = R.drawable.pop_expansion_bg_orange_two_shade;
                    }
                    break;
                }
                case GlobalBubble.COLOR_GREEN: {
                    if (round) {
                        res = R.drawable.text_popup_green_shade;
                    } else {
                        res = R.drawable.pop_expansion_bg_green_two_shade;
                    }
                    break;
                }
                case GlobalBubble.COLOR_PURPLE: {
                    if (round) {
                        res = R.drawable.text_popup_purple_shade;
                    } else {
                        res = R.drawable.pop_expansion_bg_purple_two_shade;
                    }
                    break;
                }
                case GlobalBubble.COLOR_NAVY_BLUE: {
                    if (round) {
                        res = R.drawable.ppt_pop_expansion_bg_shade;
                    } else {
                        res = R.drawable.ppt_pop_expansion_bg_two_shade;
                    }
                    break;
                }
                default: {
                    if (round) {
                        res = R.drawable.text_popup_shade;
                    } else {
                        res = R.drawable.pop_expansion_bg_two_shade;
                    }
                }
            }
            mIvBubbleRightShadow.setBackgroundResource(res);
            if (mllbackground.getAlpha() == 0.7f) {
                mIvBubbleRightShadow.getBackground().setColorFilter(sDarkerColorFilter);
            }
        }
    }

    public void show(BubbleItem item) {
        show(item, false);
    }

    public void show(BubbleItem item, boolean forbideShowAnim) {
        mBubbleItem = item;
        mBubbleItemDetailView.setBubbleItem(mBubbleItem);
        refreshColorStyle();
        if (item.isNeedInput()) {
            item.setInLargeMode(true);
        }
        mCbToDo.updateUI(mBubbleItem);
        if (item.isInLargeMode()) {
            toLargeMode(item, mBubbleItem.willPlayShowAnim());
        } else {
            toNormal(item, false);
        }
        setHasSelected(item.isSelected());
        if (item.isTemp()) {
            setVisibility(INVISIBLE);
        } else {
            setVisibility(VISIBLE);
        }
        if (mBubbleItem.isToDo() || mBubbleItem.isToDoOver()) {
            playTodo(true);
        }
        showEdit(item, false);
        if (!forbideShowAnim && mBubbleItem.willPlayShowAnim()) {
            mBubbleItem.setWillPlayShowAnim(false);
            if (mOnGlobalLayoutListener != null) {
                getViewTreeObserver().removeOnGlobalLayoutListener(mOnGlobalLayoutListener);
                mOnGlobalLayoutListener = null;
            }
            mOnGlobalLayoutListener = new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    getViewTreeObserver().removeOnGlobalLayoutListener(this);
                    mOnGlobalLayoutListener = null;
                    final AnimTimeLine timeLine = new AnimTimeLine();
                    timeLine.addAnim(new Anim(mFllayout, Anim.TRANSPARENT, Constants.BUBBLE_ANIM_DURATION, Anim.CUBIC_IN, Anim.INVISIBLE, Anim.VISIBLE));
                    timeLine.addAnim(new Anim(mCbToDo, Anim.TRANSPARENT, Constants.BUBBLE_ANIM_DURATION, Anim.CUBIC_IN, Anim.INVISIBLE, Anim.VISIBLE));
                    timeLine.addAnim(new Anim(mIvArrowImg, Anim.TRANSPARENT, Constants.BUBBLE_ANIM_DURATION, Anim.CUBIC_IN, Anim.INVISIBLE, Anim.VISIBLE));
                    timeLine.addAnim(new Anim(mCbCover, Anim.TRANSPARENT, Constants.BUBBLE_ANIM_DURATION, Anim.CUBIC_IN, Anim.INVISIBLE, Anim.VISIBLE));
                    timeLine.start();
                    if (mBubbleItem.isNeedInput()) {
                        mBubbleItemDetailView.callInputMethod();
                    }
                }
            };
            getViewTreeObserver().addOnGlobalLayoutListener(mOnGlobalLayoutListener);
        } else {
            if (mOnGlobalLayoutListener != null) {
                getViewTreeObserver().removeOnGlobalLayoutListener(mOnGlobalLayoutListener);
                mOnGlobalLayoutListener = null;
            }
            mOnGlobalLayoutListener = new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    getViewTreeObserver().removeOnGlobalLayoutListener(this);
                    mOnGlobalLayoutListener = null;
                    if (mBubbleItem.isNeedInput()) {
                        mBubbleItemDetailView.focusInputMethod();
                    }
                }
            };
            getViewTreeObserver().addOnGlobalLayoutListener(mOnGlobalLayoutListener);
        }
        LayoutParams rlp = (LayoutParams)mFllayout.getLayoutParams();
        rlp.topMargin = getResources().getDimensionPixelOffset(R.dimen.bubble_item_base_margin_top);
        rlp.bottomMargin = getResources().getDimensionPixelOffset(R.dimen.bubble_item_base_margin_bottom);
        mFllayout.setLayoutParams(rlp);
        mBubbleItemDetailView.showStaticInput(mBubbleItem.isNeedInput());
        if (!mBubbleItem.isNeedInput()) {
            mLLColorChooser.setVisibility(GONE);
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        if (mOnGlobalLayoutListener != null) {
            getViewTreeObserver().removeOnGlobalLayoutListener(mOnGlobalLayoutListener);
            mOnGlobalLayoutListener = null;
        }
        super.onDetachedFromWindow();
    }

    public boolean isInLargeMode() {
        return mFllayout.getLayoutParams().width == getResources().getDimensionPixelSize(R.dimen.bubbleopt_layout_width);
    }

    public int getEditExpandW(BubbleItem item) {
            return mEditExpandW;
    }

    public boolean isInNormalMode() {
        if (isModeEdit()) {
            return mFllayout.getLayoutParams().width == mBubbleItem.getNormalWidth() + getEditExpandW(mBubbleItem);
        } else {
            return mFllayout.getLayoutParams().width == mBubbleItem.getNormalWidth();
        }
    }

    void stopValueAnimator() {
        if (mValueAnimator != null) {
            mValueAnimator.cancel();
            mValueAnimator = null;
        }
    }

    private void toLargeCommonState(BubbleItem item) {
        ViewGroup.LayoutParams lp = mFllayout.getLayoutParams();
        if(!item.isNeedInput()) {
            lp.width = getResources().getDimensionPixelSize(R.dimen.bubbleopt_layout_width);
        }
        lp.height = ViewGroup.LayoutParams.WRAP_CONTENT;

        if (!isModeEdit() && !item.isNeedInput()) {
            mIvArrowImg.setVisibility(VISIBLE);
            mIvArrowImg.setAlpha(1.0f);
        }
        final View dateview = mBubbleItemDetailView.getDateView();
        dateview.setAlpha(1.0f);
        if (mIvBubbleRightShadow != null && mIvBubbleRightShadow.getVisibility() == View.VISIBLE) {
            mIvBubbleRightShadow.setVisibility(View.GONE);
        }
    }

    public void toLargeMode(final BubbleItem item, boolean needAnim) {
        toLargeMode(item, needAnim, true);
    }

    public List<Animator> toLargeMode(final BubbleItem item, boolean needAnim, boolean startAnimDirectly) {
        ViewGroup.LayoutParams lp = mFllayout.getLayoutParams();
        final TextView expandTextView = mBubbleItemDetailView.mTvTitle;
        final BubbleScrollView expandScrollView = mBubbleItemDetailView.mTvScroll;
        mBubbleItemDetailView.setShadeColor();
        mBubbleItemDetailView.showText();
        mBubbleItemDetailView.showDate();
        mBubbleItemDetailView.checkIsPlaying();
        expandScrollView.setDrawScrollBarEnable(false);

        final int startW = item.isNeedInput() ? BubbleController.getBubbleItemViewHelperByContext(getContext())
                .measureNormalWidth(item) : mFllayout.getWidth();
        final int endW = item.isNeedInput() ?
                BubbleController.getInstance().getBubbleListEditWidth() :
                getResources().getDimensionPixelSize(R.dimen.bubbleopt_layout_width);
        onInstalledPackageChanged();
        final LayoutParams rl_lp = (LayoutParams) expandScrollView.getLayoutParams();
        if (needAnim && startW != endW) {
            //prepare fake bg
            prepareFakeBg(true);
            final int switchBgDuration = 180;
            final ValueAnimator switchBgAnimator = ValueAnimator.ofFloat(0f, 1f);
            switchBgAnimator.setDuration(switchBgDuration);
            switchBgAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    float percent = (Float) animation.getAnimatedValue();
                    mIvBubbleBg.setAlpha(1 - percent);
                    mAttachmentTag.setAlpha(1 - percent);
                    mVLittleNotify.setAlpha(1 - percent);
                    float scale = 0.1f * percent + 0.9f;
                    mIvBubbleFakeBg.setScaleX(scale);
                    mIvBubbleFakeBg.setScaleY(scale);
                    mCbToDo.animDuringToLarge(percent);
                }
            });
            switchBgAnimator.addListener(new AnimatorListener() {
                private boolean mCanceled = false;

                @Override
                public void onAnimationStart(Animator animation) {
                    mIvBubbleFakeBg.setAlpha(1.0f);
                }

                @Override
                public void onAnimationEnd(Animator animation) {
                    mCbToDo.animDuringToLarge(1f);
                    mBubbleItemDetailView.setVisibility(VISIBLE);
                    mllbackground.setVisibility(GONE);
                    mAttachmentTag.setAlpha(1.0f);
                    mVLittleNotify.setAlpha(1.0f);
                    setImageBg(item);
                    mIvBubbleBg.setAlpha(1.0f);
                    mIvBubbleFakeBg.setScaleX(1);
                    mIvBubbleFakeBg.setScaleY(1);
                    mIvBubbleFakeBg.setVisibility(GONE);

                    if (!mCanceled) {
                        final int[] pos = mBubbleItemDetailView.measureTextInLargeMode(item);
                        if(pos != null && pos.length == 2){
                            updateTextViewLayout(expandTextView , pos[0] , pos[1]);
                        }
                    }
                }

                @Override
                public void onAnimationCancel(Animator animation) {
                    mCanceled = true;
                }

                @Override
                public void onAnimationRepeat(Animator animation) {

                }
            });

            final int fromTextMarginTop = getResources().getDimensionPixelSize(R.dimen.bubble_text_margin_top)
                    + ViewUtils.dp2px(getContext(), TOP_MARGIN_DIFF_WHEN_TRANSFORM_FROM_NORMAL_TO_LARGE);
            final int toTextMarginTop =  getResources().getDimensionPixelSize(R.dimen.bubble_text_margin_top);
            final int fromTextMarginLeft;
            CharSequence text = getAndCorrectContent(item);
            if (text.length() > 20) {
                // if text length > 20, no possible < min width of textView
                fromTextMarginLeft = 0;
            } else {
                float dragTextRealWidth = mTextView.getPaint().measureText(text, 0, text.length());
                int intervalBetweenMinWidth = (int) (mTextView.getMinWidth() - dragTextRealWidth);
                fromTextMarginLeft = intervalBetweenMinWidth < 0 ? 0 : intervalBetweenMinWidth / 2;
            }
            final int toTextMarginLeft = 0;
            rl_lp.topMargin = fromTextMarginTop;
            rl_lp.leftMargin = fromTextMarginLeft;

            final int startH = item.isNeedInput() ?
                    BubbleController.getBubbleItemViewHelperByContext(getContext()).getNormalBubbleBgHeight() : mFllayout.getHeight();
            lp.width = endW;
            final int endH = mBubbleItem.getHeightInLarge();
            final int diffH = startH - getHeight();
            Point start = new Point();
            start.set(startW, startH);
            Point end = new Point();
            end.set(endW, endH);
            stopValueAnimator();
            initValueAnimator(start, end, ToLargeAnimDura, null);
            mValueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                public void onAnimationUpdate(ValueAnimator animation) {
                    final long playTime = animation.getCurrentPlayTime();
                    final float percent = 1.0f * playTime / ToLargeAnimDura;
                    Point point = (Point) animation.getAnimatedValue();
                    mIvArrowImg.setAlpha(percent);
                    // this could make anim more quick
                    if(percent >= 0.75){
                        mBubbleItemDetailView.refreshChildViewAlphaDuringAnim(4f * (percent - 0.75f));
                    }
                    if (getAlpha() != 1.0f) {
                        setAlpha(percent);
                    }
                    // root layout
                    ViewGroup.LayoutParams lp = mFllayout.getLayoutParams();
                    lp.width = point.x;
                    lp.height = point.y;
                    mFllayout.setLayoutParams(lp);

                    if (playTime >= switchBgDuration) {
                        float marginPercent = 1.0f * (playTime - switchBgDuration) / (ToLargeAnimDura - switchBgDuration);
                        rl_lp.topMargin = (int) (fromTextMarginTop + (toTextMarginTop - fromTextMarginTop) * marginPercent);
                        rl_lp.leftMargin = (int) (fromTextMarginLeft + (toTextMarginLeft - fromTextMarginLeft) * marginPercent);
                    }

                    // shadow
                    if (percent > 0.0f && percent < 0.8f) {
                        if (mIvBubbleRightShadow != null && mIvBubbleRightShadow.getVisibility() == View.GONE) {
                            setPopShadowBg(item, false);
                            mIvBubbleRightShadow.setVisibility(View.VISIBLE);
                        }
                    } else {
                        if (mIvBubbleRightShadow != null && mIvBubbleRightShadow.getVisibility() == View.VISIBLE) {
                            mIvBubbleRightShadow.setVisibility(View.GONE);
                        }
                    }
                }

            });
            mValueAnimator.addListener(new AnimatorListener() {
                @Override
                public void onAnimationStart(Animator animation) {
                    mBubbleItemDetailView.hideChildViewDuringAnim();
                    if(mIvBubbleRightShadow != null){
                       mIvBubbleRightShadow.setVisibility(View.GONE);
                    }
                }
                @Override
                public void onAnimationEnd(Animator animation) {
                    if (needLargeCallback) {
                        needLargeCallback = false;
                        if (mToLargeCallBack != null) {
                            mToLargeCallBack.toLarge(getTop(), endH - diffH);
                        }
                    }
                    if (mBubbleItemDetailView != null) {
                        mBubbleItemDetailView.resetChildViewStatusWhenEndAnim();
                    }
                    rl_lp.topMargin = getResources().getDimensionPixelSize(R.dimen.bubble_text_margin_top);
                    rl_lp.leftMargin = 0;
                    resetTextViewLayout(expandTextView);
                    toLargeCommonState(item);
                    if (getAlpha() != 1.0f) {
                        setAlpha(1.0f);
                    }
                }
                @Override
                public void onAnimationCancel(Animator animation) {
                    if (switchBgAnimator.isRunning()) {
                        switchBgAnimator.cancel();
                    }
                }
                @Override
                public void onAnimationRepeat(Animator animation) {}
            });
            if (startAnimDirectly) {
                switchBgAnimator.start();
                mValueAnimator.start();
                return null;
            } else  {
                List<Animator> animators = new ArrayList<>();
                animators.add(switchBgAnimator);
                animators.add(mValueAnimator);
                return animators;
            }
        } else {
            rl_lp.topMargin = getResources().getDimensionPixelSize(R.dimen.bubble_text_margin_top);
            rl_lp.leftMargin = 0;
            mBubbleItemDetailView.setVisibility(VISIBLE);
            mllbackground.setVisibility(GONE);
            if(getAlpha() != 1.0f){
                setAlpha(1.0f);
            }
            setImageBg(item);
            toLargeCommonState(item);
            return startAnimDirectly ? null : new ArrayList<Animator>();
        }
    }

    private void resetTextViewLayout(TextView expandTextView){
        FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams)expandTextView.getLayoutParams();
        lp.height = ViewGroup.LayoutParams.WRAP_CONTENT;
        lp.width  = ViewGroup.LayoutParams.MATCH_PARENT;
        expandTextView.setLayoutParams(lp);
    }

    private void updateTextViewLayout(TextView expandTextView , int width , int height){
        FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams)expandTextView.getLayoutParams();
        lp.height = height;
        lp.width  = width;
        expandTextView.setLayoutParams(lp);
    }

    /**
     * get or correct mTextView content before animation
     *
     * @param item
     */
    private CharSequence getAndCorrectContent(BubbleItem item) {
        final String singleText = item.getSingleText();
        if (!TextUtils.equals(singleText, mTextView.getText())) {
            mTextView.setText(singleText);
            item.invalidateNormalWidth();
        }

        return mTextView.getText();
    }

    public void toInput(final BubbleItem item,final boolean needAnim) {
        if (!item.isInLargeMode()) {
            mLLColorChooser.setVisibility(GONE);
            mBubbleItemDetailView.setMaxHeight(false, needAnim);
            return;
        }

        final int extraHeight = getResources().getDimensionPixelOffset(R.dimen.bubble_input_extra_hight);
        LayoutParams rlp = (LayoutParams)mFllayout.getLayoutParams();
        int targetWidth = 0;
        float fromAlpha;
        float targetAlpha;
        int targetTop = getResources().getDimensionPixelOffset(R.dimen.bubble_item_base_margin_top) + extraHeight;
        int targetBottom = getResources().getDimensionPixelOffset(R.dimen.bubble_item_base_margin_bottom) + extraHeight;
        final boolean inputMode = item.isNeedInput();
        if (item.isNeedInput()) {
            if (SmtPCUtils.isValidExtDisplayId(getContext())) {
                targetWidth = BubbleController.getInstance().getBubbleListEditWidth();
            } else {
                targetWidth = getWidth() == 0 ? ViewGroup.LayoutParams.MATCH_PARENT : getWidth();
            }
            targetAlpha = 0.0f;
            fromAlpha = 1.0f;
        } else {
            targetWidth = getResources().getDimensionPixelSize(R.dimen.bubbleopt_layout_width);
            mLLColorChooser.setVisibility(GONE);
            targetAlpha = 1.0f;
            fromAlpha = 0.0f;
            targetTop = getResources().getDimensionPixelOffset(R.dimen.bubble_item_base_margin_top);
            targetBottom = getResources().getDimensionPixelOffset(R.dimen.bubble_item_base_margin_bottom);
        }
        final int fromTopMargin = rlp.topMargin;
        final int targetTopMargin = targetTop;
        final int fromBottomMargin = rlp.bottomMargin;
        final int targetBottomMargin = targetBottom;
        final int startW = mFllayout.getWidth();
        final int endW = targetWidth;
        if (!isModeEdit() && !item.isNeedInput()) {
            mIvArrowImg.setVisibility(VISIBLE);
            if(needAnim) {
                Anim anim = new Anim(mIvArrowImg, Anim.TRANSPARENT, 150, Anim.CUBIC_OUT, new Vector3f(0, 0, fromAlpha), new Vector3f(0, 0, targetAlpha));
                anim.start();
            } else {
                mIvArrowImg.setAlpha(1.0f);
            }
        } else {
            mIvArrowImg.setVisibility(GONE);
        }
        if (needAnim && (startW != endW || fromTopMargin != targetTopMargin || fromBottomMargin != targetBottomMargin)) {
            stopValueAnimator();
            mValueAnimator = ValueAnimator.ofInt(startW, endW);
            mValueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    int width = (Integer)animation.getAnimatedValue();
                    LayoutParams lp = (LayoutParams)mFllayout.getLayoutParams();
                    lp.width = width;
                    lp.topMargin = (int) ((targetTopMargin - fromTopMargin) * 1.0f * animation.getCurrentPlayTime() / animation.getDuration() + fromTopMargin);
                    lp.bottomMargin = (int) ((targetBottomMargin - fromBottomMargin) * 1.0f * animation.getCurrentPlayTime() / animation.getDuration() + fromBottomMargin);
                    mFllayout.setLayoutParams(lp);
                }
            });
            final boolean finalIsNeedInput = inputMode;
            mValueAnimator.addListener(new AnimatorListener() {

                @Override
                public void onAnimationStart(Animator animation) {
                    setScrollViewScrollbarVisible(false);
                }

                @Override
                public void onAnimationEnd(Animator animation) {

                    if (finalIsNeedInput) {
                        mBubbleItemDetailView.setMaxHeight(true, needAnim);
                    } else {
                        mBubbleItemDetailView.setMaxHeight(false, needAnim);
                    }

                    setScrollViewScrollbarVisible(inputMode);
                    mCbToDo.updateUI(item);

                    if (item.isNeedInput() && !BubbleController.getInstance().isInPptContext(getContext())) {
                        mLLColorChooser.setVisibility(VISIBLE);
                    }
                }
                @Override
                public void onAnimationCancel(Animator animation) {
                    setScrollViewScrollbarVisible(inputMode);
                    mCbToDo.updateUI(item);
                    if (item.isNeedInput() && !BubbleController.getInstance().isInPptContext(getContext())) {
                        mLLColorChooser.setVisibility(VISIBLE);
                    }
                }

                @Override
                public void onAnimationRepeat(Animator animation) {
                }});
            mValueAnimator.setDuration(200);
            mValueAnimator.start();
        } else {

            if (inputMode) {
                mBubbleItemDetailView.setMaxHeight(true, needAnim);
            } else {
                mBubbleItemDetailView.setMaxHeight(false, needAnim);
            }

            rlp.width = targetWidth;
            rlp.topMargin = targetTopMargin;
            rlp.bottomMargin = targetBottomMargin;
            setScrollViewScrollbarVisible(inputMode);
            if (item.isNeedInput() && !BubbleController.getInstance().isInPptContext(getContext())) {
                mLLColorChooser.setVisibility(VISIBLE);
            }


        }
    }

    private void setScrollViewScrollbarVisible(boolean visible){
        final View expandTextRelativeView = mBubbleItemDetailView;
        final ScrollView expandScrollView;
        if(expandTextRelativeView != null) {
            expandScrollView = (ScrollView) expandTextRelativeView.findViewById(R.id.tv_scroll);
        }else{
            expandScrollView = null;
        }
        if(expandScrollView != null && expandScrollView instanceof BubbleScrollView){
            ((BubbleScrollView)expandScrollView).setDrawScrollBarEnable(visible);
        }
    }

    private void playTodo(boolean show) {
        if (show) {
            mCbToDo.setOnCheckedChangeListener(null);
            if (mBubbleItem.isToDo()) {
                mCbToDo.setChecked(false);
            } else if (mBubbleItem.isToDoOver()) {
                mCbToDo.setChecked(true);
            }
            mCbToDo.setOnCheckedChangeListener(this);
            mCbToDo.setVisibility(VISIBLE);
        } else {
            mCbToDo.setVisibility(GONE);
        }
    }

    public void setHasSelected(boolean hasSelected) {
        mCbCover.setChecked(hasSelected);
    }

    public void toggleCheckState() {
        mCbToDo.performClick();
    }

    public void toNormal(final BubbleItem item, boolean needAnim) {
        toNormal(item, needAnim, true);
    }

    public Animator showEdit(final BubbleItem item, boolean needAnim) {
        if (needAnim) {
            final int alphaDura = 150;
            if (item.isInLargeMode()) {
                mCbCover.setVisibility(VISIBLE);
                mIvArrowImg.setVisibility(VISIBLE);
                AnimListener listener = new SimpleAnimListener() {
                    public void onStart() {

                    }

                    public void onComplete(int type) {
                        if (mIvArrowImg.getAlpha() == 0.0f) {
                            mIvArrowImg.setVisibility(GONE);
                        }
                        showEdit(item, false);
                        setHasSelected(item.isSelected());
                    }
                };
                AnimTimeLine line = new AnimTimeLine();
                if (isModeEdit()) {
                    Anim anim = new Anim(mCbCover, Anim.TRANSPARENT, alphaDura, Anim.CUBIC_OUT, Anim.INVISIBLE, Anim.VISIBLE);
                    anim.setDelay(100);
                    line.addAnim(anim);
                    anim = new Anim(mIvArrowImg, Anim.TRANSPARENT, alphaDura, Anim.CUBIC_OUT, Anim.VISIBLE, Anim.INVISIBLE);
                    line.addAnim(anim);
                } else {
                    Anim anim = new Anim(mCbCover, Anim.TRANSPARENT, alphaDura, Anim.CUBIC_OUT, Anim.VISIBLE, Anim.INVISIBLE);
                    line.addAnim(anim);
                    anim = new Anim(mIvArrowImg, Anim.TRANSPARENT, alphaDura, Anim.CUBIC_OUT, Anim.INVISIBLE, Anim.VISIBLE);
                    line.addAnim(anim);
                }
                line.setAnimListener(listener);
                line.start();
            } else {
                stopValueAnimator();
                AnimListener listener = new SimpleAnimListener() {
                    public void onStart() {

                    }

                    public void onComplete(int type) {
                        showEdit(item, false);
                    }
                };
                final int startW = mFllayout.getWidth();
                int endW = item.getNormalWidth();
                AnimTimeLine line = new AnimTimeLine();
                if (isModeEdit()) {
                    endW += getEditExpandW(item);
                    mCbCover.setVisibility(VISIBLE);
                    Anim anim = new Anim(mCbCover, Anim.TRANSPARENT, alphaDura, Anim.CUBIC_OUT, Anim.INVISIBLE, Anim.VISIBLE);
                    line.addAnim(anim);
                    if (mAttachmentTag.getVisibility() != GONE) {
                        changeAttachmentTagVisible(item);
                        anim = new Anim(mAttachmentTag, Anim.TRANSPARENT, alphaDura, Anim.CUBIC_OUT, Anim.VISIBLE, Anim.INVISIBLE);
                        line.addAnim(anim);
                    }
                } else {
                    Anim anim = new Anim(mCbCover, Anim.TRANSPARENT, alphaDura, Anim.CUBIC_OUT, Anim.VISIBLE, Anim.INVISIBLE);
                    line.addAnim(anim);
                    if (mAttachmentTag.getVisibility() != GONE) {
                        changeAttachmentTagVisible(item);
                        anim = new Anim(mAttachmentTag, Anim.TRANSPARENT, alphaDura, Anim.CUBIC_OUT, Anim.INVISIBLE, Anim.VISIBLE);
                        line.addAnim(anim);
                    }
                }
                line.setAnimListener(listener);
                line.start();
                mValueAnimator = ValueAnimator.ofInt(startW, endW);
                mValueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                    public void onAnimationUpdate(ValueAnimator animation) {
                        Integer width = (Integer)animation.getAnimatedValue();
                        ViewGroup.LayoutParams lp = mFllayout.getLayoutParams();
                        lp.width = width;
                        mFllayout.setLayoutParams(lp);
                    }
                });
                mValueAnimator.setDuration(200);
                return mValueAnimator;
            }
        } else {
            if (isModeEdit()) {
                mCbCover.setVisibility(VISIBLE);
                mCbCover.setAlpha(1.0f);
                mIvArrowImg.setVisibility(GONE);
                if (mAttachmentTag.getVisibility() == VISIBLE) {
                    mAttachmentTag.setVisibility(GONE);
                    item.invalidateNormalWidth();
                }
                if (mCbCover.isChecked()) {
                    showdark(false);
                } else {
                    showdark(true);
                }
                mBubbleItemDetailView.setOptEnable(false);
                ViewUtils.setViewEnable(mCbToDo, false);
            } else {
                if (needForbidTouch()) {
                    mBubbleItemDetailView.setOptEnable(false);
                    ViewUtils.setViewEnable(mCbToDo, false);
                    showdark(true);
                } else {
                    mBubbleItemDetailView.setOptEnable(true);
                    ViewUtils.setViewEnable(mCbToDo, true);

                    int curPptBubbleId = BubbleController.getInstance().getCurPptAddBubbleId();
                    if (curPptBubbleId >= 0) {
                        if (mBubbleItem != null && curPptBubbleId == mBubbleItem.getId()) {
                            showdark(false);
                        } else {
                            showdark(true);
                        }
                    } else {
                        showdark(false);
                    }
                }
                mCbCover.setVisibility(GONE);
                changeAttachmentTagVisible(item);
            }
        }
        return null;
    }

    private void changeAttachmentTagVisible(BubbleItem item) {
        int oldVisibility = mAttachmentTag.getVisibility();
        if (item.haveAttachments()) {
            mAttachmentTag.setVisibility(VISIBLE);
            mAttachmentTag.setAlpha(1.0f);
        } else {
            mAttachmentTag.setVisibility(GONE);
        }
        int newVisibility = mAttachmentTag.getVisibility();
        if (oldVisibility != newVisibility) {
            item.invalidateNormalWidth();
        }
    }

    private void showText(BubbleItem item) {
        if (item.isInLargeMode()) {
            mBubbleItemDetailView.showText();
        } else {
            if (item.isToDoOver()) {
                mTextView.setPaintFlags(mTextView.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
                mTextView.setAlpha(0.4f);
            } else if (TextUtils.isEmpty(item.getText()) && item.getAttachments().size() > 0) {
                mTextView.setPaintFlags(mTextView.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
                mTextView.setAlpha(0.4f);
            } else {
                mTextView.setPaintFlags(mTextView.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
                mTextView.setAlpha(1.0f);
            }

            mTextView.setText(item.getSingleText());
            if (ViewUtils.getSupposeWidth(mTextView) == mTextView.getMinWidth()) {
                mTextView.setGravity(Gravity.CENTER);
            } else {
                mTextView.setGravity(Gravity.LEFT);
            }
        }
    }

    public List<Animator> toNormal(final BubbleItem item, boolean needAnim, boolean startNow) {
        mBubbleItemDetailView.stopPlay();
        final ViewGroup.LayoutParams lp = mFllayout.getLayoutParams();
        if (needAnim) {
            final int startW = lp.width;
            CharSequence text = getAndCorrectContent(item);
            int endW = item.getNormalWidth();
            if (isModeEdit()) {
                endW += getEditExpandW(item);
            }
            final int startH = mFllayout.getHeight();
            final int endH = BubbleController.getBubbleItemViewHelperByContext(getContext()).getNormalBubbleBgHeight();
            Point start = new Point();
            start.set(startW, startH);
            Point end = new Point();
            end.set(endW, endH);
            stopValueAnimator();

            final ScrollView expandScrollView = mBubbleItemDetailView.mTvScroll;
            mBubbleItemDetailView.mTvScroll.setDrawScrollBarEnable(false);
            final ValueAnimator switchBgAnimator = ValueAnimator.ofFloat(0f, 1f);
            final int fromTextMarginTop = getResources().getDimensionPixelSize(R.dimen.bubble_text_margin_top);
            final int toTextMarginTop =  getResources().getDimensionPixelSize(R.dimen.bubble_text_margin_top)
                    + ViewUtils.dp2px(getContext(), TOP_MARGIN_DIFF_WHEN_TRANSFORM_FROM_NORMAL_TO_LARGE);
            final int toTextMarginLeft;
            if (text.length() > 20) {
                // if text length > 20, no possible < min width of textView
                toTextMarginLeft = 0;
            } else {
                float dragTextRealWidth = mTextView.getPaint().measureText(text, 0, text.length());
                int intervalBetweenMinWidth = (int) (mTextView.getMinWidth() - dragTextRealWidth);
                toTextMarginLeft = intervalBetweenMinWidth < 0 ? 0 : intervalBetweenMinWidth / 2;
            }
            final LayoutParams rl_lp = (LayoutParams) expandScrollView.getLayoutParams();
            final int fromTextMarginLeft = 0;
            rl_lp.topMargin = fromTextMarginTop;
            rl_lp.leftMargin = fromTextMarginLeft;
            prepareFakeBg(false);
            final int switchBgDuration = 50;
            switchBgAnimator.setDuration(switchBgDuration);
            switchBgAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    float percent = (Float) animation.getAnimatedValue();
                    mIvBubbleBg.setAlpha(1 - percent);
                    rl_lp.topMargin = (int) (fromTextMarginTop + (toTextMarginTop - fromTextMarginTop) * percent);
                    rl_lp.leftMargin = (int) (fromTextMarginLeft + (toTextMarginLeft - fromTextMarginLeft) * percent);

                }
            });
            switchBgAnimator.addListener(new AnimatorListener() {
                @Override
                public void onAnimationStart(Animator animation) {
                    mIvBubbleFakeBg.setAlpha(1.0f);
                }

                @Override
                public void onAnimationEnd(Animator animation) {
                    mCbToDo.playToNormalAnim(100);
                    setImageBg(item);
                    rl_lp.topMargin = getResources().getDimensionPixelSize(R.dimen.bubble_text_margin_top);
                    rl_lp.leftMargin = 0;
                    mBubbleItemDetailView.setVisibility(GONE);
                    mllbackground.setVisibility(VISIBLE);
                    mIvBubbleBg.setAlpha(1.0f);
                    mIvBubbleFakeBg.setVisibility(GONE);
                }

                @Override
                public void onAnimationCancel(Animator animation) {
                }
                @Override
                public void onAnimationRepeat(Animator animation) {
                }
            });

            final TextView expandTextView = mBubbleItemDetailView.mTvTitle;
            initValueAnimator(start, end, ToLargeAnimDura, mToNormalListener);
            mValueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                public void onAnimationUpdate(ValueAnimator animation) {
                    long playTime = animation.getCurrentPlayTime();
                    final float percent = 1.0f * playTime / ToLargeAnimDura;
                    mIvArrowImg.setAlpha(1.0f - percent); // arrow fade out
                    if (percent <= 0.25f) {
                        float detailVPercent = (0.25f - percent) * 4;
                        mBubbleItemDetailView.refreshChildViewAlphaDuringAnim(detailVPercent);
                    } else {
                        mBubbleItemDetailView.hideChildViewDuringAnim();
                    }

                    updateFllLayoutParams((Point) animation.getAnimatedValue());
                    if (percent > 0.1f && percent < 1.0f) {
                        if (mIvBubbleRightShadow != null && mIvBubbleRightShadow.getVisibility() == View.GONE) {
                            setPopShadowBg(item, true);
                            mIvBubbleRightShadow.setVisibility(View.VISIBLE);
                        }
                    } else {
                        if (mIvBubbleRightShadow != null && mIvBubbleRightShadow.getVisibility() == View.VISIBLE) {
                            mIvBubbleRightShadow.setVisibility(View.GONE);
                        }
                    }
                }
            });
            mValueAnimator.addListener(new AnimatorListener() {
                @Override
                public void onAnimationStart(Animator animation) {
                    mBubbleItemDetailView.showChildViewDuringAnim();
                    if(mIvBubbleRightShadow != null){
                        mIvBubbleRightShadow.setVisibility(View.GONE);
                    }
                    final int width = expandTextView.getWidth();
                    final int height = expandTextView.getHeight();
                    updateTextViewLayout(expandTextView, width, height);
                }
                @Override
                public void onAnimationEnd(Animator animation) {
                    mBubbleItemDetailView.resetChildViewStatusWhenEndAnim();
                    if(mIvBubbleRightShadow != null){
                        mIvBubbleRightShadow.setVisibility(View.GONE);
                    }
                    resetTextViewLayout(expandTextView);
                }
                @Override
                public void onAnimationCancel(Animator animation) {
                    if (switchBgAnimator.isRunning()) {
                        switchBgAnimator.cancel();
                    }
                }
                @Override
                public void onAnimationRepeat(Animator animation) {}
            });

            if (startNow) {
                mValueAnimator.start();
                switchBgAnimator.start();
                return null;
            } else {
                List<Animator> animators = new ArrayList<>();
                animators.add(switchBgAnimator);
                animators.add(mValueAnimator);
                return animators;
            }
        } else {
            lp.width = item.getNormalWidth();
            if (isModeEdit()) {
                lp.width += getEditExpandW(item);
            }
            lp.height = ViewGroup.LayoutParams.WRAP_CONTENT;
            mBubbleItemDetailView.setVisibility(GONE);
            mllbackground.setVisibility(VISIBLE);
            changeAttachmentTagVisible(item);
            Date remindDate = new Date(item.getDueDate());
            if (remindDate.after(new Date()) && item.getRemindTime() > 0) {
                mVLittleNotify.setVisibility(VISIBLE);
            } else {
                mVLittleNotify.setVisibility(GONE);
            }
            mAttachmentTag.setImageDrawable(getResources().getDrawable(getAttachmentIconRes(mBubbleItem)));
            showText(item);
            setImageBg(item);
            mIvArrowImg.setVisibility(GONE);
            if(mIvBubbleRightShadow != null && mIvBubbleRightShadow.getVisibility() == View.VISIBLE){
                mIvBubbleRightShadow.setVisibility(View.GONE);
            }
            return null;
        }
    }

    public void onBubbleAttachDelete() {
        if (mBubbleItem.isEmptyBubble() && !mBubbleItem.isNeedInput()) {
            deleteItemAnim(BubbleItem.FLAG_NEEDDELE);
        } else {
            if (!mBubbleItem.isTextAvailable()) {
                if (mBubbleItem.isInLargeMode()) {
                    mBubbleItemDetailView.showText();
                } else {
                    mTextView.setText(mBubbleItem.getSingleText());
                }
            }
            requestLayout();
        }
    }

    public void deleteItemAnim(final int delFlag) {
        ViewGroup.LayoutParams lp = mFllayout.getLayoutParams();
        final int startW = lp.width;
        final int startH = mFllayout.getHeight();
        Point start = new Point();
        start.set(startW, startH);
        Point end = new Point();
        final BubbleItem item = mBubbleItem;
        int endW = item.getNormalWidth();
        if (isModeEdit()) {
            endW += getEditExpandW(item);
        }
        end.set(endW, 0);
        final int endH = BubbleController.getBubbleItemViewHelperByContext(getContext()).getNormalBubbleBgHeight();
        stopValueAnimator();
        View expandTextRelativeView = mBubbleItemDetailView;
        final ScrollView expandScrollView = (ScrollView) expandTextRelativeView.findViewById(R.id.tv_scroll);
        int from = expandTextRelativeView.getTop() + expandScrollView.getTop();
        int to = mllbackground.getTop() + mTextView.getTop();
        //from > to
        final int expandTextViewMoveY = from - to;
        final int baseMargin = ((LayoutParams) expandScrollView.getLayoutParams()).topMargin;
        initValueAnimator(start, end, ToDelAnimDura, mToNormalListener, getDelBubbleItemTypeEvaluator());
        mValueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            public void onAnimationUpdate(ValueAnimator animation) {
                long playTime = animation.getCurrentPlayTime();
                moveUpScrollView(expandScrollView, expandTextViewMoveY, baseMargin, playTime * 1.0f / ToDelAnimDura);
                Point point = (Point) animation.getAnimatedValue();
                updateFllLayoutParams(point);
                int width = point.y > endH ? point.y : endH;
                float alpha = (width - endH) * 1.0f / (startH - endH);
                mFllayout.setAlpha(alpha);
            }
        });
        prepareFakeBg(false);
        mValueAnimator.addListener(new AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {
                startSwitchBgAnim(item);
                mIvArrowImg.setVisibility(GONE);
                mCbCover.setVisibility(GONE);
                if (mCbToDo != null) {
                    mCbToDo.setVisibility(GONE);
                }
                StatusManager.setStatus(StatusManager.BUBBLE_DELETING, true);
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                if (delFlag == BubbleItem.FLAG_NEEDTRASH) {
                    item.trash();
                } else if (delFlag == BubbleItem.FLAG_NEEDREMOVE) {
                    item.remove();
                } else {
                    item.dele();
                }
                GlobalBubbleManager.getInstance().removeBubbleItem(item);
                IdeaPillsApp.getInstance().getBubbleObserverManager().notifyBubbleObserver(item, BubbleItem.MSG_BUBBLE_DELETE);
                mFllayout.setAlpha(1);
                StatusManager.setStatus(StatusManager.BUBBLE_DELETING, false);
            }

            @Override
            public void onAnimationCancel(Animator animation) {
            }

            @Override
            public void onAnimationRepeat(Animator animation) {
            }
        });
        mValueAnimator.start();
    }

    private TypeEvaluator<Point> getDelBubbleItemTypeEvaluator () {
        if (mDelBubbleItemTypeEvaluator == null) {
            mDelBubbleItemTypeEvaluator = new TypeEvaluator<Point>() {
                Point mPoint = new Point();
                @Override
                public Point evaluate(float fraction, Point startValue, Point endValue) {
                    float percent = (float)(1.0f - Math.pow((1.0f - fraction), 2 * 1.5f));
                    int sx = startValue.x;
                    int sy = startValue.y;
                    int cx = (int) (sx + percent * (endValue.x - sx));
                    int cy = (int) (sy + percent * (endValue.y - sy));
                    mPoint.set(cx, cy);
                    return mPoint;
                }
            };
        }
        return mDelBubbleItemTypeEvaluator;
    }

    private void startSwitchBgAnim (final BubbleItem item) {
        final AnimTimeLine switchBgAnim = new AnimTimeLine();
        Anim bubbleBgAnim = new Anim(mIvBubbleBg, Anim.TRANSPARENT, 50, 0, Anim.VISIBLE, Anim.INVISIBLE);
        switchBgAnim.addAnim(bubbleBgAnim);
        switchBgAnim.setAnimListener(new SimpleAnimListener() {
            @Override
            public void onStart() {
                mIvBubbleFakeBg.setAlpha(1.0f);
            }

            @Override
            public void onComplete(int type) {
                setImageBg(item);
                mIvBubbleBg.setAlpha(1.0f);
                mIvBubbleFakeBg.setVisibility(GONE);
            }
        });
        switchBgAnim.start();
    }

    private void prepareFakeBg (boolean inlarge) {
        mIvBubbleFakeBg.setBackgroundResource(Utils.getBackgroudRes(inlarge, mBubbleItem));
        updateBgDarkStatus(mIvBubbleFakeBg);
        mIvBubbleFakeBg.setVisibility(VISIBLE);
        mIvBubbleFakeBg.setAlpha(0.0f);
    }

    private void moveUpScrollView(View scrollview, int scrollviewMove, int baseMargin, float percent) {
        int deltaY = (int) (scrollviewMove * percent);
        if (deltaY > scrollviewMove) {
            deltaY = scrollviewMove;
        }
        ((LayoutParams) scrollview.getLayoutParams()).topMargin = (baseMargin - deltaY);
    }

    private void updateFllLayoutParams (Point point) {
        ViewGroup.LayoutParams params = mFllayout.getLayoutParams();
        params.width = point.x;
        params.height = point.y;
        mFllayout.setLayoutParams(params);
    }

    private void initValueAnimator (Point start, Point end, long duration, AnimatorListener listener) {
        initValueAnimator(start, end, duration, listener, mToLargeTypeEvaluator);
    }

    private void initValueAnimator(Point start, Point end, long duration, AnimatorListener listener, TypeEvaluator typeEvaluator) {
        mValueAnimator = ValueAnimator.ofObject(typeEvaluator, start, end);
        if (listener != null) {
            mValueAnimator.addListener(listener);
        }
        mValueAnimator.setDuration(duration);
    }

    public int queryWidth() {
        int left = mFllayout.getLeft();
        return getRight() - left;
    }

    public void turnToDo(boolean flag) {
        if (flag) {
            Tracker.onEvent(BubbleTrackerID.BUBBLE_OPT_TODO, "source", 1);
            mBubbleItem.setToDo(GlobalBubble.TODO);
            playTodo(true);
        } else {
            mBubbleItem.setToDo(0);
            mBubbleItemDetailView.toDoOver(false);
            playTodo(false);
        }
    }

    public void onClick(View v) {
        if (!ViewUtils.isClickAvailable()) {
            return;
        }
        switch (v.getId()) {
            case R.id.iv_color_red: {
                changeItemColor(GlobalBubble.COLOR_RED, false);
            }
            break;
            case R.id.iv_color_orange: {
                changeItemColor(GlobalBubble.COLOR_ORANGE, false);
            }
            break;
            case R.id.iv_color_green: {
                changeItemColor(GlobalBubble.COLOR_GREEN, false);
            }
            break;
            case R.id.iv_color_blue: {
                changeItemColor(GlobalBubble.COLOR_BLUE, false);
            }
            break;
            case R.id.iv_color_purple: {
                changeItemColor(GlobalBubble.COLOR_PURPLE, false);
            }
            break;
            case R.id.iv_color_navy_blue: {
                changeItemColor(GlobalBubble.COLOR_NAVY_BLUE, false);
            }
            break;
            case R.id.iv_color_share: {
                changeItemColor(-1, true);
            }
            break;
            case R.id.fl_layout:
                if (isModeEdit()) {
                    mCbCover.setChecked(!mCbCover.isChecked());
                } else {
                    if (!mBubbleItem.isNeedInput()) {
                        if (mBubbleItem.isInLargeMode()) {
                            if (isModeEdit()) {
                                mCbCover.setChecked(!mCbCover.isChecked());
                            } else {
                                if (checkInModeChangedTouchArea((int)mDownX, (int)mDownY)) {
                                    performArrowClick();
                                    break;
                                }

                                if (!mBubbleItem.isNeedInput()) {
                                    mBubbleItem.setNeedInput(true);
                                    mBubbleItemDetailView.needInput(true);
                                }
                            }
                        } else {
                            needLargeCallback = true;
                            mBubbleItem.setInLargeMode(true);
                            toLargeMode(mBubbleItem, true);
                            IdeaPillsApp.getInstance().getBubbleObserverManager().notifyBubbleObserver(mBubbleItem, BubbleItem.MSG_LARGEMODE_CHANGE);
                        }
                    }
                }
                break;
            case R.id.v_speech_tag:
                needLargeCallback = true;
                mBubbleItem.setInLargeMode(true);
                toLargeMode(mBubbleItem, true);
                IdeaPillsApp.getInstance().getBubbleObserverManager().notifyBubbleObserver(mBubbleItem, BubbleItem.MSG_LARGEMODE_CHANGE);
                break;
            case R.id.iv_arrow:
                performArrowClick();
                break;
        }
    }

    private void changeItemColor(final int color, boolean isShareColor) {
         boolean isNowShare = mBubbleItem.isShareColor();
        CommonUtils.vibrateEffect(mContext, VibEffectSmt.EFFECT_SWITCH);
        if (isNowShare != isShareColor) {
            if (isShareColor) {
                if (mBubbleItem.isShareFromOthers()) {
                    return;
                }
                final long userId = SyncManager.getCloudAccountUserId(IdeaPillsApp.getInstance());
                if (userId < 0) {
                    GlobalInvitationAction.getInstance().showLoginDialog(true);
                    return;
                }
                if (!SyncManager.syncEnable(IdeaPillsApp.getInstance())) {
                    GlobalInvitationAction.getInstance().showLoginDialog(false);
                    return;
                }
                final BubbleItem updateBubbleItem = mBubbleItem;
                SyncShareManager.INSTANCE.getInvitationList(new SyncBundleRepository.RequestListener<List<SyncShareInvitation>>() {
                    @Override
                    public void onRequestStart() {

                    }

                    @Override
                    public void onResponse(List<SyncShareInvitation> response) {
                        List<Long> addParticipantIds = SyncShareUtils.findParticipantIdsForAdd(userId, response);
                        if (addParticipantIds.isEmpty()) {
                            SyncShareInvitation invitation = SyncShareManager.INSTANCE.getInvitation(userId);
                            if (invitation != null && (invitation.inviteStatus == SyncShareInvitation.INVITE_ACCEPT ||
                                    invitation.inviteStatus == SyncShareInvitation.INVITE_START)) {
                                updateBubbleItem.changeShareStatusToWaitInvitation(invitation.invitee.id);
                                updateItemColorIfSame(updateBubbleItem);
                            } else {
                                if (!SyncShareUtils.canAddAnotherInvitation(userId, response)) {
                                    GlobalBubbleUtils.showSystemToast(R.string.sync_not_support_multiple_account_tip,
                                            Toast.LENGTH_SHORT);
                                } else {
                                    GlobalInvitationAction.getInstance().showInvitationDialog(new GlobalInvitationAction.InvitationSuccessListener() {
                                        @Override
                                        public void onInvitationSendSuccess(SyncShareInvitation syncShareInvitation) {
                                            updateBubbleItem.changeShareStatusToWaitInvitation(syncShareInvitation.invitee.id);
                                            updateItemColorIfSame(updateBubbleItem);
                                        }
                                    });
                                }
                            }
                        } else {
                            updateBubbleItem.changeShareStatusToAdd(addParticipantIds);
                            updateItemColorIfSame(updateBubbleItem);
                        }
                    }

                    @Override
                    public void onError(SyncBundleRepository.DataException e) {
                        GlobalInvitationAction.getInstance().handleError(e.status);
                    }
                });
            } else {
                final long userId = SyncManager.getCloudAccountUserId(IdeaPillsApp.getInstance());
                if (userId < 0) {
                    GlobalInvitationAction.getInstance().showLoginDialog(true);
                    return;
                }
                if (!SyncManager.syncEnable(IdeaPillsApp.getInstance())) {
                    GlobalInvitationAction.getInstance().showLoginDialog(false);
                    return;
                }
                changeFromShareColor(userId,mBubbleItem.isShareFromOthers(),mBubbleItem,color);
            }
        } else {
            if (!isShareColor) {
                mBubbleItem.setColor(color);
                setImageBg(mBubbleItem);
                refreshColorStyle();
            }
        }
    }

    private void removeParticipant(final long userId, final boolean isShareFromOthers, final BubbleItem updateBubbleItem, final int color) {
        SyncShareManager.INSTANCE.getInvitationList(new SyncBundleRepository.RequestListener<List<SyncShareInvitation>>() {
            @Override
            public void onRequestStart() {

            }

            @Override
            public void onResponse(List<SyncShareInvitation> response) {
                List<Long> removeParticipantIds = SyncShareUtils.findParticipantIdsForRemove(userId,
                        isShareFromOthers, response);
                if (isShareFromOthers) {
                    SyncShareRepository.removeParticipants(updateBubbleItem.getUserId(), updateBubbleItem.getSyncId(),
                            BubbleItem.buildSharePendingParticipantsString(removeParticipantIds),
                            new SyncBundleRepository.RequestListener<List<Long>>() {
                                @Override
                                public void onRequestStart() {

                                }

                                @Override
                                public void onResponse(List<Long> response) {
                                    SyncShareManager.INSTANCE.addChangeToNotShareFromOtherItem(updateBubbleItem.getCreateAt(),
                                            color);
                                    updateBubbleItem.remove();
                                    if (updateBubbleItem == mBubbleItem) {
                                        deleteItemAnim(BubbleItem.FLAG_NEEDREMOVE);
                                    } else {
                                        GlobalBubbleManager.getInstance().removeBubbleItem(updateBubbleItem);
                                    }
                                }

                                @Override
                                public void onError(SyncBundleRepository.DataException e) {
                                    GlobalInvitationAction.getInstance().handleError(e.status);
                                }
                            });
                } else {
                    updateBubbleItem.changeShareStatusToRemove(removeParticipantIds, color);
                    updateItemColorIfSame(updateBubbleItem);
                }
            }

            @Override
            public void onError(SyncBundleRepository.DataException e) {
                GlobalInvitationAction.getInstance().handleError(e.status);
            }
        });
    }

    private void changeFromShareColor(final long userId, final boolean isShareFromOthers, final BubbleItem updateBubbleItem, final int color) {
        GlobalInvitationAction.getInstance().showDialog(R.string.bubble_notice, R.string.sync_item_share_change_tip,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        removeParticipant(userId, isShareFromOthers, updateBubbleItem, color);
                    }
                });
    }

    private void updateItemColorIfSame(BubbleItem updateBubbleItem) {
        if (updateBubbleItem == mBubbleItem) {
            setImageBg(updateBubbleItem);
            refreshColorStyle();
        }
        if (!updateBubbleItem.isNeedInput() && updateBubbleItem.getId() > 0) {
            GlobalBubbleManager.getInstance().updateBubbleItem(updateBubbleItem);
        }
    }

    private boolean onTodoOverCheckedChanged(boolean isChecked) {
        if(isChecked) {
            if (mBubbleItem.isNeedInput()) {
                if (mBubbleItem.isEmptyBubble() && !mBubbleItem.isAddingAttachment() && !mBubbleItem.isSyncLocked() && !mBubbleItem.isAttachLocked()) {
                    deleteItemAnim(BubbleItem.FLAG_NEEDDELE);
                    return true;
                } else {
                    IdeaPillsApp.getInstance().getBubbleObserverManager().notifyBubbleObserver(mBubbleItem, BubbleItem.MSG_TODO_OVER_INPUT);
                }
                return false;
            }
            CommonUtils.vibrateEffect(mContext, VibEffectSmt.EFFECT_PIN_APP);
            IdeaPillsApp.getInstance().getBubbleObserverManager().notifyBubbleObserver(mBubbleItem, BubbleItem.MSG_TODO_OVER);
        }else{
            IdeaPillsApp.getInstance().getBubbleObserverManager().notifyBubbleObserver(mBubbleItem, BubbleItem.MSG_TODO_OVER_REVERSE);
        }
        return false;
    }

    public void onCheckedChangedInner(View buttonView, boolean isChecked) {
        if (buttonView == mCbCover) {
            mBubbleItem.setSelected(isChecked);
            if (isModeEdit()) {
                if (isChecked) {
                    showdark(false);
                } else {
                    showdark(true);
                }
            }
            IdeaPillsApp.getInstance().getBubbleObserverManager().notifyBubbleObserver(mBubbleItem, BubbleItem.MSG_SELECTED_CHANGE);
            GlobalBubbleUtils.trackBubbleChange(mBubbleItem);
        } else if (buttonView == mCbToDo) {
            if (isChecked) {
                final long current = System.currentTimeMillis();
                mBubbleItem.setToDo(GlobalBubble.TODO_OVER);
                mBubbleItem.setUsedTime(current);
            } else {
                mBubbleItem.setToDo(GlobalBubble.TODO);
                mBubbleItem.setUsedTime(0);
            }
            if (onTodoOverCheckedChanged(isChecked))
                return;
            if (mBubbleItem.hasModificationFlagChanged()) {
                GlobalBubbleManager.getInstance().handleConflict(mBubbleItem);
            }
            GlobalBubbleManager.getInstance().updateBubbleItem(mBubbleItem);
            show(mBubbleItem);
            DataHandler.handleTask(DataHandler.TASK_TODO_OVER_CHANGE);
            GlobalBubbleUtils.trackBubbleChange(mBubbleItem);
        } else {
            log.error("never happen");
        }
    }

    public void stopPlay() {
        mBubbleItemDetailView.stopPlay();
    }

    public void getLeftAndRight(int[] out) {
        out[0] = mCbToDo.getVisibility() == VISIBLE ? mCbToDo.getLeft() : mFllayout.getLeft();
        out[1] = mFllayout.getRight();
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (ev.getAction() == MotionEvent.ACTION_DOWN && needForbidTouch()) {
            return false;
        }
        if (ev.getAction() == MotionEvent.ACTION_DOWN && StatusManager.getStatus(StatusManager.BUBBLE_DRAGGING)) {
            StatusManager.dumpStatus();
            return false;
        }

        if (ev.getAction() == MotionEvent.ACTION_DOWN) {
            mDownX = ev.getX();
            mDownY = ev.getY();
        }

        boolean handle = super.dispatchTouchEvent(ev);
        if (ev.getAction() == MotionEvent.ACTION_DOWN && mBubbleItem != null && mBubbleItem.isNeedInput()) {
            requestDisallowInterceptTouchEvent(true);
            return true;
        }

        return handle;
    }

    public BubbleItem getBubbleItem() {
        return mBubbleItem;
    }

    public Rect getRect() {
        return new Rect(getLeft()+mFllayout.getLeft(), getTop(), getRight(), getBottom());
    }

    public void finishInputting() {
        mBubbleItemDetailView.finishInput();
    }

    private void showdark(boolean dark) {
        if (dark) {
            mllbackground.setAlpha(0.7f);
            mIvBubbleBg.getBackground().setColorFilter(sDarkerColorFilter);
            mBubbleItemDetailView.setPress(sDarkerColorFilter);
        } else {
            mllbackground.setAlpha(1.0f);
            mIvBubbleBg.getBackground().setColorFilter(null);
            mBubbleItemDetailView.clearPress();
        }
    }

    private void updateBgDarkStatus(View bubbleBgView) {
        if (bubbleBgView.getBackground() == null) {
            return;
        }
        if (mllbackground.getAlpha() == 0.7f) {
            bubbleBgView.getBackground().setColorFilter(sDarkerColorFilter);
        } else {
            bubbleBgView.getBackground().setColorFilter(null);
        }
    }

    public void touchDown() {
        if (!needForbidTouch() && mMode != ViewMode.BUBBLE_EDIT
                && BubbleController.getInstance().getCurPptAddBubbleId() < 0) {
            showdark(true);
        }
    }

    public void touchUp() {
        if (!needForbidTouch() && mMode != ViewMode.BUBBLE_EDIT
                && BubbleController.getInstance().getCurPptAddBubbleId() < 0) {
            showdark(false);
        }
    }

    public void updatePptAddFocus(boolean hasFocus) {
        if (mBubbleItemDetailView != null) {
            mBubbleItemDetailView.updatePptAddFocus(hasFocus);
        }
    }

    public Anim getAnimTranslateY() {
        return mAnimTranslateY;
    }

    public void setAnimTranslateY(Anim anim) {
        mAnimTranslateY = anim;
    }

    public void clearAnimTranslateY() {
        if (mAnimTranslateY != null) {
            mAnimTranslateY.cancel();
            mAnimTranslateY = null;
        }
    }

    private ToLargeCallBack mToLargeCallBack = null;

    @Override
    public void onBubbleBoxCheckedChanged(BubbleToDoCheckBox buttonView, boolean isChecked) {
        onCheckedChangedInner(buttonView,isChecked);
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        onCheckedChangedInner(buttonView,isChecked);
    }

    public interface ToLargeCallBack {
        void toLarge(int top, int height);
    }

    public void setToLargeCallBack(ToLargeCallBack toLargeCallBack) {
        mToLargeCallBack = toLargeCallBack;
    }

    public boolean checkInModeChangedTouchArea(int x, int y) {
        if (mIvArrowImg.getVisibility() == VISIBLE) {
            Rect rect = new Rect();
            mIvArrowImg.getHitRect(rect);
            Rect rectXExpand = new Rect(0, rect.top, getWidth(), rect.bottom);
            return rectXExpand.contains(x, y);
        }
        return false;
    }

    public void performArrowClick() {
        if (mBubbleItem.isInLargeMode()) {
            mBubbleItem.setInLargeMode(false);
            toNormal(mBubbleItem, true);
            IdeaPillsApp.getInstance().getBubbleObserverManager().notifyBubbleObserver(mBubbleItem, BubbleItem.MSG_LARGEMODE_CHANGE);
        }
    }

    public void setPivotXY() {
        setPivotX(mFllayout.getLeft() + (mFllayout.getWidth() >> 1));
    }

    public void resetPivotXY() {
        setPivotX((getWidth() >> 1));
    }

    public void onInstalledPackageChanged() {
        mBubbleItemDetailView.onInstalledPackageChanged();
    }
}
