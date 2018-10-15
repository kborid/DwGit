package com.smartisanos.voice.service;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.os.IBinder;
import android.os.SystemProperties;
import android.provider.MediaStore;
import android.telephony.TelephonyManager;

import com.smartisanos.voice.engine.XunfeiRecognizerEngine;
import com.smartisanos.voice.util.ApplicationUtil;
import com.smartisanos.voice.util.ContactsUtil;
import com.smartisanos.voice.util.LogUtils;
import com.smartisanos.voice.util.MediaUtil;
import com.smartisanos.voice.util.SharePrefUtil;
import com.smartisanos.voice.util.VoiceConstant;
import com.smartisanos.voice.util.VoiceUtils;
import com.smartisanos.voice.util.WorkHandler;

import java.util.Locale;

import smartisanos.util.config.Features;
/**
 * VoiceAssistantService handles remote wakelock and app usage count by implementing
 * the IVoiceAssistantManager interface
 * and start inner service of voice assistant to load datas
 */
public class VoiceDataService extends Service {
    static final LogUtils log = LogUtils.getInstance(VoiceDataService.class);
    private static final boolean DEBUG = (SystemProperties.getInt("ro.debuggable", 0) == 1);
    private static int typeFlag = 0;
    private static final int FLAG_APP_CHANGES = 1;
    private static final int FLAG_CONTACT_CHANGES = 1 << 1;
    private static final int FLAG_MUSIC_CHANGES = 1 << 2;
    private static final int FLAG_BOOT_COMPLETE = 1 << 3;
    private static final int FLAG_IFLYTEK_CLEAR_DATA = 1 << 4;
    private static final int FLAG_VOICE_CLEAR_DATA = 1 << 5;
    private static final int FLAG_LOCAL_CHANGES = 1 << 6;
    private static final int FLAG_BULLET_CHANGES = 1 << 7;
    private static final int POSTDELAYTIME = 3000;
    private static final int STABLETIME = 8000;
    private static final String SHARE_NAME = "appUsageCount";
    private static final String PACKAGE_NAME_STK = "com.android.stk";
    private static final String PACKAGE_NAME_IFLYTEK = "com.iflytek.speechsuite";
    private static final String ACTION_PACKAGE_DATA_CLEARED= "com.smartisanos.intent.action.ClearPakageData";

    private static long launchTime = 0L;

    private ContentObserver mMusicChangeObserver;
    private ContentObserver mContactChangeObserver;
    private ContentObserver mBulletChangeObserver;

    private UpdateRunnable mUpdateRunnable;

    public XunfeiRecognizerEngine mEngine = null;

