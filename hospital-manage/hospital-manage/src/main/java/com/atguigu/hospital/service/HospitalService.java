package com.atguigu.hospital.service;

import java.util.Map;

public interface HospitalService {

    /**
     * 预约下单
     */
    Map<String, Object> submitOrder(Map<String, Object> paramMap);

    /**
     * 更新支付状态
     */
    void updatePayStatus(Map<String, Object> paramMap);

    /**
     * 更新取消预约状态
     */
    void updateCancelStatus(Map<String, Object> paramMap);

}
