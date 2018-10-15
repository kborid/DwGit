package com.smartisanos.sara.widget;
import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.view.View;
import android.view.animation.Animation;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import com.smartisanos.sara.widget.pinnedHeadList.WrapperView;
import com.smartisanos.sara.util.CubicInterpolator;

/**
 * used to do multi-delete animation.
 */
public abstract class MultiDeleteAnimation<T extends ListView & MultiDeleteAnimation.MultiDeleteAnimationOperator> {

    private final T mTarget;
    private boolean mHeaderMode;
    private static final long MULTI_RIGHT_SLIDE_DURATION = 200L;
    private HashSet<String> mDeletedHeaderSections;
    private boolean mResultEmpty = false;

    public MultiDeleteAnimation(T target) {
        mTarget = target;
    }

    public MultiDeleteAnimation(T target, boolean headerMode) {
        mTarget = target;
        mHeaderMode = headerMode;
    }

    public abstract void onMultiDeleteAnimationStart();

    public abstract void onMultiDeleteAnimationEnd();

    public interface MultiDeleteAnimationOperator {
        public void startDeleteAnimations(List<Integer> deletedPositions,
                Animation.AnimationListener animationListener);
    }

    public void start(final ArrayList<Integer> deleted, final ArrayList<Integer> reserved) {
        ArrayList<Animator> animators = getRightSlideAnimators(deleted);
        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {
                onMultiDeleteAnimationStart();
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                onRightSlideAnimationEnd(reserved);
            }

            @Override
            public void onAnimationCancel(Animator animation) {
            }

            @Override
            public void onAnimationRepeat(Animator animation) {

            }
        });
        animatorSet.playTogether(animators);
        animatorSet.setDuration(MULTI_RIGHT_SLIDE_DURATION);
        animatorSet.setInterpolator(CubicInterpolator.OUT);
        animatorSet.start();
    }

    public ArrayList<Animator> getRightSlideAnimators(ArrayList<Integer> positions) {
        ArrayList<Animator> animators = new ArrayList<Animator>();
        int first = mTarget.getFirstVisiblePosition();
        int last = mTarget.getLastVisiblePosition();
        for (int position : positions) {
            if (position < first || position > last) {
                continue;
            }
            final View child = mTarget.getChildAt(position - first);
            if (child != null) {
                int width = child.getWidth();
                View targetView;
                if (isHeaderRetain(child)) {
                    targetView = ((WrapperView) child).getItem();
                } else {
                    targetView = child;
                }
                Animator animator = ObjectAnimator.ofFloat(targetView, "translationX", 0, width);
                animators.add(animator);
            }
        }
        return animators;
    }

    private void onRightSlideAnimationEnd(final ArrayList<Integer> positions) {
        if (mResultEmpty) {
            end();
        } else {
            mTarget.startDeleteAnimations(positions, new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {
                }

                @Override
                public void onAnimationEnd(Animation animation) {
                    end();
                }

                @Override
                public void onAnimationRepeat(Animation animation) {
                }
            });
        }
    }

    /**
     * all animation end.
     */
    private void end() {

        // Avoiding the child view can't recovery when deleting at the same time importing.
        // so we need process all child view.
        int childCount = mTarget.getChildCount();
        for (int i = 0; i < childCount; i++) {
            View child = mTarget.getChildAt(i);
            if (child != null) {
                if (child instanceof WrapperView) {
                    ((WrapperView) child).getItem().setTranslationX(0F);
                }
                child.setTranslationX(0F);
            }
        }

        onMultiDeleteAnimationEnd();
    }

    public MultiDeleteAnimation<T> setResultListSize(int size) {
        mResultEmpty = (size <= 0);
        return this;
    }

    public MultiDeleteAnimation<T> setDeletedHeaderSection(HashSet<String> headerSection) {
        mDeletedHeaderSections = headerSection;
        return this;
    }

    /**
     * <code>
     * if (mHeaderMode) {
     *     if (child != null && child instanceof WrapperView && ((WrapperView)child).hasHeader()) {
     *         String section = (String) ((WrapperView) child).getHeader().getTag();
     *         if (mDeletedHeaderSections.contains(section)) {
     *             targetView = child;
     *         } else {
     *             targetView = ((WrapperView) child).getItem();
     *         }
     *     } else {
     *         targetView = child;
     *     }
     * } else {
     *     targetView = child;
     * }
     * </code>
     * 
     * @param child
     * @return
     */
    private boolean isHeaderRetain(View child) {
        if (mHeaderMode && child != null && child instanceof WrapperView
                && ((WrapperView) child).hasHeader()) {
            String section = (String) ((WrapperView) child).getHeader().getTag();
            if (mDeletedHeaderSections == null || mDeletedHeaderSections.isEmpty()
                    || !mDeletedHeaderSections.contains(section)) {
                return true;
            }
        }
        return false;
    }

}
