package com.smartisanos.sara.bubble.view;

import android.view.MotionEvent;

/**
 * the interface of popup wave layout
 * use interface to limit the usage
 */
public interface IWaveLayout {
    void init(boolean isLeftMode);

    void changeUiMode(boolean isLeftMode);

    void show(int bubbleColor);

    void setContentWidth(int width);

    void waveChanged(byte[] waveFormData, int pointNum);

    void waveMax(int bubbleColor);

    void stopWaveAnim(boolean isCancelWithoutCallback);

    boolean clearAnim();

    void hide();

    void hideTextPopup(boolean withAnimation);

    boolean checkFinish(MotionEvent ev);

    int getWaveLayoutHeight();
}
