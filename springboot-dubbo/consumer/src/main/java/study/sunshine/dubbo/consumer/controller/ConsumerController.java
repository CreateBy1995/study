package study.sunshine.dubbo.consumer.controller;

import org.apache.dubbo.config.annotation.Method;
import org.apache.dubbo.config.annotation.Reference;
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
    @Reference(version = "1.0",retries = 1,actives = 2,methods = {@Method(name = "getMessage",actives = 15)})
    private DemoApi demoApi ;
    @Reference(version = "2.0")
    private ProviderApi providerApi ;
    @GetMapping("/getMessage")
    public String getMessage(String msg){
//        demoApi.test();
//        return null;
        return demoApi.getMessage(msg) ;
    }
    @GetMapping("/getVersion1")
    public String getVersion1(){
        return providerApi.getVersion("1") ;
    }
    @GetMapping("/getVersion2")
    public String getVersion2(){
        return providerApi.getVersion("2","3") ;
    }
}
