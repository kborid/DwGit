package com.smartisanos.sara;


import com.smartisanos.sara.util.SaraConstant;

import java.util.ArrayList;
import java.util.List;

/**
 * use this instead of register receiver on activity create
 * because broadcast may send during activity create
 */
public enum BubbleActionUpHelper {
    INSTANCE;

    private List<MenuActionUpListener> mMenuActionUpListeners = new ArrayList<>();
    private long mLastActionUpTime;

    void onActionUp() {
        if (!mMenuActionUpListeners.isEmpty()) {
            for (MenuActionUpListener menuActionUpListener : mMenuActionUpListeners) {
                menuActionUpListener.onActionUp();
            }
            mLastActionUpTime = 0;
        } else {
            mLastActionUpTime = System.nanoTime() / 1000000;
        }
    }

    public void addActionUpListener(MenuActionUpListener menuActionUpListener) {
        boolean noListenersBefore = mMenuActionUpListeners.isEmpty();
        mMenuActionUpListeners.add(menuActionUpListener);
        if (noListenersBefore) {
            long now = System.nanoTime() / 1000000;
            if (now - mLastActionUpTime <= SaraConstant.TIME_LONG_PRESS_KEYCODE_SMART) {
                for (MenuActionUpListener nowListener : mMenuActionUpListeners) {
                    nowListener.onActionUp();
                }
            }
        }
        mLastActionUpTime = 0;
    }

    public void removeActionUpListener(MenuActionUpListener menuActionUpListener) {
        mMenuActionUpListeners.remove(menuActionUpListener);
        if (mMenuActionUpListeners.isEmpty()) {
            mLastActionUpTime = 0;
        }
    }

    public interface MenuActionUpListener {
        void onActionUp();
    }
}
