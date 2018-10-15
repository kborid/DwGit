
package com.smartisanos.voice.engine;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

import com.iflytek.business.speech.RecognitionListener;
import com.iflytek.business.speech.RecognizerResult;
import com.iflytek.business.speech.SpeechError;
import com.iflytek.business.speech.SpeechIntent;
import com.iflytek.business.speech.SpeechServiceUtil;
import com.iflytek.business.speech.SpeechServiceUtil.ISpeechInitListener;
import com.iflytek.business.speech.SynthesizerListener;
import com.iflytek.business.speech.TextToSpeech;
import com.smartisanos.voice.recoder.PcmRecorder;
import com.smartisanos.voice.recoder.RecordListener;
import com.smartisanos.voice.util.StringUtils;
import com.smartisanos.voice.util.VoiceConstant;
import com.smartisanos.voice.util.VoiceUtils;
import com.smartisanos.voice.util.XmlParser;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.os.RemoteException;
import android.os.SystemClock;
import android.provider.Settings;
import android.text.TextUtils;

import smartisanos.api.SettingsSmt;
import smartisanos.app.voiceassistant.RecognizeResult;

public class XunfeiRecognizerEngine extends VoiceRecognitionEngineBase {

    private static final int MSG_CHANGE_SKIP_CALLBACK_STATE = 0;

    private static final int MSG_BEGIN_OF_SPEECH = 1;

    private static final int MSG_BUFFER_RECEIVERD = 2;

    private static final int MSG_END_OF_SPEECH = 3;

    private static final int MSG_ERROR = 4;

    private static final int MSG_RESULTS = 5;

    private static final int MSG_BEGINNING_OF_RECORD = 6;

    private static final int MSG_END_OF_RECORD = 7;
    private static final int MSG_BUFFERS = 8;
    private static final int MSG_PARTIAL_RESULT  = 9;

    private static final int MSG_START_WRITE_AUDIO = 10;
    private static final int MSG_CANCEL_WRITE_AUDIO  = 11;
    private static final int ERROR_CODE_STOP  = -2;

    private String mCachedPartialString = null;
    private byte[] mBuffers = null;
    public static final String LOW_POWER_MODE = "low_power";
    public static final String FEATURE_PHONE_MODE = "feature_phone_mode";

    private static final String TAG = "VoiceAss.VoiceRecognizerEngine";

    private static volatile XunfeiRecognizerEngine sInstance;

    public static XunfeiRecognizerEngine getInstance() {
        if (sInstance == null) {
            synchronized (XunfeiRecognizerEngine.class) {
                if (sInstance == null) {
                    sInstance = new XunfeiRecognizerEngine();
                }
            }
        }
        return sInstance;
    }

    private SpeechServiceUtil mService;
    private boolean mSpeechInited = false;
    private final Object mStopLock = new Object(); // 处理强制停止上次识别的锁对象
    private volatile boolean mRecognizeStarted = false; // 真正请求到讯飞进行识别时才置的状态
    private boolean mRecognitionEngineInited = false;
    private boolean mCheckRecognitionStarted = false;
    private Handler mMsgReciver = null;
    private ISpeechInitListener mInitListener = new ISpeechInitListener() {
        @Override
        public void onSpeechInit(int code) {
            mSpeechInited = true;
            log.infoRelease("onSpeechInit code:" + code);
            if (code == SpeechError.SUCCESS) {
                log.e(mService + "onInit is success.");
                synchronized (XunfeiRecognizerEngine.this) {
                    // if the createRecognizer is success, the mRecognizer must not be null
                    if (mService != null) {
                        checkRecognitionEngine();
                        mService.initSynthesizerEngine(mTtsListener, getTtsParam());
                    }
                }
            } else {
                log.e("onSpeechInit error: " + code);
            }
        }

        @Override
        public void onSpeechUninit() {
            log.e("onSpeechUninit destroyService");
            destroyService();
            if (!VoiceConstant.CTA_ENABLE) {
                initEngine(mContext);
            }
        }
    };

    private TotalRecognizeListener mTotalRecognizerListener = new TotalRecognizeListener();

    private class TotalRecognizeListener extends RecognitionListener.Stub {

        @Override
        public void onVolumeGet(int v) throws RemoteException {
            if (mUsePcm){
                return;
            }
            log.i("onVolumeGet | " + v);
            Message message = mMsgReciver.obtainMessage(MSG_BUFFER_RECEIVERD);
            message.arg1 = v;
            mMsgReciver.sendMessageDelayed(message, 0);
        }

