package com.smartisanos.sara.widget;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Rect;
import android.os.Handler;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.ScaleAnimation;
import android.view.animation.TranslateAnimation;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;

import android.widget.TextView;

import smartisanos.util.LogTag;

import com.smartisanos.sara.entity.ShortcutApp;
import com.smartisanos.sara.shell.INotificationCustomLayoutStrategy;
import com.smartisanos.sara.shell.NewSystemUINotificationCustomLayoutStrategy;
import com.smartisanos.sara.shell.ShortcutAppManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.smartisanos.sara.R;

/**
 * copy from com.android.settings.widget.NotificationCustomView
 */
public class ShortcutAppSettingView extends FrameLayout implements DragGridView.GridViewDragListener {

    private final static String TAG = "ShortcutAppSettingView";
    private final int INVALID_POSITION = -1;
    private final int DELAY_SHOW_APP_NAME = 100;
    private final int ANIM_DURATION = 300;
    private final float SCALE_RATE = 1.8f;

    private int ICON_SIZE;
    private int DISTANCE_MOVE_BOTTOM;
    private int DRAG_VIEW_LEFT_MARGIN;
    private int GRID_VIEW_ITEM_PADDING;
    private int DRAG_VIEW_ITEM_PADDING;
    private long LONG_PRESS_TRIGGER_TIME;

    private boolean mIsDragGridViewItem = false;
    private boolean isAnimPlaying = false;


    private List<ShortcutApp> mNotificationList = new ArrayList<>(12);
    private List<ShortcutApp> mCandidateWidgetList = new ArrayList<>();

    private ImageView mDragView;
    private ImageView mMoveView;
    private TextView mAppNameView;

    private DragGridView mDragGridView;
    private DragAdapter mDragAdapter;

    private ViewGroup mCandidateContainer;
    private TextView mCandidateContainerTip;
    private ImageView mTargetView; // the view to be draged
    private Handler mHandler = new Handler();
    private WidgetOrderChangedListener mWidgetOrderChangedListener;

    private INotificationCustomLayoutStrategy mLayoutStrategy;

    public interface WidgetOrderChangedListener {
        void onWidgetOrderChanged();
    }

    public ShortcutAppSettingView(Context context) {
        super(context, null);
        init(context);
    }

