package study.sunshine.io.nioserver.server;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.nio.channels.SocketChannel;

/**
 * @Author: dongcx
 * @Description:
 * @Date: 2020-05-10
 **/
@Data
@AllArgsConstructor
public class IOEvent {
    private SocketChannel channel;
    private EventType eventType;
}
