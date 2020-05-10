package study.sunshine.io.nioclient.client;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;

/**
 * @Author: dongcx
 * @Description:
 * @Date: 2020-05-04
 **/
public class NioClient {
    public static void initial() throws IOException {
        // 开启一个客户端通道
        SocketChannel socketChannel = SocketChannel.open(new InetSocketAddress("127.0.0.1",90)) ;
        Selector selector = Selector.open();
        // 创建一个存放48个字节的缓冲区
        ByteBuffer byteBuffer = ByteBuffer.allocate(48) ;
        byteBuffer.put("Hello".getBytes());
        socketChannel.configureBlocking(false);
        socketChannel.register(selector, SelectionKey.OP_READ);
        socketChannel.write(byteBuffer);
        // 通知服务器数据传输完毕
        socketChannel.shutdownOutput() ;
        // 获取服务端响应
//        int len ;
        while (selector.select() > 0){
            // 获取所有准备就绪的事件
            // selector.selectedKeys()方法返回的是 publicSelectedKeys  表示所有已经准备就绪的的
            Iterator<SelectionKey> iterator = selector.selectedKeys().iterator() ;
            while (iterator.hasNext()){
                SelectionKey selectionKey = iterator.next();
                // 如果准备好的事件是 客户端连接事件
                if(selectionKey.isReadable()){
                    // 获取通道并打印读取到的数据
                    SocketChannel socketChannel1 = (SocketChannel) selectionKey.channel();
                    ByteBuffer byteBuffer1 = ByteBuffer.allocate(1024) ;
                    // 此处的socketChannel.read方法是不会阻塞的
                    // 也就是说尽管读取到的数据为空  该方法也会返回
                    // 比如在客户端要发送数据的时候打个断点就能看到效果
                    int i ;
                    while ((i=socketChannel.read(byteBuffer)) > 0){
                        byteBuffer.flip();
                        String data = new String(byteBuffer.array(),0,byteBuffer.limit()) ;
                        System.out.println("read the client "+socketChannel.getRemoteAddress()+" data : "+data);
                        byteBuffer.clear();
                    }
                    // 当客户端主动切断连接的时候，服务端 Socket 的读事件（FD_READ）仍然起作用，也就是说，服务端 Socket 的状态仍然是有东西可读
                    // socketChannel.read(buffer) 是有返回值的，这种情况下返回值是 -1，所以如果 read 方法返回的是 -1，就可以关闭和这个客户端的连接了。
                    // 如果不关闭 则selector.select()每次都会检测到这个关闭的客户端的 读事件 仍然可用  但是socketChannel.read(byteBuffer)始终为-1
                    // 导致服务器一直进行轮询
                    // 这点与在linux下的epoll空轮询bug有点类似  最终都会导致CPU占用率上升
                    if (i == -1){
                        socketChannel.close();
                    }
                }
                // 每次处理完SelectionKey  都将其清除  否则会一直获取到处理过的SelectionKey
                // 因为publicSelectedKeys是一个set select()方法底层会将准备完毕的key一直塞到这个set中
                // 比如有一个客户端连接事件已经处理完毕了  而没有将其清除掉 然后又有一个客户端连接事件触发 那么这时候publicSelectedKeys会在增加一个准备完毕的key
                // 此时在调用serverSocketChannel.accept()方法的时候 由于是非阻塞模式
                // 尽管没有客户端连接也会立即返回null，那么后续的代码就会出错
                iterator.remove();
            }
        }

//        while ((len=socketChannel.read(byteBuffer))!=-1){
//            // 切换成读取模式
//            byteBuffer.flip() ;
//            // 打印服务端响应的信息
//            System.out.println("received msg --> "+new String(byteBuffer.array(),0,len));
//        }
        // 关闭资源
        socketChannel.close();
    }
}
