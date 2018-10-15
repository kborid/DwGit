package com.smartisanos.ideapills.common.remind;


import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.smartisanos.ideapills.common.R;
import com.smartisanos.ideapills.common.remind.view.RemindeItemView;

import java.util.List;

class BeforeTimeAdapter extends BaseAdapter {
    private final Context mContext;
    private final List<RemindData.BeforeTime> mList;
    private int mCurrentPosition = 0;

    public BeforeTimeAdapter(Context context, List<RemindData.BeforeTime> list) {
        mContext = context;
        mList = list;
    }

    @Override
    public int getCount() {
        return mList.size();
    }

    @Override
    public Object getItem(int position) {
        return mList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(mContext).inflate(R.layout.remind_item_before_time_layout, null);
        }

        if (position == 0) {
            convertView.setBackgroundResource(R.drawable.remind_time_item_top_bg);
        } else if (position == mList.size() - 1) {
            convertView.setBackgroundResource(R.drawable.remind_time_item_bottom_bg);
        } else {
            convertView.setBackgroundResource(R.drawable.remind_time_item_mid_bg);
        }
        ((RemindeItemView) convertView).setClickListener(new RemindeItemView.ClickListener() {
            @Override
            public void onClick(View view, boolean select) {
                setSelectPosition(position);
                if (mItemClickListener != null) {
                    mItemClickListener.onItemClick(position, select);
                }
            }
        });
        TextView textView = (TextView) convertView.findViewById(R.id.before_time_text);
        ImageView imageView = (ImageView) convertView.findViewById(R.id.select_item_iv);
        textView.setText(mList.get(position).text);
        imageView.setVisibility(mCurrentPosition == position ? View.VISIBLE : View.INVISIBLE);
        return convertView;
    }

    public void setSelectPosition(int pos) {
        this.mCurrentPosition = pos;
        notifyDataSetChanged();
    }

    public long getSelectTime(int position) {
        return mList.get(position).time;
    }

    public int getSelectPosition() {
        return mCurrentPosition;
    }

    private ItemClickListener mItemClickListener;

    public void setItemClickListener(ItemClickListener l) {
        mItemClickListener = l;
    }

    public interface ItemClickListener {
        void onItemClick(int position, boolean select);
    }
}
