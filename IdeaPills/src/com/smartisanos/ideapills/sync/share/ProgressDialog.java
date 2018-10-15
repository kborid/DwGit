package com.smartisanos.ideapills.sync.share;

import android.app.Activity;
import android.content.DialogInterface;
import android.os.Handler;
import android.os.Message;

import smartisanos.app.SmartisanProgressDialog;

import com.smartisanos.ideapills.R;

import java.lang.ref.WeakReference;

public class ProgressDialog {
    private final Activity mActivity;
    private SmartisanProgressDialog mProgress = null;
    private DialogCancelListener mDialogCancelListener;
    private static final int MSG_SHOW_PROGRESS_DIALOG = 0;
    private UiHandler mUiHandler;

    private static class UiHandler extends Handler {
        private final WeakReference<ProgressDialog> mDialog;

        public UiHandler(ProgressDialog dialog) {
            mDialog = new WeakReference<ProgressDialog>(dialog);
        }

        @Override
        public void handleMessage(Message msg) {
            ProgressDialog dialog = mDialog.get();
            if (dialog != null) {
                switch (msg.what) {
                    case MSG_SHOW_PROGRESS_DIALOG:
                        dialog.showProgressDialog();
                        break;
                    default:
                        break;
                }
            }
        }
    }

    public ProgressDialog(final Activity activity) {
        if (activity == null) throw new IllegalArgumentException("activity must not be null");
        mActivity = activity;
        mUiHandler = new UiHandler(ProgressDialog.this);
    }

    public boolean isShowing() {
        return mProgress != null ? mProgress.isShowing() : false;
    }

    public void showProgressDelay() {
        mUiHandler.sendEmptyMessageDelayed(MSG_SHOW_PROGRESS_DIALOG, 300);
    }

    private void showProgressDialog() {
        if (mProgress == null) {
            mProgress = new SmartisanProgressDialog(mActivity);
            mProgress.setOwnerActivity(mActivity);
            mProgress.setMessage(mActivity.getString(R.string.sync_content_wait_message));
            mProgress.setCancelable(true);
            mProgress.setOnCancelListener(new DialogInterface.OnCancelListener() {
                @Override
                public void onCancel(DialogInterface dialogInterface) {
                    if (mDialogCancelListener != null) {
                        mDialogCancelListener.onDialogCancel();
                    }
                }
            });
        }
        if (isCurrentActivityAlive(mProgress) && !mProgress.isShowing()) {
            mProgress.show();
        }
    }

    private boolean isCurrentActivityAlive(SmartisanProgressDialog progressDialog) {
        Activity activity = progressDialog.getOwnerActivity();
        if (activity == null || activity.isDestroyed() || activity.isFinishing()) {
            return false;
        }
        return true;
    }

    public void hideProgressDialog() {
        mUiHandler.removeMessages(MSG_SHOW_PROGRESS_DIALOG);
        if (mProgress != null && mProgress.isShowing()) {
            mProgress.dismiss();
        }
    }

    public void setDialogCancelListener(DialogCancelListener l) {
        mDialogCancelListener = l;
    }

    public interface DialogCancelListener {
        void onDialogCancel();
    }

    public void onDestroy() {
        if (mProgress != null && mProgress.isShowing()) {
            mProgress.dismiss();
        }
        mUiHandler.removeCallbacksAndMessages(null);
    }
}
