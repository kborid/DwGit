package com.smartisanos.ideapills.common.sync;

import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import com.smartisanos.ideapills.common.R;

public class SyncUtil {

    public static final String PARAM_KEY_SHOW_TOAST = "showToast";
    public static final String PARAM_KEY_PHONE_NUM = "phoneNum";

    public static final String RESULT_KEY_CAN_SHARE = "canShare";

    public static final String SHOW_INVITATION_DIALOG_FLAG = "showInvitationDialogFlag";
    public static final String SHOW_LOGIN_SYNC_DIALOG_FLAG = "showLoginSyncDialogFlag";
    public static final String SHOW_LOGIN_DIALOG_FLAG = "showLoginDialogFlag";

    public static Intent getCloudAccountLoginIntent(String backText) {
        Intent intent = new Intent();
        intent.setComponent(new ComponentName("com.smartisanos.cloudsync", "com.smartisanos.accounts.AccountsActivity"));
        intent.setAction(Intent.ACTION_MAIN);
        intent.putExtra("back_text", backText);
        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        intent.putExtra("smartisan_origin_app_tag", "from_ideapill");
        return intent;
    }

    public static AlertDialog buildLoginDialog(Context context, boolean isLogin,
                                               DialogInterface.OnClickListener onPositiveClickListener) {
        AlertDialog loginDialog = new AlertDialog.Builder(context, AlertDialog.THEME_DEVICE_DEFAULT_LIGHT)
                .setPositiveButton(R.string.confirm_reminder, onPositiveClickListener)
                .setNegativeButton(R.string.bubble_cancel, null)
                .setCancelable(true)
                .create();
        loginDialog.setTitle(R.string.bubble_notice);
        loginDialog.setMessage(context.getString(isLogin ? R.string.sync_no_login_dialog_content :
                R.string.sync_unenable_dialog_content));

        return loginDialog;
    }

    public static AlertDialog buildInvitationDialog(Context context,
                                                    final OnInvitationClickListener onInvitationClickListener) {
        final View content = LayoutInflater.from(context).inflate(
                R.layout.sync_invitation_dialog_content, null, false);
        final EditText phoneTextView = (EditText) content.findViewById(R.id.phone_email);
        AlertDialog invitationDialog = new AlertDialog.Builder(context, AlertDialog.THEME_DEVICE_DEFAULT_LIGHT)
                .setTitle(R.string.dlg_title_notice)
                .setView(content)
                .setPositiveButton(R.string.sync_send_invitation_dialog_send, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        final String phoneNumber = phoneTextView.getText().toString();
                        if (onInvitationClickListener != null) {
                            onInvitationClickListener.onInvitationClick(phoneNumber);
                        }
                    }
                })
                .setNegativeButton(R.string.bubble_cancel, null)
                .setCancelable(true)
                .create();
        invitationDialog.getWindow().getAttributes().gravity = Gravity.BOTTOM;
        invitationDialog.getWindow().getAttributes().verticalMargin = 0.45f;
        invitationDialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE |
                WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        return invitationDialog;
    }

    public static boolean isPhoneNumber(String num) {
        if (TextUtils.isEmpty(num)) {
            return false;
        }
        Pattern pattern = Pattern.compile("^(1[3-5,7-9])\\d{9}$");
        Matcher matcher = pattern.matcher(num);
        return matcher.matches();
    }

    public static boolean isEmail(String email) {
        if (TextUtils.isEmpty(email)) {
            return false;
        }
        Pattern pattern = Pattern.compile("\\w+([-+.]\\w+)*@\\w+([-.]\\w+)*\\.\\w+([-.]\\w+)*");
        Matcher matcher = pattern.matcher(email);
        return matcher.matches();
    }

    public interface OnInvitationClickListener {
        void onInvitationClick(String phoneText);
    }
}
