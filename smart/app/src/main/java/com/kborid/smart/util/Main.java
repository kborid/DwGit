package com.kborid.smart.util;

import java.util.HashMap;
import java.util.Scanner;

public class Main {
    private static String[] ChartTable = new String[] {"a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "k", "l", "m", "n", "o", "p", "q", "r", "s", "t", "u", "v", "w", "x", "y", "z"};
    private static HashMap<String, Integer> ChartMap = new HashMap<String, Integer>(){
        {
            put("a", 0);
            put("b", 1);
            put("c", 2);
            put("d", 3);
            put("e", 4);
            put("f", 5);
            put("g", 6);
            put("h", 7);
            put("i", 8);
            put("j", 9);
            put("k", 10);
            put("l", 11);
            put("m", 12);
            put("n", 13);
            put("o", 14);
            put("p", 15);
            put("q", 16);
            put("r", 17);
            put("s", 18);
            put("t", 19);
            put("u", 20);
            put("v", 21);
            put("w", 22);
            put("x", 23);
            put("y", 24);
            put("z", 25);
        }
    };

    public static void main(String arg[]) {
        Scanner in = new Scanner(System.in);
        outputFormatString(in.nextLine());
    }

    private static void outputFormatString(String input) {
        int[] buckets = new int[ChartTable.length];
        for (char c : input.toCharArray()) {
            String s = String.valueOf(c).toLowerCase();
            if (ChartMap.containsKey(s)) {
                int index = ChartMap.get(s);
                buckets[index]++;
            }
        }

        for (int i = 0; i < buckets.length; i++) {
            if (buckets[i] > 0) {
                System.out.print(ChartTable[i] + buckets[i]);
            }
        }
    }
}
