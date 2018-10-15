package com.smartisanos.sara.widget;
import android.graphics.Canvas;
import android.graphics.Point;
import android.os.PowerManager;
import android.os.SystemClock;
import android.view.animation.Animation;
import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import com.smartisanos.sara.R;
import com.smartisanos.sara.util.CubicInterpolator;
import com.smartisanos.sara.util.LogUtils;

import java.util.HashSet;
import java.util.List;

/**
 * this listView mainly used for sliding selection
 */
public class SlideListView extends QuickListView implements
        MultiDeleteAnimation.MultiDeleteAnimationOperator {

    private static String TAG = "VoiceAss.SlideListView";
    /**
     * the checkBox's id, which in list item view.
     */
    private int mCheckboxId;
    private Listener mListener;
    private boolean mIsChecked = false;
    /**
     * Store the checkBox's position
     */
    private int[] mTempLoc = new int[2];

    private final int MISS = -1;
    private final int SELECT = 1;
    private final int SPECIAL = -2;
    private int mState = MISS;

    private final int DEFAULT_PRE_POSITION = -2;
    /**
     * Store the previous list item position, avoiding execute same operation many times.
     */
    private int mPrePosition = DEFAULT_PRE_POSITION;

    private int mStartX;
    private int mEndX;

    /**
     * used to determine whether the slide select enable or not.
     */
    private boolean mSlideEnable;

    private TouchMonitorListener mTouchMonitorListener;

    private PowerManager mPowerManager;
    private long mPrePokeTime;
    private boolean hideQuickBar = false;

    public SlideListView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context, attrs);
    }

    public SlideListView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public SlideListView(Context context) {
        super(context);
    }

    private void init(Context context, AttributeSet attrs) {
        if (attrs == null) {
            return;
        }
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.SliderListView, 0, 0);
        if (a != null) {
            mCheckboxId = a.getResourceId(R.styleable.SliderListView_slider_checkbox_id, 0);
            mSlideEnable = a.getBoolean(R.styleable.SliderListView_slider_enabled, false);
            a.recycle();
        }
        mScroller = new SelectScroller();
        mPowerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
    }

    public interface Listener {
        public void setChecked(int position, boolean isChecked);

        public boolean isChecked(int position);
    }

    public interface TouchMonitorListener {
        public void onTouchActionUp();
    }

    public void setTouchMonitorListener(TouchMonitorListener listener) {
        mTouchMonitorListener = listener;
    }

    public void setSlideListener(Listener listener) {
        mListener = listener;
    }

    public void setSlideEnable(boolean enable) {
        mSlideEnable = enable;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (!mSlideEnable) {
            return super.onInterceptTouchEvent(ev);
        }

        int position = startCheckPosition(ev);
        int action = ev.getActionMasked();

        // if the list item has checkBox and the touch spot is located in it,
        // it will intercept {@link MotionEvent#ACTION_DOWN}.
        if (position > MISS && action == MotionEvent.ACTION_DOWN) {
            return true;
        }
        // if the touch spot can't be located in checkBox, it will invoke
        // {@link super#onInterceptTouchEvent(ev)}.
        return super.onInterceptTouchEvent(ev);
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        if (processQuickBar(ev.getAction())) {
            // if the QuickBar is shown, we just need to hide QuickBar.
            return true;
        }

        if (!mSlideEnable) {
            return super.onTouchEvent(ev);
        }
        if (LogUtils.DEBUG)
            LogUtils.d(TAG, "onTouchEvent   ev  " + ev.getX() + "   " + ev.getY());
        int position = startCheckPosition(ev);
        if (LogUtils.DEBUG)
            LogUtils.d(TAG, "onTouchEvent   position  " + position);
        int x = (int) ev.getX();
        int y = (int) ev.getY();
        mCurrPoint.set(x, y);

        int action = ev.getActionMasked();
        switch (action) {
            case MotionEvent.ACTION_DOWN: {
                mPrePosition = DEFAULT_PRE_POSITION;
                if (LogUtils.DEBUG)
                    LogUtils.d(TAG, "action down ............................................");
                mDownPoint.set(x, y);
                // if the down point located in header view or other place, but checkbox area.
                // we will do noting, just invoke the super method.
                if (position == SPECIAL || (position == MISS && mState == MISS)) {
                    break;
                }

                onDown(position);
                return true;
            }
            case MotionEvent.ACTION_MOVE: {
                boolean flag = position == SPECIAL || (position == MISS && mState == MISS);
                if (mState != SELECT && !flag) {
                    // if the down point don't located in checkbox area, then we move to the
                    // checkbox area, we should set the init-state for selection.
                    onDown(position);
                    return true;
                }

                if (mState == SELECT && position == SPECIAL) {
                    return true;
                } else if (flag) {
                    break;
                }

                continueSelect(x, y, position);
                return true;
            }
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (mTouchMonitorListener != null
                        && (mState == SELECT || pointToPosition(x, y) == pointToPosition(
                                mDownPoint.x, mDownPoint.y))) {
                    mTouchMonitorListener.onTouchActionUp();
                }
                recovery();
                break;
        }

        return super.onTouchEvent(ev);
    }

    @Override
    protected void onVisibilityChanged(View changedView, int visibility) {
        super.onVisibilityChanged(changedView, visibility);
        // If we press HOME key when ListView in auto-scroll state, it will
        // continue scroll, although it exit edit mode. So we need to stop scroll
        // when it's visibility is not visible.
        if (visibility != View.VISIBLE) {
            recovery();
        }
    }

    private int startCheckPosition(MotionEvent ev) {
        return viewIdHitPosition(ev, mCheckboxId);
    }

    private void onDown(int position) {
        // Avoid touch on Scrolling.
        if (mListener == null || position <= MISS || mPrePosition == position) {
            return;
        }
        mState = SELECT;
        mIsChecked = mListener.isChecked(position);

        // click the checkBox quickly, it can't invoke {@link #onMovint(int position)}.
        // But we want the checkBox to change it's checkable. So if the condition is met,
        // the checkBox will change it's checkable when touch down.
        // See bug: 0008936
        onMoving(position);
    }

    private void onMoving(int position) {
        if (mListener == null || position <= MISS || mPrePosition == position) {
            return;
        }
        if (mState == SELECT) {
            if (mPrePosition == DEFAULT_PRE_POSITION) {
                if (LogUtils.DEBUG)
                    LogUtils.d(TAG, "mPrePosition == DEFAULT_PRE_POSITION  position  " + position
                            + "  set to be " + !mIsChecked);
                mListener.setChecked(position, !mIsChecked);
            } else if (mPrePosition > position) {
                for (int pos = mPrePosition - 1; pos >= position; pos--) {
                    if (pos >= getFirstVisiblePosition() && pos <= getLastVisiblePosition()) {
                        if (LogUtils.DEBUG)
                            LogUtils.d(TAG, "mPrePosition > position position  " + position
                                    + "  set to be " + !mIsChecked);
                        mListener.setChecked(pos, !mIsChecked);
                    }
                }
            } else {
                for (int pos = mPrePosition + 1; pos <= position; pos++) {
                    if (pos >= getFirstVisiblePosition() && pos <= getLastVisiblePosition()) {
                        if (LogUtils.DEBUG)
                            LogUtils.d(TAG, "mPrePosition > position position  " + position
                                    + "  set to be " + !mIsChecked);
                        mListener.setChecked(pos, !mIsChecked);
                    }
                }
            }
        }
        pokeActivity();
        mPrePosition = position;
    }

    private void pokeActivity() {
        long currentTime = SystemClock.uptimeMillis();
        // min time screen off is 15s, so we use 12s.
        if (currentTime - mPrePokeTime > 12000) {
            mPowerManager.userActivity(currentTime, true);
            mPrePokeTime = currentTime;
        }
    }

    private void recovery() {
        if (LogUtils.DEBUG)
            LogUtils.d(TAG, "recovery()   mState  " + mState);
        if (mState == MISS) {
            return;
        }

        if (mScroller != null && mScroller.isScrolling()) {
            mScroller.stopScroll();
        }
        mIsChecked = false;
        mState = MISS;
        mPrePosition = DEFAULT_PRE_POSITION;
        mStartX = 0;
        mEndX = 0;
    }

    public boolean isSelecting() {
        return mState == SELECT;
    }

    /**
     * check the touch spot whether be located in checkBox or not.
     * 
     * @param ev
     * @param id the resource id of checkBox.
     * @return if the touch spot is located in checkBox, it will return the occurred list item's
     *         position; Otherwise, it will return {@link MISS}
     */
    private int viewIdHitPosition(MotionEvent ev, int id) {

        final int x = (int) ev.getX();
        final int y = (int) ev.getY();

        // includes headers/footers
        int touchPos = pointToPosition(x, y);

        final int numHeaders = getHeaderViewsCount();
        final int numFooters = getFooterViewsCount();
        final int count = getCount();

        // We're only interested if the touch was on an item that's not a header
        // or footer.
        if (touchPos != AdapterView.INVALID_POSITION && touchPos >= numHeaders
                && touchPos < (count - numFooters)) {
            final int rawX = (int) ev.getRawX();

            final int rawY = (int) ev.getRawY();
            final View item = getChildAt(touchPos - getFirstVisiblePosition());

            View checkBox = id == 0 ? null : (View) item.findViewById(id);
            if (checkBox != null && (checkBox.getVisibility() == View.VISIBLE)) {
                // if in select state, we only need consider the distance of X
                // direction.
                if (mState == SELECT && rawX > mStartX && rawX < mEndX) {
                    return touchPos;
                }

                checkBox.getLocationOnScreen(mTempLoc);

                if (rawX > mTempLoc[0] && rawY > mTempLoc[1]
                        && rawX < mTempLoc[0] + checkBox.getWidth()
                        && rawY < mTempLoc[1] + checkBox.getHeight()) {

                    mStartX = mTempLoc[0];
                    mEndX = mTempLoc[0] + checkBox.getWidth();

                    return touchPos;
                }
            } else if (checkBox == null && mState == SELECT && rawX > mStartX && rawX < mEndX) {
                // if the occurred item is headers/footers. it will not scroll and not return it's
                // position.
                return SPECIAL;
            }
        }

        return MISS;
    }

    // /////////////////////////////////////////////////////////////////////////////////////

    private SelectScroller mScroller;

    /**
     * Determines the start of the upward select-scroll region at the top of the ListView. Specified
     * by a fraction of the ListView height, thus screen resolution agnostic.
     */
    private float mSelectUpScrollStartFrac = 1.0f / 5.0f;

    /**
     * Determines the start of the downward select-scroll region at the bottom of the ListView.
     * Specified by a fraction of the ListView height, thus screen resolution agnostic.
     */
    private float mSelectDownScrollStartFrac = 1.0f / 5.0f;

    /**
     * the speed cardinal number, determine the speed's value.
     */
    private final float SPEED_CARDINAL = 0.01f;

    private Point mDownPoint = new Point();
    private Point mCurrPoint = new Point();

    /**
     * the start of the downward select-scroll region.
     */
    private float mDownScrollStartY;

    /**
     * the start of the upward select-scroll region.
     */
    private float mUpScrollStartY;

    private void initScrollData() {
        final int padTop = getPaddingTop();
        final int listHeight = getHeight() - padTop - getPaddingBottom();
        float heightF = (float) listHeight;

        mUpScrollStartY = padTop + mSelectUpScrollStartFrac * heightF;
        mDownScrollStartY = padTop + (1.0f - mSelectDownScrollStartFrac) * heightF;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        initScrollData();
    }

    private void continueSelect(int x, int y, int position) {
        int deltaY = y - mDownPoint.y;
        if (y > mDownScrollStartY && deltaY > 0) {
            mScroller.startScroll(SelectScroller.DOWN);
            return;
        } else if (y < mUpScrollStartY && deltaY < 0) {
            mScroller.startScroll(SelectScroller.UP);
            return;
        } else if (y > mUpScrollStartY && y < mDownScrollStartY) {
            mScroller.stopScroll();
        }

        onMoving(position);
    }

    private final class SelectScroller implements Runnable {

        private boolean mAbort;
        private int mScrollDir;
        private boolean mScrolling = false;
        private float mScrollSpeed;

        private long mPrevTime;
        private long mCurrTime;

        public static final int UP = 1;
        public static final int DOWN = 2;

        public void startScroll(int dir) {
            if (LogUtils.DEBUG)
                LogUtils.d(TAG, "startScroll.........................");
            if (!mScrolling) {
                mScrolling = true;
                mAbort = false;
                mScrollDir = dir;
                mPrevTime = SystemClock.uptimeMillis();
                post(this);
            }
        }

        public void stopScroll() {
            if (LogUtils.DEBUG)
                LogUtils.d(TAG, "stopScroll.........................");
            SlideListView.this.removeCallbacks(this);
            mScrolling = false;
        }

        public boolean isScrolling() {
            return mScrolling;
        }

        @Override
        public void run() {
            if (mAbort) {
                return;
            }

            int first = getFirstVisiblePosition();
            int last = getLastVisiblePosition();
            int count = getCount();
            int padTop = getPaddingTop();
            final int listHeight = getHeight() - padTop - getPaddingBottom();

            performSelectAction();

            if (mScrollDir == UP) {
                View v = getChildAt(0);
                if (v == null || (first == 0 && v.getTop() == padTop)) {
                    mScrolling = false;
                    return;
                }

                int yDelta = mCurrPoint.y < padTop ? padTop : mCurrPoint.y;
                mScrollSpeed = (yDelta - mUpScrollStartY) * SPEED_CARDINAL;
            } else {
                View v = getChildAt(last - first);
                if (v == null || (last == count - 1 && v.getBottom() <= listHeight + padTop)) {
                    mScrolling = false;
                    return;
                }

                int y = getHeight() - getPaddingBottom();
                int yDelta = mCurrPoint.y > y ? y : mCurrPoint.y;
                mScrollSpeed = (yDelta - mDownScrollStartY) * SPEED_CARDINAL;
            }

            mCurrTime = SystemClock.uptimeMillis();
            float dt = mCurrTime - mPrevTime;
            int dy = Math.round(mScrollSpeed * dt);

            smoothScrollBy(dy, (int) dt);
            invalidate();

            mPrevTime = mCurrTime;
            post(this);
        }

        /**
         * select or unSelect the item when scrolling.
         */
        private void performSelectAction() {
            if (LogUtils.DEBUG)
                LogUtils.d(TAG, "performSelectAction().....................  ");
            if (mCurrPoint.y < getPaddingTop()) {
                if (LogUtils.DEBUG)
                    LogUtils.d(TAG, "mCurrPoint is above list view");
                onMoving(getFirstVisiblePosition());
                return;
            } else if (mCurrPoint.y > (getHeight() - getPaddingBottom())) {
                if (LogUtils.DEBUG)
                    LogUtils.d(TAG, "mCurrPoint is below list view");
                onMoving(getLastVisiblePosition());
                return;
            }
            int position = pointToPosition(mCurrPoint.x, mCurrPoint.y);
            View child = getChildAt(position - getFirstVisiblePosition());
            if (child != null) {
                View cb = child.findViewById(mCheckboxId);
                if (cb != null) {
                    onMoving(position);
                }
            }
        }

    }

    private ListViewCollapseRunner mCollapseRunner;

    @Override
    public void draw(Canvas canvas) {
        if (mCollapseRunner != null) {
            boolean ended = mCollapseRunner.compute();
            mCollapseRunner.draw(canvas);
            invalidate();
            if (ended) {
                mCollapseRunner = null;
            }
        } else {
            super.draw(canvas);
        }
    }

    @Override
    public void startDeleteAnimations(List<Integer> reservedPositions,
            Animation.AnimationListener animationListener) {
        mCollapseRunner = new ListViewCollapseRunner(this, reservedPositions, animationListener);
        mCollapseRunner.setInterpolator(CubicInterpolator.OUT);
        mCollapseRunner.setHeaderMode(isHeaderMode());
        mCollapseRunner.setDeletedHeaderSection(mDeletedHeaderSections);
        mCollapseRunner.start();
        invalidate();
    }

    protected boolean isHeaderMode() {
        return false;
    }

    private HashSet<String> mDeletedHeaderSections;

    public void setDeletedHeaderSections(HashSet<String> deletedHeaderSections) {
        mDeletedHeaderSections = deletedHeaderSections;
    }

    private boolean processQuickBar(int action) {
        switch (action) {
            case MotionEvent.ACTION_DOWN: {
                hideQuickBar = false;
                if (mQuickBar != null && mQuickBar.isLetterGridShow()) {
                    hideQuickBar = true;
                }
                break;
            }
            case MotionEvent.ACTION_UP: {
                if (hideQuickBar && mQuickBar != null) {
                    mQuickBar.hideLetterGrid();
                }
            }
        }
        return hideQuickBar;
    }
}
