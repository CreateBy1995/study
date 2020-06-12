package main.func;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @Author: dongcx
 * @Description:
 * @Date: 2020-05-22
 **/
public class StreamDemo {
    private List<Integer> integerList = new ArrayList<>();
    private Map<String,List<Integer>> flatMap = new HashMap<>();
    public StreamDemo(){
        for (int i = 0; i < 10; i++) {
            integerList.add(i);
        }
        flatMap.put("a",integerList);
        flatMap.put("b",integerList);
        flatMap.put("c",integerList);
    }
    public void test0001(){
       Integer num = integerList.stream().filter(item -> item <  5).findFirst().orElse(10);
        System.out.println(num);
    }
    public void test0002(){
        Boolean flag = integerList.stream().anyMatch(item -> item <  -1);
        System.out.println(flag);
    }
    public void test0003(){
        Integer num = integerList.stream().findAny().orElse(111);
        System.out.println(num);
    }
    public void test0004(){
        Integer size = flatMap.values().stream().flatMap(Collection::stream).collect(Collectors.toList()).size();
        System.out.println(size);
    }
    public void test0005(){
    }
}
