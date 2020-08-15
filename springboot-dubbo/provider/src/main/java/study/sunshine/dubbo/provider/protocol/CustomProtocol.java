package study.sunshine.dubbo.provider.protocol;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.rpc.Exporter;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Protocol;
import org.apache.dubbo.rpc.RpcException;
import study.sunshine.dubbo.provider.bean.CustomDubboIocBean;

/**
 * @Author: dongcx
 * @Description:
 * @Date: 2020-07-30
 **/
public class CustomProtocol implements Protocol {
    private Protocol protocol;
    private CustomDubboIocBean customDubboIocBean;
    @Override
    public int getDefaultPort() {
        return 0;
    }
    public void setProtocol(Protocol protocol){
        this.protocol= protocol;
    }
    public void setCustomDubboIocBean(CustomDubboIocBean customDubboIocBean){
        this.customDubboIocBean = customDubboIocBean;
    }
    @Override
    public <T> Exporter<T> export(Invoker<T> invoker) throws RpcException {
        return null;
    }

    @Override
    public <T> Invoker<T> refer(Class<T> type, URL url) throws RpcException {
        System.out.println(protocol);
        return null;
    }

    @Override
    public void destroy() {

    }
}
