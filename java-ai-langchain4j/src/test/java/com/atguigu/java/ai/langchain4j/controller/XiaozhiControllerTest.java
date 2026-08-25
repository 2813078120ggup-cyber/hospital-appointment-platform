package com.atguigu.java.ai.langchain4j.controller;

import com.atguigu.java.ai.langchain4j.assistant.XiaozhiAgent;
import com.atguigu.java.ai.langchain4j.bean.ChatForm;
import com.atguigu.java.ai.langchain4j.context.AuthenticatedRequestContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class XiaozhiControllerTest {

    @AfterEach
    void tearDown() {
        AuthenticatedRequestContext.clear();
    }

    @Test
    void exposesTokenOnlyDuringCurrentAgentCall() {
        XiaozhiAgent agent = mock(XiaozhiAgent.class);
        when(agent.chat(5L, "预约挂号")).thenAnswer(invocation -> {
            assertEquals("valid-token", AuthenticatedRequestContext.getToken());
            return "ok";
        });
        XiaozhiController controller = new XiaozhiController();
        ReflectionTestUtils.setField(controller, "xiaozhiAgent", agent);
        ChatForm form = new ChatForm();
        form.setMemoryId(5L);
        form.setMessage(" 预约挂号 ");

        assertEquals("ok", controller.chat(form, "valid-token", null));
        assertNull(AuthenticatedRequestContext.getToken());
    }
}
