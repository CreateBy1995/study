package study.sunshine.dubbo.commonapi.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.ToString;

import java.io.Serializable;

/**
 * @Author: dongcx
 * @Description:
 * @Date: 2021-01-15
 **/
@AllArgsConstructor
@Data
@ToString
public class TestDTO implements Serializable {
    private String name;
    private Integer sex;

}
