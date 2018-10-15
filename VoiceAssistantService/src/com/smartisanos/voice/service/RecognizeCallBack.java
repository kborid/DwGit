package com.smartisanos.voice.service;

public  interface RecognizeCallBack {
    public abstract void startSpeech();

    public abstract void rmsChanged(int paramInt);

    public abstract void partialResult(String result);

    public abstract void results(String[] result, boolean paramBoolean);

    public abstract void error(int errorCode);

    public abstract void endOfSpeech();
}
