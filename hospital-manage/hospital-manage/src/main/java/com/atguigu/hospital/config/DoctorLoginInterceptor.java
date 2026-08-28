package com.atguigu.hospital.config;

import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 * 医生登录拦截器：未登录访问 /doctor/** 跳转到登录页
 */
public class DoctorLoginInterceptor implements HandlerInterceptor {

    private static final String SESSION_KEY = "doctor";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        HttpSession session = request.getSession(false);
        Object doctor = session == null ? null : session.getAttribute(SESSION_KEY);
        if (doctor == null) {
            response.sendRedirect(request.getContextPath() + "/doctor/login");
            return false;
        }
        return true;
    }
}
