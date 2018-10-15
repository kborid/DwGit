package com.smartisanos.ideapills.view;

import android.graphics.Canvas;

import com.smartisanos.ideapills.common.anim.AnimListener;

public interface AnimRunnerInterface {
    void draw(Canvas canvas);
    boolean isRunning();
    void start();
    void addAnimListener(AnimListener listener);
    void cancel();
    void recycle();
}
