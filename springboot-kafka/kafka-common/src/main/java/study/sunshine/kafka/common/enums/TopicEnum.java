package study.sunshine.kafka.common.enums;

/**
 * @Author: dongcx
 * @Description:
 * @Date: 2020-04-23
 **/
public enum TopicEnum {
    TEST( "TEST"),
    ;
    private String value;
    public String getValue() {
        return value;
    }
    TopicEnum(String value) {
        this.value = value;
    }
}
