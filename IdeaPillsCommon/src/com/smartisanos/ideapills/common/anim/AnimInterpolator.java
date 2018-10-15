package com.smartisanos.ideapills.common.anim;

import android.view.animation.AccelerateInterpolator;

public class AnimInterpolator {
    public final static int QUAD_IN = 1;
    public final static int QUAD_OUT = 2;
    public final static int QUAD_IN_OUT = 3;

    public final static int CIRC_IN = 4;
    public final static int CIRC_OUT = 5;
    public final static int CIRC_IN_OUT = 6;

    public final static int CUBIC_IN = 7;
    public final static int CUBIC_OUT = 8;
    public final static int CUBIC_IN_OUT = 9;

    public final static int QUART_IN = 10;
    public final static int QUART_OUT = 11;
    public final static int QUART_IN_OUT = 12;

    public final static int QUINT_IN = 13;
    public final static int QUINT_OUT = 14;
    public final static int QUINT_IN_OUT = 15;

    public final static int SINE_IN = 16;
    public final static int SINE_OUT = 17;
    public final static int SINE_IN_OUT = 18;

    public final static int BACK_IN = 19;
    public final static int BACK_OUT = 20;
    public final static int BACK_IN_OUT = 21;

    public final static int EASE_OUT = 22;

    public final static int DEFAULT = 100;

    private interface ComputeInterpolator {
        public float compute(float t);
    }

    public static class Interpolator extends AccelerateInterpolator {

        private int mType = 0;
        private ComputeInterpolator compute;

        public Interpolator(int type) {
            mType = type;
            generateCompute(mType, -1);
        }

        public Interpolator(int type, float param) {
            mType = type;
            generateCompute(mType, param);
        }

        private void generateCompute(int type, float param) {
            switch (type) {
                case QUAD_IN : {
                    compute = new QUAD_IN();
                    break;
                }
                case QUAD_OUT : {
                    compute = new QUAD_OUT();
                    break;
                }
                case QUAD_IN_OUT : {
                    compute = new QUAD_IN_OUT();
                    break;
                }
                case CIRC_IN : {
                    compute = new CIRC_IN();
                    break;
                }
                case CIRC_OUT : {
                    compute = new CIRC_OUT();
                    break;
                }
                case CIRC_IN_OUT : {
                    compute = new CIRC_IN_OUT();
                    break;
                }
                case CUBIC_IN : {
                    compute = new CUBIC_IN();
                    break;
                }
                case CUBIC_OUT : {
                    compute = new CUBIC_OUT();
                    break;
                }
                case CUBIC_IN_OUT : {
                    compute = new CUBIC_IN_OUT();
                    break;
                }
                case QUART_IN : {
                    compute = new QUART_IN();
                    break;
                }
                case QUART_OUT : {
                    compute = new QUART_OUT();
                    break;
                }
                case QUART_IN_OUT : {
                    compute = new QUART_IN_OUT();
                    break;
                }
                case QUINT_IN : {
                    compute = new QUINT_IN();
                    break;
                }
                case QUINT_OUT : {
                    compute = new QUINT_OUT();
                    break;
                }
                case QUINT_IN_OUT : {
                    compute = new QUINT_IN_OUT();
                    break;
                }
                case SINE_IN : {
                    compute = new SINE_IN();
                    break;
                }
                case SINE_OUT : {
                    compute = new SINE_OUT();
                    break;
                }
                case SINE_IN_OUT : {
                    compute = new SINE_IN_OUT();
                    break;
                }
                case BACK_IN : {
                    float defParam = 1.70158f;
                    if (param <= 0) {
                        param = defParam;
                    }
                    compute = new BACK_IN(param);
                    break;
                }
                case BACK_OUT : {
                    float defParam = 1.70158f;
                    if (param <= 0) {
                        param = defParam;
                    }
                    compute = new BACK_OUT(param);
                    break;
                }
                case BACK_IN_OUT : {
                    float defParam = 1.70158f;
                    if (param <= 0) {
                        param = defParam;
                    }
                    compute = new BACK_IN_OUT(param);
                    break;
                }
                case EASE_OUT : {
                    compute = new EASE_OUT();
                    break;
                }
                case DEFAULT : {
                    compute = new DEFAULT();
                    break;
                }
            }
        }

        @Override
        public float getInterpolation(float input) {
            if (compute != null) {
                return compute.compute(input);
            }
            return super.getInterpolation(input);
        }
    }

