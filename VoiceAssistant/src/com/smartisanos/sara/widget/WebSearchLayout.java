package com.smartisanos.sara.widget;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.SparseIntArray;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.Window;
import android.webkit.DownloadListener;
import android.webkit.JsResult;
import android.webkit.ValueCallback;
import android.webkit.WebBackForwardList;
import android.webkit.WebChromeClient;
import android.webkit.WebHistoryItem;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.media.AudioManager;
import android.os.Build;

import com.smartisanos.sanbox.utils.SaraTracker;
import com.smartisanos.sara.R;
import com.smartisanos.sara.SaraApplication;
import com.smartisanos.sara.bubble.BubbleActivity;
import com.smartisanos.sara.bubble.WebListener;
import com.smartisanos.sara.bubble.revone.manager.SettingManager;
import com.smartisanos.sara.bubble.manager.BubbleManager;
import com.smartisanos.sara.util.DialogUtils;
import com.smartisanos.sara.util.LogUtils;
import com.smartisanos.sara.util.MultiSdkAdapter;
import com.smartisanos.sara.util.PackageUtil;
import com.smartisanos.sara.util.SaraConstant;
import com.smartisanos.sara.util.SaraUtils;
import com.smartisanos.sara.util.SharePrefUtil;
import com.smartisanos.sara.util.SaraUtils.BaseViewListener;

import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import smartisanos.api.SettingsSmt;

public class WebSearchLayout extends FrameLayout implements WebListener {


    private ValueCallback<Uri[]> uploadMessage;
    private SaraUtils.BaseViewListener mListener;
    private ImageView mGoBack;
    private ImageView mGoForward;
    private View mProgess;
    // mWebView may be null. cause of lazy add
    private WebView mWebView;
    private View mBrowser;
    private View mColse;
    private String mSearchText = "";
    private boolean mFirstPage;

    private boolean mHasRetried;
    private boolean mIsReloading;
    private String mUrl = "";
    private int mOldSearchType;
    private boolean mReceivedError = false;
    private static final String SEARCH_WEB = "search_web";
    private static final String SEARCH_DICT = "search_dict";
    private static final String SEARCH_WIKI = "search_wiki";
    private static final String SEARCH_THIRDPARTY = "search_thirdparty";

    private static final String IS_NEED_LOADURL = "is_need_loadurl";

    final static String SCHEME_WTAI = "wtai://wp/";
    final static String SCHEME_WTAI_MC = "wtai://wp/mc;";

    public static final int TYPE_BAIDU = SettingsSmt.TEXT_BOOM_SEARCH_VALUE.TYPE_BAIDU;
    public static final int TYPE_GOOGLE = SettingsSmt.TEXT_BOOM_SEARCH_VALUE.TYPE_GOOGLE;
    public static final int TYPE_BING = SettingsSmt.TEXT_BOOM_SEARCH_VALUE.TYPE_BING;
    public static final int TYPE_SOGOU = SettingsSmt.TEXT_BOOM_SEARCH_VALUE.TYPE_SOGOU;
    public static final int TYPE_360SO = SettingsSmt.TEXT_BOOM_SEARCH_VALUE.TYPE_360SO;
    public static final int TYPE_HUDONG_BAIKE = SettingsSmt.TEXT_BOOM_SEARCH_VALUE.TYPE_WIKI;
    public static final int TYPE_BAIDU_BAIKE = SettingsSmt.TEXT_BOOM_SEARCH_VALUE.TYPE_BAIKE;
    public static final int TYPE_WIKI_PEDIA_EN = SettingsSmt.TEXT_BOOM_SEARCH_VALUE.TYPE_WIKI_EN;
    public static final int TYPE_WIKI_PEDIA_CN = SettingsSmt.TEXT_BOOM_SEARCH_VALUE.TYPE_WIKI_CN;
    public static final int TYPE_YOUDAO = SettingsSmt.TEXT_BOOM_SEARCH_VALUE.TYPE_YOUDAO;
    public static final int TYPE_KINGSOFT = SettingsSmt.TEXT_BOOM_SEARCH_VALUE.TYPE_KINGSOFT;
    public static final int TYPE_BINGDICT = SettingsSmt.TEXT_BOOM_SEARCH_VALUE.TYPE_BINGDICT;
    public static final int TYPE_HIDICT = SettingsSmt.TEXT_BOOM_SEARCH_VALUE.TYPE_HIDICT;
    public static final int TYPE_GOOGLE_TRANSLATE = SettingsSmt.TEXT_BOOM_SEARCH_VALUE.TYPE_GOOGLE_TRANSLATE;
    public static final int TYPE_SEARCH_WECHAT = SettingsSmt.TEXT_BOOM_SEARCH_VALUE.TYPE_SEARCH_WEIXIN;
    public static final int TYPE_SEARCH_WEIBO = SettingsSmt.TEXT_BOOM_SEARCH_VALUE.TYPE_SEARCH_WEIBO;
    public static final int TYPE_SEARCH_ZHIHU = SettingsSmt.TEXT_BOOM_SEARCH_VALUE.TYPE_SEARCH_ZHIHU;

