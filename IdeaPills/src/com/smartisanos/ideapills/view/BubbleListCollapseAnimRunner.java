package com.smartisanos.ideapills.view;

import android.animation.Animator;
import android.animation.TypeEvaluator;
import android.animation.ValueAnimator;
import android.graphics.Canvas;
import android.util.SparseArray;
import android.util.SparseIntArray;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Interpolator;
import android.widget.ListView;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import com.smartisanos.ideapills.common.anim.Anim;
import com.smartisanos.ideapills.common.anim.AnimListener;
import com.smartisanos.ideapills.util.LOG;

public class BubbleListCollapseAnimRunner implements AnimRunnerInterface {
    private static final LOG log = LOG.getInstance(BubbleListCollapseAnimRunner.class);

    private BubbleListView mListView;
    private int mListHeight;
    private SparseArray<View> mCachedViews;
    private SparseIntArray mCachedTargetYs;
    private SparseIntArray mCachedOrgYs;
    private SparseIntArray mCachedCurYs;
    private int[] mLoc = new int[2];

    private long mDuration = 200L;

    private Interpolator mInterpolator;

    protected VisibleItems mVisibleItems;

    private int mFirstItemOffset;

    protected int mFirstIndex;
    protected int mLastIndex;
    private ValueAnimator mCollapseAnim = null;
    private List<AnimListener> mAnimList = new ArrayList<AnimListener>();

    public BubbleListCollapseAnimRunner(BubbleListView listView, List<Integer> reservedItems) {
        mListView = listView;
        mListHeight = mListView.getHeight();
        mVisibleItems = new VisibleItems(listView, reservedItems, this);
        mCachedViews = new SparseArray<View>();
        mCachedTargetYs = new SparseIntArray();
        mCachedOrgYs = new SparseIntArray();
        mCachedCurYs = new SparseIntArray();
        resetToInitialState();
    }

    @Override
    public void recycle() {
        if (mCachedViews != null) {
            for (int i = 0; i < mCachedViews.size(); i++) {
                View cachedView = mCachedViews.valueAt(i);
                mListView.removeFreeDetachedView(cachedView);
            }
            mCachedViews.clear();
        }
    }

    public void draw(Canvas canvas) {
        canvas.save();
        for (Integer i : mVisibleItems.getVislbeItemsList()) {
            View view = getItemView(i);
            if (view.getVisibility() == View.VISIBLE) {
                int[] loc = getDrawLoc(i);
                if (loc == null) {
                    view.draw(canvas);
                } else if (loc[1] > -view.getMeasuredHeight() && loc[1] < getListHeight()) {
                    canvas.translate(loc[0], loc[1]);
                    view.draw(canvas);
                    canvas.translate(-loc[0], -loc[1]);
                }
            }
        }
        canvas.restore();
    }

    protected int[] getDrawLoc(int pos) {
        mLoc[0] = 0;
        mLoc[1] = mCachedCurYs.size() == 0 ? mCachedOrgYs.get(pos) : mCachedCurYs.get(pos);
        return mLoc;
    }

    protected SparseIntArray getCachedCurYs() {
        return mCachedCurYs;
    }

    protected SparseIntArray getCachedTargetYs() {
        return mCachedTargetYs;
    }

    protected boolean reachTheEndOfList() {
        return mVisibleItems.reachTheLast();
    }

    protected boolean reachTheStartOfList() {
        return mVisibleItems.reachTheFirst();
    }

    /**
     * A negative number
     * @return
     */
    protected int getFirstItemOffset() {
        return mFirstItemOffset;
    }

    protected void setFirstItemOffset(int offset) {
        mFirstItemOffset = offset;
    }

    public void setInterpolator(Interpolator interpolator) {
        this.mInterpolator = interpolator;
    }

    public void addAnimListener(AnimListener listener) {
        mAnimList.add(listener);
    }

    public void setDuration(long duration) {
        mDuration = duration;
    }

