package com.smartisanos.ideapills.remind;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;

public class RemindAlarmSettingActivity extends Activity implements DialogInterface.OnDismissListener {

    public static final String ACTION_CLOSE_REMIND = "com.smartisan.ideapills.CLOSE_REMIND";
    private RemindAlarmSettingDialog mDialog;

    private BroadcastReceiver mBroadcaseReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (ACTION_CLOSE_REMIND.equals(action)) {
                finish();
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mDialog = RemindAlarmSettingDialog.newInstance(getIntent());
        mDialog.show(getFragmentManager(), "RemindAlarmSettingDialog");

        IntentFilter mIntentFilter = new IntentFilter();
        mIntentFilter.addAction(ACTION_CLOSE_REMIND);
        registerReceiver(mBroadcaseReceiver, mIntentFilter);

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mBroadcaseReceiver);
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        Intent data = mDialog.getIntent();
        if (data != null && data.getIntExtra(RemindAlarmSettingDialog.EXTRA_RESULT_CODE, RESULT_CANCELED) == RESULT_OK) {
            setResult(RESULT_OK, data);
        } else {
            setResult(RESULT_CANCELED);
        }
        finish();
    }
}
