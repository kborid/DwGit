package com.smartisanos.sara;


import com.smartisanos.sara.storage.BubbleDataRepository;
import com.smartisanos.sara.util.LogUtils;
import com.smartisanos.sara.util.SaraConstant;
import com.smartisanos.sara.bubble.BubbleActivityHelper;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;

public class SaraBroadcastReceiver extends BroadcastReceiver {

    private static final String TAG = SaraBroadcastReceiver.class.getSimpleName();

    @Override
    public void onReceive(final Context context, final Intent intent) {
        if (intent == null) {
            return;
        }
        String action = intent.getAction();
        LogUtils.d(TAG, "action -> " + action);
        if (ConnectivityManager.CONNECTIVITY_ACTION.equals(action)) {
            BubbleDataRepository.checkOffLineVoiceInBackground(context);
        } else if (SaraConstant.ACTION_MENU_UP.equals(action)) {
            BubbleActionUpHelper.INSTANCE.onActionUp();
        } else if (SaraConstant.ACTION_BLUETOOTH_CALL.equals(action)) {
            BubbleActivityHelper.INSTANCE.callBubbleActivityFromBluetooth(context.getApplicationContext());
        }
    }
}
