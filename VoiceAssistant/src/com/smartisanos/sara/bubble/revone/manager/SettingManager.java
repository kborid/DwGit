package com.smartisanos.sara.bubble.revone.manager;

import android.app.ActivityOptions;
import android.content.ClipData;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.support.customtabs.CustomTabsIntent;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.PopupWindow;

import com.smartisanos.sara.R;
import com.smartisanos.sara.bubble.revone.ExtScreenConstant;
import com.smartisanos.sara.bubble.revone.GlobalSearchActivity;
import com.smartisanos.sara.bubble.revone.SearchPresenter;
import com.smartisanos.sara.bubble.revone.adapter.LocalSearchAdapter;
import com.smartisanos.sara.bubble.revone.adapter.WebSearchAdapter;
import com.smartisanos.sara.bubble.revone.entity.LocalSearchItem;
import com.smartisanos.sara.bubble.revone.entity.SearchItem;
import com.smartisanos.sara.bubble.revone.entity.WebSearchItem;
import com.smartisanos.sara.bubble.revone.utils.SearchParams;
import com.smartisanos.sara.bubble.revone.utils.SequenceActivityLauncher;
import com.smartisanos.sara.bubble.revone.widget.DragListView;
import com.smartisanos.sara.bubble.revone.drag.DragSortListView;
import com.smartisanos.sara.util.SaraConstant;
import com.smartisanos.sara.util.SharePrefUtil;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import smartisanos.api.IntentSmt;
import smartisanos.api.SettingsSmt;
import smartisanos.util.LogTag;

public class SettingManager implements ISettingBase {
    public static final String TAG = "VoiceAss.SettingManager";

    private static final String LOCAL_SEARCH_APP_INFO = "local_search_app_info";
    private static final String WEB_SEARCH_ENGINE_INFO = "web_search_engine_info";
    private static final String GLOBAL_LOCAL_SEARCH_APP_INFO = "global_local_search_app_info";
    private static final String GLOBAL_WEB_SEARCH_ENGINE_INFO = "global_web_search_engine_info";

    private static final String SMARTISAN_COMMON_SEARCH_ACTION = "com.smartisanos.action.SEARCH";

    private static final int LOCAL_LAUNCH_TYPE_ACTIVITY = 1;
    private static final int LOCAL_LAUNCH_TYPE_ACTION = 2;

    private static final int SEARCH_VALUE_TYPE_TAOBAO = 0x700;
    private static final int SEARCH_VALUE_TYPE_JD = 0x701;
    private static final int SEARCH_VALUE_TYPE_TOUTIAO = 0x702;
    private static final int SEARCH_VALUE_TYPE_HAOQIXIN = 0x703;
    private static final int SEARCH_VALUE_TYPE_DOUBAN = 0x704;
    private static final int SEARCH_VALUE_TYPE_YOUKU = 0x705;
    private static final int SEARCH_VALUE_TYPE_QQVIDEO = 0x706;
    private static final int SEARCH_VALUE_TYPE_BING = 0x707;
    private static final int SEARCH_VALUE_TYPE_DIANPING = 0x708;

    private static final int SEARCH_VALUE_TYPE_YOUTUBE = 0x750;
    private static final int SEARCH_VALUE_TYPE_AMAZON = 0x751;
    private static final int SEARCH_VALUE_TYPE_YAHOO = 0x752;
    private static final int SEARCH_VALUE_TYPE_NG = 0x753;
    private static final int SEARCH_VALUE_TYPE_DDG = 0x754;
    private static final int SEARCH_VALUE_TYPE_TWITTER = 0x755;
    private static final int SEARCH_VALUE_TYPE_FB = 0x756;
    private static final int SEARCH_VALUE_TYPE_EBAY = 0x757;
    private static final int SEARCH_VALUE_TYPE_QUORA = 0x758;
    private static final int SEARCH_VALUE_TYPE_PINTEREST = 0x759;
    private static final int SEARCH_VALUE_TYPE_YELP = 0x760;

    private PopupWindow mPopupWindow;
    private View mLocalTitle;
    private View mWebTitle;
    private DragListView mLocalListView;
    private DragListView mWebListView;
    private Context mContext;
    boolean mIsResorted = false;
    private LocalSearchAdapter mLocalAdapter;
    private WebSearchAdapter mWebAdapter;
    private int mSearchType;
    private ISettingChangeListener mSettingChangeListener;

