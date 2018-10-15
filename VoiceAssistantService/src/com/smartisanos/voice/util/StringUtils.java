package com.smartisanos.voice.util;


import android.content.Context;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.regex.Pattern;

import android.graphics.Paint;
import android.os.Build;
import android.telephony.PhoneNumberUtils;
import android.text.TextUtils;
import com.smartisanos.voice.R;
//import smartisanos.app.contacts.phone.PhoneLocation;
public class StringUtils {
    private static final String TAG = "StringUtils";
    private static final boolean DEBUG = true;
    private static final int TYPE_OPERATOR_TELECOM = 0;
    private static final int TYPE_OPERATOR_UNICOM = 1;
    private static final int TYPE_OPERATOR_MOBILE = 2;
    private static final int LOLLIPOP_VERSION = 21;
    private static HashMap<String, Integer> sMapNumberToOperator;
    static {
        sMapNumberToOperator = new HashMap<String, Integer>();
        // telecom
        sMapNumberToOperator.put("133", TYPE_OPERATOR_TELECOM);
        sMapNumberToOperator.put("142", TYPE_OPERATOR_TELECOM);
        sMapNumberToOperator.put("144", TYPE_OPERATOR_TELECOM);
        sMapNumberToOperator.put("146", TYPE_OPERATOR_TELECOM);
        sMapNumberToOperator.put("148", TYPE_OPERATOR_TELECOM);
        sMapNumberToOperator.put("149", TYPE_OPERATOR_TELECOM);
        sMapNumberToOperator.put("153", TYPE_OPERATOR_TELECOM);
        sMapNumberToOperator.put("180", TYPE_OPERATOR_TELECOM);
        sMapNumberToOperator.put("181", TYPE_OPERATOR_TELECOM);
        sMapNumberToOperator.put("189", TYPE_OPERATOR_TELECOM);
        sMapNumberToOperator.put("177", TYPE_OPERATOR_TELECOM);

        // unicom
        sMapNumberToOperator.put("130", TYPE_OPERATOR_UNICOM);
        sMapNumberToOperator.put("131", TYPE_OPERATOR_UNICOM);
        sMapNumberToOperator.put("132", TYPE_OPERATOR_UNICOM);
        sMapNumberToOperator.put("141", TYPE_OPERATOR_UNICOM);
        sMapNumberToOperator.put("143", TYPE_OPERATOR_UNICOM);
        sMapNumberToOperator.put("145", TYPE_OPERATOR_UNICOM);
        sMapNumberToOperator.put("155", TYPE_OPERATOR_UNICOM);
        sMapNumberToOperator.put("156", TYPE_OPERATOR_UNICOM);
        sMapNumberToOperator.put("185", TYPE_OPERATOR_UNICOM);
        sMapNumberToOperator.put("186", TYPE_OPERATOR_UNICOM);
        sMapNumberToOperator.put("176", TYPE_OPERATOR_UNICOM);

        // mobile
        sMapNumberToOperator.put("134", TYPE_OPERATOR_MOBILE);
        sMapNumberToOperator.put("135", TYPE_OPERATOR_MOBILE);
        sMapNumberToOperator.put("136", TYPE_OPERATOR_MOBILE);
        sMapNumberToOperator.put("137", TYPE_OPERATOR_MOBILE);
        sMapNumberToOperator.put("138", TYPE_OPERATOR_MOBILE);
        sMapNumberToOperator.put("139", TYPE_OPERATOR_MOBILE);
        sMapNumberToOperator.put("140", TYPE_OPERATOR_MOBILE);
        sMapNumberToOperator.put("147", TYPE_OPERATOR_MOBILE);
        sMapNumberToOperator.put("150", TYPE_OPERATOR_MOBILE);
        sMapNumberToOperator.put("151", TYPE_OPERATOR_MOBILE);
        sMapNumberToOperator.put("152", TYPE_OPERATOR_MOBILE);
        sMapNumberToOperator.put("157", TYPE_OPERATOR_MOBILE);
        sMapNumberToOperator.put("158", TYPE_OPERATOR_MOBILE);
        sMapNumberToOperator.put("159", TYPE_OPERATOR_MOBILE);
        sMapNumberToOperator.put("182", TYPE_OPERATOR_MOBILE);
        sMapNumberToOperator.put("183", TYPE_OPERATOR_MOBILE);
        sMapNumberToOperator.put("187", TYPE_OPERATOR_MOBILE);
        sMapNumberToOperator.put("188", TYPE_OPERATOR_MOBILE);
        sMapNumberToOperator.put("178", TYPE_OPERATOR_MOBILE);
    }