        @Override
        public void onSpeechStart() throws RemoteException {
            log.d("onSpeechStart");
        }

        @Override
        public void onSpeechEnd() throws RemoteException {
            log.d("onSpeechEnd");
        }

        @Override
        public void onResult(RecognizerResult result) throws RemoteException {
            log.d("onResult: " + result);
            if (null != result) {
                RecognizeResult parsed = XmlParser.parseNluResult(result.mXmlDoc, mParams.isEnableCandidate());
                if (mParams.isEnableAsr() && !parsed.isOffline()) {
                    parsed.setItems(new RecognizeResult.Item(
                            StringUtils.getStringOrEmpty(mCachedPartialString)
                                    + StringUtils.getStringOrEmpty(parsed.getMainContent())));
                }
                mMsgReciver.sendMessageDelayed(mMsgReciver.obtainMessage(MSG_RESULTS, parsed), 0);
            } else {
                log.d("onResult is ERROR ");
            }
        }

        @Override
        public void onRecordStart() throws RemoteException {
            mCachedPartialString = null;
            if (mParams.isPreCallOnRecordStart()) {
                return;
            }
            log.infoRelease("onRecordStart:");
            mMsgReciver.sendMessageDelayed(mMsgReciver.obtainMessage(MSG_BEGINNING_OF_RECORD), 0);
        }

        @Override
        public void onRecordEnd() throws RemoteException {
            log.d("onRecordEnd:");
            mMsgReciver.sendMessageDelayed(mMsgReciver.obtainMessage(MSG_END_OF_RECORD), 0);
            postCancelWriteAudio(0);
        }

        @Override
        public void onInit(int resultCode) throws RemoteException {
            log.infoRelease("onInit | resultCode = " + resultCode);
            mRecognitionEngineInited = true;
            mCheckRecognitionStarted = false;
        }

        @Override
        public void onError(int errorCode) {
            log.w("errorCode :" + errorCode + ", mEntranceType=" + mEntranceType);
            postCancelWriteAudio(0);
            if (mEntranceType == EntranceType.TouchPal){
                Message message = mMsgReciver.obtainMessage(MSG_ERROR);
                message.arg1 = errorCode;
                mMsgReciver.sendMessageDelayed(message, 0);
            } else {
                if (mEntranceType == EntranceType.BUBBLE) {
                    Message message = mMsgReciver.obtainMessage(MSG_ERROR);
                    message.arg1 = errorCode;
                    mMsgReciver.sendMessageDelayed(message, 0);
                }
                if (mUsePcm) {
                    return;
                }
                if (errorCode == SpeechError.ERROR_NO_MATCH) {
                    mMsgReciver.sendMessageDelayed(mMsgReciver.obtainMessage(MSG_RESULTS), 0);
                } else if (errorCode == SpeechError.ERROR_AUDIO_RECORD) {
                    mContext.sendBroadcast(new Intent(VoiceConstant.ACTION_RECORD_ERROR));
                } else if (errorCode == SpeechError.ERROR_AITALK_RES_NOTFOUND) {
                    Intent intent = new Intent();
                    intent.setAction(VoiceConstant.ACTION_PACKAGE_DATA_CLEARED);
                    intent.setData(Uri.parse(VoiceConstant.URI_IFLYTEK_SPEECHCLOUD));
                    mContext.sendBroadcast(intent);
                } else if (errorCode == SpeechError.ERROR_MSP_NO_DATA
                        || errorCode == SpeechError.ERROR_INTERRUPT) {
                    if (mEntranceType != EntranceType.BUBBLE) {
                        mMsgReciver.sendMessageDelayed(mMsgReciver.obtainMessage(MSG_RESULTS), 0);
                    }
                } else if (errorCode == SpeechError.ERROR_MSP_BOS_TIMEOUT){
                    mMsgReciver.sendMessageDelayed(mMsgReciver.obtainMessage(MSG_RESULTS), 0);
                } else if (mEntranceType != EntranceType.BUBBLE) {
                    mMsgReciver.sendMessageDelayed(mMsgReciver.obtainMessage(MSG_RESULTS), 0);
                }
            }

        }