    public static final SparseIntArray sWebSearchIndexMap = new SparseIntArray();
    public static final SparseIntArray sDictSearchIndexMap = new SparseIntArray();
    public static final SparseIntArray sWikiSearchIndexMap = new SparseIntArray();
    public static final SparseIntArray sThirdpartyIndexMap = new SparseIntArray();

    static {
        sWebSearchIndexMap.put(TYPE_BAIDU, 0);
        sWebSearchIndexMap.put(TYPE_GOOGLE, 1);
        sWebSearchIndexMap.put(TYPE_BING, 2);
        sWebSearchIndexMap.put(TYPE_SOGOU, 3);
        sWebSearchIndexMap.put(TYPE_360SO, 4);
    }
    static {
        sDictSearchIndexMap.put(TYPE_BINGDICT, 0);
        sDictSearchIndexMap.put(TYPE_KINGSOFT, 1);
        sDictSearchIndexMap.put(TYPE_YOUDAO, 2);
        sDictSearchIndexMap.put(TYPE_GOOGLE_TRANSLATE, 3);
    }
    static {
        sWikiSearchIndexMap.put(TYPE_BAIDU_BAIKE, 0);
        sWikiSearchIndexMap.put(TYPE_HUDONG_BAIKE, 1);
        sWikiSearchIndexMap.put(TYPE_WIKI_PEDIA_EN, 2);
        sWikiSearchIndexMap.put(TYPE_WIKI_PEDIA_CN, 3);
    }
    static {
        sThirdpartyIndexMap.put(TYPE_SEARCH_WECHAT, 0);
        sThirdpartyIndexMap.put(TYPE_SEARCH_WEIBO, 1);
        sThirdpartyIndexMap.put(TYPE_SEARCH_ZHIHU, 2);
    }

    public static final HashMap<String, String> sSearchEngineResMap = new HashMap<String, String>();
    static {
        sSearchEngineResMap.put("com.UCMobile", "win_browser_uc");
        sSearchEngineResMap.put("com.tencent.mtt", "win_browser_qq");
        sSearchEngineResMap.put("com.baidu.searchbox", "win_browser_baidu");
        sSearchEngineResMap.put("com.qihoo.browser", "win_browser_360");
        sSearchEngineResMap.put("com.android.chrome", "win_browser_chrome");
        sSearchEngineResMap.put("com.oupeng.browser", "win_browser_oupeng");
        sSearchEngineResMap.put("sogou.mobile.explorer", "win_browser_sogou");
        sSearchEngineResMap.put("com.ijinshan.browser_fast", "win_browser_liebao");
    }

    private int mArrowHorrizontalOffset;
    private int mSearchType;
    private int mWebSearchType;
    private int mDictSearchType;
    private int mWikiSearchType;
    private int mThirdpartyType;
    private RadioButton mSearchWeb;
    private RadioButton mSearchWiki;
    private RadioButton mSearchDict;
    private RadioButton mThirdparty;

    private OptionPopupWindow mOptionPopupWindow;
    private boolean mIsNeedLoadUrl = true;
    private SearchPopupWindow mSearchPopupWindow;

    private PackageManager mPackageManager;
    private Resources mResources;
    private Context mContext;
    private MultiSdkAdapter mAdapter;

    private AudioManager mAudioManager;

    private int mCurrentWebHeight = 0;
    public WebSearchLayout(Context context) {
        super(context);
    }

    public WebSearchLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public WebSearchLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public void init() {
        mContext = getContext();
        initContentView();
        mAdapter = new MultiSdkAdapter();
        updateWebview();
        setupViews();
    }

    public void setSearchText(String str) {
        if (TextUtils.isEmpty(str)) {
            str = "";
        }
        mSearchText = str;
    }

    public void showSearchPopupWindow(int height) {
        mCurrentWebHeight = height;
        performSearchTypeChange(SEARCH_WEB, R.array.search_engine_text, R.array.search_type_engine_icon, mSearchWeb, sWebSearchIndexMap);
    }

    public void hideSearchPopupWindow() {
        if (mSearchPopupWindow != null) {
            mSearchPopupWindow.hide();
        }
    }

    public String getSearchText() {
        return mSearchText;
    }

    public void loadUrl() {
        mFirstPage = true;

        if (mWebView == null) {
            setupWebView();
        }
        if (mWebView != null) {
            mWebView.clearView();
            mWebView.loadUrl(getOriUrl());
        }
    }

