package com.smartisanos.ideapills.common.util;

import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageInfo;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.UserHandle;
import android.provider.Settings;
import android.text.TextUtils;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.drawable.Drawable;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import smartisanos.api.SettingsSmt;

import com.smartisanos.ideapills.common.model.ShareItem;
import com.smartisanos.ideapills.common.util.PackageManagerCompat;
import com.smartisanos.ideapills.common.R;
import smartisanos.util.LogTag;

public class PackageUtils {
    public static final String TAG = "VoiceAss.BubbleActivity";
    public static final String COM_AUTONAVI_MINIMAP = "com.autonavi.minimap";
    public static final String COM_AUTONAVI_MAP_ACTIVITY_NEW_MAP_ACTIVITY = "com.autonavi.map.activity.SplashActivity";
    public static final String COM_SINA_WEIBO = "com.sina.weibo";
    public static final String COM_SINA_WEIBO_COMPOSERINDE_COMPOSER_DISPATCH_ACTIVITY = "com.sina.weibo.composerinde.ComposerDispatchActivity";
    public static final String WECHAT_PACKAGE = "com.tencent.mm";
    public static final String WECHAT_SHARE_ACTIVITY = "com.tencent.mm.ui.tools.ShareImgUI";
    private static final String BROWSER_PACKAGE_NAME = "com.android.browser";
    private static final String BROWSER_ACTIVITY_NAME = "com.android.browser.BrowserActivity";
    private static final String TAOBAO_PACKAGE_NAME = "com.taobao.taobao";
    public static String JSON_SHARE_DATA = "json_share_data";
    private static String JSON_packageName = "packageName";
    private static String JSON_activityName = "activityName";
    private static String JSON_isSelected = "isSelected";
    private static String JSON_displayname = "displayname";
    public static final String BULLET_AUTHORITY = "content://com.bullet.messenger";

    private static final String IS_FIRST_GET_SHARE_KEY = "if_first_get_sharekey";

