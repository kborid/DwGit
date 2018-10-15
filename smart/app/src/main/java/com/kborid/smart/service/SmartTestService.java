package com.kborid.smart.service;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.support.annotation.Nullable;

import com.kborid.library.common.LogUtils;

public class SmartTestService extends Service {

    private IBinder mBinder = new MethodBinder();

    @Override
    public void onCreate() {
        super.onCreate();
        LogUtils.i("onCreate: ");
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        LogUtils.i("onBind: ");
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        LogUtils.i("onUnbind: ");
        return super.onUnbind(intent);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        LogUtils.i("onDestroy: ");
    }

    public class MethodBinder extends Binder {
        private String string;
        public void setString(String s){
            this.string = s;
        }
        public void printlnString(){
            System.out.println(string);
        }
        public String getString(){
            return string;
        }
        public void appendString(String s){
            string += s;
        }
        public SmartTestService getService() {
            return SmartTestService.this;
        }
    }

    public void test() {
        LogUtils.d("test is called");
    }
}
