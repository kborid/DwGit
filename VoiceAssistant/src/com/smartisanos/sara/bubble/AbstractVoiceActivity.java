package com.smartisanos.sara.bubble;

import android.content.Intent;
import android.graphics.Point;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.RemoteException;
import android.text.TextUtils;

import com.smartisanos.ideapills.common.util.VoiceAssistantServiceConnection;
import com.smartisanos.sara.BaseActivity;
import com.smartisanos.sara.util.LogUtils;
import com.smartisanos.sara.util.SaraConstant;
import com.smartisanos.sara.util.SaraUtils;
import com.smartisanos.ideapills.common.util.UIHandler;
import com.smartisanos.sara.entity.SpeechError;
import com.smartisanos.sara.util.StringUtils;
import com.smartisanos.sara.util.WaveformGenerator;

import java.lang.ref.WeakReference;

import smartisanos.app.voiceassistant.IVoiceAssistantCallback;
import smartisanos.app.voiceassistant.ParcelableObject;
import smartisanos.app.voiceassistant.RecognizeArgsHelper;
import smartisanos.app.voiceassistant.RecognizeResult;
import smartisanos.app.voiceassistant.VoiceAssistantCallbackV2Adapter;

/**
 * 语音气泡交互Activity骨架
 */

public abstract class AbstractVoiceActivity extends BaseActivity implements VoiceAssistantServiceConnection.VoiceServiceListener {

    private static final String VOICE_TAG = "VoiceAss.AbstractVoiceActivity";

    private static final long SHORT_PRESS_TIMEOUT = 500;

    private static final int MSG_START_REGOGNIZE = 1;
    private static final int MSG_END_RECOGNIZE = 2;
    private static final int MSG_WAITING_RECORD_RESULT_TIME_OUT = 13;

    protected static final int RECORD_STATUS_IDLE = 0;
    protected static final int RECORD_STATUS_STARTING = 1;
    protected static final int RECORD_STATUS_STARTED = 2;
    protected static final int RECORD_STATUS_START_CANCELED = 3;
    protected static final int RECORD_STATUS_STOPPING = 4;
    protected static final int RECORD_STATUS_STOPPED = 5;
    protected static final int RECORD_STATUS_RESULT_NONE = 6;
    protected static final int RECORD_STATUS_RESULT_OK = 7;
    protected static final int RECORD_STATUS_RESULT_NONE_STOPPED = 8;
    protected static final int RECORD_STATUS_RESULT_OK_STOPPED = 9;

    private static final int RECORD_CMD_SOURCE_UNKNOWN = -1;
    private static final int RECORD_CMD_SOURCE_SELF = 0;

    // stopped after 10s, force stop this interaction
    private static final int WAITING_RECORD_RESULT_TIME_OUT_MILLIS = 10000;

    private VoiceAssistantServiceConnection mServiceConnection;
    private IVoiceAssistantCallbackImpl mCallback;
    private boolean mReRecongnize;

    private byte[] mSharedBuf = new byte[4096];
    private int mLastPointNum = 0;
    private long mRecordStartTime;
    private WaveformGenerator mWaveformGenerator;

    protected volatile int mRecordStatus = RECORD_STATUS_IDLE;

    protected VoiceHandler mVoiceHandler = new VoiceHandler(this);
    // 当前语音识别动作的发起者
    private int mStartRecordCmdSource = RECORD_CMD_SOURCE_SELF;

    protected static class VoiceHandler extends Handler {
        private final WeakReference<AbstractVoiceActivity> mActivity;

        public VoiceHandler(AbstractVoiceActivity activity) {
            mActivity = new WeakReference<AbstractVoiceActivity>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            AbstractVoiceActivity activity = mActivity.get();
            if (activity != null) {
                LogUtils.d(VOICE_TAG, "handleMessage id " + msg.what);
                switch (msg.what) {
                    case MSG_START_REGOGNIZE:
                        activity.realStartRecognize();
                        break;
                    case MSG_END_RECOGNIZE:
                        activity.stopRecognizeInternal();
                        break;
                    case MSG_WAITING_RECORD_RESULT_TIME_OUT:
                        activity.resultTimeOut();
                        break;
                    default:
                        break;
                }
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mServiceConnection = new VoiceAssistantServiceConnection(this);
//        mCallback = new IVoiceAssistantCallbackImpl(this);
        mServiceConnection.bindVoiceService();
        mServiceConnection.setVoiceListener(this);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mVoiceHandler.removeMessages(MSG_WAITING_RECORD_RESULT_TIME_OUT);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            mServiceConnection.unregisterCallbackV2(mCallbackAdapter, getPackageName());
            mServiceConnection.unBindVoiceService();
//            mCallback = null;
        } catch (Exception e) {
            // ignore
        }
    }

