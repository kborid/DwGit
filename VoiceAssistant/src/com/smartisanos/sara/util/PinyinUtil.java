package com.smartisanos.sara.util;

import android.text.TextUtils;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;

import smartisanos.t9search.HanziToPinyin;
import smartisanos.t9search.HanziToPinyin.Token;

public class PinyinUtil {
    private PinyinUtil() {}

    public static boolean isChinese(String value) {
        if (!TextUtils.isEmpty(value)) {
            int length = value.length();
            int offset = 0;
            while (offset < length) {
                if (isChinese(value.charAt(offset))) {
                    return true;
                }
                offset++;
            }
        }
        return false;
    }

    public static boolean isChinese(char c) {
        Character.UnicodeBlock ub = Character.UnicodeBlock.of(c);
        if (ub == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS
                || ub == Character.UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS
                || ub == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A
                || ub == Character.UnicodeBlock.GENERAL_PUNCTUATION
                || ub == Character.UnicodeBlock.CJK_SYMBOLS_AND_PUNCTUATION
                || ub == Character.UnicodeBlock.HALFWIDTH_AND_FULLWIDTH_FORMS) {
            return true;
        }
        return false;
    }

    public static String getPinYin(String name) {
        if (TextUtils.isEmpty(name)) {
            return "";
        }

        if (isChinese(name)) {
            StringBuilder builder = new StringBuilder();
            Iterator<String> it = getPinyinKeys(name);
            if (it != null) {
                while (it.hasNext()) {
                    builder.append(it.next());
                }
            }
            return builder.toString();
        }

        return name;
    }

    public static String getPurePinYin(String name) {
        if (TextUtils.isEmpty(name)) {
            return "";
        }
        // remove punctuation， only keep alphabet and chinese.
        name = name.replaceAll("[^0-9a-zA-Z\u4e00-\u9fa5]", "");
        return getPinYin(name);
    }

    private static Iterator<String> getPinyinKeys(String name) {
        // TODO : Reduce the object allocation.
        LinkedHashSet<String> keys = new LinkedHashSet<String>();
        ArrayList<Token> tokens = HanziToPinyin.getInstance().get(name);
        final int tokenCount = tokens.size();
        for (int i = 0; i < tokenCount; i++) {
            String keyPinyin = "";
            // There is no space among the Chinese Characters, the variant name
            // lookup key wouldn't work for Chinese. The keyOriginal is used to
            // build the lookup keys for itself.
            final Token token = tokens.get(i);
            if (Token.UNKNOWN == token.type) {
                continue;
            }
            if (Token.PINYIN == token.type) {
                keyPinyin = token.target.toString();
            } else if (Token.LATIN == token.type) {
                keyPinyin = token.source;
            }
            // Avoid adding space at the end of String.
            if (i != tokenCount - 1)
                keyPinyin += " ";
            keys.add(keyPinyin);
        }
        return keys.iterator();
    }

}
