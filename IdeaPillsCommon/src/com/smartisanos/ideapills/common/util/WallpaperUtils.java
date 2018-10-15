package com.smartisanos.ideapills.common.util;

import android.app.Activity;
import android.content.ContentResolver;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.MediaStore;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.View;

import com.smartisanos.ideapills.common.R;
import com.smartisanos.ideapills.common.util.MutiTaskHandler;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import smartisanos.api.SettingsSmt;

public class WallpaperUtils {
    private static final String DESKTOP_WALLPAPER_URI = "desktop_wallpaper_uri";//SettingsSmt.DESKTOP_WALLPAPER_URI

    public static void gaussianBlurWallpaper(Activity context, View view) {
        if (context == null || view == null) {
            return;
        }
        MutiTaskHandler.post(new Runnable() {
            @Override
            public void run() {
                String wallpaperUri = Settings.Global.getString(context.getContentResolver(), DESKTOP_WALLPAPER_URI);
                Bitmap bitmap = null;
                if (!TextUtils.isEmpty(wallpaperUri)) {
                    Uri uri = Uri.parse(wallpaperUri);
                    if (uri != null) {
                        String scheme = uri.getScheme();
                        if (ContentResolver.SCHEME_FILE.equals(scheme) ||
                                ContentResolver.SCHEME_ANDROID_RESOURCE.equals(scheme)) {
                            try {
                                bitmap = MediaStore.Images.Media.getBitmap(context.getContentResolver(), uri);
                            } catch (FileNotFoundException e) {
                                e.printStackTrace();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        } else if (ContentResolver.SCHEME_CONTENT.equals(scheme)) {
                            InputStream is = null;
                            try {
                                is = context.getContentResolver().openInputStream(uri);
                                Drawable d = Drawable.createFromResourceStream(null, null, is, null, null);
                                bitmap = ((BitmapDrawable) d).getBitmap();
                            } catch (Exception e) {
                            } finally {
                                try {
                                    if (is != null) is.close();
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                    }
                }
                if (bitmap != null) {
                    final BlurTask bt = new BlurTask(bitmap, view, context);
                    bt.setMaskResource(R.drawable.black_bg_mask);
                    bt.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                }
            }

        });
    }
}
