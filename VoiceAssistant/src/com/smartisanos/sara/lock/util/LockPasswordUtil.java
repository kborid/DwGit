package com.smartisanos.sara.lock.util;

import android.content.Context;

import com.android.internal.widget.LockPatternUtils;
import com.android.internal.widget.LockPatternView;
import android.os.UserHandle;
import java.util.List;

public class LockPasswordUtil {

    private static final String TAG = "LockPasswordUtil";

    public static final int FAILED_ATTEMPTS_BEFORE_TIMEOUT = 5;

    public static boolean isVisiblePatternEnabled(LockPatternUtils lpUtils) {
        return false;
    }

    public static boolean isSecure(LockPatternUtils lpUtils) {
        return false;
    }

    public static int getKeyguardStoredPasswordQualityWrapWifiAuth(LockPatternUtils lpUtils) {
        return SmartisanApi.getKeyguardStoredPasswordQualityWrapWifiAuth(lpUtils);
    }

    public static int getKeyguardStoredPasswordQuality(LockPatternUtils lpUtils) {
        return 0;
    }

    public static void reportSuccessfulPasswordAttempt(LockPatternUtils lpUtils) {
    }

    public static void reportFailedPasswordAttempt(LockPatternUtils lpUtils) {
    }

    public static boolean isLockPasswordEnabled(LockPatternUtils lpUtils) {
        return false;
    }

    public static boolean isLockPatternEnabled(LockPatternUtils lpUtils) {
        return false;
    }

    public static void checkPassword(LockPatternUtils lpUtils, String password, final CheckCallback callback) {
    }

    public static void checkPattern(LockPatternUtils lpUtils, List<LockPatternView.Cell> pattern,final CheckCallback callback) {
    }

    public static long getSecondsRemaining(Context context, LockPatternUtils lpUtils) {
        return 0;
    }

    public static long getLockoutAttemptDeadline(LockPatternUtils lpUtils) {
        return -1;
    }

    public static long setLockoutAttemptDeadline(LockPatternUtils lpUtils, int timeoutMs) {
        return -1;
    }

    public interface CheckCallback {
        void checkResult(boolean matched, int timeoutMs);
    }

    public static int getEasyPasswordLength(Context context) {
        return 4;
    }
}
