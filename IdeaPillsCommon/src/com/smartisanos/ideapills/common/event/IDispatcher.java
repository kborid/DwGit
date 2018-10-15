package com.smartisanos.ideapills.common.event;


public interface IDispatcher extends IPublisher {

    void subscribe(Object subscriber);

    void unsubscribe(Object subscriber);

}

