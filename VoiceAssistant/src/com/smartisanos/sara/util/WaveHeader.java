package com.smartisanos.sara.util;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Arrays;

/**
 * This class represents the header of a WAVE format audio file, which usually
 * have a .wav suffix.  The following integer valued fields are contained:
 * <ul>
 * <li> format - usually PCM, ALAW or ULAW.
 * <li> numChannels - 1 for mono, 2 for stereo.
 * <li> sampleRate - usually 8000, 11025, 16000, 22050, or 44100 hz.
 * <li> bitsPerSample - usually 16 for PCM, 8 for ALAW, or 8 for ULAW.
 * <li> numBytes - size of audio data after this header, in bytes.
 * </ul>
 *
 * Not yet ready to be supported, so
 * @hide
 */
public class WaveHeader {

    // follows WAVE format in http://ccrma.stanford.edu/courses/422/projects/WaveFormat

    private static final String TAG = "WaveHeader";

    private static final int HEADER_LENGTH = 44;

    private static int FRAME_SIZE = 2;

    /** Indicates PCM format. */
    public static final short FORMAT_PCM = 1;
    /** Indicates ALAW format. */
    public static final short FORMAT_ALAW = 6;
    /** Indicates ULAW format. */
    public static final short FORMAT_ULAW = 7;

    private short mFormat;
    private short mNumChannels;
    private int mSampleRate;
    private short mBitsPerSample;
    private int mNumBytes;
    private boolean mReadOnly = false;
    public String mFilePath = "";


    private RandomAccessFile mFile;

    /**
     * Construct a WaveHeader, with all fields defaulting to zero.
     */
    public WaveHeader() {
    }

    /**
     * Construct a WaveHeader, with fields initialized.
     * @param format format of audio data,
     * one of {@link #FORMAT_PCM}, {@link #FORMAT_ULAW}, or {@link #FORMAT_ALAW}.
     * @param numChannels 1 for mono, 2 for stereo.
     * @param sampleRate typically 8000, 11025, 16000, 22050, or 44100 hz.
     * @param bitsPerSample usually 16 for PCM, 8 for ULAW or 8 for ALAW.
     */
    public WaveHeader(short format, short numChannels, int sampleRate, short bitsPerSample) {
        mFormat = format;
        mSampleRate = sampleRate;
        mNumChannels = numChannels;
        mBitsPerSample = bitsPerSample;
        mNumBytes = 0;
    }

    /**
     * Get the format field.
     * @return format field,
     * one of {@link #FORMAT_PCM}, {@link #FORMAT_ULAW}, or {@link #FORMAT_ALAW}.
     */
    public short getFormat() {
        return mFormat;
    }

    /**
     * Set the format field.
     * @param format
     * one of {@link #FORMAT_PCM}, {@link #FORMAT_ULAW}, or {@link #FORMAT_ALAW}.
     * @return reference to this WaveHeader instance.
     */
    public WaveHeader setFormat(short format) {
        mFormat = format;
        return this;
    }

    /**
     * Get the number of channels.
     * @return number of channels, 1 for mono, 2 for stereo.
     */
    public short getNumChannels() {
        return mNumChannels;
    }

    /**
     * Set the number of channels.
     * @param numChannels 1 for mono, 2 for stereo.
     * @return reference to this WaveHeader instance.
     */
    public WaveHeader setNumChannels(short numChannels) {
        mNumChannels = numChannels;
        return this;
    }

    /**
     * Get the sample rate.
     * @return sample rate, typically 8000, 11025, 16000, 22050, or 44100 hz.
     */
    public int getSampleRate() {
        return mSampleRate;
    }

    /**
     * Set the sample rate.
     * @param sampleRate sample rate, typically 8000, 11025, 16000, 22050, or 44100 hz.
     * @return reference to this WaveHeader instance.
     */
    public WaveHeader setSampleRate(int sampleRate) {
        mSampleRate = sampleRate;
        return this;
    }

    /**
     * Get the number of bits per sample.
     * @return number of bits per sample,
     * usually 16 for PCM, 8 for ULAW or 8 for ALAW.
     */
    public short getBitsPerSample() {
        return mBitsPerSample;
    }

    /**
     * Set the number of bits per sample.
     * @param bitsPerSample number of bits per sample,
     * usually 16 for PCM, 8 for ULAW or 8 for ALAW.
     * @return reference to this WaveHeader instance.
     */
    public WaveHeader setBitsPerSample(short bitsPerSample) {
        mBitsPerSample = bitsPerSample;
        return this;
    }

