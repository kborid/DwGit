package com.smartisanos.sara.bubble.revone.widget;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff.Mode;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;

import smartisanos.util.NameAvatarController;

import com.smartisanos.sara.R;

public class AvatarImageView extends ImageView {

    private String mDrawName;
    private Bitmap mAvatarBase;
    private Paint mDrawPaint;
    private NameAvatarController mNameAvatarController;
    private Rect mRect;
    private int mDefaultAvatarId = -1;
    private Drawable mDefaultAvatar;
    private Drawable mMask;

    public AvatarImageView(Context context) {
        this(context, null);
    }

    public AvatarImageView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public AvatarImageView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (mRect == null) {
            mRect = new Rect(0, 0, getWidth(), getHeight());
        }
        Bitmap bm = null;
        if (getDrawable() != null && getDrawable() instanceof BitmapDrawable) {
            bm = ((BitmapDrawable) getDrawable()).getBitmap();
        } else {
            Drawable drawable = getDefaultAvatar();
            if (drawable != null && drawable instanceof BitmapDrawable) {
                bm = ((BitmapDrawable) drawable).getBitmap();
            } else if (!TextUtils.isEmpty(mDrawName)) {
                bm = getAvatarBase();
                mNameAvatarController.drawName(mDrawName, bm);
            }
        }
        if (bm != null) {
            drawCircle(canvas);
            canvas.drawBitmap(bm, null, mRect, mDrawPaint);
        }
        if (mMask != null) {
            mMask.setBounds(mRect);
            mMask.draw(canvas);
        }
    }

    private void drawCircle(Canvas canvas) {
        mDrawPaint.reset();
        mDrawPaint.setAntiAlias(true);
        mDrawPaint.setColor(0xFFFF0000);

        canvas.drawCircle(getWidth() / 2f, getHeight() / 2f, getWidth() / 2f, mDrawPaint);
        mDrawPaint.setXfermode(new PorterDuffXfermode(Mode.SRC_IN));
    }

    private void init() {
        mDrawPaint = new Paint();
        setLayerType(View.LAYER_TYPE_HARDWARE, null);
        mNameAvatarController = NameAvatarController.getInstances(getContext());
        mMask = mContext.getDrawable(R.drawable.revone_avatar_mask);
    }

    private Bitmap getAvatarBase() {
        if (mAvatarBase == null) {
            mAvatarBase = Bitmap.createBitmap(getWidth(), getHeight(), Config.ARGB_8888);
        }
        return mAvatarBase;
    }

    public void setName(String name) {
        name = NameAvatarController.getContactPhotoName(getContext(), name);
        if (!TextUtils.equals(name, mDrawName)) {
            mDrawName = name;
            invalidate();
        }
    }

    private Drawable getDefaultAvatar() {
        if (mDefaultAvatar == null && mDefaultAvatarId > 0) {
            mDefaultAvatar = mContext.getDrawable(mDefaultAvatarId);
        }
        return mDefaultAvatar;
    }

    public void setDefaultAvatarId(int resId) {
        mDefaultAvatarId = resId;
    }
}
