package com.atguigu.yygh.hosp.controller.api;

import com.atguigu.yygh.common.result.Result;
import com.atguigu.yygh.common.result.R;
import com.atguigu.yygh.hosp.service.ScheduleService;
import com.atguigu.yygh.model.hosp.Schedule;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ApiControllerTest {

    @Mock
    private ScheduleService scheduleService;

    @InjectMocks
    private ApiController apiController;

    @Test
    void returnsSuccessWhenScheduleIsAvailable() {
        MockHttpServletRequest request = request("内科", "2026-08-26", "上午", "张医生");
        when(scheduleService.hasAvailableSchedule("内科", "2026-08-26", "上午", "张医生"))
                .thenReturn(true);

        Result<Boolean> result = apiController.selectSchedule(request);

        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getData()).isTrue();
        assertThat(result.getMessage()).isEqualTo("有可预约号源");
    }

    @Test
    void returnsFailureWhenScheduleIsUnavailable() {
        MockHttpServletRequest request = request("内科", "2026-08-26", "下午", null);
        when(scheduleService.hasAvailableSchedule("内科", "2026-08-26", "下午", null))
                .thenReturn(false);

        Result<Boolean> result = apiController.selectSchedule(request);

        assertThat(result.getCode()).isEqualTo(201);
        assertThat(result.getData()).isFalse();
        assertThat(result.getMessage()).isEqualTo("暂无可预约号源");
    }

    @Test
    void rejectsMissingRequiredParameterBeforeQueryingMongoDb() {
        MockHttpServletRequest request = request("内科", null, "上午", null);

        Result<Boolean> result = apiController.selectSchedule(request);

        assertThat(result.getCode()).isEqualTo(201);
        assertThat(result.getData()).isFalse();
        assertThat(result.getMessage()).isEqualTo("科室名称、日期和时间不能为空");
        verify(scheduleService, org.mockito.Mockito.never())
                .hasAvailableSchedule(org.mockito.ArgumentMatchers.any(),
                        org.mockito.ArgumentMatchers.any(),
                        org.mockito.ArgumentMatchers.any(),
                        org.mockito.ArgumentMatchers.any());
    }

    @Test
    void returnsMinimalScheduleDataForAuthenticatedBookingFlow() {
        MockHttpServletRequest request = request("内科", "2026-08-26", "上午", null);
        Schedule schedule = new Schedule();
        schedule.setId("schedule-1");
        schedule.setHoscode("1000");
        schedule.setDepcode("dept-1");
        schedule.setDocname("张医生");
        schedule.setAvailableNumber(3);
        schedule.getParam().put("hosname", "测试医院");
        schedule.getParam().put("depname", "内科");
        when(scheduleService.findAvailableSchedule("内科", "2026-08-26", "上午", null))
                .thenReturn(schedule);

        R result = apiController.selectAvailableSchedule(request);

        assertThat(result.getCode()).isEqualTo(20000);
        java.util.Map<?, ?> scheduleData = (java.util.Map<?, ?>) result.getData().get("schedule");
        assertThat(scheduleData.get("scheduleId")).isEqualTo("schedule-1");
        assertThat(scheduleData.containsKey("signKey")).isFalse();
    }

    private MockHttpServletRequest request(String name, String date, String time, String doctorName) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        if (name != null) {
            request.addParameter("name", name);
        }
        if (date != null) {
            request.addParameter("date", date);
        }
        if (time != null) {
            request.addParameter("time", time);
        }
        if (doctorName != null) {
            request.addParameter("doctorName", doctorName);
        }
        return request;
    }
}
