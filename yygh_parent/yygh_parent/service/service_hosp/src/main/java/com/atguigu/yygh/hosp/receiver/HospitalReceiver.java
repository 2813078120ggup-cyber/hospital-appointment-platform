package com.atguigu.yygh.hosp.receiver;

import com.atguigu.yygh.hosp.service.ScheduleService;
import com.atguigu.yygh.rabbit.RabbitService;
import com.atguigu.yygh.rabbit.constant.MqConst;
import com.atguigu.yygh.vo.msm.MsmVo;
import com.atguigu.yygh.vo.order.OrderMqVo;
import com.rabbitmq.client.Channel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class HospitalReceiver {
    @Autowired
    ScheduleService scheduleService;

    @Autowired
    RabbitService rabbitService;

    //下单更新预约数量，以及取消预约更新预约数量，两个业务共用一个监听方法
    @RabbitListener(
            bindings = @QueueBinding(
                    value = @Queue(MqConst.QUEUE_ORDER),
                    exchange = @Exchange(MqConst.EXCHANGE_DIRECT_ORDER),
                    key = {
                            MqConst.ROUTING_ORDER
                    }
            )
    )
    public void receiver(OrderMqVo orderMqVo, Message message, Channel channel) {
        if (orderMqVo.getAvailableNumber() != null && orderMqVo.getReservedNumber() != null) {
            boolean synced = scheduleService.syncAvailableNumber(
                    orderMqVo.getScheduleId(),
                    orderMqVo.getReservedNumber(),
                    orderMqVo.getAvailableNumber());
            if (!synced) {
                log.warn("排班库存同步失败，scheduleId={}, reservedNumber={}, availableNumber={}",
                        orderMqVo.getScheduleId(), orderMqVo.getReservedNumber(),
                        orderMqVo.getAvailableNumber());
            }
        } else if (orderMqVo.getAvailableNumber() == null && orderMqVo.getReservedNumber() == null) {
            // 取消预约不能使用“读取后 +1 再 save”的非原子写法；多个取消并发时
            // 必须由 MongoDB 原子 CAS 回补，且库存不得超过总号源。
            boolean restored = scheduleService.restoreAvailableNumber(orderMqVo.getScheduleId());
            if (!restored) {
                log.warn("排班库存回补失败或已达到总号源，scheduleId={}", orderMqVo.getScheduleId());
            }
        } else {
            // 只有“两个库存字段都为空”才表示取消消息；部分字段缺失时不能误判为取消并加号。
            log.warn("排班库存消息字段不完整，scheduleId={}, reservedNumber={}, availableNumber={}",
                    orderMqVo.getScheduleId(), orderMqVo.getReservedNumber(),
                    orderMqVo.getAvailableNumber());
        }
        MsmVo msmVo = orderMqVo.getMsmVo();
        if (msmVo != null) {
            rabbitService.sendMessage(MqConst.EXCHANGE_DIRECT_MSM, MqConst.ROUTING_MSM_ITEM, msmVo);//通知用户发短信
        }
    }
}
