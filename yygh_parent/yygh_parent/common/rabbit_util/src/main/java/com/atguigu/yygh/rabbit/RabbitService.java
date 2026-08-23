package com.atguigu.yygh.rabbit;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class RabbitService {

    @Autowired
    RabbitTemplate rabbitTemplate;


    /**
     * 封装发送消息的方法
     *
     * @param exchange   交换机名称
     * @param routingKey 路由key名称
     * @param message    消息内容
     * @return 消息是否发送成功
     */
    public boolean sendMessage(String exchange, String routingKey, Object message) {
        rabbitTemplate.convertAndSend(exchange, routingKey, message);
        return true;
    }
}
