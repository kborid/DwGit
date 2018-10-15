package com.smartisanos.sara.bubble.revone.adapter;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import java.util.ArrayList;
import java.util.List;

public abstract class AdapterBase<T> extends BaseAdapter {
    protected final Context mContext;
    protected List<T> mData;
    protected final int[] mLayoutResArrays;
    protected boolean mItemClickable = true;
    protected boolean mDataChanged = false;

    public AdapterBase(Context context, int[] layoutResArrays) {
        this(context, layoutResArrays, null);
    }

    public AdapterBase(Context context, int[] layoutResArrays, List<T> data) {
        mData = data == null ? new ArrayList<T>() : data;
        mContext = context;
        mLayoutResArrays = layoutResArrays;
    }

    public void setItemClickable(boolean clickable) {
        if (mItemClickable != clickable) {
            mItemClickable = clickable;
            notifyDataSetChanged();
        }
    }

    public void setData(List<T> data) {
        mData = data;
        notifyDataSetChanged();
    }

    public void remove(int arg0) {
        mDataChanged = true;
        mData.remove(arg0);
        notifyDataSetChanged();
    }

    public void insert(T item, int arg0) {
        mDataChanged = true;
        mData.add(arg0, item);
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        return super.getItemViewType(position);
    }

    @Override
    public int getViewTypeCount() {
        return super.getViewTypeCount();
    }

    public void addData(List<T> data) {
        if (data != null) {
            mDataChanged = true;
            mData.addAll(data);
        }
        notifyDataSetChanged();
    }

    public void addData(T data) {
        mDataChanged = true;
        mData.add(data);
        notifyDataSetChanged();
    }

    public List<T> getAllData() {
        return (List<T>) mData;
    }

    @Override
    public int getCount() {
        if (mData == null) {
            return 0;
        }
        return mData.size();
    }

    @Override
    public T getItem(int position) {
        if (position >= mData.size()) {
            return null;
        }
        return mData.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        final ViewHolderHelper helper = getAdapterHelper(position, convertView, parent);
        T item = getItem(position);
        convert(helper, item, position);
        return helper.getView();
    }

    public boolean isDataChanged() {
        return mDataChanged;
    }

    public void resetState() {
        mDataChanged = false;
    }

    protected abstract void convert(ViewHolderHelper helper, T item, int position);

    protected abstract ViewHolderHelper getAdapterHelper(int position, View convertView, ViewGroup parent);

    protected abstract int getSelectedDataSize();
}
