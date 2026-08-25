package com.atguigu.yygh.msm.service;

import com.atguigu.yygh.vo.msm.MsmVo;

public interface MsmService {

    boolean send(MsmVo msmVo);
    
    boolean sendMsm(String phone, String code);
}
