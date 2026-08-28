package com.atguigu.java.ai.langchain4j.tools;

import com.atguigu.java.ai.langchain4j.context.AuthenticatedRequestContext;
import com.atguigu.java.ai.langchain4j.entity.Appointment;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.service.tool.DefaultToolExecutor;
import com.atguigu.java.ai.langchain4j.service.AppointmentService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;

class AppointmentToolsTest {

    private AppointmentTools appointmentTools;
    private AppointmentService appointmentService;
    private MockRestServiceServer mockServer;

    @BeforeEach
    void setUp() {
        appointmentService = mock(AppointmentService.class);
        appointmentTools = new AppointmentTools(appointmentService,
                new RestTemplateBuilder(), "http://localhost:8201/", "http://localhost:8222/");
        RestTemplate restTemplate = (RestTemplate) ReflectionTestUtils
                .getField(appointmentTools, "restTemplate");
        mockServer = MockRestServiceServer.bindTo(restTemplate).build();
    }

    @AfterEach
    void tearDown() {
        AuthenticatedRequestContext.clear();
    }

    @Test
    void reportsExplicitAvailableResponse() {
        mockServer.expect(requestTo("http://localhost:8201/api/hosp/selectSchedule"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("{\"code\":200,\"data\":true}", MediaType.APPLICATION_JSON));

        assertEquals("有可预约号源",
                appointmentTools.querySchedule("内科", "2026-08-26", "上午", null));
        mockServer.verify();
    }

    @Test
    void bindsScheduleQueryThroughLangChain4jToolExecutor() throws Exception {
        mockServer.expect(requestTo("http://localhost:8201/api/hosp/selectSchedule"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("{\"code\":200,\"data\":true}", MediaType.APPLICATION_JSON));
        Method method = AppointmentTools.class.getMethod("querySchedule",
                String.class, String.class, String.class, String.class);
        assertTrue(method.getParameters()[0].isNamePresent());
        assertEquals("department", method.getParameters()[0].getName());
        DefaultToolExecutor executor = new DefaultToolExecutor(appointmentTools, method);
        ToolExecutionRequest request = ToolExecutionRequest.builder()
                .id("query-1")
                .name("query_schedule")
                .arguments("{\"department\":\"内科\",\"date\":\"2026-08-26\",\"time\":\"上午\",\"doctorName\":\"张医生\"}")
                .build();

        assertEquals("有可预约号源", executor.execute(request, null));
        mockServer.verify();
    }

    @Test
    void rejectsMissingRequiredParametersWithoutRemoteCall() {
        assertTrue(appointmentTools.querySchedule("", "2026-08-26", "上午", null)
                .contains("参数不完整"));
        mockServer.verify();
    }

    @Test
    void distinguishesRemoteFailureFromNoAvailability() {
        mockServer.expect(requestTo("http://localhost:8201/api/hosp/selectSchedule"))
                .andRespond(withServerError());

        assertEquals("号源服务暂时不可用，请稍后重试",
                appointmentTools.querySchedule("内科", "2026-08-26", "上午", null));
        mockServer.verify();
    }

    @Test
    void createsOfficialOrderForOwnedPatientWithStableIdempotencyKey() throws Exception {
        AtomicReference<Appointment> savedAppointment = new AtomicReference<>();
        when(appointmentService.getOne(any(Appointment.class))).thenReturn(null);
        when(appointmentService.save(any(Appointment.class))).thenAnswer(invocation -> {
            Appointment saved = invocation.getArgument(0);
            saved.setId(12L);
            savedAppointment.set(saved);
            return true;
        });
        when(appointmentService.updateById(any(Appointment.class))).thenReturn(true);

        mockServer.expect(requestTo("http://localhost:8222/api/user/patient/auth/findAll"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("token", "valid-token"))
                .andRespond(withSuccess("{\"code\":20000,\"data\":{\"list\":[{\"id\":7,\"name\":\"张三\",\"certificatesNo\":\"123456789012345678\"}]}}", MediaType.APPLICATION_JSON));
        mockServer.expect(requestTo("http://localhost:8222/api/hosp/auth/selectSchedule"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("token", "valid-token"))
                .andRespond(withSuccess("{\"code\":20000,\"data\":{\"schedule\":{\"scheduleId\":\"schedule-1\"}}}", MediaType.APPLICATION_JSON));
        mockServer.expect(requestTo("http://localhost:8222/api/order/orderInfo/auth/submitOrder/schedule-1/7"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("token", "valid-token"))
                .andExpect(header("Idempotency-Key", "ai-appointment-12"))
                .andRespond(withSuccess("{\"code\":20000,\"data\":{\"orderId\":88}}", MediaType.APPLICATION_JSON));

        AuthenticatedRequestContext.setToken("valid-token");
        AuthenticatedRequestContext.setUserMessage("确认预约");
        Method method = AppointmentTools.class.getMethod("bookAppointment",
                Long.class, String.class, String.class, String.class,
                String.class, String.class, String.class);
        DefaultToolExecutor executor = new DefaultToolExecutor(appointmentTools, method);
        ToolExecutionRequest request = ToolExecutionRequest.builder()
                .id("book-1")
                .name("book_appointment")
                .arguments("{\"username\":\"张三\",\"idCardLast4\":\"5678\","
                        + "\"department\":\"内科\",\"date\":\"2026-08-26\","
                        + "\"time\":\"上午\"}")
                .build();
        String result = executor.execute(request, 99L);

        assertEquals("预约成功，已创建医院预约挂号平台正式订单，订单号：88", result);
        assertEquals(88L, savedAppointment.get().getPlatformOrderId());
        mockServer.verify();
    }

    @Test
    void requiresLoginBeforeCreatingFormalOrder() {
        AuthenticatedRequestContext.setUserMessage("确认预约");
        String result = appointmentTools.bookAppointment(99L,
                "张三", "5678", "内科", "2026-08-26", "上午", null);

        assertTrue(result.contains("需要先登录"));
        mockServer.verify();
    }

    @Test
    void recoversLostOrderIdBeforeCancellation() {
        Appointment appointment = appointment();
        appointment.setId(12L);
        appointment.setPatientId(7L);
        when(appointmentService.getOne(any(Appointment.class))).thenReturn(appointment);
        when(appointmentService.removeById(12L)).thenReturn(true);

        mockServer.expect(requestTo("http://localhost:8222/api/user/patient/auth/findAll"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("token", "valid-token"))
                .andRespond(withSuccess("{\"code\":20000,\"data\":{\"list\":[{\"id\":7,\"name\":\"张三\",\"certificatesNo\":\"123456789012345678\"}]}}", MediaType.APPLICATION_JSON));
        mockServer.expect(requestTo("http://localhost:8222/api/order/orderInfo/auth/findByIdempotencyKey"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("token", "valid-token"))
                .andExpect(header("Idempotency-Key", "ai-appointment-12"))
                .andRespond(withSuccess("{\"code\":20000,\"data\":{\"orderId\":88}}", MediaType.APPLICATION_JSON));
        mockServer.expect(requestTo("http://localhost:8222/api/order/orderInfo/auth/cancelOrder/88"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("token", "valid-token"))
                .andRespond(withSuccess("{\"code\":20000,\"data\":{\"flag\":true}}", MediaType.APPLICATION_JSON));

        AuthenticatedRequestContext.setToken("valid-token");
        AuthenticatedRequestContext.setUserMessage("确认取消预约");
        String result = appointmentTools.cancelAppointment(99L,
                "张三", "5678", "内科", "2026-08-26", "上午", null);

        assertEquals("取消预约成功", result);
        mockServer.verify();
    }

    @Test
    void rejectsFullCertificateNumberBeforeCallingRemoteServices() {
        String result = appointmentTools.bookAppointment(99L,
                "张三", "123456789012345678", "内科", "2026-08-26", "上午", null);

        assertTrue(result.contains("请勿发送完整证件号"));
        mockServer.verify();
    }

    @Test
    void requiresExplicitConfirmationBeforeCreatingOrder() {
        AuthenticatedRequestContext.setToken("valid-token");

        String result = appointmentTools.bookAppointment(99L,
                "张三", "5678", "内科", "2026-08-26", "上午", null);

        assertTrue(result.contains("确认预约"));
        mockServer.verify();
    }

    private Appointment appointment() {
        Appointment appointment = new Appointment();
        appointment.setUsername("张三");
        appointment.setIdCard("123456789012345678");
        appointment.setDepartment("内科");
        appointment.setDate("2026-08-26");
        appointment.setTime("上午");
        return appointment;
    }

}
