package com.atguigu.yygh.orders.service;

import com.atguigu.yygh.model.order.PaymentInfo;
import com.atguigu.yygh.model.order.RefundInfo;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 * 退款信息表 服务类
 * </p>
 *
 * @author atguigu
 * @since 2022-11-05
 */
public interface RefundInfoService extends IService<RefundInfo> {

    //2 向退款记录表添加一条记录（状态：退款中）
    RefundInfo savefundInfo(PaymentInfo paymentInfo);
}
