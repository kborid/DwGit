package com.smartisanos.sara.setting;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.WindowManager;
import android.widget.Toast;

import com.smartisanos.ideapills.common.sync.SyncUtil;
import com.smartisanos.sara.BaseActivity;
import com.smartisanos.sara.storage.BubbleDataRepository;
import com.smartisanos.sara.util.SaraUtils;
import com.smartisanos.sara.util.ToastUtil;
import com.smartisanos.sara.R;

public class SendInvitationActivity extends BaseActivity {

    public static void launchAsActivity(Context context) {
        Intent intent = new Intent(context, SendInvitationActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
        intent.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
        SaraUtils.startActivity(context, intent);
    }

    public static Dialog launchAsDialog(Context context) {
        return buildInvitationDialog(context);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Dialog dialog = buildInvitationDialog(this);
        dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                finish();
            }
        });
        dialog.show();
    }

    public static Dialog buildInvitationDialog(final Context context) {
        return SyncUtil.buildInvitationDialog(context, new SyncUtil.OnInvitationClickListener() {
            @Override
            public void onInvitationClick(String phoneText) {
                if (TextUtils.isEmpty(phoneText)) {
                    ToastUtil.showToast(context, R.string.sync_phone_email_empty_tip, Toast.LENGTH_SHORT);
                    return;
                }
                if (!SyncUtil.isPhoneNumber(phoneText) && !SyncUtil.isEmail(phoneText)) {
                    ToastUtil.showToast(context, R.string.sync_phone_email_illegal_tip, Toast.LENGTH_SHORT);
                    return;
                }
                BubbleDataRepository.sendShareInvitation(context, phoneText);
            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        finish();
    }
}
