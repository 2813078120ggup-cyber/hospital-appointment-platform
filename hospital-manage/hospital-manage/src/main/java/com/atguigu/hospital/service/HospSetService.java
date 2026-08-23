package com.atguigu.hospital.service;

import java.util.Map;

/**
 * 医院设置
 */
public interface HospSetService {

    /**
     * 同步签名秘钥
     */
    void updateSignKey(Map<String, Object> paramMap);

}
