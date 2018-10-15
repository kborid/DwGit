package com.smartisanos.ideapills;

import android.content.Context;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;

public class PhoneState {

    private Context mContext;

    public PhoneState(Context context) {
        mContext = context;
    }

    private PhoneStateListener mListener = new PhoneStateListener() {
        @Override
        public void onCallStateChanged(int state, String incomingNumber) {
            switch (state) {
                case TelephonyManager.CALL_STATE_RINGING:
                    // incoming call
                    BubbleController.getInstance().onPhoneBusy();
                    break;
                case TelephonyManager.CALL_STATE_IDLE:
                    break;
                case TelephonyManager.CALL_STATE_OFFHOOK:
                    // use Bt headset
                    BubbleController.getInstance().onPhoneBusy();
                    break;
            }
        }
    };

    private enum PhoneRegisterState {
        NOT_YET,
        REGISTERED
    }

    private PhoneRegisterState mPhoneRegisterState = PhoneRegisterState.NOT_YET;

    public void registerTelephonyState() {
        if(mPhoneRegisterState == PhoneRegisterState.REGISTERED){
            return ;
        }
        TelephonyManager telephony = (TelephonyManager) mContext.getSystemService(Context.TELEPHONY_SERVICE);
        if(telephony != null){
            telephony.listen(mListener, PhoneStateListener.LISTEN_CALL_STATE);
            mPhoneRegisterState = PhoneRegisterState.REGISTERED;
        }else{
            mPhoneRegisterState = PhoneRegisterState.NOT_YET;
        }
    }

    public void unRegisterTelephonyState() {
        if(mPhoneRegisterState == PhoneRegisterState.REGISTERED){
            TelephonyManager telephony = (TelephonyManager) mContext.getSystemService(Context.TELEPHONY_SERVICE);
            if(telephony != null){
                telephony.listen(mListener, PhoneStateListener.LISTEN_NONE);
            }
            mPhoneRegisterState = PhoneRegisterState.NOT_YET;
        }
    }
}
