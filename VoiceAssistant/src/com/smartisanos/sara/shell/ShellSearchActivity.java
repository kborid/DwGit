package com.smartisanos.sara.shell;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningTaskInfo;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Point;
import android.graphics.PointF;
import android.media.AudioManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.os.SystemClock;
import android.os.Vibrator;
import android.text.TextUtils;
import android.view.Display;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewStub;
import android.view.WindowManager;
import android.widget.FrameLayout.LayoutParams;
import android.widget.Toast;
import android.os.Build;

import com.smartisanos.ideapills.common.util.CommonUtils;
import com.smartisanos.ideapills.common.util.BlurTask;
import com.smartisanos.sanbox.utils.SaraTracker;
import com.smartisanos.sara.BubbleActionUpHelper;
import com.smartisanos.sara.R;
import com.smartisanos.sara.bubble.AbstractVoiceActivity;
import com.smartisanos.sara.bubble.manager.BubbleManager;
import com.smartisanos.sara.bubble.view.IWaveLayout;
import com.smartisanos.sara.bubble.view.WaveLayout;
import com.smartisanos.sara.util.ActivityUtil;
import com.smartisanos.sara.util.AnimManager;
import com.smartisanos.sara.util.LogUtils;
import com.smartisanos.sara.util.MultiSdkAdapter;
import com.smartisanos.sara.util.RecognizeHelper;
import com.smartisanos.sara.util.SaraConstant;
import com.smartisanos.sara.util.SaraUtils;
import com.smartisanos.sara.util.SharePrefUtil;
import com.smartisanos.sara.entity.SpeechError;
import com.smartisanos.sara.util.StringUtils;
import com.smartisanos.sara.widget.BubbleItemView;
import com.smartisanos.sara.widget.BubbleItemView.BubbleSate;
import com.smartisanos.sara.widget.LocalSearchLayout;
import com.smartisanos.sara.widget.WaveView.AnimationListener;
import com.smartisanos.sara.widget.WebSearchLayout;

import java.lang.ref.WeakReference;
import java.util.LinkedHashMap;
import java.util.List;

import smartisanos.api.VibEffectSmt;
import smartisanos.api.VibratorSmt;
import smartisanos.app.numberassistant.YellowPageResult;
import smartisanos.app.voiceassistant.ApplicationStruct;
import smartisanos.app.voiceassistant.ContactStruct;
import smartisanos.app.voiceassistant.IVoiceAssistantCallback;
import smartisanos.app.voiceassistant.MediaStruct;
import smartisanos.app.voiceassistant.ParcelableObject;
import smartisanos.util.LogTag;
import smartisanos.util.SidebarUtils;
import smartisanos.util.DeviceType;

import smartisanos.widget.SmartisanBlankView;

/**
 * 套壳呼出搜索页面
 */
public class ShellSearchActivity extends AbstractVoiceActivity implements AnimationListener ,SaraUtils.BaseViewListener {
    public static final String TAG = "VoiceAss.ShellSearchActivity";

    private static final int MSG_START_UI = 3;
    private static final int MSG_CREATE_WAV_ROOT_FILE = 5;
    private static final int MSG_FINISH_ACTIVITY_ANIM = 7;
    private static final int MSG_NO_RESULT_TIP = 8;

    // stopped after 10s, force stop this interaction
    private static final int SHOW_TOP_OF_SCREEN_FLAGS = WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED;

    private static final int RECORD_STATUS_IDLE = 0;
    private static final int RECORD_STATUS_STARTING = 1;
    private static final int RECORD_STATUS_STARTED = 2;
    private static final int RECORD_STATUS_RESULT_OK_STOPPED = 9;

    private static final int WAKE_UP_START_STATUS_NONE = 0;
    private static final int WAKE_UP_START_STATUS_PENDING = 1;
    private static final int WAKE_UP_START_STATUS_STARTED_AFTER_PAUSE = 2;
    private static final int WAKE_UP_START_STATUS_STARTED_NORMAL = 3;

    private int mWakeUpStartStatus = WAKE_UP_START_STATUS_NONE;

    ParcelableObject mParcelableObject;

    private int mDisplayWidth;
    private int mDisplayHeight;
    private int mRealScreenHeight;
    private View mNoResultView;
    private View mResultLayout;
    private View mResultEmpty;
    private LocalSearchLayout mLocalSearchLayout;
    private WebSearchLayout mWebSearchLayout;
    private BubbleItemView  mBubbleItemViewLeft;
    private IWaveLayout mWaveLayout;
    private ViewStub mBubbleLeftStub;
    private ViewStub mResultStub;
    private ViewStub mNoResultStub;
    private Vibrator mVibrator;
    private View mRootView;
    private String mLastTaskActivityName = "";
    private boolean mLastSideBarShowing;
    private boolean dlgHasShown = false;
    private StringBuffer result = new StringBuffer();
    private boolean mExistLocalView = false;
    private boolean mExistWebView = false;
    private boolean mIsOffLine = false;
    private boolean mIsMaxWave = false;
    private int mLeftBubbleWaveMinWidth;
    private int mResulTopMargin;
    private int mResulBottomMargin;
    private int mAvailableHeight;
    private static int mHeadSetTime = 0;
    private boolean mSaveHeadSetTime;
    AudioManager mAudioManager;
    private boolean isKeyOrTouchUp;
    private boolean mResumeKeyboard = true;
    private boolean mSleepValue;
    private boolean mLocalResultCall = false;
    private PowerManager mPowerManager;
    private boolean mConvertTranslucent = false;
    protected boolean mPendingFinishing;
    private boolean mIsKeyOrTouchUp;

