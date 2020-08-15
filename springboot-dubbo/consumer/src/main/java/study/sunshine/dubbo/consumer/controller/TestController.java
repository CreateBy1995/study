package study.sunshine.dubbo.consumer.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.RejectedExecutionException;

/**
 * @Author: dongcx
 * @Description:
 * @Date: 2020-08-08
 **/
@RestController
@RequestMapping("/test")
public class TestController {
    private static final int cpuNum =  Runtime.getRuntime().availableProcessors();
    @Autowired
    private AsI asI;
    @Autowired
    @Qualifier(value = "customThreadPool")
    private ThreadPoolTaskExecutor threadPoolTaskExecutor;
    @GetMapping("/test")
    public String getVersion2() {
        System.out.println("cpuNum cpuNum cpuNum" + cpuNum);
        for (int i = 0; i < 3; i++) {
            try {
                String string2 = asI.testAsync();
                System.out.println(string2);
            }catch (RejectedExecutionException e){
                System.out.println("调用主线程执行");
                asI.doTest();
            }

        }
        System.out.println("ok");
        return "ok";
    }
    @GetMapping("/test2")
    public String update() {
        System.out.println("before update");
        System.out.println(threadPoolTaskExecutor.getActiveCount());
        System.out.println(threadPoolTaskExecutor.getMaxPoolSize());
        threadPoolTaskExecutor.setMaxPoolSize(1);
        System.out.println("after update");
        System.out.println(threadPoolTaskExecutor.getActiveCount());
        System.out.println(threadPoolTaskExecutor.getMaxPoolSize());
        return "ok";
    }
}
