package com.atguigu.yygh.common.config;

import com.atguigu.yygh.common.utils.JwtHelper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Configuration;

/**
 * JWT 密钥自动注入：把 Spring 配置里的 yygh.jwt.secret 注入到静态 JwtHelper。
 * 这样所有业务服务只需在配置文件中声明 yygh.jwt.secret（支持 ${ENV:default}），
 * 无需依赖进程启动时的环境变量，彻底消除多进程密钥不一致问题。
 */
@Configuration
public class JwtAutoConfiguration implements ApplicationRunner {

    @Value("${yygh.jwt.secret:}")
    private String jwtSecret;

    @Override
    public void run(ApplicationArguments args) {
        if (jwtSecret != null && !jwtSecret.isEmpty()) {
            JwtHelper.initSecret(jwtSecret);
        }
    }
}
