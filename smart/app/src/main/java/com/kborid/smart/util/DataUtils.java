package com.kborid.smart.util;

import android.util.ArrayMap;
import android.util.SparseArray;
import android.util.SparseBooleanArray;

import java.util.HashMap;

public class DataUtils {
    private static SparseArray<String> stringSparseArray = new SparseArray<>();
    private static SparseBooleanArray sparseBooleanArray = new SparseBooleanArray();
    private static ArrayMap<Integer, String> arrayMap = new ArrayMap<>();
    private static HashMap<Integer, String> hashMap = new HashMap<>();

    public static void setValue(String[] strings) {
        for (int i = 0; i < strings.length; i++) {
            String str = strings[i];
            stringSparseArray.put(i, str);
            sparseBooleanArray.put(i, str.equals("duan"));
            arrayMap.put(i, str);
            hashMap.put(i, str);
        }
    }

    public static String getByIndex(int index) {
        return arrayMap.get(index);
    }
}
