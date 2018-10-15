package com.smartisanos.ideapills.view;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.annotation.NonNull;
import android.app.SmtPCUtils;
import android.content.ContentResolver;
import android.content.Context;
import android.os.Bundle;
import android.os.Looper;
import android.os.Parcel;
import android.provider.Settings;
import android.service.onestep.GlobalBubble;
import android.text.TextUtils;
import android.view.DragEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
import android.widget.ListView;
import android.widget.Toast;

import com.smartisanos.ideapills.Constants;
import com.smartisanos.ideapills.IdeaPillsApp;
import com.smartisanos.ideapills.R;
import com.smartisanos.ideapills.common.anim.Anim;
import com.smartisanos.ideapills.common.anim.AnimCancelableListener;
import com.smartisanos.ideapills.common.anim.AnimListener;
import com.smartisanos.ideapills.common.anim.SimpleAnimListener;
import com.smartisanos.ideapills.entity.BubbleItem;
import com.smartisanos.ideapills.interfaces.LocalInterface;
import com.smartisanos.ideapills.util.AttachmentUtils;
import com.smartisanos.ideapills.util.GlobalBubbleUtils;
import com.smartisanos.ideapills.util.InsertSortArray;
import com.smartisanos.ideapills.util.StatusManager;
import com.smartisanos.ideapills.common.anim.AnimTimeLine;
import com.smartisanos.ideapills.common.anim.Vector3f;
import com.smartisanos.ideapills.BubbleController;
import com.smartisanos.ideapills.util.GlobalBubbleManager;
import com.smartisanos.ideapills.util.LOG;
import com.smartisanos.ideapills.util.Utils;
import com.smartisanos.ideapills.common.util.UIHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import smartisanos.api.SettingsSmt;
import smartisanos.api.VibEffectSmt;
import smartisanos.support.view.ViewCompat;
import smartisanos.util.SidebarUtils;

public class BubbleAdapter extends BaseAdapter implements View.OnLongClickListener {
    private static final LOG log = LOG.getInstance(BubbleAdapter.class);
    private static final int STATE_NONE = 0;
    private static final int STATE_MOVE = 1;
    private static final int STATE_LONG_CLICK = 2;

    public static final int MOVE_THRESHOLD = 20;
    public static final float DAMPING = 1.0f;

    public interface AdapterCallback {
        int MSG_HIDE_LIST = 0;
        int MSG_SET_DRAWORDER = 1;
        int MSG_SET_DRAWORDER_OFF = 2;
        void execute(int msg);
    }

    public interface DeleteAllListener{
        void deleteListener();
    }

    private Context mContext;
    private GlobalBubbleManager mManager;
    private BubbleListView mListView;
    private String mFilterStr = null;
    private List<BubbleItem> mList;
    private List<BubbleItem> mFakeList;
    private List<BubbleItem> mSelectedList;
    private LayoutInflater mInflater;
    private int mMode = ViewMode.BUBBLE_NORMAL;

    private int mCurState;
    private float mDownRawX;
    private float mDownX;
    private float mDownY;
    private int mEmptyNeedInputHeight = 0;

    private AdapterCallback mAdapterCallback = null;
    private boolean mHandleBubbleBySelf = false;
    private AnimTimeLine mDragTimeLine = null;
    private AnimatorSet mCloseAnimatorSet = null;
    private InsertSortArray mInsertSortArray = new InsertSortArray();
    private ContentResolver mContentResolver;
    private boolean mHideTodoOverBubble;
    private int mFilterBubbleColor = FiltrateSetting.FILTRATE_ALL;

    private int mTouchSlop = 0;
    private BubbleItemView.ToLargeCallBack mToLargeCallBack = new BubbleItemView.ToLargeCallBack() {
        public void toLarge(int top, int height) {
            final int up = top + height - mListView.getHeight();
            if (up > 0) {
                mListView.scrollListUp(up);
            }
        }
    };

    public BubbleAdapter(Context context, BubbleListView listView) {
        mContext = context;
        mListView = listView;
        mInflater = LayoutInflater.from(mContext);
        mManager = GlobalBubbleManager.getInstance();
        mContentResolver = mContext.getContentResolver();
        mHideTodoOverBubble = Settings.Global.getInt(mContentResolver, SettingsSmt.Global.HIDE_TODO_OVER_BUBBLE, 0) == 1;
        mFilterBubbleColor = BubbleController.getInstance().isInPptContext(mListView.getContext()) ? GlobalBubble.COLOR_NAVY_BLUE : FiltrateSetting.getFiltrateColor(context);
        mList = mManager.getBubbles(mFilterBubbleColor, mHideTodoOverBubble);
        if (BubbleController.getInstance().isInPptContext(mListView.getContext())) {
            for (BubbleItem bubbleItem : mList) {
                bubbleItem.setInLargeMode(true);
            }
        }
        mSelectedList = new ArrayList<BubbleItem>();

        mTouchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
        if (BubbleController.getInstance().isCurControllerContext(context)) {
            observeBubbleItemChanged();
        }
    }

    public void observeBubbleItemChanged() {
        if (mManager.registerListener(mBubbleListener)) {
            onUpdateInner();
        }
    }

    public void unObserveBubbleItemChanged() {
        mManager.unregisterListener(mBubbleListener);
    }

    public int getTouchSlop() {
        return mTouchSlop;
    }

    public void refreshData() {
        mList = mManager.getBubbles(mFilterBubbleColor, mFilterStr, mHideTodoOverBubble);
        notifyDataSetChanged();
    }

    public void registerCallback(AdapterCallback callback) {
        mAdapterCallback = callback;
    }

    @Override
    public int getCount() {
        return mFakeList == null ? mList.size() : mFakeList.size();
    }

    @Override
    public BubbleItem getItem(int position) {
        return mFakeList == null ? mList.get(position) : mFakeList.get(position);
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }

    @Override
    public long getItemId(int position) {
        BubbleItem item = (BubbleItem) getItem(position);
        if (item != null && item.getTimeStamp() == 0) {
            item.setTimeStamp(System.currentTimeMillis());
        }
        return (item == null ? -1 : item.getWeight() + item.getTimeStamp());
    }

    @Override
    public boolean isEnabled(int position) {
        return false;
    }

    public BubbleItem getItem(View view) {
        if (view != null && view instanceof BubbleItemView) {
            BubbleItemView biv = (BubbleItemView) view;
            return biv.getBubbleItem();
        }
        return null;
    }

