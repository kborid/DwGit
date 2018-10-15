package com.smartisanos.ideapills;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ComponentName;
import android.os.Bundle;

import com.smartisanos.ideapills.util.Utils;

public class DialogActivity extends Activity {

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();
        String title = intent.getStringExtra("title");
        String message = intent.getStringExtra("message");
        String posiText = intent.getStringExtra("posiText");
        final String dialogType = intent.getStringExtra("dialogType");
        new AlertDialog.Builder(this, AlertDialog.THEME_DEVICE_DEFAULT_LIGHT)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(posiText, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (dialogType.equals(Utils.DIALOG_TYPE.OPEN_IDEA.name())) {
                            Intent intent = new Intent();
                            intent.setComponent(new ComponentName("com.smartisanos.sara",
                                    "com.smartisanos.sara.bubble.SettingActivity"));
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            DialogActivity.this.startActivity(intent);
                            DialogActivity.this.finish();
                        }
                    }
                })
                .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                        DialogActivity.this.finish();
                    }
                })
                .create().show();
    }
}
