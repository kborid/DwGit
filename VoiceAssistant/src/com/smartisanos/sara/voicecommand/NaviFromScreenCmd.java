package com.smartisanos.sara.voicecommand;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.text.TextUtils;

import com.smartisanos.sara.R;
import com.smartisanos.sara.util.ToastUtil;
import com.smartisanos.sanbox.utils.SaraTracker;

public class NaviFromScreenCmd extends VoiceCommand implements SmartWordCallback {
    public static final String TAG = "NaviFromScreenCmd";
    private Context mContext;
    CharSequence mNavigateCmd;

    public NaviFromScreenCmd(Context cxt, VoiceCommand next) {
        super(next);
        mContext = cxt;
        mNavigateCmd = cxt.getResources().getString(R.string.quick_command_nav_addr);
    }

    protected int onProcess(CharSequence cmd) {
        VoiceCommandEnvironment env = VoiceCommandEnvironment.getInstance();
        if (VoiceCommandUtils.matchCommand(mNavigateCmd, cmd) && env != null) {
            CharSequence text = env.getCurrentFocusText();
            if (!TextUtils.isEmpty(text)) {
                IntelligentWords.postRequest(mContext.getApplicationContext(), text.toString(), this, null);
                return FINISH_HANDLED;
            } else {
                return FINISH_NOT_HANDLED;
            }
        }
        return FORWARD;
    }

    @Override
    public void sendTextToClient(CharSequence text) {
        if (TextUtils.isEmpty(text)) {
            ToastUtil.showToast(R.string.voice_command_error_no_focus_item);
            SaraTracker.onEvent("A440004", "result", 0);
            return;
        }

        Uri uri = Uri.parse("androidamap://keywordNavi?sourceApplication=softname&keyword=" + text + "&style=2");
        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
        intent.setPackage("com.autonavi.minimap");
        VoiceCommandUtils.startActivityWithApp(mContext, intent, VoiceCommandUtils.PACKAGE_GAODE);
    }
}
