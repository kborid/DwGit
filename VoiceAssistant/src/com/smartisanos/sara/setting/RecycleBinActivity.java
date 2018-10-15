
package com.smartisanos.sara.setting;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.Parcelable;
import android.service.onestep.GlobalBubbleAttach;
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.ViewStub;
import android.view.animation.DecelerateInterpolator;
import android.view.inputmethod.InputMethodManager;
import android.widget.AbsListView;
import android.widget.Adapter;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import com.smartisanos.sanbox.utils.SaraTracker;
import com.smartisanos.sara.R;
import com.smartisanos.sara.storage.BubbleDataRepository;
import com.smartisanos.sara.util.ClickUtil;
import com.smartisanos.sara.widget.pinnedHeadList.HeadersListView;
import com.smartisanos.sara.setting.recycle.RecycleBinSearchResultAdapter;
import com.smartisanos.sara.setting.recycle.RecycleItem;
import com.smartisanos.sara.setting.recycle.RecycleItemListAdapter;
import com.smartisanos.sara.setting.recycle.RecycleItemListener;
import com.smartisanos.sara.util.LogUtils;
import com.smartisanos.sara.util.SaraConstant;
import com.smartisanos.sara.util.SaraUtils;
import com.smartisanos.sara.util.ToastUtil;
import com.smartisanos.sara.widget.HorizontalScrollListView;
import com.smartisanos.sara.widget.MultiDeleteAnimation;
import com.smartisanos.sara.widget.QuickListView;
import com.smartisanos.sara.widget.SlideListView;
import com.smartisanos.sara.BaseActivity;
import android.service.onestep.GlobalBubble;
import smartisanos.widget.SearchBar;

import android.widget.Toast;

import smartisanos.widget.Title;
import smartisanos.app.MenuDialog;
import smartisanos.app.SmartisanProgressDialog;
import smartisanos.widget.SmartisanBlankView;

