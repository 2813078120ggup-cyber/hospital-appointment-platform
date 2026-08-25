package com.atguigu.yygh.rabbit.config;

import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MQConfig {

    @Bean
    public MessageConverter messageConverter() {
        // 生产者 Java对象 → JSON序列化 → RabbitMQ → JSON消息 → JSON反序列化 → 消费者 Java对象
        return new Jackson2JsonMessageConverter();
    }
}
