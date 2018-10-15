package com.smartisanos.voice.expandscreen;

import java.util.List;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;

import com.smartisanos.voice.util.WorkHandler;

import android.database.Cursor;

// For [Rev One]
// Get all app name, and save package name, activity name, app name to database
// when received boot completed broadcast.
public class ApplicationChangeReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(final Context context, Intent intent) {
        String action = intent.getAction();
        if (Intent.ACTION_BOOT_COMPLETED.equals(action)
                || Intent.ACTION_LOCALE_CHANGED.equals(action)) {
            WorkHandler.post(new Runnable() {
                @Override
                public void run() {
                    PackageManager pm = context.getPackageManager();

                    Intent mainAppIntent = new Intent();
                    mainAppIntent.setAction(Intent.ACTION_MAIN);
                    mainAppIntent.addCategory(Intent.CATEGORY_LAUNCHER);
                    List<ResolveInfo> foundActivityList = pm.queryIntentActivities(mainAppIntent, 0);

                    ContentResolver resolver = context.getContentResolver();
                    resolver.delete(ExpandVoiceProvider.CONTENT_URI, null, null);

                    if (0 < foundActivityList.size()) {
                        ContentValues[] values = new ContentValues[foundActivityList.size()];
                        for (int i = 0; i < foundActivityList.size(); i++) {
                            ContentValues value = new ContentValues();
                            String packageName = foundActivityList.get(i).activityInfo.packageName;
                            String activityname = foundActivityList.get(i).activityInfo.name;
                            String appName = foundActivityList.get(i).loadLabel(pm).toString();
                            String appNamePinyin = VoiceCommandHanziToPinyin.getPinYin(appName);

                            value.put(ExpandVoiceProvider.APP_COLUMN_PKGNAME, packageName);
                            value.put(ExpandVoiceProvider.APP_COLUMN_ACTIVITYNAME, activityname);
                            value.put(ExpandVoiceProvider.APP_COLUMN_APPNAME, appName);
                            value.put(ExpandVoiceProvider.APP_COLUMN_APPNAMEPY, appNamePinyin);
                            values[i] = value;
                        }

                        resolver.bulkInsert(ExpandVoiceProvider.CONTENT_URI, values);
                    }
                }
            });
        }
        else if (Intent.ACTION_PACKAGE_ADDED.equals(action)){
            WorkHandler.post(new Runnable() {
                @Override
                public void run() {
                    String packageName = intent.getData().getSchemeSpecificPart();

                    PackageManager pm = context.getPackageManager();
                    Intent mainAppIntent = new Intent();
                    mainAppIntent.setAction(Intent.ACTION_MAIN);
                    mainAppIntent.addCategory(Intent.CATEGORY_LAUNCHER);
                    mainAppIntent.setPackage(packageName);
                    List<ResolveInfo> foundActivityList = pm.queryIntentActivities(mainAppIntent, 0);

                    ContentResolver resolver = context.getContentResolver();
                    if (0 < foundActivityList.size()) {
                        String activityName = foundActivityList.get(0).activityInfo.name;
                        String appName = foundActivityList.get(0).loadLabel(pm).toString();

                        ContentValues value = new ContentValues();
                        value.put(ExpandVoiceProvider.APP_COLUMN_PKGNAME, packageName);
                        value.put(ExpandVoiceProvider.APP_COLUMN_ACTIVITYNAME, activityName);
                        value.put(ExpandVoiceProvider.APP_COLUMN_APPNAME, appName);
                        value.put(ExpandVoiceProvider.APP_COLUMN_APPNAMEPY, VoiceCommandHanziToPinyin.getPinYin(appName));

                        if (isExist(resolver, packageName)){
                            resolver.update(ExpandVoiceProvider.CONTENT_URI, value,
                                    ExpandVoiceProvider.APP_COLUMN_PKGNAME + " = ?", new String[]{packageName});
                        }
                        else{
                            resolver.insert(ExpandVoiceProvider.CONTENT_URI, value);
                        }
                    }
                }
            });
        }
    }

    private boolean isExist(ContentResolver resolver, String packageName){
        Cursor cursor = resolver.query(ExpandVoiceProvider.CONTENT_URI,
                null,
                ExpandVoiceProvider.APP_COLUMN_PKGNAME + " = ?",
                new String[]{packageName},
                null);

        if (null != cursor && cursor.moveToFirst()) {
           cursor.close();
           return true;
        }

        return false;
    }
}
