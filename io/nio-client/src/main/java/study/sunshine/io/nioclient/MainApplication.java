package study.sunshine.io.nioclient;

import study.sunshine.io.nioclient.client.NioClient;

import java.io.IOException;

/**
 * @Author: dongcx
 * @Description:
 * @Date: 2020-05-04
 **/
public class MainApplication {
    public static void main(String[] args) {
        try {
            NioClient.initial();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
