package com.smartisanos.sara.bubble.revone.utils;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.RemoteException;

import com.smartisan.flashim.FlashImSendMessageInterface;

import java.util.List;

public class FlashImConnection implements ServiceConnection {

    private static final String TAG = "FlashImConnection";

    public static final String VOICE_ASSISTANT_PACKAGE_NAME = "com.smartisanos.voice";
    public static final String VOICE_ASSISTANT_CLASS_NAME = "com.smartisanos.voice.service.VoiceAssistantService";
    public static final String VOICE_ASSISTANT_EXTRA = "voice_extra";

    public FlashImSendMessageInterface mFlashImService;
    private Context mContext;
    private FlashImConnectListener mListener;

    public FlashImConnection(Context context) {
        mContext = context.getApplicationContext();
    }

    @Override
    public void onServiceConnected(ComponentName name,
                                   android.os.IBinder service) {
        mFlashImService = FlashImSendMessageInterface.Stub.asInterface(service);
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        mFlashImService = null;
    }

    public void bindService() {
        if (mFlashImService == null) {
            try {
                Intent intent = new Intent();
                intent.setAction("com.bullet.messenger.FlashImSendMessage");
                mContext.bindService(getFlashImServiceIntent(mContext, intent), this, Context.BIND_AUTO_CREATE);
            } catch (Exception e) {
            }
        }
    }

    public void unBindService() {
        if (mFlashImService != null) {
            try {
                mContext.unbindService(this);
            } catch (Exception e) {
            }
        }
    }

    public int sendVoiceMessage(int messageType, String contactId, String voiceUri, String voiceText) {
        try {
            if (mFlashImService != null) {
                return mFlashImService.sendVoiceMessage(messageType, contactId, voiceUri, voiceText);
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public int sendImageMessage(int messageType, String contactId, String voiceUri) {
        try {
            if (mFlashImService != null) {
                return mFlashImService.sendImageMessage(messageType, contactId, voiceUri);
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public int sendVideoMessage(int messageType, String contactId, String voiceUri) {
        try {
            if (mFlashImService != null) {
                return mFlashImService.sendVideoMessage(messageType, contactId, voiceUri);
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public int sendFileMessage(int messageType, String contactId, String voiceUri) {
        try {
            if (mFlashImService != null) {
                return mFlashImService.sendFileMessage(messageType, contactId, voiceUri);
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public boolean isServiceConnected() {
        return mFlashImService != null;
    }

    public Intent getFlashImServiceIntent(Context context, Intent implicitIntent) {
        PackageManager pm = context.getPackageManager();
        List<ResolveInfo> resolveInfo = pm.queryIntentServices(implicitIntent, 0);
        if (resolveInfo == null || resolveInfo.size() != 1) {
            return null;
        }
        ResolveInfo serviceInfo = resolveInfo.get(0);
        String packageName = serviceInfo.serviceInfo.packageName;
        String className = serviceInfo.serviceInfo.name;
        ComponentName component = new ComponentName(packageName, className);
        Intent explicitIntent = new Intent(implicitIntent);
        explicitIntent.setComponent(component);
        return explicitIntent;
    }

    public void setFlashImConnectListener(FlashImConnectListener listener) {
        mListener = listener;
    }

    public interface FlashImConnectListener {
        void onServiceConnected();
    }
}
