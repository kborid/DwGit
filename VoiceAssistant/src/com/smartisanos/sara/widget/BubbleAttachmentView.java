package com.smartisanos.sara.widget;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.service.onestep.GlobalBubbleAttach;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.smartisanos.ideapills.common.util.ImageLoadHelper;
import com.smartisanos.ideapills.common.widget.SmartisanPopupList;
import com.smartisanos.ideapills.common.widget.drawable.RoundedDrawable;
import com.smartisanos.sara.R;
import com.smartisanos.sara.util.AttachmentUtils;
import com.smartisanos.ideapills.common.util.BitmapUtils;
import com.smartisanos.sara.util.LogUtils;

import java.lang.ref.SoftReference;
import java.util.ArrayList;
import java.util.List;
import com.smartisanos.ideapills.common.widget.OtherIconView;
import com.smartisanos.ideapills.common.util.AttachmentUtil;

public class BubbleAttachmentView extends FrameLayout implements View.OnClickListener{

    private static Recycler sRecycler = null;
    private static ImageLoadHelper<GlobalBubbleAttach, Drawable> sImageLoader = new ImageLoadHelper<>();
    private BubbleAttachmentLayout mParent;
    private GlobalBubbleAttach mAttachment;
    private ImageLoadHelper.LoadTask<GlobalBubbleAttach, Drawable> mLoadTask = null;
    private int mImageWidth;
    private int mImageHeight;
    private int mFileThumbIconSize;
    private View mImage = null;
    private SmartisanPopupList mPopupList;

