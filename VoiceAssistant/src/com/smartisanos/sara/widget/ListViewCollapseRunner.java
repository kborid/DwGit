
package com.smartisanos.sara.widget;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.SparseArray;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.Interpolator;
import android.widget.Adapter;
import android.widget.ListView;
import android.widget.WrapperListAdapter;
import com.smartisanos.sara.widget.pinnedHeadList.WrapperView;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/**
 * Does not support divider
 */
public class ListViewCollapseRunner {

    private static final boolean DEBUG = false;

    /**
     * Deleted items' positions in ListView
     */
    private int mFirstVisibleItemOffset;
    private VisibleItems mVisibleItems;

    private ListView mListView;

    private float mInterpolatedTime;

    private SparseArray<View> mCachedViews;

    private long mStartTime;
    private static final long DURATION = 200L;

    private Interpolator mInterpolator;

    private Animation.AnimationListener mAnimationListener;

    private int startIndex, endIndex;

    public ListViewCollapseRunner(ListView listView, List<Integer> reservedItems,
            Animation.AnimationListener animationListener) {
        mListView = listView;
        mVisibleItems = new VisibleItems(
                mListView.getFirstVisiblePosition(),
                mListView.getLastVisiblePosition(),
                mListView.getCount(),
                reservedItems);
        mCachedViews = new SparseArray<View>();
        mAnimationListener = animationListener;
    }

    public void start() {
        // TODO empty listView verify
        resetToInitialState();
        fillViewCache();
        resetToInitialState();

        mStartTime = AnimationUtils.currentAnimationTimeMillis();
        onStart();
    }

    // Will be turn to the finished state
    private void fillViewCache() {
        // fill the view cache
        setTime(0f);
        computeInternal();
    }

    private void resetToInitialState() {
        startIndex = mListView.getFirstVisiblePosition();
        endIndex = mListView.getLastVisiblePosition();
        mFirstVisibleItemOffset = mListView.getChildAt(0).getTop();
        mVisibleItems.reset(startIndex, endIndex);
    }

    private void onStart() {
        if (mAnimationListener != null) {
            mAnimationListener.onAnimationStart(null);
        }
    }

    public void setInterpolator(Interpolator interpolator) {
        this.mInterpolator = interpolator;
    }

    private boolean reachTheEndOfList() {
        return mVisibleItems.reachTheLast();
    }

    private boolean reachTheStartOfList() {
        return mVisibleItems.reachTheFirst() && getFirstItemOffset() == 0;
    }

    /**
     * A negative number
     * 
     * @return
     */
    private int getFirstItemOffset() {
        return mFirstVisibleItemOffset;
    }

    public void setTime(float time) {
        if (time > 1) {
            mInterpolatedTime = 1;
        } else if (time < 0) {
            mInterpolatedTime = 0;
        } else {
            if (mInterpolator != null) {
                mInterpolatedTime = mInterpolator.getInterpolation(time);
            } else {
                mInterpolatedTime = time;
            }
        }
    }

    /**
     * @return [0, 1.0]
     */
    private float getInterpolatedTime() {
        return mInterpolatedTime;
    }

    private void setFirstVisibleItemOffset(int offset) {
        mFirstVisibleItemOffset = offset;
    }

    private View getItemView(int pos) {
        ListView listView = mListView;
        if (pos >= listView.getFirstVisiblePosition()
                && pos <= listView.getLastVisiblePosition()) {
            return listView.getChildAt(pos - listView.getFirstVisiblePosition());
        } else if (mCachedViews.indexOfKey(pos) >= 0) {
            return mCachedViews.get(pos);
        } else {
            dumpAdapter(listView.getAdapter());
            View view = listView.getAdapter().getView(pos, null, mListView);
            int listWidth = listView.getWidth() - listView.getPaddingLeft()
                    - listView.getPaddingRight();
            int widthSpec = View.MeasureSpec.makeMeasureSpec(listWidth, View.MeasureSpec.EXACTLY);
            int heightSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
            view.measure(widthSpec, heightSpec);
            view.layout(0, 0, view.getMeasuredWidth(), view.getMeasuredHeight());
            mCachedViews.put(pos, view);
            return view;
        }
    }

