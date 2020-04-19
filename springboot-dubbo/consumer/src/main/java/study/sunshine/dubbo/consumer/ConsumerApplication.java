package study.sunshine.dubbo.consumer;

import com.alibaba.dubbo.config.spring.context.annotation.EnableDubbo;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * @Author: dongcx
 * @Description:
 * @Date: 2020-04-13
 **/
@SpringBootApplication
@EnableDubbo
public class ConsumerApplication {
    public static void main(String[] args) {
        SpringApplication.run(ConsumerApplication.class) ;
    }
}
