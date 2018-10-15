package com.smartisanos.ideapills.common.util;

import android.content.Intent;
import android.content.IntentSender;
import android.net.Uri;
import android.view.WindowManager;

import smartisanos.api.IntentSmt;

public class ShareUtils {

    public static Intent createShareAttachIntent(Uri uri, String type, IntentSender intentSender) {
        Intent targetIntent = new Intent(Intent.ACTION_SEND);
        targetIntent.putExtra(Intent.EXTRA_STREAM, uri);
        if ("text/plain".equals(type)) {
            // typically, most Apps expect a text string along with "text/plain" in the Intent.
            // but we just setData with uri, so use application/*, only do this for share
            type = "application/*";
        }
        targetIntent.setType(type);
        Intent resultIntent;
        if (null != intentSender
                && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP_MR1) {
            resultIntent = Intent.createChooser(targetIntent, null, intentSender);
        } else {
            resultIntent = Intent.createChooser(targetIntent, null);
        }
        resultIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        resultIntent.putExtra("window_type", WindowManager.LayoutParams.TYPE_SYSTEM_DIALOG);
        int[] anims = {0, smartisanos.R.anim.slide_down_out};
        resultIntent.putExtra(IntentSmt.EXTRA_SMARTISAN_ANIM_RESOURCE_ID, anims);
        return resultIntent;
    }
}
