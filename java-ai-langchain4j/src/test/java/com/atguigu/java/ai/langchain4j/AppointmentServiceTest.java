package com.atguigu.java.ai.langchain4j;

import com.atguigu.java.ai.langchain4j.entity.Appointment;
import com.atguigu.java.ai.langchain4j.service.AppointmentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Transactional
class AppointmentServiceTest {

    @Autowired
    private AppointmentService appointmentService;

    @Test
    void testGetOne() {
        Appointment appointment = new Appointment();
        appointment.setUsername("张三");
        appointment.setIdCard("123456789012345678");
        appointment.setDepartment("内科");
        appointment.setDate("2025-04-14");
        appointment.setTime("上午");
        Appointment appointmentDB = appointmentService.getOne(appointment);
        System.out.println(appointmentDB);
    }

    @Test
    void testSave() {
        Appointment appointment = testAppointment();
        assertTrue(appointmentService.save(appointment));
    }

    @Test
    void testRemoveById() {
        Appointment appointment = testAppointment();
        assertTrue(appointmentService.save(appointment));
        assertTrue(appointmentService.removeById(appointment.getId()));
    }

    private Appointment testAppointment() {
        Appointment appointment = new Appointment();
        appointment.setUsername("测试就诊人");
        String unique = Long.toUnsignedString(System.nanoTime());
        appointment.setIdCard("TEST" + unique.substring(Math.max(0, unique.length() - 14)));
        appointment.setDepartment("测试科室");
        appointment.setDate("2099-12-31");
        appointment.setTime("上午");
        appointment.setDoctorName("测试医生");
        return appointment;
    }

}
