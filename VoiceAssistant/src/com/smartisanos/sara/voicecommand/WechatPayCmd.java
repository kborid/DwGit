package com.smartisanos.sara.voicecommand;

import android.content.Context;
import android.content.Intent;

import com.smartisanos.sara.R;

public class WechatPayCmd extends VoiceCommand {
    private Context mContext;
    CharSequence mWehcatPayCmd;

    public WechatPayCmd(Context cxt, VoiceCommand next) {
        super(next);
        mContext = cxt;
        mWehcatPayCmd = mContext.getResources().getString(R.string.quick_command_wechat_pay);
    }

    @Override
    protected int onProcess(CharSequence cmd) {
        if (VoiceCommandUtils.matchCommand(mWehcatPayCmd, cmd)) {
            Intent intent = new Intent("com.smartisanos.action.QuickPayment");
            intent.setPackage("com.smartisanos.wallpaperprovider");
            intent.putExtra("extra_payment", "com.tencent.mm");
            if (VoiceCommandUtils.checkAppExist(mContext, VoiceCommandUtils.PACKAGE_WECHAT)) {
                mContext.startService(intent);
            } else {
                VoiceCommandUtils.showInstallDialogDelay(mContext, VoiceCommandUtils.PACKAGE_WECHAT);
            }
            return FINISH_HANDLED;
        } else {
            return FORWARD;
        }
    }
}
