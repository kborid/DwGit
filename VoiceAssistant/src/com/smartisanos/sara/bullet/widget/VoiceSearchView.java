package com.smartisanos.sara.bullet.widget;

import android.content.Context;
import android.os.RemoteException;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.Animation;
import android.widget.RelativeLayout;

import com.smartisanos.ideapills.common.util.UIHandler;
import com.smartisanos.ideapills.common.util.VoiceAssistantServiceConnection;
import com.smartisanos.sara.R;
import com.smartisanos.sara.bullet.contact.model.AbsContactItem;
import com.smartisanos.sara.bullet.contact.model.VoiceSearchResult;
import com.smartisanos.sara.bullet.util.AnimationUtils;
import com.smartisanos.sara.util.LogUtils;
import com.smartisanos.sara.util.SaraUtils;
import com.smartisanos.sara.util.StringUtils;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import smartisanos.app.voiceassistant.ContactStruct;
import smartisanos.app.voiceassistant.IVoiceAssistantCallback;
import smartisanos.app.voiceassistant.ParcelableObject;

public class VoiceSearchView extends RelativeLayout implements VoiceAssistantServiceConnection.VoiceServiceListener {

    private static final String TAG = "VoiceAss.VoiceSearchView";
    private static final int WAVE_COVER_DELAY_MILLIS = 150;
    private volatile boolean mCancelRecognizeByTooShortTime = false;
    private View mCover;
    private VoiceRecognizeView mVoiceRecognizeView;
    private VoiceRecognizeResultView<VoiceSearchResult> mVoiceRecognizeResultView;

    private boolean mIsRecorded = false;
    private boolean mHasResult = false;
    private boolean mCoverDismiss = false;
    private boolean mStartedRecongnize = false;
    private long mStartedRecognizeTime = 0L;

