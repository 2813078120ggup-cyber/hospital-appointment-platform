package com.atguigu.yygh.orders.service;

import com.atguigu.yygh.model.order.OrderInfo;
import com.atguigu.yygh.model.order.PaymentInfo;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.Map;

/**
 * <p>
 * 支付信息表 服务类
 * </p>
 *
 * @author atguigu
 * @since 2022-11-04
 */
public interface PaymentInfoService extends IService<PaymentInfo> {

    //2 向支付记录表添加支付记录（状态：正在支付）
    void savePaymentInfo(OrderInfo orderInfo);

    //根据订单交易号，更新订单状态和支付记录状态 ：已经支付
    void paySuccess(String out_trade_no, Map<String, String> resultMap);

    //1 根据orderId查询支付记录表，获取支付记录
    PaymentInfo getPaymentInfoByOrderId(Long orderId);
}
