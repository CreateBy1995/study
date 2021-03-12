package study.sunshine.mybatis.bean;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @Author: dongcx
 * @Description:
 * @Date: 2021-01-29
 **/
@RestController
public class TestController {
    @Autowired
    private BeanA beanA1;
    @Autowired
    private BeanConfiguration beanConfiguration;
    @GetMapping("/test1")
    public void test1(){
        System.out.println(beanA1);
    }
}
