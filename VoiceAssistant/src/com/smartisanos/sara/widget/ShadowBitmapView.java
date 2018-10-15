package com.smartisanos.sara.widget;

import android.annotation.DrawableRes;
import android.annotation.Nullable;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.util.AttributeSet;
import android.widget.ImageView;

import com.smartisanos.ideapills.common.model.ShareItem;
import com.smartisanos.sara.R;
import com.smartisanos.ideapills.common.util.BitmapUtils;
import com.smartisanos.sara.util.HolographicOutlineHelper;

import java.lang.ref.WeakReference;

public class ShadowBitmapView extends ImageView {
    private Bitmap mShadowBitmap;
    private boolean shouldGenerateBitmap;
    private int mIconSize = 0;
    private Rect mTempRect = new Rect();

    public ShadowBitmapView(Context context) {
        this(context, null);
    }

    public ShadowBitmapView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ShadowBitmapView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mIconSize = getResources().getDimensionPixelSize(R.dimen.drawer_item_width);
    }

    private void generateShadowBitmapInternal() {
        Drawable drawable = getDrawable();
        if (drawable != null) {
            mShadowBitmap = generateShadowBitmap(drawable);
        }
        shouldGenerateBitmap = false;
    }

    static Bitmap generateShadowBitmap(Drawable drawable) {
        Bitmap output = null;
        if (drawable != null) {
            try {
                output = HolographicOutlineHelper.getIconWithDarkShadow(drawable);
            } catch (OutOfMemoryError e) {
                // ignore
            }
        }
        return output;
    }

    public void setImageShadowBitmap(Bitmap shadowBitmap) {
        if (shadowBitmap != null) {
            mShadowBitmap = shadowBitmap;
            invalidate();
            shouldGenerateBitmap = false;
        }
    }

    public void setImageDrawableAsyncLoadShadow(final ShareItem shareItem) {
        new AsyncLoadShareDrawableTask(this).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, shareItem);
    }

    public void setImageDrawableAsyncLoadShadow(final Drawable drawable) {
        if (getDrawable() == drawable) {
            return;
        }
        super.setImageDrawable(drawable);
        shouldGenerateBitmap = false;
        if (drawable != null) {
            new AsyncLoadShadowTask(this).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, drawable);
        }
    }

    void setImageDrawableAndShadowBitmap(Drawable drawable, Bitmap shadowBitmap) {
        super.setImageDrawable(drawable);
        shouldGenerateBitmap = false;
        setImageShadowBitmap(shadowBitmap);
    }

    @Override
    public void setImageBitmap(Bitmap bm) {
        super.setImageBitmap(bm);
        shouldGenerateBitmap = true;
    }

    @Override
    public void setImageDrawable(@Nullable Drawable drawable) {
        if (getDrawable() == drawable) {
            return;
        }
        super.setImageDrawable(drawable);
        shouldGenerateBitmap = true;
    }

    @Override
    public void setImageResource(@DrawableRes int resId) {
        super.setImageResource(resId);
        shouldGenerateBitmap = true;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (shouldGenerateBitmap) {
            generateShadowBitmapInternal();
        }
        final int left = (getWidth() - mIconSize) / 2;
        final int top = (getHeight() - mIconSize) / 2;
        if (mShadowBitmap != null) {
            mTempRect.set(left, top, left + mIconSize, top + mIconSize);
            canvas.drawBitmap(mShadowBitmap, null, mTempRect, null);
        }
    }

    @Override
    public void dispatchSetPressed(boolean pressed) {
        super.dispatchSetPressed(pressed);
        setAlpha(pressed ? 0.3f : 1f);
    }

    public Bitmap getShadowBitmap() {
        return mShadowBitmap;
    }

    public int getIconSize() {
        return mIconSize;
    }

    private static class AsyncLoadShareDrawableTask extends AsyncTask<ShareItem, Object, Object> {

        private WeakReference<ShadowBitmapView> mRelateShadowBitmapViewRef;
        private Drawable drawable;
        private Bitmap shadowBitmap;

        AsyncLoadShareDrawableTask(ShadowBitmapView shadowBitmapView) {
            mRelateShadowBitmapViewRef = new WeakReference<>(shadowBitmapView);
        }

        @Override
        protected Drawable doInBackground(ShareItem... params) {
            Drawable d = null;
            if (mRelateShadowBitmapViewRef != null) {
                ShadowBitmapView shadowBitmapView = mRelateShadowBitmapViewRef.get();
                Context context = null;
                if (shadowBitmapView != null) {
                    context = shadowBitmapView.getContext().getApplicationContext();
                }
                if (context != null) {
                    d =  params[0].getDrawable(context);
                }
            }
            if (d != null) {
                drawable = d;
                shadowBitmap = generateShadowBitmap(d);
            }
            return null;
        }

        @Override
        protected void onPostExecute(Object result) {
            if (mRelateShadowBitmapViewRef != null) {
                ShadowBitmapView shadowBitmapView = mRelateShadowBitmapViewRef.get();
                if (shadowBitmapView != null) {
                    shadowBitmapView.setImageDrawableAndShadowBitmap(drawable, shadowBitmap);
                }
            }
            drawable = null;
            shadowBitmap = null;
        }
    }

    private static class AsyncLoadShadowTask extends AsyncTask<Drawable, Object, Bitmap> {

        private WeakReference<ShadowBitmapView> mRelateShadowBitmapViewRef;

        AsyncLoadShadowTask(ShadowBitmapView shadowBitmapView) {
            mRelateShadowBitmapViewRef = new WeakReference<>(shadowBitmapView);
        }

        @Override
        protected Bitmap doInBackground(Drawable... params) {
            Bitmap shadowBitmap = null;
            try {
                shadowBitmap = generateShadowBitmap(params[0]);
            } catch (Exception e) {
                //ignore
            }
            return shadowBitmap;
        }

        @Override
        protected void onPostExecute(Bitmap o) {
            if (mRelateShadowBitmapViewRef != null) {
                ShadowBitmapView shadowBitmapView = mRelateShadowBitmapViewRef.get();
                if (shadowBitmapView != null) {
                    shadowBitmapView.setImageShadowBitmap(o);
                }
            }
        }
    }
}
