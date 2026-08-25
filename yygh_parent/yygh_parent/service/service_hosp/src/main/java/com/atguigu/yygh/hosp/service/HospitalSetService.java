package com.atguigu.yygh.hosp.service;

import com.atguigu.yygh.model.hosp.HospitalSet;
import com.atguigu.yygh.vo.order.SignInfoVo;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * 医院设置表 服务类
 */
public interface HospitalSetService extends IService<HospitalSet> {

    //根据医院编号查询签名key
    String getHospSignKey(String hoscode);

    // 功能完善：订单服务通过内部接口获取每家医院独立的 API 地址和签名密钥。
    SignInfoVo getSignInfo(String hoscode);
}
