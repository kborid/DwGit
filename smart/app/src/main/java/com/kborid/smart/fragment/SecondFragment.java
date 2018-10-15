package com.kborid.smart.fragment;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.kborid.library.common.UIHandler;
import com.kborid.smart.R;
import com.kborid.smart.activity.RecyclerViewTestActivity;

public class SecondFragment extends Fragment implements View.OnClickListener {

    private static final String KEY = "Fragment";

    private ImageView iv_voice;
    private Button btn_click1, btn_click2;

    public static Fragment newInstance(String index) {
        SecondFragment fragment = new SecondFragment();
        Bundle bundle = new Bundle();
        bundle.putString(KEY, index);
        fragment.setArguments(bundle);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_second, container, false);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        View view = getView();
        if (null != view) {
            TextView tv_test = view.findViewById(R.id.test);
            tv_test.setText(getArguments().getString(KEY));
            iv_voice = view.findViewById(R.id.iv_voice);
            btn_click1 = view.findViewById(R.id.btn_click1);
            btn_click1.setOnClickListener(this);
            btn_click2 = view.findViewById(R.id.btn_click2);
            btn_click2.setOnClickListener(this);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        UIHandler.removeCallbacks(runnable);
    }

    private void startTargetBigScaleAnim(final View view) {

        ObjectAnimator scaleX = ObjectAnimator.ofFloat(view, "scaleX", 1f, 1.07f, 1.03f, 1.1f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(view, "scaleY", 1f, 1.07f, 1.03f, 1.1f);

        AnimatorSet set = new AnimatorSet();
        set.setDuration(350);
        set.playTogether(scaleX, scaleY);
        set.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                startTargetSmallScaleAnim(view);
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

    private void startTargetSmallScaleAnim(final View view) {

        ObjectAnimator scaleX = ObjectAnimator.ofFloat(view, "scaleX", 1.1f, 1f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(view, "scaleY", 1.1f, 1f);

        AnimatorSet set = new AnimatorSet();
        set.setDuration(250);
        set.playTogether(scaleX, scaleY);
        set.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {
            }

            @Override
            public void onAnimationEnd(Animator animation) {
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

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_click1:
                UIHandler.removeCallbacks(runnable);
                UIHandler.post(runnable);
                break;
            case R.id.btn_click2:
                startActivity(new Intent(getActivity(), RecyclerViewTestActivity.class));
                break;
        }
    }

    private Runnable runnable = new Runnable() {
        @Override
        public void run() {
            startTargetBigScaleAnim(iv_voice);
            UIHandler.postDelayed(runnable, 900);
        }
    };
}
