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
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

@ExtendWith(MockitoExtension.class)
class ScheduleServiceImplTest {

    @Mock
    private ScheduleRepository scheduleRepository;

    @Mock
    private MongoTemplate mongoTemplate;

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

    @Test
    void suspendAndResumePreserveExistingAvailableNumber() {
        Schedule stopped = schedule("1000", "200040878", 3);
        stopped.setHosScheduleId("hospital-schedule-1");
        stopped.setReservedNumber(10);
        stopped.setStatus(-1);
        when(mongoTemplate.findAndModify(any(Query.class), any(Update.class),
                any(FindAndModifyOptions.class), eq(Schedule.class))).thenReturn(stopped);

        scheduleService.suspend("1000", "hospital-schedule-1", -1);
        scheduleService.suspend("1000", "hospital-schedule-1", 1);

        verify(mongoTemplate, org.mockito.Mockito.times(2)).findAndModify(
                any(Query.class), any(Update.class), any(FindAndModifyOptions.class), eq(Schedule.class));
        verify(scheduleRepository, never()).save(any(Schedule.class));
    }

    @Test
    void decrementsOnlyWhenMongoAtomicConditionMatches() {
        Schedule updated = schedule("1000", "200040878", 2);
        when(mongoTemplate.findAndModify(any(Query.class), any(Update.class),
                any(FindAndModifyOptions.class), eq(Schedule.class))).thenReturn(updated);

        assertThat(scheduleService.decrementAvailableNumber("schedule-1")).isTrue();

        org.mockito.ArgumentCaptor<Query> query = org.mockito.ArgumentCaptor.forClass(Query.class);
        org.mockito.ArgumentCaptor<Update> update = org.mockito.ArgumentCaptor.forClass(Update.class);
        verify(mongoTemplate).findAndModify(query.capture(), update.capture(),
                any(FindAndModifyOptions.class), eq(Schedule.class));
        assertThat(query.getValue().getQueryObject().get("status")).isEqualTo(1);
        assertThat(query.getValue().getQueryObject().get("availableNumber").toString())
                .contains("$gt");
        assertThat(update.getValue().getUpdateObject().get("$inc").toString())
                .contains("-1");
    }

    @Test
    void concurrentAtomicDecrementsCannotExceedStock() throws Exception {
        int initialStock = 12;
        AtomicInteger stock = new AtomicInteger(initialStock);
        Object mongoLock = new Object();
        when(mongoTemplate.findAndModify(any(Query.class), any(Update.class),
                any(FindAndModifyOptions.class), eq(Schedule.class))).thenAnswer(invocation -> {
            synchronized (mongoLock) {
                if (stock.get() <= 0) {
                    return null;
                }
                Schedule updated = schedule("1000", "200040878", stock.decrementAndGet());
                updated.setReservedNumber(initialStock);
                return updated;
            }
        });

        int callers = 64;
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(16);
        try {
            List<Future<Boolean>> results = IntStream.range(0, callers)
                    .mapToObj(index -> executor.submit(() -> {
                        start.await(5, TimeUnit.SECONDS);
                        return scheduleService.decrementAvailableNumber("schedule-1");
                    }))
                    .toList();
            start.countDown();
            long successCount = results.stream().filter(future -> {
                try {
                    return future.get(5, TimeUnit.SECONDS);
                } catch (Exception exception) {
                    throw new AssertionError(exception);
                }
            }).count();
            assertThat(successCount).isEqualTo(initialStock);
            assertThat(stock.get()).isZero();
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void concurrentRestoresCannotExceedReservedNumber() throws Exception {
        int reserved = 12;
        AtomicInteger stock = new AtomicInteger(0);
        Object mongoLock = new Object();
        when(scheduleRepository.findById("schedule-1")).thenAnswer(invocation -> {
            Schedule current = schedule("1000", "200040878", stock.get());
            current.setReservedNumber(reserved);
            return Optional.of(current);
        });
        when(mongoTemplate.findAndModify(any(Query.class), any(Update.class),
                any(FindAndModifyOptions.class), eq(Schedule.class))).thenAnswer(invocation -> {
            synchronized (mongoLock) {
                Map<String, Object> query = invocation.<Query>getArgument(0).getQueryObject();
                Object expectedAvailable = query.get("availableNumber");
                Object expectedReserved = query.get("reservedNumber");
                if (!(expectedAvailable instanceof Number)
                        || ((Number) expectedAvailable).intValue() != stock.get()
                        || !(expectedReserved instanceof Number)
                        || ((Number) expectedReserved).intValue() != reserved) {
                    return null;
                }
                if (stock.get() >= reserved) {
                    return null;
                }
                Schedule updated = schedule("1000", "200040878", stock.incrementAndGet());
                updated.setReservedNumber(reserved);
                return updated;
            }
        });

        int callers = 64;
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(16);
        try {
            List<Future<Boolean>> results = IntStream.range(0, callers)
                    .mapToObj(index -> executor.submit(() -> {
                        start.await(5, TimeUnit.SECONDS);
                        return scheduleService.restoreAvailableNumber("schedule-1");
                    }))
                    .toList();
            start.countDown();
            long successCount = results.stream().filter(future -> {
                try {
                    return future.get(5, TimeUnit.SECONDS);
                } catch (Exception exception) {
                    throw new AssertionError(exception);
                }
            }).count();
            assertThat(successCount).isEqualTo(reserved);
            assertThat(stock.get()).isEqualTo(reserved);
        } finally {
            executor.shutdownNow();
        }
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
