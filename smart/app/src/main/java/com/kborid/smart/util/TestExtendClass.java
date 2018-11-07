package com.kborid.smart.util;

import java.util.Arrays;

public class TestExtendClass {
    public static int test() {
        int a = 2;

        try {
            return a * a;
//            throw new NullPointerException("123");
        } catch (Exception e) {
            e.printStackTrace();
            return a + a + a;
        } finally {
            System.out.println("finally()");
        }
    }

    public static void main(String[] arg) {
        System.out.println("test() " + test());
        int[] arr = new int[] {12, 4, 3, 9, 10, 11, 30, 49, 5, 10, 12, 9, 29, 22, 44};
        int left = 0;
        int right = arr.length - 1;
        quickSort(arr, left, right);
        System.out.println(Arrays.toString(arr));
        bucketSort(arr);
    }

    private static void quickSort(int[] arr, int left, int right) {
        if (left >= right) {
            return;
        }
        int index = partition(arr, left, right);
        quickSort(arr, left, index - 1);
        quickSort(arr, index + 1, right);
    }

    private static int partition(int[] arr, int left, int right) {
       int base = arr[left];
       while (left < right) {
           while (arr[right] >= base && left < right) {
               right--;
           }
           arr[left] = arr[right];
           while (arr[left] <= base && left < right) {
               left++;
           }
           arr[right] = arr[left];
       }
       arr[left] = base;
       return left;
    }

    private static void bucketSort(int[] arr) {
        int max = Integer.MIN_VALUE;
        for (int i : arr) {
            max = Math.max(max, i);
        }

        int[] buckets = new int[max + 1];

        for (int i : arr) {
            buckets[i]++;
        }

        for (int i = 0; i < buckets.length; i++) {
            if (buckets[i] != 0) {
                for (int j = 0; j < buckets[i]; j++) {
                    System.out.print(i);
                    System.out.print(",");
                }
            }
        }
    }
}
