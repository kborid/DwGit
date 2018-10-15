package com.smartisanos.sara.bubble.revone.widget;

import android.content.Context;
import android.graphics.Point;
import android.util.AttributeSet;
import android.view.DragEvent;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;

import com.smartisanos.sara.bubble.revone.entity.GlobalContact;

public class DragContentLayout extends FrameLayout {
    private IDragEventListener mDragEventListener;
    private ITouchDownListener mTouchDownListener;
    private View mCurrentFocusChild;
    private boolean mDropHandled = false;
    private Point mLastLocationPoint = new Point();

    public DragContentLayout(Context context) {
        this(context, null);
    }

    public DragContentLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        mAlwaysCanAcceptDrag = true;
    }

    @Override
    public boolean dispatchDragEvent(DragEvent event) {
        boolean result = super.dispatchDragEvent(event);
        int action = event.getAction();
        if (mDragEventListener != null) {
            switch (action) {
                case DragEvent.ACTION_DRAG_STARTED: {
                    mDropHandled = false;
                    mCurrentFocusChild = null;
                    mDragEventListener.onDragStart(event);
                    break;
                }
                case DragEvent.ACTION_DRAG_ENDED: {
                    if (mCurrentFocusChild != null && mCurrentFocusChild instanceof ImContactLayout) {
                        ((ImContactLayout) mCurrentFocusChild).handleDragExited();
                    }
                    mDragEventListener.onDragEnd(mDropHandled, mLastLocationPoint);
                    mCurrentFocusChild = null;
                    return true;
                }
                case DragEvent.ACTION_DROP: {
                    mLastLocationPoint.set((int) event.getX(), (int) event.getY());
                    GlobalContact contact = null;
                    if (mCurrentFocusChild != null && mCurrentFocusChild instanceof ImContactLayout) {
                        ((ImContactLayout) mCurrentFocusChild).handleDragExited();
                    }
                    View target = mDragEventListener.getDragFocusChild(event);
                    if (target != null && target instanceof ImContactLayout) {
                        contact = ((ImContactLayout) target).getContact();
                    }
                    mDropHandled = contact != null ? true : false;
                    mDragEventListener.onDrop(contact);
                    mCurrentFocusChild = null;
                    return true;
                }
                case DragEvent.ACTION_DRAG_LOCATION: {
                    mLastLocationPoint.set((int) event.getX(), (int) event.getY());
                    View target = mDragEventListener.getDragFocusChild(event);
                    if (target != mCurrentFocusChild) {
                        if (mCurrentFocusChild != null && mCurrentFocusChild instanceof ImContactLayout) {
                            ((ImContactLayout) mCurrentFocusChild).handleDragExited();
                        }
                        if (target != null && target instanceof ImContactLayout) {
                            ((ImContactLayout) target).handleDragEntered();
                        }
                    }
                    mCurrentFocusChild = target;
                }
                break;
            }
        }
        return result;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        int action = ev.getAction();
        if (mTouchDownListener != null && action == MotionEvent.ACTION_DOWN) {
            mTouchDownListener.onTouchDown(ev);
        }
        return super.onInterceptTouchEvent(ev);
    }

    public void setTouchDownListener(ITouchDownListener l) {
        mTouchDownListener = l;
    }

    public void setDragStartListener(IDragEventListener l) {
        mDragEventListener = l;
    }

    public interface ITouchDownListener {
        void onTouchDown(MotionEvent ev);
    }

    public interface IDragEventListener {
        void onDragStart(DragEvent dragEvent);

        void onDragEnd(boolean handled, Point location);

        void onDrop(GlobalContact contact);

        View getDragFocusChild(DragEvent dragEvent);
    }
}
