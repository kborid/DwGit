
package com.smartisanos.sara.lock.util;

import android.content.Context;
import android.database.ContentObserver;
import android.hardware.fingerprint.FingerprintManager;
import android.net.Uri;
import android.os.CancellationSignal;
import android.os.Handler;
import android.os.Vibrator;
import android.provider.Settings;

import smartisanos.util.LogTag;

public class FingerprintController {
    private static final String TAG = "FingerprintController";

    public static final int FINGERPRINT_ERROR_LOCKOUT = FingerprintManager.FINGERPRINT_ERROR_LOCKOUT;
    private static final int FINGERPRINT_ERROR_GOOD_IN_LOCKOUT = FingerprintManager.FINGERPRINT_ERROR_GOOD_IN_LOCKOUT;
    private static final int FINGERPRINT_ERROR_FAILED_IN_LOCKOUT = FingerprintManager.FINGERPRINT_ERROR_FAILED_IN_LOCKOUT;
    private static final int FINGERPRINT_ERROR_AUTHENTICATION_IN_LOCKOUT = FingerprintManager.FINGERPRINT_ERROR_AUTHENTICATION_IN_LOCKOUT;

    private static final String USE_FINGERPRINT_IN_LOCKSCREEN = "use_fingerprint_in_lockscreen";

    private static FingerprintController mInstance = null;

    private FingerprintManager mFingerprintManager;
    private Callback mCallback;
    private CancellationSignal mCancellationSignal;

    private boolean mSelfCancelled;
    private Context mContext;
    private Handler mHandler;

    private int mErrorType;
    private int mRetryCount;

    private boolean mLockScreenFingerprintEnable;
    private boolean mFingerprintSucceeded;
    private boolean mInFingerprintLockout;
    private boolean mPasswordLockout;
    private String mCurrentClient;
    private FingerprintManager.FeedbackCallback mFeedbackCallback = new FingerprintManager.FeedbackCallback() {

        @Override
        public void onFeedback(int type, String msg) {
            if (type == 0) {
                mCurrentClient = msg;
            }
        }

    };


    private ContentObserver mSettingsObserver = new ContentObserver(mHandler) {
        @Override
        public void onChange(boolean selfChange, Uri uri){
            mHandler.removeCallbacks(mGetLockScreenEnableRunnable);
            mHandler.post(mGetLockScreenEnableRunnable);
        }
    };

    private Runnable mGetLockScreenEnableRunnable = new Runnable(){
        @Override
        public void run(){
            mLockScreenFingerprintEnable = isLockScreenFingerprintEnable(mContext);
            LogTag.d(TAG, "mGetLockScreenEnableRunnable: " + mLockScreenFingerprintEnable);
        }
    };

    private Runnable mResetTask = new Runnable() {
        @Override
        public void run() {
            registerFingerprint();
        }
    };

    public static FingerprintController getInstance(Context context) {
        if (mInstance == null) {
            mInstance = new FingerprintController(context);
        }
        return mInstance;
    }

    private FingerprintController(Context context) {
        mFingerprintManager = (FingerprintManager) context.getSystemService(
                Context.FINGERPRINT_SERVICE);
        mFingerprintManager.addLockoutResetCallback(mResetCallback);
        mContext = context;
        mHandler = new Handler();
        mSelfCancelled = true;
        mContext.getContentResolver().registerContentObserver(
                Settings.Global.getUriFor(USE_FINGERPRINT_IN_LOCKSCREEN), false, mSettingsObserver);
        mLockScreenFingerprintEnable = isLockScreenFingerprintEnable(context);
    }

    public void setCallback(Callback callback) {
        mCallback = callback;
    }

    public void setPasswordLockout(boolean lockout) {
        mPasswordLockout = lockout;
    }

    public boolean isFingerprintAuthAvailable() {
        return mLockScreenFingerprintEnable
                && mFingerprintManager.isHardwareDetected()
                && mFingerprintManager.hasEnrolledFingerprints();
    }

    public void startListening() {
        LogTag.d(TAG, "startListening");
        if (!isFingerprintAuthAvailable()) {
            return;
        }
        mRetryCount = 0;
        mSelfCancelled = false;
        registerFingerprint();
    }