    /**
     * Get the size of audio data after this header, in bytes.
     * @return size of audio data after this header, in bytes.
     */
    public int getNumBytes() {
        return mNumBytes;
    }

    public int getDurationMs() {
        return (int) (mNumBytes / ((float)mSampleRate / 500f));
    }

    /**
     * Set the size of audio data after this header, in bytes.
     * @param numBytes size of audio data after this header, in bytes.
     * @return reference to this WaveHeader instance.
     */
    public WaveHeader setNumBytes(int numBytes) {
        mNumBytes = numBytes;
        return this;
    }

    /**
     * Read and initialize a WaveHeader.
     * @return number of bytes consumed.
     * @throws IOException
     */
    public int readHeader() throws IOException {
        /* RIFF header */
        readId("RIFF");
        mNumBytes = readInt() - 36;
        readId("WAVE");

        /* fmt chunk */
        readId("fmt ");
        if (16 != readInt()) throw new IOException("fmt chunk length not 16");
        mFormat = readShort();
        mNumChannels = readShort();
        mSampleRate = readInt();
        int byteRate = readInt();
        short blockAlign = readShort();
        mBitsPerSample = readShort();
        if (byteRate != mNumChannels * mSampleRate * mBitsPerSample / 8) {
            throw new IOException("fmt.ByteRate field inconsistent");
        }
        if (blockAlign != mNumChannels * mBitsPerSample / 8) {
            throw new IOException("fmt.BlockAlign field inconsistent");
        }

        /* data chunk */
        readId("data");
        mNumBytes = readInt();

        LogUtils.d(TAG, "readHeader sr:" + mSampleRate + " length:" + mNumBytes);

        return HEADER_LENGTH;
    }

    private void readId(String id) throws IOException {
        for (int i = 0; i < id.length(); i++) {
            if (id.charAt(i) != mFile.read()) throw new IOException( id + " tag not present");
        }
    }

    private int readInt() throws IOException {
        return mFile.read() | (mFile.read() << 8) | (mFile.read() << 16) | (mFile.read() << 24);
    }

    private short readShort() throws IOException {
        return (short)(mFile.read() | (mFile.read() << 8));
    }

    /**
     * Write a WAVE file header.
     * @return number of bytes written.
     * @throws IOException
     */
    public int writeHeader() throws IOException {
        /* RIFF header */
        writeId("RIFF");
        writeInt(36 + mNumBytes);
        writeId("WAVE");

        /* fmt chunk */
        writeId("fmt ");
        writeInt(16);
        writeShort(mFormat);
        writeShort(mNumChannels);
        writeInt(mSampleRate);
        writeInt(mNumChannels * mSampleRate * mBitsPerSample / 8);
        writeShort((short)(mNumChannels * mBitsPerSample / 8));
        writeShort(mBitsPerSample);

        /* data chunk */
        writeId("data");
        writeInt(mNumBytes);

        LogUtils.d(TAG, "writeHeader sr:" + mSampleRate + " length:" + mNumBytes);

        return HEADER_LENGTH;
    }

    public void writeHeaderRm() throws IOException {
        byte[] buffer = new byte[HEADER_LENGTH];
        mFile.write(buffer);
        mFile.seek(0);
        /* RIFF header */
        writeId("RIFF");

        writeInt(36 + getDataSize());
        writeId("WAVE");

        /** format chunk */
        writeId("fmt ");
        writeInt(16);
        writeShort(FORMAT_PCM);
        writeShort(mNumChannels);
        writeInt(mSampleRate);

        writeInt(mNumChannels * mSampleRate * mBitsPerSample / 8);
        writeShort((short) (mNumChannels * mBitsPerSample / 8));
        writeShort(mBitsPerSample);

        /** data chunk */
        writeId("data");
        writeInt(getDataSize());
    }

    private void writeId(String id) throws IOException {
        for (int i = 0; i < id.length(); i++) mFile.write(id.charAt(i));
    }

    private void writeInt(int val) throws IOException {
        mFile.write(val >> 0);
        mFile.write(val >> 8);
        mFile.write(val >> 16);
        mFile.write(val >> 24);
    }

    private void writeShort(short val) throws IOException {
        mFile.write(val >> 0);
        mFile.write(val >> 8);
    }

    @Override
    public String toString() {
        return String.format(
                "WaveHeader format=%d numChannels=%d sampleRate=%d bitsPerSample=%d numBytes=%d",
                mFormat, mNumChannels, mSampleRate, mBitsPerSample, mNumBytes);
    }

