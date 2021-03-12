package study.sunshine.dubbo.provider.impl;

import org.apache.dubbo.config.annotation.Service;
import study.sunshine.dubbo.commonapi.api.DemoApi;
import study.sunshine.dubbo.commonapi.dto.TestDTO;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * @Author: dongcx
 * @Description:
 * @Date: 2020-04-13
 **/
@Service(version = "1.0",executes = 10,retries = 3,timeout = 1000)
public class DemoApiImpl implements DemoApi {
    @Override
    public String getMessage(String msg) {
        System.out.println(Thread.currentThread().getName() +" provider: "+msg);
        try {
            TimeUnit.SECONDS.sleep(3);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return "provider: "+msg ;
    }

    @Override
    public Map getMap(String msg) {
        return null;
    }

    @Override
    public void test() {

    }

    @Override
    public String testDTO(TestDTO testDTO) {
        Integer age = testDTO.getSex();
        System.out.println(age);
        return testDTO.toString();
    }
//    @Override
//    public String testDTO() {
//        return "sss";
//    }
    @Override
    public String getAsyncResult() {
        return null;
    }

    @Override
    public CompletableFuture<String> getAsyncResultWithFuture() {
        return null;
    }
}
