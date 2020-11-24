package study.sunshine.concurrent;

import java.util.concurrent.RecursiveTask;

/**
 * @Author: dongcx
 * @Description:
 * @Date: 2020-08-18
 **/
public class SumTask extends RecursiveTask<Integer> {
    private int start;
    private int end;
    private int threshold;

    public SumTask(int start, int end, int threshold) {
        this.start = start;
        this.end = end;
        this.threshold = threshold;
    }

    @Override
    protected Integer compute() {
        if (end - start < threshold) {
            int sum = 0;
            System.out.println("current thread is " + Thread.currentThread());
            for (int i = start; i < end; i++) {
                sum += i;
            }
            return sum;
        } else {
            int middle = (start + end) / 2;
            SumTask sumTask1 = new SumTask(start, middle, threshold);
            SumTask sumTask2 = new SumTask(middle, end, threshold);
            sumTask1.fork();
            sumTask2.fork();
            return sumTask1.join() + sumTask2.join();
        }
    }
}
