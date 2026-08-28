package com.atguigu.java.ai.langchain4j.context;

import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Component;
import org.springframework.web.context.WebApplicationContext;

/**
 * Request 作用域的 Token 持有者。
 * LangChain4j 工具可能在异步线程中执行，ThreadLocal 不可靠；
 * 但 Spring 的 RequestContextHolder 基于 InheritableThreadLocal，
 * 配合 @RequestScope 可让工具在任何线程中都能拿到当前请求的 token。
 */
@Component
@Scope(value = WebApplicationContext.SCOPE_REQUEST, proxyMode = ScopedProxyMode.TARGET_CLASS)
public class RequestScopedTokenHolder {

    private String token;

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }
}
