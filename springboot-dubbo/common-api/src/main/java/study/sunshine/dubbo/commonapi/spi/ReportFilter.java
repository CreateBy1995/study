package study.sunshine.dubbo.commonapi.spi;

import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.rpc.Filter;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Result;
import org.apache.dubbo.rpc.RpcContext;
import org.apache.dubbo.rpc.RpcException;

import java.util.UUID;

import static org.apache.dubbo.common.constants.CommonConstants.CONSUMER;
import static org.apache.dubbo.common.constants.CommonConstants.PROVIDER;

/**
 * @Author: dongcx
 * @Description: 自定义过滤器  客户端、服务端都启用，order越小优先级越高
 * @Date: 2020-08-06
 **/
@Activate(group = {CONSUMER, PROVIDER}, order = Integer.MAX_VALUE)
@Slf4j
public class ReportFilter implements Filter, Filter.Listener {
    @Override
    public Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException {
        RpcContext rpcContext = RpcContext.getContext();
        if (rpcContext.isConsumerSide()) {
            // 客户端执行逻辑
            String traceId = UUID.randomUUID().toString();
            String serviceName = invocation.getServiceName();
            String invokeMethod = invocation.getMethodName();
            rpcContext.setAttachment("traceId", traceId);
            log.info("client sent request: {} - {}, traceId : {}", serviceName, invokeMethod, traceId);
        } else {
            // 服务端执行逻辑
            String traceId = rpcContext.getAttachment("traceId");
            String invokeMethod = invocation.getMethodName();
            log.info("server handle request: {}, traceId : {}", invokeMethod, traceId);
        }
        return invoker.invoke(invocation);
    }

    /**
     * 调用正常完成时回调
     */
    @Override
    public void onMessage(Result appResponse, Invoker<?> invoker, Invocation invocation) {
        RpcContext rpcContext = RpcContext.getContext();
        if (rpcContext.isConsumerSide()) {
            String serviceName = invocation.getServiceName();
            String invokeMethod = invocation.getMethodName();
            String traceId = rpcContext.getAttachment("traceId");
            log.info("client receive response: {} - {}, traceId : {}", serviceName, invokeMethod, traceId);
        } else {
            String traceId = rpcContext.getAttachment("traceId");
            String invokeMethod = invocation.getMethodName();
            log.info("server complete request: {}, traceId : {}", invokeMethod, traceId);
        }
    }

    /**
     * 调用异常结束时回调
     */
    @Override
    public void onError(Throwable t, Invoker<?> invoker, Invocation invocation) {
        RpcContext rpcContext = RpcContext.getContext();
        if (rpcContext.isConsumerSide()) {
            String serviceName = invocation.getServiceName();
            String invokeMethod = invocation.getMethodName();
            String traceId = rpcContext.getAttachment("traceId");
            log.error("client receive response occur error : {} - {}, traceId : {}", serviceName, invokeMethod, traceId, t);
        } else {
            String traceId = rpcContext.getAttachment("traceId");
            String invokeMethod = invocation.getMethodName();
            log.error("server complete request occur error: {}, traceId : {}", invokeMethod, traceId, t);
        }

    }
}
