package com.smartisanos.voice.engine;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.os.Parcel;
import android.os.Parcelable;
import android.provider.Settings;
import android.text.TextUtils;

import com.iflytek.business.speech.SpeechIntent;
import com.smartisanos.voice.util.VoiceUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import smartisanos.app.voiceassistant.RecognizeArgsHelper;

public class RecognizeParams {
    public static final int ENGINE_TYPE_LOCAL = 0x01;
    public static final int ENGINE_TYPE_WEB = 0x10;

    public static final int TYPE_CONTACT = 0x01;
    public static final int TYPE_MUSIC = 0x02;
    public static final int TYPE_APP = 0x04;
    public static final int TYPE_ALL = TYPE_CONTACT | TYPE_MUSIC | TYPE_APP;

    private static final String LOW_POWER_MODE = "low_power";
    private static final String FEATURE_PHONE_MODE = "feature_phone_mode";
    private static final String LOCAL_SMS_DICTATION = "sms.irf";
    /**
     * 设定识别模式为语音识别，需提供识别场景。默认不开启，即使用语音听写模式。
     */
    private boolean mEnableAsr;
    /**
     * 本地搜索资源类型，可为{@link #TYPE_CONTACT}、{@link #TYPE_MUSIC}、{@link #TYPE_APP}或{@link #TYPE_ALL}
     */
    private int mLocalSearchType;
    /**
     * 引擎类型,可为{@link #ENGINE_TYPE_LOCAL} 和 {@link #ENGINE_TYPE_WEB}
     */
    private int mEngineType;
    /**
     * 前端语音超时检测时间，默认5000(ms)
     */
    private int mVadFrontTime;
    /**
     * 后端语音超时检测时间，默认2000(ms)
     */
    private int mVadEndTime;
    /**
     * 在停止识别后等待网络结果的超时时间，默认8000(ms)
     */
    private int mSessionTimeout;
    /**
     * 语音识别本地得分门限，低于此分数的本地结果将被丢弃
     */
    private int mLocalResultScore;
    /**
     * 语音识别混合引擎得分门限，使用混合引擎时，低于此分数的结果将被丢弃。
     */
    private int mMixResultScore;
    /**
     * 真正请求识别的时间阀值，以过滤掉过短的无效请求，避免讯飞服务器压力过大（重要！），默认为0，即不过滤。
     * 无延迟高频请求的场景一定要设置改项，如胶囊预启动和大屏语音指令
     */
    private int mRequestThreshold;

    /**
     * 本地结果优先
     */
    private boolean mLocalPrior;
    /**
     * 识别时请求AudioFocus焦点
     */
    private boolean mRequestAudioFocus;
    /**
     * 识别时打断tts
     */
    private boolean mInterruptTts;
    /**
     * 语音听写启用多候选结果
     */
    private boolean mEnableCandidate;
    /**
     * 语音听写启用动态修正
     */
    private boolean mEnableDynamicFix;
    /**
     * 在startRecognize时预先调用onRecordStart,而不用等待引擎回调
     */
    private boolean mPreCallOnRecordStart; // call onRecordStart when startRecognize instead of callback from recognize listener.

    /**
     * 选择已有文件进行识别时，源文件的路径
     */
    private String mSourcePcm;
    /**
     * 识别音频保存路径
     */
    private String mDstPcm;
    /**
     * 语音识别时，设定的识别场景
     */
    private final ArrayList<String> mScenes;
    /**
     * 将某些错误马映射成为空结果，不以error的形式而是以result的形式返回
     */
    private final ArrayList<Integer> mMapErrors;

    public RecognizeParams() {
        mEnableAsr = false;
        mEngineType = ENGINE_TYPE_LOCAL | ENGINE_TYPE_WEB;
        mVadFrontTime = 5000;
        mVadEndTime = 2000;
        mSessionTimeout = 8000;
        mLocalResultScore = 0;
        mMixResultScore = -1;
        mRequestThreshold = 0;
        mLocalPrior = false;
        mRequestAudioFocus = true;
        mInterruptTts = true;
        mEnableCandidate = false;
        mEnableDynamicFix = false;
        mPreCallOnRecordStart = true;

        mDstPcm = Environment.getExternalStorageDirectory() + "/sara/record.pcm";
        mScenes = new ArrayList<String>();

        mMapErrors = new ArrayList<Integer>();
    }

