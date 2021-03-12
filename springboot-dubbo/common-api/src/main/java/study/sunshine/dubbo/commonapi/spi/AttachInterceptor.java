package study.sunshine.dubbo.commonapi.spi;

import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.RpcContext;
import org.apache.dubbo.rpc.cluster.interceptor.ClusterInterceptor;
import org.apache.dubbo.rpc.cluster.support.AbstractClusterInvoker;

import static org.apache.dubbo.common.constants.CommonConstants.CONSUMER;

/**
 * @Author: dongcx
 * @Description:
 * @Date: 2021-01-21
 **/
@Activate(group = {CONSUMER}, order = Integer.MAX_VALUE)
@Slf4j
public class AttachInterceptor implements ClusterInterceptor {
    @Override
    public void before(AbstractClusterInvoker<?> clusterInvoker, Invocation invocation) {
        RpcContext.getContext().setAttachment("dubbo.tag","1995");
    }

    @Override
    public void after(AbstractClusterInvoker<?> clusterInvoker, Invocation invocation) {

    }
}
