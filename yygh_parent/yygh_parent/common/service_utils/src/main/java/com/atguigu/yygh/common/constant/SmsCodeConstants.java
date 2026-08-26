package com.atguigu.yygh.common.constant;

import java.time.Duration;

/**
 * 短信登录验证码在发送服务和用户服务之间共享的 Redis 约定。
 */
public final class SmsCodeConstants {

    public static final String LOGIN_CODE_PREFIX = "sms:login:code:";
    public static final String LOGIN_COOLDOWN_PREFIX = "sms:login:cooldown:";
    public static final Duration LOGIN_CODE_TTL = Duration.ofMinutes(5);
    public static final Duration LOGIN_COOLDOWN_TTL = Duration.ofSeconds(60);

    private SmsCodeConstants() {
    }

    public static String loginCodeKey(String phone) {
        return LOGIN_CODE_PREFIX + phone;
    }

    public static String loginCooldownKey(String phone) {
        return LOGIN_COOLDOWN_PREFIX + phone;
    }
}
