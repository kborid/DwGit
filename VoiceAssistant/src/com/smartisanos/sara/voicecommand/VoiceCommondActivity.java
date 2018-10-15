package com.smartisanos.sara.voicecommand;

import android.app.ActivityManager;
import android.app.KeyguardManager;
import android.app.StatusBarManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.AssetFileDescriptor;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.os.RemoteException;
import android.os.SystemClock;
import android.text.TextUtils;
import android.view.Display;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewStub;
import android.view.WindowManager;
import android.widget.Toast;

import com.smartisanos.ideapills.common.util.CommonUtils;
import com.smartisanos.ideapills.common.util.MutiTaskHandler;
import com.smartisanos.ideapills.common.util.BlurTask;
import com.smartisanos.ideapills.common.util.VoiceAssistantServiceConnection;
import com.smartisanos.ideapills.common.util.VoiceAssistantServiceConnection.VoiceServiceListener;

import com.smartisanos.sara.BaseActivity;
import com.smartisanos.sara.BubbleActionUpHelper;
import com.smartisanos.sara.R;

import com.smartisanos.sara.bubble.view.IWaveLayout;
import com.smartisanos.sara.bubble.view.WaveLayout;
import com.smartisanos.sara.util.BubbleSpeechPlayer;
import com.smartisanos.sara.util.LogUtils;
import com.smartisanos.sara.util.MultiSdkAdapter;
import com.smartisanos.sara.util.RecognizeHelper;
import com.smartisanos.sara.util.SaraConstant;
import com.smartisanos.sara.util.SaraUtils;
import com.smartisanos.sara.util.ToastUtil;
import com.smartisanos.ideapills.common.util.UIHandler;
import com.smartisanos.sara.util.SoundManager;
import com.smartisanos.sara.entity.SpeechError;
import com.smartisanos.sara.util.StringUtils;
import com.smartisanos.sara.util.WaveformGenerator;
import com.smartisanos.sara.widget.WaveView;
import com.smartisanos.sanbox.utils.SaraTracker;

import smartisanos.app.voiceassistant.IVoiceAssistantCallback;
import smartisanos.app.voiceassistant.ParcelableObject;
import smartisanos.widget.SmartisanBlankView;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import smartisanos.util.IdeaPillsUtils;
import smartisanos.util.LogTag;
import smartisanos.util.SidebarUtils;

public class VoiceCommondActivity extends BaseActivity implements WaveView.AnimationListener, VoiceServiceListener {
    public static final String TAG = "VoiceAss.VoiceCommondActivity";
    private static final Executor sExecutor = Executors.newFixedThreadPool(4);
    private static final int MSG_START_REGOGNIZE = 1;
    private static final int MSG_END_RECOGNIZE = 2;
    private static final int MSG_FINISH_ACTIVITY_ANIM = 7;
    private static final int MSG_NO_RESULT_TIP = 8;
    private static final int MSG_TTS_PLAY = 9;
    private static final int TYPE_UNKNOW = -1;
    private static final int TYPE_HOME = 0;
    private static final int TYPE_MENU = 1;
    private static final int TYPE_DOUBLE_SIDE = 2;
    private static final int TYPE_HEADSET = 3;
    private static final int TYPE_BRIGHTNESS = 4;
    private static final int TYPE_BLUETOOTH = 5;
    private static final int SHOW_TOP_OF_SCREEN_FLAGS = WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED;

    private static final int INVALID_VOICE_INPUT = 21;
    private static final int NO_RESULT_ERROR = 22;
    private static final int NETWORK_ERROR = 23;
    private static final int INTERRUPT_ERROR = 24;
    private static final int AUDIO_ERROR = 25;

    private static final int COMMAND_NAV_ADDR = 1;
    private static final int COMMAND_NAV_HOME = 2;
    private static final int COMMAND_WEB = 3;
    private static final int COMMAND_TRANSLATE = 4;
    private static final int COMMAND_BOOM = 5;
    private static final int COMMAND_LOCATION = 6;
    private static final int COMMAND_WECHAT_PAY = 7;
    private static final int COMMAND_WECHAT_SCAN = 8;
    private static final int COMMAND_ALIPAY_SCAN = 9;
    private static final int COMMAND_ALIPAY = 10;
    private static final int COMMAND_TIME = 11;

