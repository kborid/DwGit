package com.smartisanos.sara.bubble;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewStub;

import com.smartisanos.sara.SaraApplication;
import com.smartisanos.sara.util.LogUtils;

import java.lang.ref.WeakReference;
import java.lang.reflect.Method;

import smartisanos.util.DeviceType;
import com.smartisanos.sara.R;

public enum BubbleActivityHelper {
    INSTANCE;

    private WeakReference<BubbleActivity> mRelateBubbleActivity;
    private RootViewTag mRootViewTag = new RootViewTag();

    void attach(BubbleActivity bubbleActivity) {
        mRelateBubbleActivity = new WeakReference<BubbleActivity>(bubbleActivity);
    }

    public void callBubbleActivityFromBluetooth(Context context) {
        BubbleActivity bubbleActivity = null;
        if (mRelateBubbleActivity != null) {
            bubbleActivity = mRelateBubbleActivity.get();
        }
        if (bubbleActivity != null && !bubbleActivity.isPendingFinishing()
                && bubbleActivity.isRecognizing()) {
            bubbleActivity.endKeyOrBlueRec();
        } else {
            launchBubbleActivity(context, BubbleActivity.TYPE_BLUETOOTH);
        }
    }

    public void launchBubbleActivity(Context context, int voiceType) {
        Intent intent = new Intent("android.intent.action.launchIatSpeech");
        intent.putExtra("VoiceType", voiceType);
        PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        intent.putExtra("ScreenOff", !pm.isScreenOn());
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);

        try {
            context.startActivity(intent);
        } catch (ActivityNotFoundException e) {
            LogUtils.w("Voice Assistan is not found");
        }
    }

    public RootViewTag getRootViewTag() {
        return mRootViewTag;
    }

    public static class RootViewTag {
        private View mView;
        private final Object mInflateLock = new Object();

        static boolean isEnableRootViewCache() {
            // use async inflate root view with app context, should ensure:
            // 1 no new Handler() in View init logic
            // 2 no use view's context(app context),startActForResult and startAct(intent, bundle) when after n
            return (Build.VERSION.SDK_INT >= 23) && !DeviceType.isOneOf(DeviceType.M1L, DeviceType.M1);
        }

        public View attach(Context context) {
            if (!isEnableRootViewCache()) {
                return LayoutInflater.from(context).inflate(R.layout.sara_bubble_main, null);
            }
            synchronized (mInflateLock) {
                if (isAttached()) {
                    inflateSafely();
                } else if (mView == null) {
                    inflate();
                }
            }
            return mView;
        }

        public void detach() {
            if (!isEnableRootViewCache()) {
                return;
            }
            synchronized (mInflateLock) {
                deflate();
            }
        }

        public void initAsync() {
            if (!isEnableRootViewCache()) {
                return;
            }
            if (mView == null) {
                Thread initThread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        if (mView == null) {
                            synchronized (mInflateLock) {
                                if (mView == null) {
                                    inflate();
                                }
                            }
                        }
                    }
                });
                initThread.setPriority(Thread.NORM_PRIORITY + 1);
                initThread.start();
            }
        }

        private boolean inflate() {
            if (mView != null) {
                // throw new
                // IllegalArgumentException("You should deflate first.");
                return false;
            }
            try {
                internalInflateRootView(SaraApplication.getInstance());
            } catch (Exception e) {
                // may can not inflate in none ui thread.Cause create Handler when create or others
                LogUtils.w(e.getMessage());
            }
            return mView != null;
        }

        private boolean deflate() {
            if (mView == null) {
                return true;
            }
            mView = null;
            clearPendingActions();
            return true;
        }

        private void inflateSafely() {
            deflate();
            inflate();
        }

        private boolean isAttached() {
            return mView != null && mView.getParent() != null;
        }

        private void internalInflateRootView(Context context) {
            clearPendingActions();
            mView = LayoutInflater.from(context).inflate(
                    R.layout.sara_bubble_main, null);

            ViewStub leftBubbleStub = (ViewStub) mView.findViewById(R.id.bubble_item_left_stub);
            ViewStub waveLeftBubbleStub = (ViewStub) mView.findViewById(R.id.wave_left_stub);
            if (leftBubbleStub != null) {
                leftBubbleStub.inflate();
            }
            if (waveLeftBubbleStub != null) {
                waveLeftBubbleStub.inflate();
            }
        }

        private void clearPendingActions() {
            try {
                if (null == Looper.myLooper()) {
                    return;
                }
                Class<?> viewRootImpl = Class.forName("android.view.ViewRootImpl");
                Method method = viewRootImpl.getDeclaredMethod("getRunQueue");
                method.setAccessible(true);
                Object runQueue = method.invoke(viewRootImpl);
                method = runQueue.getClass().getDeclaredMethod(
                        "executeActions", Handler.class);
                method.setAccessible(true);
                method.invoke(runQueue, new Handler());
            } catch (Exception e) {
                LogUtils.e("clearPendingActions: " + e);
            }
        }
    }
}
