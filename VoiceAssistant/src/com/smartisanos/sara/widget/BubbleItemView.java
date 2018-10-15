
package com.smartisanos.sara.widget;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.TypeEvaluator;
import android.animation.ValueAnimator;
import android.animation.Animator.AnimatorListener;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.app.Activity;
import android.app.SmtPCUtils;
import android.app.Dialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.drawable.AnimationDrawable;
import android.net.Uri;
import android.os.Looper;
import android.service.onestep.GlobalBubble;
import android.service.onestep.GlobalBubbleAttach;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.KeyEvent;
import android.webkit.WebView;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.Toast;
import android.view.View;
import android.view.ViewStub;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Animation.AnimationListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.smartisanos.ideapills.common.anim.Anim;
import com.smartisanos.ideapills.common.anim.AnimListener;
import com.smartisanos.ideapills.common.anim.AnimTimeLine;
import com.smartisanos.ideapills.common.anim.Vector3f;
import com.smartisanos.ideapills.common.model.ShareItem;
import com.smartisanos.ideapills.common.util.CommonUtils;
import com.smartisanos.ideapills.common.util.MultiSdkUtils;
import com.smartisanos.ideapills.common.util.UIHandler;
import com.smartisanos.sanbox.utils.SaraTracker;
import com.smartisanos.sara.widget.listener.IVoiceOperationListener;
import com.smartisanos.sara.util.BubbleSpeechPlayer;
import com.smartisanos.sara.R;
import com.smartisanos.sara.bubble.BubbleActivity;
import com.smartisanos.sara.bubble.manager.BubbleManager;
import com.smartisanos.sara.bubble.manager.BubbleThemeManager;
import com.smartisanos.sara.bubble.view.BubbleColorChooserLayout;
import com.smartisanos.sara.storage.DrawerDataRepository;
import com.smartisanos.sara.util.AnimManager;
import com.smartisanos.sara.util.LogUtils;
import com.smartisanos.sara.util.SaraConstant;
import com.smartisanos.sara.util.SaraUtils;
import com.smartisanos.sara.util.SaraUtils.DialogListener;
import com.smartisanos.sara.util.SaraUtils.WaveType;
import com.smartisanos.sara.util.SoftKeyboardUtil;
import com.smartisanos.sara.util.SoftKeyboardUtil.OnSoftKeyboardChangeListener;
import com.smartisanos.sara.util.ToastUtil;
import com.smartisanos.ideapills.common.util.UIHandler;
import com.smartisanos.sara.util.ViewUtils;

import smartisanos.api.VibEffectSmt;

