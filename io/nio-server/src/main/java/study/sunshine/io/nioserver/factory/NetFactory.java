package study.sunshine.io.nioserver.factory;

import study.sunshine.io.nioserver.server.EventList;
import study.sunshine.io.nioserver.server.NetManager;
import study.sunshine.io.nioserver.thread.SenderThread;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.Selector;

/**
 * @Author: dongcx
 * @Description:
 * @Date: 2020-05-10
 **/
public class NetFactory {
    public static NetManager instanceNetManager(){
        return instanceNetManager(80,1024);
    }
    /**
     *
     * @param port 服务端监听的端口
     * @return 返回一个网络管理器 {@link NetManager}
     */
    public static NetManager instanceNetManager(Integer port, Integer eventCapacity){
        InetSocketAddress socketAddress = new InetSocketAddress(port);
        EventList eventList = new EventList(eventCapacity);
        Thread ioThread = new SenderThread(eventList);
        Selector selector = null;
        try {
            selector = Selector.open();
        } catch (IOException e) {
            e.printStackTrace();
        }
        NetManager netManager = new NetManager(socketAddress,ioThread,selector,eventList);
        return netManager;
    }
}
