package com.smartisanos.ideapills.common.util;


import java.util.Calendar;
import java.util.Formatter;
import java.util.Locale;

import android.content.Context;
import android.text.format.DateUtils;
import android.text.TextUtils;
import android.text.format.DateFormat;

public class TimeUtils {

    public static String buildDetailTime(Context context, long time) {
        StringBuilder builder = new StringBuilder(50);
        Formatter formatter = new Formatter(builder);
        return buildHourMinute(time, context, formatter,
                builder);
    }

    public static String buildHourMinute(long time, Context context, Formatter formatter,
                                         StringBuilder builder) {
        builder.setLength(0);
        String date = formatDateRangeAndLanguage(
                context,
                formatter,
                time,
                DateUtils.FORMAT_SHOW_TIME);
        date = timeCheck(context, date, time, true, false);
        return date;
    }

    public static String formatDateRangeAndLanguage(Context context, Formatter formatter,
                                                    long time, int flags) {
        return DateUtils.formatDateRange(context, formatter, time, time, flags, null)
                .toString();
    }

    /**
     * In some OS, time format is wrong. Remove all space in Chinese and add space in English and 12
     * hour format.
     * @param context
     * @param strTime: string time to be checked.
     * @param time: real time in millisecond
     * @param checkHour: whether check hour and minute format
     * @param checkMonth: whether check month format
     * @return
     */
    private static String timeCheck(Context context,
                                    String strTime,
                                    long time,
                                    boolean checkHour,
                                    boolean checkMonth) {
        if (context == null || strTime == null || time <= 0) {
            return null;
        }

        final int MAX_CHAR = 256;
        StringBuilder sb = new StringBuilder();
        String language = Locale.getDefault().getLanguage();
        strTime = strTime.trim();
        int len = strTime.length();
        if (language.equals("zh")) {
            // Remove all space
            char ch = strTime.charAt(0);
            for (int i = 0; i < len; i++) {
                if (strTime.charAt(i) == ' ') {
                    continue;
                }
                // Check Chinese month
                if (i == 0 && checkMonth) {
                    Calendar cal = Calendar.getInstance();
                    cal.setTimeInMillis(time);
                    int month = cal.get(Calendar.MONTH);
                    sb.append(String.valueOf(month + 1));
                    int doubleMonthChar = 9;
                    if (ch > MAX_CHAR) {
                        doubleMonthChar = 10;
                    }
                    if (month >= doubleMonthChar) {
                        i++;
                    }
                } else {
                    sb.append(strTime.charAt(i));
                }
            }
            String sub = strTime.substring(len - 2, len);
            if (!TextUtils.isEmpty(sub)) {
                String lowerStr = sub.toLowerCase();
                if (lowerStr.equals("am") || lowerStr.equals("pm")) {
                    sb.delete(0, sb.length());
                    sb.append(formatTime(context, strTime));
                }
            }
        } else if (language.equals("en")) {
            if (checkHour) {
                sb.append(formatTime(context, strTime));
            } else {
                sb.append(strTime);
            }
        } else {
            sb.append(strTime);
        }

        return sb.toString();
    }

    private static String formatTime(Context context, String strTime) {
        StringBuilder sb = new StringBuilder();
        int len = strTime.length();

        if (!DateFormat.is24HourFormat(context)) {
            StringBuilder sbTemp = new StringBuilder();
            for (int i = 0; i < len; i++) {
                if (strTime.charAt(i) == ' ') {
                    continue;
                }
                sbTemp.append(strTime.charAt(i));
            }
            // Maybe length of string is changed
            len = sbTemp.length();
            String subStr = sbTemp.substring(0, len - 2);
            String subStrEnd = sbTemp.substring(len - 2, len);
            sb.append(subStr);
            sb.append(" ");
            sb.append(subStrEnd);
        } else {
            sb.append(strTime);
        }

        return sb.toString();
    }
}