    @Override
    public void onServiceConnected() {
        if (mReRecongnize && !isFinishing()) {
            realStartRecognize();
        }
    }

    protected final void startRecognize() {
        mVoiceHandler.removeMessages(MSG_START_REGOGNIZE);
        mVoiceHandler.removeMessages(MSG_END_RECOGNIZE);
        mVoiceHandler.sendEmptyMessage(MSG_START_REGOGNIZE);
    }

    protected void realStartRecognize() {
        LogUtils.infoRelease(VOICE_TAG, "startRecognize " + mServiceConnection.isServiceConnected());
        if (!mServiceConnection.isServiceConnected()) {
            mReRecongnize = true;
            return;
        }
        mReRecongnize = false;
        SaraUtils.stopAudioPlay(this);
//        VibratorSmt.getInstance().vibrateWithPrivilege(mVibrator, 50);
        mLastPointNum = 0;
        mVoiceHandler.removeMessages(MSG_WAITING_RECORD_RESULT_TIME_OUT);
        mRecordStatus = RECORD_STATUS_STARTING;

        RecognizeArgsHelper helper = new RecognizeArgsHelper(RecognizeArgsHelper.BASE_TYPE_BUBBLE)
                .setLocalSearchType(RecognizeArgsHelper.TYPE_ALL);
        if (this instanceof BubbleActivity) {
            helper.setEnableDynamicFix(true);
        }
        mServiceConnection.startRecognizeV2(getPackageName(), helper.getBundle());
    }

    protected void stopRecognize(boolean force) {
        LogUtils.infoRelease(VOICE_TAG, "stopRecognize:" + force + ", recordStatus:" + getRecordStatus());
        mReRecongnize = false;
        if (force) {
            mVoiceHandler.removeMessages(MSG_START_REGOGNIZE);
            mVoiceHandler.removeMessages(MSG_END_RECOGNIZE);
            mVoiceHandler.sendEmptyMessage(MSG_END_RECOGNIZE);
        } else {
            if (mRecordStatus == RECORD_STATUS_STARTED
                    || mRecordStatus == RECORD_STATUS_RESULT_NONE
                    || mRecordStatus == RECORD_STATUS_RESULT_OK) {
                mVoiceHandler.removeMessages(MSG_START_REGOGNIZE);
                mVoiceHandler.removeMessages(MSG_END_RECOGNIZE);
                mVoiceHandler.sendEmptyMessage(MSG_END_RECOGNIZE);
            } else if (mRecordStatus == RECORD_STATUS_STARTING) {
                mRecordStatus = RECORD_STATUS_START_CANCELED;
            } else {
                mVoiceHandler.removeMessages(MSG_START_REGOGNIZE);
            }
        }
    }

    private void stopRecognizeInternal() {
        mRecordStatus = RECORD_STATUS_STOPPING;
        mServiceConnection.stopRecognizeV2(getPackageName(), null);
    }

    protected void registerVoiceCallback() {
        mServiceConnection.registerCallbackV2(mCallbackAdapter, getPackageName());
    }

    protected void unregisterVoiceCallback() {
        mServiceConnection.unregisterCallbackV2(mCallbackAdapter, getPackageName());
    }

    protected void loadLocalData(String keywords, String packageName) {
        mServiceConnection.loadLocalData(keywords, packageName);
    }

    protected final void okResultHandled() {
        mVoiceHandler.removeMessages(MSG_WAITING_RECORD_RESULT_TIME_OUT);
        if (mRecordStatus == RECORD_STATUS_STOPPING
                || mRecordStatus == RECORD_STATUS_STOPPED) {
            mRecordStatus = RECORD_STATUS_RESULT_OK_STOPPED;
        } else {
            mRecordStatus = RECORD_STATUS_RESULT_OK;
        }
    }

    protected final void noResultHandled() {
        mVoiceHandler.removeMessages(MSG_WAITING_RECORD_RESULT_TIME_OUT);
        if (mRecordStatus == RECORD_STATUS_STOPPED
                || mRecordStatus == RECORD_STATUS_STOPPING) {
            mRecordStatus = RECORD_STATUS_RESULT_NONE_STOPPED;
        } else {
            mRecordStatus = RECORD_STATUS_RESULT_NONE;
        }
    }

    protected int getRecordStatus() {
        return mRecordStatus;
    }

    protected final boolean isRecognizing() {
        return mRecordStatus == RECORD_STATUS_STARTING || mRecordStatus == RECORD_STATUS_STARTED
                || mRecordStatus == RECORD_STATUS_RESULT_NONE || mRecordStatus == RECORD_STATUS_RESULT_OK;
    }

