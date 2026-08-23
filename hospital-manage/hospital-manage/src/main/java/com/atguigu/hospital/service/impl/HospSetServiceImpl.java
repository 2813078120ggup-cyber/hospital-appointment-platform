package com.atguigu.hospital.service.impl;

import com.atguigu.hospital.mapper.HospitalSetMapper;
import com.atguigu.hospital.model.HospitalSet;
import com.atguigu.hospital.service.HospSetService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;

//医院设置
@Service
@Slf4j
public class HospSetServiceImpl implements HospSetService {

    @Autowired
    private HospitalSetMapper hospitalSetMapper;

    //同步签名秘钥
    @Override
    public void updateSignKey(Map<String, Object> paramMap) {
        //获取签名秘钥
        String sign = (String) paramMap.get("sign");
        String hoscode = (String) paramMap.get("hoscode");
        //TODO 更新id=1的签名
        HospitalSet hospitalSet = hospitalSetMapper.selectById(1);
        if (hospitalSet == null) {
            hospitalSet = new HospitalSet();
            hospitalSet.setId(1L);
            hospitalSet.setSignKey(sign);
            hospitalSet.setHoscode(hoscode);
            hospitalSet.setApiUrl("http://localhost:8201");
            hospitalSetMapper.insert(hospitalSet);
        } else {
            hospitalSet.setSignKey(sign);
            hospitalSet.setHoscode(hoscode);
            hospitalSetMapper.updateById(hospitalSet);
        }

    }
}
