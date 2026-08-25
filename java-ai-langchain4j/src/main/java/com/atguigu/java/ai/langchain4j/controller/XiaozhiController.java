package com.atguigu.java.ai.langchain4j.controller;

import com.atguigu.java.ai.langchain4j.assistant.XiaozhiAgent;
import com.atguigu.java.ai.langchain4j.bean.ChatForm;
import com.atguigu.java.ai.langchain4j.context.AuthenticatedRequestContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

@Tag(name = "硅谷小智")
@RestController
@RequestMapping("/xiaozhi")
public class XiaozhiController {
    
    @Autowired
    private XiaozhiAgent xiaozhiAgent;
    
    @Operation(summary = "对话")
    @PostMapping("/chat")
    public String chat(@RequestBody ChatForm chatForm,
                       @RequestHeader(value = "token", required = false) String token,
                       @RequestHeader(value = "X-Token", required = false) String xToken) {
        if (chatForm == null || chatForm.getMemoryId() == null
                || !StringUtils.hasText(chatForm.getMessage())) {
            throw new ResponseStatusException(BAD_REQUEST, "memoryId和message不能为空");
        }

        // 功能完善：仅在当前调用链中保存登录令牌，并在 finally 中清理，防止线程池复用造成串号。
        AuthenticatedRequestContext.setToken(StringUtils.hasText(token) ? token : xToken);
        try {
            return xiaozhiAgent.chat(chatForm.getMemoryId(), chatForm.getMessage().trim());
        } finally {
            AuthenticatedRequestContext.clear();
        }
    }
}
