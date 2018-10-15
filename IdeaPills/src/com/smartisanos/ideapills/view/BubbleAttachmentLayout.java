package com.smartisanos.ideapills.view;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.LayoutTransition;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.os.ResultReceiver;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.smartisanos.ideapills.BubbleController;
import com.smartisanos.ideapills.R;
import com.smartisanos.ideapills.common.util.UIHandler;
import com.smartisanos.ideapills.entity.AttachMentItem;
import com.smartisanos.ideapills.sync.SyncManager;
import com.smartisanos.ideapills.util.AttachmentUtils;
import com.smartisanos.ideapills.util.GlobalBubbleManager;
import com.smartisanos.ideapills.util.GlobalBubbleUtils;
import com.smartisanos.ideapills.util.LOG;
import com.smartisanos.ideapills.common.util.AttachmentUtil;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

public class BubbleAttachmentLayout extends ViewGroup {
    LOG log = LOG.getInstance(BubbleAttachmentLayout.class);
    private List<AttachMentItem> mList;
    private int mListSize = -1;
    private int mCellWidth = -1;
    private int mCellHeight = -1;
    private static final int ANIMATION_DURATION = 250;
    private LayoutTransition mLayoutTransition;
    private AttachMentItem mLastDeleteItem;
    private OnBubbleChangedListener mOnBubbleChangedListener;
    private int mGapX = 0;
    private int mGapY = 0;
    private int mChildCountPerRow = AttachmentUtils.CHILD_COUNT_PER_ROW;

