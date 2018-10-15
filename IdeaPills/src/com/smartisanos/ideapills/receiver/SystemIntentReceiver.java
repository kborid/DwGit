package com.smartisanos.ideapills.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.smartisanos.ideapills.Constants;
import com.smartisanos.ideapills.PhoneState;
import com.smartisanos.ideapills.sync.SyncProcessor;
import com.smartisanos.ideapills.util.GlobalBubbleManager;
import com.smartisanos.ideapills.util.LOG;
import com.smartisanos.ideapills.util.StatusManager;
import com.smartisanos.ideapills.common.util.UIHandler;

public class SystemIntentReceiver extends BroadcastReceiver {
    private static final LOG log = LOG.getInstance(SystemIntentReceiver.class);

    private static final String ACTION_KEYGUARD_ON = "action_keyguard_on";
    private static final String ACTION_KEYGUARD_TO_DISMISS = "action_keyguard_to_dismiss";
    private static final String ACTION_SMARTISAN_CLOUD_ACCOUNT = "com.smartisan.cloud.account";

    @Override
    public void onReceive(final Context context, Intent intent) {
        final String action = intent.getAction();
        if (ACTION_SMARTISAN_CLOUD_ACCOUNT.equals(action)) {
            boolean isLogout = intent.getBooleanExtra("smartisan_cloud_sync_account_remove", true);
            if (!isLogout) {
                SyncProcessor.syncLogin();
            }
            return;
        }
        if (!Constants.WINDOW_READY) {
            log.error("Constants.WINDOW_READY false ! abandon action "+ action);
            return;
        }
        UIHandler.post(new Runnable() {
            @Override
            public void run() {
                if (Intent.ACTION_BOOT_COMPLETED.equals(action)) {
                    PhoneState state = new PhoneState(context);
                    state.unRegisterTelephonyState();
                    state.registerTelephonyState();
                } else if (ACTION_KEYGUARD_ON.equals(action)) {
                    GlobalBubbleManager.getInstance().handleActionKeyguardOn();
                } else if (ACTION_KEYGUARD_TO_DISMISS.equals(action)) {
                    GlobalBubbleManager.getInstance().handleActionKeyguardDismiss();
                }
            }
        });
    }
}
