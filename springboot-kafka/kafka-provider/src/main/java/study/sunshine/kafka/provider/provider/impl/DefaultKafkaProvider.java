package study.sunshine.kafka.provider.provider.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import study.sunshine.kafka.common.bean.Message;
import study.sunshine.kafka.common.enums.TopicEnum;
import study.sunshine.kafka.common.utils.GsonUtil;
import study.sunshine.kafka.provider.provider.IKafkaProvider;

/**
 * @Author: dongcx
 * @Description:
 * @Date: 2020-04-23
 **/
@Service
public class DefaultKafkaProvider implements IKafkaProvider {
    @Autowired
    private KafkaTemplate<String,String> kafkaTemplate ;
    @Override
    public void send(Message message, TopicEnum topicEnum) {
        kafkaTemplate.send(topicEnum.getValue(), GsonUtil.toGson(message)) ;
    }
}
