package com.smartisanos.ideapills.common.util;

import android.annotation.NonNull;
import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.os.AsyncTask;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.SurfaceControl;
import android.view.View;
import android.view.WindowManager;

import com.smartisanos.ideapills.common.R;

import smartisanos.api.WindowManagerSmt;
import smartisanos.util.DeviceType;
import smartisanos.util.GaussianBlurUtils;

public class BlurTask extends AsyncTask<Void, Void, LayerDrawable> {

    private static final String TAG = "BlurTask";

    /**
     * the scale the bitmap, the smaller the bitmap, the less time blur will
     * take.
     */
    private static final int SCALE_FACTOR = 12;
    private static final int sParam = 10; // used to control the blur level
    private Context mContext;
    private Bitmap mBlurBitmap;
    private Bitmap mSrcBitmap;
    private View mTargetView;
    private Resources mResource;
    private Bitmap mMaskBitmap;
    private int mMaskResId;

    private WindowManager mWm;
    private int mDisplayWidth;
    private int mDisplayHeight;

    /**
     * @param offsetY the srcBitmap contains the title layout, use offsetY to cut it out.
     */
    public BlurTask(Bitmap srcBitmap, View view, Activity context) {
        mSrcBitmap = srcBitmap;
        mTargetView = view;
        mResource = context.getResources();
    }

    /**
     * @param view
     * @param activity
     */
    public BlurTask(View view, @NonNull Activity activity) {
        mSrcBitmap= null;
        mTargetView = view;
        mResource = activity.getResources();
        mWm = (WindowManager) activity.getSystemService(Context.WINDOW_SERVICE);
        Display curWinForDisplay = activity.getWindowManager().getDefaultDisplay();
        mDisplayWidth = curWinForDisplay.getWidth();
        mDisplayHeight = curWinForDisplay.getHeight();
    }

    @Override
    protected LayerDrawable doInBackground(Void... params) {
        scaleBitmap();
        GaussianBlurUtils.gaussianBlur(mBlurBitmap, sParam, sParam);
        return getBlurBitmap();
    }

    @Override
    protected void onPostExecute(LayerDrawable drawable) {
        mTargetView.setBackground(drawable);
        if (mMaskBitmap != null && !mMaskBitmap.isRecycled()) {
            mMaskBitmap.recycle();
        }
        if (mSrcBitmap != null && !mSrcBitmap.isRecycled()) {
            mSrcBitmap.recycle();
        }
    }

    public void setMaskResource(int resId) {
        mMaskResId = resId;
    }

    private void scaleBitmap() {
        if (mSrcBitmap == null) {
            mSrcBitmap = getSrcBitmap();
        }

        DisplayMetrics dm = mResource.getDisplayMetrics();
        if (mSrcBitmap == null) {
            mSrcBitmap = Bitmap.createBitmap(dm.widthPixels, dm.heightPixels, Bitmap.Config.ARGB_4444);
            mSrcBitmap.eraseColor(Color.WHITE);
        }
        int dstWidth = dm.widthPixels / SCALE_FACTOR;
        int dstHeight = dm.heightPixels / SCALE_FACTOR;
        mBlurBitmap = Bitmap.createBitmap(dstWidth, dstHeight, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(mBlurBitmap);
        canvas.drawBitmap(mSrcBitmap, new Rect(0, 0, mSrcBitmap.getWidth(), mSrcBitmap.getHeight()), new Rect(0, 0, dstWidth, dstHeight), new Paint());
    }

    public LayerDrawable getBlurBitmap() {
        Drawable[] array = new Drawable[2];
        array[0] = new BitmapDrawable(mBlurBitmap);
        array[1] = mResource.getDrawable(mMaskResId > 0 ? mMaskResId : R.drawable.bg_mask);
        LayerDrawable ld = new LayerDrawable(array);
        return ld;
    }

    private Bitmap getSrcBitmap() {
        Rect rect = new Rect();
        WindowManagerSmt.getInstance().getThumbModeCropGlobal(mWm, rect);
        return SurfaceControl.screenshot(rect, mDisplayWidth, mDisplayHeight, 0, getPillsWindowLayer(), false, 0);
    }

    private int getPillsWindowLayer() {
        if (DeviceType.isOneOf(DeviceType.T2, DeviceType.U1)) {
            return 60000;
        } else if (DeviceType.is(DeviceType.T1)) {
            return 50000;
        } else {
            return 70000;
        }
    }
}
