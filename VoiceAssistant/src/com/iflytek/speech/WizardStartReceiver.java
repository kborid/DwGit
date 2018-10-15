package com.iflytek.speech;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.RemoteException;
import android.provider.Settings;

import com.iflytek.business.speech.IResourceServiceListener;
import com.iflytek.business.speech.ResourceServiceUtil;

import com.smartisanos.sara.R;

import smartisanos.api.SettingsSmt;
import smartisanos.util.LogTag;

/**
 * Set the default TTS speaker when SetupWizard started.
 * Created by ltan on 17-11-29.
 */

public class WizardStartReceiver extends BroadcastReceiver {

    private static final String TAG = "WizardStartReceiver";
    private static final int DEFAULT_SPEAKER_INDEX = 5;

    private Context mContext;
    private ResourceServiceUtil mResourceServiceUtil;
    private String mSpeakerName;

    private final IResourceServiceListener mResourceServiceListener = new IResourceServiceListener.Stub() {
        @Override
        public void onEvent(int eventType, final int arg1, int arg2, Bundle bundle) throws RemoteException {
            switch (eventType) {
                case ResourceServiceUtil.EVENT_SERVICE_CONNECTED: {
                    LogTag.d(TAG, "EVENT_SERVICE_CONNECTED, errorCode: " + arg1);
                    if(Utils.switchSpeakerName(mResourceServiceUtil, mSpeakerName)) {
                        Settings.Global.putInt(mContext.getContentResolver(), SettingsSmt.Global.IFLY_SPEAKER_INDEX, DEFAULT_SPEAKER_INDEX);
                    }
                    break;
                }
                case ResourceServiceUtil.EVENT_SERVICE_DISCONNECTED: {
                    LogTag.w(TAG, "EVENT_SERVICE_DISCONNECTED");
                    break;
                }
            }
        }
    };

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null) {
            return;
        }
        mContext = context;
        if("com.smartisanos.START_WIZARD".equals(intent.getAction()) || "com.smartisanos.IFLY_OTA_CHECK".equals(intent.getAction())) {
            int currentSpeakerIndex = Settings.Global.getInt(context.getContentResolver(), SettingsSmt.Global.IFLY_SPEAKER_INDEX,
                                                             0);
            if (currentSpeakerIndex != DEFAULT_SPEAKER_INDEX) {
                mSpeakerName = context.getResources().getStringArray(R.array.ifly_profiles_value)[DEFAULT_SPEAKER_INDEX];
                LogTag.d(TAG, "onReceive: intent:" + intent + ", mSpeakerName: " + mSpeakerName);
                Bundle initParams = new Bundle();
                initParams.putString(ResourceServiceUtil.KEY_ACTIVITY_NAME, WizardStartReceiver.class.getSimpleName());
                initParams.putString(ResourceServiceUtil.KEY_RES_TYPE, ResourceServiceUtil.TTS);
                mResourceServiceUtil = new ResourceServiceUtil(context.getApplicationContext(), initParams,
                                                               mResourceServiceListener);
            }
        }
    }

}
