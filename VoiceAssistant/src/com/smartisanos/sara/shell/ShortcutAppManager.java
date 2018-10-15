package com.smartisanos.sara.shell;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.UserHandle;
import android.text.TextUtils;
import android.util.Log;
import android.graphics.drawable.Drawable;
import android.provider.Settings;
import android.content.ContentResolver;

import com.smartisanos.ideapills.common.util.PackageManagerCompat;
import com.smartisanos.ideapills.common.util.PackageUtils;
import com.smartisanos.sara.entity.ShortcutApp;
import com.smartisanos.sara.util.LogUtils;
import com.smartisanos.sara.util.SaraConstant;
import com.smartisanos.sara.R;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;
import static com.smartisanos.ideapills.common.util.PackageUtils.COM_AUTONAVI_MAP_ACTIVITY_NEW_MAP_ACTIVITY;
import static com.smartisanos.ideapills.common.util.PackageUtils.COM_AUTONAVI_MINIMAP;
import static com.smartisanos.ideapills.common.util.PackageUtils.COM_SINA_WEIBO;
import static com.smartisanos.ideapills.common.util.PackageUtils.COM_SINA_WEIBO_COMPOSERINDE_COMPOSER_DISPATCH_ACTIVITY;
import static com.smartisanos.ideapills.common.util.PackageUtils.WECHAT_PACKAGE;
import static com.smartisanos.ideapills.common.util.PackageUtils.WECHAT_SHARE_ACTIVITY;
import smartisanos.api.SettingsSmt;

/**
 * link PackageUtils ,DrawerSettingActivity,DrawerSettingAdapter
 */

public class ShortcutAppManager {

    public static final int MAX_SIZE = 12;
    private static ShortcutAppManager sInstance;
    private static final String TAG = "ShortcutAppManager";
    private static String JSON_packageName = "packageName";
    private static String JSON_activityName = "activityName";
    private static String JSON_isSelected = "isSelected";
    private static String JSON_displayname = "displayname";

    public static enum DATA_TYPE {
        NONE, SELECTED_DATA, CANDIDATE
    }

    private ShortcutAppManager(Context context) {
    }

    public static ShortcutAppManager init(Context context) {
        if (sInstance == null) {
            sInstance = new ShortcutAppManager(context);
        }
        return sInstance;
    }

    /**
     * Get all the installed applications can share;
     * @param isShareEnable
     * @return
     */
    public static  List<ShortcutApp> getTotalShareList(Context context, boolean isShareEnable) {
        List<ShortcutApp> allShareList = new ArrayList<ShortcutApp>();
        if (isShareEnable) {
            PackageManager packageManager = context.getPackageManager();
            List<PackageInfo> pinfo = packageManager.getInstalledPackages(0);
            Intent targetIntent = new Intent(Intent.ACTION_SEND);
            targetIntent.setType("text/plain");
            List<ResolveInfo> resolveInfos = packageManager.queryIntentActivitiesAsUser(targetIntent, PackageManagerCompat.DEFAULT_QUERY_FLAG, UserHandle.USER_OWNER);
            for (ResolveInfo resolveInfo : resolveInfos) {
                if (resolveInfo.activityInfo.packageName.equals(SaraConstant.CLASS_NAME_IDEAPILL)) {
                    continue;
                }

                List<ShortcutApp> specilData = handleSpecialData(context, resolveInfo, packageManager, pinfo);
                if (specilData.size() > 0) {
                    allShareList.addAll(specilData);
                } else {
                    ShortcutApp shortcutApp = new ShortcutApp();
                    shortcutApp.setComponentName(new ComponentName(resolveInfo.activityInfo.packageName, resolveInfo.activityInfo.name));
                    shortcutApp.setPackageName(resolveInfo.activityInfo.packageName);
                    shortcutApp.setActivityName(resolveInfo.activityInfo.name);
                    shortcutApp.setDispalyName(resolveInfo.loadLabel(packageManager).toString());
                    shortcutApp.setDrawable(resolveInfo.loadIcon(packageManager));
                    allShareList.add(shortcutApp);
                }
            }

            ShortcutApp recorderApp = getRecorderApp(context, packageManager, pinfo);
            if (recorderApp != null) {
                allShareList.add(recorderApp);
            }
        } else {
            allShareList.clear();
        }
        return allShareList;
    }


