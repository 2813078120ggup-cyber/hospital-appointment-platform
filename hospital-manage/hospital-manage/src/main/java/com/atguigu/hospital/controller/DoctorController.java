package com.atguigu.hospital.controller;

import com.atguigu.hospital.mapper.ScheduleMapper;
import com.atguigu.hospital.mapper.OrderInfoMapper;
import com.atguigu.hospital.mapper.PatientMapper;
import com.atguigu.hospital.model.Doctor;
import com.atguigu.hospital.model.OrderInfo;
import com.atguigu.hospital.model.Patient;
import com.atguigu.hospital.model.Schedule;
import com.atguigu.hospital.service.ApiService;
import com.atguigu.hospital.service.DoctorService;
import com.atguigu.hospital.util.Result;
import com.atguigu.hospital.util.YyghException;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpSession;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Controller
@RequestMapping("/doctor")
public class DoctorController extends BaseController {

    private static final String SESSION_KEY = "doctor";
    private static final String PAGE_LOGIN = "doctor/login";
    private static final String PAGE_WORKBENCH = "doctor/workbench";
    private static final String PAGE_SCHEDULE = "doctor/schedule";
    private static final String PAGE_ORDER = "doctor/order";
    private static final String PAGE_PROFILE = "doctor/profile";

    @Autowired
    private DoctorService doctorService;

    @Autowired
    private ScheduleMapper scheduleMapper;

    @Autowired
    private OrderInfoMapper orderInfoMapper;

    @Autowired
    private PatientMapper patientMapper;

    @Autowired
    private ApiService apiService;

    @GetMapping("login")
    public String loginPage() {
        return PAGE_LOGIN;
    }

    @PostMapping("login")
    @ResponseBody
    public Result login(@RequestParam String username,
                        @RequestParam String password,
                        HttpSession session) {
        try {
            Doctor doctor = doctorService.login(username, password);
            session.setAttribute(SESSION_KEY, doctor);
            return Result.ok().message("登录成功");
        } catch (YyghException e) {
            return Result.fail().message(e.getMessage());
        } catch (Exception e) {
            log.error("医生登录失败，username={}", username, e);
            return Result.fail().message("登录失败：" + e.getMessage());
        }
    }

    @GetMapping("logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/doctor/login";
    }

    @GetMapping({"", "workbench"})
    public String workbench(ModelMap model, HttpSession session) {
        Doctor doctor = currentDoctor(session);
        List<Schedule> schedules = findDoctorSchedules(doctor, null, null);
        List<OrderInfo> orders = findDoctorOrders(schedules, null);
        model.addAttribute("doctor", doctor);
        model.addAttribute("scheduleCount", schedules.size());
        model.addAttribute("availableScheduleCount", schedules.stream()
                .filter(item -> Integer.valueOf(1).equals(item.getStatus())).count());
        model.addAttribute("suspendedScheduleCount", schedules.stream()
                .filter(item -> Integer.valueOf(-1).equals(item.getStatus())).count());
        model.addAttribute("orderCount", orders.size());
        model.addAttribute("paidOrderCount", orders.stream()
                .filter(item -> Integer.valueOf(1).equals(item.getOrderStatus())).count());
        model.addAttribute("recentSchedules", schedules.stream().limit(6).collect(Collectors.toList()));
        model.addAttribute("recentOrders", orders.stream().limit(6).collect(Collectors.toList()));
        return PAGE_WORKBENCH;
    }

    //我的排班
    @GetMapping("schedule")
    public String schedule(ModelMap model,
                           HttpSession session,
                           @RequestParam(required = false) String workDate,
                           @RequestParam(required = false) Integer status) {
        Doctor doctor = currentDoctor(session);
        List<Schedule> list = findDoctorSchedules(doctor, workDate, status);
        model.addAttribute("doctor", doctor);
        model.addAttribute("list", list);
        model.addAttribute("workDate", workDate);
        model.addAttribute("status", status);
        return PAGE_SCHEDULE;
    }

    @GetMapping("order")
    public String order(ModelMap model,
                        HttpSession session,
                        @RequestParam(required = false) Integer orderStatus) {
        Doctor doctor = currentDoctor(session);
        List<Schedule> schedules = findDoctorSchedules(doctor, null, null);
        List<OrderInfo> list = findDoctorOrders(schedules, orderStatus);
        model.addAttribute("doctor", doctor);
        model.addAttribute("list", list);
        model.addAttribute("orderStatus", orderStatus);
        return PAGE_ORDER;
    }

    @GetMapping("profile")
    public String profile(ModelMap model, HttpSession session) {
        model.addAttribute("doctor", currentDoctor(session));
        return PAGE_PROFILE;
    }

