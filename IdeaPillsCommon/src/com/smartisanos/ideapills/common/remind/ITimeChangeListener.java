package com.smartisanos.ideapills.common.remind;

public interface ITimeChangeListener {

    public void selectTime(int julianDay);

    public void scroll(int scrollState);

    public void updateInfos();
}
