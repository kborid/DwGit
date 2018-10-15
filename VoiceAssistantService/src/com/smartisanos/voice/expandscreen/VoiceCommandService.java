package com.smartisanos.voice.expandscreen;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import android.app.Service;
import android.app.SmtVoiceCommandIndex;
import android.app.SmtVoiceCommandManager;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.XmlResourceParser;
import android.database.ContentObserver;
import android.database.Cursor;
import android.media.AudioSystem;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.telecom.TelecomManager;
import android.text.TextUtils;
import android.util.Log;

import smartisanos.app.voiceassistant.IVoiceAssistantService;
import smartisanos.app.voiceassistant.IVoiceAssistantCallback;
import smartisanos.app.voiceassistant.ParcelableObject;
import smartisanos.t9search.HanziToPinyin;

import android.voice.ISmtVoiceCommandCallback;
import android.voice.ISmtVoiceCommandService;
import com.smartisanos.voice.R;

import com.smartisanos.voice.expandscreen.trio.acdatrie.AhoCorasickDoubleArrayTrie;

// For [Rev One]
// This is a service that was provided to application is expand screen.
public class VoiceCommandService extends Service {
    private static final String TAG = "VoiceCommandService";
    private static final String VOICE_ASSISTANT_PACKAGE_NAME = "com.smartisanos.voice";
    private static final String VOICE_ASSISTANT_CLASS_NAME = "com.smartisanos.voice.service.VoiceAssistantService";
    private static final String VOICE_ASSISTANT_EXTRA = "voice_extra";
    private static final String MY_PACKAGE = "com.smartisanos.voice.expandscreen";
    private static final String CAPTIONBAR = "captionbar";
    private static final int TYPE_ALL = 7;
    private static final int TYPE_CANDIDATE = 8;
    private static final int TYPE_VOICE_COMMAND = 0x10;
    private static final String[] END_CHARS ={"。", "？", "！", "."};
    private static final int TEXT_WITH_PUN_LIMIT = 12;
    private static final int STOP_DELAY = 200;

    private Context mContext;
    private IVoiceAssistantService mVoiceRecognizeService;
    private IVoiceAssistantCallback mVoiceAssistCallback;
    private String mRecognizingPackage = "";
    private IBinder mBind;
    private HashMap<String, ISmtVoiceCommandCallback> mExpandVoiceCallbacks = new HashMap<String, ISmtVoiceCommandCallback>();
    private HashMap<String, ArrayList<VoiceCommand>> mVoiceCommandMap = new HashMap<String, ArrayList<VoiceCommand>>();
    private StringBuffer mRecognizingString;
    private int mFlag;
    private Object mLockObject = new Object();
    private Map<String, String> mAppNameMap = new HashMap<String, String>();
    private Map<String, String> mAppAliasNameMap = new HashMap<String, String>();
    private AhoCorasickDoubleArrayTrie<String> mComandsDict = new AhoCorasickDoubleArrayTrie<>();
    private AhoCorasickDoubleArrayTrie<String> mCaptionBarComandsDict = new AhoCorasickDoubleArrayTrie<>();
    private AhoCorasickDoubleArrayTrie<String> mLauncherComandsDict = new AhoCorasickDoubleArrayTrie<>();
    private String mDictBuildPackage = "";
    private Map<String, String> mVoiceModuleMap = new HashMap<String, String>();
    private Map<String, HashMap<String, String>> mAllAppCommandsMap = new HashMap<String, HashMap<String, String>>();
    private Map<String, HashMap<String, Command>> mAllAppIdCommandsMap = new HashMap<String, HashMap<String, Command>>();
    private boolean mIsRecognizing;
    private TelecomManager mTeleManager;

