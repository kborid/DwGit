package com.smartisanos.ideapills.common.event.utils;

public class Precondition {

    public static <T> T checkNotNull(final T reference) {
        if (reference == null) {
            throw new NullPointerException();
        }
        return reference;
    }
}
