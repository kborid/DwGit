package com.smartisanos.sara.bubble.search;

import android.app.Activity;
import android.app.Fragment;
import android.content.Intent;
import android.service.onestep.GlobalBubble;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;

import com.smartisanos.ideapills.common.util.UIHandler;
import com.smartisanos.sara.R;
import com.smartisanos.sara.bubble.search.adapter.SearchViewPagerAdapter;
import com.smartisanos.sara.bubble.search.widget.NoScrollViewPager;

import java.util.ArrayList;
import java.util.List;

import smartisanos.app.voiceassistant.ParcelableObject;

public class SearchView implements ISearchView {

    private static final int SEARCH_RESULT_FRAGMENT = 0;
    private static final int FLASH_IM_CONTACT_FRAGMENT = 1;

    private static final String TAG_FLASH_IM_CONTACT_FRAGMENT = "pager_flash_im_fragment";
    private static final String TAG_SEARCH_RESULT_FRAGMENT = "pager_search_result_fragment";

    protected Activity mActivity;
    protected FrameLayout mResultLayout;

    private NoScrollViewPager mViewPager;
    private SearchViewPagerAdapter mPagerAdapter;

    private int mLocalWeb3MarginTop;
    private int mAvailableHeight;
    private int mDisplayWidth;

    private int mDisplayHeight;
    private int mRealScreenHeight;
    private int mStatusBarHeight;

    public SearchView(Activity mActivity, int displayHeight, int displayWidth, int realScreenHeight, int statusBarHeight) {
        this.mActivity = mActivity;
        this.mDisplayHeight = displayHeight;
        this.mDisplayWidth = displayWidth;
        this.mRealScreenHeight = realScreenHeight;
        this.mStatusBarHeight = statusBarHeight;
        initDimens(mActivity);
    }

    public void initDimens(Activity context) {
        mLocalWeb3MarginTop = context.getResources().getDimensionPixelSize(R.dimen.local_web_3_margin_top);
        int resultTopMargin = context.getResources().getDimensionPixelSize(R.dimen.result_margin_top);
        int resultBottomMargin = context.getResources().getDimensionPixelSize(R.dimen.result_margin_bottom);
        mAvailableHeight = mDisplayHeight - resultTopMargin - resultBottomMargin;
    }

    private void initPagerAdapter() {
        SearchResultFragment searchResultFragment;
        searchResultFragment = (SearchResultFragment) mActivity.getFragmentManager().
                findFragmentByTag(TAG_SEARCH_RESULT_FRAGMENT);
        if (searchResultFragment == null) {
            searchResultFragment =
                    SearchResultFragment.newInstance(mLocalWeb3MarginTop,
                            mAvailableHeight, mRealScreenHeight, mStatusBarHeight);
        }
        FlashImContactsFragment flashImFragment;
        flashImFragment = (FlashImContactsFragment) mActivity.getFragmentManager().
                findFragmentByTag(TAG_FLASH_IM_CONTACT_FRAGMENT);
        if (flashImFragment == null) {
            flashImFragment = FlashImContactsFragment.newInstance();
        }

        List<String> fragmentNameList = new ArrayList<String>();
        fragmentNameList.add(TAG_SEARCH_RESULT_FRAGMENT);
        fragmentNameList.add(TAG_FLASH_IM_CONTACT_FRAGMENT);
        List<Fragment> fragmentList = new ArrayList<Fragment>();
        fragmentList.add(SEARCH_RESULT_FRAGMENT, searchResultFragment);
        fragmentList.add(FLASH_IM_CONTACT_FRAGMENT, flashImFragment);

        mPagerAdapter = new SearchViewPagerAdapter(
                mActivity.getFragmentManager(), fragmentList, fragmentNameList);
        mViewPager.setAdapter(mPagerAdapter);
        mPagerAdapter.notifyDataSetChanged();
    }

    @Override
    public boolean ensureInitialized() {
        if (mResultLayout == null) {
            mResultLayout = (FrameLayout) mActivity.findViewById(R.id.result);
            mViewPager = (NoScrollViewPager) mResultLayout.findViewById(R.id.view_pager);
            initPagerAdapter();
            mViewPager.setTranslationY(0);
            mViewPager.setScroll(false);
            mViewPager.setScrollDuration(300);
            return !mPagerAdapter.canCommitNow();
        } else {
            return !mPagerAdapter.getItem(0).isAdded();
        }
    }

