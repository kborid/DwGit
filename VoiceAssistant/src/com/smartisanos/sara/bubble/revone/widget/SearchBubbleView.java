package com.smartisanos.sara.bubble.revone.widget;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Paint;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.Drawable;
import android.service.onestep.GlobalBubble;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewStub;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.smartisanos.sara.R;
import com.smartisanos.sara.bubble.manager.BubbleThemeManager;
import com.smartisanos.sara.util.ViewUtils;
import com.smartisanos.sara.widget.IBubbleHeightChangeListener;
import com.smartisanos.sara.widget.listener.IVoiceOperationListener;
import com.smartisanos.sara.util.AnimManager;
import com.smartisanos.ideapills.common.util.SdkReflectUtils;

import java.lang.reflect.Method;
import java.util.ArrayList;

public class SearchBubbleView extends LinearLayout {
    private View mBubbleResult;
    private ImageView mLoadingImage;
    private EditText mBubbleTextView;
    private ImageView mIvBoom = null;
    private ImageView mIvSave = null;
    private View mButtonContainer;
    private View mDivider;
    private View mVoiceButtonContent;
    private BubbleSate mLastBubbleState = BubbleSate.INIT;
    private OnButtonClickListener mOnButtonClickListener;
    private IVoiceOperationListener mVoiceOperationListener;
    private TextEditorActionListener mTextEditorActionListener;
    private IBubbleHeightChangeListener mBubbleHeightChangeListener;
    private int mBubbleTopMargin;
    private int mBubbleMaxWidth;
    private int mBubblePaddingX;
    private int mBubbleLeftMargin;
    private Animator mAnimator;
    private String mBubbleText;
    private boolean mHandleTextChange = true;

    public enum BubbleSate {
        INIT, LOADING, VOICE_INPUT, NORMAL
    }

    public SearchBubbleView(Context context) {
        this(context, null);
    }

    public SearchBubbleView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SearchBubbleView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    protected void onFinishInflate() {
        super.onFinishInflate();
        Resources resources = mContext.getResources();
        mBubblePaddingX = resources.getDimensionPixelSize(R.dimen.bubble_padding_horizontal);
        mBubbleResult = findViewById(R.id.bubble_result);
        mLoadingImage = (ImageView) findViewById(R.id.load_image);
        mBubbleTextView = (EditText) findViewById(R.id.bubble_text);
        try {
            Method voiceInputMethod = SdkReflectUtils.findMethod(View.class, "setVoiceInputVisibility",
                    boolean.class);
            if (voiceInputMethod != null) {
                SdkReflectUtils.invokeMethod(voiceInputMethod, mBubbleTextView, false);
            }
        } catch (Exception e) {
            // ignore
        }
        mBubbleTextView.setOnFocusChangeListener(mOnFocusChangeListener);
        mButtonContainer = findViewById(R.id.button_container);
        mDivider = findViewById(R.id.divider);
        mIvBoom = (ImageView) findViewById(R.id.btn_boom);
        mIvSave = (ImageView) findViewById(R.id.btn_save);
        mIvBoom.setOnClickListener(mOnClickListener);
        mIvSave.setOnClickListener(mOnClickListener);
        mBubbleTextView.setOnEditorActionListener(mEditorActionListener);
        mBubbleTextView.setOnTouchListener(mTextTouchListener);
    }

    public void setBubbleState(BubbleSate state) {
        mLastBubbleState = state;
        switch (state) {
            case INIT:
                resetView();
                break;
            case LOADING:
                showLoadingView();
                break;
            case VOICE_INPUT:
                setToInputState();
                break;
            case NORMAL:
                setToNormalState();
                break;
        }
    }

    public BubbleSate getBubbleState() {
        return mLastBubbleState;
    }

    private void showLoadingView() {
        setVisibility(View.VISIBLE);
        mDivider.setVisibility(View.VISIBLE);
        setLoadImageVisible(true);
        int targetHeight = getExactHeight();
        int oldHeight = getHeight();
        if (oldHeight == 0) {
            Drawable background = getBackground();
            if (background != null) {
                oldHeight = background.getIntrinsicHeight();
            }
        }
        if (targetHeight != oldHeight) {
            cancelAnimators();
            mAnimator = heightWithTransAnim(oldHeight, targetHeight);
        }
    }

    private void setToInputState() {
        setLoadImageVisible(false);
        setVisibility(View.VISIBLE);
        mDivider.setVisibility(View.VISIBLE);
        int targetHeight = getExactHeight();
        int oldHeight = getHeight();
        if (oldHeight == 0) {
            Drawable background = getBackground();
            if (background != null) {
                oldHeight = background.getIntrinsicHeight();
            }
        }
        if (targetHeight != oldHeight) {
            cancelAnimators();
            mAnimator = heightWithTransAnim(oldHeight, targetHeight);
        }
    }