    @PostMapping("profile")
    @ResponseBody
    public Result updateProfile(@RequestParam(required = false) String title,
                                @RequestParam(required = false) String phone,
                                HttpSession session) {
        try {
            Doctor doctor = currentDoctor(session);
            Doctor updated = doctorService.updateProfile(doctor.getId(), title, phone);
            session.setAttribute(SESSION_KEY, updated);
            return Result.ok().message("个人资料已更新");
        } catch (YyghException exception) {
            return Result.fail().message(exception.getMessage());
        }
    }

    @PostMapping("password")
    @ResponseBody
    public Result changePassword(@RequestParam String currentPassword,
                                 @RequestParam String newPassword,
                                 @RequestParam String confirmPassword,
                                 HttpSession session) {
        try {
            doctorService.changePassword(currentDoctor(session).getId(), currentPassword,
                    newPassword, confirmPassword);
            return Result.ok().message("密码修改成功");
        } catch (YyghException exception) {
            return Result.fail().message(exception.getMessage());
        }
    }

    //停诊(-1) / 恢复(1)
    @PostMapping("suspend")
    @ResponseBody
    public Result suspend(@RequestParam Long scheduleId,
                          @RequestParam Integer status,
                          HttpSession session) {
        try {
            Doctor doctor = currentDoctor(session);
            Schedule schedule = scheduleMapper.selectById(scheduleId);
            if (schedule == null) {
                throw new YyghException("排班不存在", 201);
            }
            if (!doctor.getDocname().equals(schedule.getDocname())) {
                throw new YyghException("无权操作他人排班", 201);
            }
            if (status != -1 && status != 1) {
                throw new YyghException("排班状态不正确", 201);
            }
            // 先确认平台同步成功，再更新院内状态，避免平台失败时本地先显示成功。
            apiService.suspendSchedule(String.valueOf(schedule.getId()), status);
            schedule.setStatus(status);
            scheduleMapper.updateById(schedule);
            return Result.ok();
        } catch (YyghException e) {
            return Result.fail().message(e.getMessage());
        } catch (Exception e) {
            log.error("排班停诊/恢复失败，scheduleId={}, status={}", scheduleId, status, e);
            return Result.fail().message("操作失败：" + e.getMessage());
        }
    }

    private Doctor currentDoctor(HttpSession session) {
        Doctor doctor = (Doctor) session.getAttribute(SESSION_KEY);
        if (doctor == null) {
            throw new YyghException("医生登录状态已失效", 201);
        }
        return doctor;
    }

    private List<Schedule> findDoctorSchedules(Doctor doctor, String workDate, Integer status) {
        QueryWrapper<Schedule> wrapper = new QueryWrapper<Schedule>()
                .eq("docname", doctor.getDocname());
        if (workDate != null && !workDate.trim().isEmpty()) {
            wrapper.eq("work_date", workDate.trim());
        }
        if (status != null) {
            wrapper.eq("status", status);
        }
        wrapper.orderByDesc("work_date").orderByAsc("work_time");
        return scheduleMapper.selectList(wrapper);
    }

    private List<OrderInfo> findDoctorOrders(List<Schedule> schedules, Integer orderStatus) {
        if (schedules.isEmpty()) {
            return new ArrayList<>();
        }
        List<Long> scheduleIds = schedules.stream().map(Schedule::getId).collect(Collectors.toList());
        QueryWrapper<OrderInfo> wrapper = new QueryWrapper<OrderInfo>().in("schedule_id", scheduleIds);
        if (orderStatus != null) {
            wrapper.eq("order_status", orderStatus);
        }
        wrapper.orderByDesc("create_time").orderByDesc("id");
        List<OrderInfo> orders = orderInfoMapper.selectList(wrapper);
        Map<Long, Schedule> scheduleMap = new HashMap<>();
        for (Schedule schedule : schedules) {
            scheduleMap.put(schedule.getId(), schedule);
        }
        for (OrderInfo order : orders) {
            order.getParam().put("schedule", scheduleMap.get(order.getScheduleId()));
        }
        attachPatients(orders);
        return orders;
    }

    private void attachPatients(List<OrderInfo> orders) {
        List<Long> patientIds = orders.stream()
                .map(OrderInfo::getPatientId)
                .filter(id -> id != null)
                .distinct()
                .collect(Collectors.toList());
        if (patientIds.isEmpty()) {
            return;
        }
        Map<Long, Patient> patients = new HashMap<>();
        for (Patient patient : patientMapper.selectBatchIds(patientIds)) {
            patients.put(patient.getId(), patient);
        }
        for (OrderInfo order : orders) {
            order.getParam().put("patient", patients.get(order.getPatientId()));
        }
    }
}
