package com.atguigu.yygh.orders.service.impl;

import com.atguigu.yygh.common.exception.YyghException;
import com.atguigu.yygh.enums.OrderStatusEnum;
import com.atguigu.yygh.hosp.client.HospitalFeignClient;
import com.atguigu.yygh.model.order.OrderInfo;
import com.atguigu.yygh.model.user.Patient;
import com.atguigu.yygh.orders.mapper.OrderInfoMapper;
import com.atguigu.yygh.orders.service.CompensationTaskService;
import com.atguigu.yygh.orders.service.WeixinService;
import com.atguigu.yygh.rabbit.RabbitService;
import com.atguigu.yygh.user.client.PatientFeignClient;
import com.atguigu.yygh.vo.hosp.ScheduleOrderVo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;

@ExtendWith(MockitoExtension.class)
class OrderInfoServiceImplTest {

    @Mock
    private OrderInfoMapper orderInfoMapper;

    @Mock
    private WeixinService weixinService;

    @Mock
    private RabbitService rabbitService;

    @Mock
    private HospitalFeignClient hospitalFeignClient;

    @Mock
    private PatientFeignClient patientFeignClient;

    @Mock
    private CompensationTaskService compensationTaskService;

    private OrderInfoServiceImpl orderInfoService;

    @BeforeEach
    void setUp() {
        orderInfoService = new OrderInfoServiceImpl();
        ReflectionTestUtils.setField(orderInfoService, "baseMapper", orderInfoMapper);
        ReflectionTestUtils.setField(orderInfoService, "weixinService", weixinService);
        ReflectionTestUtils.setField(orderInfoService, "rabbitService", rabbitService);
        ReflectionTestUtils.setField(orderInfoService, "hospitalFeignClient", hospitalFeignClient);
        ReflectionTestUtils.setField(orderInfoService, "patientFeignClient", patientFeignClient);
        ReflectionTestUtils.setField(orderInfoService, "compensationTaskService", compensationTaskService);
        ReflectionTestUtils.setField(orderInfoService, "mockHospitalEnabled", true);
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

    @Test
    void returnsExistingOrderForRepeatedIdempotencyKey() {
        OrderInfo existing = order(42L, 7L);
        existing.setPatientId(5L);
        existing.setScheduleId("schedule-1");
        when(orderInfoMapper.selectOne(any())).thenReturn(existing);

        Long orderId = orderInfoService.createOrder(
                "schedule-1", 5L, 7L, "ai-appointment-12");

        assertEquals(42L, orderId);
    }

    @Test
    void findsExistingOrderWithoutCreatingANewOne() {
        OrderInfo existing = order(42L, 7L);
        when(orderInfoMapper.selectOne(any())).thenReturn(existing);

        Long orderId = orderInfoService.findOrderIdByIdempotencyKey(
                7L, "ai-appointment-12");

        assertEquals(42L, orderId);
    }

    @Test
    void adminCancellationRecordsAuditWithoutCallingPaymentForUnpaidOrder() {
        OrderInfo order = order(10L, 7L);
        order.setQuitTime(new Date(System.currentTimeMillis() + 60_000));
        order.setScheduleId("schedule-1");
        order.setPatientPhone("1350000000");
        when(orderInfoMapper.selectById(10L)).thenReturn(order);
        when(orderInfoMapper.update(any(OrderInfo.class), any())).thenReturn(1);
        when(orderInfoMapper.updateById(any(OrderInfo.class))).thenReturn(1);
        when(compensationTaskService.enqueueAndDispatch(
                CompensationTaskService.TYPE_STOCK_RESTORE, 10L)).thenReturn(1L);

        boolean result = orderInfoService.cancelOrderByAdmin(
                10L, "用户来电申请取消预约", "admin");

        assertTrue(result);
        assertEquals(OrderStatusEnum.CANCLE.getStatus(), order.getOrderStatus());
        assertEquals(2, order.getCancelStatus());
        assertEquals(2, order.getCancelSource());
        assertEquals("用户来电申请取消预约", order.getCancelReason());
        assertEquals("admin", order.getCancelOperator());
        verify(weixinService, never()).refund(any());
        verify(rabbitService, never()).sendMessage(any(), any(), any());
        verify(compensationTaskService).enqueueAndDispatch(
                CompensationTaskService.TYPE_STOCK_RESTORE, 10L);
    }

    @Test
    void mockOrderAtomicallyReservesStockBeforeWritingPlatformOrder() {
        Patient patient = patient(5L, 7L);
        ScheduleOrderVo schedule = scheduleOrder("schedule-1");
        when(hospitalFeignClient.getScheduleOrderVo("schedule-1")).thenReturn(schedule);
        when(patientFeignClient.getPatientInfoById(5L)).thenReturn(patient);
        when(hospitalFeignClient.decrementAvailableNumber("schedule-1")).thenReturn(true);
        when(orderInfoMapper.insert(any(OrderInfo.class))).thenAnswer(invocation -> {
            OrderInfo inserted = invocation.getArgument(0);
            inserted.setId(99L);
            return 1;
        });

        Long orderId = orderInfoService.createOrder("schedule-1", 5L, 7L, null);

        assertEquals(99L, orderId);
        verify(hospitalFeignClient).decrementAvailableNumber("schedule-1");
        verify(orderInfoMapper).insert(any(OrderInfo.class));
    }

    @Test
    void mockOrderRejectsSoldOutScheduleWithoutWritingOrder() {
        Patient patient = patient(5L, 7L);
        when(hospitalFeignClient.getScheduleOrderVo("schedule-1"))
                .thenReturn(scheduleOrder("schedule-1"));
        when(patientFeignClient.getPatientInfoById(5L)).thenReturn(patient);
        when(hospitalFeignClient.decrementAvailableNumber("schedule-1")).thenReturn(false);

        assertThrows(YyghException.class,
                () -> orderInfoService.createOrder("schedule-1", 5L, 7L, null));

        verify(orderInfoMapper, never()).insert(any(OrderInfo.class));
    }

    @Test
    void mockOrderCompensatesStockWhenPlatformOrderInsertFails() {
        Patient patient = patient(5L, 7L);
        when(hospitalFeignClient.getScheduleOrderVo("schedule-1"))
                .thenReturn(scheduleOrder("schedule-1"));
        when(patientFeignClient.getPatientInfoById(5L)).thenReturn(patient);
        when(hospitalFeignClient.decrementAvailableNumber("schedule-1")).thenReturn(true);
        when(orderInfoMapper.insert(any(OrderInfo.class))).thenReturn(0);

        assertThrows(YyghException.class,
                () -> orderInfoService.createOrder("schedule-1", 5L, 7L, null));

        verify(hospitalFeignClient).restoreAvailableNumber("schedule-1");
    }

    @Test
    void adminCancellationRequiresOperationalReason() {
        assertThrows(YyghException.class,
                () -> orderInfoService.cancelOrderByAdmin(10L, "短", "admin"));
    }

    private OrderInfo order(Long orderId, Long userId) {
        OrderInfo orderInfo = new OrderInfo();
        orderInfo.setId(orderId);
        orderInfo.setUserId(userId);
        orderInfo.setOrderStatus(OrderStatusEnum.UNPAID.getStatus());
        return orderInfo;
    }

    private Patient patient(Long patientId, Long userId) {
        Patient patient = new Patient();
        patient.setId(patientId);
        patient.setUserId(userId);
        patient.setName("测试患者");
        patient.setPhone("1350000000");
        return patient;
    }

    private ScheduleOrderVo scheduleOrder(String scheduleId) {
        ScheduleOrderVo schedule = new ScheduleOrderVo();
        schedule.setHoscode("10000");
        schedule.setHosname("测试医院");
        schedule.setDepcode("dept-1");
        schedule.setDepname("内科");
        schedule.setHosScheduleId("8");
        schedule.setReserveTime(0);
        schedule.setReserveDate(new Date(System.currentTimeMillis() + 86_400_000L));
        schedule.setQuitTime(new Date(System.currentTimeMillis() + 60_000L));
        schedule.setAmount(java.math.BigDecimal.TEN);
        return schedule;
    }
}
