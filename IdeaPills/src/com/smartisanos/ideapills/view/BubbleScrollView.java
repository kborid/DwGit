package com.smartisanos.ideapills.view;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.drawable.Drawable;
import android.service.onestep.GlobalBubble;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ScrollView;

import com.smartisanos.ideapills.R;
import com.smartisanos.ideapills.entity.BubbleItem;
import com.smartisanos.ideapills.util.LOG;
import com.smartisanos.ideapills.util.ThreadVerify;

import java.lang.reflect.Field;

public class BubbleScrollView extends ScrollView {

    private static LOG log = LOG.getInstance(BubbleScrollView.class);
    private Drawable mTopShade;
    private Drawable mBottomShade;
    private int maxHeight = Integer.MAX_VALUE;


    public int getMaxHeight() {
        return maxHeight;
    }

    /**
     *  when  mIsMaxHeight is true , will use  maxHeight,
     * @param maxHeight
     */
    public void setMaxHeight(int maxHeight) {
        this.maxHeight = maxHeight;
    }

    private boolean mIsMaxHeight = false;
    private ValueAnimator mValueAnimator = null;

    public BubbleScrollView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    public BubbleScrollView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public BubbleScrollView(Context context) {
        super(context);
        init();
    }

    private void init() {
        setDrawScrollBarEnable(false);
        this.mTopShade = this.getResources().getDrawable(R.drawable.shade_top);
        this.mBottomShade = this.getResources().getDrawable(R.drawable.shade_bottom);
    }

    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (!mIsMaxHeight) {
            return false;
        }
        return super.dispatchTouchEvent(ev);

    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        final BubbleEditText bubbleEditText = (BubbleEditText) getChildAt(0);
        if (mIsMaxHeight && (mValueAnimator == null || !mValueAnimator.isRunning())) {
            int newHeightSpec = MeasureSpec.makeMeasureSpec(maxHeight,
                    MeasureSpec.AT_MOST);
            super.onMeasure(widthMeasureSpec, newHeightSpec);
            bubbleEditText.setNeedMaxHeight(false);
        } else {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
            bubbleEditText.setNeedMaxHeight(true);
        }
    }

    
    public void setDrawScrollBarEnable(boolean flag) {
        try {
            Field field = View.class.getDeclaredField("mDrawScrollBar");
            if(field != null) {
                field.setAccessible(true);
                field.set(this, flag);
            }
        } catch (NoSuchFieldException e) {

        } catch (IllegalAccessException e) {

        }
    }

    public void setMaxHeight(boolean isMaxHeight, boolean needAnim) {
        ThreadVerify.enforceUiThread();
        if (mIsMaxHeight != isMaxHeight) {
            mIsMaxHeight = isMaxHeight;
            if (!isMaxHeight) {
                getLayoutParams().height = ViewGroup.LayoutParams.WRAP_CONTENT;
                setLayoutParams(getLayoutParams());
            } else {
                if (mValueAnimator != null) {
                    mValueAnimator.cancel();
                }
                if (getHeight() > maxHeight) {
                    if (needAnim) {
                        mValueAnimator = ValueAnimator.ofInt(getHeight(), maxHeight);
                        mValueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                            public void onAnimationUpdate(ValueAnimator animation) {
                                getLayoutParams().height = (Integer) animation.getAnimatedValue();
                                setLayoutParams(getLayoutParams());
                            }

                        });

                        mValueAnimator.start();
                    } else {
                        getLayoutParams().height = maxHeight;
                        setLayoutParams(getLayoutParams());
                    }

                }
            }
        }
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        super.dispatchDraw(canvas); // after draw child , draw the shade if possible
        if (mIsMaxHeight) {
            canvas.save();
            drawTopShade(canvas);
            drawBottomShade(canvas);
            canvas.restore();
        }
    }


    private void drawTopShade(Canvas canvas) {
        final boolean reachMax = (getHeight() >= maxHeight);
        if (reachMax) {
            mTopShade.setBounds(0, getScrollY() + getPaddingTop(), getWidth() - getPaddingRight(), mTopShade.getIntrinsicHeight() + getScrollY() + getPaddingTop());
            mTopShade.draw(canvas);
        }
    }

    private void drawBottomShade(Canvas canvas) {
        final boolean reachMax = (getHeight() >= maxHeight);
        if (reachMax) {
            mBottomShade.setBounds(0, getScrollY() + getHeight() - mBottomShade.getIntrinsicHeight() - getPaddingBottom(), getWidth() - getPaddingRight() , getHeight() + getScrollY() - getPaddingBottom());
            mBottomShade.draw(canvas);
        }
    }

    public void setPress(ColorMatrixColorFilter colorMatrixColorFilter) {
        if (mTopShade != null) {
            mTopShade.setColorFilter(colorMatrixColorFilter);
        }
        if (mBottomShade != null) {
            mBottomShade.setColorFilter(colorMatrixColorFilter);
        }
    }

    public void clearPress() {
        if (mTopShade != null) {
            mTopShade.clearColorFilter();
        }
        if (mBottomShade != null) {
            mBottomShade.clearColorFilter();
        }
    }

    void setShadeColor(BubbleItem item) {
        Drawable resTop;
        Drawable resBottom;
        int color = GlobalBubble.COLOR_BLUE; // default color;
        if (item != null) {
            if(item.isShareColor()){
                mTopShade = getResources().getDrawable(R.drawable.shade_top_share);
                mBottomShade = getResources().getDrawable(R.drawable.shade_bottom_share);
                invalidate();
                return;
            }else{
                color = item.getColor();
            }
        }
        switch (color) {
            case GlobalBubble.COLOR_RED: {
                resTop = getResources().getDrawable(R.drawable.shade_red_top);
                resBottom = getResources().getDrawable(R.drawable.shade_red_bottom);
            }
            break;
            case GlobalBubble.COLOR_ORANGE: {
                resTop = getResources().getDrawable(R.drawable.shade_orange_top);
                resBottom = getResources().getDrawable(R.drawable.shade_orange_bottom);
            }
            break;
            case GlobalBubble.COLOR_GREEN: {
                resTop = getResources().getDrawable(R.drawable.shade_green_top);
                resBottom = getResources().getDrawable(R.drawable.shade_green_bottom);
            }
            break;
            case GlobalBubble.COLOR_PURPLE: {
                resTop = getResources().getDrawable(R.drawable.shade_purple_top);
                resBottom = getResources().getDrawable(R.drawable.shade_purple_bottom);
            }
            break;
            case GlobalBubble.COLOR_NAVY_BLUE: {
                resTop = getResources().getDrawable(R.drawable.ppt_shade_top);
                resBottom = getResources().getDrawable(R.drawable.ppt_shade_bottom);
            }
            break;
            default: {
                resTop = getResources().getDrawable(R.drawable.shade_top);
                resBottom = getResources().getDrawable(R.drawable.shade_bottom);
            }
            break;
        }
        mTopShade = resTop;
        mBottomShade = resBottom;
        invalidate();
    }


    /**
     * Access to the sum of all the children height;

     * @return
     */
    public int getFixedChildenHeight() {

        int resultHight = 0;

        for (int index = 0; index < getChildCount(); index++) {
            View childView = getChildAt(index);
            if (childView == null || childView.getVisibility() == View.GONE) {
                continue;
            }
            int height;
            FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) childView.getLayoutParams();
            if (childView instanceof BubbleEditText) {
                height = ((BubbleEditText) childView).getTextHeight();
            } else {
                childView.measure(0, 0);
                height = childView.getMeasuredHeight();
            }

            resultHight = resultHight + (height + layoutParams.bottomMargin + layoutParams.topMargin);
        }
        return resultHight;
    }

}