    private static List<ShortcutApp> handleSpecialData(Context context, ResolveInfo resolveInfo, PackageManager packageManager, List<PackageInfo> pinfo) {
        List<ShortcutApp> specialData = new ArrayList<ShortcutApp>();
        List<ComponentName> specialComponent = new ArrayList<ComponentName>();
        //add weibo
        if (resolveInfo.activityInfo.packageName.equals(COM_SINA_WEIBO)) {
            specialComponent.add(new ComponentName(COM_SINA_WEIBO, COM_SINA_WEIBO_COMPOSERINDE_COMPOSER_DISPATCH_ACTIVITY));
//            specialComponent.add(new ComponentName(COM_SINA_WEIBO, "com.sina.weibo.weiyou.share.WeiyouShareDispatcher"));
            for (ComponentName component : specialComponent) {
                ShortcutApp item = getShareItem(packageManager, component, pinfo, context);
//                if (item.getActivityName().equals("com.sina.weibo.weiyou.share.WeiyouShareDispatcher")) {
//                    item.setDispalyName(context.getResources().getString(R.string.weibo_private_messages_name));
//                }
                if (item != null) {
                    specialData.add(item);
                }
            }
            return specialData;
        }

        //add map
        if (resolveInfo.activityInfo.packageName.equals(COM_AUTONAVI_MINIMAP)) {
            specialComponent.add(new ComponentName(COM_AUTONAVI_MINIMAP, COM_AUTONAVI_MAP_ACTIVITY_NEW_MAP_ACTIVITY));
            for (ComponentName component : specialComponent) {
                ShortcutApp item = getShareItem(packageManager, component, pinfo, context);
                if (item != null) {
                    specialData.add(item);
                }
            }
            return specialData;
        }

        //add dianping
        if (resolveInfo.activityInfo.packageName.equals("com.dianping.v1")) {
            specialComponent.add(new ComponentName("com.dianping.v1", "com.dianping.share.activity.OuterReceiveActivity"));
            for (ComponentName component : specialComponent) {
                ShortcutApp item = getShareItem(packageManager, component, pinfo, context);
                if (item != null) {
                    specialData.add(item);
                }
            }
            return specialData;
        }
        return specialData;
    }

    private static ShortcutApp getRecorderApp(Context context, PackageManager packageManager, List<PackageInfo> pinfo) {
        return getShareItem(packageManager,
                new ComponentName("com.smartisanos.recorder", "com.smartisanos.recorder.activity.RecorderPlayActivity"), pinfo, context);
    }

    /**
     * candidate set of data;
     * @param context
     * @return
     */
    public static List<ShortcutApp> getCandidateList(Context context) {
        //1. get save CandidateList
        List<ShortcutApp> candidateShareList = new ArrayList<ShortcutApp>();
        PackageManager packageManager = context.getPackageManager();
        List<PackageInfo> pinfo = packageManager.getInstalledPackages(0);
        //1.get history apps
        String jsonArray = Settings.Global.getString(context.getContentResolver(), SettingsSmt.Global.TOTAL_SHORTCUT_APP_INFOS);
        if (!TextUtils.isEmpty(jsonArray)) {
            try {
                JSONArray json = new JSONArray(jsonArray);
                for (int i = 0; i < json.length(); i++) {
                    ShortcutApp shareItem = parseJSONString(json.getString(i));
                    if (shareItem != null && isAvilibleApp(shareItem.getPackageName(), pinfo)) {
                        shareItem.setDrawable(getAppIcon(context, shareItem.getComponentName()));
                        candidateShareList.add(shareItem);
                    }
                }
            } catch (JSONException e) {
            }
        }

        //2. get all apps
        List<ShortcutApp> totalShareList = getTotalShareList(context, true);

        //3. remove the candidateShareList
        totalShareList.removeAll(candidateShareList);

        // 4. remove the selected list
        totalShareList.removeAll(getLastSaveShareData(context));

        //5.get the ordered candidateShareList
        candidateShareList.addAll(totalShareList);

        saveShareData(context, candidateShareList, DATA_TYPE.CANDIDATE.name());
        return candidateShareList;
    }


    /**
     * The application of capsule for sharing;
     * @param context
     * @return
     */
    public static  List<ShortcutApp> getLastSaveShareData(Context context) {
        List<ShortcutApp> shareData;
        if (Settings.Global.getInt(context.getContentResolver(), SettingsSmt.Global.IS_FIRST_GET_SHORTCUT_KEY, 0) == 0) {
            ContentResolver cr = context.getContentResolver();
            Settings.Global.putInt(cr, SettingsSmt.Global.IS_FIRST_GET_SHORTCUT_KEY, 1);
            shareData = getDefaultShareData(context);
        } else {
            shareData = getShareItemAvailiableList(context);
        }
        return  shareData;
    }