        @Override
        public void onPartialResult(RecognizerResult result) throws RemoteException {
            log.d("onPartialResult:" + result);

            if (null != result) {
                RecognizeResult parsed = XmlParser.parseNluResult(result.mXmlDoc, mParams.isEnableCandidate());
                String text = parsed.getMainContent();
                if (!mParams.isEnableAsr()) { // asr识别不支持部分结果，仅语音听写支持返回部分结果
                    mMsgReciver.sendMessageDelayed(mMsgReciver.obtainMessage(MSG_PARTIAL_RESULT, parsed), 0);
                } else {
                    // cache it and return it when onResult.
                    mCachedPartialString = StringUtils.getStringOrEmpty(mCachedPartialString) + StringUtils.getStringOrEmpty(text);
                }
            } else {
                log.d("onPartialResult is ERROR ");
            }
        }

        @Override
        public void onEnd(Intent arg0) throws RemoteException {
            log.d("----onEnd---");
            mIsRecognizing = false;
            mRecognizeStarted = false;
            synchronized (mStopLock) {
                mStopLock.notifyAll();
            }
        }

        @Override
        public void onGrammarResult(int type, String lexiconname, int errorCode) throws RemoteException {
            log.d("onGrammarResult | type = " + type + ", lexiconname = " + lexiconname + ", errorCode = " + errorCode);

            if (mLexiconCbk != null) {
                mLexiconCbk.onResult(lexiconname, errorCode);
                mLexiconCbk = null;
            }
        }

        @Override
        public void onBuffer(byte[] arg0) throws RemoteException {
            log.d("----onBuffer--- length:" + arg0.length);
            if (!mUsePcm){
                mBuffers = arg0;
                mMsgReciver.sendMessage(mMsgReciver.obtainMessage(MSG_BUFFERS));
            }
        }
    }

    private void BuildHandler(){
        if(mMsgReciver == null){
            mMsgReciver = new Handler(mContext.getMainLooper()) {
                boolean skipCallback = false;
            @Override
            public void handleMessage(Message msg) {
                if (msg.what == MSG_CHANGE_SKIP_CALLBACK_STATE) {
                    skipCallback = (Boolean) msg.obj;
                }

                if (skipCallback) { // 略过回调，因为当前处于stopping的状态，不应该回调任何事件
                    return;
                }

                switch (msg.what) {
                    case MSG_BEGIN_OF_SPEECH:
                        break;
                    case MSG_BUFFER_RECEIVERD:
                        if (mIsRecognizing && mRecognizeListener != null) {
                            mRecognizeListener.onVolumeUpdate(msg.arg1);
                        }
                        break;
                    case MSG_END_OF_SPEECH:
                        break;
                    case MSG_ERROR:
                        if (mRecognizeListener != null) {
                            mRecognizeListener.onError(msg.arg1);
                        }
                        break;
                    case MSG_RESULTS:
                        onResultsMsg((RecognizeResult) msg.obj);
                        break;
                    case MSG_BEGINNING_OF_RECORD:
                        if (mRecognizeListener != null) {
                            mRecognizeListener.onRecordStart();
                        }
                        break;
                    case MSG_END_OF_RECORD:
                     if (mRecognizeListener != null) {
                         mRecognizeListener.onRecordEnd();
                     }
                        break;
                    case MSG_BUFFERS:
                        if (mIsRecognizing && mRecognizeListener != null) {
                            mRecognizeListener.onBuffer(mBuffers);
                        }
                        break;
                    case MSG_PARTIAL_RESULT:
                        if (mRecognizeListener != null) {
                            RecognizeResult partial = (RecognizeResult) msg.obj;
                            if (partial != null && partial.isCandidate()) {
                                appendString(TextUtils.join(":", partial.getContents()));
                            }
                            mRecognizeListener.onPartialResult(partial);
                        }
                        break;
                }
            }
        };
        }
    }

    @Override
    public synchronized void initEngine(Context context) {
        log.d("initEngine is enter");
        super.initEngine(context);
        if (mGrammarManager == null) {
            mGrammarManager = new GrammarManager(mContext, mLexiconUpdater);
        }
        if (mService == null) {
            Intent serviceIntent = new Intent();
            serviceIntent.putExtra(SpeechIntent.SERVICE_LOG_ENABLE, true);
            serviceIntent.putExtra(SpeechIntent.EXT_APPID, "52dd0419");
            mService = new SpeechServiceUtil(context, mInitListener, serviceIntent);
            log.e(mService + " createRecognizer is called" + mInitListener);
        }
        BuildHandler();
    }

