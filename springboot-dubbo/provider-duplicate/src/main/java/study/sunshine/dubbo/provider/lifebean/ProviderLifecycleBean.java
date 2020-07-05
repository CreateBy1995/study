package study.sunshine.dubbo.provider.lifebean;

import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.common.context.Lifecycle;
import org.springframework.stereotype.Component;

/**
 * @Author: dongcx
 * @Description: 用于监听Dubbo的生命周期
 * @Date: 2020-05-23
 **/
@Component
@Slf4j
public class ProviderLifecycleBean implements Lifecycle {
    @Override
    public void initialize() throws IllegalStateException {
        log.info("ProviderLifecycleBean initialize");
    }

    @Override
    public void start() throws IllegalStateException {
        log.info("ProviderLifecycleBean start");
    }

    @Override
    public void destroy() throws IllegalStateException {
        log.info("ProviderLifecycleBean destroy");
    }
}
