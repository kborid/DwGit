package com.smartisanos.sara.util;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.view.Display;
import android.view.WindowManager;
import android.util.Log;

import com.smartisanos.ideapills.common.util.BitmapUtils;
import com.smartisanos.sara.R;

import smartisanos.api.BlurMaskFilterSmt;

public class HolographicOutlineHelper {

    private final Paint mHolographicPaint = new Paint();
    private final Paint mBlurPaint = new Paint();
    private final Paint mErasePaint = new Paint();
    private final Paint mAlphaClipPaint = new Paint();

    public static final int SHADOW_DARK = 0;
    public static final int SHADOW_LIGHT = 1;

    public static final int MAX_OUTER_BLUR_RADIUS;
    public static final int MIN_OUTER_BLUR_RADIUS;

    private static final float SCALE = 1.5f;

    static {
        final float scale = 1.5f;
        MIN_OUTER_BLUR_RADIUS = (int) (scale * 1.0f);
        MAX_OUTER_BLUR_RADIUS = (int) (scale * 12.0f);
    }

    private int[] mTempOffset = new int[2];

    public static int ICON_SIZE_ORIGIN;
    public static int ICON_SIZE_WITH_SHADOW;
    public static int ICON_SHADOW_SINGLE_RADIUS;

    public static int[] ICON_SHADOW_RADIUS;
    public static int[][] ICON_SHADOW_COLOR = new int[2][];

    static {
        ICON_SHADOW_COLOR[SHADOW_DARK] = new int[]{0x12000000, 0x26000000};
        ICON_SHADOW_COLOR[SHADOW_LIGHT] = new int[]{0x12000000, 0x12000000};
    }

