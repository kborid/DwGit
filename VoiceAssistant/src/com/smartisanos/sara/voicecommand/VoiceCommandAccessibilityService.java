package com.smartisanos.sara.voicecommand;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.Intent;
import android.graphics.Rect;
import android.text.TextUtils;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import com.smartisanos.sara.R;
import com.smartisanos.sara.util.SaraConstant;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public class VoiceCommandAccessibilityService extends AccessibilityService {

    private static final String TAG = VoiceCommandAccessibilityService.class.getSimpleName();
    private static final int CACHE_RESERVE_TIME = 500;

    private static VoiceCommandAccessibilityService sInstance;

    private CharSequence mLatestPackage;
    private CharSequence mLatestWindowTitle;
    private CharSequence mLatestText;
    private Rect mLatestRect;
    private CharSequence mCachedLatestText;
    private Rect mCachedLatestRect;
    private long mCacheUpdateTime;

    public boolean mStopUpdateText = false;

    static VoiceCommandAccessibilityService getInstance() {
        return sInstance;
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null) {
            return;
        }

        switch (event.getEventType()) {
            case AccessibilityEvent.TYPE_WINDOWS_CHANGED:
                CharSequence title = reflectGetWindowTitle(root);
                CharSequence voiceCommandLabel = VoiceCommandUtils.getActivityLabel(this,
                        new Intent(this, VoiceCommondActivity.class));
                if (TextUtils.equals(getPackageName(), root.getPackageName()) &&
                        TextUtils.equals(voiceCommandLabel, title)) {
                    setStopUpdateText(true);
                    return;
                }

                mLatestPackage = root.getPackageName();

                mLatestWindowTitle = title;

                setStopUpdateText(false);
                break;
            case AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED:
            case AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUSED:
            case AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUS_CLEARED:
                if (!mStopUpdateText) {
                    // just use active window to search focus,
                    // do not use "event.getSource" to avoid get a navigation bar focus.
                    prepareTextFromScreen(root);
                }
                break;
        }
    }

    @Override
    public void onServiceConnected() {
        final AccessibilityServiceInfo info = getServiceInfo();
        if (info == null) {
            // If we fail to obtain the service info, the service is not really
            // connected and we should avoid setting anything up.
            return;
        }

        // only accept these events.
        info.eventTypes = AccessibilityEvent.TYPE_WINDOWS_CHANGED |
                AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED |
                AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUSED |
                AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUS_CLEARED;
        setServiceInfo(info);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCrate()...");
        sInstance = this;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        sInstance = null;
    }

    @Override
    public void onInterrupt() {
        // NA
    }

    private void setStopUpdateText(boolean stop) {
        Log.d(TAG, "stop update text!" + stop);
        mStopUpdateText = stop;

        if (stop) {
            // if no valid focus item, try get it from cache.
            if (mLatestRect == null &&
                    (System.currentTimeMillis() - mCacheUpdateTime < CACHE_RESERVE_TIME)) {
                Log.d(TAG, "set latest text and rect from cache : " + mCachedLatestText);
                mLatestText = mCachedLatestText;
                mLatestRect = mCachedLatestRect;
            }
        }
    }

    public void prepareTextFromScreen(AccessibilityNodeInfo source) {
        if (source != null) {
            AccessibilityNodeInfo result = source.findFocus(AccessibilityNodeInfo.FOCUS_ACCESSIBILITY);
            if (result != null) {
                mLatestText = getTextFromNodeInfo(result);

                mLatestRect = new Rect();
                result.getBoundsInScreen(mLatestRect);
            } else {
                if (mLatestRect != null) {
                    mCachedLatestText = mLatestText;
                    mCachedLatestRect = mLatestRect;
                    mCacheUpdateTime = System.currentTimeMillis();
                }

                mLatestText = null;
                mLatestRect = null;
            }
        }
    }

    public CharSequence getLatestPackage() {
        Log.d(TAG, "getLatestPackage: " + mLatestPackage);
        return mLatestPackage;
    }

    public CharSequence getLatestWindowTitle() {
        Log.d(TAG, "getLatestWindowTitle: " + mLatestWindowTitle);
        return mLatestWindowTitle;
    }

    public CharSequence getAccessibilityFocusText() {
        Log.d(TAG, "getAccessibilityFocusText: " + mLatestText);
        return mLatestText;
    }

    public Rect getAccessibilityFocusRect() {
        Log.d(TAG, "getAccessibilityFocusRect: " + mLatestRect);
        return mLatestRect;
    }

    public void clickVoiceButton(String description) {
        boolean success = false;
        if (clickGlobalVoiceButton(description)) {
            success = true;
        } else {
            AccessibilityNodeInfo root = getRootInActiveWindow();

            if (root == null) {
                Log.e(TAG, "clickVoiceButton : active node is null");
            } else {
                List<AccessibilityNodeInfo> results = new ArrayList<AccessibilityNodeInfo>();
                matchNodeRecursive(root, description, results);

                for (AccessibilityNodeInfo node : results) {
                    Log.d(TAG, "match nodes: " + node);
                }

                for (AccessibilityNodeInfo target : results) {
                    if (performClickRecursive(target)) {
                        success = true;
                        break;
                    }
                }
            }
        }

        Intent result = new Intent(SaraConstant.ACTION_VOICE_BUTTON_RESULT);
        result.putExtra(SaraConstant.EXTRA_VOICE_BUTTON_RESULT, success);
        sendBroadcast(result);
    }

    private boolean clickGlobalVoiceButton(String description) {
        if (VoiceCommandUtils.matchCommand(getString(R.string.voice_command_global_back), description)) {
            performGlobalAction(GLOBAL_ACTION_BACK);
        } else if (VoiceCommandUtils.matchCommand(getString(R.string.voice_command_global_home), description)) {
            performGlobalAction(GLOBAL_ACTION_HOME);
        } else if (VoiceCommandUtils.matchCommand(getString(R.string.voice_command_global_recents), description)) {
            performGlobalAction(GLOBAL_ACTION_RECENTS);
        } else {
            return false;
        }

        return true;
    }

    private void matchNodeRecursive(AccessibilityNodeInfo node, String description, List<AccessibilityNodeInfo> results) {
        if (node == null)
            return;

        if (matchNode(node, description)) {
            results.add(node);
        }

        for (int i = 0; i < node.getChildCount(); ++i) {
            matchNodeRecursive(node.getChild(i), description, results);
        }
    }

    private boolean matchNode(AccessibilityNodeInfo node, String cmd) {
        return node != null &&
                (VoiceCommandUtils.matchCommand(cmd, node.getText()) ||
                        VoiceCommandUtils.matchCommand(cmd, node.getContentDescription()));
    }

    private boolean performClickRecursive(AccessibilityNodeInfo node) {
        if (node != null) {
            if (node.isEnabled() && node.isClickable() && node.isVisibleToUser()) {
                return node.performAction(AccessibilityNodeInfo.ACTION_CLICK);
            } else {
                return performClickRecursive(node.getParent());
            }
        }

        return false;
    }

    private static CharSequence getTextFromNodeInfo(AccessibilityNodeInfo node) {
        if (node == null) {
            return "";
        }

        if (!TextUtils.isEmpty(node.getText())) {
            return node.getText();
        }
        if (node.getChildCount() <= 0) {
            if (TextUtils.isEmpty(node.getText()) && "android.view.View".equals(node.getClassName())) {
                return node.getContentDescription();
            } else {
                return node.getText();
            }
        } else {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < node.getChildCount(); ++i) {
                CharSequence childText = getTextFromNodeInfo(node.getChild(i));
                if (!TextUtils.isEmpty(childText)) {
                    sb.append(",");
                    sb.append(childText);
                }
            }

            if (sb.length() > 0) {
                return sb.substring(1);
            } else {
                return sb.toString();
            }
        }
    }

    public CharSequence reflectGetWindowTitle(AccessibilityNodeInfo node) {
        CharSequence title = null;

        if (node != null) {
            Object window = invokeMethodNoParams(node, "getWindow");
            if (window != null) {
                Object result = invokeMethodNoParams(window, "getTitle");
                if (result != null) {
                    title = result.toString();
                }
            }
        }

        return title;
    }

    public static Object invokeMethodNoParams(Object obj, String name) {
        if (obj == null) {
            return null;
        }

        Object result = null;
        Class cls = obj.getClass();
        try {
            Method method = cls.getDeclaredMethod(name);
            result = method.invoke(obj);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }

        return result;
    }
}
