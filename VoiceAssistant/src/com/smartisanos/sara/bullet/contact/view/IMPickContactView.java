package com.smartisanos.sara.bullet.contact.view;

import android.content.Context;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewStub;
import android.widget.LinearLayout;

import com.smartisanos.sara.R;
import com.smartisanos.sara.bullet.contact.adapter.PickContactAdapter;
import com.smartisanos.sara.bullet.contact.model.AbsContactItem;
import com.smartisanos.sara.bullet.contact.model.ContactItem;
import com.smartisanos.sara.bullet.util.DisplayUtils;
import com.smartisanos.sara.util.LogUtils;

import java.util.ArrayList;
import java.util.List;

import smartisanos.util.LogTag;
import smartisanos.util.UIHandler;
import smartisanos.widget.SearchBar;

public class IMPickContactView extends LinearLayout {
    private static final String TAG = "VoiceAss.IMPickContactView";
    public interface OnPickedListener {
        void OnPickedContact(ArrayList<ContactItem> selected);

        void OnCheckContact(AbsContactItem absContactItem, View view);
    }

    private String mKeyWord;
    private SearchBar mSearchBar;
    private ViewStub mPickSearchResultStub;
    private PickContactView mPickSearchResult;
    private IPickContactView.OnScrollListener mOnScrollListener = null;
    private LinearLayout mContactLayout;
    private PickContactView mPickContactLeft;
    private PickContactView mPickContactRight;
    private OnPickedListener mOnPickListener;
    private List<AbsContactItem> mLeftContacts = new ArrayList<>();
    private List<AbsContactItem> mRightContacts = new ArrayList<>();

    public IMPickContactView(Context context) {
        this(context, null);
    }

    public IMPickContactView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public IMPickContactView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        LayoutInflater.from(context).inflate(R.layout.im_pick_contact_view, this);
        setOrientation(VERTICAL);
        initViews();
    }

    private void initViews() {
        mSearchBar = (SearchBar) findViewById(R.id.search_bar);
        mSearchBar.setListener(new SearchBarListener());
        mPickSearchResultStub = (ViewStub) findViewById(R.id.pick_search_result_stub);
        mContactLayout = (LinearLayout) findViewById(R.id.contactLayout);
        initChild();
    }

    private void initChild() {
        initPickLeftContactView();
        initPickRightContactView();
    }

    private void initPickSearchResultView() {
        if (mPickSearchResultStub != null && mPickSearchResultStub.getParent() != null) {
            mPickSearchResult = (PickContactView) mPickSearchResultStub.inflate();
        } else {
            mPickSearchResult = (PickContactView) findViewById(R.id.pick_search_result);
        }
        mPickSearchResult.setOnPickContactListener(new PickSearchResultListener());
        mPickSearchResult.setAbortPreTask(true);
        mPickSearchResult.setHasCheckEffect(false);
        mPickSearchResult.setOnScrollListener(mOnScrollListener);
    }

    private class SearchBarListener extends SearchBar.ListenerAdapter {

        @Override
        public void onClickSearchEdit() {
            super.onClickSearchEdit();
            UIHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mSearchBar.showKeyboard();
                }
            }, 200);
        }

        @Override
        public void searchText(String trimText) {
            super.searchText(trimText);
            LogUtils.d("searchText() trimText = " + trimText);
            if (TextUtils.isEmpty(trimText)) {
                mKeyWord = trimText;
                if (null != mPickSearchResult) {
                    mPickSearchResult.setVisibility(GONE);
                    mPickSearchResult.resetData();
                }
                mContactLayout.setVisibility(VISIBLE);
            } else {
                if (!trimText.equals(mKeyWord)) {
                    mKeyWord = trimText;
                    mContactLayout.setVisibility(GONE);
                    initPickSearchResultView();
                    mPickSearchResult.resetData();
                    mPickSearchResult.setVisibility(VISIBLE);
                    mPickSearchResult.query(mKeyWord);
                }
            }
        }
    }

    public void hideSearchBarKeyboard() {
        if (null != mSearchBar && mSearchBar.isSearchMode()) {
            mSearchBar.hideKeyboard();
        }
    }

    public void resetSearchResult() {
        if (null != mSearchBar && mSearchBar.isSearchMode()) {
            mSearchBar.onClickCancelView(false);
        }
    }

    private class PickSearchResultListener implements PickContactView.OnPickContactListener {

        @Override
        public void OnCheckContact(AbsContactItem absContactItem, View view) {
            if (mOnPickListener != null) {
                mOnPickListener.OnCheckContact(absContactItem, view);
            }
        }

        @Override
        public void onResultSize(int size) {
            LogUtils.d("onResultSize() size = " + size);
        }
    }

    private class PickContactListener implements PickContactView.OnPickContactListener {
        private boolean isLeft;

        PickContactListener(boolean isLeft) {
            this.isLeft = isLeft;
        }

        @Override
        public void OnCheckContact(AbsContactItem absContactItem, View view) {
            if (mOnPickListener != null) {
                mOnPickListener.OnCheckContact(absContactItem, view);
            }
        }

        @Override
        public void onResultSize(int size) {
            PickContactView pickView = !isLeft ? mPickContactRight : mPickContactLeft;
            List<AbsContactItem> items = ((PickContactAdapter)pickView.getAdapter()).getData();
            updateSource(items);
        }

        private void updateSource(List<AbsContactItem> items) {
            if(isLeft) {
                mLeftContacts = items;
            } else {
                mRightContacts = items;
            }
        }
    }

    private void initPickLeftContactView() {
        mPickContactLeft = (PickContactView) findViewById(R.id.pick_contact_left);
        mPickContactLeft.setOnPickContactListener(new PickContactListener(true));
        mPickContactLeft.getLayoutParams().width = (DisplayUtils.mScreenWidth - DisplayUtils.dp2px(12)) / 2;
    }

    private void initPickRightContactView() {
        mPickContactRight = (PickContactView) findViewById(R.id.pick_contact_right);
        mPickContactRight.setOnPickContactListener(new PickContactListener(false));
    }

    public void load() {
        LogTag.d(TAG, "load");
        mPickContactLeft.query("");
        mPickContactRight.query("");
        mPickContactLeft.preLoadContact();
    }

    public void registerPickListener(OnPickedListener listener) {
        mOnPickListener = listener;
    }

    public void setCheckEffect(boolean hasEffect) {
        mPickContactLeft.setHasCheckEffect(hasEffect);
        mPickContactRight.setHasCheckEffect(hasEffect);
    }

    public void setShowMultiple(boolean left, boolean right) {
        mPickContactLeft.setShowMultiple(left);
        mPickContactRight.setShowMultiple(right);
    }

    public boolean isMultiple(){
        return mPickContactLeft.isMultiple() && mPickContactRight.isMultiple() ;
    }

    public void onDestroy() {
        LogTag.d(TAG, "onDestory()");
        mPickContactLeft.onDestroy();
        mPickContactRight.onDestroy();
        if (null != mPickSearchResult) {
            mPickSearchResult.onDestroy();
        }
    }

    public boolean hasContacts() {
        return mLeftContacts.size() > 0 && mRightContacts.size() > 0;
    }

    public void setOnScrollListener(IPickContactView.OnScrollListener onScrollListener) {
        this.mOnScrollListener = onScrollListener;
        if (mPickContactLeft != null) {
            mPickContactLeft.setOnScrollListener(onScrollListener);
        }
        if (mPickContactRight != null) {
            mPickContactRight.setOnScrollListener(onScrollListener);
        }
    }
}