    protected final boolean isWaitingRecordResult() {
        return mRecordStatus == RECORD_STATUS_STARTING || mRecordStatus == RECORD_STATUS_STARTED
                || mRecordStatus == RECORD_STATUS_START_CANCELED
                || mRecordStatus == RECORD_STATUS_STOPPING || mRecordStatus == RECORD_STATUS_STOPPED;
    }

    private void localResultInternal(final ParcelableObject result) {
        // ignore the callback return from last search, avoid UI error
        if (mRecordStatus == RECORD_STATUS_RESULT_NONE
                || mRecordStatus == RECORD_STATUS_RESULT_NONE_STOPPED
                || mRecordStatus == RECORD_STATUS_IDLE) {
            LogUtils.d(VOICE_TAG, "localResult return of true ");
            return;
        }
        if (mRecordStatus == RECORD_STATUS_RESULT_OK
                || mRecordStatus == RECORD_STATUS_RESULT_OK_STOPPED) {
            mServiceConnection.unregisterCallbackV2(mCallbackAdapter, getPackageName());
        }
        localResult(result);
    }

    protected abstract void localResult(final ParcelableObject result);

    private void errorInternal(final int errorCode) {
        if (errorCode == SpeechError.ERROR_INTERRUPT && mRecordStatus == RECORD_STATUS_STARTING) {
            // 忽略这种打断的错误,因为是我们主动发起新的识别，导致上次打断的error
            return;
        }
        if (errorCode == SpeechError.ERROR_MSP_NO_DATA
            && System.currentTimeMillis() - mRecordStartTime < 200
            && mStartRecordCmdSource != RECORD_CMD_SOURCE_SELF) {
            // 满足以上条件时,忽略这种 ERROR_MSP_NO_DATA 错误.
            // 它通常是由在语音页面短按 smart 键时 framework 发起的一次短暂语音识别动作导致,并非 sara 请求进行语音识别,可直接忽略
            LogUtils.e(VOICE_TAG, "error " + errorCode + " come too soon and current cmd source is not our self, ignore!!!");
            return;
        } else {
            if (errorCode == SpeechError.ERROR_MSP_NO_DATA) {
                LogUtils.d(VOICE_TAG, "mStartRecordCmdSource = " + mStartRecordCmdSource + ", timeDuring = " + (System.currentTimeMillis() - mRecordStartTime));
            }
        }
        switch (mRecordStatus) {
            case RECORD_STATUS_RESULT_NONE_STOPPED:
            case RECORD_STATUS_RESULT_OK_STOPPED:
            case RECORD_STATUS_STOPPED:
                mServiceConnection.unregisterCallbackV2(mCallbackAdapter, getPackageName());
                break;
            default:
                LogUtils.e(VOICE_TAG, "skip unregister callback");
        }
        error(errorCode);
    }

    protected abstract void error(final int errorCode);

    protected abstract void resultRecived(final String resultStr, final boolean offline);

    protected abstract void resultTimeOut();

    private void bufferInternal(final byte[] buffer) {
        if (mRecordStatus == RECORD_STATUS_STARTING) {
            LogUtils.infoRelease("recordStarted by buffer come");
            recordStartedInternal();
        }
        if (mWaveformGenerator == null || mRecordStatus == RECORD_STATUS_IDLE) {
            return;
        }
        if (mRecordStatus >= RECORD_STATUS_START_CANCELED) {
            if (mRecordStatus == RECORD_STATUS_START_CANCELED) {
                stopRecognize(true);
            }
            return;
        }
        mWaveformGenerator.generate(buffer, buffer.length);
        int cap = mSharedBuf.length;
        int total_point = mWaveformGenerator.getTotalPoint();
        int copy_point = total_point - mLastPointNum;
        if (copy_point > cap) {
            copy_point = cap;
            mLastPointNum = total_point - cap;
        }
        if (copy_point <= 0) {
            return;
        }
        final byte[] bytes = new byte[copy_point];
        int i = 0;

        while (i < copy_point) {
            bytes[i] = mSharedBuf[(mLastPointNum + i) % cap];
            i++;
        }
        mLastPointNum = total_point;
        buffer(bytes, total_point);
    }

    protected abstract void buffer(final byte[] buffer, int totalPoint);

    protected abstract void parcailResult(final String partialResult);

