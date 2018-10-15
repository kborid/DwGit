package com.smartisanos.sara.bubble.revone;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.graphics.PointF;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.service.onestep.GlobalBubble;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.view.WindowManager;
import android.view.animation.AccelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.FrameLayout.LayoutParams;
import android.widget.ImageView;
import android.widget.Toast;

import com.smartisanos.ideapills.common.util.CommonUtils;
import com.smartisanos.ideapills.common.util.WallpaperUtils;
import com.smartisanos.sara.BubbleActionUpHelper;
import com.smartisanos.sara.R;
import com.smartisanos.sara.bubble.manager.BubbleManager;
import com.smartisanos.sara.bubble.manager.BubbleThemeManager;
import com.smartisanos.sara.entity.SpeechError;
import com.smartisanos.sara.util.ViewUtils;
import com.smartisanos.sara.widget.IBubbleHeightChangeListener;
import com.smartisanos.sara.widget.listener.IVoiceOperationListener;
import com.smartisanos.sara.bubble.revone.view.ButtonViewManager;
import com.smartisanos.sara.bubble.revone.manager.SettingManager;
import com.smartisanos.sara.bubble.revone.manager.StartedAppManager;
import com.smartisanos.sara.bubble.revone.view.TipViewManager;
import com.smartisanos.sara.bubble.revone.utils.SequenceActivityLauncher;
import com.smartisanos.sara.bubble.revone.widget.SearchAnimLayout;
import com.smartisanos.sara.bubble.revone.widget.SearchBubbleView;
import com.smartisanos.sara.bubble.revone.widget.SearchBubbleView.BubbleSate;
import com.smartisanos.sara.util.SaraConstant;
import com.smartisanos.sara.util.SaraUtils;
import com.smartisanos.sara.util.ToastUtil;
import com.smartisanos.sara.bubble.search.viewholder.ViewHolderApp;
import com.smartisanos.sara.widget.LocalSearchLayout;
import com.smartisanos.sara.widget.WaveView;

import java.util.List;

import smartisanos.api.ToastSmt;
import smartisanos.app.numberassistant.YellowPageResult;
import smartisanos.app.voiceassistant.ApplicationStruct;
import smartisanos.app.voiceassistant.ContactStruct;
import smartisanos.app.voiceassistant.MediaStruct;
import smartisanos.app.voiceassistant.ParcelableObject;
import smartisanos.os.RemoteCallback;

public class GlobalSearchActivity extends AbstractRevVoiceActivity implements SearchContract.View {
    public static final String TAG = "VoiceAss.GlobalSearchActivity";
    private static final String SEARCH_TYPE = "search_type";
    private static final String SEARCH_FROM_KEYWORDS = "search_key";
    private static final String ACTION_BOOM_TEXT = "smartisanos.intent.action.BOOM_TEXT";
    private static final String SEARCH_WINDOW_SELECT = "android.intent.action.SEARCH_WINDOW_SELECTED";

    private int mSearchType = SearchPresenter.SEARCH_TYPE_GLOBAL;
    private int mLaunchType = ExtScreenConstant.LAUNCH_FROM_LONG_PRESS;
    private String mSearchKeyWords;
    private ImageView mIvGaussBlur;
    private View mWaveLayout;
    private View mTextPopup;
    private WaveView mWaveForm;
    private View mPopupBottomArrow;
    private LocalSearchLayout mLocalSearchLayout;
    private SearchBubbleView mBubbleView;
    private ButtonViewManager mButtonViewManager;
    private StartedAppManager mStartedAppManager;
    private SearchAnimLayout mSearchAnimLayout;
    private TipViewManager mTipsManager;
    private SettingManager mSettingManager;
    private StringBuffer mSearchResult = new StringBuffer();
    private GlobalBubble mGlobalBubble;
    private int mBubbleWaveMinWidth;
    private int mBubbleLeftWaveMinWidth;
    private int mBubbleWaveMaxWidth;
    private int mBubbleWaveDefaultWidth;
    private int mStartWaveTranslationX;
    private int mWaveResultMarginLeft;
    private int mBubbleLeftMargin;
    private boolean mIsOffLine = false;
    private int mLaunchButtonPos = -1;
    private int mLastButtonPos = 0;
    private boolean mIsLeftBubble = true;
    private boolean mPendingFinishing = false;
    private BubbleActionUpHelper.MenuActionUpListener mActionUpListener = new BubbleActionUpHelper.MenuActionUpListener() {
        @Override
        public void onActionUp() {
            if (mLaunchType == ExtScreenConstant.LAUNCH_FROM_LONG_PRESS) {
                endKeyOrBlueRec();
            }
        }
    };
    private int mSelectTaskId;
    private SearchContract.Presenter mSearchPresenter;

