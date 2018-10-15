package com.smartisanos.sara.widget;
import java.util.LinkedList;
import java.util.List;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.os.Handler;
import android.os.Vibrator;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.ViewTreeObserver.OnPreDrawListener;
import android.view.WindowManager;
import android.view.animation.DecelerateInterpolator;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.ListAdapter;

public class DragGridView extends NoScrollGridView {
    private long dragResponseMS = 2000;
    private float MAX_VELOCITY = 300.0f;

    private boolean isDrag = false;
    private boolean mIsSqueezeModeEnabled = false;

    private int mDownX;
    private int mDownY;
    private int moveX;
    private int moveY;

    private int mDragPosition;

    private View mStartDragItemView = null;

    private ImageView mDragImageView;

    private Vibrator mVibrator;

    private WindowManager mWindowManager;

    private WindowManager.LayoutParams mWindowLayoutParams;

    private Bitmap mDragBitmap;

    private int mPoint2ItemTop;

    private int mPoint2ItemLeft;

    private int mOffset2Top;

    private int mOffset2Left;

    private int mStatusHeight;

    private int mDownScrollBorder;

    private int mUpScrollBorder;

    private static final int speed = 20;

    private boolean mAnimationEnd = true;

    private DragGridBaseAdapter mDragAdapter;
    private int mNumColumns;
    private int mColumnWidth;
    private boolean mNumColumnsSet;
    private int mHorizontalSpacing;

    private VelocityTracker mVelocityTracker;

    public DragGridView(Context context) {
        this(context, null);
    }

    public DragGridView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public DragGridView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        mVibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        mWindowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        mStatusHeight = getStatusHeight(context);

