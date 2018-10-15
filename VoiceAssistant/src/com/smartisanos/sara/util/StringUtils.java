package com.smartisanos.sara.util;

import java.util.Random;

public class StringUtils {
    private static final String TAG = "StringUtils";
    private static final boolean DEBUG = true;
    private static final int RANDOM_NUM = 6;

    public static String getRandomCharacter() {
        //0 - 9,a - z, A-Z
        String val = "";
        Random random = new Random();
        for (int i = 0; i < RANDOM_NUM; i++) {

            boolean isChar = random.nextInt(2) % 2 == 0 ? true : false;
            if (isChar) {
                int temp = random.nextInt(2) % 2 == 0 ? 65 : 97;
                val += (char) (random.nextInt(26) + temp);
            } else {
                val += String.valueOf(random.nextInt(10));
            }
        }
        return val;
    }

    public static String handleSpecialCharacter(String str){
        return str.replace("\\", "\\\\").replace("*", "\\*")
                .replace("+", "\\+").replace("|", "\\|")
                .replace("{", "\\{").replace("}", "\\}")
                .replace("(", "\\(").replace(")", "\\)")
                .replace("^", "\\^").replace("$", "\\$")
                .replace("[", "\\[").replace("]", "\\]")
                .replace("?", "\\?").replace(",", "\\,")
                .replace(".", "\\.").replace("&", "\\&");
    }

    public static String trimPunctuation(String str) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < str.length(); ++i) {
            char punct[] = getPunctuationRange();
            boolean need_filter = false;
            for (int j = 0; j < punct.length; ++j) {
                if (punct[j] == str.charAt(i)) {
                    need_filter = true;
                    break;
                }
            }

            if (!need_filter) {
                result.append(str.charAt(i));
            }
        }
        return result.toString();
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

    public static boolean isPunctuation(char c) {
        return isChinesePunctuation(c) || isPunctuationRange(c);
    }

    private static boolean isPunctuationRange(char c) {
        char punct[] = getPunctuationRange();
        for (int j = 0; j < punct.length; ++j) {
            if (punct[j] == c) {
                return true;
            }
        }
        return false;
    }

    private static char[] getPunctuationRange() {
        char punct[] = { '\u002c', '\u002e', '\u0021', '\u003f', '\u003b', '\u003a', '\uff0c', '\u3002', '\uff01', '\uff1f',
                '\uff1b', '\uff1a', '\u3001', '\u0025','\uff0f','\u002f'};
        return punct;
    }
}
