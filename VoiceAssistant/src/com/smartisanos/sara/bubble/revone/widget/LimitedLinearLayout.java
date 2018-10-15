package com.smartisanos.sara.bubble.revone.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;

import com.smartisanos.sara.R;

import static android.widget.LinearLayout.VERTICAL;

public class LimitedLinearLayout extends FrameLayout {
    private static final int ORDER_ASC = 0;
    private static final int ORDER_DESC = 1;
    private int mOrientation;
    private int mOrder;
    private int mMaxWidth;
    private int mMaxHeight;

    public LimitedLinearLayout(Context context) {
        this(context, null);
    }

    public LimitedLinearLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public LimitedLinearLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        TypedArray a = context.obtainStyledAttributes(attrs,
                R.styleable.LimitedLinearLayout);
        mMaxWidth = a.getDimensionPixelSize(
                R.styleable.LimitedLinearLayout_maxWidth, 0);
        mMaxHeight = a.getDimensionPixelSize(
                R.styleable.LimitedLinearLayout_maxHeight, 0);
        mOrientation = a.getInteger(
                R.styleable.LimitedLinearLayout_orientation, VERTICAL);
        mOrder = a.getInteger(
                R.styleable.LimitedLinearLayout_order, ORDER_ASC);
        a.recycle();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (mOrientation == VERTICAL) {
            measureVertical(widthMeasureSpec, heightMeasureSpec);
        } else {
            measureHorizontal(widthMeasureSpec, heightMeasureSpec);
        }
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        if (mOrientation == VERTICAL) {
            layoutVertical(changed, l, t, r, b);
        } else {
            layoutHorizontal(changed, l, t, r, b);
        }
    }

    private void measureVertical(int widthMeasureSpec, int heightMeasureSpec) {
        int width = 0;
        int height = 0;
        int childWidthMeasureSpec = 0;
        int childHeightMeasureSpec = 0;
        int count = getChildCount();
        final int paddingTop = getPaddingTop();
        final int paddingBottom = getPaddingBottom();
        for (int i = 0; i < count; i++) {
            final View child = getChildAt(i);
            if (child.getVisibility() != GONE) {
                final LayoutParams lp = (LayoutParams) child.getLayoutParams();
                measureChild(child, widthMeasureSpec, heightMeasureSpec);
                width = Math.max(width, child.getMeasuredWidth() + lp.leftMargin + lp.rightMargin + getPaddingLeft() + getPaddingRight());
                if (mMaxWidth > 0) {
                    width = Math.min(width, mMaxWidth);
                }
                height += child.getMeasuredHeight() + lp.topMargin + lp.rightMargin;
            }
        }
        int measureHeight = height + paddingTop + paddingBottom;
        if (mMaxHeight > 0) {
            height = Math.min(measureHeight, mMaxHeight);
        } else {
            height = measureHeight;
        }
        setMeasuredDimension(width, height);
        if (measureHeight > mMaxHeight) {
            int remainHeight = height;
            int maxWidth = width;
            int pos = 0;
            for (int i = 0; i < count; i++) {
                pos = i;
                if (mOrder == ORDER_DESC) {
                    pos = count - 1 - i;
                }
                View child = getChildAt(pos);
                if (child.getVisibility() != GONE) {
                    LayoutParams lp = (LayoutParams) child.getLayoutParams();
                    childWidthMeasureSpec = MeasureSpec.makeMeasureSpec(maxWidth, MeasureSpec.EXACTLY);
                    childHeightMeasureSpec = MeasureSpec.makeMeasureSpec(remainHeight, MeasureSpec.EXACTLY);
                    measureChild(child, childWidthMeasureSpec, childHeightMeasureSpec);
                    remainHeight -= child.getMeasuredHeight() + lp.topMargin + lp.bottomMargin;
                }
            }
        }
    }

    private void measureHorizontal(int widthMeasureSpec, int heightMeasureSpec) {
        int width = 0;
        int height = 0;
        int childWidthMeasureSpec = 0;
        int childHeightMeasureSpec = 0;
        int count = getChildCount();
        final int paddingLeft = getPaddingLeft();
        final int paddingRight = getPaddingRight();
        for (int i = 0; i < count; i++) {
            final View child = getChildAt(i);
            if (child.getVisibility() != GONE) {
                measureChild(child, widthMeasureSpec, heightMeasureSpec);
                LayoutParams lp = (LayoutParams) child.getLayoutParams();
                height = Math.max(height, child.getMeasuredHeight() + lp.topMargin + lp.bottomMargin + getPaddingTop() + getPaddingBottom());
                if (mMaxHeight > 0) {
                    height = Math.min(height, mMaxHeight);
                }
                width += child.getMeasuredWidth() + lp.leftMargin + lp.rightMargin;
            }
        }
        int measureWidth = width + paddingLeft + paddingRight;
        if (mMaxWidth > 0) {
            width = Math.min(measureWidth, mMaxWidth);
        } else {
            width = measureWidth;
        }
        setMeasuredDimension(width, height);
        if (measureWidth > mMaxWidth) {
            int remainWidth = width;
            int maxHeight = height;
            int pos = 0;
            for (int i = 0; i < count; i++) {
                pos = i;
                if (mOrder == ORDER_DESC) {
                    pos = count - 1 - i;
                }
                View child = getChildAt(pos);
                if (child.getVisibility() != GONE) {
                    childWidthMeasureSpec = MeasureSpec.makeMeasureSpec(remainWidth, MeasureSpec.EXACTLY);
                    childHeightMeasureSpec = MeasureSpec.makeMeasureSpec(maxHeight, MeasureSpec.EXACTLY);
                    measureChild(child, childWidthMeasureSpec, childHeightMeasureSpec);
                    remainWidth -= child.getMeasuredWidth();
                }
            }
        }
    }

    private void layoutVertical(boolean changed, int l, int t, int r, int b) {
        int x = getPaddingLeft();
        int y = getPaddingTop();
        int count = getChildCount();
        int itemWidth = 0;
        int itemHeight = 0;
        for (int i = 0; i < count; i++) {
            View child = getChildAt(i);
            if (child.getVisibility() != GONE) {
                itemWidth = child.getMeasuredWidth();
                itemHeight = child.getMeasuredHeight();
                final LayoutParams lp = (LayoutParams) child.getLayoutParams();
                child.layout(x + lp.leftMargin, y + lp.topMargin, x + lp.leftMargin + itemWidth, y + lp.topMargin + itemHeight);
                y += lp.topMargin + itemHeight + lp.bottomMargin;
            }
        }
    }

    private void layoutHorizontal(boolean changed, int l, int t, int r, int b) {
        int x = getPaddingLeft();
        int y = getPaddingTop();
        int count = getChildCount();
        int itemWidth = 0;
        int itemHeight = 0;
        for (int i = 0; i < count; i++) {
            final View child = getChildAt(i);
            if (child.getVisibility() != GONE) {
                itemWidth = child.getMeasuredWidth();
                itemHeight = child.getMeasuredHeight();
                final LayoutParams lp = (LayoutParams) child.getLayoutParams();
                child.layout(x + lp.leftMargin, y + lp.topMargin, x + lp.leftMargin + itemWidth, y + lp.topMargin + itemHeight);
                x += lp.leftMargin + itemWidth + lp.rightMargin;
            }
        }
    }
}
