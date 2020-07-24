package study.sunshine.dubbo.provider.impl;

import org.apache.dubbo.config.annotation.Method;
import org.apache.dubbo.config.annotation.Service;
import study.sunshine.dubbo.commonapi.api.DemoApi;

/**
 * @Author: dongcx
 * @Description:
 * @Date: 2020-04-13
 **/
@Service(version = "1.0", executes = 15, retries = 3, timeout = 5000, methods = {
        @Method(name = "getMessage", retries = 2)})
public class DemoApiImpl implements DemoApi {
    @Override
    public String getMessage(String msg) {
        System.out.println(Thread.currentThread().getName() + " provider: " + msg);
        System.out.println(10/0);
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

}
