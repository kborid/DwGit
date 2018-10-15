package com.smartisanos.sara.widget;

import com.smartisanos.sara.R;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class AppPickerSubView extends RelativeLayout {

    private boolean mSelected = false;
    private View mBackgroundView;
    private ImageView mIcon, mSelectedView;
    private TextView mAppName;

    public AppPickerSubView(Context context) {
        this(context, null);
    }

    public AppPickerSubView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public AppPickerSubView(Context context, AttributeSet attrs,
            int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public AppPickerSubView(Context context, AttributeSet attrs,
            int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mBackgroundView = findViewById(R.id.background_view);
        mIcon = (ImageView) findViewById(R.id.icon);
        mSelectedView = (ImageView) findViewById(R.id.selected);
        mAppName = (TextView) findViewById(R.id.app_name);
    }

    public void setImageDrawable(Drawable drawable) {
        mIcon.setImageDrawable(drawable);
    }

    public void setText(CharSequence cs) {
        mAppName.setText(cs);
    }

    public void setSelected(boolean selected) {
        setSelectedIcon(selected, R.drawable.icon_normal, R.drawable.icon_selected);
    }

    public void setSelectedIcon (boolean selected, int normalIcon, int selectedIcon) {
        mSelected = selected;
        mSelectedView.setImageResource(mSelected ? selectedIcon : normalIcon);
        mBackgroundView
                .setBackgroundResource(mSelected ? R.drawable.app_select_bg_selected
                        : R.drawable.app_select_bg);
    }

    public boolean isSelected() {
        return mSelected;
    }

    public ImageView getIconView() {
        return mIcon;
    }

    public ImageView getSelectedView() {
        return mSelectedView;
    }

    public void setBackgroundView(int resId) {
        mBackgroundView.setBackgroundResource(resId);
    }
}
