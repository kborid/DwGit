package com.smartisanos.voice.service;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import com.iflytek.business.speech.SpeechError;
import com.smartisanos.voice.engine.GrammarManager;
import com.smartisanos.voice.engine.RecognizeParams;
import com.smartisanos.voice.engine.VoiceRecognitionEngineBase;
import com.smartisanos.voice.engine.VoiceRecognitionEngineBase.EntranceType;
import com.smartisanos.voice.engine.VoiceRecognitionEngineBase.RecognizeListener;
import com.smartisanos.voice.engine.XunfeiRecognizerEngine;
import com.smartisanos.voice.util.CootekManager;
import com.smartisanos.voice.util.DataLoadUtil;
import com.smartisanos.voice.util.LogUtils;
import com.smartisanos.voice.util.StringUtils;
import com.smartisanos.voice.util.VoiceConstant;
import com.smartisanos.voice.util.VoiceUtils;
import com.smartisanos.voice.R;
import smartisanos.app.voiceassistant.IVoiceAssistantService;
import smartisanos.app.voiceassistant.ApplicationStruct;
import smartisanos.app.voiceassistant.ContactStruct;
import smartisanos.app.voiceassistant.IVoiceAssistantCallback;
import smartisanos.app.voiceassistant.IVoiceAssistantCallbackV2;
import smartisanos.app.voiceassistant.MediaStruct;
import smartisanos.app.voiceassistant.ParcelableObject;
import smartisanos.app.numberassistant.YellowPageResult;
import smartisanos.api.PackageManagerSmt;

import android.os.IInterface;
import android.os.SystemClock;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.media.AudioManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Parcel;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.text.TextUtils;
import android.os.Handler;
import android.os.SystemProperties;
import android.provider.MediaStore;
import android.telephony.TelephonyManager;

import smartisanos.app.voiceassistant.RecognizeArgsHelper;
import smartisanos.app.voiceassistant.RecognizeResult;
import smartisanos.app.voiceassistant.VoiceAssistantCallbackV2Adapter;
import smartisanos.util.DeviceType;

public class VoiceAssistantService extends Service {
    private static final String TAG ="VoiceRecongnizeService";
    static final LogUtils log = LogUtils.getInstance(VoiceAssistantService.class);
    private static final boolean VOICE_ENABLE = SystemProperties.getInt("ro.voice_assistant_enable", 0) == 1;
    private static final boolean DEVICES_AFTER_ODIN = !DeviceType.isOneOf(DeviceType.T1, DeviceType.T2, DeviceType.U1,
            DeviceType.M1, DeviceType.M1L, DeviceType.A1, DeviceType.BONO);
    private final IBinder mBinder = new ServiceStub(this);
    private ArrayList<MediaStruct> mMusics = new ArrayList<MediaStruct>();
    private ArrayList<ContactStruct> mContactlist = new ArrayList<ContactStruct>();
    private ArrayList<ApplicationStruct> mApps = new ArrayList<ApplicationStruct>();
    private List<YellowPageResult> mYellowPageList = new ArrayList<YellowPageResult>();
    private VoiceRecognitionEngineBase mEngine = null;
    private boolean isStopEngine;
    private int mBaseType;
    private static int mLocalSearchType;
    private boolean mImmediate;
    private Handler mHandler = new Handler();
    private AudioManager mAudioManager;
    private  String[] resultList;
    private static final int TYPE_CONTACT = 1;
    private static final int TYPE_MUSIC = 1 << 1;
    private static final int TYPE_APP = 1 << 2;
    private static final int TYPE_CANDIDATE = 1 << 3;
    private static final int TYPE_VOICE_COMMAND = 1 << 4;
    private static final int TYPE_ALL = TYPE_CONTACT | TYPE_MUSIC| TYPE_APP;
    private static final int TYPE_BULLET = 1 << 5;
    private ConcurrentHashMap<String, String[]> mClienResult ;
    private ConcurrentHashMap<IBinder,String> mCallBackList ;
    private ConcurrentHashMap<IBinder, Long> mCallBacksRegistTime;
    private volatile boolean mIsRecognizing = false;
    private static String mCurrentPackageName;
    private VoiceAssistantCallbackV2Adapter mPartialResultHelper = new VoiceAssistantCallbackV2Adapter() {
        @Override
        public void onFixedResult(RecognizeResult result, String fixedString, boolean isLast) {
            mCachedPartialString = fixedString;
        }
    };
    private String mCachedPartialString;
    private List<RecognizeResult> mPartialCaches = new ArrayList<RecognizeResult>();
    private byte[] mBytesCached;

    interface RemoteCallbackRegisterHandler<T extends IInterface> {
        void onRegister(String packageName, T cbk);
    }

