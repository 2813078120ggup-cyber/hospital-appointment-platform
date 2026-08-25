package com.atguigu.yygh.hosp.receiver;

import com.atguigu.yygh.hosp.service.ScheduleService;
import com.atguigu.yygh.model.hosp.Schedule;
import com.atguigu.yygh.rabbit.RabbitService;
import com.atguigu.yygh.rabbit.constant.MqConst;
import com.atguigu.yygh.vo.msm.MsmVo;
import com.atguigu.yygh.vo.order.OrderMqVo;
import com.rabbitmq.client.Channel;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

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
        if(orderMqVo.getAvailableNumber() !=null){
            Schedule schedule = scheduleService.getScheduleId(orderMqVo.getScheduleId());
            schedule.setReservedNumber(orderMqVo.getReservedNumber());
            schedule.setAvailableNumber(orderMqVo.getAvailableNumber());
            scheduleService.update(schedule); //预约下单  更新mongo数据库排班集合数据
        }else{
            Schedule schedule = scheduleService.getScheduleId(orderMqVo.getScheduleId());
            schedule.setAvailableNumber(schedule.getAvailableNumber().intValue()+1);
            scheduleService.update(schedule); //取消预约
        }
        MsmVo msmVo = orderMqVo.getMsmVo();
        if (msmVo != null) {
            rabbitService.sendMessage(MqConst.EXCHANGE_DIRECT_MSM, MqConst.ROUTING_MSM_ITEM, msmVo);//通知用户发短信
        }
    }
}