    private void dumpAdapter(Adapter adapter) {
        if (adapter != null) {
            if (adapter instanceof WrapperListAdapter) {
                dumpAdapter(((WrapperListAdapter) adapter).getWrappedAdapter());
            }
        }
    }

    private int getItemHeight(int pos) {
        if (!mVisibleItems.reservedContains(pos)) {
            View view = getItemView(pos);
            if (isHeaderRetain(view)) {
                int height = Math.round(view.getMeasuredHeight() * (1 - getInterpolatedTime()));
                int headerHeight = ((WrapperView) view).getHeader().getMeasuredHeight();
                return Math.max(height, headerHeight);
            }
            return Math.round(getItemView(pos).getMeasuredHeight() * (1 - getInterpolatedTime()));
        } else {
            return getItemView(pos).getHeight();
        }
    }

    private int getListHeight() {
        return mListView.getHeight() - mListView.getPaddingTop() - mListView.getPaddingBottom();
    }

    public boolean compute() {
        long pastTime = AnimationUtils.currentAnimationTimeMillis() - mStartTime;
        setTime((float) pastTime / DURATION);
        computeInternal();
        boolean ended = pastTime > DURATION;
        if (ended) {
            onEnd();
        }
        return ended;
    }

    private void computeInternal() {
        while (getVisibleItemsHeight() + getFirstItemOffset() < getListHeight()) {
            if (!reachTheEndOfList()) {
                mVisibleItems.expandLastVisibleItem();
            } else if (!reachTheStartOfList()) {
                mVisibleItems.expandFirstVisibleItem();
            } else {
                break;
            }
        }
    }

    private void onEnd() {
        mListView.postOnAnimation(new Runnable() {
            @Override
            public void run() {
                mAnimationListener.onAnimationEnd(null);
            }
        });
    }

    private int getVisibleItemsHeight() {
        int height = 0;
        for (Integer i : mVisibleItems.getVislbeItemsList()) {
            height += getItemHeight(i);
        }
        return height;
    }

    Paint debugPaint;
    {
        if (DEBUG) {
            debugPaint = new Paint();
            debugPaint.setColor(Color.RED);
            debugPaint.setStrokeWidth(30);
            debugPaint.setTextSize(30);
        }
    }

    public void draw(Canvas canvas) {
        int last = mVisibleItems.getLastVisibleItem();
        canvas.save();
        canvas.translate(0, getFirstItemOffset());
        for (int i = mVisibleItems.getFirstVisibleItem(); i <= last; i++) {
            if (mVisibleItems.reservedContains(i)) {

                View view = getItemView(i);
                view.draw(canvas);
                canvas.translate(0, getItemHeight(i));
            } else {
                if (i >= startIndex && i <= endIndex) {
                    View view = getItemView(i);
                    if (isHeaderRetain(view)) {
                        ((WrapperView) view).getHeader().draw(canvas);
                    } else {
                        drawDeletedItem(view, canvas);
                    }
                    canvas.translate(0, getItemHeight(i));
                }
            }
        }
        canvas.restore();
    }

    private void drawDeletedItem(View view, Canvas canvas) {
        // Do nothing...
    }

    private boolean mHeaderMode = false;
    private HashSet<String> mDeletedHeaderSections;

    public void setHeaderMode(boolean headerMode) {
        mHeaderMode = headerMode;
    }

    public void setDeletedHeaderSection(HashSet<String> headerSection) {
        mDeletedHeaderSections = headerSection;
    }

    private boolean isHeaderRetain(View child) {
        if (mHeaderMode && child != null && child instanceof WrapperView
                && ((WrapperView) child).hasHeader()) {
            String section = (String) ((WrapperView) child).getHeader().getTag();
            if (mDeletedHeaderSections == null || mDeletedHeaderSections.isEmpty()
                    || !mDeletedHeaderSections.contains(section)) {
                return true;
            }
        }
        return false;
    }