    /**
     * 根据传入的基本类型先得到有个基本的参数类型
     * @param context 用于获取某些系统配置信息
     * @param baseType 可为{@link RecognizeArgsHelper#BASE_TYPE_BUBBLE}、{@link RecognizeArgsHelper#BASE_TYPE_SEARCH}或{@link RecognizeArgsHelper#BASE_TYPE_VOICE_COMMAND}
     * @return  根据传入的基本类型得到的该类型默认参数对象
     */
    public static RecognizeParams getBaseParams(Context context, int baseType) {
        RecognizeParams params = new RecognizeParams();
        switch (baseType) {
            case RecognizeArgsHelper.BASE_TYPE_BUBBLE:
                if (context != null) {
                    ContentResolver resolver = context.getContentResolver();
                    boolean lowBattery = Settings.Global.getInt(resolver, LOW_POWER_MODE, 0) == 1
                            || Settings.System.getInt(resolver, LOW_POWER_MODE, 0) == 1;
                    boolean featurePhone = Settings.Global.getInt(resolver, FEATURE_PHONE_MODE, 0) == 1
                            || Settings.System.getInt(resolver, FEATURE_PHONE_MODE, 0) == 1;
                    if (lowBattery || featurePhone) {
                        params.setEngineType(ENGINE_TYPE_LOCAL);
                    }
                }
                break;
            case RecognizeArgsHelper.BASE_TYPE_SEARCH:
                params.setEnableAsr(true)
                        .setLocalPrior(true)
                        .setVadFrontTime(3000)
                        .setVadEndTime(1500)
                        .setSessionTimeout(3000)
                        .setLocalResultScore(25)
                        .setMixResultScore(35);
                break;
            case RecognizeArgsHelper.BASE_TYPE_VOICE_COMMAND:
                params.setEnableAsr(true)
                        .setLocalPrior(true)
                        .setRequestAudioFocus(false)
                        .setRequestThreshold(300)
                        .setVadFrontTime(3000)
                        .setVadEndTime(1500)
                        .setSessionTimeout(1000)
                        .setLocalResultScore(25)
                        .setMixResultScore(35);
                if (!VoiceUtils.isNetworkAvailable(context)) {
                    params.setLocalResultScore(0).setMixResultScore(0);
                }
                break;
        }

        return params;
    }

    public static RecognizeParams fromBundle(Context context, Bundle args) {
        RecognizeArgsHelper helper = new RecognizeArgsHelper(args);
        RecognizeParams params = getBaseParams(context, helper.getBaseType());

        if (helper.hasArg(RecognizeArgsHelper.ARG_LOCAL_SEARCH_TYPE)) {
            params.setLocalSearchType(helper.getLocalSearchType());
        }

        if (helper.hasArg(RecognizeArgsHelper.ARG_SOURCE_PCM)) {
            params.setSourcePcm(helper.getSourcePcm());
        }

        if (helper.hasArg(RecognizeArgsHelper.ARG_ENABLE_DYNAMIC_FIX)) {
            params.setEnableDynamicFix(helper.isEnableDynamicFix());
        }

        if (helper.hasArg(RecognizeArgsHelper.ARG_ENABLE_CANDIDATE)) {
            params.setEnableCandidate(helper.isEnableCandidate());
        }

        if (helper.hasArg(RecognizeArgsHelper.ARG_REQUEST_THRESHOLD)) {
            params.setRequestThreshold(helper.getRequestThreshold());
        }

        if (helper.hasArg(RecognizeArgsHelper.ARG_REQUEST_AUDIOFOCUS)) {
            params.setRequestAudioFocus(helper.isRequestAudioFocus());
        }

        return params;
    }

    public boolean isEnableAsr() {
        return mEnableAsr;
    }

    public RecognizeParams setEnableAsr(boolean enableAsr) {
        mEnableAsr = enableAsr;
        return this;
    }

    public int getLocalSearchType() {
        return mLocalSearchType;
    }

    public RecognizeParams setLocalSearchType(int localSearchType) {
        mLocalSearchType = localSearchType;
        return this;
    }

    public int getEngineType() {
        return mEngineType;
    }

    public RecognizeParams setEngineType(int engineType) {
        mEngineType = engineType;
        return this;
    }

    public int getVadFrontTime() {
        return mVadFrontTime;
    }

    public RecognizeParams setVadFrontTime(int vadFrontTime) {
        mVadFrontTime = vadFrontTime;
        return this;
    }

    public int getVadEndTime() {
        return mVadEndTime;
    }

    public RecognizeParams setVadEndTime(int vadEndTime) {
        mVadEndTime = vadEndTime;
        return this;
    }

    public int getSessionTimeout() {
        return mSessionTimeout;
    }

    public RecognizeParams setSessionTimeout(int sessionTimeout) {
        mSessionTimeout = sessionTimeout;
        return this;
    }

    public int getLocalResultScore() {
        return mLocalResultScore;
    }

