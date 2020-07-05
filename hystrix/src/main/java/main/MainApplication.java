package main;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.hystrix.EnableHystrix;

/**
 * @Author: dongcx
 * @Description:
 * @Date: 2020-05-20
 **/
@SpringBootApplication
@EnableHystrix
public class MainApplication {
    public static void main(String[] args) {
        String s1 = new String("n")+new String("m");
        s1.intern();
        String s2 ="nm";
        System.out.println(s1==s2); // true
//        String s1 = new String("nm");
//        s1.intern();
//        String s2 ="nm";
//        System.out.println(s1==s2); // false
//        ConfigurableApplicationContext ioc = SpringApplication.run(MainApplication.class);
//        CustomConsumer customConsumer = ioc.getBean(CustomConsumer.class);
//        CustomProvider customProvider = ioc.getBean(CustomProvider.class);
//        customConsumer.testConsumer(customProvider);
//        try {
//            TimeUnit.SECONDS.sleep(5);
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }
//        System.out.println("start");
//        System.out.println("main  --- "+customProvider.testMethod());
    }
}
