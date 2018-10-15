package com.smartisanos.ideapills.common.util;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.graphics.PixelFormat;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.text.TextUtils;
import smartisanos.util.LogTag;
import java.io.InputStream;

public class BitmapUtils {
    private static final String TAG = "BitmapUtils";
    /**
     * Returns a new Bitmap copy with a center-crop effect a la
     * {@link android.widget.ImageView.ScaleType#CENTER_CROP}. May return the input bitmap if no
     * scaling is necessary.
     *
     * @param src original bitmap of any size
     * @param w   desired width in px
     * @param h   desired height in px
     * @return a copy of src conforming to the given width and height, or src itself if it already
     * matches the given width and height
     */
    public static Bitmap centerCrop(final Bitmap src, final int w, final int h) {
        return centerCrop(src, w, h, 0);
    }

    public static Bitmap centerCrop(final Bitmap src, final int w, final int h, final int rotate) {
        return crop(src, w, h, rotate, 0.5f, 0.5f);
    }

    /**
     * Returns a new Bitmap copy with a crop effect depending on the crop anchor given. 0.5f is like
     * {@link android.widget.ImageView.ScaleType#CENTER_CROP}. The crop anchor will be be nudged
     * so the entire cropped bitmap will fit inside the src. May return the input bitmap if no
     * scaling is necessary.
     * <p>
     * <p>
     * Example of changing verticalCenterPercent:
     * _________            _________
     * |         |          |         |
     * |         |          |_________|
     * |         |          |         |/___0.3f
     * |---------|          |_________|\
     * |         |<---0.5f  |         |
     * |---------|          |         |
     * |         |          |         |
     * |         |          |         |
     * |_________|          |_________|
     *
     * @param src                     original bitmap of any size
     * @param w                       desired width in px
     * @param h                       desired height in px
     * @param horizontalCenterPercent determines which part of the src to crop from. Range from 0
     *                                .0f to 1.0f. The value determines which part of the src
     *                                maps to the horizontal center of the resulting bitmap.
     * @param verticalCenterPercent   determines which part of the src to crop from. Range from 0
     *                                .0f to 1.0f. The value determines which part of the src maps
     *                                to the vertical center of the resulting bitmap.
     * @return a copy of src conforming to the given width and height, or src itself if it already
     * matches the given width and height
     */
    public static Bitmap crop(final Bitmap src, final int w, final int h,
                              final float horizontalCenterPercent, final float verticalCenterPercent) {
        return crop(src, w, h, 0, horizontalCenterPercent, verticalCenterPercent);
    }

    public static Bitmap crop(final Bitmap src, final int w, final int h, final int rotate,
                              final float horizontalCenterPercent, final float verticalCenterPercent) {
        if (horizontalCenterPercent < 0 || horizontalCenterPercent > 1 || verticalCenterPercent < 0
                || verticalCenterPercent > 1) {
            throw new IllegalArgumentException(
                    "horizontalCenterPercent and verticalCenterPercent must be between 0.0f and "
                            + "1.0f, inclusive.");
        }
        final int srcWidth = (rotate == 90 || rotate == 270) ? src.getHeight() : src.getWidth();
        final int srcHeight = (rotate == 90 || rotate == 270) ? src.getWidth() : src.getHeight();

        // exit early if no resize/crop needed
        if (w == srcWidth && h == srcHeight) {
            return src;
        }

        final Matrix m = new Matrix();
        final float scale = Math.max(
                (float) w / srcWidth,
                (float) h / srcHeight);
        m.setScale(scale, scale);
        if (rotate > 0) {
            m.postRotate(rotate);
        }

        final int srcCroppedW, srcCroppedH;
        int srcX, srcY;

        srcCroppedW = Math.round(w / scale);
        srcCroppedH = Math.round(h / scale);
        srcX = (int) (srcWidth * horizontalCenterPercent - srcCroppedW / 2);
        srcY = (int) (srcHeight * verticalCenterPercent - srcCroppedH / 2);

        // Nudge srcX and srcY to be within the bounds of src
        srcX = Math.max(Math.min(srcX, srcWidth - srcCroppedW), 0);
        srcY = Math.max(Math.min(srcY, srcHeight - srcCroppedH), 0);

        Bitmap cropped = null;
        // if rotate is 90 and 270, and need change width and height
        if (rotate == 90 || rotate == 270) {
            cropped = Bitmap.createBitmap(src, srcY, srcX, srcCroppedH, srcCroppedW, m, true /* filter */);
        } else {
            cropped = Bitmap.createBitmap(src, srcX, srcY, srcCroppedW, srcCroppedH, m, true /* filter */);
        }

        if (w != cropped.getWidth() || h != cropped.getHeight()) {
            LogTag.e(TAG, "last center crop violated assumptions.");
        }
        return cropped;
    }