    public static boolean isBulletAppLogin(Context context) {
        try {
            Uri uri = Uri.parse(BULLET_AUTHORITY);
            Bundle bundle = context.getContentResolver().call(uri, "FLASHIM_LOGIN_ACCOUNT", null, null);
            return bundle != null && !TextUtils.isEmpty(bundle.getString("KEY_CONTENT_ACCOUNT"));
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public static boolean isAvilibleApp(Context context, String packageName) {
        if (TextUtils.isEmpty(packageName))
            return false;
        try {
            final PackageManager packageManager = context.getPackageManager();
            ApplicationInfo info = packageManager.getApplicationInfo(packageName, 0);
            return info != null;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    public static boolean isAvilibleApp(String packageName, List<PackageInfo> pinfo) {
        for (int i = 0; i < pinfo.size(); i++) {
            if (((PackageInfo) pinfo.get(i)).packageName
                    .equalsIgnoreCase(packageName))
                return true;
        }
        return false;
    }

    public static List<ShareItem> getAvilibleApp(Context context, List<ShareItem> items) {
        PackageManager packageManager = context.getPackageManager();
        List<PackageInfo> pinfo = packageManager.getInstalledPackages(0);
        return getAvilibleApp(context, items, pinfo);
    }

    public static List<ShareItem> getAvilibleApp(Context context, List<ShareItem> items, List<PackageInfo> pinfo) {
        List<ShareItem> shareItems = new ArrayList<ShareItem>();
        for (int i = 0; i < items.size(); i++) {
            ShareItem item = items.get(i);
            for (PackageInfo info : pinfo) {
                if (info.packageName.equalsIgnoreCase(item.getPackageName())) {
                    LogTag.d(TAG, "item = " + item.getDispalyName());
                    try {
                        ActivityInfo activityInfo = context.getPackageManager().getActivityInfo(item.getComponentName(), ActivityInfo.FLAG_STATE_NOT_NEEDED);
                        if (filterConditionForDrawer(activityInfo)) {
                            shareItems.add(item);
                        }
                    } catch (PackageManager.NameNotFoundException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        return shareItems;
    }

    public static boolean isWechatExist(List<ShareItem> shareItems) {
        for (ShareItem shareItem : shareItems) {
            if (shareItem.getComponentName().equals(new ComponentName(WECHAT_PACKAGE, WECHAT_SHARE_ACTIVITY))) {
                return true;
            }
        }
        return false;
    }

    public static void setOldValue(Context context) {
        SharedPreferences sp = context.getSharedPreferences(context.getPackageName(), Context.MODE_WORLD_WRITEABLE);
        if (!sp.getBoolean("ifFirstGetShareKey", true)) {
            ContentResolver cr = context.getContentResolver();
            Settings.Global.putInt(cr, IS_FIRST_GET_SHARE_KEY, 1);
            Editor editor = sp.edit();
            editor.putBoolean("ifFirstGetShareKey", true);
            editor.commit();
        }
    }

    public static List<ShareItem> getShareItemListInitDataFirstTime(Context context) {
        setOldValue(context);
        if (Settings.Global.getInt(context.getContentResolver(), IS_FIRST_GET_SHARE_KEY, 0) == 0) {
            ContentResolver cr = context.getContentResolver();
            Settings.Global.putInt(cr, IS_FIRST_GET_SHARE_KEY, 1);
            return setDefaultShareData(context);
        }
        return getShareItemAvailiableList(context);
    }

    public static List<ShareItem> getShareItemAvailiableList(Context context) {
        List<ShareItem> shareItems = new ArrayList<ShareItem>();
        String jsonArray = Settings.Global.getString(context.getContentResolver(), SettingsSmt.Global.SHARE_ITEM_INFOS);
        LogTag.d(TAG, "jsonArray = " + jsonArray);
        if (!TextUtils.isEmpty(jsonArray)) {
            try {
                JSONArray json = new JSONArray(jsonArray);
                for (int i = 0; i < jsonArray.length(); i++) {
                    ShareItem shareItem = parseJSONString(context, json.getString(i));
                    if (shareItem != null) {
                        shareItems.add(shareItem);
                    }
                }
            } catch (JSONException e) {
            }
        }
        PackageManager packageManager = context.getPackageManager();
        List<PackageInfo> pinfo = packageManager.getInstalledPackages(0);
        shareItems = getAvilibleApp(context, shareItems, pinfo);
        if (shareItems.size() < CommonConstant.APP_DRAWER_MAX_COUNT) {
            final ContentResolver cr = context.getContentResolver();
            if (isAvilibleApp(WECHAT_PACKAGE, pinfo)) {
                if (Settings.Global.getInt(cr, SettingsSmt.Global.CANCEL_WECHA_FROM_DRAWER_BY_SELF, 0) == 0
                        && !isWechatExist(shareItems)) {
                    shareItems.add(getWechatShareItem(context));
                }
            } else {
                Settings.Global.putInt(cr, SettingsSmt.Global.CANCEL_WECHA_FROM_DRAWER_BY_SELF, 0);
            }
        }
        return shareItems;
    }

    private static ShareItem getWechatShareItem(Context context) {
        ShareItem shareItem = new ShareItem();
        try {
            PackageManager pm = context.getPackageManager();
            shareItem.setSelected(true);
            shareItem.setComponentName(new ComponentName(WECHAT_PACKAGE, WECHAT_SHARE_ACTIVITY));
            shareItem.setPackageName(shareItem.getComponentName().getPackageName());
            shareItem.setActivityName(shareItem.getComponentName().getClassName());
            shareItem.setDispalyName(pm.getActivityInfo(shareItem.getComponentName(), ActivityInfo.FLAG_STATE_NOT_NEEDED).loadLabel(pm).toString());
        } catch (PackageManager.NameNotFoundException e) {
        }
        return shareItem;
    }

    public static JSONObject toJSON(ShareItem shareItem) {
        JSONObject json = new JSONObject();
        try {
            json.put(JSON_displayname, shareItem.getDispalyName());
            json.put(JSON_packageName, shareItem.getPackageName());
            json.put(JSON_activityName, shareItem.getActivityName());
            json.put(JSON_isSelected, shareItem.isSelected());
        } catch (JSONException e) {
        }
        return json;
    }

    public static void saveShareData(Context context, List<ShareItem> shareItems) {
        ContentResolver contentResolver = context.getContentResolver();
        if (shareItems == null || shareItems.size() <= 0) {
            Settings.Global.putString(contentResolver, SettingsSmt.Global.SHARE_ITEM_INFOS, "");
        } else {
            JSONArray jsonArray = new JSONArray();
            for (ShareItem shareItem : shareItems) {
                jsonArray.put(toJSON(shareItem));
            }
            if (jsonArray.length() <= 0) {
                Settings.Global.putString(contentResolver, SettingsSmt.Global.SHARE_ITEM_INFOS, "");
            } else {
                Settings.Global.putString(contentResolver, SettingsSmt.Global.SHARE_ITEM_INFOS, jsonArray.toString());
            }
        }
    }

    public static ShareItem parseJSONString(Context context, String jsonString) {
        if (jsonString.isEmpty()) {
            return null;
        }
        ShareItem shareItem = new ShareItem();
        try {
            JSONObject json = new JSONObject(jsonString);
            shareItem.setDispalyName(json.getString(JSON_displayname));
            shareItem.setPackageName(json.getString(JSON_packageName));
            shareItem.setActivityName(json.getString(JSON_activityName));
            shareItem.setSelected(json.getBoolean(JSON_isSelected));
            shareItem.setComponentName(new ComponentName(shareItem.getPackageName(), shareItem.getActivityName()));
        } catch (JSONException e) {
        }
        return shareItem;
    }

    public static ShareItem getShareItem(Context context, PackageManager pm, ComponentName component, List<PackageInfo> pinfo) {
        ShareItem item = null;
        if (pm != null && component != null && pinfo != null) {
            try {
                if (isAvilibleApp(component.getPackageName(), pinfo)) {
                    item = new ShareItem();
                    item.setSelected(true);
                    item.setComponentName(component);
                    item.setPackageName(component.getPackageName());
                    item.setActivityName(component.getClassName());
                    item.setDispalyName(pm.getActivityInfo(component, ActivityInfo.FLAG_STATE_NOT_NEEDED).loadLabel(pm).toString());
                }
            } catch (PackageManager.NameNotFoundException e) {
                LogTag.e(TAG, "NameNotFoundException e = " + e.toString());
            }
        }
        return item;
    }

    public static List<ShareItem> setDefaultShareData(Context context) {
        LogTag.d(TAG, "setDefaultShareData");
        PackageManager pm = context.getPackageManager();
        List<ShareItem> saveShareList = new ArrayList<ShareItem>();
        List<PackageInfo> pinfo = pm.getInstalledPackages(0);
        List<ComponentName> defaultAppDrawer = new ArrayList<ComponentName>();
        defaultAppDrawer.add(new ComponentName("com.android.calendar", "com.android.calendar.event.EditEventActivity"));
        defaultAppDrawer.add(new ComponentName("com.smartisanos.notes", "com.smartisanos.notes.CreateNotesActivity"));
        defaultAppDrawer.add(new ComponentName("com.android.mms", "com.android.mms.ui.ComposeMessageActivity"));
        defaultAppDrawer.add(new ComponentName(COM_SINA_WEIBO, COM_SINA_WEIBO_COMPOSERINDE_COMPOSER_DISPATCH_ACTIVITY));
        defaultAppDrawer.add(new ComponentName(COM_AUTONAVI_MINIMAP, COM_AUTONAVI_MAP_ACTIVITY_NEW_MAP_ACTIVITY));
        defaultAppDrawer.add(new ComponentName(WECHAT_PACKAGE, WECHAT_SHARE_ACTIVITY));
        for (ComponentName component : defaultAppDrawer) {
            ShareItem item = getShareItem(context, pm, component, pinfo);
            if (item != null) {
                saveShareList.add(item);
            }
        }
        saveShareData(context, saveShareList);
        return saveShareList;
    }

    public static List<ResolveInfo> getResolveInfoList(Context context) {
        PackageManager pm = context.getPackageManager();
        Intent targetIntent = new Intent(Intent.ACTION_SEND);
        targetIntent.setType("text/plain");
        List<ResolveInfo> resolveInfos = pm.queryIntentActivitiesAsUser(targetIntent, PackageManagerCompat.DEFAULT_QUERY_FLAG, UserHandle.USER_OWNER);
        return resolveInfos;
    }

    public static ResolveInfo getResolveInfo(ComponentName componentName, List<ResolveInfo> resolveInfos) {
        for (ResolveInfo resolveInfo : resolveInfos) {
            if (new ComponentName(resolveInfo.activityInfo.packageName, resolveInfo.activityInfo.name).equals(componentName)) {
                return resolveInfo;
            }
        }
        return null;
    }

    public static Drawable getAppIcon(Context context, ComponentName componentName) {
        Drawable icon = null;
        if (null == componentName) {
            icon = context.getDrawable(R.drawable.default_icon);
            return icon;
        }

        PackageManager pkm = context.getPackageManager();
        if (componentName.equals(new ComponentName(BROWSER_PACKAGE_NAME, BROWSER_ACTIVITY_NAME))) {
            List<ResolveInfo> resolveInfos = PackageUtils.getResolveInfoList(context);
            ResolveInfo resolveInfo = PackageUtils.getResolveInfo(componentName, resolveInfos);
            if (resolveInfo == null) {
                try {
                    icon = pkm.getActivityIcon(componentName);
                } catch (PackageManager.NameNotFoundException e) {
                }
            } else {
                icon = resolveInfo.loadIcon(pkm);
            }
        } else {
            try {
                icon = pkm.getActivityIcon(componentName);
                if (icon == null) {
                    ActivityInfo resolveInfo = pkm.getActivityInfo(componentName,
                            ActivityInfo.FLAG_STATE_NOT_NEEDED);
                    if (resolveInfo != null) {
                        icon = resolveInfo.loadIcon(pkm);
                    }
                }
            } catch (PackageManager.NameNotFoundException e) {
            }
        }

        if (icon == null) {
            icon = context.getDrawable(R.drawable.default_icon);
        }
        return icon;
    }

    public static boolean filterConditionForDrawer(ActivityInfo activityInfo) {
        return activityInfo.exported && !isTaoBao(activityInfo);
    }

    public static boolean isTaoBao(ActivityInfo activityInfo) {
        if (PackageUtils.TAOBAO_PACKAGE_NAME.equals(activityInfo.packageName)) {
            return true;
        }
        return false;
    }
}
