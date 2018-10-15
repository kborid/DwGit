package com.smartisanos.ideapills.common.remind;


import android.content.Context;

import com.smartisanos.ideapills.common.R;

import java.util.ArrayList;
import java.util.List;

public final class RemindData {
    private List<BeforeTime> mList = null;

    public RemindData() {
    }

    public List<BeforeTime> getBeforeData(Context context) {
        if (mList == null || mList.isEmpty()) {
            mList = getData(context);
        }
        return mList;
    }

    private List<BeforeTime> getData(Context context) {
        List<BeforeTime> list = new ArrayList<BeforeTime>();
        BeforeTime item = new BeforeTime(BeforeTimeConstant.TIME_IMMEDIATELY, BeforeTimeConstant.TIME_M_IMMEDIATELY, getString(context, R.string.set_notify_real_time));
        list.add(item);

        item = new BeforeTime(BeforeTimeConstant.TIME_5_MIN, BeforeTimeConstant.TIME_M_FOR_5_MIN, getString(context, R.string.set_notify_before_5_min));
        list.add(item);

        item = new BeforeTime(BeforeTimeConstant.TIME_15_MIN, BeforeTimeConstant.TIME_M_FOR_15_MIN, getString(context, R.string.set_notify_before_15_min));
        list.add(item);

        item = new BeforeTime(BeforeTimeConstant.TIME_30_MIN, BeforeTimeConstant.TIME_M_FOR_30_MIN, getString(context, R.string.set_notify_before_30_min));
        list.add(item);

        item = new BeforeTime(BeforeTimeConstant.TIME_1_HOUR, BeforeTimeConstant.TIME_M_FOR_1_HOUR, getString(context, R.string.set_notify_before_1_hour));
        list.add(item);

        item = new BeforeTime(BeforeTimeConstant.TIME_2_HOUR, BeforeTimeConstant.TIME_M_FOR_2_HOUR, getString(context, R.string.set_notify_before_2_hour));
        list.add(item);

        item = new BeforeTime(BeforeTimeConstant.TIME_1_DAY, BeforeTimeConstant.TIME_M_FOR_1_DAY, getString(context, R.string.set_notify_before_1_day));
        list.add(item);

        item = new BeforeTime(BeforeTimeConstant.TIME_2_DAY, BeforeTimeConstant.TIME_M_FOR_2_DAY, getString(context, R.string.set_notify_before_2_day));
        list.add(item);

        item = new BeforeTime(BeforeTimeConstant.TIME_1_WEEK, BeforeTimeConstant.TIME_M_FOR_1_WEEK, getString(context, R.string.set_notify_before_1_week));
        list.add(item);

        return list;
    }

    private String getString(Context context, int resId) {
        return context.getString(resId);
    }

    class BeforeTime {
        long time;
        String text;
        int position;

        BeforeTime(int pos, long time, String text) {
            this.position = pos;
            this.time = time;
            this.text = text;
        }
    }
}