    @Override
    public void resetSearchViewAndData(boolean isResetData, boolean isResetView) {
        SearchResultFragment searchResultFragment = getSearchResultFragment();
        if (isFragmentAdded(searchResultFragment)) {
            searchResultFragment.resetSearchViewAndData(isResetData, isResetView);
        }
        FlashImContactsFragment contactFragment = getFlashImContactsFragment();
        if (isFragmentAdded(contactFragment)) {
            contactFragment.resetBulletState();
        }
        onShowSearchView();
    }

    @Override
    public void loadSearchResult(ParcelableObject result, String tmp) {
        if (mResultLayout.getVisibility() != View.VISIBLE) {
            mResultLayout.setVisibility(View.VISIBLE);
        }
        SearchResultFragment searchResultFragment = getSearchResultFragment();
        if (isFragmentAdded(searchResultFragment)) {
            searchResultFragment.loadSearchResult(result, tmp);
        }
    }

    @Override
    public void refreshBulletContactViewHeight(int resultHeight) {
        FlashImContactsFragment flashImContactsFragment = getFlashImContactsFragment();
        if (isFragmentAdded(flashImContactsFragment)) {
            flashImContactsFragment.refreshView(resultHeight);
        }
    }

    @Override
    public void refreshView(boolean mExistLocalView, boolean mExistWebView, int resultHeight) {
        SearchResultFragment searchResultFragment = getSearchResultFragment();
        if (isFragmentAdded(searchResultFragment)) {
            searchResultFragment.refreshView(mExistLocalView, mExistWebView, resultHeight);
        }
        FlashImContactsFragment flashImContactsFragment = getFlashImContactsFragment();
        if (isFragmentAdded(flashImContactsFragment)) {
            flashImContactsFragment.refreshView(resultHeight);
        }
    }

