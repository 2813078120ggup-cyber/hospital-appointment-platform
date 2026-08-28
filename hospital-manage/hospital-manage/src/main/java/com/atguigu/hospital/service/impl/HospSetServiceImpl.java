package com.atguigu.hospital.service.impl;

import com.atguigu.hospital.mapper.HospitalSetMapper;
import com.atguigu.hospital.model.HospitalSet;
import com.atguigu.hospital.service.HospSetService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Map;

//医院设置
@Service
@Slf4j
public class HospSetServiceImpl implements HospSetService {

    @Autowired
    private HospitalSetMapper hospitalSetMapper;

    @Value("${yygh.platform-api.base-url:http://192.168.6.101:8201}")
    private String platformApiUrl;

    //同步签名秘钥
    @Override
    public void updateSignKey(Map<String, Object> paramMap) {
        //获取签名秘钥
        String sign = (String) paramMap.get("sign");
        String hoscode = (String) paramMap.get("hoscode");
        // 功能完善：同步签名时同时刷新平台地址，避免数据库继续保留旧测试机 IP。
        HospitalSet hospitalSet = hospitalSetMapper.selectById(1);
        if (hospitalSet == null) {
            hospitalSet = new HospitalSet();
            hospitalSet.setId(1L);
            hospitalSet.setSignKey(sign);
            hospitalSet.setHoscode(hoscode);
            hospitalSet.setApiUrl(platformApiUrl);
            hospitalSetMapper.insert(hospitalSet);
        } else {
            hospitalSet.setSignKey(sign);
            hospitalSet.setHoscode(hoscode);
            hospitalSet.setApiUrl(platformApiUrl);
            hospitalSetMapper.updateById(hospitalSet);
        }

    }
}
