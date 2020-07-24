package study.sunshine.dubbo.provider.test;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Result;
import org.apache.dubbo.rpc.RpcException;
import study.sunshine.dubbo.commonapi.api.DemoApi;

/**
 * @Author: dongcxz
 * @Description:
 * @Date: 2020-05-24
 **/
public class CustomInvoker implements Invoker<DemoApi> {
    @Override
    public Class<DemoApi> getInterface() {
        return DemoApi.class;
    }

    @Override
    public Result invoke(Invocation invocation) throws RpcException {
        return null;
    }

    @Override
    public URL getUrl() {
        return null;
    }

    @Override
    public boolean isAvailable() {
        return false;
    }

    @Override
    public void destroy() {

    }
}
