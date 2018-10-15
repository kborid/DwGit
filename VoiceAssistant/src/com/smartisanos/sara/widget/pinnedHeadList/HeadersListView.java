
package com.smartisanos.sara.widget.pinnedHeadList;

import com.smartisanos.sara.widget.AutoScrollListView;
import com.smartisanos.sara.widget.QuickListView.OnQuickBubbleListViewScrollListener;

import android.content.Context;
import android.database.DataSetObserver;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.ListAdapter;

import com.smartisanos.sara.R;

/**
 * @author Emil Sj��lander Copyright 2012 Emil Sj��lander Licensed under the Apache License, Version
 *         2.0 (the "License"); you may not use this file except in compliance with the License. You
 *         may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0 Unless
 *         required by applicable law or agreed to in writing, software distributed under the
 *         License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 *         either express or implied. See the License for the specific language governing
 *         permissions and limitations under the License.
 */
public class HeadersListView extends AutoScrollListView implements
        OnQuickBubbleListViewScrollListener {

    public interface OnHeaderClickListener {
        public void onHeaderClick(HeadersListView l, View header,
                int itemPosition, long headerId, boolean currentlySticky);
    }

    private OnScrollListener scrollListener;
    private boolean areHeadersSticky = true;
    private int headerBottomPosition;
    private View header;
    private int dividerHeight;
    private Drawable divider;
    private boolean clippingToPadding;
    private boolean clipToPaddingHasBeenSet;
    private final Rect clippingRect = new Rect();
    private Long currentHeaderId = null;
    private HeadersAdapterWrapper adapter;
    private int headerPosition;
    private boolean mChangePosition = false;
    private int mPosition = 0;
    private boolean mSetTop = false;
    private boolean mPositionConsiderHeader = false;

    public HeadersListView(Context context) {
        this(context, null);
    }

    public HeadersListView(Context context, AttributeSet attrs) {
        this(context, attrs, android.R.attr.listViewStyle);
    }

    public HeadersListView(Context context, AttributeSet attrs,
            int defStyle) {
        super(context, attrs, defStyle);

         setOnQuickBubbleListViewOnScrollListener(this);
        setVerticalFadingEdgeEnabled(false);
    }

    private void reset() {
        headerBottomPosition = 0;
    }

    @Override
    public boolean performItemClick(View view, int position, long id) {
        view = ((WrapperView) view).item;
        return super.performItemClick(view, position, id);
    }

    /**
     * can only be set to false if headers are sticky, not compatible with fading edges
     */
    @Override
    public void setVerticalFadingEdgeEnabled(boolean verticalFadingEdgeEnabled) {
        if (areHeadersSticky) {
            super.setVerticalFadingEdgeEnabled(false);
        } else {
            super.setVerticalFadingEdgeEnabled(verticalFadingEdgeEnabled);
        }
    }

    @Override
    public void setOnScrollListener(OnScrollListener l) {
        scrollListener = l;
    }

    @Override
    public void setAdapter(ListAdapter adapter) {
        if (!clipToPaddingHasBeenSet) {
            clippingToPadding = true;
        }
        if (adapter != null) {
            if (!(adapter instanceof HeadersAdapter)) {
                throw new IllegalArgumentException(
                        "Adapter must implement StickyListHeadersAdapter");
            }
            this.adapter = new HeadersAdapterWrapper(getContext(),
                    (HeadersAdapter) adapter);
            // this.adapter.setDivider(divider);
            // this.adapter.setDividerHeight(dividerHeight);

            // Bug 993: new observer to the adapter.
            DataSetObserver dataSetChangedObserver = new DataSetObserver() {
                @Override
                public void onChanged() {
                    reset();
                }

                @Override
                public void onInvalidated() {
                    reset();
                }
            };

            this.adapter.registerDataSetObserver(dataSetChangedObserver);
            reset();
        }
        super.setAdapter(this.adapter);
    }

    @Override
    public HeadersAdapter getAdapter() {
        return adapter == null ? null : adapter.delegate;
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.FROYO) {
            scrollChanged(getFirstVisiblePosition());
        }
        super.dispatchDraw(canvas);
        if (header == null || !areHeadersSticky) {
            return;
        }

        int headerHeight = getHeaderHeight();
        int top = headerBottomPosition - headerHeight;
        clippingRect.left = getPaddingLeft();
        clippingRect.right = getWidth() - getPaddingRight();
        clippingRect.bottom = top + headerHeight;
        if (clippingToPadding) {
            clippingRect.top = getPaddingTop();
        } else {
            clippingRect.top = 0;
        }

        canvas.save();
        canvas.clipRect(clippingRect);
        canvas.translate(getPaddingLeft(), top);
        header.draw(canvas);
        canvas.restore();
    }

    private void measureHeader() {
        int widthMeasureSpec = MeasureSpec.makeMeasureSpec(getWidth(),
                MeasureSpec.EXACTLY);
        int heightMeasureSpec = 0;

        ViewGroup.LayoutParams params = header.getLayoutParams();
        if (params != null && params.height > 0) {
            heightMeasureSpec = MeasureSpec.makeMeasureSpec(params.height, MeasureSpec.EXACTLY);
        } else {
            heightMeasureSpec = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);
        }
        header.measure(widthMeasureSpec, heightMeasureSpec);
        header.layout(getLeft() + getPaddingLeft(), 0, getRight()
                - getPaddingRight(), header.getMeasuredHeight());
    }

    private int getHeaderHeight() {
        if (header != null) {
            return header.getMeasuredHeight();
        }
        return 0;
    }

    @Override
    public void setClipToPadding(boolean clipToPadding) {
        super.setClipToPadding(clipToPadding);
        clippingToPadding = clipToPadding;
        clipToPaddingHasBeenSet = true;
    }

    public void setPosition(int position, boolean setTop) {
        mPosition = position;
        mChangePosition = true;
        mSetTop = setTop;
        layoutChildren();
        layoutChildren();
    }

    public void setPositionConsiderHeader(int position) {
        if (position < 0) {
            return;
        }
        mPosition = position;
        mChangePosition = true;
        mPositionConsiderHeader = true;
        layoutChildren();
        layoutChildren();
    }

    @Override
    public void onQuickBubbleListViewScroll(AbsListView view, int firstVisibleItem,
            int visibleItemCount, int totalItemCount) {
        if (mChangePosition) {
            // TODO improve the position logic
            ViewGroup firstChild = (ViewGroup) view.getChildAt(0);
            ViewGroup secondChild = (ViewGroup) view.getChildAt(1);
            if (firstChild == null || secondChild == null) {
                return;
            }
            if (firstVisibleItem == 0 && view instanceof HeadersListView
                    && firstChild.getBottom() <= ((HeadersListView) view).getHeaderHeight()) {
                mPosition += 1;
            }
            int top = 0;
            if (mSetTop) {
                if (firstVisibleItem == 0 && mPosition - firstVisibleItem <= 2
                        && firstChild.getChildCount() == 2
                        && firstChild.getChildAt(1).getBottom() > 0
                        && secondChild != null && secondChild.getChildCount() == 1) {
                    top = firstChild.getChildAt(0).getTop();
                } else if (mPosition > 0) {
                    top = firstChild.getTop();
                }
                mSetTop = false;
            }

            if (mPositionConsiderHeader) {
                HeadersAdapter adapter = getAdapter();
                if (adapter != null && mPosition > 0 && mPosition < adapter.getCount()) {
                    Object preO = adapter.getItem(mPosition - 1);
                    Object o = adapter.getItem(mPosition);
                    // if (o instanceof SamContact) {
                    // SamContact preSamContact = (SamContact) preO;
                    // SamContact samContact = (SamContact) o;
                    // if (TextUtils.equals(preSamContact.section, samContact.section)) {
                    // top = ContactsUtils.dipTopx(getContext(), 27);
                    // }
                    // }
                }

                mPositionConsiderHeader = false;
            }

            if (mPosition < 0) {
                mPosition = 0;
            }
            setSelectionFromTop(mPosition, top);
            mChangePosition = false;
        }
        if (scrollListener != null) {
            scrollListener.onScroll(view, firstVisibleItem, visibleItemCount,
                    totalItemCount);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.FROYO) {
            scrollChanged(firstVisibleItem);
        }
    }

    private void scrollChanged(int firstVisibleItem) {
        if (adapter == null || adapter.getCount() == 0 || !areHeadersSticky)
            return;

        firstVisibleItem = getFixedFirstVisibleItem(firstVisibleItem);

        long newHeaderId = adapter.delegate.getHeaderId(firstVisibleItem);
        if (currentHeaderId == null || currentHeaderId != newHeaderId) {
            headerPosition = firstVisibleItem;
            header = adapter.delegate.getHeaderView(headerPosition, header,
                    this);
            measureHeader();
        }
        currentHeaderId = newHeaderId;

        final int childCount = getChildCount();
        if (childCount != 0) {
            WrapperView viewToWatch = null;
            int watchingChildDistance = 99999;

            for (int i = 0; i < childCount; i++) {
                WrapperView child = (WrapperView) super.getChildAt(i);

                int childDistance;
                if (clippingToPadding) {
                    childDistance = child.getTop() - getPaddingTop();
                } else {
                    childDistance = child.getTop();
                }

                if (childDistance < 0) {
                    continue;
                }

                if (viewToWatch == null
                        || !viewToWatch.hasHeader()
                        || (child.hasHeader() && childDistance < watchingChildDistance)) {
                    viewToWatch = child;
                    watchingChildDistance = childDistance;
                }
            }

            int headerHeight = getHeaderHeight();

            if (viewToWatch != null && viewToWatch.hasHeader()) {

                if (firstVisibleItem == 0 && super.getChildAt(0).getTop() > 0
                        && !clippingToPadding) {
                    headerBottomPosition = 0;
                } else {
                    if (clippingToPadding) {
                        headerBottomPosition = Math.min(viewToWatch.getTop(),
                                headerHeight + getPaddingTop());
                        headerBottomPosition = headerBottomPosition < getPaddingTop() ? headerHeight
                                + getPaddingTop()
                                : headerBottomPosition;
                    } else {
                        headerBottomPosition = Math.min(viewToWatch.getTop(),
                                headerHeight);
                        headerBottomPosition = headerBottomPosition < 0 ? headerHeight
                                : headerBottomPosition;
                    }
                }
            } else {
                headerBottomPosition = headerHeight;
                if (clippingToPadding) {
                    headerBottomPosition += getPaddingTop();
                }
            }
        }

        int top = clippingToPadding ? getPaddingTop() : 0;
        for (int i = 0; i < childCount; i++) {
            WrapperView child = (WrapperView) super.getChildAt(i);
            if (child.hasHeader()) {
                View childHeader = child.header;
                if (child.getTop() < top) {
                    childHeader.setVisibility(View.INVISIBLE);
                } else {
                    childHeader.setVisibility(View.VISIBLE);
                }
            }
        }
    }

    private int getFixedFirstVisibleItem(int firstVisibleItem) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            return firstVisibleItem;
        }

        for (int i = 0; i < getChildCount(); i++) {
            if (getChildAt(i).getBottom() >= 0) {
                firstVisibleItem += i;
                break;
            }
        }

        // work around to fix bug with firstVisibleItem being to high because
        // listview does not take clipToPadding=false into account
        if (!clippingToPadding && getPaddingTop() > 0) {
            if (super.getChildAt(0).getTop() > 0) {
                if (firstVisibleItem > 0) {
                    firstVisibleItem -= 1;
                }
            }
        }
        return firstVisibleItem;
    }

    @Override
    public void onQuickBubbleListViewScrollStateChanged(AbsListView view, int scrollState) {
        if (scrollListener != null) {
            scrollListener.onScrollStateChanged(view, scrollState);
        }
    }

    @Override
    protected boolean isHeaderMode() {
        return true;
    }
}
