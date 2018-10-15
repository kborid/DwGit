package com.smartisanos.ideapills.view;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.app.SmtPCUtils;
import android.content.Context;
import android.content.res.Configuration;
import android.database.DataSetObserver;
import android.graphics.Canvas;
import android.graphics.PixelFormat;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.Region;
import android.os.Vibrator;
import android.provider.Settings;
import android.util.AttributeSet;
import android.view.DragEvent;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.LayoutAnimationController;
import android.view.animation.TranslateAnimation;
import android.view.inputmethod.InputMethodManager;
import android.widget.AbsListView;
import android.widget.FrameLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.Space;
import android.provider.Settings;

import com.smartisanos.ideapills.BubbleController;
import com.smartisanos.ideapills.Constants;
import com.smartisanos.ideapills.IdeaPillsApp;
import com.smartisanos.ideapills.R;
import com.smartisanos.ideapills.SidebarMode;
import com.smartisanos.ideapills.common.anim.Anim;
import com.smartisanos.ideapills.common.anim.AnimCancelableListener;
import com.smartisanos.ideapills.common.anim.AnimListener;
import com.smartisanos.ideapills.common.anim.SimpleAnimListener;
import com.smartisanos.ideapills.common.util.CommonUtils;
import com.smartisanos.ideapills.entity.BubbleItem;
import com.smartisanos.ideapills.util.BubbleTrackerID;
import com.smartisanos.ideapills.entity.BubbleObserver;
import com.smartisanos.ideapills.util.GlobalBubbleManager;
import com.smartisanos.ideapills.util.GlobalBubbleUtils;
import com.smartisanos.ideapills.util.InsertSortArray;
import com.smartisanos.ideapills.util.LOG;
import com.smartisanos.ideapills.util.StatusManager;
import com.smartisanos.ideapills.common.anim.AnimTimeLine;
import com.smartisanos.ideapills.common.anim.Vector3f;
import com.smartisanos.ideapills.BubbleController;
import com.smartisanos.ideapills.util.BubbleTrackerID;
import com.smartisanos.ideapills.util.LOG;
import com.smartisanos.ideapills.util.Tracker;
import com.smartisanos.ideapills.util.Utils;
import com.smartisanos.ideapills.common.util.UIHandler;
import com.smartisanos.ideapills.util.ViewUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import smartisanos.api.VibEffectSmt;
import smartisanos.util.SidebarUtils;

public class BubbleListView extends ListView {
    private static final LOG log = LOG.getInstance(BubbleListView.class);
    private static final int ANIMDURATION = 250;
    private static final int DRAG_STATE_NONE = 0;
    private static final int DRAG_STATE_MOVE = 1;
    public static final int sDarkColor = 0x9a;

    private static final int ANIM_NONE = 0;
    private static final int ANIM_SHOW = 1;
    private static final int ANIM_HIDE = 2;

    private static final int FOOT_SPACE_H = 200;

    private static final int DRAG_ATTACH_ADD_TO_BUBBLE = 0;
    private static final int DRAG_ATTACH_INSERT_NEW_BUBBLE = 1;

    BubbleAdapter mBubbleAdapter = null;
    private Space mHeadSpace = null;
    private Space mFootSpace = null;
    private BubbleHeadOperator mBubbleHeadOperator = null;
    PullViewGroup mPullViewGroup = null;

    private View mOptHeadView = null;
    private View mSuspendedHeadView = null;
    private Rect mCurInsertRect;
    private int mCurInsertPos;
    private int mStartPos;
    private AnimTimeLine mXAnimTimeLine = null;
    private int mXAnimType = ANIM_NONE;
    private int mOrientation;
    private int[] mDrawOrder = null;
    private float mDownX = -1;
    private float mDownY = -1;
    private int mDragState = DRAG_STATE_NONE;
    private int mNormalListItemViewHeight = -1;
    private boolean mAcceptDragEvent = false;

    // for ime overrlapped the item
    public int mNormalBubbleHeight ;
    public int mLargeBubbleTextHeight ;
    public int mLargeBubbleVoiceHeight ;
    public int mEditBubbleTextHeight;
    public int mEditBubbleVoiceHeight;
    public int mSystemStatusBarHeight ;
    public int mScreenDimensionHeight ;

    private int mBubbleDraggingWidthMax;

    private int mScrollState = 0;
    private boolean mForceStartDragging = false;

    private ValueAnimator mAutoScrollAnim = null;

    private OnSelectChangedListener mOnSelectChangedListener;
    private boolean mWaitingForLayout;
    private InternalOnScrollListener mInternalOnScrollListener;
    
    private boolean mToTopDownAnimFlag = false;
    public interface ShowBubblesListener {
        void showOver();
    }

    public interface OnSelectChangedListener {
        void onSelectChanged(int count);
    }

    public void notifyChanged() {
        mBubbleAdapter.notifyDataSetChanged();
    }

    void onInputOver() {
        mBubbleHeadOperator.setInputEnable(true);
        final InputMethodManager imm = InputMethodManager.peekInstance();
        imm.hideSoftInputFromWindow(getWindowToken(), 0);
        BubbleController.getInstance().updateInputStatus(false);
        mBubbleHeadOperator.updateExpandStatus();
    }

    public int getEditWidth() {
        if (SmtPCUtils.isValidExtDisplayId(getContext())) {
            int phoneWidth = Float.valueOf(mContext.getResources().getDimensionPixelSize(R.dimen.bubbleopt_layout_width)
                    * 1.5f).intValue();
            return phoneWidth;
        } else {
            return getWidth();
        }
    }

    private void refreshData(int delay) {
        UIHandler.removeCallbacks(mRefreshRunable);
        UIHandler.postDelayed(mRefreshRunable, delay);
    }

    private Runnable mRefreshRunable = new Runnable() {
        @Override
        public void run() {
            if (mBubbleAdapter != null) {
                mBubbleAdapter.refreshData();
                if (mBubbleAdapter.getCount() == 0) {
                    toMode(ViewMode.BUBBLE_NORMAL, true);
                }
            }
        }
    };

    private BubbleObserver mBubbleObserver = new BubbleObserver() {
        private List<BubbleItemView> mBeforeEditBubbles = new ArrayList<BubbleItemView>();
        public void onMessage(int msg , BubbleItem item) {
            switch (msg) {
                case BubbleItem.MSG_BEFOREEDIT:
                    if (!BubbleController.getInstance().isExtDisplay()) {
                        UIHandler.postDelayed(new Runnable() {
                            public void run() {
                                hideBubbleListImmediately();
                            }
                        }, 200);
                    }
                    break;
                case BubbleItem.MSG_ONEDIT:
                    mBubbleAdapter.notifyDataSetChanged();
                    break;
                case BubbleItem.MSG_AFTEREDIT:
                    if (!BubbleController.getInstance().isSecondBoom()) {
                        UIHandler.postDelayed(new Runnable() {
                            public void run() {
                                playShowAnimation(null);
                            }
                        }, 300L);
                    }
                    break;
                case BubbleItem.MSG_SELECTED_CHANGE:
                    if (mOnSelectChangedListener != null) {
                        mOnSelectChangedListener.onSelectChanged(mBubbleAdapter.getSelectedBubblesCount());
                    }
                    mBubbleHeadOperator.refreshOptEnable();
                    break;
                case BubbleItem.MSG_TODO_OVER_REVERSE:
                    mBubbleHeadOperator.updateExpandStatus();
                    break;
                case BubbleItem.MSG_TODO_OVER_INPUT:
                    if(isTodoOverHide()){
                        onInputOver();
                        final List<BubbleItem> bubbleItems = new ArrayList<BubbleItem>();
                        bubbleItems.add(item);
                        mBubbleAdapter.hideTodoOverBubble(bubbleItems);
                    }
                    mBubbleHeadOperator.updateExpandStatus();
                    break;
                case BubbleItem.MSG_TODO_OVER:
                    if(isTodoOverHide()){
                        final List<BubbleItem> bubbleItems = new ArrayList<BubbleItem>();
                        bubbleItems.add(item);
                        mBubbleAdapter.hideTodoOverBubble(bubbleItems);
                    }
                    mBubbleHeadOperator.updateExpandStatus();
                    break;
                case BubbleItem.MSG_BUBBLE_DELETE:
                    refreshData(150);
                    break;
                case BubbleItem.MSG_INPUT_OVER:
                    onInputOver();
                    break;
                case BubbleItem.MSG_LARGEMODE_CHANGE:
                    mBubbleHeadOperator.updateExpandStatus();
                    break;
                case BubbleItem.MSG_SHARE_OVER:
                    if (!isAlreadyShow() || mXAnimType == ANIM_HIDE) {
                        UIHandler.postDelayed(new Runnable() {
                            public void run() {
                                playShowAnimation(null);
                            }
                        }, 100L);
                    }
                    break;
                case BubbleItem.MSG_AFTEREDIT_TIME:
                    if (!BubbleController.getInstance().isSecondBoom()) {
                        UIHandler.postDelayed(new Runnable() {
                            public void run() {
                                playShowAnimation(null);
                            }
                        }, 300L);
                    }
            }
        }
    };

    private class InternalOnScrollListener implements OnScrollListener{

        public void onScrollStateChanged(AbsListView view, int scrollState) {
            mScrollState = scrollState;
            notifyRegionChanged();
        }

