package com.smartisanos.ideapills.sync.share;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.text.TextUtils;
import android.view.WindowManager;
import android.widget.Toast;
import android.app.Activity;

import com.smartisanos.ideapills.BubbleController;
import com.smartisanos.ideapills.ProxyActivity;
import com.smartisanos.ideapills.R;
import com.smartisanos.ideapills.common.sync.SyncUtil;
import com.smartisanos.ideapills.sync.SyncBundleRepository;
import com.smartisanos.ideapills.sync.entity.SyncShareInvitation;
import com.smartisanos.ideapills.util.GlobalBubbleUtils;

public class GlobalInvitationAction {

    private static GlobalInvitationAction sInvitationAction;
    private String mPhoneNumber = "";

    private InvitationSuccessListener mOuterInvitationSuccessListener;
    private AlertDialog mInvitationDialog;

    private SyncBundleRepository.RequestListener<SyncShareInvitation> mInviteListener = new SyncBundleRepository.RequestListener<SyncShareInvitation>() {

        public void onRequestStart() {

        }

        public void onResponse(SyncShareInvitation response) {
            if (response != null) {
                GlobalBubbleUtils.showSystemToast(R.string.sync_send_invitation_start_title, Toast.LENGTH_SHORT);
                if (mOuterInvitationSuccessListener != null) {
                    mOuterInvitationSuccessListener.onInvitationSendSuccess(response);
                }
            }
            mOuterInvitationSuccessListener = null;
        }

        public void onError(SyncBundleRepository.DataException e) {
            handleError(e.status);
            mOuterInvitationSuccessListener = null;
        }
    };

    private SyncBundleRepository.RequestListener<Integer> mRegisterInviteListener = new SyncBundleRepository.RequestListener<Integer>() {

        public void onRequestStart() {

        }

        public void onResponse(Integer response) {
            GlobalBubbleUtils.showSystemToast(R.string.sync_invitation_register_sended_tip, Toast.LENGTH_SHORT);
        }

        public void onError(SyncBundleRepository.DataException e) {
            GlobalBubbleUtils.showSystemToast(R.string.sync_invitation_register_send_fail_tip, Toast.LENGTH_SHORT);
        }
    };

    public synchronized static GlobalInvitationAction getInstance() {
        if (sInvitationAction == null) {
            synchronized (GlobalInvitationAction.class) {
                if (sInvitationAction == null) {
                    sInvitationAction = new GlobalInvitationAction();
                }
            }
        }
        return sInvitationAction;
    }

    private GlobalInvitationAction() {
    }

    public void showInvitationDialog(final InvitationSuccessListener invitationSuccessListener) {
        hideInvitationDialog();
        AlertDialog dialog = SyncUtil.buildInvitationDialog(BubbleController.getInstance().getContext(),
                new SyncUtil.OnInvitationClickListener() {
            @Override
            public void onInvitationClick(String phoneText) {
                sendInvitation(phoneText, invitationSuccessListener);
            }
        });
        mInvitationDialog = dialog;
        initInvitationDialog(null);
        mInvitationDialog.show();
    }

    public void sendInvitation(String phoneText, final InvitationSuccessListener invitationSuccessListener) {
        mPhoneNumber = phoneText;
        if (isLegitimacy(mPhoneNumber)) {
            mOuterInvitationSuccessListener = invitationSuccessListener;
            SyncShareRepository.startInvite(mPhoneNumber, mInviteListener);
            if (mInvitationDialog != null) {
                mInvitationDialog.dismiss();
            }
        }
    }

    private boolean isLegitimacy(String addr) {
        if (TextUtils.isEmpty(addr)) {
            GlobalBubbleUtils.showSystemToast(R.string.sync_phone_email_empty_tip, Toast.LENGTH_SHORT);
            return false;
        }
        if (!SyncUtil.isPhoneNumber(addr) && !SyncUtil.isEmail(addr)) {
            GlobalBubbleUtils.showSystemToast(R.string.sync_phone_email_illegal_tip, Toast.LENGTH_SHORT);
            return false;
        }
        return true;
    }

    public void showDialog(int titleId, int messageId, DialogInterface.OnClickListener listener) {
        showDialog(titleId, messageId, listener, null);
    }

