package com.smartisanos.sara.bubble.revone.view;

import android.content.Context;
import android.view.View;
import android.view.ViewStub;
import android.widget.FrameLayout;

import com.smartisanos.sara.R;
import com.smartisanos.sara.bubble.revone.ExtScreenConstant;

public class ButtonViewManager extends ViewManager implements IButtonView {
    private ICloseListener mCloseListener;
    private ISettingListener mISettingListener;

    public ButtonViewManager(Context context, View view) {
        super(context, view);
    }

    public ButtonViewManager(Context context, View view, ICloseListener closeListener) {
        super(context, view);
        mCloseListener = closeListener;
    }

    public ButtonViewManager(Context context, View view, ICloseListener closeListener, ISettingListener settingListener) {
        super(context, view);
        mCloseListener = closeListener;
        mISettingListener = settingListener;
    }

    @Override
    protected View getView() {
        View view = null;
        if (mRootView != null) {
            ViewStub buttonStup = (ViewStub) mRootView.findViewById(R.id.button_stub);
            if (buttonStup != null && buttonStup.getParent() != null) {
                view = buttonStup.inflate();
            } else {
                view = mRootView.findViewById(R.id.app_drawer);
            }
            if (ExtScreenConstant.CONTAINS_STATUS_NAVI_BAR) {
                FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) view.getLayoutParams();
                lp.topMargin += ExtScreenConstant.STATUS_BAR_HEIGHT;
            }
            View settingButton = view.findViewById(R.id.setting_button);
            settingButton.setVisibility(mISettingListener != null ? View.VISIBLE : View.GONE);
            settingButton.setOnClickListener(mOnClickListener);
            View closeButton = view.findViewById(R.id.close_button);
            closeButton.setVisibility(mCloseListener != null ? View.VISIBLE : View.GONE);
            closeButton.setOnClickListener(mOnClickListener);
        }
        return view;
    }

    @Override
    public void performSetting() {
        if (mView != null) {
            View settingButton = mView.findViewById(R.id.setting_button);
            if (mISettingListener != null && settingButton != null) {
                mISettingListener.showSettingWindow(settingButton);
            }
        }
    }

    private View.OnClickListener mOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            int id = view.getId();
            switch (id) {
                case R.id.setting_button:
                    if (mISettingListener != null) {
                        mISettingListener.showSettingWindow(view);
                    }
                    break;
                case R.id.close_button:
                    if (mCloseListener != null) {
                        mCloseListener.onClose();
                    }
                    break;
            }
        }
    };

    public interface ICloseListener {
        void onClose();
    }

    public interface ISettingListener {
        void showSettingWindow(View view);
    }
}