        public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
            boolean focus = mBubbleHeadOperator.isFocusSearch();
            if (mOptHeadView.getTop() >= 0 && firstVisibleItem <= 1) {
                if (mSuspendedHeadView != null) {
                    mSuspendedHeadView.setVisibility(GONE);
                    mOptHeadView.setVisibility(VISIBLE);
                    mBubbleHeadOperator.setOptView(mOptHeadView, focus);
                    mSuspendedHeadView.setAlpha(1.0f);
                }
            } else {
                if (mSuspendedHeadView == null) {
                    mSuspendedHeadView = LayoutInflater.from(getContext()).inflate(R.layout.bubble_head_item, null);
                    mBubbleListActionCallback.sendCommand(BubbleListActionCallback.MSG_SUSPENDHEADVIEW, mSuspendedHeadView);
                    mSuspendedHeadView.getLayoutParams().height = mOptHeadView.getHeight();
                    mSuspendedHeadView.setLayoutParams(mSuspendedHeadView.getLayoutParams());
                }
                MarginLayoutParams lp = (MarginLayoutParams) mSuspendedHeadView.getLayoutParams();
                if (lp.topMargin > 0) {
                    lp.topMargin = 0;
                    mSuspendedHeadView.setLayoutParams(lp);
                }
                mSuspendedHeadView.setVisibility(VISIBLE);
                mOptHeadView.setVisibility(INVISIBLE);
                mBubbleHeadOperator.setOptView(mSuspendedHeadView, focus);
                mOptHeadView.setAlpha(1.0f);
            }
            notifyRegionChanged();
        }
    }

    private DataSetObserver mDataSetObserver = new DataSetObserver() {
        @Override
        public void onChanged() {
            super.onChanged();
            notifyRegionChanged();
//            updateFootHeight();
            mBubbleHeadOperator.updateExpandStatus();
            mBubbleHeadOperator.refreshOptEnable();
        }

        @Override
        public void onInvalidated() {
            super.onInvalidated();
            notifyRegionChanged();
        }
    };

    private int mDropBackPosition = -1;
    private boolean mDropBack = false;
    private boolean mInDragAttachMode;
    private int[] mDragAttachModePosition = new int[2];
    private int mDragInterval;

    private BubbleListActionCallback mBubbleListActionCallback = null;

    private class ListAnimListener implements Animator.AnimatorListener {
        private boolean mCanceled = false;
        private boolean mRunning = false;
        public void onAnimationStart(Animator animation) {
            mCanceled = false;
            mRunning = true;
        }

        public void onAnimationEnd(Animator animation) {
            if (!mCanceled) {
                hideBubbleListView();
            }
            mRunning = false;
        }

        public void onAnimationCancel(Animator animation) {
            mCanceled = true;
        }

        public void onAnimationRepeat(Animator animation) {

        }

        public boolean isRunning() {
            return mRunning;
        }
    }

    private ListAnimListener mHideListener = new ListAnimListener();

    public BubbleListView(Context context) {
        this(context, null);
    }

    public BubbleListView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public BubbleListView(Context context, AttributeSet attrs,
                           int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mHeadSpace = new Space(context);
        mHeadSpace.setLayoutParams(new LayoutParams(1, getHeadSpaceHeight()));
        addHeaderView(mHeadSpace);

        mOptHeadView = LayoutInflater.from(context).inflate(R.layout.bubble_head_item, null);
        addHeaderView(mOptHeadView);
        setHeaderDividersEnabled(false);
        mBubbleHeadOperator = new BubbleHeadOperator(this, mOptHeadView);

        mFootSpace = new Space(context);
        mFootSpace.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, FOOT_SPACE_H));
        addFooterView(mFootSpace);

        setRecyclerListener(new RecyclerListener() {
            @Override
            public void onMovedToScrapHeap(View view) {
                if (view instanceof BubbleItemView && view.getParent() == BubbleListView.this) {
                    if (((BubbleItemView) view).isAbort() || ((BubbleItemView) view).isTemp()) {
                        BubbleListView.this.removeDetachedView(view, false);
                    }
                }
            }
        });

        mBubbleAdapter = new BubbleAdapter(context, this);
        setAdapter(mBubbleAdapter);

        CommonUtils.setAlwaysCanAcceptDrag(this, true);
        mInternalOnScrollListener = new InternalOnScrollListener();
        setOnScrollListener(mInternalOnScrollListener);

        if (BubbleController.getInstance().isCurControllerContext(context)) {
            IdeaPillsApp.getInstance().getBubbleObserverManager().registerBubbleObserver(mBubbleObserver);
        }

        mBubbleAdapter.registerCallback(new BubbleAdapter.AdapterCallback() {
            public void execute(int msg) {
                switch (msg) {
                    case BubbleAdapter.AdapterCallback.MSG_HIDE_LIST:
                        hideBubbleListView();
                        break;
                    case BubbleAdapter.AdapterCallback.MSG_SET_DRAWORDER:
                        mDrawOrder = null;
                        setChildrenDrawingOrderEnabled(true);
                        break;
                    case BubbleAdapter.AdapterCallback.MSG_SET_DRAWORDER_OFF:
                        setChildrenDrawingOrderEnabled(false);
                        mDrawOrder = null;
                        break;
                }
            }
        });
        setOverScrollMode(OVER_SCROLL_ALWAYS);
        Configuration configuration = getResources().getConfiguration();
        mOrientation = configuration.orientation;
        measureHeight();
        mNormalBubbleHeight = context.getResources().getDimensionPixelSize(R.dimen.bubble_item_normal_height);
        mLargeBubbleTextHeight = context.getResources().getDimensionPixelSize(R.dimen.bubble_item_large_text_height);
        mLargeBubbleVoiceHeight = context.getResources().getDimensionPixelSize(R.dimen.bubble_item_large_voice_height);
        mSystemStatusBarHeight  = context.getResources().getDimensionPixelSize(R.dimen.bubble_item_large_voice_height);
        mEditBubbleTextHeight  = context.getResources().getDimensionPixelSize(R.dimen.bubble_item_edit_text_height);
        mEditBubbleVoiceHeight  = context.getResources().getDimensionPixelSize(R.dimen.bubble_item_edit_voice_height);
        int [] size = Constants.getWindowSize(context);
        if(size != null && size.length == 2){
            mScreenDimensionHeight = size[1];
        }
        mBubbleDraggingWidthMax = getResources().getDimensionPixelOffset(R.dimen.bubble_dragging_max_width);
    }

    private AnimRunnerInterface mBubbleListDraggingAnimRunner;

    public void observeBubbleItemChanged() {
        LOG.d("observeBubbleItemChanged");
        IdeaPillsApp.getInstance().getBubbleObserverManager().registerBubbleObserver(mBubbleObserver);
        mBubbleAdapter.observeBubbleItemChanged();
    }

    public void unObserveBubbleItemChanged() {
        IdeaPillsApp.getInstance().getBubbleObserverManager().unRegisterBubbleObserver(mBubbleObserver);
        mBubbleAdapter.unObserveBubbleItemChanged();
    }

    public void clearSelectedBubbles() {
        mBubbleAdapter.selectAll(false);
    }

    public List<BubbleItem> getSelectedBubbles() {
        return mBubbleAdapter.getSelectedBubbles();
    }

    public void setOnSelectChangeListener(OnSelectChangedListener onSelectChangeListener) {
        mOnSelectChangedListener = onSelectChangeListener;
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        if (mBubbleListDraggingAnimRunner != null && mBubbleListDraggingAnimRunner.isRunning()) {
            mBubbleListDraggingAnimRunner.draw(canvas);
        } else {
            super.dispatchDraw(canvas);
        }
    }

    public void startDeleteAnimations(List<Integer> reservedPositions,
                                      AnimCancelableListener listener) {
        if (mBubbleListDraggingAnimRunner != null && mBubbleListDraggingAnimRunner.isRunning()) {
            mBubbleListDraggingAnimRunner.cancel();
        }
        if (mBubbleListDraggingAnimRunner != null) {
            mBubbleListDraggingAnimRunner.recycle();
        }
        mBubbleListDraggingAnimRunner = new BubbleListCollapseAnimRunner(this, reservedPositions);
        mBubbleListDraggingAnimRunner.addAnimListener(listener);
        mBubbleListDraggingAnimRunner.start();
        invalidate();
    }

    @Override
    public boolean dispatchDragEvent(DragEvent event) {
        if (event.getAction() == DragEvent.ACTION_DRAG_PRE_ENDED && mAcceptDragEvent) {
            if ((event.getFlag() & DragEvent.FLAG_SUCCESS_DUE_TO_TRASH) != 0) {
                CommonUtils.vibrateEffect(mContext, VibEffectSmt.EFFECT_REMOVE_APP);
            }
        }
        return super.dispatchDragEvent(event);
    }

    public int getHeadSpaceHeight() {
        return getResources().getDimensionPixelSize(R.dimen.bubble_list_top_margin);
    }

    private void measureHeight() {
        getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            public void onGlobalLayout() {
                getViewTreeObserver().removeOnGlobalLayoutListener(this);
                updateFootHeight();
            }
        });
    }

    public void updateFootHeight() {
        if (mNormalListItemViewHeight < 0) {
            mNormalListItemViewHeight = BubbleController.getBubbleItemViewHelperByContext(getContext()).measureNormalHeight(generateDefaultLayoutParams());
        }
        ViewGroup.LayoutParams layoutParams = mFootSpace.getLayoutParams();
        if (layoutParams != null) {
            layoutParams.height = 1;
            mFootSpace.setLayoutParams(layoutParams);
        }
    }

    private void notifyRegionChanged() {
        if (mBubbleListActionCallback != null) {
            mBubbleListActionCallback.sendCommand(BubbleListActionCallback.MSG_CHANGREGION);
        }
    }

    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (ev.getAction() == MotionEvent.ACTION_DOWN && (isRunningAnimation() || getVisibility() == View.GONE)) {
            log.error("isRunningAnimation=" + isRunningAnimation() + " visibility=" + getVisibility());
            return false;
        }
        if (ev.getAction() == MotionEvent.ACTION_DOWN) {
            mBubbleAdapter.clearMovingState();
            mBubbleHeadOperator.clearMovingState();
            mDownX = ev.getX();
            mDownY = ev.getY();
        }
        boolean handle = super.dispatchTouchEvent(ev);
        if (mForceStartDragging && ev.getAction() == MotionEvent.ACTION_MOVE) {
            float curX = ev.getX();
            float curY = ev.getY();
            int slop = ViewConfiguration.get(getContext()).getScaledTouchSlop();
            if (Math.abs(mDownX - curX) > slop || Math.abs(mDownY - curY) > slop) {
                mForceStartDragging = false;
                mBubbleAdapter.stopDragWhileFling();
            }
        }
        if (mForceStartDragging && ev.getAction() == MotionEvent.ACTION_CANCEL || ev.getAction() == MotionEvent.ACTION_UP) {
            mForceStartDragging = false;
            mBubbleAdapter.stopDragWhileFling();
        }
        return handle;
    }
    private float mDownRawX;
    private float mDownRawY;
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        switch (ev.getAction()) {
            case MotionEvent.ACTION_DOWN:{
                mDownRawX = ev.getRawX();
                mDownRawY = ev.getRawY();
            }
            break;
            case MotionEvent.ACTION_MOVE: {
                if (Math.abs(mDownRawX - ev.getRawX()) / 1.7f > Math.abs(mDownRawY - ev.getRawY())
                        && Math.abs(mDownRawX - ev.getRawX()) > mBubbleAdapter.getTouchSlop()) {
                    mBubbleAdapter.moveHorizontal();
                }
            }
            break;
        }
        if (mBubbleAdapter.isMovingHorizontal() || mBubbleHeadOperator.isMovingHorizontal()) {
            return false;
        }
        if (ev.getAction() == MotionEvent.ACTION_DOWN && mScrollState == OnScrollListener.SCROLL_STATE_FLING) {
            boolean handle = super.onInterceptTouchEvent(ev);
            if (handle) {
                Rect rect = new Rect();
                int count = getChildCount();
                for (int i = 0; i < count; i++) {
                    View view = getChildAt(i);
                    if (view == null) {
                        continue;
                    }
                    if (view instanceof BubbleItemView) {
                        view.getHitRect(rect);
                        if (rect.contains((int) ev.getX(), (int) ev.getY())) {
                            mForceStartDragging = true;
                            mBubbleAdapter.startDragWhileFling((BubbleItemView) view);
                            break;
                        }
                    }
                }
            }
            return handle;
        }
        return super.onInterceptTouchEvent(ev);
    }

    public boolean onTouchEvent(MotionEvent ev) {
        if (mBubbleAdapter.isMovingHorizontal() || mBubbleHeadOperator.isMovingHorizontal()) {
            return false;
        }
        return super.onTouchEvent(ev);
    }

    public void removeFreeDetachedView(View view) {
        removeDetachedView(view, false);
    }

    public int newDragSwapList(DragEvent event, boolean willSwap) {
        return newDragSwapList((int) event.getX(), (int) event.getY() + getNormalHeight(), willSwap);
    }

    public int getNormalHeight() {
        return mNormalListItemViewHeight;
    }

    public void getDragAttachModePosition(final int cx, final int cy, final int[] output) {
        final int lastInsertPos = output[0];

        output[0] = -1;
        output[1] = DRAG_ATTACH_ADD_TO_BUBBLE;

        int childCount = getChildCount();
        if (mBubbleAdapter.getRealCount() == 0 && childCount > 0) {
            output[0] = 0;
            output[1] = DRAG_ATTACH_INSERT_NEW_BUBBLE;
        } else if (childCount > getHeaderViewsCount()) {
            int startIndex = 0;
            int startPos = getFirstVisiblePosition();
            if (startPos < getHeaderViewsCount()) {
                startIndex = getHeaderViewsCount();
            }
            int curInsertPos = -1;
            boolean isInPosInterval = false;
            View view = getChildAt(startIndex);
            if (view == null) {
                log.error("wrong startIndex=" + startIndex + " " + childCount);
                return;
            }
            final int top = view.getTop();
            int toTop = top;
            int index = startIndex;
            for (; index < childCount; index++) {
                view = getChildAt(index);
                if (view instanceof BubbleItemView) {
                    if (view.getVisibility() == VISIBLE) {
                        if ((toTop + view.getHeight()) > cy && toTop <= cy) {
                            curInsertPos = mBubbleAdapter.getInsertPos(view);//index + getFirstVisiblePosition() - getHeaderViewsCount();
                            if (view.getHeight() >= 4 * mDragInterval) {
                                if (toTop + mDragInterval > cy) {
                                    isInPosInterval = true;
                                } else if ((toTop + view.getHeight() - mDragInterval) <= cy) {
                                    isInPosInterval = true;
                                    curInsertPos = curInsertPos + 1;
                                }
                            }
                            break;
                        }
                        toTop += view.getHeight();
                    } else if (view.getVisibility() == INVISIBLE) {
                        if ((toTop + view.getHeight()) > cy && toTop <= cy) {
                            isInPosInterval = true;
                            curInsertPos = lastInsertPos;
                        }
                    } else {
                        log.error("view has the wrong visibility");
                        return;
                    }
                } else {
                    break;
                }
            }
            if (curInsertPos == -1 && cy > toTop) {
                curInsertPos = mBubbleAdapter.getRealCount();
                isInPosInterval = true;
            }
            output[0] = curInsertPos;
            output[1] = isInPosInterval ? DRAG_ATTACH_INSERT_NEW_BUBBLE : DRAG_ATTACH_ADD_TO_BUBBLE;
        }
    }

    public void resetDrag() {
        if (mInDragAttachMode) {
            dragAttachModeUpdate(0, 0, true);
        } else {
            //if not ourself, then reset bubbles, equals ourself bubbles move to last
            newDragSwapList(0, getBottom(), true);
        }
    }

    public int newDragSwapList(final int cx, final int cy, boolean willSwap){
        if (mCurInsertRect != null && willSwap) {
            if (mCurInsertRect.contains(cx, cy)) {
                return mCurInsertPos;
            }
        }
        int childCount = getChildCount();
        if (childCount > 1) {
            int startIndex = 0;
            int startPos = getFirstVisiblePosition();
            if (startPos < 2) {
                startIndex = 2 - startPos;
            }
            int beforeItemCount = 0;
            int curInsertPos = -1;
            int insertHeight = getNormalHeight();
            View view = getChildAt(startIndex);
            if (view == null) {
                log.error("wrong startIndex=" + startIndex + " " + childCount);
                return -1;
            }
            final int top = view.getTop() - insertHeight * beforeItemCount;
            final int left = view.getRight() - mBubbleAdapter.getNormalItemWidth();
            final int right = view.getRight();
            int toTop = top;
            List<Integer> toTops = new ArrayList<Integer>();
            mCurInsertRect = null;
            int index = startIndex;
            if (willSwap) {
                for (; index < childCount; index++) {
                    view = getChildAt(index);
                    if (view instanceof BubbleItemView) {
                        if (view.getVisibility() == VISIBLE) {
                            if (mCurInsertRect == null && (toTop + view.getHeight()) > cy && toTop <= cy) {
                                mCurInsertRect = new Rect(left, toTop, right, toTop + view.getHeight());
                                toTop += insertHeight;
                                curInsertPos = mBubbleAdapter.getInsertPos(view);//index + getFirstVisiblePosition() - getHeaderViewsCount();
                            }
                            toTops.add(toTop);
                            toTop += view.getHeight();
                        } else if (view.getVisibility() == INVISIBLE) {
                            continue;
                        } else {
                            log.error("view has the wrong visibility");
                            return -1;
                        }
                    } else {
                        break;
                    }
                }
                if (curInsertPos == -1 && (toTop <= cy)) {
                    curInsertPos = mBubbleAdapter.getInsertPos(null);//index + getFirstVisiblePosition() - getHeaderViewsCount();
                    mCurInsertRect = new Rect(left, toTop, right, toTop + insertHeight);
                }
            }
            boolean play = false;
            if (mCurInsertRect == null && mCurInsertPos != -1) {
                boolean isInVisibleLast = false;
                play = true;
                mCurInsertPos = -1;
                toTop = top;
                toTops.clear();
                for (index = startIndex; index < childCount; index++) {
                    view = getChildAt(index);
                    if (view instanceof BubbleItemView) {
                        if (view.getVisibility() == VISIBLE) {
                            isInVisibleLast = false;
                            toTops.add(toTop);
                            toTop += view.getHeight();
                        } else if (view.getVisibility() == INVISIBLE) {
//                            toTops.add(toTop);
                            if (isInVisibleLast) {
                                continue;
                            } else {
                                isInVisibleLast = true;
                                toTop += insertHeight;
                            }
                        }
                    }
                }
            }
            if (mCurInsertRect != null || play) {
                int j = 0;
                for (int i = startIndex; i < childCount; i++) {
                    view = getChildAt(i);
                    if (view instanceof BubbleItemView) {
                        BubbleItemView bubbleItemView = (BubbleItemView) view;
                        if (bubbleItemView.getVisibility() == VISIBLE) {
                            float endDy = toTops.get(j) - bubbleItemView.getTop();
                            float startDy = bubbleItemView.getTranslationY();
                            float lastEndDy = 0;
                            Anim animation = bubbleItemView.getAnimTranslateY();
                            if (animation != null) {
                                lastEndDy = animation.getTo().getY();
                            }
                            if (lastEndDy != endDy && startDy != endDy) {
                                bubbleItemView.clearAnimTranslateY();
                                animation = new Anim(bubbleItemView, Anim.TRANSLATE, ANIMDURATION, Anim.DEFAULT, new Vector3f(0.0f, startDy), new Vector3f(0.0f, endDy));
                                bubbleItemView.setAnimTranslateY(animation);
                                animation.start();
                            }
                            j++;
                        }
                    }
                }
                mCurInsertPos = curInsertPos;
                return mCurInsertPos;
            }
        }
        return -1;
    }

    @Override
    protected void onVisibilityChanged(View changedView, int visibility) {
        super.onVisibilityChanged(changedView, visibility);
        /*if (VISIBLE != visibility){
            GlobalBubbleManager.getInstance().cycleCleaningTodoOverData();
        }*/
    }

    public void announceForAccessibilityImmediately(CharSequence text) {
        sendAccessibilityEvent(AccessibilityEvent.TYPE_TOUCH_INTERACTION_START);
        sendAccessibilityEvent(AccessibilityEvent.TYPE_TOUCH_EXPLORATION_GESTURE_START);
        sendAccessibilityEvent(AccessibilityEvent.TYPE_TOUCH_EXPLORATION_GESTURE_END);
        sendAccessibilityEvent(AccessibilityEvent.TYPE_TOUCH_INTERACTION_END);
        announceForAccessibility(text);
    }

    @Override
    public boolean onDragEvent(final DragEvent event) {
        if (BubbleController.getInstance().isInputting()) {
            if (LOG.DBG) {
                log.info("stop handle dragevent while inputting");
            }
            return false;
        }
        switch (event.getAction()) {
            case DragEvent.ACTION_DRAG_STARTED: {
//                String mimeType = BubbleMimeUtils.getCommonMimeType(event);
//                if (!ClipDescription.MIMETYPE_TEXT_PLAIN.equals(mimeType)) {
//                    if (LOG.DBG) {
//                        log.info("stop handle dragevent while have the wrong mimetype");
//                    }
//                    mAcceptDragEvent = false;
//                } else {
//                    mAcceptDragEvent = true;
//                }
                mAcceptDragEvent = (event != null && event.getClipDescription() != null && event.getClipDescription().getMimeTypeCount() > 0);
            }
        }
        if (mAcceptDragEvent) {
            switch (event.getAction()) {
                case DragEvent.ACTION_DRAG_STARTED: {
                    log.info("ACTION_DRAG_STARTED");
                    announceForAccessibilityImmediately(getResources().getString(R.string.read_bubble_moving));
                    StatusManager.setStatus(StatusManager.GLOBAL_DRAGGING, true);
                    mDropBack = false;
                    mCurInsertPos = -1;
                    mStartPos = -1;
                    mDropBackPosition = -1;
                    mInDragAttachMode = BubbleController.getInstance().isInPptContext(mContext)
                            && Utils.isClipDataContainsAttachment(event.getClipData());
                    mDragAttachModePosition[0] = -1;
                    mDragAttachModePosition[1] = 0;
                    mDragInterval = ViewUtils.dp2px(getContext(), 30);
                    mDownX = -1;
                    mDownY = -1;
                    mCurInsertRect = null;
                    mScrollDir = DIRECTION_NONE;
                    mDragState = DRAG_STATE_NONE;
                    if (StatusManager.isBubbleDragging()) {
                        notifyRegionChanged();
                    }
                    break;
                }
                case DragEvent.ACTION_DRAG_ENDED: {
                    log.info("ACTION_DRAG_ENDED");
                    stopAutoScroll();
                    if (!StatusManager.isBubbleDragging()) {
                        mDropBack = false;
                        break;
                    }
                    if (!event.getResult() || mDropBack) {
                        UIHandler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                int readMessageId = !event.getResult() || mStartPos == mDropBackPosition ?
                                        R.string.read_bubble_move_cancel : R.string.read_bubble_moved;
                                announceForAccessibilityImmediately(getResources().getString(readMessageId));
                            }
                        }, 500);
                    }

                    if (mXAnimTimeLine != null) {
                        mXAnimTimeLine.cancel();
                    }

                    if (!event.getResult()) {
                        final float rawX = event.getRawX();
                        final float rawY = event.getRawY();
                        StatusManager.setStatus(StatusManager.BUBBLE_DRAGGING, false);
                        mBubbleAdapter.clearFakeList();
                        playShowAnimationAfterPull(null);
                        if (BubbleController.getInstance().isInPptContext(getContext())) {
                            mBubbleAdapter.onSelfDragEnd(event, false);
                        } else {
                            UIHandler.post(new Runnable() {
                                public void run() {
                                    startDragEndAnim(getContext(), rawX, rawY, event);
                                }
                            });
                        }
                    } else {
                        boolean isDelayHandled = mBubbleAdapter.onSelfDragEnd(event, mDropBack);
                        if (!isDelayHandled && !BubbleController.getInstance().isBubbleHandleBySidebar()) {
                            UIHandler.postDelayed(new Runnable() {
                                public void run() {
                                    playShowAnimationAfterPull(null);
                                }
                            }, 300);
                        }
                        mDropBack = false;
                    }
                    break;
                }
                case DragEvent.ACTION_DROP: {
                    log.info("ACTION_DROP");
                    if (mStopAcceptDragLoc) {
                        return false;
                    }
                    boolean handle;
                    if (mInDragAttachMode) {
                        cancelDraggingAnim();
                        handle = mBubbleAdapter.handleDropInsertAttachment(event, mDragAttachModePosition[0],
                                mDragAttachModePosition[1] == DRAG_ATTACH_INSERT_NEW_BUBBLE);
                        updateDragAttachFocusView(-1, -1);
                    } else {
                        handle = mBubbleAdapter.handleDropBackSelected(event, mDropBackPosition);
                    }
                    mDropBack = true;
                    return handle;
                }
                case DragEvent.ACTION_DRAG_LOCATION: {
                    if (mStopAcceptDragLoc) {
                        return false;
                    }
                    if (mDownX < 0) {
                        mDownX = event.getX();
                        mDownY = event.getY();
                    } else {
                        if (DRAG_STATE_NONE == mDragState && (Math.abs(event.getY() - mDownY) > 10 || Math.abs(event.getX() - mDownX) > 10)) {
                            mDragState = DRAG_STATE_MOVE;
                            if (mInDragAttachMode) {
                                dragAttachModeUpdate(event);
                            } else {
                                if (mBubbleListDraggingAnimRunner != null) {
                                    mBubbleListDraggingAnimRunner.recycle();
                                }
                                mBubbleListDraggingAnimRunner = new BubbleListDraggingAnimRunner(this, mBubbleAdapter.getReservedItems());
                                mBubbleListDraggingAnimRunner.addAnimListener(new AnimCancelableListener() {
                                    public void onAnimCompleted() {
                                        mBubbleAdapter.setFakeList(mDropBackPosition);
                                    }
                                });
                                mDropBackPosition = ((BubbleListDraggingAnimRunner)mBubbleListDraggingAnimRunner).
                                        start(event.getX(), event.getY() + getNormalHeight());
                            }
                            mStartPos = mDropBackPosition;
                        } else {
                            if (DRAG_STATE_MOVE == mDragState) {
                                if (mBubbleListDraggingAnimRunner == null || !mBubbleListDraggingAnimRunner.isRunning()) {
                                    if (!scrollByDragEvent(event)) {
                                        if (mInDragAttachMode) {
                                            dragAttachModeUpdate(event);
                                        } else {
                                            int pos = newDragSwapList(event, true);
                                            if (mDropBackPosition != pos) {
                                                int realPos = mBubbleAdapter.getRealPosition(pos);
                                                if (realPos >= 0) {
                                                    String msgFormat = getResources().getString(R.string.read_bubble_move_to);
                                                    announceForAccessibilityImmediately(String.format(msgFormat, realPos + 1));
                                                }
                                                CommonUtils.vibrateEffect(mContext, VibEffectSmt.EFFECT_TIME_PICKER);
                                            }
                                            mDropBackPosition = pos;
                                        }
                                    } else {
                                        mDropBackPosition = -1;
                                    }
                                }
                            }
                        }
                    }
                    break;
                }
                case DragEvent.ACTION_DRAG_ENTERED: {
                    log.info("ACTION_DRAG_ENTERED");
                    if (!StatusManager.isBubbleDragging()) {
                        if (mInDragAttachMode) {
                            dragAttachModeUpdate(event);
                        } else {
                            mDropBackPosition = newDragSwapList(event, true);
                        }
                    }
                    break;
                }
                case DragEvent.ACTION_DRAG_EXITED: {
                    log.info("ACTION_DRAG_EXITED");
                    if (StatusManager.isBubbleDragging()) {
                        // only hide when item of ourself move outside.
                        if (event.getRawX() < getWidth() - 1 && ((int)event.getRawY()) > Constants.STATUS_BAR_HEIGHT) {
                            if (!BubbleController.getInstance().isExtDisplay()) {
                                hideBubbleListView();
                            }
                        } else {
                            stopAutoScroll();
                        }
                    } else {
                        resetDrag();
                    }
                    break;
                }
                default:
                    return false;
            }
            return true;
        } else {
            return false;
        }
    }

    private void dragAttachModeUpdate(DragEvent event) {
        dragAttachModeUpdate((int) event.getX(), (int) event.getY(), false);
    }

    private void dragAttachModeUpdate(int eventX, int eventY, boolean isClearDrag) {
        if (isClearDrag || mStopAcceptDragLoc) {
            mDragAttachModePosition[0] = -1;
            mDragAttachModePosition[1] = DRAG_ATTACH_ADD_TO_BUBBLE;
            cancelDraggingAnim();
            dragAttachModeBack();
            updateDragAttachFocusView(-1, -1);
            return;
        }
        if (mBubbleListDraggingAnimRunner != null && mBubbleListDraggingAnimRunner.isRunning()) {
            return;
        }
        if (mAutoScrollAnim != null && mAutoScrollAnim.isRunning()) {
            return;
        }
        int lastPos = mDragAttachModePosition[0];
        int lastDragAttachMode = mDragAttachModePosition[1];
        getDragAttachModePosition(eventX, eventY, mDragAttachModePosition);
        int nowPos = mDragAttachModePosition[0];
        int nowDragAttachMode = mDragAttachModePosition[1];
        if (lastPos != nowPos || nowDragAttachMode != lastDragAttachMode) {
            if (nowDragAttachMode == DRAG_ATTACH_INSERT_NEW_BUBBLE) {
                if (nowPos == mBubbleAdapter.getRealCount()) {
                    mBubbleAdapter.setFakeList(mDragAttachModePosition[0]);
                } else {
                    mBubbleListDraggingAnimRunner = new BubbleListDraggingAnimRunner(this,
                            mBubbleAdapter.getReservedItems(), false, 0);
                    mBubbleListDraggingAnimRunner.addAnimListener(new AnimCancelableListener() {
                        public void onAnimCompleted() {
                            mBubbleAdapter.setFakeList(mDragAttachModePosition[0]);
                        }
                    });
                    ((BubbleListDraggingAnimRunner) mBubbleListDraggingAnimRunner).
                            start(eventX, eventY + getNormalHeight());
                }
            } else {
                dragAttachModeBack();
            }
            updateDragAttachFocusView(lastPos,
                    nowDragAttachMode == DRAG_ATTACH_INSERT_NEW_BUBBLE ? -1 : nowPos);
        }
    }

    private void dragAttachModeBack() {
        if (mBubbleAdapter.hasFakeList()) {
            if (mBubbleListDraggingAnimRunner instanceof BubbleListDraggingAnimRunner) {
                float x = ((BubbleListDraggingAnimRunner) mBubbleListDraggingAnimRunner).getInsertX();
                float y = ((BubbleListDraggingAnimRunner) mBubbleListDraggingAnimRunner).getInsertY();
                mBubbleListDraggingAnimRunner = new BubbleListDraggingAnimRunner(this,
                        mBubbleAdapter.getReservedItems(), true, 0);
                mBubbleListDraggingAnimRunner.addAnimListener(new AnimCancelableListener() {
                    public void onAnimCompleted() {
                        mBubbleAdapter.clearFakeList();
                    }
                });
                ((BubbleListDraggingAnimRunner) mBubbleListDraggingAnimRunner).
                        start(x, y + getNormalHeight());
            } else {
                mBubbleAdapter.clearFakeList();
            }
        }
    }

    private void updateDragAttachFocusView(int lastPos, int nowPos) {
        BubbleItem nowItem = mBubbleAdapter.getItemByRealPosition(nowPos);
        int childCount = getChildCount();
        if (childCount > getHeaderViewsCount()) {
            int startIndex = 0;
            int startPos = getFirstVisiblePosition();
            if (startPos < getHeaderViewsCount()) {
                startIndex = getHeaderViewsCount();
            }
            View view = getChildAt(startIndex);
            if (view == null) {
                log.error("wrong startIndex=" + startIndex + " " + childCount);
                return;
            }
            int index = startIndex;
            for (; index < childCount; index++) {
                view = getChildAt(index);
                if (view instanceof BubbleItemView) {
                    if (view.getVisibility() == VISIBLE) {
                        BubbleItem item = ((BubbleItemView) view).getBubbleItem();
                        if (item != null) {
                            if (item.equals(nowItem)) {
                                ((BubbleItemView) view).updatePptAddFocus(true);
                            } else {
                                ((BubbleItemView) view).updatePptAddFocus(false);
                            }
                        }
                    }
                } else {
                    break;
                }
            }
        }
    }

    private final int DIRECTION_NONE = 0;
    private final int DIRECTION_UP   = 1;
    private final int DIRECTION_DOWN = -1;

    private int mScrollDir = DIRECTION_NONE;

    private static final float SCROLL_RATE = 1.5f;
    private int mScrolledDistance = 0;

    private void stopAutoScroll() {
        if (mAutoScrollAnim != null) {
            mAutoScrollAnim.cancel();
            mAutoScrollAnim = null;
        }
    }

    private void startAutoScroll() {
        mAutoScrollAnim = ValueAnimator.ofInt(0, Integer.MAX_VALUE);
        mAutoScrollAnim.setDuration(Integer.MAX_VALUE);
        mAutoScrollAnim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            public void onAnimationUpdate(ValueAnimator animation) {
                long time = animation.getCurrentPlayTime();
                float distance = (time * SCROLL_RATE * mScrollDir);
                if (mScrollDir != DIRECTION_NONE && canScrollList(mScrollDir)) {
                    int d = (int) (distance - mScrolledDistance);
                    if (d != 0) {
                        scrollListBy(d);
                        mScrolledDistance += d;
                    }
                } else {
                    mAutoScrollAnim.cancel();
                }
            }
        });
        mAutoScrollAnim.addListener(new Animator.AnimatorListener() {
            public void onAnimationStart(Animator animation) {
                mScrolledDistance = 0;
            }

            public void onAnimationEnd(Animator animation) {
                mScrollDir = DIRECTION_NONE;
            }

            public void onAnimationCancel(Animator animation) {

            }

            public void onAnimationRepeat(Animator animation) {

            }
        });
        mAutoScrollAnim.start();
    }

    private boolean autoScrollAnim(int dir) {
        if (!canScrollList(dir)) {
            return false;
        }
        mScrollDir = dir;
        stopAutoScroll();
        if (mDropBackPosition >= 0) {
            if (mBubbleListDraggingAnimRunner != null) {
                mBubbleListDraggingAnimRunner.recycle();
            }
            mBubbleListDraggingAnimRunner = new BubbleListDraggingAnimRunner(this, mBubbleAdapter.getReservedItems());
            mBubbleListDraggingAnimRunner.addAnimListener(new AnimCancelableListener() {
                public void onAnimCompleted() {
                    mBubbleAdapter.setFakeList(mDropBackPosition);
                    startAutoScroll();
                }
            });
            mDropBackPosition = ((BubbleListDraggingAnimRunner)mBubbleListDraggingAnimRunner).start(-1, -1);
        } else {
            startAutoScroll();
        }
        return true;
    }

    /*
     *TODO  scroll by position for now
     */
    private boolean scrollByDragEvent(DragEvent event) {
        int x = (int) event.getX();
        int y = (int) event.getY();
        if (y < Constants.getVerticalScrollResponseAreaTop(getContext())) {
            if (mScrollDir != DIRECTION_DOWN) {
                return autoScrollAnim(DIRECTION_DOWN);
            }
            return true;
        } else if (y > (Constants.WINDOW_HEIGHT - Constants.getVerticalScrollResponseAreaBottom(getContext()))) {
            if(mScrollDir != DIRECTION_UP){
                return autoScrollAnim(DIRECTION_UP);
            }
            return true;
        } else {
            stopAutoScroll();
            return false;
        }
    }

    private boolean mStopAcceptDragLoc = false;

    public boolean isStopAcceptDragLoc() {
        return mStopAcceptDragLoc;
    }

    public void setStopAcceptLoc(boolean stopAcceptLoc) {
        log.d("cx setStopAcceptLoc:" + stopAcceptLoc);
        mStopAcceptDragLoc = stopAcceptLoc;
    }

    public void closeAll() {
        mBubbleAdapter.closeAll(BubbleListView.this, mHideListener);
    }

    public interface BubbleListActionCallback{
        int MSG_SUSPENDHEADVIEW = 1;
        int MSG_CHANGREGION = 3;
        boolean sendCommand(int msg, Object... args);
    }

    public void setBubbleListActionCallback(BubbleListActionCallback callback) {
        mBubbleListActionCallback = callback;
    }

    public void setVisibility(int visibility) {
        if (getVisibility() != visibility) {
            if (visibility == VISIBLE) {
                mPullViewGroup.clearTransparent();
                CommonUtils.setAlwaysCanAcceptDrag(this, true);
                setStopAcceptLoc(false);
                BubbleController.getInstance().setWillAcceptKeyCode(true);
            }
            super.setVisibility(visibility);
            if (mSuspendedHeadView != null && mSuspendedHeadView == mBubbleHeadOperator.mView) {
                mSuspendedHeadView.setVisibility(visibility);
            }
            if (visibility == GONE) {
                CommonUtils.setAlwaysCanAcceptDrag(this, false);
                BubbleController.getInstance().setWillAcceptKeyCode(false);
            }
        }
    }

    @Override
    public void setAdapter(ListAdapter adapter) {
        ListAdapter adapterOrg = getAdapter();
        if (adapterOrg != null) {
            adapterOrg.unregisterDataSetObserver(mDataSetObserver);
        }
        super.setAdapter(adapter);
        if (adapter != null) {
            adapter.registerDataSetObserver(mDataSetObserver);
        }
    }

    public boolean isEmpty() {
        if (mBubbleAdapter != null) {
            return mBubbleAdapter.getCount() == 0;
        }
        return true;
    }

    public boolean hasNormalBubble() {
        if (mBubbleAdapter != null) {
            return mBubbleAdapter.hasNormalBubble();
        }
        return false;
    }

    public boolean isTodoOverHide() {
        if (mBubbleAdapter != null) {
            return mBubbleAdapter.isTodoOverHide();
        }
        return false;
    }

    public void switchShowHideTodoOver() {
        if (mBubbleAdapter != null) {
            mBubbleAdapter.switchShowHideTodoOver();
        }
    }

    public void updateColorFilter(int color, boolean anim) {
        if (mBubbleAdapter != null) {
            mBubbleAdapter.updateColorFilter(color);
            if (anim) {
                scheduleLoadAnim();
            }
        }
    }

    private void scheduleLoadAnim() {
        setOnScrollListener(null);
        StatusManager.setStatus(StatusManager.BUBBLE_REFRESHING, true);
        if (mSuspendedHeadView == null) {
            mSuspendedHeadView = LayoutInflater.from(getContext()).inflate(R.layout.bubble_head_item, null);
            mBubbleListActionCallback.sendCommand(BubbleListActionCallback.MSG_SUSPENDHEADVIEW, mSuspendedHeadView);
        }
        updateSuspendedHeadLayout();
        mSuspendedHeadView.setVisibility(View.VISIBLE);
        mOptHeadView.setVisibility(View.INVISIBLE);
        boolean focus = mBubbleHeadOperator.isFocusSearch();
        mBubbleHeadOperator.setOptView(mSuspendedHeadView, focus);
        getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            @Override
            public boolean onPreDraw() {
                getViewTreeObserver().removeOnPreDrawListener(this);
                setAlpha(0.0f);
                updateSuspendedHeadLayout();
                AnimationSet set = new AnimationSet(true);
                set.setInterpolator(new DecelerateInterpolator());
                Animation animation = new AlphaAnimation(0.0f, 1.0f);
                animation.setDuration(150);
                set.addAnimation(animation);
                animation = new TranslateAnimation(0, 0, 30, 0);
                animation.setDuration(150);
                set.addAnimation(animation);
                set.setStartOffset(150);

                final LayoutAnimationController controller = new LayoutAnimationController(set, 0.15f);
                setLayoutAnimation(controller);
                startLayoutAnimation();
                setLayoutAnimationListener(new Animation.AnimationListener() {
                    @Override
                    public void onAnimationStart(Animation animation) {
                        setAlpha(1.0f);
                    }

                    @Override
                    public void onAnimationEnd(Animation animation) {
                        setLayoutAnimationListener(null);
                        setLayoutAnimation(null);
                        setOnScrollListener(mInternalOnScrollListener);
                        if (getFirstVisiblePosition() <= 1) {
                            boolean focus = mBubbleHeadOperator.isFocusSearch();
                            mBubbleHeadOperator.setOptView(mOptHeadView, focus);
                            mOptHeadView.setVisibility(View.VISIBLE);
                            mSuspendedHeadView.setVisibility(View.GONE);
                        }
                        StatusManager.setStatus(StatusManager.BUBBLE_REFRESHING, false);
                    }

                    @Override
                    public void onAnimationRepeat(Animation animation) {
                    }
                });
                return true;
            }
        });
    }

    private void updateSuspendedHeadLayout(){
        int loc[] = new int[2];
        mOptHeadView.getLocationInWindow(loc);
        MarginLayoutParams lp = (MarginLayoutParams) mSuspendedHeadView.getLayoutParams();
        int height = mOptHeadView.getHeight();
        int marginTop = Math.max(loc[1] - Constants.STATUS_BAR_HEIGHT, 0);
        if (lp.topMargin != marginTop || lp.height != height) {
            lp.topMargin = marginTop;
            lp.height = height;
            mSuspendedHeadView.setLayoutParams(lp);
        }
    }

    public void computeRegion(Region region) {
        region.setEmpty();
        int count = getChildCount();
        View view = null;
        int[] out = new int[2];
        mBubbleHeadOperator.getLeftAndRight(out);
        int left = out[0];
        region.op(out[0], mBubbleHeadOperator.mView.getTop(), out[1], mBubbleHeadOperator.mView.getBottom(), Region.Op.UNION);
        for (int i = 0; i < count; i++) {
            view = getChildAt(i);
            if (view instanceof BubbleItemView) {
                if (StatusManager.isBubbleDragging()) {
                    ((BubbleItemView) view).getLeftAndRight(out);
                    out[0] = Math.max(out[1] - mBubbleDraggingWidthMax, Math.min(out[0], left));
                } else {
                    ((BubbleItemView) view).getLeftAndRight(out);
                }
                if (StatusManager.getStatus(StatusManager.GLOBAL_DRAGGING)) {
                    region.op(out[0] + (int) view.getX(), getTop(), out[1], getBottom(), Region.Op.UNION);
                } else {
                    region.op(out[0] + (int) view.getX(), view.getTop(), out[1], view.getBottom(), Region.Op.UNION);
                }
            }
        }
    }

    public void hideBubbleListImmediately() {
        log.info("hideBubbleListImmediately");
        mBubbleHeadOperator.hideHeadView(true);
        if (getVisibility() != View.GONE) {
            announceForAccessibility(mContext.getString(R.string.read_bubble_list_hide));
        }
    }

    public void hideBubbleListWithoutAnyAnim() {
        log.info("hideBubbleListWithoutAnyAnim");
        mBubbleHeadOperator.hideHeadView(false);
        if (getVisibility() != View.GONE) {
            announceForAccessibility(mContext.getString(R.string.read_bubble_list_hide));
        }
    }

    public void switchHeadOperatorEditStatus(boolean isEdit) {
        boolean needAnim = true;
        if (mBubbleAdapter.getMode() == ViewMode.BUBBLE_EDIT && isEdit) {
            needAnim = false;
        }
        mBubbleHeadOperator.toEdit(isEdit, needAnim);
    }

    public void hideBubbleListView() {
        hideBubbleListView(true);
    }

    public void hideBubbleListView(boolean isForceStopLastAnimIfRunning) {
        hideBubbleListView(isForceStopLastAnimIfRunning, null);
    }

    public void hideBubbleListView(boolean isForceStopLastAnimIfRunning, Runnable task) {
        if (!isForceStopLastAnimIfRunning && mXAnimType != ANIM_NONE) {
            LOG.d("skip hide bubble list.");
            return;
        }
        hideBubbleListView(task);
    }

    public void hideBubbleListView(final Runnable task) {
        stopAutoScroll();
        if (getVisibility() == GONE) {
            log.info("bubble list already gone");
            return;
        }
        if (BubbleController.getInstance().isInPptContext(getContext())) {
            return;
        }
        if (mBubbleListDraggingAnimRunner != null && mBubbleListDraggingAnimRunner.isRunning()) {
            log.info("mBubbleListDraggingAnimRunner running");
            mBubbleListDraggingAnimRunner.addAnimListener(new AnimCancelableListener() {
                public void onAnimCompleted() {
                    UIHandler.post(new Runnable() {
                        public void run() {
                            hideBubbleListView(task);
                        }
                    });
                }
            });
            return;
        }
        log.info("hideBubbleListView");
        mBubbleHeadOperator.hideHeadFiltrate();
        if (mXAnimTimeLine != null) {
            mXAnimTimeLine.cancel();
        }
        if (Utils.isKeyguardLocked() && !BubbleController.getInstance().isExtDisplay()) {
            //need not play anim to hide BubbleListView
            mBubbleHeadOperator.toEditImmediately(false);
            hideBubbleListImmediately();
            if (task != null) {
                task.run();
            }
        } else {
            mXAnimTimeLine = new AnimTimeLine();
            mXAnimType = ANIM_HIDE;
            int count = getChildCount();
            View view = null;
            for (int i = count - 1; i >= 0; i--) {
                view = getChildAt(i);
                if (view instanceof BubbleItemView) {
                    ((BubbleItemView) view).stopPlay();
                    Vector3f from = new Vector3f(view.getTranslationX(), 0);
                    Vector3f to = new Vector3f(((BubbleItemView) view).queryWidth(), 0);
                    final Anim anim = new Anim(view, Anim.TRANSLATE, ANIMDURATION, Anim.CUBIC_OUT, from, to);
                    anim.setListener(new SimpleAnimListener() {
                        @Override
                        public void onStart() {

                        }

                        @Override
                        public void onComplete(int type) {
                            if (type == Anim.ANIM_FINISH_TYPE_COMPLETE) {
                                anim.getView().setTranslationX(0.0f);
                            }
                        }
                    });
                    mXAnimTimeLine.addAnim(anim);
                    BubbleItemView bubble = (BubbleItemView) view;
                    bubble.finishInputting();
                }
            }
            Anim anim = mBubbleHeadOperator.getPlayhideViewAnim();
            mXAnimTimeLine.addAnim(anim);
            AnimListener listener = new AnimCancelableListener() {

                @Override
                public void onStart() {
                    super.onStart();
                    //stop the scroll animator to prevent the invisible items of list top
                    //become visible again which will not play the hide animation.
                    if (mScrollAnimator != null) {
                        mScrollAnimator.cancel();
                    }
                }

                public void onAnimCompleted() {
                    if (getPositionForFootSpace() >= 0) {  //scroll list follow scrollInputViewBack
                        int scrollHeight = getHeight() - (mFootSpace.getTop() + 1);
                        scrollListBy(-scrollHeight);
                    }
                    updateFootHeight();
                    announceForAccessibility(mContext.getString(R.string.read_bubble_list_hide));
                }

                public void onComplete(int type) {
                    super.onComplete(type);
                    mBubbleHeadOperator.hideHeadView(true);
                    notifyRegionChanged();
                    mXAnimTimeLine = null;
                    mXAnimType = ANIM_NONE;
                    if (type == Anim.ANIM_FINISH_TYPE_COMPLETE) {
                        if (task != null) {
                            task.run();
                        }
                    }
                }
            };
            mXAnimTimeLine.setAnimListener(listener);
            if (mXAnimTimeLine.isEmpty()) {
                listener.onComplete(0);
            } else {
                mXAnimTimeLine.start();
            }
        }
    }

    private void playShowAnimationInner(final ShowBubblesListener listener) {
        mWaitingForLayout = false;
        mXAnimTimeLine = new AnimTimeLine();
        mXAnimType = ANIM_SHOW;
        mPullViewGroup.clearTransparent();
        int count = getChildCount();
        View view = null;
        BubbleItemView itemView;
        boolean isInPptMode = BubbleController.getInstance().isInPptContext(getContext());
        int itemTransY = ViewUtils.dp2px(BubbleController.getInstance().getContext(), 30);
        int itemIndex = 0;
        for (int i = 0; i < count; i++) {
            view = getChildAt(i);
            if (view instanceof BubbleItemView) {
                itemView = (BubbleItemView)view;
                itemView.clearAnimTranslateY();
                BubbleItem bubbleItem = itemView.getBubbleItem();
                if (StatusManager.getStatus(StatusManager.ADDING_ATTACHMENT)) {
                    if (!BubbleController.getInstance().isExtDisplay()) {
                        StatusManager.setStatus(StatusManager.ADDING_ATTACHMENT, false);
                    }
                } else if (bubbleItem.isEmptyBubble() && !bubbleItem.isSyncLocked() && !bubbleItem.isAttachLocked()) {
                    bubbleItem.dele();
                    GlobalBubbleManager.getInstance().removeBubbleItem(bubbleItem);
                    itemView.setVisibility(INVISIBLE);
                    continue;
                }
                Vector3f from;
                int startDelay;
                if (isInPptMode) {
                    if (itemIndex == 0) {
                        from = new Vector3f(itemView.getTranslationX(), 0);
                    } else {
                        from = new Vector3f(itemView.getTranslationX(), -1 * itemTransY);
                    }
                    startDelay = itemIndex * 20;
                } else {
                    from = new Vector3f(itemView.queryWidth(), itemView.getTranslationY());
                    startDelay = 0;
                }
                itemIndex++;
                Anim anim = new Anim(itemView, Anim.TRANSLATE, 300, Anim.CUBIC_OUT, from, Anim.ZERO);
                anim.setDelay(startDelay);
                if (!from.equals(Anim.ZERO)) {
                    mXAnimTimeLine.addAnim(anim);
                }
                if (isInPptMode) {
                    Anim bgShowAnim = new Anim(itemView, Anim.TRANSPARENT, 300, Anim.CUBIC_OUT, Anim.INVISIBLE, Anim.VISIBLE);
                    bgShowAnim.setDelay(startDelay);
                    itemView.setAlpha(0f);
                    mXAnimTimeLine.addAnim(bgShowAnim);
                }
            }
        }
        mBubbleHeadOperator.updateExpandStatus();
        mBubbleHeadOperator.showHeadView();
        AnimListener animListener = new SimpleAnimListener() {
            public void onStart() {

            }

            public void onComplete(int type) {
                if (getTranslationY() > 0) {
                    mBubbleHeadOperator.setTranslateYinHideState(0);
                    Anim anim = new Anim(BubbleListView.this, Anim.TRANSLATE,
                            250, Anim.CUBIC_OUT, new Vector3f(0, getTranslationY()), Anim.ZERO);
                    anim.setListener(new SimpleAnimListener() {
                        public void onStart() {

                        }

                        public void onComplete(int type) {
                            announceForAccessibility(getResources().getString(R.string.read_bubble_list_show));
                            notifyRegionChanged();
                            onListShowAnimCompleted();
                            if (listener != null) {
                                listener.showOver();
                            }
                        }
                    });
                    anim.start();
                } else {
                    mBubbleHeadOperator.setTranslateYinHideState(0);
                    announceForAccessibility(getResources().getString(R.string.read_bubble_list_show));
                    notifyRegionChanged();
                    onListShowAnimCompleted();
                    if (listener != null) {
                        listener.showOver();
                    }
                }
                mXAnimTimeLine = null;
                mXAnimType = ANIM_NONE;
            }
        };
        if (mXAnimTimeLine.isEmpty()) {
            animListener.onComplete(0);
        } else {
            if (mPullViewGroup.getListRelateTopMargin() > 0) {
                int normalHeadSpace = getHeadSpaceHeight();
                int headInterval = Math.max(normalHeadSpace - mBubbleHeadOperator.getHeadTop(), 0);
                this.setTranslationY(mPullViewGroup.getListRelateTopMargin() + headInterval);
            }
            mXAnimTimeLine.setAnimListener(animListener);
            mXAnimTimeLine.start();
        }
    }

    public void playShowAnimationAfterPull(final ShowBubblesListener listener) {
        log.info("playShowAnimationAfterPull");
        if (!BubbleController.getInstance().isShieldShowList()) {
            playShowAnimation(listener);
        }
    }

    public boolean isAlreadyShow() {
        return getVisibility() == VISIBLE;
    }

    @Override
    public void setAlpha(float alpha) {
        super.setAlpha(alpha);
    }

    public void playShowAnimation(final ShowBubblesListener listener) {
        if (getVisibility() == VISIBLE) {
            log.info("already show bubblist");
            if (mXAnimTimeLine != null && mXAnimTimeLine.isRunning()) {
                mXAnimTimeLine.cancel();
                playShowAnimationInner(listener);
            } else {
                if (listener != null) {
                    listener.showOver();
                }
            }
        } else {
            Tracker.onEvent(BubbleTrackerID.BUBBLE_SHOW_LIST);
            mWaitingForLayout = true;
            setVisibility(VISIBLE);
            getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    getViewTreeObserver().removeOnGlobalLayoutListener(this);
                    playShowAnimationInner(listener);
                    int loginState = Settings.System.getInt(getContext().getContentResolver(), Constants.ClOUD_EVER_LOGIN, 0);
                    int checkState = Settings.System.getInt(getContext().getContentResolver(), Constants.CLOUD_IDEAPILL_CHECKED, 0);
                    log.info("loginState:" + loginState + "    checkState = " + checkState);
                    if (loginState == 0 && checkState == 0 && mBubbleAdapter != null && mBubbleAdapter.getCount() > 0) {
                        GlobalBubbleUtils.checkCloudSync(getContext());
                    }
                }
            });

            ViewParent p = getParent();
            while (p != null) {
                if (p instanceof BubbleOptLayout) {
                    ((BubbleOptLayout) p).enableTocuchArea();
                    break;
                }
                p = p.getParent();
            }
        }
        if (!hasFocus()) {
            requestFocus();
        }
    }

    protected void onListShowAnimCompleted() {
        GlobalBubbleManager.getInstance().notifySaraCheckOffline();
    }

    public void showBubbleList(long syncId){
        setVisibility(VISIBLE);
        if (mXAnimTimeLine != null) {
            mXAnimTimeLine.cancel();
        }

        int count = getChildCount();
        View view = null;
        BubbleItemView itemView;
        for (int i = 0; i < count; i++) {
            view = getChildAt(i);
            if (view instanceof BubbleItemView) {
                itemView = (BubbleItemView)view;
                itemView.setAnimation(null);
                itemView.setTranslationX(0);
            }
        }
        mBubbleHeadOperator.updateExpandStatus();
        mBubbleHeadOperator.showHeadView();
        if (syncId > 0) {
            int pos = mBubbleAdapter.getPositionBySyncId(syncId);
            if (pos >= 0) {
                BubbleItem item = mBubbleAdapter.getItem(pos);
                pos += getHeaderViewsCount();
                if (!item.isInLargeMode()) {
                    item.setInLargeMode(true);
                    int firstPos = getFirstVisiblePosition();
                    int childCount = getChildCount();
                    if (firstPos <= pos && pos < firstPos + childCount) {
                        BubbleItemView biv = (BubbleItemView) getChildAt(pos - firstPos);
                        if (biv != null) {
                            biv.show(item);
                        }
                    }
                }
                smoothScrollToPosition(pos);
            }
        }
    }

    public void showBubbleList() {
        showBubbleList(0);
    }

    public void setPullView(PullViewGroup view) {
        mPullViewGroup = view;
        updateTodoOverStatus(true, true);
    }

    public void updateTodoOverStatus(boolean visiblechange, boolean statuschange) {
        if (mPullViewGroup != null) {
            if(visiblechange) {
                mPullViewGroup.updateTodoOverVisible(mBubbleHeadOperator.getTodoOverVisible(),
                        mBubbleHeadOperator.getExpandRealWidth());
            }
            if(statuschange){
                mPullViewGroup.updateTodoOverStatus(mBubbleHeadOperator.getTodoOverImageRes());
            }
        }
    }

    public void updateFiltrateStatus(int resId) {
        if (mPullViewGroup != null) {
            mPullViewGroup.updateFiltrateStatus(resId);
        }
    }

    public void dragPullViewVertical(float moveY) {
        mPullViewGroup.dragPullViewVertical(moveY);
    }

    public boolean isRunningAnimation() {
        return (mXAnimTimeLine != null && mXAnimTimeLine.isRunning()) || mHideListener.isRunning() || mBubbleHeadOperator.hasAnimRuning() || mWaitingForLayout || mToTopDownAnimFlag;
    }

    public void handleBubbleBySelf(Set<Integer> ids) {
        mBubbleAdapter.handleBubbleBySelf(ids);
    }

    protected void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (newConfig.orientation != mOrientation) {
            mOrientation = newConfig.orientation;
            mPullViewGroup.resetTopMargin();
            mBubbleHeadOperator.hideHeadFiltrate();
            notifyRegionChanged();
        }
        if(mBubbleAdapter != null){
            mBubbleAdapter.notifyDataSetChanged();
        }
        if(mBubbleHeadOperator != null){
            mBubbleHeadOperator.onConfigurationChanged(newConfig);
        }
    }

    @Override
    protected int getChildDrawingOrder(int childCount, int i) {
        if (StatusManager.isBubbleDragging()) {
            if (mDrawOrder != null && mDrawOrder.length != childCount) {
                mDrawOrder = null;
            }
            if (mDrawOrder == null) {
                mDrawOrder = new int[childCount];
                int maxIndex = childCount - 1;
                int minIndex = 0;
                for (int index = 0; index < childCount; index++) {
                    BubbleItem item = mBubbleAdapter.getItem(getChildAt(index));
                    if (maxIndex < 0 || minIndex >= childCount) {
                        break;
                    }
                    if (item != null && item.isTemp()) {
                        mDrawOrder[maxIndex--] = index;
                    } else {
                        mDrawOrder[minIndex++] = index;
                    }
                }
            }
            return mDrawOrder[i];
        } else {
            return super.getChildDrawingOrder(childCount, i);
        }
    }

    private static class ListScrollAnimator extends ValueAnimator{

        public static ListScrollAnimator ofInt2(int... values) {
            ListScrollAnimator anim = new ListScrollAnimator();
            anim.setIntValues(values);
            return anim;
        }

        private long mLastPlayTime;
        private int mToTalScrollHeight;
        private boolean mCanceled = false;

        @Override
        public void start() {
            super.start();
            mLastPlayTime = 0;
        }

        public long getCurrentPlayTime() {
            long now = super.getCurrentPlayTime();
            mLastPlayTime = now;
            return now;
        }

        public long getLastPlayTime() {
            return mLastPlayTime;
        }

        public void setToTalScrollHeight(int height) {
            mToTalScrollHeight = height;
        }

        public int getToTalScrollHeight() {
            return mToTalScrollHeight;
        }

        public void cancel() {
            mCanceled = true;
            super.cancel();
        }

        public boolean isCanceled() {
            return mCanceled;
        }

        public void finish() {
            super.cancel();
        }

        public int getScrollBy() {
            long lasttime = getLastPlayTime();
            long nowtime = getCurrentPlayTime();
            if (nowtime > getDuration()) {
                nowtime = getDuration();
            }
            long d = nowtime - lasttime;
            int scrollBy = (int)(d*1.0f/getDuration()*getToTalScrollHeight());
            return scrollBy;
        }
    }

    private int mScrollHeight;
    private ListScrollAnimator mScrollAnimator = null;

    private void playScrollAnim(final boolean up, final Animator.AnimatorListener animatorListener) {
        if (mScrollAnimator != null) {
            mScrollAnimator.cancel();
        }
        mScrollAnimator = ListScrollAnimator.ofInt2(0, 1);
        if (up) {
            mScrollAnimator.setToTalScrollHeight(mScrollHeight);
        } else {
            mScrollAnimator.setToTalScrollHeight(-mScrollHeight);
        }
        mScrollAnimator.setDuration(200l);
        mScrollAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            public void onAnimationUpdate(ValueAnimator animation) {
                int scrollBy = ((ListScrollAnimator) animation).getScrollBy();
                if (scrollBy == 0 || canScrollList(scrollBy > 0 ? 1 : -1)) {
                    try {
                        //fixme IndexOutOfBoundsException in some case
                        scrollListBy(scrollBy);
                    } catch (IndexOutOfBoundsException e) {
                        log.error(e);
                        mScrollAnimator.finish();
                    }
                } else {
                    mScrollAnimator.finish();
                }
            }
        });
        if (animatorListener != null) {
            mScrollAnimator.addListener(animatorListener);
        }
        mScrollAnimator.start();
    }

    private Animator.AnimatorListener mResetFootListener = new Animator.AnimatorListener() {
        public void onAnimationStart(Animator animation) {
            mToTopDownAnimFlag = true;
        }

        public void onAnimationEnd(Animator animation) {
            if (animation instanceof ListScrollAnimator) {
                ListScrollAnimator animator = (ListScrollAnimator) animation;
                if (!animator.isCanceled()) {
                    updateFootHeight();
                }
            } else {
                throw new RuntimeException("unknown animator type");
            }
            mToTopDownAnimFlag = false;
        }

        public void onAnimationCancel(Animator animation) {
            mToTopDownAnimFlag = false;
        }

        public void onAnimationRepeat(Animator animation) {

        }
    };

    private Animator.AnimatorListener mTopListener = new Animator.AnimatorListener() {
        public void onAnimationStart(Animator animation) {
            mToTopDownAnimFlag = true;
        }

        public void onAnimationEnd(Animator animation) {
            mToTopDownAnimFlag = false;
        }

        public void onAnimationCancel(Animator animation) {
            mToTopDownAnimFlag = false;
        }

        public void onAnimationRepeat(Animator animation) {

        }
    };

    private Runnable mScrollAction = new Runnable() {
        public void run() {
            playScrollAnim(true, mTopListener);
        }
    };

    public void scrollInputViewToTop() {
        final BubbleItemView view = findInputtingView();
        if (view != null) {
            int addHeight = getHeight() - mOptHeadView.getHeight();
            final int targetHeight = addHeight;
            mScrollHeight = view.getTop() - mOptHeadView.getHeight();
            mFootSpace.getLayoutParams().height = targetHeight;
            mFootSpace.setLayoutParams(mFootSpace.getLayoutParams());
            UIHandler.post(mScrollAction);
        } else {
            log.error("not find inputting view");
        }
    }

    public void scrollListUp(int height) {
        mScrollHeight = height;
        playScrollAnim(true, mResetFootListener);
    }

    public void scrollInputViewBack() {
        int pos = getPositionForFootSpace();
        if (pos >= 0) {
            mScrollHeight = getHeight() - (mFootSpace.getTop() + 1);
            playScrollAnim(false, mResetFootListener);
        } else {
            if (mScrollAnimator != null) {
                mScrollAnimator.cancel();
            }
            updateFootHeight();
        }
    }

    public int getPositionForFootSpace() {
        try {
            return getPositionForView(mFootSpace);
        } catch (Exception e) {
            // catch for pre android 6.0, if mFootSpace has no parent
            // this func will throw null pointer exception
            return INVALID_POSITION;
        }
    }

    public BubbleItemView findInputtingView() {
        int count = getChildCount();
        for (int i = 0; i < count; i++) {
            View view = getChildAt(i);
            if (view instanceof BubbleItemView) {
                if (((BubbleItemView) view).getBubbleItem().isNeedInput()) {
                    return (BubbleItemView) view;
                }
            }
        }
        return null;
    }

    public PointF getToAddLoc() {
        PointF result = new PointF();
        int pos = getFirstVisiblePosition();
        if (pos > 2) {
            return new PointF(getRight() - mBubbleAdapter.getNormalItemWidth()/2, -100);
        } else {
            int[] loc = new int[2];
            if (mBubbleAdapter.getCount() > 0) {
                int count = getChildCount();
                for (int i = 0; i < count; i++) {
                    View view = getChildAt(i);
                    if(view instanceof BubbleItemView) {
                        view.getLocationOnScreen(loc);
                        int y = loc[1] + getNormalHeight() / 2;
                        int x = view.getRight() - mBubbleAdapter.getNormalItemWidth() / 2;
                        result.set(x, y);
                        return result;
                    }
                }
                mOptHeadView.getLocationOnScreen(loc);
                int y = loc[1] + mOptHeadView.getHeight() + mOptHeadView.getHeight() / 2;
                int x = mOptHeadView.getRight() - mOptHeadView.getWidth() / 2;
                result.set(x, y);
                return result;
            }
        }
        int[] loc = new int[2];
        getLocationOnScreen(loc);
        int x = getRight() - getResources().getDimensionPixelSize(R.dimen.bubble_item_normal_textwidth_min);
        int y = getResources().getDimensionPixelSize(R.dimen.bubble_list_top_margin) + loc[1] + mOptHeadView.getHeight() + mOptHeadView.getHeight()/2;
        result.set(x, y);
        return result;
    }

    private FrameLayout mFullScreenAnimView = null;

    private void addFullScreenAnimWindow(Context context, boolean sidebarShowing) {
        int flag = WindowManager.LayoutParams.TYPE_SYSTEM_DIALOG;
        if (sidebarShowing) {
            flag = WindowManager.LayoutParams.TYPE_SIDEBAR_TOOLS;
        }
        mFullScreenAnimView = new FrameLayout(context);
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT);
        mFullScreenAnimView.setLayoutParams(params);
        final WindowManager.LayoutParams lp = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT,
                flag,
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                        | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_LAYOUT_INSET_DECOR
                        | WindowManager.LayoutParams.FLAG_FULLSCREEN
                        | WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED
                        | WindowManager.LayoutParams.FLAG_SPLIT_TOUCH,
                PixelFormat.TRANSLUCENT);
        lp.flags |= WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED;
        lp.gravity = Gravity.NO_GRAVITY;
        lp.setTitle("bubble_drag_end_anim_view");
        lp.packageName = context.getPackageName();
        mFullScreenAnimView.setBackgroundResource(android.R.color.transparent);
        mFullScreenAnimView.setVisibility(View.VISIBLE);
        BubbleController.getInstance().getWindowManager().addView(mFullScreenAnimView, lp);
    }

    private void startDragEndAnim(final Context context, float rawX, float rawY, final DragEvent event) {
        if (StatusManager.getStatus(StatusManager.BUBBLE_ANIM)) {
            return;
        }
        StatusManager.setStatus(StatusManager.BUBBLE_ANIM, true);
        final boolean sidebarShowing;
        if (BubbleController.getInstance().isExtDisplay()) {
            sidebarShowing = false;
            addFullScreenAnimWindow(context, sidebarShowing);
        } else {
            sidebarShowing = SidebarUtils.isSidebarShowing(context);
            addFullScreenAnimWindow(context, sidebarShowing);
            if (sidebarShowing) {
                rawY = rawY + Constants.TOP_VIEW_HEIGHT;
            }
            if (SidebarUtils.getSidebarModeState() == SidebarMode.MODE_LEFT) {
                rawX = rawX + Constants.SIDE_VIEW_WIDTH;
            }
        }
        int count = getChildCount();
        final List<Anim> animList = new ArrayList<Anim>();
        final int time = 200;
        int animMax = 15;
        for (int i = 0; i < count; i++) {
            View view = getChildAt(i);
            if (view == null) {
                continue;
            }
            if (!(view instanceof BubbleItemView)) {
                continue;
            }
            if (view.getVisibility() == View.INVISIBLE) {
                int[] viewLoc = new int[2];
                view.getLocationOnScreen(viewLoc);
                viewLoc[0] -= view.getTranslationX();
                BubbleItemView itemView = (BubbleItemView) view;
                final BubbleItemView fakeBubble = (BubbleItemView) View.inflate(context, R.layout.bubble_item, null);
                BubbleItem bubbleItem = itemView.getBubbleItem();
                fakeBubble.setMode(mBubbleAdapter.getMode());
                int flagsbak = bubbleItem.getFlags();
                bubbleItem.setIsTemp(false);
                fakeBubble.setVisibility(View.VISIBLE);
                mFullScreenAnimView.addView(fakeBubble);
                fakeBubble.show(bubbleItem);
                bubbleItem.setFlags(flagsbak);
                Vector3f to = new Vector3f();
                if (sidebarShowing) {
                    to.setX(viewLoc[0] * Constants.WIDTH_SCALE);
                    to.setY(Constants.TOP_VIEW_HEIGHT + viewLoc[1] * Constants.HEIGHT_SCALE);
                    if (SidebarUtils.getSidebarModeState() == SidebarMode.MODE_LEFT) {
                        to.setX(to.getX() + Constants.SIDE_VIEW_WIDTH);
                    }
                } else {
                    to.setX(viewLoc[0]);
                    to.setY(viewLoc[1]);
                }
                Anim moveAnim = new Anim(fakeBubble, Anim.TRANSLATE, time, Anim.CUBIC_OUT, new Vector3f(), to);
                animList.add(moveAnim);
                if (animList.size() == animMax) {
                    break;
                }
            }
        }
        final Vector3f from = new Vector3f(rawX, rawY);
        mFullScreenAnimView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                mFullScreenAnimView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                AnimListener animEndListener = new SimpleAnimListener() {
                    @Override
                    public void onStart() {}

                    @Override
                    public void onComplete(int type) {
                        mBubbleAdapter.onSelfDragEnd(event, mDropBack);
                        mDropBack = false;
                        Anim hideAnim = new Anim(mFullScreenAnimView, Anim.TRANSPARENT, 100, Anim.CUBIC_OUT, Anim.VISIBLE, Anim.INVISIBLE);
                        hideAnim.setListener(new SimpleAnimListener() {

                            @Override
                            public void onComplete(int type) {
                                StatusManager.setStatus(StatusManager.BUBBLE_ANIM, false);
                                BubbleController.getInstance().getWindowManager().removeView(mFullScreenAnimView);
                            }
                        });
                        hideAnim.start();
                    }
                };
                AnimTimeLine timeLine = new AnimTimeLine();
                for (Anim anim : animList) {
                    View view = anim.getView();
                    View content = view.findViewById(R.id.fl_layout);
                    Vector3f newFrom = anim.getFrom();
                    newFrom.setX(from.getX() - content.getLeft() - ((BubbleItemView)view).queryWidth() / 2);
                    newFrom.setY(from.getY() - content.getHeight() / 2);
                    anim.setFrom(newFrom);
                    if (sidebarShowing) {
                        view.setScaleX(Constants.WIDTH_SCALE);
                        view.setScaleY(Constants.HEIGHT_SCALE);
                        Vector3f to = anim.getTo();
                        to.setX(to.getX() - (1 - Constants.WIDTH_SCALE) * view.getWidth() / 2);
                        to.setY(to.getY() - (1 - Constants.HEIGHT_SCALE) * view.getHeight() / 2);
                        anim.setTo(to);
                    }

                    Anim alpha = new Anim(view, Anim.TRANSPARENT, time, Anim.CUBIC_OUT, Anim.INVISIBLE, Anim.VISIBLE);
                    timeLine.addAnim(alpha);
                    timeLine.addAnim(anim);
                }
                if (timeLine.isEmpty()) {
                    animEndListener.onComplete(Anim.ANIM_FINISH_TYPE_COMPLETE);
                } else {
                    timeLine.setAnimListener(animEndListener);
                    timeLine.start();
                }
            }
        });
    }

    public void deleteBubbles(ArrayList<Integer> list) {
        mBubbleAdapter.chooseToDelete(null, list);
    }

    public boolean onKeyDown(int keyCode, KeyEvent event) {
        log.info("receive keycode down:" + keyCode);
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            BubbleController.getInstance().setWillAcceptKeyCode(false);
            hideBubbleListView();
            return true;
        }
        return false;
    }

    public void resetBubbleDragging() {
        if (StatusManager.isBubbleDragging()) {
            mBubbleAdapter.onSelfDragEnd(null, false);
        }
        mBubbleAdapter.clearFakeList();
    }

    public void playDropBackAnimtion(List<View> viewlist, List<Integer> start, List<Integer> end) {
        cancelDraggingAnim();
        if (mBubbleListDraggingAnimRunner != null) {
            mBubbleListDraggingAnimRunner.recycle();
        }
        mBubbleListDraggingAnimRunner = new BubbleListDropBackAnimRunner(this, viewlist, start, end);
        mBubbleListDraggingAnimRunner.start();
    }

    public void cancelDraggingAnim() {
        if (mBubbleListDraggingAnimRunner instanceof BubbleListDraggingAnimRunner
                && mBubbleListDraggingAnimRunner.isRunning()) {
            mBubbleListDraggingAnimRunner.cancel();
        }
    }

    public void playFlyToAnimtion(AnimTimeLine animTimeLine, final List<View> viewList) {
        final ViewGroup parent = (ViewGroup) getParent();
        for(View view:viewList) {
            parent.addView(view, parent.getChildCount());
        }
        animTimeLine.setAnimListener(new AnimCancelableListener() {
            public void onAnimCompleted() {
                int count = parent.getChildCount();
                for (int i = count - 1; i >= 0; i--) {
                    View view = parent.getChildAt(i);
                    if (view instanceof BubbleItemView) {
                        parent.removeView(view);
                    }
                }
            }
        });
        animTimeLine.start();
    }

    public boolean checkShowHeadDetail() {
        return mBubbleHeadOperator.mllHeadExpandDetail.getVisibility() == VISIBLE;
    }

    public void filtBubblesByWords(String txt) {
        mBubbleAdapter.filterBubbles(txt);
    }

    public int getEmptyAvailableHeight() {
        if (getHeight() <= 0) {
            return 0;
        }
        if (getChildCount() == 0) {
            return getHeight();
        }
        if (getChildCount() > 3) {
            return getHeight() - getChildAt(getChildCount() - 1).getBottom();
        } else {
            // just 2 header,1 footer
            View lastView = getChildAt(getChildCount() - 1);
            int bottomIncrease = FOOT_SPACE_H - lastView.getHeight();
            return getHeight() - getChildAt(getChildCount() - 1).getBottom() - bottomIncrease;
        }
    }

    public void hideFiltrateWindow() {
        mBubbleHeadOperator.hideHeadFiltrate();
    }

    void toMode(int mode, boolean needAnim) {
        if (mBubbleHeadOperator.getMode() == mode) {
            return;
        }
        mBubbleHeadOperator.toMode(mode, needAnim);
    }
}
