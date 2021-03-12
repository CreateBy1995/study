package study.sunshine.dubbo.consumer.controller;

import lombok.Data;

/**
 * @Author: dongcx
 * @Description:
 * @Date: 2021-02-19
 **/
@Data
public class BeanA {
    private String name;
    public void sayHello(){
        System.out.println("sssdfdsfds");
    }
}
