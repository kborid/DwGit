package com.smartisanos.ideapills.common.event;


public interface ISubscriber {

    void onEvent(Event event);

    String[] getInterestedEvents();
}
