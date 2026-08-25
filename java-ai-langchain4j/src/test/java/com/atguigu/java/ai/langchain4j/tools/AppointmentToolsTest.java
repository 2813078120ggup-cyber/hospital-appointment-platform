package com.atguigu.java.ai.langchain4j.tools;

import com.atguigu.java.ai.langchain4j.service.AppointmentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class AppointmentToolsTest {

    private AppointmentTools appointmentTools;
    private MockRestServiceServer mockServer;

    @BeforeEach
    void setUp() {
        appointmentTools = new AppointmentTools(mock(AppointmentService.class),
                new RestTemplateBuilder(), "http://localhost:8201/");
        RestTemplate restTemplate = (RestTemplate) ReflectionTestUtils
                .getField(appointmentTools, "restTemplate");
        mockServer = MockRestServiceServer.bindTo(restTemplate).build();
    }

    @Test
    void returnsTrueOnlyForExplicitAvailableResponse() {
        mockServer.expect(requestTo("http://localhost:8201/api/hosp/selectSchedule"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("{\"code\":200,\"data\":true}", MediaType.APPLICATION_JSON));

        assertTrue(appointmentTools.querySchedule("内科", "2026-08-26", "上午", null));
        mockServer.verify();
    }

    @Test
    void rejectsMissingRequiredParametersWithoutRemoteCall() {
        assertFalse(appointmentTools.querySchedule("", "2026-08-26", "上午", null));
        mockServer.verify();
    }
}