    public ShortcutAppSettingView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public ShortcutAppSettingView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context);
    }

    private void init(Context context) {
        mLayoutStrategy = createLayoutStrategy();
        initDimens();

        View rootChild = mLayoutStrategy.onInitRootChildView(context, this);
        if (rootChild == null) {
            throw new NullPointerException("root child can NOT be NULL");
        }
        if (rootChild == this || rootChild.getParent() != null) {
            if (rootChild != this && rootChild.getParent() != this) {
                throw new IllegalStateException("provided root child can only attach to the given parent");
            }
        } else {
            this.addView(rootChild);
        }

        mDragGridView = mLayoutStrategy.getDragGridView(rootChild);
        mCandidateContainer = mLayoutStrategy.getCandidateContainer(rootChild);
        mCandidateContainerTip = (TextView) rootChild.findViewById(R.id.candidate_container_tip);

        mDragGridView.setGridViewDragListener(this);
        mDragGridView.setDragResponseMS(50);

        mDragView = new ImageView(getContext());
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup
                .LayoutParams.WRAP_CONTENT);
        params.leftMargin = -DRAG_VIEW_LEFT_MARGIN;
        mDragView.setPadding(DRAG_VIEW_ITEM_PADDING, DRAG_VIEW_ITEM_PADDING, DRAG_VIEW_ITEM_PADDING,
                DRAG_VIEW_ITEM_PADDING);
        mDragView.setLayoutParams(params);
        mDragView.setVisibility(View.INVISIBLE);
        this.addView(mDragView);

        FrameLayout.LayoutParams params2 = new FrameLayout.LayoutParams(ICON_SIZE, ICON_SIZE);
        mMoveView = new ImageView(getContext());
        params.leftMargin = -DRAG_VIEW_LEFT_MARGIN;
        mMoveView.setLayoutParams(params2);
        mMoveView.setVisibility(View.INVISIBLE);
        this.addView(mMoveView);

        initAppNameTextView();

        loadGridViewData(false);

        loadCandidateWidgetData(false, -1, null, null);

        int dataListSize = mNotificationList.size();
        if ((mCandidateWidgetList.size() == 0 ||
                (mCandidateWidgetList.size() == 1 && TextUtils.isEmpty(mCandidateWidgetList.get(0).getDispalyName()))) &&
                (dataListSize > 0 && TextUtils.isEmpty(mNotificationList.get(dataListSize - 1).getDispalyName()))) {
            mNotificationList.remove(dataListSize - 1);
        }
        mDragAdapter = new DragAdapter(mNotificationList);
        mDragGridView.setAdapter(mDragAdapter);
    }

    private static INotificationCustomLayoutStrategy createLayoutStrategy() {

        return new NewSystemUINotificationCustomLayoutStrategy();
    }

    private void initDimens(){
        ICON_SIZE = getResources().getDimensionPixelSize(R.dimen.widget_icon_size);
        DISTANCE_MOVE_BOTTOM = getResources().getDimensionPixelOffset(R.dimen.widget_distance_move_bottom);
        DRAG_VIEW_LEFT_MARGIN = getResources().getDimensionPixelOffset(R.dimen.drag_view_left_margin);
        GRID_VIEW_ITEM_PADDING = getResources().getDimensionPixelOffset(R.dimen.widgets_grid_view_item_padding);
        DRAG_VIEW_ITEM_PADDING = getResources().getDimensionPixelOffset(R.dimen.widget_drag_view_item_padding);
        LONG_PRESS_TRIGGER_TIME = mLayoutStrategy.getLongPressTriggerTimeMs();
    }

    public void onResume() {
        updateView();
    }

    public void setListener(WidgetOrderChangedListener listener) {
        mWidgetOrderChangedListener = listener;
    }

    private void updateView() {
        // do nothing .
    }

    private ShortcutApp getCandidateWidgetByChildViewPosition(int childViewPosition) {
        return mLayoutStrategy.getCandidateWidgetByChildViewPosition(mCandidateContainer, mCandidateWidgetList, childViewPosition);
    }

    private void loadGridViewData(boolean isUpdateData) {
        mNotificationList.clear();
        List<ShortcutApp> dataList = ShortcutAppManager.getLastSaveShareData(mContext);
        if (dataList != null) {
            int dataListSize = dataList.size();
            if (dataListSize > 0) {
                if (isUpdateData && (mCandidateWidgetList.size() == 0 ||
                        (mCandidateWidgetList.size() == 1 && TextUtils.isEmpty(mCandidateWidgetList.get(0).getDispalyName()))) &&
                        TextUtils.isEmpty(dataList.get(dataListSize - 1).getDispalyName())) {
                    dataList.remove(dataListSize - 1);
                }
                mNotificationList.addAll(dataList);
            }
        }
    }


    private void loadCandidateWidgetData(boolean isUpdateData, int positionInCandidate, ShortcutApp oldCandidateWidget, ShortcutApp targetWidget) {
        mCandidateWidgetList.clear();
        List<ShortcutApp> candidateApps = ShortcutAppManager.getCandidateList(mContext);
        if (candidateApps != null && candidateApps.size() > 0) {
            mCandidateWidgetList.addAll(candidateApps);
            if (isUpdateData) {
                onCandidateWidgetChanged(positionInCandidate, oldCandidateWidget, targetWidget);
            } else {
                mLayoutStrategy.onLayoutCandidateContainer(mCandidateContainer, mCandidateWidgetList);
            }
        } else {
            mCandidateContainer.setVisibility(View.GONE);
            mCandidateContainerTip.setVisibility(View.GONE);
        }
    }

    /**
     * mCandidateWidgetList = totalData - mNotificationList
     */
    private void saveSettings() {
        ShortcutAppManager.saveShareData(mContext,mNotificationList, ShortcutAppManager.DATA_TYPE.SELECTED_DATA.name());
    }

    private void saveCandidateData() {
        ShortcutAppManager.saveShareData(mContext,mCandidateWidgetList, ShortcutAppManager.DATA_TYPE.CANDIDATE.name());
    }

    private void onWidgetOrderChanged() {
        saveSettings();
        if (mWidgetOrderChangedListener != null) {
            mWidgetOrderChangedListener.onWidgetOrderChanged();
        }
    }


    boolean mIsDragCandidate = false;
    private int mLastX, mLastY, mStartX, mStartY;

    private int mCandidatePos = INVALID_POSITION;
    private int mGridViewItemPos = INVALID_POSITION;
    private int mGridViewEndItemPos = INVALID_POSITION;
    private int mDraggingCandidatePos = INVALID_POSITION;

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        if (isAnimPlaying) {
            return false;
        }

        int eventAction = event.getAction();
        switch (eventAction) {
            case MotionEvent.ACTION_DOWN:
                if (mIsDragCandidate) {
                    break;
                }
                mStartX = mLastX = (int) event.getX();
                mStartY = mLastY = (int) event.getY();

                int draggingCandidatePos = getPositionOfCandidateIcon(mStartX, mStartY);
                mCandidatePos = draggingCandidatePos;
                mDraggingCandidatePos = draggingCandidatePos;

                if (mCandidatePos != INVALID_POSITION && mCandidatePos < mCandidateWidgetList.size() &&
                        !TextUtils.isEmpty(mCandidateWidgetList.get(mCandidatePos).getDispalyName())) {
                    mLongPressedThread = new LongPressedThread(mCandidatePos);
                    mHandler.postDelayed(mLongPressedThread, LONG_PRESS_TRIGGER_TIME);
                }

                break;

            case MotionEvent.ACTION_MOVE:
                int tempX = (int) event.getX();
                int tempY = (int) event.getY();
                mLastX = tempX;
                mLastY = tempY;
                if (!mIsDragCandidate) {
                    break;
                }

                drawDragViews(tempX, tempY);
                overlayGridViewItemIfNeed(tempX, tempY);

                if (allowCandidateExchangeSelfInside()) {
                    overlayCandidateItemIfNeed(tempX, tempY);
                }

                return true;

            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
                mLastX = (int) event.getX();
                mLastY = (int) event.getY();
                mLastY = Math.max(mLastY, - mDy);
                mHandler.removeCallbacksAndMessages(null);
                if (!mIsDragCandidate) {
                    break;
                }
                mAppNameView.setVisibility(View.INVISIBLE);

                int startX = mLastX + mDx;
                int startY = mLastY + mDy;

                if (mDraggingCandidatePos != INVALID_POSITION) {
                    if (mGridViewItemPos != INVALID_POSITION) {
                        //exchange widgets from candidate to grid.
                        Rect targetRect = getRectInParent(getDragGridViewItemIcon(mGridViewItemPos));
                        int endX = targetRect.centerX();
                        int endY = targetRect.centerY();
                        moveDragViewAnim(startX, startY, endX, endY, null);
                        moveWidgetToCandidateAnim(mGridViewItemPos, mDraggingCandidatePos);
                    } else {
                        if (mCandidatePos != INVALID_POSITION && mDraggingCandidatePos != mCandidatePos) {
                            // exchange widgets inside candidate container.
                            Rect endRect = getRectInParent(getCandidateViewIcon(mCandidatePos));
                            int endX = endRect.centerX();
                            int endY = endRect.centerY();
                            moveDragViewAnim(startX, startY, endX, endY, null);
                            animExchangeCandidateWidgetInside();
                        } else {
                            // not exchange, move back.
                            Rect endRect = getRectInParent(getCandidateViewIcon(mDraggingCandidatePos));
                            int endX = endRect.centerX();
                            int endY = endRect.centerY();
                            moveDragViewAnim(startX, startY, endX, endY, getMoveDragViewBackAnimListener());
                        }
                    }
                }
                return true;
        }

        return super.dispatchTouchEvent(event);
    }

    private void handleAppNameVisibility() {
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (mIsDragCandidate || mIsDragGridViewItem) {
                    if (Math.abs(mLastX - mStartX) > 45 || Math.abs(mLastY - mStartY) > 45) {
                        return;
                    }
                    mAppNameView.setVisibility(View.VISIBLE);

                }

            }
        }, DELAY_SHOW_APP_NAME);
    }

    private void initAppNameTextView() {
        mAppNameView = new TextView(this.getContext());
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams
                .WRAP_CONTENT);
        params.topMargin = -DRAG_VIEW_LEFT_MARGIN;
        mAppNameView.setLayoutParams(params);
        mAppNameView.setGravity(Gravity.CENTER);
        mAppNameView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12.6f);
        mAppNameView.setTextColor(Color.parseColor("#B3000000"));
        mAppNameView.setBackgroundDrawable(this.getResources().getDrawable(R.drawable.popup_text_light));
        mAppNameView.setSingleLine();
        mAppNameView.setVisibility(View.INVISIBLE);
        this.addView(mAppNameView);

    }

    private void swapWidgetAndCandidate(int positionInGridView, int positionInCandidate) {
        ShortcutApp candidateWidget = getCandidateWidgetByChildViewPosition(positionInCandidate);
        ShortcutApp targetWidget = mNotificationList.get(positionInGridView);
        LogTag.d(TAG, "swapWidgetAndCandidate: candidateWidget = "
                + candidateWidget.getDispalyName() + " , targetWidget= " + targetWidget.getDispalyName());
        if (TextUtils.isEmpty(candidateWidget.getDispalyName())) {
            return;
        }

        //update GridView data
        mNotificationList.set(positionInGridView, candidateWidget);
        onWidgetOrderChanged();
        loadGridViewData(true);
        mDragAdapter.notifyDataSetChanged();

        //update CandidateWidget
        ShortcutApp oldCandidateWidget = mCandidateWidgetList.set(positionInCandidate, targetWidget);
        if (TextUtils.isEmpty(targetWidget.getDispalyName())) {
            mCandidateWidgetList.remove(candidateWidget);
        }
        saveCandidateData();
        loadCandidateWidgetData(true, positionInCandidate, oldCandidateWidget, targetWidget);
        int mCandidateWidgetListSize = mCandidateWidgetList.size();
        if (mCandidateWidgetListSize == 0 && mCandidateContainer != null) {
            mCandidateContainer.setVisibility(View.GONE);
            mCandidateContainerTip.setVisibility(View.GONE);
        }
    }

    private void swapCandidateInsideWidgets() {
        ShortcutApp dragged = getCandidateWidgetByChildViewPosition(mDraggingCandidatePos);
        ShortcutApp target = getCandidateWidgetByChildViewPosition(mCandidatePos);
        LogTag.d(TAG, "swapCandidateInsideWidgets: dragged = "
                + dragged.getDispalyName() + " , targetWidget= " + target.getDispalyName());

        if (TextUtils.isEmpty(target.getDispalyName())) {
            return;
        }

        mCandidateWidgetList.set(mDraggingCandidatePos, target);
        onCandidateWidgetChanged(mDraggingCandidatePos, dragged, target);

        mCandidateWidgetList.set(mCandidatePos, dragged);
        onCandidateWidgetChanged(mCandidatePos, target, dragged);

        saveCandidateData();
        onWidgetOrderChanged();
    }

    private void onCandidateWidgetChanged(int changedChildPosition, ShortcutApp oldWidget, ShortcutApp newWidget) {
        mLayoutStrategy.onCandidateWidgetChanged(mCandidateContainer, changedChildPosition, oldWidget, newWidget);
    }

    private void swapWidgetInGridView() {
        ShortcutApp beginWidget = mNotificationList.get(mGridViewItemPos);
        ShortcutApp endWidget = mNotificationList.get(mGridViewEndItemPos);
        LogTag.d(TAG, "swapWidgetInGridView: beginWidget = " + beginWidget.getDispalyName() + " , endWidget = " + endWidget.getDispalyName());
        if (TextUtils.isEmpty(endWidget.getDispalyName())) {
            return;
        }
        mNotificationList.set(mGridViewItemPos, endWidget);
        mNotificationList.set(mGridViewEndItemPos, beginWidget);
        onWidgetOrderChanged();
    }

    private void moveDragViewAnim(int startX, int startY, int endX, int endY, Animation.AnimationListener
            listener) {

        TranslateAnimation mMoveAction = new TranslateAnimation(
                Animation.RELATIVE_TO_SELF, 0.0f, Animation.ABSOLUTE,
                (float) (endX - startX),
                Animation.RELATIVE_TO_SELF, 0.0f, Animation.ABSOLUTE,
                (float) (endY - startY));

        mMoveAction.setDuration(ANIM_DURATION);
        mMoveAction.setInterpolator(new DecelerateInterpolator(1.5f));

        ScaleAnimation mScaleMove = new ScaleAnimation(1.0f, 1 / SCALE_RATE, 1.0f, 1 / SCALE_RATE,
                Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
        mScaleMove.setInterpolator(new DecelerateInterpolator(1.5f));
        mScaleMove.setDuration(ANIM_DURATION);

        AlphaAnimation alphaAnimation = new AlphaAnimation(1.0f, 0.6f);
        alphaAnimation.setInterpolator(new DecelerateInterpolator(1.5f));
        alphaAnimation.setDuration(ANIM_DURATION);

        AnimationSet mAnimationSet = new AnimationSet(false);
        mAnimationSet.addAnimation(mScaleMove);
        mAnimationSet.addAnimation(mMoveAction);
        mAnimationSet.addAnimation(alphaAnimation);
        mAnimationSet.setAnimationListener(listener);
        mDragView.startAnimation(mAnimationSet);
    }

    private void moveWidgetToCandidateAnim(final int positionInGridView, final int positionInCandidate) {
        ImageView selectedWidget = getDragGridViewItemIcon(positionInGridView);
        Rect startRect = getRectInParent(selectedWidget);

        int startX = startRect.left;
        int startY = startRect.top;

        ImageView candidateIcon = getCandidateViewIcon(positionInCandidate);
        Rect endRect = getRectInParent(candidateIcon);

        int endX = endRect.left;
        int endY = endRect.top + candidateIcon.getPaddingTop();

        TranslateAnimation mMoveAction = new TranslateAnimation(
                Animation.ABSOLUTE, startX + GRID_VIEW_ITEM_PADDING,
                Animation.ABSOLUTE, (float) (endX),
                Animation.ABSOLUTE, startY + GRID_VIEW_ITEM_PADDING - DISTANCE_MOVE_BOTTOM,
                Animation.ABSOLUTE, (float) (endY));

        mMoveAction.setDuration(ANIM_DURATION);
        mMoveAction.setInterpolator(new DecelerateInterpolator(1.5f));
        mMoveAction.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
                isAnimPlaying = true;
                mDragAdapter.setHideItem(positionInGridView);
                mMoveView.setImageDrawable(mNotificationList.get(positionInGridView).getDrawable());
                mMoveView.setVisibility(View.VISIBLE);
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                mMoveView.clearAnimation();
                mMoveView.setVisibility(View.INVISIBLE);
                swapWidgetAndCandidate(positionInGridView, positionInCandidate);
                reset();
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
        mMoveView.startAnimation(mMoveAction);
    }

    private void animExchangeCandidateWidgetInside() {
        Rect startRect = getRectInParent(getCandidateViewIcon(mCandidatePos));
        int startX = startRect.left;
        int startY = startRect.top;

        ImageView candidateIcon = getCandidateViewIcon(mDraggingCandidatePos);
        Rect endRect = getRectInParent(candidateIcon);

        int endX = endRect.left;
        int endY = endRect.top + candidateIcon.getPaddingTop();

        TranslateAnimation moveAnim = new TranslateAnimation(
                Animation.ABSOLUTE, startX + GRID_VIEW_ITEM_PADDING,
                Animation.ABSOLUTE, (float) (endX),
                Animation.ABSOLUTE, startY + GRID_VIEW_ITEM_PADDING - DISTANCE_MOVE_BOTTOM,
                Animation.ABSOLUTE, (float) (endY));

        moveAnim.setDuration(ANIM_DURATION);
        moveAnim.setInterpolator(new DecelerateInterpolator(1.5f));
        moveAnim.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
                isAnimPlaying = true;
                mMoveView.setImageDrawable(getCandidateWidgetByChildViewPosition(mCandidatePos).getDrawable());
                mMoveView.setVisibility(View.VISIBLE);
                mLayoutStrategy.updateCandidateChildViewVisibility(mCandidateContainer.getChildAt(mCandidatePos), INVISIBLE);
                mLayoutStrategy.updateCandidateChildViewVisibility(mCandidateContainer.getChildAt(mDraggingCandidatePos), INVISIBLE);
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                mMoveView.clearAnimation();
                mMoveView.setVisibility(View.INVISIBLE);
                swapCandidateInsideWidgets();
                reset();
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }
        });
        mMoveView.startAnimation(moveAnim);
    }

    private ImageView getCandidateViewIcon(int candidatePosition) {
        return mLayoutStrategy.getCandidateViewIcon(mCandidateContainer.getChildAt(candidatePosition));
    }

    private void moveCandidateToWidgetAnim(final int positionInGridView, final int candidatePosition) {
        final ImageView candidateIcon = getCandidateViewIcon(this.mCandidatePos);
        Rect startRect = getRectInParent(candidateIcon);

        int startX = startRect.left;
        int startY = startRect.top + candidateIcon.getPaddingTop();

        ImageView widgetView = getDragGridViewItemIcon(mGridViewItemPos);
        Rect endRect = getRectInParent(widgetView);

        int endX = endRect.left;
        int endY = endRect.top;

        TranslateAnimation mMoveAction = new TranslateAnimation(
                Animation.ABSOLUTE, startX,
                Animation.ABSOLUTE, (float) (endX + GRID_VIEW_ITEM_PADDING),
                Animation.ABSOLUTE, startY - DISTANCE_MOVE_BOTTOM,
                Animation.ABSOLUTE, (float) (endY + GRID_VIEW_ITEM_PADDING));

        mMoveAction.setDuration(ANIM_DURATION);
        mMoveAction.setInterpolator(new DecelerateInterpolator(1.5f));
        mMoveAction.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
                isAnimPlaying = true;
                mMoveView.setImageDrawable(mCandidateWidgetList.get(candidatePosition).getDrawable());
                mMoveView.setVisibility(View.VISIBLE);
                candidateIcon.clearAnimation();
                mLayoutStrategy.updateCandidateChildViewVisibility(mCandidateContainer.getChildAt(candidatePosition), INVISIBLE);
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                mMoveView.clearAnimation();
                mMoveView.setVisibility(View.INVISIBLE);
                swapWidgetAndCandidate(positionInGridView, candidatePosition);
                reset();
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }
        });
        mMoveView.startAnimation(mMoveAction);
    }

    private void moveWidgetAnim() {
        mMoveView.setImageDrawable(mNotificationList.get(mGridViewEndItemPos).getDrawable());
        mMoveView.setVisibility(View.VISIBLE);

        ImageView selectedWidget = getDragGridViewItemIcon(mGridViewEndItemPos);
        Rect startRect = getRectInParent(selectedWidget);

        int startX = startRect.left;
        int startY = startRect.top;

        ImageView widgetView = getDragGridViewItemIcon(mGridViewItemPos);
        Rect endRect = getRectInParent(widgetView);

        int endX = endRect.left;
        int endY = endRect.top;

        TranslateAnimation mMoveAction = new TranslateAnimation(
                Animation.ABSOLUTE, startX + GRID_VIEW_ITEM_PADDING,
                Animation.ABSOLUTE, (float) (endX + GRID_VIEW_ITEM_PADDING),
                Animation.ABSOLUTE, startY + GRID_VIEW_ITEM_PADDING - DISTANCE_MOVE_BOTTOM,
                Animation.ABSOLUTE, (float) (endY + GRID_VIEW_ITEM_PADDING));

        mMoveAction.setDuration(ANIM_DURATION);
        mMoveAction.setInterpolator(new DecelerateInterpolator(1.5f));
        mMoveAction.setAnimationListener(getMoveViewAnimListener());
        mMoveView.startAnimation(mMoveAction);
    }

    private Animation.AnimationListener getMoveViewAnimListener() {
        return new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                mMoveView.clearAnimation();
                mMoveView.setVisibility(View.INVISIBLE);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        };
    }

    private Animation.AnimationListener getMoveDragViewBackAnimListener() {
        return new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
                isAnimPlaying = true;

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                reset();
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        };
    }

    private void reset() {
        if (mCandidatePos != INVALID_POSITION) {
            mLayoutStrategy.updateCandidateChildViewVisibility(mCandidateContainer.getChildAt(mCandidatePos), VISIBLE);
        }
        if (mDraggingCandidatePos != INVALID_POSITION) {
            mLayoutStrategy.updateCandidateChildViewVisibility(mCandidateContainer.getChildAt(mDraggingCandidatePos), VISIBLE);
        }
        isAnimPlaying = false;
        mCandidatePos = INVALID_POSITION;
        mGridViewItemPos = INVALID_POSITION;
        mGridViewEndItemPos = INVALID_POSITION;
        mDraggingCandidatePos = INVALID_POSITION;
        mTargetView = null;
        mDragAdapter.setHideItem(INVALID_POSITION);

        mDragView.clearAnimation();
        mDragView.setVisibility(View.INVISIBLE);
        mIsDragCandidate = false;
    }

    private void startMoveUpAnim(View image) {
        TranslateAnimation translationAnim = new TranslateAnimation(Animation.RELATIVE_TO_SELF, 0.0f,
                Animation.RELATIVE_TO_SELF, 0.0f, Animation.RELATIVE_TO_SELF, 0.0f,
                Animation.ABSOLUTE, -DISTANCE_MOVE_BOTTOM);
        translationAnim.setDuration(200);
        translationAnim.setFillAfter(true);
        translationAnim.setInterpolator(new DecelerateInterpolator(1.5f));
        image.startAnimation(translationAnim);
    }

    private void endMoveUpAnim(ImageView image) {
        TranslateAnimation translationAnim = new TranslateAnimation(Animation.RELATIVE_TO_SELF, 0.0f,
                Animation.RELATIVE_TO_SELF, 0.0f, Animation.ABSOLUTE, -DISTANCE_MOVE_BOTTOM,
                Animation.RELATIVE_TO_SELF, 0.0f
        );
        translationAnim.setDuration(200);
        translationAnim.setInterpolator(new DecelerateInterpolator(1.5f));
        image.startAnimation(translationAnim);
    }

    private void overlayGridViewItemIfNeed(int x, int y) {
        x += mDx;
        y += mDy;
        int gridViewItemPos = getPositionOfGridViewItem(x, y);
        boolean isChange = mGridViewItemPos != gridViewItemPos;
        if (isChange && mGridViewItemPos != INVALID_POSITION) {
            ImageView image = getDragGridViewItemIcon(mGridViewItemPos);
            endMoveUpAnim(image);
        }
        mGridViewItemPos = gridViewItemPos;

        if (mGridViewItemPos != INVALID_POSITION && isChange) {
            ImageView image = getDragGridViewItemIcon(mGridViewItemPos);
            startMoveUpAnim(image);
        }

    }

    private ImageView getDragGridViewItemIcon(int itemPosition) {
        return mLayoutStrategy.getDragGridViewItemIcon(mDragGridView.getChildAt(itemPosition));
    }

    private void overlayWidget(int x, int y) {
        x += mDx;
        y += mDy;
        int gridViewItemPos = getPositionOfGridViewItem(x, y);
        boolean isChange = mGridViewEndItemPos != gridViewItemPos;
        if (isChange && mGridViewEndItemPos != INVALID_POSITION) {
            ImageView image = getDragGridViewItemIcon(mGridViewEndItemPos);
            if(image.getVisibility() == View.VISIBLE) {
                endMoveUpAnim(image);
            }
        }
        mGridViewEndItemPos = gridViewItemPos;

        if (mGridViewEndItemPos != INVALID_POSITION && isChange) {
            ImageView image = getDragGridViewItemIcon(mGridViewEndItemPos);
            if(image.getVisibility() == View.VISIBLE) {
                startMoveUpAnim(image);
            }
        }

    }

    private int getPositionOfCandidateIcon(int x, int y) {
        int pos = INVALID_POSITION;
        for (int i = 0; i < mCandidateContainer.getChildCount(); i++) {
            View childView = mCandidateContainer.getChildAt(i);
            Rect viewRect = getRectInParent(childView);
            if (viewRect.contains(x, y)) {
                pos = i;
                break;
            }
        }
        return pos;
    }

    private int getPositionOfGridViewItem(int x, int y) {
        for (int i = 0; i < mDragGridView.getCount(); i++) {
            View childView = mDragGridView.getChildAt(i);
            Rect rect = getRectInParent(childView);
            if (rect.contains(x, y)) {
                return i;
            }
        }

        return INVALID_POSITION;
    }

    int mDx, mDy; //diff of the touch point relative to targetView center

    private void calculateDxDy(int touchX, int touchY) {
        Rect viewRect = getRectInParent(mTargetView);
        mDx = viewRect.centerX() - touchX;
        mDy = viewRect.centerY() - touchY;
    }

    int mLastmovell, mLastmoverr, mLastmovett, mLastmovebb;

    private void drawDragViews(int x, int y) {
        x += mDx;
        y += mDy;

        int halfScale = (int) (ICON_SIZE / 2f + DRAG_VIEW_ITEM_PADDING);
        mLastmovell = x - halfScale;
        mLastmoverr = x + halfScale;
        if (y > 0) {
            mLastmovett = y - halfScale;
            mLastmovebb = y + halfScale;
        } else {
            mLastmovett = -halfScale;
            mLastmovebb = halfScale;
        }

        mDragView.setVisibility(View.VISIBLE);

        if (Math.abs(mLastY - mStartY) > 45 || Math.abs(mLastX - mStartX) > 45) {
            mAppNameView.setVisibility(View.INVISIBLE);
        } else {
            int width = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
            int height = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
            mAppNameView.measure(width, height);
            int appNameWidth = mAppNameView.getMeasuredWidth();
            int appNameHeight = mAppNameView.getMeasuredHeight();
            int diffHeight = (int) (ICON_SIZE * (1.0 + SCALE_RATE) / 2.0f);
            mAppNameLeft = x - appNameWidth / 2;
            mAppNameTop = y - appNameHeight / 2 - diffHeight;
            mAppNameRight = x + appNameWidth / 2;
            mAppNameBottom = y + appNameHeight / 2 - diffHeight;
        }
        this.requestLayout();
    }

    int mAppNameLeft, mAppNameTop, mAppNameRight, mAppNameBottom;

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        mDragView.layout(mLastmovell, mLastmovett, mLastmoverr, mLastmovebb);
        mAppNameView.layout(mAppNameLeft, mAppNameTop,
                mAppNameRight, mAppNameBottom);
    }

    private void createDragView(ShortcutApp widget) {
        Resources resources = mContext.getResources();
        Drawable[] layers = new Drawable[2];
        //add two layers for this imageView
        layers[0] = widget.getDrawable();
        layers[1] = widget.getDrawable();
        LayerDrawable layerDrawable = new LayerDrawable(layers);
        mDragView.setImageDrawable(layerDrawable);

        mDragView.setScaleX(SCALE_RATE);
        mDragView.setScaleY(SCALE_RATE);
        mDragView.setPressed(true);
        mDragView.setVisibility(View.INVISIBLE);

        mAppNameView.setText(widget.getDispalyName());
        calculateDxDy(mLastX, mLastY);
        drawDragViews(mLastX, mLastY);
    }

    private void overlayCandidateItemIfNeed(int x, int y) {
        x += mDx;
        y += mDy;
        int pos = getPositionOfCandidateIcon(x, y);
        boolean isChange = mCandidatePos != pos;
        ImageView animView;
        if (isChange && mCandidatePos != INVALID_POSITION) {
            if (!allowCandidateExchangeSelfInside() || mCandidatePos != mDraggingCandidatePos) {
                animView = getCandidateViewIcon(mCandidatePos);
                endMoveUpAnim(animView);
            }
        }
        mCandidatePos = pos;
        if (mCandidatePos != INVALID_POSITION && isChange) {
            if (!allowCandidateExchangeSelfInside() || mCandidatePos != mDraggingCandidatePos) {
                animView = getCandidateViewIcon(mCandidatePos);
                startMoveUpAnim(animView);
            }
        }
    }

    private boolean allowCandidateExchangeSelfInside() {
        return mLayoutStrategy.allowCandidateExchangeSelfWidget(mCandidateContainer);
    }

    private Rect getRectInParent(View view) {
        int[] rootLocation = new int[2];
        getLocationInWindow(rootLocation);
        int[] viewLocation = new int[2];
        view.getLocationInWindow(viewLocation);
        Rect rectInWindow = new Rect();
        rectInWindow.left = viewLocation[0] - rootLocation[0];
        rectInWindow.top = viewLocation[1] - rootLocation[1];
        rectInWindow.right = rectInWindow.left + view.getWidth();
        rectInWindow.bottom = rectInWindow.top + view.getHeight();
        return rectInWindow;
    }

    @Override
    public void onDragStart(int position, int rawX, int rawY) {
        ShortcutApp widget = mNotificationList.get(position);
        if (TextUtils.isEmpty(widget.getDispalyName())) {
            return;
        }
        mGridViewItemPos = position;
        mDragAdapter.setHideItem(mGridViewItemPos);
        mTargetView = getDragGridViewItemIcon(position);
        calculateDxDy(rawX, rawY);
        createDragView(widget);
        handleAppNameVisibility();
        mIsDragGridViewItem = true;
    }

    @Override
    public void onDraging(MotionEvent event) {
        int x = (int) event.getX();
        int y = (int) event.getY();
        //turn to relative x, y in this view
        Rect dragGridViewRect = getRectInParent(mDragGridView);
        x += dragGridViewRect.left;
        y += dragGridViewRect.top;

        drawDragViews(x, y);
        overlayCandidateItemIfNeed(x, y);
        overlayWidget(x, y);
    }

    @Override
    public void onDragEnd(int hidePosition) {
        isAnimPlaying = true;

        final int targetPos = hidePosition == INVALID_POSITION ? mGridViewItemPos : hidePosition;
        if (targetPos != INVALID_POSITION) {

            int startX = mLastX + mDx;
            int startY = mLastY + mDy;

            int endX, endY;
            Rect endRect;
            Animation.AnimationListener dragViewAnimListener;
            if (mCandidatePos != INVALID_POSITION/* && mCandidatePos != (mCandidateWidgetList.size() - 1)*/) {
                endRect = getRectInParent(getCandidateViewIcon(mCandidatePos));
                dragViewAnimListener = null;
                moveCandidateToWidgetAnim(targetPos, mCandidatePos);
            } else if (mGridViewEndItemPos != INVALID_POSITION && mGridViewEndItemPos != targetPos/* && mGridViewEndItemPos != (mNotificationList.size() - 1)*/) {
                mDragAdapter.setHideItem(mGridViewItemPos);
                endRect = getRectInParent(getDragGridViewItemIcon(mGridViewEndItemPos));
                dragViewAnimListener = getRestoreDragGridViewItemAnim();
                moveWidgetAnim();
                swapWidgetInGridView();
            } else {
                endRect = getRectInParent(getDragGridViewItemIcon(targetPos));
                dragViewAnimListener = getRestoreDragGridViewItemAnim();
            }
            endX = endRect.centerX();
            endY = endRect.centerY();

            moveDragViewAnim(startX, startY, endX, endY, dragViewAnimListener);
        }
        mAppNameView.setVisibility(View.INVISIBLE);
        mGridViewItemPos = INVALID_POSITION;
        mIsDragGridViewItem = false;
    }

    private Animation.AnimationListener getRestoreDragGridViewItemAnim() {
        Animation.AnimationListener restoreDragViewListener = new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
                isAnimPlaying = true;
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                reset();
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        };
        return restoreDragViewListener;
    }

    private LongPressedThread mLongPressedThread;

    public class LongPressedThread implements Runnable {
        int pos;

        public LongPressedThread(int pos) {
            this.pos = pos;
        }

        @Override
        public void run() {
            if (Math.abs(mLastX - mStartX) > 45 || Math.abs(mLastY - mStartY) > 45) {
                return;
            }
            mIsDragCandidate = true;

            mTargetView = getCandidateViewIcon(pos);
            createDragView(mCandidateWidgetList.get(pos));
            handleAppNameVisibility();
            View selectedView = mCandidateContainer.getChildAt(pos);
            mLayoutStrategy.updateCandidateChildViewVisibility(selectedView, View.INVISIBLE);
        }
    }

    public class DragAdapter extends BaseAdapter implements DragGridView.DragGridBaseAdapter {
        private List<ShortcutApp> list;
        private int mHidePosition = -1;

        public DragAdapter(List<ShortcutApp> list) {
            this.list = list;
        }

        @Override
        public int getCount() {
            return list.size();
        }

        @Override
        public Object getItem(int position) {
            return list.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            convertView = mLayoutStrategy.onGetDragGridItemView(list.get(position), position, convertView, parent);
            if (convertView != null) {
                boolean invisible = position == mGridViewEndItemPos || position == mHidePosition;
                mLayoutStrategy.updateGridItemViewVisibility(convertView, invisible ? View.INVISIBLE : View.VISIBLE);
            }
            return convertView;
        }


        @Override
        public void reorderItems(int oldPosition, int newPosition) {
            ShortcutApp temp = list.get(oldPosition);
            if (oldPosition < newPosition) {
                for (int i = oldPosition; i < newPosition; i++) {
                    Collections.swap(list, i, i + 1);
                }
            } else if (oldPosition > newPosition) {
                for (int i = oldPosition; i > newPosition; i--) {
                    Collections.swap(list, i, i - 1);
                }
            }

            list.set(newPosition, temp);
        }

        @Override
        public void setHideItem(int hidePosition) {
            this.mHidePosition = hidePosition;
            notifyDataSetChanged();
        }

        @Override
        public int getHidePosition() {
            return mHidePosition;
        }
    }

}
