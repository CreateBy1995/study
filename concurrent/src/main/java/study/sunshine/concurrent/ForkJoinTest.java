package study.sunshine.concurrent;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ForkJoinPool;
import java.util.stream.IntStream;

/**
 * @Author: dongcx
 * @Description:
 * @Date: 2020-08-18
 **/
public class ForkJoinTest {
    public static void main(String[] args) {
        test0001();
    }

    public static void test0001() {
        List<Integer> integers = new ArrayList<>();
        for (int i = 0; i < 100000; i++) {
            integers.add(i);
        }
//        Map<Integer, Integer> map = new HashMap<>();
        Map<Integer, Integer> map = new ConcurrentHashMap<>();
        long now = System.currentTimeMillis();
        ForkJoinPool forkJoinPool = new ForkJoinPool(2);
//        try {
//            forkJoinPool.submit( ()->IntStream.range(0, 5).parallel().mapToObj(i -> integers.subList(i*20000, (i + 1) * 20000))
//                    .forEach(item -> {
//                        System.out.println(Thread.currentThread().getName());
//                        item.forEach(ii -> map.put(ii, ii));
//                    }));
//           ;
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        } catch (ExecutionException e) {
//            e.printStackTrace();
//        }
        forkJoinPool.submit( ()->IntStream.range(0, 5).parallel().mapToObj(i -> integers.subList(i*20000, (i + 1) * 20000))
                .forEach(item -> {
                    System.out.println(Thread.currentThread().getName());
                    item.forEach(ii -> map.put(ii, ii));
                }));
//        integers.parallelStream().forEach(i -> {
////            System.out.println(Thread.currentThread().getName());
//            map.put(i,i);
//        });
        System.out.println(map.size());
        System.out.println(map.get(1555));
        System.out.println("use " + (System.currentTimeMillis() - now));
//    IntStream intStream = IntStream.range(0,100);
//    List<String> strings = intStream.parallel().mapToObj(num -> {
//        List<String> strings1 = Arrays.asList("a","b");
//        return strings1;
//    }).flatMap(strings1 -> strings1.stream()).collect(Collectors.toList());
//        System.out.println(strings.size());

//        strings.forEach(System.out::println);
//        ForkJoinPool forkJoinPool = new ForkJoinPool();
//        ForkJoinTask<Integer> sum = forkJoinPool.submit(new SumTask(0, 100, 10));
//        try {
//            System.out.println(sum.get());
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        } catch (ExecutionException e) {
//            e.printStackTrace();
//        }
    }
}
