
package com.smartisanos.sara.bubble;

import java.io.File;
import java.util.ArrayList;

import android.app.Service;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.os.Parcelable;
import android.text.TextUtils;

import com.smartisanos.ideapills.common.util.VoiceAssistantServiceConnection;
import com.smartisanos.ideapills.common.util.TaskHandler;

import com.smartisanos.sara.storage.BubbleDataRepository;
import com.smartisanos.sara.util.LogUtils;
import com.smartisanos.sara.util.SaraConstant;
import com.smartisanos.sara.util.SaraUtils;
import com.smartisanos.sara.util.SharePrefUtil;
import com.smartisanos.sara.util.StringUtils;

import smartisanos.app.voiceassistant.IVoiceAssistantCallback;

import smartisanos.app.voiceassistant.ParcelableObject;
import android.service.onestep.GlobalBubble;

public class OffLineRecognizeService extends Service {
    private static final String TAG = OffLineRecognizeService.class.getSimpleName();

    private static final String PACKAGE_NAME = "com.smartisanos.sara.bubble.OffLineRecognizeService";
    private String mOffLinePath;
    private GlobalBubble mCurrentBubble;
    private VoiceAssistantServiceConnection mServiceConnection;
    private IVoiceAssistantCallbackImpl mCallback;

    private boolean mReRecognize;
    private Runnable mCheckOffLineTask = new Runnable() {
        @Override
        public void run() {
            checkOffLineRecognize();
        }
    };
    private VoiceAssistantServiceConnection.VoiceServiceListener mVoiceServiceListener =
            new VoiceAssistantServiceConnection.VoiceServiceListener() {
                @Override
                public void onServiceConnected() {
                    if (mReRecognize) {
                        TaskHandler.removeCallbacks(mCheckOffLineTask);
                        TaskHandler.post(mCheckOffLineTask);
                    }
                }
            };

    @Override
    public void onCreate() {
        super.onCreate();
        LogUtils.d(TAG, "onCreate()...");
        mCallback = new IVoiceAssistantCallbackImpl();
        mServiceConnection = new VoiceAssistantServiceConnection(this);
        mServiceConnection.setVoiceListener(mVoiceServiceListener);
        mServiceConnection.bindVoiceService();
        mServiceConnection.registerCallback(mCallback, PACKAGE_NAME);
    }

