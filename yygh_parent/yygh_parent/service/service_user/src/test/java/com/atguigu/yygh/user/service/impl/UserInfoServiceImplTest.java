package com.atguigu.yygh.user.service.impl;

import com.atguigu.yygh.common.exception.YyghException;
import com.atguigu.yygh.common.utils.JwtHelper;
import com.atguigu.yygh.model.user.UserInfo;
import com.atguigu.yygh.user.mapper.UserInfoMapper;
import com.atguigu.yygh.vo.user.LoginVo;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserInfoServiceImplTest {

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private UserInfoMapper userInfoMapper;

    private UserInfoServiceImpl userInfoService;

    @BeforeAll
    static void configureJwtSecret() {
        System.setProperty("yygh.jwt.secret", "unit-test-jwt-secret-with-at-least-32-bytes");
    }

    @BeforeEach
    void setUp() {
        userInfoService = new UserInfoServiceImpl();
        ReflectionTestUtils.setField(userInfoService, "redisTemplate", redisTemplate);
        ReflectionTestUtils.setField(userInfoService, "baseMapper", userInfoMapper);
    }

    @Test
    void rejectsCodeThatWasNotAtomicallyConsumed() {
        LoginVo loginVo = loginVo("19900000001", "6666");
        when(redisTemplate.execute(
                any(DefaultRedisScript.class),
                anyList(),
                eq("6666"))).thenReturn(0L);

        assertThrows(YyghException.class, () -> userInfoService.loginUser(loginVo));

        verify(userInfoMapper, never()).selectOne(any());
    }

    @Test
    void returnsTokenAfterOneTimeCodeIsConsumed() {
        LoginVo loginVo = loginVo("19900000001", "123456");
        when(redisTemplate.execute(
                any(DefaultRedisScript.class),
                anyList(),
                eq("123456"))).thenReturn(1L);
        UserInfo userInfo = new UserInfo();
        userInfo.setId(7L);
        userInfo.setPhone("19900000001");
        userInfo.setStatus(1);
        when(userInfoMapper.selectOne(any())).thenReturn(userInfo);

        Map<String, Object> result = userInfoService.loginUser(loginVo);

        assertEquals("19900000001", result.get("name"));
        assertNotNull(result.get("token"));
        assertEquals(7L, JwtHelper.getUserId(result.get("token").toString()));
    }

    private LoginVo loginVo(String phone, String code) {
        LoginVo loginVo = new LoginVo();
        loginVo.setPhone(phone);
        loginVo.setCode(code);
        return loginVo;
    }
}