    private RemoteCallbackList<IVoiceAssistantCallback> mRemoteCallBackList = new RemoteCallbackList<IVoiceAssistantCallback>();
    private RemoteCallbackRegisterHandler<IVoiceAssistantCallback> mCallbackRegisterHandler = new RemoteCallbackRegisterHandler<IVoiceAssistantCallback>() {

        @Override
        public void onRegister(String packageName, IVoiceAssistantCallback cbk) {
                if (mIsRecognizing && !VoiceConstant.PACKAGE_NAME_CONTACT.equals(packageName)
                        && !VoiceConstant.PACKAGE_NAME_MUSIC.equals(packageName)
                        && !VoiceConstant.PACKAGE_NAME_SEARCH.equals(packageName)) {
                    try {
                        cbk.onRecordStart();
                        if (mBytesCached != null && mBytesCached.length > 0) {
                            log.i("onRegister: " + mBytesCached.length);
                            cbk.onBuffer(mBytesCached);
                            mBytesCached = null;
                        }
                        cbk.onPartialResult(mCachedPartialString);
                        mPartialCaches.clear();
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                }
            }

        };

    private RemoteCallbackList<IVoiceAssistantCallbackV2> mCallbackV2List = new RemoteCallbackList<IVoiceAssistantCallbackV2>();
    private RemoteCallbackRegisterHandler<IVoiceAssistantCallbackV2> mCallbackV2RegisterHandler = new RemoteCallbackRegisterHandler<IVoiceAssistantCallbackV2>() {
        @Override
        public void onRegister(String packageName, IVoiceAssistantCallbackV2 cbk) {
            if (mIsRecognizing && !VoiceConstant.PACKAGE_NAME_CONTACT.equals(packageName)
                    && !VoiceConstant.PACKAGE_NAME_MUSIC.equals(packageName)
                    && !VoiceConstant.PACKAGE_NAME_SEARCH.equals(packageName)) {
                try {
                    cbk.onRecordStart();
                    if (mBytesCached != null && mBytesCached.length > 0) {
                        log.i("onRegister: " + mBytesCached.length);
                        cbk.onBuffer(mBytesCached);
                        mBytesCached = null;
                    }

                    for (RecognizeResult partial : mPartialCaches) {
                        cbk.onPartialResult(partial);
                    }
                    mPartialCaches.clear();
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        }
    };


    public enum ChangeType {
        CONTACT, APP, MUSIC, NONE
    }
    private static final String SETTING_APP_ICON_URI = "android.resource://com.android.settings/drawable/ic_launcher_settings";
    private static final String VOICE_READING_URI = "content://applications/applications/com.android.settings/com.android.settings.Settings$AccessibilitySpeechReadingActivity";
    private ContentObserver mMusicChangeObserver;
    private ContentObserver mContactChangeObserver;
    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent == null) {
                return;
            }
            String action = intent.getAction();
            if ((Intent.ACTION_PACKAGE_ADDED).equals(action)
                    ||(Intent.ACTION_PACKAGE_REMOVED).equals(action)
                    ||(Intent.ACTION_PACKAGE_CHANGED).equals(action)
                    ||(PackageManagerSmt.get_ACTION_SM_PACKAGES_LOCKED()).equals(action)
                    ||(PackageManagerSmt.get_ACTION_SM_PACKAGES_UNLOCKED()).equals(action)) {
                   refreshAction(ChangeType.APP);
            } else if (VoiceConstant.ACTION_RECORD_ERROR.equals(action)) {
                error(-1, false, mCurrentPackageName);
            }
        }
    };
    @Override
    public void onCreate() {
        super.onCreate();
        mEngine = XunfeiRecognizerEngine.getInstance();
        registerReceiver(this);
        ContentResolver contentResolver = getContentResolver();
        registerContactObserver(this, contentResolver);
        registerMusicObserver(this, contentResolver);
        mAudioManager = (AudioManager)getSystemService(Context.AUDIO_SERVICE);
        mClienResult = new ConcurrentHashMap<String, String[]>();
        mCallBackList= new ConcurrentHashMap<IBinder, String>();
        mCallBacksRegistTime = new ConcurrentHashMap<IBinder, Long>();
        log.e("onCreate Service");
    }

    @Override
    public void onDestroy() {
        unregisterReceiver(mReceiver);
        ContentResolver contentResolver = getContentResolver();
        contentResolver.unregisterContentObserver(mMusicChangeObserver);
        contentResolver.unregisterContentObserver(mContactChangeObserver);
        mCallBackList.clear();
        mClienResult.clear();
    }

