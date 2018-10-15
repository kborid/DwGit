package com.smartisanos.sara.bubble.revone.widget;

import android.content.Context;
import android.util.AttributeSet;
import com.smartisanos.sara.bubble.revone.drag.DragSortListView;

public class DragListView extends DragSortListView {
    public DragListView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public DragListView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public interface OnDragEventListener {
        void onStartDrag(int position);

        void onStopDrag();
    }

    private OnDragEventListener mDragEventListener;

    public void setOnDragEventListener(OnDragEventListener listener) {
        mDragEventListener = listener;
    }

    @Override
    public boolean startDrag(int position, int dragFlags, int deltaX, int deltaY) {
        if (mDragEventListener != null) {
            mDragEventListener.onStartDrag(position);
        }
        return super.startDrag(position, dragFlags, deltaX, deltaY);
    }

    @Override
    public boolean stopDrag(boolean remove) {
        if (mDragEventListener != null) {
            mDragEventListener.onStopDrag();
        }
        return super.stopDrag(remove);
    }
}
