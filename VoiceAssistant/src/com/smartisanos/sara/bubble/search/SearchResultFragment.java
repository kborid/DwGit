package com.smartisanos.sara.bubble.search;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.smartisanos.sara.util.AnimManager;
import com.smartisanos.sara.util.LogUtils;
import com.smartisanos.sara.util.SaraConstant;
import com.smartisanos.sara.util.SaraUtils;
import com.smartisanos.sara.R;
import com.smartisanos.sara.widget.LocalSearchLayout;
import com.smartisanos.sara.widget.WebSearchLayout;

import java.util.List;

import smartisanos.app.numberassistant.YellowPageResult;
import smartisanos.app.voiceassistant.ApplicationStruct;
import smartisanos.app.voiceassistant.ContactStruct;
import smartisanos.app.voiceassistant.MediaStruct;
import smartisanos.app.voiceassistant.ParcelableObject;

public class SearchResultFragment extends Fragment {
    public static final String TAG = "VoiceAss.SearchResultFragment";
    View mRootView;
    private LocalSearchLayout mLocalSearchLayout;
    private WebSearchLayout mWebSearchLayout;

    private int mLocalWeb3MarginTop;
    private int mAvailableHeight;

    private int mRealScreenHeight;
    private int mStatusBarHeight;


    public static SearchResultFragment newInstance(int mLocalWeb3MarginTop, int mAvailableHeight,
                                                   int mRealScreenHeight, int mStatusBarHeight) {

        SearchResultFragment fragment = new SearchResultFragment();
        Bundle bundle = buildArguments(mLocalWeb3MarginTop, mAvailableHeight,
                mRealScreenHeight, mStatusBarHeight);
        fragment.setArguments(bundle);
        return fragment;
    }


