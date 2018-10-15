
package com.smartisanos.sara.util;

import android.content.ContentResolver;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnErrorListener;
import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.provider.Settings;
import android.service.onestep.GlobalBubble;
import android.text.TextUtils;
import android.widget.Toast;

import com.smartisanos.ideapills.common.util.FileUtils;
import com.smartisanos.ideapills.common.util.TaskHandler;
import com.smartisanos.ideapills.common.util.UIHandler;
import com.smartisanos.sara.R;
import com.smartisanos.sara.SaraApplication;

import smartisanos.api.SettingsSmt;
import smartisanos.util.LogTag;
import java.io.File;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;

public class BubbleSpeechPlayer implements MediaPlayer.OnCompletionListener, OnErrorListener {

    private volatile static BubbleSpeechPlayer sInstance = null;

    public synchronized static BubbleSpeechPlayer getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new BubbleSpeechPlayer(SaraApplication.getInstance().getApplicationContext());
        }
        return sInstance;
    }

    public static class VoiceInfo {
        byte[] mImgData;
        int mDura;
        GlobalBubble mGlobalBubble;

        public byte[] getImgData() {
            return mImgData;
        }

        public int getDura() {
            return mDura;
        }

        public GlobalBubble getGlobalBubble() {
            return mGlobalBubble;
        }
    }

    public interface VoiceInfoPrepareCallBack {
        void prepared(VoiceInfo info);
    }

    private Context mContext = null;
    private MediaPlayer mMediaPlayer = null;
    private AudioManager mAudioManager = null;
    private Object mEmpty = new Object();
    private Map<GlobalBubble, Object> mCacheVoiceInfo = new HashMap<GlobalBubble, Object>();
    private WeakReference<SpeechPlayerCallBack> mSpeechPlayerCallBack = new WeakReference<SpeechPlayerCallBack>(null);
    private GlobalBubble mGlobalBubble = null;

    private Handler mHandler;

    private BubbleSpeechPlayer(Context context) {
        mContext = context;
        mMediaPlayer = new MediaPlayer();
        mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        mMediaPlayer.setOnErrorListener(this);
        mMediaPlayer.setVolume(1.0f, 1.0f);
        mAudioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);

        // create our thread
        HandlerThread thread = new HandlerThread(BubbleSpeechPlayer.class.getName());
        thread.start();
        mHandler = new BubbleSpeechHandler(thread.getLooper());
    }

    public void onCompletion(MediaPlayer mp) {
        mAudioManager.abandonAudioFocus(null);
        final SpeechPlayerCallBack callbackRef = mSpeechPlayerCallBack.get();
        if (callbackRef != null) {
            if (Looper.getMainLooper() != Looper.myLooper()) {
                UIHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        callbackRef.onCompleted(mGlobalBubble);
                    }
                });
            } else {
                callbackRef.onCompleted(mGlobalBubble);
            }
        }
        mMediaPlayer.reset();
    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        LogUtils.e(" media play   onError(what): " + what + "," + extra);
        //MEDIA_ERROR_SYSTEM = -2147483648
        switch (what) {
            case MediaPlayer.MEDIA_ERROR_IO:
            case MediaPlayer.MEDIA_ERROR_SERVER_DIED:
            case MediaPlayer.MEDIA_ERROR_UNKNOWN:
            case MediaPlayer.MEDIA_ERROR_MALFORMED:
            case MediaPlayer.MEDIA_ERROR_UNSUPPORTED:
            case MediaPlayer.MEDIA_ERROR_TIMED_OUT:
            case -2147483648:
                ToastUtil.showToast(R.string.error_audio_file);
                mMediaPlayer.reset();
                break;
            default:
                break;
        }
        return true;
    }
    public interface SpeechPlayerCallBack {
        void onCompleted(GlobalBubble item);

        void onDisconnected(GlobalBubble item);
    }

    public void prepareData(final GlobalBubble item) {
        if (item == null || TextUtils.isEmpty(item.getUri() + "")) {
            return;
        }
        if (!mCacheVoiceInfo.containsKey(item)) {
            mCacheVoiceInfo.put(item, mEmpty);
            TaskHandler.post(new Runnable() {
                @Override
                public void run() {
                    prepareInfo(item);
                }
            });
        }
    }

    private VoiceInfo prepareInfo(GlobalBubble item) {
        LogTag.d("BubbleSpeechPlayer", "prepareInfo:" + item + " ,this:" + this);
        VoiceInfo info = new VoiceInfo();
        info.mGlobalBubble = item;
        info.mImgData = getWaveData(item);
        MediaPlayer mediaPlayer = new MediaPlayer();
        try {
            mediaPlayer.setDataSource(mContext, item.getUri());
            mediaPlayer.prepare();
            info.mDura = mediaPlayer.getDuration();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            mediaPlayer.release();
        }
        Object object = mCacheVoiceInfo.get(info.mGlobalBubble);
        if (object != null) {
            if (object instanceof VoiceInfoPrepareCallBack) {
                ((VoiceInfoPrepareCallBack) object).prepared(info);
            }
        }
        mCacheVoiceInfo.put(info.mGlobalBubble, info);
        return info;
    }

    public synchronized void playSpeech(GlobalBubble item, SpeechPlayerCallBack callBack) {
        if (item == null) {
            return;
        }
        try {
            mMediaPlayer.reset();
            mMediaPlayer.setDataSource(mContext, item.getUri());
            SpeechPlayerCallBack callbackRef = mSpeechPlayerCallBack.get();
            if (callbackRef != null) {
                callbackRef.onDisconnected(item);
            }
            mSpeechPlayerCallBack = new WeakReference(callBack);
            mMediaPlayer.prepare();
            mMediaPlayer.setOnCompletionListener(this);
            mGlobalBubble = item;
            mAudioManager.requestAudioFocus(null, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT);
            mMediaPlayer.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private Bitmap generateWaveBitmap(byte[] waveData) {
        int measureWidth = 300;
        int measureHeight = 100;
        Resources sRes = mContext.getResources();
        int waveFormHeight = sRes
                .getDimensionPixelSize(R.dimen.bubble_list_item_wave_height);
        float waveHeightScale = waveFormHeight * 1.0f / 255;
        int waveFormMarginLeft = 0;
        int waveFormMarginRight = 0;
        Bitmap bitmap = Bitmap.createBitmap(measureWidth, measureHeight, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        // initialize each wave form width from resource
        int waveFormFrameWidth = sRes
                .getDimensionPixelSize(R.dimen.bubble_wave_form_size);

        // setup wave form draw paint
        Paint waveFormDrawPaint = new Paint();
        waveFormDrawPaint.setColor(sRes.getColor(R.color.bubble_wave_line_text_color));
        waveFormDrawPaint.setStrokeWidth(waveFormFrameWidth);

        // draw wave
        if (waveData != null) {
            canvas.save();
            canvas.clipRect(waveFormMarginLeft, 0.0f, measureWidth
                    - waveFormMarginRight, measureHeight);
            int length = waveData.length;
            float waveFormDrawStartPosition = waveFormMarginLeft;
            float waveFormPositionTranslate = waveFormFrameWidth;

            float middleRange = measureWidth - waveFormMarginLeft - waveFormMarginRight;
            int baseCenterY = (measureHeight) / 2;

            for (float p = 0; p < middleRange; p += waveFormFrameWidth) {
                float pos = p / middleRange * length;
                int i = (int) pos;
                float datal, datar;
                short data;

                byte originalData = 0;
                if (i >= 0 && i < length) {
                    originalData = waveData[i];
                }
                if (originalData >= 0) {
                    datal = originalData;
                } else {
                    datal = originalData & 0xff;
                }

                originalData = 0;
                if (i + 1 >= 0 && i + 1 < length) {
                    originalData = waveData[i + 1];
                }
                if (originalData >= 0) {
                    datar = originalData;
                } else {
                    datar = originalData & 0xff;
                }

                data = (short) ((datar - datal) * (pos - i) + datal);

                // draw one wave form frame
                float waveDataHalfHeight = data * waveHeightScale / 2;
                canvas.drawLine(waveFormDrawStartPosition, baseCenterY
                        - waveDataHalfHeight, waveFormDrawStartPosition,
                        baseCenterY + waveDataHalfHeight, waveFormDrawPaint);

                // move the wave form draw position to next frame
                waveFormDrawStartPosition += waveFormPositionTranslate;
            }

            canvas.restore();
        }
        return bitmap;
    }

    //
    public int getCurrentPosition(GlobalBubble item) {
        if (mGlobalBubble == item) {
            return mMediaPlayer.getCurrentPosition();
        }
        return 0;
    }

    public VoiceInfo getVoiceInfo(GlobalBubble item, VoiceInfoPrepareCallBack callBack) {
        if (item == null) {
            return null;
        }
        Object object = mCacheVoiceInfo.get(item);
        if (object instanceof VoiceInfo) {
            return (VoiceInfo) object;
        } else {
            mCacheVoiceInfo.put(item, callBack);
            return null;
        }
    }

    public int getDuration(GlobalBubble item) {
        VoiceInfo info;
        Object object = mCacheVoiceInfo.get(item);
        if (object != null && object instanceof VoiceInfo) {
            info = (VoiceInfo) object;
        } else {
            info = prepareInfo(item);
        }
        return info.getDura();
    }

    public long getCreatedTime(GlobalBubble item) {
        Uri uri = item.getUri();
        String path = SaraUtils.formatContent2FilePath(mContext, uri);
        if (TextUtils.isEmpty(path)) {
            return 0;
        } else {
            final File file = new File(path);
            return file.lastModified();
        }
    }

    public byte[] getWaveData(GlobalBubble item) {
        Uri uri = item.getUri();
        if (uri == null){
            return null;
        }
        final String scheme = uri.getScheme();
        if (ContentResolver.SCHEME_CONTENT.equals(scheme)) {
            String path = SaraUtils.formatContent2FilePath(mContext, uri);
            final File file = new File(path + ".wave");
             byte[] data = FileUtils.readFileToByteArray(file);
             return data;
        } else {
            LogUtils.e("the scheme of uri is not file");
        }
        return null;
    }

    public void generateWaveFile(String filePath) {
        generateWaveFile(filePath, 20, filePath + ".wave");
    }

    public void generateWaveFile(String filePath, int intervalMs, String generateWaveFilePath) {
        WaveHeader wave = new WaveHeader();
        wave.open(filePath);
        int channel = wave.getNumChannels();
        int samplerate = wave.getSampleRate();
        WaveformGenerator waveForm = null;
        try {
            waveForm = new WaveformGenerator(intervalMs, samplerate, null);
            waveForm.open(generateWaveFilePath);
            waveForm.setChannel((short) channel);
            byte[] tempBuf = new byte[10240];
            do {
                int read = wave.read(tempBuf, tempBuf.length);
                if (read <= 0) {
                    break;
                }
                waveForm.generate(tempBuf, read);
            } while (wave.getReadStatus() != WaveHeader.READ_STATUS_FINISH);
        } catch (IllegalArgumentException e) {
            new File(filePath + ".wave").delete();
            LogUtils.e("fail to generateWaveFile for " + filePath + " exception:" + e);
            return;
        } finally {
            wave.close();
            waveForm.close();
        }
    }

    public boolean isPlaying() {
        return mMediaPlayer.isPlaying();
    }

    public boolean isPlayingBubble(GlobalBubble item) {
        if (mGlobalBubble != null && isPlaying() && mGlobalBubble == item) {
            return true;
        }
        return false;
    }

    public synchronized void stop() {
        mMediaPlayer.stop();
        mAudioManager.abandonAudioFocus(null);
    }

    private static final int MSG_DELETE_BUBBLE_FILE = 0;

    private class BubbleSpeechHandler extends Handler {

        public BubbleSpeechHandler(Looper looper) {
            super(looper, null, false);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_DELETE_BUBBLE_FILE:
                    GlobalBubble bubble = (GlobalBubble) msg.obj;
                    new File(bubble.getUri().getPath()).delete();
                    new File(bubble.getUri().getPath() + ".wave").delete();
                    break;
            }
        }
    }

    public void showMuteTipIfNeeded(Context context) {
        if (isMute(context) && !isHeadsetConnected(context)) {
            Toast.makeText(context, R.string.mute_mode_is_turned_on, Toast.LENGTH_SHORT).show();
        }
    }

    private boolean isMute(Context context) {
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
