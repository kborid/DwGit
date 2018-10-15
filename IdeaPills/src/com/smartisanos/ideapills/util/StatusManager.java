package com.smartisanos.ideapills.util;

import android.util.Pair;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StatusManager {
    private static final LOG log = LOG.getInstance(StatusManager.class);

    public static final int GLOBAL_DRAGGING           = 0x1 << 0;
    public static final int BUBBLE_ANIM               = 0x1 << 1;
    public static final int BUBBLE_DRAGGING           = 0x1 << 2;
    public static final int BUBBLE_DELETING           = 0x1 << 3;
    public static final int SHIELD_SHOW_LIST          = 0x1 << 4;
    public static final int FEATURE_PHONE_MODE        = 0x1 << 5;
    public static final int ADDING_ATTACHMENT         = 0x1 << 6;
    public static final int HAS_FILTER_STRING         = 0x1 << 7;
    public static final int FORCE_HIDE_WINDOW         = 0x1 << 8;
    public static final int BUBBLE_REFRESHING         = 0x1 << 9;

    private static final int STATUS_NUM = 10;

    private static final int HIDE_ALL_BUBBLE_FLAG = 0;

    public static final Map<Integer, String> statusNameMap = new HashMap<Integer, String>();
    static {
        statusNameMap.put(GLOBAL_DRAGGING,         "GLOBAL_DRAGGING");
        statusNameMap.put(BUBBLE_ANIM,             "BUBBLE_ANIM");
        statusNameMap.put(BUBBLE_DRAGGING,         "BUBBLE_DRAGGING");
        statusNameMap.put(BUBBLE_DELETING,         "BUBBLE_DELETING");
        statusNameMap.put(SHIELD_SHOW_LIST,        "SHIELD_SHOW_LIST");
        statusNameMap.put(FEATURE_PHONE_MODE,      "FEATURE_PHONE_MODE");
        statusNameMap.put(ADDING_ATTACHMENT,       "ADDING_ATTACHMENT");
        statusNameMap.put(HAS_FILTER_STRING,       "HAS_FILTER_STRING");
        statusNameMap.put(FORCE_HIDE_WINDOW,       "FORCE_HIDE_WINDOW");
        statusNameMap.put(BUBBLE_REFRESHING,       "BUBBLE_REFRESHING");
        if (statusNameMap.size() != STATUS_NUM) {
            throw new IllegalArgumentException("statusNameMap.size() != STATUS_NUM");
        }
    }

    public static void dumpStatus() {
        List<Integer> keys = new ArrayList<Integer>(statusNameMap.keySet());
        int size = keys.size();
        for (int i = 0; i < size; i++) {
            int status = keys.get(i);
            if (getStatus(status)) {
                String statusName = statusNameMap.get(status);
                log.error("status error, "+statusName+" is true");
            }
        }
    }

    public interface StatusChangedListener {
        void onChanged();
    }

    private static volatile int mStatus = 0;
    private static List<Pair<StatusChangedListener, Integer>> mFlagListeners = new ArrayList<Pair<StatusChangedListener, Integer>>();

    public static void init() {
        mStatus = 0;
        mFlagListeners.clear();
    }

    public static void addAnimFlagStatusChangedListener(int flag, StatusChangedListener listener) {
        if(flag == 0 || listener == null) {
            return ;
        }
        mFlagListeners.add(new Pair<StatusChangedListener, Integer>(listener, flag));
    }

    public static void removeAnimFlagStatusChangedListener(int flag, StatusChangedListener listener) {
        if (listener == null) {
            return;
        }
        for (int i = 0; i < mFlagListeners.size(); ++i) {
            Pair<StatusChangedListener, Integer> pair = mFlagListeners.get(i);
            if (pair == null) {
                continue;
            }
            if (pair.first == listener && pair.second == flag) {
                mFlagListeners.remove(pair);
            }
        }
    }

    public static void setStatus(int status, boolean value) {
        if (getStatus(status) == value) {
            return;
        }
        int oldValue = mStatus;
        if (value) {
            mStatus |= status;
        } else {
            mStatus &= ~status;
        }

        for (int i = 0; i < mFlagListeners.size(); ++i) {
            int careFlag = mFlagListeners.get(i).second;
            if ((oldValue & careFlag) != (mStatus & careFlag)) {
                mFlagListeners.get(i).first.onChanged();
            }
        }
    }

    public int getStatusValue() {
        return mStatus;
    }

    public static boolean getStatus(int status) {
        return (mStatus & status) == status;
    }

    private static final int HIDE_BUBBLE = FEATURE_PHONE_MODE | HIDE_ALL_BUBBLE_FLAG;

    public static boolean canShowAllBubbles() {
        return (mStatus & HIDE_BUBBLE) == 0;
    }

    public static boolean isBubbleDragging() {
        return getStatus(StatusManager.BUBBLE_DRAGGING);
    }

    public static boolean isBubbleRefreshing() {
        return getStatus(BUBBLE_REFRESHING);
    }
}