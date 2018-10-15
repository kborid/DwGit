
package com.smartisanos.sara.lock.widget;

import android.animation.Animator;
import android.animation.Keyframe;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.app.KeyguardManager;
import android.app.KeyguardManager.OnKeyguardFingerprintVerifyResult;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.CountDownTimer;
import android.os.IBinder;
import android.os.SystemClock;
import android.os.Vibrator;
import android.provider.Settings;
import android.security.KeyStore;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.smartisanos.sara.R;
import com.smartisanos.sara.lock.util.FingerprintController;
import com.smartisanos.sara.lock.util.LockPasswordUtil;
import com.smartisanos.sara.lock.util.SmartisanApi;

import smartisanos.util.LogTag;


public abstract class LockPasswordLayout extends LinearLayout implements
        SmartisanApi.OnKeyguardVerifyResultSmto, OnKeyguardFingerprintVerifyResult, KeyguardManager.OnKeyguardFaceIDVerifyResult {

    // how long before we clear the wrong pattern
    protected static final int CLEAR_PASSWORD_TIMEOUT_MS = 400;
    private static final int MAX_FAILED_ATTEMPTS = SecureLockManager.MAX_FAILED_ATTEMPTS;
    private static final long FAILED_WAITING_SECONDS = 30000;
    private static final String TAG = "LockPasswordLayout";

    protected UnlockResultCallback mCallback;
    protected SecureLockManager mSecureLockManager;

    private int mTextColor;
    private int mErrorTextColor;

    private long mSecondsRemaining;
    private int mTotalFailedAttempts;

    private boolean mFpLockout;

    private ObjectAnimator mShakeAnim;

    private IntentFilter mIntentFilter;

    private BroadcastReceiver mBroadcaseReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (Intent.ACTION_SCREEN_OFF.equals(action)) {
                resetTips();
            }
        }
    };

    protected Runnable mCancelRunnable = new Runnable() {
        public void run() {
            reset();
        }
    };

    public LockPasswordLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        mFpLockout = false;
        mSecureLockManager = SecureLockManager.getInstance(context);
        mTextColor = getResources().getColor(R.color.unlock_view_tips);
        mErrorTextColor = getResources().getColor(R.color.unlock_view_tips_error);
    }

    public void setUnlockResultCallBack(UnlockResultCallback callback) {
        mCallback = callback;
    }

    public void onUnlockSuccess() {
        mTotalFailedAttempts = 0;
        mFpLockout = false;
        if (mCallback != null) {
            mCallback.onUnlockResult(true);
        }
        Settings.Global.putInt(mContext.getContentResolver(), "failedAttempt", 0);
    }

    public boolean onUnlockFailed(final int timeoutMs) {
        mTotalFailedAttempts = getTotalFailedAttempts();
        mTotalFailedAttempts++;

        setTotalFailedAttempts(mTotalFailedAttempts);
        if ((mTotalFailedAttempts % MAX_FAILED_ATTEMPTS) == 0 || timeoutMs > 0) {
            long watingSecond = timeoutMs > 0 ? timeoutMs : FAILED_WAITING_SECONDS;
            mSecondsRemaining = watingSecond;
            Settings.Global.putLong(mContext.getContentResolver(), "secondRemain", SystemClock.elapsedRealtime());
            final long deadline = SystemClock.elapsedRealtime() + watingSecond;
            final long elapsedRealtime = SystemClock.elapsedRealtime();
            new CountDownTimer(deadline - elapsedRealtime, 1000) {

                @Override
                public void onTick(long millisUntilFinished) {
                    mSecondsRemaining = millisUntilFinished;
                    onFailLockTick(millisUntilFinished);
                }

                @Override
                public void onFinish() {
                    mSecondsRemaining = 0;
                    mTotalFailedAttempts = 0;
                    setTotalFailedAttempts(mTotalFailedAttempts);
                    onFailLockFinish();
                    Settings.Global.putLong(mContext.getContentResolver(), "secondRemain", 0);
                }

            }.start();
            return true;
        }

        if (mCallback != null) {
            mCallback.onUnlockResult(false);
        }

        return false;
    }

    public long getSecondsRemaining() {
        return LockPasswordUtil.getSecondsRemaining(mContext,
                mSecureLockManager.getLockPatternUtils());
    }

    public int getTotalFailedAttempts() {
        final int f = Settings.Global.getInt(mContext.getContentResolver(), "failedAttempt", 0);
        return f;
    }

    private void setTotalFailedAttempts(int count) {
        Settings.Global.putInt(mContext.getContentResolver(), "failedAttempt", count);
    }

    public int getTipTextColor(boolean error) {
        return error ? mErrorTextColor : mTextColor;
    }

    public void showSoftInputIfNeedly() {
    }

    public void onCheckLockFreezed() {
        final int totalFailed = getTotalFailedAttempts();
        if ((totalFailed % 5) == 0){
            final Long m = getSecondsRemaining();
            disableOnTouch();
            mSecondsRemaining = m;
            new CountDownTimer(m, 1000) {
                @Override
                public void onTick(long millisUntilFinished) {
                    mSecondsRemaining = millisUntilFinished;
                    onFailLockTick(millisUntilFinished);
                }

                @Override
                public void onFinish() {
                    mSecondsRemaining = 0;
                    onFailLockFinish();
                }
            }.start();
        }
    }

    public boolean hideSoftInputIfNeedly() {
        return false;
    }

    public void unlockFailedVibrate() {
        Vibrator vibrator = (Vibrator) mContext.getSystemService(Context.VIBRATOR_SERVICE);
        if (vibrator != null) {
            vibrator.vibrate(new long[] {0, 200}, -1);
        }
    }

    public void setTipNormalText(TextView text, int textId, int color) {
        String fingerprint = FingerprintController.getInstance(mContext).isFingerprintAuthAvailable()
                && KeyStore.getInstance().isUnlocked()
                ? getResources().getString(R.string.fingerprint_instructions) : "";
        String normal = String.format(getResources().getString(textId), fingerprint);
        text.setText(normal);
        text.setTextColor(color);
    }

    public void setFingerprintListening(boolean enable) {
        LogTag.d(TAG, "setFingerprintListening: " + enable);
        if (enable) {
            mFpLockout = false;
            mSecureLockManager.fingerprintStartListening(this);
        } else {
            mSecureLockManager.fingerprintStopListening();
        }
    }

    public void setFaceIdListening(boolean enable) {
        LogTag.d(TAG, "setFaceIdListening: " + enable);
        if (enable) {
            mSecureLockManager.faceIdStartListening(this);
        } else {
            mSecureLockManager.faceIdStopListening();
        }
    }

    public boolean isPasswordTimeout() {
        return mSecondsRemaining > 0 ? true : false;
    }

    public boolean isFingerprintLockout() {
        return mFpLockout;
    }

    public boolean isKeyStoreUnlocked() {
        boolean fingerprintEnable =
                FingerprintController.getInstance(mContext).isFingerprintAuthAvailable();
        return KeyStore.getInstance().isUnlocked()
                || !fingerprintEnable && !mSecureLockManager.lockScreenFaceIdEnable();
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        switch (event.getKeyCode()) {
            case KeyEvent.KEYCODE_BACK:
                if (FingerprintController.getInstance(mContext)
                        .isFingerprintAuthAvailable()) {
                    return true;
                }
                break;
            default:
                break;
        }
        return super.dispatchKeyEvent(event);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        mIntentFilter = new IntentFilter();
        mIntentFilter.addAction(Intent.ACTION_SCREEN_OFF);
        mContext.registerReceiver(mBroadcaseReceiver, mIntentFilter);
    }

    @Override
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        mSecureLockManager.setFingerprintVerify(false);
        mSecureLockManager.setFaceIdVerify(false);
        if (mBroadcaseReceiver != null && mContext != null) {
            mContext.unregisterReceiver(mBroadcaseReceiver);
        }
    }

    @Override
    public void onKeyguardVerifyResult(final boolean success, final int timeoutMs) {
        LogTag.d(TAG, "onKeyguardVerifyResult: " + success);
        if (success) {
            mFpLockout = false;
        }
        onKeyguardSecurelyVerifyResult(success, timeoutMs);
    }

    public EditText getEditeView() {
        return null;
    }

    @Override
    public void onAuthenticationError(int errMsgId, CharSequence errString) {
        LogTag.d(TAG, "onAuthenticationError: " + errString.toString());
        mFpLockout = true;
        onFingerprintAuthenticationLockout(false);
    }

    @Override
    public void onAuthenticationHelp(int helpMsgId, CharSequence helpString) {
        LogTag.d(TAG, "onAuthenticationHelp: " + helpString.toString());
    }

    @Override
    public void onAuthenticationFailed() {
        LogTag.d(TAG, "onAuthenticationFailed");
        if (mFpLockout || isPasswordTimeout()) {
            onFingerprintAuthenticationLockout(true);
        } else {
            onFingerprintAuthenticationInfo(getResources().getString(
                    R.string.fingerprint_authentication_failed));
        }
    }

    public void onAuthenticationAcquired(int acquireInfo) {
    }

    @Override
    public void onAuthenticationSucceeded() {
        LogTag.d(TAG, "onAuthenticationSucceeded");
        post(new Runnable() {
            @Override
            public void run() {
                mFpLockout = false;
                mSecureLockManager.setFingerprintVerify(true);
                onFingerprintAuthenticationSucceeded();
            }
        });
    }

    @Override
    public void onFaceAuthenticationSucceeded() {
        LogTag.d(TAG, "onFaceIdAuthenticationSucceeded");
        post(new Runnable() {
            @Override
            public void run() {
                mSecureLockManager.setFaceIdVerify(true);
                onFingerprintAuthenticationSucceeded();
            }
        });
    }

    public void playShakeAnimation(final View view) {
        int delta = 25;
        PropertyValuesHolder pvhTranslateX = PropertyValuesHolder.ofKeyframe(View.TRANSLATION_X,
                Keyframe.ofFloat(0f, 0),
                Keyframe.ofFloat(.10f, -delta),
                Keyframe.ofFloat(.26f, delta),
                Keyframe.ofFloat(.42f, -delta),
                Keyframe.ofFloat(.58f, delta),
                Keyframe.ofFloat(.74f, -delta),
                Keyframe.ofFloat(.90f, delta),
                Keyframe.ofFloat(1f, 0f)
        );
        if (mShakeAnim != null) {
            mShakeAnim.cancel();
        }
        mShakeAnim = ObjectAnimator.ofPropertyValuesHolder(view, pvhTranslateX).
                setDuration(400);
        mShakeAnim.addListener(new Animator.AnimatorListener() {

            @Override
            public void onAnimationStart(Animator animator) {
            }

            @Override
            public void onAnimationEnd(Animator animator) {
                view.setTranslationX(0);
                mShakeAnim = null;
                clearPassword();
            }

            @Override
            public void onAnimationCancel(Animator animator) {
                view.setTranslationX(0);
            }

            @Override
            public void onAnimationRepeat(Animator animator) {

            }
        });
        mShakeAnim.start();
    }

    public void updatePasswordLayout() {

    }

    public static boolean hideSoftInputFromWindow(Context context, IBinder token, int flag) {
        InputMethodManager ime = (InputMethodManager) context
                .getSystemService(Context.INPUT_METHOD_SERVICE);
        return ime.hideSoftInputFromWindow(token, flag);
    }

    public static void showSoftInput(Context context, View view, int flag) {
        InputMethodManager ime = (InputMethodManager) context
                .getSystemService(Context.INPUT_METHOD_SERVICE);
        ime.showSoftInput(view, flag);
    }

    public String getFaceTipString() {
        if (mSecureLockManager.isFaceIdEnable(mContext)) {
            return mContext.getResources().getString(R.string.faceid_or_password_pattern);
        } else {
            return null;
        }
    }


    abstract protected void onFailLockTick(long millisUntilFinished);

    abstract protected void onFailLockFinish();

    abstract public void clearPassword();

    abstract public void resetTips();

    abstract public void reset();

    abstract public void updateConfiguration();

    abstract public void disableOnTouch();

    abstract public void onKeyguardSecurelyVerifyResult(final boolean success, final int timeoutMs);

    abstract protected void onFingerprintAuthenticationInfo(final CharSequence info);

    abstract protected void onFingerprintAuthenticationLockout(final boolean animat);

    abstract protected void onFingerprintAuthenticationSucceeded();

    abstract protected void onFaceIdAuthenticationSucceeded();
}