    public BubbleAttachmentView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public BubbleAttachmentView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        int outlineWidth = BitmapUtils.dp2px(2);
        mImageWidth = getResources().getDimensionPixelSize(R.dimen.bubble_attachment_image_width) - outlineWidth;
        mImageHeight = getResources().getDimensionPixelSize(R.dimen.bubble_attachment_image_height) - outlineWidth;
        mFileThumbIconSize = getResources().getDimensionPixelOffset(R.dimen.bubble_attachment_icon_size);
        mImage = findViewById(R.id.v_image);
        if (mImage != null) {
            mImage.setOnClickListener(this);
            bindOperateMenu(mImage);
        }
    }

    public void finishPopup() {
        if (mPopupList != null) {
            mPopupList.hidePopupListWindow();
        }
    }

    @Override
    protected void dispatchVisibilityChanged(View changedView, int visibility) {
        super.dispatchVisibilityChanged(changedView, visibility);
        if (visibility != VISIBLE) {
            finishPopup();
        }
    }

    private void bindOperateMenu(View v) {
        List<String> itemStringList = new ArrayList<String>();
        itemStringList.add(getResources().getString(R.string.bubble_remove));
        itemStringList.add(getResources().getString(R.string.bubble_share));
        mPopupList = new SmartisanPopupList(getContext());
        mPopupList.bind(v, itemStringList, new SmartisanPopupList.PopupItemListener() {

            @Override
            public void onPopupItemClick(int position) {
                if (position == 0) {
                    mParent.onDeleteClick(BubbleAttachmentView.this);
                } else if (position == 1) {
                    mParent.onShareClick(BubbleAttachmentView.this);
                }
            }
        });
    }

    public void bindParent(BubbleAttachmentLayout layout) {
        mParent = layout;
    }

    public void setAttachment(final GlobalBubbleAttach item) {
        if (mAttachment == item) {
            return;
        }
        mAttachment = item;
        if (!mAttachment.isNeedDel() && mImage != null) {
            mImage.setOnLongClickListener(null);
        }
        Drawable bmp = sImageLoader.getImage(mAttachment);
        if (bmp != null) {
            if (mAttachment.getType() == GlobalBubbleAttach.TYPE_IMAGE) {
                showImage(bmp);
            } else {
                showFileThumbIcon(bmp, item);
            }
        } else {
            if (mAttachment.getType() == GlobalBubbleAttach.TYPE_IMAGE) {
                if (mAttachment.getStatus() == GlobalBubbleAttach.STATUS_SUCCESS) {
                    if (mLoadTask != null) {
                        sImageLoader.removeTask(mLoadTask);
                    }
                    mLoadTask = new ImageLoadHelper.LoadTask<GlobalBubbleAttach, Drawable>(mAttachment) {
                        public Drawable doInBackground(GlobalBubbleAttach obj) {
                            Uri imageUri = obj.getUri();
                            String absolutePath;
                            if (null != mAttachment.getLocalUri()) {
                                absolutePath = mAttachment.getLocalUri().getPath();
                            } else {
                                absolutePath = imageUri.getPath();
                            }
                            int rotate = BitmapUtils.getPicExifOrientation(absolutePath);
                            Bitmap bitmap = BitmapUtils.getBitmapAtRightSize(getContext(), mImageWidth, mImageHeight, imageUri, rotate);
                            if (bitmap == null) {
                                return null;
                            } else {
                                return RoundedDrawable.fromBitmap(bitmap).setCornerRadius(
                                        BitmapUtils.dp2px(1.5f));
                            }
                        }

                        public void onLoadFinish(GlobalBubbleAttach obj, Drawable tar) {
                            if (obj == mAttachment && tar != null) {
                                showImage(tar);
                            }
                            mLoadTask = null;
                        }
                    };
                    sImageLoader.postLoadTask(mLoadTask);
                }
            } else {
                if (mLoadTask != null) {
                    sImageLoader.removeTask(mLoadTask);
                }
                mLoadTask = new ImageLoadHelper.LoadTask<GlobalBubbleAttach, Drawable>(mAttachment) {
                    public Drawable doInBackground(GlobalBubbleAttach obj) {
                        Bitmap result = obj.getThumbIcon(getContext());
                        if (result == null) {
                            return null;
                        } else {
                            Bitmap backUp = result;
                            result = BitmapUtils.getScaleToSize(backUp, mFileThumbIconSize, mFileThumbIconSize);
                            if (result != backUp) {
                                backUp.recycle();
                            }
                            return new BitmapDrawable(result);
                        }
                    }

                    public void onLoadFinish(GlobalBubbleAttach obj, Drawable tar) {
                        if (obj == mAttachment && tar != null) {
                            tar.setBounds(0, 0, mImageWidth, mImageWidth);
                            showFileThumbIcon(tar, item);
                        }
                        mLoadTask = null;
                    }
                };
                sImageLoader.postLoadTask(mLoadTask);
            }
        }
    }

    public GlobalBubbleAttach getAttachment() {
        return mAttachment;
    }

    void showImage(Drawable drawable) {
        if (mImage instanceof ImageView) {
            ((ImageView) mImage).setBackground(drawable);
        }
    }

    void showFileThumbIcon(Drawable fileIcon, GlobalBubbleAttach item) {
        if (AttachmentUtil.isSpecialType(item.getFilename()) && mImage instanceof OtherIconView) {
            ((OtherIconView)mImage).setText(AttachmentUtil.getFilenameExtension(item.getFilename()));
        } else if (mImage instanceof ImageView) {
            ((ImageView)mImage).setImageBitmap(((BitmapDrawable)fileIcon).getBitmap());
        }
    }

    public static class Recycler {
        List<SoftReference<BubbleAttachmentView>> mCachesImage = new ArrayList<SoftReference<BubbleAttachmentView>>();
        List<SoftReference<BubbleAttachmentView>> mCachesFile = new ArrayList<SoftReference<BubbleAttachmentView>>();

        public void recycleBubbleAttachmentView(BubbleAttachmentView view) {
            switch (view.mAttachment.getType()) {
                case GlobalBubbleAttach.TYPE_FILE:
                    mCachesFile.add(new SoftReference<BubbleAttachmentView>(view));
                    break;
                case GlobalBubbleAttach.TYPE_IMAGE:
                    mCachesImage.add(new SoftReference<BubbleAttachmentView>(view));
                    break;
            }
        }

        public BubbleAttachmentView getBubbleAttachmentView(Context context, int type, String fileName) {
            List<SoftReference<BubbleAttachmentView>> cache = null;
            switch (type) {
                case GlobalBubbleAttach.TYPE_FILE:
                    cache = mCachesFile;
                    break;
                case GlobalBubbleAttach.TYPE_IMAGE:
                    cache = mCachesImage;
                    break;
            }
            if (cache != null) {
                for (int index = cache.size() - 1; index >= 0; index--) {
                    SoftReference<BubbleAttachmentView> reference = cache.remove(index);
                    BubbleAttachmentView view = reference.get();
                    if (view != null) {
                        return view;
                    }
                }
            }
            switch (type) {
                case GlobalBubbleAttach.TYPE_FILE:
                    if (AttachmentUtil.isSpecialType(fileName)) {
                        return (BubbleAttachmentView)View.inflate(context, R.layout.bubble_attachment_other_file, null);
                    } else {
                        return (BubbleAttachmentView)View.inflate(context, R.layout.bubble_attachment_file, null);
                    }
                case GlobalBubbleAttach.TYPE_IMAGE:
                    return (BubbleAttachmentView)View.inflate(context, R.layout.bubble_attachment_image, null);
            }
            return null;
        }
    }

    public static Recycler getRecycler() {
        if (sRecycler == null) {
            sRecycler = new Recycler();
        }
        return sRecycler;
    }



    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.v_image:
                if (GlobalBubbleAttach.TYPE_IMAGE == mAttachment.getType()) {
                    mParent.onImageClick(this);
                } else {
                    mParent.onFileClick(this);
                }
                break;
        }
    }

    public View getImage() {
        return mImage;
    }
}
