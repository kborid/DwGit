package com.smartisanos.ideapills.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Region;
import android.util.AttributeSet;
import android.view.View;

import com.smartisanos.ideapills.R;
import com.smartisanos.ideapills.common.util.CommonUtils;
import com.smartisanos.ideapills.util.LOG;
import com.smartisanos.ideapills.common.util.CommonConstant;

public class BubbleSpeechWaveView extends View{
    private static final LOG log = LOG.getInstance(BubbleListView.class);
    private int mDuration = 1;
    private int mCurProgress = 0;
    private int mWaveFormHeight;
    private int mWaveFormFramWidth;
    private byte[] mWaveData;
    Paint mWaveFormDrawPaint;
    Paint mWaveFormDrawPassPaint;
    private int mWaveCoverWidth;
    Path mPath = new Path();
    private boolean mShowMiddle = false;

    public BubbleSpeechWaveView(Context context) {
        this(context, null);
    }

    public BubbleSpeechWaveView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public BubbleSpeechWaveView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        if (CommonUtils.shouldScaleWaveform()) {
            mWaveFormHeight = getResources().getDimensionPixelSize(R.dimen.bubble_list_item_wave_height_trident);
        } else {
            mWaveFormHeight = getResources().getDimensionPixelSize(R.dimen.bubble_list_item_wave_height);
        }
        mWaveFormFramWidth = getResources().getDimensionPixelSize(R.dimen.bubble_wave_form_size);
        // setup wave form draw paint
        mWaveFormDrawPaint = new Paint();
        mWaveFormDrawPaint.setColor(getResources().getColor(R.color.bubble_wave_line_text_color));
        mWaveFormDrawPaint.setStrokeWidth(mWaveFormFramWidth);
        mWaveFormDrawPassPaint = new Paint();
        mWaveFormDrawPassPaint.setColor(getResources().getColor(R.color.bubble_wave_line_text_color_pass));
        mWaveFormDrawPassPaint.setStrokeWidth(mWaveFormFramWidth);
        mWaveCoverWidth = getResources().getDimensionPixelOffset(R.dimen.bubble_wave_cover_width);
    }

    public void setPaintColor(int waveFromColorRes, int waveFormPassColorRes) {
        mWaveFormDrawPaint.setColor(getResources().getColor(waveFromColorRes));
        mWaveFormDrawPassPaint.setColor(getResources().getColor(waveFormPassColorRes));
    }

    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        final int measureWidth = getMeasuredWidth();
        final int measureHeight = getMeasuredHeight();
        final int waveFormHeight = mWaveFormHeight;
        final float waveHeightScale = waveFormHeight * 1.0f / 255;
        final int waveFormMarginLeft = 0;
        final int waveFormMarginRight = 0;
        final int waveFormFrameWidth = mWaveFormFramWidth;
        if (LOG.DBG) {
            log.info("onDraw mWaveData="+mWaveData);
        }
        // draw wave
        if (mWaveData != null) {
            canvas.save();
            canvas.clipRect(waveFormMarginLeft, 0.0f, measureWidth
                    - waveFormMarginRight, measureHeight);
            int length = mWaveData.length;
            float waveFormDrawStartPosition = waveFormMarginLeft;
            float waveFormPositionTranslate = waveFormFrameWidth;

            float middleRange = measureWidth - waveFormMarginLeft - waveFormMarginRight;
            if (!mShowMiddle) {
                mPath.reset();
                mPath.addCircle((middleRange / 2 + waveFormMarginLeft), getHeight() / 2, mWaveCoverWidth / 2, Path.Direction.CCW);
                canvas.clipPath(mPath, Region.Op.DIFFERENCE);
            }
            int baseCenterY = (measureHeight) / 2;
            float divide = mCurProgress * 1.0f / mDuration * middleRange;
            Paint paint = mWaveFormDrawPassPaint;
            for (float p = 0; p < middleRange; p += waveFormFrameWidth) {
                if (p >= divide) {
                    paint = mWaveFormDrawPaint;
                }
                float pos = p / middleRange * length;
                int i = (int) pos;
                float datal, datar;
                short data;

                byte originalData = 0;
                if (i >= 0 && i < length) {
                    originalData = mWaveData[i];
                }
                if (originalData >= 0) {
                    datal = originalData;
                } else {
                    datal = originalData & 0xff;
                }

                originalData = 0;
                if (i + 1 >= 0 && i + 1 < length) {
                    originalData = mWaveData[i + 1];
                }
                if (originalData >= 0) {
                    datar = originalData;
                } else {
                    datar = originalData & 0xff;
                }

                data = (short) ((datar - datal) * (pos - i) + datal);

                // draw one wave form frame
                float waveDataHalfHeight = data > CommonConstant.WAVE_DATA_CRITICAL
                        ? data * CommonConstant.WAVE_DATA_FRACTION * waveHeightScale / 2 + 0.5f
                        : data * waveHeightScale / 2 + 0.5f;
                    canvas.drawLine(waveFormDrawStartPosition, baseCenterY
                                    - waveDataHalfHeight, waveFormDrawStartPosition,
                            baseCenterY + waveDataHalfHeight, paint);
                waveFormDrawStartPosition += waveFormPositionTranslate;
            }
            canvas.restore();
        }
    }

    public void setCurPosition(int position) {
        mCurProgress = position;
        invalidate();
    }

    public void setMaxDuration(int duration) {
        if (duration > 0) {
            mDuration = duration;
        }
    }

    public void setWaveData(byte[] data) {
        mWaveData = data;
    }

    public void setShowMiddle(boolean showMiddle) {
        mShowMiddle = showMiddle;
        invalidate();
    }
}
