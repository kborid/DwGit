package com.smartisanos.sara.util;

import java.net.URISyntaxException;
import java.util.List;
import com.smartisanos.sara.bubble.WebListener;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.net.http.SslError;
import android.os.PowerManager;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.webkit.DownloadListener;
import android.webkit.JsDialogHelper;
import android.webkit.JsResult;
import android.webkit.SslErrorHandler;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
public class MultiSdkAdapter  {
    private static final int REQUEST_SELECT_FILE = 100;
    private static ValueCallback<Uri[]> uploadMessage;
    private WebListener mListener;
    private  WebViewClient mWebViewClient = new WebViewClient() {
        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            super.onPageStarted(view, url, favicon);
            mListener.onPageStarted(url);
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            mListener.onPageFinishedBefore();
            super.onPageFinished(view, url);
            mListener.onPageFinishedAfter();
        }

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            boolean sucess = mListener.shouldOverrideUrlLoading(url);
            if (sucess) {
                return true;
            }
            return false;
        }

        @Override
        public void onReceivedError(WebView view, int errorCode,
                String description, String failingUrl) {
            boolean sucess = mListener.onReceivedError(errorCode);
            if (!sucess) {
                super.onReceivedError(view, errorCode, description, failingUrl);
            }
        }

        @Override
        public void onReceivedSslError(WebView view, SslErrorHandler handler,
                SslError error) {
            handler.proceed();
        }
    };

    private WebChromeClient mWebChromeClient = new WebChromeClient() {
        @Override
        public void onProgressChanged(WebView view, int newProgress) {
            mListener.onProgressChanged(newProgress);
        }

        @Override
        public void onReceivedTitle(WebView view, String title) {
            super.onReceivedTitle(view, title);
            mListener.onReceivedTitle(title);
        }

        @Override
        public boolean onShowFileChooser(WebView webView,
                ValueCallback<Uri[]> filePathCallback,
                FileChooserParams fileChooserParams) {
              return mListener.onShowFileChooser(fileChooserParams.createIntent(),filePathCallback);
        }

        @Override
        public boolean onJsConfirm(WebView view, String url, String message, JsResult result) {
            mListener.onShowDialog(JsDialogHelper.CONFIRM, url, message, result);
            return true;
        }
    };

    private DownloadListener mDownloadListener = new DownloadListener() {
        @Override
        public void onDownloadStart(String url, String userAgent,
                String contentDisposition, String mimetype, long contentLength) {
            mListener.onDownloadStart(url);
        }
    };

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
          mListener.onReceiveValue(WebChromeClient.FileChooserParams.parseResult(resultCode, data));
    }

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
    public void setWebSetting(WebSettings settings) {
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE);
    }
    public Uri[] parseIntent(int resultCode, Intent data){
        return WebChromeClient.FileChooserParams.parseResult(resultCode, data);
    }

    public static void wakeUp(PowerManager target, long time, String reason) {
        target.wakeUp(time);
    }

    public static void cancelDragAndDrop(View view) {

    }
}