    private ParcelableObject mParcelableObject;
    private View mResultEmpty;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mRootView = inflater.inflate(R.layout.fragment_search_result, container, false);
        parseArguments(getArguments());
        return mRootView;
    }

    public SearchResultFragment() {
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mResultEmpty = mRootView.findViewById(R.id.result_hide_empty);
        mResultEmpty.setTranslationY(0);
        mLocalSearchLayout = (LocalSearchLayout) mRootView.findViewById(R.id.local_result);
        mLocalSearchLayout.init(view.getContext());
        mWebSearchLayout = (WebSearchLayout) mRootView.findViewById(R.id.web_result);
        mWebSearchLayout.init();
        attachListener(getActivity());
        mLocalSearchLayout.setTranslationY(0);
        mWebSearchLayout.setTranslationY(0);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        attachListener(activity);
    }

    private void attachListener(Context context) {
        if (context instanceof SaraUtils.BaseViewListener) {
            SaraUtils.BaseViewListener listener = (SaraUtils.BaseViewListener) context;
            if (mLocalSearchLayout != null) {
                mLocalSearchLayout.setViewListener(listener);
            }
            if (mWebSearchLayout != null) {
                mWebSearchLayout.setViewListener(listener);
            }
        }
    }

    private static Bundle buildArguments(int mLocalWeb3MarginTop, int mAvailableHeight,
                                         int mRealScreenHeight, int mStatusBarHeight) {
        Bundle bundle = new Bundle();
        bundle.putInt("localWeb3MarginTop", mLocalWeb3MarginTop);
        bundle.putInt("availableHeight", mAvailableHeight);
        bundle.putInt("realScreenHeight", mRealScreenHeight);
        bundle.putInt("statusBarHeight", mStatusBarHeight);
        return bundle;
    }

    private void parseArguments(Bundle bundle) {
        mLocalWeb3MarginTop = bundle.getInt("localWeb3MarginTop");
        mAvailableHeight = bundle.getInt("availableHeight");
        mRealScreenHeight = bundle.getInt("realScreenHeight");
        mStatusBarHeight = bundle.getInt("statusBarHeight");
    }

    public int setEmptyViewHeight(int height) {
        if (mResultEmpty == null) {
            LogUtils.d(TAG, "mResultEmpty cannot be null");
            return -1;
        }
        FrameLayout.LayoutParams emptyParams = (FrameLayout.LayoutParams) mResultEmpty.getLayoutParams();
        emptyParams.height = height;
        mResultEmpty.setLayoutParams(emptyParams);
        mResultEmpty.setVisibility(View.VISIBLE);
        return emptyParams.height;
    }


    public void refreshView(boolean mExistLocalView, boolean mExistWebView, int resultHeight) {
        resultHeight = setEmptyViewHeight(resultHeight);
        if (resultHeight == -1) {
            return;
        }
        mLocalSearchLayout.setTranslationY(0);
        mWebSearchLayout.setTranslationY(0);
        if (mExistLocalView && mExistWebView) {
            adjustSearchViewHeight(resultHeight);
            mLocalSearchLayout.setVisibility(View.VISIBLE);
            mLocalSearchLayout.showHideView();
            mWebSearchLayout.setVisibility(View.VISIBLE);
        } else if (mExistLocalView && mLocalSearchLayout != null && mLocalSearchLayout.hasData()) {
            FrameLayout.LayoutParams localParams = (FrameLayout.LayoutParams) mLocalSearchLayout.getLayoutParams();
            localParams.height = mAvailableHeight - resultHeight - mLocalWeb3MarginTop;
            localParams.topMargin = resultHeight + mLocalWeb3MarginTop - getResources().getDimensionPixelOffset(R.dimen.bubble_local_reduce_gap);
            mLocalSearchLayout.setLayoutParams(localParams);
            mLocalSearchLayout.setVisibility(View.VISIBLE);
            mWebSearchLayout.setVisibility(View.GONE);
        } else if (mExistWebView) {
            FrameLayout.LayoutParams webParams = (FrameLayout.LayoutParams) mWebSearchLayout.getLayoutParams();
            webParams.height = mAvailableHeight - resultHeight - mLocalWeb3MarginTop;
            webParams.topMargin = resultHeight + mLocalWeb3MarginTop - getResources().getDimensionPixelOffset(R.dimen.bubble_local_reduce_gap);
            mWebSearchLayout.setLayoutParams(webParams);
            mLocalSearchLayout.setVisibility(View.GONE);
            mWebSearchLayout.setVisibility(View.VISIBLE);
        } else {
            mLocalSearchLayout.setVisibility(View.GONE);
            mWebSearchLayout.setVisibility(View.GONE);
        }

        if (resultHeight <= 0) {
            mResultEmpty.setVisibility(View.GONE);
        }
    }

    public void resetSearchViewAndData(boolean isResetData, boolean isResetView) {

        if (isResetData) {
            resetSearchData();
        }
        if (isResetView) {
            resetSearchView();
        }
    }

    public void loadSearchResult(ParcelableObject result, String tmp) {
        if (result != null) {
            loadLocalSearchResult(result);
        } else {
            loadWebSearchResult(tmp);
        }
    }

    private void adjustSearchViewHeight(int resultHeight) {

        if (mParcelableObject == null) {
            return;
        }
        List<ApplicationStruct> app = mParcelableObject.getApps();
        List<ContactStruct> contact = mParcelableObject.getContacts();
        List<YellowPageResult> yellow = mParcelableObject.getYellowPages();
        List<MediaStruct> music = mParcelableObject.getMusics();
        int titleHeight = getResources().getDimensionPixelSize(R.dimen.local_title_height);
        int itemHeight = getResources().getDimensionPixelSize(R.dimen.local_item_height);
        int targetHeight = getResources().getDimensionPixelOffset(R.dimen.local_titlebar_height) + getResources().getDimensionPixelOffset(R.dimen.local_bottom_height);
        targetHeight += app.size() > 0 ? titleHeight : 0;
        targetHeight += contact.size() > 0 ? titleHeight : 0;
        targetHeight += yellow.size() > 0 ? titleHeight : 0;
        targetHeight += music.size() > 0 ? titleHeight : 0;
        for (ApplicationStruct a : app) {
            targetHeight += itemHeight;
        }
        for (ContactStruct c : contact) {
            targetHeight += itemHeight;
        }
        for (YellowPageResult y : yellow) {
            targetHeight += itemHeight;
        }
        for (MediaStruct m : music) {
            targetHeight += itemHeight;
        }

        int restHeight = 0;

        FrameLayout.LayoutParams localParams = (FrameLayout.LayoutParams) mLocalSearchLayout.getLayoutParams();
        int maxHeight = (mAvailableHeight - resultHeight) / 2 - mLocalWeb3MarginTop;
        if (targetHeight > maxHeight) {
            localParams.height = (mAvailableHeight - resultHeight) / 2 - mLocalWeb3MarginTop;
        } else {
            localParams.height = targetHeight;
            restHeight = maxHeight - targetHeight;
        }
        localParams.topMargin = resultHeight + mLocalWeb3MarginTop - getResources().getDimensionPixelOffset(R.dimen.bubble_local_reduce_gap);
        mLocalSearchLayout.setLayoutParams(localParams);

        FrameLayout.LayoutParams webParams = (FrameLayout.LayoutParams) mWebSearchLayout.getLayoutParams();
        webParams.height = (mAvailableHeight - resultHeight) / 2 - mLocalWeb3MarginTop + restHeight;
        webParams.topMargin = localParams.topMargin + localParams.height + mLocalWeb3MarginTop;
        mWebSearchLayout.setLayoutParams(webParams);
    }


    public boolean isExistWebView(boolean hasWeb, boolean mExistWebView) {
        if (getActivity() == null) {
            return mExistWebView;
        }
        boolean isWebEnabled = SaraUtils.getWebInputEnabled(getActivity());
        if (isWebEnabled && mWebSearchLayout != null && hasWeb && SaraUtils.isNetworkConnected()) {
            if (!TextUtils.isEmpty(mWebSearchLayout.getSearchText())) {
                mExistWebView = true;
            } else {
                mExistWebView = false;
            }
        } else {
            mExistWebView = false;
        }

        return mExistWebView;
    }

    public boolean isExistLocalView(boolean hasLocal, boolean mExistLocalView) {
        if (getActivity() == null) {
            return mExistLocalView;
        }
        boolean isLocalEnabled = SaraUtils.getLocalInputEnabled(getActivity());
        if (isLocalEnabled && mLocalSearchLayout != null && hasLocal) {
            mLocalSearchLayout.notifyDataSetChanged();
            boolean hasData = mLocalSearchLayout.hasData();
            if (hasData) {
                mExistLocalView = true;
            } else {
                mExistLocalView = false;
            }
        } else {
            mExistLocalView = false;
        }
        return mExistLocalView;
    }

    public boolean hideViewFromAction(int bubbleItemTranslateY, int bubbleItemHeight, boolean bubbleVisiable, int from, boolean finish, Runnable runnable) {

        if (from == 0) {
            if (mWebSearchLayout != null && mWebSearchLayout.getVisibility() == View.VISIBLE && mLocalSearchLayout != null && mLocalSearchLayout.getVisibility() == View.VISIBLE) {
                if (finish) {
                    AnimManager.HideViewWithAlphaAnim(mResultEmpty, 200, 250);
                    AnimManager.HideViewWithAlphaAnim(mLocalSearchLayout, 200, 250);
                    AnimManager.HideViewWithAlphaAnim(mWebSearchLayout, 200, 250);
                } else {
                    AnimManager.hideViewWithAlphaAndTranslate(mResultEmpty, 0, 250, bubbleItemTranslateY);
                    setSearchLayoutParams(mLocalSearchLayout, bubbleItemHeight, 0, false);
                    setSearchLayoutParams(mWebSearchLayout, bubbleItemHeight, 0, false);
                }
            } else if (mLocalSearchLayout != null && mLocalSearchLayout.getVisibility() == View.VISIBLE) {
                if (finish) {
                    AnimManager.HideViewWithAlphaAnim(mResultEmpty, 200, 250);
                    AnimManager.HideViewWithAlphaAnim(mLocalSearchLayout, 200, 250);
                } else {
                    AnimManager.hideViewWithAlphaAndTranslate(mResultEmpty, 0, 250, bubbleItemTranslateY);
                    setSearchLayoutParams(mLocalSearchLayout, bubbleItemHeight, 0, true);
                    mLocalSearchLayout.hideHideView();
                }
            } else if (mWebSearchLayout != null && mWebSearchLayout.getVisibility() == View.VISIBLE) {
                if (finish) {
                    AnimManager.HideViewWithAlphaAnim(mResultEmpty, 200, 250);
                    AnimManager.HideViewWithAlphaAnim(mWebSearchLayout, 200, 250);
                } else {
                    AnimManager.hideViewWithAlphaAndTranslate(mResultEmpty, 0, 250, bubbleItemTranslateY);
                    setSearchLayoutParams(mWebSearchLayout, bubbleItemHeight, 0, true);
                }
            } else {
                return true;
            }
            return false;

        } else if (from == 1) {
            if (bubbleVisiable
                    && mWebSearchLayout.getVisibility() == View.VISIBLE) {
                runnable.run();
                AnimManager.hideViewWithAlphaAndTranslate(mResultEmpty, 0, 200, bubbleItemTranslateY);
                AnimManager.hideViewWithAlphaAndTranslate(mWebSearchLayout, 0, 200, 150);
                setSearchLayoutParams(mLocalSearchLayout, bubbleItemHeight, 0, true);
            } else if (bubbleVisiable) {
                runnable.run();
                AnimManager.hideViewWithAlphaAndTranslate(mResultEmpty, 0, 200, bubbleItemTranslateY);
                setSearchLayoutParams(mLocalSearchLayout, bubbleItemHeight, 0, true);
            } else if (mWebSearchLayout.getVisibility() == View.VISIBLE) {
                AnimManager.hideViewWithAlphaAndTranslate(mWebSearchLayout, 0, 200, 150);
                setSearchLayoutParams(mLocalSearchLayout, bubbleItemHeight, 0, true);
            }
        } else if (from == 2) {
            if (bubbleVisiable
                    && mLocalSearchLayout.getVisibility() == View.VISIBLE) {
                runnable.run();
                AnimManager.hideViewWithAlphaAndTranslate(mResultEmpty, 0, 200, bubbleItemTranslateY);
                AnimManager.hideViewWithAlphaAndTranslate(mLocalSearchLayout, 0, 200, -150);
                setSearchLayoutParams(mWebSearchLayout, bubbleItemHeight, 0, true);
            } else if (bubbleVisiable) {
                runnable.run();
                AnimManager.hideViewWithAlphaAndTranslate(mResultEmpty, 0, 250, bubbleItemTranslateY);
                setSearchLayoutParams(mWebSearchLayout, bubbleItemHeight, 0, true);
            } else if (mLocalSearchLayout.getVisibility() == View.VISIBLE) {
                AnimManager.hideViewWithAlphaAndTranslate(mLocalSearchLayout, 0, 200, -150);
                setSearchLayoutParams(mWebSearchLayout, bubbleItemHeight, 0, true);
            }
        }
        return false;
    }

    public void setSearchLayoutParams(View view, int bubbleItemHeight, int delayTime, boolean single) {

        int targetHeight;
        int targetTranslateY = 0;
        if (!single) {
            if (view instanceof WebSearchLayout) {
                targetHeight = mAvailableHeight - mLocalSearchLayout.getHeight() - mLocalWeb3MarginTop / 2;
                targetTranslateY = -(mAvailableHeight - mLocalSearchLayout.getHeight() - view.getHeight()) + mLocalWeb3MarginTop / 2;
            } else {
                targetHeight = view.getHeight();
                targetTranslateY = -(mAvailableHeight - mWebSearchLayout.getHeight() - view.getHeight()) + mLocalWeb3MarginTop / 2;
            }
        } else {
            targetHeight = mAvailableHeight + mLocalWeb3MarginTop / 2;
            if (view instanceof WebSearchLayout) {
                targetTranslateY = -(mAvailableHeight - view.getHeight()) - mLocalWeb3MarginTop / 2;
            } else {
                targetTranslateY = -(bubbleItemHeight + mLocalWeb3MarginTop);
                int expandType = mLocalSearchLayout.getExpandType();
                if (expandType != LocalSearchLayout.EXPAND_FULL) {
                    int resultEmptyHeight = ((FrameLayout.LayoutParams) mResultEmpty.getLayoutParams()).height;
                    targetHeight = mAvailableHeight - resultEmptyHeight;
                    targetTranslateY = (mRealScreenHeight - targetHeight) / 2 - resultEmptyHeight - mStatusBarHeight;
                }
            }
        }
        AnimManager.showViewWithTranslateAndHeight(view, 250,
                view.getHeight(), targetHeight,
                targetTranslateY, delayTime);
    }

    public void loadLocalSearchResult(ParcelableObject result) {
        if (getActivity() == null) {
            return;
        }
        mParcelableObject = result;
        boolean isLocalEnabled = SaraUtils.getLocalInputEnabled(getActivity());
        LogUtils.d("localResult isLocalEnabled:" + isLocalEnabled);
        if (isLocalEnabled) {
            List<ApplicationStruct> app = result.getApps();
            List<ContactStruct> contact = result.getContacts();
            List<YellowPageResult> yellow = result.getYellowPages();
            List<MediaStruct> music = result.getMusics();
            if (mLocalSearchLayout != null) {
                mLocalSearchLayout.setData(app, contact, yellow, music);
            }
        }
    }


    public void showSearchViewithAnimation(boolean mExistLocalView, boolean mExistWebView) {
        if (mExistLocalView && mExistWebView) {
            AnimManager.showViewWithAlphaAndTranslate(mLocalSearchLayout, 150, 250, 150);
            AnimManager.showViewWithAlphaAndTranslate(mWebSearchLayout, 200, 250, 150);
        } else if (mExistLocalView) {
            AnimManager.showViewWithAlphaAndTranslate(mLocalSearchLayout, 150, 250, 150);
        } else if (mExistWebView) {
            AnimManager.showViewWithAlphaAndTranslate(mWebSearchLayout, 150, 250, 150);
        }
    }

    public boolean clearSearchViewAnimation(boolean isClearAnimation) {

        if (mLocalSearchLayout != null) {
            if (mLocalSearchLayout.getAnimation() != null && !mLocalSearchLayout.getAnimation().hasEnded()) {
                isClearAnimation = true;
            }
            mLocalSearchLayout.clearAnimation();
        }
        if (mWebSearchLayout != null) {
            if (mWebSearchLayout.getAnimation() != null && !mWebSearchLayout.getAnimation().hasEnded()) {
                isClearAnimation = true;
            }
            mWebSearchLayout.clearAnimation();
        }
        return isClearAnimation;
    }


    public void hideSearchViewWithAnimation() {

        if (mWebSearchLayout.getVisibility() == View.VISIBLE
                && mLocalSearchLayout.getVisibility() == View.VISIBLE) {
            AnimManager.hideViewWithAlphaAndTranslate(mWebSearchLayout, 0, 250, 150);
            AnimManager.hideViewWithAlphaAndTranslate(mLocalSearchLayout, 150, 250, 150);
        } else if (mWebSearchLayout.getVisibility() == View.VISIBLE) {
            AnimManager.hideViewWithAlphaAndTranslate(mWebSearchLayout, 0, 250, 150);
        } else {
            AnimManager.hideViewWithAlphaAndTranslate(mLocalSearchLayout, 0, 250, 150);
        }
    }


    public void loadWebSearchResult(String tmp) {
        if (getActivity() == null) {
            return;
        }
        boolean isWebEnabled = SaraUtils.getWebInputEnabled(getActivity());
        if (mWebSearchLayout != null && isWebEnabled && SaraUtils.isNetworkConnected()) {
            mWebSearchLayout.setSearchText(tmp);
            mWebSearchLayout.loadUrl();
        }
    }

    public void notifyDataSetChanged() {
        if (mLocalSearchLayout != null) {
            mLocalSearchLayout.notifyDataSetChanged();
        }
    }

    public void resetSearchData() {
        if (mLocalSearchLayout != null) {
            mLocalSearchLayout.setData(null, null, null, null);
            mLocalSearchLayout.setVisibility(View.GONE);
        }
        if (mWebSearchLayout != null) {
            mWebSearchLayout.setSearchText(null);
            mWebSearchLayout.setVisibility(View.GONE);
        }
    }

    public boolean checkSearchViewFinish(MotionEvent ev) {
        return SaraUtils.checkFinish(mLocalSearchLayout, ev) && SaraUtils.checkFinish(mWebSearchLayout, ev);
    }

    public void resetSearchView() {
        if (mLocalSearchLayout != null) {
            mLocalSearchLayout.setVisibility(View.GONE);
            mLocalSearchLayout.reset();
        }
        if (mWebSearchLayout != null) {
            mWebSearchLayout.setVisibility(View.GONE);
            mWebSearchLayout.reset();
        }
    }

    public void onRestoreInstanceState(Bundle savedInstanceState) {
        if (mWebSearchLayout != null) {
            mWebSearchLayout.onRestoreInstanceState(savedInstanceState);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mWebSearchLayout != null) {
            mWebSearchLayout.onSaveInstanceState(outState);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mWebSearchLayout != null) {
            mWebSearchLayout.onResume();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mWebSearchLayout != null) {
            mWebSearchLayout.onPause();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mLocalSearchLayout != null) {
            mLocalSearchLayout.destroy();
        }

        if (mWebSearchLayout != null) {
            mWebSearchLayout.onDestroy();
        }
    }

    public void onActivityResult(int resultCode, Intent data) {
        if (mWebSearchLayout != null) {
            mWebSearchLayout.onActivityResult(resultCode, data);
        }
    }
}
