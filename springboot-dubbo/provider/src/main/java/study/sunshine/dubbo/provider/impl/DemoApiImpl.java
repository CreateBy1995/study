package study.sunshine.dubbo.provider.impl;

import com.alibaba.dubbo.config.annotation.Service;
import study.sunshine.dubbo.commonapi.api.DemoApi;

/**
 * @Author: dongcx
 * @Description:
 * @Date: 2020-04-13
 **/
@Service(version = "0.1")
public class DemoApiImpl implements DemoApi {
    @Override
    public String getMessage(String msg) {
        return "provider: "+msg ;
    }
}
