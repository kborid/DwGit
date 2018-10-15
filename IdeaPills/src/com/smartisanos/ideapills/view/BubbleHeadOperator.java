package com.smartisanos.ideapills.view;

import android.animation.Animator;
import android.animation.TypeEvaluator;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.service.onestep.GlobalBubble;
import android.text.TextUtils;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.support.v4.content.LocalBroadcastManager;
import android.widget.Toast;
import android.content.Context;

import com.smartisanos.ideapills.BubbleController;
import com.smartisanos.ideapills.Constants;
import com.smartisanos.ideapills.IdeaPillsApp;
import com.smartisanos.ideapills.R;
import com.smartisanos.ideapills.common.anim.Anim;
import com.smartisanos.ideapills.common.anim.AnimListener;
import com.smartisanos.ideapills.common.anim.SimpleAnimListener;
import com.smartisanos.ideapills.common.anim.Vector3f;
import com.smartisanos.ideapills.entity.BubbleItem;
import com.smartisanos.ideapills.sync.SyncManager;
import com.smartisanos.ideapills.sync.SyncProcessor;
import com.smartisanos.ideapills.sync.entity.SyncShareInvitation;
import com.smartisanos.ideapills.sync.share.GlobalInvitationAction;
import com.smartisanos.ideapills.sync.share.SyncShareManager;
import com.smartisanos.ideapills.sync.share.SyncShareUtils;
import com.smartisanos.ideapills.util.BubbleTrackerID;
import com.smartisanos.ideapills.util.GlobalBubbleManager;
import com.smartisanos.ideapills.util.GlobalBubbleUtils;
import com.smartisanos.ideapills.util.LOG;
import com.smartisanos.ideapills.util.StatusManager;
import com.smartisanos.ideapills.util.Tracker;
import com.smartisanos.ideapills.util.Utils;
import com.smartisanos.ideapills.util.ViewUtils;
import com.smartisanos.ideapills.common.util.UIHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class BubbleHeadOperator implements View.OnClickListener {
    private static final LOG log = LOG.getInstance(BubbleHeadOperator.class);

    private static final int ANIMDURATION = 250;
    private boolean mHideTodoOver = false;
    private int mMode = ViewMode.BUBBLE_NORMAL;

    private class ExpandArg{
        int mWidth;
        float mAlpha;
    }

    private TypeEvaluator<ExpandArg> mExpandEvaluator = new TypeEvaluator<ExpandArg>() {
        private ExpandArg mExpandArg = new ExpandArg();
        public ExpandArg evaluate(float fraction, ExpandArg startValue, ExpandArg endValue) {
            int sw = startValue.mWidth;
            float sa = startValue.mAlpha;
            int cw = (int) (sw + fraction * (endValue.mWidth - sw));
            float ca = (sa + fraction * (endValue.mAlpha - sa));
            mExpandArg.mWidth = cw;
            mExpandArg.mAlpha = ca;
            return mExpandArg;
        }
    };

    BubbleHeadOptView mView = null;
    private ViewGroup mllHeadExpand = null;
    LinearLayout mllHeadExpandDetail = null;
    private LinearLayout mllHeadExpandSearch = null;
    private ImageView mIvHeadEdit = null;
    private ImageView mIvHeadOpen = null;
    private ImageView mIvHeadHide = null;
    private View mIvHeadHideDivider;
    private ImageView mIvTrash = null;
    private ImageView mIvShare = null;
    private View mIvShareDivider;
    private ImageView mIvAdd = null;
    private ImageView mIvSearch = null;
    private ImageView mIvTodoOver = null;
    private View mTodoOverDivider = null;
    private View mFlHead = null;
    private CheckBox mCbSelectAll = null;
    private BubbleTextView mTvOver = null;
    private BubbleTextView mTvCancel = null;
    private int mExpandWidth = 0;
    private int mExpandDetailWidth = 0;
    private int mExpandSearchWidth = 0;
    private Locale mLastLocale;
    private boolean mNeedExpand = true;
    private BubbleListView mListView = null;
    GlobalBubbleManager mManager = null;
    private float mTranslateYinHideState = 0;
    private BubbleSearchEditText mBubbleSearchEditText;
    private Anim mTranslateYAnim;
    private ImageView mIvFiltrate = null;
    private View mIvFiltrateDivider;
    private FiltrateSetting mFiltrateSetting;

    public BubbleHeadOperator(BubbleListView listView, View view) {
        mListView = listView;
        mFiltrateSetting = new FiltrateSetting(mListView.getContext());
        mFiltrateSetting.setFiltrateChangeListener(mFiltrateChangeListener);
        setOptView(view, false);
        mLastLocale =  view.getResources().getConfiguration().locale;
        mManager = GlobalBubbleManager.getInstance();
    }

    public void setOptView(View view, boolean focusSearch) {
        if (view == mView) {
            return;
        }
        ViewGroup llHeadExpandSearch = mllHeadExpandSearch;
        ViewGroup llHeadExpand = mllHeadExpand;
        LinearLayout llHeadExpandDetail = mllHeadExpandDetail;
        ImageView ivHeadEdit = mIvHeadEdit;
        ImageView ivHeadOpen = mIvHeadOpen;
        ImageView ivTrash = mIvTrash;
        ImageView ivShare = mIvShare;
        ImageView ivAdd = mIvAdd;
        CheckBox cbSelectAll = mCbSelectAll;
        BubbleSearchEditText bubbleSearchEditText = mBubbleSearchEditText;
        mllHeadExpand = (ViewGroup) view.findViewById(R.id.ll_headexpand);
        mllHeadExpandDetail = (LinearLayout) view.findViewById(R.id.ll_headexpand_detail);
        mllHeadExpandSearch = (LinearLayout) view.findViewById(R.id.ll_headexpand_search);
        mTvOver = (BubbleTextView) view.findViewById(R.id.tv_over);
        mTvOver.setOnClickListener(this);
        mTvCancel = (BubbleTextView) view.findViewById(R.id.tv_cancel);
        mTvCancel.setOnClickListener(this);
        mBubbleSearchEditText = (BubbleSearchEditText) mllHeadExpandSearch.findViewById(R.id.et_search);
        mIvSearch = (ImageView) view.findViewById(R.id.iv_head_search);
        mIvSearch.setOnClickListener(this);
        mIvTodoOver = (ImageView) view.findViewById(R.id.iv_todo_over);
        mIvTodoOver.setOnClickListener(this);
        mTodoOverDivider = view.findViewById(R.id.todo_over_divider);
        mIvShare = (ImageView) view.findViewById(R.id.iv_bubble_share);
        mIvShare.setOnClickListener(this);
        mIvShareDivider = view.findViewById(R.id.iv_bubble_share_divider);
        mIvTrash = (ImageView) view.findViewById(R.id.iv_trash);
        mIvTrash.setOnClickListener(this);
        mIvHeadEdit = (ImageView) view.findViewById(R.id.iv_head_edit);
        mIvHeadEdit.setOnClickListener(this);
        mIvHeadOpen = (ImageView) view.findViewById(R.id.iv_head_open);
        mIvHeadOpen.setOnClickListener(this);
        mIvHeadHide = (ImageView) view.findViewById(R.id.iv_head_hide);
        mIvHeadHide.setOnClickListener(this);
        mIvHeadHideDivider = view.findViewById(R.id.iv_hide_divider);
        mIvAdd = (ImageView) view.findViewById(R.id.iv_head_add);
        mIvAdd.setOnClickListener(this);
        mIvFiltrate = (ImageView) view.findViewById(R.id.iv_head_filtrate);
        mIvFiltrate.setOnClickListener(this);
        mIvFiltrateDivider = view.findViewById(R.id.iv_filtrate_divider);
        mFlHead = view.findViewById(R.id.fl_head_normal);
        mCbSelectAll = (CheckBox) view.findViewById(R.id.cb_select_all);
        mCbSelectAll.setOnClickListener(this);
        mCbSelectAll.setOnTouchListener(new View.OnTouchListener() {
            public boolean onTouch(View v, MotionEvent event) {
                if (StatusManager.isBubbleDragging()) {
                    return true;
                }
                return false;
            }
        });
        if (mView != null) {
            mllHeadExpandSearch.setVisibility(llHeadExpandSearch.getVisibility());
            mllHeadExpandSearch.setLayoutParams(llHeadExpandSearch.getLayoutParams());
            mllHeadExpandSearch.setAlpha(llHeadExpandSearch.getAlpha());
            mllHeadExpand.setVisibility(llHeadExpand.getVisibility());
            mllHeadExpand.setLayoutParams(llHeadExpand.getLayoutParams());
            mllHeadExpand.setAlpha(llHeadExpand.getAlpha());
            mllHeadExpandDetail.setVisibility(llHeadExpandDetail.getVisibility());
            mllHeadExpandDetail.setLayoutParams(llHeadExpandDetail.getLayoutParams());
            mllHeadExpandDetail.setAlpha(llHeadExpandDetail.getAlpha());
            mIvHeadEdit.setImageDrawable(ivHeadEdit.getDrawable());
            mIvHeadOpen.setImageDrawable(ivHeadOpen.getDrawable());
            mCbSelectAll.setChecked(cbSelectAll.isChecked());
            mIvTrash.setEnabled(ivTrash.isEnabled());
            mIvTrash.setAlpha(ivTrash.getAlpha());
            mIvShare.setEnabled(ivShare.isEnabled());
            mIvShare.setAlpha(ivShare.getAlpha());
            mIvAdd.setAlpha(ivAdd.getAlpha());
            mIvAdd.setEnabled(ivAdd.isEnabled());
            mIvHeadEdit.setEnabled(ivHeadEdit.isEnabled());
            mIvHeadOpen.setEnabled(ivHeadOpen.isEnabled());
            mIvHeadEdit.setAlpha(ivHeadEdit.getAlpha());
            mIvHeadOpen.setAlpha(ivHeadOpen.getAlpha());
            mBubbleSearchEditText.setBubbleListView(null);
            mBubbleSearchEditText.setText(bubbleSearchEditText.getText());

            LinearLayout.LayoutParams headParam = (LinearLayout.LayoutParams) mFlHead.getLayoutParams();
            headParam.topMargin = Utils.getHeadMarginTop(mView.getContext());
            mFlHead.setLayoutParams(headParam);
            updateFiltrateState();
        }
        if (focusSearch) {
            mBubbleSearchEditText.requestFocus();
            //  Restore the cursor to its original position;
            if (!TextUtils.isEmpty(bubbleSearchEditText.getText())) {
                int index = bubbleSearchEditText.getSelectionStart();
                mBubbleSearchEditText.setSelection(index);
            }
        }
        mBubbleSearchEditText.setBubbleListView(mListView);
        if (BubbleController.getInstance().isInPptContext(mBubbleSearchEditText.getContext())) {
            mFlHead.setBackgroundResource(R.drawable.ppt_popup_setting_bg);
            mllHeadExpand.setPadding(mllHeadExpand.getPaddingLeft(), 0, mllHeadExpand.getPaddingLeft(), 0);
            mllHeadExpandSearch.setPadding(mllHeadExpandSearch.getPaddingLeft(), 0, mllHeadExpandSearch.getPaddingLeft(), 0);
            mIvHeadHide.setVisibility(View.GONE);
            mIvHeadHideDivider.setVisibility(View.GONE);
            mIvShare.setVisibility(View.GONE);
            mIvShareDivider.setVisibility(View.GONE);
            mIvFiltrate.setVisibility(View.GONE);
            mIvFiltrateDivider.setVisibility(View.GONE);
        }
        if (mExpandWidth == 0) {
            mExpandWidth = ViewUtils.getSupposeWidthNoFixWidth(mllHeadExpand);
        }
        if (mExpandDetailWidth == 0) {
            mExpandDetailWidth = ViewUtils.getSupposeWidthNoFixWidth(mllHeadExpandDetail);
        }
        if (mExpandSearchWidth == 0) {
            mExpandSearchWidth = mListView.getResources().getDimensionPixelOffset(R.dimen.bubble_head_search_width);
        }
        int width = mllHeadExpand.getVisibility() == View.VISIBLE ? mExpandWidth :
                (mllHeadExpandDetail.getVisibility() == View.VISIBLE ? mExpandDetailWidth : mExpandSearchWidth);
        if (width != 0) {
            mFlHead.getLayoutParams().width = width;
            mFlHead.setPadding(0, 0, 0, 0);
            //mFlHead.requestLayout();
        }
        if (mView != null) {
            view.setTranslationX(mView.getTranslationX());
            view.setAlpha(mView.getAlpha());
        }
        mView = (BubbleHeadOptView) view;
        mView.setBubbleListView(mListView);
        mView.clearMovingState();
        updateTodoOverStatus(true);
    }

    public int getTodoOverImageRes() {
        return mHideTodoOver ? R.drawable.hide_todo_icon : R.drawable.show_todo_icon;
    }

    public int getExpandWidth() {
        return mllHeadExpand.getVisibility() == View.VISIBLE ? mExpandWidth :
                (mllHeadExpandDetail.getVisibility() == View.VISIBLE ? mExpandDetailWidth : mExpandSearchWidth);
    }

    public int getExpandRealWidth() {
        return mExpandWidth;
    }

    public int getTodoOverVisible() {
        return mIvTodoOver.getVisibility();
    }

    public void updateTodoOverStatus() {
        updateTodoOverStatus(false);
    }
    public void updateTodoOverStatus(boolean forceupdate) {
        Resources res = mView.getResources();
        boolean visibleChange = false;
        boolean statusChange = false;
        boolean showorhide = mListView.isTodoOverHide();
        int color = mFiltrateSetting.getFiltrateColor();
        int visible = GlobalBubbleManager.getInstance().hasTodoOverBubble(color) ? View.VISIBLE : View.GONE;
        if (mIvTodoOver.getVisibility() != visible) {
            mIvTodoOver.setVisibility(visible);
            mTodoOverDivider.setVisibility(visible);
            mExpandWidth = ViewUtils.getSupposeWidthNoFixWidth(mllHeadExpand);
            int width = getExpandWidth();
            if (width != 0) {
                mFlHead.getLayoutParams().width = width;
                mllHeadExpand.getLayoutParams().width = width;
            }
            visibleChange = true;
        }
        if (mHideTodoOver != showorhide || forceupdate) {
            mHideTodoOver = showorhide;
            statusChange = true;
            mIvTodoOver.setImageResource(getTodoOverImageRes());
            mIvTodoOver.setContentDescription(res.getString(mHideTodoOver ? R.string.bubble_hide_todo_over : R.string.bubble_show_todo_over));
        }
        mListView.updateTodoOverStatus(visibleChange, statusChange);
    }

    public void updateExpandStatus() {
        if (mListView.isEmpty()) {
            setViewEnable(mIvHeadEdit, false);
            setViewEnable(mIvHeadOpen, false);
            setViewEnable(mIvSearch, false);
        } else {
            setViewEnable(mIvHeadEdit, true);
            setViewEnable(mIvHeadOpen, true);
            setViewEnable(mIvSearch, true);
        }
        if (mListView.mBubbleAdapter.isAllSelected()) {
            mCbSelectAll.setChecked(true);
        } else {
            mCbSelectAll.setChecked(false);
        }
        updateFiltrateState();
        updateTodoOverStatus();
        Resources res = mView.getResources();
        setViewEnable(mCbSelectAll, !mListView.isEmpty());
        if (mListView.isEmpty() || mListView.hasNormalBubble()) {
            mNeedExpand = true;
            mIvHeadOpen.setImageDrawable(mListView.getResources().getDrawable(R.drawable.bubble_expand_all));
            mIvHeadOpen.setContentDescription(res.getString(R.string.bubble_extend_all));
        } else {
            mNeedExpand = false;
            mIvHeadOpen.setImageDrawable(mListView.getResources().getDrawable(R.drawable.bubble_collapse_all));
            mIvHeadOpen.setContentDescription(res.getString(R.string.bubble_collapse_all));
        }
    }

    public void toEditImmediately(boolean edit) {
        toEdit(edit, false);
    }

    public boolean isFocusSearch() {
        return mBubbleSearchEditText != null && mBubbleSearchEditText.isFocused();
    }

    public void toEdit(boolean edit, final boolean needAnim) {
        toMode(edit?ViewMode.BUBBLE_EDIT :ViewMode.BUBBLE_NORMAL, needAnim);
    }

    public void toMode(final int mode, final boolean needAnim) {
        ViewGroup targetVg = null;
        mMode = mode;
        switch (mode) {
            case ViewMode.BUBBLE_NORMAL:
                targetVg = mllHeadExpandDetail.getVisibility() == View.VISIBLE ? mllHeadExpandDetail : mllHeadExpandSearch;
                break;
            case ViewMode.BUBBLE_EDIT:
                targetVg = mllHeadExpand;
                break;
            case ViewMode.BUBBLE_SEARCH:
                targetVg = mllHeadExpand;
                break;
        }
        int startW = mFlHead.getWidth();
        int endW = mExpandWidth;
        if (ViewMode.BUBBLE_EDIT == mode) {
            endW = mExpandDetailWidth;
        } else if (ViewMode.BUBBLE_SEARCH == mode) {
            endW = mExpandSearchWidth;
        }
        ExpandArg startEa = new ExpandArg();
        startEa.mWidth = startW;
        startEa.mAlpha = 1.0f;
        ExpandArg endEa = new ExpandArg();
        endEa.mWidth = endW;
        endEa.mAlpha = 0.0f;
        ValueAnimator valueAnimator = ValueAnimator.ofObject(mExpandEvaluator, startEa, endEa);
        valueAnimator.setDuration(250);

        final ViewGroup target = targetVg;
        if (needAnim) {
            ValueAnimator.AnimatorUpdateListener listener = new ValueAnimator.AnimatorUpdateListener() {
                public void onAnimationUpdate(ValueAnimator animation) {
                    ExpandArg expandArg = (ExpandArg) animation.getAnimatedValue();
                    ViewGroup.LayoutParams lp = target.getLayoutParams();
                    lp.width = expandArg.mWidth;
                    target.setAlpha(expandArg.mAlpha);
                    target.setLayoutParams(lp);
                    lp = mFlHead.getLayoutParams();
                    lp.width = expandArg.mWidth;
                    mFlHead.setLayoutParams(lp);
                }
            };
            valueAnimator.addUpdateListener(listener);
        } else {
            ExpandArg expandArg = endEa;
            ViewGroup.LayoutParams lp = target.getLayoutParams();
            lp.width = expandArg.mWidth;
            target.setAlpha(expandArg.mAlpha);
            target.setLayoutParams(lp);
            lp = mFlHead.getLayoutParams();
            lp.width = expandArg.mWidth;
            mFlHead.setLayoutParams(lp);
        }
        Animator.AnimatorListener animatorListener = null;
        final int alphaDura = 150;
        if (mode != ViewMode.BUBBLE_NORMAL) {
            animatorListener = new Animator.AnimatorListener() {
                public void onAnimationStart(Animator animation) {

                }

                public void onAnimationEnd(Animator animation) {
                    target.setVisibility(View.GONE);
                    View showingView = null;
                    if (mode == ViewMode.BUBBLE_EDIT) {
                        mllHeadExpandDetail.getLayoutParams().width = mExpandDetailWidth;
                        showingView = mllHeadExpandDetail;
                    } else {
                        mllHeadExpandSearch.getLayoutParams().width = mExpandSearchWidth;
                        showingView = mllHeadExpandSearch;
                    }
                    showingView.setVisibility(View.VISIBLE);
                    if (mode == ViewMode.BUBBLE_SEARCH) {
                        //sometimes listview or it's child take away the focus,
                        // lead to bubbleSearchEditText can't get focus
                        if (mListView.isFocusable()) {
                            mListView.setFocusable(false);
                        }
                        mBubbleSearchEditText.requestFocus();
                        Utils.callInputMethod(mBubbleSearchEditText);
                    }
                    refreshOptEnable();
                    if (needAnim) {
                        Anim anim = new Anim(showingView, Anim.TRANSPARENT, alphaDura, Anim.CUBIC_OUT, Anim.INVISIBLE, Anim.VISIBLE);
                        anim.start();
                    } else {
                        showingView.setAlpha(1.0f);
                    }
                }

                public void onAnimationCancel(Animator animation) {

                }

                public void onAnimationRepeat(Animator animation) {

                }
            };
        } else {
            animatorListener = new Animator.AnimatorListener() {
                public void onAnimationStart(Animator animation) {

                }

                public void onAnimationEnd(Animator animation) {
                    if (isFocusSearch()) {
                        Utils.hideInputMethod(mView);
                    }
                    mllHeadExpand.getLayoutParams().width = mExpandWidth;
                    mllHeadExpand.setVisibility(View.VISIBLE);
                    mllHeadExpandDetail.setVisibility(View.GONE);
                    mllHeadExpandSearch.setVisibility(View.GONE);
                    if (needAnim) {
                        Anim anim = new Anim(mllHeadExpand, Anim.TRANSPARENT, alphaDura, Anim.CUBIC_OUT, Anim.INVISIBLE, Anim.VISIBLE);
                        anim.start();
                    } else {
                        mllHeadExpand.setAlpha(1.0f);
                    }
                    mBubbleSearchEditText.setText("");
                }

                public void onAnimationCancel(Animator animation) {

                }

                public void onAnimationRepeat(Animator animation) {

                }
            };
        }
        if (needAnim) {
            valueAnimator.addListener(animatorListener);
            valueAnimator.start();
        } else {
            animatorListener.onAnimationEnd(null);
        }
        mListView.mBubbleAdapter.toMode(mode, needAnim);
    }

    public int getMode() {
        return mMode;
    }

    public void toNoneEdit() {
        mllHeadExpand.getLayoutParams().width = mExpandWidth;
        mllHeadExpand.setVisibility(View.VISIBLE);
        mllHeadExpandDetail.setVisibility(View.GONE);
        mFlHead.getLayoutParams().width = mExpandWidth;
        mllHeadExpand.setAlpha(1.0f);
    }

    private void setViewEnable(View view, boolean enable) {
        ViewUtils.setViewEnable(view, enable);
    }

    public void setInputEnable(boolean enable) {
        setViewEnable(mIvAdd, enable);
    }

    public void onClick(View v) {
        if (StatusManager.isBubbleDragging() || StatusManager.isBubbleRefreshing()) {
            LOG.d("not support click while draging or refreshing global bubbles");
            return;
        }
        switch (v.getId()) {
            case R.id.tv_cancel:
                toMode(ViewMode.BUBBLE_NORMAL, true);
                break;
            case R.id.iv_head_search:
                toMode(ViewMode.BUBBLE_SEARCH, true);
                Tracker.onEvent("A420023");
                break;
            case R.id.iv_todo_over:
                mListView.switchShowHideTodoOver();
                break;
            case R.id.iv_head_edit:
                Tracker.onEvent(BubbleTrackerID.BUBBLE_OPT_SETTING);
                toEdit(true, true);
                break;
            case R.id.tv_over:
                toEdit(false, true);
                break;
            case R.id.iv_head_open:
                if (mListView.isEmpty()) {
                    log.info("nothing to expand !");
                    return;
                }
                if (mNeedExpand) {
                    Tracker.onEvent(BubbleTrackerID.BUBBLE_OPT_OPEN);
                    mListView.mBubbleAdapter.openAll(mListView);
                } else {
                    mListView.mBubbleAdapter.closeAll(mListView, null);
                }
                updateExpandStatus();
                break;
            case R.id.iv_head_hide:
                mListView.hideBubbleListView();
                Tracker.onEvent("A420024");
                break;
            case R.id.cb_select_all:
                mListView.mBubbleAdapter.selectAll(mCbSelectAll.isChecked());
                Tracker.onEvent("A420027");
                break;
            case R.id.iv_bubble_share:
                List<BubbleItem> selectedList = mListView.mBubbleAdapter.getSelectedBubbles();
                if (selectedList == null || selectedList.size() == 0) {
                    return;
                }
                Context context = mListView.getContext();
                if (GlobalBubbleUtils.isBubbleListTooLarge(GlobalBubbleUtils.getBubbleBundle(selectedList))) {
                    GlobalBubbleUtils.showSystemToast(context, context.getResources().getString(R.string.drag_max_limit_hint_text), Toast.LENGTH_SHORT);
                    return;
                }
                Boolean[] isAttachments = isOnlyAttachmentsBubble(selectedList);
                if (isAttachments[0]) {
                    GlobalBubbleUtils.showSystemToast(context,
                            context.getString(R.string.bubble_share_tip), Toast.LENGTH_SHORT);
                }

                if (isAttachments[1]) {
                    GlobalBubbleUtils.shareToApps(context, selectedList);
                    toEdit(false, true);
                }
                Tracker.onEvent("A420025");
                break;
            case R.id.iv_trash:
                mListView.mBubbleAdapter.selectToDelete(new BubbleAdapter.DeleteAllListener() {
                    @Override
                    public void deleteListener() {
                        toMode(ViewMode.BUBBLE_NORMAL, true);
                    }
                });
                Tracker.onEvent("A420026");
                break;
            case R.id.iv_head_add:
                addNewItem();
                break;
            case R.id.iv_head_filtrate:
                showHeadFiltrate();
                break;
        }
    }

    private void showHeadFiltrate() {
        if (mFiltrateSetting != null) {
            int loc[] = new int[2];
            mllHeadExpand.getLocationInWindow(loc);
            mFiltrateSetting.showSettingWindow(mIvFiltrate, loc[0] + mllHeadExpand.getWidth());
        }
    }

    public void hideHeadFiltrate() {
        if (mFiltrateSetting != null) {
            mFiltrateSetting.hideSettingWindow();
        }
    }

    public void updateFiltrateView(int color) {
        if (mIvFiltrate != null) {
            int resId = FiltrateSetting.getFiltrateDrawableRes(color);
            if (resId > 0) {
                mIvFiltrate.setImageResource(resId);
                mListView.updateFiltrateStatus(resId);
            }
        }
    }

    private void updateFiltrateState() {
        if (mFiltrateSetting != null) {
            if (mFiltrateSetting.getFiltrateColor() == GlobalBubble.COLOR_SHARE
                    && !SyncProcessor.canShare(false)) {
                mFiltrateSetting.restoreFiltrateColor();
            }
            updateFiltrateState(mFiltrateSetting.getFiltrateColor(), false);
        }
    }

    private void updateFiltrateState(int color, boolean anim) {
        updateFiltrateView(color);
        updateTodoOverStatus();
        mListView.updateColorFilter(color, anim);
    }

    public void addNewItem() {
        if (StatusManager.getStatus(StatusManager.BUBBLE_DELETING)) {
            log.error("still playing animation of inflate a new bubble");
            return;
        }
        log.info("inflate a new bubble");
        Tracker.onEvent(BubbleTrackerID.BUBBLE_OPT_INFLATE);
        setInputEnable(false);
        GlobalBubble gb = new GlobalBubble();
        gb.setType(GlobalBubble.TYPE_TEXT);
        List<BubbleItem> lists = new ArrayList<BubbleItem>();
        int color = mFiltrateSetting.getFiltrateColor();
        if (color == FiltrateSetting.FILTRATE_ALL) {
            color = Constants.DEFAULT_BUBBLE_COLOR;
        }
        BubbleItem bubbleItem = new BubbleItem(gb, 0, color);
        if (GlobalBubble.COLOR_SHARE == color) {
            SyncShareManager.INSTANCE.refreshInvitationListCycled();
            final long userId = SyncManager.getCloudAccountUserId(IdeaPillsApp.getInstance());
            List<Long> addParticipantIds = null;
            if (userId > 0 && SyncManager.syncEnable(IdeaPillsApp.getInstance())) {
                addParticipantIds = SyncShareUtils.findParticipantIdsForAdd(userId, SyncShareManager.INSTANCE.getCachedInvitationList());
                if (addParticipantIds != null) {
                    if (!addParticipantIds.isEmpty()) {
                        bubbleItem.changeShareStatusToAdd(addParticipantIds);
                    } else {
                        SyncShareInvitation invitation = SyncShareManager.INSTANCE.getInvitation(userId);
                        if (invitation != null && (invitation.inviteStatus == SyncShareInvitation.INVITE_ACCEPT ||
                                invitation.inviteStatus == SyncShareInvitation.INVITE_START)) {
                            bubbleItem.changeShareStatusToWaitInvitation(invitation.invitee.id);
                        }
                    }
                }
            }
        }
        lists.add(bubbleItem);
        lists.get(0).setNeedInput(true);
        GlobalBubbleManager.getInstance().addBubbleItems(lists);
    }

    public void getLeftAndRight(int[] out) {
        out[0] = mFlHead.getLeft();
        out[1] = mFlHead.getRight();
    }

    public Anim getPlayhideViewAnim() {
        Anim move = new Anim(mView, Anim.TRANSLATE, ANIMDURATION, Anim.CUBIC_OUT, new Vector3f(mView.getTranslationX(), 0.0f), new Vector3f(mView.getTranslateXMax(), 0));
        return move;
    }

    public int getHeadTop() {
        return mView == null ? 0 : mView.getTop();
    }

    public void hideHeadView(boolean withAnim) {
        if (!StatusManager.getStatus(StatusManager.BUBBLE_DRAGGING)) {
            toEdit(false, false);
        }
        mListView.mPullViewGroup.setTranslateYinHideState(mTranslateYinHideState);
        if (mView.getHeight() > 0) {
            mListView.mPullViewGroup.showStableState(mView.getTop(), (int) (mView.getTop()), withAnim);
        }
        mListView.setVisibility(View.GONE);
        mView.setTranslationX(mView.getTranslateXMax());
        // broadcast the bubble list hide event.
        LocalBroadcastManager.getInstance(mView.getContext()).sendBroadcast(new Intent(Constants.LOCAL_BROADCAST_ACTION_BUBBLE_LIST_HIDE));
        GlobalInvitationAction.getInstance().hideInvitationDialog();
    }

    public void showHeadView() {
        mView.setVisibility(View.INVISIBLE);
        mListView.mPullViewGroup.setVisibility(View.VISIBLE);
        AnimListener listener = new SimpleAnimListener() {
            public void onStart() {

            }

            public void onComplete(int type) {
                mListView.mPullViewGroup.bringToFront();
                int translateY = mView.getTop() - mListView.mPullViewGroup.getPullTop();
                AnimListener listener1 = new SimpleAnimListener() {

                    public void onComplete(int type) {
                        mTranslateYAnim = null;
                        completeHeadViewNoAnim();
                    }
                };
                mTranslateYAnim = new Anim(mListView.mPullViewGroup, Anim.TRANSLATE, 250, Anim.CUBIC_OUT, Anim.ZERO, new Vector3f(0, translateY));
                if (mTranslateYAnim.isEmpty()) {
                    listener1.onComplete(0);
                } else {
                    mTranslateYAnim.setListener(listener1);
                    mTranslateYAnim.start();
                }
            }
        };
        mListView.mPullViewGroup.checkToSwitch();
        if (BubbleController.getInstance().isInPptContext(mListView.getContext())) {
            completeHeadViewNoAnim();
        } else {
            mListView.mPullViewGroup.moveTo(listener);
        }
    }

    public boolean hasAnimRuning() {
        if (mTranslateYAnim != null && mTranslateYAnim.isRunning() || mListView.mPullViewGroup.hasAnimRuning()) {
            return true;
        }
        return false;
    }

    public void completeHeadViewNoAnim() {
        mView.setVisibility(View.VISIBLE);
        //if need hide bubble, should't  update dimBackground
        if (mListView.getVisibility() != View.GONE) {
            mView.setTranslationX(0.0f);
        }
        mView.setAlpha(1.0f);
        mllHeadExpand.setAlpha(1.0f);
        mListView.mPullViewGroup.setVisibleGone();
    }

    public void refreshOptEnable() {
        if (mListView.mBubbleAdapter.isNoneSelected()) {
            setOptEnable(false);
        } else {
            setOptEnable(true);
        }
        if (mListView.mBubbleAdapter.isAllSelected()) {
            mCbSelectAll.setChecked(true);
        } else {
            mCbSelectAll.setChecked(false);
        }
    }

    private void setOptEnable(boolean enable) {
        setViewEnable(mIvTrash, enable);
        setViewEnable(mIvShare, enable);
    }

    public boolean isMovingHorizontal() {
        return mView.isMovingHorizontal();
    }

    public void clearMovingState() {
        mView.clearMovingState();
    }

    public void onConfigurationChanged(Configuration newConfig) {
        Locale newLocale = newConfig.locale;
        if(newLocale == null){
            return;
        }
        if (!newLocale.equals(mLastLocale)){
            // language changed
            UIHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mTvOver.setText(mTvOver.getContext().getString(R.string.bubble_over));
                    mTvCancel.setText(mTvCancel.getContext().getString(R.string.bubble_cancel));
                    mCbSelectAll.setText(mCbSelectAll.getContext().getString(R.string.bubble_select_all));
                    FrameLayout.LayoutParams lpa = (FrameLayout.LayoutParams) mllHeadExpandDetail
                            .getLayoutParams();
                    lpa.width = FrameLayout.LayoutParams.WRAP_CONTENT;
                    mllHeadExpandDetail.setLayoutParams(lpa);
                    // re-measure the headexpanddetailview when language
                    // changed
                    mExpandDetailWidth = ViewUtils
                            .getSupposeWidth(mllHeadExpandDetail);
                }
            }, 200L);
            mLastLocale = newLocale;
        }
    }

    public void setTranslateYinHideState(float translateYinHideState) {
        mTranslateYinHideState = translateYinHideState;
    }

    private FiltrateSetting.OnFiltrateChangeListener mFiltrateChangeListener = new FiltrateSetting.OnFiltrateChangeListener() {
        @Override
        public void onFiltrateChange(int color) {
            updateFiltrateState(color, true);
        }
    };

    private Boolean[] isOnlyAttachmentsBubble(List<BubbleItem> bubbleItemList) {
        Boolean attachmentsBubble[] = new Boolean[]{false, false};
        if (bubbleItemList == null || bubbleItemList.size() <= 0) {
            return attachmentsBubble;
        }
        for (BubbleItem item : bubbleItemList) {
            if (item.haveAttachments()) {
                attachmentsBubble[0] = true;
            }
            if (!TextUtils.isEmpty(item.getText())) {
                attachmentsBubble[1] = true;
            }

            if (attachmentsBubble[0] && attachmentsBubble[1]) {
                break;
            }
        }
        return attachmentsBubble;
    }
}