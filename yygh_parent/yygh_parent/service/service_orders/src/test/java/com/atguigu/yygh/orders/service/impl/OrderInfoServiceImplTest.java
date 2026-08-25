package com.atguigu.yygh.orders.service.impl;

import com.atguigu.yygh.common.exception.YyghException;
import com.atguigu.yygh.enums.OrderStatusEnum;
import com.atguigu.yygh.model.order.OrderInfo;
import com.atguigu.yygh.orders.mapper.OrderInfoMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderInfoServiceImplTest {

    @Mock
    private OrderInfoMapper orderInfoMapper;

    private OrderInfoServiceImpl orderInfoService;

    @BeforeEach
    void setUp() {
        orderInfoService = new OrderInfoServiceImpl();
        ReflectionTestUtils.setField(orderInfoService, "baseMapper", orderInfoMapper);
    }

    @Test
    void returnsOrderForItsOwner() {
        OrderInfo order = order(10L, 7L);
        when(orderInfoMapper.selectById(10L)).thenReturn(order);

        OrderInfo result = orderInfoService.getOrderInfo(10L, 7L);

        assertEquals(10L, result.getId());
        assertEquals("预约成功，待支付", result.getParam().get("orderStatusString"));
    }

    @Test
    void rejectsOrderOwnedByAnotherUser() {
        when(orderInfoMapper.selectById(10L)).thenReturn(order(10L, 8L));

        assertThrows(YyghException.class,
                () -> orderInfoService.getOrderInfo(10L, 7L));
    }

    private OrderInfo order(Long orderId, Long userId) {
        OrderInfo orderInfo = new OrderInfo();
        orderInfo.setId(orderId);
        orderInfo.setUserId(userId);
        orderInfo.setOrderStatus(OrderStatusEnum.UNPAID.getStatus());
        return orderInfo;
    }
}
