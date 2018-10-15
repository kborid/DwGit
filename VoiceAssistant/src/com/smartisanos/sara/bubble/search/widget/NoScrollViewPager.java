package com.smartisanos.sara.bubble.search.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.animation.Interpolator;

import com.smartisanos.sara.bubble.search.util.ViewPagerScroller;

import smartisanos.view.ViewPager;


public class NoScrollViewPager extends ViewPager {

    private static final Interpolator sInterpolator = new Interpolator() {
        @Override
        public float getInterpolation(float input) {
            return (input -= 1) * input * input + 1;
        }
    };

    private int mScrollDuration = 300;   // 滑动速度

    /**
     * 设置时间
     *
     * @param duration
     */
    public void setScrollDuration(int duration) {
        this.mScrollDuration = duration;
    }

    private boolean isScroll;
    private ViewPagerScroller scroller;

    public NoScrollViewPager(Context context, AttributeSet attrs) {
        super(context, attrs);
        scroller = new ViewPagerScroller(context, sInterpolator);
        scroller.initViewPagerScroll(NoScrollViewPager.this);
        scroller.setScrollDuration(mScrollDuration);
    }

    @Override
    public void setCurrentItem(int item) {
        super.setCurrentItem(item);
        scroller.setScrollDuration(mScrollDuration);
    }

    /**
     * 1.dispatchTouchEvent一般情况不做处理
     * ,如果修改了默认的返回值,子孩子都无法收到事件
     */
    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        return super.dispatchTouchEvent(ev);   // return true;不行
    }

    /**
     * 是否拦截
     * 拦截:会走到自己的onTouchEvent方法里面来
     * 不拦截:事件传递给子孩子
     */
    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        // return false;//可行,不拦截事件,
        // return true;//不行,孩子无法处理事件
        //return super.onInterceptTouchEvent(ev);//不行,会有细微移动
        if (isScroll) {
            return super.onInterceptTouchEvent(ev);
        } else {
            return false;
        }
    }

    /**
     * 是否消费事件
     * 消费:事件就结束
     * 不消费:往父控件传
     */
    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        //return false;// 可行,不消费,传给父控件
        //return true;// 可行,消费,拦截事件
        //super.onTouchEvent(ev); //不行,
        //虽然onInterceptTouchEvent中拦截了,
        //但是如果viewpage里面子控件不是viewgroup,还是会调用这个方法.
        if (isScroll) {
            return super.onTouchEvent(ev);
        } else {
            return true;// 可行,消费,拦截事件
        }
    }

    public void setScroll(boolean scroll) {
        isScroll = scroll;
    }

}


