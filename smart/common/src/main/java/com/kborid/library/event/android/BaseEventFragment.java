package com.kborid.library.event.android;

import android.app.Activity;
import android.app.Fragment;

import com.kborid.library.event.Event;
import com.kborid.library.event.IDispatcher;
import com.kborid.library.event.ISubscriber;
import com.kborid.library.event.impl.LocalEventSubscriber;
import com.kborid.library.event.utils.ArrayUtils;

public class BaseEventFragment extends Fragment implements IDispatcher, ISubscriber {

    private IDispatcher mRealDispatcher;
    private ISubscriber mRealSubscriber;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        injectDispatcher(activity);
        onSubscribe(activity);
    }

    @Override
    public void onDetach() {
        onUnsubscribe(getActivity());
        super.onDetach();
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

    private void injectDispatcher(Object obj) {
        if (obj instanceof IDispatcher) {
            mRealDispatcher = (IDispatcher) obj;
        }
    }

    private void onSubscribe(Object dispatcher) {
        subscribe(getSubscriber());
    }

    private void onUnsubscribe(Object dispatcher) {
        unsubscribe(getSubscriber());
        mRealDispatcher = null;
        mRealSubscriber = null;
    }

    private ISubscriber getSubscriber() {
        if (isNotInterestedInAnyEvent()) {
            return null;
        }
        if (null == mRealSubscriber) {
            mRealSubscriber = new LocalEventSubscriber(getInterestedEvents()) {
                @Override
                public void onEvent(Event event) {
                    BaseEventFragment.this.onEvent(event);
                }
            };
        }
        return mRealSubscriber;
    }

    private boolean isNotInterestedInAnyEvent() {
        return ArrayUtils.isEmpty(getInterestedEvents());
    }
}