public class RecycleBinActivity extends BaseActivity implements View.OnClickListener,
        AdapterView.OnItemClickListener,
        AbsListView.OnScrollListener, AdapterView.OnItemLongClickListener {

    private static final String TAG = "RecycleBinActivity";

    private boolean DBG = false;
    private static final int COUNT_IN_PAGE = 14;
    private RelativeLayout mBubbleBody;
    private ViewStub mRecycleBinEmptyViewStub;
    private RelativeLayout mRecycleBinEmptyView;
    private SmartisanBlankView mRecycleBinEmpty;

    private Title mTitle;

    private View mFragment;
    private RelativeLayout mBubblesFrameView;
    private HeadersListView mBubbleHeadersListView;

    private ImageView mTitleBarShadow;

    private SearchBar mSearchbarView;
    private RelativeLayout mEditBubblesBarView;
    private CheckBox mSelectAllCheckBox;
    private TextView mOptionTextView;

    private LinearLayout mBottomControl;
    private Button mDeleteBtn;
    private Button mRestoreBtn;

    private RelativeLayout mSeachResultView;
    private QuickListView mSearchResultList;
    private TextView mSearchEmptyView;
    private TextView mSeachResultBG;

    private MenuDialog mMenuDialog;

    private RecycleItemListAdapter mRecycleItemListAdapter;
    private RecycleBinSearchResultAdapter mSearchResultAdapter;

    private DecelerateInterpolator mDecelerateInterpolator;
    private long mDuration = 200L;

    private boolean mIsEdit = false;

    private SelectionSet<RecycleItem> mSelectedSet = new SelectionSet<RecycleItem>();

    private HandlerThread mBackgroundThread;
    private Handler mBackgroundHandler;
    private Toast mToast;
    private DeleteMultipleTask mDeleteMultiTask;
    private int mInsertBubble2GlobalNum;
    public static final int BUBBLE_RIRECTION_RECYCLE = 0;
    public static final int BUBBLE_RIRECTION_USED = 1;
    public static final int BUBBLE_RIRECTION_HANDLED = 2;
    public static int sBubbleDirection = BUBBLE_RIRECTION_RECYCLE; // 0 is recycle, 1 is used
    public static final int SHORTCUTS_DEFAULT = 0;
    public static final int SHORTCUTS_HANDLED = 1;
    public static final int SHORTCUTS_HANDLED_OK = 2;
    public int mShortcutsDirection = SHORTCUTS_DEFAULT; // 1 is handle, 2 is handle_ok
    private RecycleBinActivity.SearchBarListener mSearchBarListener = new RecycleBinActivity.SearchBarListener();
    private Handler mHandler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            switch (msg.what) {
                case NOTIFY_SET_SEARCH_RESULT: {
                    ArrayList<RecycleItem> bubblesList = (ArrayList<RecycleItem>) msg.obj;
                    // close scroll right opened item
                    mSearchResultList.restoreScrollState(false);
                    mSearchResultAdapter.setList(bubblesList, mSearchKey);

                    if (mSearchResultList.getVisibility() == View.GONE) {
                        mSearchResultList.setVisibility(View.VISIBLE);
                    }

                    if (bubblesList.size() == 0) {
                        if (null == mSearchResultList.getEmptyView()) {
                            mSearchResultList.setEmptyView(mSearchEmptyView);
                        }
                    }
                    break;
                }
                case NOTIFY_SET_MAIN_LIST_RESULT: {
                    if (mDeleteMultiTask == null || !mDeleteMultiTask.running) {
                        List<RecycleItem> bubblesList = (ArrayList<RecycleItem>) msg.obj;
                        reloadMainAdapter(bubblesList);
                    }
                    break;
                }
                case NOTIFY_SET_DATA_CHANGE:
                    if (!isFinishing()) {
                        if (mSearchResultList.getVisibility() == View.VISIBLE) {
                            mSearchResultAdapter.notifyDataSetChanged();
                        } else {
                            mRecycleItemListAdapter.notifyDataSetChanged();
                        }
                    }
                    break;
                case SHOW_LOAD_PROGRESS:
                    showProgressDialog();
                    break;
            }

            return true;
        }
    });


    public class SearchBarListener extends SearchBar.ListenerAdapter {
        @Override
        public void onClickCancelButton() {
            if (!isSearch() || mSearchbarView.isPlayingAnimation()){ // if animation isn't over, will be return in this callback
                return;
            }
            endBubbleAnimation();
        }

        @Override
        public void onClickSearchEdit() {
            if (isSearch() || mSearchbarView.isPlayingAnimation()) {
                return;
            }
            startBubbleAnimation();
        }

        @Override
        public void searchText(String trim) {
            onQueryTextChange(trim);
        }

    }

    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (SaraConstant.ACTION_DELETE_GLOBAL_BUBBLE.equals(action) || SaraConstant.ACTION_TODO_CHANGE_GLOBAL_BUBBLE.equals(action)) {
                triggerLoad(false);
            }
        }
    };
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.recycle_bin);
        initView();
        initTitleBar();
        setCaptionTitleToPillInExtDisplay();
    }

    public void initView() {
        mBubblesFrameView = (RelativeLayout) findViewById(R.id.bubbles_view);
        mBubbleBody = (RelativeLayout) findViewById(R.id.bubble_body);

        mBubbleHeadersListView = (HeadersListView) findViewById(R.id.main_list);
        mBubbleHeadersListView.setOnItemClickListener(this);
        mBubbleHeadersListView.setSlideListener(mSlideListener);
        mBubbleHeadersListView.setAutoClose(false);
        mBubbleHeadersListView.setOnItemLongClickListener(this);
        mBubbleHeadersListView.setOnScrollListener(this);
        mRecycleBinEmptyViewStub = (ViewStub) findViewById(R.id.listview_emptyview_stub);

        mSelectAllCheckBox = (CheckBox) findViewById(R.id.checkbox_select_all);
        mSelectAllCheckBox.setOnClickListener(this);
        mOptionTextView = (TextView) findViewById(R.id.option_title);
        mEditBubblesBarView = (RelativeLayout) findViewById(R.id.option_bar);

        mBottomControl = (LinearLayout) findViewById(R.id.recycle_list_control);
        mDeleteBtn = (Button) findViewById(R.id.recycle_list_control_delete);
        mDeleteBtn.setOnClickListener(this);
        mRestoreBtn = (Button) findViewById(R.id.recycle_list_control_restore);
        mRestoreBtn.setOnClickListener(this);

        mTitleBarShadow = (ImageView) findViewById(R.id.title_bar_shadow);

        mSearchbarView = (SearchBar) findViewById(R.id.bubble_body_search);
        mSearchbarView.setListener(mSearchBarListener);
        mSeachResultView = (RelativeLayout) findViewById(R.id.seachresultview);
        mSearchResultList = (QuickListView) findViewById(R.id.search_listview);
        mSearchResultList.setAutoClose(false);
        mSearchResultList.setOnScrollListener(this);
        mSearchResultList.setOnItemClickListener(this);
        mSearchResultList.setOnItemLongClickListener(this);

        mSearchResultAdapter = new RecycleBinSearchResultAdapter(this, null, null);
        mSearchResultAdapter.setRecycleItemListener(mRecycleItemListener);
        mSearchResultList.setAdapter(mSearchResultAdapter);
        mSearchResultList.setOnScrollStateChangeListener(mSearchResultAdapter);

        mSearchEmptyView = (TextView) findViewById(R.id.search_empty_view);
        mSearchEmptyView.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                // Intercept click events, when the cursor record is empty.
            }
        });
        mSeachResultBG = (TextView) findViewById(R.id.sv_background);
        mSeachResultBG.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View arg0) {
                if(mSearchbarView != null) {
                    mSearchbarView.onClickCancelView(true);
                }
            }
        });

        mBackgroundThread = new HandlerThread("RecycleBin Worker");
        mBackgroundThread.start();
        mBackgroundHandler = new Handler(mBackgroundThread.getLooper(),mRecyclebinCallback);
        IntentFilter filter = new IntentFilter();
        filter.addAction(SaraConstant.ACTION_DELETE_GLOBAL_BUBBLE);
        filter.addAction(SaraConstant.ACTION_TODO_CHANGE_GLOBAL_BUBBLE);
        LocalBroadcastManager.getInstance(getApplicationContext()).registerReceiver(mReceiver, filter);
    }

    public void initTitleBar() {
        Intent intent = getIntent();
        sBubbleDirection = intent.getIntExtra(SaraConstant.BUBBLE_DIRECTION, BUBBLE_RIRECTION_RECYCLE);
        mShortcutsDirection = intent.getIntExtra(SaraConstant.SHORTCUTS_DIRECTION, SHORTCUTS_DEFAULT);
        mTitle = (Title) findViewById(R.id.titlebar);
        if (sBubbleDirection == BUBBLE_RIRECTION_RECYCLE) {
            mTitle.setTitle(R.string.recycle_bin);
            setTitle(R.string.recycle_bin);
        } else if (sBubbleDirection == BUBBLE_RIRECTION_USED){
            mTitle.setTitle(R.string.handled);
            setTitle(R.string.handled);
        } else {
            mTitle.setTitle(R.string.handled_ok);
            setTitle(R.string.handled_ok);
        }
        boolean isFromSearch = intent.hasExtra("from_search");
        if (isFromSearch) {
            mTitle.setBackButtonTextByIntent(intent);
            mTitle.setBackBtnArrowVisible(false);
        } else {
            if (mShortcutsDirection == SHORTCUTS_HANDLED || mShortcutsDirection == SHORTCUTS_HANDLED_OK) {
                mTitle.setBackButtonText(R.string.btn_back);
            } else {
                mTitle.setBackButtonText(R.string.idea_pills_beta);
            }
            mTitle.setBackBtnArrowVisible(true);
        }
        mTitle.setBackButtonTextGravity(Gravity.CENTER);
        mTitle.setBackButtonListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });

        mTitle.getOkButton().setVisibility(View.VISIBLE);
        initEditStyle();

        mTitle.setOkButtonListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mIsEdit) {
                    processEditBubbles(false, true);
                    initEditStyle();
                } else {
                    if (mSearchbarView.isSearchMode()){
                        return;
                    }
                    processEditBubbles(true, true);
                    initCancelEditStyle();
                }

            }
        });

        setTitleByIntent(mTitle);
    }

    private void initEditStyle() {
        mTitle.setOkButtonText(R.string.btn_edit);
        mTitle.setOkButtonTextColor(getResources().getColor(R.color.blue_shadow_color));
        mTitle.setOkButtonBackground(getResources().getDrawable(R.drawable.bule_button), false);
    }

    private void initCancelEditStyle() {
        mTitle.setOkButtonText(R.string.cancel);
        mTitle.setOkButtonTextColor(getResources().getColor(R.color.top_button_text_color_grey));
        mTitle.setOkButtonBackground(getResources().getDrawable(R.drawable.normal_button), false);
    }

    public int getBubbleDirection() {
        return sBubbleDirection;
    }
    private synchronized void reloadMainAdapter(List<RecycleItem> bubblesList) {
        if (isFinishing()) {
            return;
        }

        if (bubblesList == null || bubblesList.size() == 0) {
            mTitle.getOkButton().setEnabled(false);
            mTitle.getOkButton().setAlpha(0.5f);
        } else {
            mTitle.getOkButton().setEnabled(true);
            mTitle.getOkButton().setAlpha(1f);
        }

        if (mIsEdit && mSelectedSet.size() > 0) {
            Iterator<RecycleItem> iterator = mSelectedSet.iterator();
            while (iterator.hasNext()) {
                if (!bubblesList.contains(iterator.next())) {
                    iterator.remove();
                }
            }
        }

        hideProgressDiaLogUtils();


        if (bubblesList != null) {
            LogUtils.d(TAG,
                    mRecycleItemListAdapter + "NOTIFY_SET_MAIN_LIST_RESULT = " + bubblesList.size());
        }
        if (mRecycleItemListAdapter == null) {
            mRecycleItemListAdapter = new RecycleItemListAdapter(this, bubblesList);
            mBubbleHeadersListView.setAdapter(mRecycleItemListAdapter);
            mBubbleHeadersListView.setOnScrollStateChangeListener(mRecycleItemListAdapter);
            mRecycleItemListAdapter.setRecycleItemListener(mRecycleItemListener);
        } else {
            mRecycleItemListAdapter.setList(bubblesList);
        }
        if (bubblesList == null || bubblesList.size() <= 0) {
            setEmptyView(mBubbleHeadersListView, true);
        } else {
            setEmptyView(mBubbleHeadersListView, false);
        }
    }

    @Override
    public void onScroll(AbsListView arg0, int arg1, int arg2, int arg3) {

    }

    @Override
    public void onScrollStateChanged(AbsListView arg0, int arg1) {
        switch (arg1) {
            case AbsListView.OnScrollListener.SCROLL_STATE_TOUCH_SCROLL:
                hideSoftKeyboard();
                break;
            case AbsListView.OnScrollListener.SCROLL_STATE_IDLE:
                if (!isFinishing()) {
                    mBackgroundHandler.removeMessages(NOTIFY_LOAD_ATTACHMENT);
                    Message message = new Message();
                    if (mSearchResultList.getVisibility() == View.VISIBLE) {
                        int start = mSearchResultList.getFirstVisiblePosition();
                        message.obj = mSearchResultAdapter.getList();
                        message.arg1 = start;
                        message.arg2 = start + COUNT_IN_PAGE;
                    } else {
                        if (mRecycleItemListAdapter == null) {
                            break;
                        }
                        int start = mBubbleHeadersListView.getFirstVisiblePosition();
                        message.obj = mRecycleItemListAdapter.getList();
                        message.arg1 = start;
                        message.arg2 = start + COUNT_IN_PAGE;
                    }
                    message.what = NOTIFY_LOAD_ATTACHMENT;
                    mBackgroundHandler.sendMessage(message);
                }
                break;
        }
    }

    private Handler.Callback mRecyclebinCallback = new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            switch (msg.what) {
                case NOTIFY_LOAD_RECYCLE_BIN: {
                    List<RecycleItem> bubblesList = loadRecycleItems();
                    loadRecycleAttachments(bubblesList, 0, COUNT_IN_PAGE);
                    Message message = new Message();
                    message.what = NOTIFY_SET_MAIN_LIST_RESULT;
                    message.obj = bubblesList;
                    mHandler.sendMessage(message);
                    break;
                }
                case NOTIFY_LOAD_ATTACHMENT: {
                    List<RecycleItem> bubblesList = (ArrayList<RecycleItem>) msg.obj;
                    if (loadRecycleAttachments(bubblesList, msg.arg1, msg.arg2)) {
                        mHandler.sendEmptyMessage(NOTIFY_SET_DATA_CHANGE);
                    }
                }
            }
            return true;
        }
    };

    private AnimatorSet mStartSearchAnimatorSet;
    private boolean mIsSearch = false;
    private boolean mIsSearchAnimationing = false;

    private String mSearchKey;
    private SearchThread mSearchThread;

    private static final int NOTIFY_SET_SEARCH_RESULT = 2004;
    private static final int NOTIFY_SET_MAIN_LIST_RESULT = 2005;
    private static final int SHOW_LOAD_PROGRESS = 2006;
    private static final int NOTIFY_SET_DATA_CHANGE = 2007;

    private static final int NOTIFY_LOAD_RECYCLE_BIN = 3004;
    private static final int NOTIFY_LOAD_ATTACHMENT = 3005;

    private void cancelSearch() {
        if (mSearchbarView != null && mSearchbarView.isSearchMode()) {
            endBubbleAnimation();
        }
    }

    public boolean backToTopState() {
        boolean result = true;
        if (isSearch() && !isSearchAnimationing()) {
            cancelSearch();
            result = false;
        } else if (mIsEdit) {
            if (!mBubbleHeadersListView.isSelecting()) {
                processEditBubbles(false, true);
            }
            result = false;
        }

        return result;
    }

    public boolean isSearch() {
        return mIsSearch;
    }

    private boolean isSearchAnimationing() {
        return mIsSearchAnimationing;
    }

    @Override
    public void onResume() {
        super.onResume();
        triggerLoad(true);
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onStop() {
        super.onStop();

        if (mSearchbarView != null) {
            cancelSearchWithoutAnimation();
        }

        if (mProgress != null) {
            mProgress.dismiss();
        }

        if (mMenuDialog != null && mMenuDialog.isShowing()) {
            mMenuDialog.dismiss();
        }
        if (mIsEdit) {
            processEditBubbles(mIsEdit, false);
        }
    }

    public void cancelSearchWithoutAnimation() {
        if (mSearchbarView.isSearchMode() ) {
             endBubbleAnimationWithoutAnimation();
            mSearchbarView.onClickCancelView(false);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mBackgroundHandler != null) {
            mBackgroundHandler.removeCallbacksAndMessages(null);
        }
        if(mHandler != null){
            mHandler.removeCallbacksAndMessages(null);
        }
        if (mBackgroundThread != null) {
            mBackgroundThread.quitSafely();
        }
        LocalBroadcastManager.getInstance(getApplicationContext()).unregisterReceiver(mReceiver);
        LogUtils.i("onDestroy");
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.recycle_list_control_delete: {
                if (!ClickUtil.isFastClick()) {
                    showMultiDeletionDialog(R.id.recycle_list_control_delete);
                    if (sBubbleDirection == BUBBLE_RIRECTION_USED) {
                        SaraTracker.onEvent("A130024", "type", 0);
                    } else if (sBubbleDirection == BUBBLE_RIRECTION_RECYCLE) {
                        SaraTracker.onEvent("A130025", "type", 0);
                    }
                }
                break;
            }
            case R.id.recycle_list_control_restore: {
                if (!ClickUtil.isFastClick()) {
                    if (SaraUtils.SUPPORT_MAX_BUBBLE_COUNT) {
                        int selectSetSize = mSelectedSet.size();
                        mInsertBubble2GlobalNum = BubbleDataRepository.getCanInsertGlobalBubbleNum(this, selectSetSize);
                          if (selectSetSize > mInsertBubble2GlobalNum){
                              ToastUtil.showToast(R.string.max_global_bubbles_tips);
                          } else {
                              doRemoveAction(true);
                          }
                    } else {
                         doRemoveAction(true);
                    }
                    if (sBubbleDirection == BUBBLE_RIRECTION_USED) {
                        SaraTracker.onEvent("A130024", "type", 1);
                    } else if (sBubbleDirection == BUBBLE_RIRECTION_RECYCLE) {
                        SaraTracker.onEvent("A130025", "type", 1);
                    }
                }
                break;
            }
            case R.id.checkbox_select_all: {
                if (mSelectAllCheckBox.isChecked()) {
                    List<RecycleItem> list = mRecycleItemListAdapter.getList();
                    mSelectedSet.addAll(list);
                    if (SaraUtils.SUPPORT_MAX_BUBBLE_COUNT) {
                        mInsertBubble2GlobalNum = BubbleDataRepository.getCanInsertGlobalBubbleNum(this, list.size());
                        if (list.size() > mInsertBubble2GlobalNum) {
                            mRestoreBtn.setAlpha(0.5F);
                        } else {
                            mRestoreBtn.setAlpha(1);
                        }
                    }
                } else {
                    mSelectedSet.clear();
                }
                refreshAdapter(true);
                break;
            }
            default:
                break;
        }
    }

    private void showMultiDeletionDialog(final int id) {
        mMenuDialog = new MenuDialog(this);

        if (id == R.id.recycle_list_control_delete) {
            mMenuDialog.setTitle(getResources().getQuantityString(
                    R.plurals.delete_recycle_bubbles_dialog_title, mSelectedSet.size(),
                    mSelectedSet.size()));
            mMenuDialog.setPositiveButton(R.string.btn_delete_permanently_confirm,
                    new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            doRemoveAction(false);
                        }
                    });
            mMenuDialog.setPositiveRedBg(true);
        }

        mMenuDialog.show();
    }

    private void showSingleDeletionDialog(final View itemView, final int id,
            final RecycleItem deletedItem) {

        if (id == R.id.umb_clear) {
            mMenuDialog = new MenuDialog(this);
            mMenuDialog
                    .setTitle(getString(R.string.delete_single_recycle_bubble_dialog_title));
            mMenuDialog.setPositiveButton(R.string.btn_delete_permanently_confirm,
                    new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            doSingleRemoveAction(itemView, deletedItem, false);
                        }
                    });
            mMenuDialog.setPositiveRedBg(true);
            mMenuDialog.show();
            mMenuDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                @Override
                public void onDismiss(DialogInterface dialog) {
                    if (isSearch()) {
                        mSearchResultList.restoreScrollState(true);
                    } else {
                        mBubbleHeadersListView.restoreScrollState(true);
                    }

                }
            });
            if (sBubbleDirection == BUBBLE_RIRECTION_USED) {
                SaraTracker.onEvent("A130024", "type", 0);
            } else if (sBubbleDirection == BUBBLE_RIRECTION_RECYCLE) {
                SaraTracker.onEvent("A130025", "type", 0);
            }
        } else {
            if (SaraUtils.SUPPORT_MAX_BUBBLE_COUNT) {
                mInsertBubble2GlobalNum = BubbleDataRepository.getCanInsertGlobalBubbleNum(this,1);
                if (mInsertBubble2GlobalNum >0) {
                    doSingleRemoveAction(itemView, deletedItem, true);
                } else {
                    ToastUtil.showToast(R.string.max_global_bubbles_tips);
                }
            } else {
                doSingleRemoveAction(itemView, deletedItem, true);
            }
            if (sBubbleDirection == BUBBLE_RIRECTION_USED) {
                SaraTracker.onEvent("A130024", "type", 1);
            } else if (sBubbleDirection == BUBBLE_RIRECTION_RECYCLE) {
                SaraTracker.onEvent("A130025", "type", 1);
            }
        }

    }

    private synchronized void doRemoveAction(final boolean restored) {
        if (mSelectedSet.isEmpty() || mRecycleItemListAdapter == null) {
            return;
        }

        List<RecycleItem> items = mRecycleItemListAdapter.getList();
        final ArrayList<RecycleItem> bubbles = (ArrayList<RecycleItem>) ((ArrayList<RecycleItem>) items)
                .clone();

        HashSet<String> deletedHeaderSections = new HashSet<String>();
        final List<RecycleItem> deletedItems = new ArrayList<RecycleItem>();

        ArrayList<Integer> deleted = new ArrayList<Integer>();
        ArrayList<Integer> reserved = new ArrayList<Integer>();
        Iterator<RecycleItem> iterator = bubbles.iterator();
        int count = 0;
        while (iterator.hasNext()) {
            RecycleItem bubble = iterator.next();
            if (mSelectedSet.contains(bubble)) {
                if (restored) {

                }
                deletedItems.add(bubble);
                deleted.add(count);
                if (!deletedHeaderSections.contains(bubble.getSectionHeader(this))) {
                    deletedHeaderSections.add(bubble.getSectionHeader(this));
                }
                iterator.remove();
            } else {
                reserved.add(count);
            }
            count++;
        }

        HashSet<String> temp = new HashSet<String>();
        for (String section : deletedHeaderSections) {
            for (RecycleItem bubble : bubbles) {
                if (TextUtils.equals(bubble.getSectionHeader(this), section)) {
                    temp.add(section);
                }
            }
        }
        deletedHeaderSections.removeAll(temp);
        mBubbleHeadersListView.setDeletedHeaderSections(deletedHeaderSections);

        new MultiDeleteAnimation<HeadersListView>(mBubbleHeadersListView, true) {
            @Override
            public void onMultiDeleteAnimationStart() {
            }

            @Override
            public void onMultiDeleteAnimationEnd() {
                synchronized (RecycleBinActivity.this) {
                    mRecycleItemListAdapter.setList(bubbles);
                    deleteMultipleBubbles(restored, deletedItems);
                }
            }
        }.setResultListSize(bubbles.size())
                .setDeletedHeaderSection(deletedHeaderSections)
                .start(deleted, reserved);
        initEditStyle();
    }

    private void doSingleRemoveAction(final View itemView, final RecycleItem deletedItem,
            final boolean restored) {
        HorizontalScrollListView currentListView;
        if (mIsSearch) {
            currentListView = mSearchResultList;
        } else {
            currentListView = mBubbleHeadersListView;
        }

        if (currentListView != null) {
            currentListView.playDeleteItemAnimation(new Animator.AnimatorListener() {
                @Override
                public void onAnimationStart(Animator animation) {
                }

                @Override
                public void onAnimationEnd(Animator animation) {
                    deleteSingleBubble(itemView, restored, deletedItem);
                }

                @Override
                public void onAnimationCancel(Animator animation) {
                }

                @Override
                public void onAnimationRepeat(Animator animation) {
                }
            });
        }
    }

    private void deleteMultipleBubbles(final boolean restored, final List<RecycleItem> deletedItems) {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                if (restored) {
                    processEditBubbles(false, true, RESTORE_OPERATION, deletedItems);
                } else {
                    processEditBubbles(false, true, DELETE_OPERATION, deletedItems);
                }

            }
        });
    }

    private synchronized void deleteSingleBubble(View itemView, final boolean restored,
            final RecycleItem deletedItem) {
        if (itemView != null) {
            View convertView = itemView.findViewById(R.id.bubble_list_item);
            if (convertView != null) {
                convertView.setTag(null);
            }
        }
        Bundle bunble = new Bundle();
        bunble.putIntArray(SaraConstant.KEY_BUBBLE_IDS, new int[] {deletedItem.bubbleId});
        // restore to bubbles
        if (restored) {
            if (sBubbleDirection == BUBBLE_RIRECTION_USED) {
                BubbleDataRepository.restoreGlobleBubble(this, bunble);
            } else if (sBubbleDirection == BUBBLE_RIRECTION_HANDLED) {
                BubbleDataRepository.restoreLegacyGlobleBubble(this, bunble);
            } else {
                BubbleDataRepository.restoreDeleteGlobleBubble(this, bunble);
            }
        } else {
            String value = "";
            if (sBubbleDirection == BUBBLE_RIRECTION_RECYCLE) {
                value = SaraConstant.DESTROY_TYPE_REMOVED;
            } else if (sBubbleDirection == BUBBLE_RIRECTION_USED) {
                value = SaraConstant.DESTROY_TYPE_USED;
            } else {
                value = SaraConstant.DESTROY_TYPE_LEGACY_USED;
            }

            bunble.putString(SaraConstant.KEY_DESTROY_TYPE, value);
            BubbleDataRepository.destroyGlobleBubble(this, bunble);
            if (deletedItem.bubbleType != GlobalBubble.TYPE_TEXT) {
                new File(deletedItem.bubblePath).delete();
                new File(deletedItem.bubblePath + ".wave").delete();
            }
        }

        if (mIsSearch) {
            List<RecycleItem> searchResults = mSearchResultAdapter.getList();
            if (searchResults != null) {
                searchResults.remove(deletedItem);

                if (searchResults.size() == 0) {
                    if (null == mSearchResultList.getEmptyView()) {
                        mSearchResultList.setEmptyView(mSearchEmptyView);
                    }
                }

                mSearchResultAdapter.notifyDataSetChanged();
            }
        }

        List<RecycleItem> bubblesList = mRecycleItemListAdapter.getList();
        if (bubblesList != null) {
            bubblesList.remove(deletedItem);
            mRecycleItemListAdapter.notifyDataSetChanged();

            if (bubblesList.size() == 0) {
                mTitle.getOkButton().setEnabled(false);
                mTitle.getOkButton().setAlpha(0.5f);
                setEmptyView(mBubbleHeadersListView, true);
            }
        }
    }

    @Override
    public void onItemClick(AdapterView<?> arg0, View arg1, int position, long id) {
        if (!mIsEdit) {
            if (!isSearch()) {
                RecycleItem item = mRecycleItemListAdapter.getItem(position);
                mRecycleItemListAdapter.initDialog(item);
            } else {
                RecycleItem item = mSearchResultAdapter.getItem(position);
                mSearchResultAdapter.initDialog(item);
            }
            if (mMenuDialog != null) {
                mMenuDialog.dismiss();
            }
            return;
        }

        processSelectOnItemClicked(position);
    }

    @Override
    public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long l) {
        if (!mIsEdit && position < parent.getCount()) {
            if (mToast != null) {
                mToast.cancel();
            }
            mToast = Toast.makeText(this, smartisanos.R.string.toast_swipe_right,
                    Toast.LENGTH_SHORT);
            mToast.show();
        }
        return mIsEdit ? false : true;
    }

    private void processSelectOnItemClicked(int position) {
        View child = mBubbleHeadersListView.getChildAt(position
                - mBubbleHeadersListView.getFirstVisiblePosition());
        CheckBox cb = (CheckBox) child.findViewById(R.id.checkBox);
        if (cb == null) {
            return;
        }

        RecycleItem clickedItem = mRecycleItemListAdapter.getItem(position);
        if (mSelectedSet.contains(clickedItem)) {
            mSelectedSet.remove(clickedItem);
            cb.setChecked(false);
        } else {
            mSelectedSet.add(clickedItem);
            cb.setChecked(true);
        }
    }

    private List<RecycleItem> loadRecycleItems() {
        String type = SaraConstant.LIST_TYPE_LEGACY_USED;
        if (sBubbleDirection == BUBBLE_RIRECTION_RECYCLE) {
            type = SaraConstant.LIST_TYPE_REMOVED;
        } else if (sBubbleDirection == BUBBLE_RIRECTION_USED) {
            type = SaraConstant.LIST_TYPE_USED;
        }
        List<RecycleItem> recycleItemList = BubbleDataRepository.getRecycleBubbleList(this, type);
        if (recycleItemList != null && recycleItemList.size() > 1) {
            Collections.sort(recycleItemList, new RecycleItemSortComparator());
        }
        List<RecycleItem> nullRecycleItemList = new ArrayList<RecycleItem>();
        for (RecycleItem item : recycleItemList) {
            if (TextUtils.isEmpty(item.bubbleText)) {
                nullRecycleItemList.add(item);
            }
        }
        if (nullRecycleItemList.size() > 0) {
            List<RecycleItem> realNullItem = new ArrayList<RecycleItem>();
            StringBuffer ids = new StringBuffer();
            ids.append("(");
            for (RecycleItem temp : nullRecycleItemList) {
                ids.append(temp.bubbleId + ", ");
            }
            ids.deleteCharAt(ids.lastIndexOf(","));
            ids.append(")");

            ArrayList<Parcelable> globalBubbleAttaches;
            Bundle tmp = new Bundle();
            tmp.putString("type", SaraConstant.LIST_TYPE_REMOVED);
            tmp.putString("bubbleIds", ids.toString());
            globalBubbleAttaches = BubbleDataRepository.getGlobleBubblesAttachList(this, tmp);

            if (globalBubbleAttaches != null && globalBubbleAttaches.size() > 0) {
                List<Integer> attachmentIds = new ArrayList<>();
                for (Parcelable parcelable : globalBubbleAttaches) {
                    if (parcelable instanceof GlobalBubbleAttach) {
                        GlobalBubbleAttach attach = (GlobalBubbleAttach) parcelable;
                        attachmentIds.add(attach.getBubbleId());
                    }
                }

                if (attachmentIds.size() > 0) {
                    for (RecycleItem temp : nullRecycleItemList) {
                        if (!attachmentIds.contains(temp.bubbleId)) {
                            realNullItem.add(temp);
                        }
                    }
                }
            } else {
                realNullItem = nullRecycleItemList;
            }

            if (realNullItem.size() > 0) {
                recycleItemList.removeAll(realNullItem);
            }
        }
        return recycleItemList;
    }

    private boolean loadRecycleAttachments(List<RecycleItem> items, int start, int end) {
        if (items == null) {
            return false;
        }
        final int count = items.size();
        boolean ret = false;
        if (items == null || items.size() == 0 || start >= count) {
            return ret;
        }
        if (end >= count) {
            end = count;
        }
        ArrayList<Parcelable> globalBubbleAttaches;
        for (int i = start; i < end; i++) {
            final RecycleItem item = items.get(i);
            if (!item.isAttachLoaded()) {
                Bundle tmp = new Bundle();
                tmp.putString("type", SaraConstant.LIST_TYPE_REMOVED);
                tmp.putInt("bubbleId", item.bubbleId);
                globalBubbleAttaches = BubbleDataRepository.getGlobleBubbleAttachList(this, tmp);
                if (globalBubbleAttaches != null && globalBubbleAttaches.size() > 0) {
                    List<GlobalBubbleAttach> attaches = new ArrayList<>();
                    for (Parcelable parcelable : globalBubbleAttaches) {
                        if (parcelable instanceof GlobalBubbleAttach) {
                            GlobalBubbleAttach attach = (GlobalBubbleAttach) parcelable;
                            attach.setNeedDel(false);
                            attaches.add(attach);
                        }
                    }
                    item.setBubbleAttaches(attaches);
                } else {
                    item.setBubbleAttaches(null);
                }
                ret = true;
            }
        }
        return ret;
    }


    public void setSoftKeyboardHide() {
        // TODO Auto-generated method stub
        this.hideSoftKeyboard();
    }

    private void hideSoftKeyboard() {
        // Hide soft keyboard, if visible
        InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        inputMethodManager.hideSoftInputFromWindow(
                mBubbleHeadersListView.getWindowToken(), 0);
    }


    public void startBubbleAnimation() {
        mTitleBarShadow.setVisibility(View.GONE);
        SaraUtils.setAnimatorScale(this);
        mSeachResultView.setVisibility(View.VISIBLE);
        if (mStartSearchAnimatorSet != null) {
            mStartSearchAnimatorSet.start();
            return;
        }
        final int titleBarHeight = mTitle.getHeight();
        PropertyValuesHolder pvhTranslateY = PropertyValuesHolder.ofFloat("y", 0, -titleBarHeight);
        Animator translateAnimator = ObjectAnimator.ofPropertyValuesHolder(mBubblesFrameView,
                pvhTranslateY);
        PropertyValuesHolder pvhAlpha = PropertyValuesHolder.ofFloat("alpha", 0, 1);
        Animator alphaAnimator = ObjectAnimator.ofPropertyValuesHolder(mSeachResultView, pvhAlpha);
        mStartSearchAnimatorSet = new AnimatorSet();
        mStartSearchAnimatorSet.setStartDelay(10);
        mStartSearchAnimatorSet.setDuration(mDuration);
        mStartSearchAnimatorSet.playTogether(translateAnimator, alphaAnimator);
        mStartSearchAnimatorSet.addListener(new Animator.AnimatorListener() {

            @Override
            public void onAnimationStart(Animator arg0) {
                mIsSearch = true;
                mIsSearchAnimationing = true;

                FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) mBubblesFrameView.getLayoutParams();
                params.bottomMargin = -titleBarHeight;
                mBubblesFrameView.setLayoutParams(params);
            }

            @Override
            public void onAnimationRepeat(Animator arg0) {

            }

            @Override
            public void onAnimationEnd(Animator arg0) {
                mIsSearchAnimationing = false;
            }

            @Override
            public void onAnimationCancel(Animator arg0) {
                mIsSearchAnimationing = false;
            }
        });
        mStartSearchAnimatorSet.start();
    }


    public void endBubbleAnimationWithoutAnimation() {
        if (mIsSearchAnimationing) {
            mStartSearchAnimatorSet.cancel();
        }
        SaraUtils.setAnimatorScale(this);
        mSearchKey = null;
        mTitleBarShadow.setVisibility(View.VISIBLE);
        mBubblesFrameView.setTranslationY(0);
        mSeachResultView.setAlpha(0);
        mIsSearch = false;
        searchAnimationEnd();
    }

    private void searchAnimationEnd() {
        new Handler().postDelayed(new Runnable() {

            @Override
            public void run() {
                mSearchResultList.setEmptyView(null);
                mSeachResultView.setVisibility(View.INVISIBLE);
                mSearchEmptyView.setVisibility(View.GONE);
                mSearchResultList.setVisibility(View.GONE);

                FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) mBubblesFrameView.getLayoutParams();
                params.bottomMargin = 0;
                mBubblesFrameView.setLayoutParams(params);
            }
        }, 30);
    }


    public void endBubbleAnimation() {

        mTitleBarShadow.setVisibility(View.VISIBLE);
        final int titleBarHeihgt = mTitle.getHeight();
        SaraUtils.setAnimatorScale(this);
        mSearchKey = null;
        PropertyValuesHolder pvhY = PropertyValuesHolder.ofFloat("y", -titleBarHeihgt, 0);
        Animator animator = ObjectAnimator.ofPropertyValuesHolder(mBubblesFrameView, pvhY);
        PropertyValuesHolder pvhA = PropertyValuesHolder.ofFloat("alpha", 1, 0);
        Animator alpha = ObjectAnimator.ofPropertyValuesHolder(mSeachResultView, pvhA);
        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.setDuration(mDuration);
        animatorSet.playTogether(animator, alpha);
        animatorSet.addListener(new Animator.AnimatorListener() {

            @Override
            public void onAnimationStart(Animator arg0) {
            }

            @Override
            public void onAnimationRepeat(Animator arg0) {
                // TODO Auto-generated method stub

            }

            @Override
            public void onAnimationEnd(Animator arg0) {
                mIsSearch = false;
                searchAnimationEnd();
            }

            @Override
            public void onAnimationCancel(Animator arg0) {
                searchAnimationEnd();
            }
        });
        animatorSet.start();
    }


    public void onQueryTextChange(String newString) {
        mSearchKey = newString;
        getAllsearch(newString);
    }

    private void getAllsearch(String searchKey) {
        if (null == searchKey) {
            return;
        }

        if (mSearchThread != null) {
            mSearchThread.setTrue(false);
            mSearchThread = null;
        }

        if (TextUtils.isEmpty(searchKey)) {
            mSearchResultList.setVisibility(View.GONE);
            mSearchResultAdapter.setList(null, null);
            mSearchEmptyView.setVisibility(View.GONE);
        } else {
            if (mSearchThread == null) {
                mSearchThread = new SearchThread();
                mSearchThread.setSearchKey(searchKey);
                mSearchThread.start();
            }

        }
    }

    public void processEditBubbles(final boolean isEdit, boolean performAnimation) {
        processEditBubbles(isEdit, performAnimation, EMPTY_OPERATION, null);
    }

    private static final int EMPTY_OPERATION = 0;
    private static final int DELETE_OPERATION = 1;
    private static final int RESTORE_OPERATION = 2;

    private void processEditBubbles(final boolean isEdit, boolean performAnimation, int operation,
            List<RecycleItem> deletedItems) {
        mIsEdit = isEdit;
        if (mBubbleHeadersListView == null) {
            return;
        }

        if (isEdit) {
            mBubbleHeadersListView.restoreScrollState(false);
        }
        mBubbleHeadersListView.setScrollEnabled(!isEdit);

        mBottomControl.setVisibility(mIsEdit ? View.VISIBLE : View.GONE);

        if (isEdit) {
            refreshOptionBar(mSelectedSet.size());
        } else {
            mSelectedSet.clear();
        }

        if (performAnimation) {
            performAnimationForEdit(isEdit, mBubbleHeadersListView, operation, deletedItems);
        } else {
            notPerformAnimationForEdit(isEdit, mBubbleHeadersListView);
        }
    }

    private void performAnimationForEdit(final boolean isEdit, ListView targetListView,
            final int operation, final List<RecycleItem> deletedItems) {
        final ArrayList<Animator> animators = getEditAnimators(isEdit, targetListView);
        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {
                if (isEdit) {
                    mEditBubblesBarView.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                if (mSearchbarView != null) {
                    mSearchbarView.setLayerType(View.LAYER_TYPE_NONE, null);
                }
                if (mEditBubblesBarView != null) {
                    mEditBubblesBarView.setLayerType(View.LAYER_TYPE_NONE, null);
                }
                for (Animator anim : animators) {
                    if (anim instanceof ObjectAnimator) {
                        if (((ObjectAnimator) anim).getTarget() != null) {
                            ((View) ((ObjectAnimator) anim).getTarget()).setLayerType(
                                    View.LAYER_TYPE_NONE, null);
                        }
                    }
                }
                if (!isEdit && mEditBubblesBarView != null) {
                    mEditBubblesBarView.setVisibility(View.GONE);
                }
                refreshAdapter(isEdit);
                freezeButtonWhenEditBubbles(false);
                if (operation == DELETE_OPERATION || operation == RESTORE_OPERATION) {
                    mDeleteMultiTask = new DeleteMultipleTask(RecycleBinActivity.this, operation == RESTORE_OPERATION);
                    mDeleteMultiTask.execute(deletedItems);
                }
            }

            @Override
            public void onAnimationCancel(Animator animation) {

            }

            @Override
            public void onAnimationRepeat(Animator animation) {

            }
        });
        animatorSet.playTogether(animators);
        animatorSet.start();
        freezeButtonWhenEditBubbles(true);
    }

    private class DeleteMultipleTask extends AsyncTask<List<RecycleItem>, Void, Void> {

        private SmartisanProgressDialog progressDialog;
        private boolean running = false;
        private Handler handler;
        private Context context;
        private static final long DELAY_TIME = 1000L;
        private boolean restored;

        public DeleteMultipleTask(Context context, boolean restored) {
            this.context = context;
            this.restored = restored;
            handler = new Handler(Looper.getMainLooper());
        }

        @Override
        protected void onPreExecute() {
            running = true;
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (running && progressDialog == null && !isFinishing()) {
                        progressDialog = new SmartisanProgressDialog(context);
                        progressDialog
                                .setMessage(R.string.progress_dialog_message_for_wait);
                        progressDialog.setCancelable(false);
                        progressDialog.setCanceledOnTouchOutside(false);
                        if(isCurrentActivityAlive(progressDialog))progressDialog.show();
                    }
                }
            }, DELAY_TIME);
        }

        @Override
        protected Void doInBackground(List<RecycleItem>... params) {
            List<RecycleItem> selectionItemList = params[0];
            int size = selectionItemList.size();
            int[] bubbleIds = new int[size];
            boolean resetAll = false;
            int recycleItemNum = 0;
            if (restored) {
                if (SaraUtils.SUPPORT_MAX_BUBBLE_COUNT) {
                    mInsertBubble2GlobalNum = BubbleDataRepository.getCanInsertGlobalBubbleNum(context,selectionItemList.size());
                    if (selectionItemList.size() <= mInsertBubble2GlobalNum) {
                        resetAll = true;
                    }
                } else {
                     resetAll = true;
                }
                recycleItemNum = resetAll ? selectionItemList.size() : mInsertBubble2GlobalNum;
            } else {
                recycleItemNum = selectionItemList.size();
            }

            for (int i = 0; i < recycleItemNum; i++) {
                RecycleItem item = selectionItemList.get(i);
                bubbleIds[i] = item.bubbleId;
            }
            Bundle bunble = new Bundle();
            bunble.putIntArray(SaraConstant.KEY_BUBBLE_IDS, bubbleIds);
            // restore to bubbles
            if (restored) {
                if (!resetAll) {
                    ToastUtil.showToast(R.string.max_global_bubbles_tips);
                }
                if (sBubbleDirection == BUBBLE_RIRECTION_USED) {
                    BubbleDataRepository.restoreGlobleBubble(context, bunble);
                } else if (sBubbleDirection == BUBBLE_RIRECTION_HANDLED) {
                    BubbleDataRepository.restoreLegacyGlobleBubble(context, bunble);
                } else {
                    BubbleDataRepository.restoreDeleteGlobleBubble(context, bunble);
                }
            } else {
                String value = "";
                if (sBubbleDirection == BUBBLE_RIRECTION_RECYCLE) {
                    value = SaraConstant.DESTROY_TYPE_REMOVED;
                } else if (sBubbleDirection == BUBBLE_RIRECTION_USED) {
                    value = SaraConstant.DESTROY_TYPE_USED;
                } else {
                    value = SaraConstant.DESTROY_TYPE_LEGACY_USED;
                }

                bunble.putString(SaraConstant.KEY_DESTROY_TYPE, value);
                // delete recycle items
                BubbleDataRepository.destroyGlobleBubble(context, bunble);
                for (RecycleItem item : selectionItemList) {
                    if (item.bubbleType != GlobalBubble.TYPE_TEXT) {
                        new File(item.bubblePath).delete();
                        new File(item.bubblePath + ".wave").delete();
                    }
                }

            }

            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            completed();
        }

        @Override
        protected void onCancelled() {
            completed();
        }

        private void completed() {
            running = false;
            if (progressDialog != null && progressDialog.isShowing()) {
                progressDialog.dismiss();
            }
            triggerLoad(false);
        }
    }

    private void triggerLoad(final boolean showProgress) {
        if (isFinishing()) {
            return;
        }
        mBackgroundHandler.removeMessages(NOTIFY_LOAD_RECYCLE_BIN);
        mBackgroundHandler.sendEmptyMessage(NOTIFY_LOAD_RECYCLE_BIN);
        if (showProgress) {
            mHandler.sendEmptyMessageDelayed(SHOW_LOAD_PROGRESS, 200);
        }
    }

    private void notPerformAnimationForEdit(final boolean isEdit, ListView targetListView) {
        final int width = mSearchbarView.getWidth();
        int firstPos = targetListView.getFirstVisiblePosition();
        int lastPos = targetListView.getLastVisiblePosition();

        if (isEdit) {
            for (int i = firstPos; i <= lastPos; i++) {
                final View child = targetListView.getChildAt(i - firstPos);

                final View checkBoxFrame = child.findViewById(R.id.checkBoxFrame);
                checkBoxFrame.setVisibility(View.VISIBLE);
                checkBoxFrame.setAlpha(1);
                checkBoxFrame.setScaleX(1F);
                checkBoxFrame.setScaleY(1F);
            }
            mEditBubblesBarView.setVisibility(View.VISIBLE);
            mSearchbarView.setTranslationX(width);
        } else {
            for (int i = firstPos; i <= lastPos; i++) {
                final View child = targetListView.getChildAt(i - firstPos);
                long id = targetListView.getAdapter().getItemId(i);

                final View checkBoxFrame = child.findViewById(R.id.checkBoxFrame);
                if (checkBoxFrame == null) {
                    continue;
                }
                checkBoxFrame.setVisibility(View.VISIBLE);
                checkBoxFrame.setAlpha(0);
                checkBoxFrame.setScaleX(0.1F);
                checkBoxFrame.setScaleY(0.1F);

            }
            mEditBubblesBarView.setVisibility(View.GONE);
            mSearchbarView.setTranslationX(0);
            freezeButtonWhenEditBubbles(false);
        }
        refreshAdapter(isEdit);
    }

    private void freezeButtonWhenEditBubbles(boolean freeze) {
        if (freeze) {
            mTitle.getOkButton().setEnabled(false);
        } else {
            if (mIsEdit) {
                mTitle.getOkButton().setEnabled(true);
            } else {
                // if the bubbles is few, the time spent on deleting may be less than
                // the animation time. then we not let the edit bubbles button enabled
                // when the bubbles is empty.
                if (!(mRecycleItemListAdapter == null || mRecycleItemListAdapter.getCount() == 0)) {
                    mTitle.getOkButton().setEnabled(true);
                } else {
                    mTitle.getOkButton().setEnabled(false);
                }
            }
        }
    }

    private ArrayList<Animator> getEditAnimators(boolean isEdit, ListView targetListView) {
        ArrayList<Animator> animators = new ArrayList<Animator>();

        final int width = mSearchbarView.getWidth();
        int firstPos = targetListView.getFirstVisiblePosition();
        int lastPos = targetListView.getLastVisiblePosition();

        if (mDecelerateInterpolator == null) {
            mDecelerateInterpolator = new DecelerateInterpolator(1.5F);
        }

        Adapter adapter = targetListView.getAdapter();
        int leftMarginIcon = targetListView.getResources().getDimensionPixelSize(R.dimen.recycle_list_margin_left_icon);
        int leftMargin = targetListView.getResources().getDimensionPixelSize(R.dimen.recycle_list_margin_left);
        if (isEdit) {
            for (int i = firstPos; i <= lastPos; i++) {
                if (i >= adapter.getCount()) {
                    break;
                }
                final View child = targetListView.getChildAt(i - firstPos);
                View bubbleContent = child.findViewById(R.id.bubble_content);


                PropertyValuesHolder holder = PropertyValuesHolder.ofFloat("translationX", 0.0f, leftMarginIcon - leftMargin);
                ObjectAnimator bubbleContentAnimator = ObjectAnimator.ofPropertyValuesHolder(bubbleContent, holder);
                bubbleContentAnimator.setDuration(mDuration);
                bubbleContentAnimator.setStartDelay(0);
                bubbleContentAnimator.setInterpolator(mDecelerateInterpolator);
                animators.add(bubbleContentAnimator);

                final View checkBoxFrame = child.findViewById(R.id.checkBoxFrame);
                checkBoxFrame.setVisibility(View.VISIBLE);
                checkBoxFrame.setAlpha(0);

                final ValueAnimator valueAnimator = ValueAnimator.ofFloat(0.1f, 1.0f);
                valueAnimator.setDuration(mDuration);
                valueAnimator.setStartDelay(mDuration / 2);
                valueAnimator.setInterpolator(mDecelerateInterpolator);
                valueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                    @Override
                    public void onAnimationUpdate(ValueAnimator animation) {
                        Object temp = animation.getAnimatedValue();
                        if (temp instanceof Float){
                            float value = (Float)temp;
                            checkBoxFrame.setAlpha(value);
                            checkBoxFrame.setScaleY(value);
                            checkBoxFrame.setScaleX(value);
                        }
                    }
                });
                animators.add(valueAnimator);

                // every enter edit bubbles mode, we want all the checkbox is not checked.
                final CheckBox checkBox = (CheckBox) checkBoxFrame.findViewById(R.id.checkBox);
                checkBox.setChecked(false);
            }

            Animator out = ObjectAnimator.ofFloat(mSearchbarView, "translationX", 0, width);
            out.setDuration(mDuration);
            animators.add(out);
            Animator in = ObjectAnimator.ofFloat(mEditBubblesBarView, "translationX", -width, 0);
            in.setDuration(mDuration);
            animators.add(in);

        } else {
            for (int i = firstPos; i <= lastPos; i++) {
                if (i >= adapter.getCount()) {
                    break;
                }
                final View child = targetListView.getChildAt(i - firstPos);
                final View checkBoxFrame = child.findViewById(R.id.checkBoxFrame);
                long id = adapter.getItemId(i);
                if (checkBoxFrame == null) {
                    continue;
                }
                checkBoxFrame.setVisibility(View.VISIBLE);
                PropertyValuesHolder scaleOutX = PropertyValuesHolder.ofFloat("scaleX", 1, 0.1f);
                Animator photoScaleOutX = ObjectAnimator.ofPropertyValuesHolder(checkBoxFrame,
                        scaleOutX);
                photoScaleOutX.setDuration(mDuration);
                photoScaleOutX.setInterpolator(mDecelerateInterpolator);
                animators.add(photoScaleOutX);
                PropertyValuesHolder scaleOutY = PropertyValuesHolder.ofFloat("scaleY", 1, 0.1f);
                Animator photoScaleOutY = ObjectAnimator.ofPropertyValuesHolder(checkBoxFrame,
                        scaleOutY);
                photoScaleOutY.setDuration(mDuration);
                photoScaleOutY.setInterpolator(mDecelerateInterpolator);
                animators.add(photoScaleOutY);
                PropertyValuesHolder alphaOutY = PropertyValuesHolder.ofFloat("alpha", 1, 0);
                Animator photoAlphaOutY = ObjectAnimator.ofPropertyValuesHolder(checkBoxFrame,
                        alphaOutY);
                photoAlphaOutY.setDuration(mDuration);
                photoAlphaOutY.setInterpolator(mDecelerateInterpolator);
                animators.add(photoAlphaOutY);

                View bubbleContent = child.findViewById(R.id.bubble_content);
                PropertyValuesHolder holder = PropertyValuesHolder.ofFloat("translationX", leftMarginIcon - leftMargin, 0.0f);
                ObjectAnimator bubbleContentAnimator = ObjectAnimator.ofPropertyValuesHolder(bubbleContent, holder);
                bubbleContentAnimator.setDuration(mDuration);
                bubbleContentAnimator.setStartDelay(mDuration / 2);
                bubbleContentAnimator.setInterpolator(mDecelerateInterpolator);
                animators.add(bubbleContentAnimator);
            }

            Animator in = ObjectAnimator.ofFloat(mSearchbarView, "translationX", width, 0);
            in.setDuration(mDuration);
            animators.add(in);
            Animator out = ObjectAnimator.ofFloat(mEditBubblesBarView, "translationX", 0, -width);
            out.setDuration(mDuration);
            animators.add(out);
        }
        mSearchbarView.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        mEditBubblesBarView.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        return animators;
    }

    private void refreshAdapter(boolean isEdit) {
        if (mRecycleItemListAdapter == null) return;
        mRecycleItemListAdapter.setEdit(isEdit);
        mRecycleItemListAdapter.notifyDataSetChanged();
        mBubbleHeadersListView.setSlideEnable(isEdit);
    }

    /**
     * If the selected set changed, we should refresh the UI.
     *
     * @param <T>
     */
    private final class SelectionSet<T> extends HashSet<T> {

        @Override
        public boolean add(T object) {
            boolean success = super.add(object);
            if (success) {
                refreshOptionBar(size());
            }
            return success;
        }

        @Override
        public boolean remove(Object object) {
            boolean success = super.remove(object);
            if (success) {
                refreshOptionBar(size());
            }
            return success;
        }

        @Override
        public boolean addAll(Collection<? extends T> collection) {
            boolean success = super.addAll(collection);
            if (success) {
                refreshOptionBar(size());
            }
            return success;
        }

        @Override
        public boolean removeAll(Collection<?> collection) {
            boolean success = super.removeAll(collection);
            if (success) {
                refreshOptionBar(size());
            }
            return success;
        }

        @Override
        public void clear() {
            super.clear();
            refreshOptionBar(size());
        }
    }

    private void setEmptyView(ListView list, boolean visible) {
        if (mRecycleBinEmptyView == null) {
            mRecycleBinEmptyView = (RelativeLayout) mRecycleBinEmptyViewStub.inflate();
            mRecycleBinEmpty =  (SmartisanBlankView)mRecycleBinEmptyView.findViewById(R.id.black_view);
            mRecycleBinEmpty.getImageView().setVisibility(View.GONE);
            mRecycleBinEmpty.getSecondaryHintView().setGravity(Gravity.LEFT);
            mRecycleBinEmpty.setAccessibilityTraversalAfter(R.id.search_edit_text);
        }
        if (sBubbleDirection == BUBBLE_RIRECTION_RECYCLE){
            mRecycleBinEmpty.getPrimaryHintView().setText(R.string.recycle_bin_empty);
            mRecycleBinEmpty.getSecondaryHintView().setText(R.string.recycle_bin_empty_secondary);
        } else if (sBubbleDirection == BUBBLE_RIRECTION_USED) {
            mRecycleBinEmpty.getPrimaryHintView().setText(R.string.handled_empty);
            mRecycleBinEmpty.getSecondaryHintView().setText(R.string.handled_empty_secondary);
        } else {
            mRecycleBinEmpty.getPrimaryHintView().setText(R.string.handled_ok_empty);
            mRecycleBinEmpty.getSecondaryHintView().setText(R.string.handled_ok_empty_secondary);
        }

        mBubbleHeadersListView.setVisibility(visible ? View.INVISIBLE : View.VISIBLE);
        mRecycleBinEmptyView.setVisibility(visible ? View.VISIBLE : View.GONE);
    }

    private void refreshOptionBar(int count) {
        if (mRecycleItemListAdapter == null) {
            return;
        }
        int total = mRecycleItemListAdapter.getCount();
        String msg = getResources().getString(R.string.edit_bubbles_option_bar_msg, count, total);
        mOptionTextView.setText(msg);
        mSelectAllCheckBox.setChecked(count == total);

        mDeleteBtn.setEnabled(count != 0);
        mDeleteBtn.setAlpha(count == 0 ? 0.5F : 1F);
        mRestoreBtn.setEnabled(count != 0);
        if (SaraUtils.SUPPORT_MAX_BUBBLE_COUNT && count != 0 && mInsertBubble2GlobalNum < 0){
            mRestoreBtn.setAlpha(0.5f);
        } else {
            mRestoreBtn.setAlpha(count == 0 ? 0.5F : 1F);
        }
    }

    private RecycleItemListener mRecycleItemListener = new RecycleItemListener() {
        @Override
        public boolean isSelected(RecycleItem item) {
            return mSelectedSet.contains(item);
        }

        @Override
        public void umbButtonClicked(View itemView, RecycleItem item, int buttonId) {
            showSingleDeletionDialog(itemView, buttonId, item);
        }

        @Override
        public void onBubbleDelete(RecycleItem item) {
            showSingleDeletionDialog(null, R.id.umb_clear, item);
        }

        @Override
        public void onBubbleRestore(RecycleItem item) {
            deleteSingleBubble(null, true, item);
            if (sBubbleDirection == BUBBLE_RIRECTION_USED) {
                SaraTracker.onEvent("A130024","type",1);
            } else if (sBubbleDirection == BUBBLE_RIRECTION_RECYCLE) {
                SaraTracker.onEvent("A130025","type",1);
            }
        }

    };

    private SlideListView.Listener mSlideListener = new SlideListView.Listener() {
        @Override
        public void setChecked(int position, boolean isChecked) {
            View child = mBubbleHeadersListView.getChildAt(position
                    - mBubbleHeadersListView.getFirstVisiblePosition());
            CheckBox cb = (CheckBox) child.findViewById(R.id.checkBox);
            if (cb == null) {
                return;
            }

            cb.setChecked(isChecked);

            RecycleItem item = mRecycleItemListAdapter.getItem(position);
            if (isChecked) {
                if (!mSelectedSet.contains(item)) {
                    mSelectedSet.add(item);
                }
                if (SaraUtils.SUPPORT_MAX_BUBBLE_COUNT) {
                    mInsertBubble2GlobalNum = BubbleDataRepository.getCanInsertGlobalBubbleNum(RecycleBinActivity.this,mSelectedSet.size());
                    if (mInsertBubble2GlobalNum >= mSelectedSet.size()) {
                        mRestoreBtn.setAlpha(1F);
                    } else {
                        mRestoreBtn.setAlpha(0.5F);
                    }
                }
            } else {
                if (mSelectedSet.contains(item)) {
                    mSelectedSet.remove(item);
                }
            }
        }

        @Override
        public boolean isChecked(int position) {
            RecycleItem item = mRecycleItemListAdapter.getItem(position);
            return mSelectedSet.contains(item);
        }
    };

    private class SearchThread extends Thread {
        protected String mSearchKey = null;
        private boolean mIsTrue = true;

        public void setSearchKey(String searchKey) {
            mSearchKey = searchKey;
        }

        public void setTrue(boolean isTrue) {
            mIsTrue = isTrue;
        }

        @Override
        public void run() {
            if (mSearchKey != null) {
                List<RecycleItem> searchResults = new ArrayList<RecycleItem>();
                final ArrayList<RecycleItem> bubbles = (ArrayList<RecycleItem>) ((ArrayList<RecycleItem>) mRecycleItemListAdapter
                        .getList()).clone();

                for (RecycleItem item : bubbles) {
                    if (!mIsTrue) {
                        LogUtils.e(TAG, "mSearchThread isInterrupted");
                        return;
                    }

                    if (item.search(mSearchKey)) {
                        searchResults.add(item);
                    }
                }

                if (mIsTrue) {
                    loadRecycleAttachments(searchResults,0,COUNT_IN_PAGE);
                    mHandler.removeMessages(NOTIFY_SET_SEARCH_RESULT);
                    Message message = new Message();
                    message.what = NOTIFY_SET_SEARCH_RESULT;
                    message.obj = searchResults;

                    mHandler.sendMessage(message);
                }

            }
        }
    }

    private SmartisanProgressDialog mProgress = null;

    private void showProgressDialog() {
        if (mProgress == null) {
            mProgress = new SmartisanProgressDialog(this);
            mProgress.setOwnerActivity(this);
            mProgress.setMessage(getString(R.string.dialog_content_wait_message));
            mProgress.setCancelable(true);
            mProgress.setOnCancelListener(new DialogInterface.OnCancelListener() {
                @Override
                public void onCancel(DialogInterface dialogInterface) {
                    finish();
                }
            });
        }
        if (isCurrentActivityAlive(mProgress) && !mProgress.isShowing()) {
            mProgress.show();
        }
    }

    private boolean isCurrentActivityAlive(SmartisanProgressDialog progressDialog) {
        Activity activity = progressDialog.getOwnerActivity();
        LogUtils.d(TAG, "isCurrentActivityAlive activity = " + activity);
        if (activity == null || activity.isDestroyed() || activity.isFinishing()) {
            return false;
        }
        return true;
    }

    public void hideProgressDiaLogUtils() {
        mHandler.removeMessages(SHOW_LOAD_PROGRESS);
        if (mProgress != null && mProgress.isShowing()) {
            mProgress.dismiss();
        }
    }

    public GlobalBubble createGlobleBubble(RecycleItem item) {
        GlobalBubble bubble = null;
        if (item.bubbleType != GlobalBubble.TYPE_TEXT) {
            Uri uri = Uri.parse(SaraUtils.formatFilePath2Content(this, item.bubblePath));
            bubble = SaraUtils.toGlobalBubble(this, item.bubbleText, item.bubbleType, uri, item.bubbleColor, item.remindTime, item.dueDate);
        } else {
            bubble = SaraUtils.toGlobalBubbleText(this, item.bubbleColor);
            bubble.setText(item.bubbleText);
        }
        bubble.setId(item.bubbleId);
        return bubble;
    }
    class RecycleItemSortComparator implements Comparator<RecycleItem> {

        @Override
        public int compare(RecycleItem lhs, RecycleItem rhs) {
            long temp = lhs.getRecycleDate() - rhs.getRecycleDate();
            if (temp > 0) {
                return -1;
            } else if (temp < 0) {
                return 1;
            } else {
                return 0;
            }
        }
    }

    @Override
    public void onBackPressed() {
        if (mIsEdit) {
            initEditStyle();
            processEditBubbles(false, true);
        } else {
            super.onBackPressed();
        }
    }
}
