package com.atguigu.yygh.msm.controller;

import com.atguigu.yygh.common.constant.SmsCodeConstants;
import com.atguigu.yygh.common.exception.YyghException;
import com.atguigu.yygh.common.result.R;
import com.atguigu.yygh.msm.service.MsmService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.SecureRandom;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/api/msm")
public class MsmController {

    private static final Pattern PHONE_PATTERN = Pattern.compile("^1[3-9]\\d{9}$");
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    @Autowired
    private MsmService msmService;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    //根据手机号发送短信验证码
    @GetMapping(value = "/send/{phone}")
    public R code(@PathVariable String phone) {
        if (phone == null || !PHONE_PATTERN.matcher(phone).matches()) {
            throw new YyghException(20001, "手机号格式不正确");
        }

        String cooldownKey = SmsCodeConstants.loginCooldownKey(phone);
        // 功能完善：先用 Redis 原子占位限制一分钟内重复发送，避免短信接口被刷。
        Boolean acquired = redisTemplate.opsForValue().setIfAbsent(
                cooldownKey,
                "1",
                SmsCodeConstants.LOGIN_COOLDOWN_TTL);
        if (!Boolean.TRUE.equals(acquired)) {
            throw new YyghException(20001, "验证码发送过于频繁，请稍后再试");
        }

        String code = String.format("%06d", SECURE_RANDOM.nextInt(1_000_000));
        try {
            if (!msmService.sendMsm(phone, code)) {
                throw new YyghException(20001, "验证码发送失败");
            }
            // 功能完善：只有供应商确认发送成功后才保存验证码，五分钟后由 Redis 自动过期。
            redisTemplate.opsForValue().set(
                    SmsCodeConstants.loginCodeKey(phone),
                    code,
                    SmsCodeConstants.LOGIN_CODE_TTL);
            return R.ok();
        } catch (RuntimeException exception) {
            // 发送失败时释放冷却键，允许用户修复配置后立即重试。
            redisTemplate.delete(cooldownKey);
            throw exception;
        }

    }
}