    public HolographicOutlineHelper() {
        mHolographicPaint.setFilterBitmap(true);
        mHolographicPaint.setAntiAlias(true);
        mBlurPaint.setFilterBitmap(true);
        mBlurPaint.setAntiAlias(true);
        mErasePaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_OUT));
        mErasePaint.setFilterBitmap(true);
        mErasePaint.setAntiAlias(true);
    }

    public static void init(Resources resources, boolean is2K) {
        ICON_SIZE_ORIGIN = resources.getInteger(R.integer.icon_size_origin);
        ICON_SIZE_WITH_SHADOW = resources.getInteger(R.integer.icon_size_with_shadow);
        ICON_SHADOW_SINGLE_RADIUS = resources.getInteger(R.integer.icon_shadow_single_radius);
        if (is2K) {
            ICON_SHADOW_RADIUS = new int[]{25, 10};
        } else {
            ICON_SHADOW_RADIUS = new int[]{20, 8};
        }
    }

    public static void init(Context context) {
        Resources resources = context.getResources();
        int[] size = getWindowSize(context);
        init(resources, size[0] == 1440);
    }

    public static int[] getWindowSize(Context context) {
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();
        Point realSize = new Point();
        display.getRealSize(realSize);
        int[] size = new int[]{realSize.x, realSize.y};
        if (size[0] > size[1]) {
            size = new int[]{realSize.y, realSize.x};
        }
        return size;
    }

    Bitmap applyCustomExpensiveOutlineWithBlur(Bitmap srcDst, Canvas srcDstCanvas, int color,
                                               int outlineColor, float radius) {
        // We start by removing most of the alpha channel so as to ignore shadows, and
        // other types of partial transparency when defining the shape of the object
        Bitmap glowShape = srcDst.extractAlpha(mAlphaClipPaint, mTempOffset);

        BlurMaskFilter outerBlurMaskFilter = getBlurMaskFilter(SCALE, radius);
        mBlurPaint.setMaskFilter(outerBlurMaskFilter);
        int[] outerBlurOffset = new int[2];
        Bitmap thickOuterBlur = glowShape.extractAlpha(mBlurPaint, outerBlurOffset);

        int[] brightOutlineOffset = new int[2];
        Bitmap brightOutline = glowShape.extractAlpha(mBlurPaint, brightOutlineOffset);

        // calculate the inner blur
        srcDstCanvas.setBitmap(glowShape);
        srcDstCanvas.drawColor(0xFF000000, PorterDuff.Mode.CLEAR);
        BlurMaskFilter innerBlurMaskFilter = new BlurMaskFilter(SCALE * radius, BlurMaskFilter.Blur.OUTER);
        mBlurPaint.setMaskFilter(innerBlurMaskFilter);
        int[] thickInnerBlurOffset = new int[2];
        Bitmap thickInnerBlur = glowShape.extractAlpha(mBlurPaint, thickInnerBlurOffset);

        // mask out the inner blur
        srcDstCanvas.setBitmap(thickInnerBlur);
        srcDstCanvas.drawBitmap(glowShape, -thickInnerBlurOffset[0],
                -thickInnerBlurOffset[1], mErasePaint);
        srcDstCanvas.drawRect(0, 0, -thickInnerBlurOffset[0], thickInnerBlur.getHeight(),
                mErasePaint);
        srcDstCanvas.drawRect(0, 0, thickInnerBlur.getWidth(), -thickInnerBlurOffset[1],
                mErasePaint);

        Bitmap dest = Bitmap.createBitmap(brightOutline.getWidth(), brightOutline.getHeight(), Bitmap.Config.ARGB_8888);
        float offsetx = (brightOutline.getWidth() - srcDst.getWidth()) / 2f;
        // draw the inner and outer blur
        srcDstCanvas.setBitmap(dest);
        mHolographicPaint.setColor(color);
        srcDstCanvas.drawBitmap(thickInnerBlur, thickInnerBlurOffset[0] + offsetx, thickInnerBlurOffset[1],
                mHolographicPaint);
        srcDstCanvas.drawBitmap(thickOuterBlur, outerBlurOffset[0] + offsetx, outerBlurOffset[1],
                mHolographicPaint);

        // draw the bright outline
        mHolographicPaint.setColor(outlineColor);
        srcDstCanvas.drawBitmap(brightOutline, brightOutlineOffset[0] + offsetx, brightOutlineOffset[1],
                mHolographicPaint);

        // cleanup
        srcDstCanvas.setBitmap(null);
        brightOutline.recycle();
        thickOuterBlur.recycle();
        thickInnerBlur.recycle();
        glowShape.recycle();
        return dest;
    }

    public static Bitmap getIconWithDarkShadow(Bitmap src) {
        return attachShadow(src, SHADOW_DARK);
    }

    public static Bitmap getIconWithDarkShadow(Drawable src) {
        return attachShadow(src, SHADOW_DARK);
    }

    public static Bitmap getIconWithLightShadow(Bitmap src) {
        return attachShadow(src, SHADOW_LIGHT);
    }

    public static Bitmap getIconWithLightShadow(Drawable src) {
        return attachShadow(src, SHADOW_LIGHT);
    }

    private static Bitmap attachShadow(Drawable src, int shadowIndex) {
        int originIconSize = ICON_SIZE_ORIGIN;
        //format img with size
        Bitmap img = BitmapUtils.drawableToBitmap(src);
        if (src.getIntrinsicWidth() != originIconSize || src.getIntrinsicWidth() != originIconSize) {
            img = BitmapUtils.getSquareBitmap(img, originIconSize);
        }
        //ICON_SIZE_WITH_SHADOW;//
        int shadowIconSize = ICON_SIZE_WITH_SHADOW;
        Bitmap iconWithShadow = attachMultiShadow(img, shadowIndex, originIconSize, shadowIconSize);
        img.recycle();
        return iconWithShadow;
    }

    private static Bitmap attachShadow(Bitmap src, int shadowIndex) {
        int originIconSize = ICON_SIZE_ORIGIN;
        //format img with size
        Bitmap img = BitmapUtils.allNewBitmap(src);
        if (src.getWidth() != originIconSize || src.getHeight() != originIconSize) {
            img = BitmapUtils.getSquareBitmap(img, originIconSize);
        }
        //ICON_SIZE_WITH_SHADOW;//
        int shadowIconSize = ICON_SIZE_WITH_SHADOW;
        Bitmap iconWithShadow = attachMultiShadow(img, shadowIndex, originIconSize, shadowIconSize);
        img.recycle();
        return iconWithShadow;
    }

    private static Bitmap attachMultiShadow(Bitmap src, int shadowIndex, int originIconSize, int shadowIconSize) {
        int[] radius = ICON_SHADOW_RADIUS;
        int[] color = ICON_SHADOW_COLOR[shadowIndex];
        Bitmap dest = Bitmap.createBitmap(shadowIconSize, shadowIconSize, Bitmap.Config.ARGB_8888);
        float deltaX = (shadowIconSize - originIconSize) / 2;
        float deltaY = deltaX / 2;

        Paint paint = new Paint();
        paint.setAntiAlias(true);
        Canvas canvas = new Canvas(dest);
        for (int i = 0; i < radius.length; i++) {
            Bitmap shadow = getOutlineFill(src, radius[i], color[i]);
            float x = (shadowIconSize - shadow.getWidth()) / 2;
            float y = deltaY + Math.round(Math.sqrt(radius[i]));
            canvas.drawBitmap(shadow, x, y, paint);
            shadow.recycle();
        }
        canvas.drawBitmap(src, deltaX, deltaY, paint);
        return dest;
    }

    public static Bitmap getOutlineFill(Bitmap src, float r, int alpha) {
        HolographicOutlineHelper mOutlineHelper = new HolographicOutlineHelper();
        final int outlineColor = alpha;
        final Bitmap b = Bitmap.createBitmap(src.getWidth() + 2 * Math.round(r),
                src.getHeight() + 2 * Math.round(r),
                Bitmap.Config.ARGB_8888);

        Canvas canvas = new Canvas(b);
        canvas.drawBitmap(src, Math.round(r), Math.round(r), new Paint());
        Bitmap dest = mOutlineHelper.applyCustomExpensiveOutlineWithBlur(b, canvas, outlineColor, outlineColor, r);
        canvas.setBitmap(null);
        return dest;
    }

    public Bitmap getOutline(Context context, Bitmap src, int innerColor, int outerColor, int radius) {
        Bitmap dest = Bitmap.createBitmap(src.getWidth(), src.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(dest);
        Paint paint = new Paint();
        BlurMaskFilter blurFilter = new BlurMaskFilter(radius, BlurMaskFilter.Blur.OUTER);
        Paint shadowPaint = new Paint();
        shadowPaint.setMaskFilter(blurFilter);
        int[] outerPosition = new int[2];
        Bitmap shadowBitmap = src.extractAlpha(shadowPaint, outerPosition);
        paint.setColor(outerColor);
        canvas.drawBitmap(shadowBitmap, outerPosition[0], outerPosition[1], paint);

        BlurMaskFilter blurFilter2 = new BlurMaskFilter(radius, BlurMaskFilter.Blur.INNER);
        Paint shadowPaint2 = new Paint();
        shadowPaint.setMaskFilter(blurFilter2);
        int[] innerPosition = new int[2];
        Bitmap shadowBitmap2 = src.extractAlpha(shadowPaint2, innerPosition);
        paint.setColor(innerColor);
        canvas.drawBitmap(shadowBitmap2, -innerPosition[0], -innerPosition[1], paint);
        return dest;
    }

    public BlurMaskFilter getBlurMaskFilter(float scale, float radius) {
        return BlurMaskFilterSmt.getInstance().newBlurMaskFilter(scale * radius, BlurMaskFilter.Blur.NORMAL, BlurMaskFilter.Quality.HIGH_QUALITY);
    }
}
