package com.smartisanos.voice.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import com.smartisanos.voice.engine.GrammarManager;
import com.smartisanos.voice.providers.VoiceSettings;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.content.res.XmlResourceParser;
import android.database.Cursor;

import android.net.Uri;
import android.text.TextUtils;
import android.text.format.Formatter;
import android.util.AttributeSet;
import android.util.Log;
import android.util.Pair;
import android.util.Xml;

import smartisanos.api.PackageManagerSmt;
import smartisanos.app.voiceassistant.ApplicationStruct;
import smartisanos.app.voiceassistant.ParcelableMap;

import com.smartisanos.voice.R;

public class ApplicationUtil {
    static final LogUtils log = LogUtils.getInstance(ApplicationUtil.class);
    static ArrayList<ApplicationStruct> sAppStructList = new ArrayList<ApplicationStruct>(2);
    static ArrayList<String> sLastAppNameList = new ArrayList<String>(2);
    static boolean isUpdating =false;

    public static ArrayList<String> getAppNameList(Context context) {
        if (context == null) {
            return null;
        }
        // first get info from sLastAppNameList ,or get it from database
        if (sLastAppNameList.size() == 0) {
            // select title from sara where type = 0 and applicationtype = 0;
            Cursor c = context
                    .getContentResolver()
                    .query(VoiceSettings.ApplicationColumns.CONTENT_URI_APPLICATION,
                            VoiceSettings.ApplicationColumns.PROJECTION_LAST_APP,
                            VoiceSettings.ApplicationColumns.ITEM_APPLICATOIN_TYPE + "=?",
                            new String[] {
                                    String.valueOf(VoiceSettings.ApplicationColumns.ITEM_TYPE_APP) }, null);
            synchronized (sLastAppNameList) {
                try {
                    if (c != null && c.getCount() > 0) {
                        while (c.moveToNext()) {
                            String name = c.getString(c.getColumnIndex(VoiceSettings.ApplicationColumns.NAME));
                            if (!sLastAppNameList.contains(name)) {
                                sLastAppNameList.add(name);
                            }
                        }
                    }
                } catch (Exception e) {
                    sLastAppNameList.clear();
                    log.e("it has names in last list "+e.getMessage());
                } finally {
                    if (c != null) {
                        c.close();
                    }
                }
            }
        }
        if (sLastAppNameList.size()==0){
            buildAppList(context,true,true,true);
        }
        return new ArrayList<String>(sLastAppNameList);
    }

    public static List<String> getCustomizedTitle(String title) {
        List<String> customTitles = new ArrayList<String>();

        if (!TextUtils.isEmpty(title)) {
            Pattern pWord = Pattern.compile("[\\u4e00-\\u9fa5]|[a-zA-Z]+|[0-9]+");
            List<String> words = new ArrayList<String>();
            Matcher matcher = pWord.matcher(title);
            while (matcher.find()) {
                words.add(matcher.group());
            }

            int size = words.size();
            // size > seg.first, concat fist seg.first words or last seg.second words as a custom title.
            List<Pair<Integer, Integer>> segRule = new ArrayList<Pair<Integer, Integer>>();
            segRule.add(new Pair<Integer, Integer>(3, 2));
            segRule.add(new Pair<Integer, Integer>(5, 3));
            segRule.add(new Pair<Integer, Integer>(6, 4));
            segRule.add(new Pair<Integer, Integer>(7, 5));
            for (Pair<Integer, Integer> seg : segRule) {
                if (size >= seg.first && seg.first > seg.second) {
                    String tmpPrefix = TextUtils.join("", words.subList(0, seg.second));
                    if (!customTitles.contains(tmpPrefix) && !TextUtils.isEmpty(tmpPrefix)) {
                        customTitles.add(tmpPrefix);
                    }
                    String tmpSuffix = TextUtils.join("", words.subList(size - seg.second, size));
                    if (!customTitles.contains(tmpSuffix) && !TextUtils.isEmpty(tmpSuffix)) {
                        customTitles.add(tmpSuffix);
                    }
                } else {
                    break;
                }
            }
        }

        return customTitles;
    }

