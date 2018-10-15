package com.smartisanos.sara.bubble.revone.utils;

import android.animation.TimeInterpolator;

public class Cubic {
    private static final float DOMAIN = 1.0f;
    private static final float DURATION = 1.0f;
    private static final float START = 0.0f;
    public static final TimeInterpolator easeIn = new TimeInterpolator() {
        public float getInterpolation(float input) {
            return DOMAIN * (input /= DURATION) * input * input + START;
        }
    };
    public static final TimeInterpolator easeOut = new TimeInterpolator() {
        public float getInterpolation(float input) {
            return DOMAIN
                    * ((input = input / DURATION - 1) * input * input + 1)
                    + START;
        }
    };
    public static final TimeInterpolator easeInOut = new TimeInterpolator() {
        public float getInterpolation(float input) {
            return ((input /= DURATION / 2) < 1.0f) ? (DOMAIN / 2 * input
                    * input * input + START) : (DOMAIN / 2
                    * ((input -= 2) * input * input + 2) + START);
        }
    };
}