    private ImageView mPptAddView;
    public BubbleAttachmentLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public BubbleAttachmentLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mGapX = context.getResources().getDimensionPixelSize(R.dimen.attachment_container_gap_x);
        mGapY = context.getResources().getDimensionPixelSize(R.dimen.attachment_container_gap_y);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        reAddPptHolder(BubbleController.getInstance().isInPptContext(getContext()));
    }

    public void setOnBubbleChangedListener(OnBubbleChangedListener listener) {
        mOnBubbleChangedListener = listener;
    }

    public void finishPopup() {
        int count = getChildCount();
        for (int i = 0; i < count; i++) {
            View child = getChildAt(i);
            if (child instanceof BubbleAttachmentView) {
                ((BubbleAttachmentView) child).finishPopup();
            }
        }
    }

    public void updatePptAddFocus(boolean hasFocus) {
        if (mPptAddView != null) {
            if (hasFocus) {
                mPptAddView.setImageResource(R.drawable.ppt_photo_add_shine);
            } else {
                mPptAddView.setImageResource(R.drawable.ppt_photo_add);
            }
        }
    }

    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) {
            View child = getChildAt(i);
            measureChild(child, widthMeasureSpec, heightMeasureSpec);
            if (mCellHeight < 0) {
                mCellHeight = child.getMeasuredHeight();
            }
            if (mCellWidth < 0) {
                mCellWidth = child.getMeasuredWidth();
            }
        }
        final int width = getMeasuredWidth();
        final int numCol = (width - getPaddingLeft() - getPaddingRight() + mGapX) / (mCellWidth + mGapX);
        if (numCol > 0) {
            mChildCountPerRow = Math.min(numCol, AttachmentUtils.CHILD_COUNT_PER_ROW);
        }
        int row = (int) Math.ceil((float) childCount / mChildCountPerRow);
        int height = row == 0 ? 0 : row * mCellHeight + (row - 1) * mGapY + getPaddingTop() + getPaddingBottom();
        setMeasuredDimension(width, height);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        final int childCount = getChildCount();
        int x = getPaddingLeft();
        int y = getPaddingTop();
        final int cellWidth = mCellWidth;
        final int cellHeight = mCellHeight;
        int col = 0;
        int row = 0;
        for (int i = 0; i < childCount; i++) {
            col = i % mChildCountPerRow;
            row = i / mChildCountPerRow;
            if (col == 0) {
                x = getPaddingLeft();
                y = getPaddingTop() + row * (cellHeight + mGapY);
            } else {
                x += cellWidth + mGapX;
            }
            final View child = getChildAt(i);
            child.layout(x, y, x + cellWidth, y + cellHeight);
        }
    }

    public void forceRefresh() {
        mListSize = -1;
    }

    private void recycle() {
        int count = getChildCount();
        for (int i = count - 1; i >= 0; i--) {
            View view = getChildAt(i);
            if (view instanceof BubbleAttachmentView) {
                BubbleAttachmentView.getRecycler().recycleBubbleAttachmentView((BubbleAttachmentView) view);
                removeViewAt(i);
            }
        }
    }

    public void setAttachmentList(List<AttachMentItem> list) {
        if (mList == list && (list != null && mListSize == list.size())) {
            return;
        }
        recycle();
        mList = list;
        if (mList != null) {
            mListSize = mList.size();
            // Utils.sortAttachments(mList);
            for (AttachMentItem item : mList) {
                addAttachMent(item);
            }
        }
    }

    public void addAttachMent(AttachMentItem item) {
        BubbleAttachmentView view = BubbleAttachmentView.getRecycler().getBubbleAttachmentView(getContext(), item.getType(), item.getFilename());
        if (view != null) {
            view.bindParent(this);
            view.setAttachment(item);
            addView(view);

            int type = item.getType() == AttachMentItem.TYPE_IMAGE
                    ? R.string.bubble_image : R.string.bubble_file;

            StringBuilder desc = new StringBuilder(
                    getResources().getString(R.string.bubble_attach) + getChildCount());
            desc.append(" ").append(getResources().getString(type));
            if (item.getUri() != null) {
                desc.append(" ").append(item.getUri().getLastPathSegment());
            }
            view.setContentDescription(desc.toString());
            view.setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_YES);

            View image = view.getImage();
            if (image != null) {
                image.setContentDescription(desc.toString());
            }
        }
        reAddPptHolder(BubbleController.getInstance().isInPptContext(getContext()));
    }

    public void reAddPptHolder(boolean isInPptMode) {
        if (isInPptMode) {
            if (mPptAddView == null) {
                mPptAddView = (ImageView) LayoutInflater.from(getContext()).inflate(R.layout.rev_vw_ppt_attachment_add, this, false);
                mPptAddView.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (mOnBubbleChangedListener != null) {
                            mOnBubbleChangedListener.onBubblePptAdd();
                        }
                    }
                });
            }
            removeView(mPptAddView);
            if (getChildCount() < AttachmentUtils.ATTACHMENT_QUANTITY_LIMIT) {
                addView(mPptAddView);
            }
        }
    }

    public void onShareClick(BubbleAttachmentView attachmentView) {
        AttachMentItem attachMentItem = attachmentView.getAttachment();
        if (attachMentItem.getDownloadStatus() == AttachMentItem.DOWNLOAD_STATUS_DOWNLOAD_SUCCESS) {
            GlobalBubbleUtils.shareAttachToApps(getContext(), attachMentItem.getUri(), attachMentItem.guessContentType());
        }
    }

    public void onDeleteClick(BubbleAttachmentView attachmentView) {
        if (mLayoutTransition == null) {
            mLayoutTransition = generateLayoutAnimate();
        }
        updateTransitionPrama(indexOfChild(attachmentView));
        //Each time set the animation because the animation causes The listview misplaced when swiping fast
        setLayoutTransition(mLayoutTransition);
        if (mLastDeleteItem != null) {
            GlobalBubbleManager.getInstance().removeAttachment(mLastDeleteItem);
            if (mOnBubbleChangedListener != null) {
                mOnBubbleChangedListener.onBubbleAttachDelete();
            }
            mLastDeleteItem = null;
        }
        mLastDeleteItem = attachmentView.getAttachment();
        removeView(attachmentView);
    }

    private void onDeleteAnimationEnd() {
        if (mLastDeleteItem != null) {
            GlobalBubbleManager.getInstance().removeAttachment(mLastDeleteItem);
            mLastDeleteItem = null;
            setLayoutTransition(null);
            if (mOnBubbleChangedListener != null) {
                mOnBubbleChangedListener.onBubbleAttachDelete();
            }
        }
    }

    public void onAttachmentClick(BubbleAttachmentView attachmentView) {
        AttachMentItem attachmentItem = attachmentView.getAttachment();
        if (null == attachmentItem) {
            return;
        }
        AttachmentUtils.restoreAttachmentsLocallyIfNeeded(getContext(), mList);
        if (AttachMentItem.TYPE_IMAGE == attachmentItem.getType()) {
            onImageClick(attachmentView);
        } else {
            onFileClick(attachmentView);
        }
        refreshView();
    }

    private void onImageClick(BubbleAttachmentView attachmentView) {
        AttachMentItem attachMentItem = attachmentView.getAttachment();
        if (attachMentItem.getDownloadStatus() == AttachMentItem.DOWNLOAD_STATUS_DOWNLOAD_SUCCESS) {
            ArrayList<Uri> uris = new ArrayList<Uri>();
            for (AttachMentItem item : mList) {
                if (item.getDownloadStatus() == AttachMentItem.DOWNLOAD_STATUS_DOWNLOAD_SUCCESS) {
                    if (item.getType() == AttachMentItem.TYPE_IMAGE && item.getStatus() == AttachMentItem.STATUS_SUCCESS) {
                        uris.add(item.getUri());
                    }
                }
            }
            GlobalBubbleUtils.previewImages(mContext, uris, attachMentItem);
        } else {
            SyncManager.downloadAttachment(getContext(), attachMentItem, getLoadCompleteReceiver(attachmentView));
            attachmentView.refresh();
        }
    }

    private void onFileClick(BubbleAttachmentView attachmentView) {
        AttachMentItem attachMentItem = attachmentView.getAttachment();
        if (attachMentItem.getDownloadStatus() == AttachMentItem.DOWNLOAD_STATUS_DOWNLOAD_SUCCESS) {
//            ArrayList<String> files = new ArrayList<String>();
//            int pos = 0;
//            for (AttachMentItem item : mList) {
//                if (item.getStatus() == AttachMentItem.STATUS_SUCCESS) {
//                    if (item == attachmentView.getAttachment()) {
//                        pos = files.size();
//                    }
//                    files.add(item.getUri().getPath());
//                }
//            }
//            GlobalBubbleUtils.previewImagesNew(getContext(), attachmentView, files, pos);
            GlobalBubbleUtils.openFile(getContext(), attachMentItem);
        } else {
            SyncManager.downloadAttachment(getContext(), attachMentItem, getLoadCompleteReceiver(attachmentView));
            attachmentView.refresh();
        }
    }

    private ResultReceiver getLoadCompleteReceiver(final BubbleAttachmentView attachmentView) {
        final WeakReference<BubbleAttachmentView> reference = new WeakReference<>(attachmentView);
        return new ResultReceiver(UIHandler.getHandler()) {
            @Override
            protected void onReceiveResult(int resultCode, Bundle resultData) {
                if (resultCode == 0) {
                    BubbleAttachmentView view = reference.get();
                    if (view != null && view.getVisibility() == View.VISIBLE) {
                        view.refresh();
                    }
                }
            }
        };
    }

    public interface OnBubbleChangedListener {
        void onBubbleAttachDelete();
        void onBubblePptAdd();
    }

    private void updateTransitionPrama(int removedIndex) {
        if (mLayoutTransition != null) {
            Animator changeanim = mLayoutTransition.getAnimator(LayoutTransition.CHANGE_DISAPPEARING);
            Animator disappearanim = mLayoutTransition.getAnimator(LayoutTransition.DISAPPEARING);
            if (changeanim != null) {
                changeanim.removeAllListeners();
            }
            if (disappearanim != null) {
                disappearanim.removeAllListeners();
            }
            Animator anim;
            if (mList.size() > 1 && removedIndex >= 0 && removedIndex < mList.size() - 1) {
                anim = changeanim;
            } else {
                anim = disappearanim;
            }
            if (anim != null) {
                anim.addListener(new AnimatorListenerAdapter() {
                    public void onAnimationEnd(Animator anim) {
                        onDeleteAnimationEnd();
                    }
                });
            }
        }
    }

    public LayoutTransition generateLayoutAnimate() {
        LayoutTransition mLayoutTransition = new LayoutTransition();
        //mLayoutTransition.setStagger(LayoutTransition.CHANGE_APPEARING, ANIMATION_DURATION);
        //mLayoutTransition.setStagger(LayoutTransition.CHANGE_DISAPPEARING, ANIMATION_DURATION);
        //mLayoutTransition.setStagger(LayoutTransition.APPEARING, ANIMATION_DURATION);
        mLayoutTransition.setStagger(LayoutTransition.DISAPPEARING, ANIMATION_DURATION);
        PropertyValuesHolder pvhLeft =
                PropertyValuesHolder.ofInt("left", 0, 1);
        PropertyValuesHolder pvhTop =
                PropertyValuesHolder.ofInt("top", 0, 1);
        PropertyValuesHolder pvhRight =
                PropertyValuesHolder.ofInt("right", 0, 1);
        PropertyValuesHolder pvhBottom =
                PropertyValuesHolder.ofInt("bottom", 0, 1);
        Animator changingDisappearingAnim = ObjectAnimator.ofPropertyValuesHolder(
                mContext, pvhLeft, pvhTop, pvhRight, pvhBottom);

        // Removing
        PropertyValuesHolder disappearingScaleX = PropertyValuesHolder.ofFloat("scaleX", 1f, 0f);
        PropertyValuesHolder disappearingScaleY = PropertyValuesHolder.ofFloat("scaleY", 1f, 0f);
        PropertyValuesHolder disappearingAlpha = PropertyValuesHolder.ofFloat("alpha", 1f, 0f);
        Animator disappearingAnim = ObjectAnimator.ofPropertyValuesHolder(mContext, disappearingAlpha, disappearingScaleX, disappearingScaleY);

        mLayoutTransition.setAnimator(LayoutTransition.APPEARING, null);
        mLayoutTransition.setAnimator(LayoutTransition.DISAPPEARING, disappearingAnim);
        mLayoutTransition.setAnimator(LayoutTransition.CHANGE_APPEARING, null);
        mLayoutTransition.setAnimator(LayoutTransition.CHANGE_DISAPPEARING, changingDisappearingAnim);
        return mLayoutTransition;
    }

    public void refreshView() {
        for (int i = 0; i < getChildCount(); i++) {
            View view = getChildAt(i);
            if (view instanceof BubbleAttachmentView) {
                ((BubbleAttachmentView) view).refreshView();
            }
        }
    }
}
