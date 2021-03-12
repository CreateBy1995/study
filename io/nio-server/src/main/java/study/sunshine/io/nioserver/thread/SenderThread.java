package study.sunshine.io.nioserver.thread;

import lombok.AllArgsConstructor;
import study.sunshine.io.nioserver.server.EventList;
import study.sunshine.io.nioserver.server.IOEvent;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

/**
 * @Author: dongcx
 * @Description: IO线程 从任务队列中获取就绪的socketChannel来进行读写操作
 * @Date: 2020-05-10
 **/
@AllArgsConstructor
public class SenderThread extends Thread {
    private EventList eventList;

    @Override
    public void run() {
        while (true) {
            // 从任务队列中获取就绪的socketChannel
            IOEvent ioEvent = eventList.getEvent();
            // 通过读写类型进行不同的操作
            if (ioEvent != null) {
                switch (ioEvent.getEventType()) {
                    case READ:
                        read(ioEvent);
                        break;
                    case WRITE:
                        write(ioEvent);
                        break;
                    default:
                }
            }
        }
    }

    /**
     * 读操作
     */
    public void read(IOEvent ioEvent) {
        SocketChannel socketChannel = ioEvent.getChannel();
        ByteBuffer byteBuffer = ByteBuffer.allocate(1024);
        try {
            while (socketChannel.read(byteBuffer) > 0) {
                byteBuffer.flip();
                String data = new String(byteBuffer.array(), 0, byteBuffer.limit());
                System.out.println("read the client" + socketChannel.getRemoteAddress() + " data : ");
                System.out.println(data);
                byteBuffer.clear();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }


    }

    /**
     * 写操作
     */
    public void write(IOEvent ioEvent) {

    }
}
