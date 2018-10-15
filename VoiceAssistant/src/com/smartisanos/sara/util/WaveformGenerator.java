package com.smartisanos.sara.util;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

public class WaveformGenerator {
    int mTotalPointNum;
    int mTotalFrameNum;
    int mPcmMax;
    int mPcmMaxTop;
    int mLastWaveForm;
    int mPointSpan;
    byte[] mWaveformBuf;
    boolean mBeautify = true;
    short mChannel;

    FileOutputStream mFile;
    private byte[] mBuffer;

    public WaveformGenerator(int intervalMs, int sampleRate, byte[] buf) {
        mTotalPointNum = 0;
        mPointSpan = intervalMs * sampleRate / 1000;
        if (mPointSpan == 0) {
            throw new IllegalArgumentException("WaveformGenerator divide by mPointSpan = 0");
        }

        mTotalFrameNum = 0;
        mPcmMax = 0;
        mPcmMaxTop = 32768;
        mLastWaveForm = 0;
        mWaveformBuf = buf;
        mChannel = 1;
        mBuffer = new byte[2048];
    }

    public void setBeautify(boolean b) {
        mBeautify = b;
    }

    public void open(String path) {
        try {
            mFile = new FileOutputStream(path);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    public void close() {
        try {
            if (mFile != null) {
                mFile.close();
            }
            mFile = null;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public int getTotalPoint() {
        return mTotalPointNum << 1;
    }

    public int generate(byte[] pcmbuf, int size) {
        if (size > 0) {
            int frame;
            int step;
            if(mChannel == 2) {
                frame = size / 4;
                step = 2;
            } else {
                frame = size / 2;
                step = 1;
            }

            int pointBufferSize = (frame + mTotalFrameNum % mPointSpan) / mPointSpan;
            short pcm;
            int interval;

            int count = 0;
            for (int f = 0; f < frame; f++) {
                mTotalFrameNum++;
                interval = f << step;
                pcm = (short) (pcmbuf[interval] + (pcmbuf[interval + 1] << 8));
                if (pcm < 0) pcm = (short) -pcm;

                mPcmMax = pcm > mPcmMax ? pcm : mPcmMax;
                if ((mTotalPointNum + 1) * mPointSpan <= mTotalFrameNum) {

                    if (mPcmMaxTop < mPcmMax) {
                        mPcmMaxTop = mPcmMax;
                    }

                    // map to 0~255
                    mPcmMax = mPcmMax * 0xff / mPcmMaxTop;

                    if (mBeautify) {
                        beautify();
                    }

                    if (mWaveformBuf != null) {
                        int first = mTotalPointNum << 1;
                        int second = first + 1;
                        mWaveformBuf[first % mWaveformBuf.length] = (byte) mPcmMax;
                        mWaveformBuf[second % mWaveformBuf.length] = (byte) mPcmMax;
                    }
                    mBuffer[count++] = (byte) mPcmMax;
                    if (count == mBuffer.length) {
                        if (mFile != null) {
                            try {
                                mFile.write(mBuffer, 0, count);
                                count = 0;
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                    mPcmMax = 0;
                    mTotalPointNum++;
                }
            }
            if (count != 0) {
                if (mFile != null) {
                    try {
                        mFile.write(mBuffer, 0, count);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
            return pointBufferSize;
        }
        return 0;
    }

    private void beautify() {
        // create some spikes
        if (mPcmMax >= 246 && mLastWaveForm >= 246) {
            mLastWaveForm = mPcmMax;
            mPcmMax = (int) (Math.random() * 9 + 246);
//          ALOGE("spiking %u to %u, last %u", last, mTotal, mLastWaveForm);
        }
        // ramp down
        else if (mPcmMax < mLastWaveForm * 0.85) {
            mPcmMax = (int) (mLastWaveForm * 0.85);
//          ALOGE("ramping down %u to %u, last %u", last, mTotal, mLastWaveForm);
            mLastWaveForm = mPcmMax;
        }
        else {
            mLastWaveForm = mPcmMax;
        }
    }

    public void setChannel(short channel) {
        mChannel = channel;
    }

}
