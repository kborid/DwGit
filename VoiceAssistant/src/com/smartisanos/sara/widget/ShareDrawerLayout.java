package com.smartisanos.sara.widget;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.content.ClipData;
import android.content.Context;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.DragEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.smartisanos.ideapills.common.model.ShareItem;
import com.smartisanos.ideapills.common.util.CommonConstant;
import com.smartisanos.sara.R;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class ShareDrawerLayout extends ViewGroup {
    private static final int DEFAULT_MAX_COUNT_PER_COL = CommonConstant.APP_DRAWER_MAX_COUNT;
    private int mCellWidth = -1;
    private int mCellHeight = -1;
    private int mRowCount = 1;
    private int mIconSize;
    private int mGapX;
    private int mGapY;
    private boolean mAllowDrag;
    private ViewGroup mRootView;
    private ShadowBitmapView mDragView;
    private static final ClipData EMPTY_CLIP_DATA = ClipData.newPlainText("", "");
    static final String DRAG_APPICON_TILE = "APPICON_TILE";
    private int mDragPosition = -1;
    private AnimatorSet mPointToNewPositionAnim = null;
    private Animator mDragEndAnim = null;
    private HashMap<Integer, View> mViewPositionMap = new HashMap<>();
    private HashMap<View, Integer> mOriginPositionMap = new HashMap<>();
    private OnSortChangeListener mOnSortChangeListener;
    private OnDragListener mOnDragListener;
    private boolean mDragAnimPlaying = false;

    public ShareDrawerLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ShareDrawerLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mIconSize = getResources().getDimensionPixelSize(R.dimen.drawer_item_width);
    }

    public void setData(List<ShareItem> shareItems, final OnShareItemClickListener onShareItemClickListener) {
        this.removeAllViews();
        if (shareItems != null && !shareItems.isEmpty()) {
            for (final ShareItem shareItem : shareItems) {
                View itemView = View.inflate(getContext(), R.layout.app_drawer_item, null);
                ShadowBitmapView imageView = (ShadowBitmapView) itemView.findViewById(R.id.share_view);
                imageView.setImageDrawableAsyncLoadShadow(shareItem);
                this.addView(itemView);
                itemView.setContentDescription(shareItem.getDispalyName());
                itemView.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if (onShareItemClickListener != null) {
                            onShareItemClickListener.onShareItemClick(shareItem);
                        }
                    }
                });
            }
        }
    }

    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int childCount = getChildCount();

        int widthSpec = MeasureSpec.makeMeasureSpec(mIconSize, MeasureSpec.EXACTLY);
        int heightSpec = MeasureSpec.makeMeasureSpec(mIconSize, MeasureSpec.EXACTLY);
        for (int i = 0; i < childCount; i++) {
            View child = getChildAt(i);
            child.measure(widthSpec, heightSpec);
            if (mCellHeight < 0) {
                mCellHeight = mIconSize;//child.getMeasuredHeight();
            }
            if (mCellWidth < 0) {
                mCellWidth = mIconSize;//child.getMeasuredWidth();
            }
        }
        mRowCount = (int) Math.ceil((float) childCount / DEFAULT_MAX_COUNT_PER_COL);
        setMeasuredDimension(getMeasuredWidth(), getMeasuredHeight());
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        mViewPositionMap.clear();
        mOriginPositionMap.clear();
        final int childCount = getChildCount();
        final int cellWidth = mCellWidth;
        final int cellHeight = mCellHeight;
        final int gapX = (getWidth() - getPaddingLeft() - getPaddingRight() - childCount * mCellWidth)
                / (childCount + 1);
        final int gapY = (getHeight() - mRowCount * mCellHeight) / (mRowCount + 1);
        mGapX = gapX;
        mGapY = gapY;
        int x = getPaddingLeft() + gapX / 2;
        int y = getPaddingTop() + gapY / 2;
        int col = 0;
        int row = 0;
        for (int i = 0; i < childCount; ++i) {
            final View child = getChildAt(i);
            mOriginPositionMap.put(child, i);
            mViewPositionMap.put(i, child);
            col = i % childCount;
            row = i / childCount;
            if (col == 0) {
                x = getPaddingLeft() + gapX / 2;
                y = getPaddingTop() + gapY / 2 + row * (cellHeight + gapY);
            } else {
                x += cellWidth + gapX;
            }
            child.layout(x, y, x + cellWidth + gapX, y + cellHeight + gapY);
        }
    }

    @Override
    public boolean onDragEvent(final DragEvent event) {
        if (mAllowDrag && mDragView != null) {
            int x = (int) event.getX();
            int y = (int) event.getY();
            switch (event.getAction()) {
                case DragEvent.ACTION_DRAG_STARTED:
                    break;
                case DragEvent.ACTION_DRAG_ENTERED:
                    break;
                case DragEvent.ACTION_DRAG_EXITED:
                    break;
                case DragEvent.ACTION_DRAG_LOCATION:
                    handleDragMove(x, y);
                    break;
                case DragEvent.ACTION_DRAG_ENDED:
                    endDrag(x, y);
                    break;
                case DragEvent.ACTION_DROP:
                    return false;
            }
            return true;
        }
        return false;
    }

    private void handleDragMove(int x, int y) {
        int position = findPosition(x, y);
        if (position >= 0 && mDragPosition != position) {
            if (mPointToNewPositionAnim != null) {
                mPointToNewPositionAnim.cancel();
            }
            mPointToNewPositionAnim = new AnimatorSet();
            List<Animator> animList = new ArrayList<Animator>();
            DecelerateInterpolator interpolator = new DecelerateInterpolator(1.5f);
            mPointToNewPositionAnim.setInterpolator(interpolator);
            HashMap<Integer, View> tmpPositionMap = (HashMap<Integer, View>) mViewPositionMap.clone();
            int from = mDragPosition < position ? mDragPosition : position;
            int to = mDragPosition < position ? position : mDragPosition;
            int deltaPos = mDragPosition < position ? 1 : -1;
            for (int i = from; i <= to; i++) {
                final View child = tmpPositionMap.get(i);
                if (child != null) {
                    final int fromX = (int) child.getTranslationX();
                    int originPosition = mOriginPositionMap.get(child);
                    int toPosition = i - deltaPos;
                    if (i == mDragPosition) {
                        toPosition = i + deltaPos * (to - from);
                    }
                    mViewPositionMap.put(toPosition, child);
                    final int toX = child.getWidth() * (toPosition - originPosition);
                    if (fromX != toX) {
                        ObjectAnimator animator = ObjectAnimator.ofFloat(child, "translationX", fromX, toX);
                        animator.addListener(new AnimatorListenerAdapter() {
                            @Override
                            public void onAnimationCancel(Animator arg0) {
                                child.setTranslationX(toX);
                            }
                        });
                        animList.add(animator);
                    }
                }
            }
            if (animList.isEmpty()) {
                mPointToNewPositionAnim = null;
            } else {
                mPointToNewPositionAnim.playTogether(animList);
                mPointToNewPositionAnim.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator arg0) {
                        mPointToNewPositionAnim = null;
                    }
                });
                mPointToNewPositionAnim.start();
            }
            mDragPosition = position;
        }
    }

    public boolean startDrag(View v) {
        if (mAllowDrag && !mDragAnimPlaying && null == mDragView) {
            ShadowBitmapView iconView = (ShadowBitmapView) v;
            mDragView = iconView;
            mDragPosition = mOriginPositionMap.get(v);
            iconView.setVisibility(View.INVISIBLE);
            startDrag(EMPTY_CLIP_DATA, new IconDragShadowBuilder(iconView), DRAG_APPICON_TILE, 0);
            if (mOnDragListener != null) {
                mOnDragListener.onDragStart();
            }
            return true;
        }
        return false;
    }

    private ViewGroup getRootContentView() {
        if (mRootView == null) {
            mRootView = (ViewGroup) getRootView();
        }
        return mRootView;
    }

    public void endDrag(final int fromX, final int fromY) {
        if (mOnDragListener != null) {
            mOnDragListener.onDragEnd();
        }
        if (mDragView == null) {
            return;
        }
        final ImageView animView = new ImageView(mContext);
        animView.setImageBitmap(mDragView.getShadowBitmap());
        final int viewSize = (int) (IconDragShadowBuilder.ICON_SCALE * mIconSize);
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(viewSize, viewSize);
        lp.leftMargin = fromX - viewSize / 2;
        lp.topMargin = fromY - viewSize / 2;
        animView.setVisibility(View.INVISIBLE);
        getRootContentView().addView(animView, lp);
        int[] loc = new int[2];
        mDragView.getLocationInWindow(loc);
        final int toX = loc[0] - fromX + mDragView.getWidth() / 2;
        final int toY = loc[1] - fromY + mDragView.getHeight() / 2;
        if (mPointToNewPositionAnim != null) {
            mPointToNewPositionAnim.cancel();
        }
        animView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                PropertyValuesHolder transX = PropertyValuesHolder.ofFloat("translationX", 0, toX);
                PropertyValuesHolder transY = PropertyValuesHolder.ofFloat("translationY", 0, toY);
                PropertyValuesHolder scaleX = PropertyValuesHolder.ofFloat("scaleX", 1f, 1f / IconDragShadowBuilder.ICON_SCALE);
                PropertyValuesHolder scaleY = PropertyValuesHolder.ofFloat("scaleY", 1f, 1f / IconDragShadowBuilder.ICON_SCALE);
                mDragEndAnim = ObjectAnimator.ofPropertyValuesHolder(animView, transX, transY, scaleX, scaleY);
                DecelerateInterpolator interpolator = new DecelerateInterpolator(1.5f);
                mDragEndAnim.setInterpolator(interpolator);
                mDragEndAnim.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationStart(Animator animation) {
                        animView.setVisibility(View.VISIBLE);
                        mDragAnimPlaying = true;
                    }

                    @Override
                    public void onAnimationEnd(Animator arg0) {
                        getRootContentView().removeView(animView);
                        if (mDragView != null) {
                            mDragView.setVisibility(View.VISIBLE);
                            mDragView = null;
                        }
                        mDragAnimPlaying = false;
                    }
                });
                mDragEndAnim.start();
            }
        });
        savePosition();
        mDragPosition = -1;
    }

    public void setAllowDrag(boolean allow) {
        mAllowDrag = allow;
    }

    private void savePosition() {
        int childCount = getChildCount();
        removeAllViews();
        List<ShareItem> shareItems = new ArrayList<ShareItem>();
        for (int i = 0; i < childCount; i++) {
            View child = mViewPositionMap.get(i);
            if (child != null) {
                child.setTranslationX(0);
                if (child.getParent() instanceof ViewGroup) {
                    ((ViewGroup) child.getParent()).removeView(child);
                }
                addView(child);
                ShareItem item = (ShareItem) child.getTag();
                shareItems.add(item);
            }
        }
        if (mOnSortChangeListener != null) {
            mOnSortChangeListener.OnSortChange(shareItems);
        }
    }

    public void setOnSortChangeListener(OnSortChangeListener l) {
        mOnSortChangeListener = l;
    }

    public void setOnDragListener(OnDragListener l) {
        mOnDragListener = l;
    }

    private int findPosition(int x, int y) {
        int position = -1;
        Rect rect = new Rect();
        getHitRect(rect);
        int count = getChildCount();
        if (rect.contains(x, y)) {
            position = x / (mCellWidth + mGapX);
        }
        if (position < 0) {
            position = 0;
        } else if (position >= count) {
            position = count - 1;
        }
        return position;
    }

    public interface OnSortChangeListener {
        void OnSortChange(List<ShareItem> shareItems);
    }

    public interface OnDragListener {
        void onDragStart();

        void onDragEnd();
    }

    public interface OnShareItemClickListener {
        void onShareItemClick(ShareItem shareItem);
    }
}
