package com.smartisanos.sara.util;

public class RecognizeHelper {

    private static RecognizeHelper sInstatnce = null;
    private boolean mIsAllowToRecognize = true;
    private long PhoneStatusStatusChangeTime = 0;
    private String mNewPhoneState;
    private RecognizeHelper() {

    }

    public synchronized static RecognizeHelper getInstance() {
        if (sInstatnce == null) {
            sInstatnce = new RecognizeHelper();
        }

        return sInstatnce;
    }

    public boolean isAllowedToRecognize() {
        return mIsAllowToRecognize;
    }

    public void setAllowedToRecognize(boolean isAllowToRecognize) {
        mIsAllowToRecognize = isAllowToRecognize;
    }

    public void setPhoneStatusChangeTime(long changTime){
        PhoneStatusStatusChangeTime = changTime;
    }

    public long getPhoneStatusChangeTime(){
        return PhoneStatusStatusChangeTime;
    }

    public String getPhoneStatus(){
        return mNewPhoneState;
    }
    public void setPhoneStatus(String state){
        mNewPhoneState = state;
    }
}
