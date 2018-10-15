package com.kborid.smart.activity;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import com.kborid.library.common.LogUtils;
import com.kborid.smart.R;
import com.kborid.smart.adapter.StringItemAdapter;
import com.kborid.smart.listener.RecyclerItemClickListener;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class RecyclerViewTestActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private List<String> list = new ArrayList<>();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recycle_test);
        list = Arrays.asList(getResources().getStringArray(R.array.letter_list));
        StringItemAdapter adapter = new StringItemAdapter(this, list);
        recyclerView = findViewById(R.id.recycle);
        recyclerView.setLayoutManager(new GridLayoutManager(this, 1));
        recyclerView.setAdapter(adapter);
        recyclerView.addOnItemTouchListener(new RecyclerItemClickListener(this, new RecyclerItemClickListener.OnItemClickListener() {
            @Override
            public void onItemClick(View view, int position) {
                LogUtils.d("recycle view item click position = " + position);
                String string = list.get(position);
                startTargetBigScaleAnim(string, view);
            }

            @Override
            public void onLongClick(View view, int position) {
                LogUtils.d("recycle view item long click position = " + position);
            }
        }));
    }

    private void startTargetBigScaleAnim(final String string, final View view) {

        ObjectAnimator scaleX = ObjectAnimator.ofFloat(view, "scaleX", 1f, 1.1f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(view, "scaleY", 1f, 1.1f);

        AnimatorSet set = new AnimatorSet();
        set.setDuration(300);
        set.playTogether(scaleX, scaleY);
        set.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {

            }

            @Override
            public void onAnimationEnd(Animator animation) {
                startTargetSmallScaleAnim(string, view);
            }

            @Override
            public void onAnimationCancel(Animator animation) {
            }

            @Override
            public void onAnimationRepeat(Animator animation) {
            }
        });
        set.start();
    }

    private void startTargetSmallScaleAnim(final String string, final View view) {

        ObjectAnimator scaleX = ObjectAnimator.ofFloat(view, "scaleX", 1.1f, 1f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(view, "scaleY", 1.1f, 1f);

        AnimatorSet set = new AnimatorSet();
        set.setDuration(200);
        set.playTogether(scaleX, scaleY);
        set.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                startTargetBigScaleAnim(string, view);
            }

            @Override
            public void onAnimationCancel(Animator animation) {
            }

            @Override
            public void onAnimationRepeat(Animator animation) {
            }
        });
        set.start();
    }
}
