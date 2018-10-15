package com.kborid.smart.service;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;

import com.kborid.library.common.LogUtils;
import com.kborid.smart.PRJApplication;

public class SmartTestServiceConnection implements ServiceConnection {
    private boolean isConn = false;
    private Context mContext;

    private SmartTestService.MethodBinder binder;
    private SmartTestService service = null;

    public SmartTestServiceConnection(){
        mContext = PRJApplication.getInstance();
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder iBinder) {
        LogUtils.d("onServiceConnected()");
        if (null != iBinder){
            binder = (SmartTestService.MethodBinder)iBinder;
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

    public void test() {
        if (null != service) {
            service.test();
        }
    }

    public void bindService(){
        if (!isConn) {
            Intent intent = new Intent();
            intent.setComponent(new ComponentName("com.kborid.smart", "com.kborid.smart.service.SmartTestService"));
            mContext.bindService(intent, this, Context.BIND_AUTO_CREATE);
            isConn = true;
        }
    }

    public void unBindService() {
        if (isConn){
            mContext.unbindService(this);
            isConn = false;
        }
    }

    public void setString(String s){
        if (null != binder){
            binder.setString(s);
        }
    }

    public void printlnString(){
        if (null != binder){
            binder.printlnString();
        }
    }

    public void appendString(String s){
        if (null != binder) {
            binder.appendString(s);
        }
    }
}
