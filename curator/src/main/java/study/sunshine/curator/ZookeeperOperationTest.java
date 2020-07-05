package study.sunshine.curator;

import org.apache.curator.framework.CuratorFramework;

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
//        zookeeperClient.delete().deletingChildrenIfNeeded().forPath("/test1");
        // TODO 创建一个指定类型的节点,名称为test1 初始内容为init
//        zookeeperClient.create().withMode(CreateMode.PERSISTENT).forPath("/test1", "init".getBytes());
//        zookeeperClient.create().withMode(CreateMode.PERSISTENT).forPath("/test1/test2","init".getBytes());
//        zookeeperClient.create().creatingParentsIfNeeded().forPath("/dubbo/config/dubbo/dubbo.properties");
        zookeeperClient.setData().forPath("/dubbo/config/dubbo/dubbo.properties","dubbo.metadata-report.address=zookeeper://127.0.0.1:2181\ndubbo.registry.address=zookeeper://127.0.0.1:2181".getBytes());
//        zookeeperClient.setData().forPath("/dubbo/config/dubbo/dubbo.properties","sdfdsfsf".getBytes());
//        // TODO 删除一个节点，只能删除叶子节点 否则抛出异常
//        zookeeperClient.delete().forPath("/test1");
        // TODO 递归删除指定节点及其子节点
//        zookeeperClient.delete().deletingChildrenIfNeeded().forPath("/test1");
        // TODO 引入curator-recipes使用其封装的watcher方法 使用这个方法会反复触发监听
//        NodeCache nodeCache = new NodeCache(zookeeperClient, "/test1");
//        nodeCache.start();
//        nodeCache.getListenable().addListener(() -> {
//            System.out.println("监听事件触发");
//            System.out.println("重新获得节点内容为：" + new String(nodeCache.getCurrentData().getData()));
//        });
//        zookeeperClient.setData().forPath("/test1", "1111".getBytes());
//        Thread.sleep(100);
//        zookeeperClient.setData().forPath("/test1", "2222".getBytes());
//        Thread.sleep(100);
//        zookeeperClient.setData().forPath("/test1", "3333".getBytes());
//        Thread.sleep(100);
//        zookeeperClient.setData().forPath("/test1", "4444".getBytes());
//        Thread.sleep(100);
//        zookeeperClient.setData().forPath("/test1", "4444".getBytes());
//        Thread.sleep(3000);
        // TODO 使用这个方法watcher只会触发一次
//        zookeeperClient.getData().usingWatcher(new Watcher() {
//            @Override
//            public void process(WatchedEvent event) {
//                System.out.println("触发监听，事件为: "+event.getType()+" 节点为: "+event.getPath());
//            }
//        }).forPath("/test1");
//        Thread.sleep(200);
//        zookeeperClient.setData().forPath("/test1", "4444".getBytes());
//        Thread.sleep(200);
//        zookeeperClient.setData().forPath("/test1", "5555".getBytes());
//        Thread.sleep(200);
//        zookeeperClient.setData().forPath("/test1", "6666".getBytes());
//        Thread.sleep(5000);
        // TODO 只有子节点被创建或者删除才能触发
//        zookeeperClient.getChildren().usingWatcher(new Watcher() {
//            @Override
//            public void process(WatchedEvent event) {
//                System.out.println("触发监听，事件为: "+event.getType()+" 节点为: "+event.getPath());
//            }
//        }).forPath("/test1");
//        Thread.sleep(200);
////        zookeeperClient.setData().forPath("/test1/test2","init2222".getBytes());
////        zookeeperClient.delete().forPath("/test1/test2");
////        Thread.sleep(200);
//        zookeeperClient.create().withMode(CreateMode.EPHEMERAL).forPath("/test1/test3","init".getBytes());
//        Thread.sleep(2000);

    }

}
