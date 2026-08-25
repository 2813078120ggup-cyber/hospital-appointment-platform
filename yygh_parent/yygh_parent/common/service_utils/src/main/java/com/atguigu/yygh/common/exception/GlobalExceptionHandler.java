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
        log.error("Unhandled application exception", e);
        return R.error().message("执行全局异常处理");
    }

    //特定  ArithmeticException
    @ExceptionHandler(ArithmeticException.class)
    @ResponseBody
    public R error(ArithmeticException e) {
        log.error("Arithmetic exception", e);
        return R.error().message("执行特定异常处理");
    }

    //自定义异常
    @ExceptionHandler(YyghException.class)
    @ResponseBody
    public R error(YyghException e) {
        // 功能完善：业务异常只记录状态和消息，避免把完整堆栈及敏感请求信息输出到控制台。
        log.warn("Business exception, code={}, message={}", e.getCode(), e.getMsg());
        return R.error().code(e.getCode()).message(e.getMsg());
    }
}