    public SettingManager(ISettingChangeListener listener) {
        mSettingChangeListener = listener;
    }

    public void showPopupList(View view, int searchType) {
        if (mPopupWindow == null) {
            mContext = view.getContext();
            Resources resources = mContext.getResources();
            View contentView = LayoutInflater.from(view.getContext()).inflate(R.layout.revone_search_setting_detail, null);
            mLocalTitle = contentView.findViewById(R.id.local_title);
            mLocalListView = (DragListView) contentView.findViewById(R.id.local_list);
            mLocalAdapter = new LocalSearchAdapter(mContext, null, getSearchApp(mContext, searchType));
            mLocalAdapter.setSettingManager(this);
            mLocalListView.setAdapter(mLocalAdapter);
            mLocalListView.setDropListener(mLocalDropListener);
            mLocalListView.setDragEnabled(true);
            mWebTitle = contentView.findViewById(R.id.web_title);
            mWebListView = (DragListView) contentView.findViewById(R.id.web_list);
            mWebAdapter = new WebSearchAdapter(mContext, null, getWebSearchEngine(mContext, searchType));
            mWebAdapter.setSettingManager(this);
            mWebListView.setAdapter(mWebAdapter);
            mWebListView.setDropListener(mWebDropListener);
            mWebListView.setDragEnabled(true);
            mPopupWindow = new PopupWindow(mContext);
            mPopupWindow.setWindowLayoutMode(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            mPopupWindow.setContentView(contentView);
            mPopupWindow.setOutsideTouchable(true);
            mPopupWindow.setBackgroundDrawable(null);
            mPopupWindow.setFocusable(true);
            mPopupWindow.setWindowLayoutType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
            mPopupWindow.setOnDismissListener(mOnDismissListener);
        } else {
            mLocalAdapter.setData(getSearchApp(mContext, searchType));
            mWebAdapter.setData(getWebSearchEngine(mContext, searchType));
        }
        mSearchType = searchType;
        mLocalTitle.setVisibility(searchType != SearchPresenter.SEARCH_TYPE_WEB ? View.VISIBLE : View.GONE);
        mLocalListView.setVisibility(searchType != SearchPresenter.SEARCH_TYPE_WEB ? View.VISIBLE : View.GONE);
        mWebTitle.setVisibility(searchType != SearchPresenter.SEARCH_TYPE_LOCAL ? View.VISIBLE : View.GONE);
        mWebListView.setVisibility(searchType != SearchPresenter.SEARCH_TYPE_LOCAL ? View.VISIBLE : View.GONE);
        onItemCheckedChange();
        int width = mContext.getResources().getDimensionPixelSize(R.dimen.popup_window_width);
        int margin = mContext.getResources().getDimensionPixelSize(R.dimen.global_setting_window_margin_right);
        int buttonWidth = view.getWidth();
        mPopupWindow.showAsDropDown(view, buttonWidth + margin - width, 0);
    }

    public void hidePopupList() {
        if (mPopupWindow != null && mPopupWindow.isShowing()) {
            mPopupWindow.dismiss();
        }
    }

    private DragSortListView.DropListener mLocalDropListener = new DragSortListView.DropListener() {
        @Override
        public void drop(int from, int to) {
            if (from != to) {
                mIsResorted = true;
                LocalSearchItem item = (LocalSearchItem) mLocalAdapter.getItem(from);
                mLocalAdapter.remove(from);
                mLocalAdapter.insert(item, to);
            }
        }
    };

    private DragSortListView.DropListener mWebDropListener = new DragSortListView.DropListener() {
        @Override
        public void drop(int from, int to) {
            if (from != to) {
                mIsResorted = true;
                WebSearchItem item = (WebSearchItem) mWebAdapter.getItem(from);
                mWebAdapter.remove(from);
                mWebAdapter.insert(item, to);
            }
        }
    };

    public static boolean hasSearchEngine(Context context, int searchType) {
        int searchCount = 0;
        if (searchType != SearchPresenter.SEARCH_TYPE_LOCAL) {
            List<WebSearchItem> webItems = getWebSearchEngine(context, searchType);
            if (webItems != null) {
                for (WebSearchItem item : webItems) {
                    if (item.isChecked()) {
                        searchCount++;
                    }
                }
            }
        }
        if (searchType != SearchPresenter.SEARCH_TYPE_WEB) {
            List<LocalSearchItem> localItems = getSearchApp(context, searchType);
            if (localItems != null) {
                for (LocalSearchItem item : localItems) {
                    if (item.isChecked()) {
                        searchCount++;
                    }
                }
            }
        }
        return searchCount > 0;
    }

    public static List<LocalSearchItem> getCheckedSearchApp(Context context, int searchType) {
        List<LocalSearchItem> searchItems = getSearchApp(context, searchType, true);
        if (searchItems == null) {
            return searchItems;
        }
        List<LocalSearchItem> checkedItems = new ArrayList<>();
        for (LocalSearchItem localSearchItem : searchItems) {
            if (localSearchItem.isChecked()) {
                checkedItems.add(localSearchItem);
            }
        }
        return checkedItems;
    }

    public static List<LocalSearchItem> getSearchApp(Context context, int searchType) {
        return getSearchApp(context, searchType, true);
    }

    private static List<LocalSearchItem> getSearchApp(Context context, int searchType, boolean isSaveDefault) {
        if (searchType == SearchPresenter.SEARCH_TYPE_WEB) {
            return null;
        }
        List<LocalSearchItem> searchApp = null;
        String saveKey = searchType == SearchPresenter.SEARCH_TYPE_LOCAL ? LOCAL_SEARCH_APP_INFO : GLOBAL_LOCAL_SEARCH_APP_INFO;
        String appInfo = SharePrefUtil.getString(context, saveKey, "");
        if (!TextUtils.isEmpty(appInfo)) {
            searchApp = new ArrayList<>();
            try {
                JSONArray json = new JSONArray(appInfo);
                for (int i = 0; i < json.length(); i++) {
                    LocalSearchItem item = new LocalSearchItem();
                    item.fromJSON(new JSONObject(json.getString(i)));
                    searchApp.add(item);
                }
            } catch (JSONException e) {
            }
        } else {
            searchApp = getDefaultSearchApp(context, searchType == SearchPresenter.SEARCH_TYPE_LOCAL);
            if (isSaveDefault) {
                saveSearchData(context, searchApp, saveKey);
            }
        }
        return searchApp;
    }

    public static List<String> getSearchAppPkgs(Context context, int searchType) {
        boolean isContainsWebPkg = searchType != SearchPresenter.SEARCH_TYPE_LOCAL;
        boolean isContainsLocalPkg = searchType != SearchPresenter.SEARCH_TYPE_WEB;
        List<String> pkgs = new ArrayList<>();
        if (isContainsLocalPkg) {
            List<LocalSearchItem> searchApps = getSearchApp(context, searchType, false);
            if (searchApps != null) {
                for (LocalSearchItem searchItem : searchApps) {
                    if (!searchItem.getPackageName().equals(context.getPackageName())) {
                        pkgs.add(searchItem.getPackageName());
                    }
                }
            }
        }
        if (isContainsWebPkg) {
            pkgs.add(SaraConstant.BROWSER_PACKAGE_NAME_SMARTISAN);
        }
        return pkgs;
    }

    private static List<LocalSearchItem> getDefaultSearchApp(Context context, boolean checked) {
        List<LocalSearchItem> searchApp = new ArrayList<>();
        String[] packageNames = context.getResources().getStringArray(R.array.local_search_package);
        String[] launchNames = context.getResources().getStringArray(R.array.local_search_key);
        int[] types = context.getResources().getIntArray(R.array.local_search_launch_type);
        int count = packageNames.length;
        LocalSearchItem item = new LocalSearchItem();
        item.mChecked = checked;
        item.mPackageName = context.getPackageName();
        item.mDisplayName = context.getString(R.string.rev_ideapills_local_search);
        searchApp.add(item);
        for (int i = 0; i < count; i++) {
            item = new LocalSearchItem();
            item.mChecked = checked;
            item.mPackageName = packageNames[i];
            if (types[i] == LOCAL_LAUNCH_TYPE_ACTION) {
                item.setActionName(launchNames[i]);
            } else {
                item.setClassName(launchNames[i]);
            }
            item.mDisplayName = getAppLabel(context, packageNames[i]);
            searchApp.add(item);
        }
        return searchApp;
    }

    public static List<WebSearchItem> getCheckedWebSearchEngine(Context context, int searchType) {
        List<WebSearchItem> searchItems = getWebSearchEngine(context, searchType);
        if (searchItems == null) {
            return searchItems;
        }
        List<WebSearchItem> checkedItems = new ArrayList<>();
        for (WebSearchItem webSearchItem : searchItems) {
            if (webSearchItem.isChecked()) {
                checkedItems.add(webSearchItem);
            }
        }
        return checkedItems;
    }

    public static List<WebSearchItem> getWebSearchEngine(Context context, int searchType) {
        if (searchType == SearchPresenter.SEARCH_TYPE_LOCAL) {
            return null;
        }
        List<WebSearchItem> searchEngine = null;
        String saveKey = searchType == SearchPresenter.SEARCH_TYPE_WEB ? WEB_SEARCH_ENGINE_INFO : GLOBAL_WEB_SEARCH_ENGINE_INFO;
        String engineInfo = SharePrefUtil.getString(context, saveKey, "");
        if (!TextUtils.isEmpty(engineInfo)) {
            searchEngine = new ArrayList<>();
            try {
                JSONArray json = new JSONArray(engineInfo);
                for (int i = 0; i < json.length(); i++) {
                    WebSearchItem item = new WebSearchItem();
                    item.fromJSON(new JSONObject(json.getString(i)));
                    searchEngine.add(item);
                }
            } catch (JSONException e) {
            }
        } else {
            searchEngine = getDefaultWebSearchEngine(context, searchType == SearchPresenter.SEARCH_TYPE_WEB);
            saveSearchData(context, searchEngine, saveKey);
        }
        return searchEngine;
    }

    public static boolean mergeLaunchWebIntent(Context context, List<Rect> rectList, List<WebSearchItem> items, List<String> keywordsList,
                                               SequenceActivityLauncher sequenceActivityLauncher) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setPackage(SaraConstant.BROWSER_PACKAGE_NAME_SMARTISAN);
        intent.setData(Uri.parse("https://www.baidu.com/"));
        intent.putExtra("smartisanos.intent.extra.SHOW_MULTIWINDOW", true);
        IntentSmt.putSmtExtra(intent, "window-type", "window_with_maximize_view");
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.addFlags(Intent.FLAG_ACTIVITY_LAUNCH_ADJACENT);
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
        intent.putExtra(SaraConstant.HOLD_CURRENT_ACTIVITY, true);
        for (int i = 0; i < rectList.size(); i++) {
            ActivityOptions options = ActivityOptions.makeBasic();
            options.setLaunchBounds(rectList.get(i));
            Bundle bundle = options.toBundle();
            Uri url = Uri.parse(SettingManager.getOriUrl(items.get(i).mType, keywordsList.get(i)));
            Intent clipIntent = new Intent(Intent.ACTION_VIEW);
            clipIntent.setData(url);
            clipIntent.putExtra("smartisanos.intent.extra.SHOW_WINDOW_WITH_OPTIONS", bundle);
            clipIntent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
            IntentSmt.putSmtExtra(clipIntent, "window-title", items.get(i).getEnginName());

            if (intent.getClipData() == null) {
                intent.setClipData(ClipData.newIntent("test", clipIntent));
            } else {
                ClipData.Item item = new ClipData.Item(clipIntent);
                intent.getClipData().addItem(item);
            }
        }
        if (sequenceActivityLauncher != null) {
            sequenceActivityLauncher.sequenceStart(context, intent, rectList);
            return true;
        } else {
            try {
                context.startActivity(intent);
                return true;
            } catch (Exception e) {
                LogTag.d(TAG, "startActivity failed ", e);
            }
        }
        return false;
    }

