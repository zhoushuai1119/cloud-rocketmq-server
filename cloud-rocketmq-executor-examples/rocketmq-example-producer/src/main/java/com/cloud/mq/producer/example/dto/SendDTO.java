package com.cloud.mq.producer.example.dto;

import com.cloud.platform.common.domain.dto.BaseDTO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @description:
 * @author: zhou shuai
 * @date: 2022/8/31 10:34
 * @version: v1
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class SendDTO extends BaseDTO {

    private static final long serialVersionUID = -7564309209154944509L;

    private String name;

    private Integer age;

}
