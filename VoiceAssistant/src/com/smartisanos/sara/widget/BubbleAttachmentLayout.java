package com.smartisanos.sara.widget;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.LayoutTransition;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.content.Context;
import android.service.onestep.GlobalBubbleAttach;
import android.util.AttributeSet;
import android.view.View;
import android.widget.RelativeLayout;
import android.net.Uri;
import android.widget.ImageView;

import java.util.ArrayList;

import com.smartisanos.sara.util.AttachmentUtils;
import com.smartisanos.ideapills.common.util.AttachmentUtil;

import java.util.List;

import com.smartisanos.sara.R;

public class BubbleAttachmentLayout extends RelativeLayout {

    private List<GlobalBubbleAttach> mList;
    private int mListSize = -1;
    private int mCellWidth = -1;
    private int mCellHeight = -1;
    private int mGapX = 0;
    private int mGapY = 0;
    private static final int ANIMATION_DURATION = 250;
    private LayoutTransition mLayoutTransition;
    private GlobalBubbleAttach mLastAttach;
    private boolean mHandledAnimationEnd;
    private static final int MIN_COUNT = 1;
    private int mChildCountPerRow = AttachmentUtils.CHILD_COUNT_PER_ROW;
    public BubbleAttachmentLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public BubbleAttachmentLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mGapX = context.getResources().getDimensionPixelSize(R.dimen.attachment_container_gap_x);
        mGapY = context.getResources().getDimensionPixelSize(R.dimen.attachment_container_gap_y);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int childCount = getChildCount();
        int widthSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
        int heightSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
        for (int i = 0; i < childCount; i++) {
            View child = getChildAt(i);
            child.measure(widthSpec, heightSpec);
            if (mCellHeight < 0) {
                mCellHeight = child.getMeasuredHeight();
            }
            if (mCellWidth < 0) {
                mCellWidth = child.getMeasuredWidth();
            }
        }
        final int width = getMeasuredWidth();
        final int numCol = (width - getPaddingLeft() - getPaddingRight() + mGapX) / (mCellWidth + mGapX);
        if (numCol > 0 && numCol <= AttachmentUtils.CHILD_COUNT_PER_ROW) {
            mChildCountPerRow = numCol;
        }
        childCount = childCount > MIN_COUNT ? childCount : MIN_COUNT;
        int row = (int) Math.ceil((float) childCount / mChildCountPerRow);
        int height = row > 0 ? row * mCellHeight + (row - 1) * mGapY + getPaddingTop() + getPaddingBottom() : 0;
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

    public void finishPopup() {
        int count = getChildCount();
        for (int i = 0; i < count; i++) {
            View child = getChildAt(i);
            if (child instanceof BubbleAttachmentView) {
                ((BubbleAttachmentView) child).finishPopup();
            }
        }
    }

    public void setAttachmentList(List<GlobalBubbleAttach> list) {
        if (mList == list && (list != null && mListSize == list.size())) {
            return;
        }
        recycle();
        mList = list;
        if (mList != null) {
            mListSize = mList.size();
//            sortAttachments(mList);
            for (GlobalBubbleAttach item : mList) {
                addAttachMent(item);
            }
        }
    }

    public void addAttachMent(GlobalBubbleAttach item) {
        BubbleAttachmentView view = BubbleAttachmentView.getRecycler().getBubbleAttachmentView(getContext(), item.getType(), item.getFilename());
        if (view != null) {
            view.bindParent(this);
            view.setAttachment(item);
            addView(view);

            int type = item.getType() == GlobalBubbleAttach.TYPE_IMAGE
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
    }

    public void onShareClick(BubbleAttachmentView attachmentView) {
        if (attachmentView == null && mAttachmentClickListener == null) return;
        GlobalBubbleAttach globalBubbleAttach = attachmentView.getAttachment();
        AttachmentUtils.shareToApp(getContext(), globalBubbleAttach.getUri(), globalBubbleAttach.getContentType());
    }

    public void onDeleteClick(BubbleAttachmentView attachmentView) {
        if (attachmentView == null && mAttachmentClickListener == null) return;
        mLastAttach = attachmentView.getAttachment();
        mHandledAnimationEnd = false;
        if (mLayoutTransition == null) {
            mLayoutTransition = generateLayoutAnimate();
        }
        //Each time set the animation because the animation causes The listview misplaced when swiping fast
		updateTransitionPrama(indexOfChild(attachmentView));
        setLayoutTransition(mLayoutTransition);
        removeView(attachmentView);
    }

    private void onDeleteAnimationEnd() {
        if (!mHandledAnimationEnd) {
            mHandledAnimationEnd = true;
            setLayoutTransition(null);
            if (mLastAttach != null) {
                mAttachmentClickListener.onDeleteClick(mLastAttach);
            }
            if(mList.size() == 0){
                setMeasuredDimension(getMeasuredWidth(),0);
            }
        }
    }

    public void onImageClick(BubbleAttachmentView attachmentView) {
        ArrayList<Uri> localUris = new ArrayList<Uri>();
        for (GlobalBubbleAttach item : mList) {
            if (item.getType() == GlobalBubbleAttach.TYPE_IMAGE && item.getStatus() == GlobalBubbleAttach.STATUS_SUCCESS) {
                localUris.add(item.getLocalUri());
            }
        }
        if (attachmentView == null || mAttachmentClickListener == null) return;
        mAttachmentClickListener.onImageClick(attachmentView, localUris);
    }

    public void onFileClick(BubbleAttachmentView attachmentView) {
        if (attachmentView == null || mAttachmentClickListener == null) return;
        mAttachmentClickListener.onFileClick(attachmentView);
    }

    private onAttachmentClickListener mAttachmentClickListener;

    public static interface onAttachmentClickListener {
        public void onDeleteClick(GlobalBubbleAttach attachment);

        public void onImageClick(BubbleAttachmentView attachmentView, ArrayList<Uri> localUris);

        public void onFileClick(BubbleAttachmentView attachmentView);
    }

    public onAttachmentClickListener getAttachmentClickListener() {
        return mAttachmentClickListener;
    }

    public void setAttachmentClickListener(onAttachmentClickListener onAttachmentClickListener) {
        this.mAttachmentClickListener = onAttachmentClickListener;
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
        mLayoutTransition.setAnimateParentHierarchy(false);
        return mLayoutTransition;
    }
}
