package com.smartisanos.voice.recoder;

import android.os.SystemClock;
import android.util.Log;

import com.iflytek.business.speech.SpeechError;


/**
 * 录音控制抽象类
 * 
 * @author dwyue 2016-1-6 19:17:56
 */
public abstract class AbsRecorder {
    protected static final String TAG = "SPEECH_Recorder";

    protected static final int SampleRate16K = 16000;
    protected static final short DEFAULT_BIT_SAMPLES = 16;
    protected static final int RECORD_BUFFER_TIMES_FOR_FRAME = 10;
    protected static final int DEFAULT_TIMER_INTERVAL = 40;
    protected static final short DEFAULT_CHANNELS = 1;
    protected static final long NO_RECORD_DATA_TIMEOUT = 3000;
    protected static final int DEFAULT_SLEEP_TIME = 100;

    protected byte[] mBuffer = null;
    protected RecordListener mRecordListener = null;
    protected Object mReadLock = new Object();
    protected long mStartTime = 0;
    protected volatile boolean mIsRecording = false;
    protected Thread mReadThread;
    protected volatile boolean mHasRecordData;
    protected long mRecordTime = 0;
    protected volatile boolean mRecordError;

    /**
     * 录音器构建类，如果创建失败直接ThrowException
     * 
     * @throws Exception
     */
    public AbsRecorder() throws Exception {
        this(DEFAULT_CHANNELS, DEFAULT_BIT_SAMPLES, SampleRate16K, DEFAULT_TIMER_INTERVAL);
    }

    public AbsRecorder(int sampleRate) throws Exception {
        this(DEFAULT_CHANNELS, DEFAULT_BIT_SAMPLES, sampleRate, DEFAULT_TIMER_INTERVAL);
    }

    public AbsRecorder(short channels, short bitSamples, int sampleRate, int timeInterval) throws Exception {
        if (timeInterval % DEFAULT_TIMER_INTERVAL != 0) {
            String desc = "parameter error, timeInterval must be multiple of " + DEFAULT_TIMER_INTERVAL;
            Log.e(TAG, desc);
            throw new Exception(desc);
        }
    }

    public int onRecordData(byte[] buffer, int offset, int length) {
        int ret = length;
        mRecordTime = SystemClock.elapsedRealtime();
        if (null == mRecordListener) {
            Log.d(TAG, "mRecordListener is null");
            ret = 0;
        } else
        if (null != buffer && 0 < length && offset + length <= buffer.length) {
            if (isZeroData(buffer)) {
                Log.d(TAG, "onRecordData --zero data.");
            } else {
                mHasRecordData = true;
                byte[] tmpBuffer = null;
                if (0 >= offset) {
                    tmpBuffer = buffer;
                } else {
                    tmpBuffer = new byte[length];
                    System.arraycopy(buffer, offset, tmpBuffer, 0, length);
                }
                mRecordListener.onRecordData(tmpBuffer, length, mRecordTime - mStartTime);
            }
            if (!mHasRecordData && mRecordTime - mStartTime > NO_RECORD_DATA_TIMEOUT) {
                Log.w(TAG, "onRecordData --no data time out.");
                mRecordListener.onRecordError(SpeechError.ERROR_AUDIO_RECORD);
                mRecordError = true;
            }
        } else {
            Log.e(TAG, "invalid buffer");
            Log.d(TAG, "count = " + length);
            if (!mHasRecordData && mRecordTime - mStartTime > NO_RECORD_DATA_TIMEOUT) {
                Log.w(TAG, "onRecordData --no data time out.");
                mRecordListener.onRecordError(SpeechError.ERROR_AUDIO_RECORD);
                mRecordError = true;
            }
        }
        return ret;
    }
    
    /**
     * 判断是不是静音数据(部分手机前后有一部分静音非正常数据)
     * 
     * @date 2012-8-21
     * @param data
     * @return
     */
    protected boolean isZeroData(byte[] data) {
        for (int i = 0; i < data.length; i++) {
            if (data[i] != 0) {
                return false;
            }
        }
        return true;
    }

    public void setRecordListener(RecordListener listener) {
        mRecordListener = listener;
    }

    public void removeRecordListener() {
        mRecordListener = null;
    }

    public abstract void startRecording() throws Exception;

    public abstract void stopRecording();

    /**
     * 停止读数据 读数据线程会根据此标志结束
     * */
    public void stopReadingData() {
        mIsRecording = false;
    }

    /**
     * 释放录音设备
     */
    public abstract void release();

    /**
     * 获取录音器采样率
     * 
     * @return
     */
    public abstract int getSampleRate();

    /**
     * 是否在录音中状态
     * 
     * @return
     */
    public abstract boolean isRecording();
}
