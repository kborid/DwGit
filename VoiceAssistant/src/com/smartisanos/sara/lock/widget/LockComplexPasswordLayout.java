
package com.smartisanos.sara.lock.widget;

import android.content.Context;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.ActionMode;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;
import com.smartisanos.sara.R;
import com.smartisanos.sara.lock.util.SmartisanApi;

public class LockComplexPasswordLayout extends LockPasswordLayout implements
        View.OnClickListener, TextWatcher, TextView.OnEditorActionListener {

    private TextView mBtnLeft;
    private TextView mBtnRight;
    private TextView mInfoView;
    private TextView mTipText;
    private EditText mEditView;

    public LockComplexPasswordLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        mBtnLeft = (TextView) findViewById(R.id.left_btn);
        mBtnRight = (TextView) findViewById(R.id.right_btn);
        mInfoView = (TextView) findViewById(R.id.info);
        mTipText = (TextView)findViewById(R.id.tip_text);
        mEditView = (EditText) findViewById(R.id.complex_passward_edit);

        mBtnLeft.setOnClickListener(this);
        mBtnRight.setOnClickListener(this);
        mEditView.setOnEditorActionListener(this);
        mEditView.addTextChangedListener(this);
        SmartisanApi.setHiddenContextMenuItem(mEditView, ActionMode.MENU_NO_ADDITIONAL);

        SmartisanApi.setMaxTextSize(mInfoView, mContext.getResources().getDimension(
                R.dimen.lock_tip_max_text_size));
        SmartisanApi.setMaxTextSize(mEditView, mContext.getResources().getDimension(
                R.dimen.pop_up_dialog_complex_passward_edit_max_text_size));

        updateFaceTipString(true);

        mEditView.setOnFocusChangeListener(new android.view.View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (!hasFocus) {
                    hideSoftInputIfNeedly();
                }
            }
        });

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
            resourceTip = R.string.input_password_reboot;
            return resourceTip;
        } else {
            if (mSecureLockManager.isFingerprintIsEnable(mContext)) {
                resourceTip = R.string.input_password_or_fingerprint;
            } else {
                resourceTip = R.string.input_password;
            }
        }
        return resourceTip;
    }

    @Override
    public void updateConfiguration() {
        if (getSecondsRemaining() == 0) {
            if (isFingerprintLockout()) {
                mInfoView.setTextColor(getTipTextColor(true));
                mInfoView.setText(R.string.fingerprint_password_too_many_times);
                updateFaceTipString(false);
            } else {
                setTipNormalText(mInfoView, getInfoTipString(), getTipTextColor(false));
            }
        }
        mBtnLeft.setText(R.string.accessibility_back);
        mBtnRight.setText(R.string.yes);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.left_btn:
                if (mCallback != null) {
                    mCallback.onBack();
                }
                break;
            case R.id.right_btn:
                String password = mEditView.getText().toString();
                mSecureLockManager.verifyKeyguardSecurely(this, password);
                break;

            default:
                break;
        }
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
        boolean enabled = (s != null && s.length() >= 4);
        mBtnRight.setEnabled(enabled);
    }

    @Override
    public void afterTextChanged(Editable s) {
    }

    @Override
    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
        if (actionId == EditorInfo.IME_NULL || actionId == EditorInfo.IME_ACTION_DONE
                || actionId == EditorInfo.IME_ACTION_NEXT) {
            String password = v.getText().toString();
            mSecureLockManager.verifyKeyguardSecurely(this, password);
            return true;
        }
        return false;
    }

    @Override
    public void onKeyguardSecurelyVerifyResult(final boolean success, final int timeoutMs) {
        post(new Runnable() {
            @Override
            public void run() {
                removeCallbacks(mCancelRunnable);
                if (success) {
                    onUnlockSuccess();
                    mEditView.setText(null);

                } else {
                    boolean reachMaxFaildCount = onUnlockFailed(timeoutMs);
                    mInfoView.setTextColor(getTipTextColor(true));
                    mEditView.setText(null);
                    unlockFailedVibrate();
                    if (reachMaxFaildCount) {
                        mEditView.setEnabled(false);
                    } else {
                        final int totalFailedAttempts = getTotalFailedAttempts();
                        mInfoView.setText(R.string.password_error_warning);
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
                    mInfoView.setText(R.string.fingerprint_password_too_many_times);
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
        if (mEditView.hasFocus()) {
            showSoftInputIfNeedly();
        }
    }

    @Override
    public void clearPassword() {
        mEditView.setText(null);
    }

    @Override
    public void resetTips() {
        if (isPasswordTimeout()) {
            return;
        }
        if (isFingerprintLockout()) {
            mInfoView.setTextColor(getTipTextColor(true));
            mInfoView.setText(R.string.fingerprint_password_too_many_times);
            updateFaceTipString(false);
        } else {
            setTipNormalText(mInfoView,  getInfoTipString(), getTipTextColor(false));
        }
    }

    @Override
    public void reset() {
        mEditView.setText(null);
        mEditView.setEnabled(true);
    }

    @Override
    public void showSoftInputIfNeedly() {
        if (!mEditView.isEnabled()) {
            return;
        }

        mEditView.requestFocus();
        mEditView.post(new Runnable() {

            @Override
            public void run() {
               showSoftInput(mContext, mEditView, 0);
            }
        });
    }

    @Override
    public boolean hideSoftInputIfNeedly() {
        return hideSoftInputFromWindow(mContext, mEditView.getWindowToken(), 0);
    }

    @Override
    public void disableOnTouch() {
        mEditView.setEnabled(false);
    }


    @Override
    public EditText getEditeView() {
        return mEditView;
    }
}
