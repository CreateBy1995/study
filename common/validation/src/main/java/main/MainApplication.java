package main;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

import java.util.ArrayList;
import java.util.List;

/**
 * @Author: dongcx
 * @Description:
 * @Date: 2020-05-20
 **/
@SpringBootApplication
@EnableSwagger2
public class MainApplication {
    public static void main(String[] args) {
        List<Integer> lists = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            lists.add(i);
        }
        lists.stream().filter(item -> item > 2);
//        lists.forEach();
//        Person person =  new Person(null,255);
//        person.setName("name");
//        person.setAge(55);
//        Set<ConstraintViolation<Person>> result = Validation.buildDefaultValidatorFactory().getValidator().validate(person);
//        System.out.println(person);
//        System.out.println(result);

        SpringApplication.run(MainApplication.class);
    }
}
