package com.kborid.smart.control;

import android.view.View;
import android.widget.RemoteViews;

public enum RemoteViewControl {
    instance;
    private RemoteViews remoteViews = null;
    private Object view = null;

    public RemoteViews getRemoteViews() {
        return remoteViews;
    }

    public void setRemoteViews(RemoteViews views) {
        remoteViews = views;
    }

    public void setView(View view) {
        this.view = view;
    }

    public Object getView() {
        return view;
    }
}
