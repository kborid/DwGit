package com.smartisanos.ideapills.common.remind.util;

import android.app.Dialog;
import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.view.Gravity;
import android.view.Window;
import android.view.WindowManager;
import android.view.WindowManagerSmt;

import com.smartisanos.ideapills.common.remind.Time;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import smartisanos.widget.SmartisanDatePickerDialog;
import smartisanos.widget.SmartisanDatePickerEx;
import smartisanos.widget.SmartisanDatePickerExDialog;


public class DateAndTimePickUtil {
    public static final int KEY_OK = 51;
    public static final int BUBBLE_WINDOW_TYPE = WindowManagerSmt.LayoutParamsSmt.TYPE_IDEA_PILLS;


    public static Dialog createDatePickerDialog(final Context context, final Handler handler,
                                                final Time time, boolean useBubbleWindowType) {
        final Calendar calendar = Calendar.getInstance();
        final SmartisanDatePickerDialog.OnDateSetListener callBack = new SmartisanDatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(smartisanos.widget.SmartisanDatePicker view, int year, int monthOfYear,
                                  int dayOfMonth) {
                setDate(handler, time, year, monthOfYear, dayOfMonth);
            }
        };

        SmartisanDatePickerDialog dlg = new SmartisanDatePickerDialog(context, callBack,
                calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH));

        if (useBubbleWindowType) {
            dlg.getWindow().setType(BUBBLE_WINDOW_TYPE);
        }

        dlg.show();
        return dlg;
    }

    public static Dialog createDatePickerExDialog(final Context context, final Handler handler, final Time time) {
        final Calendar calendar = Calendar.getInstance();
        final SmartisanDatePickerExDialog.OnDateSetListener callBack = new SmartisanDatePickerExDialog.OnDateSetListener() {
            @Override
            public void onDateSet(SmartisanDatePickerEx view, int year, int monthOfYear,
                                  int dayOfMonth) {
                setDate(handler, time, year, monthOfYear, dayOfMonth);
            }
        };

        SmartisanDatePickerExDialog dlg = new SmartisanDatePickerExDialog(context, callBack,
                calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH));
        Window window = dlg.getWindow();
        window.setType(BUBBLE_WINDOW_TYPE);
        WindowManager.LayoutParams lp = window.getAttributes();
        lp.gravity = Gravity.CENTER;
        lp.width = WindowManager.LayoutParams.WRAP_CONTENT;
        lp.flags &= ~WindowManager.LayoutParams.FLAG_DIM_BEHIND;
        window.setAttributes(lp);
        dlg.show();
        return dlg;
    }

    private static void setDate(final Handler handler, final Time time, int year, int monthOfYear, int dayOfMonth) {
        int changeJulidanDay = getJulianDay(year, monthOfYear + 1, dayOfMonth);
        Time tmpTime = new Time();
        tmpTime.setJulianDay(changeJulidanDay);
        tmpTime.hour = time.hour;
        tmpTime.minute = time.minute;
        long mills = tmpTime.normalize(true);
        //Send ok message
        Message okMessage = new Message();
        okMessage.what = KEY_OK;
        okMessage.obj = mills;
        handler.sendMessage(okMessage);
    }

    private static int getJulianDay(int year, int month, int day) {
        String initDay = year + "-" + month + "-" + day;
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        long millionSeconds = 0;
        try {
            millionSeconds = sdf.parse(initDay).getTime();
        } catch (ParseException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }//ms
        Time time = new Time();
        time.set(millionSeconds);
        int julian = Time.getJulianDay(millionSeconds, time.gmtoff);
        return julian;
    }
}
