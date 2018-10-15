package com.smartisanos.ideapills.common.remind.view;

import android.content.Context;
import android.graphics.Canvas;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.GestureDetector.OnGestureListener;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.WindowManager;
import android.widget.ViewSwitcher;

import smartisanos.api.WindowManagerSmt;

/**
 * The ViewSwitcher support dragging
 * @author zhouxiaoxi
 */
public class DragViewSwitcher extends ViewSwitcher implements OnGestureListener, OnTouchListener {

    public interface DragViewSwitcherListener {

        public boolean prepareNextView(int changeKind);

        public boolean preparePreviouseView(int changeKind);
    }

    private GestureDetector mDetector;
    private DragViewSwitcherListener mListener;
    private Context mContext;
    private boolean mIsScroll = false;
    public static final int X_CHANGE = 1;
    public static final int Y_CHANGE = 2;
    public final int POINTERS_PROTECTED_DISTANCE = 20;
    public DragViewSwitcher(Context context) {
        super(context);
        this.mContext = context;
    }
    public void addDragViewSwitcherActor(DragViewSwitcherListener listener){
        mDetector = new GestureDetector(mContext, this);
        mListener = listener;
        setFocusable(true);
        setClickable(true);
        setLongClickable(true);
        mDetector.setIsLongpressEnabled(true);
        setOnTouchListener(this);
    }
    public DragViewSwitcher(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mContext = context;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
    }

    private boolean mIsWindowInThumbMode;
    @Override
    public boolean onDown(MotionEvent e) {
        WindowManager wm = ((WindowManager) mContext
                .getSystemService(Context.WINDOW_SERVICE));
        mIsWindowInThumbMode = WindowManagerSmt.getInstance().isWindowInthumbMode(wm);
        mIsScroll = false;
        return true;
    }

    @Override
    public void onShowPress(MotionEvent e) {
    }

    @Override
    public boolean onSingleTapUp(MotionEvent e) {
        return false;
    }

    @Override
    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
        mIsScroll = true;
        return true;
    }

    @Override
    public void onLongPress(MotionEvent e) {
    }

    @Override
    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
        WindowManager wm = ((WindowManager) mContext
                .getSystemService(Context.WINDOW_SERVICE));
        boolean isWindowInthumbMode = WindowManagerSmt.getInstance().isWindowInthumbMode(wm);
        if (!mIsWindowInThumbMode && isWindowInthumbMode) {
            // the window is pulled down in this touch, ignore fling of this touch.
            return true;
        }
        mIsScroll = true;
        boolean prepareOK = false;
        boolean yChange = (Math.abs((e1.getX() - e2.getX()))>Math.abs(e1.getY()-e2.getY()))?false:true;
        if(yChange){
            if ((e1.getY() - e2.getY()) < 0) {
                prepareOK = mListener.preparePreviouseView(Y_CHANGE);
            } else {
                prepareOK = mListener.prepareNextView(Y_CHANGE);
            }
        }else{
            if((e1.getX() - e2.getX())< 0){
                prepareOK = mListener.preparePreviouseView(X_CHANGE);

            }else{
                prepareOK = mListener.prepareNextView(X_CHANGE);
            }
        }
        if(prepareOK){
            showNext();
        }
        return true;
    }

    //add for MultiTouch
    public void onMultiTouchFling(MotionEvent e1, MotionEvent e2){
        WindowManager wm = ((WindowManager) mContext
                .getSystemService(Context.WINDOW_SERVICE));
        boolean isWindowInthumbMode = WindowManagerSmt.getInstance().isWindowInthumbMode(wm);
        if(isWindowInthumbMode || e1 == null || e2 == null){
            return;
        }
        boolean prepareOK = false;
        boolean yChange = (Math.abs((e1.getX(1) - e2.getX(1)))>Math.abs(e1.getY(1)-e2.getY(1)))?false:true;
        if(yChange){
            if ((e2.getY(1) - e1.getY(1)) > POINTERS_PROTECTED_DISTANCE) {
                prepareOK = mListener.preparePreviouseView(Y_CHANGE);
            } else if((e1.getY(1) - e2.getY(1)) > POINTERS_PROTECTED_DISTANCE) {
                prepareOK = mListener.prepareNextView(Y_CHANGE);
            }
        }else{
            if((e2.getX(1) - e1.getX(1)) > POINTERS_PROTECTED_DISTANCE){
                prepareOK = mListener.preparePreviouseView(X_CHANGE);
            } else if((e1.getX(1) - e2.getX(1)) > POINTERS_PROTECTED_DISTANCE) {
                prepareOK = mListener.prepareNextView(X_CHANGE);
            }
        }
        if(prepareOK){
            showNext();
        }
    }
    @Override
    public boolean onTouch(View v, MotionEvent event) {
       return mDetector.onTouchEvent(event);
    }
    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        // feature 3.6: when in drag mode, use the second finger will recognition
        // the one finger gesture, but it didn't work in switcher's onfling
        // we should update event down time to let
        // the event work in switcher's onfling
        if ((ev.getAction() & MotionEvent.ACTION_MASK)
                == MotionEvent.ACTION_DOWN) {
            MotionEvent newEvent = MotionEvent.obtain(ev);
            newEvent.setDownTime(SystemClock.uptimeMillis());
            mDetector.onTouchEvent(newEvent);
        } else {
            mDetector.onTouchEvent(ev);
        }
        return mIsScroll;
    }
}
