package study.sunshine.dubbo.consumer.mock;

import study.sunshine.dubbo.commonapi.api.DemoApi;

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
}
