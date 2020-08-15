package study.sunshine.dubbo.consumer;

import lombok.extern.slf4j.Slf4j;
import study.sunshine.dubbo.commonapi.api.DemoApi;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * @Author: dongcx
 * @Description:
 * @Date: 2020-08-04
 **/
@Slf4j
public class DemoApiStub implements DemoApi {
    private DemoApi demoApi;

    public DemoApiStub(DemoApi demoApi) {
        this.demoApi = demoApi;
    }

    @Override
    public String getMessage(String msg) {
        log.info("invoke by stub");
        String result = null;
        try {
            result = demoApi.getMessage(msg);
            System.out.println(result);
        } catch (Exception e) {
            log.error("stub invoke occur error",e);
        }
        return result;
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
