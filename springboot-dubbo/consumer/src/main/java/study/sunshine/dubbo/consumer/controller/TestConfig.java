package study.sunshine.dubbo.consumer.controller;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @Author: dongcx
 * @Description:
 * @Date: 2021-02-19
 **/
@Configuration
public class TestConfig {
    @Bean
    public BeanA beanA1(){
        BeanA beanA = new BeanA();
        beanA.setName("1");
        return beanA;
    }
    @Bean
    public BeanA beanA2(){
        BeanA beanA = new BeanA();
        beanA.setName("2");
        return beanA;
    }
    @Bean
    public BeanB beanB(BeanA beanA1){
        System.out.println(beanA1);
        return new BeanB();
    }

}
