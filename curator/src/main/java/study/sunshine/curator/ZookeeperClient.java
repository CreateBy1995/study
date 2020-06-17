package study.sunshine.curator;

import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;

/**
 * @Author: dongcx
 * @Description:
 * @Date: 2020-06-17
 **/
public class ZookeeperClient {
    public static CuratorFramework client;
    private final static String CONNECT_STRING = "127.0.0.1:2181";
    private final static String NAME_SPACE = "testZkClient";

    public static CuratorFramework getZookeeperClient() {
        RetryPolicy retryPolicy = new ExponentialBackoffRetry(1000, 3);
        client = CuratorFrameworkFactory.builder()
                .connectString(CONNECT_STRING)
                .sessionTimeoutMs(5000)
                .connectionTimeoutMs(5000)
                .retryPolicy(retryPolicy)
                .namespace(NAME_SPACE)
                .build();
        return client;
    }
}