    public static boolean launchWebSearchItem(Context context, Rect displayRect, WebSearchItem item, String keywords,
                                              SequenceActivityLauncher sequenceActivityLauncher) {
//        Bundle bundle = new Bundle();
//        bundle.putInt(ActivityOptions.KEY_ANIM_TYPE, ActivityOptions.ANIM_SCALE_UP);
//        bundle.putInt(ActivityOptions.KEY_ANIM_START_X, 0);
//        bundle.putInt(ActivityOptions.KEY_ANIM_START_Y, 200);
//        bundle.putInt(ActivityOptions.KEY_ANIM_WIDTH, 0);
//        bundle.putInt(ActivityOptions.KEY_ANIM_HEIGHT, 0);
//        final ActivityOptions options = new ActivityOptions(bundle);
        final ActivityOptions options = ActivityOptions.makeBasic();
        options.setLaunchBounds(displayRect);
//        if (item.mType == SEARCH_VALUE_TYPE_JD) {
//            Intent i = getLocalSearchIntent(context, keywords, "com.jingdong.app.mall",
//                    "com.jd.lib.search.view.Activity.ProductListActivity", false);
//            if (i != null) {
//                if (sequenceActivityLauncher != null) {
//                    sequenceActivityLauncher.sequenceStart(context, i, options);
//                    return true;
//                } else {
//                    try {
//                        context.startActivity(i, options.toBundle());
//                        return true;
//                    } catch (Exception e) {
//                        LogTag.e(TAG, "startActivity failed " + i, e);
//                    }
//                }
//            }
//        }
        CustomTabsIntent.Builder builder = new CustomTabsIntent.Builder();
        builder.setToolbarColor(Color.WHITE);
        CustomTabsIntent customTabsIntent = builder.build();
        Intent browserIntent = customTabsIntent.intent;
        browserIntent.setPackage(SaraConstant.BROWSER_PACKAGE_NAME_SMARTISAN);
        IntentSmt.putSmtExtra(browserIntent, "window-type", "window_with_maximize_view");
        IntentSmt.putSmtExtra(browserIntent, "window-title", item.getEnginName());
        browserIntent.putExtra("smartisanos.intent.extra.SHOW_WINDOW_WITH_OPTIONS", options.toBundle());
        browserIntent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
        browserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        browserIntent.addFlags(Intent.FLAG_ACTIVITY_LAUNCH_ADJACENT);
        browserIntent.putExtra(SaraConstant.HOLD_CURRENT_ACTIVITY, true);
        Uri url = Uri.parse(getOriUrl(item.mType, keywords));
        if (sequenceActivityLauncher != null) {
            sequenceActivityLauncher.sequenceStart(context, customTabsIntent, url, new Rect(displayRect));
            return true;
        } else {
            try {
                customTabsIntent.launchUrl(context, url);
                return true;
            } catch (Exception e) {
                LogTag.d(TAG, "startActivity failed " + item.mEngineName, e);
            }
        }
        return false;
    }

