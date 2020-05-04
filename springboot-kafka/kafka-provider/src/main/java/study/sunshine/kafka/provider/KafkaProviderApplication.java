package study.sunshine.kafka.provider;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

/**
 * @Author: dongcx
 * @Description:
 * @Date: 2020-04-23
 **/
@SpringBootApplication
@EnableSwagger2
public class KafkaProviderApplication {
    public static void main(String[] args) {
        SpringApplication.run(KafkaProviderApplication.class) ;
    }
}