    private int mDisplayWidth;
    private int mDisplayHeight;
    private ViewStub mNoResultStub;
    private View mNoResultView;
    private IWaveLayout mWaveLayout;
    private WaveformGenerator mWaveformGenerator;
    private int mLastPointNum = 0;
    private byte[] mSharedBuf = new byte[1024];
    private View mRootView;
    private String mLastTaskActivityName = "";
    private boolean mLastSideBarShowing;
    private boolean dlgHasShown = false;
    private StringBuffer result = new StringBuffer();
    private boolean mIsMaxWave = false;
    private int mBubbleWavMinWidth;
    private int mBubbleTextPopupHeight;
    private boolean mRecognizing = false;
    private boolean mIgnoreResult;
    MediaPlayer mMediaPlayer;
    AudioManager mAudioManager;
    private boolean isKeyOrTouchUp;
    private long mRecordStartTime;
    private boolean isActivityVisble = false;
    private long SHORT_PRESS_TIMEOUT = 500;
    private boolean mIsShortPress;
    private VoiceAssistantServiceConnection mServiceConnection;
    private boolean mReRecongnize;
    private VoiceCommondActivity.IVoiceAssistantCallbackImpl mCallback;
    private boolean isActionUp = false;
    public static boolean isActivityExist = false;

    public HashMap<Integer, String> mCommands = new HashMap<>();

    private VoiceCommand mCmd;
    private boolean mDelayAbandonAudioFocus;