    @Override
    public IBinder onBind(Intent arg0) {
        LogUtils.d(BubbleActivity.TAG, "OffLineRecognizeService onBind "+arg0);
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && SaraConstant.ACTION_PCM_RERECOGNIZE_ONLINE.equals(intent.getAction())){
            TaskHandler.removeCallbacks(mCheckOffLineTask);
            if (!mServiceConnection.isServiceConnected()) {
                mReRecognize = true;
            } else {
                mReRecognize = false;
                TaskHandler.post(mCheckOffLineTask);
            }
        }
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        LogUtils.d(TAG, "onDestroy()...");
        mReRecognize = false;
        mServiceConnection.setVoiceListener(null);
        mServiceConnection.unregisterCallback(mCallback, PACKAGE_NAME);
        mServiceConnection.unBindVoiceService();
        mCallback = null;
    }

    private void onRecognizeOver(String result) {
        if (mCurrentBubble == null) {
            return;
        }
        LogUtils.d(TAG, "onRecognizeOver, reulst -> " + result + ", bubbleId -> " + mCurrentBubble.getId());
        if (!TextUtils.isEmpty(mOffLinePath) && !TextUtils.isEmpty(result)) {
            if (!result.equals(mCurrentBubble.getText())){
                String context = StringUtils.trimPunctuation(result.toString());

                int length = context.length() > 8 ? 8 : context.length();
                String prefix  = context.subSequence(0, length).toString();
                String path = mOffLinePath.replace(mOffLinePath.substring(mOffLinePath.lastIndexOf("_")+1,mOffLinePath.lastIndexOf(".")), prefix);

                SaraUtils.copyFileFrom2Data(path, Environment.getExternalStorageDirectory() + "/sara/record.pcm");
                SaraUtils.pcmOrWav2Mp3(path);

                String mp3Path = path.replace(SaraConstant.PCM_FILE_SUFFIX, SaraConstant.MP3_FILE_SUFFIX);
                SaraUtils.renameFile(this, path, mp3Path);
                String fileName = path.substring(path.lastIndexOf("/")+1, path.lastIndexOf("_"));
                if (SharePrefUtil.getVoiceNameValue(this).contains(fileName)){
                    SharePrefUtil.setVoiceNameValue(this, mp3Path.substring(path.lastIndexOf("/")+1));
                }
                BubbleDataRepository.updateGlobleBubble(this, mCurrentBubble.getId(), result, mp3Path);
                if (!path.equals(mOffLinePath)) {
                    new File(mOffLinePath.replace(SaraConstant.PCM_FILE_SUFFIX, SaraConstant.MP3_FILE_SUFFIX)).delete();
                    SaraUtils.renameFile(this, mOffLinePath.replace(SaraConstant.PCM_FILE_SUFFIX, SaraConstant.MP3_FILE_SUFFIX +SaraConstant.WAVE_FILE_SUFFIX), mp3Path+SaraConstant.WAVE_FILE_SUFFIX);
                }
            } else {
                BubbleDataRepository.updateGlobleBubble(this, mCurrentBubble.getId(), result, mOffLinePath.replace(SaraConstant.PCM_FILE_SUFFIX, SaraConstant.MP3_FILE_SUFFIX));
            }
            new File(mOffLinePath).delete();
        } else {
            BubbleDataRepository.updateGlobleBubble(this, mCurrentBubble.getId(), null, null);
        }
    }

    private void checkOffLineRecognize() {
        LogUtils.d(TAG, "checkOffLineRecognize()...");
        if (mServiceConnection.isRecognizing() ||  !mServiceConnection.isServiceConnected() || !SaraUtils.isNetworkConnected()){
            LogUtils.w(TAG, "just return due to some reason...");
            return;
        }
        mCurrentBubble = null;
        Bundle tmp = new Bundle();
        tmp.putString("type", SaraConstant.LIST_TYPE_OFFLINE);
        ArrayList<Parcelable> bubbleList = BubbleDataRepository.getGlobleBubbleList(tmp);
        if (bubbleList != null && bubbleList.size() > 0) {
            for (Parcelable parcelable : bubbleList) {
                if (checkOffLineRecognizeOfBubble((GlobalBubble) parcelable)) {
                    break;
                }
            }
        }
    }

    private boolean checkOffLineRecognizeOfBubble(GlobalBubble bubble) {
        LogUtils.d(TAG, "checkOffLineRecognize()...");
        if (mServiceConnection.isRecognizing() ||  !mServiceConnection.isServiceConnected() || !SaraUtils.isNetworkConnected()){
            LogUtils.w(TAG, "just return due to some reason...");
            return true;
        }
        mCurrentBubble = bubble;
        if (mCurrentBubble != null) {
            LogUtils.d(TAG, "offline bubble to recognize -> " + mCurrentBubble);
            Uri uri = mCurrentBubble.getUri();
            String path = SaraUtils.formatContent2FilePath(this, uri);
            if (!TextUtils.isEmpty(path)){
                mOffLinePath = path.substring(0,  path.lastIndexOf(".")) + SaraConstant.PCM_FILE_SUFFIX;
                File offLinePcmFile = new File(mOffLinePath);
                File offLineWavFile = new File(path);
                if (!TextUtils.isEmpty(mOffLinePath)  && offLinePcmFile.exists() && offLineWavFile.exists()) {
                    String temp = Environment.getExternalStorageDirectory() + "/sara" + mOffLinePath.substring(mOffLinePath.lastIndexOf("/"));
                    SaraUtils.copyFileFrom2Data(temp, mOffLinePath);
                    mServiceConnection.startRecongnize(PACKAGE_NAME, temp);
                    return true;
                } else {
                    Bundle destroyBunble = new Bundle();
                    destroyBunble.putIntArray(SaraConstant.KEY_BUBBLE_IDS, new int[] {mCurrentBubble.getId()});
                    BubbleDataRepository.destroyGlobleBubble(this, destroyBunble);
                    return false;
                }
            } else {
                Bundle destroyBunble = new Bundle();
                destroyBunble.putIntArray(SaraConstant.KEY_BUBBLE_IDS, new int[] {mCurrentBubble.getId()});
                BubbleDataRepository.destroyGlobleBubble(this, destroyBunble);
                return false;
            }
        } else {
            return false;
        }
    }

    private class IVoiceAssistantCallbackImpl extends IVoiceAssistantCallback.Stub {
        private StringBuilder mSb;

        @Override
        public void onVolumeUpdate(int volum) {
        }

        @Override
        public void onLocalResult(ParcelableObject result, boolean isRefresh) {

        }

        @Override
        public void onError(int errorCode, int type, boolean isToast) {
            LogUtils.e(TAG, "offline recognize error, errorCode: " + errorCode + ", type: " + type);
        }

        @Override
        public void onResultRecived(String resultStr, int type, boolean offline) {
            if (TextUtils.isEmpty(resultStr)) {
                resultStr = "";
            }
            mSb.append(resultStr);
            onRecognizeOver(mSb.toString());
            //check next one
            TaskHandler.removeCallbacks(mCheckOffLineTask);
            TaskHandler.post(mCheckOffLineTask);
        }

        @Override
        public void onBuffer(byte[] buffer) {

        }

        @Override
        public void onPartialResult(String partial) {
            mSb.append(partial);
        }

        @Override
        public void onRecordStart() {
            LogUtils.d(TAG, "onRecordStart()...");
            mSb = new StringBuilder();
        }

        @Override
        public void onRecordEnd() {
        }
    }
}
