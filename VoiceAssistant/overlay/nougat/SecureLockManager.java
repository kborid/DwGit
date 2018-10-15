
package com.smartisanos.sara.lock.widget;

import android.app.KeyguardManager;
import android.app.KeyguardManager.OnKeyguardFaceIDVerifyResult;
import android.app.KeyguardManager.OnKeyguardFingerprintVerifyResult;
import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.provider.Settings;

import com.android.internal.widget.LockPatternUtils;
import com.smartisanos.sara.lock.util.FingerprintController;
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

    private static final int PASSWORD_QUALITY_WIFI_AUTH = -1;

    private static SecureLockManager sInstance;
    private Context mContext;
    private LockPatternUtils mLockPatternUtils;
    private KeyguardManager mKeyguardManager;

    private boolean mFingerprintVerified;
    private boolean mFaceIdVerified;

    private Handler mHandler;
    private boolean mLockScreenFaceIdEnable;

    private ContentObserver mSettingsObserver = new ContentObserver(mHandler) {
        @Override
        public void onChange(boolean selfChange, Uri uri){
            mHandler.removeCallbacks(mGetFaceIdEnableRunnable);
            mHandler.post(mGetFaceIdEnableRunnable);
        }
    };

    private Runnable mGetFaceIdEnableRunnable = new Runnable() {
        @Override
        public void run() {
            mLockScreenFaceIdEnable = isFaceIdEnable(mContext);
            LogTag.d(TAG, "mLockScreenFaceIdEnable: " + mLockScreenFaceIdEnable);
        }
    };

    private SecureLockManager(Context context) {
        mContext = context;
        mHandler = new Handler();
        mLockPatternUtils = new LockPatternUtils(context);
        mKeyguardManager = (KeyguardManager) context.getSystemService(Context.KEYGUARD_SERVICE);
        mContext.getContentResolver().registerContentObserver(
                Settings.Global.getUriFor(SmartisanApi.FACE_DATA_LOCK_SCREEN_ENABLE), false, mSettingsObserver);
        mLockScreenFaceIdEnable = isFaceIdEnable(mContext);
    }

    public static synchronized SecureLockManager getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new SecureLockManager(context.getApplicationContext());
        }

        return sInstance;
    }

    public void fingerprintStartListening(final OnKeyguardFingerprintVerifyResult callback) {
        mKeyguardManager.registerFingerprintVerifyCallback(callback, true);
    }

    public void fingerprintStopListening() {
        mKeyguardManager.registerFingerprintVerifyCallback(null, false);
    }

    public void faceIdStartListening(final OnKeyguardFaceIDVerifyResult callback) {
        LogTag.d(TAG, "====faceIdStartListening====");
        mKeyguardManager.registerFaceIDVerifyCallback(callback, true);
    }

    public void faceIdStopListening() {
        LogTag.d(TAG, "====faceIdStopListening==== ");
        mKeyguardManager.registerFaceIDVerifyCallback(null, false);
    }

    public boolean isKeyguardLocked() {
        return mKeyguardManager.isKeyguardLocked();
    }

    public int getSecureLockMode() {
        int mode = UNSECURE_LOCK_MODE;
        if (LockPasswordUtil.isSecure(mLockPatternUtils)) {
            int secureLockMode = FingerprintController.getInstance(mContext)
                    .isFingerprintAuthAvailable() || mLockScreenFaceIdEnable ? LockPasswordUtil
                    .getKeyguardStoredPasswordQuality(mLockPatternUtils)
                    : LockPasswordUtil.getKeyguardStoredPasswordQualityWrapWifiAuth(mLockPatternUtils);
            switch (secureLockMode) {
                case DevicePolicyManager.PASSWORD_QUALITY_SOMETHING:
                    mode = SECURE_LOCK_MODE_PASSWORD_PATTERN;
                    break;

                case DevicePolicyManager.PASSWORD_QUALITY_NUMERIC:
                case 0x30000: // PASSWORD_QUALITY_NUMERIC_COMPLEX = 0x30000
                    mode = SECURE_LOCK_MODE_PASSWORD_EASY;
                    break;

                case DevicePolicyManager.PASSWORD_QUALITY_ALPHABETIC:
                case DevicePolicyManager.PASSWORD_QUALITY_ALPHANUMERIC:
                case DevicePolicyManager.PASSWORD_QUALITY_COMPLEX:
                    mode = SECURE_LOCK_MODE_PASSWORD_COMPLEX;
                    break;

                case PASSWORD_QUALITY_WIFI_AUTH:
                    mode = UNSECURE_LOCK_MODE;
                    break;
            }
        }

        return mode;
    }

    public LockPatternUtils getLockPatternUtils() {
        return mLockPatternUtils;
    }

    public void setFingerprintVerify(boolean verify) {
        mFingerprintVerified = verify;
    }

    public void setFaceIdVerify(boolean verify) {
        mFaceIdVerified = verify;
    }

    public boolean isKeyguardVerified() {
        if (mKeyguardManager != null) {
            boolean keyguardVerified = mKeyguardManager.isKeyguardSecure() && mKeyguardManager.isKeyguardLocked()
                    && SmartisanApi.isKeyguardVerified(mKeyguardManager);
            LogTag.d(TAG, "isKeyguardVerified finger Verified: " + mFingerprintVerified
                    + " face id Verified: " + mFaceIdVerified + " keyguard Verified: " + keyguardVerified);
            return mFingerprintVerified || mFaceIdVerified
                    || keyguardVerified;
        }
        return false;
    }

    public void verifyKeyguardSecurely(final SmartisanApi.OnKeyguardVerifyResultSmto callback, String pwd) {
        SmartisanApi.verifyKeyguardSecurely(mKeyguardManager, callback, pwd);
    }

    public boolean lockScreenFaceIdEnable() {
        return mLockScreenFaceIdEnable;
    }

    public static boolean isFaceIdEnable(Context context) {
        return Settings.Global.getInt(context.getContentResolver(),
                SmartisanApi.FACE_DATA_LOCK_SCREEN_ENABLE, 0) == 1;
    }

    public static boolean isFingerprintIsEnable(Context context) {
        return Settings.Global.getInt(context.getContentResolver(),
                SmartisanApi.USE_FINGERPRINT_IN_LOCKSCREEN, 0) == 1;
    }
}
