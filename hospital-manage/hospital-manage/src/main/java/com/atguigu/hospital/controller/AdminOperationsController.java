package com.atguigu.hospital.controller;

import com.atguigu.hospital.mapper.DoctorMapper;
import com.atguigu.hospital.mapper.OrderInfoMapper;
import com.atguigu.hospital.mapper.PatientMapper;
import com.atguigu.hospital.mapper.ScheduleMapper;
import com.atguigu.hospital.model.Doctor;
import com.atguigu.hospital.model.OrderInfo;
import com.atguigu.hospital.model.Patient;
import com.atguigu.hospital.model.Schedule;
import com.atguigu.hospital.util.MD5;
import com.atguigu.hospital.util.Result;
import com.atguigu.hospital.util.YyghException;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 医院管理员的院内运营功能：医生账号和医院侧预约记录。
 */
@Controller
@RequestMapping("/admin")
public class AdminOperationsController extends BaseController {

    @Autowired
    private DoctorMapper doctorMapper;

    @Autowired
    private OrderInfoMapper orderInfoMapper;

    @Autowired
    private PatientMapper patientMapper;

    @Autowired
    private ScheduleMapper scheduleMapper;

    @Value("${yygh.hospital-manage.doctor-default-password:123456}")
    private String doctorDefaultPassword;

    @RequestMapping("/doctor/list")
    public String doctorList(ModelMap model,
                             @RequestParam(defaultValue = "1") int pageNum,
                             @RequestParam(defaultValue = "10") int pageSize,
                             @RequestParam(required = false) String keyword,
                             @RequestParam(required = false) Integer status) {
        QueryWrapper<Doctor> wrapper = new QueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            String value = keyword.trim();
            wrapper.and(item -> item.like("docname", value)
                    .or().like("username", value)
                    .or().like("title", value));
        }
        if (status != null) {
            wrapper.eq("status", status);
        }
        wrapper.orderByAsc("docname").orderByAsc("id");
        IPage<Doctor> page = doctorMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        model.addAttribute("list", page.getRecords());
        model.addAttribute("total", page.getTotal());
        model.addAttribute("pageNum", pageNum);
        model.addAttribute("pageSize", pageSize);
        model.addAttribute("totalPages", page.getPages());
        model.addAttribute("keyword", keyword);
        model.addAttribute("status", status);
        return "admin/doctor/list";
    }

    @GetMapping("/doctor/form")
    public String doctorForm(ModelMap model, @RequestParam(required = false) Long id) {
        model.addAttribute("doctor", id == null ? new Doctor() : doctorMapper.selectById(id));
        return "admin/doctor/form";
    }

    @PostMapping("/doctor/save")
    public String saveDoctor(Doctor form,
                             @RequestParam(required = false) String loginPassword,
                             HttpServletRequest request) {
        try {
            if (!StringUtils.hasText(form.getDocname())) {
                throw new YyghException("请输入医生姓名", 201);
            }
            String doctorName = form.getDocname().trim();
            QueryWrapper<Doctor> duplicate = new QueryWrapper<Doctor>().eq("docname", doctorName);
            if (form.getId() != null) {
                duplicate.ne("id", form.getId());
            }
            if (doctorMapper.selectCount(duplicate) > 0) {
                throw new YyghException("该医生姓名已存在", 201);
            }

            Doctor target = form.getId() == null ? new Doctor() : doctorMapper.selectById(form.getId());
            if (target == null) {
                throw new YyghException("医生账号不存在", 201);
            }
            target.setDocname(doctorName);
            // 当前项目约定医生姓名就是登录账号，避免同一人员出现两套登录标识。
            target.setUsername(doctorName);
            target.setTitle(StringUtils.hasText(form.getTitle()) ? form.getTitle().trim() : null);
            target.setPhone(StringUtils.hasText(form.getPhone()) ? form.getPhone().trim() : null);
            target.setStatus(form.getStatus() == null ? 1 : form.getStatus());
            if (form.getId() == null) {
                String password = StringUtils.hasText(loginPassword)
                        ? loginPassword : doctorDefaultPassword;
                validatePassword(password);
                target.setPassword(MD5.encrypt(password));
                doctorMapper.insert(target);
            } else {
                if (StringUtils.hasText(loginPassword)) {
                    validatePassword(loginPassword);
                    target.setPassword(MD5.encrypt(loginPassword));
                }
                doctorMapper.updateById(target);
            }
            return successPage("医生账号已保存", request);
        } catch (YyghException exception) {
            return failurePage(exception.getMessage(), request);
        }
    }

    @PostMapping("/doctor/status")
    @ResponseBody
    public Result doctorStatus(@RequestParam Long id, @RequestParam Integer status) {
        if (status == null || (status != 0 && status != 1)) {
            return Result.fail().message("医生账号状态不正确");
        }
        Doctor doctor = doctorMapper.selectById(id);
        if (doctor == null) {
            return Result.fail().message("医生账号不存在");
        }
        doctor.setStatus(status);
        doctorMapper.updateById(doctor);
        return Result.ok();
    }

    @PostMapping("/doctor/reset-password")
    @ResponseBody
    public Result resetPassword(@RequestParam Long id) {
        Doctor doctor = doctorMapper.selectById(id);
        if (doctor == null) {
            return Result.fail().message("医生账号不存在");
        }
        doctor.setPassword(MD5.encrypt(doctorDefaultPassword));
        doctorMapper.updateById(doctor);
        return Result.ok().message("密码已重置为系统默认密码");
    }

    @RequestMapping("/order/list")
    public String orderList(ModelMap model,
                            @RequestParam(defaultValue = "1") int pageNum,
                            @RequestParam(defaultValue = "10") int pageSize,
                            @RequestParam(required = false) Integer orderStatus,
                            @RequestParam(required = false) String platformOrderNo) {
        QueryWrapper<OrderInfo> wrapper = new QueryWrapper<>();
        if (orderStatus != null) {
            wrapper.eq("order_status", orderStatus);
        }
        if (StringUtils.hasText(platformOrderNo)) {
            wrapper.like("platform_order_no", platformOrderNo.trim());
        }
        wrapper.orderByDesc("create_time").orderByDesc("id");
        IPage<OrderInfo> page = orderInfoMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        attachSchedules(page.getRecords());
        model.addAttribute("list", page.getRecords());
        model.addAttribute("total", page.getTotal());
        model.addAttribute("pageNum", pageNum);
        model.addAttribute("pageSize", pageSize);
        model.addAttribute("totalPages", page.getPages());
        model.addAttribute("orderStatus", orderStatus);
        model.addAttribute("platformOrderNo", platformOrderNo);
        return "admin/order/list";
    }

    private void attachSchedules(List<OrderInfo> orders) {
        List<Long> scheduleIds = orders.stream()
                .map(OrderInfo::getScheduleId)
                .filter(id -> id != null)
                .distinct()
                .collect(Collectors.toList());
        Map<Long, Schedule> schedules = new HashMap<>();
        if (!scheduleIds.isEmpty()) {
            for (Schedule schedule : scheduleMapper.selectBatchIds(scheduleIds)) {
                schedules.put(schedule.getId(), schedule);
            }
        }
        for (OrderInfo order : orders) {
            order.getParam().put("schedule", schedules.get(order.getScheduleId()));
        }
        attachPatients(orders);
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

    private void validatePassword(String password) {
        if (!StringUtils.hasText(password) || password.length() < 6 || password.length() > 30) {
            throw new YyghException("登录密码长度应为 6-30 位", 201);
        }
    }
}
