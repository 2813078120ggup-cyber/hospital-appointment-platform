package com.atguigu.yygh.user.controller;


import com.atguigu.yygh.common.result.R;
import com.atguigu.yygh.common.utils.AuthContextHolder;
import com.atguigu.yygh.model.user.Patient;
import com.atguigu.yygh.user.service.PatientService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;


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
    public R getPatient(@PathVariable Long id) {
        Patient patient = patientService.getPatientId(id);
        return R.ok().data("patient", patient);
    }
    
    //获取就诊人列表
    @GetMapping("auth/findAll")
    public R findAll(HttpServletRequest request) {
        //获取当前登录用户id
        Long userId = AuthContextHolder.getUserId(request);
        List<Patient> list = patientService.findAllUserId(userId);
        return R.ok().data("list", list);
    }
    
    //添加就诊人
    @PostMapping("auth/save")
    public R savePatient(@RequestBody Patient patient, HttpServletRequest request) {
        //获取用户id
        Long userId = AuthContextHolder.getUserId(request);
        patient.setUserId(userId);
        patientService.save(patient);
        return R.ok();
    }
    
    //修改就诊人
    @PostMapping("auth/update")
    public R updatePatient(@RequestBody Patient patient) {
        patientService.updateById(patient);
        return R.ok();
    }
    
    //删除就诊人
    @DeleteMapping("auth/remove/{id}")
    public R removePatient(@PathVariable Long id) {
        patientService.removeById(id);
        return R.ok();
    }
    
}

