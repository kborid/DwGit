package com.smartisanos.sara.util;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import android.content.ContentResolver;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.widget.ImageView;
import android.graphics.BitmapFactory;

import com.smartisanos.sara.R;

public class AppImageLoader {
    private static final String TAG = "VoiceAss.AppImgLoader";
    private final Map<ImageView, String> imageViews = Collections
            .synchronizedMap(new WeakHashMap<ImageView, String>());
    private final Map<String, Drawable> cache = Collections
            .synchronizedMap(new LinkedHashMap<String, Drawable>(10, 1.5f, false));
    private final ExecutorService executorService;
    private final Context mContext;
    public final float mIconSizeNormal;
    private static final int MAX_SIZE = 15;
    private static final int EVERY_DEL_NUMBER = 5;

    public static enum DRAWABLE_TYPE {
        LOCAL, DOWLOAD
    }

    public AppImageLoader(Context context) {
        //thread pool is fix 5 thread pool
        mContext = context;
        executorService = Executors.newFixedThreadPool(5);
        mIconSizeNormal = mContext.getResources().getDimension(R.dimen.list_icon_size_normal);
    }

    public void DisplayImage(String url, ImageView imageView, DRAWABLE_TYPE type) {
        imageViews.put(imageView, url);

        handleSizeToBig();

        //if the drawable is cached,set directly.others load by thread pool
        Drawable drawable = cache.get(url);
        if (drawable != null) {
            imageView.setImageDrawable(drawable);
        } else {
            PhotoToLoad p = new PhotoToLoad(url, imageView, type.toString());
            executorService.submit(new PhotosLoader(p));
        }
    }

    private Drawable getDrawable(String url) {
        Drawable d = null;
        try {
            // Load drawable through Resources, to get the source density information
            ContentResolver.OpenResourceIdResult r =
                    mContext.getContentResolver().getResourceId(Uri.parse(url));
            d = r.r.getDrawable(r.id);
            if (d.getIntrinsicWidth() > mIconSizeNormal && d instanceof BitmapDrawable) {
                Bitmap b = ((BitmapDrawable) d).getBitmap();
                d = new BitmapDrawable(null, resizeAppIcon(b));
            }
        } catch (Exception e) {
            LogUtils.d(TAG, "Unable to open content: " + url);
        }
        return d;
    }

    private Bitmap resizeAppIcon(Bitmap bmp) {
        int width = bmp.getWidth();
        int height = bmp.getHeight();
        float scale = mIconSizeNormal / width;
        Matrix matrix = new Matrix();
        matrix.postScale(scale, scale);
        Bitmap resizedBmp = Bitmap.createBitmap(bmp, 0, 0, width, height, matrix, true);
        return resizedBmp;
    }

    //Task for the queue
    private class PhotoToLoad {
        public String url;
        public ImageView imageView;
        public String iconType;

        public PhotoToLoad(String u, ImageView i, String type) {
            url = u;
            imageView = i;
            iconType = type;
        }
    }

    class PhotosLoader implements Runnable {
        PhotoToLoad photoToLoad;

        PhotosLoader(PhotoToLoad photoToLoad) {
            this.photoToLoad = photoToLoad;
        }

        @Override
        public void run() {
            if (imageViewReused(photoToLoad))
                return;

            Drawable drawable = null;
            if (photoToLoad.iconType.equals("DOWLOAD")) {
                drawable = downloadDrawable(photoToLoad.url);
            } else {
                drawable = getDrawable(photoToLoad.url);
            }
            //update the imageview through handler, this handler is running in UI thread
            cache.put(photoToLoad.url, drawable);
            DisplayStruct displayStruct = new DisplayStruct();
            displayStruct.drawable = drawable;
            displayStruct.imageView = photoToLoad.imageView;
            Message message = myHandler.obtainMessage(0, displayStruct);
            myHandler.sendMessage(message);
        }
    }

    //this method is used to avoid  image chaos
    boolean imageViewReused(PhotoToLoad photoToLoad) {
        String tag = imageViews.get(photoToLoad.imageView);
        if (tag == null || !tag.equals(photoToLoad.url)) {
            return true;
        }
        return false;
    }

    public static class DisplayStruct {
        public ImageView imageView;
        public Drawable drawable;
    }

    public void clearCache() {
        imageViews.clear();
    }

    final static Handler myHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            DisplayStruct struct = (DisplayStruct) msg.obj;
            try {
                struct.imageView.setImageDrawable(struct.drawable);
            } catch (Exception ex) {
                LogUtils.d(TAG, "exception happens when set drawable");
            }
        }
    };

    public Drawable get(String id) {
        try {
            if (!cache.containsKey(id))
                return null;
            return cache.get(id);
        } catch (NullPointerException ex) {
            return null;
        }
    }

    public void put(String id, Drawable drawable) {
        try {
            cache.put(id, drawable);
        } catch (Throwable th) {
            th.printStackTrace();
        }
    }

    private Drawable downloadDrawable(String uri) {
        Drawable d = null;
        try {
            URL url = new URL(uri);
            InputStream inputStream = url.openStream();
            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
            d = new BitmapDrawable(bitmap);
            inputStream.close();
            return d;
        } catch (MalformedURLException e) {
            e.printStackTrace();
            LogUtils.d(TAG, "downloadDrawable error MalformedURLException");
        } catch (IOException e) {
            e.printStackTrace();
            LogUtils.d(TAG, "downloadDrawable error IOException");
        }
        return d;
    }

    private void handleSizeToBig() {
        if (cache.size() < MAX_SIZE) {
            return;
        }

        for (int i = 0; i < EVERY_DEL_NUMBER; i++) {
            cache.remove(cache.entrySet().iterator().next().getKey());
        }
    }
}