package com.smartisanos.ideapills.view;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.smartisanos.ideapills.BubbleController;
import com.smartisanos.ideapills.R;
import com.smartisanos.ideapills.entity.BubbleItem;
import com.smartisanos.ideapills.util.ViewUtils;

import java.util.Date;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

public class BubbleItemViewHelper {
    private BubbleItemView mFakeViewForMeasure;
    private BubbleItemView mFakeViewForMeasureHeight;
    private BubbleItemView mFakeViewForMeasureHeightInPpt;
    private View mFakeIvBubbleBg;
    private int mNormalBubbleBgHeight = -1;

    public BubbleItemViewHelper(Context context) {
        mFakeViewForMeasure = (BubbleItemView) LayoutInflater.from(context).inflate(R.layout.bubble_item, null);
        mFakeViewForMeasureHeight = (BubbleItemView) LayoutInflater.from(context).inflate(R.layout.bubble_item, null);
        mFakeIvBubbleBg = mFakeViewForMeasureHeight.findViewById(R.id.iv_bubble_bg);
    }

    public int getNormalBubbleBgHeight() {
        if (mNormalBubbleBgHeight < 0) {
            mNormalBubbleBgHeight = ViewUtils.getSupposeHeight(mFakeIvBubbleBg);
        }
        return mNormalBubbleBgHeight;
    }

    public int measureNormalWidth(BubbleItem item) {
        if (!item.haveAttachments()) {
            mFakeViewForMeasure.mAttachmentTag.setVisibility(GONE);
        } else {
            mFakeViewForMeasure.mAttachmentTag.setVisibility(VISIBLE);
        }
        Date remindDate = new Date(item.getDueDate());
        if (remindDate.after(new Date()) && item.getRemindTime() > 0) {
            mFakeViewForMeasure.mVLittleNotify.setVisibility(VISIBLE);
        } else {
            mFakeViewForMeasure.mVLittleNotify.setVisibility(GONE);
        }
        if (item.isShareColor()) {
            mFakeViewForMeasure.mllbackground.setPadding(
                    BubbleController.getInstance().getContext().getResources().getDimensionPixelSize(R.dimen.drag_padding_left_share),
                    mFakeViewForMeasure.mllbackground.getPaddingTop(),
                    mFakeViewForMeasure.mllbackground.getPaddingRight(),
                    mFakeViewForMeasure.mllbackground.getPaddingBottom());
        } else {
            mFakeViewForMeasure.mllbackground.setPadding(
                    BubbleController.getInstance().getContext().getResources().getDimensionPixelSize(R.dimen.drag_padding_left_full),
                    mFakeViewForMeasure.mllbackground.getPaddingTop(),
                    mFakeViewForMeasure.mllbackground.getPaddingRight(),
                    mFakeViewForMeasure.mllbackground.getPaddingBottom());
        }
        mFakeViewForMeasure.mTextView.setText(item.getSingleText());
        int width = ViewUtils.getSupposeWidth(mFakeViewForMeasure.mFllayout);
        return width;
    }

    public int measureNormalHeight(ViewGroup.LayoutParams lp) {
        mFakeViewForMeasure.setLayoutParams(lp);
        return ViewUtils.getSupposeHeight(mFakeViewForMeasure);
    }

    public int measureLargeHeightByItem(BubbleItem item) {
        if (BubbleController.getInstance().isInPptMode()) {
            if (mFakeViewForMeasureHeightInPpt == null) {
                mFakeViewForMeasureHeightInPpt = (BubbleItemView) LayoutInflater.from(BubbleController.getInstance().getContext())
                        .inflate(R.layout.bubble_item, null);
            }
            return measureLargeHeightByItem(item, mFakeViewForMeasureHeightInPpt, true);
        } else {
            return measureLargeHeightByItem(item, mFakeViewForMeasureHeight, false);
        }
    }

    public int measureLargeHeightByItem(BubbleItem item, BubbleItemView measureView, boolean forcePpt) {
        ViewGroup.LayoutParams lp = measureView.mFllayout.getLayoutParams();
        measureView.mllbackground.setVisibility(GONE);
        measureView.mBubbleItemDetailView.setVisibility(VISIBLE);
        measureView.mBubbleItemDetailView.setBubbleItem(item);
        measureView.mBubbleItemDetailView.showText();
        if (forcePpt) {
            measureView.mBubbleItemDetailView.forceChangeToPptMode();
        }
        lp.width = measureView.getResources().getDimensionPixelSize(R.dimen.bubbleopt_layout_width);
        lp.height = ViewGroup.LayoutParams.WRAP_CONTENT;
        return ViewUtils.getSupposeHeight(measureView.mFllayout);
    }
}