    // phone state change, locale change, screen off, screen on
    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(final Context context, final Intent intent) {
            if (intent == null) {
                return;
            }
            String action = intent.getAction();
            if (DEBUG) {
                log.i("voice action is "+action);
            }
            if (TelephonyManager.ACTION_PHONE_STATE_CHANGED.equals(action)) {
                if (TelephonyManager.EXTRA_STATE_RINGING.equals(intent.getStringExtra(TelephonyManager.EXTRA_STATE)) &&
                        mEngine.isRecognizing()) {
                    mEngine.endRecognize();
                }
            } else if ((Intent.ACTION_LOCALE_CHANGED).equals(action)) {
                Locale locale = getResources().getConfiguration().locale;
                String mLanguage = locale.getLanguage();
                if (!mLanguage.endsWith("zh")) {
                    return;
                }
                initLexicon(FLAG_LOCAL_CHANGES);
            }
        }
    };
    /**
     * BroadcastReceiver for appChanges , add or remove or change or package clear of package
     */
    private BroadcastReceiver mPackageChangesReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent == null) {
                return;
            }
            String action = intent.getAction();
            if (DEBUG) {
                log.i("package change, and action is "+action);
            }
            if (Intent.ACTION_PACKAGE_ADDED.equals(action) || Intent.ACTION_PACKAGE_REMOVED.equals(action) ||
                    Intent.ACTION_PACKAGE_CHANGED.equals(action)){
                long afterBootTime = System.currentTimeMillis() - launchTime;
                if (launchTime == 0 || (afterBootTime < STABLETIME && afterBootTime > 0)) {
                    if (DEBUG) {
                        log.d("the system is not stable and return");
                    }
                    return;
                }
                if ((Intent.ACTION_PACKAGE_CHANGED).equals(action)) {
                    String PackageName = intent.getData().getSchemeSpecificPart();
                    if (DEBUG) {
                        log.i("PackageName is " + PackageName);
                    }
                    if (PACKAGE_NAME_STK.equals(PackageName)) {
                        if (DEBUG) {
                            log.d("the app is stk, so it is not be sended");
                        }
                        return;
                    }
                }
                initLexicon(FLAG_APP_CHANGES);
                if ((Intent.ACTION_PACKAGE_ADDED).equals(action)
                        && VoiceConstant.PACKAGE_NAME_BULLET.equals(intent.getData().getSchemeSpecificPart())) {
                    initLexicon(FLAG_BULLET_CHANGES);
                }
            } else   if ((Intent.ACTION_PACKAGE_DATA_CLEARED).equals(action) || ACTION_PACKAGE_DATA_CLEARED.equals(action)) {
                Uri uri = intent.getData();
                if (uri == null) {
                    return;
                }

                String pkgName = uri.getSchemeSpecificPart();
                if (pkgName == null) {
                    return;
                }
                if (PACKAGE_NAME_IFLYTEK.equals(pkgName)) {
                    initLexicon(FLAG_IFLYTEK_CLEAR_DATA);
                } else if (getPackageName().equals(pkgName)){
                    initLexicon(FLAG_VOICE_CLEAR_DATA);
                }
            }
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        if (DEBUG) {
            log.d("onCreate");
        }
        mEngine = XunfeiRecognizerEngine.getInstance();

        ContentResolver contentResolver = getContentResolver();
        if (Features.isFeatureDSDSEnabled(this)) {
            log.d("now we use U1 para is enable");
        }
        registerReceiver(this);
        registerPackageChangesReceiver(this);
        registerContactObserver(this, contentResolver);
        registerBulletObserver(this, contentResolver);
        registerMusicObserver(this, contentResolver);
        launchTime = System.currentTimeMillis();
        mUpdateRunnable=  new UpdateRunnable();
        initLexicon(FLAG_BOOT_COMPLETE);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        if (DEBUG) {
            log.d("onDestroy");
        }
        unregisterReceiver(mReceiver);
        unregisterReceiver(mPackageChangesReceiver);
        //Disable all the senser init and unreg method since pickup mode is disabled.
        //This code should be reopen if the pickup mode is enable in future.
        //unRegisterSenser();
        ContentResolver contentResolver = getContentResolver();
        contentResolver.unregisterContentObserver(mMusicChangeObserver);
        contentResolver.unregisterContentObserver(mContactChangeObserver);
        contentResolver.unregisterContentObserver(mBulletChangeObserver);
    }

    /**
     * registerObserver for contact change
     */
    private void registerContactObserver(final Context context, final ContentResolver contentResolver) {
        mContactChangeObserver = new ContentObserver(new Handler()) {
            @Override
            public void onChange(boolean selfChange) {
                if (DEBUG) {
                    log.i("the contact has changed");
                }
                long afterBootTime = System.currentTimeMillis() - launchTime;
                if (launchTime == 0 || (afterBootTime < STABLETIME && afterBootTime > 0)) {
                    if (DEBUG) {
                        log.d("the system is not stable and return");
                    }
                    return;
                }
                initLexicon(FLAG_CONTACT_CHANGES);
            }
        };

        contentResolver.registerContentObserver(Uri.parse("content://com.android.contacts/contacts/as_vcard"), false,
                mContactChangeObserver);
    }

    private void registerBulletObserver(final Context context, final ContentResolver contentResolver) {
        mBulletChangeObserver = new ContentObserver(new Handler()) {
            @Override
            public void onChange(boolean selfChange) {
                log.infoRelease("the bullet has changed");
                long afterBootTime = System.currentTimeMillis() - launchTime;
                if (launchTime == 0 || (afterBootTime < STABLETIME && afterBootTime > 0)) {
                    log.w("the system is not stable and return");
                    return;
                }
                initLexicon(FLAG_BULLET_CHANGES);
            }
        };

        contentResolver.registerContentObserver(Uri.parse("content://com.bullet.messenger/contacts"), false,
                mBulletChangeObserver);
    }

    /**
     * registerObserver for music change
     */
    private void registerMusicObserver(final Context context, final ContentResolver contentResolver) {
        mMusicChangeObserver = new ContentObserver(new Handler()) {
            @Override
            public void onChange(boolean selfChange) {
                if (DEBUG) {
                    log.i("the music has changed");
                }
                long afterBootTime = System.currentTimeMillis() - launchTime;
                if (launchTime == 0 || (afterBootTime < STABLETIME && afterBootTime > 0)) {
                    if (DEBUG) {
                        log.d("the system is not stable and return");
                    }
                    return;
                }
                initLexicon(FLAG_MUSIC_CHANGES);
            }
        };

        contentResolver.registerContentObserver(MediaUtil.checkAPP(context)
                        ? Uri.parse(VoiceConstant.SMARTISAN_MUSIC_DATA_URI)
                        : MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                false, mMusicChangeObserver);
    }

    private void registerReceiver(Context context) {
        final IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_LOCALE_CHANGED);
        filter.addAction(TelephonyManager.ACTION_PHONE_STATE_CHANGED);
        context.registerReceiver(mReceiver, filter);
    }


    private void registerPackageChangesReceiver(Context context) {
        IntentFilter changesfilter = new IntentFilter(Intent.ACTION_PACKAGE_ADDED);
        changesfilter.addAction(Intent.ACTION_PACKAGE_REMOVED);
        changesfilter.addAction(Intent.ACTION_PACKAGE_CHANGED);
        changesfilter.addAction(ACTION_PACKAGE_DATA_CLEARED);
        changesfilter.addAction(Intent.ACTION_PACKAGE_DATA_CLEARED);
        changesfilter.addDataScheme("package");
        context.registerReceiver(mPackageChangesReceiver, changesfilter);
    }

    public void initLexicon(int type){
        typeFlag =  typeFlag | type;
        mUpdateRunnable.setFlag(typeFlag);
        WorkHandler.removeCallbacks(mUpdateRunnable);
        WorkHandler.postDelayed(mUpdateRunnable, 2000);
    }

    class UpdateRunnable implements Runnable {
        int flag;

        public void setFlag(int flag) {
            this.flag = flag;
        }

        @Override
        public void run() {
            updateData(flag);
        }

    };

    private void updateData(int flag) {
        typeFlag = 0;
        // write the value of key by flag check
        SharedPreferences sharedPreferences = VoiceUtils.getSharePref(VoiceDataService.this);
        final Editor editor = sharedPreferences.edit();
        Context context = getApplicationContext();
        boolean bootFirst = SharePrefUtil.getBoolean(this, "FirstInit", true);
        if ((flag & FLAG_APP_CHANGES) == FLAG_APP_CHANGES) {
            if (ApplicationUtil.buildAppList(context, true, false, false)) {
                log.d("APP NEED BUILD");
            } else {
                log.d("APP NO NEED REBUILD");
            }
        }
        if ((flag & FLAG_LOCAL_CHANGES) == FLAG_LOCAL_CHANGES) {
            if (ApplicationUtil.buildAppList(context, true, false, false
                    )) {
                log.d("APP NEED BUILD");
            } else {
                log.d("APP NO NEED REBUILD");
            }
        }
        if ((flag & FLAG_CONTACT_CHANGES) == FLAG_CONTACT_CHANGES) {
            if (ContactsUtil.buildContactNameList(context, true, false)) {
                log.d("CONTACT NEED BUILD");
            } else {
                log.d("CONTACT NO NEED REBUILD");
            }
        }
        if ((flag & FLAG_MUSIC_CHANGES) == FLAG_MUSIC_CHANGES) {
            if (MediaUtil.buildMediaNameList(context, true, false)) {
                log.d("MUSIC NEED BUILD");
            } else {
                log.d("MUSIC NO NEED REBUILD");
            }
        }
        if (((flag & FLAG_BOOT_COMPLETE) == FLAG_BOOT_COMPLETE)
                || ((flag & FLAG_VOICE_CLEAR_DATA) == FLAG_VOICE_CLEAR_DATA)) {
            log.d("BOOT COMPLETE BUILD firstBoot " + bootFirst);
            if ((flag & FLAG_BOOT_COMPLETE) == FLAG_BOOT_COMPLETE) {
                boolean contactSucess = sharedPreferences.getBoolean(VoiceConstant.KEY_CONTACT,
                        false);
                boolean appSucess = sharedPreferences.getBoolean(VoiceConstant.KEY_APP, false);
                boolean musicSucess = sharedPreferences.getBoolean(VoiceConstant.KEY_MUSIC, false);
                // make sure check database update when boot complete
                if (contactSucess && appSucess && musicSucess) {
                    MediaUtil.onCheckSaraDBUpgrade(context);
                }
                ApplicationUtil.buildAppList(context, appSucess ? false : true, false,
                        sharedPreferences.getBoolean("FirstInit", true));
                ContactsUtil.buildContactNameList(context, contactSucess ? false : true, false);
                MediaUtil.buildMediaNameList(context, musicSucess ? false : true, false);
            } else {
                ApplicationUtil.buildAppList(context, true, false, true);
                ContactsUtil.buildContactNameList(context, true, false);
                MediaUtil.buildMediaNameList(context, true, false);
            }
            if (bootFirst) {
                mEngine.updateLexicon(null);
            }
            editor.putBoolean("FirstInit", false);
        }
        if ((flag & FLAG_IFLYTEK_CLEAR_DATA) == FLAG_IFLYTEK_CLEAR_DATA) {
            log.d("FLYTEK_RESTART BUILD");
            mEngine.updateLexicon(null);
        }

        if ((flag & (FLAG_BULLET_CHANGES | FLAG_BOOT_COMPLETE | FLAG_VOICE_CLEAR_DATA | FLAG_IFLYTEK_CLEAR_DATA)) != 0) {
            mEngine.updateLexicon("<bullet>", ContactsUtil.getBulletNameList(this));
        }
        editor.commit();
    }
}