    public static List<ShortcutApp> getShareItemAvailiableList(Context context) {
        List<ShortcutApp> shareItems = new ArrayList<ShortcutApp>();
        String jsonArray = Settings.Global.getString(context.getContentResolver(), SettingsSmt.Global.SHORTCUT_APP_INFOS);
        if (!TextUtils.isEmpty(jsonArray)) {
            try {
                JSONArray json = new JSONArray(jsonArray);
                for (int i = 0; i < json.length(); i++) {
                    ShortcutApp shareItem = parseJSONString(json.getString(i));
                    if (shareItem != null) {
                        shareItem.setDrawable(getAppIcon(context, shareItem.getComponentName()));
                        shareItems.add(shareItem);
                    }
                }
            } catch (JSONException e) {
            }
        }

        PackageManager packageManager = context.getPackageManager();
        List<PackageInfo> pinfo = packageManager.getInstalledPackages(0);
        shareItems = getAvilibleApp(context, shareItems, pinfo);

        int totalSize = shareItems.size();
        if (totalSize > 0 && totalSize < MAX_SIZE) {
            shareItems.add(new ShortcutApp());
        }

        return shareItems;
    }

    /**
     * @param context
     * @param items
     * @param pinfo
     * @return
     */
    public static List<ShortcutApp> getAvilibleApp(Context context, List<ShortcutApp> items, List<PackageInfo> pinfo) {
        List<ShortcutApp> shareItems = new ArrayList<ShortcutApp>();
        for (int i = 0; i < items.size(); i++) {
            ShortcutApp item = items.get(i);
            for (PackageInfo info : pinfo) {
                if (info.packageName.equalsIgnoreCase(item.getPackageName())) {
                    shareItems.add(item);
                }
            }
        }
        return shareItems;
    }

    /**
     * App List
     * @param context
     * @param shareItems
     */
    public static void saveShareData(Context context, List<ShortcutApp> shareItems, String type) {
        ContentResolver contentResolver = context.getContentResolver();
        if (shareItems == null || shareItems.size() <= 0) {
            if (type.equals(DATA_TYPE.CANDIDATE.name())) {
                Settings.Global.putString(contentResolver, SettingsSmt.Global.TOTAL_SHORTCUT_APP_INFOS, "");
            } else if (type.equals(DATA_TYPE.SELECTED_DATA.name())) {
                Settings.Global.putString(contentResolver, SettingsSmt.Global.SHORTCUT_APP_INFOS, "");
            }
        } else {
            JSONArray jsonArray = new JSONArray();
            for (ShortcutApp shareItem : shareItems) {
                jsonArray.put(toJSON(shareItem));
            }

            String dataString = "";
            if (jsonArray.length() > 0) {
                dataString = jsonArray.toString();
            }
            if (type.equals(DATA_TYPE.CANDIDATE.name())) {
                Settings.Global.putString(contentResolver, SettingsSmt.Global.TOTAL_SHORTCUT_APP_INFOS, dataString);
            } else if (type.equals(DATA_TYPE.SELECTED_DATA.name())) {
                Settings.Global.putString(contentResolver, SettingsSmt.Global.SHORTCUT_APP_INFOS, dataString);
            }
        }
    }

