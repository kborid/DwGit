package com.smartisanos.sara.voicecommand;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;

/**
 * when enable blind mode, alert dialog with no activity context
 * can not be read after trident(8.0?).
 * so use a proxy activity to show this dialog.
 */
public class DialogProxyActivity extends Activity {
    public static final String EXTRA_TITLE = "extra_title";
    public static final String EXTRA_MESSAGE = "extra_message";
    public static final String EXTRA_OK = "extra_ok";
    public static final String EXTRA_CANCEL = "extra_cancel";
    public static final String EXTRA_TARGET = "extra_target";

    public static void showDialog(Context context, String title, String messege, String ok, String cancel, Intent target) {
        Intent intent = new Intent(context, DialogProxyActivity.class);
        intent.putExtra(EXTRA_TITLE, title);
        intent.putExtra(EXTRA_MESSAGE, messege);
        intent.putExtra(EXTRA_OK, ok);
        intent.putExtra(EXTRA_CANCEL, cancel);
        intent.putExtra(EXTRA_TARGET, target);

        VoiceCommandUtils.startActivityCommon(context, intent);
    }

    private String mTitle;
    private String mMessage;
    private String mOk;
    private String mCancel;
    private Intent mTarget;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle(""); // dont read activity title.

        Intent data = getIntent();
        if (data == null) {
            finish();
            return;
        }

        mTitle = data.getStringExtra(EXTRA_TITLE);
        mMessage = data.getStringExtra(EXTRA_MESSAGE);
        mOk = data.getStringExtra(EXTRA_OK);
        mCancel = data.getStringExtra(EXTRA_CANCEL);
        mTarget = data.getParcelableExtra(EXTRA_TARGET);

        showDialog();
    }

    private void showDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this, AlertDialog.THEME_DEVICE_DEFAULT_LIGHT);
        builder.setTitle(mTitle)
                .setMessage(mMessage)
                .setCancelable(false)
                .setPositiveButton(mOk, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    VoiceCommandUtils.startActivityCommon(DialogProxyActivity.this, mTarget);
                }
            });

        if (!TextUtils.isEmpty(mCancel)) {
            builder.setNegativeButton(mCancel, null);
        }

        builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                finish();
            }
        });

        AlertDialog dialog = builder.create();
        dialog.show();
    }
}
