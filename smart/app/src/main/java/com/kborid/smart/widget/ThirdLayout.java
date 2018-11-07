package com.kborid.smart.widget;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.kborid.smart.R;

import java.util.HashMap;
import java.util.List;

public class ThirdLayout extends LinearLayout {
    private static final String WX_PKG = "com.tencent.mm";
    private static final String WX_CLASS = "com.tencent.mm.ui.LauncherUI";
    private static final String QQ_PKG = "com.tencent.mobileqq";
    private static final String QQ_CLASS = "com.tencent.mobileqq.activity.SplashActivity";
    private static final String ALIPAY_PKG = "com.eg.android.AlipayGphone";
    private static final String ALIPAY_CLASS = "com.eg.android.AlipayGphone.AlipayLogin";

    private static final HashMap<String, String> packageName = new HashMap<String, String>() {
        {
            put(WX_PKG, WX_CLASS);
            put(QQ_PKG, QQ_CLASS);
            put(ALIPAY_PKG, ALIPAY_CLASS);
        }
    };

    private Context mContext;

    public ThirdLayout(Context context) {
        this(context, null);
    }

    public ThirdLayout(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ThirdLayout(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.mContext = context;
        init();
    }

    private void init() {
        LayoutInflater.from(mContext).inflate(R.layout.layout_third, this);
        initThirdLoginLayout();
    }

    private void initThirdLoginLayout() {
        LinearLayout third = (LinearLayout) findViewById(R.id.third);
        third.removeAllViews();
        try {
            for (final String key : packageName.keySet()) {
                if (isInstalled(key)) {
                    ImageView icon = new ImageView(mContext);
                    LinearLayout.LayoutParams llp = new LinearLayout.LayoutParams(100, 100);
                    llp.setMargins(10, 10, 10, 10);
                    third.addView(icon, llp);
                    icon.setImageDrawable(mContext.getPackageManager().getApplicationIcon(key));
                    icon.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            Intent intent = new Intent();
                            intent.setComponent(new ComponentName(key, packageName.get(key)));
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            mContext.startActivity(intent);
                        }
                    });
                }
            }
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
    }

    private boolean isInstalled(String pkg) {
        boolean flag = false;
        List<PackageInfo> packageInfos = mContext.getPackageManager().getInstalledPackages(0);
        for (PackageInfo packageInfo : packageInfos) {
            if (packageInfo.packageName.equalsIgnoreCase(pkg)) {
                flag = true;
                break;
            }
        }
        return flag;
    }
}
