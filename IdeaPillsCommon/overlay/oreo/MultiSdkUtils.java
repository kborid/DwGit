package com.smartisanos.ideapills.common.util;

import android.app.ActivityManager;
import android.app.ActivityOptions;
import android.app.Notification;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.SmtPCUtils;
import android.content.Context;
import android.pc.ISmtPCManager;
import android.os.RemoteException;
import android.view.KeyEvent;
import android.view.View;

import java.util.ArrayList;

public class MultiSdkUtils {
    public static final int FLAG_MAC_MODE = KeyEvent.FLAG_MAC_MODE;

    public static void sendNotification(Context context, String channelId, CharSequence name, int id, Notification.Builder builder) {
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        int importance = NotificationManager.IMPORTANCE_DEFAULT;
        NotificationChannel mChannel = new NotificationChannel(channelId, name, importance);

        builder.setChannelId(channelId);
        Notification notification = builder.build();

        if (notification.sound != null) {
            mChannel.setSound(notification.sound, Notification.AUDIO_ATTRIBUTES_DEFAULT);
        }

        notificationManager.createNotificationChannel(mChannel);
        notificationManager.notify(id, notification);
    }

    public static ArrayList<ActivityManager.RunningTaskInfo> getTasks(int maxTask) {
        try {
            return (ArrayList<ActivityManager.RunningTaskInfo>) ActivityManager.getService().getTasks(maxTask, 0);
        } catch (Exception e) {
            // ignore
        }
        return null;
    }

    public static boolean moveTaskToFront(int taskId) {
        try {
            final ActivityOptions options = ActivityOptions.makeBasic();
            // resume side bar activity that was started before.
            ActivityManager.getService().moveTaskToFront(taskId, 0, options.toBundle());
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public static void removeTask(ActivityManager activityManager, int taskId, int flags) {
        if (activityManager != null) {
            try {
                activityManager.removeTaskWithFlag(taskId, flags);
            } catch (Exception e) {

            }
        }
    }

    public static void SetStartingSearchWindows(boolean isStarting) {
        try {
            ISmtPCManager pcManager = SmtPCUtils.getSmtPCManager();
            if (pcManager != null) {
                pcManager.smtSetStartingSearchWindows(isStarting);
            }
        } catch (RemoteException ex) {
        }
    }

    public static void cancelDragAndDropWithResult(View view, boolean cancel) {
        if (view != null) {
            view.cancelDragAndDropWithResult(cancel);
        }
    }

    public static int GetSidebarWidth() {
        ISmtPCManager pcManager = SmtPCUtils.getSmtPCManager();
        if (pcManager != null) {
            try {
                return pcManager.smtGetSidebarWidth();
            } catch (RemoteException ex) {
                //ignore
            }
        }
        return 0;
    }

    public static void toggleGallerySidebar() {
        try {
            final String pkgName = "com.android.gallery3d";
            final String actName = "com.android.gallery3d.app.SideBarGallery";
            ISmtPCManager pcManager = SmtPCUtils.getSmtPCManager();
            if (pcManager != null) {
                pcManager.smtToggleSideBarActivity(pkgName, actName);
            }
        } catch (RemoteException ex) {
            //ignore
        }
    }

    public static void setLaunchDisplayId(Context context, ActivityOptions options) {
        if (context != null && options != null) {
            options.setLaunchDisplayId(SmtPCUtils.getExtDisplayId(context));
        }
    }
}