    public int openNew(String path) {
        try {
            File file = new File(path);
            mFile = new RandomAccessFile(file, "rw");
            mFile.setLength(0);  //truncate file
            mFilePath = path;
            writeHeader();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return 0;
    }
    public int openRW(String path) {
        try {
            File file = new File(path);
            mFile = new RandomAccessFile(file, "rw");

            writeHeaderRm();

        } catch (IOException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public int getDataSize() throws IOException {
        return (int) (mFile.length() - HEADER_LENGTH);
    }

    public int close() {
        try {
            if (!mReadOnly) {
                mFile.seek(0);
                writeHeader();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (mFile != null) {
                    mFile.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return 0;
    }

    public int closeRm() {
        try {
            if (mFile != null){
                mFile.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public int write(byte[] buf, int size) {
        try {
            mFile.write(buf, 0, size);
            mNumBytes += size;
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

    private int mOutputChannels;
    private int mOutputSampleRate;
    private boolean mReseek = false;
    private float mConvertPosition = 0;
    private int mTempBufFrameOffset = 0;
    private int mTempBufFrame = 0;
    private int mInFrameOffset = 0;
    private int mInFrameEnd = -1;
    private int mConvertFrameCount = 0;
    private int mReadStatus;
    private byte [] mTempBuf = new byte[READER_TEMP_BUFFER_SIZE];

    public static final int READ_STATUS_ONGOING = 1;
    public static final int READ_STATUS_FINISH = 2;

    private static final int READER_TEMP_BUFFER_SIZE = 5120;

    public int open(String path) {
        try {
            mFilePath = path;
            mReadOnly = true;
            File file = new File(path);
            mFile = new RandomAccessFile(file, "r");

            readHeader();
            mReseek = true;
            mInFrameOffset = 0;
            mReadStatus = READ_STATUS_ONGOING;
            mInFrameEnd = -1;
            if(mNumChannels == 2) {
                FRAME_SIZE = 4;
            } else {
                FRAME_SIZE = 2;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return mNumBytes;
    }

    public void setOutputParameter(int sr, int ch) {
        LogUtils.d(TAG, "out sr:" + sr);
        mOutputSampleRate = sr;
        mOutputChannels = ch;
        mReseek = true;
    }

    public int getReadPosition() {
        return mInFrameOffset;
    }

    public int getReadStatus() {
        return mReadStatus;
    }

    public int setFrameStartEnd(int start, int end) {
        LogUtils.d(TAG, "setFrameStartEnd:" + start + " " + end);
        mInFrameOffset = start;
        mInFrameEnd = end;
        mReadStatus = READ_STATUS_ONGOING;

        try {
            mFile.seek(mInFrameOffset * FRAME_SIZE + HEADER_LENGTH);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public int seekMs(int time) {
        try {
            int pos = time * (mSampleRate / 1000);
            mReseek = true;
            mInFrameOffset = pos;
            mReadStatus = READ_STATUS_ONGOING;

            mFile.seek(pos * FRAME_SIZE + HEADER_LENGTH);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return 0;
    }

    private int resampleMono(byte[] pdest,
                         int size) {
        int i = 0;

        int outSample = size / FRAME_SIZE;

        if (mReseek) {
            mReseek = false;
            mConvertPosition = 0;
            mTempBufFrameOffset = 0;
            mTempBufFrame = 0;
            mConvertFrameCount = 0;
        }

        do {
            while (mTempBufFrame - mTempBufFrameOffset >= 2)
            {
                short out;

                float fract = mConvertPosition - (int) mConvertPosition;

                if (i == outSample) {
                    return i * FRAME_SIZE;
                }
                if (mInFrameEnd != -1 && mInFrameOffset > mInFrameEnd) {
                    mReadStatus = READ_STATUS_FINISH;
                    return i * FRAME_SIZE;
                }
                short leftSample, rightSample;

                leftSample = (short)(mTempBuf[2 * mTempBufFrameOffset] & 0xff | (mTempBuf[2 * mTempBufFrameOffset + 1] << 8));
                rightSample = (short)(mTempBuf[2 * (mTempBufFrameOffset+1)] & 0xff | (mTempBuf[2 * (mTempBufFrameOffset+1) + 1] << 8));

                out = (short) ((1 - fract) * leftSample + fract * rightSample);

                pdest[2 * i] = (byte) out;
                pdest[2 * i + 1] = (byte) (out >> 8);

                i ++;
                mConvertFrameCount ++;

                // update position fraction
                float new_position = mConvertFrameCount * (mSampleRate / (float)mOutputSampleRate);

//                LogUtils.d(TAG, "new:" + new_position + " pos:" + mConvertPosition + " mTempBufFrameOffset:" + mTempBufFrameOffset
//                        + " mInFrameOffset:" + mInFrameOffset + " frac:" + fract + " Sample:" + leftSample + " " + rightSample + " " + out);

                // update whole positions
                int increase = (int)new_position - (int) mConvertPosition;
                mConvertPosition = new_position;
                mTempBufFrameOffset += increase;
                mInFrameOffset += increase;

//                    LogUtils.d(TAG, "count:" + mConvertFrameCount + " sr:" + mSampleRate + " outSR:" + mOutputSampleRate);
            }
            mTempBufFrameOffset = 0;
            try {
                mFile.seek(mInFrameOffset * FRAME_SIZE + HEADER_LENGTH);
                mTempBufFrame = mFile.read(mTempBuf, 0, READER_TEMP_BUFFER_SIZE) / FRAME_SIZE;
            } catch (IOException e) {
                e.printStackTrace();
            }
//            LogUtils.d(TAG, "readFrame:" + mTempBufFrame + " offset:" + mInFrameOffset);
        } while (mTempBufFrame >= 2);

        mReadStatus = READ_STATUS_FINISH;
        return i * FRAME_SIZE;
    }

    private int resampleStereo(byte[] pdest,
                             int size) {
        int i = 0;
        int outSample = size / FRAME_SIZE;

        if (mReseek) {
            mReseek = false;
            mConvertPosition = 0;
            mTempBufFrameOffset = 0;
            mTempBufFrame = 0;
            mConvertFrameCount = 0;
        }

        do {
            while (mTempBufFrame - mTempBufFrameOffset >= 2)
            {
                short out;

                float fract = mConvertPosition - (int) mConvertPosition;

                if (i == outSample) {
                    return i * FRAME_SIZE;
                }
                if (mInFrameEnd != -1 && mInFrameOffset > mInFrameEnd) {
                    mReadStatus = READ_STATUS_FINISH;
                    return i * FRAME_SIZE;
                }
                short leftSample, rightSample;

                //left channel
                leftSample = (short)(mTempBuf[4 * mTempBufFrameOffset] & 0xff | (mTempBuf[4 * mTempBufFrameOffset + 1] << 8));
                rightSample = (short)(mTempBuf[4 * (mTempBufFrameOffset+1)] & 0xff | (mTempBuf[4 * (mTempBufFrameOffset+1) + 1] << 8));

                out = (short) ((1 - fract) * leftSample + fract * rightSample);

                pdest[4 * i] = (byte) out;
                pdest[4 * i + 1] = (byte) (out >> 8);

                //right channel
                leftSample = (short)(mTempBuf[4 * mTempBufFrameOffset + 2] & 0xff | (mTempBuf[4 * mTempBufFrameOffset + 3] << 8));
                rightSample = (short)(mTempBuf[4 * (mTempBufFrameOffset+1) + 2] & 0xff | (mTempBuf[4 * (mTempBufFrameOffset+1) + 3] << 8));

                out = (short) ((1 - fract) * leftSample + fract * rightSample);

                pdest[4 * i +2] = (byte) out;
                pdest[4 * i + 3] = (byte) (out >> 8);

                i ++;
                mConvertFrameCount ++;

                // update position fraction
                float new_position = mConvertFrameCount * (mSampleRate / (float)mOutputSampleRate);

//                LogUtils.d(TAG, "new:" + new_position + " pos:" + mConvertPosition + " mTempBufFrameOffset:" + mTempBufFrameOffset
//                        + " mInFrameOffset:" + mInFrameOffset + " frac:" + fract + " Sample:" + leftSample + " " + rightSample + " " + out);

                // update whole positions
                int increase = (int)new_position - (int) mConvertPosition;
                mConvertPosition = new_position;
                mTempBufFrameOffset += increase;
                mInFrameOffset += increase;

//                    LogUtils.d(TAG, "count:" + mConvertFrameCount + " sr:" + mSampleRate + " outSR:" + mOutputSampleRate);
            }
            mTempBufFrameOffset = 0;
            try {
                mFile.seek(mInFrameOffset * FRAME_SIZE + HEADER_LENGTH);
                mTempBufFrame = mFile.read(mTempBuf, 0, READER_TEMP_BUFFER_SIZE) / FRAME_SIZE;
            } catch (IOException e) {
                e.printStackTrace();
            }
//            LogUtils.d(TAG, "readFrame:" + mTempBufFrame + " offset:" + mInFrameOffset);
        } while (mTempBufFrame >= 2);

        mReadStatus = READ_STATUS_FINISH;
        return i * FRAME_SIZE;
    }

    private int resampleMonoBackward(byte[] pdest,
                             int size) {
        int i = 0;

        int outSample = size / FRAME_SIZE;

        if (mReseek) {
            mReseek = false;
            mConvertPosition = 0;
            mTempBufFrameOffset = 0;
            mTempBufFrame = 0;
            mConvertFrameCount = 0;
        }

        if (mInFrameOffset == 0) {
            Arrays.fill(pdest, (byte)0);
            return size;
        }

        do {
            while (mTempBufFrame - mTempBufFrameOffset >= 2)
            {
                short out;

                float fract = mConvertPosition - (int) mConvertPosition;

                if (i == outSample) {
                    return i * FRAME_SIZE;
                }
                short leftSample, rightSample;

                int bufBase = 2 * (mTempBufFrame - mTempBufFrameOffset);
                leftSample = (short)(mTempBuf[bufBase - 4] & 0xff | (mTempBuf[bufBase - 3] << 8));
                rightSample = (short)(mTempBuf[bufBase - 2] & 0xff | (mTempBuf[bufBase - 1] << 8));

                out = (short) ((1 - fract) * leftSample + fract * rightSample);

                pdest[2 * i] = (byte) out;
                pdest[2 * i + 1] = (byte) (out >> 8);

                i ++;
                mConvertFrameCount ++;

                // update position fraction
                float new_position = mConvertFrameCount * (mSampleRate / (float)mOutputSampleRate);

//                LogUtils.d(TAG, "new:" + new_position + " pos:" + mConvertPosition + " mTempBufFrameOffset:" + mTempBufFrameOffset
//                        + " mInFrameOffset:" + mInFrameOffset + " frac:" + fract + " Sample:" + leftSample + " " + rightSample + " " + out);

                // update whole positions
                int increase = (int)new_position - (int) mConvertPosition;
                mConvertPosition = new_position;
                mTempBufFrameOffset -= increase;
                mInFrameOffset += increase;

//                    LogUtils.d(TAG, "count:" + mConvertFrameCount + " sr:" + mSampleRate + " outSR:" + mOutputSampleRate);
            }
            mTempBufFrameOffset = 0;
            try {
                mFile.seek(mInFrameOffset * FRAME_SIZE > READER_TEMP_BUFFER_SIZE ?
                        mInFrameOffset * FRAME_SIZE - READER_TEMP_BUFFER_SIZE + HEADER_LENGTH :
                        HEADER_LENGTH);
                mTempBufFrame = mInFrameOffset * FRAME_SIZE > READER_TEMP_BUFFER_SIZE ?
                        READER_TEMP_BUFFER_SIZE / FRAME_SIZE :
                        mInFrameOffset;
                mFile.read(mTempBuf, 0, mTempBufFrame * FRAME_SIZE);

            } catch (IOException e) {
                e.printStackTrace();
            }
//            LogUtils.d(TAG, "readFrame:" + mTempBufFrame + " offset:" + mInFrameOffset);
        } while (mTempBufFrame >= 2);

        mInFrameOffset = 0;
        return i * FRAME_SIZE;
    }

    private int readFile(byte[] buf, int size) {
//        LogUtils.d(TAG, "readFile");
        int read = 0;
        if (mInFrameEnd != -1 && mInFrameOffset >= mInFrameEnd) {
            return 0;
        }

        try {
            read = mFile.read(buf, 0, size);
            mInFrameOffset += read / FRAME_SIZE;
            if (mInFrameEnd != -1 && mInFrameOffset > mInFrameEnd) {
                read -= (mInFrameOffset - mInFrameEnd) * FRAME_SIZE;
                mInFrameOffset = mInFrameEnd;
                mReadStatus = READ_STATUS_FINISH;
            }
            if (read <= 0)
                mReadStatus = READ_STATUS_FINISH;

        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
            LogUtils.e("read file fail "+ e.getMessage());
        }

        return read;
    }

    public int read(byte[] buf, int size) {
        if (mOutputSampleRate == 0 || mOutputSampleRate == mSampleRate) {
            return readFile(buf, size);
        }
        else if (mOutputSampleRate < 0) {
            return resampleMonoBackward(buf, size);
        }
        else {
            if(mNumChannels == 2) {
                return resampleStereo(buf, size);
            } else {
                return resampleMono(buf, size);
            }
        }
    }
}