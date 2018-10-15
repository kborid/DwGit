package com.smartisanos.voice.recoder;

import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.os.SystemClock;
import android.util.Log;

import com.iflytek.business.speech.SpeechError;


/**
 * 录音控制类
 *
 * @author zhangyun
 * @date 2013-12-30 增加start异常catch
 */
public class PcmRecorder extends AbsRecorder {
    private static final String TAG = "SPEECH_PcmRecorder";

    protected static final int DEFAULT_AUDIO_SOURCE = android.media.MediaRecorder.AudioSource.MIC;

    private Context mContext = null;
    private AudioManager mAudioManager = null;
    private AudioRecord mRecorder = null;

    /**
     * 录音器构建类，如果创建失败直接ThrowException
     *
     * @param context
     * @throws Exception
     */
    public PcmRecorder(Context context) throws Exception {
        this(context, DEFAULT_CHANNELS, DEFAULT_BIT_SAMPLES, SampleRate16K,
                DEFAULT_TIMER_INTERVAL, DEFAULT_AUDIO_SOURCE, true);
    }

    public PcmRecorder(Context context, int sampleRate, int audioSource, boolean requestAudioFocus) throws Exception {
        this(context, DEFAULT_CHANNELS, DEFAULT_BIT_SAMPLES, sampleRate,
                DEFAULT_TIMER_INTERVAL, audioSource, requestAudioFocus);
    }

    public PcmRecorder(Context context, short channels, short bitSamples, int sampleRate,
                       int timeInterval, int audioSource, boolean requestAudioFocus) throws Exception {
        super(channels, bitSamples, sampleRate, timeInterval);

        mContext = context;
        mAudioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        if (requestAudioFocus) {
            mAudioManager.requestAudioFocus(null, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT);
        }
        int framePeriod = sampleRate * timeInterval / 1000;
        int recordBufferSize = framePeriod * RECORD_BUFFER_TIMES_FOR_FRAME
                * bitSamples * channels / 8;
        int channelConfig = (channels == 1 ? AudioFormat.CHANNEL_CONFIGURATION_MONO
                : AudioFormat.CHANNEL_CONFIGURATION_STEREO);
        int audioFormat = (bitSamples == 16 ? AudioFormat.ENCODING_PCM_16BIT
                : AudioFormat.ENCODING_PCM_8BIT);

        int min = AudioRecord.getMinBufferSize(sampleRate, channelConfig,
                audioFormat);
        if (recordBufferSize < min) {
            recordBufferSize = min;
            Log.w(TAG, "Increasing buffer size to "
                    + Integer.toString(recordBufferSize));
        }
//        //8K录音强制不降噪
//        if (sampleRate == SAMPLE_RATE_8K) {
//            nAudioSource = MediaRecorder.AudioSource.MIC;            
//        }

        // 初始化重试10次
        int initCount = 0;
        while (true) {
            mRecorder = new AudioRecord(audioSource, sampleRate,
                    channelConfig, audioFormat, recordBufferSize);

            if (mRecorder.getState() != AudioRecord.STATE_INITIALIZED) {
                mRecorder.release();
                mRecorder = null;
                String desc = "create AudioRecord error";
                Log.e(TAG, desc);
                ++initCount;
                if (10 > initCount) {
                    Log.e(TAG, "will retry, initCount = " + initCount);
                    SystemClock.sleep(DEFAULT_SLEEP_TIME);
                } else {
                    mAudioManager.abandonAudioFocus(null);
                    throw new Exception(desc);
                }
            } else {
                // 成功则打断while
                break;
            }
        }

        mBuffer = new byte[framePeriod * channels * bitSamples / 8];
        Log.d(TAG, "create AudioRecord ok buffer size=" + mBuffer.length
                + " audioSource=" + audioSource + " sampleRate=" + sampleRate);
    }

    /**
     * 读取录音数据
     *
     * @return
     */
    private int readRecordData() {
        int count = 0;
        try {
            if (mRecorder != null) {
                if (mRecorder.getRecordingState() != AudioRecord.RECORDSTATE_RECORDING) {
                    Log.d(TAG, "readRecordData END RECORDSTATE_STOPPED");
                    return 0;
                }
                if (mRecordError) {
                    Log.w(TAG, "readRecordData record error");
                    return 0;
                }
                count = mRecorder.read(mBuffer, 0, mBuffer.length);
                count = onRecordData(mBuffer, 0, count);
            } else {
                Log.d(TAG, "readRecordData null");
            }
        } catch (Exception e) {
            Log.w(TAG, e);
        }
        return count;
    }

