package com.smartisanos.ideapills.view;

import android.app.SmtPCUtils;
import android.content.ContentResolver;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.smartisanos.ideapills.IdeaPillsApp;
import com.smartisanos.ideapills.R;
import com.smartisanos.ideapills.common.util.ImageLoadHelper;
import com.smartisanos.ideapills.common.widget.drawable.RoundedDrawable;
import com.smartisanos.ideapills.entity.AttachMentItem;
import com.smartisanos.ideapills.util.LOG;
import com.smartisanos.ideapills.util.ThreadVerify;
import com.smartisanos.ideapills.util.ViewUtils;
import com.smartisanos.ideapills.common.widget.SmartisanPopupList;

import java.io.File;
import java.lang.ref.SoftReference;
import java.util.ArrayList;
import java.util.List;

import smartisanos.api.ViewSmt;
import smartisanos.util.SidebarUtils;
import com.smartisanos.ideapills.common.widget.OtherIconView;
import com.smartisanos.ideapills.common.util.AttachmentUtil;
import com.smartisanos.ideapills.common.util.BitmapUtils;

public class BubbleAttachmentView extends FrameLayout implements View.OnClickListener {
    private static LOG log = LOG.getInstance(BubbleAttachmentView.class);

    private static Recycler sRecycler = null;
    private static ImageLoadHelper<AttachMentItem, Drawable> sImageLoader = new ImageLoadHelper<AttachMentItem, Drawable>();
    private BubbleAttachmentLayout mParent;
    private AttachMentItem mAttachment;
    private ImageLoadHelper.LoadTask<AttachMentItem, Drawable> mLoadTask = null;
    private int mImageWidth;
    private int mImageHeight;
    private int mFileThumbIconSize;
    private View mImage = null;
    private View mDownProgress;
    private View mDownBtn;
    private SmartisanPopupList mPopupList;
    private boolean mIsThumbnailLoadFailed = false;

    static {
        Drawable defaultImageDrawable = null;
        try {
            defaultImageDrawable = IdeaPillsApp.getInstance().getResources().getDrawable(R.drawable.attachment_default_image);
        } catch (Throwable e) {
            //ignore
        }
        sImageLoader.setDefaultImage(defaultImageDrawable);
    }

