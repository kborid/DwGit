package com.smartisanos.sara.setting;


import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.service.onestep.GlobalBubble;
import android.view.View;
import android.widget.CompoundButton;

import smartisanos.widget.Title;
import smartisanos.util.LogTag;

import com.smartisanos.ideapills.common.util.CommonConstant;
import com.smartisanos.sara.BaseActivity;
import com.smartisanos.sara.R;
import com.smartisanos.sara.bubble.manager.BubbleThemeManager;
import com.smartisanos.sara.util.MultiSdkAdapter;
import com.smartisanos.sara.util.SaraConstant;
import com.smartisanos.sara.util.SaraUtils;
import com.smartisanos.sara.widget.ShadowBitmapView;
import com.smartisanos.sara.setting.DrawerSettingAdapter.onShareIconClickListener;
import com.smartisanos.sara.widget.ShareDrawerLayout;
import com.smartisanos.ideapills.common.model.ShareItem;
import com.smartisanos.ideapills.common.util.PackageUtils;
import android.provider.Settings;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.List;
import smartisanos.widget.ListContentItemSwitch;

public class DrawerSettingActivity extends BaseActivity implements CompoundButton.OnCheckedChangeListener, ShareDrawerLayout.OnSortChangeListener {
    public static final String TAG = "VoiceAss.DrawerSettingActivity";

    private ListContentItemSwitch mAppDrawerSwitch;
    private RelativeLayout mShareLayout;
    private ShareDrawerLayout mShareDrawerLayout;
    private ListView mShareList;
    private TextView mBubbleText;
    private boolean isDrawerEnable;
    private ImageView mImageTop;
    private View mBottomView;