    public void stopListening() {
        if (mCancellationSignal != null) {
            LogTag.d(TAG, "stopListening");
            mHandler.removeCallbacks(mResetTask);
            mSelfCancelled = true;
            mCancellationSignal.cancel();
            mCancellationSignal = null;
        }
    }
    private void registerFingerprint_6_0() {
        LogTag.d(TAG, "registerFingerprint");
        if (!mSelfCancelled && isFingerprintAuthAvailable() && mRetryCount < 10) {
            mRetryCount++;
            mFingerprintManager.getCurrentClient(new FingerprintManager.FeedbackCallback() {
                public void onFeedback(int type, String currentClient) {
                    if ("com.smartisanos.keyguard".equals(currentClient)) {
                        LogTag.d(TAG, "current client is keyguard, mRetryCount = " + mRetryCount);
                        mHandler.removeCallbacks(mResetTask);
                        mHandler.postDelayed(mResetTask, 300);
                        return;
                    } else {
                        if (!mSelfCancelled && isFingerprintAuthAvailable()) {
                            mHandler.removeCallbacks(mResetTask);
                            mCancellationSignal = new CancellationSignal();
                            mFingerprintManager.authenticateWithType(null, mCancellationSignal,
                                    0 /* flags */, mAuthCallback, null,
                                    FingerprintManager.CLIENT_TYPE_NEED_FEEDBACK_IN_LOCKOUT);
                        }
                    }
                }
            });
        }
    }
    private void registerFingerprint() {
        int sdk = android.os.Build.VERSION.SDK_INT;
        if (sdk < 24) {
            registerFingerprint_6_0();
            return;
        }
        if (!mSelfCancelled && isFingerprintAuthAvailable() && mRetryCount < 10) {
            mRetryCount++;
            if ("com.smartisanos.keyguard".equals(mCurrentClient)) {
                LogTag.d(TAG, "current client is keyguard, mRetryCount = " + mRetryCount);
                mHandler.removeCallbacks(mResetTask);
                mHandler.postDelayed(mResetTask, 300);
                return;
            } else {
                if (!mSelfCancelled && isFingerprintAuthAvailable()) {
                    mHandler.removeCallbacks(mResetTask);
                    mCancellationSignal = new CancellationSignal();
                    mFingerprintManager.authenticateWithType(null, mCancellationSignal,
                            0 /* flags */, mAuthCallback, null,
                            FingerprintManager.CLIENT_TYPE_NEED_FEEDBACK_IN_LOCKOUT);
                }
            }
        }
    }

    private FingerprintManager.AuthenticationCallback mAuthCallback = new FingerprintManager.AuthenticationCallback() {
        @Override
        public void onAuthenticationError(int errMsgId, CharSequence errString) {
            LogTag.d(TAG, "onAuthenticationError: " + errMsgId);
            mErrorType = errMsgId;
            if (mSelfCancelled) return;
            if (errMsgId == FINGERPRINT_ERROR_LOCKOUT
                    || errMsgId == FINGERPRINT_ERROR_GOOD_IN_LOCKOUT
                    || errMsgId == FINGERPRINT_ERROR_FAILED_IN_LOCKOUT) {
                vibrate();
                if (mCallback != null){
                    mCallback.onAuthenticationLockout(false);
                }
                mInFingerprintLockout = true;
            } else if (errMsgId == FINGERPRINT_ERROR_AUTHENTICATION_IN_LOCKOUT) {
                if (mCallback != null){
                    mCallback.onAuthenticationLockout(true);
                }
            }
            if (mErrorType == FINGERPRINT_ERROR_GOOD_IN_LOCKOUT
                    || mErrorType == FINGERPRINT_ERROR_GOOD_IN_LOCKOUT
                    ||mFingerprintSucceeded) {
                mHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        stopListening();
                        startListening();
                    }
                }, 200);
            }
        }

        @Override
        public void onAuthenticationAcquired(int acquireInfo) {
            LogTag.d(TAG, "onAuthenticationAcquired: " + acquireInfo);
        }

        @Override
        public void onAuthenticationFailed() {
            LogTag.d(TAG, "onAuthenticationFailed");
            vibrate();
            mFingerprintSucceeded = false;
            if (mPasswordLockout) {
                if (mCallback != null) {
                    mCallback.onAuthenticationLockout(false);
                }
            } else {
                if (mCallback != null) {
                    mCallback.onAuthenticationFailed();
                }
            }
        }

        @Override
        public void onAuthenticationSucceeded(FingerprintManager.AuthenticationResult result) {
            LogTag.d(TAG, "onAuthenticationSucceeded");
            mFingerprintSucceeded = true;
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    if (mPasswordLockout) {
                        vibrate();
                        if (mCallback != null) {
                            mCallback.onAuthenticationLockout(false);
                        }
                    } else {
                        if (mCallback != null) {
                            mCallback.onAuthenticated();
                        }
                    }
                }
            });
        }
    };

    private FingerprintManager.LockoutResetCallback mResetCallback = new FingerprintManager.LockoutResetCallback() {
        @Override
        public void onLockoutReset() {
            LogTag.d(TAG, "onLockoutReset");
            if (mSelfCancelled) return;
            mInFingerprintLockout = false;
            if (mCallback != null){
                mCallback.onAuthenticationReset();
            }
            stopListening();
            startListening();
        }
    };

    private void vibrate() {
        Vibrator vibrator = (Vibrator) mContext.getSystemService(Context.VIBRATOR_SERVICE);
        if (vibrator != null) {
            vibrator.vibrate(new long[] {0, 200}, -1);
        }
    }

    private boolean isLockScreenFingerprintEnable(Context context) {
        return Settings.Global.getInt(context.getContentResolver(),
                USE_FINGERPRINT_IN_LOCKSCREEN,0) == 1;
    }

    public interface Callback {

        void onAuthenticated();

        void onAuthenticationReset();

        void onAuthenticationFailed();

        void onAuthenticationLockout(boolean init);
    }
}
