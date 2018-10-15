package com.smartisanos.ideapills.sync.share;

import android.os.Bundle;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.Toast;
import android.content.Intent;

import com.smartisanos.ideapills.BaseActivity;
import com.smartisanos.ideapills.sync.SyncBundleRepository;
import com.smartisanos.ideapills.util.GlobalBubbleUtils;
import com.smartisanos.ideapills.util.LOG;
import com.smartisanos.ideapills.R;

import smartisanos.widget.ListContentItemSwitch;
import smartisanos.widget.Title;

public class ShareSettingActivity extends BaseActivity {
    LOG log = LOG.getInstance(ShareSettingActivity.class);
    private ListContentItemSwitch mEnableShare;
    private ProgressDialog mProgressDialog;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.sync_activity_share_setting);
        mEnableShare = (ListContentItemSwitch) findViewById(R.id.enable_share);
        mEnableShare.setTitle(R.string.sync_share_accept_setting);
        mEnableShare.setOnCheckedChangeListener(mCheckedChangeListerner);
        initTitleBar();
        mProgressDialog = new ProgressDialog(this);
        getInvitationSwitch();
    }

    public void initTitleBar() {
        Title title = (Title) findViewById(R.id.title_bar);
        title.setTitle(R.string.sync_share_setting_title);
        title.setBackButtonListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });

        title.setBackButtonText(smartisanos.R.string.title_button_text_back);

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
    public void onDestroy() {
        mProgressDialog.onDestroy();
        getListener.cancel(true);
        setListener.cancel(true);
        super.onDestroy();
    }

    private void getInvitationSwitch(){
        SyncShareRepository.getInviterSwitch(getListener);
        mProgressDialog.showProgressDelay();
    }

    private void setInvitationSwitch(final boolean isChecked){
        SyncShareRepository.setInviterSwitch(isChecked, setListener);
        mProgressDialog.showProgressDelay();
    }

    private void handleResponse(final boolean enable) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mProgressDialog.hideProgressDialog();
                mEnableShare.setChecked(enable);
            }
        });
    }

    private void handleError() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mProgressDialog.hideProgressDialog();
                mEnableShare.setChecked(SyncShareManager.INSTANCE.isInviteSwitchOn());
                GlobalBubbleUtils.showSystemToast(ShareSettingActivity.this, R.string.sync_share_setting_failed_tip, Toast.LENGTH_SHORT);
            }
        });
    }

    private CompoundButton.OnCheckedChangeListener mCheckedChangeListerner = new CompoundButton.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            setInvitationSwitch(isChecked);
        }
    };
    private SyncBundleRepository.RequestListener setListener = new SyncBundleRepository.RequestListener<Integer>() {
        public void onRequestStart() {

        }

        public void onResponse(Integer value) {
            handleResponse(SyncShareManager.INSTANCE.isInviteSwitchOn());
        }

        public void onError(SyncBundleRepository.DataException e) {
            handleError();
        }
    };

    private SyncBundleRepository.RequestListener getListener = new SyncBundleRepository.RequestListener<Boolean>() {
        public void onRequestStart() {

        }

        public void onResponse(Boolean enable) {
            handleResponse(enable);
        }

        public void onError(SyncBundleRepository.DataException e) {
            handleError();
        }
    };
}
