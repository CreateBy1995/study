package strategy;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @Author: dongcx
 * @Description:
 * @Date: 2020-08-20
 **/
public class MainApplication {
    public static void main(String[] args) {
        List<TestObj> testObjs = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            TestObj testObj;
            if (i < 5) {
                testObj = new TestObj(1, i);
            } else {
                testObj = new TestObj(2, i);
            }
            testObjs.add(testObj);
        }
        System.out.println( testObjs.stream()
                .collect(Collectors.groupingBy(TestObj::getId,
                        Collectors.mapping(TestObj::getAge, Collectors.toList()))));
    }
}
