package main.controller;

import main.bean.Person;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @Author: dongcx
 * @Description:
 * @Date: 2020-05-20
 **/
@RestController
public class ValidationController {
    /**
     * 在springmvc为我们生成一个参数对象(person),并且填充了请求中的参数之后
     * ModelAttributeMethodProcessor会去判断这个对象是否被Validated注解修饰
     * 如果被修饰会调用验证器去验证person对象是否合格
     *
     * 所以说流程是 spring先为我们创建一个对象，优先调用无参构造函数(如果没有就调用有参构造，这样就会导致属性的默认值失效)
     * 然后将请求中对应的属性参数绑定到这个这个对象上  最后调用验证器验证这个属性是否合格，不合格就抛出异常，也就不来调用这个test方法 (因为这是发生在参数解析阶段)
     */

    @GetMapping("/test")
    public Integer test(@Validated Person person){
        System.out.println(person);
        return person.getAge();
    }
}
