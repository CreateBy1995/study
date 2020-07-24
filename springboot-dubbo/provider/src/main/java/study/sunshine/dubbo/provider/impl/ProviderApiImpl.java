package study.sunshine.dubbo.provider.impl;

import org.apache.dubbo.config.annotation.Service;
import study.sunshine.dubbo.commonapi.api.ProviderApi;

/**
 * @Author: dongcx
 * @Description:
 * @Date: 2020-06-14
 **/
@Service(version = "2.0",timeout = 2567)
public class ProviderApiImpl implements ProviderApi {
    @Override
    public String getVersion(String version) {
        System.out.println("vvvvvv");
        return "provider " + version;
    }

    @Override
    public String getVersion(String version, String test) {
        return "provider " + version + test;
    }
}
