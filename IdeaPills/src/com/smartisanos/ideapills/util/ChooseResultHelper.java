package com.smartisanos.ideapills.util;

import android.app.PendingIntent;
import android.app.SmtPCUtils;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;

import com.smartisanos.ideapills.BubbleController;
import com.smartisanos.ideapills.Constants;
import com.smartisanos.ideapills.entity.BubbleItem;

import java.util.ArrayList;
import java.util.List;

import smartisanos.app.share.ShareItem;
import smartisanos.util.BulletSmsUtils;

public class ChooseResultHelper {
    private static final LOG log = LOG.getInstance(ChooseResultHelper.class);
    private static final String ACTION_CHOOSE_RESULT = "ACTION_CHOOSE_RESULT";
    private static final String ACTION_ATTACH_PICK_RESULT = "ACTION_ATTACH_PICK_RESULT";
    private static final String ACTION_SAHRE_ATTACH_CHOOSE_RESULT = "ACTION_SAHRE_ATTACH_CHOOSE_RESULT";
    public static final String CHOOSE_BULLET_MESSENGER = "CHOOSE_BULLET_MESSENGER";
    private Context mContext;
    private IntentFilter mIntentFilter = null;
    private static final String BLUETOOTH_PACKAGE = "com.android.bluetooth";
    private static final String BLUETOOTH_LAUNCH_CLASS = "com.android.bluetooth.opp.BluetoothOppLauncherActivity";
    private ComponentName mLastChooseComponent;
    private boolean mChooseCustomBullet = false;
    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            ComponentName componentName = (ComponentName) intent.getParcelableExtra(Intent.EXTRA_CHOSEN_COMPONENT);
            String targetPackage = componentName != null ? componentName.getPackageName() : null;
            mLastChooseComponent = componentName;
            String action = intent.getAction();
            if (ACTION_CHOOSE_RESULT.equals(action) && !context.getPackageName().equals(targetPackage)) {
                String target = intent.getStringExtra(ShareItem.EXTRA_SHARE_ITEM_RETURN_TARGET);
                if (CHOOSE_BULLET_MESSENGER.equals(target)) {
                    mChooseCustomBullet = true;
                    BulletSmsUtils.popUpNeedInstallDialog(mContext.getApplicationContext(), true);
                }
                ArrayList<Integer> idlist = intent.getIntegerArrayListExtra("ids");
                if (idlist != null && Constants.WINDOW_READY) {
                    Tracker.onEvent(BubbleTrackerID.BUBBLE_SHARE_TO, "dest", targetPackage);
                    BubbleController.getInstance().hideBubbleListWithAnim();
                }
            } else if (ACTION_ATTACH_PICK_RESULT.equals(action) && Constants.WINDOW_READY) {
                StatusManager.setStatus(StatusManager.ADDING_ATTACHMENT, true);
                if (SmtPCUtils.isValidExtDisplayId(context)) {
                    BubbleController.getInstance().requestBubbleOptLayoutUpdateRegion();
                } else {
                    StatusManager.setStatus(StatusManager.FORCE_HIDE_WINDOW, true);
                    BubbleController.getInstance().hideBubbleListWithAnim();
                }
            } else if (ACTION_SAHRE_ATTACH_CHOOSE_RESULT.equals(action) && Constants.WINDOW_READY) {
                BubbleController.getInstance().hideBubbleListWithAnim();
            }
        }
    };

    public boolean isShareToBlueTooth() {
        boolean ret = false;
        if (mLastChooseComponent != null) {
            ret = mLastChooseComponent.getPackageName().equals(BLUETOOTH_PACKAGE) && mLastChooseComponent.getClassName().equals(BLUETOOTH_LAUNCH_CLASS);
        }
        return ret;
    }

    public boolean isChooseCustomBullet() {
        return mChooseCustomBullet;
    }

    public void resetChooseState() {
        mLastChooseComponent = null;
        mChooseCustomBullet = false;
    }

    public ComponentName getLastChooseComponent() {
        return mLastChooseComponent;
    }

    public ChooseResultHelper(Context context) {
        mContext = context;
        mIntentFilter = new IntentFilter();
        mIntentFilter.addAction(ACTION_CHOOSE_RESULT);
        mIntentFilter.addAction(ACTION_ATTACH_PICK_RESULT);
        mIntentFilter.addAction(ACTION_SAHRE_ATTACH_CHOOSE_RESULT);
        mContext.registerReceiver(mReceiver, mIntentFilter);
    }

    public void onActivityDestroy() {
        mContext.unregisterReceiver(mReceiver);
    }

    public static IntentSender getChooseIntentSender(Context context,List<BubbleItem> list) {
        Intent intent = new Intent(ACTION_CHOOSE_RESULT);
        ArrayList<Integer> idlist = new ArrayList<Integer>();
        for (BubbleItem item : list) {
            idlist.add(item.getId());
        }
        intent.putExtra("ids", idlist);
        PendingIntent broadcast = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        return broadcast.getIntentSender();
    }

    public static IntentSender getAttachPickIntentSender(Context context) {
        Intent intent = new Intent(ACTION_ATTACH_PICK_RESULT);
        PendingIntent broadcast = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        return broadcast.getIntentSender();
    }

    public static IntentSender getAttachChooseIntentSender(Context context) {
        Intent intent = new Intent(ACTION_SAHRE_ATTACH_CHOOSE_RESULT);
        PendingIntent broadcast = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        return broadcast.getIntentSender();
    }
}
