package com.smartisanos.sara.shell;

import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.text.TextUtils;

import com.smartisanos.sara.entity.ShortcutApp;

import java.util.ArrayList;
import java.util.List;

import com.smartisanos.sara.R;
import com.smartisanos.sara.widget.AppPickerSubView;


public class ShortcutAppDrawerAdapter extends BaseAdapter {

    private List<ShortcutApp> mSaveShareList = new ArrayList<ShortcutApp>();
    private Context mContext;


    public ShortcutAppDrawerAdapter(Context context,List<ShortcutApp> saveShareList) {
        mContext = context;
        mSaveShareList = saveShareList;
    }

    @Override
    public int getCount() {
        return mSaveShareList.size();
    }

    public void setData(List<ShortcutApp> list){
        mSaveShareList = list;
    }

    @Override
    public Object getItem(int position) {
        return mSaveShareList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View view, ViewGroup viewGroup) {
        ViewHolder mHolder = null;
        if (view == null) {
            mHolder = new ViewHolder();
            view = LayoutInflater.from(mContext).inflate(R.layout.shortcut_app_item, null);
            mHolder.spaceViewTop = view.findViewById(R.id.space_top);
            mHolder.subViews[0] = (AppPickerSubView) view.findViewById(R.id.shortcut_picker_sub_view_1);
            mHolder.subViews[1] = (AppPickerSubView) view.findViewById(R.id.shortcut_picker_sub_view_2);
            mHolder.subViews[2] = (AppPickerSubView) view.findViewById(R.id.shortcut_picker_sub_view_3);
            mHolder.subViews[3] = (AppPickerSubView) view.findViewById(R.id.shortcut_picker_sub_view_4);
            mHolder.view = view;
            view.setTag(mHolder);
            mHolder.subViews[0].setOnClickListener(mOnClickListener);
            mHolder.subViews[1].setOnClickListener(mOnClickListener);
            mHolder.subViews[2].setOnClickListener(mOnClickListener);
            mHolder.subViews[3].setOnClickListener(mOnClickListener);
        } else {
            mHolder = (ViewHolder) view.getTag();
        }

        if (position == 0 || position == getCount() - 1) {
            mHolder.updateSpace(View.GONE);
        } else {
            mHolder.updateSpace(View.VISIBLE);
        }

        int index = position * 4;
        for (int i = 0; i < 4; ++i) {
            final AppPickerSubView apsv = mHolder.subViews[i];
            final int pos = index + i;
            if (pos < mSaveShareList.size()) {
                final ShortcutApp item = mSaveShareList.get(pos);
                if (TextUtils.isEmpty(item.getDispalyName())) {
                    apsv.setVisibility(View.INVISIBLE);
                    continue;
                }
                apsv.setVisibility(View.VISIBLE);
                apsv.setText(item.getDispalyName());
                apsv.setTag(item);
                Drawable icon = item.getDrawable();
                if (icon == null) {
                    try {
                        icon = mContext.getPackageManager().getActivityIcon(item.getComponentName());
                    } catch (PackageManager.NameNotFoundException e) {
                    }

                    if (icon == null) {
                        icon = mContext.getDrawable(R.drawable.share_item_jd);
                    }
                }
                apsv.setImageDrawable(icon);
            } else {
                apsv.setVisibility(View.INVISIBLE);
            }
        }

        return view;
    }

    private View.OnClickListener mOnClickListener = new View.OnClickListener() {
        public void onClick(View v) {
            final AppPickerSubView apsv = (AppPickerSubView)v;
            ShortcutApp item = (ShortcutApp)apsv.getTag();
            mShareIconClickListener.onIconClick(item);
        }
    };

    class ViewHolder {
        View view;
        View spaceViewTop;
        AppPickerSubView[] subViews = new AppPickerSubView[4];

        public void updateSpace(int visible) {
            spaceViewTop.setVisibility(visible);
        }
    }

    public void setShareIconClickListener(onShareIconClickListener shareIconClickListener) {
        this.mShareIconClickListener = shareIconClickListener;
    }

    private onShareIconClickListener mShareIconClickListener;
    public static interface onShareIconClickListener {
        public void onIconClick(ShortcutApp item);
    }

}