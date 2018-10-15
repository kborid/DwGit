package com.smartisanos.voice.service;

import android.os.Bundle;
import android.os.RemoteException;
import android.speech.RecognitionService;
import android.text.TextUtils;

public class RecognizeCallBackImp implements RecognizeCallBack {

    private RecognitionService.Callback mCallBack;
    private String mResultContent = "";

    public RecognizeCallBackImp(SpeechService speechService,
            RecognitionService.Callback paramCallback) {
        mCallBack = paramCallback;
    }

    public void startSpeech() {
        if (mCallBack != null) {
            try {
                mCallBack.readyForSpeech(null);
                mCallBack.beginningOfSpeech();
                return;
            } catch (RemoteException localRemoteException) {
                localRemoteException.printStackTrace();
            }
        }
    }

    public void rmsChanged(int paramInt) {
        if (mCallBack != null) {
            try {
                mCallBack.rmsChanged(paramInt);
                return;
            } catch (RemoteException localRemoteException) {
                localRemoteException.printStackTrace();
            }
        }
    }

    public void partialResult(String result) {
        if (mCallBack != null) {
            if (!TextUtils.isEmpty(result)) {
                mResultContent += result;
            }

            try {
                Bundle localBundle = SpeechService.sting2Bundle(mResultContent);
                localBundle.putString("action_type", "recognize");

                mCallBack.partialResults(localBundle);
            } catch (RemoteException localRemoteException) {
                localRemoteException.printStackTrace();
            }

        }
    }

    public void results(String[] result, boolean paramBoolean) {
        if (mCallBack != null) {
            String text = (result == null || TextUtils.isEmpty(result[0])) ? ""
                    : result[0];

            if (!TextUtils.isEmpty(text)) {
                mResultContent += text;
            }
            try {
                Bundle localBundle2 = SpeechService.sting2Bundle(mResultContent);
                localBundle2.putString("action_type", "recognize");
                mCallBack.results(localBundle2);
                return;

            } catch (RemoteException localRemoteException) {
                localRemoteException.printStackTrace();
            }
        }
    }

    public void error(int errorCode) {
        int i = SpeechService.getSystemError(errorCode);
        if (mCallBack != null) {
            try {
                mCallBack.error(i);
                return;
            } catch (RemoteException localRemoteException) {
                localRemoteException.printStackTrace();
            }
        }
    }

    public void endOfSpeech() {
        if (mCallBack != null) {
            try {
                mCallBack.endOfSpeech();
                return;
            } catch (RemoteException localRemoteException) {
                localRemoteException.printStackTrace();
            }
        }
    }

}
