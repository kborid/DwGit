package com.smartisanos.sara;

import android.app.SmtPCUtils;
import android.app.ActivityThread;
import android.text.TextUtils;

import com.android.internal.policy.PhoneWindow;
import com.smartisanos.ideapills.common.event.android.BaseEventActivity;
import com.smartisanos.sanbox.utils.SaraTracker;
import com.smartisanos.sara.util.LogUtils;
import com.smartisanos.sara.util.SaraUtils;

import java.lang.reflect.Method;

import smartisanos.widget.Title;

public class BaseActivity extends BaseEventActivity {

    private boolean mIsPaused;
    private boolean mIsStopped;
    private boolean mIsAttachedToWindow;

    @Override
    protected void onStart() {
        super.onStart();
        mIsStopped = false;
    }

    @Override
    protected void onResume() {
        super.onResume();
        mIsPaused = false;
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        mIsAttachedToWindow = true;
    }

    @Override
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        mIsAttachedToWindow = false;
    }

    @Override
    protected void onPause() {
        super.onPause();
        mIsPaused = true;
    }

    @Override
    protected void onStop() {
        super.onStop();
        mIsStopped = true;
        SaraTracker.flush();
    }

    protected boolean isPaused() {
        return mIsPaused;
    }

    protected boolean isStopped() {
        return mIsStopped;
    }

    protected boolean isAttachedToWindow() {
        return mIsAttachedToWindow;
    }

    @Override
    public void finish() {
        super.finish();
        SaraUtils.overridePendingTransition(this);
    }

    public void setTitleByIntent(Title title) {
        if (getIntent().hasExtra(Title.EXTRA_TITLE_TEXT)) {
            String titleStr = getIntent().getStringExtra(Title.EXTRA_TITLE_TEXT);
            if (!TextUtils.isEmpty(titleStr)) {
                title.setTitle(titleStr);
            }
        } else if (getIntent().hasExtra(Title.EXTRA_TITLE_TEXT_ID)) {
            int titleId = getIntent().getIntExtra(Title.EXTRA_TITLE_TEXT_ID, -1);
            if (titleId > 0) {
                try {
                    title.setTitle(getString(titleId));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void setCaptionTitleToPillInExtDisplay() {
        if (SmtPCUtils.isValidExtDisplayId(this)) {
            try {
                Method method = PhoneWindow.class.getMethod("setCaptionTitle", String.class);
                method.invoke(getWindow(), getResources().getString(R.string.app_pill_name));
            } catch (Exception e) {
                LogUtils.error(e);
            }
        }
    }

    /**
     * 判断谁不是来自onNewIntent()的调用
     *
     * @return
     */
    protected boolean isLaunchFromOnNewIntent() {
        boolean result = false;
        try {
            StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
            for (StackTraceElement stackTraceElement : stackTrace) {
                String methodName = stackTraceElement.getMethodName();
                String className = stackTraceElement.getClassName();

                boolean assignableFromClass = Class.forName(className).isAssignableFrom(ActivityThread.class);
                if (assignableFromClass && "performNewIntents".equals(methodName)) {
                    result = true;
                    break;
                }
            }
        } catch (Exception e) {
            // ignored
            result = false;
            LogUtils.d(e);
        }
        LogUtils.d("result = " + result);
        return result;
    }

    protected boolean isLaunchFromSendResult() {
        boolean result = false;
        try {
            StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
            for (StackTraceElement stackTraceElement : stackTrace) {
                String methodName = stackTraceElement.getMethodName();
                String className = stackTraceElement.getClassName();

                boolean assignableFromClass = Class.forName(className).isAssignableFrom(ActivityThread.class);
                if (assignableFromClass && "handleSendResult".equals(methodName)) {
                    result = true;
                    break;
                }
            }
        } catch (Exception e) {
            // ignored
            result = false;
            LogUtils.d(e);
        }
        LogUtils.d("result = " + result);
        return result;
    }
}