    private BubbleActionUpHelper.MenuActionUpListener mActionUpListener = new BubbleActionUpHelper.MenuActionUpListener() {
        @Override
        public void onActionUp() {
            LogTag.d(TAG, "ACTION_MENU_UP");
            isActionUp = true;
            endRecord();
        }
    };

    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (SaraConstant.ACTION_RECORD_ERROR.equals(action) && !dlgHasShown) {
                //TODO can add some video alert
            }
        }
    };

    private static class IVoiceAssistantCallbackImpl extends IVoiceAssistantCallback.Stub {
        WeakReference<VoiceCommondActivity> mActivityRef = null;
        boolean mStarted = false;

        public IVoiceAssistantCallbackImpl(VoiceCommondActivity activity) {
            this.mActivityRef = new WeakReference<VoiceCommondActivity>(activity);
        }

        @Override
        public void onLocalResult(final ParcelableObject result, boolean isRefresh)
                throws RemoteException {
            LogUtils.e(TAG, "onLocalResult =" + isRefresh);
        }

        @Override
        public void onError(final int errorCode, int type, boolean isToast)
                throws RemoteException {
            final VoiceCommondActivity activity = mActivityRef.get();
            LogUtils.e(TAG, "onError " + errorCode + " shortPress =" + activity.mIsShortPress);
            if (activity == null || (activity.mIsShortPress && !TextUtils.isEmpty(activity.result.toString()))) {

                return;
            }

            if (SpeechError.ERROR_INTERRUPT == errorCode && !mStarted) {
                LogUtils.e(TAG, "ignore error 20017 for recognize not started!");
                return;
            }

            UIHandler.post(new Runnable() {
                @Override
                public void run() {
                    activity.error(errorCode);
                }
            });
        }

        @Override
        public void onVolumeUpdate(int volume) throws RemoteException {
            LogUtils.e(TAG, "onVolumeUpdate");
        }

        @Override
        public void onResultRecived(final String resultStr, int type, final boolean offLine)
                throws RemoteException {
            final VoiceCommondActivity activity = mActivityRef.get();
            LogUtils.e(TAG, activity.isActivityVisble + "  onResultRecived = " + resultStr
                    + " mIgnoreResult = " + activity.mIgnoreResult);

            UIHandler.post(new Runnable() {
                @Override
                public void run() {
                    activity.resultRecived(resultStr, offLine);
                }
            });
        }

        @Override
        public void onBuffer(final byte[] buffer) throws RemoteException {
            final VoiceCommondActivity activity = mActivityRef.get();
            LogUtils.i(TAG, "onBuffer  entranceType = "
                    + " mWaveformGenerator = " + activity.mWaveformGenerator + " " + buffer.length);
            UIHandler.post(new Runnable() {
                @Override
                public void run() {
                    activity.buffer(buffer);
                }
            });
        }

        @Override
        public void onPartialResult(final String partialResult) throws RemoteException {
            LogUtils.d(TAG, "onPartialResult  " + " partialResult = " + partialResult);
            if (TextUtils.isEmpty(partialResult)) {
                return;
            }
            UIHandler.post(new Runnable() {
                @Override
                public void run() {
                    VoiceCommondActivity activity = mActivityRef.get();
                    activity.parcailResult(partialResult);
                }
            });
        }

        @Override
        public void onRecordStart() throws RemoteException {
            LogUtils.d(TAG, "onRecordStart ");
            mStarted = true;
            UIHandler.post(new Runnable() {
                @Override
                public void run() {
                    VoiceCommondActivity activity = mActivityRef.get();
                    activity.recordStart();
                }
            });
        }

        @Override
        public void onRecordEnd() throws RemoteException {
            LogUtils.d(TAG, "onRecordEnd ");
            mStarted = false;
            UIHandler.post(new Runnable() {
                @Override
                public void run() {
                    VoiceCommondActivity activity = mActivityRef.get();
                    activity.recordEnd();
                }
            });
        }
    }

    private void error(int errorCode) {
        mServiceConnection.unregisterCallback(mCallback, getPackageName());
        mRecognizing = false;
        if (errorCode == -1) {
            return;
        }
        Message message = mCustomHandler.obtainMessage(MSG_NO_RESULT_TIP);
        switch (errorCode) {
            case SpeechError.ERROR_AUDIO_RECORD:
                message.arg1 = AUDIO_ERROR;
                break;
            case SpeechError.ERROR_MSP_NO_DATA:
                message.arg1 = INVALID_VOICE_INPUT;
                break;
            case SpeechError.MSP_ERROR_NET_SENDSOCK:
            case SpeechError.MSP_ERROR_NET_RECVSOCK:
                ToastUtil.showToast(this, R.string.network_error_tips, Toast.LENGTH_SHORT);
            case SpeechError.ERROR_MSP_TIMEOUT:
            case SpeechError.ERROR_MSP_BOS_TIMEOUT:
            case SpeechError.ERROR_SPEECH_TIMEOUT:
            case SpeechError.MSP_ERROR_NET_CONNECTCLOSE:
            case SpeechError.ERROR_INTERRUPT:
            case SpeechError.ERROR_FACADE_BUSY:
            default:
                message.arg1 = NETWORK_ERROR;
                break;
        }
        mCustomHandler.sendMessage(message);
    }

    private void resultRecived(String resultStr, boolean offline) {
        mServiceConnection.unregisterCallback(mCallback, getPackageName());
        if (!isActivityVisble) {
            return;
        }
        mWaveLayout.hide();
        // ignore the callback return from last search, avoid UI error
        if (mIgnoreResult) {
            LogUtils.d(TAG, "onResultRecived return of true ");
            mIgnoreResult = false;
            return;
        }
        if (mAudioManager != null) {
            mAudioManager.abandonAudioFocus(null);
        }
        Message message = mCustomHandler.obtainMessage(MSG_NO_RESULT_TIP);
        message.arg1 = INVALID_VOICE_INPUT;
        if ((TextUtils.isEmpty(resultStr) || (resultStr.length() == 1 && StringUtils.isChinesePunctuation(resultStr.charAt(0))))
                && TextUtils.isEmpty(result.toString())) {
            mCustomHandler.sendMessage(message);
        } else {
            result.append(resultStr);
            String temp = result.toString();
            if (temp.length() <= 10 && StringUtils.isChinesePunctuation(temp.charAt(temp.length() - 1))) {
                result.deleteCharAt(temp.length() - 1);
            }

            final CharSequence cmd = SaraUtils.formatResultString(result.toString());
            LogUtils.d("cmd = " + cmd);
            if (cmd.length() <= 0) {
                mCustomHandler.sendMessage(message);
                return;
            }

            if (startAppFromLauncher(cmd)) {
                SaraTracker.onEvent("A440004", "result", 1);
                finish();
                return;
            }

            mRecognizing = false;
            int handleResult = VoiceCommand.FINISH_NOT_HANDLED;
            boolean needUnlock = false;
            boolean needFocusText = false;
            final CharSequence matchedCmd = VoiceCommandUtils.matchCommand(cmd, mCommands.values());
            if (matchedCmd != null) {
                SaraTracker.onEvent("A440005", "func", matchedCmd);
                if (matchedCmd.equals(mCommands.get(COMMAND_NAV_HOME))
                        || matchedCmd.equals(mCommands.get(COMMAND_WECHAT_PAY))
                        || matchedCmd.equals(mCommands.get(COMMAND_WECHAT_SCAN))
                        || matchedCmd.equals(mCommands.get(COMMAND_ALIPAY))
                        || matchedCmd.equals(mCommands.get(COMMAND_ALIPAY_SCAN))) {
                    needUnlock = true;
                } else if (matchedCmd.equals(mCommands.get(COMMAND_NAV_ADDR))
                        || matchedCmd.equals(mCommands.get(COMMAND_WEB))
                        || matchedCmd.equals(mCommands.get(COMMAND_TRANSLATE))
                        || matchedCmd.equals(mCommands.get(COMMAND_BOOM))) {
                    needFocusText = true;
                }

                KeyguardManager km = (KeyguardManager) getSystemService(KEYGUARD_SERVICE);
                if (km.isKeyguardLocked()) {
                    if (needFocusText) {
                        ToastUtil.showToast(R.string.voice_command_error_no_focus_item);
                        finish();
                        SaraTracker.onEvent("A440004", "result", 0);
                        return;
                    }

                    if (km.isKeyguardSecure()) {
                        if (needUnlock) {
                            ToastUtil.showToast(R.string.voice_command_prompt_unlock);
                            DelayCommandReceiver.setDelayCommand(matchedCmd);
                            finish();
                            return;
                        }
                    } else {
                        if (needUnlock) {
                            CommonUtils.dismissKeyguard();
                        }
                    }
                }

                handleResult = mCmd.deliver(matchedCmd);
            }

            if (handleResult == VoiceCommand.FINISH_HANDLED) {
                finish();
                SaraTracker.onEvent("A440004", "result", 1);
            } else if (handleResult == VoiceCommand.FINISH_NOT_HANDLED) {
                // deliver cmd to voice button.
                UIHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        VoiceCommandEnvironment env = VoiceCommandEnvironment.getInstance();
                        if (env != null) {
                            env.clickVoiceButton(cmd.toString(), new VoiceCommandEnvironment.EnvironmentCallback() {
                                @Override
                                public void onVoiceButtonResult(boolean success) {
                                    SaraTracker.onEvent("A440004", "result", success ? 1 : 0);
                                    if (!success) {
                                        ToastUtil.showToast(com.smartisanos.sara.R.string.voice_command_error_no_focus_item);
                                    }
                                }
                            });
                        }
                    }
                }, 1000);
                finish();
            }
        }
    }

    private boolean startAppFromLauncher(CharSequence cmd) {
        VoiceCommandEnvironment env = VoiceCommandEnvironment.getInstance();
        //just use this function for launcher3.
        if (env != null && "com.android.launcher3".equals(env.getCurrentPackage())) {
            HashMap<String, Intent> apps = VoiceCommandUtils.getAllLaunchIntents(this);
            CharSequence targetApp = VoiceCommandUtils.matchCommand(cmd, apps.keySet());
            if (targetApp != null && apps.get(targetApp) != null) {
                if (VoiceCommandUtils.startActivityCommon(this, apps.get(targetApp))) {
                    LogUtils.d(TAG, "startAppFromLauncher...");
                    return true;
                }
            }
        }
        return false;
    }

    private void buffer(byte[] buffer) {
        if (mWaveformGenerator == null) {
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
        mWaveLayout.waveChanged(bytes, mWaveformGenerator.getTotalPoint());
    }

    private void parcailResult(String partialResult) {
        result.append(partialResult);
    }

    private void recordStart() {
        mWaveformGenerator = new WaveformGenerator(30, 16000, mSharedBuf);
        mWaveformGenerator.open(SaraUtils.getWaveTmpPath(VoiceCommondActivity.this) + SaraConstant.WAVE_FILE);
        mRecognizing = true;
        mIgnoreResult = false;
        mIsShortPress = false;
        mRecordStartTime = System.currentTimeMillis();
    }

    private void recordEnd() {
        //ignore when not headset and action not up
        if (!isActionUp && !mIsMaxWave) {
            return;
        }
        SoundManager.playSound(getApplicationContext(), false);
        mRecognizing = false;
        long currentTime = System.currentTimeMillis();
        if (currentTime - mRecordStartTime < SHORT_PRESS_TIMEOUT) {
            mIsShortPress = true;
            mWaveLayout.stopWaveAnim(true);
        } else {
            mWaveLayout.stopWaveAnim(false);
        }
        checkTimeOutWarning();
        if (mWaveformGenerator != null) {
            mWaveformGenerator.close();
        }
    }

    private void endRecord() {
        mReRecongnize = false;
        if (mRecognizing) {
            isKeyOrTouchUp = true;
            mCustomHandler.removeMessages(MSG_START_REGOGNIZE);
            //sometimes this task will be block by other task in main thread.
            //so run it with TaskHandler.
            //mCustomHandler.sendEmptyMessage(MSG_END_RECOGNIZE);
            MutiTaskHandler.post(new Runnable() {
                @Override
                public void run() {
                    mServiceConnection.stopRecongnize(true);
                }
            });
        }
    }

    private VoiceCommondActivity.CustomHandler mCustomHandler = new VoiceCommondActivity.CustomHandler(this);

    private static class CustomHandler extends Handler {
        private final WeakReference<VoiceCommondActivity> mActivity;

        public CustomHandler(VoiceCommondActivity activity) {
            mActivity = new WeakReference<VoiceCommondActivity>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            VoiceCommondActivity activity = mActivity.get();
            LogTag.d(TAG, "handleMessage id " + msg.what);
            if (activity != null) {
                switch (msg.what) {
                    case MSG_START_REGOGNIZE:
                        SaraUtils.stopAudioPlay(activity);
                        activity.startRecognize(true);
                        break;
                    case MSG_END_RECOGNIZE:
                        activity.mServiceConnection.stopRecongnize(true);
                        break;
                    case MSG_FINISH_ACTIVITY_ANIM:
                        activity.finish();
                        break;
                    case MSG_NO_RESULT_TIP:
                        activity.showNoResultView(msg.arg1);
                        break;
                    case MSG_TTS_PLAY:
                        activity.playTTS();
                        break;
                    default:
                        break;
                }
            }
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!RecognizeHelper.getInstance().isAllowedToRecognize()) {
            finish();
            return;
        }
        BubbleActionUpHelper.INSTANCE.addActionUpListener(mActionUpListener);

        mServiceConnection = new VoiceAssistantServiceConnection(this);
        mCallback = new VoiceCommondActivity.IVoiceAssistantCallbackImpl(this);
        mServiceConnection.bindVoiceService();
        mServiceConnection.setVoiceListener(this);
        showAsTopOfLockScreenIfNeeded();

        LayoutInflater inflater = LayoutInflater.from(this);
        mRootView = inflater.inflate(R.layout.voice_commond_main, null);
        Display display = getWindowManager().getDefaultDisplay();
        mDisplayWidth = display.getWidth();
        mDisplayHeight = display.getHeight();
        gaussianBlur();
        setContentView(mRootView);

        IntentFilter filter = new IntentFilter(SaraConstant.ACTION_RECORD_ERROR);
        filter.addAction(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
        registerReceiver(mReceiver, filter);

        isActivityExist = true;
        initView();
        initDimens();
        SaraUtils.buildWavRootPathAsync(this);
        mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        initRecord();
        mMediaPlayer = new MediaPlayer();
        mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        mMediaPlayer.setVolume(1.0f, 1.0f);

        initCommands();
        mCmd = getLinkedVoiceCommands();
        IdeaPillsUtils.callIdeaPills(this, "METHOD_HIDE_BUBBLE_LIST", null);

        SaraTracker.onLaunch();
    }

    private void initCommands() {
        mCommands.put(COMMAND_NAV_ADDR, getString(R.string.quick_command_nav_addr));
        mCommands.put(COMMAND_NAV_HOME, getString(R.string.quick_command_nav_home));
        mCommands.put(COMMAND_WEB, getString(R.string.quick_command_web));
        mCommands.put(COMMAND_TRANSLATE, getString(R.string.quick_command_translate));
        mCommands.put(COMMAND_BOOM, getString(R.string.quick_command_boom));
        mCommands.put(COMMAND_LOCATION, getString(R.string.quick_command_location));
        mCommands.put(COMMAND_WECHAT_PAY, getString(R.string.quick_command_wechat_pay));
        mCommands.put(COMMAND_WECHAT_SCAN, getString(R.string.quick_command_scan_wx));
        mCommands.put(COMMAND_ALIPAY_SCAN, getString(R.string.quick_command_scan_zfb));
        mCommands.put(COMMAND_ALIPAY, getString(R.string.quick_command_zfb_pay));
        mCommands.put(COMMAND_TIME, getString(R.string.quick_command_time));
    }

    private VoiceCommand.OnHandleFinishListener mVoiceCommandFinishListener = new VoiceCommand.OnHandleFinishListener() {
        @Override
        public void onFinish(boolean isSuccess) {
            finish();
        }
    };

    public VoiceCommand getLinkedVoiceCommands() {
        Context context = getApplicationContext();

        VoiceCommand aliPayCmd = new AliPayCmd(context, null);
        VoiceCommand aliPayScanCmd = new AliPayScanCmd(context, aliPayCmd);
        VoiceCommand translateCmd = new TranslateCmd(context, aliPayScanCmd);
        VoiceCommand locationCmd = new ReportLocationCmd(context, translateCmd);
        locationCmd.setOnHandleFinshListener(mVoiceCommandFinishListener);
        VoiceCommand boomCmd = new TextBoomCmd(context, locationCmd);
        VoiceCommand wechatPayCmd = new WechatPayCmd(context, boomCmd);
        VoiceCommand webUrlCmd = new WebUrlCmd(context, wechatPayCmd);
        VoiceCommand navi = new NaviFromScreenCmd(context, webUrlCmd);
        VoiceCommand reportTimeCmd = new ReportTimeCmd(context, navi);
        reportTimeCmd.setOnHandleFinshListener(mVoiceCommandFinishListener);
        VoiceCommand navHomeCmd = new NavigateHomeCmd(context, reportTimeCmd);
        VoiceCommand wechatScanCmd = new WechatScanCmd(context, navHomeCmd);
        return wechatScanCmd;
    }

    private SmartisanBlankView mNoResultViewTip;

    public void initView() {
        mWaveLayout = new WaveLayout(this, mRootView, this);
        mNoResultStub = (ViewStub) findViewById(R.id.no_result);
        mWaveLayout.init(true);
    }

    public void initDimens() {
        mBubbleWavMinWidth = getResources().getDimensionPixelSize(R.dimen.bubble_wave_left_min_width);
        mBubbleTextPopupHeight = getResources().getDimensionPixelSize(R.dimen.bubble_text_popup_height);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        LogTag.d(TAG, "onNewIntent");
        if (!SaraUtils.isSettingEnable(this) || !RecognizeHelper.getInstance().isAllowedToRecognize()) {
            finish();
            return;
        }
        setIntent(intent);
        com.smartisanos.sanbox.utils.SaraTracker.onEvent("A429000");
        showAsTopOfLockScreenIfNeeded();
        gaussianBlur();
        //avoid user launch again when last search is not ready
        if (mRecognizing) {
            mIgnoreResult = true;
            LogTag.d(TAG, "mIgnoreResult set true");
        }
        isActionUp = false;
        initRecord();
    }

    private void initRecord() {
        LogTag.d(TAG, "initRecord");
        if (isFinishing()) {
            return;
        }
        mCustomHandler.removeCallbacksAndMessages(null);
        clearAnimation(true);
        mServiceConnection.registerCallback(mCallback, getPackageName());
        screenOnAndCloseStatusBar();
        if (BubbleSpeechPlayer.getInstance(this).isPlaying()) {
            BubbleSpeechPlayer.getInstance(this).stop();
        }
        mCustomHandler.sendMessage(mCustomHandler.obtainMessage(MSG_START_REGOGNIZE));
        mAudioManager.requestAudioFocus(null, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT);
        SoundManager.playSound(getApplicationContext(), true);
//            mCustomHandler.sendEmptyMessageDelayed(MSG_TTS_PLAY, 2000);
        //TODO if need video start alert, add some other video
        startUI();

        SaraUtils.setVoiceInputMode(this, SaraConstant.VOICE_INPUT_MODE);
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        LogTag.d(TAG, "enter onPause() mRecognizing = " + mRecognizing);
        if (mRecognizing) {
            mRecognizing = false;
            showNoResultView(INTERRUPT_ERROR);
        }

        LogUtils.d(TAG, "result = " + result);
    }

    @Override
    protected void onStop() {
        super.onStop();
        LogTag.d(TAG, "enter onStop()");
        mCustomHandler.removeCallbacksAndMessages(null);
        if (!mDelayAbandonAudioFocus && mAudioManager != null) {
            mAudioManager.abandonAudioFocus(null);
        }
        isActivityVisble = false;
        isActivityExist = false;

        PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
        if (!pm.isScreenOn()) {
            finish();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        isActivityVisble = true;
        if (!isActivityExist) {
            isActivityExist = true;
        }
    }

    @Override
    public void finish() {
        super.finish();
        LogTag.d(TAG, "enter finish()");
        SaraUtils.overridePendingTransition(this, true);
    }

    @Override
    public void onDestroy() {
        if (mRootView != null) {
            mRootView.setBackground(null);
        }
        if (mMediaPlayer != null) {
            mMediaPlayer.release();
        }
        try {
            // Activity may finish without register any receiver or bind any service
            // Just ignore the exception.
            BubbleActionUpHelper.INSTANCE.removeActionUpListener(mActionUpListener);
            unregisterReceiver(mReceiver);
            mServiceConnection.unregisterCallback(mCallback, getPackageName());
            mServiceConnection.unBindVoiceService();
            mCallback = null;
        } catch (Exception e) {
        }
        isActivityExist = false;
        super.onDestroy();
    }

    public void startUI() {
        LogTag.d(TAG, "enter startUI()");
        result = new StringBuffer();
        clearAnimation(true);
        setParamWidth(mBubbleWavMinWidth);
        resetView();
        mWaveLayout.show(SaraUtils.getDefaultBubbleColor(this));
    }

    public void startRecognize(boolean needRecognize) {
        LogTag.d(TAG, "startRecognize needRecognize " + needRecognize);
//        VibratorSmt.getInstance().vibrateWithPrivilege(mVibrator, 50);
        if (!mServiceConnection.isServiceConnected()) {
            mReRecongnize = true;
            return;
        }
        result = new StringBuffer();
        mLastPointNum = 0;
        if (needRecognize) {
            mServiceConnection.startRecongnize(getPackageName(), null);
        }
    }

    private void trackResultEvent(boolean hasResult) {
        LinkedHashMap<String, Object> trackerData = new LinkedHashMap<String, Object>();
        trackerData.put("result", hasResult ? 1 : 0);
        com.smartisanos.sanbox.utils.SaraTracker.onEvent("A429001", trackerData);
    }

    private void showNoResultView(int errorType) {
        //ignore the callback return from last search, avoid UI error
        if (mIgnoreResult) {
            LogUtils.d(TAG, "showNoResultView return of true ");
            mIgnoreResult = false;
            return;
        }
        if (mNoResultView == null) {
            mNoResultView = mNoResultStub.inflate();
            mNoResultView.setVisibility(View.GONE);
        }

        mDelayAbandonAudioFocus = errorType == AUDIO_ERROR;
        if (!mDelayAbandonAudioFocus && mAudioManager != null) {
            mAudioManager.abandonAudioFocus(null);
        }
        if (mNoResultViewTip == null) {
            mNoResultViewTip = (SmartisanBlankView) mNoResultView.findViewById(R.id.no_result_view);
            mNoResultViewTip.getImageView().setVisibility(View.GONE);
            mNoResultViewTip.getSecondaryHintView().setGravity(Gravity.LEFT);
        }
        switch (errorType) {
            case INVALID_VOICE_INPUT:
                ToastUtil.showToast(this, R.string.blind_mode_invalid_voice, Toast.LENGTH_SHORT);
                mNoResultViewTip.getPrimaryHintView().setText(R.string.touch_speak_home);
                mNoResultViewTip.getSecondaryHintView().setText(R.string.blind_mode_invalid_voice);
                SaraTracker.onEvent("A440004", "result", 5);
                break;
            case NO_RESULT_ERROR:
                ToastUtil.showToast(this, R.string.blind_mode_no_result, Toast.LENGTH_LONG);
                mNoResultViewTip.getPrimaryHintView().setText(R.string.blind_mode_no_result);
                mNoResultViewTip.getSecondaryHintView().setText("");
                SaraTracker.onEvent("A440004", "result", 5);
                break;
            case NETWORK_ERROR:
                ToastUtil.showToast(this, R.string.blind_mode_network_error, Toast.LENGTH_SHORT);
                mNoResultViewTip.getPrimaryHintView().setText(R.string.blind_mode_network_error);
                mNoResultViewTip.getSecondaryHintView().setText("");
                SaraTracker.onEvent("A440004", "result", 4);
                break;
            case INTERRUPT_ERROR:
                ToastUtil.showToast(this, R.string.blind_mode_running_error, Toast.LENGTH_SHORT);
                mNoResultViewTip.getPrimaryHintView().setText("");
                mNoResultViewTip.getSecondaryHintView().setText("");
                SaraTracker.onEvent("A440004", "result", 3);
                break;
            case AUDIO_ERROR:
                ToastUtil.showToast(this, R.string.voice_commond_cannot_used, Toast.LENGTH_SHORT);
                mNoResultViewTip.getPrimaryHintView().setText("");
                mNoResultViewTip.getSecondaryHintView().setText("");
                SaraTracker.onEvent("A440004", "result", 2);
                finish();
                UIHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        mAudioManager.abandonAudioFocus(null);
                    }
                }, 2000);
            default:
                break;
        }

        mNoResultView.setVisibility(View.VISIBLE);
        mWaveLayout.hide();
        trackResultEvent(false);
    }

    public void hideNoResultView() {
        if (mNoResultView != null) {
            mNoResultView.setVisibility(View.GONE);
        }
    }

    public void clearAnimation(boolean cancel) {
        mWaveLayout.clearAnim();
    }

    public void resetView() {
        if (mNoResultView != null) {
            mNoResultView.setVisibility(View.GONE);
        }
        mIsMaxWave = false;
    }

    @Override
    public void onAnimationEnd(int width, boolean isCanceled) {
        if (!isActivityVisble) {
            return;
        }
        mIsMaxWave = true;
        LogTag.d(TAG, "mIsMaxWave is true");
    }

    @Override
    public void onAnimationCancel(int width) {
    }

    @Override
    public void onAnimationUpdate(int width) {
        setParamWidth(width);
    }

    public void setParamWidth(int width) {
        mWaveLayout.setContentWidth(width);
    }

    private void showAsTopOfLockScreenIfNeeded() {
        KeyguardManager km = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
        if (km.isKeyguardLocked()) {
            getWindow().addFlags(SHOW_TOP_OF_SCREEN_FLAGS);
        } else {
            getWindow().clearFlags(SHOW_TOP_OF_SCREEN_FLAGS);
        }
    }

    private void screenOnAndCloseStatusBar() {
        StatusBarManager sbm = (StatusBarManager) getSystemService(Context.STATUS_BAR_SERVICE);
        sbm.collapsePanels();
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        if (!pm.isScreenOn()) {
            MultiSdkAdapter.wakeUp(pm, SystemClock.uptimeMillis(), getPackageName() + ":vc_headset");
        }
    }

    private void playTTS() {
        try {
            mMediaPlayer.reset();
            AssetFileDescriptor fileDescriptor;
            if (Locale.TAIWAN.equals(Locale.getDefault()) || Locale.CHINA.equals(Locale.getDefault())) {
                fileDescriptor = getAssets().openFd("chinese.wav");
            } else {
                fileDescriptor = getAssets().openFd("english.wav");
            }
            mMediaPlayer.setDataSource(fileDescriptor.getFileDescriptor(), fileDescriptor.getStartOffset(),
                    fileDescriptor.getLength());
            mMediaPlayer.prepare();
            mAudioManager.requestAudioFocus(null, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT);
            mMediaPlayer.start();
            if (fileDescriptor != null) {
                fileDescriptor.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void checkTimeOutWarning() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - mRecordStartTime >= 60000 && !isKeyOrTouchUp) {
            if (mCustomHandler.hasMessages(MSG_TTS_PLAY)) {
                mCustomHandler.removeMessages(MSG_TTS_PLAY);
                mCustomHandler.sendEmptyMessage(MSG_TTS_PLAY);
            }
        }
    }

    @Override
    public void onServiceConnected() {
        if (mReRecongnize && !isFinishing()) {
            boolean needRec = mServiceConnection.reRecognize();
            LogTag.d(TAG, "startRec needRec " + needRec);
            startRecognize(!needRec ? false : true);
        }
    }

    public void gaussianBlur() {
        ActivityManager am = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
        List<ActivityManager.RunningTaskInfo> runningTask = am.getRunningTasks(2);
        String tmp = null;
        try {
            tmp = runningTask.get(1).topActivity.getClassName();
        } catch (Exception e) {
            LogUtils.e(TAG, " get last task fail");
        } finally {
            boolean sideBarShow = SidebarUtils.isSidebarShowing(this);
            if (!mLastTaskActivityName.equals(tmp) || mLastSideBarShowing != sideBarShow) {
                final BlurTask bt = new BlurTask(mRootView, this);
                bt.executeOnExecutor(sExecutor);
                mLastTaskActivityName = (tmp == null ? "" : tmp);
                mLastSideBarShowing = sideBarShow;
            }
        }
    }
}
