package com.smartisanos.sara.bubble.revone;

import android.hardware.display.DisplayManager;

import com.smartisanos.ideapills.common.util.UIHandler;

import java.lang.ref.WeakReference;

public enum RevActivityManager implements DisplayManager.DisplayListener {
    INSTANCE;

    private int mDisplayId = -1;
    public WeakReference<GlobalSearchActivity> mGlobalSearchActivityRef;
    public WeakReference<GlobalBubbleCreateActivity> mGlobalBubbleCreateActivityRef;
    public WeakReference<FlashImActivity> mFlashImActivityActivityRef;

    RevActivityManager() {
    }

    void attach(GlobalSearchActivity globalSearchActivity) {
        mGlobalSearchActivityRef = new WeakReference<GlobalSearchActivity>(globalSearchActivity);
        UIHandler.post(new Runnable() {
            @Override
            public void run() {
                finishGlobalBubbleCreateActivity();
                finishFlashImActivity();
            }
        });
    }

    void attach(GlobalBubbleCreateActivity globalBubbleCreateActivity) {
        mGlobalBubbleCreateActivityRef = new WeakReference<GlobalBubbleCreateActivity>(globalBubbleCreateActivity);
        UIHandler.post(new Runnable() {
            @Override
            public void run() {
                finishGlobalSearchActivity();
                finishFlashImActivity();
            }
        });
    }

    void attach(FlashImActivity flashImActivity) {
        mFlashImActivityActivityRef = new WeakReference<FlashImActivity>(flashImActivity);
        UIHandler.post(new Runnable() {
            @Override
            public void run() {
                finishGlobalSearchActivity();
                finishGlobalBubbleCreateActivity();
            }
        });
    }

    void finishGlobalSearchActivity() {
        if (mGlobalSearchActivityRef != null) {
            GlobalSearchActivity globalSearchActivity = mGlobalSearchActivityRef.get();
            if (globalSearchActivity != null && !globalSearchActivity.isFinishing()
                    && !globalSearchActivity.isDestroyed()) {
                globalSearchActivity.finishAll();
            }
            mGlobalSearchActivityRef = null;
        }
    }

    void finishGlobalBubbleCreateActivity() {
        if (mGlobalBubbleCreateActivityRef != null) {
            GlobalBubbleCreateActivity globalBubbleCreateActivity = mGlobalBubbleCreateActivityRef.get();
            if (globalBubbleCreateActivity != null && !globalBubbleCreateActivity.isPendingFinishing()
                    && !globalBubbleCreateActivity.isDestroyed()) {
                globalBubbleCreateActivity.finishAll();
            }
            mGlobalBubbleCreateActivityRef = null;
        }
    }

    void finishFlashImActivity() {
        if (mFlashImActivityActivityRef != null) {
            FlashImActivity flashImActivity = mFlashImActivityActivityRef.get();
            if (flashImActivity != null && !flashImActivity.isPendingFinishing()
                    && !flashImActivity.isDestroyed()) {
                flashImActivity.finishAll();
            }
            mFlashImActivityActivityRef = null;
        }
    }

    public void start() {
//        DisplayManager dm = (DisplayManager) SaraApplication.getInstance().getSystemService(Context.DISPLAY_SERVICE);
//        if (dm != null) {
//            dm.registerDisplayListener(this, null);
//        }
    }

    public void release() {
//        DisplayManager dm = (DisplayManager) SaraApplication.getInstance().getSystemService(Context.DISPLAY_SERVICE);
//        if (dm != null) {
//            dm.unregisterDisplayListener(this);
//        }
    }

    @Override
    public void onDisplayAdded(int displayId) {
        mDisplayId = displayId;
    }

    @Override
    public void onDisplayRemoved(int displayId) {
        mDisplayId = -1;
        finishGlobalSearchActivity();
        finishGlobalBubbleCreateActivity();
        finishFlashImActivity();
    }

    @Override
    public void onDisplayChanged(int displayId) {
    }
}
