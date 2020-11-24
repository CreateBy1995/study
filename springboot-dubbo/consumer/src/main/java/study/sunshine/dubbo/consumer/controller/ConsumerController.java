package study.sunshine.dubbo.consumer.controller;

import org.apache.dubbo.config.ApplicationConfig;
import org.apache.dubbo.config.ReferenceConfig;
import org.apache.dubbo.config.RegistryConfig;
import org.apache.dubbo.config.annotation.Method;
import org.apache.dubbo.config.annotation.Reference;
import org.apache.dubbo.rpc.service.GenericService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import study.sunshine.dubbo.commonapi.api.DemoApi;
import study.sunshine.dubbo.commonapi.api.ProviderApi;

/**
 * @Author: dongcx
 * @Description:
 * @Date: 2020-04-13
 **/
@RestController
@RequestMapping("/consumer")
public class ConsumerController {
    //    @Reference(version = "1.0",retries = 1,mock = "study.sunshine.dubbo.consumer.mock.DemoApiMock")
    @Reference(version = "1.0", retries = 0,
            timeout = 1000,
            mock = "default", methods = {
            @Method(name = "getMessage",actives = 3,retries = 0),
            @Method(name = "getAsyncResult",async = true),
            @Method(name = "getAsyncResultWithFuture",async = true)
    }
    )

    private DemoApi demoApi;
    @Reference(version = "2.0", group = "sunshineGroup")
    private ProviderApi providerApi;

    @GetMapping("/getMessage")
    public String getMessage(String msg) {
//        demoApi.getAsyncResultWithFuture().whenComplete((o,t)->{
//            System.out.println("done");
//            System.out.println(o);
//            System.out.println(t);
//        });
//        try {
//            System.out.println("sleep");
//            TimeUnit.SECONDS.sleep(3);
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }
       ;
//        for (int i = 0; i < 10 ; i++) {
//            new Thread(()->{
//                demoApi.getMessage(msg);
//            }).start();
//        }
        demoApi.test();
        return"ss";
//        return  demoApi.getMessage(msg);
//        return demoApi.getMessage(msg);
    }

    @GetMapping("/getVersion1")
    public String getVersion1() {
        return providerApi.getVersion("1");
    }

    @GetMapping("/getVersion2")
    public String getVersion2() {
        return providerApi.getVersion("2", "3");
    }

    /**
     * 测试泛化调用
     */
    @GetMapping("/getGeneric")
    public void getGeneric() {
        // 流程大概就是创建一个要调用的服务对应的ReferenceConfig
        // 然后调用ReferenceConfig.get方法用于引用服务
        // 最后就可以直接调用了  泛化的好处就在于消费者可以完全不用依生产者的jar包(比如接口、pojo等)
        ApplicationConfig application = new ApplicationConfig();
        application.setName("gateway-test");

        RegistryConfig registry = new RegistryConfig();
        registry.setAddress("zookeeper://127.0.0.1:2181");

        application.setRegistry(registry);

        ReferenceConfig<GenericService> reference = new ReferenceConfig<GenericService>();
        reference.setApplication(application);
        reference.setInterface("study.sunshine.dubbo.provider.api.CustomGenericApi");
        reference.setGeneric(true);

        GenericService genericService = reference.get();
        // 要调用的方法名、参数类型列表、参数值列表
        // 如果返回的是pojo类型，那么会自动转成Map
        Object result = genericService.$invoke("printInfo", new String[]{"java.lang.String"},
                new Object[]{"hello"});

        System.out.println(result);

    }
}
