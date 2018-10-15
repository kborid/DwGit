package com.smartisanos.sara.bubble.revone.manager;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManagerNative;
import android.app.SmtPCUtils;
import android.app.TaskStackListener;
import android.content.ComponentName;
import android.content.Intent;
import android.graphics.Rect;
import android.os.RemoteException;
import android.pc.ISmtPCManager;
import android.view.DisplayInfo;

import com.smartisanos.ideapills.common.util.TaskUtils;
import com.smartisanos.ideapills.common.util.MutiTaskHandler;
import com.smartisanos.sara.util.SaraUtils;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StartedAppManager {
    private final static int PORTRAIT_MAX_HEIGHT = 900;
    private final static int LANDSCAPE_MAX_WIDTH = 1200;
    private static final int MAX_TASKS = 26;
    public static final String APPLICATION_SELECT = "android.intent.action.APPLICATION_SELECTED";
    private static final List<String> NEED_CLEAR_TASK_LIST = Arrays.asList(
            "com.android.mms",
            "com.android.settings");
    private volatile HashMap<Integer, ComponentName> mStartedPkgMap = new HashMap<Integer, ComponentName>();
    private WeakReference<Activity> mActRef;
    private DisplayInfo mDisplayInfo = new DisplayInfo();

    public StartedAppManager(Activity act) {
        mActRef = new WeakReference<>(act);
        SmtPCUtils.getExtDisplay(act).getDisplayInfo(mDisplayInfo);
    }

    public void registerTaskListener() {
        try {
            SmtPCUtils.getSmtPCManager().registerSmtTaskStackListener(mTaskStackListener);
        } catch (RemoteException e) {
        }
    }

    public void unRegisterTaskListener() {
        try {
            if (mTaskStackListener != null) {
                SmtPCUtils.getSmtPCManager().unRegisterSmtTaskStackListener(mTaskStackListener);
                mTaskStackListener = null;
            }
        } catch (RemoteException e) {
        }
    }

    public boolean hasStartActivity(ComponentName component) {
        if (component != null) {
            if (mStartedPkgMap.containsValue(component)) {
                return true;
            }
        }
        return false;
    }

    public boolean closePhoneApp(String packageName) {
        boolean ret = false;
        if (packageName == null) {
            return false;
        }
        try {
            ArrayList<ActivityManager.RunningTaskInfo> recentTasks = (ArrayList<ActivityManager.RunningTaskInfo>) ActivityManagerNative.getDefault().getTasks(MAX_TASKS, 0);
            if (recentTasks != null) {
                int numTasks = recentTasks.size();
                for (int i = 0; i < numTasks; ++i) {
                    final ActivityManager.RunningTaskInfo ri = recentTasks.get(i);
                    final boolean isOnExternalDisplay = SmtPCUtils.isPcDynamicStack(ri.stackId);
                    if (!isOnExternalDisplay && packageName.contains(ri.baseActivity.getPackageName())) {
                        ret = true;
                        ActivityManagerNative.getDefault().removeTask(ri.id);
                    }
                }
            }
        } catch (Exception e) {
        }
        return ret;
    }

    public void closePhoneApp(final List<String> openedPkgList) {
        if (openedPkgList == null || openedPkgList.size() == 0) {
            return;
        }
        MutiTaskHandler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    ArrayList<ActivityManager.RunningTaskInfo> recentTasks = (ArrayList<ActivityManager.RunningTaskInfo>) ActivityManagerNative.getDefault().getTasks(MAX_TASKS, 0);
                    if (recentTasks != null) {
                        int numTasks = recentTasks.size();
                        for (int i = 0; i < numTasks; ++i) {
                            final ActivityManager.RunningTaskInfo ri = recentTasks.get(i);
                            final boolean isOnExternalDisplay = SmtPCUtils.isPcDynamicStack(ri.stackId);
                            if (!isOnExternalDisplay && openedPkgList.contains(ri.baseActivity.getPackageName())) {
                                ActivityManagerNative.getDefault().removeTask(ri.id);
                            }
                        }
                    }
                } catch (Exception e) {
                }
            }
        });
    }

    public void closeStartedApp(final List<String> packageList, final boolean exit, final Intent intent) {
        closeStartedApp(0, packageList, exit, intent, null);
    }

    public void closeStartedApp(final int taskId, final List<String> packageList, final boolean exit, final Intent intent) {
        closeStartedApp(taskId, packageList, exit, intent, null);
    }

    public void closeStartedApp(final int taskId, final List<String> packageList, final boolean exit, final Intent intent, final IAppCloseListener listener) {
        if (exit) {
            unRegisterTaskListener();
        }
        MutiTaskHandler.post(new Runnable() {
            @Override
            public void run() {
                List<Map.Entry<Integer, ComponentName>> startedAppList = new ArrayList<Map.Entry<Integer, ComponentName>>(mStartedPkgMap.entrySet());
                List<String> killTasks = new ArrayList<String>();
                int selectTaskId = 0;
                String selectPackage = null;
                ComponentName removeComponent = null;
                if (startedAppList != null) {
                    int size = startedAppList.size();
                    int id = 0;
                    ComponentName component = null;
                    if (intent != null) {
                        component = intent.getComponent();
                        if (component != null) {
                            selectPackage = component.getPackageName();
                        }
                    }
                    for (int i = 0; i < size; i++) {
                        id = startedAppList.get(i).getKey();
                        if (taskId == id) {
                            component = startedAppList.get(i).getValue();
                            if (component != null && NEED_CLEAR_TASK_LIST.contains(component.getPackageName())) {
                                removeComponent = component;
                            }
                            continue;
                        }
                        component = startedAppList.get(i).getValue();
                        if (packageList != null) {
                            if (selectPackage != null && component != null && selectPackage.equals(component.getPackageName())) {
                                selectTaskId = id;
                                continue;
                            }
                            if (packageList.contains(component.getPackageName())) {
                                killTasks.add(Integer.toString(id));
                            }
                        }
                    }
                }
                try {
                    ISmtPCManager pcManager = SmtPCUtils.getSmtPCManager();
                    if (pcManager != null) {
                        pcManager.smtRemoveTasksFast(killTasks);
                        Rect bounds = calculateResizeBounds(selectPackage);
                        if (selectTaskId > 0 && bounds != null) {
                            pcManager.smtResizeTask(selectTaskId, bounds);
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                if (listener != null) {
                    listener.onAppCloseEnd();
                }
                Activity act = null;
                if (mActRef != null) {
                    act = mActRef.get();
                }
                if (act != null) {
                    if (removeComponent != null) {
                        TaskUtils.removeTask(act, removeComponent.getPackageName(), removeComponent.getClassName(), taskId);
                    }
                    if (selectTaskId > 0) {
                        Intent intent = new Intent();
                        intent.setAction("android.intent.action.SEARCH_WINDOW_SELECTED");
                        intent.putExtra("taskId", selectTaskId);
                        intent.addFlags(Intent.FLAG_RECEIVER_REGISTERED_ONLY);
                        act.sendBroadcast(intent);
                    } else if (intent != null) {
                        SaraUtils.startActivity(act, intent);
                    }
                    if (exit) {
                        act.finish();
                    }
                }
            }
        });
    }

    private Rect calculateResizeBounds(String packageName) {
        int displayWidth = mDisplayInfo.largestNominalAppWidth;
        int displayHeight = mDisplayInfo.smallestNominalAppWidth;

        Rect defaultPortraitBounds = null;
        int defaultPortraitWidth = 360 * mDisplayInfo.logicalDensityDpi / 160;
        int defaultPortraitHeight = defaultPortraitWidth * 16 / 9;
        try {
            ISmtPCManager pcManager = SmtPCUtils.getSmtPCManager();
            if (pcManager != null) {
                defaultPortraitBounds = pcManager.getDefaultWindowBounds(true);
            }
        } catch (RemoteException ex) {
        }
        if (defaultPortraitBounds != null) {
            defaultPortraitWidth = defaultPortraitBounds.width();
            defaultPortraitHeight = defaultPortraitBounds.height();
        }

        // Default will resize as portrait window.
        int resizeWidth = defaultPortraitWidth;
        int resizeHeight = defaultPortraitHeight;

        if ("com.smartisanos.music".equals(packageName)
                || "com.smartisanos.filemanager".equals(packageName)
                || "com.smartisanos.notes".equals(packageName)
                || "com.android.calendar".equals(packageName)
                || "com.android.browser".equals(packageName)
                || "com.android.email".equals(packageName)) {
            // For these packages, don't need resize, app will start new activity by themselves.
            return null;
        }

        Rect resizeBounds = new Rect((displayWidth - resizeWidth) / 2,
                (displayHeight - resizeHeight) / 2,
                (displayWidth + resizeWidth) / 2,
                (displayHeight + resizeHeight) / 2);
        return resizeBounds;
    }

    private TaskStackListener mTaskStackListener = new TaskStackListener() {
        @Override
        public void onTaskCreated(int taskId, ComponentName componentName) throws RemoteException {
            if (taskId != -1 && !mStartedPkgMap.containsKey(taskId)) {
                mStartedPkgMap.put(taskId, componentName);
            }
        }

        @Override
        public void onTaskRemoved(int taskId) throws RemoteException {
            if (mStartedPkgMap.containsKey(taskId)) {
                mStartedPkgMap.remove(taskId);
            }
        }
    };

    public interface IAppCloseListener {
        void onAppCloseEnd();
    }
}
