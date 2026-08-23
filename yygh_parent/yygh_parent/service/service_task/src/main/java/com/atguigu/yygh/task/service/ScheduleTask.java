package com.atguigu.yygh.task.service;

import com.atguigu.yygh.rabbit.RabbitService;
import com.atguigu.yygh.rabbit.constant.MqConst;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@EnableScheduling
public class ScheduleTask {

    @Autowired
    RabbitService rabbitService;

    //每天晚上八点，短信提醒就医
    //@Scheduled(cron = "0 0 20 * * ?")
    @Scheduled(cron = "0/30 * * * * ?")
    public void tesk() {
        DateTime dateTime = new DateTime().plusDays(1);
        String dateString = dateTime.toString("yyyy-MM-dd");
        rabbitService.sendMessage(MqConst.EXCHANGE_DIRECT_TASK, MqConst.ROUTING_TASK_8, dateString);
    }
}
