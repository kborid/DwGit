package com.smartisanos.sara.bullet.util;

import android.text.TextUtils;

import net.sourceforge.pinyin4j.PinyinHelper;
import net.sourceforge.pinyin4j.format.HanyuPinyinCaseType;
import net.sourceforge.pinyin4j.format.HanyuPinyinOutputFormat;
import net.sourceforge.pinyin4j.format.HanyuPinyinToneType;
import net.sourceforge.pinyin4j.format.HanyuPinyinVCharType;
import net.sourceforge.pinyin4j.format.exception.BadHanyuPinyinOutputFormatCombination;

import java.util.ArrayList;

public class PinYinUtils {

    private static HanyuPinyinOutputFormat format;
    static {
        format = new HanyuPinyinOutputFormat();
        format.setCaseType(HanyuPinyinCaseType.LOWERCASE);
        format.setToneType(HanyuPinyinToneType.WITHOUT_TONE);
        format.setVCharType(HanyuPinyinVCharType.WITH_V);
    }

    public static String getPinYin(char c) {
        String pinyin = String.valueOf(c);
        if (Character.isWhitespace(c)) {
            return pinyin;
        } else if (c >= -127 && c < 128) {
            return pinyin;
        } else {
            try {
                pinyin = PinyinHelper.toHanyuPinyinStringArray(c, format)[0];
            } catch (Exception e) {
                pinyin = String.valueOf(c);
            }
            return pinyin;
        }
    }

    public static String getPinYin(String str) {
        if (TextUtils.isEmpty(str)) {
            return null;
        }
        StringBuilder pinyin = new StringBuilder();
        for (char c : str.toCharArray()) {
            pinyin.append(getPinYin(c));
        }
        return pinyin.toString();
    }

    public static PinYinStructInfo getPinYinAndInitialSet(String str) {
        if (TextUtils.isEmpty(str)) {
            return null;
        }
        StringBuilder pinyin = new StringBuilder();
        StringBuilder initialSet = new StringBuilder();
        ArrayList<String> pinyinList = new ArrayList<>();
        for (char c : str.toCharArray()) {
            String cPinyin = getPinYin(c).toLowerCase();
            pinyin.append(cPinyin);
            if (cPinyin.length() > 0) {
                initialSet.append(cPinyin.charAt(0));
            }
            pinyinList.add(cPinyin);
        }
        return new PinYinStructInfo(pinyin.toString(), initialSet.toString(), pinyinList);
    }

    public static String getFirstChar(String str) {
        return String.valueOf(getPinYin(str.charAt(0)).charAt(0));
    }

    public static String getLeadingLower(char c) {
        return String.valueOf(getPinYin(c).charAt(0)).toLowerCase();
    }

    public static String getLeadingUpper(char c) {
        return String.valueOf(getPinYin(c).charAt(0)).toUpperCase();
    }

    public static class PinYinStructInfo {
        String pinyin;
        String initialSet;
        ArrayList<String> pinYinList;

        public PinYinStructInfo(String pinyin, String initialSet, ArrayList<String> pinYinList) {
            this.pinyin = pinyin;
            this.initialSet = initialSet;
            this.pinYinList = pinYinList;
        }

        public String getPinyin() {
            return pinyin;
        }

        public String getInitialSet() {
            return initialSet;
        }

        public ArrayList<String> getPinYinList() {
            return pinYinList;
        }
    }
}