    public static boolean buildAppList(Context context, boolean CheckUpdate,
            boolean isLastAppNameList,boolean needLoadAlias) {
        if (context == null) {
            return false;
        }
        ArrayList<String> appNameList = new ArrayList<String>(2);
        log.d("buildAppList is enter");

        if (CheckUpdate && !isUpdating) {
            SharePrefUtil.putBoolean(context, VoiceConstant.KEY_APP, false);
            isUpdating = true;
            sAppStructList.clear();
            ArrayList<ContentValues> listContentValues = new ArrayList<ContentValues>();
            boolean sucess = false;
            if (!sucess){
                PackageManager packageManager = context.getPackageManager();
                Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
                mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);
                List<ResolveInfo> activities = packageManager
                        .queryIntentActivities(mainIntent, 0);
                int activityCount = activities == null ? 0 : activities.size();

                for (int i = 0; i < activityCount; i++) {
                    ResolveInfo info = activities.get(i);
                    String title = info.loadLabel(packageManager).toString();
                    String activityClassName = info.activityInfo.name;
                    if (TextUtils.isEmpty(title)) {
                        title = activityClassName;
                    }

                    if (!appNameList.contains(title)) {
                        appNameList.add(title);
                    }
                    ContentValues value = readyData(title, getActivityIconUri(context, info.activityInfo),
                            getActivityStartUri(info.activityInfo), -1);
                    listContentValues.add(value);
                }
            }
            if (listContentValues != null && listContentValues.size() > 0) {
                //delete all application data from database, excluding whitelist and alias
                context.getContentResolver()
                        .delete(VoiceSettings.ApplicationColumns.CONTENT_URI_APPLICATION,
                                VoiceSettings.ApplicationColumns.ITEM_APPLICATOIN_TYPE+ "=?",
                                new String[] {
                                        String.valueOf(VoiceSettings.ApplicationColumns.ITEM_TYPE_APP) });
                ContentValues[] values = new ContentValues[listContentValues.size()];
                context.getContentResolver().bulkInsert(
                        VoiceSettings.ApplicationColumns.CONTENT_URI_APPLICATION, listContentValues.toArray(values));
            }
            loadIndexAlias(context, appNameList );
            if (needLoadAlias){
                loadAlias(context);
            }
            SharePrefUtil.putBoolean(context, VoiceConstant.KEY_APP, true);
            synchronized (sLastAppNameList) {
                if (!ListUtils.isSame(sLastAppNameList, appNameList)) {
                    sLastAppNameList.clear();
                    sLastAppNameList.addAll(appNameList);
                    if (!isLastAppNameList) {
                        VoiceUtils.buildGrammar(GrammarManager.LEXICON_APP);
                    }
                    appNameList.clear();
                    isUpdating = false;
                    return true;
                }
            }
            appNameList.clear();
        }
        isUpdating = false;
        return false;
    }

    public static ArrayList<ApplicationStruct> getSearchedLocalAppNameList(
            Context context, String name, ArrayList<String> realNameList) {
        String namePinyin = PinYinUtil.getPinYin(name);
        ArrayList<ApplicationStruct> appStructList = new ArrayList<ApplicationStruct>(2);
        final Cursor c = context
                .getContentResolver()
                .query(VoiceSettings.ApplicationColumns.CONTENT_URI_APPLICATION,
                        VoiceSettings.ApplicationColumns.PROJECTION_REAL_NAME,
                        VoiceSettings.ApplicationColumns.ITEM_NAME_PINYIN +" like ? or "
                                + VoiceSettings.ApplicationColumns.NAME + " like ? ",
                        new String[] { "%"+namePinyin+"%", "%"+namePinyin+"%"},
                        null);
        try {
            if (c != null && c.getCount() > 0) {
                c.moveToFirst();
                do {
                    String realName = c.getString(c
                            .getColumnIndex(VoiceSettings.ApplicationColumns.REAL_NAME));
                    if (!realNameList.contains(realName)) {
                        realNameList.add(realName);
                    }
                } while (c.moveToNext());
            }
        } catch (Exception e) {
            Log.e("ApplicationUtil", "VoiceSettings-realNameList", e);
        } finally {
            if (c != null) {
                c.close();
            }
        }

        if (sAppStructList == null || sAppStructList.size() == 0) {
            Cursor cursor = context
                    .getContentResolver()
                    .query(VoiceSettings.ApplicationColumns.CONTENT_URI_APPLICATION,
                            VoiceSettings.ApplicationColumns.PROJECTION_APP_STRUCT_LIST,
                            VoiceSettings.ApplicationColumns.ITEM_APPLICATOIN_TYPE + "=?",
                            new String[] {
                                    String.valueOf(VoiceSettings.ApplicationColumns.ITEM_TYPE_APP) }, null);
            if (sAppStructList == null) {
                sAppStructList = new ArrayList<ApplicationStruct>(2);
            }
            sAppStructList.clear();
            try {
                if (cursor != null && cursor.getCount() > 0) {
                    cursor.moveToFirst();
                    do {
                        ApplicationStruct appStruct = new ApplicationStruct();
                        appStruct.mAppName = cursor.getString(cursor
                                .getColumnIndex(VoiceSettings.ApplicationColumns.NAME));
                        appStruct.mIconUri = Uri.parse(cursor.getString(cursor
                                .getColumnIndex(VoiceSettings.ApplicationColumns.ICON_URI)));
                        appStruct.mStartUri = Uri.parse(cursor.getString(cursor
                                .getColumnIndex(VoiceSettings.ApplicationColumns.START_URI)));
                        appStruct.mAppIndex = cursor.getInt(cursor.getColumnIndex(VoiceSettings.ApplicationColumns.INDEX));
                        appStruct.mMatchName = DataLoadUtil.getMatchString(appStruct.mAppName,name);
                        sAppStructList.add(appStruct);
                    } while (cursor.moveToNext());
                }
            } catch (Exception e) {
                Log.e("ApplicationUtil", "VoiceSettings-ApplicationStruct", e);
            } finally {
                if (cursor != null){
                    cursor.close();
                }
            }
        }

        ArrayList<String> appNameList = null;
        int len = realNameList.size();
        if (len > 0) {
            for (int i = 0; i < len; i++) {
                appNameList = findWhiteList(context, realNameList.get(i));
                getAppList(context, realNameList.get(i), appStructList, true, false,name);
                int nameLen = (appNameList == null ? 0 : appNameList.size());
                for (int j = 0; j < nameLen; j++) {
                    getAppList(context, appNameList.get(j), appStructList, false, false,name);
                }
            }
        } else {
            getAppList(context, namePinyin, appStructList, false, true,name);
        }
        return appStructList;
    }
    public static  boolean isPackageLocked(Uri uri,PackageManager packageManger) {
        if (uri != null) {
            String[] uriSeg = uri.toString().split("/");
            return PackageManagerSmt.getInstance().isPackageAlreadyLocked(packageManger, uriSeg[uriSeg.length - 2]);
        }
        return false;
    }

    public static void getAppList(Context context, String name,
            ArrayList<ApplicationStruct> appList, boolean isAccurate, boolean isUsePinyin, String searchName) {
        if (name == null || name.trim().length() == 0 || context == null) {
            return;
        }
        int activityCount = sAppStructList == null ? 0 : sAppStructList
                .size();
        PackageManager packageManger = context.getPackageManager();
        for (int i = 0; i < activityCount; i++) {
            if (isUpdating) {
                break;
            }
            ApplicationStruct info = sAppStructList.get(i);
            if (isAccurate) {
                if (info.mAppName.equalsIgnoreCase(name.toLowerCase(Locale.getDefault()))) {
                    if (isPackageLocked(info.mStartUri,packageManger)) {
                        continue;
                    }
                    if (!appList.contains(info)) {
                        if (TextUtils.isEmpty(searchName)) {
                            info.setMatchName(DataLoadUtil.getMatchString(info.mAppName,searchName));
                        }
                        appList.add(info);
                    }
                }
            } else {
                if (!isUsePinyin) {
                    if (info.mAppName.contains(name.toLowerCase(Locale.getDefault()))
                            || name.toLowerCase(Locale.getDefault()).contains(
                                    info.mAppName.toLowerCase(Locale.getDefault()))) {
                        if (isPackageLocked(info.mStartUri,packageManger)) {
                            continue;
                        }
                        if (!appList.contains(info)) {
                            if (TextUtils.isEmpty(searchName)) {
                                info.setMatchName(DataLoadUtil.getMatchString(info.mAppName,searchName));
                            }
                            appList.add(info);
                        }
                    }
                } else {
                    Pattern p = Pattern.compile(VoiceConstant.REGEX_SPECIAL);
                    Matcher m;
                    String[] splitResult = info.mAppName
                            .split(VoiceConstant.REGEX_SEPARATOR);
                    for (int j = 0; j < splitResult.length; j++) {
                        m = p.matcher(splitResult[j]);
                        String temp = m.replaceAll("")/*.replaceAll(VoiceConstant.REGEX_NOT_NORMAL, "")*/;
                        if (!TextUtils.isEmpty(temp)) {
                            String pinyin = PinYinUtil.getPinYin(temp);
                            if (pinyin.equalsIgnoreCase(name.toLowerCase(Locale.US))) {
                                if (isPackageLocked(info.mStartUri,packageManger)) {
                                    continue;
                                }
                                if (!appList.contains(info)) {
                                    appList.add(info);
                                    if (TextUtils.isEmpty(searchName)) {
                                        info.setMatchName(DataLoadUtil.getMatchString(info.mAppName,searchName));
                                    }
                                }
                                break;
                            }
                        }
                    }
                }
            }
        }
    }

    public static ArrayList<String> getAliasName(Context context, ArrayList<String> names) {
        ArrayList<String> validNames = new ArrayList<String>();
        ArrayList<String> aliasList = new ArrayList<String>();
        if (names != null && names.size() > 0) {
            StringBuffer selections = new StringBuffer();
            selections.append(VoiceSettings.ApplicationColumns.ITEM_APPLICATOIN_TYPE)
                    .append(" = ").append(VoiceSettings.ApplicationColumns.ITEM_TYPE_ALIAS)
                    .append(" and ")
                    .append(VoiceSettings.ApplicationColumns.REAL_NAME).append(" in (");
            for (String name : names) {
                if (!TextUtils.isEmpty(name)) {
                    selections.append("?,");
                    validNames.add(name);
                }
            }

            if (validNames.size() == 0) {
                return aliasList;
            }

            selections.deleteCharAt(selections.length() - 1);
            selections.append(")");

            String[] values = new String[validNames.size()];
            validNames.toArray(values);
            Cursor c = context
                    .getContentResolver()
                    .query(VoiceSettings.ApplicationColumns.CONTENT_URI_APPLICATION,
                            VoiceSettings.ApplicationColumns.PROJECTION_LAST_APP,
                            selections.toString(),
                            values, null);
            try {
                if (c != null && c.getCount() > 0) {
                    c.moveToFirst();
                    do {

                        String title = c.getString(0);
                        if (!aliasList.contains(title)) {
                            aliasList.add(title);
                        }
                    } while (c.moveToNext());
                }
            } catch (Exception e) {
                Log.e("ApplicationUtil", "getAliasName", e);
            } finally {
                if (c != null) {
                    c.close();
                }
            }
        }
        return aliasList;
    }

    private static ArrayList<String> findWhiteList(Context context, String name) {
        String tempalias = null;
        //replace the full-width space character to half-width and ignore the space
        //character in the front and the end of the name string.
        name = name.replaceAll(" ", " ").trim();
        try {
            tempalias = new String(StringUtils.readFileFromAssets(context, VoiceConstant.APP_WHITELIST_FILE), "UTF-8");
        } catch (Exception e) {
            return null;
        }
        ArrayList<String> temp = new ArrayList<String>();
        if (!TextUtils.isEmpty(tempalias)) {
            String[] destString = tempalias.split(";");
            for (int i = 0; i < destString.length; i++) {
                if (destString[i].contains(name)) {
                    temp.add(name);
                    String[] valueString = destString[i].split(",");
                    for (int j = 0; j < valueString.length; j++) {
                        if (!name.equals(valueString[j])) {
                            temp.add(valueString[j]);
                        }
                    }
                    return temp;
                }
            }
        }
        return temp;
    }

    private static Uri getActivityIconUri(Context context, ActivityInfo activityInfo) {
        int icon = activityInfo.getIconResource();
        if (icon == 0) return null;
        Uri uri = getResourceUri(context, activityInfo.applicationInfo, icon);
        return uri;
    }

    private static Uri getResourceUri(Context context, ApplicationInfo appInfo, int res) {
        try {
            Resources resources = context.getPackageManager().getResourcesForApplication(appInfo);
            return getResourceUri(resources, appInfo.packageName, res);
        } catch (PackageManager.NameNotFoundException e) {
            return null;
        } catch (Resources.NotFoundException e) {
            return null;
        } catch (NullPointerException e) {
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    private static Uri getResourceUri(Resources resources, String appPkg, int res)
            throws Exception {
        String resPkg = resources.getResourcePackageName(res);
        String type = resources.getResourceTypeName(res);
        String name = resources.getResourceEntryName(res);
        return makeResourceUri(appPkg, resPkg, type, name);
    }

    private static Uri makeResourceUri(String appPkg, String resPkg, String type, String name)
            throws Resources.NotFoundException {
        Uri.Builder uriBuilder = new Uri.Builder();
        uriBuilder.scheme(ContentResolver.SCHEME_ANDROID_RESOURCE);
        uriBuilder.encodedAuthority(appPkg);
        uriBuilder.appendEncodedPath(type);
        if (!appPkg.equals(resPkg)) {
            uriBuilder.appendEncodedPath(resPkg + ":" + name);
        } else {
            uriBuilder.appendEncodedPath(name);
        }
        return uriBuilder.build();
    }

    public static Uri getActivityStartUri(ActivityInfo activityInfo) {
        String StartUri = "content://applications/applications/" +
              activityInfo.applicationInfo.packageName+"/"+activityInfo.name;
        return Uri.parse(StartUri);
    }

    private static void loadAlias(Context context) {
        ArrayList<ContentValues> list = new ArrayList<ContentValues>();
        try {
            XmlResourceParser parser = context.getResources().getXml(R.xml.app_alias);
            AttributeSet attrs = Xml.asAttributeSet(parser);
            beginDocument(parser, VoiceSettings.ApplicationColumns.TAG_ALIAS);
            final int depth = parser.getDepth();
            int type;
            while (((type = parser.next()) != XmlPullParser.END_TAG ||
                    parser.getDepth() > depth) && type != XmlPullParser.END_DOCUMENT) {
                if (type != XmlPullParser.START_TAG) {
                    continue;
                }
                TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.alias);
                String realName = a.getString(R.styleable.alias_realName);
                String aliasNames = a.getString(R.styleable.alias_aliasName);
                String[] alias = aliasNames.split(",");
                for(int i = 0;i< alias.length;i++){
                    ContentValues value = new ContentValues();
                    final String titlePinyin = PinYinUtil.getPinYin(alias[i]);
                    value.put(VoiceSettings.ApplicationColumns.NAME, alias[i]);
                    value.put(VoiceSettings.ApplicationColumns.REAL_NAME, realName);
                    value.put(VoiceSettings.ApplicationColumns.ITEM_NAME_PINYIN, titlePinyin);
                    value.put(VoiceSettings.ApplicationColumns.ITEM_APPLICATOIN_TYPE,
                            VoiceSettings.ApplicationColumns.ITEM_TYPE_ALIAS);
                    list.add(value);
                }
                a.recycle();
            }
            if (list != null && list.size() > 0) {
                ContentValues[] values = new ContentValues[list.size()];
                context.getContentResolver().bulkInsert(
                        VoiceSettings.ApplicationColumns.CONTENT_URI_APPLICATION, list.toArray(values));
            }
        } catch (XmlPullParserException e) {
            log.e("Got exception parsing favorites."+e);
        } catch (IOException e) {
            log.e("Got exception parsing favorites."+ e);
        } catch (RuntimeException e) {
            log.e("Got exception parsing favorites."+ e);
        }
    }

    private static void loadIndexAlias(Context context,
            ArrayList<String> appNameList) {
        ArrayList<ContentValues> list = new ArrayList<ContentValues>();
        try {
            XmlResourceParser parser = context.getResources().getXml(R.xml.index_alias);
            AttributeSet attrs = Xml.asAttributeSet(parser);
            beginDocument(parser, VoiceSettings.ApplicationColumns.TAG_FAVORITES);
            final int depth = parser.getDepth();
            int type;
            while (((type = parser.next()) != XmlPullParser.END_TAG || parser
                    .getDepth() > depth) && type != XmlPullParser.END_DOCUMENT) {
                if (type != XmlPullParser.START_TAG) {
                    continue;
                }
                TypedArray a = context.obtainStyledAttributes(attrs,
                        R.styleable.favorites);
                int index = a.getInteger(R.styleable.favorites_index, -1);
                String lableName = a.getString(R.styleable.favorites_lableName);
                String icon = a.getString(R.styleable.favorites_icon);
                String uri = a.getString(R.styleable.favorites_uri);
                ContentValues value = readyData(lableName, Uri.parse(icon), Uri.parse(uri),index);
                list.add(value);
                if (appNameList != null && !appNameList.contains(lableName) && !TextUtils.isEmpty(lableName)) {
                    appNameList.add(lableName);
                }
                a.recycle();
            }
            if (list != null && list.size() > 0) {
                ContentValues[] values = new ContentValues[list.size()];
                context.getContentResolver().bulkInsert(
                        VoiceSettings.ApplicationColumns.CONTENT_URI_APPLICATION,
                        list.toArray(values));
            }
        } catch (XmlPullParserException e) {
            log.e("Got exception parsing favorites." + e);
        } catch (IOException e) {
            log.e("Got exception parsing favorites." + e);
        } catch (RuntimeException e) {
            log.e("Got exception parsing favorites." + e);
        }
    }
    private static final void beginDocument(XmlPullParser parser, String firstElementName)
            throws XmlPullParserException, IOException {
        int type;
        while ((type = parser.next()) != XmlPullParser.START_TAG
                && type != XmlPullParser.END_DOCUMENT) {
        }

        if (type != XmlPullParser.START_TAG) {
            throw new XmlPullParserException("No start tag found");
        }

        if (!parser.getName().equals(firstElementName)) {
            throw new XmlPullParserException("Unexpected start tag: found " + parser.getName() +
                    ", expected " + firstElementName);
        }
    }

    public static ContentValues readyData(String title, Uri iconUri, Uri startUri, int index) {
        ApplicationStruct appStruct = new ApplicationStruct();
        appStruct.mAppName = title;
        appStruct.mIconUri = iconUri;
        appStruct.mStartUri = startUri;
        appStruct.mAppIndex = index;
        sAppStructList.add(appStruct);
        ContentValues value = new ContentValues();
        value.put(VoiceSettings.ApplicationColumns.NAME, title);
        value.put(VoiceSettings.ApplicationColumns.ICON_URI,
                iconUri == null ? "" : iconUri.toString());
        value.put(VoiceSettings.ApplicationColumns.START_URI,
                startUri == null ? "" : startUri.toString());
        value.put(VoiceSettings.ApplicationColumns.ITEM_NAME_PINYIN,
                PinYinUtil.getPinYin(title));
        value.put(VoiceSettings.ApplicationColumns.REAL_NAME, title);
        value.put(VoiceSettings.ApplicationColumns.ITEM_APPLICATOIN_TYPE,
                VoiceSettings.ApplicationColumns.ITEM_TYPE_APP);
        value.put(VoiceSettings.ApplicationColumns.INDEX, index);
        return value;
    }

    public static boolean hasApp(Context context){
          PackageManager packageManager = context.getPackageManager();
          Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
          mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);
          List<ResolveInfo> activities = packageManager.queryIntentActivities(mainIntent, 0);
          if (activities != null && activities.size() >0 ){
              return true;
          } else {
              return false;
          }
    }
    public static ArrayList<ApplicationStruct> OrderApps(Context context, ArrayList<ApplicationStruct> apps, ArrayList<String> realName) {
            ArrayList<ApplicationStruct> tmpApps = new ArrayList<ApplicationStruct>();
            try {
                 ParcelableMap parcelableMap = VoiceUtils.getCurrentAllPackageLaunchCounts(context);
                 Map<String, Integer> allApp = parcelableMap.getMap();
                for (int i = 0; i < apps.size(); i++) {
                    ApplicationStruct as = apps.get(i);
                    String packageName = as.mStartUri.getPathSegments().get(1);
                    if (ApplicationUtil.isPackageLocked(as.mStartUri, context.getPackageManager())){
                        tmpApps.add(as);
                        continue;
                    }
                    if (allApp != null && allApp.containsKey(packageName)) {
                        if (allApp.get(packageName) == null) {
                            as.mLaunchCount = 0;
                        } else {
                            as.mLaunchCount = allApp.get(packageName);
                        }
                    }
                    for (int j = 0; j < realName.size(); j++) {
                        if (realName.get(j).equals(as.mAppName)) {
                            tmpApps.add(as);
                            break;
                        }
                    }
                    as.setInstalledState(1);
                }
                apps.removeAll(tmpApps);
                Collections.sort(apps, new LaunchCountComparator());
                apps.addAll(tmpApps);
            } catch (Exception e) {
                log.e("exception is : " + e.getMessage());
            }
        return apps;
    }
    static class LaunchCountComparator implements Comparator<ApplicationStruct> {
        public final int compare(ApplicationStruct a, ApplicationStruct b) {
            return a.mLaunchCount - b.mLaunchCount;
        }
    }

    public static  ArrayList<ApplicationStruct> checkUninstalledApps(Context context, ArrayList<ApplicationStruct> appList) {
        PackageManager packageManager = context.getPackageManager();
        List<PackageInfo> pinfo = packageManager.getInstalledPackages(0);
        ArrayList<ApplicationStruct> installedApps = new ArrayList<>();
        for (ApplicationStruct app : appList) {
            String packageName = app.getPackageName();
            String size = app.getAppSize();
            app.setAppSize(Formatter.formatFileSize(context, Long.valueOf(size)));
            for (int i = 0; i < pinfo.size(); i++) {
                if (((PackageInfo) pinfo.get(i)).packageName
                        .equalsIgnoreCase(packageName)) {
                    installedApps.add(app);
                    break;
                }
            }
        }
        appList.removeAll(installedApps);
        return appList;
    }

    public static ArrayList<ApplicationStruct> getSearchedUninstallAppNameList(Context context, String key) {
        HttpURLConnection conn = null;
        BufferedReader reader = null;
        String encodeKey;
        try {
            encodeKey = URLEncoder.encode(key, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            encodeKey = key;
        }
        try {
            URL url = new URL("http://api-app.smartisan.com/api/v1_5/search/apps?size=10&kwd=" + encodeKey + "&page=1&version=2&source=2");
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(10 * 1000);
            if (conn.getResponseCode() == 200) {
                InputStream inputStream = conn.getInputStream();
                reader = new BufferedReader(new InputStreamReader(inputStream));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                String appContents = response.toString();
                log.d("getSearchedUninstallAppNameList -- appContents = " + appContents);
                ArrayList<ApplicationStruct> tempApps = XmlParser.parseApp(appContents.toString());
                if (tempApps != null && tempApps.size() > 0) {
                    return ApplicationUtil.checkUninstalledApps(context, tempApps);
                } else {
                    return null;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (conn != null) {
                conn.disconnect();
            }
        }
        return null;
    }
}
