package study.sunshine.kafka.provider.provider;


import study.sunshine.kafka.common.bean.Message;
import study.sunshine.kafka.common.enums.TopicEnum;

public interface IKafkaProvider {
    void send(Message message, TopicEnum topicEnum);
}