    private void resetAnim() {
        stopCollapsing();
        mCachedCurYs.clear();
        mCollapseAnim = ValueAnimator.ofObject(new TypeEvaluator<SparseIntArray>() {
            public SparseIntArray evaluate(float fraction, SparseIntArray startValue, SparseIntArray endValue) {
                mCachedCurYs.clear();
                for (Integer i : mVisibleItems.getVislbeItemsList()) {
                    int sy = startValue.get(i);
                    int cy = (int) (sy + fraction * (endValue.get(i) - sy));
                    mCachedCurYs.append(i, cy);
                }
                return mCachedCurYs;
            }
        }, mCachedOrgYs, mCachedTargetYs);
        mCollapseAnim.setInterpolator(mInterpolator);
        mCollapseAnim.setDuration(mDuration);
        mCollapseAnim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            public void onAnimationUpdate(ValueAnimator animation) {
                mListView.invalidate();
            }
        });
        mCollapseAnim.addListener(new Animator.AnimatorListener() {
            public void onAnimationStart(Animator animation) {
                List<AnimListener> copy = new ArrayList<AnimListener>(mAnimList);
                for (AnimListener listener : copy) {
                    listener.onStart();
                }
            }

            public void onAnimationEnd(Animator animation) {
                List<AnimListener> copy = new ArrayList<AnimListener>(mAnimList);
                for (AnimListener listener : copy) {
                    listener.onComplete(Anim.ANIM_FINISH_TYPE_COMPLETE);
                }
            }

            public void onAnimationCancel(Animator animation) {
                List<AnimListener> copy = new ArrayList<AnimListener>(mAnimList);
                for (AnimListener listener : copy) {
                    listener.onComplete(Anim.ANIM_FINISH_TYPE_CANCELED);
                }
            }

            public void onAnimationRepeat(Animator animation) {

            }
        });
    }

    public boolean isRunning() {
        return mCollapseAnim != null && mCollapseAnim.isRunning();
    }

    public void start() {
        computeInternal();
        resetAnim();
        mCollapseAnim.start();
        mListView.invalidate();
    }

    private void resetToInitialState() {
        mFirstIndex = mListView.getFirstVisiblePosition();
        mLastIndex = mListView.getLastVisiblePosition();
        mFirstItemOffset = mListView.getChildAt(0).getTop();
        mVisibleItems.reset(mFirstIndex, mLastIndex);
    }

    protected int getItemHeight(int pos) {
        if (!mVisibleItems.reservedContains(pos)) {
            return 0;
        } else {
            return getItemView(pos).getMeasuredHeight();
        }
    }

    protected int getVisibleItemsHeight() {
        int height = 0;
        for (Integer i:mVisibleItems.getVislbeItemsList()) {
            height += getItemHeight(i);
        }
        return height;
    }

    protected int getListHeight() {
        return mListHeight;
    }

    protected int getPaddingButtom() {
        return 0;
    }

    private void expandForEnoughViews() {
        if (getFirstItemOffset() > 0) {
            if (mVisibleItems.getFirstVisibleItem() > 0) {
                mVisibleItems.expandFirstVisibleItem();
                expandForEnoughViews();
                return;
            } else {
                setFirstItemOffset(0);
            }
        }
        boolean rollback = false;
        while (getVisibleItemsHeight() + (rollback ? 0 : getFirstItemOffset()) < getListHeight()) {
            if (!reachTheEndOfList()) {
                mVisibleItems.expandLastVisibleItem();
            } else if (!reachTheStartOfList()) {
                mVisibleItems.expandFirstVisibleItem();
                rollback = true;
            } else {
                break;
            }
        }
    }

    private void computeInternal() {
        expandForEnoughViews();
        mCachedTargetYs.clear();
        computeTargetYs(mCachedTargetYs);
        mCachedOrgYs.clear();
        computeOrgYs(mCachedOrgYs);
    }

    private static Method sDispatchAttachInner = null;
    private static Field sAttachInfo = null;

    private void dispatchAttachInner(ViewGroup parent, View child) {
        if (sDispatchAttachInner == null) {
            Class cls = View.class;
            try {
                Class attachClas = cls.getClassLoader().loadClass(View.class.getName() + "$AttachInfo");
                sDispatchAttachInner = cls.getDeclaredMethod("dispatchAttachedToWindow", attachClas, int.class);
                sDispatchAttachInner.setAccessible(true);
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
            }
            try {
                sAttachInfo = cls.getDeclaredField("mAttachInfo");
                sAttachInfo.setAccessible(true);
            } catch (NoSuchFieldException nme) {
                nme.printStackTrace();
            }
        }
        if (sDispatchAttachInner != null && sAttachInfo != null) {
            try {
                Object attachInfo = sAttachInfo.get(parent);
                sDispatchAttachInner.invoke(child, attachInfo, parent.getVisibility());
            } catch (IllegalAccessException iae) {
                iae.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            }
        }
    }

    protected View getItemView(int pos) {
        ListView listView = mListView;
        if (pos >= listView.getFirstVisiblePosition()
                && pos <= listView.getLastVisiblePosition()) {
            return listView.getChildAt(pos - listView.getFirstVisiblePosition());
        } else if (mCachedViews.indexOfKey(pos) >= 0) {
            return mCachedViews.get(pos);
        } else {
            View view = inflateView(pos);
            return view;
        }
    }

    protected View inflateView(int pos) {
        ListView listView = mListView;
        View view = listView.getAdapter().getView(pos, null, mListView);
        int listWidth = listView.getWidth() - listView.getPaddingLeft()
                - listView.getPaddingRight();
        int widthSpec = View.MeasureSpec.makeMeasureSpec(listWidth, View.MeasureSpec.EXACTLY);
        int heightSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
        view.measure(widthSpec, heightSpec);
        view.layout(0, 0, view.getMeasuredWidth(), view.getMeasuredHeight());
        dispatchAttachInner(mListView, view);
        mCachedViews.put(pos, view);
        return view;
    }

    public void stopCollapsing() {
        if (mCollapseAnim != null) {
            mCollapseAnim.cancel();
            mCollapseAnim = null;
        }
    }

    public static class VisibleItems {
        private int mFirstVisibleItem;
        private int mLastVisibleItem;
        private List<Integer> mReservedItems;
        private ArrayList<Integer> mVisibleReservedItems = new ArrayList<Integer>();
        private int mItemsCount;
        private BubbleListCollapseAnimRunner mAnimRunner = null;

        public VisibleItems(ListView listView, List<Integer> reservedItems, BubbleListCollapseAnimRunner animRunner) {
            mItemsCount = listView.getCount();
            mReservedItems = reservedItems;
            mAnimRunner = animRunner;
            reset(listView.getFirstVisiblePosition(), listView.getLastVisiblePosition());
        }

        public void reset(int first, int last) {
            mFirstVisibleItem = first;
            mLastVisibleItem = last;
            mVisibleReservedItems.clear();
            for (int i = mFirstVisibleItem; i <= mLastVisibleItem; i++) {
                if (reservedContains(i)) {
                    mVisibleReservedItems.add(i);
                }
            }
        }

        public int getFirstVisibleItem() {
            return mFirstVisibleItem;
        }

        public int getLastVisibleItem() {
            return mLastVisibleItem;
        }

        public boolean reachTheFirst() {
            return mFirstVisibleItem == 0;
        }

        public boolean reachTheLast() {
            return mLastVisibleItem == mItemsCount - 1;
        }

        private static int binarySearch(List<Integer> array, int value) {
            int lo = 0;
            int hi = array.size() - 1;

            while (lo <= hi) {
                final int mid = (lo + hi) >>> 1;
                final int midVal = array.get(mid);

                if (midVal < value) {
                    lo = mid + 1;
                } else if (midVal > value) {
                    hi = mid - 1;
                } else {
                    return mid;  // value found
                }
            }
            return ~lo;  // value not present
        }

        public boolean reservedContains(int value) {
            return binarySearch(mReservedItems, value) >= 0;
        }

        public List<Integer> getVislbeItemsList() {
            return mVisibleReservedItems;
        }

        public void expandLastVisibleItem() {
            int next = getNextReserved();
            if (next >= 0) {
                mLastVisibleItem = next;
                mVisibleReservedItems.add(next);
            } else {
                mLastVisibleItem = mItemsCount - 1;
            }
        }

        public void expandFirstVisibleItem() {
            int first = getPrevReserved();
            if (first >= 0) {
                setFirstVisibleItem(first);
                mVisibleReservedItems.add(0, first);
                mAnimRunner.setFirstItemOffset(mAnimRunner.getFirstItemOffset() - mAnimRunner.getItemHeight(getFirstVisibleItem()));
            } else {
                setFirstVisibleItem(0);
            }
        }

        private int getNextReserved() {
            int visibleReservedSize = mVisibleReservedItems.size();
            if (visibleReservedSize == 0) {
                for (int i = mLastVisibleItem + 1; i < mItemsCount; i++) {
                    if (reservedContains(i)) {
                        return i;
                    }
                }
            } else {
                int curLastVal = mVisibleReservedItems.get(visibleReservedSize - 1);
                int index = binarySearch(mReservedItems, curLastVal);
                if (index + 1 < mReservedItems.size()) {
                    return mReservedItems.get(index + 1);
                }
            }
            return -1;
        }

        private int getPrevReserved() {
            int visibleReservedSize = mVisibleReservedItems.size();
            if (visibleReservedSize == 0) {
                for (int i = mFirstVisibleItem - 1; i >= 0; i--) {
                    if (reservedContains(i)) {
                        return i;
                    }
                }
            } else {
                int curFirstVal = mVisibleReservedItems.get(0);
                int index = binarySearch(mReservedItems, curFirstVal);
                if (index - 1 >= 0) {
                    return mReservedItems.get(index - 1);
                }
            }
            return -1;
        }

        private void setFirstVisibleItem(int pos) {
            mFirstVisibleItem = pos;
        }
    }
    protected void computeOrgYs(SparseIntArray cachedOrgYs) {
        for (int i = mFirstIndex; i <= mLastIndex; i++) {
            cachedOrgYs.append(i, (int)getItemView(i).getY());
        }
        int firstTopBase = cachedOrgYs.valueAt(0);
        int lastTopBase = cachedOrgYs.valueAt(cachedOrgYs.size() - 1);
        int first = mVisibleItems.getFirstVisibleItem();
        for (int i = cachedOrgYs.keyAt(0) - 1; i >= first; i--) {
            if (mVisibleItems.reservedContains(i)) {
                firstTopBase -= getItemHeight(i);
                cachedOrgYs.put(i, firstTopBase);
            }
        }
        int last = mVisibleItems.getLastVisibleItem();
        int lastHeight = getItemView(mLastIndex).getHeight();
        for (int i = cachedOrgYs.keyAt(cachedOrgYs.size() - 1) + 1; i <= last; i++) {
            if (mVisibleItems.reservedContains(i)) {
                lastTopBase += lastHeight;
                cachedOrgYs.put(i, lastTopBase);
                lastHeight = getItemView(i).getMeasuredHeight();
            }
        }
    }

    protected void computeTargetYs(SparseIntArray cachedTargetYs) {
        int last = mVisibleItems.getLastVisibleItem();
        int offset = getFirstItemOffset();
        for (int i = mVisibleItems.getFirstVisibleItem(); i <= last; i++) {
            if (mVisibleItems.reservedContains(i)) {
                cachedTargetYs.append(i, offset);
                offset += getItemHeight(i);
            } else {
                if (i >= mFirstIndex && i <= mLastIndex) {
                    offset += getItemHeight(i);
                }
            }
        }
        if (reachTheEndOfList() && offset < getListHeight() + getPaddingButtom()) {
            int down = getListHeight() - offset - getPaddingButtom();
            if (reachTheStartOfList()) {
                if (getFirstItemOffset() < 0) {
                    down = Math.min(down, -getFirstItemOffset());
                    setFirstItemOffset(getFirstItemOffset() + down);
                    int size = cachedTargetYs.size();
                    for (int i = 0; i < size; i++) {
                        cachedTargetYs.put(cachedTargetYs.keyAt(i), cachedTargetYs.valueAt(i) + down);
                    }
                }
            } else {
                setFirstItemOffset(getFirstItemOffset() + down);
                int size = cachedTargetYs.size();
                for (int i = 0; i < size; i++) {
                    cachedTargetYs.put(cachedTargetYs.keyAt(i), cachedTargetYs.valueAt(i) + down);
                }
            }
        }
    }

    public void cancel() {
        stopCollapsing();
    }
}
