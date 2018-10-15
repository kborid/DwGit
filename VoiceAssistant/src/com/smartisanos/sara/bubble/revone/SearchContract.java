package com.smartisanos.sara.bubble.revone;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Rect;

import com.smartisanos.ideapills.common.mvp.BasePresenter;
import com.smartisanos.ideapills.common.mvp.BaseView;
import com.smartisanos.sara.bubble.revone.manager.StartedAppManager;
import com.smartisanos.sara.bubble.revone.utils.SequenceActivityLauncher;

import java.util.List;

public interface SearchContract {
    interface View extends BaseView<Presenter> {
        void showLocalSearch(Rect rect);

        void startSearchAnim(List<SequenceActivityLauncher.LaunchItem> launchBoundsList);

        void hideSearchAnimItem(Rect launchBounds);

        void clearSearchAnim();

        boolean isFinishing();

        Context getContext();

        void doLocalSearch(String keywords);

        void showTipView(boolean hasSearchEngine, boolean hasResult);

        String getSearchKeywords();

        List<String> getStartedAppList();
    }

    interface Presenter extends BasePresenter {
        void performSearch(int type, String keywords);

        void performReSearch(int type, String keywords);

        void setLocalSearchState(boolean searchDone, boolean hasData, String keywords);

        void clearPendingSearch();

        List<Rect> getSearchRectList();

        void cancelDelayCloseWindow();

        void closePhoneApp(Activity activity, int searchType);

        void delayCloseWindow(int selectTaskId, List<String> packageList);

        void delayCloseWindow(int selectTaskId, List<String> packageList, int delay);

        void closeWindowAndExit(int task, List<String> packageList, boolean exit);

        void closeWindowAndExit(int task, List<String> packageList, boolean exit, Intent intent);

        void closeWindowAndExit(List<String> packageList, boolean exit, StartedAppManager.IAppCloseListener listener);
    }
}