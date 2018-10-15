package com.smartisanos.ideapills.util;

import android.text.TextUtils;
import android.util.Log;
import android.os.SystemProperties;

public class LOG {
    public static final String TAG = "PILLS";

    public static final boolean DBG = SystemProperties.getInt("ro.debuggable", 0) == 1;

    private String mClassName = null;

    public static LOG DEF = getInstance(LOG.class);

    public static LOG getInstance(Class owner) {
        return new LOG(owner);
    }

    private LOG(Class owner) {
        if (owner == null) {
            throw new IllegalArgumentException("LOG must be init by class object");
        }
        mClassName = owner.getSimpleName();
    }

    public static void trace() {
        trace(null);
    }

    public static void trace(String info) {
        if (!DBG) {
            return;
        }
        Exception ex = new Exception();
        if (!TextUtils.isEmpty(info)) {
            Log.e(TAG, info);
        }
        Log.e(TAG, "########## trace begin  ##########");
        Log.e(TAG, Log.getStackTraceString(ex));
        Log.e(TAG, "########## trace finish ##########");
    }

    public void info(String info) {
        Log.i(TAG, getLogString(info));
    }

    public void warn(String info) {
        Log.w(TAG, getLogString(info));
    }

    public void error(String info) {
        Log.e(TAG, getLogString(info));
    }

    public void error(Exception e) {
        Log.e(TAG, getLogString(e.toString()), e);
    }

    private String getLogString(String info) {
        if (mClassName != null) {
            return mClassName + " : " + info;
        }
        return info;
    }

    public void assertIfDebug(String msg) {
        if (DBG) {
            throw new RuntimeException(msg);
        } else {
            error(msg);
        }
    }

    public static void d(String message) {
        if (!DBG) {
            return;
        }
        TagInfo tagInfo = getMethodNames(new Throwable().getStackTrace());
        String msg = createLogWithoutFileName(tagInfo, message);
        Log.d(tagInfo.fileName, msg);
    }

    public static void d(String tag, String message) {
        if (!DBG) {
            return;
        }
        TagInfo tagInfo = getMethodNames(new Throwable().getStackTrace());
        String msg = createLogWithoutFileName(tagInfo, message);
        Log.d(tag, msg);
    }

    public static void d(Throwable ex) {
        if (!DBG) {
            return;
        }
        // Throwable instance must be created before any methods
        TagInfo tagInfo = getMethodNames(ex.getStackTrace());
        Log.d(tagInfo.fileName, "", ex);
    }

    public static void i(String tag, String message) {
        if (!DBG) {
            return;
        }
        TagInfo tagInfo = getMethodNames(new Throwable().getStackTrace());
        String msg = createLogWithoutFileName(tagInfo, message);
        Log.i(tag, msg);
    }

    public static void i(String message) {
        if (!DBG) {
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
        if (!DBG) {
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