    public static boolean launchLocalSearchItem(Context context, Rect displayRect, LocalSearchItem item, String keywords,
                                                SequenceActivityLauncher sequenceActivityLauncher) {
        if (item.mPackageName.equals(context.getPackageName())) {
            if (sequenceActivityLauncher == null) {
                if (context instanceof GlobalSearchActivity) {
                    ((GlobalSearchActivity) context).showLocalSearch(displayRect);
                }
            } else {
                Intent intent = new Intent();
                intent.setPackage(item.mPackageName);
                final ActivityOptions options = ActivityOptions.makeBasic();
                options.setLaunchBounds(displayRect);
                sequenceActivityLauncher.sequenceStart(context, intent, options);
            }
            return true;
        }
        Intent intent;
        if (!TextUtils.isEmpty(item.getActionName())) {
            intent = getLocalSearchIntent(context, keywords, item.getPackageName(), item.getActionName());
        } else {
            if (TextUtils.isEmpty(item.getPackageName()) || TextUtils.isEmpty(item.getClassName())) {
                return false;
            }
            intent = getLocalSearchIntent(context, keywords, item.getPackageName(), item.getClassName(), true);
        }
        intent.putExtra(SaraConstant.HOLD_CURRENT_ACTIVITY, true);
        final ActivityOptions options = ActivityOptions.makeBasic();
        options.setLaunchBounds(displayRect);
        if (intent == null) {
            return false;
        }
        if (sequenceActivityLauncher != null) {
            sequenceActivityLauncher.sequenceStart(context, intent, options);
            return true;
        } else {
            try {
                context.startActivity(intent, options.toBundle());
                return true;
            } catch (Exception e) {
                LogTag.e(TAG, "startActivity failed " + intent, e);
            }
        }
        return false;
    }

