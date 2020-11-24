package study.sunshine.future;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @Author: dongcx
 * @Description:
 * @Date: 2020-07-03
 **/
public class MainApplication {
    public static void main(String[] args) throws InterruptedException, ExecutionException {
//        test1();
        CompletableFuture<Integer> future1 = CompletableFuture.supplyAsync(()->{
            try {
                System.out.println("future1 start");
                TimeUnit.SECONDS.sleep(2);
                System.out.println("future1 end");
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return 1;
        });
        CompletableFuture<Integer> future2 = future1.thenApply(item->{
            try {
                System.out.println("future2 start");
                TimeUnit.SECONDS.sleep(2);
                System.out.println("future2 end");
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return item+1;
        });
        future2.thenAccept((item)->{
            System.out.println(item);
        });
        future2.whenComplete((v,t)->{
            System.out.println("whenComplete future2");
        });
        future1.whenComplete((v,t)->{
            System.out.println("whenComplete future1");
        });
        try {
            System.out.println("wait ...");
            System.out.println(System.in.read());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public static void  test1(){
        ExecutorService executorService =new ThreadPoolExecutor(5, 5,
                0L, TimeUnit.MILLISECONDS,
                new SynchronousQueue<>());
        for (int i = 0; i < 5; i++) {
            executorService.execute(()->{
                System.out.println(Thread.currentThread().getName() + " run ");
            });
        }
        try {
            TimeUnit.SECONDS.sleep(2);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println(" --------------- ");
        for (int i = 0; i < 3; i++) {

            executorService.execute(()->{
                System.out.println(Thread.currentThread().getName() + " run ");
            });
        }
        try {
            System.out.println(System.in.read());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
