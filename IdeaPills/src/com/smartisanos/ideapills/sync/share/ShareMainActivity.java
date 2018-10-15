package com.smartisanos.ideapills.sync.share;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.Toast;

import com.nostra13.universalimageloader.core.ImageLoader;
import com.smartisanos.ideapills.BaseActivity;
import com.smartisanos.ideapills.Constants;
import com.smartisanos.ideapills.IdeaPillsApp;
import com.smartisanos.ideapills.common.sync.SyncUtil;
import com.smartisanos.ideapills.sync.SyncBundleRepository;
import com.smartisanos.ideapills.sync.SyncManager;
import com.smartisanos.ideapills.sync.entity.SyncShareInvitation;
import com.smartisanos.ideapills.util.GlobalBubbleUtils;
import com.smartisanos.ideapills.util.LOG;
import com.smartisanos.ideapills.R;
import com.smartisanos.ideapills.util.Utils;

import java.util.List;

import smartisanos.widget.SmartisanButton;
import smartisanos.widget.SmartisanComboTitleBar;
import smartisanos.widget.ShadowButton;

public class ShareMainActivity extends BaseActivity implements View.OnClickListener, OnItemClickListener {
    LOG log = LOG.getInstance(ShareMainActivity.class);
    public static final int REQUEST_INVITATION = 1001;
    public static final int REQUEST_ACCOUNT_DETAIL = 1002;
    public static final int REQUEST_LOG_IN = 1003;
    private ListView mAccountList;
    private InvitationAdapter mInvitationAdapter;
    private List<SyncShareInvitation> mLastInvitationList;
    private View mListContent;
    private View mRetryContent;
    private ShadowButton mShareButton;
    private long mUserId = -1;
    private boolean mSyncEnabled;
    private ProgressDialog mProgressDialog;
    protected BroadcastReceiver inviteChangeReceiver = null;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.sync_activity_share_main);
        mUserId = SyncManager.getCloudAccountUserId(this);
        mAccountList = (ListView) findViewById(R.id.account_list);
        mShareButton = (ShadowButton) findViewById(R.id.add_share);
        mShareButton.setOnClickListener(this);
        if(mUserId == -1){
            mShareButton.setText(R.string.sync_login_cloud_account);
        }else{
            mShareButton.setAlpha(Constants.DISABLE_ALPHA);
        }
        findViewById(R.id.refresh_btn).setOnClickListener(this);
        mListContent = findViewById(R.id.list_content);
        mRetryContent = findViewById(R.id.retry_content);
        View emptyContent = findViewById(R.id.empty_content);
        mAccountList.setEmptyView(emptyContent);
        mInvitationAdapter = new InvitationAdapter(this, mUserId);
        mAccountList.setAdapter(mInvitationAdapter);
        mProgressDialog = new ProgressDialog(this);
        initTitleBar();
        mLastInvitationList = SyncShareManager.INSTANCE.getCachedInvitationList();
        updateInviteList();
        registerInviteChangeReceiver();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        mUserId = SyncManager.getCloudAccountUserId(this);
        if (!Utils.isNetworkConnected(this)) {
            mRetryContent.setVisibility(View.VISIBLE);
            mListContent.setVisibility(View.GONE);
        }else {
            mRetryContent.setVisibility(View.GONE);
            mListContent.setVisibility(View.VISIBLE);
            if (mUserId == -1) {
                mShareButton.setText(R.string.sync_login_cloud_account);
                mInvitationAdapter.setInvitationList(null);
                mInvitationAdapter.notifyDataSetChanged();
                updateButtonState();
            } else if (SyncShareManager.INSTANCE.hasInvitationListCached()) {
                mInvitationAdapter.setInvitationList(SyncShareManager.INSTANCE.getCachedInvitationList());
                mInvitationAdapter.notifyDataSetChanged();
                updateButtonState();
            } else {
                updateInviteList();
            }
        }
    }

    private void updateInviteList() {
        if (mUserId != -1) {
            mSyncEnabled = SyncManager.syncEnable(this);
            if (mSyncEnabled) {
                SyncShareRepository.getInviteList(listener);
                mProgressDialog.showProgressDelay();
            }
        }
    }

    @Override
    public void onDestroy() {
        unregisterInviteChangeReceiver();
        mProgressDialog.onDestroy();
        listener.cancel(true);
        ImageLoader.getInstance().clearMemoryCache();
        super.onDestroy();
    }

    protected void registerInviteChangeReceiver() {
        inviteChangeReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (SyncShareManager.ACTION_INVITE_STATUS_CHANGE.equals(intent.getAction())) {
                    mInvitationAdapter.setInvitationList(SyncShareManager.INSTANCE.getCachedInvitationList());
                    mInvitationAdapter.notifyDataSetChanged();
                    updateButtonState();
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

    private void handleError() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mProgressDialog.hideProgressDialog();
                mRetryContent.setVisibility(View.VISIBLE);
                mListContent.setVisibility(View.GONE);
            }
        });
    }

    private void onGetInviteListSuccess(final List<SyncShareInvitation> invitations) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (mLastInvitationList != null) {
                    long userId = SyncManager.getCloudAccountUserId(IdeaPillsApp.getInstance());
                    if (SyncShareUtils.isWaitingParticipantRemoved(userId, mLastInvitationList, invitations)) {
                        GlobalBubbleUtils.showSystemToast(ShareMainActivity.this,
                                R.string.sync_invitation_cancel_cause_multi_invitations, Toast.LENGTH_SHORT);
                    }
                    mLastInvitationList = null;
                }
                mInvitationAdapter.setInvitationList(invitations);
                mInvitationAdapter.notifyDataSetChanged();
                updateButtonState();
            }
        });
    }

    private void updateButtonState() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mProgressDialog.hideProgressDialog();
                mRetryContent.setVisibility(View.GONE);
                mListContent.setVisibility(View.VISIBLE);
                mShareButton.setEnabled(true);
                mShareButton.setAlpha(isAllowAdd() ? Constants.ENABLE_ALPHA : Constants.DISABLE_ALPHA);
                mAccountList.setOnItemClickListener(ShareMainActivity.this);
            }
        });
    }

    private boolean isAllowAdd() {
        return SyncShareUtils.canAddAnotherInvitation(mUserId, mInvitationAdapter.getData());
    }

    public void initTitleBar() {
        SmartisanComboTitleBar title = (SmartisanComboTitleBar) findViewById(R.id.title_bar);
        title.setCenterContentText(getResources().getString(R.string.sync_share_main_title));
        title.setLeftViewClickListener(new SmartisanComboTitleBar.LeftBtnClickListener() {
            @Override
            public void onLeftViewClickListener(View view) {
                onBackPressed();
            }
        });
        title.setRightViewClickListener(new SmartisanComboTitleBar.RightBtnClickListener() {
            @Override
            public void onRightViewClickListener(View view, int position) {
                if(mUserId < 0){
                    showSetupDialog(true);
                }else {
                    Intent intent = new Intent(ShareMainActivity.this, ShareSettingActivity.class);
                    startActivity(intent);
                }
            }
        });

        SmartisanButton leftButton = (SmartisanButton) title.getLeftButton();
        leftButton.setButtonText(getResources().getString(R.string.app_name));
        leftButton.setContentDescription(getResources().getString(R.string.app_name));
        leftButton.setVisibility(View.VISIBLE);
        leftButton.setButtonStyle(1);

        SmartisanButton rightButton = (SmartisanButton) title.getRightButton();
        rightButton.setContentDescription(getResources().getString(R.string.share_main_button_setting));
        rightButton.setVisibility(View.VISIBLE);
        rightButton.setButtonStyle(6);
        rightButton.setButtonSourceBitmap(R.drawable.setting_button);

    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.add_share:
                if(mUserId < 0){
                    gotoAccountLogin();
                }else {
                    if (!mSyncEnabled) {
                        showSetupDialog(false);
                    } else if (!isAllowAdd()) {
                        String tip = getString(R.string.sync_not_support_multiple_account_tip);
                        GlobalBubbleUtils.showSystemToast(this, tip, Toast.LENGTH_SHORT);
                    } else {
                        Intent intent = new Intent(this, SyncShareAddActivity.class);
                        startActivityForResult(intent, REQUEST_INVITATION);
                    }
                }
                break;
            case R.id.refresh_btn:
                updateInviteList();
                break;
        }
    }

    private void showSetupDialog(boolean login){
        final AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(R.string.dlg_title_notice)
                .setMessage(login ? R.string.sync_no_login_dialog_content : R.string.sync_unenable_dialog_content)
                .setPositiveButton(R.string.confirm_reminder, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        gotoAccountLogin();
                    }
                })
                .setNegativeButton(R.string.bubble_cancel, null)
                .setCancelable(true)
                .create();
        dialog.show();
    }

    private void gotoAccountLogin(){
        Intent intent = SyncUtil.getCloudAccountLoginIntent(getString(R.string.app_name));
        startActivityForResult(intent, REQUEST_LOG_IN);
        overridePendingTransition(smartisanos.R.anim.slide_in_from_right, smartisanos.R.anim.fake_anim);
    }
    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int index, long l) {
        SyncShareInvitation item = mInvitationAdapter.getItem(index);
        Intent intent = new Intent(this, SyncAccountDetailActivity.class);
        Bundle extras = new Bundle();
        extras.putBoolean(SyncAccountDetailActivity.EXTRA_FROM_SETTING, true);
        extras.putParcelable(SyncAccountDetailActivity.EXTRA_INVITEE, item.toContentValues());
        intent.putExtras(extras);
        startActivityForResult(intent, REQUEST_ACCOUNT_DETAIL);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case REQUEST_INVITATION:
                if (resultCode == RESULT_OK) {
                    updateInviteList();
                }
                break;
            case REQUEST_ACCOUNT_DETAIL:
                if (resultCode == RESULT_OK) {
                    updateInviteList();
                }
                break;
            case REQUEST_LOG_IN:
                mUserId = SyncManager.getCloudAccountUserId(this);
                mInvitationAdapter.setUserId(mUserId);
                updateInviteList();
                if(mUserId == -1){
                    mShareButton.setText(R.string.sync_login_cloud_account);
                }else{
                    mShareButton.setText(R.string.sync_add_share_account);
                    mShareButton.setAlpha(mSyncEnabled && isAllowAdd() ? Constants.ENABLE_ALPHA : Constants.DISABLE_ALPHA);
                }
                break;
        }
    }

    private SyncBundleRepository.RequestListener listener = new SyncBundleRepository.RequestListener<List<SyncShareInvitation>>() {
        public void onRequestStart() {

        }

        public void onResponse(List<SyncShareInvitation> invitations) {
            onGetInviteListSuccess(invitations);
        }

        public void onError(SyncBundleRepository.DataException e) {
            handleError();
        }
    };
}
