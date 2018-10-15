
package com.smartisanos.sara.service;

import java.util.HashMap;

import com.smartisanos.music.ISmartisanosMusicService;
import com.smartisanos.sara.util.SaraConstant;

import android.content.Context;

public class MusicServiceManager {

    private static final HashMap<Context, MusicServiceManager> sManagers = new HashMap<Context, MusicServiceManager>();

    private Context mContext;
    private MusicServiceConnection mMusicServiceConnection;

    private MusicServiceManager(Context context) {
        mContext = context;
        mMusicServiceConnection = new MusicServiceConnection(context);
    }

    public synchronized static MusicServiceManager from(Context context) {
        MusicServiceManager manager = sManagers.get(context);
        if (manager == null) {
            manager = new MusicServiceManager(context);
            sManagers.put(context, manager);
        }

        return manager;
    }

    public synchronized static boolean remove(Context context) {
        MusicServiceManager manager = sManagers.get(context);
        if (manager != null) {
            manager.mMusicServiceConnection = null;
            manager = null;
        }
        boolean  s = (sManagers.remove(context) != null);
        return s;
    }

    public void addCallback(Callback callback) {
        mMusicServiceConnection.addCallback(callback);
    }

    public void removeCallback(Callback callback) {
        mMusicServiceConnection.removeCallback(callback);
    }

    public ISmartisanosMusicService getMusicService() {
        return mMusicServiceConnection.getMusicService();
    }

    public void bindMusicService() {
        mMusicServiceConnection.bindMusicService(mContext);
    }

    public void unBindMusicService() {
        mMusicServiceConnection.unBindMusicService(mContext);
    }

    public MusicState getMusicState() {
        return mMusicServiceConnection.getMusicState();
    }

    public void playNewMusic(long audioId) {
        playNewMusic(new long[] {
                audioId
        }, 0, SaraConstant.PLAYMODEL_NONE,true, false);
    }

    public void playNewMusic(long[] list, int position, int playMode,boolean play, boolean onlyOpen) {
        mMusicServiceConnection.playNewMusic(list, position, playMode,play, onlyOpen);
    }

    public void continuePlayOrPause() {
        mMusicServiceConnection.continuePlayOrPause();
    }

    public interface Callback {
        void onMusicServiceConnected();

        void onMusicStateChange();
    }
}