    public static JSONObject toJSON(ShortcutApp shareItem) {
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

    public static ShortcutApp parseJSONString(String jsonString) {
        if (jsonString.isEmpty()) {
            return null;
        }
        ShortcutApp shareItem = new ShortcutApp();
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


    public static List<ShortcutApp> getDefaultShareData(Context context) {
        PackageManager pm = context.getPackageManager();
        List<ShortcutApp> saveShareList = new ArrayList<ShortcutApp>();
        List<PackageInfo> pinfo = pm.getInstalledPackages(0);

        //1. get app from sara drawer
        String jsonArray = Settings.Global.getString(context.getContentResolver(), SettingsSmt.Global.SHARE_ITEM_INFOS);
        LogUtils.d(TAG, "jsonArray = " + jsonArray);
        if (!TextUtils.isEmpty(jsonArray)) {
            try {
                JSONArray json = new JSONArray(jsonArray);
                for (int i = 0; i < jsonArray.length(); i++) {
                    ShortcutApp shareItem = parseJSONString(json.getString(i));
                    if (shareItem != null && isAvilibleApp(shareItem.getPackageName(), pinfo)) {
                        shareItem.setDrawable(getAppIcon(context, shareItem.getComponentName()));
                        saveShareList.add(shareItem);
                    }
                }
            } catch (JSONException e) {
            }
        }

        //2. get the apps that defaultPreloadingApps - drawer app
        List<ShortcutApp> defaultPreloadingApps = defaultPreloadingApps(context);
        List<ShortcutApp> restApp = new ArrayList<ShortcutApp>();
        for (ShortcutApp app : defaultPreloadingApps) {
            if (!saveShareList.contains(app)) {
                restApp.add(app);
            }
        }

        //3. remove the apps from the rest that more than 12
        int restSize = restApp.size();
        int drawerAppSize = saveShareList.size();
        if (restSize > MAX_SIZE - drawerAppSize) {
            int removeSize = restSize + drawerAppSize - MAX_SIZE;
            for (int j = 1; j <= removeSize; j++) {
                restApp.remove(restSize - j);
            }
        }

        saveShareList.addAll(restApp);

        int totalSize = saveShareList.size();
        if (totalSize > 0 && totalSize < MAX_SIZE) {
            saveShareList.add(new ShortcutApp());
        }

        saveShareData(context, saveShareList, DATA_TYPE.SELECTED_DATA.name());
        return saveShareList;
    }

    public static ShortcutApp getShareItem(PackageManager pm, ComponentName component, List<PackageInfo> pinfo, Context context) {
        ShortcutApp item = null;
        if (pm != null && component != null && pinfo != null) {
            try {
                if (isAvilibleApp(component.getPackageName(), pinfo)) {
                    item = new ShortcutApp();
                    item.setComponentName(component);
                    item.setPackageName(component.getPackageName());
                    item.setActivityName(component.getClassName());
                    item.setDispalyName(pm.getActivityInfo(component, ActivityInfo.FLAG_STATE_NOT_NEEDED).loadLabel(pm).toString());
                    Drawable icon = item.getDrawable();
                    if (icon == null) {
                        icon = getAppIcon(context, item.getComponentName());
                    }
                    item.setDrawable(icon);
                }
            } catch (PackageManager.NameNotFoundException e) {
                Log.e(TAG, "NameNotFoundException e = " + e.toString());
            }
        }
        return item;
    }

    public static boolean isAvilibleApp(String packageName, List<PackageInfo> pinfo) {
        for (int i = 0; i < pinfo.size(); i++) {
            if (((PackageInfo) pinfo.get(i)).packageName
                    .equalsIgnoreCase(packageName))
                return true;
        }
        return false;
    }

    public static Drawable getAppIcon(Context context, ComponentName componentName) {
        Drawable icon = null;
        if (null == componentName) {
            icon = context.getDrawable(R.drawable.share_item_jd);
            return icon;
        }

        PackageManager pkm = context.getPackageManager();
        if (componentName.equals(new ComponentName(SaraConstant.BROWSER_PACKAGE_NAME, SaraConstant.BROWSER_ACTIVITY_NAME))) {
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
            } catch (PackageManager.NameNotFoundException e) {
            }
        }

        if (icon == null) {
            icon = context.getDrawable(R.drawable.share_item_jd);
        }
        return icon;
    }

    private static List<ShortcutApp> defaultPreloadingApps(Context context) {
        List<ShortcutApp> defaultData = new ArrayList<ShortcutApp>();
        List<ComponentName> defaultAppDrawer = new ArrayList<ComponentName>();
        defaultAppDrawer.add(new ComponentName("com.android.email", "com.android.email.activity.ComposeActivityEmail"));
        defaultAppDrawer.add(new ComponentName("com.android.calendar", "com.android.calendar.event.EditEventActivity"));
        defaultAppDrawer.add(new ComponentName("com.android.mms", "com.android.mms.ui.ComposeMessageActivity"));

        defaultAppDrawer.add(new ComponentName("com.smartisanos.notes", "com.smartisanos.notes.CreateNotesActivity"));
        defaultAppDrawer.add(new ComponentName(COM_SINA_WEIBO, COM_SINA_WEIBO_COMPOSERINDE_COMPOSER_DISPATCH_ACTIVITY));
        defaultAppDrawer.add(new ComponentName(WECHAT_PACKAGE, WECHAT_SHARE_ACTIVITY));

        defaultAppDrawer.add(new ComponentName("com.dianping.v1", "com.dianping.share.activity.OuterReceiveActivity"));
        defaultAppDrawer.add(new ComponentName(COM_AUTONAVI_MINIMAP, COM_AUTONAVI_MAP_ACTIVITY_NEW_MAP_ACTIVITY));
        defaultAppDrawer.add(new ComponentName("com.tencent.mobileqq", "com.tencent.mobileqq.activity.JumpActivity"));

        defaultAppDrawer.add(new ComponentName("com.jingdong.app.mall", "com.jd.lib.search.view.Activity.ProductListActivity"));
        defaultAppDrawer.add(new ComponentName("com.taobao.taobao", "com.taobao.search.mmd.SearchResultActivity"));
        defaultAppDrawer.add(new ComponentName("com.smartisanos.recorder", "com.smartisanos.recorder.activity.RecorderPlayActivity"));

        PackageManager pm = context.getPackageManager();
        List<PackageInfo> pinfo = pm.getInstalledPackages(0);
        for (ComponentName component : defaultAppDrawer) {
            ShortcutApp item = getShareItem(pm, component, pinfo, context);
            if (item != null) {
                defaultData.add(item);
            }
        }
        return defaultData;
    }
}
