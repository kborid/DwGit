package com.kborid.smart.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.kborid.library.common.LogUtils;

public class CustomSurfaceView extends SurfaceView implements SurfaceHolder.Callback, Runnable {

    private SurfaceHolder mHolder = null;
    private boolean mIsDrawing = false;
    private Canvas mCanvas = null;

    public CustomSurfaceView(Context context) {
        this(context, null);
    }

    public CustomSurfaceView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CustomSurfaceView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        mHolder = getHolder();
        mHolder.addCallback(this);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        LogUtils.d("surfaceCreated()");
        mIsDrawing = true;
        new Thread(this).start();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        LogUtils.d("surfaceChanged()");
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        LogUtils.d("surfaceDestroyed()");
        mIsDrawing = false;
    }

    @Override
    public void run() {
        while (mIsDrawing) {
            try {
                drawInner();
                Thread.sleep(1000);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void drawInner() {
        LogUtils.d("drawInner()");
        try {
            mCanvas = mHolder.lockCanvas();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (null != mCanvas) {
                mHolder.unlockCanvasAndPost(mCanvas);
            }
        }
    }
}
