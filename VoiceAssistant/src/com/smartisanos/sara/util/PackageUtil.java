package com.smartisanos.sara.util;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.text.TextUtils;


public class PackageUtil {

    private static final String COMPONENT_RESOLVERACTIVITY = "smartisanos.app.ResolverActivity";
    private static final String COMPONENT_RESOLVERACTIVITY_ANDROID = "com.android.internal.app.ResolverActivity";


    public static PackageInfo getPackageInfo(Context context, String packageName) {
        if (null == context || TextUtils.isEmpty(packageName)) {
            return null;
        }
        PackageInfo info = null;
        Context appContext = context.getApplicationContext();
        try {
            info = appContext.getPackageManager().getPackageInfo(packageName, 0);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return info;
    }

    public static String getCorrectClassName(Context context, String defaultClassName) {
        String className = defaultClassName;

        //get correct 'ContactsSyncUI' class name by WeChat version code
        if (TextUtils.equals(SaraConstant.WEIXIN_CLASS_NAME, defaultClassName)) {
            PackageInfo info = getPackageInfo(context, SaraConstant.WEIXIN_PACKAGE_NAME);
            if (null != info) {
                className = info.versionCode <= 1280
                            ? SaraConstant.WEIXIN_CLASS_NAME
                            : SaraConstant.WEIXIN_CLASS_NAME_1281;
            }
        }

        if (TextUtils.isEmpty(className)) {
            className = defaultClassName;
        }
        return className;
    }

    /**
     * @return has the default app been chosen to handle this intent
     */
    public static boolean isDefaultAppChosen(Context context, Intent intent) {
        if (null == context || null == intent) {
            return false;
        }
        boolean chosen = false;
        ResolveInfo info = context.getPackageManager().resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY);
        if (info != null && info.activityInfo != null) {
            chosen = !TextUtils.equals(info.activityInfo.name, COMPONENT_RESOLVERACTIVITY)
                     && !TextUtils.equals(info.activityInfo.name, COMPONENT_RESOLVERACTIVITY_ANDROID);
        }
        return chosen;
    }


}
