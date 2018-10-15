package com.smartisanos.sara.bullet.widget;


import android.view.View;
import android.widget.RelativeLayout;

public interface VoiceRecognizeResultView<T> {
    void init(RelativeLayout layout);

    void resultVoiceRecognize(T result);

    void startVoiceRecognize();

    void shortVoiceRecognize();

    void hideResultDialog();

    boolean isResultViewShown();

    void showTimeOutView();

    interface Select<S> {
        void select(S result, View view);
    }
}