    private static Intent getLocalSearchIntent(Context context, String keywords, String packageName, String action) {
        Intent targetIntent = new Intent(action);
        targetIntent.setPackage(packageName);
        targetIntent.putExtra("search_key", keywords);
        targetIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
        targetIntent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
        IntentSmt.putSmtExtra(targetIntent, IntentSmt.EXTRA_REVONE_SMT_LAUNCH_TYPE, "phone");
        IntentSmt.putSmtExtra(targetIntent, "window-type", "window_with_maximize_view");
        try {
            List<ResolveInfo> list = context.getPackageManager().queryIntentActivities(targetIntent, 0);
            if (list == null || list.isEmpty()) {
                if (SMARTISAN_COMMON_SEARCH_ACTION.equals(targetIntent.getAction())) {
                    return null;
                }
                targetIntent.setAction(SMARTISAN_COMMON_SEARCH_ACTION);
                list = context.getPackageManager().queryIntentActivities(targetIntent, 0);
                if (list == null || list.isEmpty()) {
                    return null;
                }
            }
        } catch (Exception e) {
            //ignore
        }
        return targetIntent;
    }

    private static Intent getLocalSearchIntent(Context context, String keywords, String packageName, String className,
                                               boolean isInternalApp) {
        Intent targetIntent;
        if (isInternalApp) {
            targetIntent = new Intent();
            targetIntent.putExtra("search_key", keywords);
        } else {
            targetIntent = new Intent(Intent.ACTION_SEND);
            targetIntent.setType("text/plain");
            targetIntent.putExtra(Intent.EXTRA_TEXT, keywords);
        }
        targetIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
        targetIntent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
        IntentSmt.putSmtExtra(targetIntent, IntentSmt.EXTRA_REVONE_SMT_LAUNCH_TYPE, "phone");
        IntentSmt.putSmtExtra(targetIntent, "window-type", "window_with_maximize_view");
        targetIntent.setComponent(new ComponentName(packageName, className));
        try {
            List<ResolveInfo> list = context.getPackageManager().queryIntentActivities(targetIntent, 0);
            if (list == null || list.isEmpty()) {
                if (!isInternalApp) {
                    return null;
                }
                targetIntent.setComponent(null);
                targetIntent.setPackage(packageName);
                targetIntent.setAction(SMARTISAN_COMMON_SEARCH_ACTION);
                list = context.getPackageManager().queryIntentActivities(targetIntent, 0);
                if (list == null || list.isEmpty()) {
                    return null;
                }
                LogTag.w(TAG, "getLocalSearchIntent:" + targetIntent + ", empty!!!");
            }
        } catch (Exception e) {
            //ignore
        }
        return targetIntent;
    }

