package com.atguigu.yygh.orders.service.impl;

import com.atguigu.yygh.hosp.client.HospitalFeignClient;
import com.atguigu.yygh.model.order.OrderCompensationTask;
import com.atguigu.yygh.model.order.OrderInfo;
import com.atguigu.yygh.orders.mapper.OrderCompensationTaskMapper;
import com.atguigu.yygh.orders.mapper.OrderInfoMapper;
import com.atguigu.yygh.orders.mapper.PaymentInfoMapper;
import com.atguigu.yygh.orders.service.CompensationTaskService;
import com.atguigu.yygh.orders.service.OrderInfoService;
import com.atguigu.yygh.rabbit.RabbitService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CompensationTaskServiceImplTest {

    @Mock
    private OrderCompensationTaskMapper taskMapper;

    @Mock
    private OrderInfoMapper orderInfoMapper;

    @Mock
    private PaymentInfoMapper paymentInfoMapper;

    @Mock
    private RabbitService rabbitService;

    @Mock
    private HospitalFeignClient hospitalFeignClient;

    @Mock
    private OrderInfoService orderInfoService;

    private CompensationTaskServiceImpl compensationTaskService;

    @BeforeEach
    void setUp() {
        compensationTaskService = new CompensationTaskServiceImpl();
        ReflectionTestUtils.setField(compensationTaskService, "taskMapper", taskMapper);
        ReflectionTestUtils.setField(compensationTaskService, "orderInfoMapper", orderInfoMapper);
        ReflectionTestUtils.setField(compensationTaskService, "paymentInfoMapper", paymentInfoMapper);
        ReflectionTestUtils.setField(compensationTaskService, "rabbitService", rabbitService);
        ReflectionTestUtils.setField(compensationTaskService, "hospitalFeignClient", hospitalFeignClient);
        ReflectionTestUtils.setField(compensationTaskService, "orderInfoService", orderInfoService);
        ReflectionTestUtils.setField(compensationTaskService, "maxAttempts", 1);
        ReflectionTestUtils.setField(compensationTaskService, "mockHospitalEnabled", true);
        ReflectionTestUtils.setField(compensationTaskService, "reconcileLookbackHours", 24);
    }

    @Test
    void successfulCancellationTaskIsMarkedComplete() {
        OrderCompensationTask task = task(11L, CompensationTaskService.TYPE_CANCEL_FLOW);
        OrderInfo order = order(11L);
        when(taskMapper.selectById(11L)).thenReturn(task);
        when(taskMapper.claim(eq(11L), any(String.class), any(Date.class))).thenReturn(1);
        when(taskMapper.markSuccess(eq(11L), any(String.class))).thenReturn(1);
        when(orderInfoMapper.selectById(11L)).thenReturn(order);
        when(orderInfoService.retryCancellationFromCompensation(11L)).thenReturn(true);

        compensationTaskService.dispatchNow(11L);

        verify(orderInfoService).retryCancellationFromCompensation(11L);
        verify(taskMapper).markSuccess(eq(11L), any(String.class));
        verify(taskMapper, never()).markFailure(any(), any(), any(Integer.class), any(Integer.class),
                any(Date.class), any(String.class));
    }

    @Test
    void failedStockRestoreBecomesDeadLetterAfterMaxAttempts() {
        OrderCompensationTask task = task(12L, CompensationTaskService.TYPE_STOCK_RESTORE);
        OrderInfo order = order(12L);
        order.setScheduleId("schedule-12");
        when(taskMapper.selectById(12L)).thenReturn(task);
        when(taskMapper.claim(eq(12L), any(String.class), any(Date.class))).thenReturn(1);
        when(orderInfoMapper.selectById(12L)).thenReturn(order);
        when(hospitalFeignClient.restoreAvailableNumber("schedule-12")).thenReturn(false);

        compensationTaskService.dispatchNow(12L);

        verify(taskMapper).markFailure(eq(12L), any(String.class), eq(CompensationTaskService.STATUS_DEAD),
                eq(1), any(Date.class), any(String.class));
    }

    @Test
    void reconcileEnqueuesStaleCancellationForRecovery() {
        OrderInfo order = order(13L);
        order.setCancelStatus(3);
        order.setUpdateTime(new Date(System.currentTimeMillis() - 120_000L));
        OrderCompensationTask persisted = task(31L, CompensationTaskService.TYPE_CANCEL_FLOW);
        when(orderInfoMapper.selectList(any())).thenReturn(List.of(order));
        when(paymentInfoMapper.selectList(any())).thenReturn(List.of());
        when(taskMapper.insertPending(any(OrderCompensationTask.class))).thenReturn(1);
        when(taskMapper.selectByTaskKey("CANCEL_FLOW:13")).thenReturn(persisted);
        when(taskMapper.selectByTaskKey("STOCK_RESTORE:13")).thenReturn(persisted);

        compensationTaskService.reconcile();

        ArgumentCaptor<OrderCompensationTask> captor = ArgumentCaptor.forClass(OrderCompensationTask.class);
        verify(taskMapper).insertPending(captor.capture());
        assertEquals("CANCEL_FLOW:13", captor.getValue().getTaskKey());
        assertEquals(CompensationTaskService.TYPE_CANCEL_FLOW, captor.getValue().getTaskType());
    }

    private OrderCompensationTask task(Long id, String taskType) {
        OrderCompensationTask task = new OrderCompensationTask();
        task.setId(id);
        task.setTaskKey(taskType + ":" + id);
        task.setTaskType(taskType);
        task.setOrderId(id);
        task.setStatus(CompensationTaskService.STATUS_PENDING);
        task.setAttemptCount(0);
        task.setNextRetryTime(new Date());
        return task;
    }

    private OrderInfo order(Long id) {
        OrderInfo order = new OrderInfo();
        order.setId(id);
        order.setScheduleId("schedule-" + id);
        order.setOrderStatus(0);
        return order;
    }
}
