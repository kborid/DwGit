package com.smartisanos.sara.voicecommand;

import android.content.ContentResolver;
import android.content.Context;
import android.provider.Settings;

import com.smartisanos.sara.R;
import com.smartisanos.sara.util.LogUtils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ReportTimeCmd extends VoiceCommand {
    public static final String TAG = "ReportTimeCmd";
    private Context mContext;
    CharSequence mReportTimeCmd;
    String mReportString;

    public ReportTimeCmd(Context cxt, VoiceCommand next) {
        super(next);
        mContext = cxt;
        mReportTimeCmd = mContext.getResources().getString(R.string.quick_command_time);
        mReportString = mContext.getResources().getString(R.string.voice_command_report_time_words);
    }

    @Override
    protected int onProcess(CharSequence cmd) {
        if (VoiceCommandUtils.matchCommand(mReportTimeCmd, cmd)) {
            LogUtils.d(TAG, "report time now!");
            VoiceCommandUtils.speak(mContext, getTimeString(), new VoiceCommandUtils.OnSpeakListener() {
                @Override
                public void onSpeakDone(String utteranceId, boolean success) {
                    onFinish(success);
                }
            });

            return FINISH_HANDLING;
        } else {
            return FORWARD;
        }
    }

    String getTimeString() {
        ContentResolver cv = mContext.getContentResolver();
        String strTimeFormat = Settings.System.getString(cv, Settings.System.TIME_12_24);

        boolean is24 = "24".equals(strTimeFormat);
        String format;
        if (is24) {
            format = "HH:mm";
        } else {
            if (Locale.CHINA.equals(Locale.getDefault())) {
                format = "ahh:mm";
            } else {
                format = "hh:mma";
            }
        }

        SimpleDateFormat df = new SimpleDateFormat(format);
        return mReportString + df.format(new Date());
    }
}
