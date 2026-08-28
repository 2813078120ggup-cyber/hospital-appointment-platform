package com.atguigu.yygh.hosp.client;

import com.atguigu.yygh.vo.hosp.ScheduleOrderVo;
import com.atguigu.yygh.vo.order.SignInfoVo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.stereotype.Repository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

@FeignClient(value = "service-hosp")
@Repository
public interface HospitalFeignClient {

    // 根据排班id查询预约挂号相关数据
    @GetMapping("/api/hosp/hospital/inner/getScheduleOrderVo/{scheduleId}")
    public ScheduleOrderVo getScheduleOrderVo(@PathVariable("scheduleId") String scheduleId);

    @GetMapping("/api/hosp/hospital/inner/getSignInfo/{hoscode}")
    SignInfoVo getSignInfo(@PathVariable("hoscode") String hoscode);

    @PostMapping("/api/hosp/hospital/inner/decrementAvailableNumber/{scheduleId}")
    Boolean decrementAvailableNumber(@PathVariable("scheduleId") String scheduleId);

    @PostMapping("/api/hosp/hospital/inner/restoreAvailableNumber/{scheduleId}")
    Boolean restoreAvailableNumber(@PathVariable("scheduleId") String scheduleId);

}
