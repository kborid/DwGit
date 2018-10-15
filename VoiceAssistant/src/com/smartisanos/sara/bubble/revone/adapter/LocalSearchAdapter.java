package com.smartisanos.sara.bubble.revone.adapter;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.Toast;

import com.smartisanos.sara.R;
import com.smartisanos.sara.bubble.revone.manager.ISettingBase;
import com.smartisanos.sara.bubble.revone.entity.LocalSearchItem;
import com.smartisanos.sara.util.ToastUtil;

import java.util.List;

public class LocalSearchAdapter extends AdapterBase<LocalSearchItem> {
    private ISettingBase mSettingManager;
    public LocalSearchAdapter(Context context, int[] layoutResArrays) {
        super(context, layoutResArrays);
    }

    public LocalSearchAdapter(Context context, int[] layoutResArrays, List<LocalSearchItem> data) {
        super(context, layoutResArrays, data);
    }

    @Override
    protected ViewHolderHelper getAdapterHelper(int position, android.view.View convertView, ViewGroup parent) {
        return ViewHolderHelper.get(mContext, convertView, parent, R.layout.revone_search_settings_item, position);
    }

    @Override
    protected void convert(ViewHolderHelper helper, final LocalSearchItem item, final int position) {
        if (item != null) {
            if (!mItemClickable) {
                helper.getView().setAlpha(item.isChecked() ? 1f : 0.4f);
            } else {
                helper.getView().setAlpha(1f);
            }
            helper.setChecked(R.id.select, item.isChecked());
            helper.setText(R.id.title, item.getDisplayName());
            helper.setClickListener(R.id.select, new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    CheckBox checkbox = (CheckBox) view;
                    boolean checked = checkbox.isChecked();
                    if (!mItemClickable && checked) {
                        checkbox.setChecked(false);
                        ToastUtil.showToast(mContext, R.string.rev_search_reach_upper_limit, Toast.LENGTH_SHORT);
                    } else if (mSettingManager != null) {
                        mDataChanged = true;
                        item.setChecked(checked);
                        mSettingManager.onItemCheckedChange();
                    }
                }
            });
        }
    }

    @Override
    public int getSelectedDataSize() {
        int count = 0;
        if (mData != null) {
            for (LocalSearchItem item : mData) {
                if (item.isChecked()) {
                    count++;
                }
            }
        }
        return count;
    }

    public void setSettingManager(ISettingBase manager) {
        mSettingManager = manager;
    }
}
