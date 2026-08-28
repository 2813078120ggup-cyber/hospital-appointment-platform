package com.atguigu.java.ai.langchain4j.controller;

import com.atguigu.java.ai.langchain4j.bean.ChatForm;
import com.atguigu.java.ai.langchain4j.context.AuthenticatedRequestContext;
import com.atguigu.java.ai.langchain4j.service.ModelReadinessService;
import com.atguigu.java.ai.langchain4j.service.XiaozhiConversationService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.junit.jupiter.api.Assertions.assertTrue;

class XiaozhiControllerTest {

    @AfterEach
    void tearDown() {
        AuthenticatedRequestContext.clear();
    }

    @Test
    void exposesTokenOnlyDuringCurrentAgentCall() {
        XiaozhiConversationService conversationService = mock(XiaozhiConversationService.class);
        when(conversationService.chat(5L, "预约挂号")).thenAnswer(invocation -> {
            assertEquals("valid-token", AuthenticatedRequestContext.getToken());
            assertEquals("预约挂号", AuthenticatedRequestContext.getUserMessage());
            return "ok";
        });
        XiaozhiController controller = new XiaozhiController();
        ReflectionTestUtils.setField(controller, "xiaozhiConversationService", conversationService);
        ChatForm form = new ChatForm();
        form.setMemoryId(5L);
        form.setMessage(" 预约挂号 ");

        assertEquals("ok", controller.chat(form, "valid-token", null));
        assertNull(AuthenticatedRequestContext.getToken());
        assertNull(AuthenticatedRequestContext.getUserMessage());
    }

    @Test
    void reportsDegradedStatusWhenModelEndpointIsUnavailable() {
        XiaozhiController controller = new XiaozhiController();
        ModelReadinessService readinessService = mock(ModelReadinessService.class);
        when(readinessService.isReachable()).thenReturn(false);
        ReflectionTestUtils.setField(controller, "modelReadinessService", readinessService);

        assertEquals("degraded", ((java.util.Map<?, ?>) controller.status().get("data")).get("status"));
        assertTrue(controller.status().get("message").toString().contains("模型服务未连接"));
    }
}
