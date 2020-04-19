package study.sunshine.dubbo.provider;

import com.alibaba.dubbo.config.spring.context.annotation.EnableDubbo;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * @Author: dongcx
 * @Description:
 * @Date: 2020-04-13
 **/
@SpringBootApplication
@EnableDubbo
public class ProviderApplication {
    public static void main(String[] args) {
        ConfigurableApplicationContext ioc =
                SpringApplication.run(ProviderApplication.class) ;
    }
}
