package com.smartisanos.sara.bubble.revone;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.graphics.Point;
import android.graphics.PointF;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.service.onestep.GlobalBubble;
import android.service.onestep.GlobalBubbleAttach;
import android.text.TextUtils;
import android.view.Display;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.view.animation.AnimationSet;
import android.view.animation.AnimationUtils;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.FrameLayout.LayoutParams;
import android.widget.ImageView;
import android.widget.Toast;

import com.smartisanos.ideapills.common.util.WallpaperUtils;

import com.smartisanos.sara.bubble.SettingActivity;
import com.smartisanos.sara.bubble.manager.BubbleManager;
import com.smartisanos.sara.bubble.manager.BubbleThemeManager;
import com.smartisanos.sara.bubble.revone.fragment.GlobalRemindAlarmSettingFragment;
import com.smartisanos.sara.BubbleActionUpHelper;
import com.smartisanos.sara.R;
import com.smartisanos.sara.bubble.view.FakeBubbleView;
import com.smartisanos.sara.bubble.view.IFakeBubbleView;
import com.smartisanos.sara.storage.BubbleDataRepository;
import com.smartisanos.sara.storage.DrawerDataRepository;
import com.smartisanos.sara.util.ViewUtils;
import com.smartisanos.sara.widget.IBubbleHeightChangeListener;
import com.smartisanos.sara.widget.listener.IVoiceOperationListener;
import com.smartisanos.sara.bubble.revone.view.ButtonViewManager;
import com.smartisanos.sara.bubble.revone.manager.StartedAppManager;
import com.smartisanos.sara.util.AnimManager;
import com.smartisanos.sara.util.AttachmentUtils;
import com.smartisanos.sara.util.LogUtils;
import com.smartisanos.sara.util.SaraConstant;
import com.smartisanos.sara.util.SaraUtils;
import com.smartisanos.sara.entity.SpeechError;
import com.smartisanos.sara.util.StringUtils;
import com.smartisanos.sara.util.ToastUtil;
import com.smartisanos.sara.widget.BubbleItemView;
import com.smartisanos.sara.widget.WaveView;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import smartisanos.app.voiceassistant.ParcelableObject;
import smartisanos.util.Utils;
import smartisanos.util.LogTag;
import smartisanos.widget.support.SmartisanPopupMenu;

