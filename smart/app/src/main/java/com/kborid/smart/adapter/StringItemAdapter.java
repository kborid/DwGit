package com.kborid.smart.adapter;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.kborid.smart.R;

import java.util.ArrayList;
import java.util.List;


public class StringItemAdapter extends RecyclerView.Adapter<StringItemAdapter.ItemViewHolder> implements View.OnClickListener {

    private Context mContext;
    private List<String> lists = new ArrayList<>();

    public StringItemAdapter(Context context, List<String> lists) {
        this.mContext = context;
        this.lists = lists;
    }

    @Override
    public ItemViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(mContext).inflate(R.layout.rv_string_item, parent, false);
        ItemViewHolder holder = new ItemViewHolder(view);
        view.setOnClickListener(this);
        return holder;
    }

    @Override
    public void onBindViewHolder(@NonNull ItemViewHolder holder, int position) {
        holder.itemView.setTag(position);
        StringBuilder text = new StringBuilder();
        String chars = lists.get(position);
        for (int i = 0; i < 30; i++) {
            text.append(chars);
        }
        holder.nameTextView.setText(text.toString());
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public int getItemCount() {
        return lists.size();
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

    public class ItemViewHolder extends RecyclerView.ViewHolder {

        private TextView nameTextView;

        public ItemViewHolder(View itemView) {
            super(itemView);
            nameTextView = (TextView) itemView.findViewById(R.id.textViewName);
        }
    }
}