    private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (SaraConstant.ACTION_UPDATE_BUBBLE.equals(action)) {
                resultFromIntentUpdate(intent);
            } else if (SEARCH_WINDOW_SELECT.equals(action)) {
                int taskId = intent.getIntExtra("taskId", 0);
                mSelectTaskId = taskId;
                mSearchPresenter.closeWindowAndExit(taskId, mStartedPackageList, true);
            } else if (StartedAppManager.APPLICATION_SELECT.equals(action)) {
                mSearchPresenter.closeWindowAndExit(0, mStartedPackageList, true);
            }
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        RevActivityManager.INSTANCE.attach(this);
        mStartedAppManager = new StartedAppManager(this);
        mStartedAppManager.registerTaskListener();
        new SearchPresenter(this, mStartedAppManager);
        updateSearchInfo(getIntent());
        BubbleActionUpHelper.INSTANCE.addActionUpListener(mActionUpListener);
        IntentFilter filter = new IntentFilter(SaraConstant.ACTION_UPDATE_BUBBLE);
        filter.addAction(SaraConstant.ACTION_UPDATE_BUBBLE);
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        filter.addAction(SEARCH_WINDOW_SELECT);
        filter.addAction(StartedAppManager.APPLICATION_SELECT);
        registerReceiver(mBroadcastReceiver, filter);
        SaraUtils.buildWavRootPathAsync(this);
        setContentView(R.layout.revone_search_activity);
        initView();
        startVoiceSearch(200);
        mSearchPresenter.cancelDelayCloseWindow();
        mSearchPresenter.closePhoneApp(this, mSearchType);
        mSelectTaskId = 0;
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        int delay = 0;
        int lastSearchType = mSearchType;
        int lastLaunchType = mLaunchType;
        String lastSearchKeyWords = mSearchKeyWords;
        updateSearchInfo(intent);
        mVoiceHandler.removeCallbacksAndMessages(null);
        mSearchResult = new StringBuffer();
        boolean isTheSameSearch = lastSearchType == mSearchType && lastLaunchType == mLaunchType;
        isTheSameSearch &= (lastSearchKeyWords == null && mSearchKeyWords == null) ||
                (lastSearchKeyWords != null && lastSearchKeyWords.equals(mSearchKeyWords));
        if (isRecognizing()) {
            delay = 200;
            stopRecognize(true);
            if (isTheSameSearch) {
                return;
            }
        }
        if (mSettingManager != null) {
            mSettingManager.hidePopupList();
        }
        mSearchPresenter.cancelDelayCloseWindow();
        mSearchPresenter.closePhoneApp(this, lastSearchType);
        mSearchPresenter.delayCloseWindow(mSelectTaskId, mStartedPackageList, TextUtils.isEmpty(mSearchKeyWords) ? 600 : 0);
        mRecordStatus = RECORD_STATUS_IDLE;
        mGlobalBubble = null;
        mSelectTaskId = 0;
        clearAnimation();
        startVoiceSearch(delay);
    }

    private void startVoiceSearch(int delay) {
        BubbleManager.markAddBubble2List(true);
        hideUpperWindows();
        if (TextUtils.isEmpty(mSearchKeyWords)) {
            resetView(delay);
            mVoiceHandler.postDelayed(mStartVoiceRunable, delay);
        } else {
            mVoiceHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    searchByKeyword(mSearchKeyWords);
                }
            }, delay);
        }
    }

    @Override
    protected void loadLocalData(String keywords, String packageName) {
        mSearchPresenter.setLocalSearchState(false, false, mSearchResult.toString());
        super.loadLocalData(keywords, packageName);
    }

    public void finishAll() {
        mSearchPresenter.closeWindowAndExit(0, mStartedPackageList, true);
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (isRecognizing()) {
            stopRecognize(true);
        }
        if (!isFinishing()) {
            mSearchPresenter.closeWindowAndExit(0, mStartedPackageList, true);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mVoiceHandler.removeCallbacksAndMessages(null);
        if (mStartedAppManager != null) {
            mStartedAppManager.unRegisterTaskListener();
        }
        if (mSettingManager != null) {
            mSettingManager.hidePopupList();
        }
        if (mLocalSearchLayout != null) {
            mLocalSearchLayout.destroy();
        }
        if (mBroadcastReceiver != null) {
            unregisterReceiver(mBroadcastReceiver);
            mBroadcastReceiver = null;
        }
        try {
            BubbleActionUpHelper.INSTANCE.removeActionUpListener(mActionUpListener);
        } catch (Exception e) {
        }
        mSearchPresenter.delayCloseWindow(mSelectTaskId, mStartedPackageList);
    }

    private void updateSearchInfo(Intent intent) {
        mSearchType = intent.getIntExtra(SEARCH_TYPE, SearchPresenter.SEARCH_TYPE_GLOBAL);
        int launchType = intent.getIntExtra(ExtScreenConstant.LAUNCH_TYPE, ExtScreenConstant.LAUNCH_FROM_LONG_PRESS);
        String searchKeyWords = intent.getStringExtra(SEARCH_FROM_KEYWORDS);
        boolean calledBySearchKey = intent.hasExtra(SEARCH_FROM_KEYWORDS);
        if (calledBySearchKey && TextUtils.isEmpty(searchKeyWords)) {
            mLaunchType = ExtScreenConstant.LAUNCH_FROM_CLICK;
            mSearchKeyWords = null;
        } else {
            mLaunchType = launchType;
            mSearchKeyWords = searchKeyWords;
        }
        mLaunchButtonPos = intent.getIntExtra(ExtScreenConstant.LAUNCH_BUTTON_POS, -1);
        mIsLeftBubble = mLaunchButtonPos > 0 ? false : true;
        if (mLastButtonPos != mLaunchButtonPos) {
            mLastButtonPos = mLaunchButtonPos;
            clearWaveLayout();
        }
    }

    public void preStartVoiceRecognition() {
        if (isFinishing()) {
            return;
        }
        mIsOffLine = false;
        mSearchPresenter.setLocalSearchState(false, false, mSearchResult.toString());
        startRecognize();
        registerVoiceCallback();
    }

    @Override
    protected void realStartRecognize() {
        super.realStartRecognize();
        mSearchResult = new StringBuffer();
    }

    private void initView() {
        Resources res = getResources();
        mBubbleLeftWaveMinWidth = res.getDimensionPixelSize(R.dimen.bubble_wave_left_min_width);
        mBubbleWaveMinWidth = res.getDimensionPixelSize(R.dimen.bubble_wave_min_width);
        mBubbleWaveMaxWidth = res.getDimensionPixelSize(R.dimen.global_search_bubble_max_width);
        mBubbleWaveDefaultWidth = res.getDimensionPixelSize(R.dimen.global_search_bubble_default_width);
        mStartWaveTranslationX = res.getDimensionPixelSize(R.dimen.revone_bubble_wave_start_translationx);
        mWaveResultMarginLeft = res.getDimensionPixelSize(R.dimen.wave_result_margin_left);
        mBubbleLeftMargin = res.getDimensionPixelSize(R.dimen.global_search_bubble_margin_left);
        View rootView = findViewById(R.id.root_view);
        mIvGaussBlur = (ImageView) findViewById(R.id.gauss_blur);
        WallpaperUtils.gaussianBlurWallpaper(this, mIvGaussBlur);
        mButtonViewManager = new ButtonViewManager(this, rootView, mCloseListener, mSettingListener);
        mTipsManager = new TipViewManager(this, rootView, mButtonViewManager);
        mSearchAnimLayout = (SearchAnimLayout) rootView.findViewById(R.id.search_anim_layout);
        mSearchAnimLayout.loadPlaceHolderImg();
    }

    public boolean clearAnimation() {
        boolean isClearAnimation = false;
        if (mLocalSearchLayout != null) {
            if (mLocalSearchLayout.getAnimation() != null && !mLocalSearchLayout.getAnimation().hasEnded()) {
                isClearAnimation = true;
            }
            mLocalSearchLayout.clearAnimation();
        }
        View waveLayout = getWaveLayout();
        if (waveLayout.getAnimation() != null && !waveLayout.getAnimation().hasEnded()) {
            isClearAnimation = true;
        }
        waveLayout.clearAnimation();
        if (mWaveForm.getAnimation() != null && !mWaveForm.getAnimation().hasEnded()) {
            isClearAnimation = true;
        }
        mWaveForm.clearAnimation();
        mWaveForm.stopAnimation(false);
        SearchBubbleView bubbleView = getBubbleView();
        isClearAnimation |= bubbleView.cancelAnimations();
        isClearAnimation |= bubbleView.cancelAnimators();
        return isClearAnimation;
    }

    private void resetView(long delay) {
        final boolean fromClick = mLaunchType == ExtScreenConstant.LAUNCH_FROM_CLICK;
        int color = SaraUtils.getDefaultBubbleColor(this);
        mIvGaussBlur.setVisibility(View.GONE);
        if (mTipsManager != null) {
            mTipsManager.hide();
        }
        if (mButtonViewManager != null) {
            mButtonViewManager.hide();
        }
        resetSearchViewData();
        resetBubbleView(color, fromClick);
        resetWaveLayout(color, fromClick, delay);
        setParamWidth(fromClick ? mBubbleWaveDefaultWidth : mIsLeftBubble ? mBubbleLeftWaveMinWidth : mBubbleWaveMinWidth);
    }

    private void resetWaveLayout(int color, boolean fromClick, long delay) {
        int topMargin = getWaveTopMargin();
        View waveLayout = getWaveLayout();
        LayoutParams params = new LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.gravity = Gravity.LEFT | Gravity.TOP;
        params.topMargin = topMargin;
        params.leftMargin = mWaveResultMarginLeft;
        waveLayout.setTranslationY(0);
        if (fromClick) {
            mWaveForm.setWaveMaxWidth(mBubbleWaveDefaultWidth);
            mWaveForm.setWaveMinWidth(mBubbleWaveDefaultWidth);
            mWaveForm.updateLeftPadding(mIsLeftBubble);
            waveLayout.setVisibility(View.VISIBLE);
            if (!mIsLeftBubble) {
                SearchBubbleView bubbleView = getBubbleView();
                params.height = ViewUtils.getSupposeHeight(bubbleView);
            }
        } else {
            mWaveForm.updateWidthPrama(mIsLeftBubble);
            mWaveForm.setWaveMaxWidth(mBubbleWaveMaxWidth);
            if (mIsLeftBubble) {
                if (delay > 0) {
                    waveLayout.setVisibility(View.GONE);
                }
                startWaveLayoutAnim(delay);
            } else {
                waveLayout.setVisibility(View.VISIBLE);
            }
        }
        resetWaveFrom();
        waveLayout.setLayoutParams(params);
        mPopupBottomArrow.setBackgroundResource(mIsLeftBubble
                ? fromClick ? BubbleThemeManager.getWaveLargeArrowRes(color) : BubbleThemeManager.getWaveArrowRes(color, mIsLeftBubble)
                : BubbleThemeManager.getWaveArrowRes(color, mIsLeftBubble));
        mPopupBottomArrow.setVisibility(View.VISIBLE);
        mTextPopup.setVisibility(fromClick ? View.GONE : View.VISIBLE);
        mTextPopup.setBackgroundResource(BubbleThemeManager.getBackgroudRes(color, BubbleThemeManager.BACKGROUND_BUBBLE_NORMAL));
    }

    private void resetBubbleView(int color, boolean fromClick) {
        int topMargin = getWaveTopMargin();
        SearchBubbleView bubbleView = getBubbleView();
        bubbleView.setBubbleState(BubbleSate.INIT);
        if (fromClick) {
            bubbleView.showVoiceInputButton();
        } else {
            bubbleView.hideVoiceInputButton();
        }
        bubbleView.setVisibility(fromClick ? View.VISIBLE : View.GONE);
        bubbleView.resetBubbleContent(color);
        bubbleView.setBackgroundResource(BubbleThemeManager.getBackgroudRes(color,
                fromClick ? BubbleThemeManager.BACKGROUND_BUBBLE_LARGE : BubbleThemeManager.BACKGROUND_BUBBLE_NORMAL));
        LayoutParams bubbleParams = (LayoutParams) bubbleView.getLayoutParams();
        bubbleParams.leftMargin = mBubbleLeftMargin;
        bubbleParams.topMargin = topMargin;
        bubbleView.setLayoutParams(bubbleParams);
    }

    public void resetWaveFrom() {
        if (mWaveForm != null) {
            mWaveForm.reset(mIsLeftBubble);
        }
    }

    protected void stopWaveAnim(boolean isCancelWithoutCallback) {
        if (mWaveForm != null) {
            mWaveForm.stopAnimation(isCancelWithoutCallback);
        }
    }

    public void setBubbleState(BubbleSate state) {
        final boolean fromClick = mLaunchType == ExtScreenConstant.LAUNCH_FROM_CLICK;
        int color = SaraUtils.getDefaultBubbleColor(this);
        SearchBubbleView bubbleView = getBubbleView();
        BubbleSate lastState = bubbleView.getBubbleState();
        if (state == BubbleSate.INIT) {
            bubbleView.setBackgroundResource(BubbleThemeManager.getBackgroudRes(color,
                    fromClick ? BubbleThemeManager.BACKGROUND_BUBBLE_LARGE : BubbleThemeManager.BACKGROUND_BUBBLE_NORMAL));
            if (mIsLeftBubble) {
                mPopupBottomArrow.setBackgroundResource(fromClick ? BubbleThemeManager.getWaveLargeArrowRes(color) : BubbleThemeManager.getWaveArrowRes(color, mIsLeftBubble));
            }
        } else if (state == BubbleSate.NORMAL) {
            bubbleView.setBackgroundResource(BubbleThemeManager.getBackgroudRes(color, BubbleThemeManager.BACKGROUND_BUBBLE_NORMAL));
        } else if (lastState == BubbleSate.INIT) {
            bubbleView.setBackgroundResource(BubbleThemeManager.getBackgroudRes(color, BubbleThemeManager.BACKGROUND_BUBBLE_LARGE));
            if (mIsLeftBubble) {
                mPopupBottomArrow.setBackgroundResource(BubbleThemeManager.getWaveLargeArrowRes(color));
            }
        }
        bubbleView.setBubbleState(state);
    }

    private LocalSearchLayout getLocalSearchLayout() {
        if (mLocalSearchLayout == null) {
            ViewStub searchStub = (ViewStub) findViewById(R.id.result_stub);
            if (searchStub != null && searchStub.getParent() != null) {
                searchStub.inflate();
                mLocalSearchLayout = (LocalSearchLayout) findViewById(R.id.result);
            } else {
                mLocalSearchLayout = (LocalSearchLayout) findViewById(R.id.result);
            }
            mLocalSearchLayout.init(this);
            mLocalSearchLayout.setViewListener(new SaraUtils.BaseViewListener() {
                @Override
                public void hideView(int from, PointF point, boolean finish, boolean needSleep) {

                }

                @Override
                public Activity getActivityContext() {
                    return GlobalSearchActivity.this;
                }
            });
            mLocalSearchLayout.switchExtDisplay(true);
            mLocalSearchLayout.setAppStartListener(mIAppStartListener);
            mLocalSearchLayout.hideHideView();
            mLocalSearchLayout.setVisibility(View.GONE);
        }
        return mLocalSearchLayout;
    }

    @Override
    public void setPresenter(SearchContract.Presenter presenter) {
        mSearchPresenter = presenter;
    }

    @Override
    public void showLocalSearch(final Rect rect) {
        mVoiceHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                LocalSearchLayout localSearch = getLocalSearchLayout();
                ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) mLocalSearchLayout.getLayoutParams();
                lp.height = rect.height();
                lp.width = rect.width();
                lp.topMargin = ExtScreenConstant.CONTAINS_STATUS_NAVI_BAR ? rect.top : rect.top - ExtScreenConstant.STATUS_BAR_HEIGHT;
                lp.leftMargin = rect.left;
                localSearch.setLayoutParams(lp);
                localSearch.setVisibility(View.VISIBLE);
            }
        }, 350);
    }

    @Override
    public void startSearchAnim(List<SequenceActivityLauncher.LaunchItem> launchBoundsList) {
        mSearchAnimLayout.clearItems();
        for (SequenceActivityLauncher.LaunchItem launchItem : launchBoundsList) {
            for (Rect bounds : launchItem.boundsList) {
                mSearchAnimLayout.addItem(bounds,
                        launchItem.isWebItem() ? SearchAnimLayout.PLACE_TYPE_WEB : SearchAnimLayout.PLACE_TYPE_NORMAL);
            }
        }
        mSearchAnimLayout.startAnim(0);
    }

    @Override
    public void hideSearchAnimItem(Rect launchBounds) {
        mSearchAnimLayout.hideItem(launchBounds);
    }

    @Override
    public void clearSearchAnim() {
        mSearchAnimLayout.clearItems();
    }

    @Override
    public Context getContext() {
        return this;
    }

    @Override
    public void doLocalSearch(String keywords) {
        loadLocalData(keywords, getPackageName());
        registerVoiceCallback();
    }

    @Override
    public String getSearchKeywords() {
        return mSearchResult.toString();
    }

    @Override
    public void showTipView(boolean hasSearchEngine, boolean hasResult) {
        mTipsManager.show(mSearchType, hasSearchEngine, hasResult);
    }

    @Override
    public List<String> getStartedAppList() {
        return mStartedPackageList;
    }

    private SearchBubbleView getBubbleView() {
        if (mBubbleView == null) {
            ViewStub bubbleStub = (ViewStub) findViewById(R.id.bubble_stub);
            if (bubbleStub != null && bubbleStub.getParent() != null) {
                mBubbleView = (SearchBubbleView) bubbleStub.inflate();
            } else {
                mBubbleView = (SearchBubbleView) findViewById(R.id.bubble);
            }
            mBubbleView.setOnBubbleSaveListener(mButtonClickListener);
            mBubbleView.setVoiceOperationListener(mVoiceOperationListener);
            mBubbleView.setTextEditorActionListener(mTextEditorActionListener);
            mBubbleView.setBubbleHeightChangeListener(mBubbleHeightChangeListener);
            int topMargin = getResources().getDimensionPixelSize(R.dimen.global_search_margin_top);
            if (ExtScreenConstant.CONTAINS_STATUS_NAVI_BAR) {
                topMargin += ExtScreenConstant.STATUS_BAR_HEIGHT;
            }
            mBubbleView.setBubbleMargin(mBubbleLeftMargin, topMargin);
            mBubbleView.setBubbleMaxWidth(mBubbleWaveMaxWidth);
        }
        return mBubbleView;
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
        mWaveForm.setAnimationListener(mWaveAnimListener);
        mWaveForm.setWaveType(SaraUtils.WaveType.START_WAVE);
        mPopupBottomArrow = mWaveLayout.findViewById(R.id.popup_bottom_arrow);
    }

    private void showNoResultView() {
        showNoResultView(0);
    }

    private void showNoResultView(final int errorCode) {
        if (!mPendingFinishing) {
            mPendingFinishing = true;
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
            mSearchPresenter.closeWindowAndExit(0, mStartedPackageList, true);
        }
    }

    @Override
    protected void localResult(ParcelableObject result) {
        if (isStopped() || mSearchType == SearchPresenter.SEARCH_TYPE_WEB) {
            return;
        }
        LocalSearchLayout localsearch = getLocalSearchLayout();
        List<ApplicationStruct> app = result.getApps();
        List<ContactStruct> contact = result.getContacts();
        List<YellowPageResult> yellow = result.getYellowPages();
        List<MediaStruct> music = result.getMusics();
        if (localsearch != null) {
            localsearch.setData(app, contact, yellow, music);
        }
        boolean hasData = false;
        if (app != null && app.size() > 0
                || contact != null && contact.size() > 0
                || yellow != null && yellow.size() > 0
                || music != null && music.size() > 0) {
            hasData = true;
        }
        mSearchPresenter.setLocalSearchState(true, hasData, mSearchResult.toString());
    }

    @Override
    protected void error(int errorCode) {
        showNoResultView(errorCode);
    }

    @Override
    protected void resultRecived(String resultStr, boolean offline) {
        if (isStopped()) {
            return;
        }
        if (isRecognizing() || mVoiceHandler.hasCallbacks(mStartVoiceRunable)) {
            mSearchResult = new StringBuffer();
            SearchBubbleView bubbleView = getBubbleView();
            bubbleView.cancelAnimators();
            setBubbleState(BubbleSate.INIT);
            LayoutParams bubbleParams = (LayoutParams) bubbleView.getLayoutParams();
            bubbleParams.height = bubbleView.getExactHeight();
            bubbleView.setLayoutParams(bubbleParams);
            return;
        }
        mIsOffLine = offline;
        mIvGaussBlur.setVisibility(View.VISIBLE);
        mButtonViewManager.show();
        stopWaveAnim(false);
        View waveLayout = getWaveLayout();
        waveLayout.clearAnimation();
        waveLayout.setVisibility(View.GONE);
        mSearchResult.append(resultStr);
        optimizeResultPunctutation(mSearchResult, Integer.MAX_VALUE);
        if (TextUtils.isEmpty(mSearchResult)) {
            showNoResultView();
        } else {
            okResultHandled();
            formatBubble(offline);
            SearchBubbleView bubbleView = getBubbleView();
            bubbleView.setText(mSearchResult.toString());
            setBubbleState(BubbleSate.NORMAL);
            performSearch();
        }
    }

    @Override
    protected void resultTimeOut() {
//        showNoResultView();
    }

    @Override
    protected void buffer(byte[] buffer, int totalPoint) {
        if (mWaveForm != null) {
            mWaveForm.waveChanged(buffer, totalPoint);
        }
    }

    @Override
    protected void parcailResult(String partialResult) {
        mSearchResult.append(partialResult);
        mTextPopup.setVisibility(View.GONE);
        SearchBubbleView bubbleView = getBubbleView();
        bubbleView.setText(mSearchResult.toString());
        setBubbleState(BubbleSate.VOICE_INPUT);
    }

    @Override
    protected void recordStarted() {
    }

    @Override
    protected void recordEnded(boolean isShortRecord, boolean isMaxRecord) {
        stopWaveAnim(false);
    }

    void endKeyOrBlueRec() {
        if (getRecordStatus() == RECORD_STATUS_IDLE) {
            finish();
        }
        stopRecognize(false);
    }

    private void resetSearchViewData() {
        if (mLocalSearchLayout != null) {
            mLocalSearchLayout.setData(null, null, null, null);
            mLocalSearchLayout.setVisibility(View.GONE);
        }
    }

    private void startWaveLayoutAnim(long delay) {
        PropertyValuesHolder translateX = PropertyValuesHolder.ofFloat("translationX", mStartWaveTranslationX, 0);
        PropertyValuesHolder scaleX = PropertyValuesHolder.ofFloat("scaleX", 0f, 1f);
        PropertyValuesHolder scaleY = PropertyValuesHolder.ofFloat("scaleY", 0f, 1f);
        PropertyValuesHolder alpha = PropertyValuesHolder.ofFloat("alpha", 0f, 1f);
        final View waveLayout = getWaveLayout();
        Animator anim = ObjectAnimator.ofPropertyValuesHolder(waveLayout, translateX, scaleX, scaleY, alpha);
        anim.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                waveLayout.setVisibility(View.VISIBLE);
            }
        });
        anim.setStartDelay(delay);
        anim.start();
    }

    public void setParamWidth(int width) {
        View waveLayout = getWaveLayout();
        SearchBubbleView bubbleView = getBubbleView();
        LayoutParams bubbleParams = (LayoutParams) bubbleView.getLayoutParams();
        if (!mIsLeftBubble) {
            LayoutParams waveParams = (LayoutParams) waveLayout.getLayoutParams();
            waveParams.leftMargin = mLaunchButtonPos;
            waveParams.leftMargin = Math.max(mLaunchButtonPos - width / 2, -bubbleView.getPaddingLeft());
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

        LayoutParams textPopupParams = (LayoutParams) mTextPopup.getLayoutParams();
        if (textPopupParams.width != width) {
            textPopupParams.width = width;
            mTextPopup.setLayoutParams(textPopupParams);
        }
        bubbleParams.width = width;
        bubbleView.setLayoutParams(bubbleParams);
    }

    private int getWaveTopMargin() {
        if (!mIsLeftBubble) {
            boolean fromClick = mLaunchType == ExtScreenConstant.LAUNCH_FROM_CLICK;
            return getResources().getDimensionPixelSize(fromClick
                    ? R.dimen.revone_click_wave_bottom_margin_top
                    : R.dimen.revone_wave_bottom_margin_top);
        }
        int resId = R.dimen.global_search_wave_margin_top;
        switch (mSearchType) {
            case SearchPresenter.SEARCH_TYPE_GLOBAL:
                resId = R.dimen.global_search_wave_margin_top;
                break;
            case SearchPresenter.SEARCH_TYPE_LOCAL:
                resId = R.dimen.local_search_wave_margin_top;
                break;
            case SearchPresenter.SEARCH_TYPE_WEB:
                resId = R.dimen.internet_search_wave_margin_top;
                break;
        }
        return getResources().getDimensionPixelSize(resId);
    }

    public void formatBubble(boolean offline) {
        int type = offline ? GlobalBubble.TYPE_VOICE_OFFLINE : GlobalBubble.TYPE_VOICE;
        mGlobalBubble = SaraUtils.toGlobalBubble(this, mSearchResult.toString(), type, SaraUtils.getUri(this), SaraUtils.getDefaultBubbleColor(this), 0, 0);
    }

    private void resultFromIntentUpdate(Intent intent) {
        StringBuffer newBubbleText = BubbleManager.updateBubbleFromIntent(intent, mGlobalBubble);
        if (newBubbleText != null) {
            mSearchResult = newBubbleText;
            performSearch();
        }
    }

    private void performSearch() {
        mSearchPresenter.performSearch(mSearchType, mSearchResult.toString());
    }

    private void performReSearch(String text) {
        mSearchResult = new StringBuffer();
        mSearchResult.append(text);
        performReSearch();
    }

    private void performReSearch() {
        if (mLocalSearchLayout != null) {
            mLocalSearchLayout.setVisibility(View.GONE);
        }
        mSearchPresenter.performReSearch(mSearchType, mSearchResult.toString());
    }

    private void searchByKeyword(String keyword) {
        mSearchResult = new StringBuffer();
        mSearchResult.append(keyword);
        mGlobalBubble = SaraUtils.toGlobalBubble(this, mSearchResult.toString(), GlobalBubble.TYPE_TEXT, null, SaraUtils.getDefaultBubbleColor(this), 0, 0);
        SearchBubbleView bubbleView = getBubbleView();
        bubbleView.setText(mSearchResult.toString());
        setBubbleState(BubbleSate.NORMAL);
        mButtonViewManager.show();
        doLocalSearch(mSearchResult.toString());
        performSearch();
    }

    public void hideBubbleViewWithAnim() {
        View bubble = getBubbleView();
        ObjectAnimator translateAnimator = ObjectAnimator.ofFloat(bubble, "translationX", 0, getScreenWidth());
        translateAnimator.setDuration(250);
        translateAnimator.setInterpolator(new AccelerateInterpolator(1.0f));
        translateAnimator.addListener(new Animator.AnimatorListener() {

            @Override
            public void onAnimationStart(Animator animation) {
            }

            @Override
            public void onAnimationRepeat(Animator animation) {
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                mSearchPresenter.closeWindowAndExit(0, mStartedPackageList, true);
            }

            @Override
            public void onAnimationCancel(Animator animation) {
            }
        });
        translateAnimator.start();
    }

    private WaveView.AnimationListener mWaveAnimListener = new WaveView.AnimationListener() {
        @Override
        public void onAnimationEnd(int width, boolean isCanceled) {
            if (!isCanceled) {
                SearchBubbleView bubbleView = getBubbleView();
                mTextPopup.setVisibility(View.GONE);
                if (!TextUtils.isEmpty(mSearchResult)) {
                    setBubbleState(BubbleSate.VOICE_INPUT);
                } else {
                    setBubbleState(BubbleSate.LOADING);
                }
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
    };

    private SearchBubbleView.OnButtonClickListener mButtonClickListener = new SearchBubbleView.OnButtonClickListener() {
        @Override
        public void onSaveButtonClick() {
            if (!SaraUtils.isSettingEnable(GlobalSearchActivity.this)) {
                ToastUtil.showToast(GlobalSearchActivity.this, R.string.ideapills_not_opened, Toast.LENGTH_SHORT);
                return;
            }
            if (mGlobalBubble != null && (!TextUtils.isEmpty(mGlobalBubble.getText()))) {
                SaraUtils.recordFile(GlobalSearchActivity.this, mIsOffLine, mGlobalBubble.getText());
                mGlobalBubble.setUri(SaraUtils.getUri(GlobalSearchActivity.this));
                BubbleManager.addBubble2SideBar(GlobalSearchActivity.this, mGlobalBubble, null, mIsOffLine, true);
                hideBubbleViewWithAnim();
            }
        }

        @Override
        public void onBoomButtonClick(View view) {
            if (mGlobalBubble != null && (!TextUtils.isEmpty(mGlobalBubble.getText()))) {
                startBoomActivity(view, mGlobalBubble.getText());
            }
        }
    };

    public void startBoomActivity(View view, final String text) {
        Intent intent = new Intent(ACTION_BOOM_TEXT);
        intent.putExtra(Intent.EXTRA_TEXT, text);
        int[] pos = new int[2];
        view.getLocationOnScreen(pos);
        intent.putExtra("boom_startx", pos[0]);
        intent.putExtra("boom_starty", pos[1]);
        intent.putExtra("caller_pkg", GlobalSearchActivity.this.getPackageName());
        intent.putExtra("show_all_text", true);
        intent.putExtra("force_callback", true);
        intent.putExtra("editable_resource", true);
        intent.putExtra("enter_edit_mode", true);
        final GlobalBubble item = mGlobalBubble;
        intent.setFlags(Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        intent.addFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
        intent.setComponent(new ComponentName("com.smartisanos.textboom", "com.smartisanos.textboom.BoomActivity"));
        RemoteCallback callback = new RemoteCallback(new RemoteCallback.OnResultListener() {
            @Override
            public void onResult(Bundle result) {
                BubbleManager.markAddBubble2List(true);
                final String newText = result.getString(Intent.EXTRA_RETURN_RESULT);
                if (newText != null) {
                    if (!newText.trim().equals(item.getText())) {
                        if (CommonUtils.getStringLength(newText.trim()) <= SaraConstant.BUBBLE_TEXT_MAX) {
                            item.setText(newText.trim());
                        } else {
                            item.setText(text.trim());
                            ToastSmt.getInstance().makeText(GlobalSearchActivity.this, GlobalSearchActivity.this.getResources().getString(
                                    R.string.bubble_add_string_limit), Toast.LENGTH_SHORT,
                                    WindowManager.LayoutParams.TYPE_TOAST).show();
                        }
                        final String text = item.getText();
                        mBubbleView.setEllipsizeText(text);
                        if (mGlobalBubble.getType() == GlobalBubble.TYPE_VOICE_OFFLINE) {
                            mGlobalBubble.setType(GlobalBubble.TYPE_VOICE);
                        }
                        performReSearch(text);
                    }
                }
            }
        }, new Handler());
        intent.putExtra("smartisanos.textboom.REMOTE_CALLBACK", callback);
        intent.putExtra("ideapills_content", true);
        BubbleManager.markAddBubble2List(false);
        SaraUtils.startActivity(GlobalSearchActivity.this, intent, true);
    }

    @Override
    protected boolean allowStartActivity(ComponentName component) {
        if (mStartedAppManager != null) {
            return !mStartedAppManager.hasStartActivity(component);
        }
        return true;
    }

    private IVoiceOperationListener mVoiceOperationListener = new IVoiceOperationListener() {
        @Override
        public void onCancel() {
            mSearchPresenter.closeWindowAndExit(0, mStartedPackageList, true);
        }

        @Override
        public void onDone() {
            stopRecognize(false);
        }
    };

    private ButtonViewManager.ICloseListener mCloseListener = new ButtonViewManager.ICloseListener() {
        @Override
        public void onClose() {
            stopRecognize(false);
            mSearchPresenter.closeWindowAndExit(0, mStartedPackageList, true);
        }
    };

    private ButtonViewManager.ISettingListener mSettingListener = new ButtonViewManager.ISettingListener() {
        @Override
        public void showSettingWindow(View view) {
            if (mSettingManager == null) {
                mSettingManager = new SettingManager(mSettingChangeListener);
            }
            mSettingManager.showPopupList(view, mSearchType);
        }
    };

    private ViewHolderApp.IAppStartListener mIAppStartListener = new ViewHolderApp.IAppStartListener() {
        @Override
        public void startApplication(Intent intent) {
            mSearchPresenter.closeWindowAndExit(0, mStartedPackageList, true, intent);
        }
    };

    private SettingManager.ISettingChangeListener mSettingChangeListener = new SettingManager.ISettingChangeListener() {
        @Override
        public void onSettingChange(final int select) {
            performReSearch();
        }
    };

    private SearchBubbleView.TextEditorActionListener mTextEditorActionListener = new SearchBubbleView.TextEditorActionListener() {
        @Override
        public void onActionDone(int keyCode, String text) {
            switch (keyCode) {
                case KeyEvent.KEYCODE_ENTER:
                case KeyEvent.KEYCODE_SEARCH:
                    performReSearch(text);
                    break;
            }
        }

        @Override
        public void onTextChange(String text, boolean complete) {
            final GlobalBubble item = mGlobalBubble;
            if (item != null) {
                item.setText(text);
            }
            if (complete) {
                performReSearch(text);
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
            final View waveLayout = getWaveLayout();
            if (deltaTransY != 0) {
                return ObjectAnimator.ofFloat(waveLayout, "translationY", waveLayout.getTranslationY(), waveLayout.getTranslationY() + deltaTransY);
            }
            return null;
        }

        @Override
        public void updateWaveHeight(int targetHeight) {

        }
    };

    private Runnable mStartVoiceRunable = new Runnable() {
        @Override
        public void run() {
            preStartVoiceRecognition();
        }
    };
}