package com.smartisanos.sara.widget;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.app.SmtPCUtils;
import android.content.Context;
import android.content.res.Resources;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Region;
import android.os.Build;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.util.Spline;
import android.view.View;
import android.widget.FrameLayout.LayoutParams;

import com.smartisanos.ideapills.common.util.CommonUtils;
import com.smartisanos.sara.R;

import com.smartisanos.sara.bubble.manager.BubbleThemeManager;
import com.smartisanos.sara.util.SaraUtils;
import com.smartisanos.sara.util.SaraUtils.WaveType;
import com.smartisanos.ideapills.common.util.CommonConstant;
import java.util.ArrayList;
import java.util.List;

public class WaveView extends View {
    private static final Boolean DEBUG = Build.IS_DEBUGGABLE;
    private static final String LOG_TAG = "VoiceAss.WaveView";
    private static final int POINT_INTERVAL_MS = 20;
    private Paint mWaveFormDrawPaint;
    private Paint mWaveFormDrawPassPaint;
    private Paint mWaveFormDrawNormlPaint;
    private static final int WAVEFORM_LENGTH = 4096;
    private static final int DELAY_SHOW = 100;
    private static final int LEFT_WAVE_ANIM_DURATION = 5000;
    private static final int OTHER_WAVE_ANIM_DURATION = 3700;
    private List<Long> mWaveStamp = new ArrayList<Long>();
    private List<Long> mWavePointNum = new ArrayList<Long>();
    private Spline mSpline;
    private byte mWaveData[];
    private int mWavePaddingLeft;
    private int mWavePaddingRight;
    private float mWaveHeightScale;

    private float mWaveFormFramWidth;
    private float mWaveFormDrawStartPosition;
    private int mDrawPointNum;
    private int mTotalPointNum;
    private int mWaveMinWidth;
    private int mWaveMaxWidth;
    private WaveType mType;
    private int mDuration = 1;
    private int mCurProgress = 0;
    private AnimationListener mListener;
    private ValueAnimator mAnimator;
    private ValueAnimator mWaveUpdateAnimator;
    private Path mPath;
    private int mRadius;
    private float[] mLines = new float[320];
    private int mWaveCoverWidth;
    private int mBubbleArrowViewWidth;
    private boolean mShowMiddle = false;
    private int mPathClipTop;
    private RectF mTempRect = new RectF();
    private int mMaxAnimDuration = LEFT_WAVE_ANIM_DURATION;
    private int mWaveAnimDuration;
    private float mWaveWidthFraction = 1.0f;

    public static interface AnimationListener {
        public void onAnimationEnd(int witdh, boolean isCanceled);
        public void onAnimationCancel(int width);
        public void onAnimationUpdate(int width);
    }

    public WaveView(Context context) {
        super(context);
        init();
    }