    public void startRecognize(final String packageName, int baseType, RecognizeParams params) {
        if (mIsRecognizing &&
                mEngine != null && mEngine.isUsePcm() && !params.hasSourcePcm()) {
            // last time used by offline, this time higher priority, stop it
            stopRecongize();
        }

        if (mIsRecognizing || mEngine == null) {
            if (TextUtils.equals(mCurrentPackageName, packageName)) {
                log.w("startRecongnize return cause of recognizing");
                return;
            } else {
                log.w("startRecongnize interrupt : " + mCurrentPackageName);
                stopRecongize();
                error(SpeechError.ERROR_INTERRUPT, true, mCurrentPackageName);
            }
        }

        mIsRecognizing = true;
        cleanCached();
        mLocalSearchType = params.getLocalSearchType();
        resultList = null;
        mClienResult.put(packageName, new String[1]);
        mCurrentPackageName = packageName;
        if (phoneIsInUse()) {
            error(SpeechError.ERROR_AUDIO_RECORD, false, packageName);
            log.e("startRecongnize failed, try again in a few minutes");
            return;
        }

        isStopEngine = false;
        mImmediate = false;
        mEngine.setRecognizeListener(new RecognizeListener() {
            @Override
            public void onResultReceived(RecognizeResult result) {
                resultReceived(result, packageName);
            }

            @Override
            public void onRecordEnd() {
                recordEnd(packageName);
            }

            @Override
            public void onRecordStart() {
                recordStart(packageName);
            }
            @Override
            public void onVolumeUpdate(int volume) {
                volumeUpdate(volume, packageName);
            }

            @Override
            public void onPartialResult(RecognizeResult partial) {
                partialResult(partial, packageName);
            }

            @Override
            public void onBuffer(byte[] buffer) {
                buffer(buffer,packageName);
            }
            @Override
            public void onError(int errorCode) {
                error(errorCode,true, packageName);
            }
        });

        EntranceType entranceType = null;
        switch (baseType) {
            case RecognizeArgsHelper.BASE_TYPE_SEARCH:
                entranceType = EntranceType.SEARCH;
                break;
            case RecognizeArgsHelper.BASE_TYPE_VOICE_COMMAND:
                entranceType = EntranceType.VOICE_COMMAND;
                break;
            case RecognizeArgsHelper.BASE_TYPE_BUBBLE:
            default:
                entranceType = EntranceType.BUBBLE;
        }

        if (!mEngine.startRecognize(entranceType, params)){
            error(-1, true, packageName);
        } else {
            mBaseType = baseType;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    public void stopRecongize() {
        mIsRecognizing = false;
        if (mEngine != null) {
            stopEngine();
            isStopEngine = true;
        }
        mHandler.removeCallbacksAndMessages(null);
    }

    public void stopEngine() {
        if (mEngine != null) {
            if (mEngine.isRecognizing()) {
                mEngine.stopRecognize();
            }
            if (mEngine.isTtsPlaying()) {
                mEngine.stopPlayText();
            }
        }

    }

    public void endRecongnize(boolean immediate) {
        mImmediate = immediate;
        if (mEngine != null && mEngine.isRecognizing()) {
            if (!immediate) {
                mHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if (mEngine != null && mEngine.isRecognizing()) {
                            mEngine.endRecognize();
                        }
                        mIsRecognizing = false;
                    }
                }, 600);
            } else {
                mEngine.endRecognize();
                mIsRecognizing = false;
            }
        } else {
            mIsRecognizing = false;
        }
    }

    private <T extends IInterface> void addCallBack(String packageName, RemoteCallbackList<T> callbackList, T cb, RemoteCallbackRegisterHandler<T> handler) {
        mCallBacksRegistTime.put(cb.asBinder(), SystemClock.elapsedRealtime());
        mCallBackList.put(cb.asBinder() , packageName);
        callbackList.register(cb);

        log.i("addCallBack  register : " + cb + " | IBinder : " + cb.asBinder());
        try {
            handler.onRegister(packageName, cb);
        } catch (Exception e) {
            log.e("addCallBack error: " + e.getMessage(), e);
        }
    }
    private <T extends IInterface> void removeCallBack(RemoteCallbackList<T> callbackList, T callback){
        callbackList.unregister(callback);
        mCallBackList.remove(callback.asBinder());
        mCallBacksRegistTime.remove(callback.asBinder());
    }

    private boolean isRecognizing(){
        if (mEngine != null) {
            return mEngine.isRecognizing() && mIsRecognizing;
        }
        return false;
    }

    public String getRecognizingPackage() {
        if (mIsRecognizing && mEngine.isRecognizing()) {
            log.d("getRecognizingPackage: " + mCurrentPackageName);
            return mCurrentPackageName;
        }
        return null;
    }

    static class ServiceStub extends IVoiceAssistantService.Stub {
        WeakReference<VoiceAssistantService> mService;
        boolean reRecognize = false;
        int currentUid = -1;
        ServiceStub(VoiceAssistantService service) {
            mService = new WeakReference<VoiceAssistantService>(service);
        }

        @Override
        public void startRecongnize( String name, int type, String pcmPath) throws RemoteException {
            log.infoRelease("startRecongnize: name=" + name + ", type=" + type + ", pcmPath=" + pcmPath
                    + ", callinguid=" + Binder.getCallingUid() + ", callingpid=" + Binder.getCallingPid());
            final VoiceAssistantService service = mService.get();
            if (service != null) {
                reRecognize = false;
                if (VoiceConstant.PACKAGE_NAME_ANDROID.equals(name)) {
                    // 1.recording  2.play music in none oscar,osborn,trident
                    if ((VoiceUtils.checkRecorder() && !service.isRecognizing())
                            || (service.mAudioManager.isMusicActive() && !DeviceType.isOneOf(DeviceType.OSBORN, DeviceType.OSCAR, DeviceType.TRIDENT))) {
                        reRecognize = true;
                        return;
                    }
                }
                currentUid = Binder.getCallingUid();
                String mapName = name;
                if (VoiceConstant.PACKAGE_NAME_ANDROID.equals(name)){
                    mapName = VoiceConstant.PACKAGE_NAME_SARA;
                }

                RecognizeParams params = null;
                int baseType = 0;
                if (VoiceConstant.PACKAGE_NAME_SARA_BULLET.equals(name) || type == TYPE_BULLET) {
                    type = TYPE_BULLET | TYPE_CONTACT;
                    baseType = RecognizeArgsHelper.BASE_TYPE_SEARCH;
                    params = RecognizeParams.getBaseParams(service, baseType)
                            .setScenes("bullet");
                } else if (!VoiceConstant.PACKAGE_NAME_SEARCH.equals(mapName) && type == TYPE_ALL) {
                    baseType = RecognizeArgsHelper.BASE_TYPE_BUBBLE;
                    params = RecognizeParams.getBaseParams(service, baseType);
                    params.setSourcePcm(pcmPath);
                    if (VoiceConstant.PACKAGE_NAME_SARA.equals(mapName)) {
                        if (DEVICES_AFTER_ODIN && !params.hasSourcePcm()) {
                            params.setRequestThreshold(300);
                        }
                        if (VoiceConstant.PACKAGE_NAME_ANDROID.equals(name)) { // 胶囊键预启表明是手机胶囊，应用动态修正
                            params.setEnableDynamicFix(true);
                        }
                    }
                } else if (type == TYPE_CANDIDATE) {
                    baseType = RecognizeArgsHelper.BASE_TYPE_BUBBLE;
                    params = RecognizeParams.getBaseParams(service, baseType);
                    params.setEnableCandidate(true);
                } else if (type == TYPE_VOICE_COMMAND) {
                    baseType = RecognizeArgsHelper.BASE_TYPE_VOICE_COMMAND;
                    params = RecognizeParams.getBaseParams(service, baseType);
                    params.addScenes(pcmPath);
                } else {
                    baseType = RecognizeArgsHelper.BASE_TYPE_SEARCH;
                    params = RecognizeParams.getBaseParams(service, baseType);
                }

                params.setLocalSearchType(type);
                service.startRecognize(mapName, baseType, params);
            }
        }

        @Override
        public void registerCallback(IVoiceAssistantCallback cb,String packageName) throws RemoteException {
            VoiceAssistantService service = mService.get();
            if (cb != null && service != null) {
                service.addCallBack(packageName, service.mRemoteCallBackList, cb, service.mCallbackRegisterHandler);
            }
        }

        @Override
        public void unregisterCallback(IVoiceAssistantCallback cb) throws RemoteException {
            VoiceAssistantService service = mService.get();
               if (service != null) {
                service.removeCallBack(service.mRemoteCallBackList, cb);
            }
        }

        @Override
        public void stopRecongnize(final boolean immediate) throws RemoteException {
            if (Binder.getCallingUid() != currentUid) {
                return;
            }
            log.infoRelease("stopRecongnize: immediate=" + immediate + ", callinguid=" + Binder.getCallingUid() + ", callingpid=" + Binder.getCallingPid());
            final VoiceAssistantService service = mService.get();
            if (service != null) {
                service.endRecongnize(immediate);
            }
        }

        @Override
        public boolean reRecongnize() throws RemoteException {
            return reRecognize;
        }
        @Override
        public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
            try {
                return super.onTransact(code, data, reply, flags);
            } catch (RuntimeException e) {
                log.w("Unexpected remote exception" + e);
                throw e;
            }
        }

        @Override
        public boolean isRecognizing() throws RemoteException {
            VoiceAssistantService service = mService.get();
            if (service != null) {
                return service.isRecognizing();
            }
            return false;
        }

        public String getRecognizingPackage() throws RemoteException {
            VoiceAssistantService service = mService.get();
            if (service != null) {
                return service.getRecognizingPackage();
            }
            return null;
        }

        public void loadLocalData(String keywords, String packageName) throws RemoteException {
            VoiceAssistantService service = mService.get();
            if (service != null) {
                service.loadLocalData(keywords, packageName);
            }
        }

        @Override
        public void startRecognizeV2(String packageName, Bundle args) throws RemoteException {
            log.infoRelease("startRecognize: packageName=" + packageName + ", args=" + args);
            VoiceAssistantService service = mService.get();
            if (service != null) {
                service.startRecognize(packageName,
                        new RecognizeArgsHelper(args).getBaseType(),
                        RecognizeParams.fromBundle(service, args));
            }
        }

        @Override
        public void stopRecognizeV2(String packageName, Bundle args) throws RemoteException {
            if (!TextUtils.equals(mCurrentPackageName, packageName)) {
                return;
            }
            log.infoRelease("stopRecognize: packageName=" + packageName + ", args=" + args);
            final VoiceAssistantService service = mService.get();
            if (service != null) {
                service.endRecongnize(true);
            }
        }

        @Override
        public void registerCallbackV2(IVoiceAssistantCallbackV2 cb, String packageName) throws RemoteException {
            VoiceAssistantService service = mService.get();
            if (cb != null && service != null) {
                service.addCallBack(packageName, service.mCallbackV2List, cb, service.mCallbackV2RegisterHandler);
            }
        }

        @Override
        public void unregisterCallbackV2(IVoiceAssistantCallbackV2 cb) throws RemoteException {
            VoiceAssistantService service = mService.get();
            if (cb != null && service != null) {
                service.removeCallBack(service.mCallbackV2List, cb);
            }
        }
    }

    void showResultCallback(boolean empty, boolean refresh, String packageName, String resultStr) {
        try {
            if (empty) {
                localResult(new ParcelableObject(),refresh,packageName);
            } else {
              ParcelableObject result = new ParcelableObject();
              result.setContacts(mContactlist);
              result.setMusics(mMusics);
              result.setResultStr(resultStr);
              if (mApps == null) {
                  mApps = new ArrayList<ApplicationStruct>();
              }

              //add for blind mode
              if (resultStr.equals(getResources().getString(R.string.blind_mode_voice_string))) {
                  ApplicationStruct app = new ApplicationStruct();
                  app.setAppIndex(mApps.size() - 1);
                  app.setAppName(getResources().getString(R.string.blind_mode_voice_string));
                  app.setIconUri(Uri.parse(SETTING_APP_ICON_URI));
                  app.setStartUri(Uri.parse(VOICE_READING_URI));
                  app.setInstalledState(1);
                  mApps.add(app);
              }
              result.setApps(mApps);
              result.setYellowPages(mYellowPageList);
              localResult(result,refresh,packageName);
            }
        } catch (Exception e) {
            log.e("exception when showing result , "+ e.getMessage());
        }
    }

    public void localResult(final ParcelableObject result, final boolean refresh, String packageName){
        broadcastCallback(packageName, mRemoteCallBackList, new RemoteCallbackBroadcastHandler<IVoiceAssistantCallback>() {
            @Override
            public void onBroadcast(String packageName, IVoiceAssistantCallback cbk) throws Exception {
                cbk.onLocalResult(result, refresh);
            }
        });
        broadcastCallback(packageName, mCallbackV2List, new RemoteCallbackBroadcastHandler<IVoiceAssistantCallbackV2>() {
            @Override
            public void onBroadcast(String packageName, IVoiceAssistantCallbackV2 cbk) throws Exception {
                RecognizeResult local = new RecognizeResult();
                local.setLocalResult(result);
                cbk.onLocalResult(local);
            }
        });
    }

    public void loadLocalData(String keywords, final String packageName) {
        final String[] names = new String[]{keywords};
        loadDataInBackground(names, ChangeType.NONE, false, packageName);
    }

    public void loadDataInBackground(final String[] names,final ChangeType changeType, final boolean isRefresh, final String packageName) {
        log.i("loadDataInBackground: " + Arrays.toString(names));
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                mContactlist.clear();
                mMusics.clear();
                if (mApps != null) {
                    mApps.clear();
                }
                mYellowPageList.clear();

                if ((mLocalSearchType & TYPE_BULLET) == TYPE_BULLET) {
                    mContactlist.addAll(DataLoadUtil.loadContacts(VoiceAssistantService.this, names, true));
                }

                if (VoiceConstant.PACKAGE_NAME_SEARCH.equals(packageName) || VoiceConstant.PACKAGE_NAME_CONTACT.equals(packageName)) {
                    String[] searchKey = new String[] {names[0].contains("。") ? names[0].substring(0,names[0].indexOf("。")) : names[0]};
                    if ((mLocalSearchType & TYPE_CONTACT) == TYPE_CONTACT || changeType == ChangeType.CONTACT) {
                        mContactlist.addAll(DataLoadUtil.loadContacts(VoiceAssistantService.this, searchKey, false));
                        CootekManager cootekManager = CootekManager.getInstance(VoiceAssistantService.this);
                        List<YellowPageResult> yellowPage = DataLoadUtil.loadYellowPages(VoiceAssistantService.this, searchKey[0]);
                        if (yellowPage != null) {
                            mYellowPageList = yellowPage;
                            for (YellowPageResult yellowPageResult:mYellowPageList){
                                yellowPageResult.matchName = DataLoadUtil.getMatchString(yellowPageResult.name,searchKey[0]);
                            }
                        }
                    }
                    if ((mLocalSearchType & TYPE_MUSIC) == TYPE_MUSIC || changeType == ChangeType.MUSIC) {
                        mMusics = DataLoadUtil.loadMusics(VoiceAssistantService.this, searchKey);
                        for (MediaStruct mediaStruct:mMusics){
                            mediaStruct.mMatchName = DataLoadUtil.getMatchString(mediaStruct.mArtist,searchKey[0]);
                        }
                    }
                    if ((mLocalSearchType & TYPE_APP) == TYPE_APP || changeType == ChangeType.APP) {
                        mApps = DataLoadUtil.loadApps(VoiceAssistantService.this, searchKey[0], packageName);
                    }
                    showResultCallback(false, isRefresh, packageName, searchKey[0].toString());
                } else {
                    if ((mLocalSearchType & TYPE_CONTACT) == TYPE_CONTACT || changeType == ChangeType.CONTACT) {
                        mContactlist.addAll(DataLoadUtil.loadContacts(VoiceAssistantService.this, names, false));
                        CootekManager cootekManager = CootekManager.getInstance(VoiceAssistantService.this);
                        String searchText = names[0].split(",")[0];
                        List<YellowPageResult> yellowPage = DataLoadUtil.loadYellowPages(VoiceAssistantService.this, searchText);
                        if (yellowPage != null) {
                            mYellowPageList = yellowPage;
                        }
                    }
                    if ((mLocalSearchType & TYPE_MUSIC) == TYPE_MUSIC || changeType == ChangeType.MUSIC) {
                        mMusics = DataLoadUtil.loadMusics(VoiceAssistantService.this, names);
                    }
                    if ((mLocalSearchType & TYPE_APP) == TYPE_APP || changeType == ChangeType.APP) {
                        mApps = DataLoadUtil.loadApps(VoiceAssistantService.this, names[0].split(",")[0], packageName);
                    }
                    StringBuffer resultStr = new StringBuffer();
                    for (String name : names) {
                        resultStr.append(name);
                    }
                    showResultCallback(false, isRefresh, packageName, resultStr.toString());
                }
            }
        });

    }
    private void error(final int errorCode, final boolean isToast, String packageName) {
        mIsRecognizing = false;
        broadcastCallback(packageName, mRemoteCallBackList, new RemoteCallbackBroadcastHandler<IVoiceAssistantCallback>() {
            @Override
            public void onBroadcast(String packageName, IVoiceAssistantCallback cbk) throws Exception {
                cbk.onError(errorCode, mLocalSearchType, isToast);
            }
        });
        broadcastCallback(packageName, mCallbackV2List, new RemoteCallbackBroadcastHandler<IVoiceAssistantCallbackV2>() {
            @Override
            public void onBroadcast(String packageName, IVoiceAssistantCallbackV2 cbk) throws Exception {
                cbk.onError(errorCode, null);
            }
        });
        cleanCached();
    }

    private void resultReceived(final RecognizeResult result, String packageName) {
        log.i("resultReceived: " + result);
        mPartialResultHelper.onResult(result);
        // 离线的结果只取第一个item即解析出的“rawtext”进行处理，后面的语义结果忽略
        String[] results = result.isOffline()
                ? new String[] {StringUtils.getStringOrEmpty(result.getMainContent())}
                : result.getContents();
        if(mBaseType == RecognizeArgsHelper.BASE_TYPE_SEARCH) {
            if (isStopEngine) {
                log.infoRelease("resultReceived return for is stop engine");
                return;
            }
            if ((mLocalSearchType & (TYPE_ALL | TYPE_BULLET)) != 0) {
                if (results == null || results.length == 0 || TextUtils.isEmpty(results[0])) {
                    showResultCallback(true, false, packageName, "");
                    return;
                }
                resultList = results;
                mClienResult.put(packageName, resultList);
                loadDataInBackground(results, ChangeType.NONE, false, packageName);
            }
        } else {
            String resultStr;
            if (results == null || results.length < 1) {
                resultStr = "";
            } else {
                if (result.isCandidate()) {
                    resultStr = result.getExtras().getString("candidate_string");
                } else if (mBaseType == RecognizeArgsHelper.BASE_TYPE_VOICE_COMMAND) {
                    StringBuilder sb = new StringBuilder();
                    RecognizeResult.Item[] items = result.getItems();
                    if (items != null && items.length > 0) {
                        for (RecognizeResult.Item item : items) {
                            sb.append(":").append(item.content);
                            if (item.id != RecognizeResult.ID_INVALID) {
                                sb.append("-");
                                if (item.id >= 0) {
                                    sb.append(item.id);
                                } else {
                                    sb.append(GrammarManager.getPreloadLexiconName(item.id));
                                }
                            }
                        }
                        sb.deleteCharAt(0);
                    }
                    resultStr = sb.toString();
                } else {
                    resultStr = results[0];
                }
            }

            final String finalResult = resultStr;

            broadcastCallback(packageName, mRemoteCallBackList, new RemoteCallbackBroadcastHandler<IVoiceAssistantCallback>() {
                @Override
                public void onBroadcast(String packageName, IVoiceAssistantCallback cbk) throws Exception {
                    cbk.onResultRecived(finalResult, mLocalSearchType, result.isOffline());
                }
            });
            broadcastCallback(packageName, mCallbackV2List, new RemoteCallbackBroadcastHandler<IVoiceAssistantCallbackV2>() {
                @Override
                public void onBroadcast(String packageName, IVoiceAssistantCallbackV2 cbk) throws Exception {
                    cbk.onResult(result);
                }
            });

            StringBuilder sb = new StringBuilder(StringUtils.getStringOrEmpty(mCachedPartialString));
            String allResult = sb.toString();
            if (TextUtils.isEmpty(allResult)){
                return;
            }
            if (allResult.length() <= VoiceConstant.INTERVAL_SHORT && StringUtils.isChinesePunctuation(allResult.charAt(allResult.length() - 1))) {
                int index = allResult.length() - 1;
                if(index < 0 || index >= sb.length()) {
                    log.e("StringIndexOutOfBoundsException index is less than 0 or larger than  sb length");
                } else {
                    sb.deleteCharAt(index);
                }
            }
            if (sb.toString().length() <= VoiceConstant.INTERVAL_SHORT) {
                loadDataInBackground(new String[] {StringUtils.trimPunctuation(sb.toString())}, ChangeType.NONE, false, packageName);
            } else {
                showResultCallback(true, false, packageName, "");
            }
        }
    }
    private void volumeUpdate(final int volume, String packageName) {
        broadcastCallback(packageName, mRemoteCallBackList, new RemoteCallbackBroadcastHandler<IVoiceAssistantCallback>() {
            @Override
            public void onBroadcast(String packageName, IVoiceAssistantCallback cbk) throws Exception {
                cbk.onVolumeUpdate(volume);
            }
        });
        broadcastCallback(packageName, mCallbackV2List, new RemoteCallbackBroadcastHandler<IVoiceAssistantCallbackV2>() {
            @Override
            public void onBroadcast(String packageName, IVoiceAssistantCallbackV2 cbk) throws Exception {
                cbk.onVolumeUpdate(volume);
            }
        });
    }

    private void partialResult(final RecognizeResult partial, String packageName) {
        if (partial == null || TextUtils.isEmpty(partial.getMainContent())){
            return;
        }

        log.i("partialResult: " + partial);
        mPartialResultHelper.onPartialResult(partial);
        final String partialString = partial.getMainContent();
        int handled = broadcastCallback(packageName, mRemoteCallBackList, new RemoteCallbackBroadcastHandler<IVoiceAssistantCallback>() {
            @Override
            public void onBroadcast(String packageName, IVoiceAssistantCallback cbk) throws Exception {
                cbk.onPartialResult(partialString);
            }
        });
        handled += broadcastCallback(packageName, mCallbackV2List, new RemoteCallbackBroadcastHandler<IVoiceAssistantCallbackV2>() {
            @Override
            public void onBroadcast(String packageName, IVoiceAssistantCallbackV2 cbk) throws Exception {
                cbk.onPartialResult(partial);
            }
        });

        if (handled < 1) {
            mPartialCaches.add(partial);
        }
    }

    private void recordStart(String packageName) {
        broadcastCallback(packageName, mRemoteCallBackList, new RemoteCallbackBroadcastHandler<IVoiceAssistantCallback>() {
            @Override
            public void onBroadcast(String packageName, IVoiceAssistantCallback cbk) throws Exception {
                cbk.onRecordStart();
            }
        });
        broadcastCallback(packageName, mCallbackV2List, new RemoteCallbackBroadcastHandler<IVoiceAssistantCallbackV2>() {
            @Override
            public void onBroadcast(String packageName, IVoiceAssistantCallbackV2 cbk) throws Exception {
                cbk.onRecordStart();
            }
        });
    }
    private void recordEnd(String packageName){
        mIsRecognizing = false;
        broadcastCallback(packageName, mRemoteCallBackList, new RemoteCallbackBroadcastHandler<IVoiceAssistantCallback>() {
            @Override
            public void onBroadcast(String packageName, IVoiceAssistantCallback cbk) throws Exception {
                cbk.onRecordEnd();
            }
        });
        broadcastCallback(packageName, mCallbackV2List, new RemoteCallbackBroadcastHandler<IVoiceAssistantCallbackV2>() {
            @Override
            public void onBroadcast(String packageName, IVoiceAssistantCallbackV2 cbk) throws Exception {
                cbk.onRecordEnd();
            }
        });
    }

    private void buffer(final byte[] buffer, String packageName) {
        int handled = broadcastCallback(packageName, mRemoteCallBackList, new RemoteCallbackBroadcastHandler<IVoiceAssistantCallback>() {
            @Override
            public void onBroadcast(String packageName, IVoiceAssistantCallback cbk) throws Exception {
                cbk.onBuffer(buffer);
            }
        });
        handled += broadcastCallback(packageName, mCallbackV2List, new RemoteCallbackBroadcastHandler<IVoiceAssistantCallbackV2>() {
            @Override
            public void onBroadcast(String packageName, IVoiceAssistantCallbackV2 cbk) throws Exception {
                cbk.onBuffer(buffer);
            }
        });

        if (handled < 1) { // 没有任何回调执行，则先缓存buffer数据
            if (mBytesCached == null) {
                mBytesCached = buffer;
            } else {
                mBytesCached = Arrays.copyOf(mBytesCached, mBytesCached.length + buffer.length);
            }
        }
    }

    interface RemoteCallbackBroadcastHandler<T extends IInterface> {
        void onBroadcast(String packageName, T cbk) throws Exception;
    }
    private <T extends IInterface> int broadcastCallback(String packageName, RemoteCallbackList<T> callbackList, RemoteCallbackBroadcastHandler<T> handler) {
        if (callbackList == null || callbackList.getRegisteredCallbackCount() == 0
                || handler == null || TextUtils.isEmpty(packageName)) {
            return 0;
        }

        int handledCount = 0;
        synchronized (this) {
            try {
                int count = callbackList.beginBroadcast();
                List<T> oldCbks = getBroadcastCallbacks(packageName, callbackList, count);
                handledCount = oldCbks.size();
                for (T callback : oldCbks) {
                    handler.onBroadcast(packageName, callback);
                }
            } catch (Exception e) {
                log.e("exception when broadcastCallback " + e.getMessage(), e);
            } finally {
                callbackList.finishBroadcast();
            }
        }

        return handledCount;
    }

    private <T extends IInterface> List<T> getBroadcastCallbacks(String packageName, RemoteCallbackList<T> callbackList, int count) {
        List<T> cbks = new ArrayList<T>();

        if (!TextUtils.isEmpty(packageName) && callbackList != null && count > 0) {
            long registerTime = Long.MIN_VALUE;
            for (int i = 0; i < count; i++) {
                T callback = callbackList.getBroadcastItem(i);
                if (packageName.equals(mCallBackList.get(callback.asBinder()))) {
                    Long time = mCallBacksRegistTime.get(callback.asBinder());
                    if (time != null && time > registerTime) {
                        registerTime = mCallBacksRegistTime.get(callback.asBinder());
                        cbks.add(callback);
                    }
                }
            }
        }

        return cbks;
    }

    private void cleanCached() {
        mPartialCaches.clear();
        mPartialResultHelper.onRecordStart(); // 清除helper中缓存的result
        mCachedPartialString = null;
        mBytesCached = null;
    }
    private void registerReceiver(Context context) {
        IntentFilter filter = new IntentFilter(Intent.ACTION_PACKAGE_ADDED);
        filter.addAction(Intent.ACTION_PACKAGE_REMOVED);
        filter.addAction(Intent.ACTION_PACKAGE_CHANGED);
        filter.addAction(PackageManagerSmt.get_ACTION_SM_PACKAGES_LOCKED());
        filter.addAction(PackageManagerSmt.get_ACTION_SM_PACKAGES_UNLOCKED());
        filter.addAction(VoiceConstant.ACTION_RECORD_ERROR);
        context.registerReceiver(mReceiver, filter);
    }
    /**
     * registerObserver for contact change
     */
    private void registerContactObserver(final Context context, final ContentResolver contentResolver) {
        mContactChangeObserver = new ContentObserver(new Handler()) {
            @Override
            public void onChange(boolean selfChange) {
               refreshAction(ChangeType.CONTACT);
            }
        };

        contentResolver.registerContentObserver(Uri.parse("content://com.android.contacts/contacts/as_vcard"), false,
                mContactChangeObserver);
    }
    private void registerMusicObserver(final Context context, final ContentResolver contentResolver) {
        mMusicChangeObserver = new ContentObserver(new Handler()) {
            @Override
            public void onChange(boolean selfChange) {
               refreshAction(ChangeType.MUSIC);
            }
        };

        contentResolver.registerContentObserver(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, false,
                mMusicChangeObserver);
    }

    private void refreshAction(final ChangeType changeType) {
        Iterator<String> keyIterator = mClienResult.keySet().iterator();
        while (keyIterator.hasNext()) {
            final String key = keyIterator.next();
            if ((changeType == ChangeType.APP && !VoiceConstant.PACKAGE_NAME_SEARCH.equals(key))
                    || (changeType == ChangeType.MUSIC && VoiceConstant.PACKAGE_NAME_CONTACT.equals(key))
                    || changeType == ChangeType.CONTACT
                    && VoiceConstant.PACKAGE_NAME_MUSIC.equals(key)) {
                continue;
            }
            Object temp = mClienResult.get(key);
            if (temp == null) {
                continue;
            }
            final String[] value = (String[]) temp;
            if (value.length != 0 && !TextUtils.isEmpty(value[0])) {
                mHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        loadDataInBackground(value, changeType, true, key);
                    }
                }, 500);
            }
        }
    }

    private boolean phoneIsInUse() {
        TelephonyManager telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        return telephonyManager.getCallState() != TelephonyManager.CALL_STATE_IDLE;
    }
}
