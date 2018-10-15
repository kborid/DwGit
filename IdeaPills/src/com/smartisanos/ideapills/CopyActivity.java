package com.smartisanos.ideapills;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.WindowManager;
import android.widget.Toast;

import com.smartisanos.ideapills.util.BubbleTrackerID;
import com.smartisanos.ideapills.util.GlobalBubbleUtils;
import com.smartisanos.ideapills.util.LOG;
import com.smartisanos.ideapills.util.Tracker;
import com.smartisanos.ideapills.util.Utils;

import smartisanos.api.ToastSmt;

public class CopyActivity extends Activity{
    LOG log = LOG.getInstance(CopyActivity.class);
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();
        CharSequence charSequence = intent.getCharSequenceExtra(Intent.EXTRA_TEXT);
        Tracker.onEvent(BubbleTrackerID.BUBBLE_OPT_COPY, "source", 1);
        Utils.copyText(getApplicationContext(), charSequence, true);
        ToastSmt.getInstance().makeText(getApplicationContext(), getApplicationContext().getResources().getString(R.string.text_copied), Toast.LENGTH_SHORT, WindowManager.LayoutParams.TYPE_SYSTEM_ERROR).show();
        finish();
    }
}
