/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.smartisanos.sara.bullet.widget;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff.Mode;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;
import com.smartisanos.sara.R;

import smartisanos.util.NameAvatarController;

public class AvatarImageView extends ImageView {
    private final static String TAG = "AvatarImageView";
    private long mPhotoID = 0;
    private String mDrawName = null;
    private AvatarImageViewControl mAvatarTextPaintControl = null;
    private Bitmap mAvatar = null;
    private Paint mDrawPaint = new Paint();
    private boolean mUseDefaultAvatar = true;

    private Bitmap mAvatarBitmap = null;
    private Rect mRect = new Rect();
    private Rect mSRect = new Rect();
    private PorterDuffXfermode mPorterDuffXfermode = new PorterDuffXfermode(Mode.SRC_IN);
    public AvatarImageView(Context context) {
        super(context);
        init();
    }

    public AvatarImageView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public AvatarImageView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    private void init() {
        if (!AvatarImageViewControl.NEED_HARDWARD_ACC) {
            setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        }
        mAvatarTextPaintControl = AvatarImageViewControl.getInstances(getContext());
    }

    public void setName(String name) {
        mDrawName = NameAvatarController.getContactPhotoName(getContext(), name);
        mUseDefaultAvatar = NameAvatarController.isDefaultName(getContext(), mDrawName);
        invalidate();
    }

    public void setPhotoID(long photoID) {
        mPhotoID = photoID;
    }

    public long getPhotoID() {
        return mPhotoID;
    }

    public void setAvatarBitmap(Bitmap avatarBitmap) {
        mAvatarBitmap = avatarBitmap;
        invalidate();
    }

    public void setmUseDefaultAvatar(boolean mUseDefaultAvatar) {
        this.mUseDefaultAvatar = mUseDefaultAvatar;
    }

    public Bitmap loadDefaultAvatarPhoto(Context context, boolean hires, boolean darkTheme) {
        return BitmapFactory.decodeResource(context.getResources(),
                R.drawable.photo_default_in_phone);
    }

    public void setNameTextSize(float size) {
        if (mAvatarTextPaintControl != null) {
            mAvatarTextPaintControl.setFontSize(size);
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (mPhotoID > 0) {
            super.onDraw(canvas);
        } else {
            int width = getWidth();
            mRect.right = width;
            mRect.bottom = width;

            boolean drawDefaultAvater = true;
            if (mUseDefaultAvatar) {
                if (mAvatarBitmap != null) {
                    if (mAvatar == null) {
                        mAvatar = mAvatarTextPaintControl.getAvatarBase(width);
                    }

                    mAvatarTextPaintControl.drawAvatarDrawable(mAvatarBitmap, mAvatar);
                    drawDefaultAvater = false;
                } else {
                    Bitmap defaultAvatar = loadDefaultAvatarPhoto(getContext(), true, false);
                    mSRect.right = defaultAvatar.getWidth();
                    mSRect.bottom = defaultAvatar.getHeight();
                    canvas.drawBitmap(defaultAvatar,
                            mSRect, mRect, null);
                }
            } else {
                if (mAvatar == null) {
                    mAvatar = mAvatarTextPaintControl.getAvatarBase(width);
                }

                mAvatarTextPaintControl.drawName(mDrawName, mAvatar);
                drawDefaultAvater = false;
            }

            if (!drawDefaultAvater) {
                mDrawPaint.reset();
                mDrawPaint.setAntiAlias(true);
                mDrawPaint.setColor(0xFFFF0000);

                canvas.drawCircle(width / 2, width / 2, width / 2, mDrawPaint);
                mDrawPaint.setXfermode(mPorterDuffXfermode);

                canvas.drawBitmap(mAvatar, mRect, mRect, mDrawPaint);
            }
        }
    }

}
