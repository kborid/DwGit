package com.smartisanos.sara.bullet.contact.holder;

import android.support.v7.widget.RecyclerView;
import android.view.View;

import com.smartisanos.sara.bullet.contact.adapter.PickContactAdapter;
import com.smartisanos.sara.bullet.contact.model.AbsContactItem;

public abstract class ViewHolderBase extends RecyclerView.ViewHolder {
    protected PickContactAdapter adapter;

    ViewHolderBase(View itemView) {
        super(itemView);
        inflate(itemView);
    }

    protected void inflate(View view){}
    public abstract void refreshView(PickContactAdapter adapter, AbsContactItem item);
}