    public static Bitmap getBitmapAtRightSize(Context context, int width, int height, Uri uri, int rotate) {
        Bitmap result = null;
        if (uri != null && ("file".equals(uri.getScheme()) || "content".equals(uri.getScheme()))) {
            LogTag.e(TAG, "uri = " + uri);
            if ("file".equals(uri.getScheme())) {
                result = decodeBitmapFromFile(width, height, uri, rotate);
            } else if ("content".equals(uri.getScheme())) {
                result = decodeBitmapFromContent(context, width, height, uri, rotate);
            }
        } else {
            LogTag.e(TAG, "uri must not be null");
        }
        return result;
    }

    public static Bitmap getBitmapAtRightSize(Context context, int width, int height, Uri uri) {
        return getBitmapAtRightSize(context, width, height, uri, 0);
    }

    private static Bitmap decodeBitmapFromFile(int width, int height, Uri uri, int rotate) {
        Bitmap result = null;
        if (uri != null && "file".equals(uri.getScheme())) {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(uri.getPath(), options);
            if (options.outWidth == -1 || options.outHeight == -1) {
                LogTag.e(TAG, "decodeBitmapFromFile options.outWidth == -1 || options.outHeight == -1 ");
                return null;
            }
            options.inSampleSize = Math.min(options.outWidth / width, options.outHeight / height);
            options.inJustDecodeBounds = false;
            Bitmap bmp = BitmapFactory.decodeFile(uri.getPath(), options);
            if (bmp != null && (width != options.outWidth || height != options.outHeight)) {
                result = centerCrop(bmp, width, height, rotate);
                bmp.recycle();
            } else {
                result = bmp;
            }
        } else {
            LogTag.e(TAG, "uri must not be null");
        }
        return result;
    }

    private static Bitmap decodeBitmapFromContent(Context context, int width, int height, Uri uri, int rotate) {

        BitmapFactory.Options options = new BitmapFactory.Options();
        InputStream decodeTrueInSteam = null;
        InputStream decodeFalseInSteam= null;
        Bitmap result = null;

        try {
            options.inJustDecodeBounds = true;
            decodeTrueInSteam = context.getContentResolver().openInputStream(uri);
            BitmapFactory.decodeStream(decodeTrueInSteam, null, options);
            AttachmentUtil.quietClose(decodeTrueInSteam);

            if (options.outWidth == -1 || options.outHeight == -1) {
                LogTag.e(TAG, "decodeBitmapFromContent options.outWidth == -1 || options.outHeight == -1 ");
                return null;
            }
            options.inSampleSize = Math.min(options.outWidth / width, options.outHeight / height);
            options.inJustDecodeBounds = false;
            decodeFalseInSteam = context.getContentResolver().openInputStream(uri);
            Bitmap bmp =BitmapFactory.decodeStream(decodeFalseInSteam, null, options);

            if (bmp != null && (width != options.outWidth || height != options.outHeight)) {
                result = centerCrop(bmp, width, height, rotate);
                bmp.recycle();
            } else {
                result = bmp;
            }

        } catch (Exception e) {
            e.printStackTrace();
            LogTag.e(TAG, "decodeBitmapFromFile Exception");
        } finally {
            AttachmentUtil.quietClose(decodeFalseInSteam);
            AttachmentUtil.quietClose(decodeTrueInSteam);
        }
        return result;
    }

