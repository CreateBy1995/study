package study.sunshine.dubbo.consumer.utils;

import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.common.utils.StringUtils;

import java.util.concurrent.TimeUnit;

/**
 * @Author: dongcx
 * @Description:
 * @Date: 2020-07-03
 **/
@Slf4j
public class ThreadUtil {
    public static void sleep(int time) {
        try {
            log.info("current thread {} sleep", Thread.currentThread().getName());
            TimeUnit.SECONDS.sleep(time);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static void sleep(double time) {
        try {
            log.info("current thread {} sleep", Thread.currentThread().getName());
            TimeUnit.MILLISECONDS.sleep((long) time * 1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static void print(String msg){
        StackTraceElement stack[] = Thread.currentThread().getStackTrace();
        if(StringUtils.isNotEmpty(msg)){
            log.info("current thread {} , {} ",Thread.currentThread().getName(),msg);
        }else{
            log.info("current thread {} execute {} method ",Thread.currentThread().getName(),stack[2].getMethodName());
        }

    }
}
