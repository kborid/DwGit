package com.smartisanos.ideapills.view;

import android.animation.ValueAnimator;
import android.graphics.Canvas;
import android.util.SparseArray;
import android.util.SparseIntArray;
import android.view.View;
import android.widget.ListView;

import com.smartisanos.ideapills.common.anim.AnimListener;
import com.smartisanos.ideapills.util.LOG;

import java.util.ArrayList;
import java.util.List;

public class BubbleListDropBackAnimRunner implements AnimRunnerInterface{
    private static final LOG log = LOG.getInstance(BubbleListDropBackAnimRunner.class);
    private List<View> mViewList;
    private ValueAnimator mAnimator;
    private List<Integer> mStart;
    private List<Integer> mEnd;
    private List<Integer> mCurY;
    private BubbleListView mListView;

    public BubbleListDropBackAnimRunner(BubbleListView listView, List<View> viewlist, List<Integer> start, List<Integer> end) {
        mViewList = viewlist;
        mListView = listView;
        mCurY = new ArrayList<Integer>(start);
        mStart = start;
        mEnd = end;
        mAnimator = ValueAnimator.ofFloat(0.0f, 1.0f);
        mAnimator.setDuration(200);
        mAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            public void onAnimationUpdate(ValueAnimator animation) {
                Float f = (Float) animation.getAnimatedValue();
                mCurY.clear();
                int size = mViewList.size();
                for (int i = 0; i < size; i++) {
                    int sy = mStart.get(i);
                    int cy = (int) (sy + f * (mEnd.get(i) - sy));
                    mCurY.add(cy);
                }
                mListView.invalidate();
            }
        });
    }

    public boolean isRunning() {
        return mAnimator != null && mAnimator.isRunning();
    }

    public void draw(Canvas canvas) {
        canvas.save();
        int size = mViewList.size();
        for (int i = 0; i < size; i++) {
            View view = mViewList.get(i);
            canvas.translate(0, mCurY.get(i));
            view.draw(canvas);
            canvas.translate(0, -mCurY.get(i));
        }
        canvas.restore();
    }

    public void start() {
        mAnimator.start();
        mListView.invalidate();
    }

    public void addAnimListener(AnimListener listener) {

    }

    public void cancel() {
        if (mAnimator != null) {
            mAnimator.cancel();
            mAnimator = null;
        }
    }

    @Override
    public void recycle() {

    }
}
