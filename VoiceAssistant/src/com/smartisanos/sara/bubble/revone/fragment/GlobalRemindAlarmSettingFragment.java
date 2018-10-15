package com.smartisanos.sara.bubble.revone.fragment;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.smartisanos.ideapills.common.remind.AbstractRemindAlarmSettingFragment;
import com.smartisanos.sara.util.ToastUtil;

public class GlobalRemindAlarmSettingFragment extends AbstractRemindAlarmSettingFragment {

    public static GlobalRemindAlarmSettingFragment newInstance(Intent intent) {
        GlobalRemindAlarmSettingFragment fragment = new GlobalRemindAlarmSettingFragment();
        fragment.setShowsDialog(false);
        return fragment;
    }

    private IRemindSettingListener mRemindSettingListener;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        if (activity instanceof IRemindSettingListener) {
            mRemindSettingListener = (IRemindSettingListener) activity;
        }
    }

    @Override
    public void onDetach() {
        mRemindSettingListener = null;
        super.onDetach();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = super.onCreateView(inflater, container, savedInstanceState);
        return root;
    }

    public void showView(long dueDate, long remindTime) {
        if (getView() != null) {
            getView().setVisibility(View.VISIBLE);
            refreshByTime(dueDate, remindTime, false);
        }
    }

    public void hideView() {
        if (getView() != null) {
            getView().setVisibility(View.GONE);
        }
    }

    @Override
    protected void onClickCancel() {
        if (mRemindSettingListener != null) {
            mRemindSettingListener.onRemindTimeSet(0, 0);
        }
    }

    @Override
    protected void onClickConfirm() {
        boolean isNotify = !isSwitchFullDayChecked();
        long remindTime = getRemindTime();
        if (isNotify && isExpireTime(remindTime - getSelectBeforeTime())) {
            ToastUtil.showToast(getActivity(), getSelectBeforeTip());
            return;
        }
        if (mRemindSettingListener != null) {
            mRemindSettingListener.onRemindTimeSet(remindTime, isNotify ? remindTime - getSelectBeforeTime() : 0);
        }
    }

    @Override
    protected void onTouchOutside() {

    }

    @Override
    protected boolean hasBubbleItem() {
        return false;
    }

    @Override
    protected long getInitDueDate() {
        return 0;
    }

    @Override
    protected long getInitRemindTime() {
        return 0;
    }

    @Override
    protected boolean isSetBubbleWindowType() {
        return false;
    }

    public interface IRemindSettingListener {
        void onRemindTimeSet(long dueDate, long remindTime);
    }
}