    private void recordStartedInternal() {
        // 根据当前 mRecordStatus 更新语音指令来源(若来源是 sara 自己的话,按正常的流程走到这里之前 mRecordStatus 只会是
        // RECORD_STATUS_IDLE 或 RECORD_STATUS_STARTING 中的一种,如果不是的话,说明这次识别动作并非 sara 发起的)
        mStartRecordCmdSource = mRecordStatus == RECORD_STATUS_IDLE || mRecordStatus == RECORD_STATUS_STARTING
                                ? RECORD_CMD_SOURCE_SELF
                                : RECORD_CMD_SOURCE_UNKNOWN;

        if (mRecordStatus == RECORD_STATUS_START_CANCELED) {
            stopRecognize(true);
            return;
        }
        if (mRecordStatus == RECORD_STATUS_STARTED) {
            return;
        }
        mRecordStatus = RECORD_STATUS_STARTED;
        mWaveformGenerator = new WaveformGenerator(20, 16000, mSharedBuf);
        mWaveformGenerator.open(SaraUtils.getWaveTmpPath(AbstractVoiceActivity.this) + SaraConstant.WAVE_FILE);
        mRecordStartTime = System.currentTimeMillis();
        recordStarted();
    }

    protected abstract void recordStarted();

    private void recordEndedInternal() {
        mVoiceHandler.removeMessages(MSG_WAITING_RECORD_RESULT_TIME_OUT);
        if (mRecordStatus == RECORD_STATUS_RESULT_NONE
                || mRecordStatus == RECORD_STATUS_RESULT_NONE_STOPPED) {
            mRecordStatus = RECORD_STATUS_RESULT_NONE_STOPPED;
        } else if (mRecordStatus == RECORD_STATUS_RESULT_OK
                || mRecordStatus == RECORD_STATUS_RESULT_OK_STOPPED) {
            mRecordStatus = RECORD_STATUS_RESULT_OK_STOPPED;
        } else {
            mRecordStatus = RECORD_STATUS_STOPPED;
            mVoiceHandler.sendEmptyMessageDelayed(MSG_WAITING_RECORD_RESULT_TIME_OUT, WAITING_RECORD_RESULT_TIME_OUT_MILLIS);
        }
        long currentTime = System.currentTimeMillis();
        boolean isMaxRecord = false;
        boolean isShortPress = false;
        if (currentTime - mRecordStartTime < SHORT_PRESS_TIMEOUT) {
            isShortPress = true;
        } else if (currentTime - mRecordStartTime >= SaraConstant.MAXIMUM_RECORDING_TIME - 500) {
            isMaxRecord = true;
        }
        recordEnded(isShortPress, isMaxRecord);
        if (mWaveformGenerator != null) {
            mWaveformGenerator.close();
            mWaveformGenerator = null;
        }
    }

    protected int getScreenWidth() {
        Point point = new Point();
        getDisplay().getSize(point);
        return point.x;
    }

    protected void optimizeResultPunctutation(StringBuffer result) {
        optimizeResultPunctutation(result, 10);
    }

    protected void optimizeResultPunctutation(StringBuffer result, int size) {
        if (result != null && result.length() <= size && result.length() >= 1 && StringUtils.isPunctuation(result.charAt(result.length() - 1))) {
            result.deleteCharAt(result.length() - 1);
        }
    }

    protected abstract void recordEnded(boolean isShortRecord, boolean isMaxRecord);

