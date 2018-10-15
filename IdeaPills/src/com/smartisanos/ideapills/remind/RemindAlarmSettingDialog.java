package com.smartisanos.ideapills.remind;

import android.app.Activity;
import android.app.Dialog;
import android.app.SmtPCUtils;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.view.DisplayInfo;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

import com.smartisanos.ideapills.BubbleController;
import com.smartisanos.ideapills.Constants;
import com.smartisanos.ideapills.R;
import com.smartisanos.ideapills.common.remind.AbstractRemindAlarmSettingFragment;
import com.smartisanos.ideapills.remind.util.AlarmUtils;
import com.smartisanos.ideapills.util.GlobalBubbleManager;
import com.smartisanos.ideapills.util.GlobalBubbleUtils;
import com.smartisanos.ideapills.entity.BubbleItem;

public class RemindAlarmSettingDialog extends AbstractRemindAlarmSettingFragment {

    private final static String TAG = "RemindAlarmDlg";
    public final static String EXTRA_RESULT_CODE = "result_code";
    public final static String EXTRA_DUE_DATE = "due_date";
    public final static String EXTRA_REMIND_TIME = "remind_time";
    public final static String EXTRA_SET_BUBBLE_WINDOW_TYPE = "set_bubble_window_type";
    public final static String EXTRA_WINDOW_POSITION = "window_position";
    public final static String EXTRA_END_POSITION = "is_end_position";

