package com.smartisanos.sara.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.FrameLayout;
import android.view.View;
import android.widget.ImageView;

import com.smartisanos.sara.entity.ShortcutApp;
import com.smartisanos.sara.shell.ShortcutAppDrawerAdapter;
import com.smartisanos.sara.shell.ShortcutAppManager;
import com.smartisanos.sara.R;

import java.util.List;


public class ShortcutAppLayout extends FrameLayout {
    private NoScrollListView  mAppList;
    private ShortcutAppDrawerAdapter mAdapter;
    private ImageView mSettingImg;
    List<ShortcutApp> saveShareList;
    private onShareClickListener mOnShareClickListener;
    public ShortcutAppLayout(Context context) {
        super(context);
    }

    public ShortcutAppLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ShortcutAppLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public ShortcutAppLayout(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public static interface onShareClickListener {
        public void onShareCutAppChangedClick();
        public void onIconClick(ShortcutApp item);
    }

    public void setOnShareClickListener(onShareClickListener mOnSettingClickListener) {
        this.mOnShareClickListener = mOnSettingClickListener;
    }

    public void init() {
        mAppList = (NoScrollListView) findViewById(R.id.result_list_view);
        mSettingImg = (ImageView) findViewById(R.id.shortcut_app_setting);
        saveShareList = ShortcutAppManager.getLastSaveShareData(getContext());
        mAdapter = new ShortcutAppDrawerAdapter(mContext,saveShareList);
        mAppList.setAdapter(mAdapter);
        mAdapter.setShareIconClickListener(new ShortcutAppDrawerAdapter.onShareIconClickListener() {
            public void onIconClick(ShortcutApp item) {
                mOnShareClickListener.onIconClick(item);
            }
        });
        mSettingImg.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                mOnShareClickListener.onShareCutAppChangedClick();
            }
        });
    }


    public void notifyDataSetChanged(){
        saveShareList = ShortcutAppManager.getLastSaveShareData(getContext());
        mAdapter.setData(saveShareList);
        mAdapter.notifyDataSetChanged();
    }


    public int countData() {
        return mAdapter != null ? mAdapter.getCount() : 0;
    }
}
