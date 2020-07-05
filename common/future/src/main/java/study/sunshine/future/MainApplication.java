package study.sunshine.future;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * @Author: dongcx
 * @Description:
 * @Date: 2020-07-03
 **/
public class MainApplication {
    public static void main(String[] args) {
        CompletableFuture<String> completableFuture = CompletableFuture.supplyAsync(()->{
            try {
                TimeUnit.SECONDS.sleep(1);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return "success";
        });
         completableFuture.thenAccept(value -> {
            System.out.println("thenAccept  "+value);
        });
        CompletableFuture completableFuture1 = completableFuture.thenApply(value -> {
            System.out.println("thenApply  "+value);
            return value;
        });
        try {
            System.out.println("completableFuture1 "+ completableFuture1.get());
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
        try {
            System.out.println("sleep");
            TimeUnit.SECONDS.sleep(5);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
