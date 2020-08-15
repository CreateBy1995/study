package study.sunshine.dubbo.provider.impl;

import org.apache.dubbo.config.annotation.Service;
import study.sunshine.dubbo.commonapi.api.SyncApi;

/**
 * @Author: dongcx
 * @Description:
 * @Date: 2020-08-13
 **/
@Service(timeout = 3000, retries = 1)
public class SyncApiImpl implements SyncApi {
    @Override
    public String getSyncResult(String result, Integer version) {
        return "provider-" + result + version;
    }
}
