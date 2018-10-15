package com.smartisanos.sara;

import java.util.Calendar;
import com.smartisanos.sara.util.LogUtils;
import com.smartisanos.sara.util.SaraConstant;
import com.smartisanos.sara.util.SharePrefUtil;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
public class TimeChangeReceiver extends BroadcastReceiver {
    private String TAG = "TimeChangeReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        LogUtils.e(TAG, "onReceive");
        if (intent == null) {
            return;
        }
        if (SaraConstant.ACTION_UPDATE_DATE.equals(intent.getAction())) {
            SharePrefUtil.updateSharePrefByTime(getCurrentDay());
        } else if (Intent.ACTION_TIME_CHANGED.equals(intent.getAction())
                || Intent.ACTION_TIMEZONE_CHANGED.equals(intent.getAction())) {
            SharePrefUtil.updateSharePrefByTime(getCurrentDay());
            setDateChangeIntent();
        }
    }

    private static int getCurrentDay() {
        Calendar calendar = Calendar.getInstance();
        long currentMillis = System.currentTimeMillis();
        calendar.setTimeInMillis(currentMillis);
        return calendar.get(Calendar.DAY_OF_MONTH);
    }

    public void setDateChangeIntent() {
        SaraApplication mSaraApplication = SaraApplication.getInstance();
        AlarmManager alarmManager = (AlarmManager) mSaraApplication.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager != null) {
            Intent updateDateIntent = new Intent(SaraConstant.ACTION_UPDATE_DATE);
            PendingIntent pendingIntent = PendingIntent.getBroadcast(mSaraApplication, 0, updateDateIntent, PendingIntent.FLAG_CANCEL_CURRENT);
            alarmManager.set(AlarmManager.RTC_WAKEUP, getTriggerTime(), pendingIntent);
        }
    }

    private long getTriggerTime() {
        Calendar calendar = Calendar.getInstance();
        long currentMillis = System.currentTimeMillis();
        calendar.setTimeInMillis(currentMillis);
        int day = calendar.get(Calendar.DAY_OF_MONTH);
        int month = calendar.get(Calendar.MONTH);
        int year = calendar.get(Calendar.YEAR);

        Calendar nc = Calendar.getInstance();
        nc.set(Calendar.YEAR, year);
        nc.set(Calendar.MONTH, month);
        nc.set(Calendar.DAY_OF_MONTH, day + 1);
        nc.set(Calendar.HOUR_OF_DAY, 0);
        nc.set(Calendar.MINUTE, 0);
        nc.set(Calendar.SECOND, 0);
        return nc.getTimeInMillis();
    }
}