    private void initContentView() {
        mArrowHorrizontalOffset = getContext().getResources().getDimensionPixelOffset(R.dimen.popup_arrow_horrizontal_offset);
        mProgess = findViewById(R.id.search_progress);
        mColse = findViewById(R.id.search_setting);
        mGoBack = (ImageView) findViewById(R.id.go_back);
        mGoForward = null;
        mBrowser = findViewById(R.id.goto_browser);
        mSearchWeb = (RadioButton) findViewById(R.id.search_web);
        mSearchWiki = (RadioButton) findViewById(R.id.search_wiki);
        mSearchDict = (RadioButton) findViewById(R.id.search_dict);
        mThirdparty = (RadioButton) findViewById(R.id.search_thirdparty);

        mWebSearchType = mDictSearchType = mWikiSearchType = mThirdpartyType = mSearchType = -1;
        mResources = getResources();
        mPackageManager = getContext().getPackageManager();
    }

    private void setupWebView() {
        FrameLayout mFlView = (FrameLayout) findViewById(R.id.fl_webview);
        mWebView = new WebView(SaraApplication.getInstance().getApplicationContext());
        mWebView.setOverScrollMode(OVER_SCROLL_NEVER);
        mWebView.setClickable(true);
        mFlView.addView(mWebView,0);
        // // init web settings
        WebSettings settings = mWebView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setJavaScriptCanOpenWindowsAutomatically(true);
        settings.setBlockNetworkImage(false);
        settings.setDomStorageEnabled(true);

        settings.supportZoom();
        settings.setUseWideViewPort(true);
        settings.setLoadWithOverviewMode(true);
        settings.setLayoutAlgorithm(WebSettings.LayoutAlgorithm.TEXT_AUTOSIZING);
        mAdapter.setWebSetting(settings);
        mAdapter.setWebListener(this);
        mWebView.setWebViewClient(mAdapter.getWebViewClient());

        mWebView.setWebChromeClient(mAdapter.getWebChromeClient());
        DownloadListener downLoadListener = mAdapter.getDownloadListener();
        if (downLoadListener != null) {
            mWebView.setDownloadListener(downLoadListener);
        }
        mWebView.resumeTimers();
    }

    public interface onCheckChangedListener {
        void onCheckChanged(int newType);
    }

