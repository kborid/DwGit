
package com.smartisanos.sara.util;

import android.text.TextUtils;
import android.util.Log;
import android.os.SystemProperties;

public class LogUtils {
    public static final boolean DEBUG = (SystemProperties.getInt("ro.debuggable", 0) == 1);
    private static final String DEF_TAG = "VoiceAss";

    public static void d(String tag, String message) {
        if (!DEBUG) {
            return;
        }
        TagInfo tagInfo = getMethodNames(new Throwable().getStackTrace());
        String msg = createLogWithoutFileName(tagInfo, message);
        Log.d(tag, msg);
    }

    public static void d(String message) {
        if (!DEBUG) {
            return;
        }
        TagInfo tagInfo = getMethodNames(new Throwable().getStackTrace());
        String msg = createLogWithoutFileName(tagInfo, message);
        Log.d(tagInfo.fileName, msg);
    }

    public static void d(Throwable ex) {
        if (!DEBUG) {
            return;
        }
        // Throwable instance must be created before any methods
        TagInfo tagInfo = getMethodNames(ex.getStackTrace());
        Log.d(tagInfo.fileName, "", ex);
    }

    public static void df(String message) {
        if (!DEBUG) {
            return;
        }
        TagInfo tagInfo = getMethodNames(new Throwable().getStackTrace());
        String msg = createLogWithoutFileName(tagInfo, message);
        Log.d(DEF_TAG, msg);
    }

    public static void i(String tag, String message) {
        if (!DEBUG) {
            return;
        }
        TagInfo tagInfo = getMethodNames(new Throwable().getStackTrace());
        String msg = createLogWithoutFileName(tagInfo, message);
        Log.i(tag, msg);
    }

    public static void i(String message) {
        if (!DEBUG) {
            return;
        }
        TagInfo tagInfo = getMethodNames(new Throwable().getStackTrace());
        String msg = createLogWithoutFileName(tagInfo, message);
        Log.i(tagInfo.fileName, msg);
    }

    public static void w(String tag, String message) {
        TagInfo tagInfo = getMethodNames(new Throwable().getStackTrace());
        String msg = createLogWithoutFileName(tagInfo, message);
        Log.w(tag, msg);
    }

    public static void w(String message) {
        TagInfo tagInfo = getMethodNames(new Throwable().getStackTrace());
        String msg = createLogWithoutFileName(tagInfo, message);
        Log.w(tagInfo.fileName, msg);
    }

    public static void w(Throwable ex) {
        // Throwable instance must be created before any methods
        TagInfo tagInfo = getMethodNames(ex.getStackTrace());
        Log.w(tagInfo.fileName, "", ex);
    }

    public static void e(String tag, String message) {
        TagInfo tagInfo = getMethodNames(new Throwable().getStackTrace());
        String msg = createLogWithoutFileName(tagInfo, message);
        Log.e(tag, msg);
    }

    public static void e(String message) {
        TagInfo tagInfo = getMethodNames(new Throwable().getStackTrace());
        String msg = createLogWithoutFileName(tagInfo, message);
        Log.e(tagInfo.fileName, msg);
    }

    public static void e(Throwable ex) {
        // Throwable instance must be created before any methods
        TagInfo tagInfo = getMethodNames(ex.getStackTrace());
        Log.e(tagInfo.fileName, "", ex);
    }

    public static void error(Throwable ex) {
        // Throwable instance must be created before any methods
        TagInfo tagInfo = getMethodNames(ex.getStackTrace());
        Log.e(tagInfo.fileName, "", ex);
    }

    public static void infoRelease(String tag, String message) {
        if (!DEBUG) {
            Log.i(tag, message);
        } else {
            TagInfo tagInfo = getMethodNames(new Throwable().getStackTrace());
            String msg = createLogWithoutFileName(tagInfo, message);
            Log.i(tag, msg);
        }
    }

    public static void infoRelease(String message) {
        TagInfo tagInfo = getMethodNames(new Throwable().getStackTrace());
        String msg = createLogWithoutFileName(tagInfo, message);
        Log.i(tagInfo.fileName, msg);
    }

    public static void printStackTrace() {
        if (!DEBUG) {
            return;
        }
        Throwable throwable = new Throwable();
        Log.w(DEF_TAG, Log.getStackTraceString(throwable));
    }

    public static String filterSensitiveLog(String sensitiveLog) {
        if (TextUtils.isEmpty(sensitiveLog)) {
            return "[empty]";
        } else {
            if (!DEBUG) {
                return "[size:" + sensitiveLog.length() + "]";
            } else {
                return "[" + sensitiveLog + "]";
            }
        }
    }

    private static String createLogWithoutFileName(TagInfo tagInfo, String log, Object... args) {
        StringBuilder buffer = new StringBuilder();
        buffer.append("[");
        buffer.append(tagInfo.methodName);
        buffer.append("():");
        buffer.append(tagInfo.lineNumber);
        buffer.append("] ");
        buffer.append(formatString(log, args));
        return buffer.toString();
    }

    private static TagInfo getMethodNames(StackTraceElement[] sElements) {
        TagInfo info = new TagInfo();
        if (sElements.length > 1) {
            info.fileName = sElements[1].getFileName();
            if (info.fileName.endsWith(".java")) {
                info.fileName = info.fileName.substring(0, info.fileName.length() - 5);
            }
            info.methodName = sElements[1].getMethodName();
            info.lineNumber = sElements[1].getLineNumber();
        }
        return info;
    }

    private static String formatString(String message, Object... args) {
        return args.length == 0 ? message : String.format(message, args);
    }

    static class TagInfo {
        String fileName;
        String methodName;
        int lineNumber;
    }
}
