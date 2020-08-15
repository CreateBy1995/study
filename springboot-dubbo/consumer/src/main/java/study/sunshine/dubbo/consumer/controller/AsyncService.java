package study.sunshine.dubbo.consumer.controller;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import study.sunshine.dubbo.consumer.utils.ThreadUtil;

/**
 * @Author: dongcx
 * @Description:
 * @Date: 2020-08-08
 **/
@Service("asI")
public class AsyncService implements AsI {
    @Async("customThreadPool")
    public String testAsync(){
        return doTest();
    }

    @Override
    public String doTest() {
        ThreadUtil.print("start");
        ThreadUtil.sleep(20);
        ThreadUtil.print("end");
        return "sss";
    }

    public String testSync(){
        ThreadUtil.print("start");
        ThreadUtil.sleep(3);
        ThreadUtil.print("end");
        return "sss";
    }
}
