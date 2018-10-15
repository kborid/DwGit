package com.smartisanos.ideapills;

import android.app.ActivityManager;
import android.app.Application;
import android.app.IActivityObserver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.database.ContentObserver;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.StrictMode;
import android.provider.Settings;

import com.nostra13.universalimageloader.cache.disc.naming.Md5FileNameGenerator;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import com.nostra13.universalimageloader.core.assist.ImageScaleType;
import com.nostra13.universalimageloader.core.assist.QueueProcessingType;
import com.android.internal.sidebar.IIdeaPills;
import com.android.internal.sidebar.ISidebarService;
import com.smartisanos.ideapills.common.util.UIHandler;
import com.smartisanos.ideapills.data.DBHelper;
import com.smartisanos.ideapills.data.DataHandler;
import com.smartisanos.ideapills.entity.BubbleObserver;
import com.smartisanos.ideapills.interfaces.LocalInterface;
import com.smartisanos.ideapills.service.IActions;
import com.smartisanos.ideapills.service.LazyService;
import com.smartisanos.ideapills.util.LOG;
import com.smartisanos.ideapills.util.StatusManager;
import com.smartisanos.ideapills.util.Tracker;
import com.smartisanos.ideapills.util.Utils;

import java.util.Arrays;
import java.util.List;

import smartisanos.api.ApplicationInfoSmt;
import smartisanos.api.SettingsSmt;

public class IdeaPillsApp extends Application {
    private static final LOG log = LOG.getInstance(IdeaPillsApp.class);

    private volatile static IdeaPillsApp myself;
    private BubbleObserverManager mBubbleObserverManager;

