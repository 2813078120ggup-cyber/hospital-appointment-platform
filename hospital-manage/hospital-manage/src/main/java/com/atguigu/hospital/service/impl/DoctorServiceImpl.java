package com.atguigu.hospital.service.impl;

import com.atguigu.hospital.mapper.DoctorMapper;
import com.atguigu.hospital.model.Doctor;
import com.atguigu.hospital.service.DoctorService;
import com.atguigu.hospital.util.MD5;
import com.atguigu.hospital.util.YyghException;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;

@Service
public class DoctorServiceImpl implements DoctorService {

    @Autowired
    private DoctorMapper doctorMapper;

    @Override
    public Doctor login(String username, String password) {
        if (!StringUtils.hasText(username) || !StringUtils.hasText(password)) {
            throw new YyghException("请输入医生姓名和密码", 201);
        }
        String account = username.trim();
        List<Doctor> doctors = doctorMapper.selectList(
                new QueryWrapper<Doctor>()
                        .and(wrapper -> wrapper.eq("username", account).or().eq("docname", account))
                        .orderByAsc("id"));
        Doctor doctor = doctors.isEmpty() ? null : doctors.get(0);
        if (doctor == null || doctor.getStatus() == null || doctor.getStatus() != 1) {
            throw new YyghException("医生账号不存在或已停用", 201);
        }
        byte[] expected = doctor.getPassword() == null ? new byte[0]
                : doctor.getPassword().toLowerCase().getBytes(StandardCharsets.UTF_8);
        byte[] actual = MD5.encrypt(password).toLowerCase().getBytes(StandardCharsets.UTF_8);
        if (!MessageDigest.isEqual(expected, actual)) {
            throw new YyghException("用户名或密码错误", 201);
        }
        return doctor;
    }

    @Override
    public Doctor updateProfile(Long doctorId, String title, String phone) {
        Doctor doctor = requireDoctor(doctorId);
        doctor.setTitle(StringUtils.hasText(title) ? title.trim() : null);
        doctor.setPhone(StringUtils.hasText(phone) ? phone.trim() : null);
        doctorMapper.updateById(doctor);
        return doctor;
    }

    @Override
    public void changePassword(Long doctorId, String currentPassword, String newPassword, String confirmPassword) {
        Doctor doctor = requireDoctor(doctorId);
        if (!StringUtils.hasText(currentPassword)
                || !MessageDigest.isEqual(
                doctor.getPassword() == null ? new byte[0]
                        : doctor.getPassword().toLowerCase().getBytes(StandardCharsets.UTF_8),
                MD5.encrypt(currentPassword).toLowerCase().getBytes(StandardCharsets.UTF_8))) {
            throw new YyghException("原密码不正确", 201);
        }
        if (!StringUtils.hasText(newPassword) || newPassword.length() < 6 || newPassword.length() > 30) {
            throw new YyghException("新密码长度应为 6-30 位", 201);
        }
        if (!newPassword.equals(confirmPassword)) {
            throw new YyghException("两次输入的新密码不一致", 201);
        }
        doctor.setPassword(MD5.encrypt(newPassword));
        doctorMapper.updateById(doctor);
    }

    private Doctor requireDoctor(Long doctorId) {
        Doctor doctor = doctorId == null ? null : doctorMapper.selectById(doctorId);
        if (doctor == null) {
            throw new YyghException("医生账号不存在", 201);
        }
        return doctor;
    }
}