    private boolean isBindShareView = false;
    private boolean isDestroy = false;
    private Animator mDisappearAnim;
    private DrawerSettingAdapter mAdapter;
    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (SaraConstant.ACTION_FINISH_SETTINGS_ACTIVITY.equals(action)) {
                finish();
            } else if (Intent.ACTION_CLOSE_SYSTEM_DIALOGS.equals(action)) {
                String reason = intent.getStringExtra("reason");
                if (SaraConstant.ACTION_REASON_HOME_KEY.equals(reason) && SaraUtils.isKillSelf(context)) {
                    SaraUtils.killSelf(context);
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.app_drawer_main);

        IntentFilter filter = new IntentFilter();
        filter.addAction(SaraConstant.ACTION_FINISH_SETTINGS_ACTIVITY);
        filter.addAction(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
        registerReceiver(mReceiver, filter);

        initView();
        initTitleBar();
        setCaptionTitleToPillInExtDisplay();
    }

    private void initView() {
        mShareList = (ListView) findViewById(R.id.list_view);

        View headerView = getLayoutInflater().inflate(R.layout.app_drawer_content_layout, null);
        mAppDrawerSwitch = (ListContentItemSwitch) headerView.findViewById(R.id.app_drawer_switch);
        mAppDrawerSwitch.setTitle(R.string.app_drawer_string);
        mShareLayout = (RelativeLayout) headerView.findViewById(R.id.share_layout);
        mBubbleText = (TextView) headerView.findViewById(R.id.app_drawer_text);
        mShareDrawerLayout = (ShareDrawerLayout) headerView.findViewById(R.id.share_drawer_layout);
        mShareDrawerLayout.setOnSortChangeListener(this);
        mShareDrawerLayout.setOnDragListener(mOnDragListener);
        mImageTop = (ImageView) headerView.findViewById(R.id.v_image);
        mBottomView = (View) headerView.findViewById(R.id.app_drawer_bottom_view);

        View footView = getLayoutInflater().inflate(R.layout.app_drawer_bottom, null);

        mShareList.addHeaderView(headerView);
        mShareList.addFooterView(footView);

        updateDrawerEnable();
        final int color = SaraUtils.getDefaultBubbleColor(this);
        final int padding = getResources().getDimensionPixelOffset(R.dimen.app_drawer_bubble_item_padding);
        mBubbleText.setBackground(getDrawable(BubbleThemeManager.getBackgroudRes(color, BubbleThemeManager.BACKGROUND_BUBBLE_LARGE)));
        if(color == GlobalBubble.COLOR_SHARE){
            int paddingLeft = getResources().getDimensionPixelOffset(R.dimen.app_drawer_bubble_item_share_padding_left);
            mBubbleText.setTextColor(getResources().getColor(R.color.share_text_color));
            mBubbleText.setPadding(paddingLeft, 0, padding, 0);
        }else{
            mBubbleText.setPadding(padding, 0, padding, 0);
        }
        mShareLayout.setAlpha(isDrawerEnable ? 1f : 0.3f);
        mAppDrawerSwitch.setChecked(isDrawerEnable);
        mAppDrawerSwitch.setFocusable(true);
    }

    private boolean updateDrawerEnable() {
        isDrawerEnable = SaraUtils.isDrawerEnable(this);
        mShareDrawerLayout.setAllowDrag(isDrawerEnable);
        return isDrawerEnable;
    }

    public void initTitleBar() {
        Title title = (Title) findViewById(R.id.title_bar);
        title.setTitle(R.string.app_drawer_string);
        title.setBackButtonListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });
        title.setBackButtonText(R.string.idea_pills_beta);
        Intent fromIntent = getIntent();
        //if from launcher, hidden the back button, otherwise set the background
        if (!fromIntent.hasExtra(Title.EXTRA_BACK_BTN_TEXT) && !fromIntent.hasExtra("from_search")) {
            title.getBackButton().setVisibility(View.VISIBLE);
        } else {
            title.getBackButton().setVisibility(View.GONE);
        }
        setTitleByIntent(title);

    }

    @Override
    public void OnSortChange(List<ShareItem> shareItems) {
        mAdapter.updateShareData(shareItems);
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        if (buttonView == mAppDrawerSwitch.getSwitch()) {
            initBottomView(isChecked);
        }
    }

    @Override
    protected void onResume() {
        LogTag.d(TAG, "onResume()");
        super.onResume();
        mAdapter = new DrawerSettingAdapter(this);
        mShareList.setAdapter(mAdapter);
        mAdapter.setShareIconClickListener(mIconListener);
        mAppDrawerSwitch.setOnCheckedChangeListener(this);
        isBindShareView = false;
        updateShareData();
    }


    @Override
    protected void onPause() {
        LogTag.d(TAG, "onPause()");
        super.onPause();
        MultiSdkAdapter.cancelDragAndDrop(mShareDrawerLayout);
        mAppDrawerSwitch.setOnCheckedChangeListener(null);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mReceiver);
        isDestroy = true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void finish() {
        super.finish();
        if (SaraUtils.isKillSelf(this)) {
            SaraUtils.killSelf(this);
        }
    }


    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        initTitleBar();
    }

    private void initBottomView(boolean isChecked) {
        ContentResolver cr = getContentResolver();
        Settings.Global.putInt(cr, SaraConstant.APP_DRAWER_ENABLE, isChecked ? 1 : 0);
        updateDrawerEnable();
        mShareLayout.setAlpha(isDrawerEnable ? 1f : 0.3f);
        updateShareData();
    }

    private boolean bindShareItem(ShareItem shareItem, List<ResolveInfo> resolveInfos, boolean anim) {
        boolean ret = false;
        if(mDisappearAnim != null){
            mDisappearAnim.cancel();
            mDisappearAnim = null;
        }
        if (mShareDrawerLayout.getChildCount() < CommonConstant.APP_DRAWER_MAX_COUNT) {
            PackageManager pkm = this.getPackageManager();
            ShadowBitmapView itemView = (ShadowBitmapView) View.inflate(this, R.layout.app_drawer_item, null);
            itemView.setContentDescription(shareItem.getDispalyName());
            itemView.setTag(shareItem);
            itemView.setImageDrawableAsyncLoadShadow(shareItem.getDrawable(getApplicationContext()));
            itemView.setOnLongClickListener(mItemLongClickListener);
            mShareDrawerLayout.addView(itemView);
            ret = true;
            if (anim) {
                appDrawerAddAnim(itemView);
            }
        }
        return ret;
    }

    private void updateDrawerAppIcon() {
        List<ShareItem> shareList = mAdapter.getSaveShareList();
        List<ResolveInfo> resolveInfos = PackageUtils.getResolveInfoList(this);
        mShareDrawerLayout.removeAllViews();
        if (shareList == null || shareList.size() <= 0) {
            return;
        }
        for (ShareItem shareItem : shareList) {
            bindShareItem(shareItem, resolveInfos, false);
        }
    }

    private void appDrawerAddAnim(View view) {
        if(view != null){
            PropertyValuesHolder scaleX = PropertyValuesHolder.ofFloat("scaleX", 0.3f, 1f);
            PropertyValuesHolder scaleY = PropertyValuesHolder.ofFloat("scaleY", 0.3f, 1f);
            PropertyValuesHolder alpha = PropertyValuesHolder.ofFloat("alpha", 0f, 1f);
            Animator anim = ObjectAnimator.ofPropertyValuesHolder(view, scaleX, scaleY, alpha);
            anim.start();
        }
    }

    private boolean removeAppDrawerWithAnim(final View view) {
        if (view != null) {
            PropertyValuesHolder scaleX = PropertyValuesHolder.ofFloat("scaleX", 1f, 0.3f);
            PropertyValuesHolder scaleY = PropertyValuesHolder.ofFloat("scaleY", 1f, 0.3f);
            PropertyValuesHolder alpha = PropertyValuesHolder.ofFloat("alpha", 1f, 0.3f);
            mDisappearAnim = ObjectAnimator.ofPropertyValuesHolder(view, scaleX, scaleY, alpha);
            mDisappearAnim.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator arg0) {
                    if (mShareDrawerLayout != null && view != null) {
                        mShareDrawerLayout.removeView(view);
                    }
                }
            });
            mDisappearAnim.start();
        }
        return true;
    }

    private onShareIconClickListener mIconListener = new onShareIconClickListener() {
        @Override
        public boolean onIconClick(boolean add, ShareItem item) {
            if (add) {
                List<ResolveInfo> resolveInfos = PackageUtils.getResolveInfoList(DrawerSettingActivity.this);
                return bindShareItem(item, resolveInfos, true);
            } else {
                int childCount = mShareDrawerLayout.getChildCount();
                View child = null;
                for (int i = 0; i < childCount; i++) {
                    child = mShareDrawerLayout.getChildAt(i);
                    ShareItem shareitem = (ShareItem) child.getTag();
                    if (item.getComponentName().equals(shareitem.getComponentName())) {
                        break;
                    }
                }
                return removeAppDrawerWithAnim(child);
            }
        }
    };

    private View.OnLongClickListener mItemLongClickListener = new View.OnLongClickListener() {
        @Override
        public boolean onLongClick(View v) {
            return mShareDrawerLayout.startDrag(v);
        }
    };

    private void updateShareData() {
        new AsyncTask<Void, Void, List<ShareItem>>() {

            @Override
            protected List<ShareItem> doInBackground(Void... voids) {
                mAdapter.updateDefaultShareData();
                return mAdapter.updateShareList(isDrawerEnable);
            }

            @Override
            protected void onPostExecute(List<ShareItem> shareItems) {
                mAdapter.notifyDataChanged(shareItems);
                if (!isBindShareView) {
                    isBindShareView = true;
                    updateDrawerAppIcon();
                }
                if (isDrawerEnable && shareItems.size() > 0) {
                    mBottomView.setVisibility(View.GONE);
                } else {
                    mBottomView.setVisibility(View.VISIBLE);
                }
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    private ShareDrawerLayout.OnDragListener mOnDragListener = new ShareDrawerLayout.OnDragListener() {
        @Override
        public void onDragStart() {
            mAppDrawerSwitch.getSwitch().setEnabled(false);
            mAdapter.setClickEnable(false);
        }

        @Override
        public void onDragEnd() {
            mAppDrawerSwitch.getSwitch().setEnabled(true);
            mAdapter.setClickEnable(true);
        }
    };
}