    private static class QUAD_IN implements ComputeInterpolator {
        public float compute(float t) {
            return t*t;
        }
    }

    private static class QUAD_OUT implements ComputeInterpolator {
        public float compute(float t) {
            return -t*(t-2);
        }
    }

    private static class QUAD_IN_OUT implements ComputeInterpolator {
        public float compute(float t) {
            if ((t*=2) < 1) return 0.5f*t*t;
            return -0.5f * ((--t)*(t-2) - 1);
        }
    }

    private static class CIRC_IN implements ComputeInterpolator {
        public float compute(float t) {
            return (float) -Math.sqrt(1 - t*t) - 1;
        }
    }

    private static class CIRC_OUT implements ComputeInterpolator {
        public float compute(float t) {
            return (float) Math.sqrt(1 - (t-=1)*t);
        }
    }

    private static class CIRC_IN_OUT implements ComputeInterpolator {
        public float compute(float t) {
            if ((t*=2) < 1) return -0.5f * ((float)Math.sqrt(1 - t*t) - 1);
            return 0.5f * ((float)Math.sqrt(1 - (t-=2)*t) + 1);
        }
    }

    private static class CUBIC_IN implements ComputeInterpolator {
        public float compute(float t) {
            return t*t*t;
        }
    }

    private static class CUBIC_OUT implements ComputeInterpolator {
        public float compute(float t) {
            return (t-=1)*t*t + 1;
        }
    }

    private static class CUBIC_IN_OUT implements ComputeInterpolator {
        public float compute(float t) {
            if ((t*=2) < 1) return 0.5f*t*t*t;
            return 0.5f * ((t-=2)*t*t + 2);
        }
    }

    private static class QUART_IN implements ComputeInterpolator {
        public float compute(float t) {
            return t*t*t*t;
        }
    }

    private static class QUART_OUT implements ComputeInterpolator {
        public float compute(float t) {
            return -((t-=1)*t*t*t - 1);
        }
    }

    private static class QUART_IN_OUT implements ComputeInterpolator {
        public final float compute(float t) {
            if ((t*=2) < 1) return 0.5f*t*t*t*t;
            return -0.5f * ((t-=2)*t*t*t - 2);
        }
    }

    private static class QUINT_IN implements ComputeInterpolator {
        public float compute(float t) {
            return t*t*t*t*t;
        }
    }

    private static class QUINT_OUT implements ComputeInterpolator {
        public float compute(float t) {
            return (t-=1)*t*t*t*t + 1;
        }
    }

    private static class QUINT_IN_OUT implements ComputeInterpolator {
        public float compute(float t) {
            if ((t*=2) < 1) return 0.5f*t*t*t*t*t;
            return 0.5f*((t-=2)*t*t*t*t + 2);
        }
    }

    private static class SINE_IN implements ComputeInterpolator {
        public float compute(float t) {
            return (float) -Math.cos(t * (Math.PI/2)) + 1;
        }
    }

    private static class SINE_OUT implements ComputeInterpolator {
        public float compute(float t) {
            return (float) Math.sin(t * (Math.PI/2));
        }
    }

    private static class SINE_IN_OUT implements ComputeInterpolator {
        public float compute(float t) {
            return -0.5f * ((float) Math.cos(Math.PI*t) - 1);
        }
    }

    private static class BACK_IN implements ComputeInterpolator {
        private float mParam = 1;

        public BACK_IN(float param) {
            mParam = param;
        }

        public float compute(float t) {
            float s = mParam;
            return t*t*((s+1)*t - s);
        }
    }

    private static class BACK_OUT implements ComputeInterpolator {
        private float mParam = 1;

        public BACK_OUT(float param) {
            mParam = param;
        }

        public float compute(float t) {
            float s = mParam;
            return (t-=1)*t*((s+1)*t + s) + 1;
        }
    }

    private static class BACK_IN_OUT implements ComputeInterpolator {
        private float mParam = 1;

        public BACK_IN_OUT(float param) {
            mParam = param;
        }

        public final float compute(float t) {
            float s = mParam;
            if ((t*=2) < 1) return 0.5f*(t*t*(((s*=(1.525f))+1)*t - s));
            return 0.5f*((t-=2)*t*(((s*=(1.525f))+1)*t + s) + 2);
        }
    }

    private static class EASE_OUT implements ComputeInterpolator {
        public float compute(float t) {
            return (t -= 1) * t * t + 1;
        }
    }

    private static class DEFAULT implements ComputeInterpolator{
        public float compute(float t) {
            return t;
        }
    }
}