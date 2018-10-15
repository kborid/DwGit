package com.smartisanos.sara.bubble.revone.utils;


import android.content.Context;
import android.graphics.Point;
import android.graphics.Rect;

import com.smartisanos.sara.bubble.revone.ExtScreenConstant;
import com.smartisanos.sara.util.LogUtils;
import com.smartisanos.sara.util.ViewUtils;

import java.util.ArrayList;
import java.util.List;

public class SearchDisplayPosHelper implements ISearchDisplayPosHelper {

    public static final String TAG = "VoiceAss.SearchDisplayPosHelper";

    private int mColumnInScreen;
    private int mRowInScreen;

    private int mActivityDisplayGapX;
    private int mActivityDisplayGapY;
    private int mActivityDisplayPaddingX;
    private int mActivityDisplayPaddingTop;
    private int mActivityDisplayPaddingBottom;
    private int mActivityDisplayWidth;
    private int mActivityDisplayHeight;

    // true: 从上往下优先，再从左到右排列, false: 从左到右优先，再从上往下排列
    private boolean mIsRowOrderFirst = false;
    private Rect mStandardRect = new Rect();

    private int mWidthOffset = 0;

    public SearchDisplayPosHelper(Context context, Point screenSize, int columnInScreen, int rowInScreen) {
        mColumnInScreen = columnInScreen;
        mRowInScreen = rowInScreen;
        int screenWidth = screenSize.x;
        int screenHeight = screenSize.y;
        LogUtils.i(TAG, "mScreenWidth:" + screenWidth + ",mScreenHeight:" + screenHeight);
        mActivityDisplayGapX = ViewUtils.dp2px(context, SearchParams.ACTIVITY_DISPLAY_GAP_X);
        mActivityDisplayGapY = ViewUtils.dp2px(context, SearchParams.ACTIVITY_DISPLAY_GAP_Y);
        mActivityDisplayPaddingX = ViewUtils.dp2px(context, SearchParams.ACTIVITY_DISPLAY_PADDING_X);
        mActivityDisplayPaddingTop = getDefaultPaddingTop(context);
        mActivityDisplayPaddingBottom = ViewUtils.dp2px(context, SearchParams.ACTIVITY_DISPLAY_PADDING_BOTTOM);
        if (ExtScreenConstant.CONTAINS_STATUS_NAVI_BAR) {
            mActivityDisplayPaddingTop += ExtScreenConstant.STATUS_BAR_HEIGHT;
            mActivityDisplayPaddingBottom += ExtScreenConstant.NAVIGATION_BAR_HEIGHT;
        } else {
            screenHeight -= ExtScreenConstant.STATUS_BAR_HEIGHT - ExtScreenConstant.NAVIGATION_BAR_HEIGHT;
        }
        int gapXCount = (mColumnInScreen - 1);
        int gapYCount = (mRowInScreen - 1);
        int paddingCount = 2;
        mActivityDisplayWidth = (screenWidth - mActivityDisplayPaddingX * paddingCount - mActivityDisplayGapX * gapXCount) / mColumnInScreen;
        mActivityDisplayHeight = (screenHeight - mActivityDisplayPaddingTop - mActivityDisplayPaddingBottom
                - mActivityDisplayGapY * gapYCount) / mRowInScreen;
        correctionByScale(gapXCount, gapYCount, paddingCount);
        getDisplayRect(0, 0, mStandardRect);
    }

    public void alignTo(SearchDisplayPosHelper otherSearchDisplayPosHelper) {
        int diffY = 0;
        if (mActivityDisplayPaddingTop > otherSearchDisplayPosHelper.mActivityDisplayPaddingTop) {
            diffY += mActivityDisplayPaddingTop - otherSearchDisplayPosHelper.mActivityDisplayPaddingTop;
            mActivityDisplayPaddingTop = otherSearchDisplayPosHelper.mActivityDisplayPaddingTop;
        }
        if (mActivityDisplayPaddingBottom > otherSearchDisplayPosHelper.mActivityDisplayPaddingBottom) {
            diffY += mActivityDisplayPaddingBottom - otherSearchDisplayPosHelper.mActivityDisplayPaddingBottom;
            mActivityDisplayPaddingBottom = otherSearchDisplayPosHelper.mActivityDisplayPaddingBottom;
        }
        int gapYCount = (mRowInScreen - 1);
        if (gapYCount > 0) {
            mActivityDisplayGapY += 1f * diffY / gapYCount;
        }

        int diffX = 0;
        if (mActivityDisplayPaddingX > otherSearchDisplayPosHelper.mActivityDisplayPaddingX) {
            diffX += 2 * (mActivityDisplayPaddingX - otherSearchDisplayPosHelper.mActivityDisplayPaddingX);
            mActivityDisplayPaddingX = otherSearchDisplayPosHelper.mActivityDisplayPaddingX;
        }
        int gapXCount = (mColumnInScreen - 1);
        if (gapXCount > 0) {
            mActivityDisplayGapX += 1f * diffX / gapXCount;
        }
    }

