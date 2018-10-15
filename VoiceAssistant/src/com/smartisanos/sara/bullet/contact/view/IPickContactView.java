package com.smartisanos.sara.bullet.contact.view;

import android.support.v7.widget.RecyclerView;


import com.smartisanos.sara.bullet.contact.model.AbsContactItem;

import java.util.List;


public interface IPickContactView {

    void refreshContactList(List<AbsContactItem> datas);
    void addContactList(List<AbsContactItem> data);
    void setLoadMoreEnable(boolean enable);

    RecyclerView.Adapter getAdapter();


    public static interface OnScrollListener{
        void onScrolled();
    }
}