    public WaveView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public WaveView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    public void init() {
        Resources res = getResources();
        mWaveCoverWidth = res.getDimensionPixelSize(R.dimen.wave_cover_width);
        mWaveMaxWidth = res.getDimensionPixelSize(R.dimen.bubble_wave_max_width);
        mBubbleArrowViewWidth = res.getDimensionPixelSize(R.dimen.bubble_arrow_width);
        if (CommonUtils.shouldScaleWaveform()) {
            mWaveHeightScale = res.getDimensionPixelSize(R.dimen.bubble_list_item_wave_height_trident) * 1.0f / 255;
        } else {
            mWaveHeightScale = res.getDimensionPixelSize(R.dimen.bubble_list_item_wave_height) * 1.0f / 255;
        }
        mWaveFormFramWidth = res.getDimensionPixelSize(R.dimen.bubble_wave_form_size) / 2.0f;
        updateWidthPrama();
        mWavePaddingRight = res.getDimensionPixelSize(R.dimen.wave_padding_right);
        mWaveFormDrawNormlPaint = new Paint();
        mWaveFormDrawNormlPaint.setColor(getResources().getColor(R.color.bubble_wave_line_text_normal_color));
        mWaveFormDrawNormlPaint.setStrokeWidth(mWaveFormFramWidth);
        mWaveFormDrawNormlPaint.setAntiAlias(true);
        mWaveFormDrawPaint = new Paint();
        mWaveFormDrawPaint.setColor(getResources().getColor(R.color.bubble_wave_line_text_color));
        mWaveFormDrawPaint.setStrokeWidth(mWaveFormFramWidth);

        mWaveFormDrawPassPaint = new Paint();
        mWaveFormDrawPassPaint.setColor(getResources().getColor(R.color.bubble_wave_line_text_color_pass));
        mWaveFormDrawPassPaint.setStrokeWidth(mWaveFormFramWidth);
        mPath = new Path();
        mRadius = res.getDimensionPixelOffset(R.dimen.bubble_wave_text_popup_radius);
        mPathClipTop = res.getDimensionPixelOffset(R.dimen.bubble_wave_path_clip);

        mWaveUpdateAnimator = ValueAnimator.ofInt(0, 60);
        mWaveUpdateAnimator.setDuration(1000);
        mWaveUpdateAnimator.addUpdateListener(new AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                invalidate();
            }
        });
        mWaveUpdateAnimator.setRepeatCount(ValueAnimator.INFINITE);
    }

    public void updateWidthPrama() {
        updateWidthPrama(SaraUtils.isLeftPopBubble(), SaraUtils.isBlindMode());
    }

    public void updateWidthPrama(boolean isLeftPop) {
        updateWidthPrama(isLeftPop, SaraUtils.isBlindMode());
    }

    public void updateWidthPrama(boolean isLeftPop, boolean isBlindMode) {
        Resources res = getResources();
        boolean isLeftSlide = isLeftPop || isBlindMode;
        mWaveMinWidth = isLeftPop ? res.getDimensionPixelSize(R.dimen.bubble_wave_left_min_width) : res.getDimensionPixelSize(R.dimen.bubble_wave_min_width);
        mWavePaddingLeft = res.getDimensionPixelSize(isLeftSlide ? R.dimen.left_wave_padding_left : R.dimen.wave_padding_left);
        mMaxAnimDuration = isLeftSlide ? LEFT_WAVE_ANIM_DURATION : OTHER_WAVE_ANIM_DURATION;
        mWaveMaxWidth = res.getDimensionPixelSize(R.dimen.bubble_wave_max_width);
        mWaveWidthFraction = SmtPCUtils.isValidExtDisplayId(mContext) ? 2f : 1f;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (mWaveData == null) {
            return;
        }
        final int measureWidth = getMeasuredWidth();
        final int measureHeight = getMeasuredHeight();
        mWaveFormDrawStartPosition = measureWidth/* - mWavePaddingRight*/;
        int baseCenterY = (measureHeight) / 2;
        canvas.save();
        if (mType == WaveType.START_WAVE) {
            calculateElapsedTime();
            mPath.reset();
            canvas.clipPath(mPath);
            mTempRect.set(mWavePaddingLeft, mPathClipTop, measureWidth - mWavePaddingRight, measureHeight - mPathClipTop);
            mPath.addRoundRect(mTempRect, mRadius, mRadius, Path.Direction.CCW);
            canvas.clipPath(mPath, Region.Op.UNION);
            canvas.drawLine(mWavePaddingLeft, baseCenterY, mWaveFormDrawStartPosition, baseCenterY, mWaveFormDrawNormlPaint);
            short data;
            byte originalData;
            float waveDataHalfHeight;
            int count = 0;
            int length = mTotalPointNum - mWaveData.length;
            int min = length > 0 ? length : 0;
            float detY = 0;
            float waveFormWidth = mWaveFormFramWidth * mWaveWidthFraction;
            for (int i = mDrawPointNum - 1; i >= min; i--) {
                originalData = mWaveData[i % mWaveData.length];

                if (originalData >= 0) {
                    data = originalData;
                } else {
                    data = (short) (originalData & 0xff);
                }
                waveDataHalfHeight = data > CommonConstant.WAVE_DATA_CRITICAL
                        ? data * CommonConstant.WAVE_DATA_FRACTION * mWaveHeightScale / 2 + 0.5f
                        : data * mWaveHeightScale / 2 + 0.5f;
                if (waveDataHalfHeight > waveFormWidth / 2 + 0.5f) {
                    if (mWaveFormDrawStartPosition <= mBubbleArrowViewWidth + mWavePaddingLeft) {
                        detY = (float) Math.sin(Math.PI/4) * (mWaveFormDrawStartPosition - mWavePaddingLeft);
                        waveDataHalfHeight = Math.min(Math.abs(detY),Math.abs(waveDataHalfHeight));
                    }
                    mLines[count++] = mWaveFormDrawStartPosition;
                    mLines[count++] = baseCenterY - waveDataHalfHeight;
                    mLines[count++] = mWaveFormDrawStartPosition;
                    mLines[count] = baseCenterY + waveDataHalfHeight;
                    if (count >= mLines.length - 4) {
                        canvas.drawLines(mLines, 0, count + 1, mWaveFormDrawNormlPaint);
                        count = 0;
                    } else {
                        count++;
                    }
                }
                mWaveFormDrawStartPosition -= waveFormWidth;
                if (mWaveFormDrawStartPosition <= mWavePaddingLeft) {
                    break;
                }
            }
            if (count > 0) {
                canvas.drawLines(mLines, 0, count + 1, mWaveFormDrawNormlPaint);
            }
        } else {

            final int waveFormMarginLeft = 5;
            final int waveFormMarginRight = 5;

            canvas.clipRect(waveFormMarginLeft, 0.0f, measureWidth - waveFormMarginRight, measureHeight);
            int length = mWaveData.length;
            float waveFormDrawStartPosition = waveFormMarginLeft;

            float middleRange = measureWidth - waveFormMarginLeft - waveFormMarginRight;

            float divide = mCurProgress * 1.0f / mDuration * middleRange;
            Paint paint = mWaveFormDrawPassPaint;
            if (!mShowMiddle) {
                mPath.reset();
                mPath.addCircle((middleRange / 2 + waveFormMarginLeft - 0.4f), getHeight() / 2, mWaveCoverWidth / 2, Path.Direction.CCW);
                canvas.clipPath(mPath, Region.Op.DIFFERENCE);
            }
            float pos;
            int i;
            float datal, datar;
            short data;
            byte originalData;
            for (float p = 0; p < middleRange; p += mWaveFormFramWidth) {
                if (p >= divide) {
                    paint = mWaveFormDrawPaint;
                }
                pos = p / middleRange * length;
                i = (int) pos;
                originalData = 0;
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
                        ? data * CommonConstant.WAVE_DATA_FRACTION * mWaveHeightScale / 2 + 0.5f
                        : data * mWaveHeightScale / 2 + 0.5f;
                canvas.drawLine(waveFormDrawStartPosition, baseCenterY - waveDataHalfHeight, waveFormDrawStartPosition, baseCenterY + waveDataHalfHeight, paint);

                // move the wave form draw position to next frame
                waveFormDrawStartPosition += mWaveFormFramWidth;
            }
        }
        canvas.restore();
    }

    public void waveChanged(byte[] waveFormData, int pointNum) {
        if (mWaveData == null) {
            return;
        }
        if (mTotalPointNum == 0) {
            mAnimator.start();
            startWaveUpdate();
        }
        for (int i = 0; i < mWaveData.length && i < waveFormData.length; i++) {
            mWaveData[(pointNum - 1 - i) % WAVEFORM_LENGTH] = waveFormData[waveFormData.length - 1 - i];
        }
        long timeStamp = SystemClock.uptimeMillis();
        mTotalPointNum = pointNum;
        if (mWaveStamp.size() <= 0) {
            mWaveStamp.add(timeStamp);
            mWavePointNum.add((long) mTotalPointNum);
        } else {
            if (mTotalPointNum <= mWavePointNum.get(mWavePointNum.size() - 1)
                    || timeStamp <= mWaveStamp.get(mWaveStamp.size() - 1) + 2) {
                // NA
            } else {
                mWaveStamp.add(timeStamp);
                mWavePointNum.add((long) mTotalPointNum);
            }
        }
        buildSpline();
    }

    private void buildSpline() {
        if (mWaveStamp.size() < 2) {
            return;
        }
        float[] stmp = new float[mWaveStamp.size()];
        for (int i = 0; i < mWaveStamp.size(); ++i) {
            stmp[i] = mWaveStamp.get(i) - mWaveStamp.get(0);
        }
        float[] num = new float[mWavePointNum.size()];
        for (int i = 0; i < mWavePointNum.size(); ++i) {
            num[i] = mWavePointNum.get(i);
        }
        mSpline = Spline.createSpline(stmp, num);
    }

    private int getTargetNum() {
        if (mSpline == null || mWaveStamp.size() < 2) {
            return 0;
        }
        long now = SystemClock.uptimeMillis();
        float x = now - DELAY_SHOW - mWaveStamp.get(0);
        return (int) (mSpline.interpolate(x) + 0.5f);
    }

    public void reset() {
        reset(SaraUtils.isLeftPopBubble() || SaraUtils.isBlindMode());
    }

    public void reset(boolean isLeftSide) {
        stopWaveUpdate();
        mWaveData = new byte[WAVEFORM_LENGTH];
        mWaveFormDrawNormlPaint.setColor(getResources().getColor(BubbleThemeManager.getWaveColor(SaraUtils.getDefaultBubbleColor(getContext()))));
        if (mType == WaveType.START_WAVE) {
            mWaveStamp.clear();
            mWavePointNum.clear();
            mSpline = null;
            mDrawPointNum = 0;
            mTotalPointNum = 0;
            LayoutParams params = (LayoutParams) getLayoutParams();
            params.width = mWaveMinWidth;
            setLayoutParams(params);
            mWaveFormDrawStartPosition = mWaveMinWidth;
            if (mAnimator != null) {
                clearAnimation();
                mAnimator.cancel();
                if (SmtPCUtils.isValidExtDisplayId(mContext)) {
                    mAnimator = null;
                }
            }
            if (mAnimator == null) {
                mAnimator = ValueAnimator.ofFloat(mWaveMinWidth, mWaveMaxWidth);
                if (mWaveAnimDuration != 0) {
                    mAnimator.setDuration(mWaveAnimDuration);
                } else {
                    mAnimator.setDuration(mMaxAnimDuration);
                }
                mAnimator.setInterpolator(null);
                mAnimator.addUpdateListener(new AnimatorUpdateListener() {
                    @Override
                    public void onAnimationUpdate(ValueAnimator animation) {
                        Object value = animation.getAnimatedValue();
                        if (value instanceof Float) {
                            mListener.onAnimationUpdate(((Float) value).intValue());
                        }
                    }
                });
                mAnimator.addListener(new Animator.AnimatorListener() {
                    private boolean mIsCanceled = false;
                    @Override
                    public void onAnimationStart(Animator animation) {
                        mIsCanceled = false;
                    }

                    @Override
                    public void onAnimationRepeat(Animator animation) {
                    }

                    @Override
                    public void onAnimationEnd(Animator animation) {
                        LayoutParams params = (LayoutParams) getLayoutParams();
                        if (params.width == mWaveMaxWidth) {
                            mListener.onAnimationEnd(params.width, mIsCanceled);
                        }
                    }

                    @Override
                    public void onAnimationCancel(Animator animation) {
                        mIsCanceled = true;
                    }
                });
            }
        }
        invalidate();
    }

    public void setAnimationListener(AnimationListener listener) {
        mListener = listener;
    }

    public void stopAnimation(boolean isCancelWithoutCallback) {
        stopWaveUpdate();
        if (mAnimator != null) {
            if (!isCancelWithoutCallback) {
                mListener.onAnimationCancel(((LayoutParams) getLayoutParams()).width);
            }
            mAnimator.cancel();
        }
    }

    private void calculateElapsedTime() {
        int toDrawPoint = getTargetNum();
        if (toDrawPoint <= mDrawPointNum) {
            if (mDrawPointNum != 0) {
                mDrawPointNum = mDrawPointNum + 1;
            }
        } else {
            mDrawPointNum = toDrawPoint;
        }
        if (mTotalPointNum - mDrawPointNum >= mWaveData.length
                || mDrawPointNum > mTotalPointNum) {
            mDrawPointNum = mTotalPointNum;
        }
    }

    public void setCurPosition(int position) {
        mCurProgress = position;
        invalidate();
    }

    public void setMaxDuration(int duration) {
        mDuration = duration;
    }

    public void setWaveType(WaveType type) {
        mType = type;
    }

    public void setWaveData(byte[] data) {
        mWaveData = data;
    }
    public void setShowMiddle(boolean showMiddle) {
        mShowMiddle = showMiddle;
        invalidate();
    }

    private void startWaveUpdate() {
        mWaveUpdateAnimator.start();
    }

    private void stopWaveUpdate() {
        mWaveUpdateAnimator.cancel();
    }

    public void setPaintColor(int color) {
        mWaveFormDrawNormlPaint.setColor(getResources().getColor(BubbleThemeManager.getWaveColor(color)));
        mWaveFormDrawPaint.setColor(getResources().getColor(BubbleThemeManager.getWaveNomalColor(color)));
        mWaveFormDrawPassPaint.setColor(getResources().getColor(BubbleThemeManager.getWavePassColor(color)));
    }

    public void setWaveMaxWidth(int width) {
        int oldMaxWidth = mWaveMaxWidth;
        if (oldMaxWidth != 0) {
            mWaveAnimDuration = (int) (1f * width / oldMaxWidth * LEFT_WAVE_ANIM_DURATION);
        } else {
            mWaveAnimDuration = LEFT_WAVE_ANIM_DURATION;
        }
        mWaveMaxWidth = width;
    }

    public void setWaveMinWidth(int width) {
        mWaveMinWidth = width;
    }

    public void updateLeftPadding(boolean isLeftSlide) {
        Resources res = getResources();
        mWavePaddingLeft = res.getDimensionPixelSize(isLeftSlide ? R.dimen.left_wave_padding_left : R.dimen.wave_padding_left);
    }
}
