package com.smartisanos.sara.bubble.revone;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
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
import android.support.v4.content.FileProvider;
import android.text.TextUtils;
import android.view.Display;
import android.view.DragEvent;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.AnimationSet;
import android.view.animation.AnimationUtils;
import android.view.animation.DecelerateInterpolator;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.FrameLayout.LayoutParams;
import android.widget.ImageView;
import android.widget.Toast;

import com.smartisanos.ideapills.common.util.MultiSdkUtils;
import com.smartisanos.ideapills.common.util.PackageUtils;
import com.smartisanos.ideapills.common.util.CommonUtils;
import com.smartisanos.ideapills.common.util.UIHandler;
import com.smartisanos.ideapills.common.util.WallpaperUtils;
import com.smartisanos.sara.BubbleActionUpHelper;
import com.smartisanos.sara.R;
import com.smartisanos.sara.bubble.manager.BubbleManager;
import com.smartisanos.sara.bubble.manager.BubbleThemeManager;
import com.smartisanos.sara.bubble.revone.view.DragViewManager;
import com.smartisanos.sara.bubble.view.FakeBubbleView;
import com.smartisanos.sara.bubble.view.IFakeBubbleView;
import com.smartisanos.sara.storage.BubbleDataRepository;
import com.smartisanos.sara.util.ViewUtils;
import com.smartisanos.sara.widget.IBubbleHeightChangeListener;
import com.smartisanos.sara.widget.listener.IVoiceOperationListener;
import com.smartisanos.sara.bubble.revone.view.AllContactsViewManager;
import com.smartisanos.sara.bubble.revone.view.ButtonViewManager;
import com.smartisanos.sara.bubble.revone.view.ImFavoriteViewManager;
import com.smartisanos.sara.bubble.revone.view.ImVoiceViewManager;
import com.smartisanos.sara.bubble.revone.view.ImBaseViewManager;
import com.smartisanos.sara.bubble.revone.view.ImRecentViewManager;
import com.smartisanos.sara.bubble.revone.manager.StartedAppManager;
import com.smartisanos.sara.bubble.revone.entity.GlobalContact;
import com.smartisanos.sara.bubble.revone.utils.FlashImConnection;
import com.smartisanos.sara.bubble.revone.widget.DragContentLayout;
import com.smartisanos.sara.util.AnimManager;
import com.smartisanos.sara.util.AttachmentUtils;
import com.smartisanos.sara.util.LogUtils;
import com.smartisanos.ideapills.common.util.MutiTaskHandler;
import com.smartisanos.sara.util.SaraConstant;
import com.smartisanos.sara.util.SaraUtils;
import com.smartisanos.sara.entity.SpeechError;
import com.smartisanos.sara.util.StringUtils;
import com.smartisanos.sara.util.ToastUtil;
import com.smartisanos.sara.widget.BubbleItemView;
import com.smartisanos.sara.widget.WaveView;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import smartisanos.app.voiceassistant.ParcelableObject;
import smartisanos.util.LogTag;

public class FlashImActivity extends AbstractRevVoiceActivity implements SaraUtils.BubbleViewChangeListener, WaveView.AnimationListener {

    public static final String TAG = "VoiceAss.FlashImActivity";
    private static final int MSG_START_UI = 3;
    private static final int MSG_CREATE_WAV_ROOT_FILE = 5;
    private static final int MSG_HIDE_GLOBLE_BUBBLE = 6;
    private static final int MSG_FINISH_ACTIVITY_ANIM = 7;
    private static final int MSG_NO_RESULT_TIP = 8;

    private static final int BUBBLE_STATUS_NORMAL = 0;
    private static final int BUBBLE_STATUS_HIDDING = 1;
    private static final int SEND_MESSAGE_SUCCESS = 1;
    public static final String FILEPROVIDER_AUTHORITY = "com.smartisanos.sara.fileprovider";
    public static final String FLASH_IM_PACKAGE = "com.bullet.messenger";
    private int mLaunchType = ExtScreenConstant.LAUNCH_FROM_LONG_PRESS;
    private boolean mDragState = false;
    private ImageView mIvGaussBlur;
    private BubbleItemView mBubbleView;
    private View mWaveLayout;
    private View mTextPopup;
    private WaveView mWaveForm;
    private View mPopupBottomArrow;
    private DragContentLayout mContentView;
    private IFakeBubbleView mFakeBubbleView;

