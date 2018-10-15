package com.smartisanos.sara.setting;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import com.smartisanos.sara.R;

import java.util.List;

import smartisanos.widget.ListContentItemCheck;

import com.smartisanos.sara.setting.TodoOverActivity.TodoOverItem;
import com.smartisanos.sara.util.SaraUtils;

public class TodoOverAdapter extends ArrayAdapter<TodoOverItem> {
    private int mResourceId;
    private LayoutInflater mLayoutInflater;

    public TodoOverAdapter(Context context, int resId, List<TodoOverItem> objects) {
        super(context, resId, objects);
        mResourceId = resId;
        mLayoutInflater = LayoutInflater.from(context);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder viewholder = null;
        if (convertView == null || convertView.getTag() == null) {
            convertView = mLayoutInflater.inflate(mResourceId, null);
            viewholder = new ViewHolder();
            viewholder.itemCheck = (ListContentItemCheck) convertView.findViewById(R.id.item_check);
            convertView.setTag(viewholder);
        } else {
            viewholder = (ViewHolder) convertView.getTag();
        }
        TodoOverItem item = getItem(position);
        viewholder.itemCheck.setTitle(item.title);
        viewholder.itemCheck.setChecked(item.settingsValue == SaraUtils.getTodoOverType(getContext()));
        if (position == 0) {
            viewholder.itemCheck.setBackgroundResource(R.drawable.selector_setting_sub_item_bg_top);
        } else if (position == getCount() - 1) {
            viewholder.itemCheck.setBackgroundResource(R.drawable.selector_setting_sub_item_bg_bottom);
        } else {
            viewholder.itemCheck.setBackgroundResource(R.drawable.selector_setting_sub_item_bg_middle);
        }
        return convertView;
    }

    private static class ViewHolder {
        public ListContentItemCheck itemCheck;
    }
}
