package study.sunshine.dubbo.commonapi.api;

import study.sunshine.dubbo.commonapi.dto.TestDTO;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * @Author: dongcx
 * @Description:
 * @Date: 2020-09-11
 **/
public class DemoApiMock implements DemoApi {
    @Override
    public String getMessage(String msg) {
        return null;
    }

    @Override
    public Map getMap(String msg) {
        return null;
    }

    @Override
    public void test() {
        System.out.println("test mock");
    }

//    @Override
//    public String testDTO(@NotNull TestDTO2 testDTO) {
//        return null;
//    }

//
    @Override
    public String testDTO(TestDTO testDTO) {
        return null;
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
