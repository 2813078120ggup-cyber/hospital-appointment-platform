package com.atguigu.yygh.orders.receiver;

import com.atguigu.yygh.model.order.OrderInfo;
import com.atguigu.yygh.orders.service.OrderInfoService;
import com.atguigu.yygh.rabbit.constant.MqConst;
import com.rabbitmq.client.Channel;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class OrderReceiver {

    @Autowired
    OrderInfoService orderInfoService;

    @RabbitListener(
            bindings = @QueueBinding(
                    value = @Queue(MqConst.QUEUE_TASK_8),
                    exchange = @Exchange(MqConst.EXCHANGE_DIRECT_TASK),
                    key = {MqConst.ROUTING_TASK_8}
            )
    )
    public void patientTips(String dateString, Message message, Channel channel) {
        orderInfoService.patientTips(dateString);
    }
}
