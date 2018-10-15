package com.smartisanos.ideapills.common.util;

import android.app.SmtPCUtils;
import android.content.Context;
import android.os.Build;
import android.os.ServiceManager;
import android.os.Vibrator;
import android.telephony.SmsMessage;
import android.text.TextUtils;
import android.view.IWindowManager;
import android.view.View;
import android.view.ViewGroup;

import com.smartisanos.ideapills.common.R;
import smartisanos.api.VibratorSmt;
import smartisanos.util.DeviceType;
import smartisanos.util.OverlayUtils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Calendar;
import java.util.Locale;

public class CommonUtils {

    public static boolean isLanguageZhCN(Context context) {
        Locale locale = context.getResources().getConfiguration().locale;
        String country = locale.getCountry();
        String language = locale.getLanguage();
        if (Locale.SIMPLIFIED_CHINESE.getCountry().equals(country)
                && Locale.SIMPLIFIED_CHINESE.getLanguage().equals(language)) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * detail for pm doc.
     *
     * @param context
     * @param time
     * @param isNotify
     * @return
     */
    public static String getNotifyDate(Context context, long time, boolean isNotify) {
        Calendar currentCalendar = Calendar.getInstance();
        currentCalendar.setTimeInMillis(System.currentTimeMillis());

        Calendar setCalendar = Calendar.getInstance();
        setCalendar.setTimeInMillis(time);

        int cYear = currentCalendar.get(Calendar.YEAR);
        int sYear = setCalendar.get(Calendar.YEAR);

        int cMonth = currentCalendar.get(Calendar.MONTH) + 1;
        int sMonth = setCalendar.get(Calendar.MONTH) + 1;

        int sDayOfMonth = setCalendar.get(Calendar.DAY_OF_MONTH);

        int cDayOfWeek = currentCalendar.get(Calendar.DAY_OF_WEEK);

        int cDayOfYear = currentCalendar.get(Calendar.DAY_OF_YEAR);
        int sDayOfYear = setCalendar.get(Calendar.DAY_OF_YEAR);

        int sHour = setCalendar.get(Calendar.HOUR_OF_DAY);
        int sMinute = setCalendar.get(Calendar.MINUTE);

        String dateText = "";
        if (sYear != cYear) {
            int diffYear = sYear - cYear;
            String yearText;
            if (diffYear == 1 || diffYear == -1) {
                yearText = context.getResources().getString(R.string.format_year_month_day);
                dateText = String.format(yearText, String.valueOf(sYear), String.valueOf(sMonth), String.valueOf(sDayOfMonth));
            } else {
                yearText = context.getResources().getString(R.string.format_year);
                dateText = String.format(yearText, String.valueOf(sYear));
            }
        } else {
            String monthText = context.getResources().getString(R.string.format_month_day);
            if (sMonth != cMonth) {
                dateText = String.format(monthText, String.valueOf(sMonth), String.valueOf(sDayOfMonth));
            } else {
                boolean is24HourTime = is24HourTime(context);
                String amText = context.getResources().getString(R.string.set_time_am);
                String pmText = context.getResources().getString(R.string.set_time_pm);

                int diff = sDayOfYear - cDayOfYear;
                if (diff > 6 || diff < 0) { // diff week
                    // pm discuss temp use month-day text show before week date month day
                    dateText = String.format(monthText, String.valueOf(sMonth), String.valueOf(sDayOfMonth));
                } else if (diff == 0) { // Today
                    if (isNotify) {
                        String todayText = context.getResources().getString(R.string.format_today_notify);
                        String amPm = "";
                        if (!is24HourTime) {
                            amPm = sHour < 12 ? amText : pmText;
                            sHour = getTrans12TimeHour(sHour);
                        }
                        dateText = String.format(todayText, String.valueOf(sHour), getMinute(sMinute), amPm);
                    } else {
                        dateText = context.getResources().getString(R.string.format_today);
                    }
                } else if (diff == 1) {
                    if (cDayOfWeek == 1) { // sat and diff is 1 -- > diff week
                        dateText = String.format(monthText, String.valueOf(sMonth), String.valueOf(sDayOfMonth));
                    } else { // TOM
                        if (isNotify) {
                            String tomorrowText = context.getResources().getString(R.string.format_tomorrow_notify);
                            String amPm = "";
                            if (!is24HourTime) {
                                amPm = sHour < 12 ? amText : pmText;
                                sHour = getTrans12TimeHour(sHour);
                            }
                            dateText = String.format(tomorrowText, String.valueOf(sHour), getMinute(sMinute), amPm);
                        } else {
                            dateText = context.getResources().getString(R.string.format_tomorrow);
                        }
                    }
                } else {
                    String weekNumber = null;
                    if (cDayOfWeek == 1) { // sat 0 --> diff week
                        dateText = String.format(monthText, String.valueOf(sMonth), String.valueOf(sDayOfMonth));
                    } else if (cDayOfWeek == 2) { // mon 1
                        weekNumber = getWeek(context, 1 + diff);
                    } else if (cDayOfWeek == 3) { // tue 2
                        weekNumber = getWeek(context, 2 + diff);
                    } else if (cDayOfWeek == 4) { // wed 3
                        weekNumber = getWeek(context, 3 + diff);
                    } else if (cDayOfWeek == 5) { // thu 4
                        weekNumber = getWeek(context, 4 + diff);
                    } else if (cDayOfWeek == 6) { // fri 5
                        weekNumber = getWeek(context, 5 + diff);
                    } else { // sun 6 ==> cDayOfWeek == 7
                        weekNumber = getWeek(context, 6 + diff);
                    }

                    if (TextUtils.isEmpty(dateText)) {
                        if (TextUtils.isEmpty(weekNumber)) {
                            // diff week
                            dateText = String.format(monthText, String.valueOf(sMonth), String.valueOf(sDayOfMonth));
                        } else {
                            // same week
                            String weekText;
                            if (isNotify) {
                                weekText = context.getResources().getString(R.string.format_week_notify);
                                String amPm = "";
                                if (!is24HourTime) {
                                    amPm = sHour < 12 ? amText : pmText;
                                    sHour = getTrans12TimeHour(sHour);
                                }
                                dateText = String.format(weekText, weekNumber, String.valueOf(sHour), getMinute(sMinute), amPm);
                            } else {
                                dateText = context.getResources().getString(R.string.format_week, weekNumber);
                            }
                        }
                    }
                }
            }
        }

        return dateText;
    }

    /**
     * calculate correct hour value for system set 12-hour time
     */
    private static int getTrans12TimeHour(int hour) {
        final int tempHour;
        if (hour < 12) {
            tempHour = hour == 0 ? 12 : hour;
        } else {
            tempHour = hour == 12 ? hour : hour - 12;
        }
        return tempHour;
    }

    public static boolean is24HourTime(Context context) {
        return android.text.format.DateFormat.is24HourFormat(context);
    }

    private static String getWeek(Context context, int t) {
        String str = null;
        switch (t) {
            case 1:
                str = context.getResources().getString(R.string.format_week_1);
                break;
            case 2:
                str = context.getResources().getString(R.string.format_week_2);
                break;
            case 3:
                str = context.getResources().getString(R.string.format_week_3);
                break;
            case 4:
                str = context.getResources().getString(R.string.format_week_4);
                break;
            case 5:
                str = context.getResources().getString(R.string.format_week_5);
                break;
            case 6:
                str = context.getResources().getString(R.string.format_week_6);
                break;
            case 7:
                str = context.getResources().getString(R.string.format_week_7);
                break;
        }
        return str;
    }

    private static String getMinute(int minute) {
        if (minute < 10) {
            return "0" + minute;
        }

        return String.valueOf(minute);
    }

    public static boolean isAndrodSdkUpO() {
        return Build.VERSION.SDK_INT >= 26;
    }

    public static void dismissKeyguard() {
        if (isAndrodSdkUpO()) {
            keyguardWaitingForActivityDrawn();
        } else {
            IWindowManager windowManager = IWindowManager.Stub.asInterface(ServiceManager.getService(Context.WINDOW_SERVICE));
            try {
                Class clazz = windowManager.getClass();
                Method[] methods = clazz.getMethods();
                for (Method method : methods) {
                    if (method != null && "dismissKeyguard".equals(method.getName())) {
                        method.invoke(windowManager);
                        return;
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static void keyguardWaitingForActivityDrawn() {
        try {
            OverlayUtils.ActivityManagerSmt.keyguardWaitingForActivityDrawn();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static int getStringLength(String source) {
        int len = 0;
        try {
            if (!TextUtils.isEmpty(source)) {
                int[] r = SmsMessage.calculateLength(source.replaceAll("\n", ""), false);
                if (r != null && r.length > 1) {
                    len = r[1];
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return len;
    }

    public static void setAlwaysCanAcceptDrag(View view, boolean can) {
        // NA
        try {
            Method setAlwaysCanAcceptDrag = view.getClass().getMethod("setAlwaysCanAcceptDrag", boolean.class);
            try {
                setAlwaysCanAcceptDrag.invoke(view, can);
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            }
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
    }

    public static void setAlwaysCanAcceptDragForAll(View view, boolean can) {
        setAlwaysCanAcceptDrag(view, can);
        if (view instanceof ViewGroup) {
            ViewGroup vg = (ViewGroup) view;
            for (int i = 0; i < vg.getChildCount(); ++i) {
                setAlwaysCanAcceptDragForAll(vg.getChildAt(i), can);
            }
        }
    }

    public static boolean shouldScaleWaveform() {
        return DeviceType.isOneOf(DeviceType.TRIDENT, DeviceType.OCEAN);
    }

    public static void vibrateEffect(Context context, int effect) {
        if (!SmtPCUtils.isValidExtDisplayId(context)) {
            Vibrator vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
            VibratorSmt.vibrateEffect(vibrator, effect);
        }
    }
}
