package com.smartisanos.sara.voicecommand;

import android.text.TextUtils;

import com.amap.api.services.core.LatLonPoint;

public class AddressInfo {
    public String name;
    public String addr;
    public LatLonPoint point;

    public AddressInfo() {}

    public AddressInfo(String name, String address, LatLonPoint point) {
        this.name = name;
        this.addr = address;
        this.point = point;
    }

    public void reset() {
        name = "";
        addr = "";
        point = null;
    }

    public boolean isEmpty() {
        return TextUtils.isEmpty(name) && TextUtils.isEmpty(addr) && point == null;
    }

    public boolean isAccuracy() {
        return point != null;
    }

    @Override
    public String toString() {
        return toRegularString().replaceAll("\0", ",");
    }

    public String toRegularString() {
        return TextUtils.join("\0", new String[] {
                getStringOrEmptyString(name),
                getStringOrEmptyString(addr),
                getStringOrEmptyString(point)});
    }

    private static String getStringOrEmptyString(Object o) {
        return o != null ? o.toString() : "";
    }

    public static AddressInfo fromString(String s) {
        String[] fileds = s.split("\0");
        AddressInfo address = new AddressInfo();

        if (fileds.length > 0) {
            address.name = fileds[0];
        }

        if (fileds.length > 1) {
            address.addr = fileds[1];
        }

        if (fileds.length > 2) {
            String[] pointString = fileds[2].split(",");
            LatLonPoint point = null;
            try {
                point = new LatLonPoint(
                        Double.parseDouble(pointString[0]),
                        Double.parseDouble(pointString[1]));
            } catch (NumberFormatException e) {
                e.printStackTrace();
            }
            address.point = point;
        }

        return address;
    }
}
