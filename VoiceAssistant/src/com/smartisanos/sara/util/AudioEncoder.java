package com.smartisanos.sara.util;

/**
 * Created by fangle on 17-6-10.
 *
 *
 sample code
 AudioEncoder mp3Encoder = new AudioEncoder("/sdcard/smartisan/Test/Test.mp3");
 mp3Encoder.setFormat(44100, 2, 1);
 mp3Encoder.doEncode("/sdcard/smartisan/Test/test.wav");
 *
 *
 */

public class AudioEncoder {
    static{
        System.loadLibrary("audioencoder_va");
    }

    private static long sNativeEncoder = 0;     // record native encoder ptr.

    public AudioEncoder(String dstPath){
        native_setup(dstPath);
    }

    native void native_setup(String dstPath);
    native boolean setFormat(int samplerate, int numchannels, int pcmFormat);
    native boolean doEncode(String srcPath);
}