    private OnClickListener mOnClickListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.search_setting:
                    try {
                        Intent intent = new Intent();
                        intent.setClassName("com.android.settings",
                                "com.android.settings.GlobalSearchCategoryActivity");
                        BubbleManager.markAddBubble2List(false);
                        SaraUtils.startActivity(mContext, intent, true);
                    } catch (ActivityNotFoundException e) {
                        LogUtils.e(e);
                    }
                    // finish();
                    break;
                case R.id.goto_browser:
                    gotoBrowser();
                    break;
                case R.id.go_back:
                    if (mWebView != null && mWebView.canGoBack()) {
                        mWebView.goBack();
                    }
                    break;
            }
        }
    };

    private CompoundButton.OnCheckedChangeListener mCheckedChangeListener = new CompoundButton.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(CompoundButton buttonView,
                                     boolean isChecked) {
            boolean isSearchType = SaraUtils.getBubbleType(mContext) == SaraUtils.BUBBLE_TYPE.SHELL_SEARCH;
            if (buttonView == mSearchWeb) {
                if (isChecked) {
                    mProgess.setTranslationX(0);
                    if (isSearchType) {
                        performSearchTypeChange(SEARCH_WEB, R.array.search_engine_text, R.array.search_type_engine_icon, mSearchWeb, sWebSearchIndexMap);
                    }
                    performSearch(getSearchInfo(SEARCH_WEB));
                }
            } else if (buttonView == mSearchDict) {
                if (isChecked) {
                    mProgess.setTranslationX(buttonView.getWidth());
                    if (isSearchType) {
                        performSearchTypeChange(SEARCH_DICT, R.array.search_dict_text, R.array.search_type_dict_icon, mSearchDict, sDictSearchIndexMap);
                    }
                    performSearch(getSearchInfo(SEARCH_DICT));
                }
            } else if (buttonView == mSearchWiki) {
                if (isChecked) {
                    mProgess.setTranslationX(buttonView.getWidth() * 2);
                    if (isSearchType) {
                        performSearchTypeChange(SEARCH_WIKI, R.array.search_wiki_text, R.array.search_type_wiki_icon, mSearchWiki, sWikiSearchIndexMap);
                    }
                    performSearch(getSearchInfo(SEARCH_WIKI));
                }
            } else if (buttonView == mThirdparty) {
                if (isChecked) {
                    mProgess.setTranslationX(buttonView.getWidth() * 3);
                    if (isSearchType) {
                        performSearchTypeChange(SEARCH_THIRDPARTY, R.array.search_thirdparty_text, R.array.search_type_thirdparty_icon, mThirdparty, sThirdpartyIndexMap);
                    }
                    performSearch(getSearchInfo(SEARCH_THIRDPARTY));
                }
            }
        }
    };

    private void performTypeChange(final String type, final int textResId, int iconResId, final RadioButton searchButton, final SparseIntArray map, int[] position, int index) {
        if (mOptionPopupWindow != null) {
            mOptionPopupWindow.hide();
        }
        final int search_type = getSearchInfo(type);
        mOptionPopupWindow = new OptionPopupWindow(getContext(),
                textResId, iconResId,
                map,
                map.get(search_type), index,
                new onCheckChangedListener() {
                    @Override
                    public void onCheckChanged(int newType) {
                        if (search_type != newType) {
                            searchButton.setButtonDrawable(getIconResByType(newType));
                            searchButton.setContentDescription(getDescription(textResId, map.get(newType)));
                            putSearchInfo(type, newType);
                            if (searchButton.isChecked()) {
                                performSearch(newType);
                            } else {
                                searchButton.setChecked(true);
                            }
                        }
                    }
                }, position[1]);
        mOptionPopupWindow.show((View) searchButton);
    }

    private void performSearchTypeChange(final String type, final int textResId, int iconResId, final RadioButton searchButton, final SparseIntArray map) {
        if (mSearchPopupWindow != null) {
            mSearchPopupWindow.hide();
        }
        final int search_type = getSearchInfo(type);
        mSearchPopupWindow = new SearchPopupWindow(getContext(),
                textResId, iconResId,
                map,
                map.get(search_type), -1,
                new onCheckChangedListener() {
                    @Override
                    public void onCheckChanged(int newType) {
                        searchButton.setButtonDrawable(getIconResByType(newType));
                        searchButton.setContentDescription(getDescription(textResId, map.get(newType)));
                        putSearchInfo(type, newType);
                        if (searchButton.isChecked()) {
                            performSearch(newType);
                        } else {
                            searchButton.setChecked(true);
                        }
                    }
                }, mCurrentWebHeight);
        mSearchPopupWindow.show((View) searchButton);
    }

    private OnLongClickListener mLongClickListener = new OnLongClickListener() {
        @Override
        public boolean onLongClick(View v) {
            int[] position = new int[2];
            v.getLocationOnScreen(position);
            if (v == mSearchWeb) {
                performTypeChange(SEARCH_WEB, R.array.search_engine_text, R.array.search_engine_icon, mSearchWeb, sWebSearchIndexMap, position, -1);
                return true;
            } else if (v == mSearchDict) {
                performTypeChange(SEARCH_DICT, R.array.search_dict_text, R.array.search_dict_icon, mSearchDict, sDictSearchIndexMap, position, 0);
                return true;
            } else if (v == mSearchWiki) {
                performTypeChange(SEARCH_WIKI, R.array.search_wiki_text, R.array.search_wiki_icon, mSearchWiki, sWikiSearchIndexMap, position, 1);
                return true;
            } else if (v == mThirdparty) {
                performTypeChange(SEARCH_THIRDPARTY, R.array.search_thirdparty_text, R.array.search_thirdparty_icon, mThirdparty, sThirdpartyIndexMap, position, 2);
                return true;
            }
            return false;
        }
    };

    private void setupViews() {

        boolean isSearchType = SaraUtils.getBubbleType(mContext) == SaraUtils.BUBBLE_TYPE.SHELL_SEARCH;
        mColse.setOnClickListener(mOnClickListener);
        mBrowser.setOnClickListener(mOnClickListener);
        mGoBack.setOnClickListener(mOnClickListener);

        if (!isSearchType) {
            mSearchWeb.setOnLongClickListener(mLongClickListener);
            mSearchDict.setOnLongClickListener(mLongClickListener);
            mSearchWiki.setOnLongClickListener(mLongClickListener);
            mThirdparty.setOnLongClickListener(mLongClickListener);
        }
        mSearchWeb.setOnCheckedChangeListener(mCheckedChangeListener);
        mSearchDict.setOnCheckedChangeListener(mCheckedChangeListener);
        mSearchWiki.setOnCheckedChangeListener(mCheckedChangeListener);
        mThirdparty.setOnCheckedChangeListener(mCheckedChangeListener);

    }

    public void reset(){
        mWebSearchType = mDictSearchType = mWikiSearchType = mThirdpartyType = mSearchType = -1;
        updateWebview();
    }

    public void setViewListener(BaseViewListener listener){
        mListener = listener;
    }

    public void onRestoreInstanceState(Bundle savedInstanceState) {
        mIsNeedLoadUrl = savedInstanceState.getBoolean(IS_NEED_LOADURL);
        if (mWebView != null){
            mWebView.restoreState(savedInstanceState);
        }
    }

    public void onSaveInstanceState(Bundle outState) {
        outState.putBoolean(IS_NEED_LOADURL, false);
        if (mWebView !=null){
            mWebView.saveState(outState);
        }
    }

    public void onResume() {
        if (mWebView != null) {
            mWebView.onResume();
            mWebView.resumeTimers();
        }
        LogUtils.d("mSearchType: " + mSearchType);
        mOldSearchType = mSearchType;
        updateWebview();
        if (mOldSearchType != mSearchType) {
            performSearch(mSearchType);
        }
    }

    private void updateWebview() {
        mIsNeedLoadUrl = true;
        final int webType = Settings.Global.getInt(getContext().getContentResolver(), SettingsSmt.Global.TEXT_BOOM_SEARCH_METHOD, TYPE_SOGOU);
        final int dictType = Settings.Global.getInt(getContext().getContentResolver(), SettingsSmt.Global.BIG_BANG_DEFAULT_DICT, TYPE_YOUDAO);
        final int wikiType = Settings.Global.getInt(getContext().getContentResolver(), SettingsSmt.Global.GLOBAL_DEFAULT_WIKI, TYPE_BAIDU_BAIKE);
        final int thirdpartyType = Settings.Global.getInt(getContext().getContentResolver(), SettingsSmt.Global.THIRD_SEARCH_DEFAULT, TYPE_SEARCH_WECHAT);
        if (mSearchType < TYPE_HUDONG_BAIKE) {
            if (webType != mWebSearchType) {
                mSearchType = mWebSearchType = webType;
            }
            putSearchInfo(SEARCH_WEB, mSearchType);
            mSearchWeb.setChecked(true);

            if (wikiType != mWikiSearchType) {
                mWikiSearchType = wikiType;
                putSearchInfo(SEARCH_WIKI, wikiType);
            }
            if (dictType != mDictSearchType) {
                mDictSearchType = dictType;
                putSearchInfo(SEARCH_DICT, dictType);
            }
            if (thirdpartyType != mThirdpartyType) {
                mThirdpartyType = thirdpartyType;
                putSearchInfo(SEARCH_THIRDPARTY, thirdpartyType);
            }
        } else if (mSearchType < TYPE_YOUDAO) {
            if (wikiType != mWikiSearchType) {
                mSearchType = mWikiSearchType = wikiType;
            }

            putSearchInfo(SEARCH_WIKI, mSearchType);
            mSearchWiki.setChecked(true);

            if (webType != mWebSearchType) {
                mWebSearchType = webType;
                putSearchInfo(SEARCH_WEB, webType);
            }
            if (dictType != mDictSearchType) {
                mDictSearchType = dictType;
                putSearchInfo(SEARCH_DICT, dictType);
            }
            if (thirdpartyType != mThirdpartyType) {
                mThirdpartyType = thirdpartyType;
                putSearchInfo(SEARCH_THIRDPARTY, thirdpartyType);
            }
        } else if (mSearchType < TYPE_SEARCH_WECHAT){
            if (dictType != mDictSearchType) {
                mSearchType = mDictSearchType = dictType;
            }
            putSearchInfo(SEARCH_DICT, mSearchType);
            mSearchDict.setChecked(true);
            if (webType != mWebSearchType) {
                mWebSearchType = webType;
                putSearchInfo(SEARCH_WEB, webType);
            }
            if (wikiType != mWikiSearchType) {
                mWikiSearchType = wikiType;
                putSearchInfo(SEARCH_WIKI, wikiType);
            }
            if (thirdpartyType != mThirdpartyType) {
                mThirdpartyType = thirdpartyType;
                putSearchInfo(SEARCH_THIRDPARTY, thirdpartyType);
            }
        } else{
            if (thirdpartyType != mThirdpartyType) {
                mSearchType = mThirdpartyType = thirdpartyType;
            }
            putSearchInfo(SEARCH_THIRDPARTY, mSearchType);
            mThirdparty.setChecked(true);
            if (webType != mWebSearchType) {
                mWebSearchType = webType;
                putSearchInfo(SEARCH_WEB, webType);
            }
            if (dictType != mDictSearchType) {
                mDictSearchType = dictType;
                putSearchInfo(SEARCH_DICT, dictType);
            }
            if (wikiType != mWikiSearchType) {
                mWikiSearchType = wikiType;
                putSearchInfo(SEARCH_WIKI, wikiType);
            }
        }
        int search_type = getSearchInfo(SEARCH_WEB);
        mSearchWeb.setButtonDrawable(getIconResByType(search_type));
        mSearchWeb.setContentDescription(getDescription(R.array.search_engine_text, sWebSearchIndexMap.get(search_type)));
        search_type = getSearchInfo(SEARCH_DICT);
        mSearchDict.setButtonDrawable(getIconResByType(search_type));
        mSearchDict.setContentDescription(getDescription(R.array.search_dict_text, sDictSearchIndexMap.get(search_type)));
        search_type = getSearchInfo(SEARCH_WIKI);
        mSearchWiki.setButtonDrawable(getIconResByType(search_type));
        mSearchWiki.setContentDescription(getDescription(R.array.search_wiki_text, sWikiSearchIndexMap.get(search_type)));
        search_type = getSearchInfo(SEARCH_THIRDPARTY);
        mThirdparty.setButtonDrawable(getIconResByType(search_type));
        mThirdparty.setContentDescription(getDescription(R.array.search_thirdparty_text, sThirdpartyIndexMap.get(search_type)));
        LogUtils.d("mSearchType: " + mSearchType);

        updateBrowserIcon();
        popupWindowHide();
    }

    private void popupWindowHide() {
        if (mOptionPopupWindow != null) {
            mOptionPopupWindow.hide();
       }
    }

    public void onPause() {
        if (mWebView != null){
            mWebView.onPause();
            mWebView.pauseTimers();
        }
    }

    public void onDestroy() {
        if (mWebView == null) {
            return;
        }
        ViewParent parent = mWebView.getParent();
        if (parent != null) {
            ((ViewGroup)parent).removeView(mWebView);
        }
        mWebView.stopLoading();
        mWebView.getSettings().setJavaScriptEnabled(false);
        mWebView.clearHistory();
        mWebView.clearView();
        mWebView.removeAllViews();
        mWebView.destroy();
        mWebView = null;
    }

    public boolean onTouchEvent(MotionEvent event) {
        // Remove slop for shadow
        if (event.getAction() == MotionEvent.ACTION_DOWN && mListener != null) {
            final Window window = mListener.getActivityContext().getWindow();
            final int x = (int) event.getX();
            final int y = (int) event.getY();
            final View decorView = window.getDecorView();
            if (x < 0 || x > decorView.getWidth() || y < 0 || y > decorView.getHeight()) {
                if (window.peekDecorView() != null) {
                    // finish();
                    return true;
                }
            }
        }
        return false;
    }

    private String getOriUrl() {
        return SettingManager.getOriUrl(mSearchType, mSearchText);
    }

    private int getIconResByType(int type) {
        int resId = 0;
        switch (type) {
            case TYPE_BAIDU:
                resId = R.drawable.win_search_baidu;
                break;
            case TYPE_GOOGLE:
                resId = R.drawable.win_search_google;
                break;
            case TYPE_BING:
                resId = R.drawable.win_search_bing;
                break;
            case TYPE_SOGOU:
                resId = R.drawable.win_search_sogou;
                break;
            case TYPE_360SO:
                resId = R.drawable.win_search_360;
                break;
            case TYPE_HUDONG_BAIKE:
                resId = R.drawable.win_search_hudongdict;
                break;
            case TYPE_BAIDU_BAIKE:
                resId = R.drawable.win_search_baike;
                break;
            case TYPE_YOUDAO:
                resId = R.drawable.win_search_youdao;
                break;
            case TYPE_KINGSOFT:
                resId = R.drawable.win_search_kingsoft;
                break;
            case TYPE_BINGDICT:
                resId = R.drawable.win_search_bingdict;
                break;
            case TYPE_GOOGLE_TRANSLATE:
                resId = R.drawable.boom_win_search_googletrans;
                break;
            case TYPE_WIKI_PEDIA_EN:
                resId = R.drawable.boom_win_search_wiki_en;
                break;
            case TYPE_WIKI_PEDIA_CN:
                resId = R.drawable.boom_win_search_wiki_cn;
                break;
            case TYPE_SEARCH_WECHAT:
                resId = R.drawable.boom_win_search_weixin;
                break;
            case TYPE_SEARCH_WEIBO:
                resId = R.drawable.boom_win_search_weibo;
                break;
            case TYPE_SEARCH_ZHIHU:
                resId = R.drawable.boom_win_search_zhihu;
                break;
        }
        return resId;
    }

    private String getSearchInfoByType(int type) {
        switch (type) {
            case TYPE_BAIDU:
                return "Baidu";
            case TYPE_GOOGLE:
                return "Google";
            case TYPE_BING:
                return "Bing";
            case TYPE_SOGOU:
                return "Sogou";
            case TYPE_360SO:
                return "360";
            case TYPE_HUDONG_BAIKE:
                return "Hudong Baike";
            case TYPE_BAIDU_BAIKE:
                return "Baidu Baike";
            case TYPE_YOUDAO:
                return "Youdao Dictionary";
            case TYPE_KINGSOFT:
                return "Iciba";
            case TYPE_BINGDICT:
                return "Bing Dictionary";
            case TYPE_GOOGLE_TRANSLATE:
                return "Google Translate";
            case TYPE_WIKI_PEDIA_EN:
                return "Wiki Engish";
            case TYPE_WIKI_PEDIA_CN:
                return "Wiki Chinese";
            case TYPE_SEARCH_WECHAT:
                return "Wechat";
            case TYPE_SEARCH_WEIBO:
                return "Weibo";
            case TYPE_SEARCH_ZHIHU:
                return "Zhihu";
            default:
                return "Unknown";
        }
    }

    private CharSequence getDescription(int resId, int index) {
        if (resId > 0 && index >= 0) {
            Resources res = mContext.getResources();
            String[] description = res.getStringArray(resId);
            if (description != null && index < description.length) {
                return description[index];
            }
        }
        return null;
    }

    private void performSearch(int type) {
        mSearchType = type;
        mFirstPage = true;
        if (mIsNeedLoadUrl && getVisibility() == View.VISIBLE && mWebView !=null) {
             mWebView.clearHistory();
             mWebView.loadUrl(getOriUrl());
        }
    }

    private int getSearchInfo(String key) {
        int defaultValue = TYPE_SOGOU;
        if (key.equals(SEARCH_DICT)) {
            defaultValue = TYPE_YOUDAO;
        } else if (key.equals(SEARCH_WIKI)) {
            defaultValue = TYPE_BAIDU_BAIKE;
        } else if (key.equals(SEARCH_THIRDPARTY)) {
            defaultValue = TYPE_SEARCH_WECHAT;
        }
        return SharePrefUtil.getSearchInfoValue(getContext(), key, defaultValue);
    }

    private void putSearchInfo(String key, int value) {
        SharePrefUtil.setSearchInfoValue(getContext(), key, value);
    }

    private void updateBrowserIcon() {
        try {
            ResolveInfo resolveInfo = getDefaultResolveInfo();
            if (resolveInfo == null) {
                LogUtils.e("resolveInfo is null");
                return;
            }
            String defaultSearchEngineName = resolveInfo.activityInfo.packageName;
            // don't set default browser
            if (TextUtils.equals("smartisanos", defaultSearchEngineName)
                    || TextUtils.equals("android", defaultSearchEngineName)) {
                mBrowser.setBackgroundResource(R.drawable.win_browser_smartisan);
                return;
            }
            // redraw browser icon
            for (Map.Entry<String, String> entry : sSearchEngineResMap
                    .entrySet()) {
                if (TextUtils.equals(entry.getKey(), defaultSearchEngineName)) {
                    int iconResId = mResources.getIdentifier(entry.getValue(),
                            "drawable", getContext().getPackageName());
                    // use system util create circle bitmap.
                    if (iconResId > 0) {
                        Bitmap circleBitmap = smartisanos.util.NameAvatarController.getCircleBitmap(
                                BitmapFactory.decodeResource(mResources, iconResId), false);
                        mBrowser.setBackground(new BitmapDrawable(mResources, circleBitmap));
                    }
                    return;
                }
            }
            // other browser icon
            ApplicationInfo info = mPackageManager.getApplicationInfo(defaultSearchEngineName, 0);
            BitmapDrawable bitmapDrawable = (BitmapDrawable) info.loadIcon(mPackageManager);
            if (bitmapDrawable != null
                    && !bitmapDrawable.getBitmap().isRecycled()) {
                Bitmap circleBitmap = smartisanos.util.NameAvatarController
                        .getCircleBitmap(bitmapDrawable.getBitmap(), false);
                mBrowser.setBackground(new BitmapDrawable(mResources, circleBitmap));
            }
        } catch (Exception e) {
            LogUtils.e(e);
        }
    }

    private ResolveInfo getDefaultResolveInfo() {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(new Uri.Builder().scheme("http").authority("*").build());
        return mPackageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY);
    }

    private void gotoBrowser() {
        if (mWebView == null) {
            return;
        }
        String url = mUrl;
        if (TextUtils.isEmpty(url)) {
            final String webViewOriUrl = mWebView.getOriginalUrl();
            if (TextUtils.isEmpty(webViewOriUrl)) {
                url = getOriUrl();
            } else {
                url = webViewOriUrl;
            }
        }
        Uri uri = Uri.parse(url);
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_VIEW);
        intent.setData(uri);

        ResolveInfo resolveInfo = getDefaultResolveInfo();
        if (TextUtils.equals("smartisanos", resolveInfo.activityInfo.packageName)
                || TextUtils.equals("android", resolveInfo.activityInfo.packageName)) {
            intent.setPackage(SaraConstant.BROWSER_PACKAGE_NAME_SMARTISAN);
        } else {
            intent.setPackage(resolveInfo.activityInfo.packageName);
        }
        //pass arbitrary pendingIntent to browser for being verify calling uid
        PendingIntent fakePi = PendingIntent.getActivity(mContext, 0, new Intent(mContext, BubbleActivity.class),
                PendingIntent.FLAG_UPDATE_CURRENT);
        intent.putExtra("trusted_application_code_extra", fakePi);
        WebBackForwardList historyList = mWebView.copyBackForwardList();
        int size = historyList == null ? 0 : historyList.getSize();
        if (size > 1) {
            ArrayList<String> history = new ArrayList<String>();
            for (int i = 0; i < size; i++) {
                WebHistoryItem historyItem = historyList.getItemAtIndex(i);
                history.add(historyItem.getUrl());
            }
            intent.putExtra("currentItemIndex", historyList.getCurrentIndex());
            intent.putStringArrayListExtra("shareHistory", history);
        }
        SaraUtils.startActivity(getContext(), intent);
    }

    public void onActivityResult(int resultCode, Intent data) {
        if (uploadMessage == null) {
            return;
        }
        Uri[] uris = mAdapter.parseIntent(resultCode, data);
        uploadMessage.onReceiveValue(uris);
        uploadMessage = null;
    }

    @Override
    public boolean shouldOverrideUrlLoading(String url) {
        if (!url.startsWith("http://") && !url.startsWith("https://") && !mFirstPage) {
            try {
                Intent intent = new Intent().parseUri(url, Intent.URI_INTENT_SCHEME);
                if (intent != null) {
                    if (mWebView != null) {
                        mWebView.stopLoading();
                    }
                    ResolveInfo info = mPackageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY);
                    if (info != null) {
                        SaraUtils.startActivity(getContext(), intent);
                    } else {
                        //fix some web page can not call phone
                        if (url.startsWith(SCHEME_WTAI)) {
                            // wtai://wp/mc;number
                            // number=string(phone-number)
                            if (url.startsWith(SCHEME_WTAI_MC)) {
                                Intent intentTel = new Intent(Intent.ACTION_VIEW,
                                        Uri.parse(WebView.SCHEME_TEL +
                                                url.substring(SCHEME_WTAI_MC.length())));
                                SaraUtils.startActivity(getContext(), intentTel);
                            }
                        }
                    }
                    return true;
                }
            } catch (URISyntaxException e) {
                LogUtils.e("Can't resolve url: " + e);
            }
        }
        return false;
    }
    @Override
    public boolean onShowFileChooser(Intent intent, ValueCallback<Uri[]> filePathCallback) {
        try {
            if (uploadMessage != null) {
                uploadMessage.onReceiveValue(null);
                uploadMessage = null;
            }

            uploadMessage = filePathCallback;
            if (mListener != null) {
                Intent resultIntent = Intent.createChooser(intent, mContext.getText(R.string.select_attachment_type));
                resultIntent.putExtra("window_type", SaraConstant.BUBBLE_WINDOW_TYPE);
                resultIntent.putExtra("FLAG_SHOW_WHEN_LOCKED", true);
                SaraUtils.startActivityForResult(mListener.getActivityContext(), resultIntent, SaraConstant.REQUEST_SELECT_FILE);
            }
        } catch (ActivityNotFoundException e) {
            uploadMessage = null;
            return false;
        } catch (Exception e) {
            LogUtils.e("start activity for result exception " + e);
        }
        return true;
    }

    @Override
    public void onReceiveValue(Uri[] uris) {
        if (uploadMessage == null) {
            return;
        }
        uploadMessage.onReceiveValue(uris);
        uploadMessage = null;
    }
    @Override
    public void onReceivedTitle(String title) {
        if (!mReceivedError){
            SaraTracker.onEvent("A420020", "search_type", getSearchInfoByType(mSearchType));
        }
        mReceivedError = false;
    }

    @Override
    public boolean onReceivedError(int errorcode) {
        mReceivedError = true;
        if (!mHasRetried) {
            mHasRetried = true;
            if (errorcode == WebViewClient.ERROR_UNKNOWN) {
                mIsReloading = true;
                if (mWebView != null){
                    mWebView.reload();
                }
            }
            LogUtils.w("Fail to load page, error code=" + errorcode + " des=");
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void onProgressChanged(int progress) {
        mProgess.setVisibility(progress < 100 ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onPageStarted(String url) {
        if (mFirstPage) {
            LogUtils.d("onPageStarted is first page");
            mGoBack.setEnabled(false);
            if (mGoForward != null)
                mGoForward.setEnabled(false);
        } else if(mWebView != null) {
            LogUtils.d("onPageStarted is not first page = " + mWebView.canGoBack());
            mGoBack.setEnabled(mWebView.canGoBack());
            if (mGoForward != null)
                mGoForward.setEnabled(mWebView.canGoForward());
        }
        mIsReloading = false;
        mUrl = url;
    }

    @Override
    public void onDownloadStart(String url) {
        Uri uri = Uri.parse(url);
        final Intent intent = new Intent(Intent.ACTION_VIEW, uri);
        final Context context = getContext();
        post(new Runnable() {
            @Override public void run() {
                if (context instanceof Activity && PackageUtil.isDefaultAppChosen(context, intent)) {
                    ((Activity) context).finish();
                }
                SaraUtils.startActivity(context, intent, true);
            }
        });
    }

    @Override
    public void onPageFinishedBefore() {
        if (mFirstPage) {
            mFirstPage = false;
            if (mWebView != null){
                mWebView.goBackOrForward(Integer.MIN_VALUE);
                mWebView.clearHistory();
            }
        }
    }

    @Override
    public void onPageFinishedAfter() {
        if (!mIsReloading) {
            mHasRetried = false;
        }
        if (mWebView != null){
            LogUtils.d("onPageFinishedAfter mWebView.canGoBack() = " + mWebView.canGoBack());
            mGoBack.setEnabled(mWebView.canGoBack());
            if (mGoForward != null)
                mGoForward.setEnabled(mWebView.canGoForward());
        } else {
            LogUtils.d("onPageFinishedAfter mWebView is null");
            mGoBack.setEnabled(false);
            if (mGoForward != null)
                mGoForward.setEnabled(false);
        }

    }

    @Override
    public void onShowDialog(int type, String url, String message, JsResult result) {
        DialogUtils.showCustomJsDialog(getContext(), type, url, message, result);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        BubbleManager.markAddBubble2List(false);
        return super.onInterceptTouchEvent(ev);
    }
}
