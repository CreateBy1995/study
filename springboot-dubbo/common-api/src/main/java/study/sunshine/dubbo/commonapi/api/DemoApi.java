package study.sunshine.dubbo.commonapi.api;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

public interface DemoApi {
    String getMessage(String msg);
    Map getMap(String msg);
    void test();
    String getAsyncResult();
    CompletableFuture<String> getAsyncResultWithFuture();
}
