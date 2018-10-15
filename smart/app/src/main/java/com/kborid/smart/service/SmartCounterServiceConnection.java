package com.kborid.smart.service;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;

import com.kborid.library.common.LogUtils;
import com.kborid.smart.PRJApplication;

public class SmartCounterServiceConnection implements ServiceConnection {
    private boolean isConn = false;
    private Context mContext;

    private SmartCounterService.CounterBinder binder;
    private SmartCounterService service = null;

    public SmartCounterServiceConnection(){
        mContext = PRJApplication.getInstance();
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder iBinder) {
        LogUtils.d("onServiceConnected()");
        if (null != iBinder){
            binder = (SmartCounterService.CounterBinder)iBinder;
            service = binder.getService();
        }
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        LogUtils.d("onServiceDisconnected()");
    }

    @Override
    public void onBindingDied(ComponentName name) {

    }

    public void bindService(){
        if (!isConn) {
            Intent intent = new Intent();
            intent.setComponent(new ComponentName("com.kborid.smart", "com.kborid.smart.service.SmartCounterService"));
            intent.putExtra("messenger", new Messenger(new MyHandler()));
            mContext.bindService(intent, this, Context.BIND_AUTO_CREATE);
            isConn = true;
        }
    }

    private class MyHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case SmartCounterService.UPDATE_COUNTER:
                    if (null != listener) {
                        listener.onCountChanged(msg.arg1);
                    }
                    break;
            }
        }
    }

    public void unBindService() {
        if (isConn){
            mContext.unbindService(this);
            isConn = false;
        }
    }

    public void setCounter(int counter){
        if (null != service){
            service.setCountValue(counter);
        }
    }

    public void startCount(){
        if (null != service){
            service.start();
        }
    }

    public void pauseCount() {
        if (null != service) {
            service.pause();
        }
    }

    public void stopCount(){
        if (null != service){
            service.stop();
        }
    }

    public interface CountChangedListener {
        void onCountChanged(int value);
    }
    private CountChangedListener listener = null;
    public void setCountChangedListener(CountChangedListener listener) {
        this.listener = listener;
    }
}
