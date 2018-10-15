
package com.smartisanos.sara.service;

import java.util.HashSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


import com.smartisanos.music.ISmartisanosMusicService;
import com.smartisanos.sara.service.MusicServiceManager.Callback;
import com.smartisanos.sara.util.LogUtils;
import com.smartisanos.sara.util.SaraConstant;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.Looper;
import android.os.RemoteException;
import android.os.SystemClock;

class MusicServiceConnection implements ServiceConnection {

    private boolean mIsBinded;
    private int mRepeatConnNum = 0;
    private long mLastConnTime = 0;

    private HashSet<Callback> mCallbacks;
    private ISmartisanosMusicService mMusicService;
    private MusicStateReceiver mMusicStateReceiver;

    private Context mContext;
    private Handler mHandler;
    private ExecutorService mSingleThreadExecutor;
    private MusicState mMusicState;

    public MusicServiceConnection(Context context) {
        mContext = context;
        mMusicState = new MusicState();
        mHandler = new Handler(Looper.getMainLooper());
        mCallbacks = new HashSet<Callback>();
        mMusicStateReceiver = new MusicStateReceiver();
        mSingleThreadExecutor = Executors.newSingleThreadExecutor();
    }

    public void addCallback(Callback callback) {
        if (callback != null) {
            mCallbacks.add(callback);
        }
    }

    public void removeCallback(Callback callback) {
        mCallbacks.remove(callback);
    }

    public void bindMusicService(Context context) {
        LogUtils.d("bindMusicService, mIsBinded=" + mIsBinded);
        if (!mIsBinded) {
            Intent intent = new Intent().setPackage(SaraConstant.MUSIC_PACKAGE);
            try {
                context.startService(intent);
                context.bindService(intent, this, Context.BIND_AUTO_CREATE);
                registerReceiver();
                mIsBinded = true;
            } catch (Exception e) {
            }
        }
    }

    public void unBindMusicService(Context context) {
        LogUtils.d("unBindMusicService, mIsBinded=" + mIsBinded);
        if (mIsBinded) {
            try {
                context.unbindService(this);
                unRegisterReceiver();
            } catch (Exception e) {
            }
        }
        mMusicService = null;
    }

    public ISmartisanosMusicService getMusicService() {
        return mMusicService;
    }

    public MusicState getMusicState() {
        return mMusicState;
    }

    public void playNewMusic(final long[] list, final int position, final int playMode,final boolean play, final boolean onlyOpen) {
        if (mMusicService == null) {
            return;
        }
        mSingleThreadExecutor.execute(new Runnable() {
            @Override
            public void run() {
                if (mMusicService == null) {
                    return;
                }
                try {
                    if (mMusicService.isPlaying()) {
                        mMusicService.pause();
                    }
//                    mMusicService.setPlayingMode(playMode);
                    if (SaraConstant.PLAYMODEL_NONE != playMode) {
                        mMusicService.setShuffleMode(SaraConstant.PLAYMODEL_SHUFFLEMODE);
                    }
                    mMusicService.open(list, position);
                    if (play && !onlyOpen){
                        mMusicService.play();
                    }

                } catch (RemoteException e) {
                    LogUtils.e("playNewMusic", e.getMessage());
                } catch (Exception e) {
                    LogUtils.e("mMusicService maybe null ", e.getMessage());
                }
            }
        });
    }

    public void continuePlayOrPause() {
        if (mMusicService == null) {
            return;
        }
        try {
            if (mMusicService.isPlaying()) {
                mMusicService.pause();
            } else {
                mMusicService.play();
            }
        } catch (RemoteException e) {
            LogUtils.e("continuePlayOrPause", e.getMessage());
        }
    }
    private void registerReceiver() {
        IntentFilter filter = new IntentFilter(SaraConstant.META_CHANGED);
        filter.addAction(SaraConstant.PLAYSTATE_CHANGED);
        mContext.registerReceiver(mMusicStateReceiver, filter);
    }

    private void unRegisterReceiver() {
        mContext.unregisterReceiver(mMusicStateReceiver);
    }

    private void updateMusicState() {
        mSingleThreadExecutor.execute(new Runnable() {

            @Override
            public void run() {
                if (null != mMusicService) {
                    try {
                        mMusicState.setPlaying(mMusicService.isPlaying());
                        mMusicState.setAudioId(mMusicService.getAudioId());
                        mMusicState.setAlbumId(mMusicService.getAlbumId());
                        mMusicState.setArtistId(mMusicService.getArtistId());
                        LogUtils.d("updateMusicState, mMusicState=" + mMusicState);
                    } catch (RemoteException e) {
                    } catch (NullPointerException e) {
                        LogUtils.e("set music state fail");
                    }
                }
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (mCallbacks != null){
                            for (Callback callback : mCallbacks) {
                                callback.onMusicStateChange();
                            }
                        }
                    }
                });
            }
        });
    }

    @Override
    public void onServiceConnected(android.content.ComponentName name,
            android.os.IBinder service) {
        mMusicService = ISmartisanosMusicService.Stub.asInterface(service);
        LogUtils.d("onServiceConnected ComponentName=" + name + ", IBinder=" + service
                + ", mMusicService=" + mMusicService);
        mSingleThreadExecutor.execute(new Runnable() {
            @Override
            public void run() {
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (mCallbacks != null) {
                            for (Callback callback : mCallbacks) {
                                callback.onMusicServiceConnected();
                            }
                        }
                    }
                });
            }
        });
        updateMusicState();
    }

    @Override
    public void onServiceDisconnected(android.content.ComponentName name) {
        if (null != mMusicService) {
            mMusicService = null;
        }

        long repeatInterval = SystemClock.elapsedRealtime() - mLastConnTime;
        LogUtils.d("onServiceDisconnected ComponentName=" + name
                + ", repeat connect num=" + mRepeatConnNum + ", repeatInterval= " + repeatInterval);
        long connectWaitTime = SaraConstant.REPEAT_CONNECT_SERVER_MAX_TIME - repeatInterval;
        if (connectWaitTime <= 0) {
            mRepeatConnNum = 0;
        }

        if (mRepeatConnNum < SaraConstant.REPEAT_CONNECT_MAX_NUMS) {
            bindMusicService(mContext);
            mRepeatConnNum++;
            // record final connect time
            mLastConnTime = SystemClock.elapsedRealtime();
        } else {
            LogUtils.d("repeat connect music service need waiting: " + connectWaitTime);
        }
    }

    private class MusicStateReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            LogUtils.d("Music state receive, action=" + intent.getAction());
            updateMusicState();
        }
    }
}
