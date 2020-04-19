package study.sunshine.dubbo.consumer.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import study.sunshine.dubbo.commonapi.api.DemoApi;

/**
 * @Author: dongcx
 * @Description:
 * @Date: 2020-04-13
 **/
@RestController
@RequestMapping("/consumer")
public class ConsumerController {
    @Reference
    private DemoApi demoApi ;
    @GetMapping("/getMessage")
    public String getMessage(String msg){
        return demoApi.getMessage(msg) ;
    }
}
