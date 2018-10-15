package com.smartisanos.voice;
import com.smartisanos.voice.engine.VoiceRecognitionEngineBase;
import com.smartisanos.voice.engine.XunfeiRecognizerEngine;
import com.smartisanos.voice.service.VoiceAssistantService;
import com.smartisanos.voice.service.VoiceDataService;
import com.smartisanos.voice.util.CootekManager;
import com.smartisanos.voice.util.VoiceConstant;
import com.smartisanos.voice.util.WorkHandler;
import android.content.Context;
import android.location.Country;
import android.location.CountryDetector;
import android.location.CountryListener;
import android.app.Application;
import android.content.Intent;
import smartisanos.util.config.Features;
import smartisanos.t9search.HanziToPinyin;
import android.os.SystemProperties;

public class VoiceApplication extends Application {
    private static final boolean VOICE_ENABLE = SystemProperties.getInt("ro.voice_assistant_enable", 0) == 1;
    private CountryDetector mCountryDetector;
    private CountryListener mCountryListener;
    private String mCountryIso;
    private static VoiceApplication mSelf;
    public VoiceRecognitionEngineBase mEngine = null;

    @Override
    public void onCreate() {
        super.onCreate();
        mSelf = this;
        if (!VoiceConstant.CTA_ENABLE) {
            mEngine = XunfeiRecognizerEngine.getInstance();
            mEngine.initEngine(this);
        }
        if (VOICE_ENABLE && !Features.isFeatureUSEnabled(this)) {
            Intent intentVoice = new Intent();
            startService(new Intent(this, VoiceDataService.class));
            startService(new Intent(this, VoiceAssistantService.class));
        }
        mCountryDetector = (CountryDetector) getSystemService(Context.COUNTRY_DETECTOR);
        mCountryListener = new CountryListener() {
            @Override
            public synchronized void onCountryDetected(Country country) {
                mCountryIso = country.getCountryIso();
            }
        };
        mCountryDetector.addCountryListener(mCountryListener, getMainLooper());
        WorkHandler.post(new Runnable() {
            @Override
            public void run() {
                HanziToPinyin.getInstance();
                CootekManager.getInstance(mSelf);
            }
        });
    }

    public String getCountryIso() {
        if (mCountryIso == null) {
            Country country = mCountryDetector.detectCountry();
            if (country != null) {
                mCountryIso = country.getCountryIso();
            }
        }

        return mCountryIso;
    }

    public static VoiceApplication getInstance() {
        return mSelf;
    }
}
