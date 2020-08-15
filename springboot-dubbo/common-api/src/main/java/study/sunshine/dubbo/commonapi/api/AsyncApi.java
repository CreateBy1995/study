package study.sunshine.dubbo.commonapi.api;

import java.util.concurrent.CompletableFuture;

/**
 * 异步调用demo
 */
public interface AsyncApi {
    CompletableFuture<String> getAsyncResult();
}
