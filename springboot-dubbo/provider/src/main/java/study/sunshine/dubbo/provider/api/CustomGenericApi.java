package study.sunshine.dubbo.provider.api;

/**
 * @Author: dongcx
 * @Description: dubbo泛化调用示例
 * 泛化调用的场景 一般情况下我们需要通过RPC调用接口提供方的服务，首先在消费端嵌入提供方的Jar包，
 * 从而使用Jar包中的类和方法。那对于网关服务来说，如果一个网关调用了N个服务，那就需要引入N个Jar依赖，
 * 这样网关系统难以维护
 * @Date: 2020-07-24
 **/
public interface CustomGenericApi {
    void printInfo(String info);
}
