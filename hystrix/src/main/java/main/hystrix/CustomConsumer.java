package main.hystrix;

import org.springframework.stereotype.Component;

/**
 * @Author: dongcx
 * @Description:
 * @Date: 2020-05-20
 **/
@Component
public class CustomConsumer {
    public void testConsumer(CustomProvider customProvider){
        for (int i = 0; i < 10; i++) {
            new Thread(()->{
                Long now = System.currentTimeMillis();
                System.out.println("start ---"+now);
                System.out.println(customProvider.testMethod());
                System.out.println("start ---"+(now-System.currentTimeMillis()));
            }).start();
        }
    }
}
