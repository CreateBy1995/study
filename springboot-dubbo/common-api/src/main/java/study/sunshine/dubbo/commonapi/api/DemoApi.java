package study.sunshine.dubbo.commonapi.api;

import study.sunshine.dubbo.commonapi.dto.TestDTO;

import javax.validation.constraints.NotNull;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public interface DemoApi {
    String getMessage(String msg);
    Map getMap(String msg);
    void test();
    String testDTO(@NotNull TestDTO testDTO);
    String getAsyncResult();
    CompletableFuture<String> getAsyncResultWithFuture();
}
