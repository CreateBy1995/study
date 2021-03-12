package study.sunshine.mybatis.bean;

import lombok.Data;

/**
 * @Author: dongcx
 * @Description:
 * @Date: 2020-11-10
 **/
@Data
public class BeanA {
    public BeanA(){
        System.out.println("bbbb");
    }
    private String name;
}
