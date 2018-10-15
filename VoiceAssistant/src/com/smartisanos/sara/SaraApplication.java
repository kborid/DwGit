package com.smartisanos.sara;

import android.app.Application;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;

import com.nostra13.universalimageloader.cache.disc.naming.Md5FileNameGenerator;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import com.nostra13.universalimageloader.core.assist.ImageScaleType;
import com.nostra13.universalimageloader.core.assist.QueueProcessingType;
import com.nostra13.universalimageloader.core.process.BitmapProcessor;
import com.smartisanos.sara.bubble.revone.ExtScreenConstant;
import com.smartisanos.sara.bubble.revone.RevActivityManager;
import com.smartisanos.sanbox.utils.SaraTracker;
import com.smartisanos.sara.bubble.BubbleActivityHelper;
import com.smartisanos.sara.bubble.manager.BubbleCleaner;
import com.smartisanos.sara.service.LazyService;
import com.smartisanos.sara.util.AppImageLoader;
import com.smartisanos.sara.util.HolographicOutlineHelper;
import com.smartisanos.sara.util.SaraConstant;
import com.smartisanos.sara.util.SaraUtils;
import com.smartisanos.sara.voicecommand.VoiceCommandEnvironment;

public class SaraApplication extends Application {
    private static SaraApplication mSelf;

    private AppImageLoader mAppImageLoader;
    private final static int MSG_RESET_MAUNAL_TODO = 11;

    @Override
    public void onCreate() {
        super.onCreate();
        if (!SaraUtils.isMainProcess(this))
            return;

        mSelf = this;
        RevActivityManager.INSTANCE.start();
        BubbleActivityHelper.INSTANCE.getRootViewTag().initAsync();
        LazyService.initRes(mSelf);
        SaraTracker.init(this);
        SaraTracker.trackAllIfNeeded(this);
        initImageLoader();
        mAppImageLoader = new AppImageLoader(this);
        HolographicOutlineHelper.init(this);
        if (!SaraUtils.isManualBackupTodo(mSelf)) {
            SaraUtils.setManualBackupTodo(mSelf);
        }
        BubbleCleaner.INSTANCE.autoClear();
        Intent intent = new Intent("android.accessibilityservice.AccessibilityService");
        intent.setPackage(getPackageName());
        startService(intent);
        SaraConstant.init(this);
        ExtScreenConstant.init(this);
        VoiceCommandEnvironment.init(this);
    }

    public static SaraApplication getInstance() {
        return mSelf;
    }

    @Override
    public void onTerminate() {
        super.onTerminate();
    }

    private void initImageLoader() {
        final Resources resources = getResources();
        Drawable mDefalutDrawable = resources.getDrawable(
                R.drawable.default_music_pic);
        DisplayImageOptions.Builder build = new DisplayImageOptions.Builder()
                .cacheInMemory(true).cacheOnDisk(true)
                .bitmapConfig(Bitmap.Config.RGB_565)
                .imageScaleType(ImageScaleType.EXACTLY)
                .showImageOnLoading(mDefalutDrawable).preProcessor(new BitmapProcessor() {
                    @Override
                    public Bitmap process(Bitmap arg0) {
                        return SaraUtils.scaleCenterCrop(arg0,
                                resources.getInteger(R.integer.config_MusicAlbumPicWith),
                                resources.getInteger(R.integer.config_MusicAlbumPicHeight));
                    }
                });

        ImageLoaderConfiguration config = new ImageLoaderConfiguration.Builder(
                this).defaultDisplayImageOptions(build.syncLoading(false).build())
                .threadPriority(Thread.NORM_PRIORITY - 2)
                .denyCacheImageMultipleSizesInMemory()
                .diskCacheFileNameGenerator(new Md5FileNameGenerator())
                .tasksProcessingOrder(QueueProcessingType.LIFO).build();
        ImageLoader.getInstance().init(config);
    }

    public AppImageLoader getAppImageLoader() {
        return mAppImageLoader;
    }
}