    public static IdeaPillsApp getInstance() {
        return myself;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Constants.WINDOW_READY = false;
        myself = this;


        mVoiceInputObs = createAndRegisterObserver(InterfaceDefine.SETTINGS_VOICE_INPUT, TYPE_GLOBAL);
        registerIdeaPills();
        if (!Utils.isIdeaPillsEnable(this)) {
            log.error("IdeaPillsApp onCreate return by isIdeaPillsEnable false");
            return;
        }
        BubbleDisplayManager.INSTANCE.start();
        registerActivityObserver();
        DBHelper.init();
        StatusManager.init();
        setStrictMode();
        LazyService.startService(myself, IActions.ACTION_INIT_CONSTANTS);
        Constants.initGlobalInfo(myself);
        Tracker.init();
        initImageLoader();
        LazyService.startService(myself, IActions.ACTION_INIT_DEVICE_STATS);
        registerObserver();
        DataHandler.handleTask(DataHandler.TASK_DATA_INIT);
        mBubbleObserverManager = new BubbleObserverManager();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    private void setStrictMode() {
        if (!LOG.DBG) {
            return;
        }
        StrictMode.ThreadPolicy threadPolicy = new StrictMode.ThreadPolicy.Builder()
                .detectAll()
                .detectDiskReads()
                .detectDiskWrites()
                .detectNetwork()
                .detectCustomSlowCalls()
                .build();
        StrictMode.setThreadPolicy(threadPolicy);

        StrictMode.VmPolicy vmPolicy = new StrictMode.VmPolicy.Builder()
                .detectLeakedSqlLiteObjects()
                .detectLeakedClosableObjects()
                .penaltyLog()
                .penaltyDeath()
                .build();
        StrictMode.setVmPolicy(vmPolicy);
    }

    private static final String TYPE_GLOBAL = "Global";
    private static final String TYPE_SYSTEM = "System";
    private static final String TYPE_SECURE = "Secure";

    private Uri getSettingsGlobalUri(String key) {
        return Settings.Global.getUriFor(key);
    }

    private Uri getSettingsSystemUri(String key) {
        return Settings.System.getUriFor(key);
    }

    private Uri getSettingsSecureUri(String key) {
        return Settings.Secure.getUriFor(key);
    }


    private ContentObserver createAndRegisterObserver(final String configKey, String type) {
        ContentObserver obs = new ContentObserver(UIHandler.getHandler()) {
            @Override
            public void onChange(boolean selfChange) {
                observerOnChanged(configKey);
            }
        };
        Uri uri = null;
        if (TYPE_GLOBAL.equals(type)) {
            uri = getSettingsGlobalUri(configKey);
        } else if (TYPE_SYSTEM.equals(type)) {
            uri = getSettingsSystemUri(configKey);
        } else if (TYPE_SECURE.equals(type)) {
            uri = getSettingsSecureUri(configKey);
        } else {
            log.error("unknown type for config key [" + configKey + "]");
        }
        if (uri != null) {
            getContentResolver().registerContentObserver(uri, false, obs);
        }
        return obs;
    }

    private static ContentObserver mSetupCompleteObs;
    private static ContentObserver mVoiceInputObs;
    private static ContentObserver mFeaturePhoneModeObs;
    private static ContentObserver mDefaultBubbleColorObs;
    private static ContentObserver mSmartKeyFuncObs;
    private static ContentObserver mStatusBarStateObs;

    private void registerObserver() {
        if (!Utils.isSetupComplete(this)) {
            mSetupCompleteObs = createAndRegisterObserver(InterfaceDefine.SETTINGS_USER_SETUP_COMPLETE, TYPE_SECURE);
        }
        mFeaturePhoneModeObs = createAndRegisterObserver(InterfaceDefine.SETTINGS_FEATURE_PHONE_MODE, TYPE_GLOBAL);
        mDefaultBubbleColorObs = createAndRegisterObserver(InterfaceDefine.SETTINGS_DEFAULT_BUBBLE_COLOR, TYPE_GLOBAL);
        mSmartKeyFuncObs = createAndRegisterObserver(SettingsSmt.Global.SMART_KEY_CLICK_FUNCTION, TYPE_GLOBAL);
        mStatusBarStateObs = createAndRegisterObserver(Constants.STATUS_BAR_EXPAND, TYPE_GLOBAL);
    }

    public void unregisterContentObserver() {
        ContentResolver resolver = getContentResolver();
        if (mSetupCompleteObs != null) {
            resolver.unregisterContentObserver(mSetupCompleteObs);
        }
        if (mVoiceInputObs != null) {
            resolver.unregisterContentObserver(mVoiceInputObs);
        }
        if (mFeaturePhoneModeObs != null) {
            resolver.unregisterContentObserver(mFeaturePhoneModeObs);
        }
        if (mDefaultBubbleColorObs != null) {
            resolver.unregisterContentObserver(mDefaultBubbleColorObs);
        }
        if (mSmartKeyFuncObs != null) {
            resolver.unregisterContentObserver(mSmartKeyFuncObs);
        }
        if (mStatusBarStateObs != null) {
            resolver.unregisterContentObserver(mStatusBarStateObs);
        }
    }

    private void observerOnChanged(String type) {
        if (InterfaceDefine.SETTINGS_USER_SETUP_COMPLETE.equals(type)) {
            getContentResolver().unregisterContentObserver(mSetupCompleteObs);
            mSetupCompleteObs = null;
        } else if (SettingsSmt.Global.SMART_KEY_CLICK_FUNCTION.equals(type)) {
            String currentFunc = Settings.Global.getString(getContentResolver(), SettingsSmt.Global.SMART_KEY_CLICK_FUNCTION);
            if (!SettingsSmt.SHORTCUT_KEY_VALUE.IDEA_PILLS_LIST.equals(currentFunc)) {
                Settings.Global.putInt(getContentResolver(), SettingsSmt.Global.LEFT_SLIDE_LUNCH_GLOBAL_PILLS, 1);
            }
            return;
        }
        BubbleController.getInstance().handleObsChanged(this, type);
    }

    private void registerIdeaPills() {
        ISidebarService sidebarService = ISidebarService.Stub.asInterface(ServiceManager.getService(Context.SIDEBAR_SERVICE));
        if (sidebarService != null) {
            try {
                sidebarService.registerIdeaPills(mBinder);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    private void initImageLoader() {
        DisplayImageOptions.Builder build = new DisplayImageOptions.Builder()
                .cacheInMemory(true).cacheOnDisk(true)
                .bitmapConfig(Bitmap.Config.RGB_565)
                .imageScaleType(ImageScaleType.EXACTLY)
                .showImageOnLoading(com.smartisanos.ideapills.R.drawable.sync_default_avatar)
                .showImageForEmptyUri(com.smartisanos.ideapills.R.drawable.sync_default_avatar);

        DisplayImageOptions options = build.syncLoading(false).build();
        ImageLoaderConfiguration config = new ImageLoaderConfiguration.Builder(
                this).defaultDisplayImageOptions(options)
                .threadPriority(Thread.NORM_PRIORITY - 2)
                .denyCacheImageMultipleSizesInMemory()
                .diskCacheFileNameGenerator(new Md5FileNameGenerator())
                .tasksProcessingOrder(QueueProcessingType.LIFO).build();
        ImageLoader.getInstance().init(config);
    }

    private final IIdeaPills.Stub mBinder = new IIdeaPills.Stub() {
        @Override
        public Bundle callIdeaPills(String method, Bundle data) {
            return LocalInterface.call(myself, method, data);
        }
    };

    private void registerActivityObserver() {
        try {
            ActivityManager am = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
            am.registerActivityObserver(mActivityObserverStub);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static final String RECENT_ACTIVITY = "com.android.systemui/com.android.systemui.recents.RecentsActivity";
    private static final String ALARM_ACTIVITY = "com.smartisanos.ideapills/com.smartisanos.ideapills.remind.RemindAlarmSettingActivity";

    private static final List<String> SHIELD_BUBBLE_WHITE_LIST = Arrays.asList(
            "com.android.phone/com.android.phone.EmergencyDialer",
            "com.smartisanos.sara/com.smartisanos.sara.bubble.BubbleActivity",
            "com.android.gallery3d/com.android.gallery3d.photoeditor.PhotoEditor3",
            "com.android.incallui/com.android.incallui.InCallScreen",
            RECENT_ACTIVITY,
            "com.android.systemui/com.android.systemui.recents.perspective.RecentsPspActivity");

    private static final List<String> HIDE_BUBBLE_WHITE_LIST = Arrays.asList(
            "com.android.mms/com.android.mms.ui.ComposeMessageActivity");

    private static final List<String> HIDE_BUBBLE_IMMEDIATELY_WHITE_LIST = Arrays.asList(
            "com.smartisanos.sara/com.smartisanos.sara.bubble.revone.FlashImActivity",
            "com.smartisanos.sara/com.smartisanos.sara.bubble.revone.GlobalBubbleCreateActivity",
            "com.smartisanos.sara/com.smartisanos.sara.bubble.revone.GlobalSearchActivity");

    private final IActivityObserver.Stub mActivityObserverStub = new IActivityObserver.Stub() {
        @Override
        public void onActivitiesForeground(final String activity, int uid, final int smtAppFlag) {
            UIHandler.post(new Runnable() {
                @Override
                public void run() {
                    if (ALARM_ACTIVITY.equals(activity)) {
                        return;
                    }
                    if (Constants.IS_IDEA_PILLS_ENABLE && Constants.WINDOW_READY) {
                        final int hideBubbleStatus;
                        if (isShieldShowIdeapills(activity, smtAppFlag) && !BubbleController.getInstance().isExtDisplay()) {
                            StatusManager.setStatus(StatusManager.SHIELD_SHOW_LIST, true);
                            hideBubbleStatus = BubbleController.HIDE_BUBBLE_STATUS_NONE;
                        } else {
                            if (BubbleController.getInstance().isAlreadyShow()
                                    && HIDE_BUBBLE_WHITE_LIST.contains(activity)) {
                                hideBubbleStatus = BubbleController.HIDE_BUBBLE_STATUS_NORMAL;
                            } else if (HIDE_BUBBLE_IMMEDIATELY_WHITE_LIST.contains(activity)) {
                                hideBubbleStatus = BubbleController.HIDE_BUBBLE_STATUS_IMMEDIATELY;
                            } else {
                                hideBubbleStatus = BubbleController.HIDE_BUBBLE_STATUS_NONE;
                            }
                            StatusManager.setStatus(StatusManager.SHIELD_SHOW_LIST, false);
                        }
                        BubbleController.getInstance().updateVisibility(hideBubbleStatus);
                    }
                }
            });
        }
    };

    private static boolean isShieldShowIdeapills(final String activity, final int smtAppFlag) {
        boolean isGame = (smtAppFlag & ApplicationInfo.FLAG_CATEGORY_GAME) != 0;
        return isGame || SHIELD_BUBBLE_WHITE_LIST.contains(activity);
    }

    public static boolean isShieldShowIdeapillsByTopAppInfo(Context context) {
        ComponentName componentName = Utils.getTopActivityName();
        String topActivity = null;
        if (componentName != null) {
            topActivity = componentName.getPackageName() + "/" + componentName.getClassName();
        }

        if (SHIELD_BUBBLE_WHITE_LIST.contains(topActivity)) {
            return true;
        }
        if (componentName != null) {
            try {
                String pkg = componentName.getPackageName();
                PackageManager pm = context.getPackageManager();
                ApplicationInfo appInfo = pm.getApplicationInfo(pkg, 0);
                if (appInfo != null && ApplicationInfoSmt.getInstance().isGameApp(appInfo)) {
                    //is game
                    return true;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    public BubbleObserverManager getBubbleObserverManager() {
        return mBubbleObserverManager;
    }
}