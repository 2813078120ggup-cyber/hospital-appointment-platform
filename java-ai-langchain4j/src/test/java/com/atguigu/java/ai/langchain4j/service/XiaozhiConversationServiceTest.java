package com.atguigu.java.ai.langchain4j.service;

import com.atguigu.java.ai.langchain4j.assistant.XiaozhiAgent;
import com.atguigu.java.ai.langchain4j.tools.AppointmentTools;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;

class XiaozhiConversationServiceTest {

    private XiaozhiAgent agent;
    private AppointmentTools tools;
    private XiaozhiConversationService conversationService;

    @BeforeEach
    void setUp() {
        agent = mock(XiaozhiAgent.class);
        tools = mock(AppointmentTools.class);
        conversationService = new XiaozhiConversationService(agent, tools, new ObjectMapper());
    }

    @Test
    void returnsNormalAgentAnswerUnchanged() {
        when(agent.chat(1L, "你好")).thenReturn("你好，我是硅谷小智");

        assertEquals("你好，我是硅谷小智", conversationService.chat(1L, "你好"));
    }

    @Test
    void executesReadOnlyRawScheduleQuerySafely() {
        when(agent.chat(1L, "查询号源")).thenReturn("""
                <query>
                {"name":"query_schedule","arguments":{"department":"内科","date":"2026-08-28","time":"上午"}}
                </query>
                """);
        when(tools.querySchedule("内科", "2026-08-28", "上午", null))
                .thenReturn("号源服务暂时不可用，请稍后重试");

        assertEquals("号源查询结果：号源服务暂时不可用，请稍后重试",
                conversationService.chat(1L, "查询号源"));
    }

    @Test
    void neverExecutesRawBookingWithoutExplicitConfirmation() {
        when(agent.chat(1L, "我想预约")).thenReturn("""
                <query>
                {"name":"book_appointment","arguments":{"username":"张三","department":"内科","date":"2026-08-28","time":"上午"}}
                </query>
                """);

        assertEquals("预约信息已整理，请核对就诊人、科室、日期、时段和医生后回复“确认预约”",
                conversationService.chat(1L, "我想预约"));
        verify(tools, never()).bookAppointment(anyLong(), any(), any(), any(), any(), any(), any());
    }
}
