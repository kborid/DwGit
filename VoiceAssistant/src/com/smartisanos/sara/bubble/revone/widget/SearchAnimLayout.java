package com.smartisanos.sara.bubble.revone.widget;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;

import com.smartisanos.sara.R;
import com.smartisanos.sara.SaraApplication;
import com.smartisanos.sara.bubble.revone.utils.Cubic;

import java.util.ArrayList;
import java.util.List;

public class SearchAnimLayout extends FrameLayout {

    private static final int COMMON_TIME = 350;
    private static final int COMMON_DELAY = 100;

    public static final int PLACE_TYPE_NORMAL = 1;
    public static final int PLACE_TYPE_WEB = 2;

    private ArrayList<View> mAnimChildren = new ArrayList<>();
    private ArrayList<Rect> mAnimLaunchBoundsList = new ArrayList<>();
    private Rect mAnimSrcBounds;
    private AnimatorSet mAnim;

    private final Object mResLoadLock = new Object();
    private Drawable mNormalAppPlaceDrawable;
    private Drawable mWebAppPlaceDrawable;

    public SearchAnimLayout(Context context) {
        super(context);
    }

    public SearchAnimLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public SearchAnimLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void loadPlaceHolderImg() {
        new AsyncTask<Object, Object, Object>() {

            @Override
            protected Object doInBackground(Object... params) {
                Resources resources = SaraApplication.getInstance().getResources();
                synchronized (mResLoadLock) {
                    mNormalAppPlaceDrawable = resources.getDrawable(R.drawable.revone_splash_normal_app);
                    mWebAppPlaceDrawable = resources.getDrawable(R.drawable.revone_splash_browser);
                }
                return null;
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    public void clearItems() {
        if (mAnim != null && mAnim.isRunning()) {
            mAnim.cancel();
            mAnim = null;
        }
        mAnimLaunchBoundsList.clear();
        mAnimChildren.clear();
        mAnimSrcBounds = null;
        removeAllViews();
    }

    public void hideItem(Rect launchBounds) {
        int index = mAnimLaunchBoundsList.indexOf(launchBounds);
        if (index >= 0) {
            View v = mAnimChildren.get(index);
            v.clearAnimation();
            v.setAlpha(0);
        }
    }

    public void addItem(Rect launchBounds, int type) {
        if (mAnimLaunchBoundsList.isEmpty()) {
            mAnimSrcBounds = launchBounds;
        }
        View v = generatePlaceView(type);
        LayoutParams lp = new LayoutParams(launchBounds.width(), launchBounds.height());
        lp.leftMargin = launchBounds.left;
        lp.topMargin = launchBounds.top;
        v.setVisibility(INVISIBLE);
        addView(v, lp);
        mAnimLaunchBoundsList.add(launchBounds);
        mAnimChildren.add(v);
    }

    public void startAnim(int delay) {
        if (mAnimSrcBounds == null || mAnimLaunchBoundsList.isEmpty()) {
            return;
        }
        mAnim = new AnimatorSet();
        List<Animator> animatorList = new ArrayList<>();
        for (int i = 0; i < mAnimLaunchBoundsList.size(); i++) {
            final Rect launchBounds = mAnimLaunchBoundsList.get(i);
            final View itemView = mAnimChildren.get(i);
            itemView.setVisibility(VISIBLE);
            itemView.setAlpha(0);
            final ValueAnimator searchAnim = ValueAnimator.ofFloat(0f, 1f);
            searchAnim.setDuration(COMMON_TIME + COMMON_DELAY * i);
            searchAnim.setStartDelay(COMMON_DELAY * i);
            searchAnim.setInterpolator(Cubic.easeOut);
            searchAnim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {

                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    float percent = (Float) animation.getAnimatedValue();
                    itemView.setAlpha(percent);
                    itemView.setTranslationX(1f * (mAnimSrcBounds.left - launchBounds.left) * (1 - percent));
                    itemView.setTranslationY(1f * (mAnimSrcBounds.top - launchBounds.top) * (1 - percent));
                }
            });
            searchAnim.setStartDelay(i * 30);
            animatorList.add(searchAnim);
        }
        mAnim.setStartDelay(delay);
        mAnim.playTogether(animatorList);
        mAnim.start();
    }

    private View generatePlaceView(int type) {
//        LinearLayout v = new LinearLayout(getContext());
//        v.setOrientation(LinearLayout.VERTICAL);
//        View title = new View(getContext());
//        title.setBackgroundResource(R.drawable.revone_window_frame_tittlebar_inactived);
//        v.addView(title, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
//                getResources().getDimensionPixelSize(R.dimen.revone_caption_bar_height)));
        View content = new View(getContext());
        synchronized (mResLoadLock) {
            if (type == PLACE_TYPE_WEB) {
                content.setBackgroundDrawable(mWebAppPlaceDrawable);
            } else {
                content.setBackgroundDrawable(mNormalAppPlaceDrawable);
            }
        }
//        v.addView(content, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
//                ViewGroup.LayoutParams.MATCH_PARENT));
        return content;
    }
}
