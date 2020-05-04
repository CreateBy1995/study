package study.sunshine.kafka.consumer;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * @Author: dongcx
 * @Description:
 * @Date: 2020-04-23
 **/
@SpringBootApplication
public class KafkaConsumerApplication {
    public static void main(String[] args) {
        Properties kafkaProps = new Properties();
        kafkaProps.put("bootstrap.servers","127.0.0.1:9093");
        kafkaProps.put("key.serializer","org.apache.kafka.common.serialization.StringSerializer");
        kafkaProps.put("value.serializer","org.apache.kafka.common.serialization.StringSerializer");
        kafkaProps.put("interceptor.classes","study.sunshine.kafka.consumer.CustomInterceptor");
//        kafkaProps.put("retries",5);
        kafkaProps.put("transactional.id","9527");
        KafkaProducer kafkaProducer = new KafkaProducer<String,String>(kafkaProps) ;
        kafkaProducer.beginTransaction();
        for (int i = 0; i < 3; i++) {
            String topic = "HELLO_TOPIC";
            String key = "hi1995"+i;
            byte []datas = new byte[1024*50];
            String value = new String(datas);
            ProducerRecord msg = new ProducerRecord(topic,key,value);
            Future<RecordMetadata> future = kafkaProducer.send(msg);
            try {
                RecordMetadata recordMetadata = future.get();
                System.out.println(recordMetadata);
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (ExecutionException e) {
                e.printStackTrace();
            }
        }
        kafkaProducer.commitTransaction();
        kafkaProducer.abortTransaction();
 //        SpringApplication.run(KafkaConsumerApplication.class) ;
    }
}
