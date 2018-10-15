package com.smartisanos.sara.voicecommand;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import com.smartisanos.sanbox.utils.SaraTracker;

import com.smartisanos.sara.R;
import com.smartisanos.ideapills.common.util.UIHandler;

public class DelayCommandReceiver extends BroadcastReceiver {
    public static final String TAG = "DelayCommandReceiver";
    private static final String ACTION_KEYGUARD_TO_DISMISS = "action_keyguard_to_dismiss";

    private static CharSequence sCommand = "";

    @Override
    public void onReceive(final Context context, Intent intent) {
        if (ACTION_KEYGUARD_TO_DISMISS.equals(intent.getAction())) {
            Log.d(TAG, "onReceive: delay command " + sCommand);
            UIHandler.post(new Runnable() {
                @Override
                public void run() {
                    processCommand(context);
                }
            });
        }
    }

    private void processCommand(Context context) {
        context = context.getApplicationContext();
        VoiceCommand vc = null;
        if (sCommand.equals(context.getString(R.string.quick_command_nav_home))) {
            vc = new NavigateHomeCmd(context, null);
        } else if (sCommand.equals(context.getString(R.string.quick_command_wechat_pay))) {
            vc = new WechatPayCmd(context, null);
        } else if (sCommand.equals(context.getString(R.string.quick_command_scan_wx))) {
            vc = new WechatScanCmd(context, null);
        } else if (sCommand.equals(context.getString(R.string.quick_command_scan_zfb))) {
            vc = new AliPayScanCmd(context, null);
        } else if (sCommand.equals(context.getString(R.string.quick_command_zfb_pay))) {
            vc = new AliPayCmd(context, null);
        }

        if (vc != null) {
            int result = vc.deliver(sCommand);
            if (result == VoiceCommand.FINISH_HANDLED) {
                SaraTracker.onEvent("A440004", "result", 1);
            }
            sCommand = "";
        }
    }

    public static void setDelayCommand(CharSequence command) {
        sCommand = VoiceCommandUtils.getStringOrEmpty(command);
    }
}
