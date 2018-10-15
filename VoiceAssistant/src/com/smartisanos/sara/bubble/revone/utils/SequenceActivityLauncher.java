package com.smartisanos.sara.bubble.revone.utils;

import android.app.ActivityManager;
import android.app.ActivityOptions;
import android.app.IActivityObserver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Rect;
import android.net.Uri;
import android.support.customtabs.CustomTabsIntent;
import android.text.TextUtils;

import com.smartisanos.sara.SaraApplication;
import com.smartisanos.ideapills.common.util.UIHandler;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import smartisanos.util.LogTag;

public class SequenceActivityLauncher {
    private static final String TAG = "SequenceActivityLauncher";
    private LinkedList<LaunchItem> mLaunchList = new LinkedList<LaunchItem>();
    private OnLaunchListener mOnLaunchListener;
    private Runnable mStarter = new Runnable() {
        @Override
        public void run() {
            int startDelay = 30;
            int lastLaunchCount = 0;
            if (!mLaunchList.isEmpty()) {
                LaunchItem launchItem = mLaunchList.getFirst();
                try {
                    lastLaunchCount = launchItem.boundsList.size();
                    if (launchItem.customTabsIntent != null) {
                        LogTag.d(TAG, "launch activity:" + launchItem.customTabsIntent);
                        launchItem.customTabsIntent.launchUrl(launchItem.context, launchItem.uri);
                    } else {
                        if (launchItem.intent.getPackage() != null && launchItem.intent.getPackage().equals(
                                launchItem.context.getPackageName())) {
                            if (mOnLaunchListener != null && !launchItem.boundsList.isEmpty()) {
                                mOnLaunchListener.onLaunchSelfLocal(launchItem.boundsList.get(0));
                            }
                        } else {
                            if (launchItem.activityOptions != null) {
                                launchItem.context.startActivity(launchItem.intent, launchItem.activityOptions.toBundle());
                            } else {
                                launchItem.context.startActivity(launchItem.intent);
                            }
                            LogTag.d(TAG, "launch activity:" + launchItem.intent + ", delay:" + startDelay);
                        }
                    }
                } catch (Exception e) {
                    LogTag.e(TAG, "startActivity failed " + launchItem.intent, e);
                }
            }
            if (!mLaunchList.isEmpty()) {
                mLaunchList.removeFirst();
                UIHandler.removeCallbacks(mStarter);
                if (mLaunchList.isEmpty()) {
                    UIHandler.postDelayed(this, getStartDelayForFinish(lastLaunchCount));
                } else {
                    UIHandler.postDelayed(this, startDelay);
                }
            } else {
                if (mOnLaunchListener != null) {
                    mOnLaunchListener.onLaunchFinish();
                }
            }
        }
    };

    private boolean mIsActivityObserverRegistered;
    private final IActivityObserver.Stub mActivityObserverStub = new IActivityObserver.Stub() {
        @Override
        public void onActivitiesForeground(final String activity, int uid, final int smtAppFlag) {
            UIHandler.post(new Runnable() {
                @Override
                public void run() {
                    LogTag.d(TAG, "onActivitiesForeground:" + activity);
                }
            });
        }
    };

    public SequenceActivityLauncher(OnLaunchListener onLaunchListener) {
        mOnLaunchListener = onLaunchListener;
    }

    public boolean sequenceStart(Context context, Intent intent, List<Rect> boundsList) {
        mLaunchList.add(new LaunchItem(context, intent, boundsList));
        if (!UIHandler.getHandler().hasCallbacks(mStarter)) {
            UIHandler.removeCallbacks(mStarter);
            UIHandler.post(mStarter);
        }
        return true;
    }

    public boolean sequenceStart(Context context, Intent intent, ActivityOptions activityOptions) {
        mLaunchList.add(new LaunchItem(context, intent, activityOptions, activityOptions.getLaunchBounds()));
        if (!UIHandler.getHandler().hasCallbacks(mStarter)) {
            UIHandler.removeCallbacks(mStarter);
            UIHandler.post(mStarter);
        }
        return true;
    }

