package study.sunshine.kafka.consumer;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;

import java.util.Arrays;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;

/**
 * @Author: dongcx
 * @Description:
 * @Date: 2020-04-23
 **/
//@SpringBootApplication
public class KafkaConsumerApplication {
    public static void main(String[] args) {
        CustomConsumerInterceptor customConsumerInterceptor = new CustomConsumerInterceptor();
        customConsumerInterceptor.test(null);
//        send(5);
//        consumer();
    }
    public static void consumer(){
        while (true){
            Properties kafkaProps = new Properties();
            kafkaProps.put("bootstrap.servers","127.0.0.1:9093");
            kafkaProps.put("group.id", "testtest1"); // 消费者加入的消费者组
            kafkaProps.put("auto.offset.reset", "earliest");
            kafkaProps.put("enable.auto.commit", "true"); // 自动commit
            kafkaProps.put("auto.commit.interval.ms", "1000"); // 自动commit的间隔
            kafkaProps.put("session.timeout.ms", "30000");
            kafkaProps.put("max.poll.records", "2");
            kafkaProps.put("interceptor.classes","study.sunshine.kafka.consumer.CustomConsumerInterceptor");
            // 反序列化器
            kafkaProps.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
            kafkaProps.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
            KafkaConsumer<String, String> consumer = new KafkaConsumer<>(kafkaProps);
            consumer.subscribe(Arrays.asList("TEST"));
            try {
                while (true) {
                    ConsumerRecords<String, String> records = consumer.poll(1000000000);
                    for (ConsumerRecord<String, String> record : records) {
                        System.out.printf("offset = %d, key = %s, value = %s \n", record.offset(), record.key(), record.value());
                        try {
                            Thread.sleep(100);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }catch(Exception e){

            }finally{
                consumer.close();
            }
        }
    }
    public static void send(Integer sendTimes){
        Properties kafkaProps = new Properties();
        kafkaProps.put("bootstrap.servers","127.0.0.1:9093");
        kafkaProps.put("key.serializer","org.apache.kafka.common.serialization.StringSerializer");
        kafkaProps.put("value.serializer","org.apache.kafka.common.serialization.StringSerializer");
        kafkaProps.put("interceptor.classes","study.sunshine.kafka.consumer.CustomInterceptor");
        kafkaProps.put("enable.idempotence",true);
//        kafkaProps.put("retries",5);
//        kafkaProps.put("transactional.id","9527");
        KafkaProducer kafkaProducer = new KafkaProducer<String,String>(kafkaProps) ;
        String topic = "TEST";
        String key = "test";
//        String value = new String(new byte[96]);
        CountDownLatch countDownLatch = new CountDownLatch(sendTimes);
        for (int i = 0; i < sendTimes; i++) {
            ProducerRecord msg = new ProducerRecord(topic, key+i, String.valueOf(i));
            kafkaProducer.send(msg,(x,y)->{
                countDownLatch.countDown();
            });
        }
        try {
            countDownLatch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