    private VoiceAssistantCallbackV2Adapter mCallbackAdapter = new VoiceAssistantCallbackV2Adapter() {
        @Override
        public void onRecordStart() {
            super.onRecordStart();
            LogUtils.infoRelease(VOICE_TAG, "onRecordStart ");
            UIHandler.post(new Runnable() {
                @Override
                public void run() {
                    recordStartedInternal();
                }
            });
        }

        @Override
        public void onBuffer(final byte[] buffer) {
            LogUtils.infoRelease(VOICE_TAG, "onBuffer  mRecordStatus = " + mRecordStatus);
            UIHandler.post(new Runnable() {
                @Override
                public void run() {
                    bufferInternal(buffer);
                }
            });
        }

        @Override
        public void onRecordEnd() {
            LogUtils.infoRelease(VOICE_TAG, "onRecordEnd ");
            UIHandler.post(new Runnable() {
                @Override
                public void run() {
                    recordEndedInternal();
                }
            });
        }

        @Override
        public void onError(final int errorCode, Bundle extra) {
            LogUtils.infoRelease(VOICE_TAG, "onError " + errorCode + " mRecordStatus = " + mRecordStatus);
            UIHandler.post(new Runnable() {
                @Override
                public void run() {
                    errorInternal(errorCode);
                }
            });
        }

        @Override
        public void onFixedResult(final RecognizeResult result, final String fixedString, boolean isLast) {
            LogUtils.infoRelease(VOICE_TAG, "onFixedResult: " + LogUtils.filterSensitiveLog(fixedString)
                    + ", isOffline = " + result.isOffline() + ", mRecordStatus = " + mRecordStatus);
            final boolean isBubbleActivity = AbstractVoiceActivity.this instanceof BubbleActivity;
            if (isLast) {
                UIHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        resultRecived(isBubbleActivity ? fixedString : result.getMainContent(), result.isOffline());
                    }
                });
            } else {
                UIHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        parcailResult(isBubbleActivity ? fixedString : result.getMainContent());
                    }
                });
            }
        }

        @Override
        public void onLocalResult(final RecognizeResult result) {
            LogUtils.infoRelease(VOICE_TAG, "onLocalResult");
            UIHandler.post(new Runnable() {
                @Override
                public void run() {
                    localResultInternal(result.getLocalResult());
                }
            });
        }
    };

    private static class IVoiceAssistantCallbackImpl extends IVoiceAssistantCallback.Stub {
        WeakReference<AbstractVoiceActivity> mActivityRef = null;

        public IVoiceAssistantCallbackImpl(AbstractVoiceActivity activity) {
            this.mActivityRef = new WeakReference<AbstractVoiceActivity>(activity);
        }

        @Override
        public void onLocalResult(final ParcelableObject result, boolean isRefresh)
                throws RemoteException {
            LogUtils.infoRelease(VOICE_TAG, "onLocalResult =" + isRefresh);
            UIHandler.post(new Runnable() {
                @Override
                public void run() {
                    AbstractVoiceActivity activity = mActivityRef.get();
                    if (activity != null) {
                        activity.localResultInternal(result);
                    }
                }
            });
        }

        @Override
        public void onError(final int errorCode, int type, boolean isToast)
                throws RemoteException {
            final AbstractVoiceActivity activity = mActivityRef.get();
            if (activity == null) {
                return;
            }
            LogUtils.infoRelease(VOICE_TAG, "onError " + errorCode + " mRecordStatus = " + activity.mRecordStatus);

            UIHandler.post(new Runnable() {
                @Override
                public void run() {
                    activity.errorInternal(errorCode);
                }
            });
        }

        @Override
        public void onVolumeUpdate(int volume) throws RemoteException {
        }

        @Override
        public void onResultRecived(final String resultStr, int type, final boolean offLine)
                throws RemoteException {
            final AbstractVoiceActivity activity = mActivityRef.get();
            if (activity == null) {
                return;
            }
            LogUtils.infoRelease(VOICE_TAG, "  onResultRecived = " + LogUtils.filterSensitiveLog(resultStr)
                    + " mRecordStatus = " + activity.mRecordStatus);
            UIHandler.post(new Runnable() {
                @Override
                public void run() {
                    activity.resultRecived(resultStr, offLine);
                }
            });
        }

        @Override
        public void onBuffer(final byte[] buffer) throws RemoteException {
            final AbstractVoiceActivity activity = mActivityRef.get();
            if (activity == null) {
                return;
            }
            LogUtils.infoRelease(VOICE_TAG, "onBuffer  mRecordStatus = " + activity.mRecordStatus);
            UIHandler.post(new Runnable() {
                @Override
                public void run() {
                    activity.bufferInternal(buffer);
                }
            });
        }

        @Override
        public void onPartialResult(final String partialResult) throws RemoteException {
            LogUtils.infoRelease(VOICE_TAG, "onPartialResult partialResult = " + LogUtils.filterSensitiveLog(partialResult));
            if (TextUtils.isEmpty(partialResult)) {
                return;
            }
            UIHandler.post(new Runnable() {
                @Override
                public void run() {
                    AbstractVoiceActivity activity = mActivityRef.get();
                    if (activity != null) {
                        activity.parcailResult(partialResult);
                    }
                }
            });
        }

        @Override
        public void onRecordStart() throws RemoteException {
            LogUtils.infoRelease(VOICE_TAG, "onRecordStart ");
            UIHandler.post(new Runnable() {
                @Override
                public void run() {
                    AbstractVoiceActivity activity = mActivityRef.get();
                    if (activity != null) {
                        activity.recordStartedInternal();
                    }
                }
            });
        }

        @Override
        public void onRecordEnd() throws RemoteException {
            LogUtils.infoRelease(VOICE_TAG, "onRecordEnd ");
            UIHandler.post(new Runnable() {
                @Override
                public void run() {
                    AbstractVoiceActivity activity = mActivityRef.get();
                    if (activity != null) {
                        activity.recordEndedInternal();
                    }
                }
            });
        }
    }
}
