package study.sunshine.kafka.common.bean;

import lombok.Data;

/**
 * @Author: dongcx
 * @Description:
 * @Date: 2020-04-23
 **/
@Data
public class Message {
    private String id ;
    private String message ;
    private Long timeCreate ;
}
