package com.smartisanos.ideapills.common.util;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.SystemClock;
import android.util.Log;

import java.util.HashMap;
import java.util.Iterator;

import smartisanos.app.voiceassistant.IVoiceAssistantCallback;
import smartisanos.app.voiceassistant.IVoiceAssistantService;
import smartisanos.app.voiceassistant.VoiceAssistantCallbackV2Adapter;

public class VoiceAssistantServiceConnection implements ServiceConnection {

    private static final String TAG = "VoiceAssistantServiceConnection";

    public static final String VOICE_ASSISTANT_PACKAGE_NAME = "com.smartisanos.voice";
    public static final String VOICE_ASSISTANT_CLASS_NAME = "com.smartisanos.voice.service.VoiceAssistantService";
    public static final String VOICE_ASSISTANT_EXTRA = "voice_extra";

    private static final int TYPE_ALL = 7;

    public IVoiceAssistantService mVoiceRecognizeService;
    private boolean mIsBinded;
    private int mRepeatConnNum = 0;
    private long mLastConnTime = 0;

    private Context mContext;
    private HashMap<IVoiceAssistantCallback, String> mCallbacks;
    private HashMap<VoiceAssistantCallbackV2Adapter, String> mCallbackAdapters;
    private VoiceServiceListener mListener;

    public VoiceAssistantServiceConnection(Context context) {
        mContext = context.getApplicationContext();
        mCallbacks = new HashMap<IVoiceAssistantCallback, String>();
        mCallbackAdapters = new HashMap<VoiceAssistantCallbackV2Adapter, String>();
    }