    public BubbleItemView inflateBubbleView(BubbleItem item) {
        BubbleItemView biv = (BubbleItemView) mInflater.inflate(R.layout.bubble_item, null);
        biv.setMode(getMode());
        biv.show(item);
        int listWidth = mListView.getWidth() - mListView.getPaddingLeft()
                - mListView.getPaddingRight();
        int widthSpec = View.MeasureSpec.makeMeasureSpec(listWidth, View.MeasureSpec.EXACTLY);
        int heightSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
        biv.measure(widthSpec, heightSpec);
        return biv;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder viewHolder;
        if (convertView == null || ((BubbleItemView) convertView).isAbort()) {
            viewHolder = new ViewHolder();
            convertView = mInflater.inflate(R.layout.bubble_item, null);
            viewHolder.containerView = convertView.findViewById(R.id.fl_layout);
            viewHolder.containerView.setOnTouchListener(mOnTouchListener);
            viewHolder.biv = ((BubbleItemView) convertView);
            viewHolder.biv.setToLargeCallBack(mToLargeCallBack);
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }

        BubbleItem item = (BubbleItem) getItem(position);
        if (viewHolder.containerView != null) {
            String content = mContext.getString(R.string.bubble_item_description);
            content = String.format(content, position + 1);
            content = content + " " + item.getText();
            viewHolder.containerView.setContentDescription(content);
        }
        viewHolder.biv.stopValueAnimator();
        viewHolder.biv.setMode(getMode());
        viewHolder.biv.show(item);
        viewHolder.biv.clearAnimTranslateY();
        viewHolder.biv.setTranslationY(0.0f);

        return convertView;
    }


    private class ViewHolder {
        View containerView;
        BubbleItemView biv;
    }

    private void getSelectedBubbleItems(List<BubbleItem> list) {
        list.clear();
        for (BubbleItem item : mList) {
            if (item.isSelected()) {
                list.add(item);
            }
        }
    }

    private void doFlyToDragging(final BubbleItemView bubbleView, final View view, final int bubbleCount, final String showText, final String dragText, final Bundle bundle, final int backgroundRes, final int colorRes) {
        mDragTimeLine = new AnimTimeLine();
        List<BubbleItemView> selectViews = new ArrayList<BubbleItemView>();
        int childCount = mListView.getChildCount();
        for (int i = 0; i < childCount; i++) {
            View child = mListView.getChildAt(i);
            if (child == null) {
                continue;
            }
            if (!(child instanceof BubbleItemView)) {
                continue;
            }
            final BubbleItemView itemView = (BubbleItemView) child;
            BubbleItem item = getItem(itemView);
            if (item == null) {
                continue;
            }
            if (!item.isSelected()) {
                continue;
            }
            selectViews.add(itemView);
            Vector3f from = new Vector3f();
            Vector3f to = new Vector3f();
            Vector3f visible = new Vector3f(1, 0, 1f);
            Vector3f invisible = new Vector3f(0, 0, 0.0f);
            int toX = (int) mDownX - view.getWidth() / 2;
            int toY = (int) (mDownY - view.getHeight() / 2);
            to.setX(toX);
            to.setY(toY);
            log.error("to ["+to+"]");
            int time = 200;
            Anim moveAnim = new Anim(itemView, Anim.TRANSLATE, time, Anim.QUAD_IN, from, to);
            Anim alphaAnim = new Anim(itemView, Anim.TRANSPARENT, time - 50, Anim.QUAD_IN, visible, invisible);
            float toScalX = 200.0f / itemView.queryWidth();
            float toScalY = mListView.getNormalHeight()*1.0f/itemView.getHeight();
            Anim scaleAnim = new Anim(itemView, Anim.SCALE, time, Anim.QUAD_IN, new Vector3f(1.0f, 1.0f), new Vector3f(toScalX, toScalY));
            moveAnim.setListener(new SimpleAnimListener() {
                @Override
                public void onStart() {
                    itemView.setPivotXY();
                }

                @Override
                public void onComplete(int type) {
                    itemView.resetPivotXY();
                    itemView.setVisibility(View.INVISIBLE);
                    itemView.setTranslationX(0);
                    itemView.setTranslationY(0);
                    itemView.setAlpha(1.0f);
                    itemView.setScaleX(1.0f);
                    itemView.setScaleY(1.0f);
                }
            });
            mDragTimeLine.addAnim(scaleAnim);
            mDragTimeLine.addAnim(moveAnim);
            mDragTimeLine.addAnim(alphaAnim);
        }
        if (mDragTimeLine.isEmpty()) {
            log.info("startDrag when time line is empty");
            dragMultiBubbles(bubbleView, showText, dragText, bubbleCount, bundle, backgroundRes, colorRes);
            return;
        }
        mAdapterCallback.execute(AdapterCallback.MSG_SET_DRAWORDER);
        mDragTimeLine.setAnimListener(new SimpleAnimListener() {
            private boolean cancel = false;

            @Override
            public void onStart() {

            }

            @Override
            public void onComplete(int type) {
                if (type == Anim.ANIM_FINISH_TYPE_CANCELED) {
                    cancel = true;
                }
                if (!cancel) {
                    log.info("startDragMulti");
                    dragMultiBubbles(bubbleView, showText, dragText, bubbleCount, bundle, backgroundRes, colorRes);
                }
                if (type == Anim.ANIM_FINISH_TYPE_COMPLETE) {
                    mAdapterCallback.execute(AdapterCallback.MSG_SET_DRAWORDER_OFF);
                }
            }
        });
        mDragTimeLine.start();
    }

    private void startDrag(final View view) {
        if (StatusManager.isBubbleDragging()) {
            log.error("startDrag return by mIsDragging true");
            return;
        }
        getSelectedBubbleItems(mSelectedList);
        final BubbleItemView bubbleView = (BubbleItemView) view.getParent();
        final BubbleItem bubbleItem = getItem(bubbleView);
        if (mSelectedList.size() == 0) {
            bubbleItem.setSelected(true);
            mSelectedList.add(bubbleItem);
        } else {
            if (mSelectedList.remove(bubbleItem)) {
                mSelectedList.add(0, bubbleItem);
            }
        }
        final int count = mSelectedList.size();
        if (mSelectedList.size() > 0) {
            final Bundle bundle = GlobalBubbleUtils.getBubbleBundle(mSelectedList);
            if (GlobalBubbleUtils.isBubbleListTooLarge(bundle)) {
                GlobalBubbleUtils.showSystemToast(mContext, mContext.getResources().getString(R.string.drag_max_limit_hint_text), Toast.LENGTH_SHORT);
                return;
            }
//            CommonUtils.vibrateEffect(mContext, VibEffectSmt.EFFECT_EDITING_MODE);
            log.error("startDrag xy [" + mDownX + ", " + mDownY + "]");
            StatusManager.setStatus(StatusManager.BUBBLE_DRAGGING, true);
            mHandleBubbleBySelf = false;
            String varShowText = "";
            StringBuilder builder = new StringBuilder();
            final List<BubbleItem> selectedItems = new ArrayList<BubbleItem>();
            for (BubbleItem item : mSelectedList) {
                item.setIsTemp(true);
                selectedItems.add(item);
                builder.append(item.getText()).append("\n\n");
                if (TextUtils.isEmpty(varShowText) && !TextUtils.isEmpty(item.getText())) {
                    varShowText = item.getText();
                }
            }
            if (builder.length() > 0) {
                builder.setLength(builder.length() - 2);
            }
            if (TextUtils.isEmpty(varShowText)) {
                varShowText = mSelectedList.get(0).getSingleText();
            }
            final String showText = varShowText;
            final String dragText;
            if (TextUtils.isEmpty(builder.toString()) && !mSelectedList.isEmpty()) {
                dragText = mSelectedList.get(0).getSingleText();
            } else {
                dragText = builder.toString();
            }
            final int backgroundRes = bubbleItem.getDragBackground(count);
            final int colorRes = bubbleItem.getDragTextColor();
            doFlyToDragging(bubbleView, view, count, showText, dragText, bundle, backgroundRes, colorRes);
        }
    }

