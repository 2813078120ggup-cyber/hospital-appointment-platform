package com.atguigu.hospital.controller;


import com.atguigu.hospital.mapper.DoctorMapper;
import com.atguigu.hospital.mapper.HospitalSetMapper;
import com.atguigu.hospital.mapper.OrderInfoMapper;
import com.atguigu.hospital.mapper.ScheduleMapper;
import com.atguigu.hospital.model.Doctor;
import com.atguigu.hospital.model.HospitalSet;
import com.atguigu.hospital.model.OrderInfo;
import com.atguigu.hospital.model.Schedule;
import com.atguigu.hospital.util.MD5;
import com.atguigu.hospital.util.Result;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.OutputStream;
import java.time.LocalDate;
import java.util.Random;

@Controller
public class IndexController {

    private final String LIST_INDEX = "redirect:/";

    private final static String PAGE_INDEX = "frame/index";
    private final static String PAGE_MAIN = "frame/main";
    private final static String PAGE_LOGIN = "frame/login";
    private final static String PAGE_AUTH = "frame/auth";

    @Autowired
    private HospitalSetMapper hospitalSetMapper;

    @Autowired
    private DoctorMapper doctorMapper;

    @Autowired
    private ScheduleMapper scheduleMapper;

    @Autowired
    private OrderInfoMapper orderInfoMapper;

    @Value("${yygh.hospital-manage.admin-username:admin}")
    private String adminUsername;

    @Value("${yygh.hospital-manage.admin-password:111111}")
    private String adminPassword;

    private static final String CAPTCHA_SESSION_KEY = "captchaCode";
    private static final String ADMIN_SESSION_KEY = "hospitalAdmin";

    /**
     * 框架首页
     */
    @RequestMapping(value = "/")
    public String index(ModelMap model, HttpServletRequest request) {
        return PAGE_INDEX;
    }

    /**
     * 框架主页
     */
    @RequestMapping(value = "/main", method = RequestMethod.GET)
    public String main(ModelMap model) {
        HospitalSet hospitalSet = hospitalSetMapper.selectById(1);
        String today = LocalDate.now().toString();
        model.addAttribute("hospitalSet", hospitalSet);
        model.addAttribute("doctorCount", doctorMapper.selectCount(
                new QueryWrapper<Doctor>().eq("status", 1)));
        model.addAttribute("scheduleCount", scheduleMapper.selectCount(
                new QueryWrapper<Schedule>()));
        model.addAttribute("availableScheduleCount", scheduleMapper.selectCount(
                new QueryWrapper<Schedule>().eq("status", 1).ge("work_date", today)));
        model.addAttribute("suspendedScheduleCount", scheduleMapper.selectCount(
                new QueryWrapper<Schedule>().eq("status", -1)));
        model.addAttribute("orderCount", orderInfoMapper.selectCount(
                new QueryWrapper<OrderInfo>()));
        model.addAttribute("paidOrderCount", orderInfoMapper.selectCount(
                new QueryWrapper<OrderInfo>().eq("order_status", 1)));
        model.addAttribute("pendingOrderCount", orderInfoMapper.selectCount(
                new QueryWrapper<OrderInfo>().eq("order_status", 0)));
        model.addAttribute("recentSchedules", scheduleMapper.selectList(
                new QueryWrapper<Schedule>()
                        .orderByDesc("work_date")
                        .orderByAsc("work_time")
                        .last("LIMIT 6")));
        return PAGE_MAIN;
    }

    /**
     * 框架主页
     */
    @RequestMapping(value = "/auth", method = RequestMethod.GET)
    public String auth() {
        return PAGE_AUTH;
    }

    /**
     * 登录
     */
    @RequestMapping(value = "/login", method = RequestMethod.GET)
    public String login() {
        return PAGE_LOGIN;
    }

    /**
     * 兼容院端旧版顶部菜单的退出链接，避免退出后落到 404 页面。
     * 管理端登录仍由现有 admin 入口负责，医生端使用独立会话。
     */
    @RequestMapping(value = {"/logout", "/index/logout", "/acl/logout"}, method = RequestMethod.GET)
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/login";
    }

    /**
     * 生成图形验证码，写入 session 并以 PNG 图片返回。
     */
    @GetMapping("/validate/code")
    public void captcha(HttpServletResponse response, HttpSession session) throws IOException {
        int width = 100;
        int height = 30;
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        Random random = new Random();

        // 背景
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, width, height);

        // 干扰线
        for (int i = 0; i < 5; i++) {
            g.setColor(new Color(random.nextInt(150), random.nextInt(150), random.nextInt(150)));
            g.drawLine(random.nextInt(width), random.nextInt(height),
                    random.nextInt(width), random.nextInt(height));
        }

        // 干扰点
        for (int i = 0; i < 50; i++) {
            g.setColor(new Color(random.nextInt(200), random.nextInt(200), random.nextInt(200)));
            g.fillOval(random.nextInt(width), random.nextInt(height), 2, 2);
        }

        // 4 位验证码字符
        String chars = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ";
        StringBuilder code = new StringBuilder();
        g.setFont(new Font("Arial", Font.BOLD, 22));
        for (int i = 0; i < 4; i++) {
            char c = chars.charAt(random.nextInt(chars.length()));
            code.append(c);
            g.setColor(new Color(random.nextInt(80), random.nextInt(80), random.nextInt(80)));
            g.drawString(String.valueOf(c), 20 + i * 20, 23 + random.nextInt(4));
        }

        g.dispose();
        String captchaValue = code.toString();
        session.setAttribute(CAPTCHA_SESSION_KEY, captchaValue);
        System.out.println("[CAPTCHA] Generated captcha='" + captchaValue + "' session=" + session.getId());

        response.setContentType("image/png");
        response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
        try (OutputStream os = response.getOutputStream()) {
            javax.imageio.ImageIO.write(image, "png", os);
        }
    }

    /**
     * 医院模拟系统管理员登录。
     */
    @PostMapping("/login")
    @ResponseBody
    public Result doLogin(@RequestBody java.util.Map<String, String> params,
                          HttpSession session) {
        String userName = params.get("userName");
        String password = params.get("password");
        String code = params.get("code");

        // 验证码校验
        Object sessionCode = session.getAttribute(CAPTCHA_SESSION_KEY);
        System.out.println("[LOGIN] Submitted code='" + code + "' session code='" + sessionCode + "' session=" + session.getId());
        if (code == null || sessionCode == null || !code.equalsIgnoreCase(sessionCode.toString())) {
            return Result.fail().message("验证码错误");
        }
        session.removeAttribute(CAPTCHA_SESSION_KEY);

        // 账号密码校验
        System.out.println("[LOGIN] adminUsername='" + adminUsername + "' input='" + userName + "' adminPassword='" + adminPassword + "' inputPassword='" + password + "'");
        if (!adminUsername.equals(userName)) {
            return Result.fail().message("用户名或密码错误");
        }
        // 支持明文或 MD5 存储的密码
        boolean passwordMatch = adminPassword.equals(password)
                || MD5.encrypt(password).equals(adminPassword);
        System.out.println("[LOGIN] passwordMatch=" + passwordMatch + " MD5(input)=" + MD5.encrypt(password));
        if (!passwordMatch) {
            return Result.fail().message("用户名或密码错误");
        }

        session.setAttribute(ADMIN_SESSION_KEY, userName);
        return Result.ok().message("登录成功");
    }

}