    boolean checkRecognitionEngine() {
        if (!mRecognitionEngineInited) {
            if (!mCheckRecognitionStarted &&
                    mGrammarManager != null && mGrammarManager.isInited() &&
                    mSpeechInited && mService != null) {
                mCheckRecognitionStarted = true;
                mService.initRecognitionEngine(mTotalRecognizerListener, getRecInitParams());
            }
            return false;
        }

        return true;
    }

    public void addHotWords(ArrayList<String> words) {
        mGrammarManager.updateLexicon(GrammarManager.LEXICON_HOT_WORD, words);
    }

    public void updateLexicon(String lexicon) {
        log.infoRelease("updateLexicon : " + lexicon);
        mGrammarManager.updateLexicon(lexicon);
    }

    public void updateLexicon(String lexicon, List<String> words) {
        log.infoRelease("updateLexicon : " + lexicon);
        mGrammarManager.updateLexicon(lexicon, words);
    }

    private GrammarManager mGrammarManager;
    private GrammarManager.LexiconUpdateResult mLexiconCbk;
    private GrammarManager.LexiconUpdater mLexiconUpdater = new GrammarManager.LexiconUpdater() {
        @Override
        public void onUpdate(String lexicon, String content, GrammarManager.LexiconUpdateResult cbk) {
            if (TextUtils.isEmpty(lexicon) || TextUtils.isEmpty(content) || cbk == null) {
                throw new RuntimeException("error params");
            }

            Intent intent = new Intent();
            intent.putExtra(SpeechIntent.EXT_LEXICON_NAME, lexicon);
            if (GrammarManager.LEXICON_HOT_WORD.equals(lexicon)) {
                intent.putExtra(SpeechIntent.EXT_ENGINE_TYPE, SpeechIntent.ENGINE_WEB);
                intent.putExtra(SpeechIntent.EXT_LEXICON_ITEM, content);
            } else {
                intent.putExtra(SpeechIntent.EXT_GRAMMARS_FLUSH, true);
                intent.putExtra(SpeechIntent.EXT_GRAMMARS_PREBUILD, 0);
                intent.putExtra(SpeechIntent.EXT_ENGINE_TYPE, SpeechIntent.ENGINE_LOCAL);
                intent.putExtra(SpeechIntent.EXT_LEXICON_ITEM, content.split("\\|"));
                intent.putExtra(SpeechIntent.ARG_RES_TYPE, SpeechIntent.RES_FROM_CLIENT);
                intent.putExtra(SpeechIntent.EXT_GRAMMARS_FILES, mGrammarManager.getGrammarFiles(lexicon));
                intent.putExtra(SpeechIntent.EXT_GRAMMARS_NAMES, mGrammarManager.getScenes(lexicon));
            }

            if(!checkRecognitionEngine() || mIsRecognizing || mService == null) {
                cbk.onResult(lexicon,-1);
            } else {
                mLexiconCbk = cbk;
                mService.updateLexicon(intent);
            }
        }
    };

    private SynthesizerListener.Stub mTtsListener = new SynthesizerListener.Stub() {

        @Override
        public void onInit(int arg0) throws RemoteException {
        }

        @Override
        public void onInterruptedCallback() throws RemoteException {
        }

        @Override
        public void onPlayBeginCallBack() throws RemoteException {
        }

        @Override
        public void onPlayCompletedCallBack(int arg0) throws RemoteException {
            mContext.sendBroadcast(new Intent(VoiceConstant.ACTION_TTS_FINISH));
        }

        @Override
        public void onProgressCallBack(int arg0) throws RemoteException {
        }

        @Override
        public void onSpeakPaused() throws RemoteException {
        }

        @Override
        public void onSpeakResumed() throws RemoteException {
        }

        public void onEvent(int arg0, int arg1, int arg2, Bundle arg3) throws RemoteException {
        }
    };

    @Override
    public boolean startRecognize(RecognizeParams params) {
        if (mService == null || !checkRecognitionEngine()) {
            log.e("startRecognize but engine not ready! mService=" + mService);
            return false;
        }

        if (params == null) {
            log.e("startRecognize but params is null!");
            return false;
        }

        if (mIsRecognizing) {
            if (params.hasSourcePcm()) {
                log.w("startRecognize with pcm return for other recognize are started.");
                return false;
            } else {
                stopRecognize();
            }
        }

        if (params.getRequestThreshold() > 0) { // 异步处理识别
            postStartWriteAudio(params.getRequestThreshold(), params);
        } else { // 直接请求识别
            if (!startRecognizeInternal(params)) {
                return false;
            }
        }

        if (params.isPreCallOnRecordStart() && mRecognizeListener != null) {
            mRecognizeListener.onRecordStart();
        }

        mIsRecognizing = true;
        return true;
    }