    private void dragMultiBubbles(View view, String showText, String text, int bubbleCount, Bundle bundle, int backgroundRes, int colorRes) {
        if (showText == null) {
            showText = "";
        }
        String title = null;
        String message = null;
        if (bubbleCount > 1) {
            mContext.getResources().getString(R.string.bubble_notice);
            message = mContext.getResources().getString(R.string.bubble_delete_multiple);
        }

        boolean flag = SidebarUtils.dragText(view, mContext, showText, text, null, true, backgroundRes, bundle, title, message, colorRes);
        if (!flag) {
            if (StatusManager.isBubbleDragging()) {
                onSelfDragEnd(null, false);
            }
        }
    }

    public InsertSortArray getInsertSortArray() {
        if (mInsertSortArray.isEmpty()) {
            int size = mList.size();
            for (int i = 0; i < size; i++) {
                BubbleItem item = mList.get(i);
                if (item.isSelected()) {
                    mInsertSortArray.insertValue(i);
                }
            }
        }
        return mInsertSortArray;
    }

    public boolean onSelfDragEnd(final DragEvent event, boolean dropBack) {
        log.info("onSelfDragEnd drapBack="+dropBack);
        mListView.cancelDraggingAnim();
        StatusManager.setStatus(StatusManager.BUBBLE_DRAGGING, false);
        mInsertSortArray.clear();
        for (int i = 0; i < mSelectedList.size(); i++) {
            mSelectedList.get(i).setIsTemp(false);
            mSelectedList.get(i).setSelected(false);
        }
        if (mList != null) {
            for (BubbleItem bubbleItem : mList) {
                if (bubbleItem != null) {
                    bubbleItem.setIsTemp(false);
                }
            }
        }
        boolean delayHandled = false;
        if (event != null && event.getResult()) {
            if (mHandleBubbleBySelf) {
                notifyDataSetChanged();
            } else {
                if (!dropBack) {
                    final List<BubbleItem> delItems = new ArrayList<>(mSelectedList);
                    final boolean needTrash = (event.getFlag() & DragEvent.FLAG_SUCCESS_DUE_TO_TRASH
                            | event.getFlag() & ViewCompat.DRAG_FLAG_DROP_IN_DOCK_TRASH) != 0;

                    UIHandler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            int readMsgId = needTrash ? R.string.read_bubble_deleted : R.string.read_bubble_shared;
                            mListView.announceForAccessibilityImmediately(mContext.getString(readMsgId));
                        }
                    }, 500);

                    if (needTrash) {
                        // only drag to trash will del these items
                        if (delItems.size() > 1) {
                            dragDelete(delItems, true, true);
                        } else {
                            dragDelete(delItems, true, false);
                        }
                    } else {
                        if (BubbleController.getInstance().isBubbleHandleBySidebar() || SmtPCUtils.isValidExtDisplayId(mContext)) {
                            if (delItems.size() > 0) {
                                for (BubbleItem item : delItems) {
                                    if (item.haveAttachments()) {
                                        GlobalBubbleUtils.showSystemToast(mContext,
                                                mContext.getString(R.string.bubble_share_tip), Toast.LENGTH_SHORT);
                                        break;
                                    }
                                }
                            }
                            mListView.hideBubbleListView(false);
                            notifyDataSetChanged();
                        } else {
                            notifyDataSetChanged();
                        }
                    }
                }
            }
        } else if (!dropBack) {
            notifyDataSetChanged();
        }
        mSelectedList.clear();
        return delayHandled;
    }

    private void dragDelete(@NonNull final List<BubbleItem> delItems, boolean needTrash, boolean needShowListAfterHandled) {
        if (needTrash) {
            for (BubbleItem item : delItems) {
                item.trash();
            }
        } else {
            if (delItems.size() > 1) {
                Toast.makeText(mContext, R.string.toast_for_bubble_receive_by_app, Toast.LENGTH_SHORT).show();
            }
            if (BubbleController.getInstance().isBubbleHandleBySidebar()) {
                mListView.hideBubbleListView();
                needShowListAfterHandled = false;
            }
        }
        if (needShowListAfterHandled) {
            UIHandler.postDelayed(new Runnable() {
                public void run() {
                    mListView.playShowAnimationAfterPull(null);
                }
            }, 300);
        }
        mManager.removeBubbleItems(delItems);
        if (delItems.size() > 0) {
            mListView.switchHeadOperatorEditStatus(false);
        }
    }

    private void onOrderChanged() {
        int lastWeight = Integer.MIN_VALUE;
        for (int j = mList.size() - 1; j >= 0; j--) {
            BubbleItem bubbleItem = mList.get(j);
            int nowWeight;
            if (bubbleItem.getWeight() > lastWeight) {
                nowWeight = bubbleItem.getWeight();
            } else {
                nowWeight = lastWeight + 1;
                bubbleItem.setWeight(nowWeight);
            }
            lastWeight = nowWeight;
        }
        mManager.updateOrder();
    }

    public void playDropBackAnimtionWidthoutMove(DragEvent event, int insertIndex, final AnimListener listener) {
        int time = 200;
        int childrenCount = mListView.getChildCount();
        final List<View> views = new ArrayList<View>();
        AnimTimeLine animTimeLine = new AnimTimeLine();
        final float x = event.getX();
        final float y = event.getY();
        for (int i = 0; i < childrenCount; i++) {
            View view = mListView.getChildAt(i);
            if (view instanceof BubbleItemView) {
                if (view.getVisibility() == View.INVISIBLE) {
                    views.add(view);
                    view.setVisibility(View.VISIBLE);
                    view.setPivotX(view.getWidth() - ((BubbleItemView) view).queryWidth() / 2);
                    animTimeLine.addAnim(new Anim(view, Anim.TRANSLATE, time, Anim.CUBIC_OUT, new Vector3f(x - view.getPivotX(), y - view.getTop()), new Vector3f(0, 0)));
                    animTimeLine.addAnim(new Anim(view, Anim.SCALE, time, Anim.CUBIC_OUT, new Vector3f(0.3f, mListView.getNormalHeight()*1.0f/view.getHeight()), new Vector3f(1.0f, 1.0f)));
                    animTimeLine.addAnim(new Anim(view, Anim.TRANSPARENT, time, Anim.CUBIC_OUT, new Vector3f(1, 0, 0.0f), new Vector3f(0, 0, 1.0f)));
                } else {
                    ((BubbleItemView) view).clearAnimTranslateY();
                    float curDy = view.getTranslationY();
                    if (curDy != 0.0f) {
                        animTimeLine.addAnim(new Anim(view, Anim.TRANSLATE, time, Anim.CUBIC_OUT, new Vector3f(0.0f, curDy), Anim.VISIBLE));
                    }
                }
            }
        }
        animTimeLine.setAnimListener(new SimpleAnimListener() {
            public void onStart() {

            }

            public void onComplete(int type) {
                for (View view : views) {
                    view.setPivotX(view.getWidth()/2);
                    view.setTranslationY(0.0f);
                }
                if (listener != null) {
                    listener.onComplete(type);
                }
            }
        });
        boolean success = animTimeLine.start();
        if(!success){ // if animTime line is empty or execute failed , restore the views
            for (View view : views) {
                view.setPivotX(view.getWidth()/2);
                view.setTranslationY(0.0f);
            }
            if (listener != null) {
                listener.onComplete(0);
            }
        }
    }

    private void resetBubbleItemInList(List<BubbleItem> selectedList) {
        for (BubbleItem item : selectedList) {
            item.setSelected(false);
            item.setIsTemp(false);
        }
        clearFakeList();
        notifyDataSetChanged();
    }

    public boolean hasFakeList() {
        return mFakeList != null;
    }

    public void clearFakeList() {
        if (mFakeList != null) {
            mFakeList = null;
            notifyDataSetChanged();
        }
    }

    public int getInsertPos(View view) {
        if (view instanceof BubbleItemView) {
            BubbleItem item = ((BubbleItemView)view).getBubbleItem();
            return mList.indexOf(item);
        } else {
            return mList.size();
        }
    }

    public BubbleItem getItemByRealPosition(int position) {
        if (position >= 0 && position < mList.size()) {
            return mList.get(position);
        }
        return null;
    }

    public int getPositionBySyncId(long syncId) {
        if (mFakeList != null) {
            return -1;
        }
        int count = mList.size();
        for (int i = 0; i < count; i++) {
            BubbleItem item = mList.get(i);
            try {
                if (!TextUtils.isEmpty(item.getSyncId())
                        && Long.parseLong(item.getSyncId()) == syncId) {
                    return i;
                }
            } catch (NumberFormatException e) {
                // ignore
            }
        }
        return -1;
    }

    public void setFakeList(int position) {
        int count = 0;
        for (int i = 0; i < position; ++i) {
            if (mList.get(i).isSelected()) {
                count++;
            }
        }
        position -= count;
        mFakeList = new ArrayList<BubbleItem>();
        for (BubbleItem item : mList) {
            if (!item.isSelected()) {
                mFakeList.add(item);
            }
        }
        if (position >= 0) {
            mFakeList.add(position, BubbleItem.sBubbleItemFake);
        } else {
            mFakeList.add(BubbleItem.sBubbleItemFake);
        }
        notifyDataSetChanged();
    }

    public void playDropBackAnimtion(DragEvent event, int insertListPos) {
        int fakePos = mFakeList == null ? -1 : mFakeList.indexOf(BubbleItem.sBubbleItemFake);
        mList.removeAll(mSelectedList);
        mList.addAll(insertListPos, mSelectedList);
        onOrderChanged();
        resetBubbleItemInList(mSelectedList);
        int first = mListView.getFirstVisiblePosition();
        int last = mListView.getLastVisiblePosition();
        insertListPos += mListView.getHeaderViewsCount();
        List<View> viewlist = new ArrayList<View>();
        List<Integer> startlist = new ArrayList<Integer>();
        List<Integer> endlist = new ArrayList<Integer>();
        if (fakePos >= 0) {
            fakePos += mListView.getHeaderViewsCount();
        }
        Integer baseTop = null;
        View insertAtFront = null;
        if (fakePos <= insertListPos) {
            insertListPos += 1;
        }
        int heightSum = 0;
        for (int i = first; i < last; i++) {
            View view = mListView.getChildAt(i - first);
            if (view.getVisibility() == View.VISIBLE) {
                if (baseTop == null) {
                    baseTop = view.getTop();
                    if (viewlist.size() == 0 && fakePos >= 0 && fakePos < i) {
                        baseTop -= mListView.getNormalHeight();
                    }
                }
                startlist.add((int) view.getY());
                if (view instanceof BubbleItemView) {
//                    view = inflateBubbleView(((BubbleItemView)view).getBubbleItem());
                    ((BubbleItemView)view).abort();
                }
                viewlist.add(view);
                if (insertAtFront == null && i >= insertListPos) {
                    insertAtFront = view;
                }
                if (insertAtFront == null) {
                    heightSum += view.getMeasuredHeight();
                }
            }
        }
        int insertHeight = 0;
        List<View> flyList = new ArrayList<View>();
        for (BubbleItem item : mSelectedList) {
            BubbleItemView view = inflateBubbleView(item);
            view.setVisibility(View.VISIBLE);
            ViewGroup.LayoutParams layoutParams = new FrameLayout.LayoutParams(view.getMeasuredWidth(), view.getMeasuredHeight());
            view.setLayoutParams(layoutParams);
            flyList.add(view);
            insertHeight += view.getMeasuredHeight();
            if (insertHeight > mListView.getHeight() - heightSum) {
                break;
            }
        }
        int index = insertAtFront != null ? viewlist.indexOf(insertAtFront) : -1;
        int flyToBase = 0;
        for (int i = 0; i < viewlist.size(); i++) {
            if (i < index || index < 0) {
                endlist.add(baseTop);
                baseTop += viewlist.get(i).getHeight();
                flyToBase = baseTop;
            } else {
                endlist.add(baseTop + insertHeight);
                baseTop += viewlist.get(i).getHeight();
            }
        }
        final float x = event.getX();
        final float y = event.getY();
        AnimTimeLine animTimeLine = new AnimTimeLine();
        int time = 200;
        for (View view : flyList) {
            view.setPivotX(view.getMeasuredWidth() - ((BubbleItemView) view).queryWidth() / 2);
            animTimeLine.addAnim(new Anim(view, Anim.TRANSLATE, time, Anim.CUBIC_OUT, new Vector3f(x - view.getPivotX(), y), new Vector3f(0, flyToBase)));
            animTimeLine.addAnim(new Anim(view, Anim.SCALE, time, Anim.CUBIC_OUT, new Vector3f(0.3f, mListView.getNormalHeight()*1.0f/view.getMeasuredHeight()), new Vector3f(1.0f, 1.0f)));
            animTimeLine.addAnim(new Anim(view, Anim.TRANSPARENT, time, Anim.CUBIC_OUT, new Vector3f(1, 0, 0.0f), new Vector3f(0, 0, 1.0f)));
            flyToBase += view.getMeasuredHeight();
        }
        mListView.playFlyToAnimtion(animTimeLine, flyList);
        mListView.playDropBackAnimtion(viewlist, startlist, endlist);
    }

    public int getRealPosition(int oriPos) {
        int count = 0;
        for (int i = 0; i < oriPos; ++i) {
            if (mList.get(i).isSelected()) {
                count++;
            }
        }
        oriPos -= count;
        return oriPos;
    }

    public int getRealCount() {
        return mList != null ? mList.size() : 0;
    }

    public boolean handleDropBackSelected(DragEvent event, int position) {
        log.info("handleDropBackSelected position=" + position);
        if (position > mList.size()) {
            log.info("change drop position to " + position);
            position = mList.size();
        }
        boolean handle = true;
        int count = 0;
        for (int i = 0; i < position; ++i) {
            if (mList.get(i).isSelected()) {
                count++;
            }
        }
        position -= count;
        getSelectedBubbleItems(mSelectedList);
        if (position < 0 || position > mList.size() - mSelectedList.size()) {
            playDropBackAnimtionWidthoutMove(event, -1, new AnimCancelableListener() {
                public void onAnimCompleted() {
                    resetBubbleItemInList(mSelectedList);
                }
            });
        } else {
            if (mSelectedList.size() > 0) {
                playDropBackAnimtion(event, position);
            } else {
                List<GlobalBubble> items = Utils.convertToBubbleItems(event.getClipData());
                if (items != null) {
                    if (items.size() > 0) {
                        List<BubbleItem> bubbleItems = mManager.getBubbleItemsFrom(items);
                        List<BubbleItem> toAdd = new ArrayList<BubbleItem>();
                        for (BubbleItem item : bubbleItems) {
                            if (item.getTimeStamp() == 0) {
                                item.setTimeStamp(System.currentTimeMillis());
                            }
                            item.setWillPlayShowAnim(true);
                            toAdd.add(item);
                        }
                        if (GlobalBubble.COLOR_SHARE == Constants.getDefaultBubbleColor()) {
                            GlobalBubbleManager.getInstance().handleShareItems(bubbleItems);
                        }
                        addData(position, toAdd);
                        mManager.addBubbleItemsOnly(toAdd, position);
                        onOrderChanged();
                    }
                } else {
                    final BubbleItem bubbleItem = new BubbleItem();
                    bubbleItem.setTimeStamp(System.currentTimeMillis());
                    bubbleItem.setWillPlayShowAnim(true);
                    bubbleItem.setColor(Constants.getNewBubbleColor());
                    final List<BubbleItem> bubbleItems = new ArrayList<BubbleItem>();
                    bubbleItems.add(bubbleItem);
                    Runnable callback = new Runnable() {
                        public void run() {
                            UIHandler.post(new Runnable() {
                                public void run() {
                                    bubbleItem.setRefreshAttachment(true);
                                    GlobalBubbleManager.getInstance().notifyBubbleAdded(bubbleItems);
                                }
                            });
                        }
                    };
                    if (GlobalBubble.COLOR_SHARE == Constants.getDefaultBubbleColor()) {
                        GlobalBubbleManager.getInstance().handleShareItems(bubbleItems);
                    }

                    int result = LocalInterface.handleAttachments(bubbleItem, event.getClipDescription(), event.getClipData(), mContext, callback);
                    if (result == LocalInterface.HANDLE_ATTCHMENT_OK) {
                        addData(position, bubbleItems);
                        mManager.addBubbleItemsToVisible(bubbleItems,0);
                        onOrderChanged();
                        handle = true;
                    } else if (result == LocalInterface.HANDLE_ATTCHMENT_INVALID) {
                        GlobalBubbleUtils.showSystemToast(R.string.drag_empty_tip, Toast.LENGTH_SHORT);
                    }
                }
                resetBubbleItemInList(mSelectedList);
            }
        }
        return handle;
    }

    public boolean handleDropInsertAttachment(DragEvent event, int position, boolean isInsertNewBubble) {
        if (isInsertNewBubble) {
            return handleDropBackSelected(event, position);
        }

        BubbleItem item = null;
        if (position >= 0 && position < mList.size()) {
            item = mList.get(position);
        }
        if (item == null) {
            return false;
        }
        clearFakeList();
        AttachmentUtils.handleAttachmentPickResult(event.getClipData(), null, item.getId(), mContext);
        return true;
    }

    public void toMode(int mode, boolean needAnim) {
        final int lastMode = mMode;
        mMode = mode;
        switch (mode) {
            case ViewMode.BUBBLE_EDIT: {
                editAll(true, needAnim);
            }
            break;
            case ViewMode.BUBBLE_NORMAL: {
                if (lastMode == ViewMode.BUBBLE_EDIT) {
                    editAll(false, needAnim);
                } else {
                    notifyDataSetChanged();
                }
            }
            break;
            case ViewMode.BUBBLE_SEARCH: {
                notifyDataSetChanged();
            }
            break;
        }
    }

    public int getMode() {
        return mMode;
    }

    private void editAll(boolean edit, boolean needAnim) {
        if (!edit) {
            for (BubbleItem item : mList) {
                item.setSelected(false);
            }
        }
        final ListView listView = mListView;
        List<Animator> list = new ArrayList<Animator>();
        int count = listView.getChildCount();
        for (int i = 0; i < count; i++) {
            View view = listView.getChildAt(i);
            if (view instanceof BubbleItemView) {
                BubbleItemView bubbleItemView = (BubbleItemView) view;
                bubbleItemView.setMode(getMode());
                Animator animator = bubbleItemView.showEdit(bubbleItemView.getBubbleItem(), needAnim);
                if (animator != null) {
                    list.add(animator);
                }
            }
        }
        if (list.size() > 0) {
            AnimatorSet animatorSet = new AnimatorSet();
            animatorSet.playTogether(list);
            animatorSet.addListener(new Animator.AnimatorListener() {
                public void onAnimationStart(Animator animation) {

                }

                public void onAnimationEnd(Animator animation) {
                    notifyDataSetChanged();
                }

                public void onAnimationCancel(Animator animation) {

                }

                public void onAnimationRepeat(Animator animation) {

                }
            });
            animatorSet.start();
        } else {
            notifyDataSetChanged();
        }
    }

    public List<BubbleItemView> closeAll(final ListView listView, final Animator.AnimatorListener extraListener) {
        List<BubbleItemView> result = new ArrayList<BubbleItemView>();
        int startIndex = findFirstIndex(true);
        List<Animator> list = new ArrayList<Animator>();
        if (startIndex >= 0) {
            int size = mList.size();
            BubbleItem item = null;
            for (int i = 0; i < size; i++) {
                item = mList.get(i);
                if (item.isInLargeMode()) {
                    item.setInLargeMode(false);
                }
            }
            int count = listView.getChildCount();
            for (int i = 0; i < count; i++) {
                View view = listView.getChildAt(i);
                if (view instanceof BubbleItemView) {
                    BubbleItemView bubbleItemView = (BubbleItemView) view;
                    if (!bubbleItemView.isInNormalMode()) {
                        List<Animator> animators = bubbleItemView.toNormal(bubbleItemView.getBubbleItem(), true, false);
                        result.add(bubbleItemView);
                        if (animators != null) {
                            list.addAll(animators);
                        }
                    }
                }
            }
        }
        if (list.size() == 0 && extraListener != null) {
            extraListener.onAnimationEnd(null);
            return new ArrayList<BubbleItemView>();
        }
        if (list.size() > 0) {
            AnimatorSet animatorSet = new AnimatorSet();
            if (extraListener != null) {
                animatorSet.addListener(extraListener);
            }
            animatorSet.playTogether(list);
            animatorSet.start();
        }
        return result;
    }

    public void openAll(final ListView listView) {
        int startIndex = findFirstIndex(false);
        if (startIndex >= 0) {
            int size = mList.size();
            BubbleItem item = null;
            for (int i = 0; i < size; i++) {
                item = mList.get(i);
                if (!item.isInLargeMode()) {
                    item.setInLargeMode(true);
                }
            }
            int count = listView.getChildCount();
            AnimatorSet animatorSet = new AnimatorSet();
            List<Animator> animators = new ArrayList<>();
            for (int i = 0; i < count; i++) {
                View view = listView.getChildAt(i);
                if (view instanceof BubbleItemView) {
                    BubbleItemView bubbleItemView = (BubbleItemView) view;
                    if (!bubbleItemView.isInLargeMode()) {
                        animators.addAll(bubbleItemView.toLargeMode(bubbleItemView.getBubbleItem(), true, false));
                    }
                }
            }
            if (animators.size() > 0) {
                animatorSet.playTogether(animators);
                animatorSet.start();
            }
        }
    }

    public int findFirstIndex(boolean open) {
        int size = mList.size();
        BubbleItem item = null;
        if (open) {
            for (int i = 0; i < size; i++) {
                item = mList.get(i);
                if (item.isInLargeMode()) {
                    return i;
                }
            }
        } else {
            for (int i = 0; i < size; i++) {
                item = mList.get(i);
                if (!item.isInLargeMode()) {
                    return i;
                }
            }
        }
        return -1;
    }

    /**
     * a probable value is enough
     * @return
     */
    public int getNormalItemWidth() {
        return 450;
    }

    private void playDownAnimation(ListView listView, int offsetDown, final AnimListener listener) {
        int count = listView.getChildCount();
        View target = null;
        if (listView.getVisibility() == View.VISIBLE) {
            for (int i = 0; i < count; i++) {
                View view = listView.getChildAt(i);
                if (view instanceof BubbleItemView) {
                    BubbleItemView itemView = (BubbleItemView)view;
                    itemView.clearAnimTranslateY();
                    float startY = 0;
                    float endY = offsetDown;
                    final Anim animation = new Anim(itemView, Anim.TRANSLATE, 200, Anim.DEFAULT, new Vector3f(0.0f, startY), new Vector3f(0.0f, endY));
                    if (target == null) {
                        target = itemView;
                        animation.setListener(new SimpleAnimListener() {
                            @Override
                            public void onStart() {

                            }

                            @Override
                            public void onComplete(int type) {
                                animation.getView().setTranslationY(0.0f);
                                if (listener != null) {
                                    listener.onComplete(type);
                                }
                            }
                        });
                    } else {
                        animation.setListener(new SimpleAnimListener() {
                            @Override
                            public void onStart() {

                            }

                            @Override
                            public void onComplete(int type) {
                                animation.getView().setTranslationY(0.0f);
                            }
                        });
                    }
                    animation.start();
                }
            }
        }
        if (target == null) {
            listener.onComplete(0);
        }
    }

    private void onUpdateInner() {
        if (Looper.getMainLooper() == Looper.myLooper()) {
            mList = mManager.getBubbles(mFilterBubbleColor, mFilterStr, mHideTodoOverBubble);
            notifyDataSetChanged();
        } else {
            UIHandler.post(new Runnable() {
                public void run() {
                    onUpdateInner();
                }
            });
        }
    }

    private void addData(int index, List<BubbleItem> data) {
        if (mList != null && data != null) {
            List<BubbleItem> realAddData = new ArrayList<BubbleItem>();
            for (BubbleItem item : data) {
                if (!mList.contains(item)) {
                    if (!mHideTodoOverBubble || item.getUsedTime() <= 0) {
                        realAddData.add(item);
                    }
                }
            }
            mList.addAll(index, realAddData);
        }
    }

    private GlobalBubbleManager.OnUpdateListener mBubbleListener = new GlobalBubbleManager.OnUpdateListener() {
        @Override
        public void onUpdate() {
            onUpdateInner();
        }

        @Override
        public boolean onBubblesAdd(List<BubbleItem> bubbles) {
            log.info("onAddBubbles: " + bubbles);
            final List<BubbleItem> items = new ArrayList<BubbleItem>();
            for (BubbleItem item : bubbles) {
                if (!isAcceptBubble(item)) {
                    continue;
                }
                item.setWillPlayShowAnim(mListView.getVisibility() == View.VISIBLE);
                items.add(item);
            }
            if (!items.isEmpty()) {
                UIHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        addData(0, items);
                        notifyDataSetChanged();
                        if (mListView.getVisibility() == View.GONE) {
                            mListView.setSelection(0);
                        } else {
                            final int totalCount = getCount() + mListView.getHeaderViewsCount() + mListView.getFooterViewsCount() - items.size();
                            final int scrollStart = (mListView.getLastVisiblePosition() - mListView.getFirstVisiblePosition()) * 2;
                            final int position = mListView.getLastVisiblePosition();
                            if (position > scrollStart && totalCount > scrollStart) {
                                mListView.setSelection(scrollStart);
                            }
                            mListView.smoothScrollToPosition(1);
                        }
                    }
                });
            }
            return true;
        }
    };

    public void handleBubbleBySelf(Set<Integer> ids) {
        mHandleBubbleBySelf = true;
    }

    public void selectAll(boolean select) {
        for (BubbleItem item : mList) {
            item.setSelected(select);
        }
        notifyDataSetChanged();
    }

    public boolean isAllSelected() {
        for (BubbleItem item : mList) {
            if (!item.isSelected()) {
                return false;
            }
        }
        if (mList.size() == 0) {
            return false;
        } else {
            return true;
        }
    }

    public boolean isNoneSelected() {
        for (BubbleItem item : mList) {
            if (item.isSelected() && !item.isTemp()) {
                return false;
            }
        }
        return true;
    }

    public void chooseToDelete(DeleteAllListener listener, final List<Integer> idlist) {
        final List<BubbleItem> bubbleItems = new ArrayList<BubbleItem>();
        for (int i = 0; i < mList.size(); i++) {
            BubbleItem item = mList.get(i);
            if (idlist.contains(item.getId())) {
                bubbleItems.add(item);
            }
        }
        if (listener != null) {
            listener.deleteListener();
        }
        if (bubbleItems.size() > 0) {
            mManager.removeBubbleItems(bubbleItems);
            bubbleItems.clear();
        }
    }

    public void selectToDelete(final DeleteAllListener listener) {
        final List<BubbleItem> bubbleItems = new ArrayList<BubbleItem>();
        for (int i = 0; i < mList.size(); i++) {
            BubbleItem item = mList.get(i);
            if (item.isSelected()) {
                bubbleItems.add(item);
            }
        }
        final List<Integer> reservedPos = getReservedItems();
        if (bubbleItems.size() > 1) {
            String title = mContext.getResources().getString(R.string.bubble_notice);
            int msgResId = bubbleItems.size() > 1 ? R.string.bubble_delete_multiple : R.string.bubble_delete_single;
            String message = mContext.getResources().getString(msgResId);
            BubbleController.getInstance().showConfirmDialog(mContext, title, message, new Runnable() {
                public void run() {
                    for (BubbleItem item : bubbleItems) {
                        item.trash();
                    }
                    selectToDelete(listener, bubbleItems, reservedPos, true);
                }
            }, null);
        } else {
            for (BubbleItem item : bubbleItems) {
                item.trash();
            }
            selectToDelete(listener, bubbleItems, reservedPos, true);
        }
    }

    private void selectToDelete(final DeleteAllListener listener, final List<BubbleItem> bubbleItems, final List<Integer> reservedPos, final boolean realDelete) {
        List<BubbleItemView> deleteViews = new ArrayList<BubbleItemView>();
        for (int i = 0; i < mListView.getChildCount(); i++) {
            View child = mListView.getChildAt(i);
            if (child instanceof BubbleItemView) {
                BubbleItemView biv = (BubbleItemView) child;
                if (bubbleItems.contains(biv.getBubbleItem())) {
                    deleteViews.add((BubbleItemView)child);
                }
            }
        }
        AnimCancelableListener animCancelableListener = new AnimCancelableListener() {
            public void onAnimCompleted() {
                mListView.startDeleteAnimations(reservedPos, new AnimCancelableListener() {
                    public void onAnimCompleted() {
                        if (bubbleItems.size() > 0) {
                            if (realDelete) {
                                mManager.removeBubbleItems(bubbleItems);
                                bubbleItems.clear();
                            } else {
                                filterBubbles(mFilterStr);
                            }
                        }
                    }
                });
                if (listener != null) {
                    listener.deleteListener();
                }
            }
        };
        animCancelableListener.onAnimCompleted();
    }

    private void hideTodoOverBubble() {
        final List<BubbleItem> bubbleItems = new ArrayList<BubbleItem>();
        for (int i = 0; i < mList.size(); i++) {
            BubbleItem item = mList.get(i);
            if (item.getUsedTime() > 0) {
                bubbleItems.add(item);
            }
        }
        hideTodoOverBubble(bubbleItems);
    }

    public void hideTodoOverBubble(List<BubbleItem> items) {
        final List<Integer> reservedPos = getReservedItems(false);
        selectToDelete(null, items, reservedPos, false);
    }

    public List<Integer> getReservedItems() {
        return getReservedItems(true);
    }

    public List<Integer> getReservedItems(boolean realDelete) {
        List<Integer> reserved = new ArrayList<Integer>();
        int headviewcount = mListView.getHeaderViewsCount();
        for (int i = 0; i < headviewcount; i++) {
            reserved.add(i);
        }
        int index = 0;
        for (BubbleItem item : mFakeList == null ? mList : mFakeList) {
            if (realDelete && !item.isSelected() || !realDelete && item.getUsedTime() <= 0) {
                reserved.add(index + headviewcount);
            }
            index++;
        }
        return reserved;
    }

    public List<BubbleItem> getSelectedBubbles() {
        List<BubbleItem> selectBubbles = new ArrayList<BubbleItem>();
        for (BubbleItem item : mList) {
            if (item.isSelected()) {
                selectBubbles.add(item);
            }
        }
        return selectBubbles;
    }

    public int getSelectedBubblesCount() {
        int count = 0;
        for (BubbleItem item : mList) {
            if (item.isSelected()) {
                count++;
            }
        }
        return count;
    }

    private CheckForLongPress mPendingCheckForLongPress = null;

    private void checkForLongClick(View view) {
        removeLongPressCallback();
        if (mPendingCheckForLongPress == null) {
            mPendingCheckForLongPress = new CheckForLongPress();
        }
        mPendingCheckForLongPress.setView(view);
        // add 50ms delay for waiting AttachmentView onLongClick result.
        // it will cancel current long press if occurs.
        UIHandler.postDelayed(mPendingCheckForLongPress, ViewConfiguration.getLongPressTimeout() + 50);
    }

    private void removeLongPressCallback() {
        if (mPendingCheckForLongPress != null) {
            UIHandler.removeCallbacks(mPendingCheckForLongPress);
        }
    }

    private View.OnTouchListener mOnTouchListener = new View.OnTouchListener() {
        public boolean onTouch(final View v, MotionEvent event) {
            if (BubbleController.getInstance().isInputting()) {
                log.error("forbide draging while inputting !");
                return false;
            }
            if (event.getAction() == MotionEvent.ACTION_DOWN && StatusManager.isBubbleDragging()) {
                return false;
            }
            BubbleItemView parent = ((BubbleItemView) v.getParent());
            int animDura = 200;
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    mCurState = STATE_NONE;
                    mDownRawX = event.getRawX();
                    mDownX = event.getX();
                    mDownY = event.getY();
                    //parent.touchDown();
                    checkForLongClick(v);
                    return false;
                case MotionEvent.ACTION_MOVE:
                    if (!BubbleController.getInstance().isInPptContext(v.getContext())) {
                        float curX = event.getRawX();
                        float curY = event.getY();
                        if (Math.abs(mDownRawX - curX) > Math.abs(mDownY - curY) && Math.abs(mDownRawX - curX) > mTouchSlop) {
                            mCurState = STATE_MOVE;
                            removeLongPressCallback();
                        }
                        if (STATE_MOVE == mCurState) {
                            float moveX = curX - mDownRawX;
                            if (moveX > 0) {
                                float move = moveX/DAMPING;
                                parent.setTranslationX(move);
                            }
                        }
                    }
                    break;
                case MotionEvent.ACTION_UP:
                    parent.touchUp();
                    if(STATE_MOVE == mCurState) {
                        if (parent.getTranslationX() >= MOVE_THRESHOLD) {
                            if (mAdapterCallback != null) {
                                mAdapterCallback.execute(AdapterCallback.MSG_HIDE_LIST);
                            }
                        } else {
                            if (parent.getTranslationX() != 0) {
                                Vector3f from = new Vector3f(parent.getTranslationX(), 0);
                                Anim anim = new Anim(parent, Anim.TRANSLATE, animDura, Anim.CUBIC_OUT, from, Anim.ZERO);
                                anim.start();
                            }
                        }
                    }
                    removeLongPressCallback();
                    mCurState = STATE_NONE;
                    if (StatusManager.isBubbleDragging()) {
                        if (mCloseAnimatorSet != null) {
                            log.info("cancel drag mCloseAnimatorSet");
                            mCloseAnimatorSet.cancel();
                        }
                        if (mDragTimeLine != null) {
                            log.info("cancel drag mDragTimeLine");
                            mDragTimeLine.cancel();
                        }
                        onSelfDragEnd(null, false);
                    }
                    break;
                case MotionEvent.ACTION_CANCEL:
                    parent.touchUp();
                    if(STATE_MOVE == mCurState) {
                        if (parent.getTranslationX() >= MOVE_THRESHOLD) {
                            if (mAdapterCallback != null) {
                                mAdapterCallback.execute(AdapterCallback.MSG_HIDE_LIST);
                            }
                        } else {
                            Vector3f from = new Vector3f(parent.getTranslationX(), 0);
                            Anim anim = new Anim(parent, Anim.TRANSLATE, animDura, Anim.CUBIC_OUT, from, Anim.ZERO);
                            if (!anim.isEmpty()) {
                                anim.start();
                            }
                        }
                    }
                    removeLongPressCallback();
                    mCurState = STATE_NONE;
                    break;
            }
            return mCurState != STATE_NONE;
        }
    };

    @Override
    public boolean onLongClick(View v) {
        if (mCurState == STATE_LONG_CLICK) {
            return true;
        }
        View root = v.getRootView();
        if (root != null && !root.isLongPressSwipe()) {
            if (StatusManager.getStatus(StatusManager.BUBBLE_ANIM)) {
                log.error("drag end anim is running, startDrag return !");
                return false;
            }
            startDrag(v);
            mCurState = STATE_LONG_CLICK;
        }
        return true;
    }

    private class CheckForLongPress implements LongPressAction {
        private View mView;

        public void setView(View view) {
            mView = view;
        }

        @Override
        public void run() {
            if(mView != null){
                final BubbleItemView parent = ((BubbleItemView) mView.getParent());
                if(parent != null){
                    parent.touchDown();
                }
            }
            onLongClick(mView);
        }
    };

    public void moveHorizontal() {
        mCurState = STATE_MOVE;
    }

    public boolean isMovingHorizontal() {
        return mCurState == STATE_MOVE;
    }

    public void clearMovingState() {
        mCurState = STATE_NONE;
    }

    public void startDragWhileFling(BubbleItemView bubbleItemView) {
        bubbleItemView.touchDown();
        checkForLongClick(bubbleItemView.findViewById(R.id.fl_layout));
    }

    public void stopDragWhileFling() {
        if (mPendingCheckForLongPress != null) {
            BubbleItemView parent = (BubbleItemView) mPendingCheckForLongPress.mView.getParent();
            if (parent != null) {
                parent.touchUp();
            }
        }
        removeLongPressCallback();
    }

    public boolean isAcceptBubble(BubbleItem bubbleItem) {
        if (bubbleItem == null) {
            return false;
        }
        int itemColor = bubbleItem.getColor();
        if (bubbleItem.getShareStatus() > GlobalBubble.SHARE_STATUS_NOT_SHARED) {
            itemColor = GlobalBubble.COLOR_SHARE;
        }
        if (mFilterBubbleColor > FiltrateSetting.FILTRATE_ALL && itemColor != mFilterBubbleColor) {
            return false;
        }

        return true;
    }

    private void filterBubbles(boolean hidedone) {
        StatusManager.setStatus(StatusManager.HAS_FILTER_STRING, !TextUtils.isEmpty(mFilterStr));
        mList = mManager.getBubbles(mFilterBubbleColor, mFilterStr, mHideTodoOverBubble);
        if (!hidedone) {
            for (BubbleItem item : mList) {
                if (item.getUsedTime() > 0) {
                    item.setWillPlayShowAnim(true);
                }
            }
        }
        notifyDataSetChanged();
    }

    public void filterBubbles(String filter) {
        mFilterStr = filter;
        StatusManager.setStatus(StatusManager.HAS_FILTER_STRING, !TextUtils.isEmpty(mFilterStr));
        mList = mManager.getBubbles(mFilterBubbleColor, filter, mHideTodoOverBubble);
        notifyDataSetChanged();
    }

    public void switchShowHideTodoOver() {
        mHideTodoOverBubble = !mHideTodoOverBubble;
        Settings.Global.putInt(mContentResolver, SettingsSmt.Global.HIDE_TODO_OVER_BUBBLE, mHideTodoOverBubble ? 1 : 0);
        if (mHideTodoOverBubble) {
            hideTodoOverBubble();
        } else {
            filterBubbles(mHideTodoOverBubble);
        }
    }

    public void updateColorFilter(int color) {
        if (!BubbleController.getInstance().isInPptContext(mListView.getContext())) {
            if (mFilterBubbleColor != color) {
                mFilterBubbleColor = color;
                refreshData();
            }
        }
    }

    public boolean isTodoOverHide() {
        return mHideTodoOverBubble;
    }

    public boolean hasNormalBubble() {
        for (BubbleItem bi : mList) {
            if (!bi.isInLargeMode()) {
                return true;
            }
        }
        return false;
    }
}