    public static String getOriUrl(int searchType, String text) {
        String encode;
        try {
            encode = URLEncoder.encode(text, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            encode = text;
        }
        String url;
        switch (searchType) {
            case SettingsSmt.TEXT_BOOM_SEARCH_VALUE.TYPE_GOOGLE:
                url = "https://www.google.com/search?q=" + encode;
                break;
            case SettingsSmt.TEXT_BOOM_SEARCH_VALUE.TYPE_BING:
                url = "https://www.bing.com/search?q=" + encode;
                break;
            case SettingsSmt.TEXT_BOOM_SEARCH_VALUE.TYPE_SOGOU:
                url = "http://wap.sogou.com/web/sl?keyword=" + encode + "&pid=sogou-mobp-ef48e3ef07e35900";
                break;
            case SettingsSmt.TEXT_BOOM_SEARCH_VALUE.TYPE_BAIKE:
                url = "http://wapbaike.baidu.com/search/word?word=" + encode;
                break;
            case SettingsSmt.TEXT_BOOM_SEARCH_VALUE.TYPE_YOUDAO:
                url = "http://smartisandict.youdao.com/dict?q=" + encode;
                break;
            case SettingsSmt.TEXT_BOOM_SEARCH_VALUE.TYPE_KINGSOFT:
                url = "http://www.iciba.com/" + encode;
                break;
            case SettingsSmt.TEXT_BOOM_SEARCH_VALUE.TYPE_BINGDICT:
                url = "http://cn.bing.com/dict/?q=" + encode;
                break;
            case SettingsSmt.TEXT_BOOM_SEARCH_VALUE.TYPE_GOOGLE_TRANSLATE:
                url =  "https://translate.google.cn/m/translate#auto/zh-CN/" + encode;
                break;
            case SettingsSmt.TEXT_BOOM_SEARCH_VALUE.TYPE_WIKI_EN:
                url = "https://en.m.wikipedia.org/wiki/" + encode;
                break;
            case SettingsSmt.TEXT_BOOM_SEARCH_VALUE.TYPE_SEARCH_WEIXIN:
                return "http://weixin.sogou.com/weixinwap?type=2&query=" + encode;
            case SettingsSmt.TEXT_BOOM_SEARCH_VALUE.TYPE_SEARCH_WEIBO:
                return "http://s.weibo.com/weibo/" + encode;
            case SettingsSmt.TEXT_BOOM_SEARCH_VALUE.TYPE_SEARCH_ZHIHU:
                return "http://zhihu.sogou.com/zhihuwap?query=" + encode;
            case SettingsSmt.TEXT_BOOM_SEARCH_VALUE.TYPE_WIKI:
                return "http://www.baike.com/gwiki/" + encode;
            case SettingsSmt.TEXT_BOOM_SEARCH_VALUE.TYPE_WIKI_CN:
                return "https://zh.m.wikipedia.org/wiki/" + encode;
            case SettingsSmt.TEXT_BOOM_SEARCH_VALUE.TYPE_360SO:
                return "https://m.so.com/s?q=" + encode + "&src=home&srcg=cs_chuizi_1&nav=2";
            case SEARCH_VALUE_TYPE_JD:
                return "https://so.m.jd.com/ware/search.action?enc=utf-8&keyword=" + encode;
            case SEARCH_VALUE_TYPE_TAOBAO:
                return "https://s.m.taobao.com/h5?q=" + encode;
            case SEARCH_VALUE_TYPE_TOUTIAO:
                return "https://m.toutiao.com/search/?keyword=" + encode;
            case SEARCH_VALUE_TYPE_HAOQIXIN:
                return "http://m.qdaily.com/mobile/searches?key=" + encode;
            case SEARCH_VALUE_TYPE_DOUBAN:
                return "https://m.douban.com/search?query=" + encode;
            case SEARCH_VALUE_TYPE_YOUKU:
                return "http://www.soku.com/m/y/video?q=" + encode;
            case SEARCH_VALUE_TYPE_QQVIDEO:
                return "https://m.v.qq.com/search.html?keyWord=" + encode;
            case SEARCH_VALUE_TYPE_YOUTUBE:
                return "https://m.youtube.com/results?q=" + encode;
            case SEARCH_VALUE_TYPE_AMAZON:
                return "https://www.amazon.com/gp/aw/s/ref=is_s?k=" + encode;
            case SEARCH_VALUE_TYPE_YAHOO:
                return "https://search.yahoo.com/search?fr=sfp&q=" + encode;
            case SEARCH_VALUE_TYPE_NG:
                return "https://www.nationalgeographic.com/search?q=" + encode;
            case SEARCH_VALUE_TYPE_DDG:
                return "https://www.duckduckgo.com/?q=" + encode;
            case SEARCH_VALUE_TYPE_TWITTER:
                return "https://mobile.twitter.com/search?q=" + encode;
            case SEARCH_VALUE_TYPE_FB:
                return "https://m.facebook.com/search?q=" + encode;
            case SEARCH_VALUE_TYPE_EBAY:
                return "https://m.ebay.com/sch/i.html?_nkw=" + encode;
            case SEARCH_VALUE_TYPE_QUORA:
                return "https://www.quora.com/search?q=" + encode;
            case SEARCH_VALUE_TYPE_PINTEREST:
                return "https://www.pinterest.com/search?q=" + encode;
            case SEARCH_VALUE_TYPE_YELP:
                return "https://m.yelp.com/search?find_desc=" + encode;
            case SEARCH_VALUE_TYPE_BING:
                return "https://cn.bing.com/search?q=" + encode + "&qs=n&FORM=BESBTB&ensearch=1";
            case SEARCH_VALUE_TYPE_DIANPING:
                return "https://m.dianping.com/shoplist/8/search?from=m_search&keyword=" + encode;
            default:
                url = "http://m.baidu.com/s?word=" + encode + "&from=1013377a";
                break;
        }
        return url;
    }

    private static List<WebSearchItem> getDefaultWebSearchEngine(Context context, boolean checked) {
        List<WebSearchItem> searchEngine = new ArrayList<>();
        String[] titles = context.getResources().getStringArray(R.array.web_search_title);
        int[] values = context.getResources().getIntArray(R.array.web_search_values);
        int[] default_checked = context.getResources().getIntArray(R.array.web_search_default_checked);
        int count = titles.length;
        for (int i = 0; i < count; i++) {
            WebSearchItem item = new WebSearchItem();
            item.mType = values[i];
            item.mEngineName = titles[i];
            item.mChecked = (default_checked[i] == 1 && checked);
            searchEngine.add(item);
        }
        return searchEngine;
    }

    private static void saveSearchData(Context context, List<? extends SearchItem> list, String key) {
        if (list != null) {
            JSONArray jsonArray = new JSONArray();
            for (SearchItem item : list) {
                jsonArray.put(item.toJSON());
            }
            SharePrefUtil.savePref(context, key, jsonArray.toString());
        }
    }

    private static String getAppLabel(Context context, String packageName) {
        PackageManager pm = context.getPackageManager();
        try {
            ApplicationInfo appInfo = pm.getApplicationInfo(packageName, PackageManager.GET_META_DATA);
            CharSequence appLabel = pm.getApplicationLabel(appInfo);
            if (appLabel != null) {
                return appLabel.toString();
            }
        } catch (PackageManager.NameNotFoundException e) {
        }
        return null;
    }

    private void updateCheckState() {
        int webCount = 0;
        int localCount = 0;
        boolean isOnlyForLocal = true;
        boolean isOnlyForWeb = true;
        if (mWebListView != null && mWebListView.getVisibility() == View.VISIBLE && mWebAdapter != null) {
            webCount = mWebAdapter.getSelectedDataSize();
            isOnlyForLocal = false;
        }
        if (mLocalListView != null && mLocalListView.getVisibility() == View.VISIBLE && mLocalAdapter != null) {
            localCount = mLocalAdapter.getSelectedDataSize();
            isOnlyForWeb = false;
        }
        mLocalAdapter.setItemClickable(SearchParams.isAvailableForMoreLocalSearchItems(localCount, webCount, isOnlyForLocal));
        mWebAdapter.setItemClickable(SearchParams.isAvailableForMoreWebSearchItems(localCount, webCount, isOnlyForWeb));
    }

    @Override
    public void onItemCheckedChange() {
        updateCheckState();
    }

    private PopupWindow.OnDismissListener mOnDismissListener = new PopupWindow.OnDismissListener() {
        @Override
        public void onDismiss() {
            boolean dataChanged = false;
            if (mLocalAdapter.isDataChanged()) {
                dataChanged = true;
                mLocalAdapter.resetState();
                saveSearchData(mContext, mLocalAdapter.getAllData(), mSearchType == SearchPresenter.SEARCH_TYPE_LOCAL ? LOCAL_SEARCH_APP_INFO : GLOBAL_LOCAL_SEARCH_APP_INFO);
            }
            if (mWebAdapter.isDataChanged()) {
                dataChanged = true;
                mWebAdapter.resetState();
                saveSearchData(mContext, mWebAdapter.getAllData(), mSearchType == SearchPresenter.SEARCH_TYPE_WEB ? WEB_SEARCH_ENGINE_INFO : GLOBAL_WEB_SEARCH_ENGINE_INFO);
            }
            if (dataChanged && mSettingChangeListener != null) {
                int count = mWebAdapter.getSelectedDataSize() + mLocalAdapter.getSelectedDataSize();
                mSettingChangeListener.onSettingChange(count);
            }
        }
    };

    public interface ISettingChangeListener {
        void onSettingChange(int select);
    }
}
