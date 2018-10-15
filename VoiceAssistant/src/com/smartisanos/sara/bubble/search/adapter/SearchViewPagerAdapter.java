package com.smartisanos.sara.bubble.search.adapter;

import android.app.Fragment;
import android.app.FragmentManager;

import java.util.List;

public class SearchViewPagerAdapter extends FragmentPagerAdapter {

    private List<Fragment> mFragmentList;
    private List<String> mFragmentNameList;

    public SearchViewPagerAdapter(FragmentManager fm, List<Fragment> fragmentList, List<String> fragmentNameList) {
        super(fm);
        this.mFragmentList = fragmentList;
        this.mFragmentNameList = fragmentNameList;
    }

    @Override
    public Fragment getItem(int position) {
        return mFragmentList.get(position);
    }

    @Override
    public int getCount() {
        return mFragmentList.size();
    }

    @Override
    public String getFragmentName(int viewId, int position) {
        return mFragmentNameList.get(position);
    }
}
