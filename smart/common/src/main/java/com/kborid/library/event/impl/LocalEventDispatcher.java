package com.kborid.library.event.impl;

import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;

import com.kborid.library.event.Event;
import com.kborid.library.event.IDispatcher;
import com.kborid.library.event.utils.ArrayUtils;
import com.kborid.library.event.utils.Precondition;


public class LocalEventDispatcher implements IDispatcher {

    private final LocalBroadcastManager mLocalBroadcastManager;


    public LocalEventDispatcher(LocalBroadcastManager localBroadcastManager) {
        mLocalBroadcastManager = Precondition.checkNotNull(localBroadcastManager);
    }

    @Override
    public void subscribe(Object subscriber) {
        if (!(subscriber instanceof LocalEventSubscriber)) {
            return;
        }
        String[] events = ((LocalEventSubscriber) subscriber).getInterestedEvents();
        if (ArrayUtils.isEmpty(events)) {
            return;
        }
        IntentFilter filter = new IntentFilter();
        for (int i = 0; i < events.length; i++) {
            filter.addAction(events[i]);
        }
        mLocalBroadcastManager.registerReceiver(((LocalEventSubscriber) subscriber).getReceiver(), filter);
    }

    @Override
    public void unsubscribe(Object subscriber) {
        if (!(subscriber instanceof LocalEventSubscriber)) {
            return;
        }
        mLocalBroadcastManager.unregisterReceiver(((LocalEventSubscriber) subscriber).getReceiver());
    }

    @Override
    public void postAsync(Event event) {
        Intent intent = newIntent(event);
        if (null == intent) {
            return;
        }
        mLocalBroadcastManager.sendBroadcast(intent);
    }

    @Override
    public void postAsync(String action) {
        postAsync(new Event(action));
    }

    @Override
    public void post(Event event) {
        Intent intent = newIntent(event);
        if (null == intent) {
            return;
        }
        mLocalBroadcastManager.sendBroadcastSync(intent);
    }

    @Override
    public void post(String action) {
        post(new Event(action));
    }

    private static Intent newIntent(Event event) {
        if (null == event || TextUtils.isEmpty(event.getAction())) {
            return null;
        }
        Intent intent = new Intent(event.getAction());
        Object extra = event.getExtra();
        if (extra instanceof Bundle) {
            intent.putExtra(Event.EXTRA, (Bundle) extra);
        }
        return intent;
    }

}
