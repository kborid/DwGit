package com.smartisanos.sara.bubble.search;

public interface IFingerPrintAuthListener {
    void onSupportFailed();

    void onInsecurity();

    void onEnrollFailed();

    void onAuthenticationStart();

    void onAuthenticationError(int errMsgId, CharSequence errString);

    void onAuthenticationFailed();

    void onAuthenticationHelp(int helpMsgId, CharSequence helpString);

    void onAuthenticationSucceeded();
}