    private int getDefaultPaddingTop(Context context) {
        return ViewUtils.dp2px(context, SearchParams.ACTIVITY_DISPLAY_PADDING_TOP);
    }

    protected void correctionByScale(int gapXCount, int gapYCount, int paddingCount) {
        float scaleNow = 1f * mActivityDisplayHeight / mActivityDisplayWidth;
        if (scaleNow < SearchParams.DISPLAY_SCALE) {
            int newWidth = Float.valueOf(mActivityDisplayHeight / SearchParams.DISPLAY_SCALE).intValue();
            int remainWidthDivided;
            remainWidthDivided = (mActivityDisplayWidth - newWidth) * mColumnInScreen / (gapXCount + paddingCount);
            mActivityDisplayGapX += remainWidthDivided;
            mActivityDisplayPaddingX += remainWidthDivided;
            mActivityDisplayWidth = newWidth;
        } else {
            int newHeight = Float.valueOf(mActivityDisplayWidth *SearchParams. DISPLAY_SCALE).intValue();
            int remainHeightDivided;
            remainHeightDivided = (mActivityDisplayHeight - newHeight) * mRowInScreen / (gapYCount + paddingCount);
            mActivityDisplayGapY += remainHeightDivided;
            mActivityDisplayPaddingTop += remainHeightDivided;
            mActivityDisplayPaddingBottom += remainHeightDivided;
            mActivityDisplayHeight = newHeight;
        }
    }

    @Override
    public void getDisplayRect(int position, Rect outRect) {
        if (mIsRowOrderFirst) {
            getDisplayRect((position) / mRowInScreen, (position) % mRowInScreen, outRect);
        } else {
            getDisplayRect((position) % mColumnInScreen, (position) / mColumnInScreen, outRect);
        }
        outRect.offset(mWidthOffset,0);
    }

    protected void getDisplayRect(int column, int row, Rect outRect) {
        int x = mActivityDisplayPaddingX + (mActivityDisplayGapX + mActivityDisplayWidth) * column;
        int y = mActivityDisplayPaddingTop + row * (mActivityDisplayGapY + mActivityDisplayHeight);
        outRect.set(x, y, x + mActivityDisplayWidth, y + mActivityDisplayHeight);
    }

    @Override
    public List<Rect> getStandardRectList() {
        ArrayList<Rect> rectArrayList = new ArrayList<>();
        rectArrayList.add(new Rect(mStandardRect));
        return rectArrayList;
    }

    public void setFixCenterAlignMode(int fixDisplayCount) {
        int usedColumn = getUsedColumn(fixDisplayCount);
        if (usedColumn >= mColumnInScreen) {
            mWidthOffset = 0;
        } else {
            mWidthOffset = (int) ((mColumnInScreen - usedColumn) / 2f * getRectWidthWithGap());
        }
    }

    protected int getColumnInScreen() {
        return mColumnInScreen;
    }

    protected int getRowInScreen() {
        return mRowInScreen;
    }

    int getRectWidthWithGap() {
        return (mActivityDisplayGapX + mActivityDisplayWidth);
    }

    boolean isRowOrderFirst() {
        return mIsRowOrderFirst;
    }

    int getUsedColumn(int count) {
        if (mIsRowOrderFirst) {
            if (count > getColumnInScreen() * getRowInScreen()) {
                return getColumnInScreen();
            } else {
                return count / getRowInScreen() + (count % getRowInScreen() == 0 ? 0 : 1);
            }
        } else {
            if (count / getColumnInScreen() > 0) {
                return getColumnInScreen();
            } else {
                return count % getColumnInScreen();
            }
        }
    }
}
