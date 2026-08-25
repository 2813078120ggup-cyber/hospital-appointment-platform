package com.atguigu.yygh.common.exception;

import lombok.Data;

@Data
public class YyghException extends RuntimeException {

    private Integer code;  //异常状态码

    private String msg;  //异常描述

    public YyghException() {
        super();
    }

    public YyghException(Integer code, String msg) {
        super(msg);
        this.code = code;
        this.msg = msg;
    }
}
