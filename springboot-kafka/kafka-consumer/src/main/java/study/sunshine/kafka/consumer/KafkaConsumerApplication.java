package study.sunshine.kafka.consumer;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;

/**
 * @Author: dongcx
 * @Description:
 * @Date: 2020-04-23
 **/
@SpringBootApplication
public class KafkaConsumerApplication {
    public static void main(String[] args) {
        FutureTask futureTask = new FutureTask(()->{
            return "5";
        }) ;

        Properties kafkaProps = new Properties();
        kafkaProps.put("bootstrap.servers","127.0.0.1:9093");
        kafkaProps.put("key.serializer","org.apache.kafka.common.serialization.StringSerializer");
        kafkaProps.put("value.serializer","org.apache.kafka.common.serialization.StringSerializer");
        kafkaProps.put("interceptor.classes","study.sunshine.kafka.consumer.CustomInterceptor");
        kafkaProps.put("max.in.flight.requests.per.connection",1);
        //        kafkaProps.put("retries",5);
//        kafkaProps.put("transactional.id","9527");
        KafkaProducer kafkaProducer = new KafkaProducer<String,String>(kafkaProps) ;
        String topic = "WRONG";
            for (int j = 20; j < 25 ; j++) {
                Integer size = 1024;
                if(j == 23){
                    size = 1024*512;
                }
                String key = "hi1995" + j;
                byte[] datas = new byte[size];
                String value = new String(datas);
                ProducerRecord msg = new ProducerRecord(topic, key, value);
                kafkaProducer.send(msg);
                try {
                    Thread.sleep(4000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
//        kafkaProducer.beginTransaction();
//        new Thread(()->{
//            String topic = "HELLO_TOPIC0";
//            for (int j = 0; j < 1 ; j++) {
//                String key = "hi1995"+j;
//                byte []datas = new byte[1024];
//                String value = new String(datas);
//                ProducerRecord msg = new ProducerRecord(topic,key,value);
//                Future<RecordMetadata> future = kafkaProducer.send(msg,(metadata,exception) -> {
//                    System.out.println(metadata.offset());
////                    System.out.println(exception.getMessage());
//                });
//                try {
//                    RecordMetadata recordMetadata = future.get();
//                    System.out.println(recordMetadata);
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                } catch (ExecutionException e) {
//                    e.printStackTrace();
//                }
//            }
//        }).start();
//        new Thread(()->{
//            String topic = "HELLO_TOPIC1";
//            for (int j = 0; j < 1 ; j++) {
//                String key = "hi1995"+j;
//                byte []datas = new byte[1024];
//                String value = new String(datas);
//                ProducerRecord msg = new ProducerRecord(topic,key,value);
//                Future<RecordMetadata> future = kafkaProducer.send(msg);
//                try {
//                    RecordMetadata recordMetadata = future.get();
//                    System.out.println(recordMetadata);
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                } catch (ExecutionException e) {
//                    e.printStackTrace();
//                }
//            }
//        }).start();
//        new Thread(()->{
//            String topic = "HELLO_TOPIC2";
//            for (int j = 0; j < 1 ; j++) {
//                String key = "hi1995"+j;
//                byte []datas = new byte[512];
//                String value = new String(datas);
//                ProducerRecord msg = new ProducerRecord(topic,key,value);
//                Future<RecordMetadata> future = kafkaProducer.send(msg);
//                try {
//                    RecordMetadata recordMetadata = future.get();
//                    System.out.println(recordMetadata);
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                } catch (ExecutionException e) {
//                    e.printStackTrace();
//                }
//            }
//        }).start();
//        kafkaProducer.commitTransaction();
//        kafkaProducer.abortTransaction();
 //        SpringApplication.run(KafkaConsumerApplication.class) ;
    }
}
