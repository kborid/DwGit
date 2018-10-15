package com.smartisanos.sara.bubble.search;


import com.smartisanos.ideapills.common.event.Event;


public class FlashImContactsEvent extends Event {

    public static final String ACTION_SEND_BULLET_MESSAGE_PRE = "ACTION_SEND_BULLET_MESSAGE_PRE";
    public static final String ACTION_SEND_BULLET_MESSAGE = "ACTION_SEND_BULLET_MESSAGE";
    public static final String ACTION_SEND_BULLET_FRAGMENT_VISIBLE_STATE = "ACTION_SEND_BULLET_FRAGMENT_VISIBLE_STATE";
    public static final String ACTION_SEND_BULLET_HIDE_IUPUT_METHOD = "HIDE_IUPUT_METHOD";


    public FlashImContactsEvent(String action) {
        super(action);
    }
}
