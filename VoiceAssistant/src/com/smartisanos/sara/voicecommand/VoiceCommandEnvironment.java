package com.smartisanos.sara.voicecommand;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.Rect;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

import com.smartisanos.ideapills.common.util.MutiTaskHandler;
import com.smartisanos.ideapills.common.util.TaskHandler;
import com.smartisanos.sara.util.SaraConstant;

import java.util.List;
import java.util.Vector;

public class VoiceCommandEnvironment {

    public static final String TAG = "VoiceCommandEnvironment";

    private static VoiceCommandEnvironment sInstance;

    private IVoiceCommandEnvironment mEnvService;

    private List<EnvironmentCallback> mCallbacks = new Vector<EnvironmentCallback>();

    private VoiceCommandEnvironment(Context context, IVoiceCommandEnvironment environment) {
        registerReceiver(context);
        mEnvService = environment;
    }

    public static VoiceCommandEnvironment getInstance() {
        return sInstance;
    }

    public static void init(final Context context) {
        context.bindService(new Intent(context, VoiceCommandEnvironmentService.class),
                new ServiceConnection() {
                    @Override
                    public void onServiceConnected(ComponentName name, IBinder service) {
                        Log.d(TAG, "onServiceConnected: create VoiceCommandEnvironment instance.");
                        sInstance = new VoiceCommandEnvironment(context,
                                IVoiceCommandEnvironment.Stub.asInterface(service));
                    }

                    @Override
                    public void onServiceDisconnected(ComponentName name) {
                        sInstance = null;
                    }
                }, Context.BIND_AUTO_CREATE);
    }

    public CharSequence getCurrentPackage() {
        CharSequence result = null;
        try {
            result = mEnvService.getCurrentPackage();
        } catch (RemoteException e) {

        }
        return result;
    }

    public CharSequence getCurrentWindowTitle() {
        CharSequence result = null;
        try {
            result = mEnvService.getCurrentWindowTitle();
        } catch (RemoteException e) {

        }
        return result;
    }
    public Rect getCurrentFocusRect() {
        Rect result = null;
        try {
            result = mEnvService.getCurrentFocusRect();
        } catch (RemoteException e) {

        }
        return result;
    }
    public CharSequence getCurrentFocusText() {
        CharSequence result = null;
        try {
            result = mEnvService.getCurrentFocusText();
        } catch (RemoteException e) {

        }
        return result;
    }
    public void clickVoiceButton(String text, EnvironmentCallback callback) {
        try {
            mEnvService.clickVoiceButton(text);
            addCallback(callback);
        } catch (RemoteException e) {

        }
    }

    private void registerReceiver(Context context) {
        IntentFilter filter = new IntentFilter(SaraConstant.ACTION_VOICE_BUTTON_RESULT);
        Log.d(TAG, "register voice button result receiver.");
        context.registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (SaraConstant.ACTION_VOICE_BUTTON_RESULT.equals(intent.getAction())) {
                    Log.d(TAG, "voice button result received.");
                    boolean success = intent.getBooleanExtra(SaraConstant.EXTRA_VOICE_BUTTON_RESULT, false);

                    EnvironmentCallback callback = null;
                    try {
                        callback = mCallbacks.remove(0);
                    } catch (ArrayIndexOutOfBoundsException e) {
                        // do nothing
                    }
                    if (callback != null) {
                        callback.onVoiceButtonResult(success);
                    }
                }
            }
        }, filter);
    }

    private void addCallback(final EnvironmentCallback callback) {
        if (callback != null && !mCallbacks.contains(callback)) {
            mCallbacks.add(callback);
            MutiTaskHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (mCallbacks.remove(callback)) {
                        Log.d(TAG, "remove timeout callback.");
                    }
                }
            }, 3000);
        }
    }

    public interface EnvironmentCallback {
        void onVoiceButtonResult(boolean success);
    }
}
