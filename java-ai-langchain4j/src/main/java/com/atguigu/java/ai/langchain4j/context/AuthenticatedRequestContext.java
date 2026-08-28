package com.atguigu.java.ai.langchain4j.context;

/**
 * 功能完善：在单次 AI 请求线程内传递用户令牌。
 * 令牌不会进入提示词、聊天记忆或工具参数，工具只将其转发给平台网关完成鉴权。
 */
public final class AuthenticatedRequestContext {

    private static final ThreadLocal<String> TOKEN_HOLDER = new InheritableThreadLocal<>();
    private static final ThreadLocal<String> USER_MESSAGE_HOLDER = new InheritableThreadLocal<>();

    private AuthenticatedRequestContext() {
    }

    public static void setToken(String token) {
        if (token == null || token.isBlank()) {
            TOKEN_HOLDER.remove();
        } else {
            TOKEN_HOLDER.set(token.trim());
        }
    }

    public static String getToken() {
        return TOKEN_HOLDER.get();
    }

    public static void setUserMessage(String userMessage) {
        if (userMessage == null || userMessage.isBlank()) {
            USER_MESSAGE_HOLDER.remove();
        } else {
            USER_MESSAGE_HOLDER.set(userMessage.trim());
        }
    }

    public static String getUserMessage() {
        return USER_MESSAGE_HOLDER.get();
    }

    public static void clear() {
        TOKEN_HOLDER.remove();
        USER_MESSAGE_HOLDER.remove();
    }
}
