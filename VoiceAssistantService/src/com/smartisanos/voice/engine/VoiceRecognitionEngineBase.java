package com.smartisanos.voice.engine;

import android.content.Context;
import android.media.AudioManager;

import com.smartisanos.voice.util.LogUtils;

import smartisanos.app.voiceassistant.RecognizeArgsHelper;
import smartisanos.app.voiceassistant.RecognizeResult;

public abstract class VoiceRecognitionEngineBase {

    static final LogUtils log = LogUtils.getInstance(VoiceRecognitionEngineBase.class);

    private boolean mIsInitialized = false;
    protected boolean mUsePcm = false;
    public interface RecognizeListener {
        void onVolumeUpdate(int volume);
        void onResultReceived(RecognizeResult result);
        void onPartialResult(RecognizeResult partial);
        void onBuffer(byte[] arg0);
        void onError(int errorCode);
        void onRecordStart();
        void onRecordEnd();
    }

    protected Context mContext;

    public enum EntranceType {
        TouchPal, BUBBLE, SEARCH, CANDIDATE, VOICE_COMMAND
    }
    protected EntranceType mEntranceType;

    protected RecognizeListener mRecognizeListener;

    protected RecognizeParams mParams;
    protected boolean mIsRecognizing = false;
    protected AudioManager mAudioManager;
    public boolean mTtsPlaying = false;
    protected boolean mEarpieceMode = false;

    public void initEngine(Context context) {
        mContext = context.getApplicationContext();
        mAudioManager = (AudioManager)context.getSystemService(Context.AUDIO_SERVICE);
        mIsInitialized = true;
    }

    public boolean isInitialized() {
        return mIsInitialized;
    }

    public boolean isUsePcm() {
        return mUsePcm;
    }

    public void setRecognizeListener(RecognizeListener recognizeListener) {
        mRecognizeListener = recognizeListener;
    }

    public boolean startRecognize(EntranceType entranceType) {
        return startRecognize(entranceType,
                RecognizeParams.getBaseParams(mContext, RecognizeArgsHelper.BASE_TYPE_BUBBLE));
    }

    public boolean startRecognize(EntranceType entranceType, RecognizeParams params) {
        mEntranceType = entranceType;
        return startRecognize(params);
    }

    public abstract boolean startRecognize(RecognizeParams params);

    public boolean releaseResource() {
        mIsInitialized = false;
        return true;
    }

    public void releaseTts() {
    }

    public void releaseRecognizer() {
    }
    public void destroyService(){
    }
    public boolean isRecognizing() {
        return mIsRecognizing;
    }

    public abstract boolean isTtsPlaying();

    public abstract void playText(String text);

    public abstract void stopPlayText();

    public abstract void endRecognize();
    public abstract void stopRecognize();

    public void setTtsMode(boolean isEarpieceMode){}
}
