package com.smartisanos.sara.bubble;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningTaskInfo;
import android.app.ActivityThread;
import android.app.AlertDialog;
import android.app.StatusBarManager;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothHeadset;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.AssetFileDescriptor;
import android.graphics.Point;
import android.graphics.PointF;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.os.SystemClock;
import android.os.Vibrator;
import android.provider.Settings;
import android.service.onestep.GlobalBubble;
import android.service.onestep.GlobalBubbleAttach;
import android.support.v4.content.FileProvider;
import android.text.TextUtils;
import android.view.Display;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.SurfaceControl;
import android.view.View;
import android.view.ViewStub;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.Toast;

import com.smartisanos.ideapills.common.util.BlurTask;
import com.smartisanos.ideapills.common.event.Event;
import com.smartisanos.ideapills.common.util.CommonUtils;

import com.smartisanos.ideapills.common.util.MutiTaskHandler;
import com.smartisanos.ideapills.common.util.PackageUtils;
import com.smartisanos.ideapills.common.util.UIHandler;
import com.smartisanos.sanbox.utils.SaraTracker;
import com.smartisanos.sara.BubbleActionUpHelper;
import com.smartisanos.sara.R;
import com.smartisanos.sara.bubble.manager.BubbleCleaner;
import com.smartisanos.sara.bubble.manager.BubbleManager;
import com.smartisanos.sara.bubble.manager.BubbleThemeManager;
import com.smartisanos.sara.bubble.search.FlashImContactsEvent;
import com.smartisanos.sara.bubble.search.ISearchView;
import com.smartisanos.sara.bubble.search.SearchView;
import com.smartisanos.sara.bubble.view.FakeBubbleView;
import com.smartisanos.sara.bubble.view.IFakeBubbleView;
import com.smartisanos.sara.bubble.view.IWaveLayout;
import com.smartisanos.sara.bubble.view.WaveLayout;
import com.smartisanos.sara.bullet.contact.model.AbsContactItem;
import com.smartisanos.sara.bullet.contact.model.ContactItem;
import com.smartisanos.sara.bullet.service.FlashImConnection;
import com.smartisanos.sara.bullet.widget.MultiVoiceRecognizeResultView;
import com.smartisanos.sara.bullet.widget.VoiceRecognizeResultView;
import com.smartisanos.sara.bullet.widget.VoiceSearchView;
import com.smartisanos.sara.storage.BubbleDataRepository;
import com.smartisanos.sara.storage.DrawerDataRepository;
import com.smartisanos.sara.util.ActivityUtil;
import com.smartisanos.sara.util.AnimManager;
import com.smartisanos.sara.util.BubbleSpeechPlayer;
import com.smartisanos.sara.util.LogUtils;
import com.smartisanos.sara.util.MultiSdkAdapter;
import com.smartisanos.sara.util.RecognizeHelper;
import com.smartisanos.sara.util.SaraConstant;
import com.smartisanos.sara.util.SaraUtils;
import com.smartisanos.sara.util.SharePrefUtil;
import com.smartisanos.sara.util.SoundManager;
import com.smartisanos.sara.entity.SpeechError;
import com.smartisanos.sara.util.StringUtils;
import com.smartisanos.sara.util.ToastUtil;
import com.smartisanos.sara.widget.BubbleItemView;
import com.smartisanos.sara.widget.BubbleItemView.BubbleSate;
import com.smartisanos.sara.widget.WaveView.AnimationListener;

import smartisanos.app.voiceassistant.ParcelableObject;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import smartisanos.api.SettingsSmt;
import smartisanos.api.WindowManagerSmt;
import smartisanos.api.VibratorSmt;
import smartisanos.api.VibEffectSmt;
import smartisanos.app.voiceassistant.ParcelableObject;
import smartisanos.util.DeviceType;
import smartisanos.util.NStringUtils;
import smartisanos.util.SidebarUtils;
import smartisanos.widget.SmartisanBlankView;
import smartisanos.util.NStringUtils;

import com.smartisanos.sara.util.AttachmentUtils;
import android.content.ClipData;
import android.service.onestep.GlobalBubbleAttach;

public class BubbleActivity extends AbstractVoiceActivity implements AnimationListener, SaraUtils.BubbleViewChangeListener {
    public static final String TAG = "VoiceAss.BubbleActivity";

    public static final String FILEPROVIDER_AUTHORITY = "com.smartisanos.sara.fileprovider";
    private static final int MSG_START_UI = 3;
    private static final int MSG_CREATE_WAV_ROOT_FILE = 5;
    private static final int MSG_HIDE_GLOBLE_BUBBLE = 6;
    private static final int MSG_FINISH_ACTIVITY_ANIM = 7;
    private static final int MSG_NO_RESULT_TIP = 8;
    private static final int MSG_TTS_PLAY = 9;
    private static final int MSG_REFRESH_SEARCH_VIEW = 14;
    private static final int MSG_DELAY_ENSURE_WAKE_UP = 15;
    private static final int MSG_ADD_BUBBLE_AND_HIDE_VIEW = 20;

    private static final int TYPE_UNKNOW = -1;
    static final int TYPE_HOME = 0;
    static final int TYPE_MENU = 1;
    static final int TYPE_DOUBLE_SIDE = 2;
    static final int TYPE_HEADSET = 3;
    static final int TYPE_BRIGHTNESS = 4;
    static final int TYPE_BLUETOOTH = 5;
    private static final int SHOW_TOP_OF_SCREEN_FLAGS = WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED;

    private static final int BUBBLE_STATUS_NORMAL = 0;
    private static final int BUBBLE_STATUS_HIDDING = 1;

    private static final int WAKE_UP_START_STATUS_NONE = 0;
    private static final int WAKE_UP_START_STATUS_PENDING = 1;
    private static final int WAKE_UP_START_STATUS_STARTED_ONCE = 2;
    private static final int WAKE_UP_START_STATUS_STARTED_NORMAL = 3;

    private static final String[] INTERESTED_EVENTS = {
            FlashImContactsEvent.ACTION_SEND_BULLET_MESSAGE_PRE,
            FlashImContactsEvent.ACTION_SEND_BULLET_MESSAGE,
            FlashImContactsEvent.ACTION_SEND_BULLET_FRAGMENT_VISIBLE_STATE,
            FlashImContactsEvent.ACTION_SEND_BULLET_HIDE_IUPUT_METHOD
    };

    private int mBubbleStatus = BUBBLE_STATUS_NORMAL;
    private int mWakeUpStartStatus = WAKE_UP_START_STATUS_NONE;
    private int mDisplayWidth;
    private int mDisplayHeight;
    private int mRealScreenHeight;
    private View mNoResultView;
    protected ISearchView mSearchView;
    private BubbleItemView mBubbleItemView;
    private BubbleItemView mBubbleItemViewLeft;
    private ViewStub mBubbleStub;
    private ViewStub mBubbleLeftStub;
    private ViewStub mNoResultStub;
    private ViewStub mBulletVoiceSearchStub;
    private VoiceSearchView mBulletVoiceSearchView;
    private Vibrator mVibrator;
    private IFakeBubbleView mFakeBubbleView;
    private IWaveLayout mWaveLayout;
    private View mRootView;
    private String mLastTaskActivityName = "";
    private boolean mLastSideBarShowing;
    private boolean dlgHasShown = false;
    private StringBuffer result = new StringBuffer();
    private boolean mExistBubbleView = false;
    private boolean mExistLocalView = false;
    private boolean mExistWebView = false;
    private boolean mIsOffLine = false;
    private boolean mIsMaxWave = false;
    private GlobalBubble mGlobalBubble;
    private int mBubbleWavMinWidth;
    private int mLeftBubbleWaveMinWidth;
    private int mResulTopMargin;
    private int mStatusBarHeight;
    private boolean mHeadSetSearchStarted;

    private static int mHeadSetTime = 0;
    private boolean mSaveHeadSetTime;
    MediaPlayer mMediaPlayer;
    AudioManager mAudioManager;
    private boolean mIsKeyOrTouchUp;
    private boolean mResumeKeyboard = false;
    private boolean mSleepValue;
    private boolean mLocalResultCall = false;
    public static boolean sIsHeadSetOrBluetooth;
    private boolean mLastHeadSetState = false;
    private boolean mBluetoothAudioConnected;
    private boolean mBluetoothRecognize;
    private BluetoothHeadset mBluetoothHeadset;
    private boolean isFromSideBar = false;
    private PowerManager mPowerManager;
    private PowerManager.WakeLock mWakeLock;
    private boolean mConvertTranslucent = false;
    protected boolean mPendingFinishing;
    private boolean mIsPendingStartVoice = false;
    private boolean mIsErrorHaveShow = false;
    private FlashImConnection mFlashImConnection;
    private boolean mShouldRestoreState;
    private boolean mLastShowBullet;

    private BubbleActionUpHelper.MenuActionUpListener mActionUpListener = new BubbleActionUpHelper.MenuActionUpListener() {
        @Override
        public void onActionUp() {
            LogUtils.infoRelease(TAG, "ACTION_MENU_UP");
            SaraTracker.onEvent("A420034");

            if (stopVoiceSearchViewTouchAction()) {
                return;
            }

            endKeyOrBlueRec();
        }
    };

