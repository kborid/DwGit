
package com.smartisanos.sara.util;

import android.view.animation.Interpolator;

public class CubicInterpolator implements Interpolator {

    public static CubicInterpolator IN = new CubicInterpolator() {
        @Override
        public final float getInterpolation(float t) {
            return t * t * t;
        }

        @Override
        public String toString() {
            return "CubicInterpolator.IN";
        }
    };

    public static final CubicInterpolator OUT = new CubicInterpolator() {
        @Override
        public final float getInterpolation(float t) {
            return (t -= 1) * t * t + 1;
        }

        @Override
        public String toString() {
            return "CubicInterpolator.OUT";
        }
    };

    public static final CubicInterpolator INOUT = new CubicInterpolator() {
        @Override
        public final float getInterpolation(float t) {
            if ((t *= 2) < 1)
                return 0.5f * t * t * t;
            return 0.5f * ((t -= 2) * t * t + 2);
        }

        @Override
        public String toString() {
            return "CubicInterpolator.INOUT";
        }
    };

    @Override
    public float getInterpolation(float input) {
        return OUT.getInterpolation(input);
    }

}
