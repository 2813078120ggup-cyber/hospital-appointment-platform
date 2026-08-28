package com.atguigu.yygh.hosp.receiver;

import com.atguigu.yygh.hosp.service.ScheduleService;
import com.atguigu.yygh.rabbit.RabbitService;
import com.atguigu.yygh.vo.order.OrderMqVo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class HospitalReceiverTest {

    @Mock
    private ScheduleService scheduleService;

    @Mock
    private RabbitService rabbitService;

    @InjectMocks
    private HospitalReceiver hospitalReceiver;

    @Test
    void synchronizesBothAuthoritativeStockValues() {
        OrderMqVo message = new OrderMqVo();
        message.setScheduleId("schedule-1");
        message.setReservedNumber(10);
        message.setAvailableNumber(4);

        hospitalReceiver.receiver(message, null, null);

        verify(scheduleService).syncAvailableNumber("schedule-1", 10, 4);
        verify(scheduleService, never()).restoreAvailableNumber("schedule-1");
    }

    @Test
    void restoresOnlyForExplicitCancellationMessage() {
        OrderMqVo message = new OrderMqVo();
        message.setScheduleId("schedule-1");

        hospitalReceiver.receiver(message, null, null);

        verify(scheduleService).restoreAvailableNumber("schedule-1");
        verify(scheduleService, never()).syncAvailableNumber("schedule-1", null, null);
    }

    @Test
    void ignoresPartialStockMessageInsteadOfAddingAFalseSlot() {
        OrderMqVo message = new OrderMqVo();
        message.setScheduleId("schedule-1");
        message.setAvailableNumber(4);

        hospitalReceiver.receiver(message, null, null);

        verify(scheduleService, never()).restoreAvailableNumber("schedule-1");
        verify(scheduleService, never()).syncAvailableNumber("schedule-1", null, 4);
    }
}