    public BubbleAttachmentView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public BubbleAttachmentView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    protected void onFinishInflate() {
        super.onFinishInflate();
        int outlineWidth = ViewUtils.dp2px(getContext(), 2);
        mImageWidth = getResources().getDimensionPixelSize(R.dimen.bubble_attachment_image_width) - outlineWidth;
        mImageHeight = getResources().getDimensionPixelSize(R.dimen.bubble_attachment_image_height) - outlineWidth;
        mFileThumbIconSize = getResources().getDimensionPixelOffset(R.dimen.bubble_attachment_icon_size);
        mImage = findViewById(R.id.v_image);
        mDownProgress = findViewById(R.id.down_progress);
        mDownBtn = findViewById(R.id.down_btn);
        if (mImage != null) {
            mImage.setOnClickListener(this);
            bindOperateMenu(mImage);
            ViewSmt.getInstance().setOnForceTouchListener(mImage, new ViewSmt.OnForceTouchListener() {
                @Override
                public void onForceTouch() {
                    if (isVisibleToUser() && mAttachment != null
                            && AttachMentItem.TYPE_IMAGE == mAttachment.getType()
                            && mAttachment.getUri() != null
                            && !mPopupList.isShowing()
                            && ContentResolver.SCHEME_FILE.equals(mAttachment.getUri().getScheme())) {
                        SidebarUtils.dragImage(mImage,
                                mContext, null,
                                new File(mAttachment.getUri().getPath()),
                                mAttachment.getContentType(), true);
                    }
                }
            });
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

    protected void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
//        if (mPopupList != null) {
//            List<String> itemStringList = new ArrayList<String>();
//            itemStringList.add(getResources().getString(R.string.bubble_remove));
//            itemStringList.add(getResources().getString(R.string.bubble_share));
//            mPopupList.layoutMainPanelItems(itemStringList);
//        }
        LOG.d("onConfigurationChanged");
        finishPopup();
        if (mPopupList != null) {
            mPopupList.updateWidthAndHeight();
            mPopupList.resetPopupWindow();
        }
    }

    public void bindParent(BubbleAttachmentLayout layout) {
        mParent = layout;
    }

    public void refresh() {
        if (mAttachment == null) {
            return;
        }
        if (mPopupList != null) {
            mPopupList.disable(mAttachment.getDownloadStatus() != AttachMentItem.DOWNLOAD_STATUS_DOWNLOAD_SUCCESS);
        }
        if (mAttachment.getDownloadStatus() == AttachMentItem.DOWNLOAD_STATUS_DOWNLOADING) {
            mDownProgress.setVisibility(VISIBLE);
            mDownBtn.setVisibility(GONE);
        } else if (mAttachment.getDownloadStatus() == AttachMentItem.DOWNLOAD_STATUS_DOWNLOAD_FAIL
                || mAttachment.getDownloadStatus() == AttachMentItem.DOWNLOAD_STATUS_NOT_DOWNLOAD) {
            mDownProgress.setVisibility(GONE);
            mDownBtn.setVisibility(VISIBLE);
        } else {
            mDownProgress.setVisibility(GONE);
            mDownBtn.setVisibility(GONE);
        }

        Drawable bmp = sImageLoader.getImage(mAttachment);
        if (bmp != null) {
            if (mAttachment.getType() == AttachMentItem.TYPE_IMAGE) {
                showImage(bmp);
            } else {
                showFileThumbIcon(bmp);
            }
        } else {
            if (mAttachment.getType() == AttachMentItem.TYPE_IMAGE) {
                if (mAttachment.getStatus() == AttachMentItem.STATUS_SUCCESS) {
                    if (mLoadTask != null) {
                        sImageLoader.removeTask(mLoadTask);
                    }
                    if (mAttachment.getUri() != null && mAttachment.getDownloadStatus() == AttachMentItem.DOWNLOAD_STATUS_DOWNLOAD_SUCCESS) {
                        showImage(sImageLoader.getDefaultImage());
                        mLoadTask = new ImageLoadHelper.LoadTask<AttachMentItem, Drawable>(mAttachment) {
                            public Drawable doInBackground(AttachMentItem obj) {
                                Uri imageUri = obj.getUri();
                                if (imageUri != null) {
                                    int rotate = 0;
                                    if (!TextUtils.isEmpty(imageUri.getPath())) {
                                        rotate = BitmapUtils.getPicExifOrientation(imageUri.getPath());
                                    }
                                    Bitmap bitmap = BitmapUtils.getBitmapAtRightSize(mContext, mImageWidth, mImageHeight, imageUri, rotate);
                                    if (bitmap == null) {
                                        return null;
                                    } else {
                                        return RoundedDrawable.fromBitmap(bitmap).setCornerRadius(
                                                ViewUtils.dp2px(getContext(), 1.5f));
                                    }
                                }
                                return null;
                            }

                            public void onLoadFinish(AttachMentItem obj, Drawable tar) {
                                if (tar == null) {
                                    mIsThumbnailLoadFailed = true;
                                }
                                if (obj == mAttachment  && tar != null) {
                                    showImage(tar);
                                }
                                mLoadTask = null;
                            }
                        };
                        sImageLoader.postLoadTask(mLoadTask);
                    } else {
                        showImage(sImageLoader.getDefaultImage());
                    }

                } else {
                    showImage(sImageLoader.getDefaultImage());
                }
            } else {
                if (mLoadTask != null) {
                    sImageLoader.removeTask(mLoadTask);
                }
                mLoadTask = new ImageLoadHelper.LoadTask<AttachMentItem, Drawable>(mAttachment) {
                    public Drawable doInBackground(AttachMentItem obj) {
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

                    public void onLoadFinish(AttachMentItem obj, Drawable tar) {
                        if (obj == mAttachment && tar != null) {
                            tar.setBounds(0, 0, mImageWidth, mImageWidth);
                            showFileThumbIcon(tar);
                        }
                        mLoadTask = null;
                    }
                };
                sImageLoader.postLoadTask(mLoadTask);
            }
        }
    }

    public void setAttachment(AttachMentItem item) {
        mAttachment = item;
        refresh();
    }

    public AttachMentItem getAttachment() {
        return mAttachment;
    }

    void showImage(Drawable drawable) {
        if (mImage instanceof ImageView) {
            ((ImageView) mImage).setImageDrawable(drawable);
        }
    }

    void showFileThumbIcon(Drawable fileIcon) {
        if (AttachmentUtil.isSpecialType(mAttachment.getFilename()) && mImage instanceof OtherIconView) {
            ((OtherIconView)mImage).setText(AttachmentUtil.getFilenameExtension(mAttachment.getFilename()));
        } else if (mImage instanceof ImageView) {
            ((ImageView)mImage).setImageBitmap(((BitmapDrawable)fileIcon).getBitmap());
        }
    }

    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.v_image:
                mParent.onAttachmentClick(this);
                break;
        }
    }

