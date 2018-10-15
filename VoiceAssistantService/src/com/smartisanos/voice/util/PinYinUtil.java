
package com.smartisanos.voice.util;


import java.text.Collator;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import android.text.TextUtils;

import smartisanos.t9search.HanziToPinyin;
import smartisanos.t9search.HanziToPinyin.Token;

public class PinYinUtil {
    private static final String FIRST_PINYIN_UNIHAN = "\u963F";
    private static final String LAST_PINYIN_UNIHAN = "\u9FFF";
    private static final int MAX_CHAR = 256;
    private static Map<String,String> mapPinyin = new HashMap<String,String>();

    public static String getPinYin(String input) {
        ArrayList<Token> tokens = HanziToPinyin.getInstance().get(input);
        StringBuilder sb = new StringBuilder();
        if (tokens != null && tokens.size() > 0) {
            for (Token token : tokens) {
                if (Token.PINYIN == token.type) {
                    sb.append(token.target);
                } else {
                    sb.append(token.source);
                }
            }
        }
        return sb.toString().toLowerCase(Locale.US).replaceAll("\\s+", "");
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

    public static String stringToPinyin(String inputStr) {
        int len = inputStr.length();
        StringBuilder sb = new StringBuilder();
        Collator collator = Collator.getInstance(Locale.CHINA);

        for (int i = 0; i < len; i++) {
            int offset = -1;
            final char ch = inputStr.charAt(i);

            // For normal character, do not convert it.
            if (ch < MAX_CHAR) {
                sb.append(ch);
                continue;
            } else {
                String letter = Character.toString(ch);
                // Compare with first Chinese Unicode value
                int cmp = collator.compare(letter, FIRST_PINYIN_UNIHAN);
                if (cmp < 0) {
                    sb.append(ch);
                    continue;
                } else if (cmp == 0) {
                    offset = 0;
                } else {
                    cmp = collator.compare(letter, LAST_PINYIN_UNIHAN);
                    if (cmp > 0) {
                        sb.append(ch);
                        continue;
                    } else if (cmp == 0) {
                        offset = UNIHANS.length - 1;
                    }
                }

                // If offset is less than zero, string must be located between
                // FIRST and LAST PINYIN_UNIHAN. Find location using binary search.
                if (offset < 0) {
                    int begin = 0;
                    int end = UNIHANS.length - 1;
                    while (begin <= end) {
                        offset = (begin + end) / 2;
                        final String unihan = Character.toString(UNIHANS[offset]);
                        cmp = collator.compare(letter, unihan);
                        if (cmp == 0) {
                            break;
                        } else if (cmp > 0) {
                            begin = offset + 1;
                        } else {
                            end = offset - 1;
                        }
                    }
                }

                // If cmp is less than zero, previous uicode will match with letter's
                // Unicode
                if (cmp < 0) {
                    offset--;
                }

                // Get all characters which match with Chinese's pinyin.
                for (int j = 0; j < PINYINS[offset].length&& PINYINS[offset][j] != 0; j++) {
                    sb.append((char) PINYINS[offset][j]);
                }
            }
        }

        return sb.toString().toLowerCase(Locale.US);
    }

    public static String filterString(String inputStr) {
        int len = inputStr.length();
        StringBuilder sb = new StringBuilder();
        Collator collator = Collator.getInstance(Locale.CHINA);

        for (int i = 0; i < len; i++) {
            final char ch = inputStr.charAt(i);
            // For normal character, do not convert it.
            if (ch < MAX_CHAR) {
                sb.append(ch);
                continue;
            } else {
                String letter = Character.toString(ch);
                // Compare with first Chinese Unicode value
                int cmp = collator.compare(letter, FIRST_PINYIN_UNIHAN);
                if (cmp < 0) {
                    sb.append('0');
                    continue;
                } else if (cmp == 0) {
                } else {
                    cmp = collator.compare(letter, LAST_PINYIN_UNIHAN);
                    if (cmp > 0) {
                        sb.append('0');
                        continue;
                    } else if (cmp == 0) {
                    }
                }
                sb.append(ch);
            }
        }
        return sb.toString().toLowerCase(Locale.US);
    }

    private static final char[] UNIHANS = {
        '\u963f', '\u54ce', '\u5b89', '\u80ae', '\u51f9', '\u516b',
        '\u6300', '\u6273', '\u90a6', '\u52f9', '\u9642', '\u5954',
        '\u4f3b', '\u5c44', '\u8fb9', '\u706c', '\u618b', '\u6c43',
        '\u51ab', '\u7676', '\u5cec', '\u5693', '\u5072', '\u53c2',
        '\u4ed3', '\u64a1', '\u518a', '\u5d7e', '\u66fd', '\u66fe',
        '\u5c64', '\u53c9', '\u8286', '\u8fbf', '\u4f25', '\u6284',
        '\u8f66', '\u62bb', '\u6c88', '\u6c89', '\u9637', '\u5403',
        '\u5145', '\u62bd', '\u51fa', '\u6b3b', '\u63e3', '\u5ddb',
        '\u5205', '\u5439', '\u65fe', '\u9034', '\u5472', '\u5306',
        '\u51d1', '\u7c97', '\u6c46', '\u5d14', '\u90a8', '\u6413',
        '\u5491', '\u5446', '\u4e39', '\u5f53', '\u5200', '\u561a',
        '\u6265', '\u706f', '\u6c10', '\u55f2', '\u7538', '\u5201',
        '\u7239', '\u4e01', '\u4e1f', '\u4e1c', '\u543a', '\u53be',
        '\u8011', '\u8968', '\u5428', '\u591a', '\u59b8', '\u8bf6',
        '\u5940', '\u97a5', '\u513f', '\u53d1', '\u5e06', '\u531a',
        '\u98de', '\u5206', '\u4e30', '\u8985', '\u4ecf', '\u7d11',
        '\u4f15', '\u65ee', '\u4f85', '\u7518', '\u5188', '\u768b',
        '\u6208', '\u7ed9', '\u6839', '\u522f', '\u5de5', '\u52fe',
        '\u4f30', '\u74dc', '\u4e56', '\u5173', '\u5149', '\u5f52',
        '\u4e28', '\u5459', '\u54c8', '\u548d', '\u4f44', '\u592f',
        '\u8320', '\u8bc3', '\u9ed2', '\u62eb', '\u4ea8', '\u5677',
        '\u53ff', '\u9f41', '\u4e6f', '\u82b1', '\u6000', '\u72bf',
        '\u5ddf', '\u7070', '\u660f', '\u5419', '\u4e0c', '\u52a0',
        '\u620b', '\u6c5f', '\u827d', '\u9636', '\u5dfe', '\u5755',
        '\u5182', '\u4e29', '\u51e5', '\u59e2', '\u5658', '\u519b',
        '\u5494', '\u5f00', '\u520a', '\u5ffc', '\u5c3b', '\u533c',
        '\u808e', '\u52a5', '\u7a7a', '\u62a0', '\u625d', '\u5938',
        '\u84af', '\u5bbd', '\u5321', '\u4e8f', '\u5764', '\u6269',
        '\u5783', '\u6765', '\u5170', '\u5577', '\u635e', '\u808b',
        '\u52d2', '\u5d1a', '\u5215', '\u4fe9', '\u5941', '\u826f',
        '\u64a9', '\u5217', '\u62ce', '\u5222', '\u6e9c', '\u56d6',
        '\u9f99', '\u779c', '\u565c', '\u5a08', '\u7567', '\u62a1',
        '\u7f57', '\u5463', '\u5988', '\u57cb', '\u5ada', '\u7264',
        '\u732b', '\u4e48', '\u5445', '\u95e8', '\u753f', '\u54aa',
        '\u5b80', '\u55b5', '\u4e5c', '\u6c11', '\u540d', '\u8c2c',
        '\u6478', '\u54de', '\u6bea', '\u55ef', '\u62cf', '\u8149',
        '\u56e1', '\u56d4', '\u5b6c', '\u7592', '\u5a1e', '\u6041',
        '\u80fd', '\u59ae', '\u62c8', '\u5b22', '\u9e1f', '\u634f',
        '\u56dc', '\u5b81', '\u599e', '\u519c', '\u7fba', '\u5974',
        '\u597b', '\u759f', '\u9ec1', '\u90cd', '\u5594', '\u8bb4',
        '\u5991', '\u62cd', '\u7705', '\u4e53', '\u629b', '\u5478',
        '\u55b7', '\u5309', '\u4e15', '\u56e8', '\u527d', '\u6c15',
        '\u59d8', '\u4e52', '\u948b', '\u5256', '\u4ec6', '\u4e03',
        '\u6390', '\u5343', '\u545b', '\u6084', '\u767f', '\u4eb2',
        '\u72c5', '\u828e', '\u4e18', '\u533a', '\u5cd1', '\u7f3a',
        '\u590b', '\u5465', '\u7a63', '\u5a06', '\u60f9', '\u4eba',
        '\u6254', '\u65e5', '\u8338', '\u53b9', '\u909a', '\u633c',
        '\u5827', '\u5a51', '\u77a4', '\u637c', '\u4ee8', '\u6be2',
        '\u4e09', '\u6852', '\u63bb', '\u95aa', '\u68ee', '\u50e7',
        '\u6740', '\u7b5b', '\u5c71', '\u4f24', '\u5f30', '\u5962',
        '\u7533', '\u8398', '\u6552', '\u5347', '\u5c38', '\u53ce',
        '\u4e66', '\u5237', '\u8870', '\u95e9', '\u53cc', '\u8c01',
        '\u542e', '\u8bf4', '\u53b6', '\u5fea', '\u635c', '\u82cf',
        '\u72fb', '\u590a', '\u5b59', '\u5506', '\u4ed6', '\u56fc',
        '\u574d', '\u6c64', '\u5932', '\u5fd1', '\u71a5', '\u5254',
        '\u5929', '\u65eb', '\u5e16', '\u5385', '\u56f2', '\u5077',
        '\u51f8', '\u6e4d', '\u63a8', '\u541e', '\u4e47', '\u7a75',
        '\u6b6a', '\u5f2f', '\u5c23', '\u5371', '\u6637', '\u7fc1',
        '\u631d', '\u4e4c', '\u5915', '\u8672', '\u4eda', '\u4e61',
        '\u7071', '\u4e9b', '\u5fc3', '\u661f', '\u51f6', '\u4f11',
        '\u5401', '\u5405', '\u524a', '\u5743', '\u4e2b', '\u6079',
        '\u592e', '\u5e7a', '\u503b', '\u4e00', '\u56d9', '\u5e94',
        '\u54df', '\u4f63', '\u4f18', '\u625c', '\u56e6', '\u66f0',
        '\u6655', '\u7b60', '\u7b7c', '\u5e00', '\u707d', '\u5142',
        '\u5328', '\u50ae', '\u5219', '\u8d3c', '\u600e', '\u5897',
        '\u624e', '\u635a', '\u6cbe', '\u5f20', '\u957f', '\u9577',
        '\u4f4b', '\u8707', '\u8d1e', '\u4e89', '\u4e4b', '\u5cd9',
        '\u5ea2', '\u4e2d', '\u5dde', '\u6731', '\u6293', '\u62fd',
        '\u4e13', '\u5986', '\u96b9', '\u5b92', '\u5353', '\u4e72',
        '\u5b97', '\u90b9', '\u79df', '\u94bb', '\u539c', '\u5c0a',
        '\u6628', '\u5159', '\u9fc3', '\u9fc4', };

    private static final byte[][] PINYINS = {
        { 65,   0,   0,   0,   0,   0}, { 65,  73,   0,   0,   0,   0},
        { 65,  78,   0,   0,   0,   0}, { 65,  78,  71,   0,   0,   0},
        { 65,  79,   0,   0,   0,   0}, { 66,  65,   0,   0,   0,   0},
        { 66,  65,  73,   0,   0,   0}, { 66,  65,  78,   0,   0,   0},
        { 66,  65,  78,  71,   0,   0}, { 66,  65,  79,   0,   0,   0},
        { 66,  69,  73,   0,   0,   0}, { 66,  69,  78,   0,   0,   0},
        { 66,  69,  78,  71,   0,   0}, { 66,  73,   0,   0,   0,   0},
        { 66,  73,  65,  78,   0,   0}, { 66,  73,  65,  79,   0,   0},
        { 66,  73,  69,   0,   0,   0}, { 66,  73,  78,   0,   0,   0},
        { 66,  73,  78,  71,   0,   0}, { 66,  79,   0,   0,   0,   0},
        { 66,  85,   0,   0,   0,   0}, { 67,  65,   0,   0,   0,   0},
        { 67,  65,  73,   0,   0,   0}, { 67,  65,  78,   0,   0,   0},
        { 67,  65,  78,  71,   0,   0}, { 67,  65,  79,   0,   0,   0},
        { 67,  69,   0,   0,   0,   0}, { 67,  69,  78,   0,   0,   0},
        { 67,  69,  78,  71,   0,   0}, { 90,  69,  78,  71,   0,   0},
        { 67,  69,  78,  71,   0,   0}, { 67,  72,  65,   0,   0,   0},
        { 67,  72,  65,  73,   0,   0}, { 67,  72,  65,  78,   0,   0},
        { 67,  72,  65,  78,  71,   0}, { 67,  72,  65,  79,   0,   0},
        { 67,  72,  69,   0,   0,   0}, { 67,  72,  69,  78,   0,   0},
        { 83,  72,  69,  78,   0,   0}, { 67,  72,  69,  78,   0,   0},
        { 67,  72,  69,  78,  71,   0}, { 67,  72,  73,   0,   0,   0},
        { 67,  72,  79,  78,  71,   0}, { 67,  72,  79,  85,   0,   0},
        { 67,  72,  85,   0,   0,   0}, { 67,  72,  85,  65,   0,   0},
        { 67,  72,  85,  65,  73,   0}, { 67,  72,  85,  65,  78,   0},
        { 67,  72,  85,  65,  78,  71}, { 67,  72,  85,  73,   0,   0},
        { 67,  72,  85,  78,   0,   0}, { 67,  72,  85,  79,   0,   0},
        { 67,  73,   0,   0,   0,   0}, { 67,  79,  78,  71,   0,   0},
        { 67,  79,  85,   0,   0,   0}, { 67,  85,   0,   0,   0,   0},
        { 67,  85,  65,  78,   0,   0}, { 67,  85,  73,   0,   0,   0},
        { 67,  85,  78,   0,   0,   0}, { 67,  85,  79,   0,   0,   0},
        { 68,  65,   0,   0,   0,   0}, { 68,  65,  73,   0,   0,   0},
        { 68,  65,  78,   0,   0,   0}, { 68,  65,  78,  71,   0,   0},
        { 68,  65,  79,   0,   0,   0}, { 68,  69,   0,   0,   0,   0},
        { 68,  69,  78,   0,   0,   0}, { 68,  69,  78,  71,   0,   0},
        { 68,  73,   0,   0,   0,   0}, { 68,  73,  65,   0,   0,   0},
        { 68,  73,  65,  78,   0,   0}, { 68,  73,  65,  79,   0,   0},
        { 68,  73,  69,   0,   0,   0}, { 68,  73,  78,  71,   0,   0},
        { 68,  73,  85,   0,   0,   0}, { 68,  79,  78,  71,   0,   0},
        { 68,  79,  85,   0,   0,   0}, { 68,  85,   0,   0,   0,   0},
        { 68,  85,  65,  78,   0,   0}, { 68,  85,  73,   0,   0,   0},
        { 68,  85,  78,   0,   0,   0}, { 68,  85,  79,   0,   0,   0},
        { 69,   0,   0,   0,   0,   0}, { 69,  73,   0,   0,   0,   0},
        { 69,  78,   0,   0,   0,   0}, { 69,  78,  71,   0,   0,   0},
        { 69,  82,   0,   0,   0,   0}, { 70,  65,   0,   0,   0,   0},
        { 70,  65,  78,   0,   0,   0}, { 70,  65,  78,  71,   0,   0},
        { 70,  69,  73,   0,   0,   0}, { 70,  69,  78,   0,   0,   0},
        { 70,  69,  78,  71,   0,   0}, { 70,  73,  65,  79,   0,   0},
        { 70,  79,   0,   0,   0,   0}, { 70,  79,  85,   0,   0,   0},
        { 70,  85,   0,   0,   0,   0}, { 71,  65,   0,   0,   0,   0},
        { 71,  65,  73,   0,   0,   0}, { 71,  65,  78,   0,   0,   0},
        { 71,  65,  78,  71,   0,   0}, { 71,  65,  79,   0,   0,   0},
        { 71,  69,   0,   0,   0,   0}, { 71,  69,  73,   0,   0,   0},
        { 71,  69,  78,   0,   0,   0}, { 71,  69,  78,  71,   0,   0},
        { 71,  79,  78,  71,   0,   0}, { 71,  79,  85,   0,   0,   0},
        { 71,  85,   0,   0,   0,   0}, { 71,  85,  65,   0,   0,   0},
        { 71,  85,  65,  73,   0,   0}, { 71,  85,  65,  78,   0,   0},
        { 71,  85,  65,  78,  71,   0}, { 71,  85,  73,   0,   0,   0},
        { 71,  85,  78,   0,   0,   0}, { 71,  85,  79,   0,   0,   0},
        { 72,  65,   0,   0,   0,   0}, { 72,  65,  73,   0,   0,   0},
        { 72,  65,  78,   0,   0,   0}, { 72,  65,  78,  71,   0,   0},
        { 72,  65,  79,   0,   0,   0}, { 72,  69,   0,   0,   0,   0},
        { 72,  69,  73,   0,   0,   0}, { 72,  69,  78,   0,   0,   0},
        { 72,  69,  78,  71,   0,   0}, { 72,  77,   0,   0,   0,   0},
        { 72,  79,  78,  71,   0,   0}, { 72,  79,  85,   0,   0,   0},
        { 72,  85,   0,   0,   0,   0}, { 72,  85,  65,   0,   0,   0},
        { 72,  85,  65,  73,   0,   0}, { 72,  85,  65,  78,   0,   0},
        { 72,  85,  65,  78,  71,   0}, { 72,  85,  73,   0,   0,   0},
        { 72,  85,  78,   0,   0,   0}, { 72,  85,  79,   0,   0,   0},
        { 74,  73,   0,   0,   0,   0}, { 74,  73,  65,   0,   0,   0},
        { 74,  73,  65,  78,   0,   0}, { 74,  73,  65,  78,  71,   0},
        { 74,  73,  65,  79,   0,   0}, { 74,  73,  69,   0,   0,   0},
        { 74,  73,  78,   0,   0,   0}, { 74,  73,  78,  71,   0,   0},
        { 74,  73,  79,  78,  71,   0}, { 74,  73,  85,   0,   0,   0},
        { 74,  85,   0,   0,   0,   0}, { 74,  85,  65,  78,   0,   0},
        { 74,  85,  69,   0,   0,   0}, { 74,  85,  78,   0,   0,   0},
        { 75,  65,   0,   0,   0,   0}, { 75,  65,  73,   0,   0,   0},
        { 75,  65,  78,   0,   0,   0}, { 75,  65,  78,  71,   0,   0},
        { 75,  65,  79,   0,   0,   0}, { 75,  69,   0,   0,   0,   0},
        { 75,  69,  78,   0,   0,   0}, { 75,  69,  78,  71,   0,   0},
        { 75,  79,  78,  71,   0,   0}, { 75,  79,  85,   0,   0,   0},
        { 75,  85,   0,   0,   0,   0}, { 75,  85,  65,   0,   0,   0},
        { 75,  85,  65,  73,   0,   0}, { 75,  85,  65,  78,   0,   0},
        { 75,  85,  65,  78,  71,   0}, { 75,  85,  73,   0,   0,   0},
        { 75,  85,  78,   0,   0,   0}, { 75,  85,  79,   0,   0,   0},
        { 76,  65,   0,   0,   0,   0}, { 76,  65,  73,   0,   0,   0},
        { 76,  65,  78,   0,   0,   0}, { 76,  65,  78,  71,   0,   0},
        { 76,  65,  79,   0,   0,   0}, { 76,  69,   0,   0,   0,   0},
        { 76,  69,  73,   0,   0,   0}, { 76,  69,  78,  71,   0,   0},
        { 76,  73,   0,   0,   0,   0}, { 76,  73,  65,   0,   0,   0},
        { 76,  73,  65,  78,   0,   0}, { 76,  73,  65,  78,  71,   0},
        { 76,  73,  65,  79,   0,   0}, { 76,  73,  69,   0,   0,   0},
        { 76,  73,  78,   0,   0,   0}, { 76,  73,  78,  71,   0,   0},
        { 76,  73,  85,   0,   0,   0}, { 76,  79,   0,   0,   0,   0},
        { 76,  79,  78,  71,   0,   0}, { 76,  79,  85,   0,   0,   0},
        { 76,  85,   0,   0,   0,   0}, { 76,  85,  65,  78,   0,   0},
        { 76,  85,  69,   0,   0,   0}, { 76,  85,  78,   0,   0,   0},
        { 76,  85,  79,   0,   0,   0}, { 77,   0,   0,   0,   0,   0},
        { 77,  65,   0,   0,   0,   0}, { 77,  65,  73,   0,   0,   0},
        { 77,  65,  78,   0,   0,   0}, { 77,  65,  78,  71,   0,   0},
        { 77,  65,  79,   0,   0,   0}, { 77,  69,   0,   0,   0,   0},
        { 77,  69,  73,   0,   0,   0}, { 77,  69,  78,   0,   0,   0},
        { 77,  69,  78,  71,   0,   0}, { 77,  73,   0,   0,   0,   0},
        { 77,  73,  65,  78,   0,   0}, { 77,  73,  65,  79,   0,   0},
        { 77,  73,  69,   0,   0,   0}, { 77,  73,  78,   0,   0,   0},
        { 77,  73,  78,  71,   0,   0}, { 77,  73,  85,   0,   0,   0},
        { 77,  79,   0,   0,   0,   0}, { 77,  79,  85,   0,   0,   0},
        { 77,  85,   0,   0,   0,   0}, { 78,   0,   0,   0,   0,   0},
        { 78,  65,   0,   0,   0,   0}, { 78,  65,  73,   0,   0,   0},
        { 78,  65,  78,   0,   0,   0}, { 78,  65,  78,  71,   0,   0},
        { 78,  65,  79,   0,   0,   0}, { 78,  69,   0,   0,   0,   0},
        { 78,  69,  73,   0,   0,   0}, { 78,  69,  78,   0,   0,   0},
        { 78,  69,  78,  71,   0,   0}, { 78,  73,   0,   0,   0,   0},
        { 78,  73,  65,  78,   0,   0}, { 78,  73,  65,  78,  71,   0},
        { 78,  73,  65,  79,   0,   0}, { 78,  73,  69,   0,   0,   0},
        { 78,  73,  78,   0,   0,   0}, { 78,  73,  78,  71,   0,   0},
        { 78,  73,  85,   0,   0,   0}, { 78,  79,  78,  71,   0,   0},
        { 78,  79,  85,   0,   0,   0}, { 78,  85,   0,   0,   0,   0},
        { 78,  85,  65,  78,   0,   0}, { 78,  85,  69,   0,   0,   0},
        { 78,  85,  78,   0,   0,   0}, { 78,  85,  79,   0,   0,   0},
        { 79,   0,   0,   0,   0,   0}, { 79,  85,   0,   0,   0,   0},
        { 80,  65,   0,   0,   0,   0}, { 80,  65,  73,   0,   0,   0},
        { 80,  65,  78,   0,   0,   0}, { 80,  65,  78,  71,   0,   0},
        { 80,  65,  79,   0,   0,   0}, { 80,  69,  73,   0,   0,   0},
        { 80,  69,  78,   0,   0,   0}, { 80,  69,  78,  71,   0,   0},
        { 80,  73,   0,   0,   0,   0}, { 80,  73,  65,  78,   0,   0},
        { 80,  73,  65,  79,   0,   0}, { 80,  73,  69,   0,   0,   0},
        { 80,  73,  78,   0,   0,   0}, { 80,  73,  78,  71,   0,   0},
        { 80,  79,   0,   0,   0,   0}, { 80,  79,  85,   0,   0,   0},
        { 80,  85,   0,   0,   0,   0}, { 81,  73,   0,   0,   0,   0},
        { 81,  73,  65,   0,   0,   0}, { 81,  73,  65,  78,   0,   0},
        { 81,  73,  65,  78,  71,   0}, { 81,  73,  65,  79,   0,   0},
        { 81,  73,  69,   0,   0,   0}, { 81,  73,  78,   0,   0,   0},
        { 81,  73,  78,  71,   0,   0}, { 81,  73,  79,  78,  71,   0},
        { 81,  73,  85,   0,   0,   0}, { 81,  85,   0,   0,   0,   0},
        { 81,  85,  65,  78,   0,   0}, { 81,  85,  69,   0,   0,   0},
        { 81,  85,  78,   0,   0,   0}, { 82,  65,  78,   0,   0,   0},
        { 82,  65,  78,  71,   0,   0}, { 82,  65,  79,   0,   0,   0},
        { 82,  69,   0,   0,   0,   0}, { 82,  69,  78,   0,   0,   0},
        { 82,  69,  78,  71,   0,   0}, { 82,  73,   0,   0,   0,   0},
        { 82,  79,  78,  71,   0,   0}, { 82,  79,  85,   0,   0,   0},
        { 82,  85,   0,   0,   0,   0}, { 82,  85,  65,   0,   0,   0},
        { 82,  85,  65,  78,   0,   0}, { 82,  85,  73,   0,   0,   0},
        { 82,  85,  78,   0,   0,   0}, { 82,  85,  79,   0,   0,   0},
        { 83,  65,   0,   0,   0,   0}, { 83,  65,  73,   0,   0,   0},
        { 83,  65,  78,   0,   0,   0}, { 83,  65,  78,  71,   0,   0},
        { 83,  65,  79,   0,   0,   0}, { 83,  69,   0,   0,   0,   0},
        { 83,  69,  78,   0,   0,   0}, { 83,  69,  78,  71,   0,   0},
        { 83,  72,  65,   0,   0,   0}, { 83,  72,  65,  73,   0,   0},
        { 83,  72,  65,  78,   0,   0}, { 83,  72,  65,  78,  71,   0},
        { 83,  72,  65,  79,   0,   0}, { 83,  72,  69,   0,   0,   0},
        { 83,  72,  69,  78,   0,   0}, { 88,  73,  78,   0,   0,   0},
        { 83,  72,  69,  78,   0,   0}, { 83,  72,  69,  78,  71,   0},
        { 83,  72,  73,   0,   0,   0}, { 83,  72,  79,  85,   0,   0},
        { 83,  72,  85,   0,   0,   0}, { 83,  72,  85,  65,   0,   0},
        { 83,  72,  85,  65,  73,   0}, { 83,  72,  85,  65,  78,   0},
        { 83,  72,  85,  65,  78,  71}, { 83,  72,  85,  73,   0,   0},
        { 83,  72,  85,  78,   0,   0}, { 83,  72,  85,  79,   0,   0},
        { 83,  73,   0,   0,   0,   0}, { 83,  79,  78,  71,   0,   0},
        { 83,  79,  85,   0,   0,   0}, { 83,  85,   0,   0,   0,   0},
        { 83,  85,  65,  78,   0,   0}, { 83,  85,  73,   0,   0,   0},
        { 83,  85,  78,   0,   0,   0}, { 83,  85,  79,   0,   0,   0},
        { 84,  65,   0,   0,   0,   0}, { 84,  65,  73,   0,   0,   0},
        { 84,  65,  78,   0,   0,   0}, { 84,  65,  78,  71,   0,   0},
        { 84,  65,  79,   0,   0,   0}, { 84,  69,   0,   0,   0,   0},
        { 84,  69,  78,  71,   0,   0}, { 84,  73,   0,   0,   0,   0},
        { 84,  73,  65,  78,   0,   0}, { 84,  73,  65,  79,   0,   0},
        { 84,  73,  69,   0,   0,   0}, { 84,  73,  78,  71,   0,   0},
        { 84,  79,  78,  71,   0,   0}, { 84,  79,  85,   0,   0,   0},
        { 84,  85,   0,   0,   0,   0}, { 84,  85,  65,  78,   0,   0},
        { 84,  85,  73,   0,   0,   0}, { 84,  85,  78,   0,   0,   0},
        { 84,  85,  79,   0,   0,   0}, { 87,  65,   0,   0,   0,   0},
        { 87,  65,  73,   0,   0,   0}, { 87,  65,  78,   0,   0,   0},
        { 87,  65,  78,  71,   0,   0}, { 87,  69,  73,   0,   0,   0},
        { 87,  69,  78,   0,   0,   0}, { 87,  69,  78,  71,   0,   0},
        { 87,  79,   0,   0,   0,   0}, { 87,  85,   0,   0,   0,   0},
        { 88,  73,   0,   0,   0,   0}, { 88,  73,  65,   0,   0,   0},
        { 88,  73,  65,  78,   0,   0}, { 88,  73,  65,  78,  71,   0},
        { 88,  73,  65,  79,   0,   0}, { 88,  73,  69,   0,   0,   0},
        { 88,  73,  78,   0,   0,   0}, { 88,  73,  78,  71,   0,   0},
        { 88,  73,  79,  78,  71,   0}, { 88,  73,  85,   0,   0,   0},
        { 88,  85,   0,   0,   0,   0}, { 88,  85,  65,  78,   0,   0},
        { 88,  85,  69,   0,   0,   0}, { 88,  85,  78,   0,   0,   0},
        { 89,  65,   0,   0,   0,   0}, { 89,  65,  78,   0,   0,   0},
        { 89,  65,  78,  71,   0,   0}, { 89,  65,  79,   0,   0,   0},
        { 89,  69,   0,   0,   0,   0}, { 89,  73,   0,   0,   0,   0},
        { 89,  73,  78,   0,   0,   0}, { 89,  73,  78,  71,   0,   0},
        { 89,  79,   0,   0,   0,   0}, { 89,  79,  78,  71,   0,   0},
        { 89,  79,  85,   0,   0,   0}, { 89,  85,   0,   0,   0,   0},
        { 89,  85,  65,  78,   0,   0}, { 89,  85,  69,   0,   0,   0},
        { 89,  85,  78,   0,   0,   0}, { 74,  85,  78,   0,   0,   0},
        { 89,  85,  78,   0,   0,   0}, { 90,  65,   0,   0,   0,   0},
        { 90,  65,  73,   0,   0,   0}, { 90,  65,  78,   0,   0,   0},
        { 90,  65,  78,  71,   0,   0}, { 90,  65,  79,   0,   0,   0},
        { 90,  69,   0,   0,   0,   0}, { 90,  69,  73,   0,   0,   0},
        { 90,  69,  78,   0,   0,   0}, { 90,  69,  78,  71,   0,   0},
        { 90,  72,  65,   0,   0,   0}, { 90,  72,  65,  73,   0,   0},
        { 90,  72,  65,  78,   0,   0}, { 90,  72,  65,  78,  71,   0},
        { 67,  72,  65,  78,  71,   0}, { 90,  72,  65,  78,  71,   0},
        { 90,  72,  65,  79,   0,   0}, { 90,  72,  69,   0,   0,   0},
        { 90,  72,  69,  78,   0,   0}, { 90,  72,  69,  78,  71,   0},
        { 90,  72,  73,   0,   0,   0}, { 83,  72,  73,   0,   0,   0},
        { 90,  72,  73,   0,   0,   0}, { 90,  72,  79,  78,  71,   0},
        { 90,  72,  79,  85,   0,   0}, { 90,  72,  85,   0,   0,   0},
        { 90,  72,  85,  65,   0,   0}, { 90,  72,  85,  65,  73,   0},
        { 90,  72,  85,  65,  78,   0}, { 90,  72,  85,  65,  78,  71},
        { 90,  72,  85,  73,   0,   0}, { 90,  72,  85,  78,   0,   0},
        { 90,  72,  85,  79,   0,   0}, { 90,  73,   0,   0,   0,   0},
        { 90,  79,  78,  71,   0,   0}, { 90,  79,  85,   0,   0,   0},
        { 90,  85,   0,   0,   0,   0}, { 90,  85,  65,  78,   0,   0},
        { 90,  85,  73,   0,   0,   0}, { 90,  85,  78,   0,   0,   0},
        { 90,  85,  79,   0,   0,   0}, {  0,   0,   0,   0,   0,   0},
        { 83,  72,  65,  78,   0,   0}, {  0,   0,   0,   0,   0,   0}, };
    // get all realName which has alias
    public static List<Character> getMultipleYin(String source) {
        List<Character> aliasChar = new ArrayList<Character>();
        if (!TextUtils.isEmpty(source)) {
            char[] ch = source.toCharArray();
            for (int i = 0; i < ch.length; i++) {
                if (mFamilyAliasNameMap.containsValue(ch[i])) {
                    aliasChar.add(ch[i]);
                }
            }
        }
        return aliasChar;
    }
    public static List<Character> getAliasByRealName(char key) {
        List<Character> aliasNames = new ArrayList<Character>();
        Set<Character> values = mFamilyAliasNameMap.keySet();
        Iterator<Character> iterator = values.iterator();
        while (iterator.hasNext()) {
            Character tmp = iterator.next();
            Character value = mFamilyAliasNameMap.get(tmp);
            if (value != null && value.equals(Character.valueOf(key))) {
                aliasNames.add(tmp);
            }
        }
        return aliasNames;
    }
    public static Map<Character, Character> getRealByAliasName(String source) {
        Map<Character, Character> realName = new HashMap<Character, Character>();
        if (!TextUtils.isEmpty(source)) {
            char[] ch = source.toCharArray();
            for (int i = 0; i < ch.length; i++) {
                if (mFamilyAliasNameMap.containsKey(ch[i])) {
                    realName.put(ch[i], mFamilyAliasNameMap.get(ch[i]));
                    break;
                }
            }
        }
        return realName;
    }
    private static HashMap<Character, Character> mFamilyAliasNameMap = new HashMap<Character, Character>() {
        {
            put('\u80bf', '\u79cd'); put('\u866b','\u79cd'); put('\u4f17','\u79cd');// "ZHONG", "CHONG" 种
            put('\u798f', '\u5b93'); put('\u89c5','\u5b93'); // "MI", "FU" 宓
            put('\u5c18', '\u8c0c');  put('\u751a', '\u8c0c'); // "CHEN" , "SHEN" 谌
            put('\u8238', '\u76d6') ; put('\u9499','\u76d6') ; //gài gě 盖
            put('\u73af', '\u90c7') ; put('\u5faa','\u90c7') ; //"HUAN","XUN" 郇
            put('\u79e6', '\u8983') ; put('\u575b','\u8983') ; //"QIN","TAN" 覃
            put('\u4e39', '\u5355') ; put('\u5584','\u5355') ; put('\u7f20','\u5355') ;//"DAN","SHAN" 单
            put('\u7167', '\u53ec') ; put('\u54e8','\u53ec') ; //"ZHAO","SHAO" 召
            put('\u7389', '\u851a') ; put('\u672a','\u851a') ; //"YU","WEI" 蔚
            put('\u4f1f', '\u9697') ; put('\u9b41','\u9697') ; //"WEI","KUI" 隗
            put('\u897f', '\u90d7') ; put('\u5403','\u90d7') ; //"XI","CHI" 郗
            put('\u8c22', '\u89e3') ; put('\u59d0','\u89e3') ;put('\u501f','\u89e3') ; //"XIE","JIE" 解
            put('\u6708', '\u4e50') ; put('\u53fb','\u4e50') ; put('\u836f','\u4e50') ;//"YUE","LE" yao 乐
            put('\u6e23', '\u67e5') ; put('\u8336','\u67e5') ; //"ZHA","CHA" 查
            put('\u554a', '\u963f') ; put('\u989d','\u963f') ; // "A", "E" 阿
            put('\u9698', '\u827e') ; put('\u4ea6','\u827e') ; // "AI", "YI" 艾
            put('\u7206', '\u66b4') ; put('\u5703','\u66b4') ; // "BAO", "PU" 暴
            put('\u53d8', '\u4fbf') ; put('\u9a88','\u4fbf') ; // "BIAN", "PIAN" 便
            put('\u4ed3', '\u85cf') ; put('\u846c','\u85cf') ; // "CANG", "ZANG" 藏
            put('\u5c42', '\u66fe') ; put('\u589e','\u66fe') ; // "CENG", "ZENG" 曾
            put('\u6f6e', '\u671d') ; put('\u62db','\u671d') ; // "CHAO", "ZHAO" 朝
            put('\u8eca', '\u8f66') ; put('\u5c45','\u8f66') ; // "CHE", "JU" 车
            put('\u57ce', '\u76db') ; put('\u5723','\u76db') ; // "CHENG", "SHENG" 盛
            put('\u6101', '\u4ec7') ; put('\u56da','\u4ec7') ; // "CHOU", "QIU" 仇
            put('\u6ef4', '\u63d0') ; put('\u9898','\u63d0') ; // "DI", "TI" 提
            put('\u76ef', '\u4e01') ; put('\u4e89','\u4e01') ; // "DING", "ZHENG" 丁
            put('\u800c', '\u800f') ; put('\u5948','\u800f') ; // "ER", "NAI" 耏
            put('\u70e6', '\u7e41') ; put('\u5a46','\u7e41') ; // "FAN", "PO" 繁
            put('\u65c1', '\u9022') ; put('\u670b','\u9022') ;// "FENG", "PANG" 逢
            put('\u7f1d', '\u51af') ; put('\u74f6','\u51af') ; // "FENG", "PING" 冯
            put('\u6cb3', '\u5408') ; put('\u683c','\u5408') ; // "GE", "HE" 合
            put('\u72b7', '\u5e7f') ; put('\u5b89','\u5e7f') ; // "GUANG", "AN" 广
            put('\u70af', '\u7085') ; put('\u8d35','\u7085') ; // "GUI", "JIONG" 炅
            put('\u6c47', '\u4f1a') ; put('\u7b77','\u4f1a') ; // "HUI", "KUAI" 会
            put('\u673a', '\u5947') ; put('\u9a91','\u5947') ; // "JI", "QI" 奇
            put('\u5047', '\u8d3e') ; put('\u86ca','\u8d3e') ; // "JIA", "GU" 贾
            put('\u5efa', '\u89c1') ; put('\u53bf','\u89c1') ; // "JIAN", "XIAN" 见
            put('\u6c5f', '\u5c06') ; put('\u6d46','\u5c06') ; put('\u8154','\u5c06') ; // jiāng  jiàng  qiāng  将
            put('\u9171', '\u964d') ; put('\u7965','\u964d') ; // "JIANG", "XIANG" 降
            put('\u811a', '\u7f34') ; put('\u5353','\u7f34') ;// "JIAO", "ZHUO" 缴
            put('\u6559', '\u6821') ; put('\u7b11','\u6821') ; // "JIAO", "XIAO" 校
            put('\u8ddd', '\u53e5') ; put('\u6c9f','\u53e5') ; // "JU", "GOU" 句
            put('\u4f67', '\u5361') ; put('\u6390','\u5361') ; // "KA", "QIA" 卡
            put('\u780d', '\u961a') ; put('\u558a','\u961a') ; // "KAN", "HAN" 阚
            put('\u5415', '\u7387') ; put('\u5e05','\u7387') ; // "LV", "SHUAI" 率
            put('\u5bc6', '\u79d8') ; put('\u5fc5','\u79d8') ; // "MI", "BI" 秘
            put('\u8885', '\u9e1f') ; put('\u540a','\u9e1f') ; // "NIAO", "DIAO" 鸟
            put('\u6d85', '\u4e5c') ; put('\u54a9','\u4e5c') ; // "NIE", "MIE" 乜
            put('\u68cb', '\u9f50') ; put('\u8bb0','\u9f50') ; // "QI", "JI" 齐
            put('\u8d77', '\u7a3d') ; put('\u57fa','\u7a3d') ; // "QI", "JI" 稽
            put('\u524d', '\u4e7e') ; put('\u7518','\u4e7e') ; // "QIAN", "GAN" 乾
            put('\u5899', '\u5f3a') ; put('\u5320','\u5f3a') ;put('\u62a2','\u5f3a') ; // "QIANG", "JIANG" 强
            put('\u5207', '\u90c4') ; put('\u620f','\u90c4') ; // qiè  xì 郄
            put('\u9a71', '\u533a') ; put('\u6b27','\u533a') ; // "QU", "OU" 区
            put('\u5026', '\u5708') ; put('\u609b','\u5708') ;put('\u9e43','\u5708') ; // juàn juān  quān  圈
            put('\u867d', '\u772d') ; put('\u7070','\u772d') ; // suī   huī 眭
            put('\u7802', '\u5239') ; put('\u8be7','\u5239') ; // "SHA", "CHA" 刹
            put('\u820c', '\u6298') ; put('\u54f2','\u6298') ;put('\u906e','\u6298') ; // "SHE", "ZHE" 折
            put('\u6df1', '\u8398') ; put('\u65b0','\u8398') ; // "SHEN", "XIN" 莘
            put('\u5ba1', '\u6c88') ; put('\u9648','\u6c88') ; // "SHEN", "CHEN" 沈
            put('\u65f6', '\u77f3') ; put('\u86cb','\u77f3') ; // "SHI", "DAN" 石
            put('\u8c08', '\u9561') ; put('\u6b23','\u9561') ; // "TAN", "XIN" 镡
            put('\u9003', '\u9676') ; put('\u59da','\u9676') ; // "TAO", "YAO" 陶
            put('\u5510', '\u6c64') ; put('\u4f24','\u6c64') ; // "TANG", "SHANG" 汤
            put('\u7897', '\u4e07') ; put('\u9ed8','\u4e07') ; // "WAN", "MO" 万
            put('\u5582', '\u5c09') ; put('\u9047','\u5c09') ; // "WEI", "YU" 尉
            put('\u8e29', '\u91c7') ; put('\u83dc','\u91c7') ;// cǎi  cài 采
            put('\u978b', '\u9889') ; put('\u6770','\u9889') ; // "XIE", "JIE" 颉
            put('\u578b', '\u884c') ; put('\u822a','\u884c') ; put('\u6c86','\u884c') ;put('\u6052','\u884c') ;// xíng háng  hàng  héng   行
            put('\u9192', '\u7701') ; put('\u58f0','\u7701') ; // "XING", "SHENG" 省
            put('\u7d20', '\u5bbf') ; put('\u673d', '\u5bbf') ;put('\u8896', '\u5bbf') ;// sù  xiǔ   xiù 宿
            put('\u4e1a', '\u53f6') ; put('\u659c','\u53f6') ; // "YE", "XIE" 叶
            put('\u5a31', '\u65bc') ; put('\u5c4b','\u65bc') ; put('\u7600','\u65bc') ;// yú  wū  yū 於
            put('\u547c', '\u5401') ; put('\u865a','\u5401') ; put('\u9884','\u5401') ;// "YU", "XU" yù 吁
            put('\u95f8', '\u8f67') ; put('\u4e9a','\u8f67') ; // "ZHA", "YA" "GA" 轧
            put('\u6cbe', '\u7c98') ; put('\u5e74','\u7c98') ; // "ZHAN", "NIAN" 粘
            put('\u4e3b', '\u891a') ; put('\u695a','\u891a') ; // "ZHU", "CHU" 褚
            put('\u82ad', '\u5df4') ; put('\u5427','\u5df4') ; // "ba", "bā" 巴
            put('\u5954', '\u8d32') ; put('\u5e01','\u8d32') ; // "ben", "bi" 贲
            put('\u5e01', '\u90b2') ; put('\u53d8','\u90b2') ; //"BI", "BIAN" 邲
            put('\u640f', '\u4f2f') ; put('\u6446','\u4f2f') ; // "BO", "BAI" 伯
            put('\u818a', '\u67cf') ; put('\u767e','\u67cf') ; put('\u6a97','\u67cf') ; //"BAI", "BO" 柏
            put('\u52c3', '\u8584') ; put('\u96f9','\u8584') ;put('\u7c38','\u67cf') ; //"BAO", "BO" 薄
            put('\u8865', '\u535c') ; put('\u64ad','\u535c') ; //"BO", "BU" 卜
            put('\u640b', '\u63e3') ; put('\u8e39','\u63e3') ; // chuǎi chuāi  (chuài) 揣
            put('\u70df', '\u71d5') ; put('\u8273','\u71d5') ; // yān   yàn   燕
            put('\u76fe', '\u987f') ; put('\u6bd2','\u987f') ; // dùn  dú 顿
            put('\u675c', '\u987f') ; put('\u593a','\u987f') ; // dù   duó 度
            put('\u53ee', '\u4e01') ; put('\u5f81','\u4e01') ; // dīng  zhēng 丁
            put('\u8239', '\u4f20') ; put('\u8d5a','\u4f20') ; // chuán  zhuàn传
            put('\u5c48', '\u66f2') ; put('\u53d6','\u66f2') ; // qū qǔ 曲
            put('\u7f3a', '\u9619') ; put('\u786e','\u9619') ; //  quē què 阙
            put('\u5b85', '\u7fdf') ; put('\u8fea','\u7fdf') ; //"ZHAI", "DI"翟
            put('\u5145', '\u51b2') ; put('\u94f3','\u51b2') ;// chōng   chòng 冲
            put('\u94db', '\u5f53') ; put('\u8361','\u5f53') ;// dāng   dàng 当
            put('\u90b8', '\u5e95') ; put('\u7684','\u5e95') ;// dǐ de底
            put('\u8c46', '\u6597') ; put('\u9661','\u6597') ;// dòu dǒu斗
            put('\u7763', '\u90fd') ; put('\u515c','\u90fd') ;//  dū   dōu都
            put('\u809d', '\u5e72') ; put('\u8d63','\u5e72') ;// gān gàn 干
            put('\u6e2f', '\u5c97') ; put('\u7f38','\u5c97') ;// gǎng gāng 岗
            put('\u94a9', '\u52fe') ; put('\u5920','\u52fe') ;//gōu  gòu勾
            put('\u6cbe', '\u5360') ; put('\u6218','\u5360') ;//zhān zhàn 占
            put('\u949f', '\u4e2d') ; put('\u4ef2','\u4e2d') ;//zhōng   zhòng 中
            put('\u8bc1', '\u6b63') ; put('\u84b8','\u6b63') ;//zhèng   zhēng 正
            put('\u88c5', '\u58ee') ; put('\u649e','\u58ee') ;//zhuāng  zhuàng 壮
            put('\u8d44', '\u8a3e') ; put('\u7d2b','\u8a3e') ;//zī  zǐ訾
            put('\u8e2a', '\u7efc') ; put('\u8d60','\u7efc') ;//zōng zèng综
            put('\u6cfd', '\u7b2e') ; put('\u51ff','\u7b2e') ;//zé  zuó 笮
            put('\u518d', '\u8f7d') ; put('\u4ed4','\u8f7d') ;//zài  zǎi载
            put('\u8fd0', '\u5458') ; put('\u539f','\u5458') ; put('\u4e91','\u5458') ;//yùn  yuán  yún  员
            put('\u704c', '\u51a0') ; put('\u5b98','\u51a0') ;// guàn   guān  冠
            put('\u7737', '\u96bd') ; put('\u4fca','\u96bd') ;//juàn jùn 隽
            put('\u5f55', '\u9646') ; put('\u905b','\u9646') ;// lù  liù  陆
            put('\u8c0b', '\u725f') ; put('\u5893','\u725f') ;//móu  mù 牟
            put('\u4ec2', '\u52d2') ;                 // lè  (lēi) 勒
            put('\u8ff7', '\u9e8b') ; put('\u6885','\u9e8b') ;//mí  méi 麋
            put('\u5e99', '\u7f2a') ; put('\u8c2c','\u7f2a') ; put('\u7738','\u7f2a') ;//miào  miù   móu 缪
            put('\u5f31', '\u82e5') ; put('\u60f9','\u82e5') ;//ruò rě 若
            put('\u6da9', '\u8272') ; put('\u6652','\u8272') ;//sè   （shǎi) 色
            put('\u663e', '\u6d17') ; put('\u559c','\u6d17') ;// xiǎn   xǐ 洗
            put('\u87c6', '\u9ebb') ; put('\u5988','\u9ebb') ;// má  mā 麻
            put('\u8302', '\u5192') ; put('\u672b','\u5192') ;// mào mò 冒
            put('\u72fc', '\u90ce') ; put('\u6d6a','\u90ce') ;// láng  làng            郎
            put('\u529b', '\u90e6') ; put('\u79bb','\u90e6') ;// lì  lí   郦
            put('\u9886', '\u4ee4') ; put('\u7075','\u4ee4') ; put('\u53e6','\u4ee4') ;// lǐng   líng  lìng            令
            put('\u7b3c', '\u9686') ;                 // lóng (lōng)    隆
            put('\u5362', '\u82a6') ; put('\u9c81','\u82a6') ;//lú  lǔ   芦
            put('\u8bba', '\u8bba') ; put('\u56f5','\u8bba') ;// lùn lún    论
            put('\u76df', '\u8499') ; put('\u63b9','\u8499') ; put('\u731b','\u8499') ;//méng mēng měng 蒙
            put('\u6577', '\u592b') ; put('\u670d','\u592b') ;// fū  fú 夫
            put('\u4ed8', '\u7236') ; put('\u752b','\u7236') ;// fù  fǔ 父
            put('\u9694','\u845b') ;// (gě)   gé 葛
            put('\u94ec', '\u5404') ;                 // gè  (gě) 各
            put('\u5bab', '\u4f9b') ; put('\u5171','\u4f9b') ;// gōng   gòng   供
            put('\u553e', '\u62d3') ; put('\u69bb','\u62d3') ;//tuò  tà 拓
            put('\u95ee', '\u6c76') ; put('\u95e8','\u6c76') ;//wèn  mén 汶
            put('\u81aa', '\u555c') ; put('\u7ef0','\u555c') ;//啜   chuài  chuò
            put('\u90ed', '\u8fc7') ; // 过  guō  （guò  ③ guo）
            put('\u5964', '\u54c8') ; put('\u94ea','\u54c8') ;// 哈  hǎ  ( hà)  ③ hā
            put('\u4f55', '\u548c') ; put('\u8d3a','\u548c') ;put('\u6e56','\u548c') ;put('\u6d3b','\u548c') ;put('\u8d27','\u548c') ;// 和  hé   hè  ③ hú  ④ huó  ⑤ huò
            put('\u7334', '\u4faf') ; put('\u540e','\u4faf') ;// 侯   hóu   hòu
            put('\u753b', '\u534e') ; put('\u6ed1','\u534e') ;// 华   huà   huá
            put('\u6bc1', '\u867a') ; put('\u8f89','\u867a') ;//虺   huǐ   huī
            put('\u6324', '\u7eaa') ; put('\u65e2','\u7eaa') ;// 纪   jǐ   jì
            put('\u5251', '\u76d1') ; put('\u95f4','\u76d1') ;//监   jiàn  jiān
            put('\u997a', '\u77eb') ; put('\u89d2','\u77eb') ;// 矫   jiǎo  jiáo
            put('\u6d01', '\u6770') ; put('\u63a5','\u6770') ;// 节   jié   jiē
            put('\u7cbe', '\u7ecf') ; put('\u7adf','\u7ecf') ;// 经   jīng  jìng
            put('\u6e34', '\u53ef') ; put('\u8bfe','\u53ef') ;// 可   kě   kè
            put('\u632a', '\u90a3') ; put('\u7eb3','\u90a3') ;put('\u5185','\u90a3') ;// 那 ( nā)   nuó  ③ nà  ④ nèi
            put('\u7537', '\u5357') ; put('\u51ea','\u5357') ;// 南   nán  ③ nā
            put('\u4f5e', '\u5b81') ; put('\u51dd','\u5b81') ;// 宁   nìng  níng
            put('\u9a97', '\u7247') ; put('\u7bc7','\u7247') ;// 片   piàn  piān
            put('\u74e2', '\u6734') ; put('\u5761','\u6734') ;put('\u7834','\u6734') ;put('\u666e','\u6734') ;// 朴  piáo  pō  ③ pò  ④ pǔ
            put('\u8461', '\u4ec6') ; put('\u5657','\u4ec6') ;// 仆   pú   pū
            put('\u6865', '\u8c2f') ; put('\u7fd8','\u8c2f') ;// 谯   qiáo  qiào
            put('\u4ec1', '\u4efb') ; put('\u8ba4','\u4efb') ;//  任   rén   rèn
            put('\u6740', '\u6c99') ; put('\u715e','\u6c99') ;// 沙   shā   shà
            put('\u5220', '\u9490') ; put('\u64c5','\u9490') ;// 钐   shān  shàn
            put('\u62ac', '\u53f0') ; put('\u80ce','\u53f0') ;// 台   tái   tāi
            put('\u7ae5', '\u540c') ; put('\u75db','\u540c') ;// 同   tóng  tòng
            put('\u5deb', '\u4e4c') ; put('\u7269','\u4e4c') ;// 乌   wū   wù
            put('\u6380', '\u9c9c') ; put('\u9669','\u9c9c') ;// 鲜   xiān  xiǎn
            put('\u9999', '\u76f8') ; put('\u5411','\u76f8') ;// 相   xiāng  xiàng
            put('\u661f', '\u5174') ; put('\u59d3','\u5174') ;// 兴   xīng  xìng
            put('\u60ac', '\u65cb') ; put('\u70ab','\u65cb') ;// 旋   xuán  xuàn
            put('\u9080', '\u8981') ; put('\u8000','\u8981') ;//要   yāo   yào
            put('\u5df2', '\u6905') ; put('\u4f0a','\u6905') ;// 椅   yǐ   yī
            put('\u56e0', '\u6bb7') ; put('\u9609','\u6bb7') ; put('\u5f15','\u6bb7') ;// 殷   yīn   yān  ③ yǐn
            put('\u82f1', '\u5e94') ; put('\u786c','\u5e94') ;//应   yīng  yìng
            put('\u6108', '\u55bb') ; put('\u9c7c','\u55bb') ;//喻   yù   yú
            put('\u6893', '\u5b50') ; put('\u5b57','\u5b50') ;// 子   zǐ   zi
            put('\u9080', '\u4e48') ; put('\u9ebd','\u4e48') ;// 么   me   yao
            put('\u81f0', '\u81ed') ; put('\u79c0','\u81ed') ; // "chou", "xiu" 臭
           }
        };
    }
