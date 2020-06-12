package study.sunshine.dubbo.provider;

import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * @Author: dongcx
 * @Description:
 * @Date: 2020-04-13
 **/
@SpringBootApplication
@EnableDubbo
public class ProviderApplication {
    public static void main(String[] args) {
//        TestSpi testSpi =ExtensionLoader.getExtensionLoader(TestSpi.class).getAdaptiveExtension();
//        System.out.println(testSpi);
        SpringApplication.run(ProviderApplication.class);
    }
}
