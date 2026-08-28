package com.atguigu.java.ai.langchain4j.context;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 基于 memoryId 的会话级 Token 注册表。
 * LangChain4j 的工具调用可能在异步线程执行，ThreadLocal 和 RequestContextHolder
 * 都不可靠。但每个 @Tool 方法都有 @ToolMemoryId Long memoryId 参数，
 * 所以可以用 memoryId 作为 key，在 Controller 入口设置 token，工具执行时读取。
 * <p>
 * token 在会话结束后（Controller finally）清除，防止内存泄漏。
 */
public final class SessionTokenRegistry {

    private static final Map<Long, String> TOKENS = new ConcurrentHashMap<>();

    private SessionTokenRegistry() {
    }

    public static void register(Long memoryId, String token) {
        if (memoryId == null) return;
        if (token == null || token.isBlank()) {
            TOKENS.remove(memoryId);
        } else {
            TOKENS.put(memoryId, token.trim());
        }
    }

    public static String get(Long memoryId) {
        return memoryId == null ? null : TOKENS.get(memoryId);
    }

    public static void remove(Long memoryId) {
        if (memoryId != null) {
            TOKENS.remove(memoryId);
        }
    }
}
