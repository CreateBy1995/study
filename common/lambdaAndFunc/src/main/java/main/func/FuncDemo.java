package main.func;

import java.util.function.Function;

/**
 * @Author: dongcx
 * @Description:
 * @Date: 2020-05-19
 **/
public class FuncDemo {
    public Integer method1(Function<String,Integer> function){
        Integer sum = 10;
        return sum + function.apply("5");

    }
}