        if (!mNumColumnsSet) {
            mNumColumns = AUTO_FIT;
        }

    }

    private Handler mHandler = new Handler();

    private Runnable mLongClickRunnable = new Runnable() {

        @Override
        public void run() {
            isDrag = true;
            mStartDragItemView.setVisibility(View.INVISIBLE);

            calculateDxDy();
            if (mGridViewDragListener != null) {
                mGridViewDragListener.onDragStart(mDragPosition, mLastX, mLastY);
            }
        }
    };

    int mDx, mDy;

    private void calculateDxDy() {
        Rect rect = new Rect();
        mStartDragItemView.getGlobalVisibleRect(rect);
        mDx = rect.centerX() - mLastX;
        mDy = rect.centerY() - mLastY;
    }

    @Override
    public void setAdapter(ListAdapter adapter) {
        super.setAdapter(adapter);

        if (adapter instanceof DragGridBaseAdapter) {
            mDragAdapter = (DragGridBaseAdapter) adapter;
        } else {
            throw new IllegalStateException("the adapter must be implements DragGridAdapter");
        }
    }


    @Override
    public void setNumColumns(int numColumns) {
        super.setNumColumns(numColumns);
        mNumColumnsSet = true;
        this.mNumColumns = numColumns;
    }


    @Override
    public void setColumnWidth(int columnWidth) {
        super.setColumnWidth(columnWidth);
        mColumnWidth = columnWidth;
    }


    @Override
    public void setHorizontalSpacing(int horizontalSpacing) {
        super.setHorizontalSpacing(horizontalSpacing);
        this.mHorizontalSpacing = horizontalSpacing;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (mNumColumns == AUTO_FIT) {
            int numFittedColumns;
            if (mColumnWidth > 0) {
                int gridWidth = Math.max(MeasureSpec.getSize(widthMeasureSpec) - getPaddingLeft()
                        - getPaddingRight(), 0);
                numFittedColumns = gridWidth / mColumnWidth;
                if (numFittedColumns > 0) {
                    while (numFittedColumns != 1) {
                        if (numFittedColumns * mColumnWidth + (numFittedColumns - 1)
                                * mHorizontalSpacing > gridWidth) {
                            numFittedColumns--;
                        } else {
                            break;
                        }
                    }
                } else {
                    numFittedColumns = 1;
                }
            } else {
                numFittedColumns = 2;
            }
            mNumColumns = numFittedColumns;
        }

        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    public void setDragResponseMS(long dragResponseMS) {
        this.dragResponseMS = dragResponseMS;
    }

    int mLastX, mLastY;

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        switch (ev.getAction()) {
            case MotionEvent.ACTION_DOWN:
                mDownX = (int) ev.getX();
                mDownY = (int) ev.getY();
                mLastX = (int) ev.getRawX();
                mLastY = (int) ev.getRawY();

                mDragPosition = pointToPosition(mDownX, mDownY);


                if (mDragPosition == AdapterView.INVALID_POSITION) {
                    return super.dispatchTouchEvent(ev);
                }

                mHandler.postDelayed(mLongClickRunnable, dragResponseMS);

                mStartDragItemView = getChildAt(mDragPosition - getFirstVisiblePosition());

                mPoint2ItemTop = mDownY - mStartDragItemView.getTop();
                mPoint2ItemLeft = mDownX - mStartDragItemView.getLeft();

                mOffset2Top = (int) (ev.getRawY() - mDownY);
                mOffset2Left = (int) (ev.getRawX() - mDownX);

                mDownScrollBorder = getHeight() / 5;
                mUpScrollBorder = getHeight() * 4 / 5;


                mStartDragItemView.setDrawingCacheEnabled(true);
                mDragBitmap = Bitmap.createBitmap(mStartDragItemView.getDrawingCache());
                mStartDragItemView.destroyDrawingCache();


                break;
            case MotionEvent.ACTION_MOVE:
                int moveX = (int) ev.getX();
                int moveY = (int) ev.getY();
                mLastX = (int) ev.getRawX();
                mLastY = (int) ev.getRawY();

                if (!isTouchInItem(mStartDragItemView, moveX, moveY)) {
                    mHandler.removeCallbacks(mLongClickRunnable);
                }
                break;
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
                mHandler.removeCallbacks(mLongClickRunnable);
                mHandler.removeCallbacks(mScrollRunnable);
                break;
        }
        return super.dispatchTouchEvent(ev);
    }

    private boolean isTouchInItem(View dragView, int x, int y) {
        if (dragView == null) {
            return false;
        }
        int leftOffset = dragView.getLeft();
        int topOffset = dragView.getTop();
        if (x < leftOffset || x > leftOffset + dragView.getWidth()) {
            return false;
        }

        if (y < topOffset || y > topOffset + dragView.getHeight()) {
            return false;
        }

        return true;
    }


    private boolean mIsActionDown = false;

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (mVelocityTracker == null) {
            mVelocityTracker = VelocityTracker.obtain();
        }
        mVelocityTracker.addMovement(event);
        if (isDrag /*&& mDragImageView != null*/) {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    mIsActionDown = true;
                    break;
                case MotionEvent.ACTION_MOVE:
                    moveX = (int) event.getX();
                    moveY = (int) event.getY();
                    mVelocityTracker.computeCurrentVelocity(1000);
                    float velocityX = mVelocityTracker.getXVelocity();
                    float velocityY = mVelocityTracker.getYVelocity();
                    if (Math.abs(velocityX) < MAX_VELOCITY && Math.abs(velocityY) < MAX_VELOCITY) {
                        onDragItem(moveX, moveY);
                    }

                    if (mGridViewDragListener != null) {
                        mGridViewDragListener.onDraging(event);
                    }
                    break;
                case MotionEvent.ACTION_CANCEL:
                case MotionEvent.ACTION_UP:
                    mIsActionDown = false;
                    if (mVelocityTracker != null) {
                        mVelocityTracker.recycle();
                        mVelocityTracker = null;
                    }
                    moveX = (int) event.getX();
                    moveY = (int) event.getY();
                    onDragItem(moveX, moveY);
                    onStopDrag();
                    isDrag = false;
                    break;
            }
            return true;
        }
        return super.onTouchEvent(event);
    }



    private void onDragItem(int moveX, int moveY) {

        if (mIsSqueezeModeEnabled) {
            onSwapItem(moveX, moveY);
        }

    }

    private Runnable mScrollRunnable = new Runnable() {

        @Override
        public void run() {
            int scrollY;
            if (getFirstVisiblePosition() == 0 || getLastVisiblePosition() == getCount() - 1) {
                mHandler.removeCallbacks(mScrollRunnable);
            }

            if (moveY > mUpScrollBorder) {
                scrollY = speed;
                mHandler.postDelayed(mScrollRunnable, 25);
            } else if (moveY < mDownScrollBorder) {
                scrollY = -speed;
                mHandler.postDelayed(mScrollRunnable, 25);
            } else {
                scrollY = 0;
                mHandler.removeCallbacks(mScrollRunnable);
            }

        }
    };

    private void onSwapItem(int moveX, int moveY) {
        final int tempPosition = pointToPosition(moveX + mDx, moveY + mDy);

        if (tempPosition != mDragPosition && tempPosition != AdapterView.INVALID_POSITION && mAnimationEnd) {
            mDragAdapter.reorderItems(mDragPosition, tempPosition);
            mDragAdapter.setHideItem(tempPosition);

            final ViewTreeObserver observer = getViewTreeObserver();
            observer.addOnPreDrawListener(new OnPreDrawListener() {

                @Override
                public boolean onPreDraw() {
                    observer.removeOnPreDrawListener(this);
                    animateReorder(mDragPosition, tempPosition);
                    mDragPosition = tempPosition;
                    return true;
                }
            });

        }
    }

    private AnimatorSet createTranslationAnimations(View view, float startX,
                                                    float endX, float startY, float endY) {
        ObjectAnimator animX = ObjectAnimator.ofFloat(view, "translationX",
                startX, endX);
        ObjectAnimator animY = ObjectAnimator.ofFloat(view, "translationY",
                startY, endY);
        AnimatorSet animSetXY = new AnimatorSet();
        animSetXY.playTogether(animX, animY);
        return animSetXY;
    }


    private void animateReorder(final int oldPosition, final int newPosition) {
        boolean isForward = newPosition > oldPosition;
        List<Animator> resultList = new LinkedList<Animator>();
        if (isForward) {
            for (int pos = oldPosition; pos < newPosition; pos++) {
                View view = getChildAt(pos - getFirstVisiblePosition());

                if ((pos + 1) % mNumColumns == 0) {
                    resultList.add(createTranslationAnimations(view,
                            -view.getWidth() * (mNumColumns - 1), 0,
                            view.getHeight(), 0));
                } else {
                    resultList.add(createTranslationAnimations(view,
                            view.getWidth(), 0, 0, 0));
                }
            }
        } else {
            for (int pos = oldPosition; pos > newPosition; pos--) {
                View view = getChildAt(pos - getFirstVisiblePosition());
                if ((pos + mNumColumns) % mNumColumns == 0) {
                    resultList.add(createTranslationAnimations(view,
                            view.getWidth() * (mNumColumns - 1), 0,
                            -view.getHeight(), 0));
                } else {
                    resultList.add(createTranslationAnimations(view,
                            -view.getWidth(), 0, 0, 0));
                }
            }
        }

        AnimatorSet resultSet = new AnimatorSet();
        resultSet.playTogether(resultList);
        resultSet.setDuration(400);
        resultSet.setInterpolator(new DecelerateInterpolator(1.5f));
        resultSet.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                mAnimationEnd = false;
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                mAnimationEnd = true;
                if (mIsActionDown) {
                    onStopDrag();
                }
            }
        });
        resultSet.start();
    }

    private void onStopDrag() {
        if (mGridViewDragListener != null) {
            mGridViewDragListener.onDragEnd(mDragAdapter.getHidePosition());
        } else {
            View view = getChildAt(mDragPosition - getFirstVisiblePosition());
            if (view != null) {
                view.setVisibility(View.VISIBLE);
            }
            mDragAdapter.setHideItem(-1);
        }
    }

    private static int getStatusHeight(Context context) {
        int statusHeight = 0;
        Rect localRect = new Rect();
        ((Activity) context).getWindow().getDecorView().getWindowVisibleDisplayFrame(localRect);
        statusHeight = localRect.top;
        if (0 == statusHeight) {
            Class<?> localClass;
            try {
                localClass = Class.forName("com.android.internal.R$dimen");
                Object localObject = localClass.newInstance();
                int i5 = Integer.parseInt(localClass.getField("status_bar_height").get(localObject).toString());
                statusHeight = context.getResources().getDimensionPixelSize(i5);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return statusHeight;
    }

    private GridViewDragListener mGridViewDragListener;

    public void setGridViewDragListener(GridViewDragListener listener) {
        mGridViewDragListener = listener;
    }

    interface GridViewDragListener {

        public void onDragStart(int position, int rawX, int rawY);

        public void onDraging(MotionEvent ev);

        public void onDragEnd(int position);
    }

    public interface DragGridBaseAdapter {
        public void reorderItems(int oldPosition, int newPosition);
        public void setHideItem(int hidePosition);
        public int getHidePosition();

    }

}
