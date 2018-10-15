package com.smartisanos.sara.util;

import android.content.Context;
import android.telephony.TelephonyManager;
import smartisanos.util.MultiSimAdapter;
import smartisanos.util.MultiSimUtil;
public class DSUtils {
    private static final String TAG = "VoiceAss.DSUtils";
    public static int SIM_NO = -1;
    public static int SIM_1 = 0;
    public static int SIM_2 = 1;

    public static int getSimState(int simId) {
        return MultiSimAdapter.getSimState(simId);
    }

    public static boolean hasIccCard(int slotId) {
        return MultiSimAdapter.hasIccCard(slotId);
    }

    public static boolean useDSFeature() {
        boolean use = false;
        if (MultiSimAdapter.isMultiSimEnabled()) {
            int sim1State = getSimState(SIM_1);
            int sim2State = getSimState(SIM_2);

            boolean sim1Active = MultiSimAdapter.isSlotActive(SIM_1);
            boolean sim2Active = MultiSimAdapter.isSlotActive(SIM_2);

            boolean sim1Inserted = hasIccCard(SIM_1);
            boolean sim2Inserted = hasIccCard(SIM_2);

            boolean sim1Enable = (sim1State > TelephonyManager.SIM_STATE_ABSENT) && sim1Active && sim1Inserted;
            boolean sim2Enable = (sim2State > TelephonyManager.SIM_STATE_ABSENT) && sim2Active && sim2Inserted;

            use = sim1Enable && sim2Enable;
            LogUtils.e(TAG, "useDSFeature, " + sim1Enable + ", " + sim2Enable);
        }
        return use;
    }
    public static String getSimName(Context context, int simId) {
        if (simId != SIM_1 && simId != SIM_2) {
            return "";
        }
        return MultiSimUtil.getSimName(context, simId);
    }
}
