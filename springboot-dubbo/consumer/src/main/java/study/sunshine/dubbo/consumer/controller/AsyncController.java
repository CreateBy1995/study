package study.sunshine.dubbo.consumer.controller;

import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.Reference;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import study.sunshine.dubbo.commonapi.api.AsyncApi;

import java.util.concurrent.CompletableFuture;

import static study.sunshine.dubbo.commonapi.constant.DubboVersion.VERSION_1;

/**
 * @Author: dongcx
 * @Description:
 * @Date: 2020-08-05
 **/
@Slf4j
@RestController
@RequestMapping("/async")
public class AsyncController {
    @Reference(version = VERSION_1, timeout = 3000)
    private AsyncApi asyncApi;
    @GetMapping("/result")
    public String getResult() {
        log.info("异步调用开始");
        CompletableFuture<String> completableFuture = asyncApi.getAsyncResult();
        completableFuture.whenComplete((result, exception) -> {
            if (exception != null) {
                log.error("异步调用出错", exception);
            } else {
                log.info("异步调用完成");
            }
        });
        log.info("模拟业务操作");
        return HttpStatus.OK.getReasonPhrase();
    }
}
