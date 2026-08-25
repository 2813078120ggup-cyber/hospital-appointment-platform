package com.atguigu.yygh.user.controller;

import com.atguigu.yygh.common.exception.YyghException;
import com.atguigu.yygh.common.utils.JwtHelper;
import com.atguigu.yygh.model.user.Patient;
import com.atguigu.yygh.user.service.PatientService;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PatientControllerTest {

    @Mock
    private PatientService patientService;

    private PatientController patientController;

    @BeforeAll
    static void configureJwtSecret() {
        System.setProperty("yygh.jwt.secret", "unit-test-jwt-secret-with-at-least-32-bytes");
    }

    @BeforeEach
    void setUp() {
        patientController = new PatientController();
        ReflectionTestUtils.setField(patientController, "patientService", patientService);
    }

    @Test
    void rejectsPatientOwnedByAnotherUser() {
        Patient patient = new Patient();
        patient.setId(11L);
        patient.setUserId(8L);
        when(patientService.getById(11L)).thenReturn(patient);

        assertThrows(YyghException.class,
                () -> patientController.getPatient(11L, requestForUser(7L)));
    }

    @Test
    void forcesAuthenticatedUserIdDuringUpdate() {
        Patient existing = new Patient();
        existing.setId(11L);
        existing.setUserId(7L);
        when(patientService.getById(11L)).thenReturn(existing);
        Patient update = new Patient();
        update.setId(11L);
        update.setUserId(99L);

        patientController.updatePatient(update, requestForUser(7L));

        assertEquals(7L, update.getUserId());
        verify(patientService).updateById(update);
    }

    private MockHttpServletRequest requestForUser(Long userId) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("token", JwtHelper.createToken(userId, "test-user"));
        return request;
    }
}
