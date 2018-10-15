package com.smartisanos.voice.expandscreen;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import smartisanos.t9search.HanziToPinyin;
import smartisanos.t9search.HanziToPinyin.Token;

// For [Rev One]
// Translate hanzi to pin yin.
public class VoiceCommandHanziToPinyin {
    private static final String TAG = "VoiceCommandHanziToPinyin";
    private static Map<String,String> mapPinyin = new HashMap<String,String>();

    public static String getPinYin(String input) {
        ArrayList<Token> tokens = HanziToPinyin.getInstance().get(input);
        StringBuilder sb = new StringBuilder();
        if (tokens != null && tokens.size() > 0) {
            for (Token token : tokens) {
                if (Token.PINYIN == token.type) {
                    sb.append(replaceSpecialString(token.target));
                } else {
                    sb.append(token.source);
                }
            }
        }
        return sb.toString().toLowerCase(Locale.US);
    }

    private static String replaceSpecialString(String str){
        String strReplaced = str;
        if (str.startsWith("ZH")){
            strReplaced = str.replaceFirst("ZH", "Z");
        }
        else if (str.startsWith("CH")){
            strReplaced = str.replaceFirst("CH", "C");
        }
        else if (str.startsWith("SH")){
            strReplaced = str.replaceFirst("SH", "S");
        }
        else if (str.startsWith("N")){
            strReplaced = str.replaceFirst("N", "L");
        }
        str = strReplaced;
        if (str.endsWith("ANG")){
            strReplaced = str.replace("ANG", "AN");
        }
        else if (str.endsWith("ENG")){
            strReplaced = str.replace("ENG", "EN");
        }
        else if (str.endsWith("ING")){
            strReplaced = str.replace("ING", "IN");
        }
        else if (str.endsWith("ONG")){
            strReplaced = str.replace("ONG", "ON");
        }

        return strReplaced;
    }

    public static String getPinYin(String input , boolean useCache) {
        if(!useCache){
            return getPinYin(input);
        }
        if(mapPinyin.containsKey(input)){
            String value = mapPinyin.get(input);
            return value == null ? "" : value;
        }
        String result = getPinYin(input);
        mapPinyin.put(input, result);
        return result;
    }

    public static void clearCache() {
        mapPinyin.clear();
    }
 }