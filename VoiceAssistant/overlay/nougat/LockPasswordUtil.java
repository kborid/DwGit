package com.smartisanos.sara.lock.util;

import android.content.Context;
import android.app.ActivityManager;
import android.os.SystemClock;

import com.android.internal.widget.LockPatternChecker;
import com.android.internal.widget.LockPatternUtils;
import com.android.internal.widget.LockPatternView;
import android.os.UserHandle;
import java.util.List;

public class LockPasswordUtil {

    private static final String TAG = "LockPasswordUtil";

    public static final int FAILED_ATTEMPTS_BEFORE_TIMEOUT = 5;

    public static boolean isVisiblePatternEnabled(LockPatternUtils lpUtils) {
        return lpUtils.isVisiblePatternEnabled(ActivityManager.getCurrentUser());
    }

    public static boolean isSecure(LockPatternUtils lpUtils) {
        return lpUtils.isSecure(ActivityManager.getCurrentUser());
    }

    public static int getKeyguardStoredPasswordQualityWrapWifiAuth(LockPatternUtils lpUtils) {
        return SmartisanApi.getKeyguardStoredPasswordQualityWrapWifiAuth(lpUtils);
    }

    public static int getKeyguardStoredPasswordQuality(LockPatternUtils lpUtils) {
        return lpUtils.getKeyguardStoredPasswordQuality(ActivityManager.getCurrentUser());
    }

    public static void reportSuccessfulPasswordAttempt(LockPatternUtils lpUtils) {
        lpUtils.reportSuccessfulPasswordAttempt(ActivityManager.getCurrentUser());
    }

    public static void reportFailedPasswordAttempt(LockPatternUtils lpUtils) {
        lpUtils.reportFailedPasswordAttempt(ActivityManager.getCurrentUser());
    }

    public static boolean isLockPasswordEnabled(LockPatternUtils lpUtils) {
        return lpUtils.isLockPasswordEnabled(ActivityManager.getCurrentUser());
    }

    public static boolean isLockPatternEnabled(LockPatternUtils lpUtils) {
        return lpUtils.isLockPatternEnabled(ActivityManager.getCurrentUser());
    }

    public static void checkPassword(LockPatternUtils lpUtils, String password, final CheckCallback callback) {
        LockPatternChecker.checkPassword(lpUtils, password, ActivityManager.getCurrentUser(),
                new LockPatternChecker.OnCheckCallback() {
                    @Override
                    public void onChecked(boolean matched, int timeoutMs) {
                        callback.checkResult(matched, timeoutMs);
                    }
                });
    }

    public static void checkPattern(LockPatternUtils lpUtils, List<LockPatternView.Cell> pattern,final CheckCallback callback) {
        LockPatternChecker.checkPattern(lpUtils, pattern, ActivityManager.getCurrentUser(),
                new LockPatternChecker.OnCheckCallback() {
                    @Override
                    public void onChecked(boolean matched, int timeoutMs) {
                        callback.checkResult(matched, timeoutMs);
                    }
                });
    }

    public static long getSecondsRemaining(Context context, LockPatternUtils lpUtils) {
        long deadline = lpUtils.getLockoutAttemptDeadline(ActivityManager.getCurrentUser());
        final long now = SystemClock.elapsedRealtime();
        long timeoutms = deadline - now;
        if (deadline != 0 && timeoutms > 0) {
            return timeoutms;
        }
        return 0;
    }

    public static long getLockoutAttemptDeadline(LockPatternUtils lpUtils) {
        return lpUtils.getLockoutAttemptDeadline(ActivityManager.getCurrentUser());
    }

    public static long setLockoutAttemptDeadline(LockPatternUtils lpUtils, int timeoutMs) {
        return lpUtils.setLockoutAttemptDeadline(ActivityManager.getCurrentUser(), timeoutMs);
    }

    public interface CheckCallback {
        void checkResult(boolean matched, int timeoutMs);
    }

    public static int getEasyPasswordLength(Context context) {
        LockPatternUtils lockUtils = new LockPatternUtils(context);
        return (int) lockUtils.getPasswordLengthSmartisan(UserHandle.myUserId());
    }
}
