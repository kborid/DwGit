package com.smartisanos.ideapills.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.ImageView;
import com.smartisanos.ideapills.R;
import com.smartisanos.ideapills.common.anim.Anim;
import com.smartisanos.ideapills.common.anim.SimpleAnimListener;
import com.smartisanos.ideapills.common.anim.Vector3f;
import com.smartisanos.ideapills.entity.BubbleItem;

public class BubbleToDoCheckBox extends ImageView {
    private Anim mShowAnim = null;
    private int mStartTran;
    private int mMiddleTran;
    private int mEndTran;
    private boolean mCheckouted = false;
    private int mDownTranInLarge;
    protected OnCheckedChangeListener mKeepCheckedChangeListener;
    private int[] mCheckedResource = BUBBLE_TODO_CHECKBOX;
    public static int [] BUBBLE_TODO_CHECKBOX_SHARE = {R.drawable.to_do_ok_share,R.drawable.to_do_backlog_share };
    public static int [] BUBBLE_TODO_CHECKBOX = {R.drawable.to_do_ok,R.drawable.to_do_backlog};

    public BubbleToDoCheckBox(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public BubbleToDoCheckBox(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mStartTran = getResources().getDimensionPixelOffset(R.dimen.bubble_todo_show_width);
        mMiddleTran = -getResources().getDimensionPixelOffset(R.dimen.bubble_todo_show_maxout);
        mEndTran = 0;
        mDownTranInLarge = getResources().getDimensionPixelSize(R.dimen.bubble_todo_move_down);
        setClickable(true);
        setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                mCheckouted = !mCheckouted;
                setChecked(mCheckouted);
                if (mKeepCheckedChangeListener != null) {
                    mKeepCheckedChangeListener.onBubbleBoxCheckedChanged(BubbleToDoCheckBox.this, mCheckouted);
                }
            }
        });
    }

    private void resetAnim() {
        if (mShowAnim != null) {
            mShowAnim.cancel();
        }
    }

    public void playShowAnim() {
        resetAnim();
        mShowAnim = new Anim(this, Anim.TRANSLATE, 150, Anim.CUBIC_OUT, new Vector3f(mStartTran, 0), new Vector3f(mMiddleTran, 0));
        mShowAnim.setListener(new SimpleAnimListener() {
            public void onStart() {
                setVisibility(VISIBLE);
            }

            public void onComplete(int type) {
                mShowAnim = new Anim(BubbleToDoCheckBox.this, Anim.TRANSLATE, 200, Anim.CUBIC_OUT, new Vector3f(mMiddleTran, 0), new Vector3f(mEndTran, 0));
                mShowAnim.setListener(new SimpleAnimListener() {

                    public void onComplete(int type) {
                        mShowAnim = null;
                    }
                });
                mShowAnim.start();
            }
        });
        mShowAnim.start();
    }

    public void playHideAnim() {
        resetAnim();
        mShowAnim = new Anim(this, Anim.TRANSLATE, 200, Anim.CUBIC_OUT, new Vector3f(getTranslationX(), 0), new Vector3f(mStartTran, 0));
        mShowAnim.setListener(new SimpleAnimListener() {
            public void onStart() {
                setVisibility(VISIBLE);
            }

            public void onComplete(int type) {
                setVisibility(GONE);
                mShowAnim = null;
            }
        });
        mShowAnim.start();
    }

    public void animDuringToLarge(float percent) {
        if (getVisibility() == VISIBLE) {
            setTranslationY(mDownTranInLarge * percent);
        }
    }

    public void playToLargeAnim(int duration) {
        resetAnim();
        mShowAnim = new Anim(this, Anim.TRANSLATE, duration, Anim.CIRC_OUT, Anim.ZERO, new Vector3f(0.0f, mDownTranInLarge));
        mShowAnim.setListener(new SimpleAnimListener() {
            public void onStart() {

            }

            public void onComplete(int type) {
                mShowAnim = null;
            }
        });
        mShowAnim.start();
    }

    public void playToNormalAnim(int duration) {
        resetAnim();
        mShowAnim = new Anim(this, Anim.TRANSLATE, duration, Anim.QUAD_IN, new Vector3f(0.0f, getTranslationY()), Anim.ZERO);
        mShowAnim.setListener(new SimpleAnimListener() {
            public void onStart() {

            }

            public void onComplete(int type) {
                mShowAnim = null;
            }
        });
        mShowAnim.start();
    }


    public void setOnCheckedChangeListener(OnCheckedChangeListener listener) {
        mKeepCheckedChangeListener = listener;
    }

    public void updateUI(BubbleItem item) {
        if (item.willPlayShowAnim()) {
            setVisibility(GONE);
            if (item.isInLargeMode()) {
                setTranslationY(mDownTranInLarge);
            } else {
                setTranslationY(0.0f);
            }
            setTranslationX(0.0f);
        } else {
            OnCheckedChangeListener onCheckedChangeListener = mKeepCheckedChangeListener;
            setOnCheckedChangeListener(null);
            if (item.isToDo()) {
                setVisibility(VISIBLE);
                setChecked(false);
            } else if (item.isToDoOver()) {
                setVisibility(VISIBLE);
                setChecked(true);
            } else {
                setVisibility(GONE);
            }
            setOnCheckedChangeListener(onCheckedChangeListener);
            if (getVisibility() == VISIBLE) {
                if (item.isInLargeMode()) {
                    setTranslationY(mDownTranInLarge);
                } else {
                    setTranslationY(0.0f);
                }
                setTranslationX(0.0f);
            }
        }
    }

    /**
     *  not use the method ,use  link@setBackgroundResources
     * @param resid The identifier of the resource.
     *
     */
    @Override
    public void setBackgroundResource(int resid) {
        super.setBackgroundResource(resid);
    }

    public void setBackgroundResources(int[] backgroundResources) {
        if(backgroundResources != null && backgroundResources.length == 2){
            mCheckedResource = backgroundResources;
            setChecked(mCheckouted);
        }
    }

    public void setChecked(boolean checked) {
        this.mCheckouted = checked;
        if (mCheckouted) {
            setBackgroundResource(mCheckedResource[0]);
        } else {
            setBackgroundResource(mCheckedResource[1]);
        }
    }

    public interface OnCheckedChangeListener {
        void onBubbleBoxCheckedChanged(BubbleToDoCheckBox buttonView, boolean isChecked);
    }

    @Override
    public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo info) {
        super.onInitializeAccessibilityNodeInfo(info);
        info.setSelected(mCheckouted);
    }

}
