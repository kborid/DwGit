package com.smartisanos.sara;

import com.smartisanos.sara.util.LogUtils;
import com.smartisanos.sara.util.RecognizeHelper;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.telephony.TelephonyManager;

public class OpenListenerServiceReceiver extends BroadcastReceiver {
    private String TAG = "OpenListenerServiceReceiver";
    @Override
    public void onReceive(Context context, Intent intent) {
        LogUtils.e(TAG, "onReceive");

        if (intent == null) {
            return;
        }
        if(TelephonyManager.ACTION_PHONE_STATE_CHANGED.equals(intent.getAction())) {
            String newPhoneState = intent.hasExtra(TelephonyManager.EXTRA_STATE)
                    ? intent.getStringExtra(TelephonyManager.EXTRA_STATE)
                            : null;
            if(newPhoneState != null && !newPhoneState.equals(TelephonyManager.EXTRA_STATE_IDLE)) {
                RecognizeHelper.getInstance().setAllowedToRecognize(false);
            } else {
                RecognizeHelper.getInstance().setAllowedToRecognize(true);
            }
            RecognizeHelper.getInstance().setPhoneStatus(newPhoneState);
            RecognizeHelper.getInstance().setPhoneStatusChangeTime(System.currentTimeMillis());
        }
    }
}