    @Override
    public void onServiceConnected(ComponentName name,
                                   android.os.IBinder service) {
        Log.i(TAG, "onServiceConnected");
        mVoiceRecognizeService = IVoiceAssistantService.Stub.asInterface(service);
        registerCallback();
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {

        long repeatInterval = SystemClock.elapsedRealtime() - mLastConnTime;
        Log.i(TAG, "onServiceDisconnected ComponentName=" + name
                + ", repeat connect num=" + mRepeatConnNum + ", repeatInterval= " + repeatInterval);
        long connectWaitTime = CommonConstant.REPEAT_CONNECT_SERVER_MAX_TIME - repeatInterval;
        if (connectWaitTime <= 0) {
            mRepeatConnNum = 0;
        }

        if (mRepeatConnNum < CommonConstant.REPEAT_CONNECT_MAX_NUMS) {
            bindVoiceService();
            mRepeatConnNum++;
            mLastConnTime = SystemClock.elapsedRealtime();
        } else {
            Log.d(TAG, "repeat connect music service need waiting: " + connectWaitTime);
        }
    }

    public void bindVoiceService() {
        Log.d(TAG, "bindVoiceService, mIsBinded=" + mIsBinded);
        if (!mIsBinded) {
            try {
                Intent intent = new Intent();
                intent.setComponent(new ComponentName(VOICE_ASSISTANT_PACKAGE_NAME, VOICE_ASSISTANT_CLASS_NAME));
                intent.putExtra(VOICE_ASSISTANT_EXTRA, true);
                mContext.bindService(intent, this, Context.BIND_AUTO_CREATE);
                mIsBinded = true;
            } catch (Exception e) {
            }
        }
    }

    public void unBindVoiceService() {
        Log.d(TAG, "unBindVoiceService, mIsBinded=" + mIsBinded);
        if (mIsBinded) {
            try {
                mContext.unbindService(this);
            } catch (Exception e) {
            }
        }
        mIsBinded = false;
        mVoiceRecognizeService = null;
    }

    public void startRecongnize(String packageName, String pcmPath) {
        Log.d(TAG, "startRecongnize, packageName=" + packageName);
        if (mVoiceRecognizeService == null) {
            return;
        }
        try {
            mVoiceRecognizeService.startRecongnize(packageName, TYPE_ALL, pcmPath);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public void startRecognizeV2(String packageName, Bundle args) {
        Log.d(TAG, "startRecognizeV2, packageName=" + packageName + ", args=" + args);
        if (mVoiceRecognizeService == null) {
            return;
        }
        try {
            mVoiceRecognizeService.startRecognizeV2(packageName, args);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public void stopRecongnize(boolean immediate) {
        Log.d(TAG, "stopRecongnize, immediate=" + immediate);
        if (mVoiceRecognizeService == null) {
            return;
        }
        try {
            mVoiceRecognizeService.stopRecongnize(immediate);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public void stopRecognizeV2(String packageName, Bundle args) {
        Log.d(TAG, "stopRecognizeV2, packageName=" + packageName + ", args=" + args);
        if (mVoiceRecognizeService == null) {
            return;
        }
        try {
            mVoiceRecognizeService.stopRecognizeV2(packageName, args);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    private void registerCallback() {
        if (mVoiceRecognizeService == null
                || ((mCallbacks == null || mCallbacks.size() <= 0)
                    && (mCallbackAdapters == null || mCallbackAdapters.size() <= 0))) {
            Log.w(TAG, "fail to registerCallback due to mVoiceRecognizeService =" + mVoiceRecognizeService + "  mCallbacks = " + mCallbacks);
            return;
        }
        try {
            Iterator<IVoiceAssistantCallback> iterator = mCallbacks.keySet().iterator();
            while (iterator.hasNext()) {
                IVoiceAssistantCallback callback = iterator.next();
                String packageName = mCallbacks.get(callback);
                if (mVoiceRecognizeService != null) {
                    mVoiceRecognizeService.registerCallback(callback, packageName);
                }
            }

            Iterator<VoiceAssistantCallbackV2Adapter> it = mCallbackAdapters.keySet().iterator();
            while (it.hasNext()) {
                VoiceAssistantCallbackV2Adapter callback = it.next();
                String packageName = mCallbackAdapters.get(callback);
                if (mVoiceRecognizeService != null) {
                    mVoiceRecognizeService.registerCallbackV2(callback, packageName);
                }
            }
            if (mListener != null) {
                mListener.onServiceConnected();
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public void registerCallback(IVoiceAssistantCallback cb, String packageName) {
        if (mCallbacks.containsKey(cb))
            return;
        try {
            Log.d(TAG, "registerCallback " + cb);
            mCallbacks.put(cb, packageName);
            if (mVoiceRecognizeService != null) {
                mVoiceRecognizeService.registerCallback(cb, packageName);
            }
        } catch (RemoteException e) {
            e.printStackTrace();
            mCallbacks.remove(cb);
        }
    }

    public void registerCallbackV2(VoiceAssistantCallbackV2Adapter cb, String packageName) {
        if (mCallbackAdapters.containsKey(cb))
            return;
        try {
            Log.d(TAG, "registerCallbackV2 " + cb);
            mCallbackAdapters.put(cb, packageName);
            if (mVoiceRecognizeService != null) {
                mVoiceRecognizeService.registerCallbackV2(cb, packageName);
            }
        } catch (RemoteException e) {
            e.printStackTrace();
            mCallbackAdapters.remove(cb);
        }
    }

    public void unregisterCallback(IVoiceAssistantCallback cb, String packageName) {
        try {
            if (mVoiceRecognizeService != null && mCallbacks.remove(cb) != null) {
                Log.d(TAG, "unregisterCallback " + cb);
                mVoiceRecognizeService.unregisterCallback(cb);
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public void unregisterCallbackV2(VoiceAssistantCallbackV2Adapter cb, String packageName) {
        try {
            if (mVoiceRecognizeService != null && mCallbackAdapters.remove(cb) != null) {
                Log.d(TAG, "unregisterCallbackV2 " + cb);
                mVoiceRecognizeService.unregisterCallbackV2(cb);
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public boolean isRecognizing() {
        try {
            if (mVoiceRecognizeService != null) {
                return mVoiceRecognizeService.isRecognizing();
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean isServiceConnected() {
        return mVoiceRecognizeService != null;
    }

    public boolean reRecognize() {
        try {
            if (mVoiceRecognizeService != null) {
                return mVoiceRecognizeService.reRecongnize();
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return false;
    }

    public void loadLocalData(String keywords, String packageName) {
        try {
            if (mVoiceRecognizeService != null) {
                mVoiceRecognizeService.loadLocalData(keywords, packageName);
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public void setVoiceListener(VoiceServiceListener listener) {
        mListener = listener;
    }

    public interface VoiceServiceListener {
        void onServiceConnected();
    }
}
