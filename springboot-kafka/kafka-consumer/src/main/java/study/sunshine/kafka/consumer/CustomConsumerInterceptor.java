package study.sunshine.kafka.consumer;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerInterceptor;
import org.apache.kafka.clients.consumer.ConsumerRecords;

import java.util.Map;

/**
 * @Author: dongcx
 * @Description:
 * @Date: 2020-05-14
 **/
@Slf4j
public class CustomConsumerInterceptor implements ConsumerInterceptor {
    @Override
    public ConsumerRecords onConsume(ConsumerRecords records) {
        return records;
    }

    @Override
    public void close() {
        log.info("close");
    }
    public void test(String msg) {
        log.error("close = {}",msg);
    }
    @Override
    public void onCommit(Map offsets) {
        log.info("onCommit");
    }

    @Override
    public void configure(Map<String, ?> configs) {
        log.info("onCommit");
    }
}
