package com.kborid.library.event.android;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;

import com.kborid.library.event.Event;
import com.kborid.library.event.IDispatcher;
import com.kborid.library.event.ISubscriber;
import com.kborid.library.event.impl.LocalEventDispatcher;
import com.kborid.library.event.impl.LocalEventSubscriber;
import com.kborid.library.event.utils.ArrayUtils;

public class BaseEventActivity extends Activity implements IDispatcher, ISubscriber {

    private IDispatcher mRealDispatcher;
    private ISubscriber mRealSubscriber;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        injectDispatcher(new LocalEventDispatcher(LocalBroadcastManager.getInstance(this)));
    }

    @Override
    protected void onResume() {
        super.onResume();
        onSubscribe(this);
    }

    @Override
    protected void onPause() {
        onUnsubscribe(this);
        super.onPause();
    }

    @Override
    public final void subscribe(Object subscriber) {
        if (null == mRealDispatcher || null == subscriber) {
            return;
        }
        mRealDispatcher.subscribe(subscriber);
    }

    @Override
    public final void unsubscribe(Object subscriber) {
        if (null == mRealDispatcher || null == subscriber) {
            return;
        }
        mRealDispatcher.unsubscribe(subscriber);
    }

    @Override
    public final void postAsync(Event event) {
        if (null == mRealDispatcher) {
            return;
        }
        mRealDispatcher.postAsync(event);
    }

    @Override
    public final void postAsync(String action) {
        if (null == mRealDispatcher) {
            return;
        }
        mRealDispatcher.postAsync(action);
    }

    @Override
    public final void post(Event event) {
        if (null == mRealDispatcher) {
            return;
        }
        mRealDispatcher.post(event);
    }

    @Override
    public final void post(String action) {
        if (null == mRealDispatcher) {
            return;
        }
        mRealDispatcher.post(action);
    }

    @Override
    public void onEvent(Event event) {
        //override if needed
    }

    @Override
    public String[] getInterestedEvents() {
        //override if needed
        return null;
    }

    //----------------------------------------------------------------------------------------------------------------//

    private void injectDispatcher(IDispatcher dispatcher) {
        mRealDispatcher = dispatcher;
    }

    private void onSubscribe(IDispatcher dispatcher) {
        subscribe(getSubscriber());
    }

    private void onUnsubscribe(IDispatcher dispatcher) {
        unsubscribe(getSubscriber());
    }

    private ISubscriber getSubscriber() {
        if (isNotInterestedInAnyEvent()) {
            return null;
        }
        if (null == mRealSubscriber) {
            mRealSubscriber = new LocalEventSubscriber(getInterestedEvents()) {
                @Override
                public void onEvent(Event event) {
                    BaseEventActivity.this.onEvent(event);
                }
            };
        }
        return mRealSubscriber;
    }

    private boolean isNotInterestedInAnyEvent() {
        return ArrayUtils.isEmpty(getInterestedEvents());
    }

}
