package study.sunshine.io.nioserver.thread;

import lombok.AllArgsConstructor;
import study.sunshine.io.nioserver.server.EventList;
import study.sunshine.io.nioserver.server.IOEvent;

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
        while (true){
            // 从任务队列中获取就绪的socketChannel
            IOEvent ioEvent = eventList.getEvent();
            // 通过读写类型进行不同的操作
            if(ioEvent != null){
                switch (ioEvent.getEventType()){
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
    public void read(IOEvent ioEvent){
    }

    /**
     * 写操作
     */
    public void write(IOEvent ioEvent){

    }
}
