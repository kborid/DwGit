package com.smartisanos.sara.bubble;

import android.content.Intent;
import android.net.Uri;
import android.webkit.JsResult;
import android.webkit.ValueCallback;
public  interface WebListener {
    public void onPageStarted(String url);
    public void onPageFinishedBefore();
    public void onPageFinishedAfter();
    public boolean shouldOverrideUrlLoading(String url);
    public boolean onReceivedError(int errorcode);
    public void onProgressChanged(int progress);
    public void onReceivedTitle(String url);
    public boolean onShowFileChooser(Intent intent,ValueCallback<Uri[]> callback);
    public void onDownloadStart(String url);
    public void onReceiveValue(Uri[] uris);
    void onShowDialog(int type, String url, String message, JsResult result);
}