public class GlobalBubbleCreateActivity extends AbstractRevVoiceActivity implements SaraUtils.BubbleViewChangeListener, WaveView.AnimationListener,
        GlobalRemindAlarmSettingFragment.IRemindSettingListener {

    public static final String TAG = "VoiceAss.GlobalBubbleCreateActivity";

    private static final int MSG_START_UI = 3;
    private static final int MSG_CREATE_WAV_ROOT_FILE = 5;
    private static final int MSG_HIDE_GLOBLE_BUBBLE = 6;
    private static final int MSG_FINISH_ACTIVITY_ANIM = 7;
    private static final int MSG_NO_RESULT_TIP = 8;

    private static final int BUBBLE_STATUS_NORMAL = 0;
    private static final int BUBBLE_STATUS_HIDDING = 1;
    private int mLaunchType = ExtScreenConstant.LAUNCH_FROM_LONG_PRESS;
    private ImageView mIvGaussBlur;
    private BubbleItemView mBubbleView;
    private View mWaveLayout;
    private View mTextPopup;
    private WaveView mWaveForm;
    private View mPopupBottomArrow;
    private View mRootView;
    private IFakeBubbleView mFakeBubbleView;

    private StringBuffer result = new StringBuffer();
    private GlobalBubble mGlobalBubble;
    protected boolean mPendingFinishing;
    private boolean mIsOffLine = false;
    private int mBubbleStatus = BUBBLE_STATUS_NORMAL;
    private boolean mIsMaxWave = false;

    private int mScreenWidth;
    private int mDisplayWidth;
    private int mDisplayHeight;
    private int mWaveResultMarginBottom;
    private int mWaveBottomMarginBottom;
    private int mBubbleWaveMinWidth;
    private int mBubbleLeftWaveMinWidth;
    private int mWaveResultMarginLeft;
    private int mBubbleWaveDefaultWidth;
    private int mBubbleDefaultWidthCalibration;
    private int mBubbleLeftMargin;
    private ButtonViewManager mButtonViewManager;
    private StartedAppManager mStartedAppManager;
    private int mLaunchButtonPos = -1;
    private int mLastButtonPos = 0;
    private boolean mIsLeftBubble = true;
    //    private GlobalRemindAlarmSettingFragment mRemindFragment;
    private BubbleActionUpHelper.MenuActionUpListener mActionUpListener = new BubbleActionUpHelper.MenuActionUpListener() {
        @Override
        public void onActionUp() {
            LogTag.d(TAG, "ACTION_MENU_UP");
            if (mLaunchType == ExtScreenConstant.LAUNCH_FROM_LONG_PRESS) {
                endKeyOrBlueRec();
            }
        }
    };
    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (SaraConstant.ACTION_UPDATE_BUBBLE.equals(action)) {
                resultFromIntentUpdate(intent);
            } else if (SaraConstant.ACTION_IDEAPILLS_SHARE_INVITATION_SEND.equals(action)) {
                updateBubble2Share();
            } else if (Intent.ACTION_CLOSE_SYSTEM_DIALOGS.equals(action)) {
                String reason = intent.getStringExtra("reason");
                if (SaraConstant.ACTION_REASON_HOME_KEY.equals(reason)) {
                    BubbleItemView bubbleItemView = getBubbleView();
                    if (!isStopped() && mGlobalBubble != null && BubbleManager.isAddBubble2List()) {
                        bubbleItemView.addBubble2SideBar(true);
                    }
                    SaraUtils.overridePendingTransition(GlobalBubbleCreateActivity.this, true);
                    finishAll();
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
            } else if (StartedAppManager.APPLICATION_SELECT.equals(action)) {
                if (mStartedAppManager != null) {
                    mStartedAppManager.closeStartedApp(mStartedPackageList, true, null);
                }
            } else if (ExtScreenConstant.ACTION_CLOSE_BUBBLE.equals(action)) {
                saveBubbleWithAnim(true, false);
            }
        }
    };
    private CustomHandler mCustomHandler = new CustomHandler(this);

    private static class CustomHandler extends Handler {
        private final WeakReference<GlobalBubbleCreateActivity> mActivity;

        public CustomHandler(GlobalBubbleCreateActivity activity) {
            mActivity = new WeakReference<GlobalBubbleCreateActivity>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            GlobalBubbleCreateActivity activity = mActivity.get();
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
                        activity.finishAll();
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
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!checkSettingEnabled()) {
            return;
        }
        mStartedAppManager = new StartedAppManager(this);
        mStartedAppManager.registerTaskListener();
        updateLaunchInfo(getIntent());
        RevActivityManager.INSTANCE.attach(this);
        mPendingFinishing = false;
        BubbleActionUpHelper.INSTANCE.addActionUpListener(mActionUpListener);
        IntentFilter filter = new IntentFilter(SaraConstant.ACTION_RECORD_ERROR);
        filter.addAction(SaraConstant.ACTION_UPDATE_BUBBLE);
        filter.addAction(SaraConstant.ACTION_IDEAPILLS_SHARE_INVITATION_SEND);
        filter.addAction(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
        filter.addAction(SaraConstant.ACTION_CHOOSE_RESULT);
        filter.addAction(StartedAppManager.APPLICATION_SELECT);
        filter.addAction(ExtScreenConstant.ACTION_CLOSE_BUBBLE);
        registerReceiver(mReceiver, filter);
        DrawerDataRepository.INSTANCE.reloadAsync();
        SaraUtils.buildWavRootPathAsync(this);
        LayoutInflater inflater = LayoutInflater.from(this);
        initDimens();
        setContentView(R.layout.revone_bubble_create_act);
        initView(inflater);
        preStartVoiceRecognition(true);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        LogUtils.infoRelease(TAG, "onNewIntent():" + getRecordStatus());
        super.onNewIntent(intent);
        if (!checkSettingEnabled()) {
            return;
        }
        updateLaunchInfo(intent);
        mPendingFinishing = false;
        if (isRecognizing()) {
            stopRecognize(false);
        } else {
            mRecordStatus = RECORD_STATUS_IDLE;
            clearAnimation();
            mGlobalBubble = null;
            DrawerDataRepository.INSTANCE.reloadAsync();
            BubbleItemView bubbleItemView = getBubbleView();
            bubbleItemView.setAttachmentList(null);
            bubbleItemView.setVisibility(View.GONE);
            preStartVoiceRecognition(false);
        }
    }

    private boolean checkSettingEnabled() {
        if (!SaraUtils.isSettingEnable(this)) {
            ToastUtil.showToast(this, R.string.ideapills_not_opened, Toast.LENGTH_SHORT);
            finish();
            return false;
        }
        return true;
    }

    private void updateLaunchInfo(Intent intent) {
        mLaunchType = intent.getIntExtra(ExtScreenConstant.LAUNCH_TYPE, ExtScreenConstant.LAUNCH_FROM_LONG_PRESS);
        mLaunchButtonPos = intent.getIntExtra(ExtScreenConstant.LAUNCH_BUTTON_POS, -1);
        mIsLeftBubble = mLaunchButtonPos > 0 ? false : true;
        if (mLastButtonPos != mLaunchButtonPos) {
            mLastButtonPos = mLaunchButtonPos;
            clearWaveLayout();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        LogTag.d(TAG, "onResume()");
        mCustomHandler.removeMessages(MSG_FINISH_ACTIVITY_ANIM);
        if (mBubbleView != null) {
            mBubbleView.onResume();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        BubbleItemView bubbleItemView = getBubbleView();
        if (BubbleManager.isAddBubble2List() && mGlobalBubble != null) {
            if (SaraUtils.isTopSelfApp(this) && !isLaunchFromSendResult()) {
                saveBubbleWithAnim(false, false);
            }
        }
        bubbleItemView.checkInput(false);
        bubbleItemView.hideSoftInputFromWindow();
        mBubbleStatus = BUBBLE_STATUS_NORMAL;
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (isRecognizing()) {
            stopRecognize(true);
        }
        if (!isFinishing()) {
            mCustomHandler.sendEmptyMessageDelayed(MSG_FINISH_ACTIVITY_ANIM, 100);
        }
    }

    @Override
    public void onDestroy() {
        LogTag.d(TAG, "onDestroy()");
        if (mStartedAppManager != null) {
            mStartedAppManager.unRegisterTaskListener();
        }
        mCustomHandler.removeCallbacksAndMessages(null);
        try {
            BubbleActionUpHelper.INSTANCE.removeActionUpListener(mActionUpListener);
            unregisterReceiver(mReceiver);
        } catch (Exception e) {
        }
        super.onDestroy();
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_FORWARD_DEL:
                finishAll();
                return true;
        }
        return super.onKeyUp(keyCode, event);
    }

    public void finishAll() {
        if (mStartedAppManager != null) {
            mStartedAppManager.closeStartedApp(mStartedPackageList, true, null);
        }
    }

    private void saveBubbleWithAnim(boolean finish, boolean sleep) {
        PointF point = null;
        BubbleItemView bubbleItemView = getBubbleView();
        if (mGlobalBubble != null && !TextUtils.isEmpty(mGlobalBubble.getText())) {
            point = BubbleManager.addBubble2SideBar(this, mGlobalBubble, bubbleItemView.getAttachmentList(), mIsOffLine, false);
        }
        hideView(0, point, finish, sleep);
    }

    @Override
    protected boolean allowStartActivity(ComponentName component) {
        if (mStartedAppManager != null) {
            return !mStartedAppManager.hasStartActivity(component);
        }
        return true;
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
        }
    }

    public void preStartVoiceRecognition(boolean createFlag) {
        if (isFinishing()) {
            return;
        }
        hideUpperWindows();
        startRecognize();
        if (createFlag) {
            mCustomHandler.sendEmptyMessageDelayed(MSG_START_UI, 200);
        } else {
            mCustomHandler.sendEmptyMessage(MSG_START_UI);
        }
        registerVoiceCallback();
    }

    void endKeyOrBlueRec() {
        if (getRecordStatus() == RECORD_STATUS_IDLE) {
            finish();
        }
        stopRecognize(false);
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
        mIvGaussBlur.setVisibility(View.VISIBLE);
        View waveView = getWaveLayout();
        waveView.clearAnimation();
        waveView.setVisibility(View.GONE);

        if (!isWaitingRecordResult()) {
            LogUtils.d(TAG, "onResultRecived ignore:" + getRecordStatus());
            return;
        }
        if ((TextUtils.isEmpty(resultStr) || (resultStr.length() == 1 && StringUtils.isChinesePunctuation(resultStr.charAt(0))))
                && TextUtils.isEmpty(result.toString())) {
            postShowNoResultView();
        } else {
            mCustomHandler.removeMessages(MSG_START_UI);
            okResultHandled();
            mIsOffLine = offline;
            result.append(resultStr);
            optimizeResultPunctutation(result);
            if (mIsMaxWave) {
                playPopup2TextAnim();
                mWaveForm.stopAnimation(true);
            }
            formatBubble(mIsOffLine);
            loadResult(true, true);
        }
    }

    @Override
    protected void resultTimeOut() {
        //showNoResultViewDuringRecording(-1);
    }

    @Override
    protected void buffer(byte[] buffer, int totalPoint) {
        if (mWaveForm != null) {
            mWaveForm.waveChanged(buffer, totalPoint);
        }
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
            stopWaveAnim(true);
            postShowNoResultView();
        } else {
            stopWaveAnim(false);
        }
    }

    @Override
    public void hideView(int from, PointF point, final boolean finish, boolean needSleep) {
        int[] positionBubble = new int[2];
        BubbleItemView bubbleItemView = getBubbleView();
        View waveLayout = getWaveLayout();
        waveLayout.setVisibility(View.GONE);
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
                mFakeBubbleView.initFakeAnim(mGlobalBubble, bubbleItemView.getAttachmentList(), mScreenWidth);

                int[] position = new int[2];
                bubbleItemView.getLocationOnScreen(position);
                mFakeBubbleView.setFakeAnimOffset(position[0], position[1],
                        bubbleItemView.getMeasuredWidth(), bubbleItemView.getMeasuredHeight());
                int targetX = mFakeBubbleView.getFakeAnimTargetX();

                int translateX = targetX - position[0];
                bubbleItemView.hideViewWithScaleAnim(point, mFakeBubbleView.getFakeBubbleBgWidth(),
                        mFakeBubbleView.getFakeBubbleBgHeight(), translateX, position[1]);
                mFakeBubbleView.startFakeAnim(0, new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        if (finish) {
                            Message message = mCustomHandler.obtainMessage(MSG_FINISH_ACTIVITY_ANIM);
                            mCustomHandler.sendMessage(message);
                        }
                    }
                });
            } else {
                AnimManager.HideViewWithAlphaAnim(bubbleItemView, 0, 250);
            }

            Message message = mCustomHandler.obtainMessage(MSG_FINISH_ACTIVITY_ANIM);
            if (point == null) {
                mCustomHandler.sendMessageDelayed(message, 250);
            }
        } else if (from == 1) {
            if (bubbleItemView.getVisibility() == View.VISIBLE) {
                AnimManager.HideViewWithAlphaAnim(bubbleItemView, 0, 200);
            }
        } else if (from == 2) {
            if (bubbleItemView.getVisibility() == View.VISIBLE) {
                AnimManager.HideViewWithAlphaAnim(bubbleItemView, 0, 200);
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
    public void loadResultForKeyboard(GlobalBubble bubble, boolean isSmallEdit, boolean needScaleAnim) {
        String result = bubble.getText();
        BubbleItemView bubbleItemView = getBubbleView();
        List<GlobalBubbleAttach> attachmentList = bubbleItemView.getAttachmentList();
        if ((result != null && !TextUtils.isEmpty(result.trim())) || (attachmentList != null && attachmentList.size() > 0)) {
            mGlobalBubble = bubble;
            loadResult(true, needScaleAnim);
        } else {
            finish();
        }
    }

    @Override
    public void editView(boolean keyInvisible, boolean isSmallEdit) {

    }

    @Override
    public Activity getActivityContext() {
        return this;
    }

    @Override
    public int getBubbleSmallTranslation(int bubbleHeight) {
        int targetY = 0;
        if (ExtScreenConstant.CONTAINS_STATUS_NAVI_BAR) {
            targetY = (bubbleHeight - mDisplayHeight - ExtScreenConstant.STATUS_BAR_HEIGHT - ExtScreenConstant.NAVIGATION_BAR_HEIGHT) / 2;
        } else {
            targetY = (bubbleHeight - mDisplayHeight) / 2;
        }
        return targetY;
    }

    @Override
    public int getBubbleLargeTranslation(int bubbleHeight) {
        return getBubbleSmallTranslation(bubbleHeight);
    }

    @Override
    public int getBubbleKeyboardTranslation(int keyboardHeight) {
        return -keyboardHeight;
    }

    @Override
    protected void realStartRecognize() {
        super.realStartRecognize();
        result = new StringBuffer();
        mIsOffLine = false;
    }

    @Override
    public void onAnimationEnd(int witdh, boolean isCanceled) {
        if (isStopped()) {
            return;
        }
        mIsMaxWave = true;
        LogTag.d(TAG, "mIsMaxWave is true");
        if (!isCanceled) {
            playPopup2TextAnim();
        }
    }

    @Override
    public void onAnimationCancel(int width) {
    }

    @Override
    public void onAnimationUpdate(int width) {
        if (mLaunchType != ExtScreenConstant.LAUNCH_FROM_CLICK) {
            setParamWidth(width);
        }
    }

    protected void formatBubble(boolean offline) {
        SaraUtils.recordFile(this, offline, result.toString());
        int type = offline ? GlobalBubble.TYPE_VOICE_OFFLINE : GlobalBubble.TYPE_VOICE;
        mGlobalBubble = SaraUtils.toGlobalBubble(this, result.toString(), type, SaraUtils.getUri(this),
                SaraUtils.getDefaultBubbleColor(this), 0, 0);
        BubbleManager.markAddBubble2List(true);
    }

    protected void loadResult(boolean transAnim, boolean scaleAnim) {
        BubbleItemView bubbleItemView = getBubbleView();
        if (mGlobalBubble != null) {
            bubbleItemView.setGlobalBubble(mGlobalBubble, mIsOffLine);
        }
        bubbleItemView.setVisibility(View.VISIBLE);
        bubbleItemView.setBubbleState(BubbleItemView.BubbleSate.LARGE, transAnim, false, false, scaleAnim);
        bubbleItemView.setEditable(true);
//        showRemindView();
        mButtonViewManager.show();
    }

//    private void showRemindView() {
//        FragmentTransaction ft = getFragmentManager().beginTransaction();
//        if (mRemindFragment == null) {
//            mRemindFragment = new GlobalRemindAlarmSettingFragment();
//            ft.add(R.id.bubble_reminder_container, mRemindFragment);
//            ft.show(mRemindFragment);
//            ft.commit();
//        } else {
//            long current = System.currentTimeMillis();
//            mRemindFragment.showView(current, current);
//        }
//    }

    public void onRemindTimeSet(long dueDate, long remindTime) {
        if (mGlobalBubble != null) {
            mGlobalBubble.setRemindTime(remindTime);
            mGlobalBubble.setDueDate(dueDate);
        }
        BubbleItemView bubbleItemView = getBubbleView();
        bubbleItemView.setGlobalBubble(mGlobalBubble);
        bubbleItemView.refreshRemindView();
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

    protected void stopWaveAnim(boolean shortPress) {
        if (mWaveForm != null) {
            mWaveForm.stopAnimation(shortPress);
        }
    }

    protected void hideTextPopup() {
        if (mWaveLayout != null) {
            mTextPopup.setVisibility(View.GONE);
            mWaveForm.setVisibility(View.GONE);
            mPopupBottomArrow.setVisibility(View.GONE);
        }
        if (mBubbleView != null) {
            LogTag.d(TAG, "bubbleItemView GONE hideTextPopup ");
            mBubbleView.setVisibility(View.GONE);
        }
    }

    public void startUI() {
        LogTag.d(TAG, "enter startUI()");
        result = new StringBuffer();
        resetView(true, true);
        mCustomHandler.sendEmptyMessage(MSG_HIDE_GLOBLE_BUBBLE);
    }

    private void showNoResultViewDuringRecording(final int errorCode) {
        //mButtonViewManager.show();
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

    private void showNoResultView(int errorCode) {
        int resId = R.string.recognize_no_result_tip;
        switch (errorCode) {
            case SpeechError.ERROR_AUDIO_RECORD:
                resId = R.string.rev_record_error_tip;
                break;
            case SpeechError.ERROR_INTERRUPT:
            case SpeechError.ERROR_FACADE_BUSY:
                resId = R.string.voice_search_interrupt;
                break;
        }
        ToastUtil.showToast(this, resId, Toast.LENGTH_SHORT);
        finish();
    }

    public void playPopup2TextAnim() {
        if (mIsMaxWave && mIsLeftBubble) {
            int color = SaraUtils.getDefaultBubbleColor(this);
            if (mPopupBottomArrow != null) {
                mPopupBottomArrow.setBackgroundResource(BubbleThemeManager.getBackgroudRes(color, BubbleThemeManager.BACKGROUND_BUBBLE_ARROW_lARGE));
            }
        }
        BubbleItemView bubbleItemView = getBubbleView();
        if (!TextUtils.isEmpty(result)) {
            mTextPopup.setVisibility(View.GONE);
            bubbleItemView.setText(result.toString());
            bubbleItemView.setBubbleState(BubbleItemView.BubbleSate.NORMAL, true, false, false);
        } else {
            if (mIsMaxWave) {
                mTextPopup.setVisibility(View.GONE);
                bubbleItemView.setBubbleState(BubbleItemView.BubbleSate.LOADING, true, false, false);
                AnimManager.HideViewWithAlphaAnim(mTextPopup, AnimManager.HIDE_BUBBLE_TEXT_POPUP_DURATION, 0);
            }
        }
    }

    public void setParamWidth(int width) {
        BubbleItemView bubbleItemView = getBubbleView();
        FrameLayout.LayoutParams bubbleParams = (FrameLayout.LayoutParams) bubbleItemView.getLayoutParams();
        View waveLayout = getWaveLayout();
        if (!mIsLeftBubble) {
            LayoutParams waveParams = (LayoutParams) waveLayout.getLayoutParams();
            waveParams.leftMargin = mLaunchButtonPos;
            waveParams.leftMargin = Math.max(mLaunchButtonPos - width / 2, -bubbleItemView.getPaddingLeft());
            bubbleParams.leftMargin = waveParams.leftMargin;
            LayoutParams arrowParams = (LayoutParams) mPopupBottomArrow.getLayoutParams();
            int arrowWidth = mPopupBottomArrow.getWidth();
            if (arrowWidth <= 0) {
                int spec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
                mPopupBottomArrow.measure(spec, spec);
                arrowParams.width = arrowWidth = mPopupBottomArrow.getMeasuredWidth();
                arrowParams.height = mPopupBottomArrow.getMeasuredHeight();
            }
            arrowParams.leftMargin = Math.max(mLaunchButtonPos - arrowWidth / 2 - waveParams.leftMargin, arrowWidth);
            mPopupBottomArrow.setLayoutParams(arrowParams);
        }
        LayoutParams waveFormParams = (LayoutParams) mWaveForm.getLayoutParams();
        if (waveFormParams.width != width) {
            waveFormParams.width = width;
            mWaveForm.setLayoutParams(waveFormParams);
        }
        FrameLayout.LayoutParams textPopupParams = (FrameLayout.LayoutParams) mTextPopup.getLayoutParams();
        if (textPopupParams.width != width) {
            textPopupParams.width = width;
            mTextPopup.setLayoutParams(textPopupParams);
        }
        bubbleParams.width = width + getResources().getDimensionPixelOffset(R.dimen.nomal_bubble_left_margin);
        bubbleItemView.setLayoutParams(bubbleParams);
    }

    private void resetView(boolean initState, boolean hideBubble) {
        final boolean fromClick = mLaunchType == ExtScreenConstant.LAUNCH_FROM_CLICK;
        mIvGaussBlur.setVisibility(View.GONE);
        int color = SaraUtils.getDefaultBubbleColor(this);
        resetBubbleView(color, fromClick);
        resetWaveLayout(color, fromClick);
        setParamWidth(fromClick ? mBubbleWaveDefaultWidth : mIsLeftBubble ? mBubbleLeftWaveMinWidth : mBubbleWaveMinWidth);
        mIsMaxWave = false;
    }

    private void resetBubbleView(int color, boolean fromClick) {
        BubbleItemView bubbleItemView = getBubbleView();
        LogTag.d(TAG, "bubbleItemView GONE resetView ");
        if (fromClick) {
            bubbleItemView.updateBubbleLayoutParam(mBubbleWaveDefaultWidth + mBubbleDefaultWidthCalibration, mBubbleLeftMargin,
                    mIsLeftBubble ? mWaveResultMarginBottom : mWaveBottomMarginBottom);
            bubbleItemView.showVoiceInputButton();
            bubbleItemView.setBackgroundResource(BubbleThemeManager.getBackgroudRes(color, BubbleThemeManager.BACKGROUND_BUBBLE_LARGE));
            bubbleItemView.setVisibility(View.VISIBLE);
        } else {
            bubbleItemView.resetBubbleLayoutPrama();
            bubbleItemView.setBackgroundResource(BubbleThemeManager.getBackgroudRes(color, BubbleThemeManager.BACKGROUND_BUBBLE_NORMAL));
            bubbleItemView.setVisibility(View.GONE);
            bubbleItemView.checkInput(false);
            bubbleItemView.hideSoftInputFromWindow();
        }
        bubbleItemView.setBubbleState(BubbleItemView.BubbleSate.INIT, true, false, false);
        bubbleItemView.setEditable(false);
    }

    private void resetWaveLayout(int color, boolean fromClick) {
        View waveLayout = getWaveLayout();
        waveLayout.setVisibility(View.VISIBLE);
        mPopupBottomArrow.setBackgroundResource(mIsLeftBubble
                ? fromClick ? BubbleThemeManager.getWaveLargeArrowRes(color) : BubbleThemeManager.getWaveArrowRes(color, mIsLeftBubble)
                : BubbleThemeManager.getWaveArrowRes(color, mIsLeftBubble));
        mTextPopup.setBackgroundResource(BubbleThemeManager.getBackgroudRes(color, BubbleThemeManager.BACKGROUND_BUBBLE_NORMAL));
        waveLayout.setTranslationY(0);
        LayoutParams params = new LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.gravity = Gravity.LEFT | Gravity.BOTTOM;
        params.leftMargin = mWaveResultMarginLeft;
        params.bottomMargin = mIsLeftBubble ? mWaveResultMarginBottom : mWaveBottomMarginBottom;
        if (fromClick) {
            mWaveForm.setWaveMaxWidth(mBubbleWaveDefaultWidth);
            mWaveForm.setWaveMinWidth(mBubbleWaveDefaultWidth);
            mWaveForm.updateLeftPadding(mIsLeftBubble);
            if (!mIsLeftBubble) {
                BubbleItemView bubbleView = getBubbleView();
                params.height = bubbleView.getBubbleInitHeight();
            }
        } else {
            mWaveForm.updateWidthPrama(mIsLeftBubble);
            if (mIsLeftBubble) {
                startWaveLayoutAnim(0);
            }
        }
        waveLayout.setLayoutParams(params);
        resetWaveFrom();
        mWaveForm.setAlpha(1);
        waveLayout.setAlpha(1);
        mPopupBottomArrow.setAlpha(1);
        mPopupBottomArrow.setVisibility(View.VISIBLE);
        mTextPopup.setAlpha(1);
        mTextPopup.setVisibility(fromClick ? View.GONE : View.VISIBLE);
        mWaveForm.setVisibility(View.VISIBLE);
    }

    private void startWaveLayoutAnim(long delay) {
        AnimationSet shrink = (AnimationSet) AnimationUtils.loadAnimation(this, R.anim.revone_bubble_left_anim);
        shrink.setStartOffset(delay);
        View waveLayout = getWaveLayout();
        waveLayout.startAnimation(shrink);
    }

    public void resetWaveFrom() {
        if (mWaveForm != null) {
            mWaveForm.reset(mIsLeftBubble);
        }
    }

    public void setBubbleState(BubbleItemView.BubbleSate state, boolean isSmallEdit, boolean isAnim) {
        setBubbleState(state, isSmallEdit, isAnim, false);
    }

    public void setBubbleState(BubbleItemView.BubbleSate state, boolean isSmallEdit, boolean isAnim, boolean needScaleAnim) {
        LogTag.d(TAG, "setBubbleState state:" + state);
        BubbleItemView bubbleItemView = getBubbleView();
        if (isAnim) {
            bubbleItemView.setBubbleState(state, true, isSmallEdit, true, needScaleAnim);
        } else {
            int targetY = 0;
            int itemHeight = 0;
            if (bubbleItemView.getLastBubbleState() == BubbleItemView.BubbleSate.SMALL
                    || bubbleItemView.getLastBubbleState() == BubbleItemView.BubbleSate.LARGE) {
                itemHeight = bubbleItemView.getExactHeight();
            } else if (bubbleItemView.getLastBubbleState() == BubbleItemView.BubbleSate.NORMAL
                    || bubbleItemView.getLastBubbleState() == BubbleItemView.BubbleSate.LOADING) {
                itemHeight = ViewUtils.getExactHeight(bubbleItemView,
                        bubbleItemView.getLayoutParams().width);
            } else {
                itemHeight = ViewUtils.getSupposeHeightNoFixWidth(bubbleItemView);
            }
            if (state == BubbleItemView.BubbleSate.SMALL || state == BubbleItemView.BubbleSate.LARGE) {
                targetY = getBubbleLargeTranslation(itemHeight);
            }
            FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) bubbleItemView.getLayoutParams();
            params.height = itemHeight;
            bubbleItemView.setLayoutParams(params);
            bubbleItemView.setTranslationY(targetY);
        }
        View waveLayout = getWaveLayout();
        waveLayout.setVisibility(View.GONE);
    }

    private void resultFromIntentUpdate(Intent intent) {
        StringBuffer newBubbleText = BubbleManager.updateBubbleFromIntent(intent, mGlobalBubble);
        if (newBubbleText != null) {
            result = newBubbleText;
        }
    }

    private void updateBubble2Share() {
        BubbleItemView bubbleItemView = getBubbleView();
        if (mGlobalBubble != null) {
            bubbleItemView.changeColor2Share();
        }
    }

    public void initView(LayoutInflater inflater) {
        Resources res = getResources();
        Display display = getDisplay();
        Point realSize = new Point();
        display.getRealSize(realSize);
        mScreenWidth = realSize.x;
        mDisplayHeight = realSize.y;
        mRootView = findViewById(R.id.root_view);
        mIvGaussBlur = (ImageView) findViewById(R.id.gauss_blur);
        WallpaperUtils.gaussianBlurWallpaper(this, mIvGaussBlur);
        mFakeBubbleView = new FakeBubbleView(this, (ViewStub) findViewById(R.id.fak_bubble_stub), 250);
        mButtonViewManager = new ButtonViewManager(this, mRootView, mCloseListener, mSettingListener);
    }

    private BubbleItemView getBubbleView() {
        if (mBubbleView == null) {
            initBubbleView();
        }
        return mBubbleView;
    }

    public void initBubbleView() {
        ViewStub bubbleStub = (ViewStub) findViewById(R.id.bubble_item_stub);
        if (bubbleStub != null && bubbleStub.getParent() != null) {
            mBubbleView = (BubbleItemView) bubbleStub.inflate();
        } else {
            mBubbleView = (BubbleItemView) findViewById(R.id.bubble_item);
        }
        mBubbleView.updateTargetWidth(mDisplayWidth);
        mBubbleView.setBubbleMarginX((mScreenWidth - mDisplayWidth) / 2);
        mBubbleView.setViewListener(this);
        mBubbleView.setSoftListener(this);
        mBubbleView.setVoiceOperationListener(mVoiceOperationListener);
        mBubbleView.setBubbleHeightChangeListener(mBubbleHeightChangeListener);
        mBubbleView.setBubbleClickListener(new BubbleItemView.OnBubbleClickListener() {
            public void onAddAttachmentClick() {
                SaraUtils.startAttachementChoose(GlobalBubbleCreateActivity.this);
            }

            public void onAttachmentChanged() {
                mBubbleView.refreshAttachmentView();
                setBubbleState(BubbleItemView.BubbleSate.LARGE, false, false);
            }

            public void onImageAttchmentClick(GlobalBubbleAttach globalBubbleAttach, ArrayList<Uri> localUris) {
                SaraUtils.startImagePreview(GlobalBubbleCreateActivity.this, globalBubbleAttach, localUris);
            }

            public void onFileClick(GlobalBubbleAttach globalBubbleAttach) {
                SaraUtils.startFilePreview(GlobalBubbleCreateActivity.this, globalBubbleAttach);
            }
        });
    }

    private void clearWaveLayout() {
        if (mWaveLayout != null) {
            mWaveLayout.setVisibility(View.GONE);
            mWaveLayout = null;
        }
    }

    private View getWaveLayout() {
        if (mWaveLayout == null) {
            initWaveLayout();
        }
        return mWaveLayout;
    }

    public void initWaveLayout() {
        ViewStub waveStub = (ViewStub) findViewById(mIsLeftBubble ? R.id.wave_left_stub : R.id.wave_bottom_stub);
        if (waveStub != null && waveStub.getParent() != null) {
            mWaveLayout = waveStub.inflate();
        } else {
            mWaveLayout = findViewById(mIsLeftBubble ? R.id.wave_left : R.id.wave_bottom);
        }
        mTextPopup = mWaveLayout.findViewById(R.id.text_popup);
        mWaveForm = (WaveView) mWaveLayout.findViewById(R.id.waveform);
        mWaveForm.setAnimationListener(this);
        mWaveForm.setWaveType(SaraUtils.WaveType.START_WAVE);
        mPopupBottomArrow = mWaveLayout.findViewById(R.id.popup_bottom_arrow);
    }

    public void initDimens() {
        Resources res = getResources();
        mDisplayWidth = res.getDimensionPixelSize(R.dimen.revone_bubble_width);
        mWaveResultMarginBottom = res.getDimensionPixelSize(R.dimen.global_bubble_wave_margin_bottom);
        mWaveBottomMarginBottom = res.getDimensionPixelSize(R.dimen.revone_wave_bottom_margin_bottom);
        mBubbleLeftWaveMinWidth = res.getDimensionPixelSize(R.dimen.bubble_wave_left_min_width);
        mBubbleWaveMinWidth = res.getDimensionPixelSize(R.dimen.bubble_wave_min_width);
        mWaveResultMarginLeft = res.getDimensionPixelSize(R.dimen.wave_result_margin_left);
        mBubbleWaveDefaultWidth = res.getDimensionPixelSize(R.dimen.global_search_bubble_default_width);
        mBubbleDefaultWidthCalibration = res.getDimensionPixelSize(R.dimen.global_bubble_large_mode_width_calibration);
        mBubbleLeftMargin = res.getDimensionPixelSize(R.dimen.global_bubble_large_mode_margin_left);
    }

    public boolean clearAnimation() {
        boolean isClearAnimation = false;
        BubbleItemView bubbleItemView = getBubbleView();
        if (bubbleItemView.getAnimation() != null && !bubbleItemView.getAnimation().hasEnded()) {
            isClearAnimation = true;
        }
        bubbleItemView.clearAnimation();
        if (bubbleItemView.isAnimSetRunning()) {
            isClearAnimation = true;
        }
        bubbleItemView.cancelAnimSet();

        View waveLayout = getWaveLayout();
        if (waveLayout.getAnimation() != null && !waveLayout.getAnimation().hasEnded()) {
            isClearAnimation = true;
        }
        waveLayout.clearAnimation();
        if (mWaveForm.getAnimation() != null && !mWaveForm.getAnimation().hasEnded()) {
            isClearAnimation = true;
        }
        mWaveForm.clearAnimation();
        mWaveForm.stopAnimation(true);
        return isClearAnimation;
    }

    private void handleImagePreviewResult(Intent data) {
        ClipData cd = data.getClipData();
        if (cd == null) {
            return;
        }
        BubbleItemView bubbleItemView = getBubbleView();
        List<GlobalBubbleAttach> listAttach = AttachmentUtils.mergeImageAttachList(
                GlobalBubbleCreateActivity.this, bubbleItemView.getAttachmentList(), cd);
        bubbleItemView.setAttachmentList(listAttach);
        bubbleItemView.refreshAttachmentView();
    }

    private void handleAttachmentPickResult(Intent data) {
        if (data == null || mGlobalBubble == null) {
            LogUtils.e("handleAttachmentPickResult return by data or bubble is null");
            return;
        }
        BubbleItemView bubbleItemView = getBubbleView();
        List<GlobalBubbleAttach> realAttachList =
                AttachmentUtils.mergeAttachmentList(GlobalBubbleCreateActivity.this, bubbleItemView.getAttachmentList(), data);
        bubbleItemView.setAttachmentList(realAttachList);
        bubbleItemView.refreshAttachmentView();
        setBubbleState(BubbleItemView.BubbleSate.LARGE, false, false);
    }

    private void handleRemindViewResult(Intent data) {
        BubbleItemView bubbleItemView = getBubbleView();
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

    private IVoiceOperationListener mVoiceOperationListener = new IVoiceOperationListener() {
        @Override
        public void onCancel() {
            finishAll();
        }

        @Override
        public void onDone() {
            stopRecognize(false);
        }
    };

    private ButtonViewManager.ICloseListener mCloseListener = new ButtonViewManager.ICloseListener() {
        @Override
        public void onClose() {
            finishAll();
        }
    };

    private ButtonViewManager.ISettingListener mSettingListener = new ButtonViewManager.ISettingListener() {
        @Override
        public void showSettingWindow(View view) {
            int width = (int) getResources().getDimension(R.dimen.popup_activity_width);
            int height = (int) getResources().getDimension(R.dimen.popup_activity_height);
            Intent intent = new Intent(GlobalBubbleCreateActivity.this, SettingActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
            intent.putExtra(SaraConstant.HOLD_CURRENT_ACTIVITY, true);
            Utils.startPopupActivity(GlobalBubbleCreateActivity.this, view, intent, width, height, SmartisanPopupMenu.DIRECTION_TOP, 0.85f);
        }
    };

    private IBubbleHeightChangeListener mBubbleHeightChangeListener = new IBubbleHeightChangeListener() {
        @Override
        public boolean needPerformAnimator() {
            return !mIsLeftBubble;
        }

        @Override
        public Animator getWaveHeightAnimator(int targetHeight) {
            final View waveLayout = getWaveLayout();
            int height = mWaveLayout.getHeight();
            if (height != targetHeight) {
                ValueAnimator heightAnimator = ValueAnimator.ofFloat(height, targetHeight);
                heightAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                    @Override
                    public void onAnimationUpdate(ValueAnimator animation) {
                        float value = (Float) animation.getAnimatedValue();
                        FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) waveLayout.getLayoutParams();
                        params.height = (int) value;
                        waveLayout.setLayoutParams(params);
                    }
                });
                return heightAnimator;
            }
            return null;
        }

        @Override
        public Animator getWaveTransAnimator(int deltaTransY) {
            return null;
        }

        @Override
        public void updateWaveHeight(int targetHeight) {
            final View waveLayout = getWaveLayout();
            FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) waveLayout.getLayoutParams();
            params.height = targetHeight;
            waveLayout.setLayoutParams(params);
        }
    };
}
