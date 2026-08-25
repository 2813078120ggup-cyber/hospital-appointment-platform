package com.atguigu.yygh.hosp.controller;


import com.atguigu.yygh.common.exception.YyghException;
import com.atguigu.yygh.common.result.R;
import com.atguigu.yygh.common.utils.AuthContextHolder;
import com.atguigu.yygh.common.utils.JwtHelper;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Map;

/**
 * 医院设置表 前端控制器
 */
@RestController
@RequestMapping("/user/hosp")
//@CrossOrigin  //解决跨域
public class LoginController {

    @Value("${yygh.admin.username:admin}")
    private String adminUsername;

    @Value("${yygh.admin.password:}")
    private String adminPassword;

    // 功能完善：管理端凭据由环境变量提供，校验成功后签发短期 JWT。
    @PostMapping("login")
    public R login(@RequestBody Map<String, String> loginForm) {
        if (!StringUtils.hasText(adminPassword)) {
            throw new YyghException(20001, "管理端密码未配置，请设置 YYGH_ADMIN_PASSWORD");
        }

        String username = loginForm == null ? null : loginForm.get("username");
        String password = loginForm == null ? null : loginForm.get("password");
        if (!constantTimeEquals(adminUsername, username)
                || !constantTimeEquals(adminPassword, password)) {
            throw new YyghException(20001, "用户名或密码错误");
        }
        return R.ok().data("token", JwtHelper.createToken(0L, adminUsername));
    }

    // 功能完善：即使绕过网关直连服务，也必须验证当前 token 具有管理员身份。
    @GetMapping("info")
    public R info(HttpServletRequest request) {
        Long userId = AuthContextHolder.getUserId(request);
        if (userId == null || userId != 0L) {
            throw new YyghException(20001, "管理员登录已失效");
        }
        return R.ok().data("roles", "admin")
                .data("introduction", "医院预约挂号平台管理员")
                .data("avatar", "https://wpimg.wallstcn.com/f778738c-e4f8-4870-b634-56703b4acafe.gif")
                .data("name", adminUsername);
    }

    private boolean constantTimeEquals(String expected, String actual) {
        if (expected == null || actual == null) {
            return false;
        }
        return MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8),
                actual.getBytes(StandardCharsets.UTF_8));
    }
}

