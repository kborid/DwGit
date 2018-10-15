package com.kborid.smart.impl;

import android.os.RemoteException;
import android.widget.RemoteViews;

import com.kborid.smart.IRemoteViewManager;
import com.kborid.smart.control.RemoteViewControl;


public class RemoteViewImpl extends IRemoteViewManager.Stub {
    @Override
    public void basicTypes(int anInt, long aLong, boolean aBoolean, float aFloat, double aDouble, String aString) throws RemoteException {

    }

    @Override
    public RemoteViews getRemoteView() throws RemoteException {
        return RemoteViewControl.instance.getRemoteViews();
    }
}
