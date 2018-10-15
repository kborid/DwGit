
package com.smartisanos.sara.lock.widget;
import android.content.Context;

import com.android.internal.widget.LockPatternUtils;
import com.smartisanos.sara.lock.util.LockPasswordUtil;
import com.smartisanos.sara.lock.util.SmartisanApi;

import smartisanos.util.LogTag;

public class SecureLockManager {
    public static final String TAG = "SecureLockManager";

    public static final int MAX_FAILED_ATTEMPTS = LockPasswordUtil.FAILED_ATTEMPTS_BEFORE_TIMEOUT;

    public static final int UNSECURE_LOCK_MODE = 0;
    public static final int SECURE_LOCK_MODE_PASSWORD_PATTERN = 1;
    public static final int SECURE_LOCK_MODE_PASSWORD_EASY = 2;
    public static final int SECURE_LOCK_MODE_PASSWORD_COMPLEX = 3;
    private LockPatternUtils mLockPatternUtils;

    private static SecureLockManager sInstance;

    private SecureLockManager(Context context) {
    }

    public static synchronized SecureLockManager getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new SecureLockManager(context.getApplicationContext());
        }

        return sInstance;
    }


    public void fingerprintStopListening() {
    }

    public void faceIdStopListening() {

    }

    public boolean isKeyguardLocked() {
        return false;
    }

    public int getSecureLockMode() {
        int mode = UNSECURE_LOCK_MODE;
        return mode;
    }

    public LockPatternUtils getLockPatternUtils() {
        return mLockPatternUtils;
    }

    public void setFingerprintVerify(boolean verify) {
    }

    public void setFaceIdVerify(boolean verify) {

    }

    public boolean isKeyguardVerified() {

        return false;
    }

    public void verifyKeyguardSecurely(final SmartisanApi.OnKeyguardVerifyResultSmto callback, String pwd) {
    }

    public boolean lockScreenFaceIdEnable() {
        return false;
    }

    public static boolean isFaceIdEnable(Context context) {
        return false;
    }

    public static boolean isFingerprintIsEnable(Context context) {
        return false;
    }

}
