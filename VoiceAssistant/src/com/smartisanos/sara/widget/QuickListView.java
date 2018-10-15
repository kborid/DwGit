package com.smartisanos.sara.widget;
import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AbsListView;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import com.smartisanos.sara.R;
import com.smartisanos.sara.setting.recycle.QuickBubbleListener;

import android.widget.AbsListView.OnScrollListener;
import smartisanos.widget.letters.QuickBarEx;

public class QuickListView extends HorizontalScrollListView implements QuickBubbleListener,
        OnScrollListener {

    private static final String TAG = "QuickBubbleListView";
    private RelativeLayout mQuickBubbleView;
    private boolean mIsShowQuickBubble = false;
    private ImageView mCacheQuickButton;
    private boolean mIsTouchOther = true;
    private int mIndex = -1;
    protected QuickBarEx mQuickBar;
    private int mFirstPosition;
    private View mCacheView;
    public OnScrollListener mOnScrollListener;
    private OnQuickBubbleListViewScrollListener mOnQuickBubbleListViewOnScrollListener;
    private long mQuickDataCacheId = -1;
    private boolean mHasAddedTime = false;

    public interface OnQuickBubbleListViewScrollListener {

        public void onQuickBubbleListViewScroll(AbsListView view, int firstVisibleItem,
                int visibleItemCount,
                int totalItemCount);

        public void onQuickBubbleListViewScrollStateChanged(AbsListView view, int scrollState);
    }

    public QuickListView(Context context) {
        super(context);
        super.setOnScrollListener(this);
        this.setOverScrollMode(View.OVER_SCROLL_ALWAYS);
    }

    public QuickListView(Context context, AttributeSet attrs) {
        super(context, attrs);
        super.setOnScrollListener(this);
        this.setOverScrollMode(View.OVER_SCROLL_ALWAYS);
    }

    public QuickListView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        super.setOnScrollListener(this);
        this.setOverScrollMode(View.OVER_SCROLL_ALWAYS);
    }

    @Override
    public void setIsShowQuickBubble(boolean isShowQuickBubble) {
        if (isShowQuickBubble && mQuickBar != null)
            mQuickBar.hideLetterGrid();
        if (!isShowQuickBubble)
            mQuickBubbleView = null;
        mIsShowQuickBubble = isShowQuickBubble;
    }

    @Override
    public boolean isShowQuickBubble() {
        return mIsShowQuickBubble;
    }

    public void setOnQuickBubbleListViewOnScrollListener(OnQuickBubbleListViewScrollListener l) {
        mOnQuickBubbleListViewOnScrollListener = l;
    }

    @Override
    public void setOnScrollListener(OnScrollListener l) {
        mOnScrollListener = l;
    }

    @Override
    public void hideQuickBubble() {
        this.restoreScrollState(false);
    }

    @Override
    public void setQuickBubbleCacheId(long id) {
        mQuickDataCacheId = id;
    }

    @Override
    public long getQuickBubbleCacheId() {
        return mQuickDataCacheId;
    }

    public void setQuickBar(QuickBarEx quickBar) {
        mQuickBar = quickBar;
    }
    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);

        boolean hasQuickBubbleDisplayed = false;
        for (int i = getFirstVisiblePosition(); i <= getLastVisiblePosition(); i++) {
            if (mQuickDataCacheId == getAdapter().getItemId(i)) {
                hasQuickBubbleDisplayed = true;
                break;
            }
        }
        if (!hasQuickBubbleDisplayed) {
            mQuickDataCacheId = -1;
            mHasAddedTime = false;
        }
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {

        if (mIsShowQuickBubble) {
            int action = event.getAction();
            switch (action) {
                case MotionEvent.ACTION_DOWN:
                    return true;
            }

            return super.onInterceptTouchEvent(event);
        }

        return super.onInterceptTouchEvent(event);
    }

    @Override
    public void setVisibility(int visibility) {
        super.setVisibility(visibility);
        if (visibility == View.INVISIBLE || visibility == View.GONE) {
            mQuickDataCacheId = -1;
            mHasAddedTime = false;
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int action = event.getAction();
        if (mQuickBar != null) {
            mQuickBar.hideLetterGrid();
        }

        // if (mIsShowQuickBubble) {
        // if (mQuickBubbleView == null)
        // return true;
        // if (mQuickBubbleView.isAnimationing())
        // return true;
        // Rect rect = new Rect();
        // mQuickBubbleView.getGlobalVisibleRect(rect);
        // int x = (int) event.getRawX();
        // int y = (int) event.getRawY();
        // switch (action) {
        // case MotionEvent.ACTION_DOWN:
        // if (!rect.contains(x, y)) {
        // mQuickBubbleView.hideChild(-1, mQuickBubbleView
        // .exitAnimationEndRunnable());
        // } else {
        // for (int i = 0; i < mQuickBubbleView.getButtonCount(); i++) {
        // Rect childViewRect = new Rect();
        // ImageView view =
        // mQuickBubbleView.getButtonAt(i);
        // view.getGlobalVisibleRect(childViewRect);
        // if (x >= childViewRect.left + SaraUtils.dipTopx(getContext(), 7)
        // && x <= childViewRect.right - SaraUtils.dipTopx(getContext(), 7)) {
        // mIndex = i;
        // mCacheQuickButton = view;
        // mIsTouchOther = false;
        // break;
        // }
        // }
        // if (mIsTouchOther)
        // mQuickBubbleView.hideChild(-1, mQuickBubbleView
        // .exitAnimationEndRunnable());
        // }
        // break;
        // case MotionEvent.ACTION_UP:
        // if (mIsTouchOther) {
        // return super.onTouchEvent(event);
        // } else {
        // mIsTouchOther = true;
        // Rect childViewRect = new Rect();
        // mCacheQuickButton.getGlobalVisibleRect(childViewRect);
        // if (childViewRect.contains(x, y)) {
        // mCacheQuickButton.performClick();
        // mQuickBubbleClickedView.setClicked(mIndex, childViewRect.left, childViewRect.top);
        // }
        // }
        // break;
        // case MotionEvent.ACTION_CANCEL:
        // mIsTouchOther = true;
        // super.onTouchEvent(event);
        // break;
        // default:
        // super.onTouchEvent(event);
        // break;
        // }
        // return true;
        // }
        // switch (action) {
        // case MotionEvent.ACTION_DOWN:
        // mFirstPosition = pointToPosition((int) event.getX(), (int) event.getY()) -
        // getFirstVisiblePosition();
        // mCacheView = getChildAt(mFirstPosition);
        // break;
        // case MotionEvent.ACTION_UP:
        // mCacheView = null;
        // break;
        // }
        return super.onTouchEvent(event);
    }

    @Override
    public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount,
            int totalItemCount) {
        if (mOnScrollListener != null)
            mOnScrollListener.onScroll(view, firstVisibleItem, visibleItemCount, totalItemCount);
        if (mOnQuickBubbleListViewOnScrollListener != null)
            mOnQuickBubbleListViewOnScrollListener.onQuickBubbleListViewScroll(view,
                    firstVisibleItem,
                    visibleItemCount, totalItemCount);
    }

    @Override
    public void onScrollStateChanged(AbsListView view, int scrollState) {
        if (mOnScrollListener != null)
            mOnScrollListener.onScrollStateChanged(view, scrollState);
        if (mOnQuickBubbleListViewOnScrollListener != null)
            mOnQuickBubbleListViewOnScrollListener.onQuickBubbleListViewScrollStateChanged(view,
                    scrollState);
        if (scrollState == OnScrollListener.SCROLL_STATE_TOUCH_SCROLL) {
            if (mCacheView != null) {
                mCacheView.setPressed(false);
                mCacheView = null;
            }
        }
    }

    @Override
    public void setHasAddedTime(boolean hasAddedTime) {
        mHasAddedTime = hasAddedTime;
    }

    @Override
    public boolean hasAddedTime() {
        return mHasAddedTime;
    }

}
