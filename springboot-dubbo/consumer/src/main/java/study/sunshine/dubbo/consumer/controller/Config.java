package study.sunshine.dubbo.consumer.controller;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * @Author: dongcx
 * @Description:
 * @Date: 2020-08-08
 **/
@Configuration
@EnableAsync
public class Config {
    @Bean("customThreadPool")
    public ThreadPoolTaskExecutor customThreadPool(){
        ThreadPoolTaskExecutor threadPoolTaskExecutor = new ThreadPoolTaskExecutor();
        threadPoolTaskExecutor.setMaxPoolSize(1);
        threadPoolTaskExecutor.setCorePoolSize(1);
        threadPoolTaskExecutor.setQueueCapacity(1);
        threadPoolTaskExecutor.setThreadNamePrefix("custom222-");
//        threadPoolTaskExecutor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        return threadPoolTaskExecutor;
    }
    @Bean("customThreadPool2")
    public ThreadPoolTaskExecutor customThreadPool2(){
        ThreadPoolTaskExecutor threadPoolTaskExecutor = new ThreadPoolTaskExecutor();
        threadPoolTaskExecutor.setMaxPoolSize(1);
        threadPoolTaskExecutor.setCorePoolSize(1);
        threadPoolTaskExecutor.setQueueCapacity(100);
        threadPoolTaskExecutor.setThreadNamePrefix("custom333-");
//        threadPoolTaskExecutor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        return threadPoolTaskExecutor;
    }
}
