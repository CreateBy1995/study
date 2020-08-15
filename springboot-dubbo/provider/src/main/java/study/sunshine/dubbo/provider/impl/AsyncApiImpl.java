package study.sunshine.dubbo.provider.impl;

import org.apache.dubbo.config.annotation.Service;
import study.sunshine.dubbo.commonapi.api.AsyncApi;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static study.sunshine.dubbo.commonapi.constant.DubboVersion.VERSION_1;

/**
 * @Author: dongcx
 * @Description:
 * @Date: 2020-08-05
 **/
@Service(version = VERSION_1)
public class AsyncApiImpl implements AsyncApi {
    @Override
    public CompletableFuture<String> getAsyncResult() {
        CompletableFuture<String> completableFuture = CompletableFuture.supplyAsync(()->{
            try {
                TimeUnit.MILLISECONDS.sleep(5000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return "asyncResult";
        });
        return completableFuture;
    }
}
