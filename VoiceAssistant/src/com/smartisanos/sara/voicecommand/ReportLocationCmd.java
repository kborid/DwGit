package com.smartisanos.sara.voicecommand;

import android.content.Context;
import android.util.Log;

import com.amap.api.location.AMapLocation;
import com.smartisanos.sara.R;
import com.smartisanos.sara.util.MapUtils;

public class ReportLocationCmd extends VoiceCommand {
    public static final String TAG = "ReportLocationCmd";
    public static final String UT_ID_WAIT = "id_wait";
    public static final String UT_ID_LOCATION = "id_location";

    private Context mContext;
    CharSequence mReportLocationCmd;

    private VoiceCommandUtils.Speaker mSpeaker;

    public ReportLocationCmd(Context cxt, VoiceCommand next) {
        super(next);
        mContext = cxt;
        mReportLocationCmd = mContext.getResources().getString(R.string.quick_command_location);
    }

    @Override
    protected int onProcess(CharSequence cmd) {
        if (VoiceCommandUtils.matchCommand(mReportLocationCmd, cmd)) {
            mSpeaker = new VoiceCommandUtils.Speaker(mContext,
                    new VoiceCommandUtils.OnSpeakListener() {
                        @Override
                        public void onSpeakDone(String utteranceId, boolean success) {
                            if (UT_ID_LOCATION.equals(utteranceId)) {
                                onFinish(success);
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
            return FINISH_HANDLING;
        } else {
            return FORWARD;
        }
    }
}
