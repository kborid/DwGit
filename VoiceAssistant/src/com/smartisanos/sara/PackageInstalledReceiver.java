package com.smartisanos.sara;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.smartisanos.ideapills.common.util.PackageUtils;
import com.smartisanos.sara.shell.ShortcutAppManager;

import java.util.List;

import smartisanos.util.LogTag;

public class PackageInstalledReceiver extends BroadcastReceiver {
    private String TAG = "PackageInstalledReceiver";

    @Override
    public void onReceive(Context context, Intent intent){
        if (intent.getAction().equals("android.intent.action.PACKAGE_REMOVED")) {
            String packageName = intent.getDataString();
            LogTag.d(TAG, "package removed " + packageName);
            PackageUtils.saveShareData(context, PackageUtils.getShareItemAvailiableList(context));
            ShortcutAppManager.saveShareData(context, ShortcutAppManager.getShareItemAvailiableList(context), ShortcutAppManager.DATA_TYPE.SELECTED_DATA.name());
        }
    }
}
