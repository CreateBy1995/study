package main.bean;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.ToString;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

/**
 * @Author: dongcx
 * @Description:
 * @Date: 2020-05-20
 **/
@Data
@ToString
@AllArgsConstructor
public class Person {
    @NotNull
    private String name = "11";
    @NotNull
    private Integer sex = 5;
    @Min(value = 10)
    @Max(value = 30)
    private Integer age;

}
