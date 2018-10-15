package com.smartisanos.sara.voicecommand;

import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;
import android.util.Log;

import com.smartisanos.sara.R;
import com.smartisanos.sara.util.SharePrefUtil;
import com.smartisanos.ideapills.common.util.UIHandler;

public class NavigateHomeCmd extends VoiceCommand {
    public static final String TAG = "NavigateHomeCmd";

    private Context mContext;
    CharSequence mNavigateHomeCmd;

    public NavigateHomeCmd(Context cxt, VoiceCommand next) {
        super(next);
        mContext = cxt;
        mNavigateHomeCmd = mContext.getResources().getString(R.string.quick_command_nav_home);
    }

    @Override
    protected int onProcess(CharSequence cmd) {
        if (VoiceCommandUtils.matchCommand(mNavigateHomeCmd, cmd)) {

            AddressInfo address = AddressInfo.fromString(SharePrefUtil.getHomeAddr(mContext));
            Log.d(TAG, "onProcess: address=" + address);

            if (TextUtils.isEmpty(address.name)) {
                UIHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        showConfirmDialog();
                    }
                }, 500);
            } else {
                VoiceCommandUtils.goNavigateHome(mContext, address);
            }
            return FINISH_HANDLED;
        } else {
            return FORWARD;
        }
    }

    private void showConfirmDialog() {
        Context ctx = mContext;
        String title = ctx.getString(R.string.button_set_home_title);
        String message = ctx.getString(R.string.button_set_home_content);
        String ok = ctx.getString(R.string.button_setting);
        String cancel = ctx.getString(R.string.cancel);
        Intent target = new Intent(mContext, HomeInfoActivity.class);
        target.putExtra(HomeInfoActivity.EXTRA_FINISH_WITH_NAVIGATE, true);
        target.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        DialogProxyActivity.showDialog(ctx, title, message, ok, cancel, target);
    }
}
