package com.smartisanos.sara.lock;
import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.animation.TimeInterpolator;
import android.animation.ValueAnimator;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.Handler;
import android.text.Layout;
import android.text.Selection;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewStub;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.view.animation.DecelerateInterpolator;
import android.widget.EditText;
import android.widget.RelativeLayout;
import com.smartisanos.sara.R;
import com.smartisanos.sara.lock.widget.LockPasswordLayout;
import com.smartisanos.sara.lock.widget.SecureLockManager;
import com.smartisanos.sara.lock.widget.UnlockResultCallback;
import com.smartisanos.sara.util.LogUtils;

import smartisanos.util.LogTag;

import static com.smartisanos.sara.bubble.search.FlashImContactsFragment.ACTION_FINSH_VIEW;
import static com.smartisanos.sara.bubble.search.FlashImContactsFragment.ACTION_UNLOCK_SUCCESS;

public class LockContainerView extends RelativeLayout implements View.OnClickListener, ViewTreeObserver.OnGlobalLayoutListener
       , UnlockResultCallback {
    public static final String ACTION_DISMISS_PREVENT_TOUCH_WINDOW = "action_keyguard_dismiss_prevent_touch_window";
    public final static int POP_UP_DIALOG_NORMAL = 0;
    public final static int POP_UP_DIALOG_UNLOCK = 2;
    private View mLockRootLayout;
    private ViewStub mLockPatternPasswordViewStub;
    private ViewStub mLockEasyPasswordViewStub;
    private ViewStub mLockComplexPasswordViewStub;
    private LockPasswordLayout mCurrentLockTypeLayout;
    private Rect mRect = new Rect();
    private int mKeyboardHeight;
    private boolean mIsKeyboardShow;
    private boolean mIsUnlockViewSwitching;
    private SecureLockManager mSecureLockManager;
    private Handler mHandler;
    private int mPopUpDialogMode = POP_UP_DIALOG_NORMAL;
    private int mEnterLineNumber = 0;
    private ObjectAnimator mTranslationYAnimator = null;
    private IntentFilter mIntentFilter;

    public LockContainerView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mSecureLockManager = SecureLockManager.getInstance(context);
    }

    public void setHandler(Handler handler) {
        mHandler = handler;
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mLockRootLayout = findViewById(R.id.lock_layout);
        mLockPatternPasswordViewStub = (ViewStub)findViewById(R.id.lock_pattern_password);
        mLockEasyPasswordViewStub = (ViewStub) findViewById(R.id.lock_easy_passward);
        mLockComplexPasswordViewStub = (ViewStub) findViewById(R.id.lock_complex_passward);
    }

    public void onConfigurationChanged(Configuration newConfig) {
        if (mCurrentLockTypeLayout != null) {
            mCurrentLockTypeLayout.updateConfiguration();
        }
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        getViewTreeObserver().addOnGlobalLayoutListener(this);

        mIntentFilter = new IntentFilter();
        mIntentFilter.addAction(Intent.ACTION_SCREEN_OFF);
        LogUtils.d("createPopUpDialogLayout");
        mContext.registerReceiver(mBroadcaseReceiver, mIntentFilter);
    }

    @Override
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if(mCurrentLockTypeLayout != null){
            mCurrentLockTypeLayout.setFingerprintListening(false);
            mCurrentLockTypeLayout.setFaceIdListening(false);
            getViewTreeObserver().removeOnGlobalLayoutListener(this);
        }
        mContext.unregisterReceiver(mBroadcaseReceiver);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            default:
                break;
        }
    }

    public void reset() {
        mIsKeyboardShow = false;
        mIsUnlockViewSwitching = false;
        mPopUpDialogMode = POP_UP_DIALOG_NORMAL;
        mLockRootLayout.setVisibility(View.INVISIBLE);
    }

    public int getDialogMode(){
        return mPopUpDialogMode;
    }
    @Override
    public void onUnlockResult(boolean success) {
        if (success) {
            backToPopUpDialogPanel(true);
        }
    }

    @Override
    public void onBack() {
        if (mHandler != null) {
            mHandler.sendEmptyMessage(ACTION_FINSH_VIEW);
        }
    }

    private void doAfterUnlockSuccess() {
        if (mHandler != null) {
            mHandler.sendEmptyMessage(ACTION_UNLOCK_SUCCESS);
        }
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (KeyEvent.KEYCODE_BACK == event.getKeyCode() && KeyEvent.ACTION_UP == event.getAction()) {
            onBack();
            return true;
        }
        return super.dispatchKeyEvent(event);
    }

    public boolean backToPopUpDialogPanel(final boolean isUnlockSuccess) {
        if (mCurrentLockTypeLayout == null) {
            return false;
        }
        mCurrentLockTypeLayout.hideSoftInputIfNeedly();
        if (mLockRootLayout.getVisibility() != View.VISIBLE) {
            return false;
        }
        mPopUpDialogMode = POP_UP_DIALOG_NORMAL;
        postDelayed(new Runnable() {
            @Override
            public void run() {
                mCurrentLockTypeLayout.setFingerprintListening(false);
                mCurrentLockTypeLayout.setFaceIdListening(false);
                mLockRootLayout.setVisibility(View.INVISIBLE);
                if (isUnlockSuccess) {
                    doAfterUnlockSuccess();
                }
            }
        }, 200);
        return true;
    }

    //显示解锁界面入口
    public boolean unlockIfNeedly() {

        if (!isKeyguardVerified()) {
            return false;
        }

        int secureLockMode = mSecureLockManager.getSecureLockMode();
        switch (secureLockMode) {
            case SecureLockManager.SECURE_LOCK_MODE_PASSWORD_PATTERN:
                mLockPatternPasswordViewStub.setVisibility(View.VISIBLE);
                mCurrentLockTypeLayout = (LockPasswordLayout) findViewById(R.id.lock_pattern_password_layout);
                mLockEasyPasswordViewStub.setVisibility(View.GONE);
                mLockComplexPasswordViewStub.setVisibility(View.GONE);
                break;
            case SecureLockManager.SECURE_LOCK_MODE_PASSWORD_EASY:
                mLockEasyPasswordViewStub.setVisibility(View.VISIBLE);
                mCurrentLockTypeLayout = (LockPasswordLayout) findViewById(R.id.lock_easy_passward_layout);
                mLockPatternPasswordViewStub.setVisibility(View.GONE);
                mLockComplexPasswordViewStub.setVisibility(View.GONE);
                break;
            case SecureLockManager.SECURE_LOCK_MODE_PASSWORD_COMPLEX:
                mLockComplexPasswordViewStub.setVisibility(View.VISIBLE);
                mCurrentLockTypeLayout = (LockPasswordLayout) findViewById(R.id.lock_complex_passward_layout);
                mLockPatternPasswordViewStub.setVisibility(View.GONE);
                mLockEasyPasswordViewStub.setVisibility(View.GONE);
                break;
            default:
                return false;
        }

        if (mIsUnlockViewSwitching) {
            return true;
        }

        mPopUpDialogMode = POP_UP_DIALOG_UNLOCK;
        mCurrentLockTypeLayout.onCheckLockFreezed();
        mCurrentLockTypeLayout.clearPassword();
        mCurrentLockTypeLayout.resetTips();
        mCurrentLockTypeLayout.setVisibility(View.VISIBLE);
        mCurrentLockTypeLayout.setUnlockResultCallBack(this);
        mCurrentLockTypeLayout.setFingerprintListening(true);
        mCurrentLockTypeLayout.setFaceIdListening(true);
        mIsUnlockViewSwitching = true;
        mCurrentLockTypeLayout.post(new Runnable() {

            @Override
            public void run() {
                mLockRootLayout.setVisibility(View.VISIBLE);
                mIsUnlockViewSwitching = false;
            }
        });

        return true;
    }

    public static int getScreenWidth(Context context) {
        Point screenSize = new Point();
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        wm.getDefaultDisplay().getRealSize(screenSize);
        int rotate = wm.getDefaultDisplay().getRotation();
        return screenSize.x;
    }

    @Override
    public void onGlobalLayout() {

        View focusView = findFocus();
        getRootView().getWindowVisibleDisplayFrame(mRect);
        final int heightDiff = getRootView().getBottom() - mRect.bottom;
        boolean isKeyboardShow = heightDiff > 0;
        boolean isKeyBoardStateChanged = false;

        if (isKeyboardShow
                && ( focusView != null
                && !(focusView instanceof EditText))) {
            return;
        }
        if(mCurrentLockTypeLayout == null || mCurrentLockTypeLayout.getEditeView() == null){
            return;
        }
        if (mIsKeyboardShow != isKeyboardShow
                || (mKeyboardHeight != heightDiff)) {
            isKeyBoardStateChanged = true;
        }

        if (heightDiff > 0) {
            mKeyboardHeight = heightDiff;
        }
        mIsKeyboardShow = isKeyboardShow;

        int currentBottom = focusView == null ? 0 : focusView.getBottom();
        int currentCursorLineInBox = getCurrentLineOfCursorInBox(mCurrentLockTypeLayout.getEditeView());
        if (isKeyBoardStateChanged) {
            mEnterLineNumber = currentCursorLineInBox;
        }
        float translationY = isKeyboardShow ? (focusView == null ? 0
                : -(currentBottom - mEnterLineNumber * mCurrentLockTypeLayout.getEditeView().getLineHeight())) : 0;

        //when the line of editText change from 4 to 5, the bottom will decrease 2 px. This change will cause
        // an animation which can cause a visual problem.
        if (Math.abs(getTranslationY() - translationY) < 5) {
            return;
        }
        playTranslationYAnimator(getTranslationY(), translationY, 200,
                new DecelerateInterpolator(), new Animator.AnimatorListener() {

                    @Override
                    public void onAnimationStart(Animator animation) {
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
                }, null);
    }

    public int getCurrentLineOfCursorInBox(EditText editText) {
        int selectionStart = Selection.getSelectionStart(editText.getText());
        int result = 0;
        Layout layout = editText.getLayout();
        if (null != layout && selectionStart != -1) {
            int yInBox = layout.getLineBottom(layout.getLineForOffset(selectionStart)) - editText.getScrollY();
            int firstLineBottom = layout.getLineBottom(0);
            int halfLineHeight = editText.getLineHeight() / 2;
            if (yInBox > firstLineBottom - halfLineHeight && yInBox <= firstLineBottom + halfLineHeight) {
                result = 0;
            } else if (yInBox > firstLineBottom + editText.getLineHeight() - halfLineHeight &&
                    yInBox <= firstLineBottom + editText.getLineHeight() + halfLineHeight) {
                result = 1;
            } else if (yInBox > firstLineBottom + editText.getLineHeight() * 2 - halfLineHeight &&
                    yInBox <= firstLineBottom + editText.getLineHeight() * 2 + halfLineHeight) {
                result = 2;
            } else if (yInBox > firstLineBottom + editText.getLineHeight() * 3 - halfLineHeight) {
                result = 3;
            }

        }
        return result;
    }

    private void playTranslationYAnimator(float startY, float finalY, long duration,
                                          TimeInterpolator interpolator, Animator.AnimatorListener listener,
                                          ValueAnimator.AnimatorUpdateListener updateListener) {
        if (mTranslationYAnimator != null && mTranslationYAnimator.isRunning()) {
            mTranslationYAnimator.cancel();
        }
        mTranslationYAnimator = ObjectAnimator.ofFloat(this,
                "translationY", startY, finalY);
        mTranslationYAnimator.setDuration(duration);

        if (interpolator != null) {
            mTranslationYAnimator.setInterpolator(interpolator);
        }
        if (listener != null) {
            mTranslationYAnimator.addListener(listener);
        }
        if (updateListener != null) {
            mTranslationYAnimator.addUpdateListener(updateListener);
        }
        mTranslationYAnimator.start();
    }


    private BroadcastReceiver mBroadcaseReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (Intent.ACTION_SCREEN_OFF.equals(action)) {
                mSecureLockManager.setFingerprintVerify(false);
            }
        }
    };

    public boolean isKeyguardVerified() {
        if (!mSecureLockManager.isKeyguardLocked()) {
            return false;
        }

        int secureLockMode = mSecureLockManager.getSecureLockMode();
        if (secureLockMode == SecureLockManager.UNSECURE_LOCK_MODE) {
            return false;
        }
        if (mSecureLockManager.isKeyguardVerified()) {
            return false;
        }
        return true;
    }
}
