package com.smartisanos.ideapills.view;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.graphics.Rect;
import android.util.SparseIntArray;
import android.view.DragEvent;
import android.view.MenuInflater;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.ListView;

import com.smartisanos.ideapills.common.anim.Anim;
import com.smartisanos.ideapills.common.anim.AnimCancelableListener;
import com.smartisanos.ideapills.common.anim.AnimInterpolator;
import com.smartisanos.ideapills.util.LOG;
import com.smartisanos.ideapills.util.StatusManager;

import java.util.List;

public class BubbleListDraggingAnimRunner extends BubbleListCollapseAnimRunner{
    private static final LOG log = LOG.getInstance(BubbleListDraggingAnimRunner.class);
    private int mInsertPos = -1;
    private BubbleListView mBubbList = null;
    private float mInsertX, mInsertY;
    private Rect mInsertRect = new Rect(-1, -1, -1, -1);
    private SparseIntArray mCachedTargetYs;
    private boolean mIsDraggingBack;
    private int mBottomPadding;

    public BubbleListDraggingAnimRunner(BubbleListView listView, List<Integer> reservedItems) {
        this(listView, reservedItems, false, listView.getNormalHeight());
    }

    public BubbleListDraggingAnimRunner(BubbleListView listView, List<Integer> reservedItems, boolean isDraggingBack, int bottomPadding) {
        super(listView, reservedItems);
        mBubbList = listView;
        mCachedTargetYs = getCachedTargetYs();
        mIsDraggingBack = isDraggingBack;
        mBottomPadding = bottomPadding;
    }

    public int start(float x, float y) {
        mInsertX = x;
        mInsertY = y;
        super.start();
        if (mInsertPos == -1) {
            return -1;
        } else {
            return mInsertPos - mBubbList.getHeaderViewsCount();
        }
    }

    public float getInsertX() {
        return mInsertX;
    }

    public float getInsertY() {
        return mInsertY;
    }

    protected void computeTargetYs(SparseIntArray cachedTargetYs) {
        super.computeTargetYs(cachedTargetYs);
        mInsertPos = findInsertPos();
    }

    private int findInsertPos() {
        int size = mCachedTargetYs.size();
        if (size > 0) {
            int insertPos = -1;
            int base = 0;
            for (int i = 0; i < size; i++) {
                int top = mCachedTargetYs.valueAt(i);
                base = top + getItemHeight(mCachedTargetYs.keyAt(i));
                if (insertPos==-1&&(mInsertY >= top && mInsertY < base)) {
                    mInsertRect.set(0, top, mBubbList.getWidth(), base);
                    if (mCachedTargetYs.keyAt(i) < mBubbList.getHeaderViewsCount()) {
                        return -1;
                    }
                    insertPos = mCachedTargetYs.keyAt(i);
                }
                if (insertPos != -1) {
                    int value;
                    if (mIsDraggingBack) {
                        value = mCachedTargetYs.valueAt(i);
                    } else {
                        value = mCachedTargetYs.valueAt(i) + mBubbList.getNormalHeight();
                    }

                    mCachedTargetYs.put(mCachedTargetYs.keyAt(i), value);
                }
            }
            if (insertPos == -1) {
                mInsertRect.set(0, base, mBubbList.getWidth(), mBubbList.getBottom());
                return mVisibleItems.getLastVisibleItem();
            } else {
                return insertPos;
            }
        } else {
            return -1;
        }
    }

    protected int getPaddingButtom() {
        return mBottomPadding;
    }
}