    private VoiceAssistantServiceConnection mVoiceAssistantServiceConnection = null;
    private IVoiceAssistantCallbackImpl mCallback = null;
    private VoiceRecognizeView.VoiceRecognizeListener mVoiceRecognizeListener = new VoiceRecognizeView.VoiceRecognizeListener() {
        @Override
        public void startRecognize() {
            registerServiceCallback();
            result = new StringBuffer();
            UIHandler.removeCallbacks(mStartRecord);
            UIHandler.post(mStartRecord);
            UIHandler.removeCallbacks(mVoiceUiRunnable);
            UIHandler.postDelayed(mVoiceUiRunnable, WAVE_COVER_DELAY_MILLIS);
            UIHandler.removeCallbacks(mTimeoutThread);
            startVoiceRecognize();
        }

        @Override
        public void stopRecognize() {
            stopRecord();
            UIHandler.removeCallbacks(mStartRecord);
            UIHandler.removeCallbacks(mTimeoutThread);
            UIHandler.postDelayed(mTimeoutThread, 5000);
            mVoiceRecognizeView.stopRecognizeWave();
            mVoiceRecognizeView.dismissWave();
            if (!mStartedRecongnize && !isResultDialogShown()) {
                dismissCover();
            }
        }

        @Override
        public void shortRecognize() {
            unRegisterServiceCallback();
            stopRecord();
            dismissCover();
            UIHandler.removeCallbacks(mVoiceUiRunnable);
            UIHandler.removeCallbacks(mStartRecord);
            UIHandler.removeCallbacks(mTimeoutThread);
            shortVoiceRecognize();
        }

        @Override
        public void outOfTouchRange() {
            unRegisterServiceCallback();
            if (!isResultDialogShown()) {
                stopRecord();
                UIHandler.removeCallbacks(mVoiceUiRunnable);
                UIHandler.removeCallbacks(mStartRecord);
                UIHandler.removeCallbacks(mTimeoutThread);
                mVoiceRecognizeView.stopRecognizeWave();
                mVoiceRecognizeView.dismissWave();
                dismissCover();
            }

            UIHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mVoiceRecognizeView.setVoiceEnable(true);
                }
            }, 100);
        }
    };

    public VoiceSearchView(Context context) {
        this(context, null);
    }

    public VoiceSearchView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public VoiceSearchView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        mVoiceAssistantServiceConnection = new VoiceAssistantServiceConnection(mContext);
        mCallback = new IVoiceAssistantCallbackImpl(this);
        mVoiceAssistantServiceConnection.setVoiceListener(this);
        mVoiceAssistantServiceConnection.bindVoiceService();
        initView();
    }

    private void registerServiceCallback() {
        if (null != mVoiceAssistantServiceConnection) {
            mVoiceAssistantServiceConnection.registerCallback(mCallback, getRecognizePackageName());
        }
    }

    private void unRegisterServiceCallback() {
        if (null != mVoiceAssistantServiceConnection) {
            mVoiceAssistantServiceConnection.unregisterCallback(mCallback, getRecognizePackageName());
        }
    }

    private String getRecognizePackageName() {
        return mContext.getPackageName() + ":bullet";
    }

    public void showCoverWithAnim() {
        AnimationUtils.showCoverWithAnimation(mCover, null);
    }

    public void hideCoverWithAnim() {
        AnimationUtils.hideCoverWithAnimation(mCover);
    }

    public void dismissCover() {
        if (mCover != null) {
            AnimationUtils.cancelAnimation(mCover);
            mCover.clearAnimation();
            mCover.setVisibility(View.GONE);
            mCoverDismiss = true;
        }
    }

    private Runnable mStartRecord = new Runnable() {
        @Override
        public void run() {
            mHasResult = false;
            mCoverDismiss = false;
            mStartedRecognizeTime = System.currentTimeMillis();
            if (mVoiceAssistantServiceConnection.isServiceConnected()) {
                mVoiceAssistantServiceConnection.startRecongnize(getRecognizePackageName(), null);
                mStartedRecongnize = true;
            }
        }
    };

    private void initView() {
        LayoutInflater.from(mContext).inflate(R.layout.voice_search_button, this, true);
        mCover = findViewById(R.id.cover);
        mCover.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                hideResultView();
            }
        });
        mVoiceRecognizeView = (VoiceRecognizeView)findViewById(R.id.voice_recognize_view);
        int bottomMargin = 0;
        if (SaraUtils.isNavigationBarMode(mContext)) {
            bottomMargin = getResources().getDimensionPixelSize(R.dimen.navigation_bar_wave_result_margin_bottom);
        } else {
            bottomMargin = getResources().getDimensionPixelSize(R.dimen.wave_result_margin_bottom);
        }
        ((LayoutParams)mVoiceRecognizeView.getLayoutParams()).bottomMargin = bottomMargin;
        mVoiceRecognizeView.setLayoutParams(mVoiceRecognizeView.getLayoutParams());
    }

    private StringBuffer result = new StringBuffer();
    private Runnable mVoiceUiRunnable = new Runnable() {
        @Override
        public void run() {
            AnimationUtils.showCoverWithAnimation(mCover, mCoverAnimationTracker);
            mVoiceRecognizeView.startRecognizeWave();
        }
    };
    private Animation.AnimationListener mCoverAnimationTracker = new Animation.AnimationListener() {
        @Override
        public void onAnimationStart(Animation animation) {
        }

        @Override
        public void onAnimationEnd(Animation animation) {
            if (mCover != null) {
                if (isResultDialogShown()) {
                    mCover.setVisibility(View.VISIBLE);
                } else {
                    if (mStartedRecongnize && !mCancelRecognizeByTooShortTime) {
                        mCover.setVisibility(View.VISIBLE);
                    } else {
                        mCover.setVisibility(View.GONE);
                    }
                }
            }
        }

        @Override
        public void onAnimationRepeat(Animation animation) {
        }
    };

    private void stopRecord() {
        try {
            mIsRecorded = false;
            if (mVoiceAssistantServiceConnection.isServiceConnected()) {
                mVoiceAssistantServiceConnection.stopRecongnize(true);
            }
        } catch (Exception re) {
            re.printStackTrace();
        } finally {
            if (System.currentTimeMillis() - mStartedRecognizeTime <= WAVE_COVER_DELAY_MILLIS) {
                // too short for a recongnize
                mCancelRecognizeByTooShortTime = true;
                mVoiceRecognizeView.dismissWave();
                dismissCover();
                resetRecognizeByTooShortTime();
            } else {
                mCancelRecognizeByTooShortTime = false;
            }
        }
    }

    public void show(boolean isAnim) {
        if (mVoiceRecognizeView != null) {
            mVoiceRecognizeView.registerVoiceRecognizeListener(mVoiceRecognizeListener);
        }
        AnimationUtils.showViewWithAlphaAndScale(mVoiceRecognizeView, isAnim);
        hideResultView();
    }

    public void hide(boolean isAnim) {
        AnimationUtils.hideViewWithAlphaAndScale(mVoiceRecognizeView, isAnim);
        hideResultView();
        if (mVoiceRecognizeView != null) {
            mVoiceRecognizeView.registerVoiceRecognizeListener(null);
        }
    }

    private void resetRecognizeByTooShortTime() {
        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                mCancelRecognizeByTooShortTime = false;
            }
        };
        Timer timer = new Timer();
        timer.schedule(task, 200L);
    }

    private Runnable mTimeoutThread = new Runnable() {
        @Override
        public void run() {
            if (mStartedRecongnize && !mHasResult /*&& !mHasOccupied*/ && !mCoverDismiss) {
                LogUtils.d(TAG, "overtime show empty dialog");
                unRegisterServiceCallback();
                UIHandler.removeCallbacks(mStartRecord);
                if (mVoiceRecognizeResultView != null) {
                    mVoiceRecognizeResultView.showTimeOutView();
                }
                mStartedRecongnize = false;
            }
        }
    };

    public void initVoiceRecognizeResultView(final VoiceRecognizeResultView.Select<AbsContactItem> absContactItemSelect) {
        if (mVoiceRecognizeResultView == null) {
            VoiceRecognizeResultView<VoiceSearchResult> voiceRecognizeResultView = new MultiVoiceRecognizeResultView(null, new VoiceRecognizeResultView.Select<AbsContactItem>() {
                @Override
                public void select(AbsContactItem result, View view) {
                    if (absContactItemSelect != null) {
                        absContactItemSelect.select(result, view);
                    }
                }
            }, false);
            voiceRecognizeResultView.init(this);
            mVoiceRecognizeResultView = voiceRecognizeResultView;
        }
    }

    public void resultVoiceRecognize(VoiceSearchResult resultContact) {
        if (mVoiceRecognizeResultView != null) {
            mVoiceRecognizeResultView.resultVoiceRecognize(resultContact);
        }
    }

    public void startVoiceRecognize() {
        if (mVoiceRecognizeResultView != null) {
            mVoiceRecognizeResultView.startVoiceRecognize();
        }
    }

    public void shortVoiceRecognize() {
        if (mVoiceRecognizeResultView != null) {
            mVoiceRecognizeResultView.shortVoiceRecognize();
        }
    }

    public void setHasResult(boolean hasResult) {
        mHasResult = hasResult;
    }

    public void hideResultView() {
        mCoverDismiss = true;
        UIHandler.removeCallbacks(mTimeoutThread);
        hideCoverWithAnim();
        if (mVoiceRecognizeResultView != null) {
            mVoiceRecognizeResultView.hideResultDialog();
        }
    }

    private boolean isResultDialogShown() {
        return mVoiceRecognizeResultView != null && mVoiceRecognizeResultView.isResultViewShown();
    }

    @Override
    public void onServiceConnected() {
    }

    private static class IVoiceAssistantCallbackImpl extends IVoiceAssistantCallback.Stub {
        private WeakReference<VoiceSearchView> view = null;

        public IVoiceAssistantCallbackImpl(VoiceSearchView view) {
            this.view = new WeakReference<>(view);
        }

        @Override
        public void onLocalResult(ParcelableObject result, boolean isRefresh) throws RemoteException {
            LogUtils.d(TAG, "onLocalResult() result = " + result.getResultStr() + ", " + isRefresh);
            final VoiceSearchView voiceSearchView = view.get();
            if (null != voiceSearchView) {
                final VoiceSearchResult voiceSearchResult = new VoiceSearchResult();
                //1. list result
                LogUtils.d(TAG, "onLocalResult(): result.getContacts().size() = " + result.getContacts().size());
                voiceSearchResult.setContactStruct(result.getContacts());

                //2. string  result
                if (!TextUtils.isEmpty(result.getResultStr()) && result.getResultStr().length() > 0) {
                    String resultStr = result.getResultStr();
                    LogUtils.d(TAG, "onLocalResult() result = " + resultStr);
                    final StringBuffer sb = new StringBuffer();
                    sb.append(resultStr);
                    char lastChar = resultStr.charAt(resultStr.length() - 1);
                    if (StringUtils.isChinesePunctuation(lastChar) || String.valueOf(lastChar).equals(".")) {
                        sb.deleteCharAt(sb.length() - 1);
                    }
                    voiceSearchResult.setResultString(sb.toString());
                }

                UIHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        UIHandler.removeCallbacks(voiceSearchView.mTimeoutThread);
                        voiceSearchView.resultVoiceRecognize(voiceSearchResult);
                        voiceSearchView.unRegisterServiceCallback();
                    }
                });
            }
        }

        @Override
        public void onError(int errorCode, int type, boolean isToast) throws RemoteException {
            LogUtils.d(TAG, "onError() errorCode = " + errorCode + ", " + type + ", " + isToast);
            final VoiceSearchView voiceSearchView = view.get();
            if (null == voiceSearchView || voiceSearchView.mIsRecorded) {
                return;
            }
            UIHandler.post(new Runnable() {
                @Override
                public void run() {
                    UIHandler.removeCallbacks(voiceSearchView.mTimeoutThread);
                    voiceSearchView.unRegisterServiceCallback();
                }
            });
        }

        @Override
        public void onVolumeUpdate(int volume) throws RemoteException {
        }

        @Override
        public void onResultRecived(String result, int type, boolean offline) throws RemoteException {
            LogUtils.d(TAG, "onResultRecived() result = " + result + ", " + type + ", " + offline);
        }

        @Override
        public void onBuffer(byte[] buffer) throws RemoteException {
        }

        @Override
        public void onPartialResult(String partial) throws RemoteException {
            LogUtils.d(TAG, "onPartialResult() partial = " + partial);
            VoiceSearchView voiceSearchView = view.get();
            if (null != voiceSearchView) {
                voiceSearchView.result.append(partial);
            }
        }

        @Override
        public void onRecordStart() throws RemoteException {
            LogUtils.d(TAG, "onRecordStart()");
            VoiceSearchView voiceSearchView = view.get();
            if (null != voiceSearchView) {
                voiceSearchView.mIsRecorded = true;
            }
        }

        @Override
        public void onRecordEnd() throws RemoteException {
            VoiceSearchView voiceSearchView = view.get();
            if (null != voiceSearchView) {
                voiceSearchView.mIsRecorded = false;
                LogUtils.d(TAG, "onRecordEnd()");
            }
        }
    }

    public void abortVoiceSearch(boolean isSmartKeyClick) {
        hideResultView();
        UIHandler.removeCallbacks(mVoiceUiRunnable);
        UIHandler.removeCallbacks(mStartRecord);
        UIHandler.removeCallbacks(mTimeoutThread);
        if (null != mVoiceAssistantServiceConnection) {
            unRegisterServiceCallback();
        }
        if (null != mVoiceRecognizeView) {
            mVoiceRecognizeView.stopRecognizeWave();
            mVoiceRecognizeView.dismissWave();
            mVoiceRecognizeView.cancelTouch();
        }
        if (isSmartKeyClick) {
            if (mVoiceRecognizeView != null) {
                mVoiceRecognizeView.registerVoiceRecognizeListener(null);
            }
        }
    }

    public void onPause() {
        abortVoiceSearch(false);
        dismissCover();
    }

    public void onDestroy() {
        LogUtils.d(TAG, "onDestroy()");
        if (null != mVoiceAssistantServiceConnection) {
            unRegisterServiceCallback();
            mVoiceAssistantServiceConnection.unBindVoiceService();
            mCallback = null;
            mVoiceAssistantServiceConnection = null;
        }
    }


    public void startTouchAction(boolean isStart) {
        LogUtils.infoRelease(TAG, "startTouchAction = " + isStart);
        if (isStart) {
            dispatchTouchEvent(MotionEvent.obtain(SystemClock.uptimeMillis(), SystemClock.uptimeMillis(), MotionEvent.ACTION_DOWN,
                    mVoiceRecognizeView.getLeft() + mVoiceRecognizeView.getWidth() / 2,
                    mVoiceRecognizeView.getTop() + mVoiceRecognizeView.getHeight() / 2,
                    0));
        } else {
            dispatchTouchEvent(MotionEvent.obtain(SystemClock.uptimeMillis(), SystemClock.uptimeMillis(),
                    MotionEvent.ACTION_UP,
                    mVoiceRecognizeView.getLeft() + mVoiceRecognizeView.getWidth() / 2,
                    mVoiceRecognizeView.getTop() + mVoiceRecognizeView.getHeight() / 2,
                    0));
        }
    }
}
