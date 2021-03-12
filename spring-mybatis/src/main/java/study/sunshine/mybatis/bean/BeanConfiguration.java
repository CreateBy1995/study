package study.sunshine.mybatis.bean;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @Author: dongcx
 * @Description:
 * @Date: 2021-01-29
 **/
@Configuration
public class BeanConfiguration {
    @Bean
    public BeanA beanA1(){
        BeanA beanA = new BeanA();
        beanA.setName("a1");
        return beanA;
    }
    @Bean
    public BeanA beanA2(){
        BeanA beanA = new BeanA();
        beanA.setName("a2");
        return beanA;
    }
}
