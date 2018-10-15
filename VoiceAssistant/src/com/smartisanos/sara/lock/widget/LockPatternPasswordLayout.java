
package com.smartisanos.sara.lock.widget;

import android.content.Context;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TextView;

import com.android.internal.widget.LockPatternView.Cell;
import com.smartisanos.sara.R;
import com.smartisanos.sara.lock.util.LockPasswordUtil;
import com.smartisanos.sara.lock.util.SmartisanApi;

import java.util.List;

import smartisanos.util.LogTag;

public class LockPatternPasswordLayout extends LockPasswordLayout implements
        View.OnClickListener, KeyguardLockPatternView.OnPatternListener {

    private static final String TAG = "LockPatternPasswordLayout";
    private TextView mBtnBack;
    private TextView mInfoView;
    private KeyguardLockPatternView mLockPatternView;
    private TextView mTipText;

    public LockPatternPasswordLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        LogTag.d(TAG," pattern onFinishInflate");
        mInfoView = (TextView) findViewById(R.id.info);
        mLockPatternView = (KeyguardLockPatternView) findViewById(R.id.lockPattern);
        mTipText = (TextView)findViewById(R.id.tip_text);
        mLockPatternView.setOnPatternListener(this);
        mLockPatternView.setFocusable(false);

        mBtnBack = (TextView) findViewById(R.id.btn_back);
        mBtnBack.setOnClickListener(this);

        SmartisanApi.setMaxTextSize(mInfoView, mContext.getResources().getDimension(
                R.dimen.lock_tip_max_text_size));

        updateFaceTipString(true);
    }


    private void updateFaceTipString(boolean isShow) {
        if (isShow) {
            if (TextUtils.isEmpty(getFaceTipString()) || !isKeyStoreUnlocked()) {
                mTipText.setVisibility(GONE);
            } else {
                mTipText.setVisibility(VISIBLE);
                mTipText.setText(getFaceTipString());
            }
        } else {
            mTipText.setVisibility(GONE);
        }
    }

    public int getInfoTipString() {
        int resourceTip;
        if (!isKeyStoreUnlocked()) {
            resourceTip = R.string.input_pattern_reboot;
            return resourceTip;
        } else {
            if (mSecureLockManager.isFingerprintIsEnable(mContext)) {
                resourceTip = R.string.input_pattern_or_fingerprint;
            } else {
                resourceTip = R.string.input_pattern;
            }
        }
        return resourceTip;
    }


    @Override
    public void updateConfiguration() {
        if (getSecondsRemaining() == 0) {
            if (isFingerprintLockout()) {
                mInfoView.setTextColor(getTipTextColor(true));
                mInfoView.setText(R.string.fingerprint_pattern_too_many_times);
                updateFaceTipString(false);
            } else {
                setTipNormalText(mInfoView,  getInfoTipString(), getTipTextColor(false));
            }
        }
        mBtnBack.setText(R.string.accessibility_back);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_back:
                if (mCallback != null) {
                    mCallback.onBack();
                }
                break;

            default:
                break;
        }
    }

    @Override
    public void clearPassword() {
        mLockPatternView.clearPattern();
    }

    @Override
    public void resetTips() {
        if (isPasswordTimeout()) {
            return;
        }
        if (isFingerprintLockout()) {
            mInfoView.setTextColor(getTipTextColor(true));
            mInfoView.setText(R.string.fingerprint_pattern_too_many_times);
            updateFaceTipString(false);
        } else {
            setTipNormalText(mInfoView,  getInfoTipString(), getTipTextColor(false));
        }
    }

    @Override
    public void onPatternStart() {
        removeCallbacks(mCancelRunnable);
    }

    @Override
    public void onPatternCleared() {
    }

    @Override
    public void onPatternCellAdded(List<Cell> pattern) {
    }

    @Override
    public void onPatternDetected(List<Cell> pattern) {
        String password = transformPasswordStr(pattern);
        mSecureLockManager.verifyKeyguardSecurely(this, password);
    }

    @Override
    public void onKeyguardSecurelyVerifyResult(final boolean success, final int timeoutMs) {
        post(new Runnable() {
            @Override
            public void run() {
                removeCallbacks(mCancelRunnable);
                if (success) {
                    onUnlockSuccess();
                    mLockPatternView
                            .setDisplayMode(KeyguardLockPatternView.DisplayMode.Correct);
                } else {
                    boolean reachMaxFaildCount = onUnlockFailed(timeoutMs);

                    mLockPatternView.setEnabled(false);
                    mInfoView.setTextColor(getTipTextColor(true));
                    unlockFailedVibrate();
                    if (reachMaxFaildCount) {
                        mLockPatternView.clearPattern();
                    } else {
                        mLockPatternView
                                .setDisplayMode(KeyguardLockPatternView.DisplayMode.Wrong);
                        final int totalFailedAttempts = getTotalFailedAttempts();
                        mInfoView.setText(R.string.pattern_error_warning);
                        playShakeAnimation(mInfoView);
                        postDelayed(mCancelRunnable, CLEAR_PASSWORD_TIMEOUT_MS);
                    }
                }
            }
        });
    }

    @Override
    protected void onFingerprintAuthenticationInfo(final CharSequence info) {
        post(new Runnable() {
            @Override
            public void run() {
                mInfoView.setTextColor(getTipTextColor(true));
                mInfoView.setText(info);
                playShakeAnimation(mInfoView);
                removeCallbacks(mCancelRunnable);
                postDelayed(mCancelRunnable, CLEAR_PASSWORD_TIMEOUT_MS);
            }
        });
    }

    @Override
    protected void onFingerprintAuthenticationLockout(final boolean animat) {
        post(new Runnable() {
            @Override
            public void run() {
                if (!isPasswordTimeout() && !animat) {
                    mInfoView.setTextColor(getTipTextColor(true));
                    mInfoView.setText(R.string.fingerprint_pattern_too_many_times);
                    updateFaceTipString(false);
                }
                if (animat) {
                    playShakeAnimation(mInfoView);
                }
            }
        });
    }

    @Override
    protected void onFingerprintAuthenticationSucceeded() {
        post(new Runnable() {
            @Override
            public void run() {
                if (mCallback != null) {
                    mCallback.onUnlockResult(true);
                }
            }
        });
    }

    @Override
    protected void onFaceIdAuthenticationSucceeded() {
        post(new Runnable() {
            @Override
            public void run() {
                if (mCallback != null) {
                    mCallback.onUnlockResult(true);
                }
            }
        });
    }

    private String transformPasswordStr(List<Cell> pattern) {
        int size = pattern.size();
        int i = 0;
        StringBuffer sb = new StringBuffer();
        while (i < size) {
            sb.append(pattern.get(i).getRow()).append(pattern.get(i).getColumn());
            i++;
        }
        return sb.toString();
    }

    @Override
    public void reset() {
        mLockPatternView.enableInput();
        mLockPatternView.clearPattern();
        mLockPatternView.setEnabled(true);
    }

    @Override
    protected void onFailLockTick(long millisUntilFinished) {
        final int secondsRemaining = (int) (millisUntilFinished / 1000);
        if (secondsRemaining > 0) {
            mInfoView.setText(getResources().getQuantityString(R.plurals.unlock_error_try_later,
                    secondsRemaining, secondsRemaining));
            mInfoView.setTextColor(getTipTextColor(true));
            updateFaceTipString(false);
        }
    }

    @Override
    protected void onFailLockFinish() {
        reset();
        resetTips();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        LogTag.d(TAG," pattern onAttachedToWindow");
        // stealth mode will be the same for the life of this screen
        mLockPatternView.setInStealthMode(!LockPasswordUtil.isVisiblePatternEnabled(
                mSecureLockManager.getLockPatternUtils()));
        // vibrate mode will be the same for the life of this screen
        mLockPatternView.setTactileFeedbackEnabled(mSecureLockManager.getLockPatternUtils()
                .isTactileFeedbackEnabled());
    }

    @Override
    public void disableOnTouch() {
        mLockPatternView.setEnabled(false);
    }
}
