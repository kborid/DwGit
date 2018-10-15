package com.smartisanos.sara.bullet.contact.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.smartisanos.sara.R;
import com.smartisanos.sara.bullet.contact.adapter.PickContactAdapter;
import com.smartisanos.sara.bullet.contact.model.AbsContactItem;
import com.smartisanos.sara.bullet.contact.model.ContactItem;
import com.smartisanos.sara.bullet.contact.model.LabelItem;
import com.smartisanos.sara.bullet.contact.model.VoiceSearchResult;
import com.smartisanos.sara.bullet.contact.presenter.IPickContactPresenter;
import com.smartisanos.sara.bullet.contact.presenter.PickContactPresenter;
import com.smartisanos.sara.bullet.util.DisplayUtils;
import com.smartisanos.sara.bullet.widget.LoadMoreRecyclerView;
import com.smartisanos.sara.bullet.widget.letterIndex.LetterIndexView;
import com.smartisanos.sara.bullet.widget.letterIndex.LivIndexRecycleView;
import com.smartisanos.sara.util.LogUtils;

import java.util.ArrayList;
import java.util.List;

import me.everything.android.ui.overscroll.OverScrollDecoratorHelper;

public class PickContactView extends RelativeLayout implements IPickContactView {

    private static final String TAG = "VoiceAss.PickContactView";

    public static final int SOURCE_TYPE_FRIEND = 1;
    public static final int SOURCE_TYPE_RECENT = 2;
    public static final int SOURCE_TYPE_STAR_FRIEND = 4;
    public static final int SOURCE_TYPE_SEARCH_FRIEND = 6;

    public static final int SEARCH_TYPE_VOICE = 1;
    public static final int SEARCH_TYPE_TEXT = 2;

    private IPickContactPresenter contactPresenter;
    private PickContactAdapter pickContactAdapter;
    private LoadMoreRecyclerView recyclerView;
    private GridLayoutManager gridLayoutManager;
    private OnPickContactListener listener;
    private int numColumns;
    private boolean mLoadMore;
    private boolean mShowLabel;
    private OnScrollListener mOnScrollListener;