    private boolean stopVoiceSearchViewTouchAction() {
        if (isInterceptHandleSearchEvent()) {
            LogUtils.d(TAG, "stopVoiceSearchViewTouchAction" );
            // 结束录音.
            mBulletVoiceSearchView.startTouchAction(false);
            mIsPendingStartVoice = false;
            return true;
        }
        return false;
    }

    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            LogUtils.d(TAG, "action = " + action);
            final BubbleItemView bubbleItemView = getCurrentBubbleView();
            if (SaraConstant.ACTION_RECORD_ERROR.equals(action) && !dlgHasShown) {
                showDialog(context);
            } else if (SaraConstant.IMAGE_LOADER_CACHE_CHANGE.equals(action)) {
                mSearchView.notifyDataSetChanged();
            } else if (SaraConstant.ACTION_FINISH_BUBBLE_ACTIVITY.equals(action)){
                finish();
            } else if (SaraConstant.ACTION_UPDATE_BUBBLE.equals(action)){
                resultFromIntentUpdate(intent);
            } else if (SaraConstant.ACTION_IDEAPILLS_SHARE_INVITATION_SEND.equals(action)){
                updateBubble2Share();
            } else if (Intent.ACTION_CLOSE_SYSTEM_DIALOGS.equals(action)) {
                String reason = intent.getStringExtra("reason");
                if (SaraConstant.ACTION_REASON_HOME_KEY.equals(reason) || SaraConstant.ACTION_REASON_RECENT.equals(reason)) {
                    if (bubbleItemView != null && !isStopped() &&
                            mGlobalBubble != null && BubbleManager.isAddBubble2List()) {
                        bubbleItemView.addBubble2SideBar(true);
                    }
                    SaraUtils.overridePendingTransition(BubbleActivity.this, true);
                    finish();
                }
            } else if (SaraConstant.ACTION_CHOOSE_RESULT.equals(action)) {
                ComponentName componentName = (ComponentName) intent.getParcelableExtra(SaraConstant.EXTRA_CHOSEN_COMPONENT);
                if (componentName == null || !getPackageName().equals(componentName.getPackageName())) {
                   boolean delay = intent.getBooleanExtra("delay", false);
                    if (delay) {
                        mCustomHandler.sendMessageDelayed(mCustomHandler.obtainMessage(MSG_FINISH_ACTIVITY_ANIM), 600);
                    } else {
                        mCustomHandler.sendMessage(mCustomHandler.obtainMessage(MSG_FINISH_ACTIVITY_ANIM));
                    }
                }
            } else if (BluetoothHeadset.ACTION_AUDIO_STATE_CHANGED.equals(action)) {
                int btState = intent.getIntExtra(BluetoothProfile.EXTRA_STATE, -1);
                LogUtils.d(TAG, "ACTION_AUDIO_STATE_CHANGED btState:" + btState);
                switch (btState) {
                    case BluetoothHeadset.STATE_AUDIO_CONNECTED:
                        mBluetoothAudioConnected = true;
                        int launchType = getIntent().getIntExtra(SaraConstant.LUNCH_KEY, TYPE_UNKNOW);
                        if (!mBluetoothRecognize && launchType == TYPE_BLUETOOTH) {
                            startBluetoothRec();
                        }
                        break;
                    case BluetoothHeadset.STATE_AUDIO_DISCONNECTED:
                        mBluetoothAudioConnected = false;
                        endKeyOrBlueRec();
                        break;
                }
            } else if (SaraConstant.ACTION_RECEIVE_TEXTBOOM_CALLBACK.equals(action)) {
                String newText = intent.getStringExtra(Intent.EXTRA_TEXT);
                BubbleManager.handleTextBoomResult(BubbleActivity.this, mGlobalBubble, newText,
                        new BubbleManager.ITextBoomFinishedListener() {
                    @Override
                    public void onTextBoomFinished(String newText, String oldText, boolean isTextChanged) {
                        if (isTextChanged) {
                            bubbleItemView.setMaxLineAndHeight();
                            String text = mGlobalBubble != null ? mGlobalBubble.getText() : "";
                            bubbleItemView.setText(text);
                            loadResultForKeyboard(mGlobalBubble, false, false);
                        }
                    }
                });
            }
        }
    };
    private BluetoothProfile.ServiceListener mBluetoothProfileServiceListener =
            new BluetoothProfile.ServiceListener() {
                public void onServiceConnected(int profile, BluetoothProfile proxy) {
                    if (profile == BluetoothProfile.HEADSET) {
                        mBluetoothHeadset = (BluetoothHeadset) proxy;
                        startVoiceRecognition();
                    }
                }

                public void onServiceDisconnected(int profile) {
                    if (profile == BluetoothProfile.HEADSET) {
                        stopRecognize(false);
                        mBluetoothHeadset = null;
                    }
                }
            };

    private void initBlueToothHeadset() {
        if (getIntent() == null ||
                getIntent().getIntExtra(SaraConstant.LUNCH_KEY, TYPE_UNKNOW) != TYPE_BLUETOOTH) {
            return;
        }

        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        if (adapter != null) {
            adapter.getProfileProxy(getApplication(),
                    mBluetoothProfileServiceListener,
                    BluetoothProfile.HEADSET);
        }
    }
    private void startVoiceRecognition(){
        boolean bluetooth = TYPE_BLUETOOTH == getIntent().getIntExtra(SaraConstant.LUNCH_KEY, TYPE_UNKNOW);
        if (bluetooth && mBluetoothHeadset != null){
            List<BluetoothDevice> devices = mBluetoothHeadset.getConnectedDevices();
            LogUtils.df("devices = " + devices);
            if (devices != null && devices.size() > 0) {
                if (!mBluetoothHeadset.isAudioConnected(devices.get(0))){
                    setErrorState(false);
                    mBluetoothHeadset.startVoiceRecognition(devices.get(0));
                } else {
                    startBluetoothRec();
                }
            }
        }
    }

    @Override
    protected void localResult(final ParcelableObject result){
        if (isBluetooth()) {
            return;
        }
        if (isStopped() && !isFromSideBar) {
            return;
        }

        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                mSearchView.loadSearchResult(result, null);

                mLocalResultCall = true;
                if (mGlobalBubble != null && mGlobalBubble.getUri() != null &&
                        BubbleActivity.this.result.length() <= SaraConstant.INTERVAL_SHORT) {
                    if (result.getResultStr().equals(BubbleActivity.this.result.toString())) {
                        // 不立即refresh, 这个间隔保证动画更连续
                        if (mCustomHandler.hasMessages(MSG_REFRESH_SEARCH_VIEW)) {
                            postRefreshSearchView(true, true, false,
                                    false, false, 200);
                        } else {
                            postRefreshSearchView(true, true, false,
                                    false, false, 0);
                        }
                    }
                }
            }
        };
        if (mSearchView.ensureInitialized()) {
            mCustomHandler.post(runnable);
        } else {
            runnable.run();
        }
    }

    @Override
    protected void error(int errorCode){
        LogUtils.d(TAG, "errorCode = " + errorCode);
        if (!TextUtils.isEmpty(result)) {
            LogUtils.d(TAG, "errorCode  = " + errorCode + " , but content is not null ! content = " + result.toString());
            setErrorState(true);
            disposeResult("", true);
            return;
        }
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
        if (getErrorState()) {
            LogUtils.d(TAG, " because of have error by onError . the content is not null ,so have done!");
            return;
        } else {
            LogUtils.d(TAG, " no error by onError. can do result show!");
        }
        disposeResult(resultStr, offline);
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
        result.setLength(0);
        result.append(partialResult);
        if (mIsMaxWave && isWaitingRecordResult()) {
            playPopup2TextAnim();
        }
    }

    @Override
    protected void recordStarted() {
        SaraUtils.setBluetoothValue(this, 1);
    }

    @Override
    protected void recordEnded(boolean isShortRecord, boolean isMaxRecord) {
        SaraUtils.setBluetoothValue(this, 0);
        stopBluetoothRec();
        mLocalResultCall = false;
        boolean isHeadsetType = isHeadsetOrBluetooth() && hasWindowFocus() && isHeadsetConnected();
        if (isHeadsetType) {
            SoundManager.playSound(getApplicationContext(), false);
        }
        if (isShortRecord) {
            mWaveLayout.stopWaveAnim(true);
            if(!isHeadsetType) {
                postShowNoResultView();
            }
        } else {
            mWaveLayout.stopWaveAnim(false);
        }
        if (isMaxRecord) {
            checkTimeOutWarning();
        }
    }

    private boolean getErrorState() {
        return mIsErrorHaveShow;
    }

    private void setErrorState(boolean state) {
        mIsErrorHaveShow = state;
    }

    private void disposeResult(String resultStr, boolean offline) {
        if (isStopped() && !isFromSideBar) {
            return;
        }

        mWaveLayout.hide();
        if (!isWaitingRecordResult() && !isFromSideBar) {
            LogUtils.d(TAG, "onResultRecived ignore:" + getRecordStatus());
            return;
        }
        if (mAudioManager != null) {
            mAudioManager.abandonAudioFocus(null);
        }
        if ((TextUtils.isEmpty(resultStr) || (resultStr.length() == 1 && StringUtils.isChinesePunctuation(resultStr.charAt(0))))
                && TextUtils.isEmpty(result.toString())) {
            postShowNoResultView();
            mSearchView.resetSearchViewAndData(true, false);
        } else {
            mCustomHandler.removeMessages(MSG_START_UI);
            okResultHandled();
            mIsOffLine = offline;
            result.setLength(0);
            result.append(resultStr);
            String temp = result.toString();

            if (!TextUtils.isEmpty(temp) && temp.length() > 0) {
                char lastChar = temp.charAt(temp.length() - 1);
                if (temp.length() <= 10 && (StringUtils.isChinesePunctuation(lastChar) || String.valueOf(lastChar).equals("."))) {
                    result.deleteCharAt(temp.length() - 1);
                }
            }
            if (mIsMaxWave) {
                playPopup2TextAnim();
            }
            formatBubble(mIsOffLine);
            if (isBluetooth()) {
                PointF pointF = BubbleManager.addBubble2SideBar(this, mGlobalBubble, null, mIsOffLine, false);
                hideView(0, pointF, true, true);
                return;
            }
            if (isFromSideBar) {
                loadShareResult(false, resultStr, false);
            } else {
                loadResult(false, result.toString(), false);
            }
            // 若此时已经过了系统设置的熄屏 timeout, 释放 wakelock 会导致马上熄屏, 因此需要延迟一段时间再释放以给用户操作的时间
            releaseWakeLockDelayed(10 * 1000);
        }
    }

    private void startBluetoothRec(){
        LogUtils.df("mBluetoothRecognize = " + mBluetoothRecognize + "---mBluetoothAudioConnected = " + mBluetoothAudioConnected);
        if (!mBluetoothRecognize && mBluetoothAudioConnected){
            mBluetoothRecognize = true;
            startRecognize();
            if (mCustomHandler.hasMessages(MSG_TTS_PLAY)){
                mCustomHandler.removeMessages(MSG_TTS_PLAY);
                mCustomHandler.sendEmptyMessage(MSG_TTS_PLAY);
            }
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

    @Override
    protected void stopRecognize(boolean force) {
        super.stopRecognize(force);
        stopBluetoothRec();
    }

    private void stopBluetoothRec() {
        setErrorState(false);
        if (mBluetoothRecognize){
            mBluetoothRecognize = false;
            if (null != mBluetoothHeadset) {
                List<BluetoothDevice> devices = mBluetoothHeadset.getConnectedDevices();
                if (devices != null && devices.size() > 0) {
                    mBluetoothHeadset.stopVoiceRecognition(devices.get(0));
                }
            }
        }
    }

    private CustomHandler mCustomHandler = new CustomHandler(this);

    private static class CustomHandler extends Handler {
        private final WeakReference<BubbleActivity> mActivity;

        public CustomHandler(BubbleActivity activity) {
            mActivity = new WeakReference<BubbleActivity>(activity);
        }

        @Override
        public boolean sendMessageAtTime(Message msg, long uptimeMillis) {
            LogUtils.d(TAG, "sendMessage id " + msg.what + ", " + testPrint(msg.what));
            return super.sendMessageAtTime(msg, uptimeMillis);
        }

        private String testPrint(int msgId) {
            String msg = "UNKNOWN";
            switch (msgId) {
                case MSG_START_UI:
                    msg = "MSG_START_UI";
                    break;
                case MSG_CREATE_WAV_ROOT_FILE:
                    msg = "MSG_CREATE_WAV_ROOT_FILE";
                    break;
                case MSG_HIDE_GLOBLE_BUBBLE:
                    msg = "MSG_HIDE_GLOBLE_BUBBLE";
                    break;
                case MSG_FINISH_ACTIVITY_ANIM:
                    msg = "MSG_FINISH_ACTIVITY_ANIM";
                    break;
                case MSG_NO_RESULT_TIP:
                    msg = "MSG_NO_RESULT_TIP";
                    break;
                case MSG_TTS_PLAY:
                    msg = "MSG_TTS_PLAY";
                    break;
                case MSG_REFRESH_SEARCH_VIEW:
                    msg = "MSG_REFRESH_SEARCH_VIEW";
                    break;
                case MSG_DELAY_ENSURE_WAKE_UP:
                    msg = "MSG_DELAY_ENSURE_WAKE_UP";
                    break;
                case MSG_ADD_BUBBLE_AND_HIDE_VIEW:
                    msg = "MSG_ADD_BUBBLE_AND_HIDE_VIEW";
                    break;
            }
            return msg;
        }

        @Override
        public void handleMessage(Message msg) {
            BubbleActivity activity = mActivity.get();
            LogUtils.d(TAG, "handleMessage id " + msg.what + ", " + testPrint(msg.what));
            if (activity != null) {
                switch (msg.what) {
                case MSG_START_UI:
                    activity.startUI();
                    break;
                case MSG_CREATE_WAV_ROOT_FILE:
                    SaraUtils.buildWavRootPathAsync(activity);
                    break;
                case MSG_HIDE_GLOBLE_BUBBLE:
                    BubbleDataRepository.hideGlobleBubble(activity);
                    break;
                case MSG_FINISH_ACTIVITY_ANIM:
                    boolean clearAnim = msg.getData().getBoolean("clearAnim");
                    activity.finishInner(clearAnim);
                    break;
                case MSG_NO_RESULT_TIP:
                    activity.showNoResultViewDuringRecording(msg.arg1);
                    break;
                case MSG_TTS_PLAY:
                    if (mHeadSetTime < 5) {
                        Toast.makeText(activity, R.string.touch_speak_headset_toast, Toast.LENGTH_SHORT).show();
                    }
                    mHeadSetTime ++;
                    activity.playTTS();
                    break;
                case MSG_REFRESH_SEARCH_VIEW:
                    Bundle data = msg.getData();
                    boolean hasBubble = data.getBoolean("hasBubble");
                    boolean hasLocal = data.getBoolean("hasLocal");
                    boolean hasWeb = data.getBoolean("hasWeb");
                    boolean update = data.getBoolean("update");
                    boolean isSmallEdit = data.getBoolean("isSmallEdit");
                    boolean needScaleAnim = data.getBoolean("needScaleAnim");
                    activity.refreshSearchView(hasBubble, hasLocal, hasWeb, update, isSmallEdit, needScaleAnim);
                    break;
                case MSG_DELAY_ENSURE_WAKE_UP:
                    activity.checkWakeUpAndPreStartVoiceRecognition();
                    break;
                case MSG_ADD_BUBBLE_AND_HIDE_VIEW:
                    boolean finish = msg.getData().getBoolean("finish");
                    activity.addBubbleAndHideView(finish);
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
        SaraTracker.onEvent("A420031");
        getWindow().addFlags(SHOW_TOP_OF_SCREEN_FLAGS);
        mIsPendingStartVoice = false;
        SaraUtils.setSearchType(this, SaraUtils.BUBBLE_TYPE.PHONE_BUBBLE.name());
        mPendingFinishing = false;
        mIsKeyOrTouchUp = false;
        mSleepValue = getIntent().getBooleanExtra(SaraConstant.SCREEN_OFF_KEY, false);
        if (!SaraUtils.isSettingEnable(this) || !RecognizeHelper.getInstance().isAllowedToRecognize()) {
            finish();
            return;
        }
        BubbleActivityHelper.INSTANCE.attach(this);
        LogUtils.infoRelease(TAG, "onCreate():" + getRecordStatus());
        mPowerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        mWakeLock = mPowerManager.newWakeLock(PowerManager.FULL_WAKE_LOCK, "saraLock");
        SaraTracker.onLaunch();
        SaraTracker.onEvent("A429000");
        SaraTracker.onEvent("A420028","type",0);
        initBlueToothHeadset();
        isHeadsetOrBluetooth();
        mLastHeadSetState = sIsHeadSetOrBluetooth;
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
        filter.addAction(BluetoothHeadset.ACTION_AUDIO_STATE_CHANGED);
        filter.addAction(SaraConstant.ACTION_RECEIVE_TEXTBOOM_CALLBACK);
        registerReceiver(mReceiver, filter);

        DrawerDataRepository.INSTANCE.reloadAsync();

        isFromSideBar = getIntent().getBooleanExtra(SaraConstant.ACTION_FROM_SIDEBAR, false);
        int launchType =  getIntent().getIntExtra(SaraConstant.LUNCH_KEY, TYPE_UNKNOW);

        mCustomHandler.removeCallbacksAndMessages(null);
        SaraUtils.buildWavRootPathAsync(this);
        mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

        initDimens();
        LayoutInflater inflater = LayoutInflater.from(this);
        mRootView = BubbleActivityHelper.INSTANCE.getRootViewTag().attach(this);
        if (mSleepValue) {
            gaussianBlur(true);
        } else {
            gaussianBlur(false);
        }
        SaraUtils.closeSystemDialog();
        setContentView(mRootView);
        initView(inflater);

        String resultStr = getIntent().getStringExtra(SaraConstant.ACTION_FROM_SIDEBAR_CONTENT);
        if (isFromSideBar) {
            result = new StringBuffer();
            resultRecived(resultStr, true);
            return;
        }

        if (savedInstanceState != null
                && savedInstanceState.getParcelable("bubble") != null) {
            registerVoiceCallback();
            mGlobalBubble = savedInstanceState.getParcelable("bubble");
            ArrayList<GlobalBubbleAttach> attachArrayList = savedInstanceState.getParcelableArrayList("attach");
            BubbleItemView bubbleItemView = getCurrentBubbleView();
            bubbleItemView.initShareDrawerView();
            bubbleItemView.setAttachmentList(attachArrayList);
            bubbleItemView.refreshAttachmentView();
            mShouldRestoreState = true;
            mLastShowBullet = savedInstanceState.getBoolean("showBullet");
        } else {
            mWakeUpStartStatus = mSleepValue ? WAKE_UP_START_STATUS_PENDING : WAKE_UP_START_STATUS_NONE;
            if (mWakeUpStartStatus != WAKE_UP_START_STATUS_PENDING) {
                preStartVoiceRecognition(launchType, true);
                mWakeUpStartStatus = WAKE_UP_START_STATUS_STARTED_ONCE;
            } else {
                mCustomHandler.removeMessages(MSG_DELAY_ENSURE_WAKE_UP);
                mCustomHandler.sendEmptyMessageDelayed(MSG_DELAY_ENSURE_WAKE_UP, 500);
            }
            getCurrentBubbleView().setDefaultShowBulletFlag(getDefaultSettingShowBullet());
        }
        mFlashImConnection = new FlashImConnection(this);
        mFlashImConnection.bindService();
    }

    public void initView(LayoutInflater inflater) {
        mWaveLayout = new WaveLayout(this, (ViewStub) findViewById(R.id.wave_stub),
                (ViewStub) findViewById(R.id.wave_left_stub), this);
        mBubbleStub = (ViewStub)findViewById(R.id.bubble_item_stub);
        mBubbleLeftStub = (ViewStub)findViewById(R.id.bubble_item_left_stub);
        mNoResultStub = (ViewStub)findViewById(R.id.no_result_stub);
        mFakeBubbleView = new FakeBubbleView(this, (ViewStub) findViewById(R.id.fak_bubble_stub));
        mBulletVoiceSearchStub = (ViewStub) findViewById(R.id.bullet_voice_search_stub);
        mWaveLayout.init(SaraUtils.isLeftPopBubble());

        mSearchView = new SearchView(this, mDisplayHeight,mDisplayWidth,
                mRealScreenHeight, mStatusBarHeight);

        if(SaraUtils.isLeftPopBubble()) {
            initBubbleViewLeft();
        } else {
            initBubbleView();
        }
    }

    public void initBubbleView() {
        if (mBubbleStub != null && mBubbleStub.getParent() != null) {
            mBubbleItemView = (BubbleItemView)mBubbleStub.inflate();
        }else {
            mBubbleItemView = (BubbleItemView)findViewById(R.id.bubble_item);
        }
        mBubbleItemView.updateTargetWidth(mDisplayWidth);
        initBubbleViewListener(mBubbleItemView);
    }

    public void initBubbleViewLeft() {
        if (mBubbleLeftStub != null && mBubbleLeftStub.getParent() != null) {
            mBubbleItemViewLeft = (BubbleItemView) mBubbleLeftStub.inflate();
        } else {
            mBubbleItemViewLeft = (BubbleItemView) findViewById(R.id.bubble_item_left);
        }
        mBubbleItemViewLeft.updateTargetWidth(mDisplayWidth);
        initBubbleViewListener(mBubbleItemViewLeft);
    }

    private void initBubbleViewListener(final BubbleItemView bubbleItemView) {
        bubbleItemView.setViewListener(this);
        bubbleItemView.setBulletViewChangeListener(mSearchView);
        bubbleItemView.setShowViewChangeListener(mSearchView);
        bubbleItemView.setSoftListener(this);

        bubbleItemView.setBubbleClickListener(new BubbleItemView.OnBubbleClickListener() {
            public void onAddAttachmentClick() {
                SaraUtils.startAttachementChoose(BubbleActivity.this);
            }

            public void onAttachmentChanged() {
                bubbleItemView.refreshAttachmentView();
                setBubbleState(BubbleSate.LARGE, false, false);
            }

            public void onImageAttchmentClick(GlobalBubbleAttach globalBubbleAttach, ArrayList<Uri> localUris) {
                SaraUtils.startImagePreview(BubbleActivity.this, globalBubbleAttach, localUris);
            }

            public void onFileClick(GlobalBubbleAttach globalBubbleAttach) {
                SaraUtils.startFilePreview(BubbleActivity.this, globalBubbleAttach);
            }
        });
    }

    private BubbleItemView getCurrentBubbleView() {
        return getCurrentBubbleView(true);
    }

    private BubbleItemView getCurrentBubbleView(boolean createIfNull) {
        if (SaraUtils.isLeftPopBubble()) {
            if (mBubbleItemViewLeft == null && createIfNull) {
                initBubbleViewLeft();
            }
            return mBubbleItemViewLeft;
        } else {
            if (mBubbleItemView == null && createIfNull) {
                initBubbleView();
            }
            return mBubbleItemView;
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            checkWakeUpAndPreStartVoiceRecognition();
        }
    }

    private boolean isWaitingWakeUpAndStart() {
        return mWakeUpStartStatus != WAKE_UP_START_STATUS_NONE
                && mWakeUpStartStatus != WAKE_UP_START_STATUS_STARTED_NORMAL
                && !mIsKeyOrTouchUp && !isPendingFinishing();
    }

    private boolean isRecordingWithoutInteraction() {
        return isRecognizing() &&
                (mIsKeyOrTouchUp || isPendingFinishing() || mWakeUpStartStatus == WAKE_UP_START_STATUS_NONE);
    }

    private void checkWakeUpAndPreStartVoiceRecognition() {
        mCustomHandler.removeMessages(MSG_DELAY_ENSURE_WAKE_UP);
        // Because BubbleAct may pause, stop by activitySlept before we wakeup screen at onWindowFocused
        if (mWakeUpStartStatus == WAKE_UP_START_STATUS_PENDING) {
            mWakeUpStartStatus = WAKE_UP_START_STATUS_STARTED_ONCE;
            if (Build.VERSION.SDK_INT >= 26) {
                // when >= 26, activity is turn on in manifest
            } else {
                MultiSdkAdapter.wakeUp(mPowerManager, SystemClock.uptimeMillis(), getPackageName() + ":window_focus");
            }
            preStartVoiceRecognition(getIntent().getIntExtra(SaraConstant.LUNCH_KEY, TYPE_UNKNOW), true);
        } else if (mWakeUpStartStatus == WAKE_UP_START_STATUS_STARTED_ONCE) {
            mWakeUpStartStatus = WAKE_UP_START_STATUS_STARTED_NORMAL;
        }
    }

    public void preStartVoiceRecognition(int launchType, boolean createFlag) {
        if (isFinishing()) {
            return;
        }
        if (launchType == TYPE_HOME) {
            SaraTracker.onEvent("A420032", "source", 1);
        } else {
            SaraTracker.onEvent("A420032");
        }
        if (launchType == TYPE_UNKNOW) {
            showNoResultView();
            return;
        }
        if (mBulletVoiceSearchView != null) {
            mBulletVoiceSearchView.abortVoiceSearch(true);
        }
        // 防止在录音过程中熄屏而申请的 wakelock
        mWakeLock.acquire(5 * 60 * 1000);
        if (launchType != TYPE_HEADSET && launchType != TYPE_BLUETOOTH) {
            startRecognize();
        } else {
            screenOnAndCloseStatusBar();
            if (BubbleSpeechPlayer.getInstance(this).isPlaying()) {
                BubbleSpeechPlayer.getInstance(this).stop();
            }
            mAudioManager.requestAudioFocus(null, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT);
            SoundManager.playSound(getApplicationContext(), true);
            if (launchType == TYPE_HEADSET) {
                mCustomHandler.sendEmptyMessageDelayed(MSG_TTS_PLAY, 2000);
            } else if (launchType == TYPE_BLUETOOTH) {
                startVoiceRecognition();
            }
        }
        if (createFlag) {
            mCustomHandler.sendEmptyMessageDelayed(MSG_START_UI, 200);
        } else {
            mCustomHandler.sendEmptyMessage(MSG_START_UI);
        }
        SaraUtils.setVoiceInputMode(this,  SaraConstant.VOICE_INPUT_MODE);

        registerVoiceCallback();
        if (DeviceType.isOneOf(DeviceType.TRIDENT, DeviceType.OCEAN)) {
            CommonUtils.vibrateEffect(this, VibEffectSmt.EFFECT_RECORDING);
        } else if (SaraUtils.isGlobalVibrateOn(this)) {
            VibratorSmt.getInstance().vibrateWithPrivilege(mVibrator, SaraConstant.VIBRATE_TIME);
        }
        if (createFlag) {
            mHeadSetSearchStarted = false;
            mHeadSetTime = SharePrefUtil.getSearchInfoValue(this, SaraConstant.PREF_HEADSET_LAUNCH_TIME, 0);
            mSaveHeadSetTime = mHeadSetTime >= 5 ? false : true;
        }
    }

    public void initDimens() {
        mLeftBubbleWaveMinWidth = getResources().getDimensionPixelSize(R.dimen.bubble_wave_left_min_width);
        mBubbleWavMinWidth = getResources().getDimensionPixelSize(R.dimen.bubble_wave_min_width);
        mResulTopMargin = getResources().getDimensionPixelSize(R.dimen.result_margin_top);
        mStatusBarHeight = getResources().getDimensionPixelSize(com.android.internal.R.dimen.status_bar_height);
    }

    private void updateWaveLayoutVisibility() {
        boolean isLeftBubble = SaraUtils.isLeftPopBubble();
        mWaveLayout.changeUiMode(isLeftBubble);
        if(isLeftBubble) {
            if(mBubbleItemViewLeft != null)mBubbleItemViewLeft.setVisibility(View.VISIBLE);
            if(mBubbleItemView != null)mBubbleItemView.setVisibility(View.GONE);
        } else {
            if(mBubbleItemViewLeft != null)mBubbleItemViewLeft.setVisibility(View.GONE);
            if(mBubbleItemView != null)mBubbleItemView.setVisibility(View.VISIBLE);
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        LogUtils.infoRelease(TAG, "onNewIntent():" + getRecordStatus());
        SaraTracker.onEvent("A420031");
        super.onNewIntent(intent);

        if (isInterceptHandleSearchEvent() && !isHeadsetOrBluetooth(intent)) {
            // 开始录音.
            unregisterVoiceCallback();
            getCurrentBubbleView().hideSoftInputFromWindow();
            mBulletVoiceSearchView.startTouchAction(true);
            return;
        } else if(isShouldStartVoiceSearchView()){
            mIsPendingStartVoice = false;
            pendingOnPause();
        }

        Window window = getWindow();
        WindowManager.LayoutParams params = window.getAttributes();
        if ((params.flags & SHOW_TOP_OF_SCREEN_FLAGS) == 0) {
            getWindow().addFlags(SHOW_TOP_OF_SCREEN_FLAGS);
            window.setAttributes(params);
        }
        SaraUtils.setSearchType(this, SaraUtils.BUBBLE_TYPE.PHONE_BUBBLE.name());
        mPendingFinishing = false;
        if (!SaraUtils.isSettingEnable(this) || !RecognizeHelper.getInstance().isAllowedToRecognize()) {
            finish();
            return;
        }
        setIntent(intent);
        isFromSideBar = getIntent().getBooleanExtra(SaraConstant.ACTION_FROM_SIDEBAR, false);
        SaraTracker.onEvent("A429000");
        mSleepValue = getIntent().getBooleanExtra(SaraConstant.SCREEN_OFF_KEY, false);
        gaussianBlur(false);
        addBubbleAndHideViewImmediatelyIfNeeded();
        isHeadsetOrBluetooth();
        if (mBluetoothHeadset == null) {
            initBlueToothHeadset();
        }
        mHeadSetSearchStarted = false;
        mCustomHandler.removeCallbacksAndMessages(null);
        clearAnimation();
        if(mLastHeadSetState != sIsHeadSetOrBluetooth){
            mLastHeadSetState = sIsHeadSetOrBluetooth;
            mWaveLayout.changeUiMode(SaraUtils.isLeftPopBubble());
        }
        mResumeKeyboard = false;
        mIsKeyOrTouchUp = false;
        mGlobalBubble = null;

        String resultStr = getIntent().getStringExtra("ACTION_FROM_SIDEBAR_CONTENT");
        if (isFromSideBar) {
            result = new StringBuffer();
            resultRecived(resultStr, true);
            return;
        }
        SaraUtils.closeSystemDialog();
        int launchType = getIntent().getIntExtra(SaraConstant.LUNCH_KEY, TYPE_UNKNOW);
        BubbleItemView bubbleItemView = getCurrentBubbleView();
        bubbleItemView.setAttachmentList(null);
        DrawerDataRepository.INSTANCE.reloadAsync();
        if (mSleepValue && !mPowerManager.isScreenOn()) {
            PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
            MultiSdkAdapter.wakeUp(pm, SystemClock.uptimeMillis(), getPackageName() + ":window_focus");
        }
        preStartVoiceRecognition(launchType, false);
        if (isStopped()) {
            mWakeUpStartStatus = WAKE_UP_START_STATUS_STARTED_ONCE;
        } else {
            if (DeviceType.isOneOf(DeviceType.M1, DeviceType.M1L, DeviceType.U1, DeviceType.T2)
                    && SaraUtils.isKeyguardLocked()) {
                mWakeUpStartStatus = WAKE_UP_START_STATUS_STARTED_ONCE;
            } else {
                mWakeUpStartStatus = WAKE_UP_START_STATUS_NONE;
            }
        }
        bubbleItemView.setVisibility(View.GONE);
        bubbleItemView.setDefaultShowBulletFlag(getDefaultSettingShowBullet());
    }

    @Override
    public void onResume() {
        super.onResume();
        LogUtils.d(TAG,"onResume()");
        mCustomHandler.removeMessages(MSG_FINISH_ACTIVITY_ANIM);
        mCustomHandler.removeMessages(MSG_ADD_BUBBLE_AND_HIDE_VIEW);
        SaraUtils.setSearchType(this, SaraUtils.BUBBLE_TYPE.PHONE_BUBBLE.name());
        BubbleItemView bubbleItemView = getCurrentBubbleView();
        bubbleItemView.onResume();
        if (mResumeKeyboard) {
            mResumeKeyboard = false;
            bubbleItemView.showSoftInputFromWindow();
        }
    }

    @Override
    public void onEnterAnimationComplete() {
        final Runnable runnable = new Runnable() {
            @Override public void run() {
                if (!mConvertTranslucent) {
                    convertFromTranslucent();
                    mConvertTranslucent = true;
                }
            }
        };
        mCustomHandler.postDelayed(runnable, isAttachedToWindow() ? 0 : 400);
    }

    @Override
    public void onPause() {
        super.onPause();

        //拦截事件
        if (isShouldStartVoiceSearchView() && isLaunchFromOnNewIntent()) {
            mIsPendingStartVoice = true;
            return;
        } else {
            mIsPendingStartVoice = false;
        }
        pendingOnPause();
    }

    private void pendingOnPause() {
        LogUtils.d(TAG,"enter onPause():" + getRecordStatus());

        BubbleItemView bubbleItemView = getCurrentBubbleView();
        if (BubbleManager.isAddBubble2List() && mGlobalBubble != null) {
            boolean screenOn = mPowerManager.isScreenOn();
            if (!screenOn || SaraUtils.isTopSelfApp(this)) {
                mCustomHandler.sendEmptyMessageDelayed(MSG_ADD_BUBBLE_AND_HIDE_VIEW, 1000);
            }
        }
        if (isRecordingWithoutInteraction()) {
            stopRecognize(true);
        }
        if (bubbleItemView != null){
            bubbleItemView.checkInput(false);
            if (bubbleItemView.isKeyBoardVisible()) {
                mResumeKeyboard = true;
                bubbleItemView.hideSoftInputFromWindow();
            }
            bubbleItemView.dismissShareDialogIfNeed();
        }

        LogUtils.d(TAG,"result = " + result);

        mBubbleStatus = BUBBLE_STATUS_NORMAL;
        releaseWakeLock();
        if (null != mBulletVoiceSearchView) {
            mBulletVoiceSearchView.onPause();
        }
    }



    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mGlobalBubble != null) {
            BubbleItemView bubbleItemView = getCurrentBubbleView();
            outState.putBoolean("showBullet", bubbleItemView.isCurrentBulletShow());
            outState.putParcelable("bubble", mGlobalBubble);
            if (bubbleItemView.getAttachmentList() != null) {
                ArrayList<GlobalBubbleAttach> attaches = new ArrayList<GlobalBubbleAttach>();
                attaches.addAll(bubbleItemView.getAttachmentList());
                outState.putParcelableArrayList("attach", attaches);
            }
        }
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
            isUiChanging = isUiChanging || mCustomHandler.hasMessages(MSG_REFRESH_SEARCH_VIEW);
            boolean isScreenOn = mPowerManager.isScreenOn();
            boolean isPendingAddBubbleAndHideView = mCustomHandler.hasMessages(MSG_ADD_BUBBLE_AND_HIDE_VIEW);
            mCustomHandler.removeCallbacksAndMessages(null);
            if (isPendingAddBubbleAndHideView) {
                Message msg = mCustomHandler.obtainMessage(MSG_ADD_BUBBLE_AND_HIDE_VIEW);
                msg.getData().putBoolean("finish", true);
                mCustomHandler.sendMessageDelayed(msg, 200);
            }
            if (!isScreenOn || isFinishPosted) {
                LogUtils.d(TAG, "finish ourself due to screen off!");
                mCustomHandler.sendEmptyMessageDelayed(MSG_FINISH_ACTIVITY_ANIM, 100);
            } else if (!isPendingFinishing()) {
                if (isUiChanging || getRecordStatus() != RECORD_STATUS_RESULT_OK_STOPPED) {
                    unregisterVoiceCallback();
                    showInterruptView();
                }
            }
        } else {
            checkWakeUpAndPreStartVoiceRecognition();
            mCustomHandler.sendEmptyMessageDelayed(MSG_FINISH_ACTIVITY_ANIM, 1000);
        }
        if (Build.VERSION.SDK_INT >= 26) {
            ActivityUtil.setTurnScreenOn(this, false);
        }
        isFromSideBar = false;

        BubbleActionUpHelper.INSTANCE.removeActionUpListener(mActionUpListener);
        SaraUtils.setBluetoothValue(this, 0);
    }

    @Override
    protected void onStart() {
        super.onStart();
        LogUtils.infoRelease(TAG, "onStart()");
        mCustomHandler.removeMessages(MSG_ADD_BUBBLE_AND_HIDE_VIEW);
        BubbleActionUpHelper.INSTANCE.addActionUpListener(mActionUpListener);
    }

    private void finishInner(boolean clearAnim) {
        finish();
        if (clearAnim) {
            overridePendingTransition(0, 0);
        }
    }

    @Override
    public void finish() {
        if (isFinishing()) {
            return;
        }
        super.finish();
        LogUtils.d(TAG,"enter finish()");
        SaraUtils.overridePendingTransition(this, true);
    }
    @Override
    public void onDestroy() {
        closeRemind();
        LogUtils.d(TAG,"onDestroy()");
        addBubbleAndHideViewImmediatelyIfNeeded();
        mCustomHandler.removeCallbacksAndMessages(null);
        BubbleActivityHelper.INSTANCE.getRootViewTag().detach();
        if (mMediaPlayer != null) {
            if(mMediaPlayer.isPlaying()){
                mMediaPlayer.stop();
            }
            mMediaPlayer.release();
        }
        if (mBluetoothHeadset != null) {
            List<BluetoothDevice> devices = mBluetoothHeadset.getConnectedDevices();
            if (devices != null && devices.size() > 0) {
                if (mBluetoothHeadset.isAudioConnected(devices.get(0))){
                    mBluetoothHeadset.stopVoiceRecognition(devices.get(0));
                }
            }
            BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
            adapter.closeProfileProxy( BluetoothProfile.HEADSET,mBluetoothHeadset);
            mBluetoothHeadset = null;
        }
        try {
            // Activity may finish without register any receiver or bind any service
            // Just ignore the exception.
            unregisterReceiver(mReceiver);
        } catch (Exception e) {
        }
        if (null != mBulletVoiceSearchView) {
            mBulletVoiceSearchView.onDestroy();
        }

        ArrayList<GlobalBubbleAttach> clearAttaches = new ArrayList<GlobalBubbleAttach>();
        if (mBubbleItemView != null && mBubbleItemView.getAttachmentList() != null) {
            clearAttaches.addAll(mBubbleItemView.getAttachmentList());
        }
        BubbleCleaner.INSTANCE.clearBubbleFilesWhenDestroy(getApplicationContext(), mGlobalBubble, clearAttaches);
        if (null != mFlashImConnection) {
            mFlashImConnection.unBindService();
        }
        super.onDestroy();
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (ev.getAction() == MotionEvent.ACTION_DOWN) {
            BubbleItemView bubbleItemView = getCurrentBubbleView();
            if (SaraUtils.checkFinish(bubbleItemView, ev) && mWaveLayout.checkFinish(ev)
                    && mSearchView.checkSearchViewFinish(ev)) {
                PointF pointF = null;
                if (mGlobalBubble != null && BubbleManager.isAddBubble2List()
                        && !TextUtils.isEmpty(bubbleItemView.getEditTextString())) {
                    pointF = BubbleManager.addBubble2SideBar(this, mGlobalBubble, bubbleItemView.getAttachmentList(), mIsOffLine, false);
                }
                if (pointF != null) {
                    hideView(0, pointF, true, true);
                } else {
                    if (!isRecognizing() && !bubbleItemView.isKeyboardViewShow() ||
                            (mNoResultView != null && mNoResultView.getVisibility() == View.VISIBLE)) {
                        finish();
                    }
                }
            }
        }
        try {
            return super.dispatchTouchEvent(ev);
        } catch (Exception e) {
            return false;
        }
    }

    public void formatBubble(boolean offline) {
        SaraUtils.recordFile(this, offline,  result.toString());
        int type;
        if (isFromSideBar) {
            type = GlobalBubble.TYPE_TEXT;
        } else {
            type = offline ? GlobalBubble.TYPE_VOICE_OFFLINE : GlobalBubble.TYPE_VOICE;
        }
        mGlobalBubble = SaraUtils.toGlobalBubble(this, NStringUtils.addSpaceIfENConnectedCN(result.toString()), type,
                isFromSideBar ? null : SaraUtils.getUri(this), SaraUtils.getDefaultBubbleColor(this), 0, 0);
        BubbleManager.markAddBubble2List(true);
        BubbleCleaner.INSTANCE.addPendingClearBubble(mGlobalBubble);
    }

    public void startUI() {
        LogUtils.d(TAG,"enter startUI()");
        result = new StringBuffer();
        setParamWidth(SaraUtils.isLeftPopBubble() ? mLeftBubbleWaveMinWidth : mBubbleWavMinWidth);
        resetView();
        mWaveLayout.show(SaraUtils.getDefaultBubbleColor(this));
        mCustomHandler.sendEmptyMessage(MSG_HIDE_GLOBLE_BUBBLE);
    }

    @Override
    protected void realStartRecognize() {
        LogUtils.d(TAG,"startRecognize ---> mIsErrorHaveShow = " + false);
        super.realStartRecognize();
        result = new StringBuffer();
        mIsOffLine = false;
        setErrorState(false);
    }

    @Override
    public void onServiceConnected() {
        super.onServiceConnected();
        if (mShouldRestoreState) {
            mShouldRestoreState = false;
            restoreSearchState(mLastShowBullet);
        }
    }

    private void restoreSearchState(boolean showBullet) {
        getCurrentBubbleView().setDefaultShowBulletFlag(showBullet);
        String bubbleText = mGlobalBubble.getText();
        List<GlobalBubbleAttach> listAttachs = getCurrentBubbleView().getAttachmentList();
        if (bubbleText.length() <= SaraConstant.INTERVAL_SHORT && (listAttachs == null || listAttachs.size() == 0)) {
            mRecordStatus = RECORD_STATUS_RESULT_OK;
            result.append(bubbleText);
            loadLocalData(bubbleText, getPackageName());
            mSearchView.ensureInitialized();
            mSearchView.loadSearchResult(null, bubbleText);
        } else {
            loadResult(false, mGlobalBubble.getText(), true, false);
        }
    }

    public void loadResult(boolean update,String tmp, boolean isSmallEdit) {
        loadResult(update, tmp, isSmallEdit, false);
    }

    public void loadResult(final boolean update, final String tmp, final boolean isSmallEdit, final boolean needScaleAnim) {
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                if (tmp.length() <= SaraConstant.INTERVAL_SHORT) {
                    List<GlobalBubbleAttach> listAttachs = getCurrentBubbleView().getAttachmentList();
                    if (listAttachs != null && listAttachs.size() > 0) {
                        refreshSearchView(true, false, false, update, false, needScaleAnim);
                    } else {
                        mSearchView.loadSearchResult(null, tmp);

                        // loadSearchResult依赖WebView创建,耗时.延迟一下动画防止卡顿
                        if (mLocalResultCall) {
                            postRefreshSearchView(true, true, update,
                                    isSmallEdit, needScaleAnim, 200);
                        } else {
                            postRefreshSearchView(false, true, update,
                                    isSmallEdit, needScaleAnim, 450);
                        }
                    }
                } else if (tmp.length() > SaraConstant.INTERVAL_SHORT
                        && tmp.length() < SaraConstant.INTERVAL_LONG) {
                    mSearchView.loadSearchResult(null, tmp);

                    // loadSearchResult依赖WebView创建,耗时.延迟一下动画防止卡顿
                    postRefreshSearchView(false, true, update,
                            isSmallEdit, needScaleAnim, 200);
                } else {
                    refreshSearchView(true, false, false, update, isSmallEdit, needScaleAnim);
                }
            }
        };
        if (mSearchView.ensureInitialized()) {
            mCustomHandler.post(runnable);
        } else {
            runnable.run();
        }
    }

    public void loadShareResult(boolean update,String resultStr, boolean isSmallEdit) {
        LogUtils.d(TAG, "loadShareResult() resultStr = " + resultStr);
        if (resultStr.length() < SaraConstant.INTERVAL_LONG) {
            mSearchView.loadSearchResult(null, resultStr);
            refreshSearchView(true, false, true, update, isSmallEdit);
        } else {
            refreshSearchView(true, false, false, update, isSmallEdit);
        }
    }

    private void resultFromIntentUpdate(Intent intent) {
        StringBuffer newBubbleText = BubbleManager.updateBubbleFromIntent(intent, mGlobalBubble);
        if (newBubbleText != null) {
            result = newBubbleText;
            loadResult(true, result.toString(), false);
        }
    }

    private void updateBubble2Share(){
        if (mGlobalBubble != null) {
            getCurrentBubbleView().changeColor2Share();
        }
    }

    private void postRefreshSearchView(boolean hasLocal, boolean hasWeb, boolean update,
                                       boolean isSmallEdit, boolean needScaleAnim, long delay) {
        mCustomHandler.removeMessages(MSG_REFRESH_SEARCH_VIEW);
        Message msg = mCustomHandler.obtainMessage();
        msg.what = MSG_REFRESH_SEARCH_VIEW;
        Bundle data = new Bundle();
        data.putBoolean("hasBubble", true);
        data.putBoolean("hasLocal", hasLocal);
        data.putBoolean("hasWeb", hasWeb);
        data.putBoolean("update", update);
        data.putBoolean("isSmallEdit", isSmallEdit);
        data.putBoolean("needScaleAnim", needScaleAnim);
        msg.setData(data);
        mCustomHandler.sendMessageDelayed(msg, delay);
    }

    private void refreshSearchView(boolean hasBubble, boolean hasLocal, boolean hasWeb,boolean update, boolean isSmallEdit) {
        refreshSearchView(hasBubble, hasLocal, hasWeb, update, isSmallEdit, false);
    }

    private void refreshSearchView(boolean hasBubble, boolean hasLocal, boolean hasWeb,boolean update, boolean isSmallEdit, boolean needScaleAnim) {
        BubbleItemView bubbleItemView = getCurrentBubbleView();
        if (hasBubble && mGlobalBubble != null) {
            bubbleItemView.setGlobalBubble(mGlobalBubble, mIsOffLine);
            mExistBubbleView = true;
        } else {
            mExistBubbleView = false;
        }

        mExistLocalView = mSearchView.isExistLocalView(hasLocal, mExistLocalView);
        mExistWebView = mSearchView.isExistWebView(hasWeb, mExistWebView);

        List<GlobalBubbleAttach> attachList = bubbleItemView.getAttachmentList();
        if (attachList != null && attachList.size() > 0) {
            mExistLocalView = false;
            mExistWebView = false;
        }

        int resultHeight = 0;
        if (mExistBubbleView && mExistLocalView && mExistWebView) {
            resultHeight = updateBubbleItemState(BubbleSate.SMALL, isSmallEdit, needScaleAnim);
        } else if (mExistBubbleView && mExistLocalView) {
            resultHeight = updateBubbleItemState(BubbleSate.SMALL, isSmallEdit, needScaleAnim);
        } else if (mExistBubbleView && mExistWebView) {
            resultHeight = updateBubbleItemState(BubbleSate.SMALL, isSmallEdit, needScaleAnim);
        } else if (mExistBubbleView) {
            resultHeight = updateBubbleItemState(BubbleSate.LARGE, isSmallEdit, needScaleAnim);
        } else {
            bubbleItemView.setVisibility(View.GONE);
        }
        mSearchView.refreshView(mExistLocalView, mExistWebView, resultHeight);

        if (!update) {
            cardAnimation(isSmallEdit, needScaleAnim);
        }

        boolean hasResult = mExistLocalView || mExistWebView;
        bubbleItemView.setEditable(true);
        LogUtils.df("refreshSearchView hasResult:"+hasResult);
        trackResultEvent(hasResult);

        //如果设定默认显示子弹短信，则手动切换到子弹短信页面
        bubbleItemView.consumeDefaultShowBulletFlag();
    }

    private void trackResultEvent(boolean hasResult) {
        LinkedHashMap<String, Object> trackerData = new LinkedHashMap<String, Object>();
        trackerData.put("result", hasResult ? 1 : 0);
        SaraTracker.onEvent("A429001", trackerData);
    }

    private int updateBubbleItemState(BubbleSate state, boolean isSmallEdit, boolean needScaleAnim) {
        BubbleItemView bubbleItemView = getCurrentBubbleView();
        bubbleItemView.setVisibility(View.VISIBLE);
        bubbleItemView.setBubbleState(state, false, isSmallEdit, false, needScaleAnim);
        return bubbleItemView.getBubbleItemHeight();
    }

    public void cardAnimation(boolean isSmallEdit, boolean needScaleAnim) {
        if (mExistBubbleView && mExistLocalView && mExistWebView) {
            setBubbleState(BubbleSate.SMALL, isSmallEdit, true, needScaleAnim);
            mSearchView.showSearchViewithAnimation(mExistLocalView, mExistWebView);
        } else if (mExistBubbleView && mExistLocalView) {
            setBubbleState(BubbleSate.SMALL, isSmallEdit, true, needScaleAnim);
            mSearchView.showSearchViewithAnimation(mExistLocalView, false);
        } else if (mExistBubbleView && mExistWebView) {
            setBubbleState(BubbleSate.SMALL, isSmallEdit, true, needScaleAnim);
            mSearchView.showSearchViewithAnimation(false, mExistWebView);
        } else if (mExistBubbleView) {
            setBubbleState(BubbleSate.LARGE, isSmallEdit, true, needScaleAnim);
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
        LogUtils.d(TAG, "postShowNoResultView()");
        mCustomHandler.removeMessages(MSG_NO_RESULT_TIP);
        Message message = mCustomHandler.obtainMessage(MSG_NO_RESULT_TIP);
        message.arg1 = errorCode;
        mCustomHandler.sendMessage(message);
    }

    private void showInterruptView() {
        noResultHandled();
        showNoResultView(SpeechError.ERROR_INTERRUPT);
        mSearchView.resetSearchViewAndData(false, true);
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
        if (mNoResultView == null) {
            if (mNoResultStub != null && mNoResultStub.getParent() != null) {
                mNoResultView = mNoResultStub.inflate();
            } else {
                mNoResultView = findViewById(R.id.no_result);
            }
        }
        if (mAudioManager != null) {
            mAudioManager.abandonAudioFocus(null);
        }
        LogUtils.d(TAG, "bubbleItemView GONE hideTextPopup ");
        getCurrentBubbleView().setVisibility(View.GONE);
        if (mSearchView != null) {
            mSearchView.hideResultView();
            hideShowBulletVoiceSearch(false);
        }
        SmartisanBlankView noResultView = (SmartisanBlankView) mNoResultView.findViewById(R.id.no_result_view);
        noResultView.getImageView().setVisibility(View.GONE);
        noResultView.getSecondaryHintView().setGravity(Gravity.LEFT);
        int key = getIntent().getIntExtra(SaraConstant.LUNCH_KEY, TYPE_UNKNOW);
        if (key == TYPE_BLUETOOTH) {
            noResultView.getPrimaryHintView().setText(R.string.touch_speak_bluetooth);
        } else {
            noResultView.getPrimaryHintView().setText(R.string.touch_speak_home);
        }

        if (key == TYPE_HOME || key == TYPE_UNKNOW) {
            if (SaraUtils.isLeftPopBubble()){
                noResultView.getSecondaryHintView().setText(R.string.touch_speak_leftside_tip);
            } else {
                noResultView.getSecondaryHintView().setText(R.string.touch_speak_home_tip);
            }
        } else if (key == TYPE_MENU) {
            noResultView.getSecondaryHintView().setText(R.string.touch_speak_menu_tip);
        } else if (key == TYPE_DOUBLE_SIDE) {
            noResultView.getSecondaryHintView().setText(R.string.touch_speak_doubleside_tip);
        } else if (key == TYPE_HEADSET) {
            noResultView.getSecondaryHintView().setText(R.string.touch_speak_headset_tip);
        } else if (key == TYPE_BRIGHTNESS) {
            noResultView.getSecondaryHintView().setText(R.string.touch_speak_singleside_tip);
        }  else if (key == TYPE_BLUETOOTH) {
            noResultView.getSecondaryHintView().setText(R.string.touch_speak_bluetooth_tip);
        }

        switch (errorCode) {
            case SpeechError.ERROR_INTERRUPT:
            case SpeechError.ERROR_FACADE_BUSY:
                noResultView.getSecondaryHintView().setText(R.string.voice_search_interrupt);
                break;
        }
        mNoResultView.setVisibility(View.VISIBLE);
        mWaveLayout.hide();
        trackResultEvent(false);
        releaseWakeLock();
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
        releaseWakeLock();
    }

    public boolean clearAnimation() {
        boolean isClearAnimation = false;
        BubbleItemView bubbleItemView = getCurrentBubbleView(false);
        if (bubbleItemView != null) {
            if (bubbleItemView.getAnimation() != null && !bubbleItemView.getAnimation().hasEnded()) {
                isClearAnimation = true;
            }
            bubbleItemView.clearAnimation();
            if (bubbleItemView.isAnimSetRunning()) {
                isClearAnimation = true;
            }
            bubbleItemView.cancelAnimSet();
        }

        isClearAnimation = mSearchView.clearSearchViewAnimation(isClearAnimation);
        isClearAnimation |= mWaveLayout.clearAnim();
        return isClearAnimation;
    }

    private void resetView() {
        mExistBubbleView = false;
        mExistLocalView = false;
        mExistWebView = false;

        mSearchView.resetSearchViewAndData(false, true);
        updateWaveLayoutVisibility();
        int color = SaraUtils.getDefaultBubbleColor(this);
        BubbleItemView bubbleItemView = getCurrentBubbleView();
        LogUtils.d(TAG, "bubbleItemView GONE resetView ");
        bubbleItemView.setVisibility(View.GONE);
        bubbleItemView.checkInput(false);
        bubbleItemView.hideSoftInputFromWindow();
        bubbleItemView.setBackgroundResource(BubbleThemeManager.getBackgroudRes(color, BubbleThemeManager.BACKGROUND_BUBBLE_NORMAL));
        if (mNoResultView != null) {
            mNoResultView.setVisibility(View.GONE);
        }
        bubbleItemView.setBubbleState(BubbleSate.INIT, true, false, false);
        bubbleItemView.setEditable(false);
        resetBubbleScaleAndPositionIfNeeded(bubbleItemView);
        mIsMaxWave = false;
        if (null != mBulletVoiceSearchView) {
            mBulletVoiceSearchView.hide(false);
        }
    }

    @Override
    public void onAnimationEnd(int width, boolean isCanceled) {
        if (isStopped()){
            return;
        }
        mIsMaxWave = true;
        LogUtils.d(TAG, "mIsMaxWave is true");
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
        FrameLayout.LayoutParams bubbleParams = (FrameLayout.LayoutParams) getCurrentBubbleView().getLayoutParams();
        if (bubbleParams.width != width) {
            if(SaraUtils.isLeftPopBubble()){
                bubbleParams.width = width + getResources().getDimensionPixelOffset(R.dimen.nomal_bubble_left_margin);
            }else {
                bubbleParams.width = width + getResources().getDimensionPixelOffset(R.dimen.bubble_loading_width_calibration);
            }
            getCurrentBubbleView().setLayoutParams(bubbleParams);
        }
    }

    public void setBubbleState(BubbleSate state, boolean isSmallEdit, boolean isAnim) {
        setBubbleState(state, isSmallEdit, isAnim, false);
    }

    public void setBubbleState(BubbleSate state, boolean isSmallEdit, boolean isAnim, boolean needScaleAnim) {
        LogUtils.d(TAG, "setBubbleState state:" + state);
        BubbleItemView bubbleItemView = getCurrentBubbleView();
        if (isAnim) {
            bubbleItemView.setBubbleState(state, true, isSmallEdit, true, needScaleAnim);
        } else {
            FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) bubbleItemView.getLayoutParams();
            params.height = (int) bubbleItemView.getExactHeight();
            bubbleItemView.setLayoutParams(params);
        }
        mWaveLayout.hide();
    }

    public void playPopup2TextAnim() {
        if (mIsMaxWave) {
            mWaveLayout.waveMax(SaraUtils.getDefaultBubbleColor(this));
        }
        BubbleItemView bubbleItemView = getCurrentBubbleView();
        if (!TextUtils.isEmpty(result)) {
            mWaveLayout.hideTextPopup(false);
            bubbleItemView.setText(NStringUtils.addSpaceIfENConnectedCN(result.toString()));
            bubbleItemView.setBubbleState(BubbleSate.NORMAL, true, false, false );
        } else {
            if (mIsMaxWave) {
                mWaveLayout.hideTextPopup(true);
                bubbleItemView.setBubbleState(BubbleSate.LOADING, true, false, false);
            }
        }
    }

    @Override
    public void hideView(int from, PointF point, final boolean finish, final boolean needSleep) {
        mWaveLayout.hide();
        int[] positionBubble = new int[2];
        final BubbleItemView bubbleItemView = getCurrentBubbleView();
        bubbleItemView.getLocationOnScreen(positionBubble);
        final int bubbleItemTranslateY =(int)bubbleItemView.getTranslationY() - 500;
        if (from == 0) {
            LogUtils.d(TAG, "hideView mBubbleStatus = " + mBubbleStatus);
            if (mBubbleStatus == BUBBLE_STATUS_HIDDING) {
                return;
            }
            mBubbleStatus = BUBBLE_STATUS_HIDDING;
            if (finish) {
                mPendingFinishing = true;
            }
            if (point != null){
                List<GlobalBubbleAttach> globalBubbleAttaches = bubbleItemView.getAttachmentList();
                mFakeBubbleView.initFakeAnim(mGlobalBubble, globalBubbleAttaches, mDisplayWidth);
                int[] position = new int[2];
                bubbleItemView.getLocationOnScreen(position);
                int targetY = mFakeBubbleView.getFakeAnimTargetY(bubbleItemView.getLayoutParams().height);
                int targetX = mFakeBubbleView.getFakeAnimTargetX();
                int bluetoothTargetY = mDisplayHeight - mWaveLayout.getWaveLayoutHeight();
                int translateX = targetX - position[0];

                bubbleItemView.hideViewWithScaleAnim(point, mFakeBubbleView.getFakeBubbleBgWidth(),
                        mFakeBubbleView.getFakeBubbleBgHeight(), translateX, targetY + position[1]);
                final boolean isForceSleep = mSearchView != null && mSearchView.isCurrentBulletShow();
                mFakeBubbleView.startFakeAnim(isBluetooth() ? bluetoothTargetY : targetY + position[1],
                        new AnimatorListenerAdapter() {
                            @Override
                            public void onAnimationEnd(Animator animation) {
                                finishAndGotoSleep(finish, false, needSleep, isForceSleep);
                            }
                        });
            } else {
                AnimManager.hideViewWithAlphaAndTranslate(bubbleItemView, 0, 250, bubbleItemTranslateY);
            }

            boolean handle = mSearchView.hideViewFromAction(bubbleItemTranslateY, getCurrentBubbleView().getHeight(),
                    bubbleItemView.getVisibility() == View.VISIBLE
                    , from, finish, new Runnable() {
                        @Override
                        public void run() {
                            bubbleItemView.clearSwitchAnimation();
                        }
                    });
            Message message = mCustomHandler.obtainMessage(MSG_FINISH_ACTIVITY_ANIM);

            if (handle && point == null) {
                mCustomHandler.sendMessageDelayed(message, 250);
            }

        } else {
            mSearchView.hideViewFromAction(bubbleItemTranslateY, getCurrentBubbleView().getHeight(),
                    bubbleItemView.getVisibility() == View.VISIBLE
                    , from, finish, new Runnable() {
                        @Override
                        public void run() {
                            AnimManager.hideViewWithAlphaAndTranslate(bubbleItemView, 0, 200, bubbleItemTranslateY);
                        }
                    });
        }
    }

    public void finishAndGotoSleep(boolean finish, boolean clearAnim, boolean needSleep,boolean force) {
        SaraUtils.goToSleepIfNoKeyguard(BubbleActivity.this, mSleepValue && needSleep,force);
        if (finish) {
            Message message = mCustomHandler.obtainMessage(MSG_FINISH_ACTIVITY_ANIM);
            message.getData().putBoolean("clearAnim", clearAnim);
            mCustomHandler.sendMessage(message);
        }
    }

    @Override
    public void deleteVoice(GlobalBubble globalBubble) {
        Bundle bunble = new Bundle();
        bunble.putIntArray(SaraConstant.KEY_BUBBLE_IDS, new int[] {globalBubble.getId()});
        bunble.putString(SaraConstant.KEY_DESTROY_TYPE, SaraConstant.DESTROY_TYPE_REMOVED);
        BubbleDataRepository.destroyGlobleBubble(this, bunble);
    }

    @Override
    public void editView(final boolean keyboardVisible, final boolean isSmallEdit) {
        final BubbleItemView bubbleItemView = getCurrentBubbleView();
        if (bubbleItemView.getBubbleState() == BubbleSate.INIT) {
            if (keyboardVisible) {
                bubbleItemView.setBubbleState(BubbleSate.KEYBOARD, true, false, false);
            }
        } else {
            final int targetTranslate;
            final int bubbleTargetHeight = bubbleItemView.getExactHeight();
            if (keyboardVisible) {
                targetTranslate = -bubbleItemView.getKeyboardHeight() + mResulTopMargin;
            } else {
                if (bubbleItemView.getBubbleState() == BubbleSate.SMALL) {
                    targetTranslate = bubbleTargetHeight - mRealScreenHeight + mResulTopMargin;
                } else if(bubbleItemView.getBubbleState() == BubbleSate.LARGE){
                    targetTranslate = (bubbleTargetHeight - mRealScreenHeight)/2 - mStatusBarHeight;
                } else {
                    targetTranslate = 0;
                }
            }
            Runnable runnable = new Runnable() {
                @Override
                public void run() {
                    mSearchView.hideSearchViewWithAnimation();

                    AnimManager.showViewWithTranslate(bubbleItemView, 150, 250, targetTranslate, null, bubbleTargetHeight);
                    if (isSmallEdit){
                        bubbleItemView.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                bubbleItemView.setGravityLeft();
                            }
                        },250);
                    }
                }
            };
            if (mSearchView.ensureInitialized()) {
                mCustomHandler.post(runnable);
            } else {
                runnable.run();
            }
        }
    }

    @Override
    public Activity getActivityContext() {
        return this;
    }

    @Override
    public int getBubbleSmallTranslation(int bubbleHeight) {
        return bubbleHeight - mRealScreenHeight + mResulTopMargin;
    }

    @Override
    public int getBubbleLargeTranslation(int bubbleHeight) {
        return (bubbleHeight - mRealScreenHeight) / 2 - mStatusBarHeight;
    }

    @Override
    public int getBubbleKeyboardTranslation(int keyboardHeight) {
        return -keyboardHeight + mStatusBarHeight;
    }

    @Override
    public void loadResultForKeyboard(GlobalBubble bubble, boolean isSmallEdit, boolean needScaleAnim) {
        String result = bubble.getText();
        List<GlobalBubbleAttach> attachmentList = getCurrentBubbleView().getAttachmentList();
        if ((result != null && !TextUtils.isEmpty(result.trim())) || (attachmentList != null && attachmentList.size() > 0)) {
            mGlobalBubble = bubble;
            loadResult(false, bubble.getText(), isSmallEdit, needScaleAnim);
        } else {
            showNoResultView();
            mSearchView.resetSearchViewAndData(false, true);
        }
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        final int keyCode = event.getKeyCode();
        if (keyCode == SaraConstant.KEYCODE_SMART && event.getAction() == KeyEvent.ACTION_UP) {
            long duration = event.getEventTime() - event.getDownTime();
            if (duration < SaraConstant.TIME_LONG_PRESS_KEYCODE_SMART) {
                // keycode for click
                if (!saveBubbleAndResetScreen(true)) {
                    finish();
                }
                return true;
            }
        }
        return super.dispatchKeyEvent(event);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        LogUtils.d(TAG, "onKeyUp keycode = " + keyCode);
        if (!isHeadsetOrBluetooth()) {
            return super.onKeyUp(keyCode, event);
        }
        if (keyCode == KeyEvent.KEYCODE_HEADSETHOOK || keyCode == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE) {
            if(!mHeadSetSearchStarted) {
                mHeadSetSearchStarted = true;
                startRecognize();
                if (mCustomHandler.hasMessages(MSG_TTS_PLAY)){
                    mCustomHandler.removeMessages(MSG_TTS_PLAY);
                }
                if (mMediaPlayer != null && mMediaPlayer.isPlaying()) {
                    stopTTS();
                }
            } else {
                endKeyOrBlueRec();
            }
            return true;
        }
        return super.onKeyUp(keyCode, event);
    }

    private boolean isInterceptHandleSearchEvent() {
        return mIsPendingStartVoice && isShouldStartVoiceSearchView();
    }

    private void screenOnAndCloseStatusBar() {
        StatusBarManager sbm = (StatusBarManager) getSystemService(Context.STATUS_BAR_SERVICE);
        sbm.collapsePanels();
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        if (!pm.isScreenOn()) {
            MultiSdkAdapter.wakeUp(pm, SystemClock.uptimeMillis(), getPackageName() + ":headset");
        }
    }

    private void playTTS() {
        if (mMediaPlayer == null) {
            mMediaPlayer = new MediaPlayer();
            mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            mMediaPlayer.setVolume(1.0f, 1.0f);
        }
        try {
            mMediaPlayer.reset();
            AssetFileDescriptor fileDescriptor;
            if ( Locale.TAIWAN.equals(Locale.getDefault()) || Locale.CHINA.equals(Locale.getDefault())) {
                fileDescriptor = getAssets().openFd("chinese.wav");
            } else {
                fileDescriptor = getAssets().openFd("english.wav");
            }
            mMediaPlayer.setDataSource(fileDescriptor.getFileDescriptor(), fileDescriptor.getStartOffset(),
                    fileDescriptor.getLength());
            mMediaPlayer.prepare();
            mAudioManager.requestAudioFocus(null, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT);
            mMediaPlayer.start();
            if(fileDescriptor != null) {
                fileDescriptor.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void stopTTS() {
        if (mMediaPlayer == null) {
            return;
        }
        mMediaPlayer.stop();
        mAudioManager.abandonAudioFocus(null);
    }

    private void checkTimeOutWarning(){
        if (!mIsKeyOrTouchUp){
            if (isHeadset()){
                if (mCustomHandler.hasMessages(MSG_TTS_PLAY)){
                    mCustomHandler.removeMessages(MSG_TTS_PLAY);
                    mCustomHandler.sendEmptyMessage(MSG_TTS_PLAY);
                }
            } else {
                if (hasWindowFocus()) {
                    if (SaraUtils.isGlobalVibrateOn(this)) {
                        VibratorSmt.getInstance().vibrateWithPrivilege(mVibrator, SaraConstant.VIBRATE_TIME);
                    }
                }
            }
        }
    }
    @Override
    public void onBackPressed() {
        if (!saveBubbleAndResetScreen(false)) {
            super.onBackPressed();
        }
    }

    private boolean saveBubbleAndResetScreen(boolean isSmartKey) {
        BubbleItemView bubbleItemView = getCurrentBubbleView();
        if (bubbleItemView != null) {
            boolean addBubble = true;
            if (isSmartKey) {
                boolean showBullet = getDefaultSettingShowBullet();
                if (showBullet && !bubbleItemView.isCurrentBulletShow()) {
                    addBubble = false;
                }
            }
            if (addBubble && mGlobalBubble != null && BubbleManager.isAddBubble2List()) {
                PointF point = bubbleItemView.addBubble2SideBar(false);
                hideView(0, point, true, true);
                return true;
            }
        }
        return false;
    }

    private boolean isHeadsetConnected() {
        if (mAudioManager != null) {
            return mAudioManager.isWiredHeadsetOn() || mAudioManager.isBluetoothA2dpOn();
        }
        return false;
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data){
        super.onActivityResult(requestCode, resultCode, data);
        LogUtils.d(TAG,"onActivityResult()");
        switch (requestCode) {
            case SaraConstant.RESULT_PICK_ATTACHMENT:
                if (resultCode == RESULT_OK) {
                    handleAttachmentPickResult(data);
                }
                break;
            case SaraConstant.RESULT_IMAGE_PREVIEW:
                if (resultCode == RESULT_OK) {
                    handleImagePreviewResult(data);
                }
                break;
            case SaraConstant.RESULT_FILE_PREVIEW:
                //TODO
                break;
            case SaraConstant.RESULT_PICK_REMIND:
                handleRemindViewResult(data);
                break;
            case SaraConstant.REQUEST_SELECT_FILE:
                mSearchView.onActivityResult(resultCode, data);
                break;
        }
    }

    private void handleImagePreviewResult(Intent data) {
        ClipData cd = data.getClipData();
        if (cd == null) {
            return;
        }
        isHeadsetOrBluetooth();
        BubbleItemView bubbleItemView = getCurrentBubbleView();
        List<GlobalBubbleAttach> listAttach = AttachmentUtils.mergeImageAttachList(
                BubbleActivity.this, bubbleItemView.getAttachmentList(), cd);
        bubbleItemView.setAttachmentList(listAttach);
        bubbleItemView.refreshAttachmentView();
        refreshSearchView(true, false, false, false, false);
    }

    private void handleAttachmentPickResult(Intent data) {
        if (data == null || mGlobalBubble == null) {
            LogUtils.e("handleAttachmentPickResult return by data or bubble is null");
            return;
        }
        isHeadsetOrBluetooth();
        BubbleItemView bubbleItemView = getCurrentBubbleView();
        List<GlobalBubbleAttach> realAttachList =
                AttachmentUtils.mergeAttachmentList(BubbleActivity.this, bubbleItemView.getAttachmentList(), data);
        bubbleItemView.setAttachmentList(realAttachList);
        bubbleItemView.refreshAttachmentView();
        refreshSearchView(true, false, false, false, false);
    }

    private boolean isHeadset() {
        if (getIntent() != null) {
            return TYPE_HEADSET == getIntent().getIntExtra(SaraConstant.LUNCH_KEY, TYPE_UNKNOW);
        } else {
            return false;
        }
    }

    private boolean isHeadsetOrBluetooth() {
        return isHeadsetOrBluetooth(getIntent());
    }

    private boolean isHeadsetOrBluetooth(Intent intent) {
        if (intent != null) {
            int launchType = intent.getIntExtra(SaraConstant.LUNCH_KEY, TYPE_UNKNOW);
            LogUtils.d(TAG, "launchType = " + launchType);
            sIsHeadSetOrBluetooth = (launchType == TYPE_BLUETOOTH || launchType == TYPE_HEADSET);
        } else {
            sIsHeadSetOrBluetooth = false;
        }
        return sIsHeadSetOrBluetooth;
    }

    private boolean isBluetooth() {
        if (getIntent() != null) {
            return TYPE_BLUETOOTH == getIntent().getIntExtra(SaraConstant.LUNCH_KEY, TYPE_UNKNOW);
        }
        return false;
    }

    private void handleRemindViewResult(Intent data) {
        BubbleItemView bubbleItemView = getCurrentBubbleView();
        if (mGlobalBubble != null) {
            long remindTime = data == null ? 0 : data.getLongExtra(SaraConstant.REMIND_TIME_KEY, 0);
            long dueTime = data == null ? 0 : data.getLongExtra(SaraConstant.DUE_DATE_KEY, 0);
            mGlobalBubble.setRemindTime(remindTime);
            mGlobalBubble.setDueDate(dueTime);
            bubbleItemView.setGlobalBubble(mGlobalBubble);
            bubbleItemView.trackBubbleChange(false);
            bubbleItemView.refreshRemindView();
        }
    }

    protected boolean isPendingFinishing() {
        return isFinishing() || mPendingFinishing;
    }

    public boolean getDefaultSettingShowBullet() {
        int isManual = Settings.Global.getInt(getContentResolver(), SettingsSmt.Global.VOICE_INPUT_SHOW_RESULT_CHANGED_BY, SettingsSmt.VOICE_SHOW_RESULT_CHANGED_BY.AUTO);
        int checkIndex = Settings.Global.getInt(getContentResolver(), SettingsSmt.Global.VOICE_INPUT_SHOW_RESULT_KEY, SettingsSmt.VOICE_SHOW_RESULT_VALUE.SEARCH);
        // 用户未干预过的前提下，如下逻辑；否则参照设定值显示
        if (SettingsSmt.VOICE_SHOW_RESULT_CHANGED_BY.AUTO == isManual) {
            boolean installedBullet = PackageUtils.isAvilibleApp(this, SaraConstant.PACKAGE_NAME_BULLET);
            if (installedBullet) {
                //用户安装子弹短信，如果未登录，则设定显示为搜索结果；否则如果登录，设定值显示为子弹短信
                boolean isLoginBullet = PackageUtils.isBulletAppLogin(this);
                checkIndex = isLoginBullet ? SettingsSmt.VOICE_SHOW_RESULT_VALUE.BULLET : SettingsSmt.VOICE_SHOW_RESULT_VALUE.SEARCH;
            } else {
                //用户安装子弹短信后，又卸载，则设定值显示为搜索结果
                //用户未安装子弹短信，则设定值显示为搜索结果
                checkIndex = SettingsSmt.VOICE_SHOW_RESULT_VALUE.SEARCH;
            }
        }
        return checkIndex == SettingsSmt.VOICE_SHOW_RESULT_VALUE.BULLET;
    }

    private void initBulletVoiceSearchView() {
        if (mBulletVoiceSearchStub != null && mBulletVoiceSearchStub.getParent() != null) {
            mBulletVoiceSearchView = (VoiceSearchView) mBulletVoiceSearchStub.inflate();
        } else {
            mBulletVoiceSearchView = (VoiceSearchView) findViewById(R.id.bullet_voice_search);
        }
        mBulletVoiceSearchView.initVoiceRecognizeResultView(new VoiceRecognizeResultView.Select<AbsContactItem>() {
            @Override
            public void select(AbsContactItem result, View view) {
                BubbleItemView bubbleItemView = getCurrentBubbleView();
                if (null != bubbleItemView.getAttachmentList() && bubbleItemView.getAttachmentList().size() > 0) {
                    ToastUtil.showToast(R.string.bubble_share_tip);
                }
                bubbleItemView.stopPlay();
                handleSendBulletMessage(result);
                finishAndGotoSleep(true, false, true, true);
            }
        });
    }

    public void hideShowBulletVoiceSearch(boolean isShow) {
        if (!isShow) {
            if (null != mBulletVoiceSearchView) {
                mBulletVoiceSearchView.hide(true);
            }
        } else if (mSearchView.isCurrentBulletShow()) {
            initBulletVoiceSearchView();
            mBulletVoiceSearchView.show(true);
        }
    }

    private void releaseWakeLock() {
        releaseWakeLockDelayed(0);
    }

    private void releaseWakeLockDelayed(int delay) {
        Runnable runnable = new Runnable() {
            @Override public void run() {
                LogUtils.d(TAG, "release wakelock");
                if (mWakeLock.isHeld()) {
                    mWakeLock.release();
                }
            }
        };
        if (delay == 0) {
            runnable.run();
        } else {
            mCustomHandler.postDelayed(runnable, delay);
        }
    }

    private void closeRemind() {
        Intent intent = new Intent(SaraConstant.ACTION_CLOSE_REMIND);
        sendBroadcast(intent);
    }

    @Override
    public String[] getInterestedEvents() {
        return INTERESTED_EVENTS;
    }

    @Override
    public void onEvent(Event event) {
        String action = event.getAction();
        Bundle bundle = (Bundle) event.getExtra();
        BubbleItemView bubbleItemView = getCurrentBubbleView();
        if (FlashImContactsEvent.ACTION_SEND_BULLET_MESSAGE.equals(action)) {
            if (null != bubbleItemView.getAttachmentList() && bubbleItemView.getAttachmentList().size() > 0) {
                ToastUtil.showToast(R.string.bubble_share_tip);
            }
            if (null != bundle) {
                AbsContactItem contactItem = bundle.getParcelable("contact");
                if (null != contactItem) {
                    handleSendBulletMessage(contactItem);
                }
                UIHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        finishAndGotoSleep(true, true, true, true);
                    }
                }, 150);
            }
        } else if (FlashImContactsEvent.ACTION_SEND_BULLET_MESSAGE_PRE.equals(action)) {
            bubbleItemView.stopPlay();
            if (null != bundle) {
                if (bundle.getBoolean("withFlyAnim")) {
                    int[] selectLoc = bundle.getIntArray("selectLoc");
                    if (null != selectLoc) {
                        startSendBulletMsgFlyAnim(selectLoc);
                    }
                }
            }
        } else if (FlashImContactsEvent.ACTION_SEND_BULLET_FRAGMENT_VISIBLE_STATE.equals(action)) {
            boolean isShowVoiceSearch = null != bundle && bundle.getBoolean("showVoiceSearch");
            hideShowBulletVoiceSearch(isShowVoiceSearch);
        }else if(FlashImContactsEvent.ACTION_SEND_BULLET_HIDE_IUPUT_METHOD.equals(action)) {
            boolean isHideInputMethod = null != bundle && bundle.getBoolean("hideInputMethod");
            if (isHideInputMethod && bubbleItemView != null) {
                bubbleItemView.hideSoftInputFromWindow();
            }
        }
    }

    private void startSendBulletMsgFlyAnim(int[] selectLoc) {
        BubbleItemView bubbleItemView = getCurrentBubbleView();
        if (bubbleItemView.isKeyBoardVisible()) {
            bubbleItemView.hideSoftInputFromWindow();
        }
        List<GlobalBubbleAttach> globalBubbleAttaches = bubbleItemView.getAttachmentList();
        mFakeBubbleView.initFakeAnim(mGlobalBubble, globalBubbleAttaches, mDisplayWidth);
        int[] position = new int[2];
        bubbleItemView.getLocationOnScreen(position);
        int targetY = mFakeBubbleView.getFakeAnimTargetY(bubbleItemView.getLayoutParams().height);
        int translateY = targetY + position[1];
        bubbleItemView.setVisibility(View.GONE);
        position[0] = position[0] + bubbleItemView.getLayoutParams().width / 2;
        position[1] = position[1] + bubbleItemView.getLayoutParams().height / 2;

        mFakeBubbleView.startFakeBubbleSendFlyAnim(translateY, position, selectLoc);
    }

    private void addBubbleAndHideViewImmediatelyIfNeeded() {
        if (mCustomHandler.hasMessages(MSG_ADD_BUBBLE_AND_HIDE_VIEW)) {
            mCustomHandler.removeMessages(MSG_ADD_BUBBLE_AND_HIDE_VIEW);
            addBubbleAndHideView(false);
            mBubbleStatus = BUBBLE_STATUS_NORMAL;
        }
    }

    private void addBubbleAndHideView(boolean finish) {
        LogUtils.d(TAG, "addBubbleAndHideView() finish = " + finish);
        BubbleItemView bubbleItemView = getCurrentBubbleView(false);
        if (null == bubbleItemView) {
            return;
        }
        PointF point = null;
        boolean empty = mGlobalBubble == null || TextUtils.isEmpty(mGlobalBubble.getText());

        if (!empty) {
            point = BubbleManager.addBubble2SideBar(this, mGlobalBubble, bubbleItemView.getAttachmentList(), mIsOffLine, false);
        }
        hideView(0, point, finish, false);
    }

    private static void resetBubbleScaleAndPositionIfNeeded(BubbleItemView bubbleItemView) {
        if (null == bubbleItemView) {
            return;
        }
        float translationX = bubbleItemView.getTranslationX();
        float translationY = bubbleItemView.getTranslationY();
        float scaleX = bubbleItemView.getScaleX();
        float scaleY = bubbleItemView.getScaleY();
        boolean needReset = (scaleX == 0 || scaleY == 0) && (translationX != 0 || translationY != 0);
        if (needReset) {
            bubbleItemView.setTranslationX(0);
            bubbleItemView.setTranslationY(0);
            bubbleItemView.setScaleX(1.0f);
            bubbleItemView.setScaleY(1.0f);
        }

    }

    private boolean isShouldStartVoiceSearchView() {
        return mSearchView != null && mSearchView.isCurrentBulletShow() &&
                (mBulletVoiceSearchView != null && mBulletVoiceSearchView.getVisibility() == View.VISIBLE) &&
                !(mNoResultView != null && mNoResultView.getVisibility() == View.VISIBLE);
    }

    private int sendBulletMessage(AbsContactItem absContactItem) {
        int ret = 0;
        if (null != mGlobalBubble && mFlashImConnection.isServiceConnected()) {
            if (null != absContactItem) {
                ContactItem contact = (ContactItem) absContactItem;
                File file = new File(getFilesDir(), mGlobalBubble.getUri().getPath());
                Uri uri = FileProvider.getUriForFile(this, FILEPROVIDER_AUTHORITY, file);
                grantUriPermission(SaraConstant.PACKAGE_NAME_BULLET, uri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
                ret = mFlashImConnection.sendVoiceMessage(contact.getMessageType(), contact.getContactId(), uri.toString(), mGlobalBubble.getText());
            }
        }
        return ret;
    }

    private void handleSendBulletMessage(final AbsContactItem absContactItem) {
        BubbleManager.markAddBubble2List(false);
        MutiTaskHandler.post(new Runnable() {
            @Override
            public void run() {
                if (null != mGlobalBubble) {
                    synchronized (BubbleCleaner.INSTANCE.getLock()) {
                        int ret = sendBulletMessage(absContactItem);
                        LogUtils.infoRelease("handleSendMessage() ret = " + ret);
                    }
                }
            }
        });
    }
}
