
package com.smartisanos.sara.lock.widget;
import android.content.Context;

import android.os.IBinder;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.smartisanos.sara.R;
import com.smartisanos.sara.lock.util.SmartisanApi;
public abstract class LockPasswordLayout extends LinearLayout implements SmartisanApi.OnKeyguardVerifyResultSmto {

    // how long before we clear the wrong pattern
    protected static final int CLEAR_PASSWORD_TIMEOUT_MS = 400;
    private static final String TAG = "LockPasswordLayout";
    protected UnlockResultCallback mCallback;
    protected SecureLockManager mSecureLockManager;
    private boolean mFpLockout;

    public LockPasswordLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    protected Runnable mCancelRunnable = new Runnable() {
        public void run() {
        }
    };

    public void setUnlockResultCallBack(UnlockResultCallback callback) {
        mCallback = callback;
    }

    public void onUnlockSuccess() {

    }

    public boolean onUnlockFailed(final int timeoutMs) {

        return false;
    }

    public long getSecondsRemaining() {
        return 0;
    }

    public int getTotalFailedAttempts() {
      return 0;
    }

    private void setTotalFailedAttempts(int count) {
    }

    public int getTipTextColor(boolean error) {
        return 0;
    }

    public void showSoftInputIfNeedly() {
    }

    public void onCheckLockFreezed() {

    }

    public boolean hideSoftInputIfNeedly() {
        return false;
    }

    public void unlockFailedVibrate() {

    }

    public void setTipNormalText(TextView text, int textId, int color) {

    }

    public void setFingerprintListening(boolean enable) {

    }

    public void setFaceIdListening(boolean enable) {

    }

    public boolean isPasswordTimeout() {
        return false;
    }

    public boolean isFingerprintLockout() {
        return mFpLockout;
    }

    public boolean isKeyStoreUnlocked() {
        return false;
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        return super.dispatchKeyEvent(event);
    }

    @Override
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();

    }

    public void onKeyguardVerifyResult(final boolean success, final int timeoutMs) {
    }

    public EditText getEditeView() {
        return null;
    }


    public void playShakeAnimation(final View view) {
    }

    public void updatePasswordLayout() {

    }

    public String getFaceTipString() {
        if (mSecureLockManager.isFaceIdEnable(mContext)) {
            return mContext.getResources().getString(R.string.faceid_or_password_pattern);
        } else {
            return null;
        }
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
