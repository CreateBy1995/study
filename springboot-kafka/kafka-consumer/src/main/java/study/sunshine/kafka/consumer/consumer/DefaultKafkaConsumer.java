package study.sunshine.kafka.consumer.consumer;

import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

/**
 * @Author: dongcx
 * @Description:
 * @Date: 2020-04-23
 **/
@Slf4j
@Component
public class DefaultKafkaConsumer {
    @KafkaListener(containerFactory = "kafkaListenerContainerFactory", topics = "TEST")
    public void consumer(String records, Acknowledgment acknowledgment){
        System.out.println(records);
//        acknowledgment.acknowledge();
//        records.forEach(record -> {
//            Message message = GsonUtil.fromGson(record.key(),Message.class) ;
//            log.info("received the message is : {}",message.getMessage());
//        });
    }
}
