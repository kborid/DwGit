package com.kborid.library.event;


public interface IDispatcher extends IPublisher {

    void subscribe(Object subscriber);

    void unsubscribe(Object subscriber);

}

