package study.sunshine.io.nioserver.server;

import java.util.concurrent.LinkedBlockingQueue;

/**
 * @Author: dongcx
 * @Description:
 * @Date: 2020-05-10
 **/
public class EventList {
    private LinkedBlockingQueue<IOEvent> queue;
    public EventList(Integer capacity){
        queue = new LinkedBlockingQueue<>(capacity);
    }
    public void addEvent(IOEvent ioEvent){
        try {
            queue.put(ioEvent);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
    public IOEvent getEvent(){
        IOEvent ioEvent = null;
        try {
            ioEvent = queue.take();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return ioEvent;
    }
}
