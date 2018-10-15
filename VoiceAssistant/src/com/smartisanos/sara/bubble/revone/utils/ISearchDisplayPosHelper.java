package com.smartisanos.sara.bubble.revone.utils;

import android.graphics.Rect;

import java.util.List;

public interface ISearchDisplayPosHelper {

    void getDisplayRect(int position, Rect outRect);

    List<Rect> getStandardRectList();
}
