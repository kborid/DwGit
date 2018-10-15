package com.smartisanos.sara.util;

import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.util.Log;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;

public class WaveFileGenerator {
    private static final String TAG = "WaveFileGenerator";
    private static final boolean DEBUG = false;

    private static final int TIMEOUT_US = 1000;

    private static final String KEY_PCM_ENCODING = "pcm-encoding";

    private boolean mStopGenerate;

    public WaveFileGenerator() {
        mStopGenerate = false;
    }

    public boolean generateWav(FileDescriptor srcFile, String dstPath) {
        if (srcFile == null || dstPath == null) {
            return false;
        }
        Log.d(TAG, "generateWav srcFile=" + srcFile.toString() + " dstPath=" + dstPath);

        MediaExtractor extractor = new MediaExtractor();
        try {
            extractor.setDataSource(srcFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
        int trackNum = extractor.getTrackCount();
        if (trackNum <= 0) {
            return false;
        }

        String mimeType = null;
        MediaFormat inputFormat = null;

        for (int i = 0; i < trackNum; i++) {
            MediaFormat format = extractor.getTrackFormat(i);
            String mime = format.getString(MediaFormat.KEY_MIME);
            if (DEBUG) Log.d(TAG, "mime=" + mime);
            if (mime.contains("audio")) {
                extractor.selectTrack(i);
                inputFormat = format;
                mimeType = mime;
                if (DEBUG) Log.d(TAG, "inputFormat=" + inputFormat.toString());
                break;
            }
        }

        if (mimeType == null || inputFormat == null) {
            return false;
        }

        MediaCodec decoder = null;
        ByteBuffer[] decoderInputBuffers = null;
        ByteBuffer[] decoderOutputBuffers = null;

        try {
            decoder = MediaCodec.createDecoderByType(mimeType);
            decoder.configure(inputFormat, null, null, 0);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }

        if (DEBUG) Log.d(TAG, "outputFormat:" + decoder.getOutputFormat());

        /*
        * KEY_PCM_ENCODING value added in API level 24
        * before level 24, get KEY_PCM_ENCODING will crashed
        * if not able to get KEY_PCM_ENCODING value, use default value '2'
        * */
        int pcmEncodingValue = 2;
        try {
            pcmEncodingValue = decoder.getOutputFormat().getInteger(KEY_PCM_ENCODING);
        } catch (Exception e) {
            e.printStackTrace();
        }

        File file = null;
        WaveHeader outputStream = null;
        try {
            file = new File(dstPath);
            if (!file.exists()) {
                file.createNewFile();
            }
            outputStream = new WaveHeader(WaveHeader.FORMAT_PCM,
                    (short) inputFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT),
                    inputFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE),
                    getBitDepth(pcmEncodingValue));
            outputStream.openNew(dstPath);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return false;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }

        decoder.start();

        decoderInputBuffers = decoder.getInputBuffers();
        decoderOutputBuffers = decoder.getOutputBuffers();
        MediaCodec.BufferInfo decoderOutputBufferInfo = new MediaCodec.BufferInfo();

        boolean allDone = false;
        boolean extractorDone = false;
        boolean decodeDone = false;
        int pendingDecoderOutputBufferIndex = -1;
        while (!allDone) {
            while (!extractorDone) {
                int inputIndex = decoder.dequeueInputBuffer(TIMEOUT_US);

                if (inputIndex == MediaCodec.INFO_TRY_AGAIN_LATER) {
                    Log.i(TAG, "DECODER: dequeueInputBuffer, try again");
                    break;
                }

                ByteBuffer inputBuffer;
                if (android.os.Build.VERSION.SDK_INT >= 21) {
                    inputBuffer = decoder.getInputBuffer(inputIndex);
                } else {
                    inputBuffer = decoderInputBuffers[inputIndex];
                }
                int size = extractor.readSampleData(inputBuffer, 0);
                long presentationTime = extractor.getSampleTime();

                if (size < 0) {
                    extractorDone = true;
                } else {
                    decoder.queueInputBuffer(inputIndex,
                            0, size, presentationTime, extractor.getSampleFlags());
                    extractorDone = !extractor.advance();
                }
                if (extractorDone) {
                    decoder.queueInputBuffer(inputIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                }

                break;
            }

            while (!decodeDone && pendingDecoderOutputBufferIndex == -1) {
                int decoderOutputIndex = decoder.dequeueOutputBuffer(
                        decoderOutputBufferInfo, TIMEOUT_US);

                if (decoderOutputIndex == MediaCodec.INFO_TRY_AGAIN_LATER) {
                    Log.i(TAG, "DECODER: dequeueOutputBuffer, try again");
                    break;
                } else if (decoderOutputIndex == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                    decoderOutputBuffers = decoder.getOutputBuffers();
                    Log.i(TAG, "DECODER: dequeueOutputBuffer, INFO_OUTPUT_BUFFERS_CHANGED");
                    break;
                } else if (decoderOutputIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    Log.i(TAG, "DECODER: dequeueOutputBuffer, INFO_OUTPUT_FORMAT_CHANGED:"
                            + decoder.getOutputFormat()
                    );
                    break;
                }

                if ((decoderOutputBufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                    decoder.releaseOutputBuffer(decoderOutputIndex, false);
                    if (DEBUG) Log.d(TAG, "BUFFER_FLAG_CODEC_CONFIG release buffer");
                }

                pendingDecoderOutputBufferIndex = decoderOutputIndex;
                break;
            }

            while (pendingDecoderOutputBufferIndex != -1) {
                int size = decoderOutputBufferInfo.size;
                if (DEBUG) Log.d(TAG, "DECODER: size=" + size);
                if (size > 0) {
                    ByteBuffer decoderOutputBuffer =
                            decoderOutputBuffers[pendingDecoderOutputBufferIndex]
                                    .duplicate();
                    byte[] bytes = new byte[decoderOutputBuffer.remaining()];
                    decoderOutputBuffer.get(bytes);

                    outputStream.write(bytes, size);
                }

                decoder.releaseOutputBuffer(pendingDecoderOutputBufferIndex, false);
                pendingDecoderOutputBufferIndex = -1;
                if ((decoderOutputBufferInfo.flags
                        & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                    if (DEBUG) Log.i(TAG, "Decode DONE!!!");
                    decodeDone = true;
                    allDone = true;
                }
                break;
            }

            if (mStopGenerate) {
                decoder.stop();
                break;
            }
        }

        extractor.release();
        decoder.release();

        outputStream.close();
        outputStream = null;

        if (!allDone) {
            File dstFile = new File(dstPath);
            if (dstFile.exists()) {
                dstFile.delete();
                Log.d(TAG, "generate interrupt, delete file!");
            }
            return false;
        }

        return true;
    }

    public void stopGenerate() {
        mStopGenerate = true;
    }

    private short getBitDepth(int pcmEncodeing) {
        switch (pcmEncodeing) {
            case 2:
                return (short) 16;
            case 3:
                return (short) 8;
            default:
                break;
        }

        return (short) 16;
    }
}