    public static class Recycler {
        List<SoftReference<BubbleAttachmentView>> mCachesImage = new ArrayList<SoftReference<BubbleAttachmentView>>();
        List<SoftReference<BubbleAttachmentView>> mCachesFile = new ArrayList<SoftReference<BubbleAttachmentView>>();

        public void clear() {
            mCachesImage = new ArrayList<>();
            mCachesFile = new ArrayList<>();
        }

        public void recycleBubbleAttachmentView(BubbleAttachmentView view) {
            if (view == null) {
                return;
            }
            view.setAlpha(1f);
            view.setScaleX(1f);
            view.setScaleY(1f);
            switch (view.mAttachment.getType()) {
                case AttachMentItem.TYPE_FILE:
                    mCachesFile.add(new SoftReference<BubbleAttachmentView>(view));
                    break;
                case AttachMentItem.TYPE_IMAGE:
                    mCachesImage.add(new SoftReference<BubbleAttachmentView>(view));
                    break;
            }
        }

        public BubbleAttachmentView getBubbleAttachmentView(Context context, int type, String fileName) {
            List<SoftReference<BubbleAttachmentView>> cache = null;
            switch (type) {
                case AttachMentItem.TYPE_FILE:
                    cache = mCachesFile;
                    break;
                case AttachMentItem.TYPE_IMAGE:
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
                case AttachMentItem.TYPE_FILE:
                    if (AttachmentUtil.isSpecialType(fileName)) {
                        return (BubbleAttachmentView)View.inflate(context, R.layout.bubble_attachment_other_file, null);
                    } else {
                        return (BubbleAttachmentView)View.inflate(context, R.layout.bubble_attachment_file, null);
                    }
                case AttachMentItem.TYPE_IMAGE:
                    return (BubbleAttachmentView) View.inflate(context, R.layout.bubble_attachment_image, null);
            }
            return null;
        }
    }

    public static Recycler getRecycler() {
        ThreadVerify.enforceUiThread();
        if (sRecycler == null) {
            sRecycler = new Recycler();
        }
        return sRecycler;
    }

    public static void releaseCaches() {
        if (sRecycler != null) {
            sRecycler.clear();
        }
        sImageLoader.clear();
    }

    public View getImage() {
        return mImage;
    }

    public void refreshView() {
        if (!mIsThumbnailLoadFailed) {
            return;
        }
        refresh();
        mIsThumbnailLoadFailed = false;
    }
}