    public PickContactView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public PickContactView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        final TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.ContactSelectView);
        numColumns = a.getInt(R.styleable.ContactSelectView_numColumns, 1);
        boolean multiple = a.getBoolean(R.styleable.ContactSelectView_multiple, false);
        boolean showLivIndex = a.getBoolean(R.styleable.ContactSelectView_showLivIndex, false);
        mShowLabel = a.getBoolean(R.styleable.ContactSelectView_showLabel, true);
        mLoadMore = a.getBoolean(R.styleable.ContactSelectView_loadMore, false);
        int sourceType = a.getInt(R.styleable.ContactSelectView_sourceType, SOURCE_TYPE_FRIEND);
        int searchType = a.getInt(R.styleable.ContactSelectView_searchType, SEARCH_TYPE_VOICE);
        a.recycle();

        LayoutInflater.from(context).inflate(R.layout.pick_contact_layout, this, true);
        initView(multiple);
        initLivIndex(showLivIndex);
        initPresent(mShowLabel, sourceType, searchType);
    }

    private void initView(boolean multiple) {
        recyclerView = (LoadMoreRecyclerView)findViewById(R.id.recycler_view);
        gridLayoutManager = new GridLayoutManager(getContext(), numColumns);
        gridLayoutManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                int type = pickContactAdapter.getItem(position).getItemType();
                if (type == AbsContactItem.ItemType.LABEL) {
                    return numColumns;
                } else {
                    return 1;
                }
            }
        });
        pickContactAdapter = new PickContactAdapter(getContext(), new ArrayList<AbsContactItem>());
        pickContactAdapter.setMultiple(multiple);
        recyclerView.setAdapter(pickContactAdapter);
        recyclerView.setLayoutManager(gridLayoutManager);
        recyclerView.setOnYScrollChageListener(new LoadMoreRecyclerView.OnYScrollChageListener() {
            @Override
            public void onYScrolled() {
                if (mOnScrollListener != null) {
                    mOnScrollListener.onScrolled();
                }
            }
        });

        pickContactAdapter.setOnItemClickListener(new PickContactAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(View v, int position) {
                LogUtils.d(TAG, "recycle view item click position = " + position);
                ContactItem account = getAccount(position);
                if (account == null || TextUtils.isEmpty(account.getContactId())) {
                    return;
                }
                account.setChecked(!account.isChecked());
                if (listener != null) {
                    listener.OnCheckContact(account, v);
                }
                if (pickContactAdapter.hasCheckedEffect()) {
                    refresh(account);
                }
            }
        });

        OverScrollDecoratorHelper.setUpOverScroll(recyclerView, OverScrollDecoratorHelper.ORIENTATION_VERTICAL);
    }

    @Override
    public void setLoadMoreEnable(final boolean enable) {
        post(new Runnable() {
            @Override
            public void run() {
                if (recyclerView != null && mLoadMore && enable) {
                    recyclerView.setLoadMoreListener(new LoadMoreRecyclerView.AutoLoadMoreListener() {
                        @Override
                        public void onLoadMore(int currentPage, int pageSize) {
                            query("", currentPage, pageSize);
                        }
                    });
                }
            }
        });

    }

    private ContactItem getAccount(int position) {
        AbsContactItem item = (AbsContactItem) pickContactAdapter.getData().get(position);
        if (item != null && item instanceof ContactItem) {
            return (ContactItem) item;
        }
        return null;
    }

    private void initLivIndex(boolean showLivIndex) {
        // 字母导航
        TextView letterHit = (TextView) findViewById(R.id.tv_hit_letter);
        LetterIndexView idxView = (LetterIndexView) findViewById(R.id.liv_index);
        idxView.setLetters(getResources().getStringArray(R.array.letter_list));
        ImageView imgBackLetter = (ImageView) findViewById(R.id.img_hit_letter);
        LivIndexRecycleView livIndex = pickContactAdapter.createLivIndex(recyclerView, idxView, letterHit, imgBackLetter);

        if (showLivIndex) {
            livIndex.show();
        } else {
            livIndex.hide();
            RelativeLayout.LayoutParams params = ((RelativeLayout.LayoutParams) recyclerView.getLayoutParams());
            params.setMargins(DisplayUtils.dp2px(7), params.topMargin, DisplayUtils.dp2px(11), params.bottomMargin);
        }
    }

    private void initPresent(boolean showLabel, int sourceType, int searchType) {
        contactPresenter = new PickContactPresenter();
        contactPresenter.attachView(this);
        contactPresenter.setShowLabel(showLabel);
        contactPresenter.setSourceType(sourceType);
        contactPresenter.setSearchType(searchType);
    }

    private void initPresenter() {
        contactPresenter = new PickContactPresenter();
        contactPresenter.attachView(this);
    }

    public void query(String keyword, int currentPage, int pageSize) {
        LogUtils.d(TAG, "query()");
        if (contactPresenter == null) {
            initPresenter();
        }
        pickContactAdapter.setSearchText(keyword);
        contactPresenter.setQuery(keyword, currentPage, pageSize);
    }

    public void preLoadContact() {
        LogUtils.d(TAG, "preLoadContact()");
        if (null != contactPresenter) {
            contactPresenter.preLoad();
        }
    }

    public void query(VoiceSearchResult result, int currentPage, int pageSize) {
        LogUtils.d(TAG, "query()");
        if (contactPresenter == null) {
            initPresenter();
        }
        if (null != result) {
            if (null != result.getContactStruct() && result.getContactStruct().size() > 0) {
                pickContactAdapter.setSearchText(result);
            } else {
                pickContactAdapter.setSearchText(result.getResultString());
            }
        }
        contactPresenter.setQuery(result, currentPage, pageSize);
    }

    public void query(String keyword) {
        query(keyword, recyclerView.getCurrentPage(), recyclerView.getPageSize());
    }

    public void query(VoiceSearchResult keyword) {
        query(keyword, recyclerView.getCurrentPage(), recyclerView.getPageSize());
    }

    public void onDestroy() {
        contactPresenter.detachView();
    }

    public void resetData() {
        if (null != pickContactAdapter) {
            pickContactAdapter.getData().clear();
            pickContactAdapter.notifyDataSetChanged();
        }
    }

    public void setSpanCount(int numColumns) {
        if (gridLayoutManager != null) {
            gridLayoutManager.setSpanCount(numColumns);
        }
    }

    public void setShowMultiple(boolean multiple) {
        pickContactAdapter.setMultiple(multiple);
    }

    public boolean isMultiple() {
        return pickContactAdapter.isMultiple();
    }

    public void setSourceType(int sourceType) {
        contactPresenter.setSourceType(sourceType);
    }

    public void setSearchType(int searchType) {
        contactPresenter.setSearchType(searchType);
    }

    public void setAbortPreTask(boolean abortPreTask) {
        contactPresenter.setAbortPreTask(abortPreTask);
    }

    public void setChecked(String contactId, boolean checked) {
        for (int i = 0; i < pickContactAdapter.getItemCount(); i++) {
            AbsContactItem item = pickContactAdapter.getItem(i);
            if (item.getItemType() != AbsContactItem.ItemType.LABEL && TextUtils.equals(item.getContactId(), contactId)) {
                item.setChecked(checked);
            }
        }
        pickContactAdapter.notifyDataSetChanged();
    }

    public boolean isShowLabel() {
        return mShowLabel;
    }

    public void resetDataStatus(AbsContactItem disChecked) {
        for (int i = 0; i < pickContactAdapter.getItemCount(); i++) {
            AbsContactItem item = pickContactAdapter.getItem(i);
            if (disChecked.getContactId().equals(item.getContactId())) {
                item.setChecked(false);
                pickContactAdapter.notifyItemChanged(i);
                break;
            }
        }
    }

    private void refresh(ContactItem contactItem) {
        for (int i = 0; i < pickContactAdapter.getItemCount(); i++) {
            AbsContactItem item = pickContactAdapter.getItem(i);
            if (contactItem.equals(item)) {
                pickContactAdapter.notifyItemChanged(i);
                return;
            }
        }
    }

    @Override
    public void refreshContactList(List<AbsContactItem> data) {
        pickContactAdapter.setNewData(data);
        if (listener != null) listener.onResultSize(data == null ? 0 : data.size());
    }

    @Override
    public void addContactList(List<AbsContactItem> data) {
        if (data == null || data.size() == 0 || isLastSpaceLabelItem(data)) {
            recyclerView.notifyLoadMoreFailed();
        } else {
            if (pickContactAdapter.getData().size() > 0) {
                pickContactAdapter.getData().remove(pickContactAdapter.getData().size() - 1);
            }
            pickContactAdapter.addNewData(data);
            if (listener != null) {
                listener.onResultSize(pickContactAdapter.getItemCount());
            }
            //因为每次分页返回时候，在实际返回数据上多加一条空的LabelItem占位,
            // 所以data.size()-1 才是实际返回数据条数
            if ((data.size() - 1) < recyclerView.getPageSize()) {
                recyclerView.notifyLoadMoreFailed();
            } else {
                recyclerView.notifyLoadMoreSuccessful();
            }
        }
    }

    private boolean isLastSpaceLabelItem(List<AbsContactItem> data) {
        return data != null && (data.size() == 1) && (data.get(0) instanceof LabelItem)
                && TextUtils.isEmpty(((LabelItem) data.get(0)).getText());
    }


    @Override
    public RecyclerView.Adapter getAdapter() {
        return pickContactAdapter;
    }

    public void setOnPickContactListener(OnPickContactListener listener) {
        this.listener = listener;
    }

    public void setHasCheckEffect(boolean checkEffect) {
        pickContactAdapter.setHasCheckedEffect(checkEffect);
    }

    public interface OnPickContactListener {
        void onResultSize(int size);

        void OnCheckContact(AbsContactItem absContactItem, View view);
    }

    public void setOnScrollListener(OnScrollListener onScrollListener) {
        this.mOnScrollListener = onScrollListener;
    }

}
