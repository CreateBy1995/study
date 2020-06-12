package main;

import main.hystrix.CustomConsumer;
import main.hystrix.CustomProvider;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.hystrix.EnableHystrix;
import org.springframework.context.ConfigurableApplicationContext;

import java.util.concurrent.TimeUnit;

/**
 * @Author: dongcx
 * @Description:
 * @Date: 2020-05-20
 **/
@SpringBootApplication
@EnableHystrix
public class MainApplication {
    public static void main(String[] args) {
        ConfigurableApplicationContext ioc = SpringApplication.run(MainApplication.class);
        CustomConsumer customConsumer = ioc.getBean(CustomConsumer.class);
        CustomProvider customProvider = ioc.getBean(CustomProvider.class);
        customConsumer.testConsumer(customProvider);
        try {
            TimeUnit.SECONDS.sleep(5);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println("start");
        System.out.println("main  --- "+customProvider.testMethod());
    }
}
