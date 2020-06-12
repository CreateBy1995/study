package main.hystrix;

import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand;
import com.netflix.hystrix.contrib.javanica.annotation.HystrixProperty;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * @Author: dongcx
 * @Description:
 * @Date: 2020-05-20
 **/
@Component
public class CustomProvider {
    private final static Integer SUCCESS = 200;
    private final static Integer FAILED = 500;
    @HystrixCommand(fallbackMethod = "testMethodFallback",
            commandProperties = {
                    @HystrixProperty(name = "execution.isolation.strategy", value = "SEMAPHORE"),
                    @HystrixProperty(name = "execution.isolation.semaphore.maxConcurrentRequests", value = "5"),
                    @HystrixProperty(name = "execution.isolation.thread.timeoutInMilliseconds", value = "2000"),
                    @HystrixProperty(name = "execution.timeout.enabled", value = "false"),

                    @HystrixProperty(name = "circuitBreaker.errorThresholdPercentage", value = "10"),
                    @HystrixProperty(name = "circuitBreaker.sleepWindowInMilliseconds", value = "30000"),
                    @HystrixProperty(name = "circuitBreaker.requestVolumeThreshold", value = "5")
            })
    public Integer testMethod(){
        try {
            TimeUnit.SECONDS.sleep(4);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return SUCCESS;
    }
    private Integer testMethodFallback(){
        return FAILED;
    }
}
