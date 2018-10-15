package com.smartisanos.sara.util;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.webkit.JsDialogHelper;
import android.webkit.JsResult;

import java.net.URL;


public final class DialogUtils {


    public static void showCustomJsDialog(Context activity, int type, String url, String message, final JsResult result) {

        switch (type) {
            case JsDialogHelper.ALERT:
                break;
            case JsDialogHelper.CONFIRM:
                showConfirmJsDialog(activity, url, message, result);
                break;
            case JsDialogHelper.PROMPT:
                break;
            case JsDialogHelper.UNLOAD:
                break;
            default:
                break;
        }
    }

    public static void showConfirmJsDialog(Context activity, String url, String message, final JsResult result) {
        if (null == activity) {
            return;
        }
        if (null == result) {
            return;
        }
        String title = "";
        try {
            URL u = new URL(url);
            title = activity.getString(com.android.internal.R.string.js_dialog_title, u.getHost());
        } catch (Exception e) {
            e.printStackTrace();
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(activity, AlertDialog.THEME_DEVICE_DEFAULT_LIGHT);
        builder.setTitle(title)
               .setMessage(message)
               .setOnCancelListener(new DialogInterface.OnCancelListener() {
                   @Override public void onCancel(DialogInterface dialog) {
                       result.cancel();
                   }
               })
               .setPositiveButton(com.android.internal.R.string.ok, new DialogInterface.OnClickListener() {
                   @Override public void onClick(DialogInterface dialog, int which) {
                       result.confirm();
                   }
               })
               .setNegativeButton(com.android.internal.R.string.cancel, new DialogInterface.OnClickListener() {
                   @Override public void onClick(DialogInterface dialog, int which) {
                       result.cancel();
                   }
               })
               .show();
    }
}
