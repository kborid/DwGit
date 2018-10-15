package com.smartisanos.sara.widget;

import com.smartisanos.sara.util.LogUtils;
import com.smartisanos.sara.util.SaraUtils;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Canvas;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.AnimationSet;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;

import com.smartisanos.sara.R;

public class LeftSlideGuideView extends FrameLayout {
    private View mCloseBtn;
    private View mGuideBody;
    private View mDimLayer;
    private TwistGuideAnimController mAnimController = new TwistGuideAnimController();

    private class TwistGuideAnimController {
        private Runnable mRemoveViewRunnable = new Runnable() {
            public void run() {
                if (getParent() != null){
                    ((RelativeLayout) getParent()).removeView(LeftSlideGuideView.this);
                }
            }
        };

        public void close() {
            long duration = 0;

            AnimationSet shrink = (AnimationSet) AnimationUtils.loadAnimation(getContext(), R.anim.shrink_to_right_top);
            mGuideBody.startAnimation(shrink);
            duration = shrink.getDuration();
            AnimationSet rotate = (AnimationSet) AnimationUtils.loadAnimation(getContext(), R.anim.rotate);
            mCloseBtn.startAnimation(rotate);
            duration = Math.max(duration, rotate.getDuration());

            AnimationSet fade = (AnimationSet) AnimationUtils.loadAnimation(getContext(), smartisanos.R.anim.fade_out);
            mDimLayer.startAnimation(fade);
            duration = Math.max(duration, fade.getDuration());

            LeftSlideGuideView.this.postDelayed(mRemoveViewRunnable, duration);
        }
    }

    private OnClickListener mListener = new OnClickListener() {

      public void onClick(View arg0) {
          mAnimController.close();
      };
    };

    public LeftSlideGuideView(Context context) {
        super(context);
        initLayout();
    }

    private void initLayout() {
        mDimLayer = new View(getContext());
        mDimLayer.setBackgroundColor(0x98000000); // Dim 0.6
        // Disable hardware acceleration for smooth animation
        mDimLayer.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        addView(mDimLayer);

        LayoutInflater inflater = (LayoutInflater) getContext()
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.left_slide_guide_view, this);

        mCloseBtn = findViewById(R.id.btn_close);
        mCloseBtn.setOnClickListener(mListener);
        mCloseBtn.setLayerType(View.LAYER_TYPE_SOFTWARE, null);

        mGuideBody = findViewById(R.id.guid_body);
        mGuideBody.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
    }
    public boolean clickCloseBtn(MotionEvent event){
        return SaraUtils.checkFinish(mCloseBtn, event);
    }
}
