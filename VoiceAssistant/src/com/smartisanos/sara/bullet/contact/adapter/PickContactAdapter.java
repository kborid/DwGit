package com.smartisanos.sara.bullet.contact.adapter;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.smartisanos.sara.R;
import com.smartisanos.sara.bullet.contact.holder.PhoneContactPhotoManager;
import com.smartisanos.sara.bullet.contact.holder.ViewHolderBase;
import com.smartisanos.sara.bullet.contact.holder.ViewHolderContact;
import com.smartisanos.sara.bullet.contact.holder.ViewHolderLabel;
import com.smartisanos.sara.bullet.contact.model.AbsContactItem;
import com.smartisanos.sara.bullet.contact.view.PickContactView;
import com.smartisanos.sara.bullet.widget.letterIndex.LetterIndexView;
import com.smartisanos.sara.bullet.widget.letterIndex.LivIndexRecycleView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class PickContactAdapter<T> extends RecyclerView.Adapter<ViewHolderBase> implements View.OnClickListener {

    private boolean isMultiple = false ;
    protected final HashMap<String, Integer> indexes = new HashMap<>();
    private boolean mHasCheckedEffect = true;
    private LivIndexRecycleView mLivIndexRecycleView;
    private Context mContext;
    private List<AbsContactItem> lists = new ArrayList<>();
    private T mSearchText;
    private int mSearchType = PickContactView.SEARCH_TYPE_TEXT;

    private PhoneContactPhotoManager mPhotoManager;

    public PickContactAdapter(Context context, List<AbsContactItem> lists) {
        this.mContext = context;
        this.lists = lists;
        mPhotoManager = PhoneContactPhotoManager.getInstance();
    }

    public Context getContext() {
        return mContext;
    }

    public void setSearchText(T search) {
        mSearchText = search;
    }

    public T getSearchText() {
        return mSearchText;
    }

    public void setSearchType(int searchType) {
        mSearchType = searchType;
    }

    public int getSearchType() {
        return mSearchType;
    }

    @Override
    public ViewHolderBase onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = null;
        ViewHolderBase holder = null;
        switch (viewType) {
            case AbsContactItem.ItemType.SEARCH:
                if (PickContactView.SEARCH_TYPE_VOICE == mSearchType) {
                    view = LayoutInflater.from(mContext).inflate(R.layout.rv_search_item, parent, false);
                } else {
                    view = LayoutInflater.from(mContext).inflate(R.layout.rv_contact_item, parent, false);
                }
                holder = new ViewHolderContact(view);
                break;
            case AbsContactItem.ItemType.LABEL:
                view = LayoutInflater.from(mContext).inflate(R.layout.rv_label_item, parent, false);
                holder =  new ViewHolderLabel(view);
                break;
            case AbsContactItem.ItemType.CONTACT:
            default:
                view = LayoutInflater.from(mContext).inflate(R.layout.rv_contact_item, parent, false);
                holder = new ViewHolderContact(view);
                break;
        }
        view.setOnClickListener(this);
        return holder;
    }

    @Override
    public void onBindViewHolder(ViewHolderBase holder, int position) {
        holder.refreshView(this, lists.get(position));
        holder.itemView.setTag(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    public AbsContactItem getItem(int position) {
        return lists.get(position);
    }

    @Override
    public int getItemCount() {
        return lists.size();
    }

    @Override
    public int getItemViewType(int position) {
        return lists.get(position).getItemType();
    }

    public List<AbsContactItem> getData() {
        return lists;
    }

    public void setMultiple(boolean multiple) {
        isMultiple = multiple;
    }

    public boolean isMultiple() {
        return isMultiple;
    }

    public LivIndexRecycleView createLivIndex(RecyclerView recyclerView, LetterIndexView idxView, TextView letterHit, ImageView imgBackLetter) {
        mLivIndexRecycleView = new LivIndexRecycleView(recyclerView, idxView, letterHit, imgBackLetter, getIndexes());
        return mLivIndexRecycleView;
    }

    public void updateIndexes(Map<String, Integer> indexes) {
        // CLEAR
        this.indexes.clear();
        // SET
        this.indexes.putAll(indexes);
        if(mLivIndexRecycleView != null) {
            mLivIndexRecycleView.updateIndexes(indexes);
        }
    }

    public HashMap<String, Integer> getIndexes() {
        return indexes;
    }

    public void setNewData(List<AbsContactItem> data) {
        lists = data;
        notifyDataSetChanged();
    }

    public void addNewData(List<AbsContactItem> data) {
        lists.addAll(data);
        notifyDataSetChanged();
    }

    public void setHasCheckedEffect(boolean hasEffect) {
        mHasCheckedEffect = hasEffect;
    }

    public boolean hasCheckedEffect() {
        return mHasCheckedEffect;
    }

    @Override
    public void onClick(View v) {
        if (null != listener) {
            listener.onItemClick(v, (Integer) v.getTag());
        }
    }

    public interface OnItemClickListener {
        void onItemClick(View v, int position);
    }
    private OnItemClickListener listener = null;
    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }


    public PhoneContactPhotoManager getPhotoManager() {
        return mPhotoManager;
    }

}
