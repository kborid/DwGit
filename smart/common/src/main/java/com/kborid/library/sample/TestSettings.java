package com.kborid.library.sample;

import java.util.ArrayList;

public enum TestSettings {
    instance;

    private int mFlag = 0;
    private static final int FLAG_ONE = 0x0001;
    private static final int FLAG_TWO = 0x0002;
    private static final int FLAG_THR = 0x0004;

    private ArrayList<Integer> testList = new ArrayList<>();

    TestSettings(){
        if (null != testList){
            testList.clear();
            testList.add(FLAG_ONE);
            testList.add(FLAG_TWO);
            testList.add(FLAG_THR);
        }
    }

    public ArrayList<Integer> getTestList() {
        return testList;
    }

    public void setSettingOne(boolean one){
        if (one){
            mFlag |= FLAG_ONE;
        } else {
            mFlag &= ~FLAG_ONE;
        }
    }

    public void setSettingTwo(boolean two){
        if (two){
            mFlag |= FLAG_TWO;
        } else {
            mFlag &= ~FLAG_TWO;
        }
    }

    public void setSettingThr(boolean thr){
        if (thr){
            mFlag |= FLAG_THR;
        } else {
            mFlag &= ~FLAG_THR;
        }
    }

    public int getFlag(){
        System.out.println("current Flag = " + mFlag);
        return mFlag;
    }

    public boolean isFlagOne(){
        return (mFlag & FLAG_ONE) == FLAG_ONE;
    }

    public boolean isFlagTwo(){
        return (mFlag & FLAG_TWO) == FLAG_TWO;
    }

    public boolean isFlagThr(){
        return (mFlag & FLAG_THR) == FLAG_THR;
    }

    public void reset() {
        mFlag = 0;
    }
}
