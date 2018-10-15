package com.smartisanos.sara.voicecommand;

import android.content.Context;
import android.content.Intent;

import com.smartisanos.sara.R;

public class AliPayCmd extends VoiceCommand {
    private Context mContext;
    CharSequence mAliPayCmd;

    public AliPayCmd(Context cxt, VoiceCommand next) {
        super(next);
        mContext = cxt;
        mAliPayCmd = mContext.getResources().getString(R.string.quick_command_zfb_pay);
    }

    @Override
    protected int onProcess(CharSequence cmd) {
        if (VoiceCommandUtils.matchCommand(mAliPayCmd, cmd)) {
            Intent intent = new Intent("com.smartisanos.action.QuickPayment");
            intent.setPackage("com.smartisanos.wallpaperprovider");
            intent.putExtra("extra_payment", "com.eg.android.AlipayGphone");
            if (VoiceCommandUtils.checkAppExist(mContext, VoiceCommandUtils.PACKAGE_ALIPAY)) {
                mContext.startService(intent);
            } else {
                VoiceCommandUtils.showInstallDialogDelay(mContext.getApplicationContext(), VoiceCommandUtils.PACKAGE_ALIPAY);
            }
            return FINISH_HANDLED;
        } else {
            return FORWARD;
        }
    }
}
