package com.kborid.library.event;

public class Event {

    public static final String EXTRA = "extra";
    private final String action;
    private Object extra;

    public Event(String action) {
        this.action = action;
    }

    public String getAction() {
        return action;
    }

    public Object getExtra() {
        return extra;
    }

    public void putExtra(Object obj) {
        this.extra = obj;
    }
}