    private void setToNormalState() {
        setLoadImageVisible(false);
        setVisibility(View.VISIBLE);
        hideVoiceInputButton();
        mDivider.setVisibility(View.GONE);
        mButtonContainer.setVisibility(View.VISIBLE);
        mBubbleTextView.setSingleLine(true);
        final String text = mBubbleText;
        setEllipsizeText(text, true);
        mBubbleTextView.addTextChangedListener(mTextWatcher);
        FrameLayout.LayoutParams bubbleparams = (FrameLayout.LayoutParams) getLayoutParams();
        int width = View.MeasureSpec.makeMeasureSpec(mBubbleMaxWidth, mBubbleMaxWidth != 0 ? MeasureSpec.AT_MOST : MeasureSpec.UNSPECIFIED);
        int height = View.MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);
        measure(width, height);
        final int startTopMarigin = bubbleparams.topMargin;
        final int endTopMargin = mBubbleTopMargin;
        final int startWidth = getWidth();
        final int endWidth = getMeasuredWidth();
        final int startHeight = getHeight();
        final int endHeight = getMeasuredHeight();
        final int startLeftMargin = bubbleparams.leftMargin;
        final int endLeftMargin = mBubbleLeftMargin;
        AnimatorSet set = new AnimatorSet();
        final ArrayList<Animator> animatorList = new ArrayList<Animator>();
        if (getMeasuredHeight() != getHeight() || getMeasuredWidth() != getWidth()
                || startTopMarigin != endTopMargin || startLeftMargin != endLeftMargin) {
            final ValueAnimator animator = ValueAnimator.ofFloat(0f, 1f);
            animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    float percent = (Float) animation.getAnimatedValue();
                    bubbleparams.width = (int) ((startWidth - endWidth) * (1 - percent)) + endWidth;
                    bubbleparams.height = (int) ((startHeight - endHeight) * (1 - percent)) + endHeight;
                    bubbleparams.topMargin = (int) ((startTopMarigin - endTopMargin) * (1 - percent)) + endTopMargin;
                    bubbleparams.leftMargin = (int) ((startLeftMargin - endLeftMargin) * (1 - percent)) + endLeftMargin;
                    setLayoutParams(bubbleparams);
                }
            });
            animatorList.add(animator);
        }
        final int translationY = (int) getTranslationY();
        if (translationY != 0) {
            ObjectAnimator translateYAnimator = ObjectAnimator.ofFloat(this, "translationY", translationY, 0);
            animatorList.add(translateYAnimator);
        }
        set.playTogether(animatorList);
        set.setDuration(AnimManager.SHOW_BUBBLE_RESULT_DURATION);
        set.start();
        mAnimator = set;
    }

    public boolean cancelAnimations() {
        if (getAnimation() != null && !getAnimation().hasEnded()) {
            clearAnimation();
            return true;
        }
        return false;
    }

    public boolean cancelAnimators() {
        if (mAnimator != null && mAnimator.isRunning()) {
            mAnimator.cancel();
            return true;
        }
        return false;
    }

    private void resetView() {
        setLoadImageVisible(false);
        mDivider.setVisibility(View.GONE);
        mButtonContainer.setVisibility(View.GONE);
        mBubbleTextView.removeTextChangedListener(mTextWatcher);
        mBubbleTextView.setText("");
        mBubbleTextView.setSingleLine(false);
        setTranslationY(0);
    }

    private void setLoadImageVisible(boolean show) {
        int visible = show ? View.VISIBLE : View.GONE;
        if (mLoadingImage.getVisibility() != visible) {
            mLoadingImage.setVisibility(visible);
            AnimationDrawable animation = (AnimationDrawable) mLoadingImage.getDrawable();
            animation.stop();
            if (show) {
                animation.start();
            }
        }
    }

    public void setText(String text) {
        mHandleTextChange = true;
        mBubbleText = text;
        if (mBubbleTextView != null) {
            mBubbleTextView.setText(text);
        }
    }

    public void setEllipsizeText(String text) {
        setEllipsizeText(text, false);
    }

    private void setEllipsizeText(String text, boolean handleTextChange) {
        mBubbleText = text;
        mHandleTextChange = handleTextChange;
        CharSequence ellipsText = text;
        if (mBubbleTextView != null) {
            if (!TextUtils.isEmpty(text)) {
                int buttonWidth = mButtonContainer.getWidth();
                if (buttonWidth <= 0) {
                    int measureSpec = View.MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);
                    mButtonContainer.measure(measureSpec, measureSpec);
                    buttonWidth = mButtonContainer.getMeasuredWidth();
                }
                int maxWidth = mBubbleMaxWidth - getPaddingLeft() - mBubbleTextView.getPaddingLeft() - mBubblePaddingX - mBubblePaddingX
                        - buttonWidth - getPaddingRight() - mBubbleTextView.getPaddingRight();
                ellipsText = TextUtils.ellipsize(text, mBubbleTextView.getPaint(), maxWidth, TextUtils.TruncateAt.END);
            }
            mBubbleTextView.setText(ellipsText);
        }
        updateLayout();
    }

    private void updateLayout() {
        Paint paint = mBubbleTextView.getPaint();
        int textWidth = mBubbleText == null ? 0 : mBubbleTextView.getPaddingLeft() + (int) paint.measureText(mBubbleText) + mBubbleTextView.getPaddingRight();
        int width = Math.min(getPaddingLeft() + mBubblePaddingX + textWidth + mBubblePaddingX + mButtonContainer.getMeasuredWidth() + getPaddingRight(), mBubbleMaxWidth);
        FrameLayout.LayoutParams bubbleparams = (FrameLayout.LayoutParams) getLayoutParams();
        int height = getMeasuredHeight();
        if (bubbleparams.width != width || bubbleparams.height != height) {
            bubbleparams.width = width;
            bubbleparams.height = height;
            setLayoutParams(bubbleparams);
        }
    }

    public int getExactHeight() {
        int targetHeight = ViewUtils.getSupposeHeight(this);
        int childCount = getChildCount();
        int allChildHeight = 0;
        for (int i = 0; i < childCount; i++) {
            View child = getChildAt(i);
            if (child.getVisibility() == View.VISIBLE) {
                allChildHeight += child.getMeasuredHeight();
                if (child.getLayoutParams() instanceof MarginLayoutParams) {
                    allChildHeight += ((MarginLayoutParams) child.getLayoutParams()).bottomMargin
                            + ((MarginLayoutParams) child.getLayoutParams()).topMargin;
                }
            }
        }
        if (targetHeight > allChildHeight) {
            targetHeight = allChildHeight;
        }
        return targetHeight;
    }

    public void setBubbleMargin(int leftMargin, int topMargin) {
        mBubbleLeftMargin = leftMargin;
        mBubbleTopMargin = topMargin;
    }

    public void setBubbleMaxWidth(int width) {
        mBubbleMaxWidth = width;
    }

    public AnimatorSet heightWithTransAnim(int startHeight, int targetHeight) {
        AnimatorSet set = new AnimatorSet();
        final ArrayList<Animator> animatorList = new ArrayList<Animator>();
        ValueAnimator heightAnimator = ValueAnimator.ofFloat(startHeight, targetHeight);
        heightAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float value = (Float) animation.getAnimatedValue();
                FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) getLayoutParams();
                params.height = (int) value;
                setLayoutParams(params);
            }
        });
        animatorList.add(heightAnimator);
        final int deltaTranslationY = startHeight - targetHeight;
        if (deltaTranslationY != 0 && mBubbleHeightChangeListener != null && mBubbleHeightChangeListener.needPerformAnimator()) {
            ObjectAnimator translateYAnimator = ObjectAnimator.ofFloat(this, "translationY", getTranslationY(), getTranslationY() + deltaTranslationY);
            animatorList.add(translateYAnimator);
            Animator animator = mBubbleHeightChangeListener.getWaveHeightAnimator(targetHeight);
            if (animator != null) {
                animatorList.add(animator);
            }
            animator = mBubbleHeightChangeListener.getWaveTransAnimator(deltaTranslationY);
            if (animator != null) {
                animatorList.add(animator);
            }
        }
        set.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animator) {
            }

            @Override
            public void onAnimationEnd(Animator animator) {
                FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) getLayoutParams();
                if (params.height != targetHeight) {
                    params.height = targetHeight;
                    setLayoutParams(params);
                }
            }

            @Override
            public void onAnimationCancel(Animator animator) {
            }

            @Override
            public void onAnimationRepeat(Animator animator) {
            }
        });
        set.playTogether(animatorList);
        set.setDuration(AnimManager.SHOW_BUBBLE_TEXT_CONTENT_DURATION);
        set.start();
        return set;
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
                findViewById(R.id.voice_button_divider).setVisibility(View.VISIBLE);
                findViewById(R.id.voice_cancel).setOnClickListener(mVoiceButtonClickListener);
                findViewById(R.id.voice_over).setOnClickListener(mVoiceButtonClickListener);
            }
        }
        if (mVoiceButtonContent != null) {
            mVoiceButtonContent.setVisibility(View.VISIBLE);
            FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) getLayoutParams();
            params.height = getExactHeight();
            setLayoutParams(params);
        }
    }

    public void hideVoiceInputButton() {
        if (mVoiceButtonContent != null) {
            mVoiceButtonContent.setVisibility(View.GONE);
        }
    }

    public void hideSoftInputFromWindow() {
        InputMethodManager imm = (InputMethodManager)
                getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(getWindowToken(), 0);
    }

    public void setOnBubbleSaveListener(OnButtonClickListener l) {
        mOnButtonClickListener = l;
    }

    public void setVoiceOperationListener(IVoiceOperationListener listener) {
        mVoiceOperationListener = listener;
    }

    public void setTextEditorActionListener(TextEditorActionListener l) {
        mTextEditorActionListener = l;
    }

    public void setBubbleHeightChangeListener(IBubbleHeightChangeListener l) {
        mBubbleHeightChangeListener = l;
    }

    public void resetBubbleContent(int color) {
        int textColor = getResources().getColor(BubbleThemeManager.getBackgroudRes(color, BubbleThemeManager.BACKGROUND_BUBBLE_TEXT_COLOR));
        mBubbleTextView.setTextColor(textColor);
        mIvBoom.setImageResource(BubbleThemeManager.getBackgroudRes(color, BubbleThemeManager.BACKGROUND_BUBBLE_BOOM_ICON));
        mIvSave.setImageResource(BubbleThemeManager.getBackgroudRes(color, BubbleThemeManager.BACKGROUND_BUBBLE_INSERT_ICON));
        int leftMargin = 0;
        if (color == GlobalBubble.COLOR_SHARE) {
            leftMargin = mContext.getResources().getDimensionPixelSize(R.dimen.share_bubble_padding_horizontal);
        } else {
            leftMargin = mContext.getResources().getDimensionPixelSize(R.dimen.bubble_padding_horizontal);
        }
        RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams) mBubbleResult.getLayoutParams();
        lp.leftMargin = leftMargin;
        mBubbleResult.setLayoutParams(lp);
        if (mVoiceButtonContent != null) {
            TextView tvCancel = (TextView) findViewById(R.id.voice_cancel);
            TextView tvDone = (TextView) findViewById(R.id.voice_over);
            tvCancel.setTextColor(textColor);
            tvDone.setTextColor(textColor);
        }
    }

    private View.OnClickListener mOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            if (mOnButtonClickListener != null) {
                int id = view.getId();
                switch (id) {
                    case R.id.btn_save:
                        mOnButtonClickListener.onSaveButtonClick();
                        break;
                    case R.id.btn_boom:
                        requestFocus();
                        mOnButtonClickListener.onBoomButtonClick(view);
                        break;
                }
            }
        }
    };

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

    private TextWatcher mTextWatcher = new TextWatcher() {
        public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
        }

        public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
        }

        public void afterTextChanged(Editable editable) {
            if (mBubbleTextView.hasFocus() || mHandleTextChange) {
                String text = editable.toString();
                mBubbleText = text;
                updateLayout();
                if (mTextEditorActionListener != null) {
                    mTextEditorActionListener.onTextChange(text, !mBubbleTextView.hasFocus());
                }
            }
            mHandleTextChange = true;
        }
    };

    private TextView.OnEditorActionListener mEditorActionListener = new TextView.OnEditorActionListener() {

        @Override
        public boolean onEditorAction(TextView v, int actionCode, KeyEvent event) {
            int action = KeyEvent.ACTION_DOWN;
            int keycode = KeyEvent.KEYCODE_ENTER;
            if (event != null) {
                action = event.getAction();
                keycode = event.getKeyCode();
            }
            v.clearFocus();
            hideSoftInputFromWindow();
            if (mTextEditorActionListener != null && action == KeyEvent.ACTION_DOWN) {
                mTextEditorActionListener.onActionDone(keycode, mBubbleText);
            }
            return true;
        }
    };

    private OnFocusChangeListener mOnFocusChangeListener = new OnFocusChangeListener() {
        @Override
        public void onFocusChange(View view, boolean hasFocus) {
            if (hasFocus) {
                mBubbleTextView.setText(mBubbleText);
            } else {
                setEllipsizeText(mBubbleText);
            }
        }
    };

    protected OnTouchListener mTextTouchListener = new OnTouchListener() {
        public boolean onTouch(View v, MotionEvent event) {
            if (v.isFocused() || !event.isFromSource(InputDevice.SOURCE_MOUSE) ||
                    (event.getButtonState() & MotionEvent.BUTTON_SECONDARY) == 0) {
                return false;
            }
            return true;
        }
    };

    public interface OnButtonClickListener {
        void onSaveButtonClick();

        void onBoomButtonClick(View view);
    }

    public interface TextEditorActionListener {
        void onActionDone(int keyCode, String text);

        void onTextChange(String text, boolean complete);
    }
}