package study.sunshine.dubbo.consumer.mock;

import study.sunshine.dubbo.commonapi.api.DemoApi;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * @Author: dongcx
 * @Description:
 * @Date: 2020-07-03
 **/
public class DemoApiMock implements DemoApi {
    @Override
    public String getMessage(String msg) {
        return "failed to mock";
    }

    @Override
    public Map getMap(String msg) {
        return null;
    }

    @Override
    public void test() {

    }

    @Override
    public String getAsyncResult() {
        return null;
    }

    @Override
    public CompletableFuture<String> getAsyncResultWithFuture() {
        return null;
    }
}
