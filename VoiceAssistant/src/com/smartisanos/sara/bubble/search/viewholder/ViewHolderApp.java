package com.smartisanos.sara.bubble.search.viewholder;

import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.smartisanos.sara.R;
import com.smartisanos.sara.SaraApplication;
import com.smartisanos.sara.entity.AppModel;
import com.smartisanos.sara.entity.IModel;
import com.smartisanos.sara.util.AppImageLoader;
import com.smartisanos.sara.util.LogUtils;
import com.smartisanos.sara.util.SaraConstant;
import com.smartisanos.sara.util.SaraUtils;

import smartisanos.app.voiceassistant.ApplicationStruct;

import java.util.List;

import smartisanos.util.DeviceType;

public class ViewHolderApp extends ViewHolder implements View.OnClickListener,
        View.OnLongClickListener {
    private static String TAG = "VoiceAss.ViewHolderApp";
    private static final String APP_PROVIDER_AUTHORITY = "applications";
    private static final String APPLICATION_PATH = "applications";
    private static final String KEY_TAB_INDEX = "show_tab_at";

    TextView mTitleView;
    ImageView mAppIcon;
    TextView mAppName;
    RelativeLayout mAppList;
    TextView mInstalledView;
    TextView mAppSizeView;
    View mAppLayout;

    private AppImageLoader mAppImageLoader;
    private AppModel model;
    private IAppStartListener mIAppStartListener;

    public ViewHolderApp(Context context, View v) {
        super(context, v);
        mAppImageLoader = SaraApplication.getInstance().getAppImageLoader();
        mAppIcon = (ImageView) v.findViewById(R.id.app_icon);
        mAppName = (TextView) v.findViewById(R.id.app_name);
        mTitleView = (TextView) v.findViewById(R.id.title_app);
        mAppList = (RelativeLayout) v.findViewById(R.id.app_list);
        mInstalledView = (TextView) v.findViewById(R.id.install_btn);
        mAppSizeView = (TextView) v.findViewById(R.id.app_size_tv);
        mAppLayout = v.findViewById(R.id.app_install_layout);

        mAppList.setOnClickListener(this);
        mAppList.setOnLongClickListener(this);
        mInstalledView.setOnClickListener(this);

    }

    public void bindView(IModel m, boolean showTitle) {
        model = (AppModel) m;
        ApplicationStruct struct = model.struct;
        difSetVis(mTitleView, showTitle?View.VISIBLE:View.GONE);
        difSetText(mAppName, struct.mAppName);

        int installedState = struct.getInstalledState();
        mAppImageLoader.DisplayImage(struct.mIconUri != null ? struct.mIconUri.toString() : null,
                mAppIcon, installedState == 1 ? AppImageLoader.DRAWABLE_TYPE.LOCAL : AppImageLoader.DRAWABLE_TYPE.DOWLOAD);
        difSetText(mAppSizeView, struct.getAppSize());
        difSetVis(mAppLayout, installedState == 1 ? View.GONE : View.VISIBLE);
    }

    private void difSetText(TextView view, String text) {
        if (!TextUtils.equals(view.getText(), text)) {
            view.setText(text);
        }
    }

    private void difSetVis(View view, int vis) {
        if (view.getVisibility() != vis) {
            view.setVisibility(vis);
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.app_list:
                LogUtils.d(TAG, "isInstalled = " + model.struct.getInstalledState());
                if (model.struct.getInstalledState() == 1) {
                    launchApp();
                } else {
                    SaraUtils.jumpToAppStore(mContext, model.struct.getPackageName());
                }
                break;
            case R.id.install_btn:
                SaraUtils.startInstallApp(mContext, model.struct.getPackageName());
                break;
        }
    }

    @Override
    public boolean onLongClick(View v) {
        return false;
    }

    private void launchApp() {
        try {
            ComponentName componentName = uriToComponentName(model.struct.mStartUri);
            if (componentName != null) {
                Intent launchIntent = new Intent();
                launchIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
                launchIntent.setComponent(componentName);
                if (model.struct.mAppIndex != -1) {
                    launchIntent.putExtra(KEY_TAB_INDEX, model.struct.mAppIndex);
                }
                if (SaraConstant.SECURITY_PACKAGE_NAME.equals(componentName.getPackageName())) {
                    launchIntent.putExtra(SaraConstant.SECURITY_FROM_KEY, false);
                }
                if (mIAppStartListener != null) {
                    mIAppStartListener.startApplication(launchIntent);
                } else {
                    SaraUtils.startActivity(mContext, launchIntent);
                }
            }
        } catch (Exception e) {
            LogUtils.e(TAG, e.getMessage());
        }
    }

    private ComponentName uriToComponentName(Uri appUri) {
        if (appUri == null)
            return null;
        if (!ContentResolver.SCHEME_CONTENT.equals(appUri.getScheme()))
            return null;
        if (!APP_PROVIDER_AUTHORITY.equals(appUri.getAuthority()))
            return null;
        List<String> pathSegments = appUri.getPathSegments();
        if (pathSegments.size() != 3)
            return null;
        if (!APPLICATION_PATH.equals(pathSegments.get(0)))
            return null;
        String packageName = pathSegments.get(1);
        String name;
        ResolveInfo resolveInfo = getDefaultResolveInfo();
        if (SaraConstant.BROWSER_PACKAGE_NAME_SMARTISAN.equals(packageName) && packageName.equals(resolveInfo.activityInfo.packageName) && !DeviceType.isOneOf(DeviceType.OSCAR)) {
            name = resolveInfo.activityInfo.name;
        } else {
            name = pathSegments.get(2);
        }
        return new ComponentName(packageName, name);
    }

    public ResolveInfo getDefaultResolveInfo() {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(new Uri.Builder().scheme("http").authority("*").build());
        return mContext.getPackageManager().resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY);
    }

    public void setAppStartListener(IAppStartListener listener) {
        mIAppStartListener = listener;
    }

    public interface IAppStartListener {
        void startApplication(Intent intent);
    }
}