    private class VisibleItems {
        private int mFirstVisibleItem;
        private int mLastVisibleItem;
        private List<Integer> mReservedItems;
        private ArrayList<Integer> mVisibleReservedItems = new ArrayList<Integer>();
        private int mItemsCount;

        private int mReservedTop = 0;
        private int mReservedBot = 0;

        public VisibleItems(int first, int last, int count, List<Integer> reservedItems) {
            mItemsCount = count;
            mReservedItems = reservedItems;
            reset(first, last);
        }

        public void reset(int first, int last) {
            mFirstVisibleItem = first;
            mLastVisibleItem = last;
            mVisibleReservedItems.clear();
            for (int i = mFirstVisibleItem; i <= mLastVisibleItem; i++) {
                if (mReservedItems.contains(i)) {
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

        public boolean reservedContains(int value) {
            return mReservedItems.contains(value);
        }

        public List<Integer> getVislbeItemsList() {
            return mVisibleReservedItems;
        }

        public void expandLastVisibleItem() {
            int next = getNextReserved(mLastVisibleItem);
            if (next >= 0) {
                mLastVisibleItem = next;
                mVisibleReservedItems.add(next);
            } else {
                mLastVisibleItem = mItemsCount - 1;
            }
        }

        public void expandFirstVisibleItem() {
            if (getFirstItemOffset() != 0) {
                if (getListHeight() < getVisibleItemsHeight()) {
                    setFirstVisibleItemOffset(getListHeight() - getVisibleItemsHeight());
                } else {
                    setFirstVisibleItemOffset(0);
                }
            } else if (getFirstVisibleItem() > 0) {
                int currentFirst = getFirstVisibleItem();
                int first = getPrevReserved(currentFirst);
                if (first > 0) {
                    setFirstVisibleItem(first);
                    mVisibleReservedItems.add(0, first);
                } else {
                    setFirstVisibleItem(0);
                }
                setFirstVisibleItemOffset(-getItemHeight(getFirstVisibleItem()));
            }
        }

        private int getNextReserved(int value) {
            int v = mReservedItems.get(mReservedBot);
            int size = mReservedItems.size();
            if (v == value) {
                mReservedBot++;
                if (mReservedBot >= size) {
                    mReservedBot = size - 1;
                    return -1;
                } else {
                    return mReservedItems.get(mReservedBot);
                }
            } else if (value < mReservedItems.get(0)) {
                mReservedBot = 0;
                return mReservedItems.get(0);
            } else if (v < value) {
                for (int i = mReservedBot; i < size; i++) {
                    v = mReservedItems.get(i);
                    if (v > value) {
                        mReservedBot = i;
                        return v;
                    }
                }
            } else {
                for (int i = mReservedBot; i >= 0; i--) {
                    v = mReservedItems.get(i);
                    if (v <= value) {
                        mReservedBot = i + 1;
                        return mReservedItems.get(mReservedBot);
                    }
                }
            }
            return -1;
        }

        private int getPrevReserved(int value) {
            int v = mReservedItems.get(mReservedTop);
            if (v == value) {
                mReservedTop--;
                if (mReservedTop < 0) {
                    mReservedTop = 0;
                    return -1;
                } else {
                    return mReservedItems.get(mReservedTop);
                }
            } else if (value > mReservedItems.get(mReservedItems.size() - 1)) {
                mReservedTop = mReservedItems.size() - 1;
                return mReservedItems.get(mReservedTop);
            } else if (v < value) {
                int size = mReservedItems.size();
                for (int i = mReservedTop; i < size; i++) {
                    v = mReservedItems.get(i);
                    if (v >= value) {
                        mReservedTop = i - 1;
                        return mReservedItems.get(mReservedTop);
                    }
                }
            } else {
                for (int i = mReservedTop; i >= 0; i--) {
                    v = mReservedItems.get(i);
                    if (v < value) {
                        mReservedTop = i;
                        return v;
                    }
                }
            }
            return -1;
        }

        private void setFirstVisibleItem(int pos) {
            mFirstVisibleItem = pos;
        }
    }
}
