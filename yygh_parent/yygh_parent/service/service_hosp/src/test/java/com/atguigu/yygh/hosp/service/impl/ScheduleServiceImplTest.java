package com.atguigu.yygh.hosp.service.impl;

import com.atguigu.yygh.hosp.repository.ScheduleRepository;
import com.atguigu.yygh.hosp.service.DepartmentService;
import com.atguigu.yygh.hosp.service.HospitalService;
import com.atguigu.yygh.model.hosp.Department;
import com.atguigu.yygh.model.hosp.Hospital;
import com.atguigu.yygh.model.hosp.Schedule;
import org.joda.time.DateTime;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ScheduleServiceImplTest {

    @Mock
    private ScheduleRepository scheduleRepository;

    @Mock
    private DepartmentService departmentService;

    @Mock
    private HospitalService hospitalService;

    @InjectMocks
    private ScheduleServiceImpl scheduleService;

    @Test
    void returnsTrueWhenAnyDoctorHasAvailableSchedule() {
        Department department = department("1000", "200040878");
        Date workDate = new DateTime("2026-08-26").withTimeAtStartOfDay().toDate();
        when(departmentService.findByDepname("内科")).thenReturn(List.of(department));
        Schedule schedule = schedule("1000", "200040878", 8);
        when(scheduleRepository
                .findFirstByHoscodeAndDepcodeAndWorkDateAndWorkTimeAndStatusAndAvailableNumberGreaterThanOrderByAvailableNumberDesc(
                        "1000", "200040878", workDate, 0, 1, 0))
                .thenReturn(schedule);
        stubPackageData(department);

        boolean available = scheduleService.hasAvailableSchedule("内科", "2026-08-26", "上午", null);

        assertThat(available).isTrue();
    }

    @Test
    void filtersByDoctorNameWhenDoctorIsProvided() {
        Department department = department("1000", "200040878");
        Date workDate = new DateTime("2026-08-26").withTimeAtStartOfDay().toDate();
        when(departmentService.findByDepname("内科")).thenReturn(List.of(department));
        Schedule schedule = schedule("1000", "200040878", 3);
        when(scheduleRepository
                .findFirstByHoscodeAndDepcodeAndWorkDateAndWorkTimeAndDocnameAndStatusAndAvailableNumberGreaterThanOrderByAvailableNumberDesc(
                        "1000", "200040878", workDate, 1, "张医生", 1, 0))
                .thenReturn(schedule);
        stubPackageData(department);

        boolean available = scheduleService.hasAvailableSchedule(
                "内科", "2026-08-26", "下午", " 张医生 ");

        assertThat(available).isTrue();
        verify(scheduleRepository, never())
                .findFirstByHoscodeAndDepcodeAndWorkDateAndWorkTimeAndStatusAndAvailableNumberGreaterThanOrderByAvailableNumberDesc(
                        "1000", "200040878", workDate, 1, 1, 0);
    }

    @Test
    void returnsFalseWhenDepartmentDoesNotExist() {
        when(departmentService.findByDepname("不存在的科室")).thenReturn(List.of());

        boolean available = scheduleService.hasAvailableSchedule(
                "不存在的科室", "2026-08-26", "上午", null);

        assertThat(available).isFalse();
    }

    @Test
    void rejectsUnsupportedTime() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> scheduleService.hasAvailableSchedule(
                        "内科", "2026-08-26", "晚上", null))
                .withMessage("时间仅支持上午或下午");
    }

    private Department department(String hoscode, String depcode) {
        Department department = new Department();
        department.setHoscode(hoscode);
        department.setDepcode(depcode);
        return department;
    }

    private Schedule schedule(String hoscode, String depcode, int availableNumber) {
        Schedule schedule = new Schedule();
        schedule.setId("schedule-1");
        schedule.setHoscode(hoscode);
        schedule.setDepcode(depcode);
        schedule.setAvailableNumber(availableNumber);
        return schedule;
    }

    private void stubPackageData(Department department) {
        Hospital hospital = new Hospital();
        hospital.setHosname("测试医院");
        department.setDepname("内科");
        when(hospitalService.getHosp(department.getHoscode())).thenReturn(hospital);
        when(departmentService.getDepartment(department.getHoscode(), department.getDepcode()))
                .thenReturn(department);
    }
}
