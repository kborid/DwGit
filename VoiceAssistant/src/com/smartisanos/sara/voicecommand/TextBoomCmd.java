package com.smartisanos.sara.voicecommand;

import android.content.Context;
import android.content.Intent;
import android.graphics.Rect;
import android.provider.Settings;
import android.text.TextUtils;

import com.smartisanos.sara.R;
import com.smartisanos.sara.util.LogUtils;
import com.smartisanos.sara.util.ToastUtil;
import com.smartisanos.ideapills.common.util.UIHandler;

public class TextBoomCmd extends VoiceCommand {
    public static final String TAG = "TextBoomCmd";
    private static final String ACTION_BOOM_TEXT = "smartisanos.intent.action.BOOM_TEXT";
    private Context mContext;
    CharSequence mTextBoomCmd;

    public TextBoomCmd(Context cxt, VoiceCommand next) {
        super(next);
        mContext = cxt;
        mTextBoomCmd = mContext.getResources().getString(R.string.quick_command_boom);
    }

    @Override
    protected int onProcess(CharSequence cmd) {
        final VoiceCommandEnvironment env = VoiceCommandEnvironment.getInstance();
        if (VoiceCommandUtils.matchCommand(mTextBoomCmd, cmd) && env != null) {
            LogUtils.d(TAG, "onProcess boom cmd!");

            final Rect r = env.getCurrentFocusRect();
            if (r == null || r.isEmpty()) {
                return FINISH_NOT_HANDLED;
            }

            if (!VoiceCommandUtils.checkAppExist(mContext, VoiceCommandUtils.PACKAGE_BIGBANG)) {
                VoiceCommandUtils.showInstallDialogDelay(mContext.getApplicationContext(),
                        VoiceCommandUtils.PACKAGE_BIGBANG);
                return FINISH_HANDLED;
            }

            boolean enabled = Settings.Global.getInt(mContext.getContentResolver(),
                    smartisanos.api.SettingsSmt.Global.TEXT_BOOM, 1) == 1;

            if (!enabled) {
                ToastUtil.showToast(R.string.voice_command_func_disable);
            } else {
                if (TextUtils.equals(env.getCurrentWindowTitle(),
                        VoiceCommandUtils.getActivityLabel(mContext, new Intent(ACTION_BOOM_TEXT)))) {
                    ToastUtil.showToast(R.string.voice_command_result_has_shown);
                    return FINISH_HANDLED;
                }

                CharSequence text = env.getCurrentFocusText();
                if (!TextUtils.isEmpty(text)) {
                    goTextBoom(text);
                } else {
                    LogUtils.d(TAG, "text is empty, go ocr boom!");
                    UIHandler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            Intent intent = new Intent("smartisanos.intent.action.BOOM_ACCESSBILITY");
                            String size = r.flattenToString().replaceAll(" ", ",");
                            intent.putExtra("view_size", size);
                            intent.putExtra("process_hint_message", mContext.getString(R.string.voice_command_process_hint));
                            intent.putExtra("boom", true);
                            intent.putExtra("caller_pkg", mContext.getPackageName());
                            intent.setFlags(Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
                                    | Intent.FLAG_ACTIVITY_NEW_TASK
                                    | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            VoiceCommandUtils.startActivityCommon(mContext, intent);
                        }
                    }, 1000);
                }
            }

            return FINISH_HANDLED;
        } else {
            return FORWARD;
        }
    }

    private void goTextBoom(CharSequence content) {
        Intent intent = new Intent(ACTION_BOOM_TEXT);
        intent.putExtra(Intent.EXTRA_TEXT, content.toString());
        intent.putExtra("show_all_text", true);
        intent.putExtra("caller_pkg", mContext.getPackageName());
        intent.setFlags(Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
                | Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        VoiceCommandUtils.startActivityCommon(mContext, intent);
    }
}
