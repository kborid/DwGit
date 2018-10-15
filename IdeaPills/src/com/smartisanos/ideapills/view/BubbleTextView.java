package com.smartisanos.ideapills.view;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.widget.TextView;

import com.smartisanos.ideapills.util.LOG;

public class BubbleTextView extends TextView {
    private static final LOG log = LOG.getInstance(BubbleTextView.class);
    private int mTextResId = 0;

    public BubbleTextView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public BubbleTextView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        final Resources.Theme theme = context.getTheme();
        TypedArray a = theme.obtainStyledAttributes(
                attrs, com.android.internal.R.styleable.TextView, defStyleAttr, 0);

        int n = a.getIndexCount();
        for (int i = 0; i < n; i++) {
            int attr = a.getIndex(i);
            if (com.android.internal.R.styleable.TextView_text == attr) {
                mTextResId = a.getResourceId(attr, 0);
                break;
            }
        }
        a.recycle();
    }

    protected void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (mTextResId != 0) {
            setText(mTextResId);
        }
    }
}
