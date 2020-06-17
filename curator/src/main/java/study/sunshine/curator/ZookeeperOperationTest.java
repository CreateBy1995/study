package study.sunshine.curator;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.cache.NodeCache;
import org.apache.zookeeper.CreateMode;

/**
 * @Author: dongcx
 * @Description:
 * @Date: 2020-06-17
 **/
public class ZookeeperOperationTest {
    private static CuratorFramework zookeeperClient = ZookeeperClient.getZookeeperClient();

    public static void main(String[] args) throws Exception {
        zookeeperClient.start();
//        // TODO 创建一个指定类型的节点,名称为test 初始内容为空
//        zookeeperClient.create().withMode(CreateMode.PERSISTENT).forPath("/test");
        zookeeperClient.delete().deletingChildrenIfNeeded().forPath("/test1");
        // TODO 创建一个指定类型的节点,名称为test1 初始内容为init
        zookeeperClient.create().withMode(CreateMode.PERSISTENT).forPath("/test1", "init".getBytes());
//        zookeeperClient.create().withMode(CreateMode.PERSISTENT).forPath("/test1/test2","init".getBytes());
//        // TODO 删除一个节点，只能删除叶子节点 否则抛出异常
//        zookeeperClient.delete().forPath("/test1");
        // TODO 递归删除指定节点及其子节点
//        zookeeperClient.delete().deletingChildrenIfNeeded().forPath("/test1");
        // TODO 引入curator-recipes使用其封装的watcher方法 使用这个方法会反复触发监听
        NodeCache nodeCache = new NodeCache(zookeeperClient, "/test1");
        nodeCache.start();
        nodeCache.getListenable().addListener(() -> {
            System.out.println("监听事件触发");
            System.out.println("重新获得节点内容为：" + new String(nodeCache.getCurrentData().getData()));
        });
        zookeeperClient.setData().forPath("/test1", "1111".getBytes());
        Thread.sleep(100);
        zookeeperClient.setData().forPath("/test1", "2222".getBytes());
        Thread.sleep(100);
        zookeeperClient.setData().forPath("/test1", "3333".getBytes());
        Thread.sleep(100);
        zookeeperClient.setData().forPath("/test1", "4444".getBytes());
        Thread.sleep(100);
        zookeeperClient.setData().forPath("/test1", "4444".getBytes());
        Thread.sleep(3000);
    }

}
