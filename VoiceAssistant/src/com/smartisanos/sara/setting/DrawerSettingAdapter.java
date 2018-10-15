package com.smartisanos.sara.setting;

import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.UserHandle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.widget.BaseAdapter;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;
import com.smartisanos.ideapills.common.model.ShareItem;
import com.smartisanos.ideapills.common.util.CommonConstant;
import com.smartisanos.ideapills.common.util.PackageUtils;
import com.smartisanos.ideapills.common.util.PackageManagerCompat;
import com.smartisanos.sara.widget.AppPickerSubView;
import com.smartisanos.sara.R;
import com.smartisanos.sara.util.SaraConstant;
import com.smartisanos.sara.util.ToastUtil;

import smartisanos.api.SettingsSmt;


public class DrawerSettingAdapter extends BaseAdapter {
    private final String TAG = DrawerSettingAdapter.class.getName();
    private List<ShareItem> mShareList = new ArrayList<ShareItem>();
    private List<ShareItem> mSaveShareList = new ArrayList<ShareItem>();
    private Context mContext;
    private boolean mIsClickEnabled = true;
    public DrawerSettingAdapter(Context context) {
        mContext = context;
    }

    @Override
    public int getCount() {
        return (mShareList.size() + 2) / 3;
    }

    @Override
    public Object getItem(int position) {
        return position;
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
            view = LayoutInflater.from(mContext).inflate(R.layout.app_picker_item, null);
            mHolder.spaceViewTop = view.findViewById(R.id.space_top);
            mHolder.subViews[0] = (AppPickerSubView) view.findViewById(R.id.app_picker_sub_view_1);
            mHolder.subViews[1] = (AppPickerSubView) view.findViewById(R.id.app_picker_sub_view_2);
            mHolder.subViews[2] = (AppPickerSubView) view.findViewById(R.id.app_picker_sub_view_3);
            mHolder.view = view;
            view.setTag(mHolder);
            mHolder.subViews[0].setOnClickListener(mOnClickListener);
            mHolder.subViews[1].setOnClickListener(mOnClickListener);
            mHolder.subViews[2].setOnClickListener(mOnClickListener);
        } else {
            mHolder = (ViewHolder) view.getTag();
        }
        if (position == getCount() -1) {
            view.setBackground(mContext.getDrawable(R.drawable.common_bg_bottom));
            mHolder.updateSpace(View.VISIBLE);
        } else {
            view.setBackground(mContext.getDrawable(R.drawable.common_bg_middle));
            mHolder.updateSpace(View.GONE);
        }
        int index = position * 3;
        for (int i = 0; i < 3; ++i) {
            final AppPickerSubView apsv = mHolder.subViews[i];
            final int pos = index + i;
            if (pos < mShareList.size()) {
                final ShareItem item = mShareList.get(pos);
                apsv.setVisibility(View.VISIBLE);
                apsv.setText(item.getDispalyName());
                apsv.setSelected(item.isSelected());
                apsv.setTag(item);
                apsv.setImageDrawable(item.getDrawable(mContext));
            } else {
                apsv.setVisibility(View.INVISIBLE);
            }
        }
        return view;
    }

    public List<ShareItem> updateShareList(boolean isShareEnable) {
        List<ShareItem> shareItems = new ArrayList<>();
        if (isShareEnable) {
            Intent targetIntent = new Intent(Intent.ACTION_SEND);
            targetIntent.setType("text/plain");
            PackageManager pm = mContext.getPackageManager();
            List<ResolveInfo> resolveInfos = pm.queryIntentActivitiesAsUser(targetIntent, PackageManagerCompat.DEFAULT_QUERY_FLAG, UserHandle.USER_OWNER);
            for (ResolveInfo resolveInfo : resolveInfos) {
                String activityName = resolveInfo.activityInfo.name;
                if (resolveInfo.activityInfo.packageName.equals(SaraConstant.CLASS_NAME_IDEAPILL)) {
                    continue;
                }
                if (!PackageUtils.filterConditionForDrawer(resolveInfo.activityInfo)) {
                    continue;
                }
                //1. ignore weibo and GaoDe Map
                if (isWeiboOrGaoDeMap(resolveInfo)) {
                    continue;
                }
                ShareItem shareItem = new ShareItem();
                ShareItem temp = isCheckedItem(activityName);
                if (temp != null) {
                    shareItem.setSelected(true);
                } else {
                    shareItem.setSelected(false);
                }
                shareItem.setComponentName(new ComponentName(resolveInfo.activityInfo.packageName, resolveInfo.activityInfo.name));
                shareItem.setPackageName(resolveInfo.activityInfo.packageName);
                shareItem.setActivityName(resolveInfo.activityInfo.name);
                shareItem.setDispalyName(resolveInfo.loadLabel(pm).toString());
                shareItem.setDrawable(resolveInfo.loadIcon(pm));
                shareItems.add(shareItem);
            }
            //2. add  weibo and GaoDe Map
            shareItems = handleWeiboAndGaoDeMap(shareItems);
        } else {
            shareItems.clear();
        }
        return shareItems;
    }

    public void notifyDataChanged(List<ShareItem> shareItems) {
        if (null != shareItems) {
            if (mShareList.size() > 0) {
                mShareList.clear();
            }
            mShareList.addAll(shareItems);
            notifyDataSetChanged();
        }
    }

    private boolean isWeiboOrGaoDeMap(ResolveInfo resolveInfo) {
        if (resolveInfo.activityInfo.packageName.equals(PackageUtils.COM_AUTONAVI_MINIMAP)
                || resolveInfo.activityInfo.packageName.equals(PackageUtils.COM_SINA_WEIBO)) {
            return true;
        }
        return false;
    }


    private List<ShareItem> handleWeiboAndGaoDeMap(List<ShareItem> mShareList) {
        String activityName;
        String packageName;
        PackageManager packageManager = mContext.getPackageManager();
        try {
            for (int index = 0; index < 2; index++) {
                if (index == 0) {
                    packageName = PackageUtils.COM_SINA_WEIBO;
                    activityName = PackageUtils.COM_SINA_WEIBO_COMPOSERINDE_COMPOSER_DISPATCH_ACTIVITY;
                } else {
                    packageName = PackageUtils.COM_AUTONAVI_MINIMAP;
                    activityName = PackageUtils.COM_AUTONAVI_MAP_ACTIVITY_NEW_MAP_ACTIVITY;
                }

                if (PackageUtils.isAvilibleApp(mContext, packageName)) {
                    ShareItem shareItem = new ShareItem();
                    ShareItem temp = isCheckedItem(activityName);
                    if (temp != null) {
                        shareItem.setSelected(true);
                    } else {
                        shareItem.setSelected(false);
                    }
                    shareItem.setComponentName(new ComponentName(packageName, activityName));
                    shareItem.setPackageName(packageName);
                    shareItem.setActivityName(activityName);

                    ActivityInfo resolveInfo = packageManager.getActivityInfo(shareItem.getComponentName(),
                            ActivityInfo.FLAG_STATE_NOT_NEEDED);
                    shareItem.setDispalyName(resolveInfo.loadLabel(packageManager).toString());
                    mShareList.add(shareItem);
                }
            }
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return mShareList;
    }

    public boolean isExistApp(ShareItem shareItem) {
        return mShareList.contains(shareItem);
    }

    class ViewHolder {
        View view;
        View spaceViewTop;
        AppPickerSubView[] subViews = new AppPickerSubView[3];

        public void updateSpace(int visible) {
            spaceViewTop.setVisibility(visible);
        }
    }

    private View.OnClickListener mOnClickListener = new View.OnClickListener() {
        public void onClick(View v) {
            if (!mIsClickEnabled) {
                return;
            }
            final AppPickerSubView apsv = (AppPickerSubView)v;
            ShareItem item = (ShareItem) apsv.getTag();
            int size = mSaveShareList.size();
            boolean ret = false;
            if (!item.isSelected()) {
                if (size >= CommonConstant.APP_DRAWER_MAX_COUNT) {
                    ToastUtil.showToast(R.string.app_drawer_max_number_alert);
                    return;
                } else {
                    ret = mShareIconClickListener.onIconClick(true, item);
                    if(ret) {
                        mSaveShareList.add(item);
                        item.setSelected(true);
                        apsv.setSelected(true);
                    }
                }
            } else {
                ret = mShareIconClickListener.onIconClick(false, item);
                if(ret) {
                    removeShareItem(item);
                    item.setSelected(false);
                    apsv.setSelected(false);
                }
            }
            if (ret) {
                PackageUtils.saveShareData(mContext, mSaveShareList);
                if (item.getComponentName().equals(new ComponentName(PackageUtils.WECHAT_PACKAGE, PackageUtils.WECHAT_SHARE_ACTIVITY))) {
                    ContentResolver cr = mContext.getContentResolver();
                    Settings.Global.putInt(cr, SettingsSmt.Global.CANCEL_WECHA_FROM_DRAWER_BY_SELF, item.isSelected() ? 0 : 1);
                }
            }
        }
    };

    public onShareIconClickListener getShareIconClickListener() {
        return mShareIconClickListener;
    }

    public void setShareIconClickListener(onShareIconClickListener shareIconClickListener) {
        this.mShareIconClickListener = shareIconClickListener;
    }

    private onShareIconClickListener mShareIconClickListener;
    public static interface onShareIconClickListener {
        public boolean onIconClick(boolean add, ShareItem item);
    }

    public void notifyDataSetChanged() {
        super.notifyDataSetChanged();
    }

    public void updateDefaultShareData() {
        mSaveShareList = PackageUtils.getShareItemListInitDataFirstTime(mContext);
    }

    public void updateShareData(List<ShareItem> shareItems) {
        mSaveShareList = shareItems;
        PackageUtils.saveShareData(mContext, mSaveShareList);
    }

    public List<ShareItem> getSaveShareList() {
        return mSaveShareList;
    }

    public List<ShareItem> getShareList() {
        return mShareList;
    }

    private boolean removeShareItem(ShareItem item){
        if(item != null) {
            for (ShareItem shareitem : mSaveShareList) {
                if (item.getComponentName().equals(shareitem.getComponentName())){
                    mSaveShareList.remove(shareitem);
                    return true;
                }
            }
        }
        return false;
    }

    private ShareItem isCheckedItem(String activityName) {
        if (mSaveShareList == null) {
            return null;
        }
        for (ShareItem shareItem : mSaveShareList) {
            if (shareItem.getActivityName().equals(activityName)) {
                return shareItem;
            }
        }
        return null;
    }

    public void setClickEnable(boolean enable) {
        mIsClickEnabled = enable;
    }
}
