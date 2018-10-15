package com.smartisanos.voice.util;

import android.content.Context;
import android.util.Log;
import java.util.List;
import smartisanos.app.numberassistant.CallerIdDetail;
import smartisanos.app.numberassistant.PhoneNumberAssistantProxy;
import smartisanos.app.numberassistant.YellowPageResult;

public class CootekManager {
    static final LogUtils log = LogUtils.getInstance(CootekManager.class);
    private boolean DEBUG = true;
    private static CootekManager sCootekManager;
    private PhoneNumberAssistantProxy mAssistant;
    private boolean mIsConnected = false;
    private Context mContext;

    public static synchronized CootekManager getInstance(Context context) {
        if (sCootekManager == null) {
            sCootekManager = new CootekManager(context);
        }
        return sCootekManager;
    }

    private CootekManager(Context context) {
        mContext = context;
        mAssistant = new PhoneNumberAssistantProxy();
        mAssistant.bind(mContext, sCallBack);
    }

    public PhoneNumberAssistantProxy.BindCallBack sCallBack = new PhoneNumberAssistantProxy.BindCallBack() {
        @Override
        public void onConnected() {
            log.d("PhoneNumberAssistantProxy connected");
            mIsConnected = true;
        }

        @Override
        public void onDisconnected() {
            mIsConnected = false;
            log.d("PhoneNumberAssistantProxy disconnected");
        }
    };

    public List<YellowPageResult> queryYellowPage(String query, int type) {
        ensureConnected();
        return mAssistant.queryYellowPage(query, type);
    }

    public CallerIdDetail queryCallerIdDetailOffline(String number) {
        ensureConnected();
        return mAssistant.queryCallerIdDetail(number, PhoneNumberAssistantProxy.CALLERID_QUERY_STRATEGY_OFFLINE_ONLY);
    }

    private void ensureConnected() {
        if (!mIsConnected) {
            for (int i = 0; i < 3 && !mIsConnected; i++) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}