package com.smartisanos.sara.voicecommand;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;

import com.smartisanos.sara.R;

public class WechatScanCmd extends VoiceCommand {
    private Context mContext;
    CharSequence mStartWehcatCmd;

    public WechatScanCmd(Context cxt, VoiceCommand next) {
        super(next);
        mContext = cxt;
        mStartWehcatCmd = mContext.getResources().getString(R.string.voice_cmd_start_wechat_scan);
    }

    @Override
    protected int onProcess(CharSequence cmd) {
        if (VoiceCommandUtils.matchCommand(mStartWehcatCmd, cmd)) {
            Intent intent = new Intent("com.tencent.mm.action.BIZSHORTCUT");
            intent.putExtra("LauncherUI.From.Scaner.Shortcut", true);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            VoiceCommandUtils.startActivityWithApp(mContext, intent, VoiceCommandUtils.PACKAGE_WECHAT);
            return FINISH_HANDLED;
        } else {
            return FORWARD;
        }
    }
}
