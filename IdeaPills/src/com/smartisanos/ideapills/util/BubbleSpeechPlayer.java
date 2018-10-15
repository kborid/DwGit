package com.smartisanos.ideapills.util;

import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Looper;
import android.provider.Settings;
import android.widget.Toast;

import com.smartisanos.ideapills.common.util.TaskHandler;

import com.smartisanos.ideapills.R;
import com.smartisanos.ideapills.entity.BubbleItem;
import com.smartisanos.ideapills.common.util.UIHandler;

import java.io.IOException;
import java.lang.ref.WeakReference;

import smartisanos.api.SettingsSmt;

public class BubbleSpeechPlayer implements MediaPlayer.OnCompletionListener, MediaPlayer.OnErrorListener{
    private static final LOG log = LOG.getInstance(BubbleSpeechPlayer.class);

    private volatile static BubbleSpeechPlayer sInstance = null;

    public synchronized static BubbleSpeechPlayer getInstance(Context context) {
        if (sInstance == null) {
            synchronized (BubbleSpeechPlayer.class) {
                if (sInstance == null) {
                    sInstance = new BubbleSpeechPlayer(context);
                }
            }
        }
        return sInstance;
    }

    private Context mContext = null;
    private MediaPlayer mMediaPlayer = null;
    private AudioManager mAudioManager = null;
    private WeakReference<SpeechPlayerCallBack> mSpeechPlayerCallBack = new WeakReference<SpeechPlayerCallBack>(null);
    private BubbleItem mBubbleItem = null;
    private volatile boolean isPlaying = false;

    private BubbleSpeechPlayer(Context context) {
        mContext = context;
        mMediaPlayer = getNewMediaplayer();
        mAudioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
    }

    private MediaPlayer getNewMediaplayer() {
        MediaPlayer mediaPlayer = new MediaPlayer();
        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        mediaPlayer.setVolume(1.0f, 1.0f);
        return mediaPlayer;
    }

    private void playStopped() {
        if (isPlaying) {
            isPlaying = false;
            if (mAudioManager != null) {
                mAudioManager.abandonAudioFocus(mAudioFocusChangeListener);
            }
        }
    }

    public void onCompletion(MediaPlayer mp) {
        playStopped();
        final SpeechPlayerCallBack callbackRef = mSpeechPlayerCallBack.get();
        if (callbackRef != null) {
            if (Looper.getMainLooper() != Looper.myLooper()) {
                UIHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        callbackRef.onCompleted(mBubbleItem);
                        mMediaPlayer.reset();
                    }
                });
            } else {
                callbackRef.onCompleted(mBubbleItem);
                mMediaPlayer.reset();
            }
        }
    }

    public boolean onError(MediaPlayer mp, int what, int extra) {
        playStopped();
        mMediaPlayer.reset();
        return true;
    }

    public interface SpeechPlayerCallBack{
        void onStarted(BubbleItem item, boolean isStarted);
        void onCompleted(BubbleItem item);
        void onDisconnected(BubbleItem item);
        void onFocusChanged(boolean isLossFocus);
    }

    public void replaceSpeechPlayCallBack(final SpeechPlayerCallBack callBack) {
        mSpeechPlayerCallBack = new WeakReference(callBack);
    }

    public void playSpeech(final BubbleItem item, final SpeechPlayerCallBack callBack) {
        final SpeechPlayerCallBack callbackRef = mSpeechPlayerCallBack.get();
        if (mBubbleItem != null && item != mBubbleItem) {
            stop();
            if (callbackRef != null) {
                callbackRef.onFocusChanged(true);
            }
        }
        TaskHandler.post(new Runnable() {
            @Override
            public void run() {
                if (!Utils.checkIsVoiceAvailable(mContext, item.getUri())) {
                    return;
                }
                if (isMute(mContext) && !isHeadsetConnected(mContext)) {
                    GlobalBubbleUtils.showSystemToast(mContext, R.string.mute_mode_is_turned_on, Toast.LENGTH_SHORT);
                }
                final BubbleSpeechPlayer player = BubbleSpeechPlayer.this;
                boolean isPrepared = false;
                try {
                    log.info("play speech uri=" + String.valueOf(item.getUri()));
                    UIHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            if (callbackRef != null) {
                                callbackRef.onDisconnected(mBubbleItem);
                            }
                        }
                    });
                    mSpeechPlayerCallBack = new WeakReference(callBack);
                    mMediaPlayer.release();
                    mMediaPlayer = getNewMediaplayer();
                    mMediaPlayer.setDataSource(mContext, item.getUri());
                    mMediaPlayer.prepare();
                    mMediaPlayer.setOnCompletionListener(player);
                    mMediaPlayer.setOnErrorListener(player);
                    mBubbleItem = item;
                    if (mAudioManager != null) {
                        mAudioManager.requestAudioFocus(mAudioFocusChangeListener, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT);
                    }
                    mMediaPlayer.start();
                    isPlaying = true;
                    isPrepared = true;
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    callBack.onStarted(item, isPrepared);
                }
            }
        });
    }

    private AudioManager.OnAudioFocusChangeListener mAudioFocusChangeListener = new AudioManager.OnAudioFocusChangeListener() {
        @Override
        public void onAudioFocusChange(int focusChange) {
            SpeechPlayerCallBack callbackRef = mSpeechPlayerCallBack.get();
            if (callbackRef != null) {
                callbackRef.onFocusChanged(focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT);
            }
        }
    };

    public int getCurrentPosition(BubbleItem item) {
        if(mBubbleItem == item) {
            try {
                return mMediaPlayer.getCurrentPosition();
            } catch (IllegalStateException e) {
                // may get current pos in run state of player
            }
        }
        return 0;
    }

    public boolean isPlayingBubble(BubbleItem item) {
        if (mBubbleItem != null && isPlaying && mBubbleItem == item) {
            return true;
        }
        return false;
    }

    public void stop() {
        playStopped();
        mMediaPlayer.stop();
        mMediaPlayer.reset();
    }

    public long getVoiceCreateTime(BubbleItem bubble) {
        return bubble.getTimeStamp();
    }

    public static boolean isMute(Context context) {
        return Settings.System.getInt(context.getContentResolver(),
                SettingsSmt.get_VOLUME_PANEL_MUTE_ENABLE(), 0) == 1;
    }

    private boolean isHeadsetConnected(Context context) {
        AudioManager audioManager = (AudioManager)context.getSystemService(Context.AUDIO_SERVICE);
        if (audioManager.isWiredHeadsetOn()) {
            return true;
        }
        if (audioManager.isBluetoothA2dpOn()) {
            return true;
        }
        return false;
    }
}