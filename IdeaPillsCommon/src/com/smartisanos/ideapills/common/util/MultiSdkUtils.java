package com.smartisanos.ideapills.common.util;

import android.app.ActivityManager;
import android.app.ActivityManagerNative;
import android.app.ActivityOptions;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.view.View;

import java.util.ArrayList;

public class MultiSdkUtils {
    public static final int FLAG_MAC_MODE = 0;

    public static void sendNotification(Context context, String channelId, CharSequence name, int id, Notification.Builder builder) {
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(id, builder.build());
    }

    public static ArrayList<ActivityManager.RunningTaskInfo> getTasks(int maxTask) {
        try {
            return (ArrayList<ActivityManager.RunningTaskInfo>) ActivityManagerNative.getDefault().getTasks(maxTask, 0);
        } catch (Exception e) {
            // ignore
        }
        return null;
    }

    public static boolean moveTaskToFront(int taskId) {
        try {
            final ActivityOptions options = ActivityOptions.makeBasic();
            // resume side bar activity that was started before.
            ActivityManagerNative.getDefault().moveTaskToFront(taskId, 0, options.toBundle());
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public static void removeTask(ActivityManager activityManager, int taskId, int flags) {

    }

    public static void SetStartingSearchWindows(boolean isStarting) {

    }

    public static void cancelDragAndDropWithResult(View view, boolean cancel) {

    }

    public static int GetSidebarWidth() {
        return 0;
    }

    public static void toggleGallerySidebar() {

    }

    public static void setLaunchDisplayId(Context context, ActivityOptions options) {

    }
}
