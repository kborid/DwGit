package com.smartisanos.sara.bubble.revone.utils;

import android.content.Context;
import android.graphics.Point;
import android.graphics.Rect;

import com.smartisanos.sara.bubble.revone.ExtScreenConstant;

import java.util.ArrayList;
import java.util.List;

public class MixSearchDisplayPosHelper implements ISearchDisplayPosHelper {

    private SearchDisplayPosHelper mFirstTypeSearchDisplayPosHelper;
    private SearchDisplayPosHelper mSecondTypeSearchDisplayPosHelper;

    private int mColumnInScreen;
    private int mFirstTypeCountLimit;
    private int mFirstTypeCountLimitInSecond;

    private int mWidthOffset = 0;

    public MixSearchDisplayPosHelper(Context context, Point screenSize, int columnInScreen
            , int firstTypeRowInScreen, int secondTypeRowInScreen) {
        mColumnInScreen = columnInScreen;
        mFirstTypeSearchDisplayPosHelper = new NoScaleSearchDisplayPosHelper(context, screenSize, columnInScreen,
                firstTypeRowInScreen);
        mSecondTypeSearchDisplayPosHelper = new SearchDisplayPosHelper(context, screenSize, columnInScreen,
                secondTypeRowInScreen);
        mSecondTypeSearchDisplayPosHelper.alignTo(mFirstTypeSearchDisplayPosHelper);
        mFirstTypeSearchDisplayPosHelper.alignTo(mSecondTypeSearchDisplayPosHelper);
    }

    public void setFirstTypeCountLimit(int firstTypeCountLimit) {
        int firstTypeColumnLimit = mFirstTypeSearchDisplayPosHelper.getUsedColumn(firstTypeCountLimit);
        mFirstTypeCountLimit = firstTypeColumnLimit * mFirstTypeSearchDisplayPosHelper.getRowInScreen();
        mFirstTypeCountLimitInSecond = firstTypeColumnLimit * mSecondTypeSearchDisplayPosHelper.getRowInScreen();
    }

    @Override
    public void getDisplayRect(int position, Rect outRect) {
        if (position < mFirstTypeCountLimit) {
            mFirstTypeSearchDisplayPosHelper.getDisplayRect(position, outRect);
            outRect.offset(mWidthOffset, 0);
        } else {
            if (mSecondTypeSearchDisplayPosHelper.isRowOrderFirst()) {
                int positionInSecond = (position - mFirstTypeCountLimit) + mFirstTypeCountLimitInSecond;
                mSecondTypeSearchDisplayPosHelper.getDisplayRect(positionInSecond, outRect);
                outRect.offset(mWidthOffset, 0);
            } else {
                int usedColumn = mFirstTypeCountLimit / mFirstTypeSearchDisplayPosHelper.getRowInScreen();
                int leftColumn = mFirstTypeSearchDisplayPosHelper.getColumnInScreen() - usedColumn;
                int secondPos = position - mFirstTypeCountLimit;
                if (leftColumn > 0) {
                    int newColumn = secondPos % leftColumn;
                    int newRow = secondPos / leftColumn;
                    int positionInSecond = usedColumn + newColumn + mSecondTypeSearchDisplayPosHelper.getColumnInScreen() * newRow;
                    mSecondTypeSearchDisplayPosHelper.getDisplayRect(positionInSecond, outRect);
                    outRect.offset(mWidthOffset, 0);
                }
            }
        }
    }

    @Override
    public List<Rect> getStandardRectList() {
        ArrayList<Rect> rectArrayList = new ArrayList<>();
        rectArrayList.addAll(mFirstTypeSearchDisplayPosHelper.getStandardRectList());
        rectArrayList.addAll(mSecondTypeSearchDisplayPosHelper.getStandardRectList());
        return rectArrayList;
    }

    public void setFixCenterAlignMode(int firstTypeCountLimit, int secondTypeCountLimit) {
        int firstTypeColumnLimit = mFirstTypeSearchDisplayPosHelper.getUsedColumn(firstTypeCountLimit);
        int secondTypeColumnLimit = mSecondTypeSearchDisplayPosHelper.getUsedColumn(secondTypeCountLimit);
        if (firstTypeColumnLimit + secondTypeColumnLimit >= mColumnInScreen) {
            mWidthOffset = 0;
        } else {
            mWidthOffset = (int)((mColumnInScreen - firstTypeColumnLimit - secondTypeColumnLimit) / 2f
                    * mFirstTypeSearchDisplayPosHelper.getRectWidthWithGap());
        }
    }

}
