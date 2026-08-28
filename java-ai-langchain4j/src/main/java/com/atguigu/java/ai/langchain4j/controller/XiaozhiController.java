package com.atguigu.java.ai.langchain4j.controller;

import com.atguigu.java.ai.langchain4j.bean.ChatForm;
import com.atguigu.java.ai.langchain4j.context.AuthenticatedRequestContext;
import com.atguigu.java.ai.langchain4j.context.SessionTokenRegistry;
import com.atguigu.java.ai.langchain4j.service.ModelReadinessService;
import com.atguigu.java.ai.langchain4j.service.XiaozhiConversationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE;

@Tag(name = "硅谷小智")
@RestController
@RequestMapping("/xiaozhi")
@Slf4j
public class XiaozhiController {
    
    @Autowired
    private XiaozhiConversationService xiaozhiConversationService;

    @Autowired
    private ModelReadinessService modelReadinessService;

    @Operation(summary = "服务状态")
    @GetMapping("/status")
    public Map<String, Object> status() {
        boolean modelReady = modelReadinessService.isReachable();
        return Map.of(
                "code", 20000,
                "message", modelReady ? "硅谷小智服务已就绪" : "Agent 已启动，模型服务未连接",
                "data", Map.of(
                        "name", "硅谷小智",
                        "status", modelReady ? "ready" : "degraded",
                        "modelStatus", modelReady ? "online" : "offline",
                        "capabilities", List.of("智能导诊", "号源查询", "预约挂号", "取消预约")));
    }
    
    @Operation(summary = "对话")
    @PostMapping("/chat")
    public String chat(@RequestBody ChatForm chatForm,
                       @RequestHeader(value = "token", required = false) String token,
                       @RequestHeader(value = "X-Token", required = false) String xToken) {
        if (chatForm == null || chatForm.getMemoryId() == null
                || !StringUtils.hasText(chatForm.getMessage())) {
            throw new ResponseStatusException(BAD_REQUEST, "memoryId和message不能为空");
        }

        // 功能完善：同时在 ThreadLocal 和 SessionTokenRegistry 中保存令牌，
        // 防止 LangChain4j 异步工具执行时 ThreadLocal 丢失。
        String effectiveToken = StringUtils.hasText(token) ? token : xToken;
        log.info("【Token诊断】Controller 入口, memoryId={}, token={}", chatForm.getMemoryId(), StringUtils.hasText(effectiveToken) ? effectiveToken.substring(0, Math.min(8, effectiveToken.length())) + "..." : "null");
        AuthenticatedRequestContext.setToken(effectiveToken);
        AuthenticatedRequestContext.setUserMessage(chatForm.getMessage());
        SessionTokenRegistry.register(chatForm.getMemoryId(), effectiveToken);
        log.info("【Token诊断】SessionTokenRegistry 已注册, memoryId={}", chatForm.getMemoryId());
        try {
            return xiaozhiConversationService.chat(chatForm.getMemoryId(), chatForm.getMessage().trim());
        } catch (RuntimeException exception) {
            log.error("硅谷小智模型调用失败，memoryId={}", chatForm.getMemoryId(), exception);
            throw new ResponseStatusException(
                    SERVICE_UNAVAILABLE, "硅谷小智暂时无法连接模型服务，请确认模型已启动后重试");
        } finally {
            AuthenticatedRequestContext.clear();
            SessionTokenRegistry.remove(chatForm.getMemoryId());
        }
    }
}
