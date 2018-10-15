package com.kborid.library.event;


public interface ISubscriber {

    void onEvent(Event event);

    String[] getInterestedEvents();
}
