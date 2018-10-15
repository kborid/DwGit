package com.smartisanos.sara.shell;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningTaskInfo;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.PointF;
import android.graphics.Point;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Message;
import android.os.PowerManager;
import android.os.SystemClock;
import android.service.onestep.GlobalBubble;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewStub;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.Toast;
import android.os.Build;
import android.os.Vibrator;
import android.media.AudioManager;
import android.os.Handler;
import android.view.Display;
import android.app.AlertDialog;
import android.content.DialogInterface;
import com.smartisanos.ideapills.common.util.BlurTask;
import com.smartisanos.sanbox.utils.SaraTracker;
import com.smartisanos.sara.BubbleActionUpHelper;
import com.smartisanos.sara.R;
import com.smartisanos.sara.bubble.AbstractVoiceActivity;
import com.smartisanos.sara.bubble.manager.BubbleManager;
import com.smartisanos.sara.bubble.view.FakeBubbleView;
import com.smartisanos.sara.bubble.view.IFakeBubbleView;
import com.smartisanos.sara.bubble.view.IWaveLayout;
import com.smartisanos.sara.bubble.view.WaveLayout;
import com.smartisanos.sara.entity.ShortcutApp;
import com.smartisanos.sara.storage.BubbleDataRepository;
import com.smartisanos.sara.storage.DrawerDataRepository;
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
import com.smartisanos.sara.util.ToastUtil;
import com.smartisanos.sara.util.ViewUtils;
import com.smartisanos.sara.widget.BubbleItemView;
import com.smartisanos.sara.widget.BubbleItemView.BubbleSate;
import com.smartisanos.sara.widget.ShortcutAppLayout;
import com.smartisanos.sara.widget.WaveView.AnimationListener;

import smartisanos.app.voiceassistant.ParcelableObject;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.lang.ref.WeakReference;

import smartisanos.util.LogTag;
import smartisanos.util.SidebarUtils;
import smartisanos.api.VibratorSmt;
import smartisanos.api.VibEffectSmt;
import smartisanos.widget.SmartisanBlankView;
import com.smartisanos.sara.util.AttachmentUtils;
import android.content.ClipData;
import android.service.onestep.GlobalBubbleAttach;
import smartisanos.util.DeviceType;
import com.smartisanos.ideapills.common.util.CommonUtils;

/**
 * 套壳呼出胶囊页面
 */
public class ShellBubbleActivity extends AbstractVoiceActivity implements AnimationListener, SaraUtils.BubbleViewChangeListener {
    public static final String TAG = "VoiceAss.ShellBubbleActivity";

    private static final int MSG_START_UI = 3;
    private static final int MSG_CREATE_WAV_ROOT_FILE = 5;
    private static final int MSG_HIDE_GLOBLE_BUBBLE = 6;
    private static final int MSG_FINISH_ACTIVITY_ANIM = 7;
    private static final int MSG_NO_RESULT_TIP = 8;
    private static final int MSG_REFRESH_SEARCH_VIEW = 14;

    private static final int SHOW_TOP_OF_SCREEN_FLAGS = WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED;

    private static final int BUBBLE_STATUS_NORMAL = 0;
    private static final int BUBBLE_STATUS_HIDDING = 1;

    private static final int WAKE_UP_START_STATUS_NONE = 0;
    private static final int WAKE_UP_START_STATUS_PENDING = 1;
    private static final int WAKE_UP_START_STATUS_STARTED_AFTER_PAUSE = 2;
    private static final int WAKE_UP_START_STATUS_STARTED_NORMAL = 3;

    private int mBubbleStatus = BUBBLE_STATUS_NORMAL;
    private int mWakeUpStartStatus = WAKE_UP_START_STATUS_NONE;

    private int mDisplayWidth;
    private int mDisplayHeight;
    private int mRealScreenHeight;
    private View mNoResultView;
    private View mResultLayout;
    private View mResultEmpty;
    private BubbleItemView mBubbleItemViewLeft;
    private ViewStub mBubbleLeftStub;
    private IWaveLayout mWaveLayout;
    private ViewStub mResultStub;
    private ViewStub mNoResultStub;
    private Vibrator mVibrator;
    private IFakeBubbleView mFakeBubbleView;
    private View mRootView;
    private String mLastTaskActivityName = "";
    private boolean mLastSideBarShowing;
    private boolean dlgHasShown = false;
    private StringBuffer result = new StringBuffer();
    private boolean mExistBubbleView = false;
    private boolean mExistShortcutView = false;
    private boolean mIsOffLine = false;
    private boolean mIsMaxWave = false;
    private GlobalBubble mGlobalBubble;
    private int mLocalWeb3MarginTop;
    private int mLeftBubbleWaveMinWidth;
    private int mResulTopMargin;
    private int mResulBottomMargin;
    private int mAvailableHeight;
    private int mStatusBarHeight;
    private static int mHeadSetTime = 0;
    private boolean mSaveHeadSetTime;
    AudioManager mAudioManager;
    private boolean mIsKeyOrTouchUp;
    private boolean mResumeKeyboard = true;
    private boolean mSleepValue;
    private PowerManager mPowerManager;
    private boolean mConvertTranslucent = false;
    protected boolean mPendingFinishing;
    protected ShortcutAppLayout mShortcutAppLayout;

