package com.smartisanos.ideapills.common.remind.util;

import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;
import android.view.animation.LinearInterpolator;

public class SequenceAnimUtils {
    private static final AccelerateInterpolator accelerateInterpolator
            = new AccelerateInterpolator();
    private static final DecelerateInterpolator decelerateInterpolator
            = new DecelerateInterpolator();
    private static final LinearInterpolator linearInterpolator
            = new LinearInterpolator();

    /**
     * @param needToRunCount Range: [1, 2]
     * @param currentCount Range: [0, needToRunCount]
     * @return
     */
    public static Interpolator getInterpolator(int needToRunCount, int currentCount) {
        if (needToRunCount == 1) {
            if (currentCount == 1) {
                return accelerateInterpolator;
            } else { // == 0
                return decelerateInterpolator;
            }
        } else if (needToRunCount == 2) {
            if (currentCount == 2) {
                return accelerateInterpolator;
            } else if (currentCount == 1) {
                return linearInterpolator;
            } else { // == 0
                return decelerateInterpolator;
            }
        }
        return null;

    }

    /**
     * @param needToRunCount Range: [1, 2]
     * @param currentCount Range: [0, needToRunCount]
     * @return
     */
    public static int getDuration(int needToRunCount, int currentCount) {
        if (needToRunCount == 1) {
            return 200;
        } else if (needToRunCount == 2) {
            if (currentCount == 2) {
                return 150;
            } else if (currentCount == 1) {
                return 100;
            } else {
                return 150;
            }
        }
        return 300;
    }

}
