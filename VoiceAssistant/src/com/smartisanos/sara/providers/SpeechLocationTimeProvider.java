package com.smartisanos.sara.providers;

import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.util.Log;
import com.amap.api.location.AMapLocation;
import com.smartisanos.sara.R;
import com.smartisanos.sara.util.LogUtils;
import com.smartisanos.sara.util.MapUtils;
import com.smartisanos.sara.voicecommand.VoiceCommandUtils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class SpeechLocationTimeProvider extends ContentProvider {
    private static final String TAG = "SpeechLocationTimeProvider";
    public static final String AUTHORITY = "com.smartisanos.sara.speech.misc";
    public static final String REQUEST_LOCATION_ID = "location";
    public static final String REQUEST_TIME_ID = "time";
    private static final long LOCATION_TIMEOUT_DURATION = 20 * 1000L;
    private static final long TIME_TIMEOUT_DURATION = 10 * 1000L;
    private Context mContext;
    public static final String UT_ID_WAIT = "id_wait";
    public static final String UT_ID_LOCATION = "id_location";
    private static final int EVENT_LOCATION_TIMEOUT = 0;
    private static final int EVENT_TIME_TIMEOUT = 1;
    private VoiceCommandUtils.Speaker mSpeaker;
    private boolean mSpeakingLocation, mSpeakingTime;
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case EVENT_TIME_TIMEOUT:
                    mSpeakingTime = false;
                    break;
                case EVENT_LOCATION_TIMEOUT:
                    mSpeakingLocation = false;
                    break;
            }
        }
    };

    @Override
    public boolean onCreate() {
        mContext = getContext();
        return false;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        return null;
    }

    @Override
    public String getType(Uri uri) {
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        return null;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        return 0;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        return 0;
    }

    @Override
    public Bundle call(String method, String arg, Bundle extras) {
        if (method != null) {
            if (REQUEST_LOCATION_ID.equals(method)) {
                if (mSpeakingLocation) {
                    return null;
                }
                mSpeakingLocation = true;

                mSpeaker = new VoiceCommandUtils.Speaker(mContext,
                        new VoiceCommandUtils.OnSpeakListener() {
                            @Override
                            public void onSpeakDone(String utteranceId, boolean success) {
                                if (UT_ID_LOCATION.equals(utteranceId)) {
                                    mSpeakingLocation = false;
                                    mHandler.removeMessages(EVENT_LOCATION_TIMEOUT);
                                    mSpeaker.shutdown();
                                }
                            }
                        }, false);
                MapUtils.getLocation(mContext, MapUtils.LOCATION_MODE_HIGHT_ACCURACY,
                        new MapUtils.OnLocationResult() {
                            @Override
                            public void onLocationResult(AMapLocation amapLocation) {
                                if (amapLocation != null && amapLocation.getErrorCode() == 0) {
                                    String content = mContext.getString(R.string.voice_command_report_location_words)
                                            + "," + amapLocation.getAddress();
                                    mSpeaker.speak(content, UT_ID_LOCATION);
                                    Log.d(TAG, "onLocationResult: \n" + content);
                                } else {
                                    mSpeaker.speak(mContext.getString(R.string.voice_command_location_failed), UT_ID_LOCATION);
                                }
                            }
                        });

                mSpeaker.speak(mContext.getString(R.string.voice_command_location_process), UT_ID_WAIT);
                mHandler.removeMessages(EVENT_LOCATION_TIMEOUT);
                mHandler.sendEmptyMessageDelayed(EVENT_LOCATION_TIMEOUT , LOCATION_TIMEOUT_DURATION);
            } else if (REQUEST_TIME_ID.equals(method)) {
                LogUtils.d(TAG, "report time now! : " + mSpeakingTime);
                if (mSpeakingTime) {
                    return null;
                }
                mSpeakingTime = true;
                VoiceCommandUtils.speak(mContext, getTimeString(), new VoiceCommandUtils.OnSpeakListener() {
                    @Override
                    public void onSpeakDone(String utteranceId, boolean success) {
                        mSpeakingTime = false;
                        mHandler.removeMessages(EVENT_TIME_TIMEOUT);
                    }
                });
                mHandler.removeMessages(EVENT_TIME_TIMEOUT);
                mHandler.sendEmptyMessageDelayed(EVENT_TIME_TIMEOUT , TIME_TIMEOUT_DURATION);
            }
        }
        return null;
    }


    private String getTimeString() {
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
        String reportString = mContext.getResources().getString(R.string.voice_command_report_time_words);
        SimpleDateFormat df = new SimpleDateFormat(format);
        return reportString + df.format(new Date());
    }
}
