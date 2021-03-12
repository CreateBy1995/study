package study.sunshine.dubbo.provider.impl;

import org.apache.dubbo.config.annotation.Method;
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
 * //
 **/
@Service(version = "1.0", executes = 2, retries = 3,actives = 3,
        validation = "true",
        timeout = 5000,
        methods = {
        @Method(name = "getMessage", retries = 2,actives = 1, timeout = 876)})
public class DemoApiImpl implements DemoApi {
    @Override
    public String getMessage(String msg) {
        System.out.println(Thread.currentThread().getName() + " provider: " + msg);
        try {
            TimeUnit.MILLISECONDS.sleep(2700);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return "provider: " + msg;
    }

    @Override
    public Map getMap(String msg) {
        try {
            TimeUnit.MILLISECONDS.sleep(2700);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public void test() {
        System.out.println("test");
    }

    @Override
    public String testDTO(TestDTO testDTO) {
        System.out.println(testDTO.getSex());
        return testDTO.toString();
    }
    @Override
    public String getAsyncResult() {
        return "AsyncResult";
    }

    @Override
    public CompletableFuture<String> getAsyncResultWithFuture() {
        return CompletableFuture.supplyAsync(() -> "AsyncResultWithFuture");
    }

}
