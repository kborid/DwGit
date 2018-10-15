
package com.smartisanos.sara.transaction;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.smartisanos.sara.util.SaraConstant;

public class SaraStatusReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (SaraConstant.ACTION_MUSIC_ALBUME_ART_CHANGE.equals(action)) {
            intent.setClass(context, SaraStatusService.class);
            context.startService(intent);
        }
    }
}
