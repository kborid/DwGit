package com.smartisanos.sara.storage;

import android.content.Context;

import com.smartisanos.ideapills.common.model.ShareItem;
import com.smartisanos.ideapills.common.util.MutiTaskHandler;
import com.smartisanos.ideapills.common.util.PackageUtils;
import com.smartisanos.sara.SaraApplication;

import java.util.List;

public enum DrawerDataRepository {
    INSTANCE;
    private List<ShareItem> mSaveShareList;
    private Context mContext;

    DrawerDataRepository() {
        mContext = SaraApplication.getInstance();
    }

    public void reloadAsync() {
        if (mSaveShareList != null) {
            mSaveShareList.clear();
            mSaveShareList = null;
        }
        MutiTaskHandler.post(new Runnable() {
            @Override
            public void run() {
                getDrawerData();
                if (mSaveShareList != null) {
                    for (final ShareItem shareItem : mSaveShareList) {
                        shareItem.getDrawable(mContext);
                    }
                }
            }
        });

    }

    public List<ShareItem> getDrawerData() {
        if (mSaveShareList == null) {
            synchronized (this) {
                if (mSaveShareList == null) {
                    mSaveShareList = PackageUtils.getShareItemListInitDataFirstTime(mContext);
                }
            }
        }
        return mSaveShareList;
    }
}
