package com.atguigu.yygh.hosp.service;

import com.atguigu.yygh.model.hosp.HospitalSet;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * 医院设置表 服务类
 */
public interface HospitalSetService extends IService<HospitalSet> {

    //根据医院编号查询签名key
    String getHospSignKey(String hoscode);
}