    @Override
    public boolean hideViewFromAction(int bubbleItemTranslateY, int bubbleItemHeight, boolean bubbleVisiable, int from, boolean finish, Runnable runnable) {
        if (getCurrentPage() == SEARCH_RESULT_FRAGMENT) {
            SearchResultFragment searchResultFragment = getSearchResultFragment();
            if (isFragmentAdded(searchResultFragment)) {
                return searchResultFragment.hideViewFromAction(bubbleItemTranslateY, bubbleItemHeight, bubbleVisiable, from, finish, runnable);
            }
        } else if (getCurrentPage() == FLASH_IM_CONTACT_FRAGMENT) {
            boolean ret = false;
            FlashImContactsFragment fragment = getFlashImContactsFragment();
            if (fragment != null) {
                ret = fragment.hideViewFromAction(bubbleItemTranslateY, from, finish, runnable);
            }
            if (!finish) {
                SearchResultFragment searchResultFragment = getSearchResultFragment();
                if (isFragmentAdded(searchResultFragment)) {
                    ret = searchResultFragment.hideViewFromAction(bubbleItemTranslateY, bubbleItemHeight, bubbleVisiable, from, finish, runnable);
                }
                UIHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        mViewPager.setCurrentItem(SEARCH_RESULT_FRAGMENT, false);
                    }
                });
            }
            return ret;
        }
        return false;
    }

    public void showSearchViewithAnimation(boolean mExistLocalView, boolean mExistWebView) {
        if (getCurrentPage() == SEARCH_RESULT_FRAGMENT) {
            SearchResultFragment searchResultFragment = getSearchResultFragment();
            if (isFragmentAdded(searchResultFragment)) {
                searchResultFragment.showSearchViewithAnimation(mExistLocalView, mExistWebView);
            }
        }
    }

    public boolean clearSearchViewAnimation(boolean isClearAnimation) {
        if (null == mPagerAdapter) {
            return false;
        }
        SearchResultFragment searchResultFragment = getSearchResultFragment();
        if (isFragmentAdded(searchResultFragment)) {
            isClearAnimation = searchResultFragment.clearSearchViewAnimation(isClearAnimation);
        }
        FlashImContactsFragment flashImContactsFragment = getFlashImContactsFragment();
        if (isFragmentAdded(flashImContactsFragment)) {
            isClearAnimation = flashImContactsFragment.clearBulletViewAnimation(isClearAnimation);
        }
        return isClearAnimation;
    }

    private SearchResultFragment getSearchResultFragment() {
        if (mPagerAdapter != null) {
            return (SearchResultFragment) mPagerAdapter.getItem(SEARCH_RESULT_FRAGMENT);
        }
        return null;
    }

    private FlashImContactsFragment getFlashImContactsFragment() {
        if (mPagerAdapter != null) {
            return (FlashImContactsFragment) mPagerAdapter.getItem(FLASH_IM_CONTACT_FRAGMENT);
        }
        return null;
    }

    private int getCurrentPage() {
        if (mViewPager != null) {
            return mViewPager.getCurrentItem();
        }
        return SEARCH_RESULT_FRAGMENT;
    }

    public void hideSearchViewWithAnimation() {
        if (getCurrentPage() == SEARCH_RESULT_FRAGMENT) {
            SearchResultFragment searchResultFragment = getSearchResultFragment();
            if (isFragmentAdded(searchResultFragment)) {
                searchResultFragment.hideSearchViewWithAnimation();
            }
        }
    }

    public boolean checkSearchViewFinish(MotionEvent ev) {
        if (getCurrentPage() == SEARCH_RESULT_FRAGMENT) {
            SearchResultFragment searchResultFragment = getSearchResultFragment();
            if (isFragmentAdded(searchResultFragment)) {
                return searchResultFragment.checkSearchViewFinish(ev);
            }
            return true;
        }
        return false;
    }

    @Override
    public void notifyDataSetChanged() {
        if (getCurrentPage() == SEARCH_RESULT_FRAGMENT) {
            SearchResultFragment searchResultFragment = getSearchResultFragment();
            if (isFragmentAdded(searchResultFragment)) {
                searchResultFragment.notifyDataSetChanged();
            }
        }
    }

    @Override
    public boolean isExistWebView(boolean hasWeb, boolean mExistWebView) {
        SearchResultFragment searchResultFragment = getSearchResultFragment();
        return isFragmentAdded(searchResultFragment) && searchResultFragment.isExistWebView(hasWeb, mExistWebView);
    }

    @Override
    public boolean isExistLocalView(boolean hasLocal, boolean mExistLocalView) {
        SearchResultFragment searchResultFragment = getSearchResultFragment();
        return isFragmentAdded(searchResultFragment) && searchResultFragment.isExistLocalView(hasLocal, mExistLocalView);
    }

    @Override
    public void onShowSearchView() {
        if (mViewPager != null) {
            mViewPager.setCurrentItem(SEARCH_RESULT_FRAGMENT);
        }
    }

    @Override
    public void onShowBulletViewForDefaultSetting() {
        if (null != mViewPager && getCurrentPage() != FLASH_IM_CONTACT_FRAGMENT) {
            //如果设置默认显示子弹短信,则禁止viewpager动画,显示自定动画
            mViewPager.setCurrentItem(FLASH_IM_CONTACT_FRAGMENT, false);
            FlashImContactsFragment contactsFragment = getFlashImContactsFragment();
            if (isFragmentAdded(contactsFragment)) {
                contactsFragment.showBulletViewWithAnim();
            }
        }
    }

    @Override
    public void onShowBulletView() {
        if (mViewPager != null) {
            mViewPager.setCurrentItem(FLASH_IM_CONTACT_FRAGMENT);
        }
    }

    @Override
    public boolean isCurrentBulletShow() {
        return getCurrentPage() == FLASH_IM_CONTACT_FRAGMENT;
    }

    private boolean isFragmentAdded(Fragment fragment) {
        return fragment != null && fragment.isAdded();
    }

    @Override
    public void hideResultView() {
        if (mResultLayout != null) {
            mResultLayout.setVisibility(View.GONE);
        }
    }

    @Override
    public void onActivityResult(int result, Intent data) {
        SearchResultFragment searchResultFragment = getSearchResultFragment();
        if (isFragmentAdded(searchResultFragment)) {
            searchResultFragment.onActivityResult(result, data);
        }
    }
}