    private BubbleActionUpHelper.MenuActionUpListener mActionUpListener = new BubbleActionUpHelper.MenuActionUpListener() {
        @Override
        public void onActionUp() {
            LogTag.d(TAG, "ACTION_MENU_UP");
            endKeyOrBlueRec();
        }
    };

    void endKeyOrBlueRec(){
        mIsKeyOrTouchUp = true;
        if (getRecordStatus() == RECORD_STATUS_IDLE) {
            mWakeUpStartStatus = WAKE_UP_START_STATUS_NONE;
            finish();
        }
        stopRecognize(false);
    }

    protected BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            BubbleItemView bubbleItemView = getCurrentBubbleView();
            if (SaraConstant.ACTION_RECORD_ERROR.equals(action) && !dlgHasShown) {
                showDialog(context);
            } else if (SaraConstant.ACTION_FINISH_BUBBLE_ACTIVITY.equals(action)) {
                finish();
            } else if (SaraConstant.ACTION_UPDATE_BUBBLE.equals(action)) {
                resultFromIntentUpdate(intent);
            } else if (SaraConstant.ACTION_IDEAPILLS_SHARE_INVITATION_SEND.equals(action)) {
                updateBubble2Share();
            } else if (Intent.ACTION_CLOSE_SYSTEM_DIALOGS.equals(action)) {
                String reason = intent.getStringExtra("reason");
                if (SaraConstant.ACTION_REASON_HOME_KEY.equals(reason)) {
                    if (bubbleItemView != null && !isStopped() &&
                            mGlobalBubble != null && BubbleManager.isAddBubble2List()) {
                        bubbleItemView.addBubble2SideBar(true);
                    }
                    SaraUtils.overridePendingTransition(ShellBubbleActivity.this, true);
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

    @Override
    protected void localResult(ParcelableObject result) {
    }

    @Override
    protected void error(int errorCode) {
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
    protected void resultRecived(String resultStr, boolean offline) {
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
        if ((TextUtils.isEmpty(resultStr) || (resultStr.length() == 1 && StringUtils.isChinesePunctuation(resultStr.charAt(0))))
                && TextUtils.isEmpty(result.toString())) {
            postShowNoResultView();
            resetSearchViewData();
        } else {
            mCustomHandler.removeMessages(MSG_START_UI);
            okResultHandled();
            mIsOffLine = offline;
            result.append(resultStr);
            String temp = result.toString();
            if (temp.length() <= 10 && StringUtils.isChinesePunctuation(temp.charAt(temp.length() - 1))) {
                result.deleteCharAt(temp.length() - 1);
            }
            if (mIsMaxWave) {
                playPopup2TextAnim();
            }
            formatBubble(mIsOffLine);
            loadResult(false, result.toString(), false);
        }
    }

    @Override
    protected void resultTimeOut() {
        showNoResultViewDuringRecording(-1);
    }

    @Override
    protected void buffer(byte[] buffer, int totalPoint) {
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

    private CustomHandler mCustomHandler = new CustomHandler(this);
    private static class CustomHandler extends Handler {
        private final WeakReference<ShellBubbleActivity> mActivity;

        public CustomHandler(ShellBubbleActivity activity) {
            mActivity = new WeakReference<ShellBubbleActivity>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            ShellBubbleActivity activity = mActivity.get();
            LogTag.d(TAG, "handleMessage id " + msg.what);
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
                        activity.finish();
                        break;
                    case MSG_NO_RESULT_TIP:
                        activity.showNoResultViewDuringRecording(msg.arg1);
                        break;
                    case MSG_REFRESH_SEARCH_VIEW:
                        Bundle data = msg.getData();
                        boolean hasBubble = data.getBoolean("hasBubble");
                        boolean hasLocal = data.getBoolean("hasLocal");
                        boolean update = data.getBoolean("update");
                        boolean isSmallEdit = data.getBoolean("isSmallEdit");
                        boolean needScaleAnim = data.getBoolean("needScaleAnim");
                        activity.refreshSearchView(hasBubble, update, isSmallEdit, needScaleAnim);
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
        SaraUtils.setSearchType(this, SaraUtils.BUBBLE_TYPE.SHELL_BUBBLE.name());
        mPendingFinishing = false;
        mIsKeyOrTouchUp = false;
        mSleepValue = getIntent().getBooleanExtra(SaraConstant.SCREEN_OFF_KEY, false);
        if (!SaraUtils.isSettingEnable(this) || !RecognizeHelper.getInstance().isAllowedToRecognize()) {
            finish();
            return;
        }
        LogUtils.infoRelease(TAG, "onCreate():" + getRecordStatus());
        mPowerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);

        registerRecever();
        LayoutInflater inflater = LayoutInflater.from(this);
        mRootView = inflater.inflate(R.layout.sara_shell_bubble_main, null);
        if (mSleepValue) {
            gaussianBlur(true);
        } else {
            gaussianBlur(false);
        }
        setContentView(mRootView);
        initView(inflater);
        initDimens();

        Display display = getWindowManager().getDefaultDisplay();
        mDisplayWidth = display.getWidth();
        mDisplayHeight = display.getHeight();
        Point realSize = new Point();
        display.getRealSize(realSize);
        mRealScreenHeight = realSize.y;
        mVibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

        mWakeUpStartStatus = mSleepValue ? WAKE_UP_START_STATUS_PENDING : WAKE_UP_START_STATUS_NONE;
        if (mWakeUpStartStatus != WAKE_UP_START_STATUS_PENDING) {
            preStartVoiceRecognition(true);
            mWakeUpStartStatus = WAKE_UP_START_STATUS_STARTED_NORMAL;
        }
    }

    public void preStartVoiceRecognition(boolean createFlag) {
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

    private void registerRecever() {
        IntentFilter filter = new IntentFilter(SaraConstant.ACTION_RECORD_ERROR);
        filter.addAction(SaraConstant.IMAGE_LOADER_CACHE_CHANGE);
        filter.addAction(SaraConstant.ACTION_FINISH_BUBBLE_ACTIVITY);
        filter.addAction(SaraConstant.ACTION_UPDATE_BUBBLE);
        filter.addAction(SaraConstant.ACTION_IDEAPILLS_SHARE_INVITATION_SEND);
        filter.addAction(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
        filter.addAction(SaraConstant.ACTION_CHOOSE_RESULT);
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        registerReceiver(mReceiver, filter);
    }

    public void initView(LayoutInflater inflater) {
        mWaveLayout = new WaveLayout(this, null,
                (ViewStub) findViewById(R.id.wave_left_stub), this);
        mBubbleLeftStub = (ViewStub) findViewById(R.id.bubble_item_left_stub);
        mResultStub = (ViewStub) findViewById(R.id.result_stub);
        mNoResultStub = (ViewStub) findViewById(R.id.no_result_stub);
        mFakeBubbleView = new FakeBubbleView(this, (ViewStub) findViewById(R.id.fak_bubble_stub));
        mWaveLayout.init(true);
        initBubbleViewLeft();
        BubbleItemView bubbleItemView = getCurrentBubbleView();
        bubbleItemView.updateTargetWidth(mDisplayWidth);
    }

    public void initResultLayout() {
        if (mResultLayout == null) {
            if (mResultStub.getParent() != null) {
                mResultLayout = mResultStub.inflate();
            } else {
                mResultLayout = findViewById(R.id.result);
            }
            mResultEmpty = mResultLayout.findViewById(R.id.result_hide_empty);
            mShortcutAppLayout = (ShortcutAppLayout) mResultLayout.findViewById(R.id.local_result);
            mShortcutAppLayout.init();
            mShortcutAppLayout.setOnShareClickListener(new ShortcutAppLayout.onShareClickListener(){
                public void onShareCutAppChangedClick() {
                    Intent settingIntent = new Intent(ShellBubbleActivity.this, ShortCutAppSettingActivity.class);
                    SaraUtils.startActivityForResult(ShellBubbleActivity.this, settingIntent, SaraConstant.RESULT_CHANGE_SHORTCUTAPP);
                }

                public void onIconClick(ShortcutApp item) {
                    if (item.getPackageName().equals(SaraConstant.RECORDER_PACKAGE_NAME)) {
                        if (mGlobalBubble != null && mGlobalBubble.getType() != GlobalBubble.TYPE_TEXT && mGlobalBubble.getUri() != null) {
                            Intent intent = new Intent(SaraConstant.ACTION_SHARE_RECORD);
                            intent.putExtra(SaraConstant.SHARE_AUDIO_KEY, SaraUtils.bubble2Bundle(ShellBubbleActivity.this, mGlobalBubble));
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            intent.setPackage(SaraConstant.RECORDER_PACKAGE_NAME);
                            intent.setComponent(item.getComponentName());
                            ShellBubbleActivity.this.startActivity(intent);
                        } else {
                            ToastUtil.showToast(ShellBubbleActivity.this, ShellBubbleActivity.this.getString(R.string.voice_file_null));
                        }
                    } else {
                        Intent intent = new Intent(Intent.ACTION_SEND);
                        intent.setComponent(item.getComponentName());
                        if (mGlobalBubble != null) {
                            intent.putExtra(Intent.EXTRA_TEXT, mGlobalBubble.getText());
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        }
                        ShellBubbleActivity.this.startActivity(intent);
                    }
                }
            });
        }
        mResultEmpty.setTranslationY(0);
        mShortcutAppLayout.setTranslationY(0);
    }

    public void initBubbleViewLeft() {
        if (mBubbleLeftStub.getParent() != null) {
            mBubbleItemViewLeft = (BubbleItemView) mBubbleLeftStub.inflate();
        } else {
            mBubbleItemViewLeft = (BubbleItemView) findViewById(R.id.bubble_item_left);
        }
        mBubbleItemViewLeft.setViewListener(this);
        mBubbleItemViewLeft.setSoftListener(this);

        mBubbleItemViewLeft.setBubbleClickListener(new BubbleItemView.OnBubbleClickListener() {
            public void onAddAttachmentClick() {
                SaraUtils.startAttachementChoose(ShellBubbleActivity.this);
            }

            public void onAttachmentChanged() {
                mBubbleItemViewLeft.refreshAttachmentView();
                setBubbleState(BubbleSate.LARGE, false, false);
            }

            public void onImageAttchmentClick(GlobalBubbleAttach globalBubbleAttach, ArrayList<Uri> localUris) {
                SaraUtils.startImagePreview(ShellBubbleActivity.this, globalBubbleAttach, localUris);
            }

            public void onFileClick(GlobalBubbleAttach globalBubbleAttach) {
                SaraUtils.startFilePreview(ShellBubbleActivity.this, globalBubbleAttach);
            }
        });
    }

    protected BubbleItemView getCurrentBubbleView() {
        if (mBubbleItemViewLeft == null) {
            initBubbleViewLeft();
        }
        return mBubbleItemViewLeft;
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            checkWakeUpAndPreStartVoiceRecognition();
        }
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

    public void initDimens() {
        mLocalWeb3MarginTop = getResources().getDimensionPixelSize(R.dimen.local_web_3_margin_top);
        mLeftBubbleWaveMinWidth = getResources().getDimensionPixelSize(R.dimen.bubble_wave_left_min_width);
        mResulTopMargin = getResources().getDimensionPixelSize(R.dimen.result_margin_top);
        mResulBottomMargin = getResources().getDimensionPixelSize(R.dimen.result_margin_bottom);
        mAvailableHeight = mDisplayHeight - mResulTopMargin - mResulBottomMargin;
        mStatusBarHeight = getResources().getDimensionPixelSize(com.android.internal.R.dimen.status_bar_height);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        LogUtils.infoRelease(TAG, "onNewIntent():" + getRecordStatus());
        super.onNewIntent(intent);
        SaraUtils.setSearchType(this, SaraUtils.BUBBLE_TYPE.SHELL_BUBBLE.name());
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
        mIsKeyOrTouchUp = false;
        mGlobalBubble = null;

        BubbleItemView bubbleItemView = getCurrentBubbleView();
        bubbleItemView.setAttachmentList(null);
        DrawerDataRepository.INSTANCE.reloadAsync();
        if (mSleepValue && !mPowerManager.isScreenOn()) {
            PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
            MultiSdkAdapter.wakeUp(pm, SystemClock.uptimeMillis(), getPackageName() + ":window_focus");
        }
        preStartVoiceRecognition(false);
        mWakeUpStartStatus = WAKE_UP_START_STATUS_NONE;
        bubbleItemView.setVisibility(View.GONE);
    }


    @Override
    public void onResume() {
        super.onResume();
        LogTag.d(TAG, "onResume()");
        mCustomHandler.removeMessages(MSG_FINISH_ACTIVITY_ANIM);
        SaraUtils.setSearchType(this, SaraUtils.BUBBLE_TYPE.SHELL_BUBBLE.name());
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

    private boolean isWaitingWakeUpAndStart() {
        return mWakeUpStartStatus != WAKE_UP_START_STATUS_NONE
                && mWakeUpStartStatus != WAKE_UP_START_STATUS_STARTED_NORMAL
                && !mIsKeyOrTouchUp && !isPendingFinishing()
                && Build.VERSION.SDK_INT < 26;
    }

    private boolean isRecordingWithoutInteraction() {
        return isRecognizing() &&
                (mIsKeyOrTouchUp || isPendingFinishing() || mWakeUpStartStatus == WAKE_UP_START_STATUS_NONE);
    }

    protected boolean isPendingFinishing() {
        return isFinishing() || mPendingFinishing;
    }

    @Override
    public void onPause() {
        super.onPause();
        LogTag.d(TAG, "enter onPause():" + getRecordStatus());
        boolean screenOn = mPowerManager.isScreenOn();

        mResumeKeyboard = true;
        BubbleItemView bubbleItemView = getCurrentBubbleView();
        if (BubbleManager.isAddBubble2List() && mGlobalBubble != null) {
            if (!screenOn || SaraUtils.isTopSelfApp(this)) {
                PointF point = null;
                if (!TextUtils.isEmpty(mGlobalBubble.getText())) {
                    point = BubbleManager.addBubble2SideBar(this, mGlobalBubble, bubbleItemView.getAttachmentList(), mIsOffLine, false);
                }
                hideView(0, point, false, false);
            }
        }
        if (isRecordingWithoutInteraction()) {
            stopRecognize(true);
        }
        if (bubbleItemView != null) {
            bubbleItemView.checkInput(false);
            bubbleItemView.hideSoftInputFromWindow();
        }

        LogTag.d(TAG, "result = " + result);

        mBubbleStatus = BUBBLE_STATUS_NORMAL;
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
            mCustomHandler.removeCallbacksAndMessages(null);
            if (!isScreenOn || isFinishPosted) {
                LogUtils.d(TAG, "finish ourself due to screen off!");
                mCustomHandler.sendEmptyMessageDelayed(MSG_FINISH_ACTIVITY_ANIM, 100);
            } else if (!isPendingFinishing()) {
                if (isUiChanging || getRecordStatus() != RECORD_STATUS_RESULT_OK_STOPPED) {
                    if (getRecordStatus() == RECORD_STATUS_STARTING || getRecordStatus() == RECORD_STATUS_STARTED) {
                        showNoResultView(SpeechError.ERROR_INTERRUPT);
                    }/* else {
                        showNoInterruptView();
                    }*/
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
        LogTag.d(TAG, "enter finish()");
        SaraUtils.overridePendingTransition(this, true);
    }

    @Override
    public void onDestroy() {
        LogTag.d(TAG, "onDestroy()");
        mCustomHandler.removeCallbacksAndMessages(null);
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
            BubbleItemView bubbleItemView = getCurrentBubbleView();
            if (SaraUtils.checkFinish(bubbleItemView, ev) && mWaveLayout.checkFinish(ev)
                    && SaraUtils.checkFinish(mShortcutAppLayout, ev)) {
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
        SaraUtils.recordFile(this, offline, result.toString());
        int type;
        type = offline ? GlobalBubble.TYPE_VOICE_OFFLINE : GlobalBubble.TYPE_VOICE;
        mGlobalBubble = SaraUtils.toGlobalBubble(this, result.toString(), type,
                SaraUtils.getUri(this), SaraUtils.getDefaultBubbleColor(this), 0, 0);
        BubbleManager.markAddBubble2List(true);
    }

    public void startUI() {
        LogTag.d(TAG, "enter startUI()");
        result = new StringBuffer();
        setParamWidth(mLeftBubbleWaveMinWidth);
        resetView();
        mWaveLayout.show(SaraUtils.getDefaultBubbleColor(this));
        mCustomHandler.sendEmptyMessage(MSG_HIDE_GLOBLE_BUBBLE);
    }

    public void loadResult(boolean update, String tmp, boolean isSmallEdit) {
        loadResult(update, tmp, isSmallEdit, false);
    }

    public void loadResult(boolean update, String tmp, boolean isSmallEdit, boolean needScaleAnim) {
        if (mResultLayout == null) {
            initResultLayout();
        }
        List<GlobalBubbleAttach> listAttachs = getCurrentBubbleView().getAttachmentList();
        if (listAttachs != null && listAttachs.size() > 0) {
            refreshSearchView(true, update, false, needScaleAnim);
        } else {
            refreshSearchView(true, update, isSmallEdit, needScaleAnim);
        }
        if (!update) {
            SaraUtils.dismissKeyguardOnNextActivity();
        }
    }

    private void resetSearchViewData() {
        if (mShortcutAppLayout != null) {
            mShortcutAppLayout.setVisibility(View.GONE);
        }
    }

    private void resultFromIntentUpdate(Intent intent){
        StringBuffer newBubbleText = BubbleManager.updateBubbleFromIntent(intent, mGlobalBubble);
        if (newBubbleText != null) {
            result = newBubbleText;
            loadResult(true, result.toString(), false);
        }
    }

    protected void updateBubble2Share() {
        BubbleItemView bubbleItemView = getCurrentBubbleView();
        if (bubbleItemView != null && mGlobalBubble != null) {
            bubbleItemView.changeColor2Share();
        }
    }

    private void refreshSearchView(boolean hasBubble, boolean update, boolean isSmallEdit) {
        refreshSearchView(hasBubble, update, isSmallEdit, false);
    }

    private void refreshSearchView(boolean hasBubble, boolean update, boolean isSmallEdit, boolean needScaleAnim) {
        BubbleItemView bubbleItemView = getCurrentBubbleView();
        if (hasBubble && bubbleItemView != null && mGlobalBubble != null) {
            bubbleItemView.setGlobalBubble(mGlobalBubble, mIsOffLine);
            mExistBubbleView = true;
        } else {
            mExistBubbleView = false;
        }

        int countData = 0;
        if (mShortcutAppLayout != null) {
            mShortcutAppLayout.notifyDataSetChanged();
            countData = mShortcutAppLayout.countData();
            if (countData > 0) {
                mExistShortcutView = true;
            } else {
                mExistShortcutView = false;
            }
        } else {
            mExistShortcutView = false;
        }

        List<GlobalBubbleAttach> attachList = bubbleItemView.getAttachmentList();
        if (attachList != null && attachList.size() > 0) {
            mExistShortcutView = false;
        }
        if (mExistBubbleView && mExistShortcutView) {
            int resultHeight = setHideEmptyViewVisible(BubbleSate.SMALL, isSmallEdit, needScaleAnim);
            initShortAppHeight(countData - 1, resultHeight);

            bubbleItemView.setVisibility(View.VISIBLE);
            mShortcutAppLayout.setVisibility(View.VISIBLE);
        } else if (mExistBubbleView) {
            setHideEmptyViewVisible(BubbleSate.LARGE, isSmallEdit, needScaleAnim);
            bubbleItemView.setVisibility(View.VISIBLE);
            mShortcutAppLayout.setVisibility(View.GONE);
        } else {
            bubbleItemView.setVisibility(View.GONE);
            mResultEmpty.setVisibility(View.GONE);
            mShortcutAppLayout.setVisibility(View.GONE);
        }
        if (!update) {
            cardAnimation(isSmallEdit, needScaleAnim);
        }
        boolean hasResult = mExistShortcutView;
        bubbleItemView.setEditable(true);
        LogUtils.d("refreshSearchView hasResult:" + hasResult);
        trackResultEvent(hasResult);
    }

    private void trackResultEvent(boolean hasResult) {
        LinkedHashMap<String, Object> trackerData = new LinkedHashMap<String, Object>();
        trackerData.put("result", hasResult ? 1 : 0);
        SaraTracker.onEvent("A429001", trackerData);
    }

    public int setHideEmptyViewVisible(BubbleSate state, boolean isSmallEdit, boolean needScaleAnim) {
        BubbleItemView bubbleItemView = getCurrentBubbleView();
        bubbleItemView.setVisibility(View.VISIBLE);
        bubbleItemView.setBubbleState(state, false, isSmallEdit, false, needScaleAnim);
        FrameLayout.LayoutParams emptyParams = (FrameLayout.LayoutParams) mResultEmpty.getLayoutParams();
        if (bubbleItemView.getLastBubbleState() == BubbleSate.SMALL
                || bubbleItemView.getLastBubbleState() == BubbleSate.LARGE) {
            emptyParams.height = bubbleItemView.getExactHeight();
        } else if (bubbleItemView.getLastBubbleState() == BubbleSate.NORMAL
                || bubbleItemView.getLastBubbleState() == BubbleSate.LOADING) {
            emptyParams.height = ViewUtils.getExactHeight(bubbleItemView,
                    bubbleItemView.getLayoutParams().width);
        } else {
            emptyParams.height = ViewUtils.getSupposeHeightNoFixWidth(bubbleItemView);
        }
        mResultEmpty.setLayoutParams(emptyParams);
        mResultEmpty.setVisibility(View.VISIBLE);
        return emptyParams.height;
    }

    public void cardAnimation(boolean isSmallEdit, boolean needScaleAnim) {
        if (mExistBubbleView && mExistShortcutView) {
            setBubbleState(BubbleSate.SMALL, isSmallEdit, true, needScaleAnim);
            AnimManager.showViewWithAlphaAndTranslate(mShortcutAppLayout, 150, 250, 150);
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
        if (bubbleItemView != null) {
            LogTag.d(TAG, "bubbleItemView GONE hideTextPopup ");
            bubbleItemView.setVisibility(View.GONE);
        }
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


    public boolean clearAnimation() {
        boolean isClearAnimation = false;
        BubbleItemView bubbleItemView = getCurrentBubbleView();
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
        if (mShortcutAppLayout != null) {
            if (mShortcutAppLayout.getAnimation() != null && !mShortcutAppLayout.getAnimation().hasEnded()) {
                isClearAnimation = true;
            }
            mShortcutAppLayout.clearAnimation();
        }

        isClearAnimation |= mWaveLayout.clearAnim();
        return isClearAnimation;
    }

    private void resetView() {
        mExistBubbleView = false;
        mExistShortcutView = false;
        if (mShortcutAppLayout != null) {
            mShortcutAppLayout.setVisibility(View.GONE);
        }

        BubbleItemView bubbleItemView = getCurrentBubbleView();
        if (bubbleItemView != null) {
            LogTag.d(TAG, "bubbleItemView GONE resetView ");
            bubbleItemView.setVisibility(View.GONE);
            bubbleItemView.checkInput(false);
            bubbleItemView.hideSoftInputFromWindow();
        }
        if (mNoResultView != null) {
            mNoResultView.setVisibility(View.GONE);
        }
        bubbleItemView.setBubbleState(BubbleSate.INIT, true, false, false);
        bubbleItemView.setEditable(false);
        mIsMaxWave = false;
    }

    @Override
    public void onAnimationEnd(int width, boolean isCanceled) {
        if (isStopped()) {
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
            FrameLayout.LayoutParams bubbleParams = (FrameLayout.LayoutParams) bubbleItemView.getLayoutParams();
            if (bubbleParams.width != width) {
                bubbleParams.width = width + getResources().getDimensionPixelOffset(R.dimen.nomal_bubble_left_margin);
                bubbleItemView.setLayoutParams(bubbleParams);
            }
        }
    }

    public void setBubbleState(BubbleSate state, boolean isSmallEdit, boolean isAnim) {
        setBubbleState(state, isSmallEdit, isAnim, false);
    }

    public void setBubbleState(BubbleSate state, boolean isSmallEdit, boolean isAnim, boolean needScaleAnim) {
        LogTag.d(TAG, "setBubbleState state:" + state);
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
            bubbleItemView.setText(result.toString());
            bubbleItemView.setBubbleState(BubbleSate.NORMAL, true, false, false);
        } else {
            if (mIsMaxWave) {
                mWaveLayout.hideTextPopup(true);
                bubbleItemView.setBubbleState(BubbleSate.LOADING, true, false, false);
            }
        }
    }

    public void setSearchLayoutParams(View view, int delayTime, boolean single) {
        int targetHeight;
        int targetTranslateY = 0;
        if (!single) {
            if (view instanceof ShortcutAppLayout) {
                targetHeight = mAvailableHeight - mShortcutAppLayout.getHeight() - mLocalWeb3MarginTop / 2;
                targetTranslateY = -(mAvailableHeight - mShortcutAppLayout.getHeight() - view.getHeight()) + mLocalWeb3MarginTop / 2;
            } else {
                targetHeight = view.getHeight();
                targetTranslateY = -(mAvailableHeight - view.getHeight()) + mLocalWeb3MarginTop / 2;
            }
        } else {
            targetHeight = mAvailableHeight + mLocalWeb3MarginTop / 2;
            if (view instanceof ShortcutAppLayout) {
                targetTranslateY = -(mAvailableHeight - view.getHeight()) - mLocalWeb3MarginTop / 2;
            } else {
                targetTranslateY = -(getCurrentBubbleView().getHeight() + mLocalWeb3MarginTop);
            }
        }
        AnimManager.showViewWithTranslateAndHeight(view, 250,
                view.getHeight(), targetHeight,
                targetTranslateY, delayTime);
    }

    @Override
    public void hideView(int from, PointF point, final boolean finish, final boolean needSleep) {
        int[] positionBubble = new int[2];
        BubbleItemView bubbleItemView = getCurrentBubbleView();
        bubbleItemView.getLocationOnScreen(positionBubble);
        int bubbleItemTranslateY = (int) bubbleItemView.getTranslationY() - 500;
        if (from == 0) {
            LogUtils.d(TAG, "hideView mBubbleStatus = " + mBubbleStatus);
            if (mBubbleStatus == BUBBLE_STATUS_HIDDING) {
                return;
            }
            mBubbleStatus = BUBBLE_STATUS_HIDDING;
            if (finish) {
                mPendingFinishing = true;
            }
            if (point != null) {
                List<GlobalBubbleAttach> globalBubbleAttaches = bubbleItemView.getAttachmentList();
                mFakeBubbleView.initFakeAnim(mGlobalBubble, globalBubbleAttaches, mDisplayWidth);
                int[] position = new int[2];
                bubbleItemView.getLocationOnScreen(position);
                int targetY = mFakeBubbleView.getFakeAnimTargetY(bubbleItemView.getLayoutParams().height);
                int targetX = mFakeBubbleView.getFakeAnimTargetX();
                int translateX = targetX - position[0];

                bubbleItemView.hideViewWithScaleAnim(point, mFakeBubbleView.getFakeBubbleBgWidth(),
                        mFakeBubbleView.getFakeBubbleBgHeight(), translateX, targetY + position[1]);
                mFakeBubbleView.startFakeAnim(targetY + position[1],
                        new AnimatorListenerAdapter() {
                            @Override
                            public void onAnimationEnd(Animator animation) {
                                SaraUtils.goToSleepIfNoKeyguard(ShellBubbleActivity.this, mSleepValue && needSleep, false);
                                if (finish) {
                                    Message message = mCustomHandler.obtainMessage(MSG_FINISH_ACTIVITY_ANIM);
                                    mCustomHandler.sendMessage(message);
                                }
                            }
                        });
            } else {
                AnimManager.hideViewWithAlphaAndTranslate(bubbleItemView, 0, 250, bubbleItemTranslateY);
            }

            Message message = mCustomHandler.obtainMessage(MSG_FINISH_ACTIVITY_ANIM);
            if (mShortcutAppLayout != null && mShortcutAppLayout.getVisibility() == View.VISIBLE) {
                if (finish) {
                    AnimManager.HideViewWithAlphaAnim(mResultEmpty, 200, 250);
                    AnimManager.HideViewWithAlphaAnim(mShortcutAppLayout, 200, 250);
                } else {
                    AnimManager.hideViewWithAlphaAndTranslate(mResultEmpty, 0, 250, bubbleItemTranslateY);
                    mCustomHandler.sendMessageDelayed(message, 250);
                }
            } else {
                if (point == null) {
                    mCustomHandler.sendMessageDelayed(message, 250);
                }
            }

        }
    }

    @Override
    public void deleteVoice(GlobalBubble globalBubble) {
        Bundle bunble = new Bundle();
        bunble.putIntArray(SaraConstant.KEY_BUBBLE_IDS, new int[]{globalBubble.getId()});
        bunble.putString(SaraConstant.KEY_DESTROY_TYPE, SaraConstant.DESTROY_TYPE_REMOVED);
        BubbleDataRepository.destroyGlobleBubble(this, bunble);
    }

    @Override
    public void editView(final boolean keyboardVisible, boolean isSmallEdit) {
        final BubbleItemView bubbleItemView = getCurrentBubbleView();
        if (bubbleItemView.getBubbleState() == BubbleSate.INIT) {
            if (keyboardVisible) {
                bubbleItemView.setBubbleState(BubbleSate.KEYBOARD, true, false, false);
            }
        } else {
            int targetTranslate = 0;
            int bubbleTargetHeight = bubbleItemView.getExactHeight();
            if (keyboardVisible) {
                targetTranslate = -bubbleItemView.getKeyboardHeight() + mResulTopMargin;
            } else {
                if (bubbleItemView.getBubbleState() == BubbleSate.SMALL) {
                    targetTranslate = bubbleTargetHeight - mRealScreenHeight + mResulTopMargin;
                } else if (bubbleItemView.getBubbleState() == BubbleSate.LARGE) {
                    targetTranslate = (bubbleTargetHeight - mRealScreenHeight) / 2 - mStatusBarHeight;
                }
            }

            AnimManager.hideViewWithAlphaAndTranslate(mShortcutAppLayout, 0, 250, 150);
            AnimManager.showViewWithTranslate(bubbleItemView, 150, 250, targetTranslate, null, bubbleTargetHeight);

            if (isSmallEdit) {
                bubbleItemView.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        bubbleItemView.setGravityLeft();
                    }
                }, 250);
            }
        }
    }

    @Override
    public Activity getActivityContext() {
        return this;
    }

    @Override
    public int getBubbleSmallTranslation(int bubbleHeight) {
        int targetY = 0;
        int resultEmptyHeight = ((FrameLayout.LayoutParams) mResultEmpty.getLayoutParams()).height;
        int shortcutAppHeight = 0;
        if (mShortcutAppLayout != null) {
            shortcutAppHeight = ((FrameLayout.LayoutParams) mShortcutAppLayout.getLayoutParams()).height;
        }
        targetY = (resultEmptyHeight - mRealScreenHeight - shortcutAppHeight - mLocalWeb3MarginTop) / 2;
        return targetY;
    }

    @Override
    public int getBubbleLargeTranslation(int bubbleHeight) {
        int resultEmptyHeight = ((FrameLayout.LayoutParams) mResultEmpty.getLayoutParams()).height;
        return (resultEmptyHeight - mRealScreenHeight) / 2 - mStatusBarHeight;
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
        }
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        final int keyCode = event.getKeyCode();
        if (keyCode == SaraConstant.KEYCODE_SMART && event.getAction() == KeyEvent.ACTION_UP) {
            long duration = event.getEventTime() - event.getDownTime();
            if (duration < SaraConstant.TIME_LONG_PRESS_KEYCODE_SMART) {
                // keycode for click
                if (!saveBubbleAndResetScreen()) {
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
        return super.onKeyUp(keyCode, event);
    }


    private void checkTimeOutWarning(){
        if (!mIsKeyOrTouchUp && hasWindowFocus() && SaraUtils.isGlobalVibrateOn(this)){
            VibratorSmt.getInstance().vibrateWithPrivilege(mVibrator, SaraConstant.VIBRATE_TIME);
        }
    }

    @Override
    public void onBackPressed() {
        if (!saveBubbleAndResetScreen()) {
            super.onBackPressed();
        }
    }

    private boolean saveBubbleAndResetScreen() {
        BubbleItemView bubbleItemView = getCurrentBubbleView();
        if (bubbleItemView != null && mGlobalBubble != null && BubbleManager.isAddBubble2List()) {
            PointF point = bubbleItemView.addBubble2SideBar(false);
            hideView(0, point, true, true);
            return true;
        }
        return false;
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        LogTag.d(TAG, "onActivityResult()");
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
            case SaraConstant.RESULT_CHANGE_SHORTCUTAPP:
                if (mShortcutAppLayout != null) {
                    mShortcutAppLayout.notifyDataSetChanged();
                    refreshSearchView(true, true, false);
                }
                break;

        }
    }

    private void handleRemindViewResult(Intent data) {
        BubbleItemView bubbleItemView = getCurrentBubbleView();
        if (mGlobalBubble != null && bubbleItemView != null) {
            long remindTime = data == null ? 0 : data.getLongExtra(SaraConstant.REMIND_TIME_KEY, 0);
            long dueTime = data == null ? 0 : data.getLongExtra(SaraConstant.DUE_DATE_KEY, 0);
            mGlobalBubble.setRemindTime(remindTime);
            mGlobalBubble.setDueDate(dueTime);
            bubbleItemView.setGlobalBubble(mGlobalBubble);
            bubbleItemView.trackBubbleChange(false);
            getCurrentBubbleView().refreshRemindView();
        }
    }

    private void handleImagePreviewResult(Intent data) {
        ClipData cd = data.getClipData();
        if (cd == null) {
            return;
        }
        BubbleItemView bubbleItemView = getCurrentBubbleView();
        List<GlobalBubbleAttach> listAttach = AttachmentUtils.mergeImageAttachList(
                ShellBubbleActivity.this, bubbleItemView.getAttachmentList(), cd);
        bubbleItemView.setAttachmentList(listAttach);
        bubbleItemView.refreshAttachmentView();
        refreshSearchView(true, false, false);
    }

    private void handleAttachmentPickResult(Intent data) {
        if (data == null || mGlobalBubble == null) {
            LogUtils.e("handleAttachmentPickResult return by data or bubble is null");
            return;
        }
        BubbleItemView bubbleItemView = getCurrentBubbleView();
        List<GlobalBubbleAttach> realAttachList =
                AttachmentUtils.mergeAttachmentList(ShellBubbleActivity.this, bubbleItemView.getAttachmentList(), data);
        bubbleItemView.setAttachmentList(realAttachList);
        bubbleItemView.refreshAttachmentView();
        refreshSearchView(true, false, false);
    }

    private void initShortAppHeight(int countApp, int resultHeight) {
        FrameLayout.LayoutParams localParams = (FrameLayout.LayoutParams) mShortcutAppLayout.getLayoutParams();
        int targetHeight = getResources().getDimensionPixelOffset(R.dimen.short_item_top_margin) * 2 + getResources().getDimensionPixelOffset(R.dimen.short_bottom_setting_height);
        int item = countApp / 4;
        if (countApp % 4 != 0) {
            item += 1;
        }
        targetHeight += item * getResources().getDimensionPixelOffset(R.dimen.shortcut_item_height) + (item - 1) * getResources().getDimensionPixelOffset(R.dimen.short_item_middle_gap);
        localParams.height = targetHeight;
        localParams.topMargin = (resultHeight + mRealScreenHeight - targetHeight + mLocalWeb3MarginTop) / 2 - mStatusBarHeight;
        mShortcutAppLayout.setLayoutParams(localParams);
    }

}
