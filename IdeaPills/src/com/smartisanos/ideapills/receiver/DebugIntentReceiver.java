package com.smartisanos.ideapills.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.smartisanos.ideapills.BubbleController;
import com.smartisanos.ideapills.Constants;
import com.smartisanos.ideapills.util.LOG;
import com.smartisanos.ideapills.util.StatusManager;

public class DebugIntentReceiver extends BroadcastReceiver {
    private static final LOG log = LOG.getInstance(DebugIntentReceiver.class);

    public static final String ACTION = "com.smartisanos.ideapills.debug";
    @Override
    public void onReceive(final Context context, Intent intent) {
        if (ACTION.equals(intent.getAction())) {
            StatusManager.dumpStatus();
            Constants.dumpStatus();
            BubbleController.getInstance().dump();
        }
    }
}
