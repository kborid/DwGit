package com.smartisanos.ideapills.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.provider.Settings;

import com.smartisanos.ideapills.BubbleController;
import com.smartisanos.ideapills.Constants;
import com.smartisanos.ideapills.common.util.PackageUtils;
import com.smartisanos.ideapills.util.LOG;
import com.smartisanos.ideapills.util.Utils;

import smartisanos.api.SettingsSmt;

public class PackageActionReceiver extends BroadcastReceiver {

    private static final LOG log = LOG.getInstance(PackageActionReceiver.class);

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (Intent.ACTION_PACKAGE_CHANGED.equals(action)
                || Intent.ACTION_PACKAGE_REMOVED.equals(action)
                || Intent.ACTION_PACKAGE_ADDED.equals(action)) {
            if (!Constants.WINDOW_READY) {
                log.error("Constants.WINDOW_READY false ! abandon action "+ action);
                return;
            }
            String pkg = intent.getData().getSchemeSpecificPart();
            boolean packageChange = false;
            if (Constants.WECHAT.equals(pkg)) {
                Constants.WECHAT_INSTALLED = PackageUtils.isAvilibleApp(context, Constants.WECHAT);
                packageChange = true;
            } else if (Constants.BOOM.equals(pkg)) {
                Constants.BOOM_INSTALLED = PackageUtils.isAvilibleApp(context, Constants.BOOM);
                packageChange = true;
            } else if (Constants.BULLET_MESSENGER.equals(pkg)) {
                if (Intent.ACTION_PACKAGE_ADDED.equals(action)) {
                    handleBulletInstalledAction(context, pkg);
                }
                Constants.BULLET_MESSENGER_INSTALLED = PackageUtils.isAvilibleApp(context, Constants.BULLET_MESSENGER);
                packageChange = true;
            }
            if (packageChange) {
                BubbleController.getInstance().onPackageChange();
            }
        }
    }

    private void handleBulletInstalledAction(Context context, String pkg) {
        int value = Settings.Global.getInt(context.getContentResolver(), SettingsSmt.Global.VOICE_INPUT_SHOW_RESULT_KEY, SettingsSmt.VOICE_SHOW_RESULT_VALUE.DEFAULT_AUTO);
        if(SettingsSmt.VOICE_SHOW_RESULT_VALUE.DEFAULT_AUTO == value) {
            Settings.Global.putInt(context.getContentResolver(), SettingsSmt.Global.VOICE_INPUT_SHOW_RESULT_KEY, SettingsSmt.VOICE_SHOW_RESULT_VALUE.BULLET);
        }
    }
}