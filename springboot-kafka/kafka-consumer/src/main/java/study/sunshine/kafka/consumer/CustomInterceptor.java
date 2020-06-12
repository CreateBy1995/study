package study.sunshine.kafka.consumer;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerInterceptor;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;

import java.util.Map;

/**
 * @Author: dongcx
 * @Description:
 * @Date: 2020-04-29
 **/
@Slf4j
public class CustomInterceptor implements ProducerInterceptor {
    @Override
    public ProducerRecord onSend(ProducerRecord record) {
        log.info("onSend");
        return record;
    }

    @Override
    public void onAcknowledgement(RecordMetadata metadata, Exception exception) {

        log.info("onAcknowledgement");
    }

    @Override
    public void close() {
        log.info("close");
    }

    @Override
    public void configure(Map<String, ?> configs) {
        log.info("configure");
    }
}
