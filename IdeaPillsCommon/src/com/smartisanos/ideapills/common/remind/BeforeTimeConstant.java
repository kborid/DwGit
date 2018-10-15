package com.smartisanos.ideapills.common.remind;


public interface BeforeTimeConstant {
    int TIME_IMMEDIATELY = 0;
    int TIME_5_MIN = 1;
    int TIME_15_MIN = 2;
    int TIME_30_MIN = 3;
    int TIME_1_HOUR = 4;
    int TIME_2_HOUR = 5;
    int TIME_1_DAY = 6;
    int TIME_2_DAY = 7;
    int TIME_1_WEEK = 8;


    long TIME_M_IMMEDIATELY = 0L;
    long TIME_M_FOR_5_MIN = 300000L; // 5*60*1000L
    long TIME_M_FOR_15_MIN = 900000L;
    long TIME_M_FOR_30_MIN = 1800000L;
    long TIME_M_FOR_1_HOUR = 3600000L;
    long TIME_M_FOR_2_HOUR = 7200000L;
    long TIME_M_FOR_1_DAY = 86400000L;
    long TIME_M_FOR_2_DAY = 172800000L;
    long TIME_M_FOR_1_WEEK = 604800000L;
}
