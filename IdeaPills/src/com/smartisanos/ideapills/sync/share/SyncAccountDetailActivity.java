package com.smartisanos.ideapills.sync.share;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.support.v4.content.LocalBroadcastManager;

import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.assist.ImageScaleType;
import com.smartisanos.ideapills.BaseActivity;
import com.smartisanos.ideapills.Constants;
import com.smartisanos.ideapills.sync.SyncBundleRepository;
import com.smartisanos.ideapills.sync.SyncBundleRepository.RequestListener;
import com.smartisanos.ideapills.sync.SyncManager;
import com.smartisanos.ideapills.sync.entity.SyncShareInvitation;
import com.smartisanos.ideapills.util.GlobalBubbleUtils;
import com.smartisanos.ideapills.util.LOG;
import com.smartisanos.ideapills.util.Utils;
import com.smartisanos.ideapills.R;

import smartisanos.widget.ShadowButton;
import smartisanos.widget.Title;

public class SyncAccountDetailActivity extends BaseActivity implements View.OnClickListener {
    LOG log = LOG.getInstance(SyncAccountDetailActivity.class);
    public static final String EXTRA_INVITEE = "invitee";
    public static final String EXTRA_FROM_SETTING = "from_setting";
    private TextView mStatusView;
    private ImageView mInviteIcon;
    private TextView mInviteName;
    private EditText mRemarkText;
    private Button mAcceptButton;
    private Button mDeleteButton;
    private ShadowButton mOkButton;
    private SyncShareInvitation mSyncShareInvitation;
    private boolean mFromSetting;
    private BroadcastReceiver mTimeTickReceiver;
    protected BroadcastReceiver inviteChangeReceiver = null;
    private long mUserId;
    private boolean mInvitationChanged;
    private ProgressDialog mProgressDialog;
    private int mHandleStatus;
    private String mLastRemark;
    private DisplayImageOptions mDisplayOption;
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getInvitationInfo(getIntent());
        setContentView(R.layout.sync_activity_account_detail);
        mUserId = SyncManager.getCloudAccountUserId(this);
        mStatusView = (TextView) findViewById(R.id.invite_status);
        mInviteIcon = (ImageView) findViewById(R.id.invite_icon);
        mInviteName = (TextView) findViewById(R.id.invite_name);
        mAcceptButton = (Button) findViewById(R.id.btn_accept);
        mDeleteButton = (Button) findViewById(R.id.btn_delete);
        mRemarkText = (EditText) findViewById(R.id.remarks);
        initTitleBar();
        updateInvitationStatus();
        mAcceptButton.setOnClickListener(this);
        mDeleteButton.setOnClickListener(this);
        mProgressDialog = new ProgressDialog(this);
        mRemarkText.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            public void afterTextChanged(Editable editable) {
                final String remark = editable.toString();
                boolean enable = false;
                if (TextUtils.isEmpty(mLastRemark)) {
                    enable = TextUtils.isEmpty(remark) ? false : true;
                } else {
                    enable = mLastRemark.equals(remark) ? false : true;
                }
                mOkButton.setEnabled(enable);
            }
        });
        registerInviteChangeReceiver();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        getInvitationInfo(intent);
        updateInvitationStatus();
    }

    private void getInvitationInfo(Intent intent) {
        if (intent == null) {
            finish();
        }
        try {
            ContentValues cv = intent.getParcelableExtra(EXTRA_INVITEE);
            SyncShareInvitation invitation = new SyncShareInvitation();
            invitation.fromContentValues(cv);
            mFromSetting = intent.getBooleanExtra(EXTRA_FROM_SETTING, false);
            if (!mFromSetting) {
                mSyncShareInvitation = SyncShareManager.INSTANCE.getInvitation(invitation.inviter.id);
            } else {
                mSyncShareInvitation = invitation;
            }
        } catch (Exception e) {

        }
        if (mSyncShareInvitation == null) {
            onBackPressed();
        }
    }

    private void updateInvitationStatus(final int status){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mProgressDialog.hideProgressDialog();
                mInvitationChanged = true;
                mSyncShareInvitation.inviteStatus = status;
                updateInvitationStatus();
            }
        });
    }

    private void updateInvitationStatus(final SyncShareInvitation invitation){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mProgressDialog.hideProgressDialog();
                if(invitation != null) {
                    mInvitationChanged = true;
                    mSyncShareInvitation = invitation;
                    SyncShareManager.INSTANCE.addInvitation(invitation);
                    updateInvitationStatus();
                }
            }
        });
    }

    private void updateInvitationStatus() {
        if (mSyncShareInvitation == null) {
            return;
        }
        mHandleStatus = mSyncShareInvitation.inviteStatus;
        mInviteName.setText(SyncShareManager.INSTANCE.getInvitationShowName(mUserId, mSyncShareInvitation));
        mLastRemark = mSyncShareInvitation.inviter.id == mUserId ? mSyncShareInvitation.inviter.remark
                : mSyncShareInvitation.invitee.remark;
        mRemarkText.setText(mLastRemark);
        if (mDisplayOption == null) {
            mDisplayOption = new DisplayImageOptions.Builder()
                    .cacheInMemory(true).cacheOnDisk(false)
                    .imageScaleType(ImageScaleType.EXACTLY)
                    .showImageOnLoading(R.drawable.sync_default_avatar)
                    .showImageForEmptyUri(R.drawable.sync_default_avatar)
                    .build();
        }
        ImageLoader.getInstance().displayImage(mSyncShareInvitation.inviter.id == mUserId ?
                mSyncShareInvitation.invitee.avatar : mSyncShareInvitation.inviter.avatar, mInviteIcon, mDisplayOption);
        updateButtonState();
    }

    public void initTitleBar() {
        Title title = (Title) findViewById(R.id.title_bar);
        title.setTitle(R.string.sync_account_detail_title);
        title.setBackButtonListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });

        title.setBackButtonText(smartisanos.R.string.title_button_text_back);
        mOkButton = (ShadowButton)title.getOkButton();
        title.setOkButtonText(R.string.save_label);
        title.setOkButtonListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setRemark();
            }
        });
        title.setOkButtonShadowColorsEnable(false);
        mOkButton.setEnabled(false);
        mOkButton.setVisibility(View.VISIBLE);

        Intent fromIntent = getIntent();
        if (!fromIntent.hasExtra(Title.EXTRA_BACK_BTN_TEXT) && !fromIntent.hasExtra("from_search")) {
            title.getBackButton().setVisibility(View.VISIBLE);
        } else {
            title.getBackButton().setVisibility(View.GONE);
        }
        title.setShadowVisible(true);
        setTitleByIntent(title);
    }

    @Override
    public void onBackPressed() {
        if (mProgressDialog != null && mProgressDialog.isShowing()) {
            mProgressDialog.hideProgressDialog();
            return;
        }
        if (mFromSetting) {
            if (mInvitationChanged) {
                setResult(RESULT_OK);
            }
            finish();
        } else {
            Intent intent = new Intent(this, ShareMainActivity.class);
            startActivity(intent);
            finish();
        }
    }

    @Override
    public void onDestroy() {
        unregisterInviteChangeReceiver();
        if (mProgressDialog != null) {
            mProgressDialog.onDestroy();
        }
        unregisterTimeReceiver();
        handleListener.cancel(true);
        remarkListener.cancel(true);
        removeListener.cancel(true);
        super.onDestroy();
    }

    private void registerTimeReceiver() {
        if (mTimeTickReceiver == null) {
            mTimeTickReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    if (intent.getAction().equals(Intent.ACTION_TIME_TICK)) {
                        updateInvitationButton();
                    }
                }
            };
        }
        IntentFilter dateFilter = new IntentFilter();
        dateFilter.addAction(Intent.ACTION_TIME_TICK);
        registerReceiver(mTimeTickReceiver, dateFilter);
    }

    private void unregisterTimeReceiver() {
        if (mTimeTickReceiver != null) {
            unregisterReceiver(mTimeTickReceiver);
            mTimeTickReceiver = null;
        }
    }

    protected void registerInviteChangeReceiver() {
        inviteChangeReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (SyncShareManager.ACTION_INVITE_STATUS_CHANGE.equals(intent.getAction())) {
                    mInvitationChanged = true;
                    if (mSyncShareInvitation == null) {
                        onBackPressed();
                        return;
                    }
                    mSyncShareInvitation = SyncShareManager.INSTANCE.getInvitation(mSyncShareInvitation.inviter.id);
                    if (mSyncShareInvitation != null) {
                        updateInvitationStatus();
                    } else {
                        onBackPressed();
                    }
                }
            }
        };
        LocalBroadcastManager.getInstance(this).registerReceiver(inviteChangeReceiver, new IntentFilter(SyncShareManager.ACTION_INVITE_STATUS_CHANGE));
    }

    private void unregisterInviteChangeReceiver() {
        if (inviteChangeReceiver != null) {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(inviteChangeReceiver);
            inviteChangeReceiver = null;
        }
    }

    private void updateInvitationButton() {
        switch (mSyncShareInvitation.inviteStatus) {
            case SyncShareInvitation.INVITE_START:
                long elapsed = System.currentTimeMillis() - mSyncShareInvitation.updatedAt;
                if (elapsed < Constants.ONE_HOUR_SECOND) {
                    mAcceptButton.setEnabled(false);
                    mAcceptButton.setAlpha(Constants.DISABLE_ALPHA);
                    final int minite = (int) Math.ceil((float) (Constants.ONE_HOUR_SECOND - elapsed) / 60 / 1000);
                    String text = "";
                    if (minite < Constants.MINITE_OF_HOUR) {
                        text = String.format(getResources().getQuantityString(
                                R.plurals.sync_send_invitation_minite_after, minite),
                                minite);
                    } else {
                        final int hour = minite / Constants.MINITE_OF_HOUR;
                        text = String.format(getResources().getQuantityString(
                                R.plurals.sync_send_invitation_hour_after, hour),
                                hour);
                    }
                    mAcceptButton.setText(text);
                } else {
                    mAcceptButton.setEnabled(true);
                    mAcceptButton.setAlpha(Constants.ENABLE_ALPHA);
                    mAcceptButton.setText(R.string.sync_send_invitation_again);
                    unregisterTimeReceiver();
                }
                break;
            case SyncShareInvitation.INVITE_DECLINE:
            case SyncShareInvitation.INVITE_CANCEL:
                mAcceptButton.setEnabled(true);
                mAcceptButton.setAlpha(1f);
                mAcceptButton.setText(mSyncShareInvitation.inviter.id == mUserId ? R.string.sync_send_invitation_again : R.string.sync_send_invitation);
                break;
        }
    }

    private void updateButtonState() {
        switch (mSyncShareInvitation.inviteStatus) {
            case SyncShareInvitation.INVITE_START:
                mAcceptButton.setVisibility(View.VISIBLE);
                mStatusView.setVisibility(View.VISIBLE);
                mRemarkText.setVisibility(View.GONE);
                mOkButton.setVisibility(View.GONE);
                if (mSyncShareInvitation.inviter.id == mUserId) {
                    mStatusView.setText(R.string.sync_send_invitation_start_title);
                    mDeleteButton.setText(R.string.sync_send_invitation_cancel);
                    updateInvitationButton();
                    registerTimeReceiver();
                } else {
                    mStatusView.setText(R.string.sync_receive_invitation_start_title);
                    mAcceptButton.setEnabled(true);
                    mAcceptButton.setAlpha(1f);
                    unregisterTimeReceiver();
                    mAcceptButton.setText(R.string.sync_receive_invitation_accept);
                    mDeleteButton.setText(R.string.sync_receive_invitation_refuse);
                }
                break;
            case SyncShareInvitation.INVITE_ACCEPT:
                mStatusView.setVisibility(View.GONE);
                mAcceptButton.setVisibility(View.GONE);
                mRemarkText.setVisibility(View.VISIBLE);
                mOkButton.setVisibility(View.VISIBLE);
                mDeleteButton.setText(R.string.sync_share_cancel);
                break;
            case SyncShareInvitation.INVITE_DECLINE:
                mAcceptButton.setVisibility(View.VISIBLE);
                updateInvitationButton();
                mStatusView.setVisibility(View.VISIBLE);
                if (mSyncShareInvitation.inviter.id == mUserId) {
                    mStatusView.setText(R.string.sync_share_status_other_refuse);
                } else {
                    mStatusView.setText(R.string.sync_share_status_refuse);
                }
                mRemarkText.setVisibility(View.GONE);
                mOkButton.setVisibility(View.GONE);
                mDeleteButton.setText(R.string.sync_delete_account);
                break;
            case SyncShareInvitation.INVITE_CANCEL:
                mAcceptButton.setVisibility(View.VISIBLE);
                updateInvitationButton();
                mStatusView.setVisibility(View.VISIBLE);
                mStatusView.setText(R.string.sync_share_status_canceled);
                mRemarkText.setVisibility(View.GONE);
                mOkButton.setVisibility(View.GONE);
                mDeleteButton.setText(R.string.sync_delete_account);
                break;
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_accept:
                onAcceptClick();
                break;
            case R.id.btn_delete:
                onCancelClick();
                break;
        }
    }

    private void setRemark() {
        mProgressDialog.showProgressDelay();
        final String remakeText = mRemarkText.getText().toString().trim();
        Utils.hideInputMethod(mRemarkText);
        mInvitationChanged = true;
        if (mSyncShareInvitation.inviter.id == mUserId) {
            SyncShareRepository.setRemark(mSyncShareInvitation.inviter.id, mSyncShareInvitation.invitee.id,
                    remakeText, mSyncShareInvitation.invitee.remark, remarkListener);
        } else if (mSyncShareInvitation.invitee.id == mUserId) {
            SyncShareRepository.setRemark(mSyncShareInvitation.inviter.id, mSyncShareInvitation.invitee.id,
                    mSyncShareInvitation.inviter.remark, remakeText, remarkListener);
        }
    }

    private void onInviteRemoved() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mProgressDialog.hideProgressDialog();
                GlobalBubbleUtils.showSystemToast(SyncAccountDetailActivity.this,
                        mSyncShareInvitation.inviteStatus == SyncShareInvitation.INVITE_START ? R.string.sync_share_invitation_canceled
                                : R.string.sync_share_status_canceled, Toast.LENGTH_SHORT);
                setResult(RESULT_OK);
                finish();
            }
        });
    }

    private void onRemarkSuccess(final SyncShareInvitation invitation) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mInvitationChanged = true;
                SyncShareManager.INSTANCE.addInvitation(invitation);
                onBackPressed();
            }
        });
    }

    private void handleError(final SyncBundleRepository.DataException e) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mProgressDialog.hideProgressDialog();
                GlobalInvitationAction.getInstance().handleError(e.status);
            }
        });
    }

    private void onAcceptClick() {
        switch (mSyncShareInvitation.inviteStatus) {
            case SyncShareInvitation.INVITE_START:
                if (mSyncShareInvitation.inviter.id == mUserId) {
                    SyncShareRepository.startInvite(mSyncShareInvitation.invitee.id, invitelistener);
                } else {
                    mHandleStatus = SyncShareInvitation.INVITE_ACCEPT;
                    SyncShareRepository.handleInvite(mSyncShareInvitation.inviter.id, SyncShareInvitation.INVITE_ACCEPT, handleListener);
                }
                break;
            case SyncShareInvitation.INVITE_DECLINE:
                SyncShareRepository.startInvite(mSyncShareInvitation.inviter.id == mUserId ?
                        mSyncShareInvitation.invitee.id : mSyncShareInvitation.inviter.id, invitelistener);
                break;
            case SyncShareInvitation.INVITE_CANCEL:
                SyncShareRepository.startInvite(mSyncShareInvitation.inviter.id == mUserId ?
                        mSyncShareInvitation.invitee.id : mSyncShareInvitation.inviter.id, invitelistener);
                break;
        }
        mProgressDialog.showProgressDelay();
    }

    private void onCancelClick() {
        switch (mSyncShareInvitation.inviteStatus) {
            case SyncShareInvitation.INVITE_START:
                if (mSyncShareInvitation.inviter.id == mUserId) {
                    GlobalInvitationAction.getInstance().showDialog(R.string.dlg_title_notice, R.string.sync_invite_cancel_tip,
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    SyncShareRepository.removeInviterWithRetry(mSyncShareInvitation.inviter.id, mSyncShareInvitation.invitee.id, removeListener);
                                    mProgressDialog.showProgressDelay();
                                }
                            }, SyncAccountDetailActivity.this);
                    return;
                } else {
                    mHandleStatus = SyncShareInvitation.INVITE_DECLINE;
                    SyncShareRepository.handleInvite(mSyncShareInvitation.inviter.id, SyncShareInvitation.INVITE_DECLINE, handleListener);
                }
                break;
            case SyncShareInvitation.INVITE_ACCEPT:
                GlobalInvitationAction.getInstance().showDialog(R.string.dlg_title_notice, R.string.sync_share_cancel_tip,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                SyncShareRepository.removeInviterWithRetry(mSyncShareInvitation.inviter.id, mSyncShareInvitation.invitee.id, removeListener);
                                mProgressDialog.showProgressDelay();
                            }
                        }, SyncAccountDetailActivity.this);
                return;
            case SyncShareInvitation.INVITE_DECLINE:
                SyncShareRepository.removeInviterWithRetry(mSyncShareInvitation.inviter.id, mSyncShareInvitation.invitee.id, removeListener);
                break;
            case SyncShareInvitation.INVITE_CANCEL:
                SyncShareRepository.removeInviterWithRetry(mSyncShareInvitation.inviter.id, mSyncShareInvitation.invitee.id, removeListener);
                break;
        }
        mProgressDialog.showProgressDelay();
    }

    private RequestListener remarkListener = new RequestListener<SyncShareInvitation>() {
        public void onRequestStart() {

        }

        public void onResponse(SyncShareInvitation response) {
            onRemarkSuccess(response);
        }

        public void onError(SyncBundleRepository.DataException e) {
            mProgressDialog.hideProgressDialog();
            GlobalBubbleUtils.showSystemToast(SyncAccountDetailActivity.this, R.string.sync_account_set_remark_failed, Toast.LENGTH_SHORT);
        }
    };

    private RequestListener invitelistener = new RequestListener<SyncShareInvitation>() {
        public void onRequestStart() {

        }

        public void onResponse(SyncShareInvitation response) {
            updateInvitationStatus(response);
            GlobalBubbleUtils.showSystemToast(SyncAccountDetailActivity.this, R.string.sync_send_invitation_start_title, Toast.LENGTH_SHORT);
        }

        public void onError(SyncBundleRepository.DataException e) {
            handleError(e);
        }
    };

    private RequestListener removeListener = new RequestListener<Integer>() {
        public void onRequestStart() {

        }

        public void onResponse(Integer response) {
            onInviteRemoved();
        }

        public void onError(SyncBundleRepository.DataException e) {
            handleError(e);
        }
    };

    private RequestListener handleListener = new RequestListener<Integer>() {
        public void onRequestStart() {

        }

        public void onResponse(Integer response) {
            final int status = mHandleStatus;
            updateInvitationStatus(status);
        }

        public void onError(SyncBundleRepository.DataException e) {
            handleError(e);
        }
    };
}
