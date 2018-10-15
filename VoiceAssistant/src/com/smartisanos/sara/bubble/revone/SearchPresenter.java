package com.smartisanos.sara.bubble.revone;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Point;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;

import com.smartisanos.ideapills.common.util.MultiSdkUtils;
import com.smartisanos.ideapills.common.util.MutiTaskHandler;
import com.smartisanos.ideapills.common.util.UIHandler;
import com.smartisanos.sara.bubble.revone.entity.LocalSearchItem;
import com.smartisanos.sara.bubble.revone.entity.WebSearchItem;
import com.smartisanos.sara.bubble.revone.manager.SettingManager;
import com.smartisanos.sara.bubble.revone.manager.StartedAppManager;
import com.smartisanos.sara.bubble.revone.utils.ISearchDisplayPosHelper;
import com.smartisanos.sara.bubble.revone.utils.MixSearchDisplayPosHelper;
import com.smartisanos.sara.bubble.revone.utils.NoScaleSearchDisplayPosHelper;
import com.smartisanos.sara.bubble.revone.utils.SearchDisplayPosHelper;
import com.smartisanos.sara.bubble.revone.utils.SearchParams;
import com.smartisanos.sara.bubble.revone.utils.SequenceActivityLauncher;
import com.smartisanos.sara.util.SaraConstant;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class SearchPresenter implements SearchContract.Presenter, SequenceActivityLauncher.OnLaunchListener {
    public static final String TAG = "VoiceAss.SearchPresenter";
    public static final int SEARCH_TYPE_GLOBAL = 0;
    public static final int SEARCH_TYPE_LOCAL = 1;
    public static final int SEARCH_TYPE_WEB = 2;
    private static final int DELAY_ENSURE_CLOSE_MILLS = 2000;

    private SearchContract.View mSearchView;

    private SearchDisplayPosHelper mSearchDisplayPosHelper;
    private SearchDisplayPosHelper mSearchWebDisplayPosHelper;
    private MixSearchDisplayPosHelper mSearchMixDisplayPosHelper;
    private Rect mTempRect = new Rect();
    private SequenceActivityLauncher mSequenceActivityLauncher;
    private int mSearchType = SEARCH_TYPE_GLOBAL;
    private StartedAppManager mStartedAppManager;
    private Runnable mDelayEnsureCloseRunnable;
    private volatile boolean mHasLocalData = false;
    private volatile boolean mLocalSearchDone = false;
    private volatile boolean mAppSearchFinish = false;
    private volatile List<LocalSearchItem> mResultApps = new ArrayList<>();
    private static HashMap<String, String> sAppProviderInfo = new HashMap<String, String>();

    static {
        sAppProviderInfo.put("com.android.calendar", "content://com.android.calendar.adapt");
        sAppProviderInfo.put("com.android.settings", "content://com.android.settings.SearchSuggestionProvider");
        sAppProviderInfo.put("com.android.mms", "content://com.android.mms.SuggestionsProvider");
        sAppProviderInfo.put("com.smartisanos.notes", "content://com.smartisanos.notes.notesinfo");
        sAppProviderInfo.put("com.android.email", "content://com.android.email.provider");
    }

    private static final List<String> sExceptionApp = Arrays.asList(
            "com.smartisanos.sara",
            "com.smartisanos.music",
            "com.smartisanos.filemanager");

    public SearchPresenter(SearchContract.View searchView, StartedAppManager appManager) {
        mSearchView = searchView;
        Context context = mSearchView.getContext();
        Point point = new Point();
        context.getDisplay().getSize(point);
        SearchParams.init(point);
        mStartedAppManager = appManager;
        mSequenceActivityLauncher = new SequenceActivityLauncher(this);
        mSearchDisplayPosHelper = new SearchDisplayPosHelper(context, point, SearchParams.getColumnInScreen(),
                SearchParams.getLocalSearchRowInScreen());
        mSearchWebDisplayPosHelper = new NoScaleSearchDisplayPosHelper(context, point, SearchParams.getColumnInScreen(),
                SearchParams.getWebSearchRowInScreen());
        mSearchMixDisplayPosHelper = new MixSearchDisplayPosHelper(context, point, SearchParams.getColumnInScreen(),
                SearchParams.getWebSearchRowInScreen(), SearchParams.getLocalSearchRowInScreen());
        mSearchView.setPresenter(this);
    }

    private void performSearchDirectly(final List<LocalSearchItem> searchApps, final int type, final String keyword) {
        if (mSearchView.isFinishing() || !keyword.equals(mSearchView.getSearchKeywords())) {
            return;
        }
        UIHandler.post(new Runnable() {
            @Override
            public void run() {
                doSearch(searchApps, type, keyword);
            }
        });
    }

    private void performSearchWaitResult(final String keywords) {
        if (TextUtils.isEmpty(keywords)) {
            return;
        }
        MutiTaskHandler.post(new Runnable() {
            @Override
            public void run() {
                Context context = mSearchView.getContext();
                List<LocalSearchItem> resultApps = SettingManager.getCheckedSearchApp(context, mSearchType);
                if (resultApps != null) {
                    mResultApps.addAll(resultApps);
                    for (LocalSearchItem item : resultApps) {
                        String packageName = item.getPackageName();
                        String uriStr = sAppProviderInfo.get(packageName);
                        if (!sExceptionApp.contains(packageName)) {
                            if (!TextUtils.isEmpty(uriStr)) {
                                Uri uri = Uri.parse(uriStr);
                                Bundle bundle;
                                try {
                                    bundle = context.getContentResolver().call(uri, "searchAppContent", keywords, null);
                                } catch (Exception e) {
                                    bundle = null;
                                }
                                boolean hasData = bundle != null && bundle.getBoolean("has_data");
                                if (!hasData) {
                                    mResultApps.remove(item);
                                }
                            } else {
                                mResultApps.remove(item);
                            }
                        }
                    }
                    mAppSearchFinish = true;
                }
                if (mLocalSearchDone || (keywords != null && keywords.length() >= SaraConstant.INTERVAL_SHORT)) {
                    if (!mHasLocalData) {
                        for (LocalSearchItem item : mResultApps) {
                            final String packageName = mSearchView.getContext().getPackageName();
                            if (packageName.equals(item.getPackageName())) {
                                mResultApps.remove(item);
                                break;
                            }
                        }
                    }
                    performSearchDirectly(mResultApps, mSearchType, keywords);
                }
            }
        });
    }

    private void doSearch(List<LocalSearchItem> searchApp, int type, String keywords) {
        mSearchType = type;
        mSequenceActivityLauncher.clear();
        mSearchView.clearSearchAnim();
        int resultCount = 0;
        switch (type) {
            case SEARCH_TYPE_GLOBAL: {
                setStartingSearchWindows(true);
                List<WebSearchItem> webSearchItems = SettingManager.getCheckedWebSearchEngine(mSearchView.getContext(), mSearchType);
                if (searchApp == null) {
                    searchApp = SettingManager.getCheckedSearchApp(mSearchView.getContext(), mSearchType);
                }
                if (webSearchItems != null || searchApp != null) {
                    // 如果使用到1080p 可显示的数目减少，需要调整数量
                    int tempBrowserSearchCount = webSearchItems == null ? 0 : webSearchItems.size();
                    int tempAppCount = searchApp == null ? 0 : searchApp.size();
                    while (SearchParams.isMoreThanMaxSearchItems(tempAppCount, tempBrowserSearchCount)
                            && (tempAppCount > 0 || tempBrowserSearchCount > 0)) {
                        if (searchApp != null && searchApp.size() > 0) {
                            searchApp.remove(searchApp.size() - 1);
                        } else if (webSearchItems != null && webSearchItems.size() > 0) {
                            webSearchItems.remove(webSearchItems.size() - 1);
                        }
                        tempBrowserSearchCount = webSearchItems == null ? 0 : webSearchItems.size();
                        tempAppCount = searchApp == null ? 0 : searchApp.size();
                    }
                }
                int browserSearchCount = webSearchItems == null ? 0 : webSearchItems.size();
                int appCount = searchApp == null ? 0 : searchApp.size();
                resultCount = browserSearchCount + appCount;
                mSearchMixDisplayPosHelper.setFirstTypeCountLimit(browserSearchCount);
                mSearchMixDisplayPosHelper.setFixCenterAlignMode(browserSearchCount, appCount);
                searchInBrowser(webSearchItems, keywords, 0, mSearchMixDisplayPosHelper, true);
                searchInApp(searchApp, keywords, browserSearchCount, mSearchMixDisplayPosHelper);
                break;
            }
            case SEARCH_TYPE_LOCAL: {
                setStartingSearchWindows(true);
                if (searchApp == null) {
                    searchApp = SettingManager.getCheckedSearchApp(mSearchView.getContext(), mSearchType);
                }
                if (searchApp != null) {
                    int maxLocalSearchItems = SearchParams.getMaxLocalSearchItems();
                    while (searchApp.size() > 0 && searchApp.size() > maxLocalSearchItems) {
                        searchApp.remove(searchApp.size() - 1);
                    }
                }
                int appCount = searchApp == null ? 0 : searchApp.size();
                resultCount = appCount;
                mSearchDisplayPosHelper.setFixCenterAlignMode(appCount);
                searchInApp(searchApp, keywords, 0, mSearchDisplayPosHelper);
                break;
            }
            case SEARCH_TYPE_WEB: {
                List<WebSearchItem> webSearchItems = SettingManager.getCheckedWebSearchEngine(mSearchView.getContext(), mSearchType);
                if (webSearchItems != null) {
                    // 如果使用到1080p 可显示的数目减少，需要调整数量
                    int maxWebSearchItems = SearchParams.getMaxWebSearchItems();
                    while (webSearchItems.size() > 0 && webSearchItems.size() > maxWebSearchItems) {
                        webSearchItems.remove(webSearchItems.size() - 1);
                    }
                }
                int browserSearchCount = webSearchItems == null ? 0 : webSearchItems.size();
                resultCount = browserSearchCount;
                mSearchWebDisplayPosHelper.setFixCenterAlignMode(browserSearchCount);
                searchInBrowser(webSearchItems, keywords, 0, mSearchWebDisplayPosHelper, true);
                break;
            }
        }
        boolean hasSearchEngine = false;
        if (SettingManager.hasSearchEngine(mSearchView.getContext(), mSearchType)) {
            hasSearchEngine = true;
        }
        mSearchView.showTipView(hasSearchEngine, resultCount > 0);
        mSearchView.startSearchAnim(mSequenceActivityLauncher.getLaunchList());
    }

    @Override
    public void performSearch(final int type, final String keywords) {
        mSearchType = type;
        mAppSearchFinish = false;
        mResultApps.clear();
        if (mSearchType != SearchPresenter.SEARCH_TYPE_WEB) {
            performSearchWaitResult(keywords);
        } else {
            performSearchDirectly(null, mSearchType, keywords);
        }
    }

    public void performReSearch(final int type, final String keywords) {
        final StartedAppManager.IAppCloseListener listener = new StartedAppManager.IAppCloseListener() {
            @Override
            public void onAppCloseEnd() {
                UIHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (!TextUtils.isEmpty(keywords)) {
                            if (mSearchType != SearchPresenter.SEARCH_TYPE_WEB) {
                                mSearchView.doLocalSearch(keywords);
                            }
                            performSearch(type, keywords);
                        } else {
                            mSearchView.showTipView(SettingManager.hasSearchEngine(mSearchView.getContext(), type), false);
                        }
                    }
                });
            }
        };
        closeWindowAndExit(mSearchView.getStartedAppList(), false, listener);
    }

    @Override
    public void setLocalSearchState(boolean searchDone, boolean hasData, String keywords) {
        mLocalSearchDone = searchDone;
        mHasLocalData = hasData;
        if (searchDone && mAppSearchFinish) {
            if (!hasData) {
                final String packageName = mSearchView.getContext().getPackageName();
                for (LocalSearchItem item : mResultApps) {
                    if (packageName.equals(item.getPackageName())) {
                        mResultApps.remove(item);
                        break;
                    }
                }
            }
            performSearchDirectly(mResultApps, mSearchType, keywords);
        }
    }

    @Override
    public void clearPendingSearch() {
        mSequenceActivityLauncher.clear();
        mSearchView.clearSearchAnim();
        setStartingSearchWindows(false);
    }

    private int searchInApp(List<LocalSearchItem> searchApp, String keywords, int startPosition,
                            ISearchDisplayPosHelper searchDisplayPosHelper) {
        int count = 0;
        if (searchApp != null && searchApp.size() > 0) {
            for (int i = 0; i < searchApp.size(); i++) {
                LocalSearchItem item = searchApp.get(i);
                if (mSearchView.isFinishing()) {
                    break;
                }
                if (!item.isChecked()) {
                    continue;
                }
                searchDisplayPosHelper.getDisplayRect(count + startPosition, mTempRect);
                if (SettingManager.launchLocalSearchItem(mSearchView.getContext(), mTempRect, item, keywords, mSequenceActivityLauncher)) {
                    count++;
                }
            }
        }
        return count;
    }

    private int searchInBrowser(List<WebSearchItem> webSearchItems, String keywords, int startPosition,
                                ISearchDisplayPosHelper searchDisplayPosHelper, boolean isMergeSearch) {
        int count = 0;
        List<Rect> rectList = new ArrayList<>();
        List<WebSearchItem> items = new ArrayList<>();
        List<String> keywordsList = new ArrayList<>();
        if (webSearchItems != null && webSearchItems.size() > 0) {
            for (int i = 0; i < webSearchItems.size(); i++) {
                WebSearchItem item = webSearchItems.get(i);
                if (mSearchView.isFinishing()) {
                    break;
                }
                if (!item.isChecked()) {
                    continue;
                }
                searchDisplayPosHelper.getDisplayRect(count + startPosition, mTempRect);
                if (isMergeSearch) {
                    rectList.add(new Rect(mTempRect));
                    items.add(item);
                    keywordsList.add(keywords);
                    count++;
                } else {
                    if (SettingManager.launchWebSearchItem(mSearchView.getContext(), mTempRect, item, keywords, mSequenceActivityLauncher)) {
                        count++;
                    }
                }
            }
        }
        if (!rectList.isEmpty()) {
            SettingManager.mergeLaunchWebIntent(mSearchView.getContext(), rectList, items, keywordsList, mSequenceActivityLauncher);
        }
        return count;
    }

    @Override
    public List<Rect> getSearchRectList() {
        List<Rect> list = new ArrayList<>();
        list.addAll(mSearchDisplayPosHelper.getStandardRectList());
        list.addAll(mSearchWebDisplayPosHelper.getStandardRectList());
        return list;
    }

    @Override
    public void cancelDelayCloseWindow() {
        if (mDelayEnsureCloseRunnable != null) {
            UIHandler.removeCallbacks(mDelayEnsureCloseRunnable);
            mDelayEnsureCloseRunnable = null;
        }
    }

    @Override
    public void delayCloseWindow(int selectTaskId, List<String> packageList) {
        delayCloseWindow(selectTaskId, packageList, DELAY_ENSURE_CLOSE_MILLS);
    }

    @Override
    public void delayCloseWindow(int selectTaskId, List<String> packageList, int delay) {
        if (mDelayEnsureCloseRunnable != null) {
            UIHandler.removeCallbacks(mDelayEnsureCloseRunnable);
        }
        mDelayEnsureCloseRunnable = new Runnable() {
            @Override
            public void run() {
                if (mStartedAppManager != null) {
                    mStartedAppManager.closeStartedApp(selectTaskId, packageList, false, null);
                }
            }
        };
        UIHandler.postDelayed(mDelayEnsureCloseRunnable, delay);
    }

    @Override
    public void closePhoneApp(Activity activity, int searchType) {
        clearPendingSearch();
        final List<String> openedPkgList = SettingManager.getSearchAppPkgs(activity.getApplicationContext(), searchType);
        mStartedAppManager.closePhoneApp(openedPkgList);
    }

    @Override
    public void closeWindowAndExit(int task, List<String> packageList, boolean exit) {
        clearPendingSearch();
        mStartedAppManager.closeStartedApp(task, packageList, exit, null);
    }

    @Override
    public void closeWindowAndExit(int task, List<String> packageList, boolean exit, Intent intent) {
        clearPendingSearch();
        mStartedAppManager.closeStartedApp(task, packageList, exit, intent);
    }

    @Override
    public void closeWindowAndExit(List<String> packageList, boolean exit, StartedAppManager.IAppCloseListener listener) {
        clearPendingSearch();
        mStartedAppManager.closeStartedApp(0, packageList, exit, null, listener);
    }

    private void setStartingSearchWindows(boolean isStarting) {
        MultiSdkUtils.SetStartingSearchWindows(isStarting);
    }

    @Override
    public void onLaunchFinish() {
        //mSearchView.clearSearchAnim();
        setStartingSearchWindows(false);
    }

    @Override
    public void onLaunchSelfLocal(Rect bounds) {
        mSearchView.showLocalSearch(bounds);
    }
}
