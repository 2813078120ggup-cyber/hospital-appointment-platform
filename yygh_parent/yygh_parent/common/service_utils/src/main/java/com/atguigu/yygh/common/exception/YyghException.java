package com.atguigu.yygh.common.exception;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class YyghException extends RuntimeException {

    private Integer code;  //异常状态码

    private String msg;  //异常描述
}