    private boolean startRecognizeInternal(RecognizeParams params) {
        boolean usePcm = params.hasSourcePcm();
        Intent intent = new Intent();
        intent.putExtra(SpeechIntent.EXT_ENGINE_TYPE, params.getEngineType());
        intent.putExtra(SpeechIntent.EXT_VAD_FRONT_TIME, "" + params.getVadFrontTime());
        intent.putExtra(SpeechIntent.EXT_VAD_END_TIME, "" + params.getVadEndTime());
        intent.putExtra(SpeechIntent.SESSION_TIMEOUT, params.getSessionTimeout());
        intent.putExtra(SpeechIntent.EXT_DEST_PCM, params.getDstPcm());
        intent.putExtra(SpeechIntent.EXT_PCM_LOG, true);
        intent.putExtra(SpeechIntent.EXT_RECORD_CALLBACK_ENABLE, true);

        // 可选参数设置
        LinkedHashSet<String> otherParams = new LinkedHashSet<String>();
        if (usePcm) {
            intent.putExtra(SpeechIntent.EXT_SOURCE_PCM, params.getSourcePcm());
        }

        if (params.getRequestThreshold() > 0) {
            intent.putExtra(SpeechIntent.EXT_RECAUDIOSOURCE, -1); // 设定引擎不录音，由我们通过writeaudio写入
        }

        if (!params.isRequestAudioFocus() || usePcm) {
            otherParams.add("request_audio_focus=false");
        }

        if (!params.isInterruptTts() || usePcm) {
            otherParams.add("tts_interrupt=false");
        }

        if (params.isEnableAsr()) { // 语音识别
            // 得到识别场景scenes
            ArrayList<String> scenes = new ArrayList<String>();
            if (params.hasScenes()) {
                for (String scene : params.getScenes()) {
                    String userScene = mGrammarManager.getUserScene(scene);
                    if (!TextUtils.isEmpty(userScene)) {
                        scenes.add(userScene);
                    } else if (mGrammarManager.isPreloadScene(scene)) {
                        scenes.add(scene);
                    }
                }
                scenes.add(SpeechIntent.LOCAL_SMS_DICTATION);
            } else {
                scenes.add(GrammarManager.SCENE_SEARCH);
            }

            intent.putExtra(SpeechIntent.EXT_LOCAL_SCENE, TextUtils.join(";", scenes));
            intent.putExtra(SpeechIntent.EXT_LOCAL_PRIOR, "1"); // 设定本地优先
            intent.putExtra(SpeechIntent.EXT_LOCAL_RESULT_SCORE, params.getLocalResultScore()); // 设定本地得分门限
            intent.putExtra(SpeechIntent.EXT_MIXED_TIMEOUT, 500); // 设定混合模式时的互相等待的超时，不设置时是默认值2000
            otherParams.add("mixed_threshold=" + params.getMixResultScore()); // 设定混合得分门限

            intent.putExtra(SpeechIntent.EXT_ASR_SCH, "0"); // 暂时在线识别使用语音听写拿纯文字信息，后续切换AIUI
        } else { // 语音听写
            intent.putExtra(SpeechIntent.EXT_ASR_SCH, "0");
            intent.putExtra(SpeechIntent.EXT_LOCAL_PRIOR, "0");
            intent.putExtra(SpeechIntent.EXT_RESULT_TYPE, "xml");
            intent.putExtra(SpeechIntent.EXT_VAD_SPEECH_TIME, 60000);
            intent.putExtra(SpeechIntent.EXT_LOCAL_SCENE, SpeechIntent.LOCAL_SMS_DICTATION); // 离线识别需要用到的本地词典
            intent.putExtra(SpeechIntent.EXT_LOCAL_RESULT_SCORE, 0); // 离线识别的得分门限设为0，保证无网络时也至少有结果
            otherParams.add(VoiceUtils.getSelectedLanguage(mContext)); // 设置识别语言
            if (params.isEnableDynamicFix()) { // 启用动态修正
                intent.putExtra("dwa", "wpgs");
            }

            if (params.isEnableCandidate()) { // 启用多候选
                intent.putExtra("nbest", 3);
                mPartialStrs = new StringBuffer[]{new StringBuffer(), new StringBuffer(), new StringBuffer()};
            }
        }

        intent.putExtra(SpeechIntent.EXT_REC_PARAMS, TextUtils.join(",", otherParams));

        if (mService != null) {
            mService.startRecognize(intent);
            mParams = params;
            mUsePcm = usePcm;
            mRecognizeStarted = true;
            log.infoRelease("startRecognizeInternal: " + params);
            return true;
        } else {
            log.infoRelease("startRecognizeInternal error! mService = null");
            return false;
        }
    }

