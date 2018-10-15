package com.smartisanos.voice.util;

import java.util.List;

public class ListUtils {
    public static boolean isSame(List<String> a, List<String> b) {
        if (a.size() != b.size()) {
            return false;
        }
        long hasha = 0;
        for (String str : a) {
            hasha += (str != null ? str.hashCode() : 0);
        }
        long hashb = 0;
        for (String str : b) {
            hashb += (str != null ? str.hashCode() : 0);
        }
        return hasha == hashb;
    }
}
