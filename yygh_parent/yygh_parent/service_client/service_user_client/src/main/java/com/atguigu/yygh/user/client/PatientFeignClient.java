package com.atguigu.yygh.user.client;

import com.atguigu.yygh.model.user.Patient;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.stereotype.Repository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(value = "service-user")
@Repository
public interface PatientFeignClient {
    // ----20260822 新增接口 辅助下单（预约挂号）业务 start ----
    @GetMapping("/api/user/patient/inner/getPatientInfoById/{id}")
    public Patient getPatientInfoById(@PathVariable("id") Long id);
    
    // ----20260822 新增接口 辅助下单（预约挂号）业务 end ----
}
