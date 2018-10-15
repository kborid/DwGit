package com.kborid.smart.service;

import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.widget.RemoteViews;

import com.kborid.smart.R;
import com.kborid.smart.control.RemoteViewControl;
import com.kborid.smart.impl.RemoteViewImpl;
import com.kborid.smart.util.ToastUtils;

public class RemoteViewService extends Service {
    private static final String TEST_ACTION = "ClickTestAction";

    private IBinder remoteViewImpl = null;
    private ClickBroadcastReceiver receiver = new ClickBroadcastReceiver();

    @Override
    public void onCreate() {
        super.onCreate();
        initRemoteView();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(TEST_ACTION);
        registerReceiver(receiver, intentFilter);
        remoteViewImpl = new RemoteViewImpl();
    }

    private void initRemoteView() {
        RemoteViews remoteViews = new RemoteViews(getPackageName(), R.layout.remote);
        remoteViews.setTextViewText(R.id.tv_name, "Smartisan RemoteView Test");
        PendingIntent intent = PendingIntent.getBroadcast(this, 0, new Intent(TEST_ACTION), PendingIntent.FLAG_UPDATE_CURRENT);
        remoteViews.setOnClickPendingIntent(R.id.tv_name, intent);
        RemoteViewControl.instance.setRemoteViews(remoteViews);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return remoteViewImpl;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        return super.onUnbind(intent);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(receiver);
    }

    private class ClickBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (TEST_ACTION.equals(intent.getAction())) {
                ToastUtils.showToast("RemoteViewClick");
            }
        }
    }
}
