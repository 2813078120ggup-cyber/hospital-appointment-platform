package com.atguigu.yygh.common.exception;

import com.atguigu.yygh.common.result.R;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;

@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(Exception.class)
    @ResponseBody
    public R error(Exception e) {
        System.out.println("全局.......");
        e.printStackTrace();
        return R.error().message("执行全局异常处理");
    }

    //特定  ArithmeticException
    @ExceptionHandler(ArithmeticException.class)
    @ResponseBody
    public R error(ArithmeticException e) {
        System.out.println("特定.......");
        e.printStackTrace();
        return R.error().message("执行特定异常处理");
    }

    //自定义异常
    @ExceptionHandler(YyghException.class)
    @ResponseBody
    public R error(YyghException e) {
        log.error("execute yygh exception");
        e.printStackTrace();
        return R.error().code(e.getCode()).message(e.getMsg());
    }
}
