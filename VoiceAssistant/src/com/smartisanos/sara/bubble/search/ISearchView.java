package com.smartisanos.sara.bubble.search;

import android.content.Intent;
import android.service.onestep.GlobalBubble;
import android.view.MotionEvent;

import com.smartisanos.sara.util.SaraUtils;

import smartisanos.app.voiceassistant.ParcelableObject;

public interface ISearchView extends SaraUtils.ShowViewChangeListener, SaraUtils.BulletViewChangeListener {

    // all at all
    boolean ensureInitialized();

    void refreshView(boolean mExistLocalView, boolean mExistWebView, int resultHeight);

    //data
    void resetSearchViewAndData(boolean resetData, boolean resetView);

    void loadSearchResult(ParcelableObject result, String tmp);

    void onActivityResult(int resultCode, Intent data);

    //view
    void showSearchViewithAnimation(boolean mExistLocalView, boolean mExistWebView);

    boolean clearSearchViewAnimation(boolean isClearAnimation);

    void hideSearchViewWithAnimation();

    boolean isExistWebView(boolean hasWeb, boolean mExistWebView);

    boolean isExistLocalView(boolean hasLocal, boolean mExistLocalView);

    boolean checkSearchViewFinish(MotionEvent ev);

    void notifyDataSetChanged();

    boolean hideViewFromAction(int bubbleItemTranslateY, int bubbleItemHeight, boolean bubbleItemVisiable, int from, boolean finish, Runnable runnable);

    void hideResultView();
}
