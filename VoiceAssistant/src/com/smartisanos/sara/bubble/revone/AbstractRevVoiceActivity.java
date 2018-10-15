package com.smartisanos.sara.bubble.revone;

import android.app.StatusBarManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.os.Bundle;

import com.smartisanos.ideapills.common.util.TaskUtils;
import com.smartisanos.sara.bubble.AbstractVoiceActivity;
import com.smartisanos.sara.util.SaraConstant;

import java.util.ArrayList;
import java.util.List;

import smartisanos.app.voiceassistant.ParcelableObject;

public abstract class AbstractRevVoiceActivity extends AbstractVoiceActivity {
    protected List<String> mStartedPackageList = new ArrayList<String>();

    @Override
    protected void onStop() {
        super.onStop();
        AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        if (audioManager != null) {
            audioManager.abandonAudioFocus(null);
        }
    }

    @Override
    public void startActivity(Intent intent, Bundle options) {
        ComponentName componentName = intent.getComponent();
        if (allowStartActivity(componentName)) {
            super.startActivity(intent, options);
        } else if (componentName != null) {
            TaskUtils.resumeActivity(componentName.getPackageName(), componentName.getClassName());
        }
        String packageName = intent.getPackage();
        if (packageName == null) {
            if (componentName != null) {
                packageName = componentName.getPackageName();
            }
        }
        if (packageName != null && !packageName.equals(getPackageName())) {
            boolean hold = intent.getBooleanExtra(SaraConstant.HOLD_CURRENT_ACTIVITY, false);
            if (hold && !mStartedPackageList.contains(packageName)) {
                mStartedPackageList.add(packageName);
            }
        }
    }

    @Override
    public void startActivityForResult(Intent intent, int requestCode, Bundle options) {
        if (requestCode != -1) {
            ComponentName componentName = intent.getComponent();
            if (allowStartActivity(componentName)) {
                super.startActivityForResult(intent, requestCode, options);
            } else if (componentName != null) {
                TaskUtils.resumeActivity(componentName.getPackageName(), componentName.getClassName());
            }
            String packageName = intent.getPackage();
            if (packageName == null) {
                if (componentName != null) {
                    packageName = componentName.getPackageName();
                }
            }
            if (packageName != null) {
                if (!mStartedPackageList.contains(packageName)) {
                    mStartedPackageList.add(packageName);
                }
            }
        } else {
            super.startActivityForResult(intent, requestCode, options);
        }
    }

    protected void hideUpperWindows() {
        collapseStatusPanels();
        hideLaunchPad();
    }

    private void collapseStatusPanels() {
        StatusBarManager sbm = (StatusBarManager) getSystemService(Context.STATUS_BAR_SERVICE);
        sbm.collapsePanels();
    }

    private void hideLaunchPad() {
        boolean isLaunchPadVisible = smartisanos.util.LaunchPadUtils.isLaunchPadVisible();
        if (isLaunchPadVisible) {
            smartisanos.util.LaunchPadUtils.notifyUpdateLaunchPadStatus();
        }
    }

    protected abstract boolean allowStartActivity(ComponentName component);
}