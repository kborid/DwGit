package com.kborid.library.event;


public interface IPublisher {

    void post(Event event);

    void post(String action);

    void postAsync(Event event);

    void postAsync(String action);
}
