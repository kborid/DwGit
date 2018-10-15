package com.smartisanos.ideapills.view;

import android.animation.ValueAnimator;
import android.app.SmtPCUtils;
import android.content.Context;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.Region;
import android.os.RemoteException;
import android.util.AttributeSet;
import android.view.DragEvent;
import android.view.IWindow;
import android.view.IWindowSession;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.ViewRootImpl;
import android.view.ViewTreeObserver;
import android.view.WindowManagerGlobal;
import android.widget.FrameLayout;

import com.smartisanos.ideapills.BubbleController;
import com.smartisanos.ideapills.Constants;
import com.smartisanos.ideapills.R;
import com.smartisanos.ideapills.common.anim.Anim;
import com.smartisanos.ideapills.common.anim.AnimListener;
import com.smartisanos.ideapills.common.anim.AnimTimeLine;
import com.smartisanos.ideapills.common.anim.SimpleAnimListener;
import com.smartisanos.ideapills.common.anim.Vector3f;
import com.smartisanos.ideapills.entity.BubbleItem;
import com.smartisanos.ideapills.common.util.CommonUtils;
import com.smartisanos.ideapills.util.LOG;
import com.smartisanos.ideapills.util.StatusManager;
import com.smartisanos.ideapills.common.util.UIHandler;
import com.smartisanos.ideapills.util.ViewUtils;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Set;

public class BubbleOptLayout extends FrameLayout {
    private static final LOG log = LOG.getInstance(BubbleOptLayout.class);
    public static final float MIN_VELOCITY = 4.0f;
    public static final float SCALE_MIN_VELOCITY = 2.5f;
    private static final int SLIDE_STATE_NONE = 0;
    private static final int SLIDE_STATE_VERTICAL = 1;
    private static final int SLIDE_STATE_HORIZONTAL = 2;
    private static final int mToFollowThreshold = 80;
    private BubbleListView mBubbleList = null;
    private PullViewGroup mIvPull = null;
    private IWindowSession mWindowSession = null;
    private IWindow mIWindow = null;
    private boolean mIsPulling = false;
    private float mDownRawX;
    private float mDownY;
    private int mSlideState;
    private Anim mResetAnim;
    private Region mTouchRegion = new Region();
    private Region mListRegion = new Region();
    private InternalInsetsInfo mInsets = new InternalInsetsInfo();
    private BubbleItemView mfakeView = null;

    private float mCurMoveX;
    private boolean mAlreadyKeep = false;
    private ValueAnimator mFollowAnimator = null;
    private boolean mIsDraggingText = false;
    private float mBaseDistance = 0.0f;
    private boolean mForceShow = false;
    private int mBubbleListWidth;
    /**
     * indicate whether the drag event from outer such as text boom edit.
     */
    private boolean mDragFromOuter = false;

    private class InternalInsetsInfo {
        public final Rect contentInsets = new Rect();
        public final Rect visibleInsets = new Rect();
        public final Region touchableRegion = new Region();

        /**
         * {@see #ViewTreeObserver.InternalInsetsInfo.setTouchableInsets(int)}
         */
        public final int mTouchableInsets = 3;
    }

    private Runnable mUpdateRegionAction = new Runnable() {
        public void run() {
            Rect lastTouchRegion = mTouchRegion.getBounds();
            if (mIsPulling) {
                mTouchRegion.set(0, 0, getWidth(), getHeight());
            } else {
                if (mBubbleList.getVisibility() == VISIBLE) {
                    mBubbleList.computeRegion(mListRegion);
                    mListRegion.translate(mBubbleList.getLeft(), mBubbleList.getTop());
                    mTouchRegion.set(getLeft(), getTop(), getLeft() + getWidth(), getTop() + getHeight());
                    if (BubbleController.getInstance().isExtDisplay()) {
                        Rect rect = mTouchRegion.getBounds();
                        if (StatusManager.isBubbleDragging()) {
                            mTouchRegion.set(rect.right - mListRegion.getBounds().width(), rect.top, rect.right, rect.bottom);
                        } else if (StatusManager.getStatus(StatusManager.ADDING_ATTACHMENT)) {
                            mTouchRegion.set(mListRegion);
                            mTouchRegion.translate(getLeft(),getTop());
                        }
                    }
                } else if(mIvPull.getVisibility() == VISIBLE){
                    mTouchRegion.setEmpty();
                    int rightTouchExpand = ViewUtils.dp2px(getContext(), 10);
                    if (mIsDraggingText) {
                        //todo : hard code
                        mTouchRegion.set(mIvPull.getLeft() - 20, mIvPull.getTop(),
                                Math.min(mIvPull.getRight(), getWidth() + rightTouchExpand), mIvPull.getBottom());
                    } else {
                        mTouchRegion.set(mIvPull.getLeft(), mIvPull.getTop(),
                                Math.min(mIvPull.getRight(), getWidth() + rightTouchExpand), mIvPull.getBottom());
                    }
                    mTouchRegion.translate(getLeft(), getTop());
                } else {
                    log.info("listview visibility=" + mBubbleList.getVisibility() + " pullview visibility=" + mIvPull.getVisibility());
                }
            }
            if (mBubbleList.getVisibility() == GONE) {
                updateTouchArea(mTouchRegion);
            } else if (BubbleController.getInstance().isExtDisplay()) {
                Rect rect = mTouchRegion.getBounds();
                if (!lastTouchRegion.equals(rect)) {
                    updateTouchArea(mTouchRegion);
                }
            }
        }
    };

