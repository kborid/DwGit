package com.smartisanos.sara.voicecommand;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import com.smartisanos.sara.R;

public class AliPayScanCmd extends VoiceCommand {
    private Context mContext;
    CharSequence mAliPayScanCmd;

    public AliPayScanCmd(Context cxt, VoiceCommand next) {
        super(next);
        mContext = cxt;
        mAliPayScanCmd = mContext.getResources().getString(R.string.quick_command_scan_zfb);
    }

    @Override
    protected int onProcess(CharSequence cmd) {
        if (VoiceCommandUtils.matchCommand(mAliPayScanCmd, cmd)) {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("alipayqr://platformapi/startapp?saId=10000007"));
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            VoiceCommandUtils.startActivityWithApp(mContext, intent, VoiceCommandUtils.PACKAGE_ALIPAY);
            return FINISH_HANDLED;
        } else {
            return FORWARD;
        }
    }
}
