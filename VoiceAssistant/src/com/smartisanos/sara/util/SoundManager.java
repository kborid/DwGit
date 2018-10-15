package com.smartisanos.sara.util;


import com.smartisanos.sara.R;

import android.app.Activity;
import android.content.Context;
import android.media.AudioManager;
import android.media.SoundPool;
import android.media.SoundPool.OnLoadCompleteListener;
import android.media.ToneGenerator;

public class SoundManager {

    private static final String TAG = "SoundManager::";

    private static final int TONE_LENGTH_MS = 300;

    private static final int TONE_RELATIVE_VOLUME = 100;

    private static int mNotifySoundID = 0;

    private static int mSearchSoundID = 0;

    public static void playTone(Activity activity) {
        // Also do nothing if the phone is in silent mode.
        // We need to re-check the ringer mode for *every* playTone()
        // call, rather than keeping a local flag that's updated in
        // onResume(), since it's possible to toggle silent mode without
        // leaving the current activity (via the ENDCALL-longpress menu.)
        AudioManager audioManager = (AudioManager)activity.getSystemService(Context.AUDIO_SERVICE);
        int ringerMode = audioManager.getRingerMode();
//        if ((ringerMode == AudioManager.RINGER_MODE_SILENT)
//                || (ringerMode == AudioManager.RINGER_MODE_VIBRATE))
//        {
//            return;
//        }
        ToneGenerator toneGenerator = null;
        try
        {
            // we want the user to be able to control the volume of the
            // dial tones
            // outside of a call, so we use the stream type that is also
            // mapped to the
            // volume control keys for this activity
            toneGenerator = new ToneGenerator(AudioManager.STREAM_VOICE_CALL,
                    TONE_RELATIVE_VOLUME);
            activity.setVolumeControlStream(AudioManager.STREAM_MUSIC);
        } catch (RuntimeException e)
        {
            LogUtils.w(TAG,
                    "Exception caught while creating local tone generator: "
                            + e);
            toneGenerator = null;
        }

        if (toneGenerator == null)
        {
            LogUtils.w(TAG, "playTone: mToneGenerator == null");
            return;
        }

        // Start the new tone (will stop any playing tone)
        toneGenerator.startTone(ToneGenerator.TONE_DTMF_2, TONE_LENGTH_MS);
    }

    private static boolean mIsAudioAdded = false;
    private static SoundPool soundPool = null;
    public static void playSound(Context context, boolean begin) {
//        playTone(context);
//        MediaPlayer mp = new MediaPlayer();
//        try {
//            mp.setDataSource("/sdcard/can_speak_tone.wav");
//            mp.prepare();
//        } catch (IllegalArgumentException e) {
//            // TODO Auto-generated catch block
//            e.printStackTrace();
//        } catch (SecurityException e) {
//            // TODO Auto-generated catch block
//            e.printStackTrace();
//        } catch (IllegalStateException e) {
//            // TODO Auto-generated catch block
//            e.printStackTrace();
//        } catch (IOException e) {
//            // TODO Auto-generated catch block
//            e.printStackTrace();
//        }
//
//
//
//        mp.start();

        if (!mIsAudioAdded || soundPool == null) {
            soundPool = new SoundPool(10, AudioManager.STREAM_SYSTEM, 0);
            mNotifySoundID = soundPool.load(context, R.raw.talkroom_begin, 1);
            mSearchSoundID = soundPool.load(context, R.raw.voice, 1);
            soundPool.setOnLoadCompleteListener(new OnLoadCompleteListener() {
                @Override
                public void onLoadComplete(SoundPool soundPool, int sampleId,
                        int status) {
                    if(sampleId == mNotifySoundID)
                        soundPool.play(mNotifySoundID, 1, 1, 0, 0, 1);
                    mIsAudioAdded = true;
                }
            });
        } else {
            if (begin) {
                soundPool.play(mNotifySoundID, 1, 1, 0, 0, 1);
            } else {
                soundPool.play(mSearchSoundID, 1, 1, 0, 0, 1);
            }
        }
    }
}