public class BubbleItemView extends RelativeLayout implements BubbleColorChooserLayout.OnColorClickListener,
        View.OnClickListener, OnCheckedChangeListener, BubbleSpeechPlayer.SpeechPlayerCallBack {
    public static final String TAG = "VoiceAss.BubbleItemView";
    private static final String ACTION_BOOM_TEXT = "smartisanos.intent.action.BOOM_TEXT";
    private WaveView mVBubbleSpeechWave = null;
    private FrameLayout mVBubbleRlWave;
    private LinearLayout mTvBubble;
    private BubbleColorChooserLayout mLLColorChooser;
    private ImageView mIvBubbleBoom = null;
    private ImageView mIvBubblePlay = null;
    private ImageView mIvBubbleWeixin = null;
    private ImageView mIvBubbleAttach = null;
    private ImageView mIvBubbleCalendar = null;
    private ImageView mIvBubbleShare = null;
    private ImageView mIvBubbleInsert = null;
    private ImageView mIvBubbleDelete = null;
    private View mSplitLine;
    private LinearLayout mLoadingLayout;
    private ImageView mLoadingImage;
    private ViewStub mRecycleStub;
    private View mRecycleView;
    private BubbleEditText mTvTitleSmall = null;
    private CheckBox mTodoCheck;
    private LinearLayout mTextContentLayout;
    private RelativeLayout mContainerLayout;

    private View mShareLayout;
    private ShareDrawerLayout mShareDrawerLayout;
    private ImageView mBubbleSignBullet;
    private ImageView mBubbleSignClose;
    private ImageView mBubbleSignCapsult;
    private View mHideView;
    private View mDividerView;

    private GlobalBubble mGlobalBubble;
    private Resources mResources;

    private SaraUtils.BubbleViewChangeListener mListener;
    private SaraUtils.ShowViewChangeListener mShowViewChangeListener;
    private SaraUtils.BulletViewChangeListener mBulletViewChangeListener;
    private IBubbleHeightChangeListener mBubbleHeightChangeListener;
    private DialogListener mDialogListener;
    private BubbleSate mBubbleState;
    private boolean mIsOffLine;
    private BubbleSate mLastBubbleState;
    private LinearLayout mNotificationLayout;
    private TextView mNotiTime;
    private ImageView mNotiIcon;
    private View mVoiceButtonContent;
    private View mVoiceButtonDivider;
    private int mBubbleTextPopupHeight;
    private int mLastHeight;
    private int mBubbleWavMinWidth;
    private int mSmallTargetWidth;
    private int mSmallTargetY;

    public int mPopExpansionMarginBottom;
    private ViewStub mKeyboardStub;
    private View mKeyboardView;
    private TextView mIvBubblefinished;
    private int mEditWavMaxHeight;
    private int mEditTextMaxHeight;
    private boolean mEditState;
    private int mKeyBoardHeight;
    private int mNewLineNum = 1;
    private boolean isEditing = false;
    private int mSmallMinHeight;
    private int mLargeMinHeight;
    private int mRecycleEditMaxHeight;
    private int mEditMarginTop;
    private int mEditMarginBottom;
    private int mNormalMarginBottom;
    private int mBubbleMarginX = -1;
    private int mBubbleInitHeight;
    private int mBubbleInitLeftMargin;
    private int mBubbleInitBottomMargin;
    private boolean mKeyBoardVisible;
    private String bubbleTextTmp = "";

    private OnBubbleClickListener mBubbleClickListener;
    //reference the anim's params, set duration about 300ms for last anim
    int animDura = (int) (300 / (0.18f + 0.1f));
    boolean hasAnim = true;

    private boolean mDefaultShowBulletFlag;

    public enum BubbleSate {
        INIT, SMALL, LARGE, LOADING, NORMAL, RECYCLE, KEYBOARD, NONE
    }

    private AnimatorSet mShowTextAnim;
    private AlphaAnimation mAnimAlpha;

    private List<GlobalBubbleAttach> mBubbleAttaches;
    private BubbleAttachmentLayout mAttachmentLayout;
    private IVoiceOperationListener mVoiceOperationListener;
    private Runnable mLoadImageAnimRunnable = new Runnable() {
        @Override
        public void run() {
            mLoadingLayout.setVisibility(View.VISIBLE);
            AnimationDrawable animation = (AnimationDrawable) mLoadingImage.getBackground();
            animation.stop();
            animation.start();
        }
    };
    private BubbleEditText.EditModeChangeListener mEditModeChangeListener = new BubbleEditText.EditModeChangeListener() {
        @Override
        public void onEditText(BubbleEditText edit, String txt) {
            editText(edit, txt);
        }

        public void onFinishEdit() {
            finishEdit();
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
        super(context, attrs, defStyleAttr);
        initDimens();
    }

    private void finishEdit() {
        if (!mEditState) {
            return;
        }
        isEditing = false;
        showImage(null);
    }

    private void editText(BubbleEditText edit, String txt) {
        if (mGlobalBubble == null) {
            return;
        }
        if (!txt.equals(mGlobalBubble.getText()) && mEditState) {
            mGlobalBubble.setText(txt);
            mGlobalBubble.setTimeStamp(System.currentTimeMillis());
            enterInput(edit);
            updateBubbleToolState(TextUtils.isEmpty(txt));
        }
    }

    private void updateBubbleToolState(boolean isEmpty) {
        if (null == mIvBubbleShare || null == mIvBubbleInsert) {
            return;
        }
        if (isEmpty) {
            mIvBubbleShare.setEnabled(false);
            mIvBubbleShare.setAlpha(0.3f);
            if (mBubbleAttaches != null && mBubbleAttaches.size() > 0) {
                mIvBubbleInsert.setEnabled(true);
                mIvBubbleInsert.setAlpha(1f);
            } else {
                mIvBubbleInsert.setEnabled(false);
                mIvBubbleInsert.setAlpha(0.3f);
            }
        } else {
            mIvBubbleShare.setEnabled(true);
            mIvBubbleShare.setAlpha(1f);
            mIvBubbleInsert.setEnabled(true);
            mIvBubbleInsert.setAlpha(1f);
        }
    }

    /**
     * 外界获取bubbleIte 高度
     *
     * @return
     */
    public int getBubbleItemHeight() {
        int height;
        if (getBubbleState() == BubbleSate.LARGE) {
            height = getExactHeight();
        } else if (getLastBubbleState() == BubbleSate.NORMAL
                || getLastBubbleState() == BubbleSate.LOADING) {
            height = ViewUtils.getExactHeight(this, getLayoutParams().width);
        } else {
            height = getExactHeight();
        }
        return height;
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mRecycleStub = (ViewStub) findViewById(R.id.vs_speech_recycle);
        mKeyboardStub = (ViewStub) findViewById(R.id.vs_speech_text);
        mLLColorChooser = (BubbleColorChooserLayout) findViewById(R.id.ll_colorchooser);
        mTvBubble = (LinearLayout) findViewById(R.id.iv_bubble);
        mTvTitleSmall = (BubbleEditText) findViewById(R.id.tv_title_small);
        mTodoCheck = (CheckBox) findViewById(R.id.cb_todo);
        mContainerLayout = (RelativeLayout) findViewById(R.id.bubble_container);
        mTextContentLayout = (LinearLayout) findViewById(R.id.text_content_layout);
        mIvBubbleBoom = (ImageView) findViewById(R.id.iv_bubble_boom);
        mIvBubbleWeixin = (ImageView) findViewById(R.id.iv_bubble_weixin);
        mIvBubbleAttach = (ImageView) findViewById(R.id.iv_bubble_attach);
        mIvBubbleCalendar = (ImageView) findViewById(R.id.iv_bubble_calendar);
        mIvBubblePlay = (ImageView) findViewById(R.id.iv_bubble_play);
        mIvBubbleShare = (ImageView) findViewById(R.id.iv_bubble_share);
        mIvBubbleInsert = (ImageView) findViewById(R.id.iv_bubble_insert);
        mIvBubbleDelete = (ImageView) findViewById(R.id.iv_bubble_del);
        mShareDrawerLayout = (ShareDrawerLayout) findViewById(R.id.share_drawer_layout);
        mBubbleSignBullet = (ImageView) findViewById(R.id.bubble_sign_bullet);
        mBubbleSignCapsult = (ImageView) findViewById(R.id.bubble_sign_capsult);
        mBubbleSignClose = (ImageView) findViewById(R.id.bubble_sign_close);
        mDividerView = findViewById(R.id.iv_bubble_divider);
        mNotificationLayout = (LinearLayout) findViewById(R.id.notification);
        mNotiTime = (TextView) findViewById(R.id.noti_time);
        mNotiIcon = (ImageView) findViewById(R.id.noti_icon);
        mVBubbleRlWave = (FrameLayout) findViewById(R.id.v_rlwave);
        mVBubbleSpeechWave = (WaveView) findViewById(R.id.v_bubblespeechwave);
        mLoadingLayout = (LinearLayout) findViewById(R.id.load_content);
        mLoadingImage = (ImageView) findViewById(R.id.load_image);
        mSplitLine = findViewById(R.id.view_split_line2);
        mHideView = findViewById(R.id.hide_view);
        mAttachmentLayout = (BubbleAttachmentLayout) findViewById(R.id.bubble_attach_layout);
        mShareLayout = findViewById(R.id.share_layout);
        mLLColorChooser.setOnColorClickListener(this);
        mIvBubbleBoom.setOnClickListener(this);
        mIvBubbleWeixin.setOnClickListener(this);
        mIvBubbleAttach.setOnClickListener(this);
        mIvBubbleCalendar.setOnClickListener(this);
        mIvBubblePlay.setOnClickListener(this);
        mIvBubbleShare.setOnClickListener(this);
        mIvBubbleInsert.setOnClickListener(this);
        mIvBubbleDelete.setOnClickListener(this);
        mTodoCheck.setOnCheckedChangeListener(this);
        if (hasBulletChangeViews()) {
            mBubbleSignBullet.setOnClickListener(this);
            mBubbleSignCapsult.setOnClickListener(this);
            mBubbleSignClose.setOnClickListener(this);
        }
        mVBubbleSpeechWave.setOnClickListener(this);
        mTvTitleSmall.setOnClickListener(this);
        mAttachmentLayout.setAttachmentClickListener(new BubbleAttachmentLayout.onAttachmentClickListener() {
            public void onDeleteClick(GlobalBubbleAttach attachment) {
                mBubbleAttaches.remove(attachment);
                if (mBubbleAttaches.size() == 0) {
                    showShareLayout(VISIBLE);
                }
                if (mBubbleClickListener != null) {
                    mBubbleClickListener.onAttachmentChanged();
                }
                if (null != mBulletViewChangeListener) {
                    if (mBulletViewChangeListener.isCurrentBulletShow()) {
                        mSmallTargetY = mListener.getBubbleSmallTranslation(getBubbleItemHeight());
                        showBubbleItemWithoutAnim();
                    }
                    mBulletViewChangeListener.refreshBulletContactViewHeight(getBubbleItemHeight());
                }
            }

            public void onImageClick(BubbleAttachmentView attachmentView, ArrayList<Uri> localUris) {
                if (mBubbleClickListener != null) {
                    mBubbleClickListener.onImageAttchmentClick(attachmentView.getAttachment(), localUris);
                }
            }

            public void onFileClick(BubbleAttachmentView attachmentView) {
                if (mBubbleClickListener != null) {
                    mBubbleClickListener.onFileClick(attachmentView.getAttachment());
                }
            }

        });
        initForAccessibility();
    }

    public void initDimens() {
        mResources = getResources();
        mBubbleInitHeight = mBubbleTextPopupHeight = getResources().getDimensionPixelSize(R.dimen.bubble_text_popup_height);
        mPopExpansionMarginBottom = mResources.getDimensionPixelSize(R.dimen.pop_expansion_margin_bottom);
        mEditWavMaxHeight = mResources.getDimensionPixelSize(R.dimen.edit_wav_max_height);
        mEditTextMaxHeight = mResources.getDimensionPixelSize(R.dimen.edit_text_max_height);
        mSmallMinHeight = mResources.getDimensionPixelSize(R.dimen.small_min_height_exclude_edit);
        mLargeMinHeight = mResources.getDimensionPixelSize(R.dimen.large_min_height_exclude_edit);
        mRecycleEditMaxHeight = mResources.getDimensionPixelSize(R.dimen.recycle_edit_max_height);
        mEditMarginTop = mResources.getDimensionPixelSize(R.dimen.bubble_edit_margin_top);
        mEditMarginBottom = mResources.getDimensionPixelSize(R.dimen.bubble_edit_margin_bottom);
        mNormalMarginBottom = mResources.getDimensionPixelSize(R.dimen.bubble_normal_margin_bottom);
    }

    public void showShareLayout(int visible) {
        if (visible == VISIBLE || visible == INVISIBLE) {
            boolean isShellBubbleType = SaraUtils.getBubbleType(getContext()) == SaraUtils.BUBBLE_TYPE.SHELL_BUBBLE;
            if (!SaraUtils.isDrawerEnable(getContext()) || isShellBubbleType) {
                visible = GONE;
            } else {
                List<ShareItem> saveShareList = DrawerDataRepository.INSTANCE.getDrawerData();
                if (saveShareList == null || saveShareList.isEmpty()) {
                    visible = GONE;
                }
            }
        }
        if (mShareLayout.getVisibility() != visible) {
            mShareLayout.setVisibility(visible);
        }
    }

    private void updateShareLayout() {
        if (mBubbleState == BubbleSate.SMALL) {
            showShareLayout(VISIBLE);
        } else if (mBubbleState == BubbleSate.LARGE) {
            if (mBubbleAttaches == null || mBubbleAttaches.size() == 0) {
                showShareLayout(VISIBLE);
            } else {
                showShareLayout(GONE);
            }
        } else {
            showShareLayout(GONE);
        }
    }

    public void setBubbleState(BubbleSate state, boolean anim, boolean isSmallEdit, boolean ignoreState) {
        setBubbleState(state, anim, isSmallEdit, ignoreState, false);
    }

    public void setBubbleState(BubbleSate state, boolean anim, boolean isSmallEdit, boolean ignoreState, boolean needScaleAnim) {
        LogUtils.d(TAG, "setBubbleState " + state);
        updateBubbleToolState(mGlobalBubble != null && TextUtils.isEmpty(mGlobalBubble.getText()));
        if (!ignoreState) {
            mLastBubbleState = mBubbleState;
            mBubbleState = state;
            if (mBubbleState != mLastBubbleState || isSmallEdit) {
                updateViewState(state);
            }
        } else {
            updateViewState(state);
        }
        if (!isCurrentBulletShow()) {
            setMaxLineAndHeight();
        }
        if (!anim) {
            return;
        }

        if (mBubbleState == BubbleSate.LOADING || mBubbleState == BubbleSate.NORMAL) {
            showLoadingOrNormalView(mBubbleState);
        } else if (mBubbleState == BubbleSate.SMALL) {
            hideVoiceInputButton();
            if (SmtPCUtils.isValidExtDisplayId(mContext) && !isFocused()) {
                requestFocus();
            }
            int itemHeight = getBubbleItemHeight();
            mSmallTargetY = mListener.getBubbleSmallTranslation(itemHeight);
            showBubbleItemWithAnim(false, isSmallEdit, needScaleAnim);
        } else if (mBubbleState == BubbleSate.LARGE) {
            hideVoiceInputButton();
            if (SmtPCUtils.isValidExtDisplayId(mContext) && !isFocused()) {
                requestFocus();
            }
            int itemHeight = getBubbleItemHeight();
            mSmallTargetY = mListener.getBubbleLargeTranslation(itemHeight);
            if (isCurrentBulletShow() || mDefaultShowBulletFlag) {
                mSmallTargetY = mListener.getBubbleSmallTranslation(itemHeight);
            }
            showBubbleItemWithAnim(false, isSmallEdit, needScaleAnim);
        } else if (mBubbleState == BubbleSate.KEYBOARD) {
            mSmallTargetY = mListener.getBubbleKeyboardTranslation(getKeyboardHeight());
            showBubbleItemWithAnim(true, false, needScaleAnim);
        }
        if (mBubbleState == BubbleSate.INIT) {
            mLastHeight = mBubbleInitHeight;
        }
    }

    public void setGlobalBubble(GlobalBubble globalBubble, boolean offLine) {
        mGlobalBubble = globalBubble;
        mIsOffLine = offLine;
    }

    public void setGlobalBubble(GlobalBubble globalBubble) {
        mGlobalBubble = globalBubble;
    }

    public void setAttachmentList(List<GlobalBubbleAttach> list) {
        this.mBubbleAttaches = list;
        trackBubbleChange(false);
    }

    public void trackBubbleChange(boolean textChange) {
        BubbleManager.trackBubbleChange(getContext(), mGlobalBubble, mBubbleAttaches, textChange);
    }

    public List<GlobalBubbleAttach> getAttachmentList() {
        return mBubbleAttaches;
    }

    public void resetWaveView() {
        if (mVBubbleSpeechWave != null) {
            mVBubbleSpeechWave.reset();
            mVBubbleSpeechWave.removeCallbacks(mUpdateWaveRunnable);
        }
    }

    public BubbleSate getBubbleState() {
        return mBubbleState;
    }

    public BubbleSate getLastBubbleState() {
        return mLastBubbleState;
    }

    public void setText(String text) {
        if (mTvTitleSmall != null) {
            mTvTitleSmall.setText(text);
            if (TextUtils.isEmpty(text)) {
                mIvBubbleBoom.setEnabled(false);
            } else {
                mIvBubbleBoom.setEnabled(true);
            }
            if (TextUtils.isEmpty(text) && mBubbleAttaches != null) {
                final int count = mBubbleAttaches.size();
                if (count > 0) {
                    final String hint = SaraUtils.getHintText(mContext, mBubbleAttaches);
                    mTvTitleSmall.updateHintText(hint);
                    return;
                }
                mTvTitleSmall.updateHintText(getResources().getString(R.string.bubble_hint));
            }
            mTvTitleSmall.updateHintText("");
        }
    }

    public void updateTargetWidth(int displayWidth) {
        mSmallTargetWidth = displayWidth - getResources().getDimensionPixelSize(R.dimen.bubble_left_side) * 2;
    }

    private void showLoadingOrNormalView(BubbleSate bubbleSate) {
        int targetHeight = getSupposeHeight(0);
        if (targetHeight == mLastHeight) {
            return;
        }
        if (mShowTextAnim != null && mShowTextAnim.isRunning()) {
            mShowTextAnim.cancel();
        }
        boolean isLoading = bubbleSate == BubbleSate.LOADING;
        if (targetHeight < mLastHeight && (!SaraUtils.isLeftPopBubble()
                || (mBubbleHeightChangeListener != null && mBubbleHeightChangeListener.needPerformAnimator()))) {
            FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) getLayoutParams();
            params.height = targetHeight;
            setLayoutParams(params);
            if ((mBubbleHeightChangeListener != null && mBubbleHeightChangeListener.needPerformAnimator())) {
                mBubbleHeightChangeListener.updateWaveHeight(targetHeight);
            }
        } else {
            FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) getLayoutParams();
            params.height = mLastHeight;
            setLayoutParams(params);
            mShowTextAnim = showViewWithAlphaAndHeightLargeAnim(mLastHeight, targetHeight, isLoading);
        }
        if (isLoading) {
            mLoadingLayout.postDelayed(mLoadImageAnimRunnable, 140);
        }
    }

    private AnimatorSet showViewWithAlphaAndHeightLargeAnim(int startHight, int targetHeight,
                                                           final boolean handleLoad) {
        AnimatorSet set = new AnimatorSet();
        ObjectAnimator alphaAnimator = ObjectAnimator.ofFloat(this, "alpha", 0f, 1f);
        ValueAnimator heightAnimator = ValueAnimator.ofFloat(startHight, targetHeight);
        heightAnimator.addUpdateListener(new AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float value = (Float) animation.getAnimatedValue();
                FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) getLayoutParams();
                params.height = (int) value;
                setLayoutParams(params);
                mLastHeight = (int) value;
            }
        });

        final ArrayList<Animator> animatorList = new ArrayList<Animator>();
        if (handleLoad) {
            animatorList.add(alphaAnimator);
        }
        animatorList.add(heightAnimator);
        boolean needTransAnim = true;
        if (mBubbleHeightChangeListener != null) {
            if (mBubbleHeightChangeListener.needPerformAnimator()) {
                needTransAnim = false;
                Animator animator = mBubbleHeightChangeListener.getWaveHeightAnimator(targetHeight);
                if (animator != null) {
                    animatorList.add(animator);
                }
            }
        }
        if (SaraUtils.isLeftPopBubble() && needTransAnim) {
            ObjectAnimator translateYAnimator = ObjectAnimator.ofFloat(this, "translationY", getTranslationY(), getTranslationY() + (targetHeight - startHight));
            animatorList.add(translateYAnimator);
        }
        set.playTogether(animatorList);
        set.setDuration(AnimManager.SHOW_BUBBLE_TEXT_CONTENT_DURATION);
        set.addListener(new AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {
                mTvTitleSmall.setVerticalScrollBarEnabled(false);
                mTvTitleSmall.setAlpha(0);
                setVisibility(View.VISIBLE);
            }

            @Override
            public void onAnimationRepeat(Animator animation) {
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                mTvTitleSmall.setVerticalScrollBarEnabled(true);
                mTvTitleSmall.setAlpha(1);
                BubbleItemView.this.setAlpha(1);
                clearAnimation();
            }

            @Override
            public void onAnimationCancel(Animator animation) {
                mTvTitleSmall.setVerticalScrollBarEnabled(true);
                mTvTitleSmall.setAlpha(1);
                BubbleItemView.this.setAlpha(1);
            }
        });
        set.start();
        return set;
    }

    public int getExactHeight() {
        return getSupposeHeight(mSmallTargetWidth);
    }

    private int getSupposeHeight(int width) {
        int bottomMargin = ((FrameLayout.LayoutParams) getLayoutParams()).bottomMargin;
        int height = ((FrameLayout.LayoutParams) getLayoutParams()).height;
        ((FrameLayout.LayoutParams) getLayoutParams()).bottomMargin = 0;
        ((FrameLayout.LayoutParams) getLayoutParams()).height = LayoutParams.WRAP_CONTENT;
        int targetHeight = width > 0
                ? ViewUtils.getExactHeight(BubbleItemView.this, width)
                : ViewUtils.getSupposeHeight(BubbleItemView.this);
        ((FrameLayout.LayoutParams) getLayoutParams()).bottomMargin = bottomMargin;
        ((FrameLayout.LayoutParams) getLayoutParams()).height = height;
        return targetHeight;
    }

    private AnimatorSet mShowBubbleViewAnimSet;

    public boolean isAnimSetRunning() {
        return mShowBubbleViewAnimSet != null && mShowBubbleViewAnimSet.isRunning();
    }

    public void cancelAnimSet() {
        if (mShowBubbleViewAnimSet != null) {
            mShowBubbleViewAnimSet.cancel();
        }
    }

    private void showBubbleItemWithoutAnim() {
        mTvTitleSmall.setVerticalScrollBarEnabled(true);
        mTvBubble.clearAnimation();
        if (mKeyboardView != null) {
            mKeyboardView.clearAnimation();
        }
        clearAnimation();
        FrameLayout.LayoutParams bubbleParams = (FrameLayout.LayoutParams) getLayoutParams();
        bubbleParams.width = mSmallTargetWidth;
        bubbleParams.height = getExactHeight();
        if (SaraUtils.isLeftPopBubble()) {
            bubbleParams.leftMargin = getResources().getDimensionPixelSize(R.dimen.bubble_left_side);
        } else {
            bubbleParams.leftMargin = getResources().getDimensionPixelSize(R.dimen.bubble_item_margin_left);
        }
        bubbleParams.rightMargin = getResources().getDimensionPixelSize(R.dimen.bubble_item_margin_left);
        bubbleParams.bottomMargin = 0;
        setLayoutParams(bubbleParams);
        setTranslationY(mSmallTargetY);
    }

    private void showBubbleItemWithAnim(boolean keyboard, final boolean isSmallEdit, boolean needScaleAnim) {
        mShowBubbleViewAnimSet = new AnimatorSet();
        ObjectAnimator translateAnimator = ObjectAnimator.ofFloat(this, "translationY", getTranslationY(), mSmallTargetY);
        int duration = 300;
        translateAnimator.setDuration(duration);
        FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) getLayoutParams();
        final int mStartWidth = needScaleAnim ? 0 : params.width;
        final int mEndWidth = mSmallTargetWidth;
        int exactHeight = getExactHeight();
        int startHeight = Math.max(getResources().getDimensionPixelOffset(R.dimen.bubble_start_height), getHeight());
        final int mStartHeight = needScaleAnim ? 0 : startHeight;
        final int mEndHeight = exactHeight;
        final int mStartMarginX = params.leftMargin;
        final int mEndMarginX = mBubbleMarginX != -1 ? mBubbleMarginX
                : SaraUtils.isLeftPopBubble() ? getResources().getDimensionPixelSize(R.dimen.bubble_left_side)
                : getResources().getDimensionPixelSize(R.dimen.bubble_item_margin_left);
        final int mStartBottomMargin = params.bottomMargin;
        final int mEndBottomMargin = 0;
        ValueAnimator paramAnimator = ValueAnimator.ofFloat(0, 1);
        paramAnimator.addUpdateListener(new AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float value = (Float) animation.getAnimatedValue();
                FrameLayout.LayoutParams bubbleParams = (FrameLayout.LayoutParams) getLayoutParams();
                bubbleParams.width = (int) (mStartWidth + value * (mEndWidth - mStartWidth));
                bubbleParams.height = (int) (mStartHeight + value * (mEndHeight - mStartHeight));
                bubbleParams.leftMargin = bubbleParams.rightMargin = (int) (mStartMarginX + value * (mEndMarginX - mStartMarginX));
                bubbleParams.bottomMargin = (int) (mStartBottomMargin + value * (mEndBottomMargin - mStartBottomMargin));
                setLayoutParams(bubbleParams);
            }
        });
        paramAnimator.setDuration(duration);

        ObjectAnimator mTvBubbleAlphaAnimator = ObjectAnimator.ofFloat(mTvBubble, "alpha", mTvBubble.getAlpha(), 1f);
        mTvBubbleAlphaAnimator.setDuration(100);

        ObjectAnimator mBubbleAlphaAnimator = null;
        if (keyboard) {
            mBubbleAlphaAnimator = ObjectAnimator.ofFloat(this, "alpha", getAlpha(), 1f);
            mBubbleAlphaAnimator.setDuration(100);
        }

        mShowBubbleViewAnimSet.setInterpolator(new DecelerateInterpolator(1.5f));

        ObjectAnimator mKeyboardAlphaAnimator = null;
        if (mKeyboardView != null) {
            mKeyboardAlphaAnimator = ObjectAnimator.ofFloat(mKeyboardView, "alpha", mKeyboardView.getAlpha(), 1f);
            mKeyboardAlphaAnimator.setDuration(100);
        }
        final ArrayList<Animator> animatorList = new ArrayList<Animator>();
        animatorList.add(translateAnimator);
        animatorList.add(paramAnimator);
        if (keyboard) {
            animatorList.add(mKeyboardAlphaAnimator);
            animatorList.add(mBubbleAlphaAnimator);
        } else {
            animatorList.add(mTvBubbleAlphaAnimator);
        }
        mShowBubbleViewAnimSet.playTogether(animatorList);
        mShowBubbleViewAnimSet.addListener(new AnimatorListener() {

            @Override
            public void onAnimationStart(Animator animation) {
                mTvTitleSmall.setVerticalScrollBarEnabled(false);
                if (getAlpha() != 1f) {
                    setAlpha(1f);
                }
            }

            @Override
            public void onAnimationRepeat(Animator animation) {
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                mTvTitleSmall.setVerticalScrollBarEnabled(true);
                mTvBubble.clearAnimation();
                if (mKeyboardView != null) {
                    mKeyboardView.clearAnimation();
                }
                clearAnimation();
            }

            @Override
            public void onAnimationCancel(Animator animation) {
            }
        });
        mShowBubbleViewAnimSet.start();
    }

    public void hideViewWithScaleAnim(PointF point, int targetWidth, int targetHeight, int translateX, int targetY) {
        final float initX = getX();
        final float initY = getY();
        final int initWidth = getLayoutParams().width;
        final int initHeight = getLayoutParams().height;
        AnimatorSet set = new AnimatorSet();
        Point start = new Point();
        start.set(initWidth, initHeight);
        Point end = new Point();
        end.set(targetWidth, targetHeight);
        ValueAnimator mValueAnimator = ValueAnimator.ofObject(mTypeEvaluator, start, end);
        mValueAnimator.setDuration(200);
        mValueAnimator.setInterpolator(new DecelerateInterpolator(1.5f));
        mValueAnimator.addUpdateListener(new AnimatorUpdateListener() {
            public void onAnimationUpdate(ValueAnimator animation) {
                Point point = (Point) animation.getAnimatedValue();
                FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) getLayoutParams();
                params.width = (int) point.x;
                params.height = (int) point.y;
                setLayoutParams(params);
            }
        });

        ObjectAnimator editAlphaAnimator = ObjectAnimator.ofFloat(mTextContentLayout, "alpha", 1, 0);
        editAlphaAnimator.setDuration(200);

        final float bubbleTranslateY = getTranslationY();
        ObjectAnimator translateAnimator = ObjectAnimator.ofFloat(this, "translationY", bubbleTranslateY, bubbleTranslateY - (initHeight - targetHeight) / 2);
        translateAnimator.setDuration(200);

        translateAnimator.setInterpolator(new DecelerateInterpolator(1.5f));
        ObjectAnimator editTranslateAnimator = ObjectAnimator.ofFloat(mTextContentLayout, "translationY", 0, -36);
        editTranslateAnimator.setDuration(200);
        editTranslateAnimator.setInterpolator(new DecelerateInterpolator(1.5f));

        ObjectAnimator editTranslateYAnimator = ObjectAnimator.ofFloat(mTextContentLayout, "translationX", 0, -10);
        editTranslateYAnimator.setDuration(200);
        editTranslateYAnimator.setInterpolator(new DecelerateInterpolator(1.5f));

        ObjectAnimator translateXAnimator = null;
        translateXAnimator = ObjectAnimator.ofFloat(this, "translationX", 0, translateX);

        translateXAnimator.setDuration(200);
        ObjectAnimator alphaAnimator = ObjectAnimator.ofFloat(this, "alpha", 1, 0);
        alphaAnimator.setStartDelay(100);
        alphaAnimator.setDuration(100);
        alphaAnimator.setInterpolator(new DecelerateInterpolator(1.5f));
        set.addListener(new AnimatorListener() {

            @Override
            public void onAnimationStart(Animator animation) {
                mLLColorChooser.setAlpha(0);
                mSplitLine.setAlpha(0);
                mVBubbleRlWave.setAlpha(0);
                mTvBubble.setAlpha(0);
                if (mAttachmentLayout != null && mAttachmentLayout.getVisibility() == VISIBLE) {
                    mAttachmentLayout.setAlpha(0);
                }
            }

            @Override
            public void onAnimationRepeat(Animator animation) {
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                resetBubble(initX, initY, initWidth, initHeight, bubbleTranslateY);
            }

            @Override
            public void onAnimationCancel(Animator animation) {
            }
        });
        set.playTogether(mValueAnimator, translateXAnimator, alphaAnimator, editAlphaAnimator, translateAnimator, editTranslateAnimator);
        set.start();
        mShowBubbleViewAnimSet = set;
    }

    private void resetBubble(float initX, float initY, int initWidth, int initHeight, float bubbleTranslateY) {
        setVisibility(View.GONE);
        setX(initX);
        setY(initY);
        setTranslationY(bubbleTranslateY);
        if (SaraUtils.isLeftPopBubble()) {
            setTranslationX(150);
        } else {
            setTranslationX(0);
        }
        FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) getLayoutParams();
        params.width = initWidth;
        params.height = initHeight;
        setLayoutParams(params);
        setAlpha(1);

        mTextContentLayout.setAlpha(1);
        mTextContentLayout.setTranslationY(0);
        mTextContentLayout.setTranslationX(0);

        mLLColorChooser.setAlpha(1);
        mVBubbleRlWave.setAlpha(1);
        mTvBubble.setAlpha(1);
        mSplitLine.setAlpha(1);
        if (mAttachmentLayout != null && mAttachmentLayout.getVisibility() == VISIBLE) {
            mAttachmentLayout.setAlpha(1);
        }
        setTranslationX(0);
        setTranslationY(0);
    }

    private TypeEvaluator<Point> mTypeEvaluator = new TypeEvaluator<Point>() {
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

    public void setBulletViewChangeListener(SaraUtils.BulletViewChangeListener listener) {
        this.mBulletViewChangeListener = listener;
    }

    public void setShowViewChangeListener(SaraUtils.ShowViewChangeListener listener) {
        this.mShowViewChangeListener = listener;
    }

    public void setViewListener(SaraUtils.BubbleViewChangeListener listener) {
        mListener = listener;
    }

    public void setDialogListener(DialogListener listener) {
        mDialogListener = listener;
    }

    public void setSoftListener(final Activity activity) {
        final View view = activity.getWindow().getDecorView();
        SoftKeyboardUtil.observeSoftKeyboard(view, new OnSoftKeyboardChangeListener() {
            @Override
            public void onSoftKeyBoardChange(int softKeybardHeight, boolean visible) {
                mKeyBoardVisible = visible;
                if (visible) {
                    mKeyBoardHeight = softKeybardHeight;
                }
                View focuseView = activity.getCurrentFocus();
                if (focuseView != null && getVisibility() == VISIBLE && focuseView instanceof BubbleEditText) {
                    mTvTitleSmall = (BubbleEditText) focuseView;
                    if (!isCurrentBulletShow()) {
                        setMaxLineAndHeight();
                        mListener.editView(visible, mBubbleState == BubbleSate.SMALL);
                    }
                } else if (focuseView != null && focuseView instanceof WebView) {
                    if (visible) {
                        final InputMethodManager imm = InputMethodManager.peekInstance();
                        if (imm != null && imm.isActive()) {
                            mListener.hideView(2, null, false, false);
                        }
                    }
                }
            }
        });
    }

    public void setGravityLeft() {
        if (mTvTitleSmall != null) {
            mTvTitleSmall.setGravity(Gravity.LEFT);
        }
    }

    public void hideSoftInputFromWindow() {
        InputMethodManager imm = (InputMethodManager)
                getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(getWindowToken(), 0);
    }

    public void showSoftInputFromWindow() {
        BubbleEditText editText = getEditText();
        if (editText != null && mTvBubble != null && mTvBubble.getVisibility() != View.VISIBLE) {
            checkInput(editText, true);
            editText.callInputMethodDelay(0);
        }
    }

    public void checkInput(boolean enter) {
        BubbleEditText editText = getEditText();
        if (editText != null) {
            checkInput(editText, enter);
        }
    }

    public void checkInput(EditText editText, boolean enter) {
        if (enter) {
            editText.setFocusable(true);
            editText.setFocusableInTouchMode(true);
            editText.requestFocus();
            updateEditState(true);
        } else {
            editText.setFocusable(false);
            editText.setFocusableInTouchMode(false);
            updateEditState(false);
        }
    }

    private void updateEditState(boolean edit) {
        if (mEditState != edit) {
            mEditState = edit;
            if (SmtPCUtils.isValidExtDisplayId(mContext)) {
                mListener.editView(mEditState, mBubbleState == BubbleSate.SMALL);
            }
        }
    }

    public String getEditTextString() {
        BubbleEditText editText = getEditText();
        if (editText != null) {
            return editText.getText().toString();
        }
        return null;
    }

    private BubbleEditText getEditText() {
        if (mBubbleState == BubbleSate.SMALL || mBubbleState == BubbleSate.LARGE
                || mBubbleState == BubbleSate.KEYBOARD) {
            return mTvTitleSmall;
        }
        return null;
    }

    public void setEditable(boolean enabled) {
        if (mTvTitleSmall != null) {
            mTvTitleSmall.setEnabled(enabled);
        }
    }

    public int getKeyboardHeight() {
        return mKeyBoardHeight;
    }

    public void setMaxLineAndHeight() {
        int temp = 0;
        int maxHeight = 0;
        if (mBubbleState == BubbleSate.RECYCLE) {
            maxHeight = mRecycleEditMaxHeight - mRecycleEditMaxHeight % mTvTitleSmall.getLineHeight();
        } else {
            int minHeight = 0;
            if (mEditState) {
                if (mGlobalBubble == null || mGlobalBubble.getUri() == null) {
                    minHeight = mSmallMinHeight;
                    temp = getRootView().getHeight() - mKeyBoardHeight - minHeight;
                    maxHeight = temp - temp % mTvTitleSmall.getLineHeight()
                            - ((LinearLayout.LayoutParams) mTvTitleSmall.getLayoutParams()).bottomMargin;
                } else {
                    minHeight = mLargeMinHeight;
                    if (mShareLayout.getVisibility() != GONE) {
                        minHeight = getResources().getDimensionPixelOffset(R.dimen.share_drawer_large_min_height_exclude_edit);
                    }
                    temp = getRootView().getHeight() - mKeyBoardHeight - minHeight;
                    maxHeight = temp - ((LinearLayout.LayoutParams) mTvTitleSmall.getLayoutParams()).bottomMargin - mAttachmentLayout.getHeight();
                }
            } else {
                if (mGlobalBubble == null || mGlobalBubble.getUri() == null) {
                    minHeight = mEditTextMaxHeight;
                } else {
                    minHeight = mEditWavMaxHeight;
                }
                maxHeight = minHeight - minHeight % mTvTitleSmall.getLineHeight();
            }
        }
        mTvTitleSmall.setMaxHeight(maxHeight);
    }

    private void enterInput(EditText edit) {
        isEditing = true;
        int oldLineNum = mNewLineNum;
        mNewLineNum = edit.getLineCount() > 0 ? edit.getLineCount() : 1;
        int translateY = (mNewLineNum - oldLineNum) * edit.getLineHeight();
        FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) getLayoutParams();
        if (mNewLineNum > oldLineNum) {
            if (edit.getMeasuredHeight() + translateY < edit.getMaxHeight()) {
                params.height += translateY;
            } else if (mNewLineNum >= oldLineNum + 1) {
                if (edit.getMaxHeight() >= edit.getMeasuredHeight()) {
                    params.height += (edit.getMaxHeight() - edit.getMeasuredHeight());
                } else {
                    params.height -= edit.getMeasuredHeight() - edit.getMaxHeight();
                }
            }
        } else if (mNewLineNum < oldLineNum) {
            if (mNewLineNum < oldLineNum - 1) {
                if (mNewLineNum * edit.getLineHeight() - translateY < edit.getMaxHeight()) {
                    params.height += translateY;
                } else if (mNewLineNum * edit.getLineHeight() <= edit.getMaxHeight() && oldLineNum * edit.getLineHeight() > edit.getMaxHeight()) {
                    params.height -= (edit.getMaxHeight() - mNewLineNum * edit.getLineHeight() - 50);
                }

            } else {
                if (edit.getMeasuredHeight() < edit.getMaxHeight()) {
                    params.height += translateY;
                } else if (edit.getMeasuredHeight() == edit.getMaxHeight()
                        && (mNewLineNum + 1) * edit.getLineHeight() < edit.getMaxHeight()) {
                    params.height -= edit.getMaxHeight() - (mNewLineNum + 1) * edit.getLineHeight();
                }
            }
        } else if (mNewLineNum == oldLineNum) {
            if (edit.getMaxHeight() <= edit.getMeasuredHeight()) {
                params.height -= edit.getMeasuredHeight() - edit.getMaxHeight();
            }
        }
        setLayoutParams(params);
        if (isCurrentBulletShow()) {
            if (mNewLineNum != oldLineNum) {
                int itemHeight = getBubbleItemHeight();
                mSmallTargetY = mListener.getBubbleSmallTranslation(itemHeight);
                showBubbleItemWithoutAnim();
                mBulletViewChangeListener.refreshBulletContactViewHeight(itemHeight);
            }
        }
    }

    private void setImageBg(View view, int color) {
        view.setBackgroundResource(BubbleThemeManager.getBackgroudRes(color, BubbleThemeManager.BACKGROUND_BUBBLE_LARGE));
        mTvTitleSmall.setShadeColor(color);
        mTvTitleSmall.setTextColor(getResources().getColor(BubbleThemeManager.getBackgroudRes(color, BubbleThemeManager.BACKGROUND_BUBBLE_TEXT_COLOR)));
        mTvTitleSmall.modifyCursorDrawable(BubbleThemeManager.getBackgroudRes(color, BubbleThemeManager.BACKGROUND_BUBBLE_CURSOR_ICON));
        if (mIvBubblefinished != null) {
            mIvBubblefinished.setTextColor(getResources().getColor(BubbleThemeManager.getBackgroudRes(mGlobalBubble.getColor(), BubbleThemeManager.BACKGROUND_BUBBLE_TEXT_COLOR)));
        }
        mIvBubbleBoom.setImageResource(BubbleThemeManager.getBackgroudRes(color, BubbleThemeManager.BACKGROUND_BUBBLE_BOOM_ICON));
        mIvBubbleAttach.setImageResource(BubbleThemeManager.getBackgroudRes(color, BubbleThemeManager.BACKGROUND_BUBBLE_ATTACHMENT_ICON));
        mIvBubbleCalendar.setImageResource(BubbleThemeManager.getBackgroudRes(color, BubbleThemeManager.BACKGROUND_BUBBLE_CALENDAR_ICON));
        mIvBubbleShare.setImageResource(BubbleThemeManager.getBackgroudRes(color, BubbleThemeManager.BACKGROUND_BUBBLE_SHARE_ICON));
        mIvBubbleInsert.setImageResource(BubbleThemeManager.getBackgroudRes(color, BubbleThemeManager.BACKGROUND_BUBBLE_INSERT_ICON));
        if (mIvBubblePlay != null) {
            mIvBubblePlay.setImageResource(BubbleThemeManager.getBackgroudRes(color, BubbleThemeManager.BACKGROUND_BUBBLE_PLAER_ICON));
        }
        prepareWave();
        if (mNotiTime != null && mGlobalBubble != null) {
            mNotiTime.setTextColor(getResources().getColor(BubbleThemeManager.getBackgroudRes(mGlobalBubble.getColor(), BubbleThemeManager.BACKGROUND_BUBBLE_TEXT_COLOR)));
        }
        if (mNotiIcon != null && mNotiIcon.getVisibility() == VISIBLE && mGlobalBubble != null) {
            mNotiIcon.setImageResource(BubbleThemeManager.getBackgroudRes(mGlobalBubble.getColor(), BubbleThemeManager.BACKGROUND_UNFOLD_REMIND_ICON));
        }
        resetShareColorViewWeight(color);
    }

    @Override
    public void setBackgroundResource(int resid) {
        findViewById(R.id.bubble_container).setBackgroundResource(resid);
    }

    public void changeColor2Share() {
        mGlobalBubble.setColor(GlobalBubble.COLOR_SHARE);
        mGlobalBubble.setShareStatus(GlobalBubble.SHARE_STATUS_ONE_TO_ONE);
        setImageBg(this, GlobalBubble.COLOR_SHARE);
    }

    private void changeColor2Normal(int color) {
        mGlobalBubble.setColor(color);
        mGlobalBubble.setShareStatus(GlobalBubble.SHARE_STATUS_NOT_SHARED);
        setImageBg(this, color);
    }

    @Override
    public void onColorClick(int color) {
        if (color == GlobalBubble.COLOR_SHARE) {
            if (BubbleManager.changeBubbleColor2ShareJumpIfNeed(mListener.getActivityContext(), mGlobalBubble, mSharedDialogCallback)) {
                changeColor2Share();
            }
        } else {
            changeColor2Normal(color);
        }
        CommonUtils.vibrateEffect(getContext(), VibEffectSmt.EFFECT_SWITCH);
    }

    private BubbleManager.SharedDialogCallback mSharedDialogCallback = new BubbleManager.SharedDialogCallback() {
        private Dialog dialog;

        @Override
        public void sharedDialogHandler(Dialog dialog) {
            this.dialog = dialog;
        }

        @Override
        public void dismissDialog() {
            if (null != dialog) {
                dialog.dismiss();
                dialog = null;
            }
        }
    };

    public void dismissShareDialogIfNeed() {
        if (null != mSharedDialogCallback) {
            mSharedDialogCallback.dismissDialog();
        }
    }

    private void updateViewState(BubbleSate state) {
        updateBeforeStateChange();
        switch (state) {
            case INIT:
                toInitState();
                break;
            case SMALL:
                toSmallState();
                break;
            case LARGE:
                toLargeState();
                break;
            case LOADING:
                toLoadingState();
                break;
            case NORMAL:
                toNormalState();
                break;
            case RECYCLE:
                toRecycleState();
                break;
            case KEYBOARD:
                toKeyboardState();
                break;
        }
        updateAfterStateChange();
    }

    private void updateBeforeStateChange() {
        updateShareLayout();
        if (mKeyboardView != null) {
            mKeyboardView.setVisibility(View.GONE);
        }
        if (SaraUtils.isLeftPopBubble()) {
            LayoutParams containerParam = (LayoutParams) mContainerLayout.getLayoutParams();
            if (mBubbleState == BubbleSate.LOADING || mBubbleState == BubbleSate.NORMAL) {
                containerParam.leftMargin = getResources().getDimensionPixelOffset(R.dimen.loading_bubble_left_margin);
            } else if (mBubbleState == BubbleSate.INIT) {
                containerParam.leftMargin = mBubbleInitLeftMargin;
            } else {
                containerParam.leftMargin = getResources().getDimensionPixelOffset(R.dimen.nomal_bubble_left_margin);
            }
            mContainerLayout.setLayoutParams(containerParam);
        }
        LayoutParams mTextContentLayoutParams = (LayoutParams) mTextContentLayout.getLayoutParams();
        mTextContentLayoutParams.topMargin = mEditMarginTop;
        mTextContentLayoutParams.bottomMargin = 0;//mEditMarginBottom;
        mTextContentLayoutParams.rightMargin = 0;
        mTextContentLayoutParams.leftMargin = 0;
        mTextContentLayout.setLayoutParams(mTextContentLayoutParams);
    }

    private void updateAfterStateChange() {
        if (mBubbleState == BubbleSate.LARGE || mBubbleState == BubbleSate.SMALL || mBubbleState == BubbleSate.RECYCLE) {
            if (BubbleSpeechPlayer.getInstance(getContext()).isPlayingBubble(mGlobalBubble)) {
                mIvBubblePlay.setImageResource(BubbleThemeManager.getBackgroudRes(mGlobalBubble.getColor(), BubbleThemeManager.BACKGROUND_BUBBLE_PAUSE_ICON));
            } else {
                mIvBubblePlay.setImageResource(BubbleThemeManager.getBackgroudRes(mGlobalBubble.getColor(), BubbleThemeManager.BACKGROUND_BUBBLE_PLAER_ICON));
            }
            showText();
        }
    }

    private void toInitState() {
        clearSwitchAnimation();
        initShareDrawerView();
        refreshRemindView();
        refreshAttachmentView();
        if (hasBulletChangeViews()) {
            if (isCurrentBulletShow()) {
                mBubbleSignCapsult.setVisibility(View.VISIBLE);
                mBubbleSignBullet.setVisibility(View.INVISIBLE);
            } else {
                mBubbleSignCapsult.setVisibility(View.INVISIBLE);
                mBubbleSignBullet.setVisibility(View.VISIBLE);
            }
        }
        mSplitLine.setVisibility(View.GONE);
        mTextContentLayout.setVisibility(GONE);
        mTvTitleSmall.setVisibility(View.GONE);
        mTodoCheck.setVisibility(VISIBLE);
        mLoadingLayout.setVisibility(View.GONE);
        setTranslationY(0);
        mHideView.setVisibility(View.VISIBLE);
        mTvBubble.setVisibility(View.GONE);
        mLLColorChooser.setVisibility(View.GONE);
        mVBubbleRlWave.setVisibility(View.GONE);
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        int bubbleInitHeight = mBubbleInitHeight;
        params.width = mBubbleWavMinWidth;
        params.height = bubbleInitHeight;
        if (SaraUtils.isLeftPopBubble()) {
            int bottomMargin = mBubbleInitBottomMargin > 0 ? mBubbleInitBottomMargin : SaraUtils.getLeftBubbleBottom(mContext);
            params.gravity = Gravity.LEFT | Gravity.BOTTOM;
            params.rightMargin = params.leftMargin = getResources().getDimensionPixelSize(R.dimen.left_wave_result_margin_left_popup);
            if (mBubbleHeightChangeListener != null && mBubbleHeightChangeListener.needPerformAnimator()) {
                bubbleInitHeight = mBubbleTextPopupHeight;
            }
            params.bottomMargin = bottomMargin - (bubbleInitHeight - mBubbleTextPopupHeight);
        } else {
            params.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
            int bottomMargin = 0;
            if (SaraUtils.isNavigationBarMode(mContext)) {
                bottomMargin = getResources().getDimensionPixelSize(R.dimen.navigation_bar_wave_result_margin_bottom);
            } else {
                bottomMargin = getResources().getDimensionPixelSize(R.dimen.wave_result_margin_bottom);
            }
            params.bottomMargin = bottomMargin;
        }
        setLayoutParams(params);
        int height = mTvTitleSmall.getSingleLineHeight();
        LinearLayout.LayoutParams param = (LinearLayout.LayoutParams) mTodoCheck.getLayoutParams();
        param.height = height;
        mTodoCheck.setLayoutParams(param);
    }

    private void toLoadingState() {
        if (hasBulletChangeViews()) {
            mBubbleSignCapsult.setVisibility(View.INVISIBLE);
            mBubbleSignBullet.setVisibility(View.INVISIBLE);
            mBubbleSignClose.setVisibility(View.INVISIBLE);
        }
        mTvBubble.setVisibility(View.GONE);
        mHideView.setVisibility(View.VISIBLE);
        if (mVoiceButtonDivider != null) {
            mVoiceButtonDivider.setVisibility(View.VISIBLE);
        }
        mSplitLine.setVisibility(View.GONE);
        RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) mLoadingLayout.getLayoutParams();
        if (mVoiceButtonContent == null || mVoiceButtonContent.getVisibility() != View.VISIBLE) {
            params.height = getResources().getDimensionPixelSize(R.dimen.bubble_load_anim_height);
            params.bottomMargin = mNormalMarginBottom - getResources().getDimensionPixelOffset(R.dimen.revone_bubble_load_margin_bottom);
        } else {
            params.height = getResources().getDimensionPixelSize(R.dimen.revone_bubble_load_anim_height);
            params.bottomMargin = 0;
        }
        mLoadingLayout.setLayoutParams(params);
        mLoadingLayout.setVisibility(View.VISIBLE);
        mVBubbleRlWave.setVisibility(View.GONE);
        setImageBg(BubbleItemView.this, SaraUtils.getDefaultBubbleColor(getContext()));
    }

    private void toSmallState() {
        if (hasBulletChangeViews()) {
            if (!isCurrentBulletShow()) {
                mBubbleSignBullet.setVisibility(View.VISIBLE);
                mBubbleSignBullet.setAlpha(1);
                mBubbleSignCapsult.setVisibility(View.INVISIBLE);
                mBubbleSignCapsult.setAlpha(0);
            }
        }
        mBubbleSignClose.setVisibility(View.VISIBLE);
        mLoadingLayout.setVisibility(View.GONE);
        View keyboardView = getKeyboardView();
        keyboardView.findViewById(R.id.ll_inputopt).setVisibility(View.GONE);
        mHideView.setVisibility(View.GONE);
        mTextContentLayout.setVisibility(VISIBLE);
        mTvTitleSmall.setVisibility(View.VISIBLE);
        mTodoCheck.setVisibility(VISIBLE);
        mTvTitleSmall.setGravity(Gravity.LEFT);
        mTvTitleSmall.registerEditModeChangeListener(mEditModeChangeListener);
        needInput(mTvTitleSmall, false, true, true);
        if (mGlobalBubble.getUri() == null) {
            mVBubbleRlWave.setVisibility(View.GONE);
        } else {
            mVBubbleRlWave.setVisibility(View.VISIBLE);
        }
        //hide weixin
        if (SaraUtils.checkAPP(getContext(), SaraConstant.PACKAGE_NAME_WEIXIN)) {
            mDividerView.setVisibility(View.GONE);
            mIvBubbleWeixin.setVisibility(View.GONE);
        } else {
            mDividerView.setVisibility(View.GONE);
            mIvBubbleWeixin.setVisibility(View.GONE);
        }
        mTvBubble.setVisibility(View.VISIBLE);
        mSplitLine.setVisibility(View.VISIBLE);
        mLLColorChooser.setVisibility(View.VISIBLE);
        setImageBg(BubbleItemView.this, mGlobalBubble.getColor());
    }

    private void toLargeState() {
        mHideView.setVisibility(View.GONE);
        mTvTitleSmall.setGravity(Gravity.LEFT);
        needInput(mTvTitleSmall, false, true, false);
        mTextContentLayout.setVisibility(VISIBLE);
        mTvTitleSmall.setVisibility(View.VISIBLE);
        if (hasBulletChangeViews()) {
            if (!isCurrentBulletShow()) {
                mBubbleSignBullet.setVisibility(View.VISIBLE);
                mBubbleSignBullet.setAlpha(1);
                mBubbleSignCapsult.setVisibility(View.INVISIBLE);
                mBubbleSignCapsult.setAlpha(0);
            }
            mBubbleSignClose.setVisibility(View.VISIBLE);
        }
        mTodoCheck.setVisibility(VISIBLE);
        mTvTitleSmall.registerEditModeChangeListener(mEditModeChangeListener);

        View keyboardView = getKeyboardView();
        keyboardView.setVisibility(View.GONE);
        if (mLoadingLayout != null) {
            mLoadingLayout.setVisibility(View.GONE);
        }

        mTvBubble.setVisibility(View.VISIBLE);
        mSplitLine.setVisibility(View.VISIBLE);
        if (mGlobalBubble.getUri() == null) {
            mVBubbleRlWave.setVisibility(View.GONE);
        } else {
            mVBubbleRlWave.setVisibility(View.VISIBLE);
        }
        //hide weixin
        if (SaraUtils.checkAPP(getContext(), SaraConstant.PACKAGE_NAME_WEIXIN)) {
            mDividerView.setVisibility(View.GONE);
            mIvBubbleWeixin.setVisibility(View.GONE);
        } else {
            mDividerView.setVisibility(View.GONE);
            mIvBubbleWeixin.setVisibility(View.GONE);
        }
        mLLColorChooser.setVisibility(View.VISIBLE);
        setImageBg(BubbleItemView.this, mGlobalBubble.getColor());
    }

    private void toNormalState() {
        if (hasBulletChangeViews()) {
            mBubbleSignCapsult.setVisibility(View.INVISIBLE);
            mBubbleSignBullet.setVisibility(View.INVISIBLE);
            mBubbleSignClose.setVisibility(View.INVISIBLE);
        }
        mSplitLine.setVisibility(View.GONE);
        mLoadingLayout.setVisibility(View.GONE);
        mLoadingLayout.removeCallbacks(mLoadImageAnimRunnable);
        mLLColorChooser.setVisibility(View.GONE);
        mTvBubble.setVisibility(View.GONE);
        mVBubbleRlWave.setVisibility(View.GONE);
        int color = SaraUtils.getDefaultBubbleColor(mContext);
        LayoutParams params = (LayoutParams) mTextContentLayout.getLayoutParams();
        if (SaraUtils.isLeftPopBubble()) {
            if (mVoiceButtonContent == null || mVoiceButtonContent.getVisibility() != View.VISIBLE) {
                params.topMargin = mEditMarginBottom - getResources().getDimensionPixelOffset(R.dimen.bubble_normal_margin_top);
                params.bottomMargin = mNormalMarginBottom - getResources().getDimensionPixelOffset(R.dimen.bubble_normal_margin_top);
            } else {
                params.topMargin = 0;
                params.bottomMargin = 0;
            }
        }
        params.leftMargin = getResources().getDimensionPixelOffset(color == GlobalBubble.COLOR_SHARE ?
                R.dimen.bubble_normal_share_margin_left : R.dimen.bubble_normal_margin_left);
        mTextContentLayout.setLayoutParams(params);
        mTextContentLayout.setVisibility(VISIBLE);
        mTvTitleSmall.setGravity(Gravity.LEFT);
        mTvTitleSmall.setVisibility(View.VISIBLE);
        mTodoCheck.setVisibility(GONE);
        mHideView.setVisibility(View.VISIBLE);
        if (mVoiceButtonDivider != null) {
            mVoiceButtonDivider.setVisibility(View.VISIBLE);
        }
        setImageBg(BubbleItemView.this, color);
    }

    private void toRecycleState() {
        setMaxLineAndHeight();
        if (hasBulletChangeViews()) {
            mBubbleSignCapsult.setVisibility(View.INVISIBLE);
            mBubbleSignBullet.setVisibility(View.INVISIBLE);
            mBubbleSignClose.setVisibility(View.INVISIBLE);
        }
        mLoadingLayout.setVisibility(View.GONE);
        mHideView.setVisibility(View.GONE);
        mTvBubble.setVisibility(View.GONE);
        mLLColorChooser.setVisibility(View.GONE);
        mTextContentLayout.setVisibility(VISIBLE);
        mTvTitleSmall.setVisibility(View.VISIBLE);
        mTodoCheck.setVisibility(GONE);
        refreshAttachmentView();
        refreshRemindView();
        LayoutParams params = (LayoutParams) mTextContentLayout.getLayoutParams();
        if (mGlobalBubble.getColor() == GlobalBubble.COLOR_SHARE) {
            params.leftMargin = getResources().getDimensionPixelSize(R.dimen.recycle_bubble_left_share);
        } else {
            params.leftMargin = getResources().getDimensionPixelSize(R.dimen.recycle_bubble_left);
        }
        mTextContentLayout.setLayoutParams(params);
        if (mAttachmentLayout != null && mAttachmentLayout.getVisibility() == View.VISIBLE) {
            LayoutParams attachParam = (LayoutParams) mAttachmentLayout.getLayoutParams();
            if (mGlobalBubble.getColor() == GlobalBubble.COLOR_SHARE) {
                attachParam.leftMargin = getResources().getDimensionPixelSize(R.dimen.attachment_share_left_margin);
            } else {
                attachParam.leftMargin = getResources().getDimensionPixelSize(R.dimen.recycle_bubble_left_attachment);
            }
            if (mGlobalBubble.getType() == GlobalBubble.TYPE_TEXT) {
                attachParam.bottomMargin = getResources().getDimensionPixelSize(R.dimen.attachment_margin_bottom);
            }
            mAttachmentLayout.setLayoutParams(attachParam);
        }
        if (mGlobalBubble.getType() == GlobalBubble.TYPE_TEXT) {
            mVBubbleRlWave.setVisibility(View.GONE);
        } else {
            mVBubbleRlWave.setVisibility(View.VISIBLE);
        }
        mSplitLine.setVisibility(View.VISIBLE);
        mTvTitleSmall.setGravity(Gravity.LEFT);
        checkInput(mTvTitleSmall, false);

        if (mRecycleView == null) {
            mRecycleView = mRecycleStub.inflate();
        }
        mRecycleView.setVisibility(View.VISIBLE);
        LayoutParams recycleParam = (LayoutParams) mRecycleView.getLayoutParams();
        if (mGlobalBubble.getColor() == GlobalBubble.COLOR_SHARE) {
            recycleParam.leftMargin = getResources().getDimensionPixelSize(R.dimen.wave_view_left_margin);
        } else {
            recycleParam.leftMargin = getResources().getDimensionPixelSize(R.dimen.wave_view_full_left_margin);
        }
        mRecycleView.setLayoutParams(recycleParam);
        TextView mIvBubbleDelete = (TextView) findViewById(R.id.iv_bubble_delete);
        TextView mIvBubbleRestore = (TextView) findViewById(R.id.iv_bubble_restore);
        int textColor = BubbleThemeManager.getBackgroudRes(mGlobalBubble.getColor(), BubbleThemeManager.BACKGROUND_BUBBLE_TEXT_COLOR);
        mIvBubbleDelete.setTextColor(getResources().getColor(textColor));
        mIvBubbleRestore.setTextColor(getResources().getColor(textColor));
        mIvBubbleDelete.setOnClickListener(this);
        mIvBubbleRestore.setOnClickListener(this);
        setImageBg(BubbleItemView.this, mGlobalBubble.getColor());
    }

    private void toKeyboardState() {
        mHideView.setVisibility(View.GONE);
        mTextContentLayout.setVisibility(VISIBLE);
        mTvTitleSmall.setVisibility(View.VISIBLE);
        mTodoCheck.setVisibility(VISIBLE);
        mTvTitleSmall.setGravity(Gravity.LEFT);
        mTvTitleSmall.requestFocus();
        mTvTitleSmall.registerEditModeChangeListener(mEditModeChangeListener);
        mTvTitleSmall.setText("");
        mTvTitleSmall.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                checkInput(mTvTitleSmall, true);
                mNewLineNum = mTvTitleSmall.getLineCount();
                mTvTitleSmall.callInputMethodDelay(0);
            }
        });
        if (mGlobalBubble.getTimeStamp() == 0) {
            mGlobalBubble.setTimeStamp(System.currentTimeMillis());
        }
        mVBubbleRlWave.setVisibility(View.GONE);
        mTvBubble.setVisibility(View.GONE);
        mLoadingLayout.setVisibility(View.GONE);
        mSplitLine.setVisibility(View.VISIBLE);
        setAlpha(0);
        mLLColorChooser.setVisibility(View.VISIBLE);
        if (mKeyboardView == null) {
            mKeyboardView = mKeyboardStub.inflate();
        }
        mKeyboardView.setVisibility(View.VISIBLE);
        setVisibility(View.VISIBLE);
        View mIvBubbleCancel = findViewById(R.id.tv_cancel);
        View mIvBubbleFinish = findViewById(R.id.tv_finish);
        mIvBubbleCancel.setVisibility(View.VISIBLE);
        mIvBubbleCancel.setOnClickListener(this);
        mIvBubbleFinish.setOnClickListener(this);
        setImageBg(BubbleItemView.this, mGlobalBubble.getColor());
    }

    private View getKeyboardView() {
        if (mKeyboardView == null) {
            mKeyboardView = mKeyboardStub.inflate();
            mIvBubblefinished = (TextView) mKeyboardView.findViewById(R.id.tv_finish);
            mIvBubblefinished.setOnClickListener(this);
            findViewById(R.id.tv_cancel).setOnClickListener(this);
        }
        return mKeyboardView;
    }

    private void initForAccessibility() {
        setContentDescriptionForView(findViewById(R.id.iv_color_blue), mResources.getString(R.string.note));
        setContentDescriptionForView(findViewById(R.id.iv_color_red), mResources.getString(R.string.important));
        setContentDescriptionForView(findViewById(R.id.iv_color_orange), mResources.getString(R.string.todo));
        setContentDescriptionForView(findViewById(R.id.iv_color_green), mResources.getString(R.string.message));
        setContentDescriptionForView(findViewById(R.id.iv_color_purple), mResources.getString(R.string.inspiration));
        setContentDescriptionForView(findViewById(R.id.iv_color_share), mResources.getString(R.string.share_ideapill));
        setContentDescriptionForView(findViewById(R.id.iv_color_navy_blue), mResources.getString(R.string.text_ppt));

        setContentDescriptionForView(mIvBubblePlay, mResources.getString(R.string.bubble_play_voice));
        setContentDescriptionForView(mIvBubbleDelete, mResources.getString(R.string.btn_delete));
        setContentDescriptionForView(mIvBubbleBoom, mResources.getString(R.string.bubble_boom));
        setContentDescriptionForView(mIvBubbleWeixin, mResources.getString(R.string.bubble_weixin));
        setContentDescriptionForView(mIvBubbleAttach, mResources.getString(R.string.bubble_attach));
        setContentDescriptionForView(mIvBubbleCalendar, mResources.getString(R.string.bubble_calendar));
        setContentDescriptionForView(findViewById(R.id.iv_bubble_share), mResources.getString(R.string.menu_share));
        setContentDescriptionForView(mIvBubbleInsert, mResources.getString(R.string.bubble_save));
    }

    private void setContentDescriptionForView(View v, String text) {
        if (v != null) {
            v.setContentDescription(text);
        }
    }

    private void prepareWave() {
        if (mGlobalBubble == null) {
            return;
        }
        if (mGlobalBubble.getType() != GlobalBubble.TYPE_TEXT) {
            if (mVBubbleSpeechWave != null) {
                mVBubbleSpeechWave.setVisibility(View.VISIBLE);
            }
            BubbleSpeechPlayer.getInstance(getContext()).prepareData(mGlobalBubble);
            if (mVBubbleSpeechWave != null && (mBubbleState == BubbleSate.LARGE || mBubbleState == BubbleSate.RECYCLE || mBubbleState == BubbleSate.SMALL)) {
                mVBubbleSpeechWave.setWaveType(WaveType.RESULT_WAVE);
                showImage(null);
            }
        } else {
            if (mVBubbleSpeechWave != null) {
                mVBubbleSpeechWave.setVisibility(View.GONE);
            }

            showImage(null);
        }
    }

    private void showSearchView() {
        showSearchView(null);
    }

    private void showSearchView(final GlobalBubble globalBubble) {
        mBubbleSignBullet.setVisibility(View.INVISIBLE);
        mBubbleSignCapsult.setVisibility(View.VISIBLE);

        final AnimTimeLine timeLine = new AnimTimeLine();
        Anim rotateAnimBullet = new Anim(mBubbleSignBullet, Anim.ROTATE, (int) (animDura * 0.15), Anim.CUBIC_OUT, new Vector3f(0, -180, 0), new Vector3f());
        Anim rotateAnimCapsult = new Anim(mBubbleSignCapsult, Anim.ROTATE, (int) (animDura * 0.15), Anim.CUBIC_OUT, new Vector3f(0, -180, 0), new Vector3f());
        Anim scaleAnimBigBullet = new Anim(mBubbleSignBullet, Anim.SCALE, (int) (animDura * 0.18), Anim.CUBIC_OUT, new Vector3f(1f, 1f), new Vector3f(1.3f, 1.3f));
        Anim scaleAnimBigCapsult = new Anim(mBubbleSignCapsult, Anim.SCALE, (int) (animDura * 0.18), Anim.CUBIC_OUT, new Vector3f(1f, 1f), new Vector3f(1.3f, 1.3f));
        Anim scaleAnimSmallBullet = new Anim(mBubbleSignBullet, Anim.SCALE, (int) (animDura * 0.1), Anim.QUAD_OUT, new Vector3f(1.3f, 1.3f), new Vector3f(1f, 1f));
        scaleAnimSmallBullet.setDelay((int) (animDura * 0.18));
        Anim scaleAnimSmallCapsult = new Anim(mBubbleSignCapsult, Anim.SCALE, (int) (animDura * 0.1), Anim.QUAD_OUT, new Vector3f(1.3f, 1.3f), new Vector3f(1f, 1f));
        scaleAnimSmallCapsult.setDelay((int) (animDura * 0.18));

        rotateAnimBullet.setListener(new AnimListener() {
            @Override
            public void onStart() {
            }

            @Override
            public void onComplete(int type) {
            }

            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                float value = valueAnimator.getAnimatedFraction();
                if (value >= 0.5f) {
                    mBubbleSignBullet.setVisibility(View.VISIBLE);
                    mBubbleSignCapsult.setVisibility(View.INVISIBLE);
                }
            }
        });

        if (mBubbleState == BubbleSate.LARGE && !mEditState) {
            int targetY = 0;
            if (mListener != null) {
                targetY = mListener.getBubbleLargeTranslation(getBubbleItemHeight());
            }
            Anim translateAnimBubble = new Anim(BubbleItemView.this, Anim.TRANSLATE, 300, Anim.CUBIC_OUT, new Vector3f(0, getTranslationY()), new Vector3f(0, targetY));
            timeLine.addAnim(translateAnimBubble);
        }
        timeLine.addAnim(rotateAnimBullet);
        timeLine.addAnim(rotateAnimCapsult);
        timeLine.addAnim(scaleAnimBigBullet);
        timeLine.addAnim(scaleAnimBigCapsult);
        timeLine.addAnim(scaleAnimSmallBullet);
        timeLine.addAnim(scaleAnimSmallCapsult);
        timeLine.setAnimListener(new AnimListener() {
            @Override
            public void onStart() {
            }

            @Override
            public void onComplete(int type) {
                mBubbleSignBullet.setVisibility(View.VISIBLE);
                mBubbleSignCapsult.setVisibility(View.INVISIBLE);
                mBubbleSignBullet.setScaleX(1f);
                mBubbleSignBullet.setScaleY(1f);
                mBubbleSignCapsult.setScaleX(1f);
                mBubbleSignCapsult.setScaleY(1f);

                LayoutParams lp = (LayoutParams) mContainerLayout.getLayoutParams();
                lp.height = LayoutParams.WRAP_CONTENT;
                mContainerLayout.setLayoutParams(lp);
            }

            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
            }
        });
        timeLine.start();
        if (mShowViewChangeListener != null) {
            mShowViewChangeListener.onShowSearchView();
        }

    }

    public void clearSwitchAnimation() {
        setTranslationY(0);
        if (hasBulletChangeViews()) {
            mBubbleSignCapsult.setVisibility(View.INVISIBLE);
            mBubbleSignBullet.setVisibility(View.VISIBLE);
            mBubbleSignCapsult.setAlpha(1.0f);
            mBubbleSignBullet.setAlpha(1.0f);
        }

        LayoutParams lp = (LayoutParams) mContainerLayout.getLayoutParams();
        lp.height = LayoutParams.WRAP_CONTENT;
        mContainerLayout.setLayoutParams(lp);
    }

    private void showBulletView() {
        setVisibility(VISIBLE);
        mBubbleSignBullet.setVisibility(View.VISIBLE);
        mBubbleSignCapsult.setVisibility(View.INVISIBLE);

        final AnimTimeLine timeLine = new AnimTimeLine();
        Anim rotateAnimBullet = new Anim(mBubbleSignBullet, Anim.ROTATE, (int) (animDura * 0.15), Anim.CUBIC_OUT, new Vector3f(), new Vector3f(0, -180, 0));
        Anim rotateAnimCapsult = new Anim(mBubbleSignCapsult, Anim.ROTATE, (int) (animDura * 0.15), Anim.CUBIC_OUT, new Vector3f(), new Vector3f(0, -180, 0));
        Anim scaleAnimBigBullet = new Anim(mBubbleSignBullet, Anim.SCALE, (int) (animDura * 0.18), Anim.CUBIC_OUT, new Vector3f(1f, 1f), new Vector3f(1.3f, 1.3f));
        Anim scaleAnimBigCapsult = new Anim(mBubbleSignCapsult, Anim.SCALE, (int) (animDura * 0.18), Anim.CUBIC_OUT, new Vector3f(1f, 1f), new Vector3f(1.3f, 1.3f));
        Anim scaleAnimSmallBullet = new Anim(mBubbleSignBullet, Anim.SCALE, (int) (animDura * 0.1), Anim.QUAD_OUT, new Vector3f(1.3f, 1.3f), new Vector3f(1f, 1f));
        scaleAnimSmallBullet.setDelay((int) (animDura * 0.18));
        Anim scaleAnimSmallCapsult = new Anim(mBubbleSignCapsult, Anim.SCALE, (int) (animDura * 0.1), Anim.QUAD_OUT, new Vector3f(1.3f, 1.3f), new Vector3f(1f, 1f));
        scaleAnimSmallCapsult.setDelay((int) (animDura * 0.18));
        rotateAnimBullet.setListener(new AnimListener() {
            @Override
            public void onStart() {
            }

            @Override
            public void onComplete(int type) {
            }

            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                float value = valueAnimator.getAnimatedFraction();
                if (value >= 0.5f) {
                    mBubbleSignBullet.setVisibility(View.INVISIBLE);
                    mBubbleSignCapsult.setVisibility(View.VISIBLE);
                }
            }
        });

        if (mBubbleState == BubbleSate.LARGE) {
            int targetY = 0;
            if (mListener != null) {
                targetY = mListener.getBubbleSmallTranslation(getBubbleItemHeight());
            }
            Anim translateAnimBubble = new Anim(BubbleItemView.this, Anim.TRANSLATE, 300, Anim.CUBIC_OUT, new Vector3f(0, getTranslationY()), new Vector3f(0, targetY));
            timeLine.addAnim(translateAnimBubble);
        }
        timeLine.addAnim(rotateAnimBullet);
        timeLine.addAnim(rotateAnimCapsult);
        timeLine.addAnim(scaleAnimBigBullet);
        timeLine.addAnim(scaleAnimBigCapsult);
        timeLine.addAnim(scaleAnimSmallBullet);
        timeLine.addAnim(scaleAnimSmallCapsult);
        timeLine.setAnimListener(new AnimListener() {
            @Override
            public void onStart() {
            }

            @Override
            public void onComplete(int type) {
                mBubbleSignBullet.setVisibility(View.INVISIBLE);
                mBubbleSignCapsult.setVisibility(View.VISIBLE);
                mBubbleSignBullet.setScaleX(1f);
                mBubbleSignBullet.setScaleY(1f);
                mBubbleSignCapsult.setScaleX(1f);
                mBubbleSignCapsult.setScaleY(1f);

                LayoutParams lp = (LayoutParams) mContainerLayout.getLayoutParams();
                lp.height = LayoutParams.WRAP_CONTENT;
                mContainerLayout.setLayoutParams(lp);

                bubbleTextTmp = mGlobalBubble.getText();
            }

            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
            }
        });
        timeLine.start();
        //切换viewPager
        if (mShowViewChangeListener != null) {
            mShowViewChangeListener.onShowBulletView();
        }
    }

    public void setDefaultShowBulletFlag(boolean defaultShowBulletFlag) {
        mDefaultShowBulletFlag = defaultShowBulletFlag;
    }

    public void consumeDefaultShowBulletFlag() {
        if (mDefaultShowBulletFlag) {
            mDefaultShowBulletFlag = false;
            this.hasAnim = false;
            if (null != mBubbleSignBullet) {
                mBubbleSignBullet.performClick();
            }
        }
    }

    private void showBulletViewWithoutAnim() {

        mBubbleSignCapsult.setVisibility(View.VISIBLE);
        mBubbleSignCapsult.setAlpha(1);

        mBubbleSignBullet.setVisibility(View.INVISIBLE);
        mBubbleSignBullet.setAlpha(0);

        //切换viewPager
        if (mShowViewChangeListener != null) {
            mShowViewChangeListener.onShowBulletViewForDefaultSetting();
        }

        LayoutParams lp = (LayoutParams) mContainerLayout.getLayoutParams();
        lp.height = LayoutParams.WRAP_CONTENT;
        mContainerLayout.setLayoutParams(lp);

        bubbleTextTmp = mGlobalBubble.getText();
    }

    private void showImage(BubbleSpeechPlayer.VoiceInfo info) {
        LogUtils.d(TAG, "showImage info:" + info + " ,mGlobalBubble:" + mGlobalBubble);

        if (mGlobalBubble == null || mGlobalBubble.getType() == GlobalBubble.TYPE_TEXT) {
            return;
        }
        if (info == null) {
            info = BubbleSpeechPlayer.getInstance(getContext()).getVoiceInfo(mGlobalBubble,
                    new BubbleSpeechPlayer.VoiceInfoPrepareCallBack() {
                        public void prepared(final BubbleSpeechPlayer.VoiceInfo info) {
                            if (info.getGlobalBubble().equals(mGlobalBubble)) {
                                post(new Runnable() {
                                    @Override
                                    public void run() {
                                        showImage(info);
                                    }
                                });
                            }
                        }
                    });
            if (info != null) {
                showImage(info);
            }
        } else {
            if (mVBubbleSpeechWave != null) {
                mVBubbleSpeechWave.setMaxDuration(info.getDura());
                mVBubbleSpeechWave.setPaintColor(mGlobalBubble.getColor());
                mVBubbleSpeechWave.setWaveData(info.getImgData());
            }
            updateSpeechWaveView();
        }
    }

    private void playSpeech() {
        BubbleSpeechPlayer.getInstance(getContext()).playSpeech(mGlobalBubble, this);
        mIvBubblePlay.setImageDrawable(mResources.getDrawable(BubbleThemeManager.getBackgroudRes(mGlobalBubble.getColor(), BubbleThemeManager.BACKGROUND_BUBBLE_PAUSE_ICON)));
        if (mAnimAlpha != null) {
            mAnimAlpha.cancel();
        }
        mAnimAlpha = new AlphaAnimation(1, 0);
        mAnimAlpha.setDuration(150);
        mAnimAlpha.setInterpolator(new DecelerateInterpolator(1.5f));
        mAnimAlpha.setAnimationListener(new AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
                mVBubbleSpeechWave.setShowMiddle(true);
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                mIvBubblePlay.setAlpha(1.0f);
                mIvBubblePlay.setVisibility(View.INVISIBLE);
                mIvBubblePlay.clearAnimation();
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }

        });
        mIvBubblePlay.startAnimation(mAnimAlpha);
        updateSpeechWaveView();
    }

    public void stopPlay() {
        if (BubbleSpeechPlayer.getInstance(getContext()).isPlayingBubble(mGlobalBubble)) {
            BubbleSpeechPlayer.getInstance(getContext()).stop();
            updateSpeechWaveView();
        }
        mIvBubblePlay.setImageDrawable(mResources.getDrawable(BubbleThemeManager.getBackgroudRes(mGlobalBubble.getColor(), BubbleThemeManager.BACKGROUND_BUBBLE_PLAER_ICON)));
    }

    @Override
    public void onCompleted(GlobalBubble item) {
        if (item == mGlobalBubble) {
            if (Looper.getMainLooper() != Looper.myLooper()) {
                UIHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        stopPlay();
                    }
                });
            } else {
                stopPlay();
            }
        }
    }

    @Override
    public void onDisconnected(GlobalBubble item) {
        onCompleted(item);
    }

    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.iv_bubble_boom:
                boolean isJumpTextBoomSuccess = BubbleManager.jump2TextBoom(
                        mListener.getActivityContext(), mGlobalBubble, mIvBubbleBoom,
                        new BubbleManager.ITextBoomFinishedListener() {
                            @Override
                            public void onTextBoomFinished(String newText, String oldText, boolean isTextChanged) {
                                if (isTextChanged) {
                                    setMaxLineAndHeight();
                                    showText();
                                    mListener.loadResultForKeyboard(mGlobalBubble, false, false);
                                }
                            }
                        });
                if (isJumpTextBoomSuccess) {
                    trackClickEvent("A420007");
                }
                break;
            case R.id.iv_bubble_play:
                if (BubbleSpeechPlayer.getInstance(getContext()).isPlayingBubble(mGlobalBubble)) {
                    stopPlay();
                } else {
                    playSpeech();
                    BubbleSpeechPlayer.getInstance(getContext()).showMuteTipIfNeeded(getContext());
                }
                trackClickEvent("A420008");
                break;
            case R.id.iv_bubble_share:
                BubbleManager.shareBubble(getContext(), mGlobalBubble, mBubbleAttaches);
                trackClickEvent("A420009");
                break;
            case R.id.iv_bubble_weixin:
                BubbleManager.shareBubble2Weixin(getContext(), mGlobalBubble);
                break;
            case R.id.iv_bubble_attach:
                //TODO
                SoftKeyboardUtil.hideInputMethod(BubbleItemView.this);
                mBubbleClickListener.onAddAttachmentClick();
                break;
            case R.id.iv_bubble_calendar:
                if (mListener != null) {
                    int windowPosition = -1;
                    if (SmtPCUtils.isValidExtDisplayId(mContext)) {
                        int loc[] = new int[2];
                        loc = getLocationOnScreen();
                        windowPosition = loc[0] + getWidth();
                    }
                    BubbleManager.jump2SetRemind(mListener.getActivityContext(), mGlobalBubble, windowPosition);
                }
                break;
            case R.id.iv_bubble_insert:
                if (SmtPCUtils.isValidExtDisplayId(getContext()) && !SaraUtils.isSettingEnable(getContext())) {
                    ToastUtil.showToast(getContext(), R.string.ideapills_not_opened, Toast.LENGTH_SHORT);
                    return;
                }
                BubbleManager.markAddBubble2List(true);
                PointF pointF = addBubble2SideBar(false);
                mListener.hideView(0, pointF, true, true);
                SaraTracker.onEvent("A420012");
                break;
            case R.id.iv_bubble_del:
                BubbleManager.markAddBubble2List(false);
                stopPlay();
                if (isCurrentBulletShow()) {
                    finishActivity();
                } else {
                    mListener.hideView(0, null, false, false);
                }
                mListener.deleteVoice(mGlobalBubble);
                trackClickEvent("A420006");
                break;
            case R.id.iv_bubble_delete:
                mDialogListener.onBubbleDelete();
                break;
            case R.id.iv_bubble_restore:
                stopPlay();
                mDialogListener.onBubbleRestore();
                break;
            case R.id.tv_cancel:
                BubbleManager.markAddBubble2List(false);
                setVisibility(View.GONE);
                hideSoftInputFromWindow();
                if (mListener != null) {
                    SaraUtils.finishActivity(mListener.getActivityContext());
                }
                break;
            case R.id.tv_finish:
                if (mBubbleState == BubbleSate.KEYBOARD) {
                    mGlobalBubble.setText(mTvTitleSmall.getText().toString());
                    updateEditState(false);
                    setVisibility(View.GONE);
                    mListener.loadResultForKeyboard(mGlobalBubble, false, true);
                    hideSoftInputFromWindow();
                } else {
                    needInput(mTvTitleSmall, false, false, mBubbleState == BubbleSate.SMALL, false);
                }
                break;
            case R.id.v_bubblespeechwave:
                stopPlay();
                break;
            case R.id.bubble_sign_bullet:

                if (mEditState) {
                    return;
                }
                if (hasAnim) {
                    showBulletView();
                } else {
                    hasAnim = true;
                    showBulletViewWithoutAnim();
                }
                break;
            case R.id.bubble_sign_capsult:
                if (null != mBulletViewChangeListener) {
                    mBulletViewChangeListener.refreshBulletContactViewHeight(getBubbleItemHeight());
                }
                showSearchView();
                if (!mTvTitleSmall.hasFocus()) {
                    checkInput(false);
                    hideSoftInputFromWindow();
                    if (!mGlobalBubble.getText().equals(bubbleTextTmp)) {
                        mListener.loadResultForKeyboard(mGlobalBubble, false, false);
                    }
                } else {
                    checkInput(true);
                    showKeyboardViewWithAnim();
                    if (!mKeyBoardVisible) {
                        showSoftInputFromWindow();
                    } else {
                        mListener.editView(true, true);
                    }
                }
                break;
            case R.id.bubble_sign_close:
                finishActivity();
                break;
            case R.id.tv_title_small:
                if (mGlobalBubble.getTimeStamp() == 0) {
                    mGlobalBubble.setTimeStamp(System.currentTimeMillis());
                }
                if (mGlobalBubble.getType() == GlobalBubble.TYPE_VOICE_OFFLINE) {
                    mGlobalBubble.setType(GlobalBubble.TYPE_VOICE);
                }
                if (mIvBubblefinished != null) {
                    mIvBubblefinished.setTextColor(getResources().getColor(
                            BubbleThemeManager.getBackgroudRes(mGlobalBubble.getColor(), BubbleThemeManager.BACKGROUND_BUBBLE_TEXT_COLOR)));
                }
                mTvTitleSmall.modifyCursorDrawable(BubbleThemeManager.getBackgroudRes(mGlobalBubble.getColor(), BubbleThemeManager.BACKGROUND_BUBBLE_CURSOR_ICON));
                needInput(mTvTitleSmall, true, false, mBubbleState == BubbleSate.SMALL);
                mNewLineNum = mTvTitleSmall.getLineCount();
                break;
            default:
                break;
        }
    }

    private void finishActivity() {
        if (mListener != null) {
            mListener.getActivityContext().finish();
        }
    }


    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        if (buttonView == mTodoCheck && mGlobalBubble != null) {
            if (isChecked) {
                final long current = System.currentTimeMillis();
                mGlobalBubble.setToDo(GlobalBubble.TODO_OVER);
                mGlobalBubble.setUsedTime(current);
                CommonUtils.vibrateEffect(mContext, VibEffectSmt.EFFECT_PIN_APP);
            } else {
                mGlobalBubble.setToDo(GlobalBubble.TODO);
                mGlobalBubble.setUsedTime(0);
            }
            toDoOver(isChecked);
            trackBubbleChange(false);
        }
    }

    private void trackClickEvent(String eventId) {
        LinkedHashMap<String, Object> trackerData = new LinkedHashMap<String, Object>();
        trackerData.put("source", 0);
        SaraTracker.onEvent(eventId, trackerData);
    }

    private void showText() {
        String text = mGlobalBubble != null ? mGlobalBubble.getText() : "";
        setText(text);
    }

    private void needInput(BubbleEditText editText, boolean flag, boolean init, boolean isSmallEdit) {
        needInput(editText, flag, init, isSmallEdit, false);
    }

    private void needInput(BubbleEditText editText, boolean flag, boolean init, boolean isSmallEdit, boolean needScaleAnim) {
        if (flag) {
            if (!mEditState) {
                if (!isCurrentBulletShow()) {
                    View keyboardView = getKeyboardView();
                    keyboardView.findViewById(R.id.tv_cancel).setVisibility(View.GONE);
                    keyboardView.findViewById(R.id.iv_bubble_line3).setVisibility(View.GONE);
                    AnimManager.showViewWithAlphaAnim(mKeyboardView, 250);
                    mTvBubble.setVisibility(View.GONE);
                }
                checkInput(editText, true);
                editText.callInputMethodDelay(0);
            }
        } else {
            if (mEditState) {
                mEditState = false;
                if (!init) {
                    setVisibility(View.GONE);
                    trackBubbleChange(isEditing);
                    mGlobalBubble.setText(editText.getText().toString());
                    mListener.loadResultForKeyboard(mGlobalBubble, isSmallEdit, needScaleAnim);
                    if (!isCurrentBulletShow()) {
                        mTvBubble.setVisibility(View.VISIBLE);
                        mKeyboardView.setVisibility(View.GONE);
                    }
                    hideSoftInputFromWindow();
                }
            }
            checkInput(editText, false);
        }
    }

    private void showKeyboardViewWithAnim() {
        View keyboardView = getKeyboardView();
        keyboardView.findViewById(R.id.tv_cancel).setVisibility(View.GONE);
        keyboardView.findViewById(R.id.iv_bubble_line3).setVisibility(View.GONE);
        AnimManager.showViewWithAlphaAnim(mKeyboardView, 250);
        mTvBubble.setVisibility(View.GONE);
    }

    public boolean isKeyboardViewShow() {
        if (mKeyboardView != null) {
            return mKeyboardView.getVisibility() == View.VISIBLE;
        }
        return false;
    }

    public PointF addBubble2SideBar(boolean anim) {
        boolean hasAttachment = mBubbleAttaches != null && mBubbleAttaches.size() > 0;
        LogUtils.d(TAG, "mGlobalBubble = " + mGlobalBubble);
        if (mGlobalBubble != null && (!TextUtils.isEmpty(mGlobalBubble.getText()) || hasAttachment)) {
            return BubbleManager.addBubble2SideBar(getContext(), mGlobalBubble, mBubbleAttaches, mIsOffLine, anim);
        }
        return null;
    }

    public void toDoOver(boolean isOver) {
        if (mTvTitleSmall != null) {
            mTvTitleSmall.toDoOver(isOver);
        }
    }

    public void refreshAttachmentView() {
        if (mAttachmentLayout == null) {
            LogUtils.e(TAG, "mAttachmentLayout mustn't be null ");
            return;
        }
        mAttachmentLayout.setAttachmentList(mBubbleAttaches);
        if (mBubbleAttaches != null && mBubbleAttaches.size() > 0) {
            mAttachmentLayout.setVisibility(View.VISIBLE);
            RelativeLayout.LayoutParams attachParam = (RelativeLayout.LayoutParams) mAttachmentLayout.getLayoutParams();
            if (mAttachmentLayout.getAlpha() == 0) {
                mAttachmentLayout.setAlpha(1);
            }
            if (mGlobalBubble != null && mGlobalBubble.getColor() == GlobalBubble.COLOR_SHARE) {
                attachParam.leftMargin = getResources().getDimensionPixelSize(R.dimen.attachment_share_left_margin);
            } else {
                attachParam.leftMargin = getResources().getDimensionPixelSize(R.dimen.attachment_left_margin);
            }
            mAttachmentLayout.setLayoutParams(attachParam);
        } else {
            mAttachmentLayout.setVisibility(View.GONE);
        }
    }

    public void refreshRemindView() {
        if (mGlobalBubble == null && mNotificationLayout != null) {
            mNotificationLayout.setVisibility(GONE);
        } else if (mGlobalBubble != null && mNotificationLayout != null && mNotiTime != null && mNotiIcon != null) {
            long remindTime = mGlobalBubble.getRemindTime();
            long dueDate = mGlobalBubble.getDueDate();
            boolean isNotify = remindTime > 0;
            String dueTime = dueDate > 0 ? CommonUtils.getNotifyDate(getContext(), dueDate, isNotify) : "";
            if (TextUtils.isEmpty(dueTime)) {
                mNotificationLayout.setVisibility(GONE);
                return;
            }
            if (!isNotify) {
                mNotiIcon.setVisibility(View.GONE);
            } else {
                mNotiIcon.setVisibility(View.VISIBLE);
                mNotiIcon.setImageResource(BubbleThemeManager.getBackgroudRes(mGlobalBubble.getColor(), BubbleThemeManager.BACKGROUND_UNFOLD_REMIND_ICON));
            }
            mNotificationLayout.setVisibility(VISIBLE);
            mNotiTime.setText(dueTime);
            mNotiTime.setTextColor(getResources().getColor(BubbleThemeManager.getBackgroudRes(mGlobalBubble.getColor(), BubbleThemeManager.BACKGROUND_BUBBLE_TEXT_COLOR)));

        }
    }

    private void resetShareColorViewWeight(int color) {
        if (mIvBubbleDelete != null) {
            LinearLayout.LayoutParams deleteParam = (LinearLayout.LayoutParams) mIvBubbleDelete.getLayoutParams();
            if (color == GlobalBubble.COLOR_SHARE) {
                deleteParam.weight = (float) 0;
                deleteParam.width = getResources().getDimensionPixelSize(R.dimen.delete_icon_width);
            } else {
                deleteParam.weight = (float) 1;
                deleteParam.width = getResources().getDimensionPixelSize(R.dimen.no_width);
            }
            mIvBubbleDelete.setLayoutParams(deleteParam);
        }

        if (mTvTitleSmall != null) {
            LinearLayout.LayoutParams textParam = (LinearLayout.LayoutParams) mTvTitleSmall.getLayoutParams();
            if (color == GlobalBubble.COLOR_SHARE) {
                textParam.leftMargin = getResources().getDimensionPixelSize(R.dimen.text_right_margin_share);
            } else {
                textParam.leftMargin = getResources().getDimensionPixelSize(R.dimen.text_right_full_margin_share);
            }
            mTvTitleSmall.setLayoutParams(textParam);
        }

        if (mVBubbleRlWave != null) {
            LayoutParams waveParam = (LayoutParams) mVBubbleRlWave.getLayoutParams();
            if (color == GlobalBubble.COLOR_SHARE) {
                waveParam.leftMargin = getResources().getDimensionPixelSize(R.dimen.wave_view_left_margin);
            } else {
                waveParam.leftMargin = getResources().getDimensionPixelSize(R.dimen.wave_view_full_left_margin);
            }
            mVBubbleRlWave.setLayoutParams(waveParam);
        }

        if (mBubbleState != null && mBubbleState != BubbleSate.RECYCLE) {
            if (mAttachmentLayout != null) {
                LayoutParams attachParam = (LayoutParams) mAttachmentLayout.getLayoutParams();
                if (color == GlobalBubble.COLOR_SHARE) {
                    attachParam.leftMargin = getResources().getDimensionPixelSize(R.dimen.attachment_share_left_margin);
                } else {
                    attachParam.leftMargin = getResources().getDimensionPixelSize(R.dimen.attachment_left_margin);
                }
                mAttachmentLayout.setLayoutParams(attachParam);
            }
        }
    }

    public void onResume() {
        if (mIvBubbleBoom != null) {
            updateBubbleToolState(mGlobalBubble != null && TextUtils.isEmpty(mGlobalBubble.getText()));
            if (getContext().getPackageManager().queryIntentActivities(
                    new Intent(ACTION_BOOM_TEXT), 0).isEmpty()) {
                mIvBubbleBoom.setAlpha(0.15f);
            } else {
                mIvBubbleBoom.setAlpha(1f);
            }
        }
    }

    public void initShareDrawerView() {
        List<ShareItem> saveShareList = DrawerDataRepository.INSTANCE.getDrawerData();
        boolean isShellBubbleType = SaraUtils.getBubbleType(getContext()) == SaraUtils.BUBBLE_TYPE.SHELL_BUBBLE;
        if (!SaraUtils.isDrawerEnable(getContext()) || isShellBubbleType
                || saveShareList == null || saveShareList.isEmpty()) {
            showShareLayout(GONE);
            return;
        }
        mShareDrawerLayout.setData(saveShareList, new ShareDrawerLayout.OnShareItemClickListener() {
            @Override
            public void onShareItemClick(ShareItem shareItem) {
                if (shareItem.getComponentName().equals(new ComponentName("com.tencent.mm", "com.tencent.mm.ui.tools.ShareImgUI")) && !SaraUtils.isNetworkConnected()) {
                    ToastUtil.showToast(R.string.no_network_connected);
                } else {
                    SaraUtils.shareGlobal(getContext(), mGlobalBubble, shareItem.getComponentName());
                }
            }
        });
    }

    public void setBubbleMarginX(int marginX) {
        mBubbleMarginX = marginX;
    }

    public void resetBubbleLayoutPrama() {
        mBubbleWavMinWidth = SaraUtils.isLeftPopBubble() ? getResources().getDimensionPixelSize(R.dimen.bubble_wave_left_min_width) : getResources().getDimensionPixelSize(R.dimen.bubble_wave_min_width);
        mBubbleInitLeftMargin = getResources().getDimensionPixelSize(R.dimen.nomal_bubble_left_margin);
        mBubbleInitHeight = mBubbleTextPopupHeight;
        mLastBubbleState = BubbleSate.NONE;
        mBubbleState = BubbleSate.NONE;
    }

    public void updateBubbleLayoutParam(int minWidth, int leftMargin, int bottomMargin) {
        mBubbleWavMinWidth = minWidth;
        mBubbleInitLeftMargin = leftMargin;
        mBubbleInitBottomMargin = bottomMargin;
        mLastBubbleState = BubbleSate.NONE;
        mBubbleState = BubbleSate.NONE;
    }

    public int getBubbleInitHeight() {
        return mBubbleInitHeight;
    }

    public void showVoiceInputButton() {
        if (mVoiceButtonContent == null) {
            ViewStub searchStub = (ViewStub) findViewById(R.id.voice_button);
            if (searchStub != null && searchStub.getParent() != null) {
                searchStub.inflate();
                mVoiceButtonContent = findViewById(R.id.voice_input_content);
            } else {
                mVoiceButtonContent = findViewById(R.id.voice_input_content);
            }
            if (mVoiceButtonContent != null) {
                findViewById(R.id.voice_cancel).setOnClickListener(mVoiceButtonClickListener);
                findViewById(R.id.voice_over).setOnClickListener(mVoiceButtonClickListener);
            }
            mVoiceButtonDivider = findViewById(R.id.voice_button_divider);
        }
        mVoiceButtonContent.setVisibility(View.VISIBLE);
        mBubbleInitHeight = mBubbleTextPopupHeight + ViewUtils.getSupposeHeight(mVoiceButtonContent);
        int color = SaraUtils.getDefaultBubbleColor(mContext);
        int textColor = getResources().getColor(BubbleThemeManager.getBackgroudRes(color, BubbleThemeManager.BACKGROUND_BUBBLE_TEXT_COLOR));
        TextView tvCancel = (TextView) findViewById(R.id.voice_cancel);
        TextView tvDone = (TextView) findViewById(R.id.voice_over);
        tvCancel.setTextColor(textColor);
        tvDone.setTextColor(textColor);
        mVoiceButtonDivider.setVisibility(View.GONE);
    }

    public void hideVoiceInputButton() {
        if (mVoiceButtonContent != null) {
            mVoiceButtonContent.setVisibility(View.GONE);
        }
    }

    public View getContainerView() {
        if (mContainerLayout == null) {
            mContainerLayout = (RelativeLayout) findViewById(R.id.bubble_container);
        }
        return mContainerLayout;
    }

    @Override
    public boolean onKeyShortcut(int keyCode, KeyEvent event) {
        boolean handled = false;
        //mac mode
        if ((event.getFlags() & MultiSdkUtils.FLAG_MAC_MODE) != 0) {
            if (event.isMetaPressed()) {
                handled = handleShortcutKeyEvent(keyCode, event.hasModifiers(KeyEvent.META_SHIFT_ON));
            }
            //windows mode
        } else if (event.hasModifiers(KeyEvent.META_CTRL_ON)) {
            handled = handleShortcutKeyEvent(keyCode, false);
        }
        if (!handled) {
            return super.onKeyShortcut(keyCode, event);
        }
        return handled;
    }

    private boolean handleShortcutKeyEvent(int keyCode, boolean shift) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_D:
                if (!shift) {
                    toggleTodoState();
                    return true;
                }
                break;
        }
        return false;
    }

    public void toggleTodoState() {
        if (mTodoCheck != null) {
            mTodoCheck.toggle();
        }
    }

    public void setVoiceOperationListener(IVoiceOperationListener listener) {
        mVoiceOperationListener = listener;
    }

    public void setBubbleHeightChangeListener(IBubbleHeightChangeListener l) {
        mBubbleHeightChangeListener = l;
    }

    private Runnable mUpdateWaveRunnable = new Runnable() {
        @Override
        public void run() {
            if (mVBubbleSpeechWave == null || mGlobalBubble == null) {
                return;
            }
            mVBubbleSpeechWave.setCurPosition(BubbleSpeechPlayer.getInstance(getContext())
                    .getCurrentPosition(mGlobalBubble));
            if (BubbleSpeechPlayer.getInstance(getContext()).isPlayingBubble(mGlobalBubble)) {
                mVBubbleSpeechWave.removeCallbacks(this);
                mVBubbleSpeechWave.post(this);
            } else {
                mVBubbleSpeechWave.setCurPosition(0);
                if (mAnimAlpha != null) {
                    mAnimAlpha.cancel();
                }
                mVBubbleSpeechWave.setShowMiddle(false);
                mIvBubblePlay.setAlpha(1.0f);
                mIvBubblePlay.setVisibility(VISIBLE);
            }
        }
    };

    private void updateSpeechWaveView() {
        if (mVBubbleSpeechWave != null) {
            mVBubbleSpeechWave.removeCallbacks(mUpdateWaveRunnable);
        }
        mUpdateWaveRunnable.run();
    }

    public boolean isCurrentBulletShow() {
        return null != mBulletViewChangeListener && mBulletViewChangeListener.isCurrentBulletShow();
    }

    public boolean isKeyBoardVisible() {
        return mKeyBoardVisible;
    }

    private boolean hasBulletChangeViews() {
        return null != mBubbleSignBullet
                && null != mBubbleSignCapsult
                && null != mBubbleSignClose;
    }

    public void setBubbleClickListener(OnBubbleClickListener bubbleClickListener) {
        this.mBubbleClickListener = bubbleClickListener;
    }

    public static interface OnBubbleClickListener {
        public void onAddAttachmentClick();

        public void onAttachmentChanged();

        public void onImageAttchmentClick(GlobalBubbleAttach globalBubbleAttach, ArrayList<Uri> uris);

        public void onFileClick(GlobalBubbleAttach globalBubbleAttach);
    }

    private View.OnClickListener mVoiceButtonClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            int id = view.getId();
            switch (id) {
                case R.id.voice_cancel:
                    if (mVoiceOperationListener != null) {
                        mVoiceOperationListener.onCancel();
                    }
                    break;
                case R.id.voice_over:
                    if (mVoiceOperationListener != null) {
                        mVoiceOperationListener.onDone();
                    }
                    break;
            }
        }
    };
}