    public RecognizeParams setLocalResultScore(int localResultScore) {
        mLocalResultScore = localResultScore;
        return this;
    }

    public int getMixResultScore() {
        return mMixResultScore;
    }

    public RecognizeParams setMixResultScore(int mixResultScore) {
        mMixResultScore = mixResultScore;
        return this;
    }

    public int getRequestThreshold() {
        return mRequestThreshold;
    }

    public RecognizeParams setRequestThreshold(int threshold) {
        mRequestThreshold = threshold;
        return this;
    }

    public boolean isLocalPrior() {
        return mLocalPrior;
    }

    public RecognizeParams setLocalPrior(boolean localPrior) {
        mLocalPrior = localPrior;
        return this;
    }

    public boolean isRequestAudioFocus() {
        return mRequestAudioFocus;
    }

    public RecognizeParams setRequestAudioFocus(boolean requestAudioFocus) {
        mRequestAudioFocus = requestAudioFocus;
        return this;
    }

    public boolean isInterruptTts() {
        return mInterruptTts;
    }

    public RecognizeParams setInterruptTts(boolean interruptTts) {
        mInterruptTts = interruptTts;
        return this;
    }

    public boolean isEnableCandidate() {
        return mEnableCandidate;
    }

    public RecognizeParams setEnableCandidate(boolean enableCandidate) {
        mEnableCandidate = enableCandidate;
        return this;
    }

    public boolean isEnableDynamicFix() {
        return mEnableDynamicFix;
    }

    public void setEnableDynamicFix(boolean enableDynamicFix) {
        mEnableDynamicFix = enableDynamicFix;
    }

    public boolean isPreCallOnRecordStart() {
        return mPreCallOnRecordStart;
    }

    public RecognizeParams setPreCallOnRecordStart(boolean preCallOnRecordStart) {
        mPreCallOnRecordStart = preCallOnRecordStart;
        return this;
    }

    public boolean hasSourcePcm() {
        return !TextUtils.isEmpty(mSourcePcm);
    }

    public String getSourcePcm() {
        return mSourcePcm;
    }

    public RecognizeParams setSourcePcm(String sourcePcm) {
        mSourcePcm = sourcePcm;
        return this;
    }

    public String getDstPcm() {
        return mDstPcm;
    }

    public RecognizeParams setDstPcm(String dstPcm) {
        mDstPcm = dstPcm;
        return this;
    }

    public boolean hasScenes() {
        return !mScenes.isEmpty();
    }

    public List<String> getScenes() {
        return mScenes;
    }

    public RecognizeParams setScenes(String... scenes) {
        mScenes.clear();
        addScenes(scenes);
        return this;
    }

    public RecognizeParams addScenes(String... scenes) {
        if (scenes != null) {
            for (int i = 0; i < scenes.length; i++) {
                if (!mScenes.contains(scenes[i])) {
                    mScenes.add(scenes[i]);
                }
            }
        }
        return this;
    }

    public ArrayList<Integer> getMapErrors() {
        return mMapErrors;
    }

    public RecognizeParams setMapErrors(int... errors) {
        mMapErrors.clear();
        addMapErrors(errors);
        return this;
    }

    public RecognizeParams addMapErrors(int... errors) {
        if (errors != null) {
            for (int error : errors) {
                if (!mMapErrors.contains(error)) {
                    mMapErrors.add(error);
                }
            }
        }
        return this;
    }

    @Override
    public String toString() {
        return "RecognizeParams{" +
                "mEnableAsr=" + mEnableAsr +
                ", mLocalSearchType=" + mLocalSearchType +
                ", mEngineType=" + mEngineType +
                ", mVadFrontTime=" + mVadFrontTime +
                ", mVadEndTime=" + mVadEndTime +
                ", mSessionTimeout=" + mSessionTimeout +
                ", mLocalResultScore=" + mLocalResultScore +
                ", mMixResultScore=" + mMixResultScore +
                ", mRequestThreshold=" + mRequestThreshold +
                ", mLocalPrior=" + mLocalPrior +
                ", mRequestAudioFocus=" + mRequestAudioFocus +
                ", mInterruptTts=" + mInterruptTts +
                ", mEnableCandidate=" + mEnableCandidate +
                ", mEnableDynamicFix=" + mEnableDynamicFix +
                ", mPreCallOnRecordStart=" + mPreCallOnRecordStart +
                ", mSourcePcm='" + mSourcePcm + '\'' +
                ", mDstPcm='" + mDstPcm + '\'' +
                ", mScenes=" + mScenes +
                ", mMapErrors=" + mMapErrors +
                '}';
    }

}