    private BubbleItem mBubbleItem = null;
    private Intent mIntent;
    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (getDialog() != null && getDialog().isShowing()) {
                dismissAllowingStateLoss();
            }
        }
    };

    public static RemindAlarmSettingDialog newInstance(Intent intent) {
        RemindAlarmSettingDialog dialog = new RemindAlarmSettingDialog();
        Bundle bundle = new Bundle();
        if (intent != null) {
            bundle.putInt(AlarmUtils.KEY_ALARM_ID, intent.getIntExtra(AlarmUtils.KEY_ALARM_ID, -1));
            bundle.putBoolean(EXTRA_SET_BUBBLE_WINDOW_TYPE, intent.getBooleanExtra(EXTRA_SET_BUBBLE_WINDOW_TYPE, false));
            bundle.putLong(EXTRA_DUE_DATE, intent.getLongExtra(EXTRA_DUE_DATE, 0));
            bundle.putLong(EXTRA_REMIND_TIME, intent.getLongExtra(EXTRA_REMIND_TIME, 0));
            bundle.putInt(EXTRA_WINDOW_POSITION, intent.getIntExtra(EXTRA_WINDOW_POSITION, 0));
            bundle.putBoolean(EXTRA_END_POSITION, intent.getBooleanExtra(EXTRA_END_POSITION, false));
        }
        dialog.setArguments(bundle);
        return dialog;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        if (getArguments() != null) {
            int bubbleId = getArguments().getInt(AlarmUtils.KEY_ALARM_ID, -1);
            if (bubbleId >= 0) {
                mBubbleItem = GlobalBubbleManager.getInstance().getBubbleItemById(bubbleId);
            }
        }
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if (container == null && getDialog() != null && getDialog().getWindow() != null) {
            final Window window = getDialog().getWindow();
            View decorView = window.getDecorView();
            container = ((ViewGroup) decorView.findViewById(android.R.id.content));
        }
        View root = super.onCreateView(inflater, container, savedInstanceState);
        return root;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = new Dialog(getActivity(), R.style.RemindDialogNoneFloating);
        Window window = dialog.getWindow();
        if (window != null) {
            WindowManager.LayoutParams p = window.getAttributes();
            p.width = WindowManager.LayoutParams.MATCH_PARENT;
            p.height = WindowManager.LayoutParams.MATCH_PARENT;
            if (SmtPCUtils.isValidExtDisplayId(getActivity())) {
                window.setType(BubbleController.BUBBLE_WINDOW_TYPE);
                p.width = getResources().getDimensionPixelSize(R.dimen.bubbleopt_layout_width);
                Bundle bundle = getArguments();
                if (bundle != null) {
                    int position = bundle.getInt(EXTRA_WINDOW_POSITION);
                    boolean isEndPosition = bundle.getBoolean(EXTRA_END_POSITION);
                    if (position != 0) {
                        if (isEndPosition) {
                            p.x = position - p.width;
                        } else {
                            DisplayInfo displayInfo = new DisplayInfo();
                            SmtPCUtils.getExtDisplay(getActivity()).getDisplayInfo(displayInfo);
                            p.x = position + (displayInfo.largestNominalAppWidth - position - p.width) / 2;
                        }
                        p.gravity = Gravity.LEFT | Gravity.CENTER_VERTICAL;
                    }
                }
                window.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            } else {
                if (getArguments() != null && getArguments().getBoolean(EXTRA_SET_BUBBLE_WINDOW_TYPE, false)) {
                    window.setType(BubbleController.BUBBLE_WINDOW_TYPE);
                }
                window.addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                        | WindowManager.LayoutParams.FLAG_LAYOUT_INSET_DECOR
                        | WindowManager.LayoutParams.FLAG_FULLSCREEN);
            }
            window.setAttributes(p);
        }
        dialog.setCanceledOnTouchOutside(true);
        return dialog;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        IntentFilter filter = new IntentFilter(Constants.LOCAL_BROADCAST_ACTION_BUBBLE_LIST_HIDE);
        filter.addAction("IDEAPILL_RIGHT_TRANSLATION");
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(mReceiver, filter);
    }

    @Override
    public void onDestroyView() {
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(mReceiver);
        super.onDestroyView();
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        super.onDismiss(dialog);
        Activity activity = getActivity();
        if (activity instanceof DialogInterface.OnDismissListener) {
            ((DialogInterface.OnDismissListener) activity).onDismiss(null);
        }
    }

    private void updateRemindBubbleData(long time) {
        if (null == mBubbleItem) return;

        boolean isNotify = !isSwitchFullDayChecked();
        boolean timeChanged = false;
        long showTime = time == 0 ? 0 : time + getSelectBeforeTime();
        if (mBubbleItem.getDueDate() != showTime) {
            mBubbleItem.setDueDate(showTime);
            timeChanged = true;
        }
        if (isNotify && mBubbleItem.getRemindTime() != time) {
            timeChanged = true;
        }

        if (timeChanged ||
                (isNotify != mBubbleItem.getRemindTime() > 0)) {   // remind setting changed
            if (isNotify) {
                mBubbleItem.setRemindTime(time);
            } else {
                mBubbleItem.setRemindTime(0);
            }
            AlarmUtils.scheduleNextAlarm(getActivity(), null);
        }
        if (showTime == 0) {
            AlarmUtils.deleteAlarmFromCalendar(getActivity(), mBubbleItem);
        } else {
            AlarmUtils.replaceAlarmToCalendar(getActivity(), mBubbleItem);
        }
        GlobalBubbleManager.getInstance().updateBubbleItem(mBubbleItem);
        GlobalBubbleManager.getInstance().notifyUpdate();
        GlobalBubbleUtils.trackBubbleChange(mBubbleItem);
        if (SmtPCUtils.isValidExtDisplayId(getActivity())) {
            GlobalBubbleManager.getInstance().notifyUpdate();
        }
    }

    public Intent getIntent() {
        return mIntent;
    }

    @Override
    protected void onClickCancel() {
        if (mBubbleItem != null) {
            if (mBubbleItem.getDueDate() > 0) { //cancel remind.
                updateRemindBubbleData(0);
                if (SmtPCUtils.isValidExtDisplayId(getActivity())) {
                    GlobalBubbleManager.getInstance().notifyUpdate();
                }
            }
        }
        mIntent = new Intent();
        mIntent.putExtra(EXTRA_RESULT_CODE, Activity.RESULT_CANCELED);
        dismissAllowingStateLoss();
    }

    @Override
    protected void onClickConfirm() {
        boolean isNotify = !isSwitchFullDayChecked();
        long remindTime = getRemindTime();
        if (isExpireTime(remindTime - getSelectBeforeTime(), !isNotify)) {
            GlobalBubbleUtils.showSystemToast(getActivity(), getSelectBeforeTip(), Toast.LENGTH_SHORT);
            return;
        }

        mIntent = new Intent();
        mIntent.putExtra(EXTRA_RESULT_CODE, Activity.RESULT_OK);
        if (mBubbleItem != null) {
            remindTime = isNotify ? remindTime - getSelectBeforeTime() : remindTime;
            updateRemindBubbleData(remindTime);
        } else {
            mIntent.putExtra(EXTRA_DUE_DATE, remindTime);
            mIntent.putExtra(EXTRA_REMIND_TIME, isNotify ? remindTime - getSelectBeforeTime() : 0);
        }
        dismissAllowingStateLoss();
    }

    @Override
    protected void onTouchOutside() {
        if (SmtPCUtils.isValidExtDisplayId(getActivity())) {
            dismissAllowingStateLoss();
        }
    }

    @Override
    protected boolean hasBubbleItem() {
        return mBubbleItem != null;
    }

    @Override
    protected long getInitDueDate() {
        if (mBubbleItem != null) {
            return mBubbleItem.getDueDate();
        } else { // from sara
            if (getArguments() != null) {
                return getArguments().getLong(EXTRA_DUE_DATE, 0);
            }
        }
        return 0;
    }

    @Override
    protected long getInitRemindTime() {
        if (mBubbleItem != null) {
            return mBubbleItem.getRemindTime();
        } else {
            if (getArguments() != null) {
                return getArguments().getLong(EXTRA_REMIND_TIME, 0);
            }
        }
        return 0;
    }

    @Override
    protected boolean isSetBubbleWindowType() {
        return getArguments() != null && getArguments().getBoolean(EXTRA_SET_BUBBLE_WINDOW_TYPE, false);
    }
}
