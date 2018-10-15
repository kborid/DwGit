package com.smartisanos.sara.widget;

import android.animation.Animator;

public interface IBubbleHeightChangeListener {
    Animator getWaveHeightAnimator(int targetHeight);

    Animator getWaveTransAnimator(int deltaTransY);

    boolean needPerformAnimator();

    void updateWaveHeight(int targetHeight);
}