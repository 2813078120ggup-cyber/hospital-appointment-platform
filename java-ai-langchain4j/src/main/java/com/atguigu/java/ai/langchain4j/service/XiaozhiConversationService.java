package com.atguigu.java.ai.langchain4j.service;

import com.atguigu.java.ai.langchain4j.assistant.XiaozhiAgent;
import com.atguigu.java.ai.langchain4j.tools.AppointmentTools;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Slf4j
public class XiaozhiConversationService {

    private static final Pattern RAW_TOOL_CALL = Pattern.compile(
            "<query>\\s*(\\{.*})\\s*</query>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private static final Pattern BOOK_CONFIRMATION = Pattern.compile(
            "^(确认|确认预约|确认挂号|同意预约|请预约|现在预约)[。！!\\s]*$");
    private static final Pattern CANCEL_CONFIRMATION = Pattern.compile(
            "^(确认取消|确认取消预约|同意取消|请取消)[。！!\\s]*$");

    private final XiaozhiAgent xiaozhiAgent;
    private final AppointmentTools appointmentTools;
    private final ObjectMapper objectMapper;

    public XiaozhiConversationService(XiaozhiAgent xiaozhiAgent,
                                      AppointmentTools appointmentTools,
                                      ObjectMapper objectMapper) {
        this.xiaozhiAgent = xiaozhiAgent;
        this.appointmentTools = appointmentTools;
        this.objectMapper = objectMapper;
    }

    public String chat(Long memoryId, String userMessage) {
        String answer = xiaozhiAgent.chat(memoryId, userMessage);
        Matcher matcher = RAW_TOOL_CALL.matcher(answer == null ? "" : answer.trim());
        if (!matcher.matches()) {
            return answer;
        }

        // 小参数模型偶尔会把工具调用作为文本返回。这里只解析平台白名单工具，绝不把原始调用展示给用户。
        try {
            JsonNode toolCall = objectMapper.readTree(matcher.group(1));
            String toolName = text(toolCall, "name");
            JsonNode arguments = toolCall.path("arguments");
            if ("query_schedule".equals(toolName) || "查询是否有号源".equals(toolName)) {
                return "号源查询结果：" + appointmentTools.querySchedule(
                        text(arguments, "department"),
                        text(arguments, "date"),
                        text(arguments, "time"),
                        text(arguments, "doctorName"));
            }
            if ("book_appointment".equals(toolName) || "预约挂号".equals(toolName)) {
                if (!BOOK_CONFIRMATION.matcher(userMessage.trim()).matches()) {
                    return "预约信息已整理，请核对就诊人、科室、日期、时段和医生后回复“确认预约”";
                }
                return appointmentTools.bookAppointment(
                        memoryId,
                        text(arguments, "username"),
                        text(arguments, "idCardLast4"),
                        text(arguments, "department"),
                        text(arguments, "date"),
                        text(arguments, "time"),
                        text(arguments, "doctorName"));
            }
            if ("cancel_appointment".equals(toolName) || "取消预约挂号".equals(toolName)) {
                if (!CANCEL_CONFIRMATION.matcher(userMessage.trim()).matches()) {
                    return "取消信息已整理，请核对对应预约后回复“确认取消预约”";
                }
                return appointmentTools.cancelAppointment(
                        memoryId,
                        text(arguments, "username"),
                        text(arguments, "idCardLast4"),
                        text(arguments, "department"),
                        text(arguments, "date"),
                        text(arguments, "time"),
                        text(arguments, "doctorName"));
            }
        } catch (Exception exception) {
            log.warn("硅谷小智原始工具调用解析失败，memoryId={}", memoryId, exception);
        }
        return "我没有正确识别这次操作，请换一种说法再试；预约和取消操作不会自动执行";
    }

    private String text(JsonNode node, String field) {
        JsonNode value = node == null ? null : node.get(field);
        return value == null || value.isNull() ? null : value.asText();
    }
}
