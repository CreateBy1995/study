package study.sunshine.io.nioserver;

import study.sunshine.io.nioserver.server.NioServer;

import java.io.IOException;

/**
 * @Author: dongcx
 * @Description:
 * @Date: 2020-05-04
 **/
public class MainApplication {
    public static void main(String[] args) {
        try {
            NioServer.initial();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