    /**
     * get the subtring of s to meet the demand that when we draw the substring
     * in @{textsize}, the text length will not exceed @{length}
     */
    public static final String getSubStringByLengthToDraw(String s, int textsize, int length) {
        Paint paint = new Paint();
        paint.setTextSize(textsize);
        // binary search for the answer
        int low = 0, high = s.length(), mid;
        while (low <= high)
            if (paint.measureText(s, 0, mid = low + high >> 1) > length)
                high = mid - 1;
            else
                low = mid + 1;
        return s.substring(0, low - 1);
    }

    /**
     * TODO, maybe it is not the best place for this method here. get the
     * location and operator by phone number. the phonenumber mustlook like
     * 18600137140,must not be 186 0013 7140.
     */
  /*  public static final String getInfoByPhoneNumber(Context context, String phoneNumber) {
        String result = null;
        String location = PhoneLocation.getCityNameByNumber(context,phoneNumber);
        String operator = getOperatorByPhoneNumber(context, phoneNumber);
        if (location != null && operator != null) {
            location = location.replace(context.getString(R.string.china_telecom), "").replace(context.getString(R.string.china_unicom), "").replace(context.getString(R.string.china_mobile), "");
        }
        if (location == null && operator == null) {
            result ="";
        } else if (location == null) {
            result = operator;
        } else if (operator == null) {
            result = location;
        } else {
            result = location + operator;
        }
        return result;
    }*/


    /**
     * like getInfoByPhoneNumber, it is also the best place for this method
     */
    private static final String getOperatorByPhoneNumber(Context context, String phoneNumber) {
        if (context == null || phoneNumber == null)
            return null;
        if (phoneNumber.length() >= 3 && phoneNumber.startsWith("+86")) {
            phoneNumber = phoneNumber.substring(3);
        }
        if (phoneNumber.length() < 3
                || !sMapNumberToOperator.containsKey(phoneNumber.substring(0, 3)))
            return null;
        int type = sMapNumberToOperator.get(phoneNumber.substring(0, 3));
        switch (type) {
            case TYPE_OPERATOR_TELECOM:
                return context.getString(R.string.china_telecom);
            case TYPE_OPERATOR_UNICOM:
                return context.getString(R.string.china_unicom);
            case TYPE_OPERATOR_MOBILE:
                return context.getString(R.string.china_mobile);
        }
        return null;
    }

    /**
     * read asset file
     *
     * @param context
     * @param fileName
     * @return
     */
    public static byte[] readFileFromAssets(Context context, String fileName) {
        byte[] buffer = null;
        InputStream in = null;
        try {
            in = context.getAssets().open(fileName);
            buffer = new byte[in.available()];
            in.read(buffer);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (in != null) {
                    in.close();
                    in = null;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return buffer;
    }

    public static String trimPunctuation(String str) {
        return Pattern.compile(VoiceConstant.REGEX_PUNCTUATION)
                .matcher(getStringOrEmpty(str)).replaceAll("");
    }
    public static boolean isChinesePunctuation(char c) {
        Character.UnicodeBlock ub = Character.UnicodeBlock.of(c);
        if (ub == Character.UnicodeBlock.GENERAL_PUNCTUATION
                || ub == Character.UnicodeBlock.CJK_SYMBOLS_AND_PUNCTUATION
                || ub == Character.UnicodeBlock.HALFWIDTH_AND_FULLWIDTH_FORMS
                || ub == Character.UnicodeBlock.CJK_COMPATIBILITY_FORMS
                || ub == Character.UnicodeBlock.VERTICAL_FORMS) {
            return true;
        } else {
            return false;
        }
    }

    public static String getStringOrEmpty(CharSequence content) {
        return content != null ? content.toString() : "";
    }

    public static boolean isBlank(String str) {
        return (str == null || str.trim().length() == 0);
    }
}
