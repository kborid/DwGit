package com.iflytek.speech;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;

import com.iflytek.business.speech.ResourceServiceUtil;
import com.iflytek.business.speech.SpeechError;

import smartisanos.util.LogTag;

/**
 *
 * Created by ltan on 17-11-29.
 */

public class Utils {

    private static final String TAG = "ifly_utils";

    public static boolean switchSpeakerName(ResourceServiceUtil serviceUtil, String newSpeakerName) {
        if(serviceUtil == null) {
            return false;
        }
        boolean succeeded = true;
        Bundle params = new Bundle();
        params.putString(ResourceServiceUtil.KEY_KEY, ResourceServiceUtil.SELECTED_SPEAKER_NAME);
        params.putString(ResourceServiceUtil.KEY_VALUE, newSpeakerName);
        int errorCode = serviceUtil.setParam(params);
        if (SpeechError.SUCCESS != errorCode) {
            LogTag.e(TAG, "switchSpeakerName: errorCode: " + errorCode);
            succeeded = false;
        }
        return succeeded;
    }

    public static boolean isNetAvailable(Context context) {
        ConnectivityManager manager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (manager != null) {
            NetworkInfo[] info = manager.getAllNetworkInfo();
            for (NetworkInfo networkInfo : info) {
                if (networkInfo.getState() == NetworkInfo.State.CONNECTED)
                    return true;
            }
        }
        LogTag.i(TAG, "isNetAvailable: no network found.");
        return false;
    }
}
