package com.smartisanos.sara.util;

import android.content.Context;
import android.util.Log;

import com.amap.api.location.AMapLocation;
import com.amap.api.location.AMapLocationClient;
import com.amap.api.location.AMapLocationClientOption;
import com.amap.api.location.AMapLocationListener;

public class MapUtils {

    public static final String TAG = "MapUtils";

    public interface OnLocationResult {
        void onLocationResult(AMapLocation aMapLocation);
    }

    public static final int LOCATION_MODE_HIGHT_ACCURACY = 0;
    public static final int LOCATION_MODE_BATTERY_SAVING = 1;

    public static void getLocation(Context context, int mode, final OnLocationResult listener) {
        AMapLocationClientOption mLocationOption = new AMapLocationClientOption();
        if (LOCATION_MODE_HIGHT_ACCURACY == mode) {
            mLocationOption.setLocationMode(AMapLocationClientOption.AMapLocationMode.Hight_Accuracy);
            mLocationOption.setOnceLocationLatest(true);
        } else if (LOCATION_MODE_BATTERY_SAVING == mode) {
            mLocationOption.setLocationMode(AMapLocationClientOption.AMapLocationMode.Battery_Saving);
            mLocationOption.setOnceLocation(true);
        }

        final AMapLocationClient mLocationClient = new AMapLocationClient(context.getApplicationContext());
        mLocationClient.setLocationOption(mLocationOption);
        mLocationClient.setLocationListener(new AMapLocationListener() {
            @Override
            public void onLocationChanged(AMapLocation aMapLocation) {
                if (aMapLocation != null && aMapLocation.getErrorCode() != 0) {
                    Log.e(TAG, "MapUtils location Error, ErrCode:"
                            + aMapLocation.getErrorCode() + ", errInfo:"
                            + aMapLocation.getErrorInfo());
                }

                if (listener != null) {
                    listener.onLocationResult(aMapLocation);
                }
                mLocationClient.onDestroy();
            }
        });

        mLocationClient.startLocation();
    }
}

