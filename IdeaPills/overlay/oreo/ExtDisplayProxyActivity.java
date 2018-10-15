package com.smartisanos.ideapills;

import android.app.ActivityOptions;
import android.app.SmtPCUtils;
import android.app.TaskStackListener;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.RemoteException;
import android.view.WindowManager;

import com.smartisanos.ideapills.util.ChooseResultHelper;
import com.smartisanos.ideapills.common.util.TaskUtils;

public class ExtDisplayProxyActivity extends ProxyActivity {
    static final String EXT_CLASS_NAME = "com.smartisanos.ideapills.ExtDisplayProxyActivity";
    static final String PKG_ATTACH_CHOOSE = "com.smartisanos.filemanager";
    static final String CLASS_ATTACH_CHOOSE = "com.smartisan.filemanager.tablet.TabletActivity";
    private TaskStackListener mTaskStackListener;
    private ComponentName mStartComponent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mRequestBeforeVisible = false;
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unRegisterTaskListener();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        ComponentName componentName = mChooseResultHelper.getLastChooseComponent();
        if (componentName == null) {
            componentName = mStartComponent;
        }
        if (componentName != null) {
            int taskId = TaskUtils.getTaskId(componentName.getPackageName(), componentName.getClassName());
            TaskUtils.removeTask(this, taskId);
        }
        finish();
        startActivity(intent);
    }

    @Override
    protected void startAttachementChoose(Intent intent) {
        mBubbleId = intent.getIntExtra(EXTRA_BUBBLE_ID, 0);
        if (mBubbleId > 0) {
            try {
                Intent i = new Intent(Intent.ACTION_GET_CONTENT);
                i.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
                i.addCategory(Intent.CATEGORY_OPENABLE);
                i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
                i.setType("*/*");
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
                i.setComponent(new ComponentName(PKG_ATTACH_CHOOSE, CLASS_ATTACH_CHOOSE));
                Intent resultIntent = null;
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP_MR1) {
                    resultIntent = Intent.createChooser(i, getText(R.string.select_attachment_type),
                            ChooseResultHelper.getAttachPickIntentSender(this));
                } else {
                    resultIntent = Intent.createChooser(i, getText(R.string.select_attachment_type));
                }
                resultIntent.putExtra("window_type", BubbleController.BUBBLE_WINDOW_TYPE);
                startActivityForResult(resultIntent,
                        RESULT_PICK_ATTACHMENT);
                overridePendingTransition(R.anim.pop_up_in, R.anim.fake_anim);
                mRequestBeforeVisible = BubbleController.getInstance().isBubbleListVisible();
                BubbleController.getInstance().hideBubbleListWithAnim();
            } catch (ActivityNotFoundException e) {
                log.error("Can not start activity " + e);
                finishAndRemoveTask();
            }
        } else {
            finishAndRemoveTask();
        }
    }

    @Override
    protected void clearAddingAttachmentStatus() {
        super.clearAddingAttachmentStatus();
        BubbleController.getInstance().requestBubbleOptLayoutUpdateRegion();
    }

    @Override
    public void startActivityForResult(Intent intent, int requestCode, Bundle options) {
        registerTaskListener();
        if (options == null) {
            options = ActivityOptions.makeBasic().toBundle();
        }
        options.putInt("android.activity.launchDisplayId", SmtPCUtils.getExtDisplayId());
        super.startActivityForResult(intent, requestCode, options);
    }

    public void registerTaskListener() {
        try {
            if (mTaskStackListener == null) {
                mTaskStackListener = new TaskStackListener() {
                    @Override
                    public void onTaskCreated(int taskId, ComponentName componentName) throws RemoteException {
                        unRegisterTaskListener();
                        mStartComponent = componentName;
                    }

                    @Override
                    public void onTaskRemoved(int taskId) throws RemoteException {

                    }
                };
            }
            SmtPCUtils.getSmtPCManager().registerSmtTaskStackListener(mTaskStackListener);
        } catch (RemoteException e) {
        }
    }

    public void unRegisterTaskListener() {
        try {
            if (mTaskStackListener != null) {
                SmtPCUtils.getSmtPCManager().unRegisterSmtTaskStackListener(mTaskStackListener);
                mTaskStackListener = null;
            }
        } catch (RemoteException e) {
        }
    }
}
