package com.smartisanos.sara.bullet.widget;

import android.content.Context;
import android.support.annotation.Nullable;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.util.AttributeSet;
import android.view.MotionEvent;

public class LoadMoreRecyclerView extends RecyclerView {

    private boolean mIsLoadMoreEnabled;
    private boolean mIsLoadingMore;
    private RecyclerView.Adapter mGloriousAdapter;
    private AutoLoadMoreListener mLoadMoreListener;
    private int pageSize = 500;
    private int currentPage = 1;
    private int lastPosition = -1;
    private OnYScrollChageListener mOnYScrollChageListener;
    private int mScrollState =  RecyclerView.SCROLL_STATE_IDLE;

    private OnScrollListener mOnScrollListener = new OnScrollListener() {
        @Override
        public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
            super.onScrollStateChanged(recyclerView, newState);
        }

        @Override
        public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
            super.onScrolled(recyclerView, dx, dy);
            if (mIsLoadMoreEnabled && !mIsLoadingMore && dy > 0) {
                if (findLastVisibleItemPosition() == mGloriousAdapter.getItemCount() - 1) {
                    mIsLoadingMore = true;
                    mLoadMoreListener.onLoadMore(currentPage, pageSize);
                }
            }
        }
    };

    public LoadMoreRecyclerView(Context context) {
        this(context, null);
    }

    public LoadMoreRecyclerView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public LoadMoreRecyclerView(Context context, @Nullable AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        setYScrolledListener();
    }

    public interface AutoLoadMoreListener {
        void onLoadMore(int currentPage, int pageSize);
    }

    /**
     * Called this also means that loadMore enabled
     *
     * @param loadMoreListener loadMoreListener
     */
    public void setLoadMoreListener(final AutoLoadMoreListener loadMoreListener) {
        if (null != loadMoreListener) {
            mLoadMoreListener = loadMoreListener;
            mIsLoadMoreEnabled = true;
            this.addOnScrollListener(mOnScrollListener);
        }
    }

    @Override
    public void setAdapter(Adapter adapter) {
        super.setAdapter(adapter);
        mGloriousAdapter = adapter;
    }

    public void notifyLoadMoreFailed() {
        notifyLoadMoreFinish(false);
    }


    public void notifyLoadMoreSuccessful() {
        notifyLoadMoreFinish(true);
    }


    private void notifyLoadMoreFinish(final boolean success) {
        this.clearOnScrollListeners();
        mIsLoadingMore = false;
        if (success) {
            addOnScrollListener(mOnScrollListener);
            mGloriousAdapter.notifyDataSetChanged();
            currentPage++;
            if (lastPosition <= 0) {
                lastPosition = mGloriousAdapter.getItemCount();
            } else {
                scrollToPosition(lastPosition - 1);
                lastPosition = mGloriousAdapter.getItemCount();
            }
        }
    }


    /**
     * Find the last visible position depends on LayoutManger
     *
     * @return the last visible position
     * @see #setLoadMoreListener(AutoLoadMoreListener)
     */
    private int findLastVisibleItemPosition() {
        int position;
        if (getLayoutManager() instanceof LinearLayoutManager) {
            position = ((LinearLayoutManager) getLayoutManager()).findLastVisibleItemPosition();
        } else if (getLayoutManager() instanceof GridLayoutManager) {
            position = ((GridLayoutManager) getLayoutManager()).findLastVisibleItemPosition();
        } else if (getLayoutManager() instanceof StaggeredGridLayoutManager) {
            StaggeredGridLayoutManager layoutManager = (StaggeredGridLayoutManager) getLayoutManager();
            int[] lastPositions = layoutManager.findLastVisibleItemPositions(new int[layoutManager.getSpanCount()]);
            position = findMaxPosition(lastPositions);
        } else {
            position = getLayoutManager().getItemCount() - 1;
        }
        return position;
    }

    /**
     * Find StaggeredGridLayoutManager the last visible position
     *
     * @see #findLastVisibleItemPosition()
     */
    private int findMaxPosition(int[] positions) {
        int maxPosition = 0;
        for (int position : positions) {
            maxPosition = Math.max(maxPosition, position);
        }
        return maxPosition;
    }


    public int getPageSize() {
        return pageSize;
    }

    public int getCurrentPage() {
        return currentPage;
    }

    public void setOnYScrollChageListener(OnYScrollChageListener scrollChageListener) {
        this.mOnYScrollChageListener = scrollChageListener;
    }

    private void setYScrolledListener() {
        setOnScrollListener(new OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                if (mScrollState == RecyclerView.SCROLL_STATE_IDLE) {
                    if (mOnYScrollChageListener != null) {
                        mOnYScrollChageListener.onYScrolled();
                    }
                }
                mScrollState = newState;
            }
        });
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        //继承了Activity的onTouchEvent方法，直接监听点击事件
        if (mOnYScrollChageListener != null && (event.getAction() == MotionEvent.ACTION_DOWN ||
                event.getAction() == MotionEvent.ACTION_POINTER_DOWN)) {
            mOnYScrollChageListener.onYScrolled();
        }
        return super.onTouchEvent(event);
    }

    public static interface OnYScrollChageListener {
        void onYScrolled();
    }
}