    private BubbleListView.BubbleListActionCallback mBubbleListActionCallback = new BubbleListView.BubbleListActionCallback() {
        public boolean sendCommand(int msg, Object... args) {
            switch (msg) {
                case MSG_SUSPENDHEADVIEW:
                    log.error("MSG_SUSPENDHEADVIEW !");
                    View view = (View) args[0];
                    BubbleOptLayout.this.addView(view);
                    return true;
                case MSG_CHANGREGION:
                    postUpdateRegionAction();
                    return true;
            }
            return false;
        }
    };

    public BubbleOptLayout(Context context) {
        this(context, null);
    }

    public BubbleOptLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public BubbleOptLayout(Context context, AttributeSet attrs,
                           int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mfakeView = (BubbleItemView) LayoutInflater.from(context).inflate(R.layout.bubble_item, null);
        mBubbleListWidth = mContext.getResources().getDimensionPixelSize(R.dimen.bubbleopt_layout_width);
    }

    protected void onFinishInflate() {
        super.onFinishInflate();
        CommonUtils.setAlwaysCanAcceptDrag(this, true);
        mBubbleList = (BubbleListView) findViewById(R.id.bubblelistview);
        mBubbleList.setBubbleListActionCallback(mBubbleListActionCallback);
        mIvPull = (PullViewGroup) findViewById(R.id.view_pull);
        mBubbleList.setPullView(mIvPull);
        mIvPull.setListView(mBubbleList);
        mIvPull.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return handleTouchEvent(event, true);
            }
        });
    }

    public BubbleListView getBubbleListView() {
        return mBubbleList;
    }

    private void dimBackgroudByMove(float interpolator) {
        BubbleController.getInstance().dimBackgroundByMove(interpolator);
    }

    private boolean handleTouchEvent(MotionEvent event, boolean dircTouch) {
        if (BubbleController.getInstance().isInPptContext(getContext())) {
            return false;
        }
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN: {
                mIsPulling = true;
                mDownRawX = event.getRawX();
                mDownY = event.getY();
                mSlideState = SLIDE_STATE_NONE;
                postUpdateRegionAction(0);
                if (mResetAnim != null) {
                    mResetAnim.cancel();
                    mResetAnim = null;
                }
                mIvPull.clearTransparent();
                mAlreadyKeep = false;
                mBaseDistance = 0.0f;
                mCurMoveX = 0.0f;
            }
            break;
            case MotionEvent.ACTION_MOVE:
                float moveX = event.getRawX() - mDownRawX;
                if (moveX > mCurMoveX) {
                    mForceShow = false;
                    log.info("mForceShow false");
                }
                mCurMoveX = moveX;
                float moveY = event.getY() - mDownY;
                if (SLIDE_STATE_NONE == mSlideState) {
                    if (Math.abs(moveX) >= Math.abs(moveY)) {
                        if (Math.abs(moveX) > 10) {
                            mSlideState = SLIDE_STATE_HORIZONTAL;
                        }
                    } else {
                        if (Math.abs(moveY) > 10) {
                            mSlideState = SLIDE_STATE_VERTICAL;
                        }
                    }
                }
                if (SLIDE_STATE_HORIZONTAL == mSlideState) {
                    if (moveX < 0) {
                        final float targetTranX = mIvPull.getStableWidth() - mIvPull.getFullWidth();
                        if (moveX >= -mToFollowThreshold && !mAlreadyKeep) {
                            float tranX = moveX / 10.0f;
                            dimBackgroudByMove(tranX / targetTranX);
                            mIvPull.setTranslationX(tranX);
                        } else if (moveX < -mToFollowThreshold && !mAlreadyKeep) {
                            mAlreadyKeep = true;
                            mBaseDistance = moveX - mIvPull.getTranslationX();
                        } else if (mAlreadyKeep) {
                            float tranX = mIvPull.getLimitTranX(moveX - mBaseDistance);
                            dimBackgroudByMove(tranX / targetTranX);
                            mIvPull.setTranslationX(tranX);
                        }
                    }
                } else if (dircTouch && SLIDE_STATE_VERTICAL == mSlideState) {
                    mBubbleList.dragPullViewVertical(moveY);
                }
                break;
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
                float moveX1 = event.getRawX() - mDownRawX;
                if (mFollowAnimator != null) {
                    mFollowAnimator.cancel();
                    mFollowAnimator = null;
                }
                mIsPulling = false;
                postUpdateRegionAction(0);
                if (SLIDE_STATE_NONE == mSlideState && dircTouch) {
                    mBubbleList.playShowAnimationAfterPull(null);
                } else if (mForceShow) {
                    mBubbleList.playShowAnimationAfterPull(null);
                    mForceShow = false;
                } else if (moveX1 > -mToFollowThreshold) {
                    Vector3f from = new Vector3f(mIvPull.getTranslationX(), 0);
                    mResetAnim = new Anim(mIvPull, Anim.TRANSLATE, 150, Anim.CUBIC_OUT, from, Anim.ZERO);
                    AnimListener listener = new SimpleAnimListener() {
                        @Override
                        public void onStart() {

                        }

                        @Override
                        public void onComplete(int type) {
                            refreshPullViewAlpha();
                        }
                    };
                    if (mResetAnim.isEmpty()) {
                        listener.onComplete(0);
                    } else {
                        mResetAnim.setListener(listener);
                        mResetAnim.start();
                    }
                } else if (mIvPull.getTranslationX() < 0) {
                    mBubbleList.playShowAnimationAfterPull(null);
                } else {
                    refreshPullViewAlpha();
                }
                break;
            default:
                break;
        }
        return true;
    }

    public BubbleItemView findInputtingView() {
        return mBubbleList.findInputtingView();
    }

    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (mBubbleList.isRunningAnimation() || mIvPull.getAnimation() != null || mIvPull.hasAnimRuning()) {
            return true;
        }
        if (ev.getAction() == MotionEvent.ACTION_DOWN) {
            if (StatusManager.isBubbleDragging() || BubbleController.getInstance().isShieldShowList()) {
                return false;
            }
            if (mBubbleList.getVisibility() == VISIBLE && !mListRegion.contains((int) ev.getX(), (int) ev.getY())) {
                log.info("hide list for touch outside");
                if (BubbleController.getInstance().isInPptContext(getContext())) {
                    return false;
                }
                super.dispatchTouchEvent(ev);
                MotionEvent cancel = MotionEvent.obtain(ev);
                cancel.setAction(MotionEvent.ACTION_CANCEL);
                super.dispatchTouchEvent(cancel);
                playHideAnimation();
                return false;
            }
            BubbleItemView view = findInputtingView();
            if (view != null) {
                Rect rect = view.getRect();
                rect.offset(mBubbleList.getLeft(), mBubbleList.getTop());
                if (!rect.contains((int) ev.getX(), (int) ev.getY())) {
                    view.finishInputting();
                    return true;
                }
            }
        }
        return super.dispatchTouchEvent(ev);
    }

    private void postUpdateRegionAction() {
        //todo hard code
        postUpdateRegionAction(50);
    }

    public void postUpdateRegionAction(int delay) {
        UIHandler.removeCallbacks(mUpdateRegionAction);
        UIHandler.postDelayed(mUpdateRegionAction, delay);
    }

    private IWindow getWindow() {
        if (mIWindow == null) {
            ViewParent viewParent = getParent().getParent();
            if (!(viewParent instanceof ViewRootImpl)) {
                return null;
            }
            ViewRootImpl viewRoot = (ViewRootImpl) viewParent;
            try {
                Field field = ViewRootImpl.class.getDeclaredField("mWindow");
                field.setAccessible(true);
                mIWindow = (IWindow) (field.get(viewRoot));
            } catch (NoSuchFieldException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
        return mIWindow;
    }

    public void enableTocuchArea() {
        Region fullRegion = new Region(0, 0, getWidth(), getHeight());
        if (BubbleController.getInstance().isExtDisplay()) {
            Rect rect = fullRegion.getBounds();
            if (StatusManager.isBubbleDragging() || StatusManager.getStatus(StatusManager.ADDING_ATTACHMENT)) {
                fullRegion.set(rect.right - mBubbleListWidth, rect.top, rect.right, rect.bottom);
            }
        }
        updateTouchArea(fullRegion);
    }

    public void updateTouchArea(Region region) {
        if (BubbleController.getInstance().isInPptContext(getContext())) {
            return;
        }
        if (mInsets.touchableRegion.equals(region)) {
            return;
        }
        if (SmtPCUtils.isValidExtDisplayId(getContext())) {
            log.d("updateTouchArea ext:" + region);
        } else {
            log.d("updateTouchArea:" + region);
        }
        mInsets.touchableRegion.set(region);
        if (mWindowSession == null) {
            mWindowSession = WindowManagerGlobal.getWindowSession();
        }
        try {
            if (getWindow() != null) {
                mWindowSession.setInsets(getWindow(), mInsets.mTouchableInsets,
                        mInsets.contentInsets, mInsets.visibleInsets, mInsets.touchableRegion);
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public void handleBubbleBySelf(Set<Integer> ids) {
        mBubbleList.handleBubbleBySelf(ids);
    }

    public void playHideAnimation() {
        playHideAnimation(false);
    }

    public void playHideAnimationAndResumeToNormal() {
        if (mBubbleList.getVisibility() == VISIBLE) {
            mBubbleList.hideBubbleListView(false, new Runnable() {
                @Override public void run() {
                    mBubbleList.toMode(ViewMode.BUBBLE_NORMAL, false);
                }
            });
        }
    }

    public void playHideAnimation(boolean isForceStopLastAnimIfRunning) {
        if (mBubbleList.getVisibility() == VISIBLE) {
            mBubbleList.hideBubbleListView(isForceStopLastAnimIfRunning);
        } else {
            log.info("mBubbleList already hide");
        }
    }

    public void playHideAnimationWithTask(Runnable task) {
        if (mBubbleList.getVisibility() == VISIBLE) {
            mBubbleList.hideBubbleListView(task);
        } else {
            if (task != null) {
                task.run();
            }
            log.info("mBubbleList already hide");
        }
    }

    public void onInputStatusChange(boolean needInput) {
        if (needInput) {
            mBubbleList.scrollInputViewToTop();
        } else {
            mBubbleList.scrollInputViewBack();
        }
    }

    public PointF getToAddLoc() {
        if (mBubbleList.getVisibility() == VISIBLE) {
            PointF result = mBubbleList.getToAddLoc();
            return result;
        } else {
            int[] loc = new int[2];
            mIvPull.getLocationOnScreen(loc);
            PointF p = new PointF(loc[0] - 50, loc[1] + mIvPull.getHeight());
            return p;
        }
    }

    public void playAnimBubbleFlyIn(BubbleItem item) {
        if (mfakeView.getParent() != null) {
            removeView(mfakeView);
        }
        mfakeView.setTranslationX(0.0f);
        mfakeView.setTranslationY(mIvPull.getBottom());
        mfakeView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                mfakeView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                mfakeView.getLayoutParams().height = mIvPull.getHeight();
                mfakeView.setLayoutParams(mfakeView.getLayoutParams());
                int time = 150;
                final AnimTimeLine timeLine = new AnimTimeLine();
                Anim alphaAnim = new Anim(mfakeView, Anim.TRANSPARENT, time, Anim.CUBIC_OUT, Anim.INVISIBLE, Anim.VISIBLE);
                Anim scaleAnim = new Anim(mfakeView, Anim.SCALE, time * 2, 0, new Vector3f(0.9f, 0.9f), new Vector3f(1, 1));
                scaleAnim.setInterpolator(Anim.BACK_OUT, 0);
                timeLine.addAnim(alphaAnim);
                timeLine.addAnim(scaleAnim);
                timeLine.setAnimListener(new SimpleAnimListener() {
                    @Override
                    public void onStart() {

                    }

                    @Override
                    public void onComplete(int type) {
                        Anim animmove = new Anim(mfakeView, Anim.TRANSLATE, 200, 0, Anim.ZERO, new Vector3f(mfakeView.queryWidth(), 0));
                        animmove.setDelay(200);
                        animmove.setListener(new SimpleAnimListener() {

                            @Override
                            public void onComplete(int type) {
                                removeView(mfakeView);
                            }
                        });
                        animmove.start();
                    }
                });
                timeLine.start();
            }
        });
        mfakeView.show(item);
        addView(mfakeView);
    }

    public void deleteBubbles(ArrayList<Integer> list) {
        mBubbleList.deleteBubbles(list);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);
        postUpdateRegionAction();
    }

    public boolean dispatchDragEvent(DragEvent event) {
        if (event == null) {
            return false;
        }
        int action = event.getAction();
        if (!Constants.IS_IDEA_PILLS_ENABLE && action == DragEvent.ACTION_DRAG_STARTED) {
            log.info("ideapills is off");
            return false;
        }
        if (action == DragEvent.ACTION_DRAG_STARTED) {
            if (mBubbleList.getVisibility() == GONE) {
                log.info("drag event started from outside!");
                mBubbleList.setStopAcceptLoc(true);
                mDragFromOuter = true;
            } else {
                mBubbleList.setStopAcceptLoc(false);
                mDragFromOuter = false;
            }
        }
        if (action == DragEvent.ACTION_DRAG_ENDED) {
            StatusManager.setStatus(StatusManager.GLOBAL_DRAGGING, false);
            mIsDraggingText = false;
            postUpdateRegionAction(0);
        }
        if (action == DragEvent.ACTION_DRAG_LOCATION && !mDragFromOuter
                && mBubbleList.getVisibility() == VISIBLE) {
            boolean actionInListRegion = mListRegion.contains((int) event.getX(), (int) event.getY());
            boolean isInExtDisplay = SmtPCUtils.isValidExtDisplayId(getContext());
            if (!mBubbleList.isStopAcceptDragLoc() && !actionInListRegion) {
                if (!isInExtDisplay) {
                    mBubbleList.hideBubbleListView();
                } else if (!BubbleController.getInstance().isInputting()) {
                    mBubbleList.resetDrag();
                }
                mBubbleList.setStopAcceptLoc(true);
            } else if (isInExtDisplay && mBubbleList.isStopAcceptDragLoc() && actionInListRegion) {
                mBubbleList.setStopAcceptLoc(false);
            }
        }
        boolean handled = super.dispatchDragEvent(event);
        if (handled || event.getAction() == DragEvent.ACTION_DRAG_STARTED) {
            mIsDraggingText = true;
            postUpdateRegionAction(0);
        }
        if (event.getAction() == DragEvent.ACTION_DRAG_ENDED) {
            mBubbleList.resetBubbleDragging();
        }
        return handled;
    }

    public void refreshPullViewAlpha() {
        mIvPull.delayToTransparent();
    }

    public void onPackageChanged() {
        int count = mBubbleList.getChildCount();
        for (int i = 0; i < count; i++) {
            View view = mBubbleList.getChildAt(i);
            if (view == null) {
                continue;
            }
            if (view instanceof BubbleItemView) {
                ((BubbleItemView)view).onInstalledPackageChanged();
            }
        }
    }

    public class TouchReceiver implements OnTouchListener{
        private boolean mDownEver = false;

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            if (!mDownEver) {
                mDownEver = true;
                MotionEvent down = MotionEvent.obtain(event);
                down.setAction(MotionEvent.ACTION_DOWN);
                return handleTouchEvent(down, false);
            }
            return handleTouchEvent(event, false);
        }
    }

    public void setForceShow(boolean forceShow) {
        mForceShow = forceShow;
    }

    public boolean isBubbleListVisible() {
        return mBubbleList.getVisibility() == VISIBLE;
    }

    public void hideBubbleListImmediately() {
        mBubbleList.hideBubbleListImmediately();
    }

    public void hideBubbleListWithAnim() {
        mBubbleList.hideBubbleListView();
    }

    public void showBubbleList(long syncId) {
        mBubbleList.showBubbleList(syncId);
    }

    public void forceHideAll() {
        mBubbleList.hideBubbleListWithoutAnyAnim();
        mBubbleList.onInputOver();
        mIvPull.clearStatusThenToTransparent();
    }
}
