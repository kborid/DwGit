package com.smartisanos.sara.bubble.revone.manager;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import java.util.List;

public class StartedAppManager {
    public static final String APPLICATION_SELECT = "android.intent.action.APPLICATION_SELECTED";
    public StartedAppManager(Activity act) {
    }

    public void registerTaskListener() {
    }

    public void unRegisterTaskListener() {
    }

    public boolean hasStartActivity(ComponentName component) {
        return false;
    }

    public boolean closePhoneApp(String packageName) {
        return false;
    }

    public void closePhoneApp(final List<String> openedPkgList) {
    }

    public void closeStartedApp(final List<String> packageList, final boolean exit, final Intent intent) {
        closeStartedApp(0, packageList, exit, intent, null);
    }

    public void closeStartedApp(final int taskId, final List<String> packageList, final boolean exit, final Intent intent) {
        closeStartedApp(taskId, packageList, exit, intent, null);
    }

    public void closeStartedApp(final int taskId, final List<String> packageList, final boolean exit, final Intent intent, final IAppCloseListener listener) {
    }

    public interface IAppCloseListener {
        void onAppCloseEnd();
    }
}