    public static int getPicExifOrientation(String path) {
        int rotation = 0;
        if (!TextUtils.isEmpty(path)) {
            try {
                ExifInterface exif = new ExifInterface(path);
                int exifOrientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
                switch (exifOrientation) {
                    case ExifInterface.ORIENTATION_NORMAL:
                        rotation = 0;
                        break;
                    case ExifInterface.ORIENTATION_ROTATE_90:
                        rotation = 90;
                        break;
                    case ExifInterface.ORIENTATION_ROTATE_180:
                        rotation = 180;
                        break;
                    case ExifInterface.ORIENTATION_ROTATE_270:
                        rotation = 270;
                        break;
                }
            } catch (Exception e) {
                // ignore
            }
        }
        return rotation;
    }

    public static Bitmap getScaleToSize(final Bitmap src, final int w, final int h) {
        float scale = 1.0f;
        if (src.getWidth() > src.getHeight()) {
            scale = scale * w / src.getWidth();
        } else {
            scale = scale * h / src.getHeight();
        }
        int width = (int) (src.getWidth() * scale);
        int height = (int) (src.getHeight() * scale);
        return Bitmap.createScaledBitmap(src, width, height, true);
    }

    public static int dp2px(float value) {
        final float scale = Resources.getSystem().getDisplayMetrics().density;
        return (int) (value * scale + 0.5f);
    }

    public static Bitmap drawableToBitmap(Drawable drawable, int width, int height) {
        Bitmap bitmap = Bitmap.createBitmap(width, height, drawable.getOpacity() != PixelFormat.OPAQUE ? Bitmap.Config.ARGB_8888 : Bitmap.Config.RGB_565);
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, width, height);
        drawable.draw(canvas);
        return bitmap;
    }

    public static Bitmap allNewBitmap(Bitmap src) {
        if (src == null || src.getWidth() <= 0 || src.getHeight() <= 0) {
            return null;
        }
        Config config = src.getConfig();
        if (config == null) {
            config = Bitmap.Config.RGB_565;
        }
        Bitmap bitmap = Bitmap.createBitmap(src.getWidth(), src.getHeight(), config);
        Canvas canvas = new Canvas(bitmap);
        canvas.drawBitmap(src, 0, 0, null);
        return bitmap;
    }

    public static Bitmap drawableToBitmap(Drawable drawable) {
        return drawableToBitmap(drawable, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
    }

    public static Bitmap getSquareBitmap(String filePath, int size) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        Bitmap bitmap = null;
        bitmap = BitmapFactory.decodeFile(filePath, options);
        options.inSampleSize = options.outHeight > options.outWidth ? options.outHeight / size
                : options.outWidth / size;
        options.inJustDecodeBounds = false;
        bitmap = BitmapFactory.decodeFile(filePath, options);

        if (bitmap == null) {
            return null;
        }

        return getSquareBitmap(bitmap, size);
    }

    public static Bitmap getSquareBitmap(Bitmap bitmap, int size) {
        if (bitmap == null) {
            return null;
        }
        bitmap = getSquareBitmap(bitmap);
        if (bitmap.getWidth() != size) {
            Bitmap newBp = Bitmap.createScaledBitmap(bitmap, size, size, true);
            bitmap.recycle();
            bitmap = newBp;
        }
        return bitmap;
    }

    public static Bitmap getSquareBitmap(Bitmap bitmap) {
        if (bitmap == null) {
            return null;
        }
        if (bitmap.getWidth() != bitmap.getHeight()) {
            int minSize = bitmap.getWidth() < bitmap.getHeight() ? bitmap.getWidth() : bitmap.getHeight();
            Bitmap newBp = Bitmap.createBitmap(bitmap, (bitmap.getWidth() - minSize) / 2, (bitmap.getHeight() - minSize) / 2, minSize, minSize);
            bitmap.recycle();
            bitmap = newBp;
        }
        return bitmap;
    }
}
