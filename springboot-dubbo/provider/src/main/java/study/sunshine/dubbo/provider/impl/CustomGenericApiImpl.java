package study.sunshine.dubbo.provider.impl;

import study.sunshine.dubbo.provider.api.CustomGenericApi;

/**
 * @Author: dongcx
 * @Description:
 * @Date: 2020-07-24
 **/
//@Service
public class CustomGenericApiImpl implements CustomGenericApi {
    @Override
    public void printInfo(String info) {
        System.out.println(info);
    }
}
