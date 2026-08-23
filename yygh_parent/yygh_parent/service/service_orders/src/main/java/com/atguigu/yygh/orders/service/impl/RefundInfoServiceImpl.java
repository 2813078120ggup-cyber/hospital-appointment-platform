package com.atguigu.yygh.orders.service.impl;

import com.atguigu.yygh.enums.RefundStatusEnum;
import com.atguigu.yygh.model.order.PaymentInfo;
import com.atguigu.yygh.model.order.RefundInfo;
import com.atguigu.yygh.orders.mapper.RefundInfoMapper;
import com.atguigu.yygh.orders.service.RefundInfoService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

import java.util.Date;

/**
 * <p>
 * 退款信息表 服务实现类
 * </p>
 *
 * @author atguigu
 * @since 2022-11-05
 */
@Service
public class RefundInfoServiceImpl extends ServiceImpl<RefundInfoMapper, RefundInfo> implements RefundInfoService {

    //2 向退款记录表添加一条记录（状态：退款中）
    @Override
    public RefundInfo savefundInfo(PaymentInfo paymentInfo) {
        //根据orderId查询是否存在退款记录
        LambdaQueryWrapper<RefundInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RefundInfo::getOrderId,paymentInfo.getOrderId());
        RefundInfo refundInfo = baseMapper.selectOne(wrapper);

        //如果存在，不需要再添加
        if(refundInfo != null) {
            return refundInfo;
        }

        //如果不存在，添加
        refundInfo = new RefundInfo();
        refundInfo.setCreateTime(new Date());
        refundInfo.setOrderId(paymentInfo.getOrderId());
        refundInfo.setPaymentType(paymentInfo.getPaymentType());
        refundInfo.setOutTradeNo(paymentInfo.getOutTradeNo());
        //退款中
        refundInfo.setRefundStatus(RefundStatusEnum.UNREFUND.getStatus());
        refundInfo.setSubject(paymentInfo.getSubject());
        refundInfo.setTotalAmount(paymentInfo.getTotalAmount());
        baseMapper.insert(refundInfo);
        return refundInfo;
    }
}
