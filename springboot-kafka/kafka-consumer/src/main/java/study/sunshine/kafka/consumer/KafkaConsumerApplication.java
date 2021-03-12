package study.sunshine.kafka.consumer;

import org.apache.kafka.clients.consumer.ConsumerRebalanceListener;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.TopicPartition;
import org.springframework.util.StopWatch;

import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.Properties;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * @Author: dongcx
 * @Description:
 * @Date: 2020-04-23
 **/
//@SpringBootApplication
public class KafkaConsumerApplication {
    public static void main(String[] args) {
//        CustomConsumerInterceptor customConsumerInterceptor = new CustomConsumerInterceptor();
//        customConsumerInterceptor.test(null);
//        send(3);
        new Thread(()->{
            consumer("abc", false,"TEST");
        }).start();
        try {
            TimeUnit.SECONDS.sleep(20);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        consumer("abc1", false,"TEST");
//        try {
//            TimeUnit.SECONDS.sleep(3);
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }
//        consumer("abc", false,"TEST2");

    }

    public static void consumer(String id,boolean close, String ...topics) {
        Properties kafkaProps = new Properties();
        kafkaProps.put("bootstrap.servers", "127.0.0.1:9093");
        kafkaProps.put("group.id", "testtest2"); // 消费者加入的消费者组
        kafkaProps.put("auto.offset.reset", "earliest");
        kafkaProps.put("client.id",id);
        kafkaProps.put("enable.auto.commit", "false"); // 自动commit
//        kafkaProps.put("auto.commit.interval.ms", "1000"); // 自动commit的间隔
        kafkaProps.put("session.timeout.ms", "8000");
        kafkaProps.put("max.poll.records", "1");
        kafkaProps.put("fetch.max.bytes",1024);
        kafkaProps.put("max.poll.interval.ms", "6000");
        kafkaProps.put("heartbeat.interval.ms", "500");
//        kafkaProps.put("interceptor.classes", "study.sunshine.kafka.consumer.CustomConsumerInterceptor");
        ConsumerRebalanceListener consumerRebalanceListener = new ConsumerRebalanceListener() {
            @Override
            public void onPartitionsRevoked(Collection<TopicPartition> partitions) {
                System.out.println(Thread.currentThread().getName() + " ---- onPartitionsRevoked");
            }

            @Override
            public void onPartitionsAssigned(Collection<TopicPartition> partitions) {
                System.out.println(Thread.currentThread().getName() + " ---- onPartitionsAssigned------patitions" + partitions);
            }
        };
        // 反序列化器
        kafkaProps.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        kafkaProps.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        KafkaConsumer<String, String> consumer = new KafkaConsumer<>(kafkaProps);
        consumer.subscribe(Arrays.asList(topics), consumerRebalanceListener);
        try {
            while (true) {
                ConsumerRecords<String, String> records = consumer.poll(1000);
                System.out.println("start sleep");
                TimeUnit.SECONDS.sleep(40);
                System.out.println("end sleep");
                if (close){
                   break;
                }
//                consumer.wakeup();
//                send(100);
                for (ConsumerRecord<String, String> record : records) {
                    System.out.printf("aaaa offset = %d, key = %s, value = %s \n", record.offset(), record.key(), record.value());

                    try {
                        consumer.commitSync();
                    }catch (Exception e){
                        e.printStackTrace();
                        System.out.println("忽略提交异常");
                    }
                }
            }
        } catch (Exception e) {
            System.out.println(e);
        } finally {
            consumer.close();
        }
    }

    public static void send(Integer sendTimes) {
        Properties kafkaProps = new Properties();
        kafkaProps.put("bootstrap.servers", "127.0.0.1:9093");
        kafkaProps.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        kafkaProps.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        kafkaProps.put("interceptor.classes", "study.sunshine.kafka.consumer.CustomInterceptor");
        kafkaProps.put("enable.idempotence", true);
//        kafkaProps.put("retries",5);
//        kafkaProps.put("transactional.id","9527");
        KafkaProducer kafkaProducer = new KafkaProducer<String, String>(kafkaProps);
        String topic = "TEST";
        String key = "test";
//        String value = new String(new byte[96]);
        CountDownLatch countDownLatch = new CountDownLatch(sendTimes);
        String msgStr = "";
        for (int i = 0 ; i < 1024 ; i++){
            msgStr = msgStr + String.valueOf(new Random().nextInt(9));
        }
        for (int i = 0; i < sendTimes; i++) {
            ProducerRecord msg = new ProducerRecord(topic, i,key + i, msgStr);
            kafkaProducer.send(msg, (x, y) -> {
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
