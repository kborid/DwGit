package com.smartisanos.sara.bubble.view;

import android.animation.Animator;
import android.service.onestep.GlobalBubble;
import android.service.onestep.GlobalBubbleAttach;

import java.util.List;

/**
 * the interface of fake bubble view
 * use interface to limit the usage
 */
public interface IFakeBubbleView {
    void initFakeAnim(GlobalBubble globalBubble, List<GlobalBubbleAttach> attachmentList, int displayWidth);

    void setFakeAnimOffset(int left, int top, int w, int h);

    int getFakeBubbleBgWidth();

    int getFakeBubbleBgHeight();

    int getFakeAnimTargetX();

    int getFakeAnimTargetY(int endBoxHeight);

    void startFakeAnim(int translationY, Animator.AnimatorListener animatorListener);

    void startFakeBubbleSendFlyAnim(int translationY, int[] bubbleLoc, int[] selectLoc);
}
