package com.smartisanos.ideapills.common.remind;

import java.util.List;

import android.graphics.Rect;
import android.os.Bundle;
import android.support.v4.view.ViewCompat;
import android.support.v4.view.accessibility.AccessibilityNodeInfoCompat;
import android.support.v4.widget.ExploreByTouchHelper;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

public class WeekViewAccessibilityHelper extends ExploreByTouchHelper {

    MonthWeekEventsView mView;
    public WeekViewAccessibilityHelper(MonthWeekEventsView forView) {
        super(forView);
        mView = forView;
    }

    public void setAccessibilityDelegateForView() {
        ViewCompat.setAccessibilityDelegate(mView, this);
    }

    @Override
    protected int getVirtualViewAt(float x, float y) {
        int pos = mView.getCellPosFromLocation(x);
        if (pos < 0 || pos > 7) {
            return -1;
        }
        return mView.mFirstJulianDay + pos;
    }

    @Override
    protected void getVisibleVirtualViews(List<Integer> virtualViewIds) {
        for (int i = 0; i < mView.mNumDays; i++) {
            virtualViewIds.add(mView.mFirstJulianDay + i);
        }
    }

    @Override
    protected void onPopulateEventForVirtualView(int virtualViewId, AccessibilityEvent event) {
        int index = virtualViewId - mView.mFirstJulianDay;
        event.setAction(AccessibilityNodeInfo.ACTION_CLICK);
        event.setContentDescription(mView.getContentDescriptionForIndex(index));
    }

    @Override
    protected void onPopulateNodeForVirtualView(int virtualViewId, AccessibilityNodeInfoCompat node) {
        int index = virtualViewId - mView.mFirstJulianDay;
        node.setContentDescription(mView.getContentDescriptionForIndex(index));
        if (mView.isSelectedForIndex(index)) {
            node.setCheckable(true);
            node.setChecked(true);
        }
        node.setClickable(true);
        node.addAction(AccessibilityNodeInfoCompat.ACTION_CLICK);
        Rect bounds = mView.getBoundsForIndex(index);
        node.setBoundsInParent(bounds);
    }

    @Override
    protected boolean onPerformActionForVirtualView(int virtualViewId, int action, Bundle arguments) {
        int index = virtualViewId - mView.mFirstJulianDay;
        switch (action) {
            case AccessibilityNodeInfo.ACTION_CLICK:
                return true;
            }
        return false;
    }

}
