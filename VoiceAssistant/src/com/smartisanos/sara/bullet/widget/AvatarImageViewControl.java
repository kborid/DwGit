
package com.smartisanos.sara.bullet.widget;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.media.ThumbnailUtils;
import com.smartisanos.sara.R;
import com.smartisanos.sara.bullet.util.PublicResource;

import smartisanos.util.NameAvatarController;

/**
 * phone contact avatar contoller
 */
public class AvatarImageViewControl {
    private static final String TAG = "AvatarImageViewControl";
    private Context mContext;
//    private Typeface mTypeface;
    private final Paint mTextPaint_Chinese;
    private final Paint mTextPaint_English;
    private static final double FONT_OFFSET_TOP = 3.3; //dip
    private static final double FONT_OFFSET_LEFT = 0.3; //dip
    private static final float FONT_SIZE = 34; //sp

    private static AvatarImageViewControl mAvatarTextPaintLoader = null;

    private int mOffsetTop = 0;
    private int mOffsetLeft = 0;

    public static boolean NEED_HARDWARD_ACC = false;
    public static String FONT_CHINESE_NAME = "FZCCHK.TTF";
    public static String FONT_ENGLIST_NAME = "Gotham-Ultra.otf";

    public static synchronized AvatarImageViewControl getInstances(Context context) {
        if (mAvatarTextPaintLoader == null) {
            mAvatarTextPaintLoader = new AvatarImageViewControl(context.getApplicationContext());
        }
        mAvatarTextPaintLoader.setFontSize(FONT_SIZE);
        return mAvatarTextPaintLoader;
    }

    private Bitmap mRoundBG = null;

    private void init() {  
        mOffsetTop = dipTopx(mContext, FONT_OFFSET_TOP);
        mOffsetLeft = dipTopx(mContext, FONT_OFFSET_LEFT);
        mRoundBG = BitmapFactory.decodeResource(mContext.getResources(), R.drawable.avatar_bag);
    }

    public int dipTopx(Context context, double dipValue) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int)(dipValue * scale + 0.5f);
    }

    private AvatarImageViewControl(Context context) {
        mContext = context.getApplicationContext();

        PublicResource.sTypeface_chinese = Typeface.createFromAsset(mContext.getAssets(),
                AvatarImageViewControl.FONT_CHINESE_NAME);
        PublicResource.sTypeface_english = Typeface.createFromAsset(mContext.getAssets(),
                AvatarImageViewControl.FONT_ENGLIST_NAME);

        mTextPaint_Chinese = new Paint();
        mTextPaint_Chinese.setColor(Color.WHITE);
        mTextPaint_Chinese.setTypeface(PublicResource.sTypeface_chinese);
        mTextPaint_Chinese.setAntiAlias(true);
        mTextPaint_Chinese.setTextSize(dipTopx(context, FONT_SIZE));

        mTextPaint_English = new Paint();
        mTextPaint_English.setColor(Color.WHITE);
        mTextPaint_English.setTypeface(PublicResource.sTypeface_english);
        mTextPaint_English.setAntiAlias(true);
        mTextPaint_English.setTextSize(dipTopx(context, FONT_SIZE));

        init();
    }

    public void setFontSize(float size){
        mTextPaint_Chinese.setTextSize(dipTopx(mContext, size));
        mTextPaint_English.setTextSize(dipTopx(mContext, size));
    }

    public void drawName(String name, Bitmap avatar) {
        boolean hasChinese = NameAvatarController.hasChinese(name);
        Paint paint = hasChinese ? mTextPaint_Chinese : mTextPaint_English;

        int width = avatar.getWidth();
        Rect rect = new Rect();
        paint.getTextBounds(name, 0, name.length(), rect);

        float x = (width - rect.width()) / 2 - rect.left;
        float y = (width + rect.height()) / 2;
        if (hasChinese) {
            x -= mOffsetLeft;
            y -= mOffsetTop;
        }

        rect = new Rect(0, 0, width, width);
        Canvas canvas = new Canvas(avatar);
        canvas.drawBitmap(mRoundBG, new Rect(0, 0, mRoundBG.getWidth(), mRoundBG.getHeight()), rect, paint);
        canvas.drawText(name, x, y, paint);
    }

    public void drawAvatarDrawable(Bitmap avatarBitmap, Bitmap avatar) {
        Paint paint = mTextPaint_English;
        int width = avatar.getWidth();
        Rect rect = new Rect(0, 0, width, width);
        Canvas canvas = new Canvas(avatar);
        canvas.drawBitmap(mRoundBG, rect, rect, paint);
        Bitmap avatarDrawableBitmap = avatarBitmap.copy(Config.ARGB_8888, true);
        avatarDrawableBitmap = ThumbnailUtils.extractThumbnail(avatarDrawableBitmap, width, width);
        canvas.drawBitmap(avatarDrawableBitmap, rect, rect, paint);
    }

    public Bitmap getAvatarBase(int width) {
        return Bitmap.createBitmap(width, width, Config.ARGB_8888);
    }

    public Bitmap createBitmapFromName(String drawText) {
        Bitmap bitmap = mRoundBG.copy(mRoundBG.getConfig(), true);

        drawName(drawText, bitmap);

        Bitmap result = NameAvatarController.getCircleBitmap(bitmap, false);
        bitmap.recycle();
        return result;
    }

}