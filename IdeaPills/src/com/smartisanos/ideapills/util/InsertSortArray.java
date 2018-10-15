package com.smartisanos.ideapills.util;

import java.util.ArrayList;

public class InsertSortArray {
    final ArrayList<Integer> mArray;

    public InsertSortArray() {
        mArray = new ArrayList<Integer>();
    }

    public int getInsertPosition(int value) {
        int size = mArray.size();
        if (size == 0) {
            return 0;
        } else {
            int start = 0;
            int end = size;
            int middle;
            for (; start < end; ) {
                middle = ((end + start) >>> 1);
                int target = (int) mArray.get(middle);
                if (target > value) {
                    end = middle;
                } else if (target < value) {
                    start = middle + 1;
                } else {
                    return middle;
                }
            }
            return start;
        }
    }

    public void insertValue(int value) {
        mArray.add(value);
    }

    public void clear() {
        mArray.clear();
    }

    public boolean isEmpty() {
        return mArray.isEmpty();
    }
}
