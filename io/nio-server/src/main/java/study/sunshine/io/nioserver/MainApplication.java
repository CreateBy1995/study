package study.sunshine.io.nioserver;

import study.sunshine.io.nioserver.factory.NetFactory;
import study.sunshine.io.nioserver.server.NetManager;

/**
 * @Author: dongcx
 * @Description:
 * @Date: 2020-05-04
 **/
public class MainApplication {
    public static void main(String[] args) {
        NetManager netManager = NetFactory.instanceNetManager();
        netManager.initial();
    }
}