    public void showDialog(int titleId, int messageId, DialogInterface.OnClickListener listener, Activity activity) {
        hideInvitationDialog();
        mInvitationDialog = (activity != null ? new AlertDialog.Builder(activity) : new AlertDialog.Builder(BubbleController.getInstance().getContext(), AlertDialog.THEME_DEVICE_DEFAULT_LIGHT))
                .setPositiveButton(R.string.confirm_reminder, listener)
                .setNegativeButton(R.string.bubble_cancel, null)
                .setCancelable(true)
                .create();
        if (titleId > 0) {
            mInvitationDialog.setTitle(titleId);
        }
        if (messageId > 0) {
            mInvitationDialog.setMessage(BubbleController.getInstance().getContext().getString(messageId));
        }
        initInvitationDialog(activity);
        mInvitationDialog.show();
    }

    public void hideInvitationDialog() {
        if (mInvitationDialog != null && mInvitationDialog.isShowing()) {
            mInvitationDialog.dismiss();
        }
    }

    private void initInvitationDialog(Activity activity) {
        if (mInvitationDialog == null) {
            return;
        }
        if (activity == null) {
            mInvitationDialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
        }
        mInvitationDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                mInvitationDialog = null;
            }
        });
    }

    public void showLoginDialog(boolean login) {
        AlertDialog loginDialog = SyncUtil.buildLoginDialog(BubbleController.getInstance().getContext(), login, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Intent intent = new Intent(BubbleController.getInstance().getContext(), ProxyActivity.class);
                intent.setAction(Intent.ACTION_MAIN);
                intent.putExtra(ProxyActivity.EXTRA_TASK, ProxyActivity.TASK_ACCOUNT_LOGIN);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                GlobalBubbleUtils.startActivityMayInExtDisplay(BubbleController.getInstance().getContext(), intent);
            }
        });
        hideInvitationDialog();
        mInvitationDialog = loginDialog;
        initInvitationDialog(null);
        mInvitationDialog.show();
    }

    public void handleError(int status, String number) {
        mPhoneNumber = number;
        handleError(status);
    }

    public void handleError(int status) {
        int resId;
        switch (status) {
            case SyncShareRepository.ERROR_ALIAS_NOT_FOUND:
                final boolean isEmail = SyncUtil.isEmail(mPhoneNumber);
                if(isEmail){
                    resId = R.string.sync_invitation_email_register;
                    break;
                }else{
                    showDialog(-1, R.string.sync_invitation_phone_register, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            SyncShareRepository.sendRegisterInvite(mPhoneNumber, mRegisterInviteListener);
                        }
                    });
                    return;
                }
            case SyncShareRepository.ERROR_UNABLE_INVITE_YOURSELF:
                resId = R.string.sync_not_share_self_tip;
                break;
            case SyncShareRepository.ERROR_SHARE_INVITE_DISABLED:
                resId = R.string.sync_not_enabled_yet_tip;
                break;
            case SyncShareRepository.ERROR_SHARE_PARTICIPANT_NOT_ALLOWED:
                resId = R.string.sync_not_enabled_yet_tip;
                break;
            case SyncShareRepository.ERROR_SHARE_INVITE_QUOTA_EXCEEDED:
                resId = R.string.sync_invitation_cancel_cause_multi_invitations;
                break;
            case SyncShareRepository.ERROR_NOT_FOUND:
            case SyncShareRepository.ERROR_SHARE_NOT_EXISTS:
                resId = R.string.sync_invitation_not_exists;
                break;
            case SyncShareRepository.ERROR_NET_WORK_ERROR:
                resId = R.string.net_error_tips;
                break;
            case SyncShareRepository.ERROR_TOO_MANY_REQUESTS:
                resId = R.string.sync_request_too_frequent_tip;
                break;
            default:
                resId = R.string.sync_request_failed;
                break;
        }
        GlobalBubbleUtils.showSystemToast(resId, Toast.LENGTH_SHORT);
    }

    public interface InvitationSuccessListener {
        void onInvitationSendSuccess(SyncShareInvitation syncShareInvitation);
    }
}