    /**
     * 每次录音开始创建一个读数据子线程
     */
    private void startReadThread() {
        mReadThread = new Thread("PcmRecorderNew") {
            @Override
            public void run() {
                Log.d(TAG, "startReadThread OK=" + this.getId());
                while (mIsRecording) {
// 防止read卡死导致不能释放                   synchronized (mReadLock) {
                    readRecordData();
//                    }
                    SystemClock.sleep(10);
                }
                Log.d(TAG, "startReadThread finish=" + this.getId());
            }

        };
        mReadThread.setPriority(Thread.MAX_PRIORITY);
        mReadThread.start();
    }

    public void startRecording() {
        Log.d(TAG, "startRecording begin");
        if (null == mRecorder || mRecorder.getState() == AudioRecord.STATE_UNINITIALIZED) {
            Log.e(TAG, "startRecording STATE_UNINITIALIZED");
            if (null != mRecordListener) {
                mRecordListener.onRecordError(SpeechError.ERROR_AUDIO_RECORD);
            }
            mRecordError = true;
            return;
        }
        if (mRecorder.getRecordingState() == AudioRecord.RECORDSTATE_RECORDING) {
            Log.e(TAG, "startRecording RECORDSTATE_RECORDING");
            return;
        }

        mHasRecordData = false;
        mRecordError = false;
        mRecordTime = 0;

        mIsRecording = true;

        // 录音机开启重试10次
        int initCount = 0;
        while (mIsRecording) {
            try {
                mRecorder.startRecording();
                if (AudioRecord.RECORDSTATE_RECORDING != mRecorder.getRecordingState()) {
                    Log.e(TAG, "startRecording RecordingState = " + mRecorder.getRecordingState());
                    throw new Exception("startRecording RecordingState = " + mRecorder.getRecordingState());
                } else {
                    mStartTime = SystemClock.elapsedRealtime();
                    startReadThread();
                    // 成功则打断while
                    break;
                }
            } catch (Throwable e) {
                Log.e(TAG, "", e);
                ++initCount;
                if (10 > initCount) {
                    Log.e(TAG, "will retry, initCount = " + initCount);
                    SystemClock.sleep(DEFAULT_SLEEP_TIME);
                } else {
                    if (null != mRecordListener) {
                        mRecordListener.onRecordError(SpeechError.ERROR_AUDIO_RECORD);
                    }
                    mRecordError = true;
                    break;
                }
            }
        }
        Log.d(TAG, "startRecording end");
    }

    public void stopRecording() {
        if (mRecorder != null) {
            Log.d(TAG, "stopRecording into");
            // FIXME 等读取完成后再Stop 2012-8-13 ，解决部分手机Stop后读取数据阻塞问题
            mIsRecording = false;
            if (mRecorder.getRecordingState() == AudioRecord.RECORDSTATE_RECORDING) {
                synchronized (mReadLock) {
                    mRecorder.stop();
                }
            }
            Log.d(TAG, "stopRecording end");
        }
    }

    /**
     * 释放录音设备
     */
    public void release() {
        if (null != mRecorder) {
            stopRecording();
        }
        // FIXME 在部分机器上release后 read方法会阻塞,增加
        Log.d(TAG, "release begin");
        synchronized (mReadLock) {
            if (mRecorder != null) {
                mRecorder.release();
                mRecorder = null;
            }
            Log.d(TAG, "release ok");
        }
        Log.d(TAG, "release end");
        mAudioManager.abandonAudioFocus(null);
    }

    /**
     * 获取录音器采样率
     *
     * @return
     */
    public int getSampleRate() {
        if (mRecorder != null) {
            return mRecorder.getSampleRate();
        } else {
            return SampleRate16K;
        }
    }

    /**
     * 获取录音开始时间
     *
     * @return
     */
    public long getStartTime() {
        return mStartTime;
    }

    /**
     * 是否在录音中状态
     *
     * @return
     */
    public boolean isRecording() {
        if (mRecorder != null) {
            return mRecorder.getRecordingState()
                    == AudioRecord.RECORDSTATE_RECORDING;
        } else {
            return false;
        }
    }
}
