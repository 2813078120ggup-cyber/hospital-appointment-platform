package com.atguigu.hospital.service.impl;

import com.atguigu.hospital.mapper.OrderInfoMapper;
import com.atguigu.hospital.mapper.ScheduleMapper;
import com.atguigu.hospital.model.OrderInfo;
import com.atguigu.hospital.model.Schedule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HospitalServiceImplTest {

    @Mock
    private ScheduleMapper scheduleMapper;

    @Mock
    private OrderInfoMapper orderInfoMapper;

    private HospitalServiceImpl hospitalService;

    @BeforeEach
    void setUp() {
        hospitalService = new HospitalServiceImpl();
        ReflectionTestUtils.setField(hospitalService, "hospitalMapper", scheduleMapper);
        ReflectionTestUtils.setField(hospitalService, "orderInfoMapper", orderInfoMapper);
    }

    @Test
    void returnsExistingHospitalOrderWithoutDecrementingScheduleAgain() {
        Schedule schedule = new Schedule();
        schedule.setId(1L);
        schedule.setHoscode("1000");
        schedule.setDepcode("dept-1");
        schedule.setWorkDate("2026-08-26");
        schedule.setWorkTime(0);
        schedule.setStatus(1);
        schedule.setAmount("10");
        schedule.setReservedNumber(10);
        schedule.setAvailableNumber(5);

        OrderInfo existingOrder = new OrderInfo();
        existingOrder.setId(9L);
        existingOrder.setScheduleId(1L);
        existingOrder.setNumber(5);
        existingOrder.setAmount(new BigDecimal("10"));
        existingOrder.setFetchTime("2026-08-26 09:30前");
        existingOrder.setFetchAddress("一楼9号窗口");

        when(scheduleMapper.selectByIdForUpdate(1L)).thenReturn(schedule);
        when(orderInfoMapper.selectOne(any())).thenReturn(existingOrder);

        Map<String, Object> result = hospitalService.submitOrder(request());

        assertEquals(9L, result.get("hosRecordId"));
        assertEquals(5, result.get("availableNumber"));
        verify(scheduleMapper, never()).updateById(any());
        verify(orderInfoMapper, never()).insert(any());
    }

    private Map<String, Object> request() {
        Map<String, Object> request = new HashMap<>();
        request.put("hoscode", "1000");
        request.put("depcode", "dept-1");
        request.put("hosScheduleId", "1");
        request.put("reserveDate", "2026-08-26");
        request.put("reserveTime", "0");
        request.put("amount", "10");
        request.put("platformOrderNo", "AI1234567890123456789012345678");
        return request;
    }
}