    public boolean sequenceStart(Context context, CustomTabsIntent customTabsIntent, Uri uri, Rect bounds) {
        mLaunchList.add(new LaunchItem(context, customTabsIntent, uri, bounds));
        if (!UIHandler.getHandler().hasCallbacks(mStarter)) {
            UIHandler.post(mStarter);
        }
        return true;
    }

    private int getStartDelayForFinish(int lastLaunchCount) {
        int startDelay;
        if (lastLaunchCount > 5) {
            startDelay = 6000;
        } else if (lastLaunchCount > 2) {
            startDelay = 5000;
        } else {
            startDelay = 3500;
        }
        return startDelay;
    }

    private int getStartDelayByPkg(Intent intent, int lastLaunchCount) {
        String pkgName = intent.getPackage();
        if (TextUtils.isEmpty(pkgName) && intent.getComponent() != null) {
            pkgName = intent.getComponent().getPackageName();
        }
        if (lastLaunchCount > 1) {
            return 1500;
        } else if ("com.smartisanos.notes".equals(pkgName)) {
            return 400;
        } else if ("com.smartisanos.filemanager".equals(pkgName)) {
            return 450;
        } else if ("com.android.calendar".equals(pkgName)) {
            return 500;
        } else if ("com.android.settings".equals(pkgName)) {
            return 400;
        } else if ("com.smartisanos.music".equals(pkgName)) {
            return 400;
        } else if ("com.android.mms".equals(pkgName)) {
            return 400;
        } else if ("com.android.email".equals(pkgName)) {
            return 550;
        }

        return 500;
    }

    public List<LaunchItem> getLaunchList() {
        return mLaunchList;
    }

    public void registerActivityObserver() {
        if (!mIsActivityObserverRegistered) {
            try {
                ActivityManager am = (ActivityManager) SaraApplication.getInstance().getSystemService(Context.ACTIVITY_SERVICE);
                am.registerActivityObserver(mActivityObserverStub);
                mIsActivityObserverRegistered = true;
            } catch (Exception e) {
                e.printStackTrace();
                mIsActivityObserverRegistered = false;
            }
        }
    }

    public void unRegisterActivityObserver() {
        try {
            if (mIsActivityObserverRegistered) {
                ActivityManager am = (ActivityManager) SaraApplication.getInstance().getSystemService(Context.ACTIVITY_SERVICE);
                am.unregisterActivityObserver(mActivityObserverStub);
                mIsActivityObserverRegistered = false;
            }
        } catch (Exception e) {
            e.printStackTrace();
            mIsActivityObserverRegistered = false;
        }
    }

    public void clear() {
        mLaunchList.clear();
        UIHandler.removeCallbacks(mStarter);
    }

    public static class LaunchItem {
        Context context;
        public List<Rect> boundsList = new ArrayList<>();

        Intent intent;
        ActivityOptions activityOptions;

        CustomTabsIntent customTabsIntent;
        Uri uri;

        public LaunchItem(Context context, Intent intent, List<Rect> boundsList) {
            this.context = context;
            this.intent = intent;
            this.boundsList.clear();
            this.boundsList.addAll(boundsList);
        }

        public LaunchItem(Context context, Intent intent, ActivityOptions activityOptions, Rect bounds) {
            this.context = context;
            this.intent = intent;
            this.activityOptions = activityOptions;
            this.boundsList.clear();
            this.boundsList.add(bounds);
        }

        public LaunchItem(Context context, CustomTabsIntent customTabsIntent, Uri uri, Rect bounds) {
            this.context = context;
            this.customTabsIntent = customTabsIntent;
            this.uri = uri;
            this.boundsList.clear();
            this.boundsList.add(bounds);
        }

        public boolean isWebItem() {
            return customTabsIntent != null || this.boundsList.size() > 1;
        }

        public String getPackageName() {
            if (intent != null) {
                return intent.getPackage();
            }
            return null;
        }
    }

    public interface OnLaunchListener {
        void onLaunchFinish();

        void onLaunchSelfLocal(Rect bounds);
    }
}