    private BubbleActionUpHelper.MenuActionUpListener mActionUpListener = new BubbleActionUpHelper.MenuActionUpListener() {
        @Override
        public void onActionUp() {
            LogTag.d(TAG, "ACTION_MENU_UP");
            endKeyOrBlueRec();
        }
    };

    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            BubbleItemView bubbleItemView = getCurrentBubbleView();
            if (SaraConstant.ACTION_RECORD_ERROR.equals(action) && !dlgHasShown) {
                showDialog(context);
            } else if (SaraConstant.ACTION_FINISH_BUBBLE_ACTIVITY.equals(action)){
                finish();
            } else if (Intent.ACTION_CLOSE_SYSTEM_DIALOGS.equals(action)) {
                String reason = intent.getStringExtra("reason");
                if (SaraConstant.ACTION_REASON_HOME_KEY.equals(reason)) {
                    finish();
                }
            } else if (SaraConstant.ACTION_CHOOSE_RESULT.equals(action)) {
                SaraUtils.dismissKeyguardOnNextActivity();
                ComponentName componentName = (ComponentName) intent.getParcelableExtra(SaraConstant.EXTRA_CHOSEN_COMPONENT);
                if (componentName == null || !getPackageName().equals(componentName.getPackageName())) {
                    boolean delay = intent.getBooleanExtra("delay", false);
                    if (delay) {
                        mCustomHandler.sendMessageDelayed(mCustomHandler.obtainMessage(MSG_FINISH_ACTIVITY_ANIM), 600);
                    } else {
                        mCustomHandler.sendMessage(mCustomHandler.obtainMessage(MSG_FINISH_ACTIVITY_ANIM));
                    }
                }
            }
        }
    };

    @Override
    protected void localResult(ParcelableObject resultData){
        if (isStopped()) {
            return;
        }

        if(mResultLayout == null){
            initResultLayout();
        }
        boolean isLocalEnabled = SaraUtils.getLocalInputEnabled(this);
        if (isLocalEnabled){
            List<ApplicationStruct> app = resultData.getApps();
            List<ContactStruct> contact = resultData.getContacts();
            List<YellowPageResult> yellow = resultData.getYellowPages();
            List<MediaStruct> music = resultData.getMusics();
            if (mLocalSearchLayout != null) {
                mLocalSearchLayout.setData(app, contact, yellow, music);
            }
        }
        LogUtils.d(TAG,"localResult isLocalEnabled:" + isLocalEnabled);
        mLocalResultCall = true;
        mParcelableObject = resultData;

        if (this.result.length() <= SaraConstant.INTERVAL_SHORT && mLocalSearchLayout != null && mLocalSearchLayout.hasData()) {
            refreshSearchView(true, true);
        }
    }

    @Override
    protected void error(int errorCode){
        switch (errorCode) {
            case SpeechError.MSP_ERROR_NET_SENDSOCK:
            case SpeechError.MSP_ERROR_NET_RECVSOCK:
                Toast.makeText(this, R.string.network_error_tips, Toast.LENGTH_SHORT).show();
            case SpeechError.ERROR_MSP_TIMEOUT:
            case SpeechError.ERROR_MSP_NO_DATA:
            case SpeechError.ERROR_MSP_BOS_TIMEOUT:
            case SpeechError.ERROR_SPEECH_TIMEOUT:
            case SpeechError.MSP_ERROR_NET_CONNECTCLOSE:
            case SpeechError.ERROR_INTERRUPT:
            case SpeechError.ERROR_FACADE_BUSY:
            default:
                postShowNoResultView(errorCode);
                break;
        }
    }

    @Override
    protected void resultRecived(String resultStr, boolean offline){
        LogUtils.d(TAG, "resultRecived" + result.toString() + "--- resultStr = " + resultStr);
        if (isStopped()) {
            return;
        }

        mWaveLayout.hide();

        if (!isWaitingRecordResult()) {
            LogUtils.d(TAG, "onResultRecived ignore:" + getRecordStatus());
            return;
        }
        if (mAudioManager != null) {
            mAudioManager.abandonAudioFocus(null);
        }

        result.append(resultStr);
        String temp = result.toString();
        okResultHandled();
        LogUtils.d(TAG, "resultRecived temp:" + temp.length());
        if ((TextUtils.isEmpty(temp) || (temp.length() == 1 && StringUtils.isChinesePunctuation(temp.charAt(0))))
                || temp.length() > SaraConstant.INTERVAL_LONG) {
            showNoResultView();
            resetSearchViewData();
        } else {
            mCustomHandler.removeMessages(MSG_START_UI);
            mIsOffLine = offline;
            if (temp.length() <= 10 && StringUtils.isChinesePunctuation(temp.charAt(temp.length()-1))){
                result.deleteCharAt(temp.length()-1);
            }
            if (mIsMaxWave) {
                playPopup2TextAnim();
            }
            loadResult(false, result.toString());
        }
    }

    @Override
    protected void resultTimeOut() {
        showNoResultViewDuringRecording(-1);
    }

    @Override
    protected void buffer(byte[] buffer, int totalPoint){
        mWaveLayout.waveChanged(buffer, totalPoint);
    }

    @Override
    protected void parcailResult(String partialResult) {
        result.append(partialResult);
        if (mIsMaxWave && isWaitingRecordResult()) {
            playPopup2TextAnim();
        }
    }
    @Override
    protected void recordStarted() {
    }

    @Override
    protected void recordEnded(boolean isShortRecord, boolean isMaxRecord) {
        mLocalResultCall = false;
        if (isShortRecord) {
            mWaveLayout.stopWaveAnim(true);
            postShowNoResultView();
        } else {
            mWaveLayout.stopWaveAnim(false);
        }
        if (isMaxRecord) {
            checkTimeOutWarning();
        }
    }

    void endKeyOrBlueRec(){
        mIsKeyOrTouchUp = true;
        if (getRecordStatus() == RECORD_STATUS_IDLE) {
            mWakeUpStartStatus = WAKE_UP_START_STATUS_NONE;
            finish();
        }
        stopRecognize(false);
    }

    private CustomHandler mCustomHandler = new CustomHandler(this);
    private static class CustomHandler extends Handler {
        private final WeakReference<ShellSearchActivity> mActivity;

        public CustomHandler(ShellSearchActivity activity) {
            mActivity = new WeakReference<ShellSearchActivity>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            ShellSearchActivity activity = mActivity.get();
            LogTag.d(TAG, "handleMessage id " + msg.what);
            if (activity != null) {
                switch (msg.what) {
                    case MSG_START_UI:
                        activity.startUI();
                        break;
                    case MSG_CREATE_WAV_ROOT_FILE:
                        SaraUtils.buildWavRootPathAsync(activity);
                        break;
                    case MSG_FINISH_ACTIVITY_ANIM:
                        activity.finish();
                        break;
                    case MSG_NO_RESULT_TIP:
                        activity.showNoResultViewDuringRecording(msg.arg1);
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
        getWindow().addFlags(SHOW_TOP_OF_SCREEN_FLAGS);
        SaraUtils.setSearchType(this, SaraUtils.BUBBLE_TYPE.SHELL_SEARCH.name());
        mPendingFinishing = false;
        mSleepValue = getIntent().getBooleanExtra(SaraConstant.SCREEN_OFF_KEY, false);
        mIsKeyOrTouchUp = false;
        if (!SaraUtils.isSettingEnable(this) || !RecognizeHelper.getInstance().isAllowedToRecognize()) {
            finish();
            return;
        }
        LogUtils.infoRelease(TAG, "onCreate():" + getRecordStatus());
        mPowerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);

        LayoutInflater inflater = LayoutInflater.from(this);
        mRootView = inflater.inflate(R.layout.sara_shell_search_main, null);
        Display display = getWindowManager().getDefaultDisplay();
        mDisplayWidth = display.getWidth();
        mDisplayHeight = display.getHeight();
        Point realSize = new Point();
        display.getRealSize(realSize);
        mRealScreenHeight = realSize.y;

        mVibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

        IntentFilter filter = new IntentFilter(SaraConstant.ACTION_RECORD_ERROR);
        filter.addAction(SaraConstant.IMAGE_LOADER_CACHE_CHANGE);
        filter.addAction(SaraConstant.ACTION_FINISH_BUBBLE_ACTIVITY);
        filter.addAction(SaraConstant.ACTION_UPDATE_BUBBLE);
        filter.addAction(SaraConstant.ACTION_IDEAPILLS_SHARE_INVITATION_SEND);
        filter.addAction(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
        filter.addAction(SaraConstant.ACTION_CHOOSE_RESULT);
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        registerReceiver(mReceiver, filter);

        mCustomHandler.removeCallbacksAndMessages(null);
        SaraUtils.buildWavRootPathAsync(this);
        mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        initDimens();

        if (mSleepValue) {
            gaussianBlur(true);
        } else {
            gaussianBlur(false);
        }
        setContentView(mRootView);
        initView(inflater);

        mWakeUpStartStatus = mSleepValue ? WAKE_UP_START_STATUS_PENDING : WAKE_UP_START_STATUS_NONE;
        if (mWakeUpStartStatus != WAKE_UP_START_STATUS_PENDING) {
            preStartVoiceRecognition( true);
            mWakeUpStartStatus = WAKE_UP_START_STATUS_STARTED_NORMAL;
        }
    }

    public void initView(LayoutInflater inflater) {
        mWaveLayout = new WaveLayout(this, null,
                (ViewStub) findViewById(R.id.wave_left_stub), this);
        mBubbleLeftStub = (ViewStub)findViewById(R.id.bubble_item_left_stub);
        mResultStub = (ViewStub)findViewById(R.id.result_stub);
        mNoResultStub = (ViewStub)findViewById(R.id.no_result_stub);
        mWaveLayout.init(true);
    }

    public void initResultLayout() {
        if (mResultLayout == null) {
            if (mResultStub.getParent() != null) {
                mResultLayout = mResultStub.inflate();
            } else {
                mResultLayout = findViewById(R.id.result);
            }
            mResultEmpty = mResultLayout.findViewById(R.id.result_hide_empty);
            mLocalSearchLayout = (LocalSearchLayout) mResultLayout.findViewById(R.id.local_result);
            mLocalSearchLayout.init(this);
            mLocalSearchLayout.setViewListener(this);
            mWebSearchLayout = (WebSearchLayout) mResultLayout.findViewById(R.id.web_result);
            mWebSearchLayout.setViewListener(this);
            mWebSearchLayout.init();
        }
        mResultEmpty.setTranslationY(0);
        mLocalSearchLayout.setTranslationY(0);
        mWebSearchLayout.setTranslationY(0);
    }

    private void checkWakeUpAndPreStartVoiceRecognition() {
        // Because BubbleAct may pause, stop by activitySlept before we wakeup screen at onWindowFocused
        if (mWakeUpStartStatus == WAKE_UP_START_STATUS_PENDING) {
            if (isPaused()) {
                mWakeUpStartStatus = WAKE_UP_START_STATUS_STARTED_AFTER_PAUSE;
            } else {
                mWakeUpStartStatus = WAKE_UP_START_STATUS_STARTED_NORMAL;
            }
            if (Build.VERSION.SDK_INT >= 26) {
                // when >= 26, activity is turn on in manifest
            } else {
                MultiSdkAdapter.wakeUp(mPowerManager, SystemClock.uptimeMillis(), getPackageName() + ":window_focus");
            }
            preStartVoiceRecognition(true);
        } else if (mWakeUpStartStatus == WAKE_UP_START_STATUS_STARTED_AFTER_PAUSE) {
            mWakeUpStartStatus = WAKE_UP_START_STATUS_STARTED_NORMAL;
        }
    }

    private BubbleItemView getCurrentBubbleView() {
        if(mBubbleItemViewLeft == null) {
            initBubbleViewLeft();
        }
        return mBubbleItemViewLeft;
    }

    public void initBubbleViewLeft() {
        if (mBubbleLeftStub != null && mBubbleLeftStub.getParent() != null) {
            mBubbleItemViewLeft = (BubbleItemView) mBubbleLeftStub.inflate();
        } else {
            mBubbleItemViewLeft = (BubbleItemView) findViewById(R.id.bubble_item_left);
        }
        mBubbleItemViewLeft.updateTargetWidth(mDisplayWidth);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            checkWakeUpAndPreStartVoiceRecognition();
        }
    }

    public void preStartVoiceRecognition (boolean createFlag) {
        if (isFinishing()) {
            return;
        }
        startRecognize();
        if (createFlag) {
            mCustomHandler.sendEmptyMessageDelayed(MSG_START_UI, 200);
        } else {
            mCustomHandler.sendEmptyMessage(MSG_START_UI);
        }
        SaraUtils.setVoiceInputMode(this,  SaraConstant.VOICE_INPUT_MODE);

        registerVoiceCallback();

        if (DeviceType.is(DeviceType.TRIDENT)) {
            CommonUtils.vibrateEffect(this, VibEffectSmt.EFFECT_RECORDING);
        } else if (SaraUtils.isGlobalVibrateOn(this)) {
            VibratorSmt.getInstance().vibrateWithPrivilege(mVibrator, SaraConstant.VIBRATE_TIME);
        }
        if (createFlag) {
            mHeadSetTime = SharePrefUtil.getSearchInfoValue(this, SaraConstant.PREF_HEADSET_LAUNCH_TIME, 0);
            mSaveHeadSetTime = mHeadSetTime >= 5 ? false : true;
        }
    }

    public void initDimens() {
        mLeftBubbleWaveMinWidth = getResources().getDimensionPixelSize(R.dimen.bubble_wave_left_min_width);
        mResulTopMargin = getResources().getDimensionPixelSize(R.dimen.search_top_margin);
        mResulBottomMargin = getResources().getDimensionPixelSize(R.dimen.search_bottom_margin);
        mAvailableHeight = mDisplayHeight -mResulTopMargin- mResulBottomMargin;
    }

    @Override
    protected void onNewIntent(Intent intent) {
        LogUtils.infoRelease(TAG, "onNewIntent():" + getRecordStatus());
        super.onNewIntent(intent);
        SaraUtils.setSearchType(this, SaraUtils.BUBBLE_TYPE.SHELL_SEARCH.name());
        mPendingFinishing = false;
        if (!SaraUtils.isSettingEnable(this) || !RecognizeHelper.getInstance().isAllowedToRecognize()) {
            finish();
            return;
        }
        setIntent(intent);
        mSleepValue = getIntent().getBooleanExtra(SaraConstant.SCREEN_OFF_KEY, false);
        gaussianBlur(false);
        mCustomHandler.removeCallbacksAndMessages(null);
        clearAnimation();
        mResumeKeyboard = false;
        isKeyOrTouchUp = false;

        if (mSleepValue && !mPowerManager.isScreenOn()) {
            PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
            MultiSdkAdapter.wakeUp(pm, SystemClock.uptimeMillis(), getPackageName() + ":window_focus");
        }
        preStartVoiceRecognition(false);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        if (mWebSearchLayout != null) {
            mWebSearchLayout.onRestoreInstanceState(savedInstanceState);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mWebSearchLayout != null) {
            mWebSearchLayout.onSaveInstanceState(outState);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        LogTag.d(TAG,"onResume()");
        mCustomHandler.removeMessages(MSG_FINISH_ACTIVITY_ANIM);
        SaraUtils.setSearchType(this, SaraUtils.BUBBLE_TYPE.SHELL_SEARCH.name());
        if (mWebSearchLayout != null) {
            mWebSearchLayout.onResume();
        }
        BubbleItemView bubbleItemView = getCurrentBubbleView();
        if (bubbleItemView != null) {
            bubbleItemView.onResume();
            if (mResumeKeyboard) {
                bubbleItemView.showSoftInputFromWindow();
            }
        }
        if (!mConvertTranslucent) {
            convertFromTranslucent();
            mConvertTranslucent = true;
        }
    }

    private boolean isRecordingWithoutInteraction() {
        return isRecognizing() &&
                (mIsKeyOrTouchUp || isPendingFinishing() || mWakeUpStartStatus == WAKE_UP_START_STATUS_NONE);
    }

    private boolean isWaitingWakeUpAndStart() {
        return mWakeUpStartStatus != WAKE_UP_START_STATUS_NONE
                && mWakeUpStartStatus != WAKE_UP_START_STATUS_STARTED_NORMAL
                && !mIsKeyOrTouchUp && !isPendingFinishing()
                && Build.VERSION.SDK_INT < 26;
    }

    @Override
    public void onPause() {
        super.onPause();
        LogTag.d(TAG,"enter onPause():" + getRecordStatus());
        boolean screenOn = mPowerManager.isScreenOn();
        if (mWebSearchLayout != null) {
            mWebSearchLayout.onPause();
        }
        mResumeKeyboard = true;

        if (isRecordingWithoutInteraction()) {
            stopRecognize(true);
        }

        LogTag.d(TAG,"result = " + result);
    }

    @Override
    protected void onStop() {
        super.onStop();
        LogUtils.infoRelease(TAG, "onStop()");
        if (mSaveHeadSetTime) {
            SharePrefUtil.setSearchInfoValue(this, SaraConstant.PREF_HEADSET_LAUNCH_TIME,
                    mHeadSetTime > 5 ? 5 : mHeadSetTime);
        }
        if (mAudioManager != null) {
            mAudioManager.abandonAudioFocus(null);
        }

        if (!isWaitingWakeUpAndStart()) {
            boolean isUiChanging = clearAnimation();
            boolean isFinishPosted = mCustomHandler.hasMessages(MSG_FINISH_ACTIVITY_ANIM);
            boolean isScreenOn = mPowerManager.isScreenOn();
            mCustomHandler.removeCallbacksAndMessages(null);
            if (!isScreenOn || isFinishPosted) {
                LogUtils.d(TAG, "finish ourself due to screen off!");
                mCustomHandler.sendEmptyMessageDelayed(MSG_FINISH_ACTIVITY_ANIM, 100);
            } else if (!isPendingFinishing()) {
                if (isUiChanging || getRecordStatus() != RECORD_STATUS_RESULT_OK_STOPPED) {
                    if (getRecordStatus() == RECORD_STATUS_STARTING || getRecordStatus() == RECORD_STATUS_STARTED) {
                        showNoResultView(SpeechError.ERROR_INTERRUPT);
                    } else {
                        showNoInterruptView();
                    }
                }
            }
        } else {
            checkWakeUpAndPreStartVoiceRecognition();
            mCustomHandler.sendEmptyMessageDelayed(MSG_FINISH_ACTIVITY_ANIM, 1000);
        }
        if (Build.VERSION.SDK_INT >= 26) {
            ActivityUtil.setTurnScreenOn(this, false);
        }
        BubbleActionUpHelper.INSTANCE.removeActionUpListener(mActionUpListener);
    }

    @Override
    protected void onStart() {
        super.onStart();
        LogUtils.infoRelease(TAG, "onStart()");
        BubbleActionUpHelper.INSTANCE.addActionUpListener(mActionUpListener);
    }

    @Override
    public void finish() {
        if (isFinishing()) {
            return;
        }
        super.finish();
        LogTag.d(TAG,"enter finish()");
        SaraUtils.overridePendingTransition(this, true);
    }
    @Override
    public void onDestroy() {
        LogTag.d(TAG,"onDestroy()");
        mCustomHandler.removeCallbacksAndMessages(null);
        if (mLocalSearchLayout != null) {
            mLocalSearchLayout.destroy();
        }

        if (mWebSearchLayout != null) {
            mWebSearchLayout.onDestroy();
        }
        try {
            // Activity may finish without register any receiver or bind any service
            // Just ignore the exception.
            unregisterReceiver(mReceiver);
        } catch (Exception e) {
        }
        super.onDestroy();
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (ev.getAction() == MotionEvent.ACTION_DOWN) {
            if (mWaveLayout.checkFinish(ev) && SaraUtils.checkFinish(mLocalSearchLayout, ev)
                    && SaraUtils.checkFinish(mWebSearchLayout, ev)) {
                if (!isRecognizing() ||
                        (mNoResultView != null && mNoResultView.getVisibility() == View.VISIBLE)) {
                    finish();
                }
            }
        }
        try {
            return super.dispatchTouchEvent(ev);
        } catch (Exception e) {
            return false;
        }
    }

    public void startUI() {
        LogTag.d(TAG,"enter startUI()");
        result = new StringBuffer();
        setParamWidth(mLeftBubbleWaveMinWidth);
        resetView();
        mWaveLayout.show(SaraUtils.getDefaultBubbleColor(this));
    }

    @Override
    protected void realStartRecognize() {
        LogTag.d(TAG,"startRecognize");
        super.realStartRecognize();
        result = new StringBuffer();
        mIsOffLine = false;
    }

    public void loadResult(boolean update,String tmp) {
        if(mResultLayout == null){
            initResultLayout();
        }

        loadWebSearchResult(tmp);
        int lenght = tmp.length();
        LogUtils.d(TAG, "loadResult -- lenght = " + lenght);
        if (lenght <= SaraConstant.INTERVAL_SHORT) {
            refreshSearchView(mLocalResultCall, update);
        } else if (lenght > SaraConstant.INTERVAL_SHORT
                && lenght < SaraConstant.INTERVAL_LONG){
            refreshSearchView(false, update);
        } else {
            showNoResultView();
        }
        if (!update) {
            SaraUtils.dismissKeyguardOnNextActivity();
        }
    }

    private void loadWebSearchResult(String tmp) {
        boolean isWebEnabled = SaraUtils.getWebInputEnabled(this);
        if (mWebSearchLayout != null && isWebEnabled && SaraUtils.isNetworkConnected()) {
            mWebSearchLayout.setSearchText(tmp);
            mWebSearchLayout.loadUrl();
        }
    }

    private void resetSearchViewData() {
        if (mLocalSearchLayout != null) {
            mLocalSearchLayout.setData(null, null, null, null);
            mLocalSearchLayout.setVisibility(View.GONE);
        }
        if (mWebSearchLayout != null) {
            mWebSearchLayout.setSearchText(null);
            mWebSearchLayout.setVisibility(View.GONE);
            mWebSearchLayout.hideSearchPopupWindow();
        }
    }


    private void refreshSearchView(boolean hasLocal, boolean update) {
        boolean isLocalEnabled = SaraUtils.getLocalInputEnabled(this);
        if (isLocalEnabled && mLocalSearchLayout != null && hasLocal) {
            mLocalSearchLayout.notifyDataSetChanged();
            boolean hasData = mLocalSearchLayout.hasData();
            if (hasData) {
                mExistLocalView = true;
            } else {
                mExistLocalView = false;
            }
        } else {
            mExistLocalView = false;
        }
        boolean isWebEnabled = SaraUtils.getWebInputEnabled(this);
        if (isWebEnabled && mWebSearchLayout != null && SaraUtils.isNetworkConnected()) {
            if (!TextUtils.isEmpty(mWebSearchLayout.getSearchText())) {
                mExistWebView = true;
            } else {
                mExistWebView = false;
            }
        } else {
            mExistWebView = false;
        }

        LogUtils.d(TAG,"mExistWebView = "+ mExistWebView + "---mExistLocalView = " + mExistLocalView);

        if (mExistWebView && mExistLocalView) {
            setHideEmptyViewVisible();
            initResultHeight();

            mLocalSearchLayout.setVisibility(View.VISIBLE);
            mWebSearchLayout.setVisibility(View.VISIBLE);
        } else if (mExistWebView) {
            setHideEmptyViewVisible();
            LayoutParams webParams = (LayoutParams) mWebSearchLayout.getLayoutParams();
            webParams.height = mAvailableHeight;
            webParams.topMargin = mResulTopMargin;//resultHeight + mLocalWeb3MarginTop - getResources().getDimensionPixelOffset(R.dimen.bubble_local_reduce_gap);
            mWebSearchLayout.setLayoutParams(webParams);

            mLocalSearchLayout.setVisibility(View.GONE);
            mWebSearchLayout.setVisibility(View.VISIBLE);
            mWebSearchLayout.showSearchPopupWindow(mAvailableHeight);
        } else if (mExistLocalView) {
            setHideEmptyViewVisible();
            LayoutParams localParams = (LayoutParams) mLocalSearchLayout.getLayoutParams();
            localParams.height = mAvailableHeight;
            localParams.topMargin = mResulTopMargin;//resultHeight + mLocalWeb3MarginTop - getResources().getDimensionPixelOffset(R.dimen.bubble_local_reduce_gap);
            mLocalSearchLayout.setLayoutParams(localParams);

            mLocalSearchLayout.setVisibility(View.VISIBLE);
            mLocalSearchLayout.hideHideView();
            mWebSearchLayout.setVisibility(View.GONE);
            mWebSearchLayout.hideSearchPopupWindow();
        } else {
            showNoResultView();
        }
        if (!update) {
            cardAnimation();
        }
        boolean hasResult = mExistLocalView || mExistWebView;
        LogUtils.d("refreshSearchView hasResult:"+hasResult);
        trackResultEvent(hasResult);
    }

    private void trackResultEvent(boolean hasResult) {
        LinkedHashMap<String, Object> trackerData = new LinkedHashMap<String, Object>();
        trackerData.put("result", hasResult ? 1 : 0);
        SaraTracker.onEvent("A429001", trackerData);
    }

    public int setHideEmptyViewVisible() {
        LayoutParams emptyParams = (LayoutParams) mResultEmpty.getLayoutParams();
        mResultEmpty.setVisibility(View.VISIBLE);
        return emptyParams.height;
    }

    public void cardAnimation() {
        if (mExistLocalView) {
            AnimManager.showViewWithAlphaAndTranslate(mLocalSearchLayout, 150, 250, 150);
        }
        if (mExistWebView) {
            AnimManager.showViewWithAlphaAndTranslate(mWebSearchLayout, 150, 250, 150);
        }
    }

    public void gaussianBlur(boolean isFake) {
        ActivityManager am = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
        List<RunningTaskInfo> runningTask = am.getRunningTasks(2);
        String tmp = null;
        try {
            tmp = runningTask.get(1).topActivity.getClassName();
        } catch (Exception e) {
            LogUtils.e(TAG, " get last task fail");
        } finally {
            boolean sideBarShow = SidebarUtils.isSidebarShowing(this);
            if (isFake) {
                mRootView.setBackgroundResource(R.drawable.bg_mask);
                mLastTaskActivityName = (tmp == null ? "" : tmp);
                mLastSideBarShowing = sideBarShow;
            } else {
                if (!mLastTaskActivityName.equals(tmp) || mLastSideBarShowing != sideBarShow) {
                    final BlurTask bt = new BlurTask(mRootView, this);
                    bt.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                    mLastTaskActivityName = (tmp == null ? "" : tmp);
                    mLastSideBarShowing = sideBarShow;
                }
            }
        }
    }

    protected void postShowNoResultView() {
        postShowNoResultView(-1);
    }

    protected void postShowNoResultView(int errorCode) {
        mCustomHandler.removeMessages(MSG_NO_RESULT_TIP);
        Message message = mCustomHandler.obtainMessage(MSG_NO_RESULT_TIP);
        message.arg1 = errorCode;
        mCustomHandler.sendMessage(message);
    }

    private void showNoInterruptView() {
        noResultHandled();
        showNoResultView();
        resetSearchViewData();
    }

    private void showNoResultView() {
        showNoResultView(0);
    }

    private void showNoResultViewDuringRecording(final int errorCode) {
        if (!isWaitingRecordResult()) {
            LogUtils.d(TAG, "showNoResultView ignore:" + getRecordStatus());
            return;
        }
        LogUtils.d(TAG, "showNoResultViewDuringRecording:" + getRecordStatus());
        mCustomHandler.removeMessages(MSG_START_UI);
        noResultHandled();
        if (!isPendingFinishing()) {
            showNoResultView(errorCode);
        }
    }

    private void showNoResultView(final int errorCode) {
        LogUtils.d(TAG, "showNoResultView:" + errorCode);
        if (mNoResultView == null) {
            if (mNoResultStub.getParent() != null) {
                mNoResultView = mNoResultStub.inflate();
            } else {
                mNoResultView = findViewById(R.id.no_result);
            }
        }
        if (mAudioManager != null) {
            mAudioManager.abandonAudioFocus(null);
        }
        BubbleItemView bubbleItemView = getCurrentBubbleView();
        bubbleItemView.clearAnimation();
        bubbleItemView.setVisibility(View.GONE);
        SmartisanBlankView noResultView = (SmartisanBlankView) mNoResultView.findViewById(R.id.no_result_view);
        noResultView.getImageView().setVisibility(View.GONE);
        noResultView.getSecondaryHintView().setGravity(Gravity.LEFT);
        noResultView.getPrimaryHintView().setText(R.string.touch_speak_search_key_title);
        noResultView.getSecondaryHintView().setText("");
        switch (errorCode) {
            case SpeechError.ERROR_INTERRUPT:
            case SpeechError.ERROR_FACADE_BUSY:
                noResultView.getSecondaryHintView().setText(R.string.voice_search_interrupt);
                break;
        }
        mNoResultView.setVisibility(View.VISIBLE);
        mWaveLayout.hide();
        trackResultEvent(false);
    }

    public void showDialog(Context context) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context,
                AlertDialog.THEME_DEVICE_DEFAULT_LIGHT);
        builder.setTitle(R.string.record_warning_dialog_title);
        builder.setMessage(R.string.record_warning_dialog_message);
        builder.setPositiveButton(
                R.string.record_warning_dialog_positive_button,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        dialog.dismiss();
                        finish();
                    }
                });
        builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
            public void onDismiss(DialogInterface dialog) {
                dlgHasShown = false;
                finish();
            }
        });
        builder.show();
        dlgHasShown = true;
    }

    public boolean clearAnimation() {
        boolean isClearAnimation = false;
        if (mLocalSearchLayout != null) {
            if (mLocalSearchLayout.getAnimation() != null && !mLocalSearchLayout.getAnimation().hasEnded()) {
                isClearAnimation = true;
            }
            mLocalSearchLayout.clearAnimation();
        }
        if (mWebSearchLayout != null) {
            if (mWebSearchLayout.getAnimation() != null && !mWebSearchLayout.getAnimation().hasEnded()) {
                isClearAnimation = true;
            }
            mWebSearchLayout.clearAnimation();
        }

        isClearAnimation |= mWaveLayout.clearAnim();
        return isClearAnimation;
    }

    private void resetView() {
        mExistLocalView = false;
        mExistWebView = false;
        if (mLocalSearchLayout != null) {
            mLocalSearchLayout.setVisibility(View.GONE);
        }
        if (mWebSearchLayout != null) {
            mWebSearchLayout.setVisibility(View.GONE);
            mWebSearchLayout.hideSearchPopupWindow();
            mWebSearchLayout.reset();
        }
        if (mNoResultView != null) {
            mNoResultView.setVisibility(View.GONE);
        }

        BubbleItemView bubbleItemView = getCurrentBubbleView();
        bubbleItemView.setBubbleState(BubbleSate.INIT, true, false, false);
        bubbleItemView.setEditable(false);
        mIsMaxWave = false;
    }

    @Override
    public void onAnimationEnd(int width, boolean isCanceled) {
        if (isStopped()){
            return;
        }
        mIsMaxWave = true;
        LogTag.d(TAG, "mIsMaxWave is true");
        BubbleItemView bubbleItemView = getCurrentBubbleView();
        if (bubbleItemView.getBubbleState() == BubbleSate.INIT) {
            playPopup2TextAnim();
        }
    }

    @Override
    public void onAnimationCancel(int width) {
        if (mIsMaxWave) {
            playPopup2TextAnim();
        } else {
            BubbleItemView bubbleItemView = getCurrentBubbleView();
            if (mIsMaxWave && !TextUtils.isEmpty(result.toString())
                    && (bubbleItemView.getBubbleState() != BubbleSate.SMALL
                    && bubbleItemView.getBubbleState() != BubbleSate.LARGE)) {
                playPopup2TextAnim();
            }
        }
    }
    @Override
    public void onAnimationUpdate(int width) {
        setParamWidth(width);
    }

    public void setParamWidth(int width) {
        mWaveLayout.setContentWidth(width);

        BubbleItemView bubbleItemView = getCurrentBubbleView();
        if (bubbleItemView != null) {
            LayoutParams bubbleParams = (LayoutParams) bubbleItemView.getLayoutParams();
            if (bubbleParams.width != width) {
                bubbleParams.width = width + getResources().getDimensionPixelOffset(R.dimen.nomal_bubble_left_margin);
                bubbleItemView.setLayoutParams(bubbleParams);
            }
        }
    }


    public void playPopup2TextAnim() {
        if (mIsMaxWave) {
            mWaveLayout.waveMax(SaraUtils.getDefaultBubbleColor(this));
        }
        BubbleItemView bubbleItemView = getCurrentBubbleView();
        if (!TextUtils.isEmpty(result)) {
            mWaveLayout.hideTextPopup(false);
            bubbleItemView.setText(result.toString());
            bubbleItemView.setBubbleState(BubbleSate.NORMAL, true, false, false );
        } else {
            if (mIsMaxWave) {
                mWaveLayout.hideTextPopup(true);
                bubbleItemView.setBubbleState(BubbleSate.LOADING, true, false, false);
            }
        }
    }

    @Override
    public void hideView(int from, PointF point, boolean finish, boolean needSleep) {
        if (from == 1 && mWebSearchLayout.getVisibility() == View.VISIBLE) {
            AnimManager.hideViewWithAlphaAndTranslate(mWebSearchLayout, 0, 200, 150);
            mWebSearchLayout.hideSearchPopupWindow();
            setLocalSearchLayoutParams();
        }
    }

    public void setLocalSearchLayoutParams() {
        LayoutParams localParams = (LayoutParams) mLocalSearchLayout.getLayoutParams();
        localParams.height = mAvailableHeight;
        localParams.topMargin = mResulTopMargin;
        mLocalSearchLayout.setLayoutParams(localParams);

        mLocalSearchLayout.hideHideView();
    }

    @Override
    public Activity getActivityContext() {
        return this;
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        return super.dispatchKeyEvent(event);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        LogUtils.d(TAG, "onKeyUp keycode = " + keyCode);
        return super.onKeyUp(keyCode, event);
    }

    private void checkTimeOutWarning(){
        if (!mIsKeyOrTouchUp && hasWindowFocus() && SaraUtils.isGlobalVibrateOn(this)){
            VibratorSmt.getInstance().vibrateWithPrivilege(mVibrator, SaraConstant.VIBRATE_TIME);
        }
    }
    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }

    private void initResultHeight() {
        if (mParcelableObject == null) {
            return;
        }
        List<ApplicationStruct> app = mParcelableObject.getApps();
        List<ContactStruct> contact = mParcelableObject.getContacts();
        List<YellowPageResult> yellow = mParcelableObject.getYellowPages();
        List<MediaStruct> music = mParcelableObject.getMusics();
        int titleHeight = getResources().getDimensionPixelSize(R.dimen.local_title_height);
        int itemHeight = getResources().getDimensionPixelSize(R.dimen.local_item_height);
        int targetHeight = getResources().getDimensionPixelOffset(R.dimen.local_titlebar_height) + getResources().getDimensionPixelOffset(R.dimen.local_bottom_height);
        targetHeight += app.size() > 0 ? titleHeight : 0;
        targetHeight += contact.size() > 0 ? titleHeight : 0;
        targetHeight += yellow.size() > 0 ? titleHeight : 0;
        targetHeight += music.size() > 0 ? titleHeight : 0;
        for(ApplicationStruct a : app) {
            targetHeight += itemHeight;
        }
        for(ContactStruct c : contact) {
            targetHeight += itemHeight;
        }
        for(YellowPageResult y : yellow) {
            targetHeight += itemHeight;
        }
        for(MediaStruct m : music) {
            targetHeight += itemHeight;
        }

        int restHeight = 0;

        LayoutParams localParams = (LayoutParams) mLocalSearchLayout.getLayoutParams();
        int maxHeight = (mAvailableHeight - mResulTopMargin / 2) / 3;
        if (targetHeight > maxHeight) {
            localParams.height = maxHeight;
        } else {
            localParams.height = targetHeight;
            restHeight = maxHeight - targetHeight;
        }
        localParams.topMargin = mResulTopMargin;//resultHeight + mLocalWeb3MarginTop - getResources().getDimensionPixelOffset(R.dimen.bubble_local_reduce_gap);
        mLocalSearchLayout.setLayoutParams(localParams);

        LayoutParams webParams = (LayoutParams) mWebSearchLayout.getLayoutParams();
        int height = maxHeight * 2 + restHeight + mResulTopMargin;
        webParams.height = height;
        webParams.topMargin = mResulTopMargin / 2 + localParams.height;//localParams.topMargin + localParams.height + mLocalWeb3MarginTop;
        mWebSearchLayout.setLayoutParams(webParams);

        mWebSearchLayout.showSearchPopupWindow(height);
    }

    protected boolean isPendingFinishing() {
        return isFinishing() || mPendingFinishing;
    }
}
