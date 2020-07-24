package study.sunshine.dubbo.provider.impl;

import org.apache.dubbo.config.annotation.Method;
import org.apache.dubbo.config.annotation.Service;
import study.sunshine.dubbo.commonapi.api.DemoApi;

/**
 * @Author: dongcx
 * @Description:
 * @Date: 2020-04-13
 **/
@Service(version = "1.0",group = "ttt",executes = 15, retries = 3, timeout = 5000, actives = 5,methods = {
        @Method(name = "getMessage", retries = 2,actives = 3)})
public class DemoApiImpl implements DemoApi {
    public DemoApiImpl(){
        System.out.println("nmsl nmsl");
    }
    @Override
    public String getMessage(String msg) {
        System.out.println(Thread.currentThread().getName() + " provider: " + msg);
//        try {
//            TimeUnit.SECONDS.sleep(3);
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }
//        try {
//            System.out.println(msg.substring(0, 9999));
//        } catch (Exception e) {
//            msg = null;
//        }
//        try {
//            TimeUnit.SECONDS.sleep(10);
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }
        return "provider: " + msg;
    }

    @Override
    public void test() {
        System.out.println("test");
    }

}
