package com.atguigu.yygh.user.controller;


import com.atguigu.yygh.common.exception.YyghException;
import com.atguigu.yygh.common.result.R;
import com.atguigu.yygh.common.utils.AuthContextHolder;
import com.atguigu.yygh.model.user.Patient;
import com.atguigu.yygh.user.service.PatientService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Objects;


/**
 * 就诊人表 前端控制器
 */
@RestController
@RequestMapping("/api/user/patient")
public class PatientController {
    
    @Autowired
    private PatientService patientService;
    
    // ----20260822 新增接口 辅助下单（预约挂号）业务 start ----
    // 挂号下单业务辅助远程接口，根据就诊人id获取就诊人实体对象数据
    @GetMapping("inner/getPatientInfoById/{id}")
    public Patient getPatientInfoById(@PathVariable("id") Long id) {
        return patientService.getById(id);
    }
    // ----20260822 新增接口 辅助下单（预约挂号）业务 end ----
    
    //根据id获取就诊人信息
    @GetMapping("auth/get/{id}")
    public R getPatient(@PathVariable Long id, HttpServletRequest request) {
        Long userId = requireUserId(request);
        requireOwnedPatient(id, userId);
        Patient patient = patientService.getPatientId(id);
        return R.ok().data("patient", patient);
    }
    
    //获取就诊人列表
    @GetMapping("auth/findAll")
    public R findAll(HttpServletRequest request) {
        //获取当前登录用户id
        Long userId = requireUserId(request);
        List<Patient> list = patientService.findAllUserId(userId);
        return R.ok().data("list", list);
    }
    
    //添加就诊人
    @PostMapping("auth/save")
    public R savePatient(@RequestBody Patient patient, HttpServletRequest request) {
        //获取用户id
        Long userId = requireUserId(request);
        // 功能完善：忽略客户端提交的主键和 userId，防止借保存接口覆盖他人数据。
        patient.setId(null);
        patient.setUserId(userId);
        patientService.save(patient);
        return R.ok();
    }
    
    //修改就诊人
    @PostMapping("auth/update")
    public R updatePatient(@RequestBody Patient patient, HttpServletRequest request) {
        Long userId = requireUserId(request);
        requireOwnedPatient(patient == null ? null : patient.getId(), userId);
        // 功能完善：资源归属以认证上下文为准，不信任客户端传入的 userId。
        patient.setUserId(userId);
        patientService.updateById(patient);
        return R.ok();
    }
    
    //删除就诊人
    @DeleteMapping("auth/remove/{id}")
    public R removePatient(@PathVariable Long id, HttpServletRequest request) {
        Long userId = requireUserId(request);
        requireOwnedPatient(id, userId);
        patientService.removeById(id);
        return R.ok();
    }

    private Long requireUserId(HttpServletRequest request) {
        Long userId = AuthContextHolder.getUserId(request);
        if (userId == null) {
            throw new YyghException(20001, "请先登录");
        }
        return userId;
    }

    private Patient requireOwnedPatient(Long patientId, Long userId) {
        if (patientId == null) {
            throw new YyghException(20001, "就诊人参数不正确");
        }
        Patient patient = patientService.getById(patientId);
        if (patient == null || !Objects.equals(patient.getUserId(), userId)) {
            // 统一返回无权限，避免通过错误信息枚举其他用户的就诊人 ID。
            throw new YyghException(20001, "无权访问该就诊人");
        }
        return patient;
    }
    
}