    private ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName arg0, IBinder service) {
            Log.d(TAG, "onServiceConnected()");
            mVoiceRecognizeService = IVoiceAssistantService.Stub.asInterface(service);
            mVoiceAssistCallback = new IVoiceAssistantCallbackImpl();
            try {
                mVoiceRecognizeService.registerCallback(mVoiceAssistCallback, MY_PACKAGE);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            Log.d(TAG, "onServiceDisconnected()");
            mVoiceRecognizeService = null;
        }
    };

    private Handler mHandler = new Handler();
    private ContentObserver mAppObserver = new ContentObserver(mHandler) {
        @Override
        public void onChange(boolean selfChange) {
            new TrioCommandsLoadThread().start();
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate(), pid = " + android.os.Process.myPid());
        mContext = getApplicationContext();
        mTeleManager = (TelecomManager)mContext.getSystemService(Context.TELECOM_SERVICE);
        new TrioCommandsLoadThread().start();
        initModule();
        getContentResolver().registerContentObserver(ExpandVoiceProvider.CONTENT_URI, false, mAppObserver);
    }

    private void initModule(){
        mVoiceModuleMap.put("com.smartisanos.desktop", "lhr");
        mVoiceModuleMap.put("com.yozo.sts.office", "ofc");
        mVoiceModuleMap.put("com.android.browser", "bsr");
        mVoiceModuleMap.put("com.android.calendar", "cld");
        mVoiceModuleMap.put("com.smartisanos.music", "msc");
        mVoiceModuleMap.put("com.smartisanos.filemanager", "fmr");
        mVoiceModuleMap.put("com.android.email", "eml");
        mVoiceModuleMap.put("com.android.gallery3d", "gly");
        mVoiceModuleMap.put("com.smartisanos.notes", "nts");
        mVoiceModuleMap.put("com.android.desktop.systemui", "syt");
        //mVoiceModuleMap.put("com.smartisan.voicedemo", "nts");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand()");
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "onBind()");
        if (null == mBind) {
            mBind = new IExpandVoiceServiceImpl();
        }

        if (null == mVoiceRecognizeService) {
            bindVoiceAssistant();
        }
        return mBind;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.d(TAG, "onUnbind(), intent = " + intent.toString());
        getContentResolver().unregisterContentObserver(mAppObserver);
        unBindVoiceAssistant();
        return super.onUnbind(intent);
    }

    private void bindVoiceAssistant() {
        Log.d(TAG, "bindVoiceService()");
        try {
            Intent intent = new Intent();
            intent.setComponent(new ComponentName(VOICE_ASSISTANT_PACKAGE_NAME, VOICE_ASSISTANT_CLASS_NAME));
            intent.putExtra(VOICE_ASSISTANT_EXTRA, true);
            bindService(intent, mServiceConnection, Context.BIND_AUTO_CREATE);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void unBindVoiceAssistant() {
        if (null != mVoiceRecognizeService) {
            try {
                mVoiceRecognizeService.unregisterCallback(mVoiceAssistCallback);
            } catch (Exception e) {
                e.printStackTrace();
            }
            unbindService(mServiceConnection);
            mVoiceRecognizeService = null;
            mVoiceAssistCallback = null;
        }
    }

    private String getModuleName(String packageName, int flag){
        String moduleName = null;
        if ((mFlag & SmtVoiceCommandManager.TYPE_CAPTIONBAR) != 0){
            moduleName = CAPTIONBAR;
        }
        else{
            moduleName = mVoiceModuleMap.get(packageName);
        }

        return moduleName;
    }

    private class IVoiceAssistantCallbackImpl extends IVoiceAssistantCallback.Stub {
        @Override
        public void onLocalResult(final ParcelableObject result, boolean isRefresh) throws RemoteException {
            Log.d(TAG, "onLocalResult()");
        }

        @Override
        public void onError(final int errorCode, int type, boolean isToast) throws RemoteException {
            Log.d(TAG, "onError(), errorCode = " + errorCode);
            if (!mIsRecognizing) return;
            mIsRecognizing = false;
            synchronized (mLockObject) {
                ISmtVoiceCommandCallback callback = mExpandVoiceCallbacks.get(mRecognizingPackage);
                if (null != callback) {
                    if (errorCode == -1 && isVoiceBusy()){
                        callback.onError(SmtVoiceCommandManager.ERROR_CANNT_USED, type, isToast);
                    }
                    else{
                        callback.onError(errorCode, type, isToast);
                    }
                }
            }
        }

        @Override
        public void onVolumeUpdate(int volume) throws RemoteException {
            synchronized (mLockObject) {
                ISmtVoiceCommandCallback callback = mExpandVoiceCallbacks.get(mRecognizingPackage);
                if (null != callback) {
                    callback.onVolumeUpdate(volume);
                }
            }
        }

        private String getAppName(String str){
            Pattern p = Pattern.compile("-app$");
            Matcher matcher = p.matcher(str);
            if (matcher.find()){
                return str.substring(0, matcher.start());
            }
            else{
                return null;
            }
        }

        private int getCommandId(String str){
            Pattern p = Pattern.compile("[0-9]{1,}$");
            Matcher matcher = p.matcher(str);
            if (matcher.find()){
                return Integer.valueOf(matcher.group());
            }
            else{
                return 0;
            }
        }

        private String getCommand(String str){
            Pattern p = Pattern.compile("-[0-9]{1,}$");
            Matcher matcher = p.matcher(str);
            if (matcher.find()){
                return str.substring(0, matcher.start());
            }
            else{
                return null;
            }
        }

        @Override
        public void onResultRecived(final String resultStr, int type, final boolean offLine) throws RemoteException {
            Log.d(TAG, "onResultRecived(), resultStr = " + resultStr);
            String resultStrToClient = resultStr;
            boolean isMatchedApp = false;
            String voiceTextPy = "";
            String resultStrNoPun = "";
            boolean isMatchedSearch = false;
            boolean isMatchedSetting = false;
            String moduleName = "";
            if (!mIsRecognizing) return;
            mIsRecognizing = false;
            synchronized (mLockObject) {
                ISmtVoiceCommandCallback callback = mExpandVoiceCallbacks.get(mRecognizingPackage);
                if (null == callback) {
                    Log.w(TAG, "onResultRecived, callback is null!");
                    return;
                }

                if (!TextUtils.isEmpty(resultStr)) {
                    String[] resultArray = resultStr.split(":");
                    if (resultArray != null && 1 < resultArray.length){
                        resultStrToClient = resultArray[0];
                        if ((mFlag & SmtVoiceCommandManager.TYPE_APP) != 0) {
                            String appName = "";
                            int openOrClose = 1;
                            if (resultArray.length == 3){
                                //example:打开便签:打开-1:便签-app
                                appName = getAppName(resultArray[2]);
                                if (appName == null){
                                    //example:打开上网:打开-1:上网-6003
                                    int idApp = getCommandId(resultArray[2]);
                                    if (SmtVoiceCommandIndex.APP < idApp && idApp < SmtVoiceCommandIndex.APP_END){
                                        String aliasName = VoiceCommandHanziToPinyin.getPinYin(getCommand(resultArray[2]));
                                        appName = mAppNameMap.get(aliasName);
                                    }
                                }
                                openOrClose = getCommandId(resultArray[1]);

                                if (appName == null){
                                    appName = getAppName(resultArray[1]);
                                    //example:把便签打开:便签-app:打开-1
                                    if (appName == null){
                                        int idApp = getCommandId(resultArray[1]);
                                        if (SmtVoiceCommandIndex.APP < idApp && idApp < SmtVoiceCommandIndex.APP_END){
                                            String aliasName = VoiceCommandHanziToPinyin.getPinYin(getCommand(resultArray[1]));
                                            appName = mAppNameMap.get(aliasName);
                                        }
                                    }

                                    openOrClose = getCommandId(resultArray[2]);
                                }
                            }
                            else if (resultArray.length == 2){
                                //example:便签:便签-app
                                appName = getAppName(resultArray[1]);
                                if (appName == null){
                                    //example:上网:上网-6003
                                    int idApp = getCommandId(resultArray[1]);
                                    if (SmtVoiceCommandIndex.APP < idApp && idApp < SmtVoiceCommandIndex.APP_END){
                                        String aliasName = VoiceCommandHanziToPinyin.getPinYin(getCommand(resultArray[1]));
                                        appName = mAppNameMap.get(aliasName);
                                    }
                                }
                            }
                            if (!TextUtils.isEmpty(appName)){
                                ContentResolver resolver = mContext.getContentResolver();
                                Cursor cursor = resolver.query(ExpandVoiceProvider.CONTENT_URI,
                                        null,
                                        ExpandVoiceProvider.APP_COLUMN_APPNAMEPY + " LIKE '%" + VoiceCommandHanziToPinyin.getPinYin(appName) + "%'",
                                        null,
                                        null);

                                if (null != cursor) {
                                    String appNameDB = "";
                                    String packageName = "";
                                    String activityName = "";
                                    while(cursor.moveToNext()){
                                        int nameIndex = cursor.getColumnIndex(ExpandVoiceProvider.APP_COLUMN_APPNAME);
                                        int packageNameIndex = cursor.getColumnIndex(ExpandVoiceProvider.APP_COLUMN_PKGNAME);
                                        int activityNameIndex = cursor.getColumnIndex(ExpandVoiceProvider.APP_COLUMN_ACTIVITYNAME);
                                        String tempAppName = cursor.getString(nameIndex);
                                        String tempPackageName = cursor.getString(packageNameIndex);
                                        String tempActivityName = cursor.getString(activityNameIndex);
                                        if (tempAppName.length() < appNameDB.length()
                                                || TextUtils.isEmpty(appNameDB)){
                                            appNameDB = tempAppName;
                                            packageName = tempPackageName;
                                            activityName = tempActivityName;
                                        }
                                    }
                                    if (!TextUtils.isEmpty(packageName)) {
                                        callback.onCommandTest(SmtVoiceCommandIndex.APP, packageName
                                                + ":" + activityName, (openOrClose == 1 ? 1 : 0));
                                        isMatchedApp = true;
                                    }
                                    cursor.close();
                                }
                            }
                        }
                        if ((mFlag & SmtVoiceCommandManager.TYPE_SETTING) != 0 && !isMatchedApp) {
                            int settingId = 0;
                            int openOrClose = 1;
                            if (resultArray.length == 3){
                                //example:打开蓝牙:打开-1:蓝牙-5002
                                settingId = getCommandId(resultArray[2]);
                                openOrClose = getCommandId(resultArray[1]);
                                //example:把蓝牙打开:蓝牙-5002:打开-1
                                if (settingId < SmtVoiceCommandIndex.QUICKSETTING){
                                    settingId = getCommandId(resultArray[1]);
                                    openOrClose = getCommandId(resultArray[2]);
                                }
                            }
                            else if (resultArray.length == 2){
                                //example:蓝牙:蓝牙-5002
                                settingId = getCommandId(resultArray[1]);
                            }
                            if (SmtVoiceCommandIndex.QUICKSETTING < settingId && settingId < SmtVoiceCommandIndex.QUICKSETTING_END){
                                callback.onCommandTest(SmtVoiceCommandIndex.QUICKSETTING,
                                        "" + (settingId - SmtVoiceCommandIndex.QUICKSETTING), (openOrClose == 1 ? 1 : 0));
                                isMatchedSetting = true;
                            }
                        }
                        if ((mFlag & SmtVoiceCommandManager.TYPE_COMMAND) != 0
                                && !isMatchedApp
                                && !isMatchedSearch
                                && !isMatchedSetting
                                ) {
                            moduleName = getModuleName(mRecognizingPackage, mFlag);
                            //关闭全部应用:关闭全部应用-102
                            if (resultArray.length == 2 && moduleName != null){
                                int id = getCommandId(resultArray[1]);
                                HashMap<String, Command> appCmdMap = mAllAppIdCommandsMap.get(moduleName);
                                Command command = null;
                                Iterator map1it = appCmdMap.entrySet().iterator();
                                while(map1it.hasNext()){
                                    Map.Entry<String, Command> entry=(Entry<String, Command>) map1it.next();
                                    command = (Command)entry.getValue();
                                    if (command.id == id){
                                        break;
                                    } else {
                                        command = null;
                                    }
                                }
                                if (command == null){
                                    callback.onCommandTest(-1, null, 0);
                                }
                                else{
                                    callback.onCommandTest(command.id, command.value, 0);
                                }
                            }
                            else{
                                callback.onCommandTest(-1, null, 0);
                            }
                        }
                    }
                    else {
                        mRecognizingString.append(resultStr);
                        resultStrNoPun = mRecognizingString.toString();
                        for (int i = 0; i < END_CHARS.length; i++) {
                            if (mRecognizingString.toString().endsWith(END_CHARS[i])) {
                                resultStrNoPun = mRecognizingString.toString().substring(0, mRecognizingString.length() - 1);
                                if (resultStr.endsWith(END_CHARS[i])
                                        && mRecognizingString.length() <= TEXT_WITH_PUN_LIMIT) {
                                    resultStrToClient = resultStr.substring(0, resultStr.length() - 1);
                                }
                            }
                        }

                        if (TextUtils.isEmpty(resultStrNoPun)) {
                            Log.w(TAG, "resultStrNoPun is empty!");
                            callback.onError(SmtVoiceCommandManager.ERROR_CODE_NO_DATA, 0, false);
                            return;
                        }

                        moduleName = getModuleName(mRecognizingPackage, mFlag);
                        String voiceText = resultStrNoPun.replace(" ", "_");
                        voiceTextPy = VoiceCommandHanziToPinyin.getPinYin(voiceText);

                        synchronized (mComandsDict) {
                            AhoCorasickDoubleArrayTrie<String> curDict = mComandsDict;
                            if ((mFlag & SmtVoiceCommandManager.TYPE_CAPTIONBAR) != 0) {
                                curDict = mCaptionBarComandsDict;
                            } else if ("com.smartisanos.desktop".equals(mRecognizingPackage)
                                    || "com.android.desktop.systemui".equals(mRecognizingPackage)) {
                                curDict = mLauncherComandsDict;
                            }

                            if ((mFlag & SmtVoiceCommandManager.TYPE_APP) != 0) {
                                isMatchedApp = sendApplicationToClient(callback, curDict, voiceTextPy);
                            }

                            if ((mFlag & SmtVoiceCommandManager.TYPE_SETTING) != 0 && !isMatchedApp) {
                                isMatchedSetting = sendSettingsOperatorToClient(callback,
                                        curDict, voiceTextPy.toLowerCase());
                            }
                            //remove search commands
                            //if ((mFlag & SmtVoiceCommandManager.TYPE_SEARCH) != 0 && !isMatchedApp && !isMatchedSetting) {
                            //    isMatchedSearch = sendSearchToClient(callback, voiceText.toLowerCase());
                            //}

                            if ((mFlag & SmtVoiceCommandManager.TYPE_COMMAND) != 0
                                    && !isMatchedApp
                                    && !isMatchedSearch
                                    && !isMatchedSetting
                                    ) {
                                List<AhoCorasickDoubleArrayTrie<String>.Hit<String>> hits
                                        = curDict.parseText(voiceTextPy.toLowerCase());
                                if (null != hits && 1 <= hits.size()) {
                                    AhoCorasickDoubleArrayTrie<String>.Hit<String> hitBeginZero = hits.get(0);
                                    int charsCount = 0;

                                    for (int i = 0; i < hits.size(); i++) {
                                        AhoCorasickDoubleArrayTrie<String>.Hit<String> hitNew = hits.get(i);
                                        int charsCountNew = hitNew.end - hitNew.begin;
                                        if (charsCount < charsCountNew) {
                                            charsCount = charsCountNew;
                                            hitBeginZero = hitNew;
                                        }
                                    }

                                    voiceText = hitBeginZero.value;
                                    Log.d(TAG, "VoiceCommand trio transfer from " + resultStrNoPun + " to " + voiceText);
                                } else {
                                    Log.d(TAG, "trio hits is null!");
                                }
                                //voiceTextPy = VoiceCommandHanziToPinyin.getPinYin(voiceText);
                                //sendCommandToClient(callback, voiceTextPy);
                                HashMap<String, Command> cmdMap = mAllAppIdCommandsMap.get(moduleName);
                                Command cmd = cmdMap.get(voiceText);
                                if (cmd == null) {
                                    callback.onCommandTest(-1, null, 0);
                                } else {
                                    callback.onCommandTest(cmd.id, cmd.value, 0);
                                }
                            }

                            curDict = null;
                        }
                    }
                }

                callback.onResultRecived(resultStrToClient, type, offLine);
            }
        }

        @Override
        public void onBuffer(final byte[] buffer) throws RemoteException {
            synchronized (mLockObject) {
                ISmtVoiceCommandCallback callback = mExpandVoiceCallbacks.get(mRecognizingPackage);
                if (null != callback) {
                    callback.onBuffer(buffer);
                }
            }
        }

        @Override
        public void onPartialResult(final String partialResult) throws RemoteException {
            Log.d(TAG, "onPartialResult(), partialResult = " + partialResult);
            if (!mIsRecognizing) return;
            synchronized (mLockObject) {
                ISmtVoiceCommandCallback callback = mExpandVoiceCallbacks.get(mRecognizingPackage);
                if (null != partialResult) {
                    mRecognizingString.append(partialResult);
                }
                if (null != callback) {
                    callback.onPartialResult(partialResult);
                }
            }
        }

        @Override
        public void onRecordStart() throws RemoteException {
            Log.d(TAG, "onRecordStart()");
            synchronized (mLockObject) {
                ISmtVoiceCommandCallback callback = mExpandVoiceCallbacks.get(mRecognizingPackage);
                if (null != callback) {
                    callback.onRecordStart();
                }
                mRecognizingString = new StringBuffer();
            }
        }

        @Override
        public void onRecordEnd() throws RemoteException {
            Log.d(TAG, "onRecordEnd()");
            if (!mIsRecognizing) return;
            synchronized (mLockObject) {
                ISmtVoiceCommandCallback callback = mExpandVoiceCallbacks.get(mRecognizingPackage);
                if (null != callback) {
                    callback.onRecordEnd();
                }
            }
        }

        private void sendCommandToClient(ISmtVoiceCommandCallback callback, String recognizingStringPy) throws RemoteException {
            ArrayList<VoiceCommand> commandList = mVoiceCommandMap.get(mRecognizingPackage);
            boolean isMatchSuc = false;
            if (null != commandList) {
                for (int i = 0; i < commandList.size(); i++) {
                    VoiceCommand command = commandList.get(i);
                    if (recognizingStringPy.equals(command.pinyin)) {
                        callback.onCommand(command.text, command.id);
                        isMatchSuc = true;
                        break;
                    }
                }
            }

            if (!isMatchSuc) callback.onCommand("", -1);
        }

        private boolean sendApplicationToClient(ISmtVoiceCommandCallback callback, AhoCorasickDoubleArrayTrie<String> dict, String recognizingString) throws RemoteException {
            boolean reValue = false;
            String trieResult = "";
            String matchStr = "";
            List<AhoCorasickDoubleArrayTrie<String>.Hit<String>> appNamehits = dict.parseText(recognizingString);
            if (null != appNamehits && 1 <= appNamehits.size()) {
                AhoCorasickDoubleArrayTrie<String>.Hit<String> hitBeginZero = appNamehits.get(0);
                int charsCount = 0;

                for (int i = 0; i < appNamehits.size(); i++) {
                    AhoCorasickDoubleArrayTrie<String>.Hit<String> hitNew = appNamehits.get(i);
                    int charsCountNew = hitNew.end - hitNew.begin;
                    if (0 == hitNew.begin && charsCount < charsCountNew) {
                        charsCount = charsCountNew;
                        hitBeginZero = hitNew;
                    }
                }

                trieResult = hitBeginZero.value;
                matchStr = recognizingString.substring(hitBeginZero.begin, hitBeginZero.end);
                ContentResolver resolver = mContext.getContentResolver();
                Cursor cursor = resolver.query(ExpandVoiceProvider.CONTENT_URI,
                        null,
                        ExpandVoiceProvider.APP_COLUMN_APPNAMEPY + " LIKE '%" + trieResult + "%'",
                        null,
                        null);

                if (null != cursor) {
                    String appNameDB = "";
                    String packageName = "";
                    String activityName = "";
                    while(cursor.moveToNext()){
                        int nameIndex = cursor.getColumnIndex(ExpandVoiceProvider.APP_COLUMN_APPNAME);
                        int packageNameIndex = cursor.getColumnIndex(ExpandVoiceProvider.APP_COLUMN_PKGNAME);
                        int activityNameIndex = cursor.getColumnIndex(ExpandVoiceProvider.APP_COLUMN_ACTIVITYNAME);
                        String tempAppName = cursor.getString(nameIndex);
                        String tempPackageName = cursor.getString(packageNameIndex);
                        String tempActivityName = cursor.getString(activityNameIndex);
                        if (tempAppName.length() < appNameDB.length()
                                || TextUtils.isEmpty(appNameDB)){
                            appNameDB = tempAppName;
                            packageName = tempPackageName;
                            activityName = tempActivityName;
                        }
                    }
                    if(recognizingString.equalsIgnoreCase(trieResult)){
                        callback.onCommandTest(SmtVoiceCommandIndex.APP, packageName
                                + ":" + activityName, 1);
                        return true;
                    }
                    // If voice engine return is alias name of app.
                    String appName = mAppNameMap.get(recognizingString);
                    if (appName != null && appName.equalsIgnoreCase(trieResult)){
                        callback.onCommandTest(SmtVoiceCommandIndex.APP, packageName
                                + ":" + activityName, 1);
                        return true;
                    }

                    String afterReplaceStr = recognizingString.replace(matchStr, "_app_");
                    List<AhoCorasickDoubleArrayTrie<String>.Hit<String>> hits = dict.parseText(afterReplaceStr);
                    if (null != hits && 1 <= hits.size()) {
                        trieResult = hits.get(hits.size() - 1).value;
                        if (trieResult.equals("CLOSEAPP")) {
                            callback.onCommandTest(SmtVoiceCommandIndex.APP, packageName
                                    + ":" + activityName, 0);
                            reValue = true;
                        } else if (trieResult.equals("OPENAPP")){
                            callback.onCommandTest(SmtVoiceCommandIndex.APP, packageName
                                    + ":" + activityName, 1);
                            reValue = true;
                        }
                    }

                    cursor.close();
                }
            }

            return reValue;
        }

        private boolean sendSearchToClient(ISmtVoiceCommandCallback callback, String recognizingString) throws RemoteException{
            String[] searchPrefArray = mContext.getResources().getStringArray(R.array.revone_voice_command_search_pref);
            String matchPref = "";
            String keyword = "";
            for (String pref: searchPrefArray) {
                int index = recognizingString.toLowerCase().indexOf(pref);
                if (-1 != index) {
                    if (pref.length() < matchPref.length()){
                        continue;
                    }
                    matchPref = pref;
                    keyword = recognizingString.substring(index + pref.length());
                }
            }

            if (TextUtils.isEmpty(keyword)){
                return false;
            }
            else{
                callback.onCommandTest(SmtVoiceCommandIndex.SEARCH, keyword, 0);
                return true;
            }
        }

        private boolean sendSettingsOperatorToClient(ISmtVoiceCommandCallback callback, AhoCorasickDoubleArrayTrie<String> dict, String recognizingString) throws RemoteException{
            String settingName;
            String regularString;
            String trieSource;
            int keyCode;
            boolean reValue = false;
            List<AhoCorasickDoubleArrayTrie<String>.Hit<String>> settingNamehits = dict.parseText(recognizingString);
            if (null != settingNamehits && 1 <= settingNamehits.size()) {
                AhoCorasickDoubleArrayTrie<String>.Hit<String> hitBeginZero = settingNamehits.get(0);
                int charsCount = 0;

                for (int i = 0; i < settingNamehits.size(); i++) {
                    AhoCorasickDoubleArrayTrie<String>.Hit<String> hitNew = settingNamehits.get(i);
                    int charsCountNew = hitNew.end - hitNew.begin;
                    if (charsCount < charsCountNew) {
                        charsCount = charsCountNew;
                        hitBeginZero = hitNew;
                    }
                }

                settingName = hitBeginZero.value;
                String[] values = settingName.split("_");
                if (values != null && 2 == values.length) {
                    keyCode = Integer.parseInt(values[1]);
                }
                else{
                    return false;
                }
                trieSource = recognizingString.substring(hitBeginZero.begin, hitBeginZero.end);
                if (trieSource.equalsIgnoreCase(recognizingString)){
                    callback.onCommandTest(SmtVoiceCommandIndex.QUICKSETTING, "" + keyCode, 1);
                    return true;
                }

                regularString = recognizingString.replace(trieSource, "_set_");
                List<AhoCorasickDoubleArrayTrie<String>.Hit<String>> hits = dict.parseText(regularString);
                if (null != hits && 1 <= hits.size()){
                    String operatorType = hits.get(hits.size() - 1).value;
                    if (operatorType.equals("CLOSESET")) {
                        callback.onCommandTest(SmtVoiceCommandIndex.QUICKSETTING, "" + keyCode, 0);
                    }
                    else if (operatorType.equals("OPENSET")){
                        callback.onCommandTest(SmtVoiceCommandIndex.QUICKSETTING, "" + keyCode, 1);
                    }

                    reValue = true;
                }
            }
            return reValue;
        }
    }

    private class IExpandVoiceServiceImpl extends ISmtVoiceCommandService.Stub {
        @Override
        public void registerCallback(String packageName, ISmtVoiceCommandCallback callback) {
            Log.d(TAG, "registerCallback()");
            synchronized (mLockObject) {
                mExpandVoiceCallbacks.put(packageName, callback);
                if (null == mVoiceRecognizeService) {
                    bindVoiceAssistant();
                }
            }
        }

        @Override
        public void registerVoiceCommand(String packageName, Map commandsMap) {
            Log.d(TAG, "registerVoiceCommand()");
            if (commandsMap.size() <= 0) {
                Log.w(TAG, "Do not register empty voice command map!");
                return;
            }

            ArrayList<VoiceCommand> commandList = new ArrayList<VoiceCommand>();
            ArrayList<VoiceCommand> existList = null;
            List<String> existCommandList = new ArrayList<String>();
            if (mVoiceCommandMap.containsKey(packageName)){
                existList = mVoiceCommandMap.get(packageName);
                for (VoiceCommand command : existList){
                    existCommandList.add(command.text);
                }
            }
            else{
                mVoiceCommandMap.put(packageName, commandList);
            }

            Iterator iter = commandsMap.entrySet().iterator();
            while (iter.hasNext()) {
                Entry entry = (Entry) iter.next();
                VoiceCommand command = new VoiceCommand();
                command.text = (String) entry.getKey();
                command.id = (Integer) entry.getValue();
                command.pinyin = VoiceCommandHanziToPinyin.getPinYin(command.text);
                if (existList != null){
                    if (existCommandList.contains(command.text)){
                        continue;
                    }
                    existList.add(command);
                }
                else{
                    commandList.add(command);
                }

            }
        }

        public void unRegisterCallback(String packageName) {
            Log.d(TAG, "unRegisterCallback()");
            synchronized (mLockObject) {
                if (mExpandVoiceCallbacks.containsKey(packageName)) {
                    mExpandVoiceCallbacks.remove(packageName);
                }
                if (mVoiceCommandMap.containsKey(packageName)) {
                    mVoiceCommandMap.remove(packageName);
                }
                if (0 == mExpandVoiceCallbacks.size()) {
                    unBindVoiceAssistant();
                }
            }
        }

        @Override
        synchronized public boolean startRecongnize(String packageName, ISmtVoiceCommandCallback callback, int flag) {
            Log.d(TAG, "startRecongnize()");
            try {
                if (isVoiceBusy()){
                    return false;
                }

                if (callback != null) {
                    if (mIsRecognizing){
                        callback.onError(SmtVoiceCommandManager.ERROR_CANNT_USED, 0, false);
                        return false;
                    }
                    mExpandVoiceCallbacks.put(packageName, callback);
                }
                else{
                    Log.e(TAG, "startRecongnize, callback is null!");
                    return false;
                }

                if (SmtVoiceCommandManager.TYPE_TEXT != flag
                        && (flag & SmtVoiceCommandManager.TYPE_CANDIDATE) == 0
                        && (flag & SmtVoiceCommandManager.TYPE_CAPTIONBAR) == 0
                        && !"com.smartisanos.desktop".equals(mRecognizingPackage)
                        && !"com.android.desktop.systemui".equals(mRecognizingPackage)){
                    createTrioDictionary(packageName);
                }

                if (mVoiceRecognizeService != null
                        && !mVoiceRecognizeService.isRecognizing()) {
                    int type = TYPE_ALL;
                    if ((flag & SmtVoiceCommandManager.TYPE_CANDIDATE) != 0){
                        type = TYPE_CANDIDATE;
                    }
                    else if (SmtVoiceCommandManager.TYPE_TEXT != flag){
                        type = TYPE_VOICE_COMMAND;
                    }

                    synchronized (mLockObject) {
                        mRecognizingPackage = packageName;
                        mFlag = flag;
                    }
                    mIsRecognizing = true;
                    if (SmtVoiceCommandManager.TYPE_TEXT == flag
                            || (flag & SmtVoiceCommandManager.TYPE_CANDIDATE) != 0) {
                        mVoiceRecognizeService.startRecongnize(MY_PACKAGE, type, null);
                    } else {
                        mVoiceRecognizeService.startRecongnize(MY_PACKAGE, type, getModuleName(packageName, flag));
                    }
                }
            } catch (RemoteException e) {
                e.printStackTrace();
                mIsRecognizing = false;
                return false;
            }

            return true;
        }

        @Override
        public void stopRecongnize(boolean immediately) {
            Log.d(TAG, "stopRecongnize()");
            if (immediately) mIsRecognizing = false;
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    try {
                        if (mVoiceRecognizeService != null
                                && mVoiceRecognizeService.isRecognizing()) {
                            mVoiceRecognizeService.stopRecongnize(true);
                        }
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                }
            }, STOP_DELAY);
        }

        @Override
        synchronized public boolean isRecognizing(){
            try {
                if (mVoiceRecognizeService != null) {
                    return mVoiceRecognizeService.isRecognizing() || mIsRecognizing;
                }
            } catch (RemoteException e) {
                e.printStackTrace();
            }

            return false;
        }
    }

    private void createTrioDictionary(final String packageName){
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                if (!packageName.equals(mDictBuildPackage)) {
                    String module;
                    if (CAPTIONBAR.equals(packageName)){
                        module = CAPTIONBAR;
                    }
                    else if (mVoiceModuleMap.containsKey(packageName)){
                        module = mVoiceModuleMap.get(packageName);
                    }
                    else{
                        return;
                    }

                    mDictBuildPackage = packageName;
                    HashMap<String, String> appCommandsMap = mAllAppCommandsMap.get(module);
                    if ("lhr".equals(module)
                            || "syt".equals(module)){
                        appCommandsMap.putAll(mAppNameMap);
                    }
                    synchronized (mComandsDict) {
                        if (0 < appCommandsMap.size()) {
                            mComandsDict.build(appCommandsMap);
                        }
                    }
                }
            }
        });
    }

    private boolean isVoiceBusy(){
        if (mTeleManager != null && mTeleManager.isInCall()
                || checkRecorder()){
            return true;
        }

        return false;
    }

    private boolean checkRecorder() {
        for (int i = 0; i < AudioSystem.STREAM_TTS; i++) {
            if (AudioSystem.isSourceActive(i)) {
                return true;
            }
        }
        return false;
    }

    class VoiceCommand {
        public int id;
        public String text;
        public String pinyin;
    }

    private class TrioCommandsLoadThread extends Thread {
        @Override
        public void run() {
            mDictBuildPackage = null;
            mAppNameMap.clear();
            if (mAllAppCommandsMap.size() == 0) {
                XmlResourceParser xmlParser = mContext.getResources().getXml(R.xml.trio_voice_commands);
                int event = 0;
                HashMap<String, String> appCommandMap = null;
                HashMap<String, Command> appIdCommandMap = null;
                try {
                    event = xmlParser.getEventType();
                    String module = "", key = "", value = "", groups = "";
                    //boolean isNeedPref = true;
                    boolean isApp = false;
                    Command command = null;
                    while (event != XmlPullParser.END_DOCUMENT) {
                        switch (event) {
                            case XmlPullParser.START_DOCUMENT:
                                break;
                            case XmlPullParser.START_TAG:
                                if (xmlParser.getName().equals("module")) {
                                    module = xmlParser.getAttributeValue(0);
                                    appCommandMap = new HashMap<String, String>();
                                    appIdCommandMap = new HashMap<String, Command>();
                                } else if (xmlParser.getName().equals("command")) {
                                    String appText = xmlParser.getAttributeValue(null, "isApp");
                                    if ("true".equals(appText)){
                                        isApp = true;
                                    }
                                    else{
                                        isApp = false;
                                    }
                                    String id = xmlParser.getAttributeValue(null, "id");
                                    command = new Command();
                                    if (!TextUtils.isEmpty(id)){
                                        command.id = Integer.valueOf(id);
                                    }
                                    groups = xmlParser.getAttributeValue(null, "groups");
                                } else if (xmlParser.getName().equals("value")) {
                                    value = xmlParser.nextText();
                                    command.value = value;
                                } else if (xmlParser.getName().equals("item")) {
                                    key = xmlParser.nextText();
                                    if (TextUtils.isEmpty(groups) || !groups.equals("operation")) {
                                        if (isApp) {
                                            mAppAliasNameMap.put(VoiceCommandHanziToPinyin.getPinYin(key), VoiceCommandHanziToPinyin.getPinYin(value));
                                        } else {
                                            appCommandMap.put(VoiceCommandHanziToPinyin.getPinYin(key), value);
                                        }
                                        command.keys.add(key);
                                    }
                                }
                                break;
                            case XmlPullParser.END_TAG:
                                if (xmlParser.getName().equals("voice_commands")) {
                                    //mComandsDict.build(mTrioCommadsMap);
                                } else if (xmlParser.getName().equals("module") && !module.equals("xunfei")) {
                                    Log.d(TAG, "module = " + module);
                                    mAllAppCommandsMap.put(module, appCommandMap);
                                    mAllAppIdCommandsMap.put(module, appIdCommandMap);
                                    module = "";
                                    appCommandMap = null;
                                } else if (xmlParser.getName().equals("command")) {
                                    //isNeedPref = true;
                                    isApp = false;
                                    if (TextUtils.isEmpty(groups) || !groups.equals("operation")) {
                                        if (!TextUtils.isEmpty(value)) {
                                            appIdCommandMap.put(value, command);
                                        }
                                    }
                                    command = null;
                                    value = "";
                                    groups = "";
                                }
                                break;
                            default:
                                break;
                        }

                        event = xmlParser.next();
                    }
                } catch (XmlPullParserException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            ContentResolver resolver = mContext.getContentResolver();
            Cursor cursor = resolver.query(ExpandVoiceProvider.CONTENT_URI, null, null, null, null);
            if (null != cursor){
                while (cursor.moveToNext()){
                    int packageNameIndex = cursor.getColumnIndex(ExpandVoiceProvider.APP_COLUMN_APPNAME);
                    int packageNamepyIndex = cursor.getColumnIndex(ExpandVoiceProvider.APP_COLUMN_APPNAMEPY);
                    String packageName = cursor.getString(packageNameIndex);
                    String packageNamepy = cursor.getString(packageNamepyIndex);
                    packageNamepy = packageNamepy.replace(" ", "_");
                    mAppNameMap.put(packageNamepy.toLowerCase(), packageNamepy);
                }
                cursor.close();
            }
            mAppNameMap.putAll(mAppAliasNameMap);
            HashMap<String, String> captionCommandsMap = mAllAppCommandsMap.get(CAPTIONBAR);
            mCaptionBarComandsDict.build(captionCommandsMap);
            HashMap<String, String> launcherCommandsMap = mAllAppCommandsMap.get("lhr");
            launcherCommandsMap.putAll(mAppNameMap);
            mLauncherComandsDict.build(launcherCommandsMap);
        }
    }

    class Command{
        List<String> keys = new ArrayList<String>();
        String value;
        int id;
    }
}
