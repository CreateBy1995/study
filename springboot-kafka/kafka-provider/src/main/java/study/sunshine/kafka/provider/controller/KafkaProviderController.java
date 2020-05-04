package study.sunshine.kafka.provider.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import study.sunshine.kafka.common.bean.Message;
import study.sunshine.kafka.common.enums.TopicEnum;
import study.sunshine.kafka.provider.provider.IKafkaProvider;

import java.util.UUID;

/**
 * @Author: dongcx
 * @Description:
 * @Date: 2020-04-23
 **/
@RestController
@RequestMapping("/kafka/provider")
public class KafkaProviderController {
    @Autowired
    private IKafkaProvider iKafkaProvider ;
    @PostMapping("/send")
    public Integer send(String msg){
        Integer resultCode = 200 ;
        try {
            Message message = new Message() ;
            message.setId(UUID.randomUUID().toString());
            message.setTimeCreate(System.currentTimeMillis());
            message.setMessage(msg);
            iKafkaProvider.send(message, TopicEnum.TEST);
        }catch (Exception e){
            resultCode = 500 ;
        }
        return resultCode ;
    }
}
