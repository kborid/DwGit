package com.smartisanos.sara.util;

import android.content.Intent;
import android.net.Uri;
import android.os.PowerManager;
import android.view.View;
import android.webkit.DownloadListener;
import android.webkit.WebChromeClient;
import android.webkit.WebViewClient;
import android.webkit.WebSettings;
import com.smartisanos.sara.bubble.WebListener;
public class MultiSdkAdapter{
    private  WebViewClient mWebViewClient;
    private  WebChromeClient mWebChromeClient;
    private  DownloadListener mDownloadListener;
    protected WebListener mListener;
    protected WebSettings mWebSettings;
    public  WebViewClient getWebViewClient() {
        return mWebViewClient;
    }
    public  WebChromeClient getWebChromeClient() {
        return mWebChromeClient;
    }

    public  DownloadListener getDownloadListener() {
        return mDownloadListener;
    }

    public void setWebListener(WebListener lis) {
        mListener = lis;
    }
    public void setWebSetting(WebSettings setting) {
        mWebSettings = setting;
    }
    public Uri[] parseIntent(int resultCode, Intent data){
        return null;
    }

    public static void wakeUp(PowerManager target, long time, String reason) {
        target.wakeUp(time, reason);
    }

    public static void cancelDragAndDrop(View view) {
        if (view != null) {
            view.cancelDragAndDrop();
        }
    }
}