    private StringBuffer result = new StringBuffer();
    private GlobalBubble mGlobalBubble;
    protected boolean mPendingFinishing;
    private boolean mIsOffLine = false;
    private int mBubbleStatus = BUBBLE_STATUS_NORMAL;
    private boolean mIsMaxWave = false;
    private boolean mRecognizeForContact = false;
    private int mDisplayWidth;
    private int mDisplayHeight;
    private int mResulTopMargin;
    private int mBubblePaddingX;
    private int mWaveResultMarginBottom;
    private int mWaveBottomMarginBottom;
    private int mBubbleWaveMinWidth;
    private int mBubbleLeftWaveMinWidth;
    private int mBubbleWaveDefaultWidth;
    private int mBubbleDefaultWidthCalibration;
    private int mWaveResultMarginLeft;
    private int mBubbleLeftMargin;
    private ButtonViewManager mButtonViewManager;
    private ImVoiceViewManager mFlashImVoiceManager;
    private ImRecentViewManager mRecentContactsManager;
    private ImFavoriteViewManager mFavoriteContactsManager;
    private AllContactsViewManager mAllContactsManager;
    private StartedAppManager mStartedAppManager;
    private FlashImConnection mFlashImConnection;
    private ClipData mClipData;
    private DragViewManager mDragContentManager;
    private volatile boolean mSendMessageFinish;
    private volatile boolean mDropAnimFinish;
    private boolean mHandleDragEvent = true;
    private boolean mPendingSentAction;
    private int mLaunchButtonPos = -1;
    private int mLastButtonPos = 0;
    private boolean mIsLeftBubble = true;
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
                    if (!isStopped() && bubbleItemView.getVisibility() == View.VISIBLE && BubbleManager.isAddBubble2List()) {
                        bubbleItemView.addBubble2SideBar(true);
                    }
                    SaraUtils.overridePendingTransition(FlashImActivity.this, true);
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
            }
        }
    };
    private CustomHandler mCustomHandler = new CustomHandler(this);

    private static class CustomHandler extends Handler {
        private final WeakReference<FlashImActivity> mActivity;

        public CustomHandler(FlashImActivity activity) {
            mActivity = new WeakReference<FlashImActivity>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            FlashImActivity activity = mActivity.get();
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
        mStartedAppManager = new StartedAppManager(this);
        if (!checkAllowUseFlashIm()) {
            finish();
            return;
        }
        mStartedAppManager.registerTaskListener();
        updateLaunchInfo(getIntent(), true);
        RevActivityManager.INSTANCE.attach(this);
        mPendingFinishing = false;
        mFlashImConnection = new FlashImConnection(this);
        mFlashImConnection.bindService();
        BubbleActionUpHelper.INSTANCE.addActionUpListener(mActionUpListener);
        IntentFilter filter = new IntentFilter(SaraConstant.ACTION_RECORD_ERROR);
        filter.addAction(SaraConstant.ACTION_UPDATE_BUBBLE);
        filter.addAction(SaraConstant.ACTION_IDEAPILLS_SHARE_INVITATION_SEND);
        filter.addAction(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
        filter.addAction(SaraConstant.ACTION_CHOOSE_RESULT);
        filter.addAction(StartedAppManager.APPLICATION_SELECT);
        registerReceiver(mReceiver, filter);
        SaraUtils.buildWavRootPathAsync(this);
        initDimens();
        setContentView(R.layout.revone_flash_im_activity);
        initView();
        if (!mDragState) {
            preStartVoiceRecognition(true);
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        LogUtils.infoRelease(TAG, "onNewIntent():" + getRecordStatus());
        super.onNewIntent(intent);
        updateLaunchInfo(intent, false);
        if (!mDragState) {
            if (isRecognizing()) {
                stopRecognize(false);
            } else {
                mPendingFinishing = false;
                mRecordStatus = RECORD_STATUS_IDLE;
                clearAnimation();
                mGlobalBubble = null;
                BubbleItemView bubbleItemView = getBubbleView();
                bubbleItemView.setAttachmentList(null);
                bubbleItemView.setVisibility(View.GONE);
                preStartVoiceRecognition(false);
            }
        }
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

    private boolean checkAllowUseFlashIm() {
        if (!PackageUtils.isAvilibleApp(this, FLASH_IM_PACKAGE)) {
            ToastUtil.showToast(this, R.string.flash_im_not_install_tip, Toast.LENGTH_SHORT);
            return false;
        }
        if (mStartedAppManager.closePhoneApp("com.bullet.messenger")) {
            checkLoginDelay();
            return true;
        } else {
            return checkLogin();
        }
    }

    private void checkLoginDelay() {
        UIHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (!checkLogin()) {
                    finishAll();
                }
            }
        }, 300);
    }

    private boolean checkLogin() {
        boolean login = false;
        try {
            Uri uri = Uri.parse(ImBaseViewManager.AUTHORITY);
            Bundle bundle = getContentResolver().call(uri, "FLASHIM_LOGIN_ACCOUNT", null, null);
            login = bundle != null && !TextUtils.isEmpty(bundle.getString("KEY_CONTENT_ACCOUNT"));
            if (!login) {
                ToastUtil.showToast(this, R.string.flash_im_not_login, Toast.LENGTH_SHORT);
            }
        } catch (Exception e) {
        } finally {
            return login;
        }
    }

    private void updateLaunchInfo(Intent intent, boolean create) {
        boolean lastDragState = mDragState;
        mLaunchType = intent.getIntExtra(ExtScreenConstant.LAUNCH_TYPE, ExtScreenConstant.LAUNCH_FROM_LONG_PRESS);
        mDragState = intent.getBooleanExtra(ExtScreenConstant.DRAG_STATE, false);
        mLaunchButtonPos = intent.getIntExtra(ExtScreenConstant.LAUNCH_BUTTON_POS, -1);
        mIsLeftBubble = mLaunchButtonPos > 0 ? false : true;
        if (mLastButtonPos != mLaunchButtonPos) {
            mLastButtonPos = mLaunchButtonPos;
            clearWaveLayout();
        }
        if (!create && lastDragState != mDragState) {
            inflateContentView();
        }
    }

    private void handleDragStart(DragEvent event) {
        mCustomHandler.removeMessages(MSG_START_UI);
        mClipData = event.getClipData();
        mDragContentManager = new DragViewManager(mClipData, mContentView, mDropStateListener);
        if (!mDragState) {
            mDragState = true;
            stopRecognize(true);
            inflateContentView();
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
        if (mBubbleView != null) {
            BubbleItemView bubbleItemView = mBubbleView;
            bubbleItemView.checkInput(false);
            bubbleItemView.hideSoftInputFromWindow();
        }
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
            // Activity may finish without register any receiver or bind any service
            // Just ignore the exception.
            mFlashImConnection.unBindService();
            BubbleActionUpHelper.INSTANCE.removeActionUpListener(mActionUpListener);
            unregisterReceiver(mReceiver);
        } catch (Exception e) {
        }
        super.onDestroy();
    }

    public void finishAll() {
        if (mStartedAppManager != null) {
            mStartedAppManager.closeStartedApp(mStartedPackageList, true, null);
        }
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
        mRecognizeForContact = false;
        startRecognize();
        if (createFlag) {
            mCustomHandler.sendEmptyMessageDelayed(MSG_START_UI, 300);
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
        if (mRecognizeForContact) {
            if (mFlashImVoiceManager != null) {
                mFlashImVoiceManager.onError(errorCode);
            }
            return;
        }
        switch (errorCode) {
            case SpeechError.MSP_ERROR_NET_SENDSOCK:
            case SpeechError.MSP_ERROR_NET_RECVSOCK:
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
        if (mRecognizeForContact) {
            if (mFlashImVoiceManager != null) {
                mFlashImVoiceManager.onResultRecived(resultStr);
            }
            return;
        }
        View waveView = getWaveLayout();
        waveView.clearAnimation();
        waveView.setVisibility(View.GONE);
        mIvGaussBlur.setVisibility(View.VISIBLE);
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
//        if (mRecognizeForContact) {
//            return;
//        }
//        showNoResultViewDuringRecording(-1);
    }

    @Override
    protected void buffer(byte[] buffer, int totalPoint) {
        if (mRecognizeForContact) {
            return;
        }
        if (mWaveForm != null) {
            mWaveForm.waveChanged(buffer, totalPoint);
        }
    }

    @Override
    protected void parcailResult(String partialResult) {
        if (mRecognizeForContact) {
            if (mFlashImVoiceManager != null) {
                mFlashImVoiceManager.onPartialResult(partialResult);
            }
            return;
        }
        result.append(partialResult);
        if (mIsMaxWave && isWaitingRecordResult()) {
            playPopup2TextAnim();
        }
    }

    @Override
    protected void recordStarted() {
        if (mRecognizeForContact) {
            if (mFlashImVoiceManager != null) {
                mFlashImVoiceManager.onRecordStart();
            }
            return;
        }
    }

    @Override
    protected void recordEnded(boolean isShortRecord, boolean isMaxRecord) {
        if (mRecognizeForContact) {
            if (mFlashImVoiceManager != null) {
                mFlashImVoiceManager.onRecordEnd();
            }
            return;
        }
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
                mFakeBubbleView.initFakeAnim(mGlobalBubble, bubbleItemView.getAttachmentList(), mDisplayWidth);

                int[] position = new int[2];
                bubbleItemView.getLocationOnScreen(position);
                mFakeBubbleView.setFakeAnimOffset(position[0], position[1],
                        bubbleItemView.getMeasuredWidth(), bubbleItemView.getMeasuredHeight());
                int targetX = mFakeBubbleView.getFakeAnimTargetX();
                int targetY = mFakeBubbleView.getFakeAnimTargetY(bubbleItemView.getLayoutParams().height);

                int translateX = targetX - position[0];
                bubbleItemView.hideViewWithScaleAnim(point, mFakeBubbleView.getFakeBubbleBgWidth(),
                        mFakeBubbleView.getFakeBubbleBgHeight(), translateX, targetY + position[1]);
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
                AnimManager.hideViewWithAlphaAndTranslate(bubbleItemView, 0, 250, bubbleItemTranslateY);
            }

            Message message = mCustomHandler.obtainMessage(MSG_FINISH_ACTIVITY_ANIM);
            if (point == null) {
                mCustomHandler.sendMessageDelayed(message, 250);
            }
        } else if (from == 1) {
            if (bubbleItemView.getVisibility() == View.VISIBLE) {
                AnimManager.hideViewWithAlphaAndTranslate(bubbleItemView, 0, 200, bubbleItemTranslateY);
            }
        } else if (from == 2) {
            if (bubbleItemView.getVisibility() == View.VISIBLE) {
                AnimManager.hideViewWithAlphaAndTranslate(bubbleItemView, 0, 200, bubbleItemTranslateY);
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
    public void editView(boolean keyboardVisible, boolean isSmallEdit) {
        if (keyboardVisible) {
            final BubbleItemView bubbleItemView = getBubbleView();
            int bubbleTargetHeight = bubbleItemView.getExactHeight();
            int targetTranslate = (bubbleTargetHeight - mDisplayHeight) / 2;
            AnimManager.showViewWithTranslate(bubbleItemView, 150, 250, targetTranslate, null, bubbleTargetHeight);
        }
    }

    @Override
    public Activity getActivityContext() {
        return this;
    }

    @Override
    public int getBubbleSmallTranslation(int bubbleHeight) {
        return bubbleHeight - mDisplayHeight + mResulTopMargin;
    }

    @Override
    public int getBubbleLargeTranslation(int bubbleHeight) {
        return bubbleHeight - mDisplayHeight + mResulTopMargin;
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
        mButtonViewManager.show();
        mFlashImVoiceManager.show();
        mRecentContactsManager.show();
        mFavoriteContactsManager.show();
        mAllContactsManager.show();
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
            mBubbleView.setVisibility(View.GONE);
        }
    }

    public void startUI() {
        LogTag.d(TAG, "enter startUI()");
        result = new StringBuffer();
        resetView();
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
        if (mClipData == null) {
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

    private void resetView() {
        if (mButtonViewManager != null) {
            mButtonViewManager.hide();
        }
        if (mFlashImVoiceManager != null) {
            mFlashImVoiceManager.hide();
        }
        if (mRecentContactsManager != null) {
            mRecentContactsManager.hide();
        }
        if (mFavoriteContactsManager != null) {
            mFavoriteContactsManager.hide();
        }
        if (mAllContactsManager != null) {
            mAllContactsManager.hide();
        }
        final boolean fromClick = mLaunchType == ExtScreenConstant.LAUNCH_FROM_CLICK;
        mIvGaussBlur.setVisibility(mDragState ? View.VISIBLE : View.GONE);
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
        View waveView = getWaveLayout();
        waveView.setVisibility(View.GONE);
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

    public void initView() {
        mContentView = (DragContentLayout) findViewById(R.id.root_view);
        mContentView.setTouchDownListener(mTouchDownListener);
        mContentView.setDragStartListener(mDragStartListener);
        mIvGaussBlur = (ImageView) findViewById(R.id.gauss_blur);
        WallpaperUtils.gaussianBlurWallpaper(this, mIvGaussBlur);
        inflateContentView();
    }

    private void removeContentView() {
        if (mContentView != null) {
            int count = mContentView.getChildCount();
            for (int i = count - 1; i >= 0; i--) {
                View child = mContentView.getChildAt(i);
                if (child != mIvGaussBlur) {
                    mContentView.removeView(child);
                }
            }
        }
    }

    private void inflateContentView() {
        boolean withDrag = mDragState;
        LayoutInflater inflater = LayoutInflater.from(this);
        View content = inflater.inflate(withDrag ? R.layout.revone_flash_im_drag_content : R.layout.revone_flash_im_content, null);
        LayoutParams lp = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
        CommonUtils.setAlwaysCanAcceptDragForAll(content, true);
        mIvGaussBlur.setVisibility(withDrag ? View.VISIBLE : View.GONE);
        removeContentView();
        mContentView.addView(content, lp);
        mButtonViewManager = new ButtonViewManager(this, mContentView, mCloseListener);
        mFlashImVoiceManager = new ImVoiceViewManager(this, mContentView);
        mFlashImVoiceManager.setItemClickListener(mItemClickListener);
        mRecentContactsManager = new ImRecentViewManager(this, mContentView, withDrag);
        mRecentContactsManager.setItemClickListener(mItemClickListener);
        mFavoriteContactsManager = new ImFavoriteViewManager(this, mContentView, withDrag);
        mFavoriteContactsManager.setItemClickListener(mItemClickListener);
        mAllContactsManager = new AllContactsViewManager(this, mContentView, withDrag);
        mAllContactsManager.setItemClickListener(mItemClickListener);
        if (withDrag) {
            mButtonViewManager.show();
            mFlashImVoiceManager.show();
            mRecentContactsManager.show();
            mFavoriteContactsManager.show();
            mAllContactsManager.show();
        } else {
            Display display = getApplicationContext().getDisplay();
            Point realSize = new Point();
            display.getRealSize(realSize);
            mDisplayHeight = realSize.y;
            mDisplayWidth = getResources().getDimensionPixelSize(R.dimen.flash_im_bubble_page_width) - mBubblePaddingX * 2;
            mFakeBubbleView = new FakeBubbleView(this, (ViewStub) findViewById(R.id.fak_bubble_stub));
        }
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
        mBubbleView.setBubbleMarginX(getResources().getDimensionPixelOffset(R.dimen.flash_im_bubble_padding_x));
        mBubbleView.setViewListener(this);
        mBubbleView.setSoftListener(this);
        mBubbleView.setVoiceOperationListener(mOnVoiceOperationListener);
        mBubbleView.setBubbleHeightChangeListener(mBubbleHeightChangeListener);
        mBubbleView.setBubbleClickListener(new BubbleItemView.OnBubbleClickListener() {
            public void onAddAttachmentClick() {
                SaraUtils.startAttachementChoose(FlashImActivity.this);
            }

            public void onAttachmentChanged() {
                mBubbleView.refreshAttachmentView();
                setBubbleState(BubbleItemView.BubbleSate.LARGE, false, false);
            }

            public void onImageAttchmentClick(GlobalBubbleAttach globalBubbleAttach, ArrayList<Uri> localUris) {
                SaraUtils.startImagePreview(FlashImActivity.this, globalBubbleAttach, localUris);
            }

            public void onFileClick(GlobalBubbleAttach globalBubbleAttach) {
                SaraUtils.startFilePreview(FlashImActivity.this, globalBubbleAttach);
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
        mBubbleLeftWaveMinWidth = res.getDimensionPixelSize(R.dimen.bubble_wave_left_min_width);
        mBubbleWaveMinWidth = res.getDimensionPixelSize(R.dimen.bubble_wave_min_width);
        mBubbleWaveDefaultWidth = res.getDimensionPixelSize(R.dimen.global_search_bubble_default_width);
        mBubbleDefaultWidthCalibration = res.getDimensionPixelSize(R.dimen.global_bubble_large_mode_width_calibration);
        mWaveResultMarginLeft = res.getDimensionPixelSize(R.dimen.wave_result_margin_left);
        mResulTopMargin = res.getDimensionPixelSize(R.dimen.flash_im_bubble_margin_top);
        if (ExtScreenConstant.CONTAINS_STATUS_NAVI_BAR) {
            mResulTopMargin += ExtScreenConstant.STATUS_BAR_HEIGHT;
        }
        mWaveResultMarginBottom = res.getDimensionPixelSize(R.dimen.flash_im_wave_margin_bottom);
        mWaveBottomMarginBottom = res.getDimensionPixelSize(R.dimen.revone_wave_bottom_margin_bottom);
        mBubblePaddingX = res.getDimensionPixelSize(R.dimen.flash_im_bubble_padding_x);
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
                FlashImActivity.this, bubbleItemView.getAttachmentList(), cd);
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
                AttachmentUtils.mergeAttachmentList(FlashImActivity.this, bubbleItemView.getAttachmentList(), data);
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

    public List<GlobalContact> getFlashImContact() {
        if (mAllContactsManager != null) {
            return mAllContactsManager.getAllContacts();
        }
        return null;
    }

    private void playSendImMessageAnim(View view) {
        int loc[] = new int[2];
        loc = view.getLocationOnScreen();
        ArrayList<Animator> animatorList = new ArrayList<Animator>();
        BubbleItemView bubbleItemView = getBubbleView();
        int bubbleLoc[] = new int[2];
        bubbleLoc = bubbleItemView.getLocationOnScreen();
        int currentY = bubbleLoc[1] - (int) bubbleItemView.getTranslationY();
        int bubbleHeight = bubbleItemView.getHeight();
        PropertyValuesHolder scaleX = PropertyValuesHolder.ofFloat("scaleX", 1f, 0f);
        PropertyValuesHolder scaleY = PropertyValuesHolder.ofFloat("scaleY", 1f, 0f);
        PropertyValuesHolder alpha = PropertyValuesHolder.ofFloat("alpha", 1f, 0f);
        PropertyValuesHolder transX = PropertyValuesHolder.ofFloat("translationX", loc[0] - bubbleLoc[0]);
        PropertyValuesHolder transY = PropertyValuesHolder.ofFloat("translationY", loc[1] - currentY - (bubbleHeight - view.getHeight()) / 2);
        Animator bubbleAnim = ObjectAnimator.ofPropertyValuesHolder(bubbleItemView, scaleX, scaleY, alpha, transX, transY);
        bubbleAnim.setDuration(ExtScreenConstant.GENERAL_ANIM_TIME);
        animatorList.add(bubbleAnim);
        PropertyValuesHolder scaleOutX = PropertyValuesHolder.ofFloat("scaleX", 1f, ExtScreenConstant.SCALE_OUT_VALUE);
        PropertyValuesHolder scaleOutY = PropertyValuesHolder.ofFloat("scaleY", 1f, ExtScreenConstant.SCALE_OUT_VALUE);
        Animator scaleOut = ObjectAnimator.ofPropertyValuesHolder(view, scaleOutX, scaleOutY);
        scaleOut.setInterpolator(new DecelerateInterpolator(1.5f));
        scaleOut.setDuration(ExtScreenConstant.GENERAL_ANIM_TIME);
        animatorList.add(scaleOut);
        PropertyValuesHolder scaleInX = PropertyValuesHolder.ofFloat("scaleX", ExtScreenConstant.SCALE_OUT_VALUE, 1f);
        PropertyValuesHolder scaleInY = PropertyValuesHolder.ofFloat("scaleY", ExtScreenConstant.SCALE_OUT_VALUE, 1f);
        Animator scaleIn = ObjectAnimator.ofPropertyValuesHolder(view, scaleInX, scaleInY);
        scaleIn.setInterpolator(new AccelerateInterpolator(1.5f));
        scaleIn.setDuration(ExtScreenConstant.DROP_ANIM_TIME);
        scaleIn.setStartDelay(ExtScreenConstant.GENERAL_ANIM_TIME);
        animatorList.add(scaleIn);
        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.playTogether(animatorList);
        animatorSet.start();
        animatorSet.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animator) {
            }

            @Override
            public void onAnimationEnd(Animator animator) {
                mDropAnimFinish = true;
                if (mSendMessageFinish) {
                    finishAll();
                }
            }

            @Override
            public void onAnimationCancel(Animator animator) {
            }

            @Override
            public void onAnimationRepeat(Animator animator) {
            }
        });
    }

    public void startContactRecognize() {
        mRecognizeForContact = true;
        startRecognize();
        registerVoiceCallback();
    }

    public void stopContactRecognize() {
        stopRecognize(false);
    }

    private void showSendMessageResult(final int msgId) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mSendMessageFinish = true;
                if (msgId > 0) {
                    ToastUtil.showToast(FlashImActivity.this, msgId, Toast.LENGTH_SHORT);
                }
                if (mDropAnimFinish) {
                    finishAll();
                }
            }
        });
    }

    private void handDropAnimFinishDelay() {
        UIHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                mDropAnimFinish = true;
                if (mSendMessageFinish) {
                    finishAll();
                }
            }
        }, ExtScreenConstant.DROP_ANIM_TIME);
    }

    private void sendDragFileMessage(final GlobalContact contact, final ClipData clipData) {
        MutiTaskHandler.post(new Runnable() {
            @Override
            public void run() {
                int ret = 0;
                int stringId = 0;
                if (mFlashImConnection.isServiceConnected() && clipData != null) {
                    if (contact != null) {
                        int count = clipData.getItemCount();
                        String mimeType = clipData.getDescription().getMimeType(0);
                        for (int i = 0; i < count; i++) {
                            Uri uri = clipData.getItemAt(i).getUri();
                            if (!TextUtils.isEmpty(mimeType)) {
                                if (mimeType.equals("text/plain")) {
                                    CharSequence text = clipData.getItemAt(i).getText();
                                    ret = mFlashImConnection.sendVoiceMessage(contact.getMessageType(), contact.getContactId(), null, text.toString());
                                    stringId = ret == SEND_MESSAGE_SUCCESS ? R.string.flash_im_send_message_success : R.string.flash_im_send_message_failed;
                                } else if (uri != null) {
                                    if (mimeType.contains("image")) {
                                        ret = mFlashImConnection.sendImageMessage(contact.getMessageType(), contact.getContactId(), uri.toString());
                                        stringId = ret == SEND_MESSAGE_SUCCESS ? R.string.flash_im_send_image_success : R.string.flash_im_send_image_failed;
                                    } else if (mimeType.equals("video/mp4") || mimeType.equals("video/3gp")) {
                                        ret = mFlashImConnection.sendVideoMessage(contact.getMessageType(), contact.getContactId(), uri.toString());
                                        stringId = ret == SEND_MESSAGE_SUCCESS ? R.string.flash_im_send_file_success : R.string.flash_im_send_file_failed;
                                    } else {
                                        ret = mFlashImConnection.sendFileMessage(contact.getMessageType(), contact.getContactId(), uri.toString());
                                        stringId = ret == SEND_MESSAGE_SUCCESS ? R.string.flash_im_send_file_success : R.string.flash_im_send_file_failed;
                                    }
                                }
                            }
                        }
                        if (stringId <= 0) {
                            stringId = R.string.flash_im_not_support_type;
                        }
                    } else {
                        stringId = R.string.flash_im_no_contacts;
                    }
                } else {
                    stringId = R.string.flash_im_send_message_failed;
                }
                showSendMessageResult(stringId);
            }
        });
    }

    private void sendFlashImMessage(GlobalContact contact, String message, Uri fileUri) {
        MutiTaskHandler.post(new Runnable() {
            @Override
            public void run() {
                long startTime = System.currentTimeMillis();
                int ret = 0;
                if (contact != null) {
                    if (mFlashImConnection.isServiceConnected()) {
                        ret = mFlashImConnection.sendVoiceMessage(contact.getMessageType(), contact.getContactId(), fileUri.toString(), message);
                    }
                }
                mSendMessageFinish = true;
                showSendMessageResult(ret == SEND_MESSAGE_SUCCESS ? R.string.flash_im_send_message_success : R.string.flash_im_send_message_failed);
            }
        });
    }

    private ImBaseViewManager.OnItemClickListener mItemClickListener = new ImBaseViewManager.OnItemClickListener() {
        @Override
        public void onItemClick(View v, GlobalContact contact) {
            if (mPendingSentAction) {
                return;
            }
            mPendingSentAction = true;
            mSendMessageFinish = false;
            mDropAnimFinish = false;
            if (mClipData != null) {
                mHandleDragEvent = false;
                MultiSdkUtils.cancelDragAndDropWithResult(mContentView, true);
                sendDragFileMessage(contact, mClipData);
                mDragContentManager.doFlyToAnimation(v);
            } else if (mGlobalBubble != null) {
                File file = new File(getFilesDir(), mGlobalBubble.getUri().getPath());
                Uri uri = FileProvider.getUriForFile(FlashImActivity.this, FILEPROVIDER_AUTHORITY, file);
                grantUriPermission(FLASH_IM_PACKAGE, uri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
                playSendImMessageAnim(v);
                sendFlashImMessage(contact, mGlobalBubble.getText(), uri);
            }
        }
    };

    private IVoiceOperationListener mOnVoiceOperationListener = new IVoiceOperationListener() {
        @Override
        public void onCancel() {
            finishAll();
        }

        @Override
        public void onDone() {
            stopRecognize(true);
        }
    };

    private ButtonViewManager.ICloseListener mCloseListener = new ButtonViewManager.ICloseListener() {
        @Override
        public void onClose() {
            finishAll();
        }
    };

    private DragContentLayout.ITouchDownListener mTouchDownListener = new DragContentLayout.ITouchDownListener() {
        @Override
        public void onTouchDown(MotionEvent ev) {
            if (mFlashImVoiceManager != null) {
                mFlashImVoiceManager.handleTouchEvent(ev);
            }
        }
    };

    private DragContentLayout.IDragEventListener mDragStartListener = new DragContentLayout.IDragEventListener() {
        @Override
        public void onDragStart(DragEvent dragEvent) {
            handleDragStart(dragEvent);
        }

        @Override
        public void onDragEnd(boolean handled, Point location) {
            if (mHandleDragEvent && !handled) {
                if (mDragContentManager != null) {
                    mDragContentManager.updateDragContent(location);
                } else {
                    finish();
                }
            }
        }

        @Override
        public void onDrop(GlobalContact contact) {
            if (contact != null) {
                sendDragFileMessage(contact, mClipData);
                handDropAnimFinishDelay();
            }
        }

        @Override
        public View getDragFocusChild(DragEvent dragEvent) {
            int x = (int) dragEvent.getX();
            int y = (int) dragEvent.getY();
            mDragContentManager.setLocation(x, y);
            View focusView = mFlashImVoiceManager.findFocusChild(x, y);
            if (focusView == null) {
                focusView = mRecentContactsManager.findFocusChild(x, y);
                if (focusView == null) {
                    focusView = mFavoriteContactsManager.findFocusChild(x, y);
                    if (focusView == null) {
                        focusView = mAllContactsManager.findFocusChild(x, y);
                    }
                }
            }
            return focusView;
        }
    };

    private DragViewManager.IDropStateListener mDropStateListener = new DragViewManager.IDropStateListener() {
        @Override
        public void onDropFinish() {
            mDropAnimFinish = true;
            if (mSendMessageFinish) {
                finishAll();
            }
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
