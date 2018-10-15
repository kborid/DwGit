package com.smartisanos.voice.service;

import java.util.ArrayList;
import java.util.HashMap;

import com.iflytek.business.speech.SpeechError;
import com.smartisanos.voice.engine.RecognizeParams;
import com.smartisanos.voice.engine.VoiceRecognitionEngineBase;
import com.smartisanos.voice.engine.VoiceRecognitionEngineBase.RecognizeListener;
import com.smartisanos.voice.engine.XunfeiRecognizerEngine;
import com.smartisanos.voice.util.LogUtils;

import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.speech.RecognitionService;

import smartisanos.app.voiceassistant.RecognizeArgsHelper;
import smartisanos.app.voiceassistant.RecognizeResult;

public class SpeechService extends RecognitionService {
    static final LogUtils log = LogUtils.getInstance(SpeechService.class);
    protected Object a = new Object();
    private HashMap<RecognitionService.Callback, RecognizeCallBack> mRecognizeCallBack;
    private VoiceRecognitionEngineBase mEngine = null;
    public static int getSystemError(int iflyteckErrorcode) {
        int error;

        switch (iflyteckErrorcode) {
        case SpeechError.ERROR_NO_NETWORK:
            error = android.speech.SpeechRecognizer.ERROR_NETWORK;
            break;
//      case ErrorCode.ERROR_ASR_ERROR_BASE:
//          error = android.speech.SpeechRecognizer.ERROR_SERVER;
//          break;
        case SpeechError.ERROR_PERMISSION_DENIED:
            error = android.speech.SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS;
            break;
        case SpeechError.ERROR_AUDIO_RECORD:
            error = android.speech.SpeechRecognizer.ERROR_AUDIO;
            break;
        case SpeechError.ERROR_MSP_NO_DATA:
        case SpeechError.ERROR_INVALID_RESULT:
        case SpeechError.ERROR_NO_MATCH:
            error = android.speech.SpeechRecognizer.ERROR_NO_MATCH;
            break;
        case SpeechError.ERROR_SPEECH_TIMEOUT:
            error = android.speech.SpeechRecognizer.ERROR_SPEECH_TIMEOUT;
            break;
        case SpeechError.ERROR_AITALK_BUSY:
            error = android.speech.SpeechRecognizer.ERROR_RECOGNIZER_BUSY;
            break;
        case SpeechError.ERROR_INVALID_PARAM:
        case SpeechError.ERROR_PLAY_MEDIA:
        case SpeechError.ERROR_TEXT_OVERFLOW:
        case SpeechError.ERROR_EMPTY_UTTERANCE:
            error = android.speech.SpeechRecognizer.ERROR_CLIENT;
            break;
        default:
            error = iflyteckErrorcode;
            break;
        }

        return error;
    }
    private RecognizeCallBack getRecognizeCallBack(RecognitionService.Callback callback) {
        if (callback == null) {
            return null;
        }
        RecognizeCallBack mTmpCallBack = (RecognizeCallBack) mRecognizeCallBack
                .get(callback);
        if (mTmpCallBack != null) {
            return mTmpCallBack;
        } else {
            RecognizeCallBack mTmpCallBack2 = new RecognizeCallBackImp(this, callback);
            this.mRecognizeCallBack.put(callback, mTmpCallBack2);
            return mTmpCallBack2;
        }

    }

    static Bundle sting2Bundle(String param) {
        Bundle localBundle = new Bundle();
        ArrayList localArrayList = new ArrayList();
        localArrayList.add(param);
        localBundle.putStringArrayList("results_recognition", localArrayList);
        return localBundle;
    }
    public Resources getResources() {
        return super.getResources();
    }

    protected void onCancel(RecognitionService.Callback paramCallback) {
        mEngine.stopRecognize();
    }

    public void onCreate() {
        super.onCreate();
        log.e("SpeechService onCreate");
        mEngine = XunfeiRecognizerEngine.getInstance();
        mRecognizeCallBack = new HashMap<RecognitionService.Callback, RecognizeCallBack>();
    }

    public void onDestroy() {
        log.e("SpeechService onDestroy");
        super.onDestroy();
    }

    public void onStartListening(Intent paramIntent,
            RecognitionService.Callback callback) {
        final RecognizeCallBack localCallBack = getRecognizeCallBack(callback);
        mEngine.setRecognizeListener(new RecognizeListener() {
            @Override
            public void onResultReceived(final RecognizeResult result) {
                localCallBack.results(result.getContents(), false);
            }

            @Override
            public void onRecordStart() {
                localCallBack.startSpeech();
            }

        @Override
            public void onRecordEnd() {
                localCallBack.endOfSpeech();
            }
            @Override
            public void onVolumeUpdate(int volume) {
                 localCallBack.rmsChanged(volume);
            }

            @Override
            public void onPartialResult(RecognizeResult partial) {
                localCallBack.partialResult(partial.getMainContent());

            }

            @Override
            public void onBuffer(byte[] arg0) {
            }
            @Override
            public void onError(int errorCode) {
                localCallBack.error(errorCode);

            }

        });

        mEngine.startRecognize(VoiceRecognitionEngineBase.EntranceType.TouchPal,
                RecognizeParams.getBaseParams(this, RecognizeArgsHelper.BASE_TYPE_BUBBLE)
                        .setPreCallOnRecordStart(false));
    }

    protected void onStopListening(RecognitionService.Callback paramCallback) {
        mEngine.endRecognize();
    }

}