    private static final int WRITE_AUDIO_DELAY = 50; // 在调用startRecognize后等待这个时间再进行writeaudio写入
    private static final HandlerThread mWriteAudioThread = new HandlerThread("WriteAudio");
    static {
        mWriteAudioThread.start();
    }
    private Handler mWriteAudioHandler = new Handler(mWriteAudioThread.getLooper()) {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_START_WRITE_AUDIO:
                    startRecognizeWithWriteAudio(msg.arg1, (RecognizeParams) msg.obj);
                    break;
                case MSG_CANCEL_WRITE_AUDIO:
                    cancelWriteAudio((Integer) msg.obj);
                    break;
            }
        }
    };
    private int mRequestThreshold;
    private Runnable mStartRecognize = null;
    private volatile boolean mStartWriteAudio = false;
    private volatile long mStartTime = 0L;
    private ByteArrayOutputStream mBos = null;
    private PcmRecorder mRecorder;
    private final RecordListener mRecordListener = new RecordListener() {
        @Override
        public void onRecordError(int errorCode) {
            postCancelWriteAudio(SpeechError.ERROR_AUDIO_RECORD);
        }

        @Override
        public void onRecordData(byte[] dataBuffer, int length,
                                 long timeMillisecond) {
            if (mStartWriteAudio && (SystemClock.elapsedRealtime() - mStartTime) > WRITE_AUDIO_DELAY) {
                // 将缓存的数据连同当前帧一起通过writeAudio写入引擎
                if (null != mRecorder && null != mService) {
                    if (mBos != null && mBos.size() > 0) {
                        mBos.write(dataBuffer, 0, length);
                        dataBuffer = mBos.toByteArray();
                        length = dataBuffer.length;
                        mBos = null;
                    }

                    log.d("write audio : " + length);
                    mService.writeAudio(dataBuffer, 0, length);
                }
            } else { // 缓存录音数据
                if (mBos == null) {
                    mBos = new ByteArrayOutputStream(1280 * 8);
                }

                // 在适当的缓存时间内缓存录音，超过这个缓存时间限制时，抛出错误
                // timeMillisecond为从开始录音到现在的时间间隔
                if (timeMillisecond < mRequestThreshold * 2) {
                    mBos.write(dataBuffer, 0, length);
                } else {
                    log.e("onRecordData wait timeout!");
                    postCancelWriteAudio(-1);
                }
            }
        }
    };

    private void postCancelWriteAudio(int error) {
        log.infoRelease("postCancelWriteAudio:" + error);
        Message msg = Message.obtain();
        msg.what = MSG_CANCEL_WRITE_AUDIO;
        msg.obj = error;
        mWriteAudioHandler.sendMessage(msg);
    }

    private void postStartWriteAudio(int threshold, RecognizeParams params) {
        threshold = Math.min(Math.max(WRITE_AUDIO_DELAY, threshold), 1000); // 要满足“50 <= threshold <= 1000”
        Message msg = mWriteAudioHandler.obtainMessage(MSG_START_WRITE_AUDIO);
        msg.arg1 = threshold;
        msg.obj = params;
        mWriteAudioHandler.sendMessage(msg);
    }

    private void cancelWriteAudio(int error) {
        log.infoRelease("cancelWriteAudio:" + error);
        mWriteAudioHandler.removeCallbacks(mStartRecognize);
        mStartRecognize = null;
        if (error != ERROR_CODE_STOP) { // 仅当不是由stopRecognize触发时，才需要进行一些处理操作
            if (null != mRecorder) {
                if (!mStartWriteAudio) {
                    mIsRecognizing = false;
                    // 若还未开始真正的startRecognize写入数据,那么释放录音时
                    // 需要由这儿负责回调错误接口
                    if (error == 0) {
                        error = SpeechError.ERROR_MSP_NO_DATA;
                    }
                } else {
                    // 若已经开始写入数据了，而且出现了非0的cancel请求，则由这儿请求一次endRecognize
                    if (error != 0 && mRecognizeStarted && mService != null) {
                        mService.endRecognize();
                    }
                }
            }

            if (error != 0) {
                mTotalRecognizerListener.onError(error);
            }
        }

        if (null != mRecorder) {
            // 释放录音机的操作放在处理引擎状态的逻辑后面，
            // 因为发现有时release会卡住一定时间，导致引擎状态不能及时更新，
            // 从而多线程的请求时需要判断状态的一些地方就会有问题
            mRecorder.release();
            mRecorder = null;
        }
        mBos = null;
        mStartWriteAudio = false;
    }

    private void startRecognizeWithWriteAudio(int threshold, final RecognizeParams params) {
        log.infoRelease("startRecognizeWithWriteAudio: threshold = " + threshold);
        if (null == mRecorder) {
            try {
                mRecorder = new PcmRecorder(mContext,
                        16000,
                        MediaRecorder.AudioSource.MIC,
                        params.isRequestAudioFocus());
                mRecorder.setRecordListener(mRecordListener);
            } catch (Throwable e) {
                e.printStackTrace();
                cancelWriteAudio(SpeechError.ERROR_AUDIO_RECORD);
                return;
            }
        }

        mRecorder.startRecording();

        mStartRecognize = new Runnable() {
            @Override
            public void run() {
                if (!mStartWriteAudio && startRecognizeInternal(params)) {
                        mStartWriteAudio = true;
                        mStartTime = SystemClock.elapsedRealtime();
                } else {
                    cancelWriteAudio(-1);
                }
            }
        };
        mRequestThreshold = threshold;
        mWriteAudioHandler.postDelayed(mStartRecognize, threshold);
    }

    @Override
    public boolean releaseResource() {
        if (isInitialized()) {
            super.releaseResource();
            if (mIsRecognizing && mService != null) {
                mService.endRecognize();
            }
        }
        return true;
    }

    public void releaseTts() {
        if(mService != null){
            mService.stopSpeak();
        }
    }

    public void destroyService() {
        synchronized (this) {
            if (mService != null) {
                mService.stopSpeak();
                mService.stopRecognize();
                super.releaseResource();
                mService.destroy();
                mSpeechInited = false;
                mRecognitionEngineInited = false;
                mCheckRecognitionStarted = false;
                mService = null;
                if (mIsRecognizing) {
                    mIsRecognizing = false;
                    mTotalRecognizerListener.onError(-1); // 回调一个错误给客户端
                }
            }
        }
    }

    private void onResultsMsg(RecognizeResult result) {
        if (mRecognizeListener != null) {
            if (result == null) {
                result = new RecognizeResult();
            }

            String content = result.getMainContent();
            if (result.isCandidate() && mPartialStrs != null && mPartialStrs.length > 0) {
                if (content != null) {
                    appendString(TextUtils.join(":", result.getContents()));
                }
                Bundle extra = new Bundle();
                extra.putString("candidate_string", TextUtils.join(":", convertStringType(mPartialStrs)));
                result.setExtras(extra);
            }

            mRecognizeListener.onResultReceived(result);
        }
    }

    private StringBuffer[] mPartialStrs;
    private void appendString(String appendStr) {
        if (mPartialStrs == null) {
            return;
        }
        if (appendStr == null) {
            mPartialStrs[0].append("");
            mPartialStrs[1].append("");
            mPartialStrs[2].append("");
        } else {
            String[] partialStr = appendStr.split(":");
            mPartialStrs[0].append(partialStr[0]);
            if (partialStr.length == 1) {
                mPartialStrs[1].append(partialStr[0]);
                mPartialStrs[2].append(partialStr[0]);
            } else if (partialStr.length == 2) {
                mPartialStrs[1].append(partialStr[1]);
                mPartialStrs[2].append(partialStr[1]);
            } else {
                mPartialStrs[1].append(partialStr[1]);
                mPartialStrs[2].append(partialStr[2]);
            }
        }
    }

    private String[] convertStringType(StringBuffer[] stringBuffers) {
        int equalCount = 0;
        for (int i = stringBuffers.length - 1; i - 1 >= 0; i--) {
            if (stringBuffers[i].toString().equals(stringBuffers[i - 1].toString())) {
                equalCount++;
            }
        }
        String[] strings = new String[stringBuffers.length - equalCount];
        for (int size = 0; size < stringBuffers.length - equalCount; size++) {
            strings[size] = mPartialStrs[size].toString();
        }
        return strings;
    }

    @Override
    public void playText(String text) {
        if (mService != null) {
            mService.speak(text, getTtsParam());
            mTtsPlaying = true;
        }
    }

    @Override
    public void stopPlayText() {
        if(mService != null)
        mService.stopSpeak();
        mTtsPlaying = false;
    }

    public boolean isTtsPlaying() {
        if(mService != null) {
            return mService.isSpeaking();
        }
        return false;
    }

    @Override
    public void endRecognize() {
        postCancelWriteAudio(0);
        if (mRecognizeStarted && mService != null) {
            mService.endRecognize();
        }
    }

    public void stopRecognize() {
        postCancelWriteAudio(ERROR_CODE_STOP);
        if (mRecognizeStarted && mService != null) {
            synchronized (mStopLock) {
                if (mRecognizeStarted && mService != null) {
                    mMsgReciver.obtainMessage(MSG_CHANGE_SKIP_CALLBACK_STATE, true).sendToTarget();
                    mService.stopRecognize();

                    log.infoRelease("stopRecognize start waiting...");
                    try {
                        mStopLock.wait(200);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    log.infoRelease("stopRecognize finish waiting.");
                    mMsgReciver.obtainMessage(MSG_CHANGE_SKIP_CALLBACK_STATE, false).sendToTarget();
                }
            }
        }
        mIsRecognizing = false;
    }

    private Intent getRecInitParams() {
        Intent recIntent = new Intent();
        Bundle offlineBundle = new Bundle();
        offlineBundle.putBoolean(SpeechIntent.EXT_GRAMMARS_FLUSH, true);
        offlineBundle.putInt(SpeechIntent.EXT_GRAMMARS_PREBUILD, 0);
        offlineBundle.putInt(SpeechIntent.ARG_RES_TYPE, SpeechIntent.RES_FROM_CLIENT);
        offlineBundle.putStringArray(SpeechIntent.EXT_GRAMMARS_FILES, mGrammarManager.getAllGrammarFiles());
        recIntent.putExtra(SpeechIntent.ENGINE_LOCAL_DEC, offlineBundle);
        recIntent.putExtra(SpeechIntent.EXT_ENGINE_TYPE, SpeechIntent.ENGINE_LOCAL);
        return recIntent;
    }

    private Intent getTtsParam() {
        Intent intent = new Intent();
        intent.putExtra(TextToSpeech.KEY_PARAM_ENGINE_TYPE, TextToSpeech.TTS_ENGINE_LOCAL);
        intent.putExtra(TextToSpeech.KEY_PARAM_SPEED, "50");
        intent.putExtra(TextToSpeech.KEY_PARAM_PITCH, "50");
        int volume = mAudioManager.getStreamVolume(AudioManager.STREAM_SYSTEM);
        boolean isMute =  Settings.System.getInt(mContext.getContentResolver(),SettingsSmt.get_VOLUME_PANEL_MUTE_ENABLE(), 0) == 1;
        if (isMute && !isHeadsetConnected()) {
                intent.putExtra(TextToSpeech.KEY_PARAM_STREAM,AudioManager.STREAM_VOICE_CALL+"");
                intent.putExtra(TextToSpeech.KEY_PARAM_VOLUME,"0");
        } else {
            intent.putExtra(TextToSpeech.KEY_PARAM_STREAM,(mEarpieceMode || isHeadsetConnected())? AudioManager.STREAM_VOICE_CALL+""
                        : AudioManager.STREAM_SYSTEM+"");
                if(mEarpieceMode || isHeadsetConnected())
                    mAudioManager.setStreamVolume(AudioManager.STREAM_VOICE_CALL, volume, 0);
                else
                    intent.putExtra(TextToSpeech.KEY_PARAM_VOLUME,volume * 7+"");
        }
        intent.putExtra(TextToSpeech.KEY_PARAM_AUDIOFOCUS, "false");
        return intent;
    }

    private boolean isHeadsetConnected() {
        if (mAudioManager.isWiredHeadsetOn()) {
            return true;
        }

        if (mAudioManager.isBluetoothA2dpOn()) {
            return true;
        }
        return false;
    }

    public void setTtsMode(boolean isEarpieceMode){
        mEarpieceMode = isEarpieceMode;
    }
}
