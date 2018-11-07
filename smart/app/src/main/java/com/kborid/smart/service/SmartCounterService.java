package com.kborid.smart.service;

import android.app.Service;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.annotation.Nullable;

import com.kborid.library.common.LogUtils;

public class SmartCounterService extends Service {

    public static final int UPDATE_COUNTER = 0x001;

    private CounterBinder counterBinder = null;
    private int mValue = 99;
    private CounterAsyncTask task = null;
    private Messenger messenger = null;

    @Override
    public void onCreate() {
        super.onCreate();
        counterBinder = new CounterBinder();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        messenger = (Messenger) intent.getExtras().get("messenger");
        return counterBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        stop();
        return super.onUnbind(intent);
    }

    public class CounterBinder extends Binder {
        public SmartCounterService getService() {
            return SmartCounterService.this;
        }
    }

    public void setCountValue(int value) {
        mValue = value;
    }

    public void start() {
        if (task == null || task.isStop) {
            task = new CounterAsyncTask();
            task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, mValue);
        } else if (task.isPause) {
            task.isPause = false;
        }
    }

    public void stop() {
        if (null != task) {
            task.stop();
        }
    }

    public void pause() {
        if (null != task) {
            task.pause();
        }
    }

    private class CounterAsyncTask extends AsyncTask<Integer, Void, Integer> {

        private boolean isStop = false;
        private boolean isPause = false;
        private int value = 0;

        @Override
        protected Integer doInBackground(Integer... integers) {
            value = integers[0];
            while (!isStop) {
                if (!isPause) {
                    if (value <= 0) {
                        break;
                    }
                    LogUtils.i("count = " + value);
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    try {
                        Message message = Message.obtain();
                        message.what = UPDATE_COUNTER;
                        message.arg1 = value;
                        messenger.send(message);
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                    value--;
                }
            }
            return 1;
        }

        @Override
        protected void onPostExecute(Integer aVoid) {
            LogUtils.i("count is over");
        }

        void stop() {
            this.isStop = true;
        }

        void pause() {
            this.isPause = true;
        }
    }
}
