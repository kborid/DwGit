package com.smartisanos.sara.bubble.revone.widget;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.content.Context;
import android.util.AttributeSet;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.LinearLayout;

import com.smartisanos.sara.bubble.revone.ExtScreenConstant;
import com.smartisanos.sara.bubble.revone.entity.GlobalContact;

public class ImContactLayout extends LinearLayout {
    private Animator mInOutAnim;
    private GlobalContact mContact;

    public ImContactLayout(Context context) {
        this(context, null);
    }

    public ImContactLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ImContactLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        mAlwaysCanAcceptDrag = true;
    }

    public void handleDragExited() {
        if (mInOutAnim != null && mInOutAnim.isRunning()) {
            mInOutAnim.cancel();
            mInOutAnim = null;
        }
        PropertyValuesHolder scaleInX = PropertyValuesHolder.ofFloat("scaleX", ExtScreenConstant.SCALE_OUT_VALUE, 1f);
        PropertyValuesHolder scaleInY = PropertyValuesHolder.ofFloat("scaleY", ExtScreenConstant.SCALE_OUT_VALUE, 1f);
        mInOutAnim = ObjectAnimator.ofPropertyValuesHolder(this, scaleInX, scaleInY);
        mInOutAnim.setInterpolator(new AccelerateInterpolator(1.5f));
        mInOutAnim.setDuration(ExtScreenConstant.DROP_ANIM_TIME);
        mInOutAnim.start();
    }

    public void handleDragEntered() {
        if (mInOutAnim != null && mInOutAnim.isRunning()) {
            mInOutAnim.cancel();
            mInOutAnim = null;
        }
        PropertyValuesHolder scaleOutX = PropertyValuesHolder.ofFloat("scaleX", 1f, ExtScreenConstant.SCALE_OUT_VALUE);
        PropertyValuesHolder scaleOutY = PropertyValuesHolder.ofFloat("scaleY", 1f, ExtScreenConstant.SCALE_OUT_VALUE);
        mInOutAnim = ObjectAnimator.ofPropertyValuesHolder(this, scaleOutX, scaleOutY);
        mInOutAnim.setInterpolator(new DecelerateInterpolator(1.5f));
        mInOutAnim.setDuration(ExtScreenConstant.GENERAL_ANIM_TIME);
        mInOutAnim.start();
    }

    public void setContact(GlobalContact contact) {
        mContact = contact;
    }

    public GlobalContact getContact() {
        return mContact;
    }
}
