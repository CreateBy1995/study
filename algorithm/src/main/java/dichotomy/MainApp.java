package dichotomy;

import org.junit.Test;

/**
 * @Author: dongcx
 * @Description: 二分查找
 * @Date: 2020-09-18
 **/
public class MainApp {
    public int[] getSequenceArray(int capacity) {
        int[] arr = new int[capacity];
        for (int i = 0; i < capacity; i++) {
            arr[i] = i + 1;
        }
        return arr;
    }

    public int findNumInSequenceArray(int num, int[] arr, int start, int end) {
        int middle = (start + end) / 2;
        if (num == arr[middle]) {
            return middle;
        } else if (num > arr[middle]) {
            return findNumInSequenceArray(num, arr, middle, end);
        } else {
            return findNumInSequenceArray(num, arr, start, middle);
        }
    }

    public int findNumInSequenceArray(int num, int[] arr) {
        int result = 0;
        for (int i = 0; i < arr.length; i++) {
            if (num == arr[i]) {
                result = i;
                break;
            }
        }
        return result;
    }

    @Test
    public void test0001() {
        int[] arr = getSequenceArray(11);
        long timeStart = System.currentTimeMillis();
        System.out.println(findNumInSequenceArray(7, arr, 0, arr.length - 1));
        System.out.println(System.currentTimeMillis() - timeStart);
        timeStart = System.currentTimeMillis();
        System.out.println(findNumInSequenceArray(7, arr));
        System.out.println(System.currentTimeMillis() - timeStart);
    }
}
