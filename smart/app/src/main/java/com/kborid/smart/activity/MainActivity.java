package com.kborid.smart.activity;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.pm.LabeledIntent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Looper;
import android.os.MessageQueue;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.ContextThemeWrapper;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.kborid.library.common.LogUtils;
import com.kborid.library.common.MultiTaskHandler;
import com.kborid.library.common.UIHandler;
import com.kborid.library.common.ViewUtils;
import com.kborid.library.sample.TestSettings;
import com.kborid.smart.PRJApplication;
import com.kborid.smart.R;
import com.kborid.smart.service.SmartCounterServiceConnection;
import com.kborid.smart.service.SmartTestServiceConnection;
import com.kborid.smart.util.ToastUtils;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final String WX_PKG = "com.tencent.mm";
    private static final String WX_CLASS = "com.tencent.mm.ui.LauncherUI";
    private static final String QQ_PKG = "com.tencent.mobileqq";
    private static final String QQ_CLASS = "com.tencent.mobileqq.activity.SplashActivity";
    private static final String ALIPAY_PKG = "com.eg.android.AlipayGphone";
    private static final String ALIPAY_CLASS = "com.eg.android.AlipayGphone.AlipayLogin";

    private ScrollView scrollView;
    private TextView tv_console;

    private SmartCounterServiceConnection counterConn = null;
    private SmartTestServiceConnection testConn = null;
    private static Drawable mDrawable;

    private static final HashMap<String, String> packageName = new HashMap<String, String>() {
        {
            put(WX_PKG, WX_CLASS);
            put(QQ_PKG, QQ_CLASS);
            put(ALIPAY_PKG, ALIPAY_CLASS);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
        setContentView(R.layout.activity_main);
        scrollView = findViewById(R.id.scrollView);
        tv_console = findViewById(R.id.tv_console);
        printTest();
        initThirdLoginLayout();
        bindServiceInner();
    }

    @Override
    protected void onResume() {
        super.onResume();
        LogUtils.d("onResume()");
    }

    public void onShare(View v) {
        ToastUtils.showToast("share");
        Intent targetIntent = new Intent(Intent.ACTION_SEND);
        targetIntent.setType("text/plain");
        targetIntent.putExtra(Intent.EXTRA_TEXT, "Hello World!!!");
        targetIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        List<Intent> targetedShareIntents = new ArrayList<Intent>();
        targetedShareIntents.add(targetIntent);
        Intent resultIntent = null;
        if (targetedShareIntents.size() > 0) {
            resultIntent = Intent.createChooser(targetedShareIntents.remove(0), null);
            LabeledIntent[] li = targetedShareIntents.toArray(new LabeledIntent[targetedShareIntents.size()]);
            resultIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, li);
            resultIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        }
        try {
            startActivity(resultIntent);
        } catch (android.content.ActivityNotFoundException ex) {
            ToastUtils.showToast("Can't find share component to share");
        }
        counterConn.stopCount();
    }

    public void onTest2(View v) {
        ToastUtils.showToast("jump");
        UIHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                startActivity(new Intent(MainActivity.this, FragmentContainerActivity.class));
            }
        }, 1000);
        counterConn.pauseCount();
    }

    public void onReflect(View v) {
        ToastUtils.showToast("reflect");
        reflectInvokeTest();
        counterConn.startCount();
        idleHandler();
    }

    public void onPrintContext(View v) {
        printContextType(getBaseContext());         //Context
        printContextType(getApplicationContext());  //ContextWrapper
        printContextType(getActivityContext());     //ContextThemeWrapper
        /**
         *  内存泄漏
         *  原因：mDrawable是一个静态对象，常驻内存，通过ImageView方法setImageDrawable时，会导致mDrawable引用ImageView（Drawable.setCallBack），
         *  同时，ImageView又对activity有引用，所以导致mDrawable间接引用activity，使activity无法被回收。
         */
        ImageView iv = new ImageView(this);
        mDrawable = getResources().getDrawable(R.mipmap.ic_launcher);
        iv.setImageDrawable(mDrawable);

        testForEach();
    }

    private void printContextType(Context context) {
        outputConsoleTextView("Type is ["+context+"]");
        outputConsoleTextView("It is Context!");
        if (context instanceof ContextWrapper) {
            outputConsoleTextView("It is ContextWrapper too!");
        }
        if (context instanceof ContextThemeWrapper) {
            outputConsoleTextView("It is ContextThemeWrapper too!");
        }
    }

    private void testForEach() {
        ArrayList<String> temp = getStringData();
        for (String str : temp) {
            outputConsoleTextView("str = " + str);
        }
    }

    private ArrayList<String> getStringData() {
        ArrayList<String> temp = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            temp.add(String.valueOf(i));
        }
        return temp;
    }

    private Activity getActivityContext() {
        return this;
    }

    private void idleHandler() {
        Looper.myQueue().addIdleHandler(new MessageQueue.IdleHandler() {
            @Override
            public boolean queueIdle() {
                outputConsoleTextView("IdleHandler queueIdle()");
                return false;
            }
        });

        UIHandler.post(new Runnable() {
            @Override
            public void run() {
                outputConsoleTextView("normal handler post");
            }
        });

        UIHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                outputConsoleTextView("delay 200 handler post");
            }
        }, 200);
    }

    public void onWebView(View v) {
        Intent intent = new Intent(this, WebViewActivity.class);
        intent.putExtra("from", "main");
        startActivity(intent);
    }

    public void onClickMeasure(View v) {
        View view1 = findViewById(R.id.view_1);
        View view2 = findViewById(R.id.view_2);
        View view3 = findViewById(R.id.view_3);
        View view4 = findViewById(R.id.view_4);

        int view1_h1, view1_h2, view1_h3;
        view1_h1 = ViewUtils.getSupposeHeight(view1);
        view1_h2 = ViewUtils.getExactHeight(view1, view1.getWidth());
        view1_h3 = ViewUtils.getSupposeHeightNoFixWidth(view1);
        outputConsoleTextView("[view1]:height1 = " + view1_h1 + ", height2 = " + view1_h2 + ", height3 = " + view1_h3);
        outputConsoleTextView("[view1]:myTestHeight = " + ViewUtils.getMyTestHeight(view1));

        int view2_h1, view2_h2, view2_h3;
        view2_h1 = ViewUtils.getSupposeHeight(view2);
        view2_h2 = ViewUtils.getExactHeight(view2, view2.getWidth());
        view2_h3 = ViewUtils.getSupposeHeightNoFixWidth(view2);
        outputConsoleTextView("[view2]:height1 = " + view2_h1 + ", height2 = " + view2_h2 + ", height3 = " + view2_h3);
        outputConsoleTextView("[view2]:myTestHeight = " + ViewUtils.getMyTestHeight(view2));

        int view3_h1, view3_h2, view3_h3;
        view3_h1 = ViewUtils.getSupposeHeight(view3);
        view3_h2 = ViewUtils.getExactHeight(view3, view3.getWidth());
        view3_h3 = ViewUtils.getSupposeHeightNoFixWidth(view3);
        outputConsoleTextView("[view3]:height1 = " + view3_h1 + ", height2 = " + view3_h2 + ", height3 = " + view3_h3);
        outputConsoleTextView("[view3]:myTestHeight = " + ViewUtils.getMyTestHeight(view3));

        int view4_h1, view4_h2, view4_h3;
        view4_h1 = ViewUtils.getSupposeHeight(view4);
        view4_h2 = ViewUtils.getExactHeight(view4, view4.getWidth());
        view4_h3 = ViewUtils.getSupposeHeightNoFixWidth(view4);
        outputConsoleTextView("[view4]:height1 = " + view4_h1 + ", height2 = " + view4_h2 + ", height3 = " + view4_h3);
        outputConsoleTextView("[view4]:myTestHeight = " + ViewUtils.getMyTestHeight(view4));

        LinearLayout test = findViewById(R.id.test);
        view3_h1 = ViewUtils.getSupposeHeight(test);
        view3_h2 = ViewUtils.getExactHeight(test, test.getWidth());
        view3_h3 = ViewUtils.getSupposeHeightNoFixWidth(test);
        outputConsoleTextView("[test]:height1 = " + view3_h1 + ", height2 = " + view3_h2 + ", height3 = " + view3_h3);
        outputConsoleTextView("[test]:myTestHeight = " + ViewUtils.getMyTestHeight(test));
    }

    private void printTest() {
        TestSettings.instance.reset();
        outputConsoleTextView("\nnothing set===========================");
        TestSettings.instance.getFlag();
        outputConsoleTextView("isFlagOne = " + TestSettings.instance.isFlagOne());
        outputConsoleTextView("isFlagTwo = " + TestSettings.instance.isFlagTwo());
        outputConsoleTextView("isFlagThr = " + TestSettings.instance.isFlagThr());

        outputConsoleTextView("\none set===========================");
        TestSettings.instance.setSettingOne(true);
        TestSettings.instance.getFlag();
        outputConsoleTextView("isFlagOne = " + TestSettings.instance.isFlagOne());
        outputConsoleTextView("isFlagTwo = " + TestSettings.instance.isFlagTwo());
        outputConsoleTextView("isFlagThr = " + TestSettings.instance.isFlagThr());

        outputConsoleTextView("\none two set===========================");
        TestSettings.instance.setSettingTwo(true);
        TestSettings.instance.getFlag();
        outputConsoleTextView("isFlagOne = " + TestSettings.instance.isFlagOne());
        outputConsoleTextView("isFlagTwo = " + TestSettings.instance.isFlagTwo());
        outputConsoleTextView("isFlagThr = " + TestSettings.instance.isFlagThr());

        outputConsoleTextView("\none two thr set===========================");
        TestSettings.instance.setSettingThr(true);
        TestSettings.instance.getFlag();
        outputConsoleTextView("isFlagOne = " + TestSettings.instance.isFlagOne());
        outputConsoleTextView("isFlagTwo = " + TestSettings.instance.isFlagTwo());
        outputConsoleTextView("isFlagThr = " + TestSettings.instance.isFlagThr());

        outputConsoleTextView("lists = " + TestSettings.instance.getTestList());

        outputConsoleTextView(getResources().getQuantityString(R.plurals.test_plurals, 0, 0));
        outputConsoleTextView(getResources().getQuantityString(R.plurals.test_plurals, 1, 1));
        outputConsoleTextView(getResources().getQuantityString(R.plurals.test_plurals, 2, 2));
        outputConsoleTextView(getResources().getQuantityString(R.plurals.test_plurals, 3, 3));
        outputConsoleTextView(getResources().getQuantityString(R.plurals.test_plurals, 4, 4));
    }

    private void initThirdLoginLayout() {
        LinearLayout third = (LinearLayout) findViewById(R.id.third);
        third.removeAllViews();
        try {
            for (final String key : packageName.keySet()) {
                if (isInstalled(key)) {
                    ImageView icon = new ImageView(this);
                    LinearLayout.LayoutParams llp = new LinearLayout.LayoutParams(100, 100);
                    llp.setMargins(10, 10, 10, 10);
                    third.addView(icon, llp);
                    icon.setImageDrawable(getPackageManager().getApplicationIcon(key));
                    icon.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            Intent intent = new Intent();
                            intent.setComponent(new ComponentName(key, packageName.get(key)));
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            startActivity(intent);
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
        List<PackageInfo> packageInfos = getPackageManager().getInstalledPackages(0);
        for (PackageInfo packageInfo : packageInfos) {
            if (packageInfo.packageName.equalsIgnoreCase(pkg)) {
                flag = true;
                break;
            }
        }
        return flag;
    }

    private void bindServiceInner() {
        bindTestService();
        bindCounterService();
    }

    private void unBindServiceInner() {
        unBindTestService();
        unBindCounterService();
    }

    private void bindCounterService() {
        if (null == counterConn) {
            counterConn = new SmartCounterServiceConnection();
        }
        counterConn.bindService();
        counterConn.setCountChangedListener(countChangedListener);
    }

    private SmartCounterServiceConnection.CountChangedListener countChangedListener = new SmartCounterServiceConnection.CountChangedListener() {
        @Override
        public void onCountChanged(int value) {
            ((TextView)findViewById(R.id.tv_counter)).setText(String.valueOf(value));
        }
    };

    private void unBindCounterService() {
        if (null != counterConn) {
            counterConn.unBindService();
            counterConn = null;
        }
    }

    private void bindTestService() {
        if (null == testConn) {
            testConn = new SmartTestServiceConnection();
        }
        testConn.bindService();
    }

    private void unBindTestService() {
        if (null != testConn) {
            testConn.unBindService();
            testConn = null;
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        LogUtils.d("onPause()");
    }

    @Override
    protected void onStop() {
        super.onStop();
        LogUtils.d("onStop()");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        LogUtils.d("onDestroy()");
        unBindServiceInner();
    }

    private void reflectInvokeTest() {
        MultiTaskHandler.post(new Runnable() {
            @Override
            public void run() {
                try {
//                    Class<?> clazz = Class.forName("com.kborid.smart.PRJApplication"); // 1
//                    Class<?> clazz = PRJApplication.class; // 2
                    Class<?> clazz = PRJApplication.getInstance().getClass(); // 3
                    Object obj = clazz.newInstance();
                    Method method = clazz.getDeclaredMethod("initFunc1");
                    method.setAccessible(true);
                    method.invoke(obj);
                    Method method1 = clazz.getDeclaredMethod("reflectMethod", int.class);
                    method1.setAccessible(true);
                    method1.invoke(clazz, 30);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void outputConsoleTextView(String string) {
        LogUtils.d("outputConsoleTextView() " + string);
        if (null != tv_console) {
            StringBuilder sb = new StringBuilder();
            if (!TextUtils.isEmpty(tv_console.getText())) {
                sb.append("\n");
            }
            sb.append(string);
            tv_console.append(sb.toString());
            scroll2Bottom(scrollView, tv_console);
        }
    }

    public static void scroll2Bottom(final ScrollView scroll, final View inner) {
        UIHandler.post(new Runnable() {
            @Override
            public void run() {
                if (scroll == null || inner == null) {
                    return;
                }
                // 内层高度超过外层
                int offset = inner.getMeasuredHeight() - scroll.getMeasuredHeight();
                if (offset < 0) {
                    offset = 0;
                }
                scroll.smoothScrollTo(0, offset);
            }
        });
    }
}
