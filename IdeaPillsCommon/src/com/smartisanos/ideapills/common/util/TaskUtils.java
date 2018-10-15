package com.smartisanos.ideapills.common.util;

import android.app.ActivityManager;
import android.app.ActivityManagerNative;
import android.app.ActivityOptions;
import android.content.Context;
import android.text.TextUtils;

import java.util.ArrayList;

public class TaskUtils {
    private static final int MAX_TASKS = 32;

    private static int getTaskId(String packageName, String activityName, boolean isActivityNameCanNull) {
        ArrayList<ActivityManager.RunningTaskInfo> recentTasks = MultiSdkUtils.getTasks(MAX_TASKS);
        if (recentTasks == null) {
            return -1;
        }

        int numTasks = recentTasks.size();
        for (int i = 0; i < numTasks; ++i) {
            final ActivityManager.RunningTaskInfo ri = recentTasks.get(i);
            if (ri == null) {
                continue;
            }

            // Don't remove the current showing side bar activity.
            if (ri.baseActivity != null
                    && ri.baseActivity.getPackageName().equals(packageName)) {
                if ((activityName == null && isActivityNameCanNull)
                        || ri.baseActivity.getClassName().equals(activityName)) {
                    return ri.id;
                }
            }
        }
        return -1;
    }

    public static int getTaskId(String packageName, String activityName) {
        return getTaskId(packageName, activityName, false);
    }

    public static int getTaskId(String packageName) {
        return getTaskId(packageName, null, true);
    }

    public static boolean resumeActivity(String packageName, String activityName) {
        return resumeActivity(packageName, activityName, getTaskId(packageName, activityName));
    }

    public static boolean resumeActivity(String packageName, String activityName, int taskId) {
        if (taskId > 0) {
            MultiSdkUtils.moveTaskToFront(taskId);
        }
        return false;
    }


    public static void removeTask(Context context, String packageName, String activityName, int taskId) {
        if (context != null) {
            ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
            ArrayList<ActivityManager.RunningTaskInfo> recentTasks = MultiSdkUtils.getTasks(MAX_TASKS);
            int numTasks = recentTasks.size();
            for (int i = 0; i < numTasks; ++i) {
                final ActivityManager.RunningTaskInfo ri = recentTasks.get(i);
                if (ri == null) {
                    continue;
                }
                if (ri.baseActivity != null
                        && ri.baseActivity.getPackageName().equals(packageName)
                        && ri.baseActivity.getClassName().equals(activityName)) {
                    if (taskId != ri.id) {
                        try {
                            MultiSdkUtils.removeTask(activityManager, ri.id, 0);
                        } catch (Exception e) {

                        }
                    }
                }
            }
        }
    }

    public static void removeTask(Context context, int taskId) {
        if (context != null && taskId > 0) {
            try {
                ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
                MultiSdkUtils.removeTask(am, taskId, 0);
            } catch (Exception e) {
            }
        }
    }

    public static boolean removeTask(Context context, String packageName, String activityName) {
        int taskId = 0;
        if (context != null && !TextUtils.isEmpty(packageName) && !TextUtils.isEmpty(activityName)) {
            taskId = getTaskId(packageName, activityName);
            removeTask(context, taskId);
        }
        return taskId > 0;
    }
}
