package com.kborid.library.event.impl;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;

import com.kborid.library.event.Event;
import com.kborid.library.event.ISubscriber;


public abstract class LocalEventSubscriber implements ISubscriber {

    private final String[] mInterestedEvents;

    public LocalEventSubscriber(String... events) {
        mInterestedEvents = events;
    }

    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Event event = newEvent(intent);
            if (null == event) {
                return;
            }
            onEvent(event);
        }
    };

    BroadcastReceiver getReceiver() {
        return mReceiver;
    }

    @Override
    public String[] getInterestedEvents() {
        return mInterestedEvents;
    }

    private static Event newEvent(Intent intent) {
        if (intent == null) {
            return null;
        }
        String action = intent.getAction();
        if (TextUtils.isEmpty(action)) {
            return null;
        }
        Event event = new Event(action);
        event.putExtra(intent.getBundleExtra(Event.EXTRA));
        return event;
    }


}
