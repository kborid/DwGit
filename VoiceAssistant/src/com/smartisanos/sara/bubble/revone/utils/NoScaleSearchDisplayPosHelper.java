package com.smartisanos.sara.bubble.revone.utils;

import android.content.Context;
import android.graphics.Point;

public class NoScaleSearchDisplayPosHelper extends SearchDisplayPosHelper {
    public NoScaleSearchDisplayPosHelper(Context context, Point screenSize, int columnInScreen, int rowInScreen) {
        super(context, screenSize, columnInScreen, rowInScreen);
    }

    @Override
    protected void correctionByScale(int gapXCount, int gapYCount, int paddingCount) {

    }
}
