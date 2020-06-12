package study.sunshine.dubbo.provider.impl;

import org.apache.dubbo.config.annotation.Service;
import study.sunshine.dubbo.commonapi.api.DemoApi;

/**
 * @Author: dongcx
 * @Description:
 * @Date: 2020-04-13
 **/
@Service(version = "1.0")
public class DemoApiImpl implements DemoApi {
    @Override
    public String getMessage(String msg) {
        return "provider: "+msg ;
    }
}
