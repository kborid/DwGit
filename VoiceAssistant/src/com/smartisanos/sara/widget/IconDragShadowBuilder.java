package com.smartisanos.sara.widget;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.text.TextUtils;

import com.smartisanos.sara.R;
import com.smartisanos.ideapills.common.model.ShareItem;
import com.smartisanos.sara.util.LogUtils;
import com.smartisanos.sara.util.ViewUtils;

public class IconDragShadowBuilder extends View.DragShadowBuilder {
    public static final String TAG = "VoiceAss.IconDragShadowBuilder";
    public final static float ICON_SCALE = 1.5f;
    private final Bitmap mAppIcon;
    private final Drawable mTextBackground;
    private Paint mTextPaint;
    private final int mIconSize;
    private String mAppLabel;
    private int mTextSize = 14;
    private int mTextPaddingX = 18;
    private int mTextPaddingY = 8;
    private int mTextIconGap = 16;
    private int mTextWidth;
    private int mTextHeight;
    private int mDrawableHeight;
    private int mDrawablePaddingTop;

    public IconDragShadowBuilder(ShadowBitmapView icon) {
        Context context = icon.getContext();
        mAppIcon = icon.getShadowBitmap();
        ShareItem shareItem = (ShareItem) icon.getTag();
        if (shareItem != null) {
            try {
                PackageManager pkm = context.getPackageManager();
                ActivityInfo resolveInfo = pkm.getActivityInfo(shareItem.getComponentName(),
                        ActivityInfo.FLAG_STATE_NOT_NEEDED);
                mAppLabel = resolveInfo != null ? resolveInfo.loadLabel(pkm).toString() : shareItem.getDispalyName();
            } catch (NameNotFoundException e) {
                LogUtils.d(TAG, e.toString());
            }
        }

        if (TextUtils.isEmpty(mAppLabel)) {
            mAppLabel = icon.getContentDescription().toString();
        }

        mIconSize = (int) (icon.getIconSize() * ICON_SCALE);
        mTextBackground = context.getResources().getDrawable(R.drawable.float_text_bg);
        mTextSize = ViewUtils.dp2px(context, mTextSize);
        mTextPaddingX = ViewUtils.dp2px(context, mTextPaddingX);
        mTextPaddingY = ViewUtils.dp2px(context, mTextPaddingY);
        mTextIconGap = ViewUtils.dp2px(context, mTextIconGap);
        mTextPaint = new Paint();
        mTextPaint.setTextSize(mTextSize);
        mTextPaint.setColor(Color.WHITE);
        mTextPaint.setAntiAlias(true);
    }

    @Override
    public void onProvideShadowMetrics(Point size, Point touch) {
        int width = mIconSize;
        int height = mIconSize;
        if (!TextUtils.isEmpty(mAppLabel)) {
            Rect padding = new Rect();
            mTextBackground.getPadding(padding);
            mDrawablePaddingTop = padding.top;
            Rect rect = new Rect();
            mTextPaint.getTextBounds(mAppLabel, 0, mAppLabel.length(), rect);
            mTextWidth = mTextPaddingX + rect.width() + mTextPaddingX;
            if (width < mTextWidth) {
                width = mTextWidth;
            }
            mTextHeight = rect.height();
            int textHeight = mTextHeight + mTextPaddingY + mTextPaddingY;
            int drawableHeight = mTextBackground.getIntrinsicHeight();
            mDrawableHeight = drawableHeight > textHeight ? drawableHeight : textHeight;
            height += mDrawableHeight + mTextIconGap;
        }
        size.set(width, height);
        touch.set(width / 2, height - mIconSize / 2);
    }

    @Override
    public void onDrawShadow(Canvas canvas) {
        int x = 0;
        int y = 0;
        if (!TextUtils.isEmpty(mAppLabel)) {
            Rect oldBounds = mTextBackground.copyBounds();
            mTextBackground.setBounds(x, y, x + mTextWidth, y + mDrawableHeight);
            mTextBackground.draw(canvas);
            mTextBackground.setBounds(oldBounds);
            canvas.drawText(mAppLabel, x + mTextPaddingX, y + (mDrawableHeight + mTextHeight) / 2 - mDrawablePaddingTop, mTextPaint);
        }
        if (mAppIcon != null) {
            x = (canvas.getWidth() - mIconSize) / 2;
            y = mDrawableHeight + mTextIconGap;
            canvas.drawBitmap(mAppIcon, null, new Rect(x, y, x + mIconSize, y + mIconSize), null);
        }
    }
}
