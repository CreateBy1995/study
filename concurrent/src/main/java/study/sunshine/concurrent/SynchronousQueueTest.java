package study.sunshine.concurrent;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @Author: dongcx
 * @Description:
 * @Date: 2020-06-24
 **/
public class SynchronousQueueTest {
    /**
     * dubbo默认使用的线程池的队列数为0，也就是用SynchronousQueue来实现
     * 该队列最多的缓存的任务数只有一个，下面这个例子，线程池的线程数为1，队列长度最高为1
     * 也就是说如果下面第一个任务执行的过程中休眠了2s，导致线程池的线程数被耗尽，那么第二个任务的话就进入到队列中
     * 而当第三个任务要执行的时候线程池的没有空闲线程，并且由于队列中已经有一个等待被消费的任务，所以无法在进入队列
     * 此时，线程池就会抛出异常
     */
    public static void test001(){
        BlockingQueue<Runnable> blockingQueue = new SynchronousQueue<>();
        ExecutorService executorService = new ThreadPoolExecutor(1,1,1000L, TimeUnit.MILLISECONDS,blockingQueue);
        for (int i = 0; i < 5; i++) {
            System.out.println(i);
            executorService.execute(()->{
                try {
                    System.out.println("task  start");
                    TimeUnit.SECONDS.sleep(2);
                    System.out.println("task  end");
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            });
        }
    }
    public static void test1(){
        BlockingQueue<String> blockingQueue = new SynchronousQueue<>();
        blockingQueue.offer("a");
    }
    public static void main(String[] args) throws InterruptedException {
        BlockingQueue<String> blockingQueue = new SynchronousQueue<>();
        new Thread(()->{
            try {
                System.out.println(Thread.currentThread().getName() +" waiting for take");
                System.out.println(blockingQueue.take());
                System.out.println(Thread.currentThread().getName() +" end");
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
        new Thread(()->{
            try {
                System.out.println(Thread.currentThread().getName() + " waiting for take");
                System.out.println(blockingQueue.take());
                System.out.println(Thread.currentThread().getName() +" end");
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
        TimeUnit.SECONDS.sleep(2);
        System.out.println("start offer");
        blockingQueue.offer("b");
//        blockingQueue.offer("ba");
        System.out.println("end offer");

//        test001();
    }
}
