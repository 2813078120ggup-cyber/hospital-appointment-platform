package com.atguigu.yygh.orders.service.impl;

import com.atguigu.yygh.enums.OrderStatusEnum;
import com.atguigu.yygh.enums.PaymentStatusEnum;
import com.atguigu.yygh.model.order.OrderInfo;
import com.atguigu.yygh.model.order.PaymentInfo;
import com.atguigu.yygh.orders.mapper.OrderInfoMapper;
import com.atguigu.yygh.orders.mapper.PaymentInfoMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentInfoServiceImplTest {

    @Mock
    private PaymentInfoMapper paymentInfoMapper;

    @Mock
    private OrderInfoMapper orderInfoMapper;

    private PaymentInfoServiceImpl paymentInfoService;

    @BeforeEach
    void setUp() {
        paymentInfoService = new PaymentInfoServiceImpl();
        ReflectionTestUtils.setField(paymentInfoService, "baseMapper", paymentInfoMapper);
        ReflectionTestUtils.setField(paymentInfoService, "orderInfoMapper", orderInfoMapper);
    }

    @Test
    void repeatedPaymentCallbackDoesNotReopenCancelledOrder() {
        OrderInfo order = new OrderInfo();
        order.setId(8L);
        order.setOrderStatus(OrderStatusEnum.CANCLE.getStatus());
        PaymentInfo payment = new PaymentInfo();
        payment.setOrderId(8L);
        when(orderInfoMapper.selectOne(any())).thenReturn(order);
        when(paymentInfoMapper.selectOne(any())).thenReturn(payment);

        paymentInfoService.paySuccess("trade-8", Map.of("transaction_id", "wx-8"));

        verify(orderInfoMapper, never()).updateById(any(OrderInfo.class));
        verify(paymentInfoMapper).updateById(payment);
        org.junit.jupiter.api.Assertions.assertEquals(
                PaymentStatusEnum.PAID.getStatus(), payment.getPaymentStatus());
    }
}
