package com.atguigu.yygh.orders.service;

import java.util.Map;

public interface WeixinService {

    //生成微信支付二维码
    Map<String, Object> createNative(Long orderId);

    //1 调用微信接口，查询订单支付状态
    Map<String, String> queryPayStatus(Long orderId);

    //退款
    boolean refund(Long orderId);
}
