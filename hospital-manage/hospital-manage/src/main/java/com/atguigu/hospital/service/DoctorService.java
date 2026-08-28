package com.atguigu.hospital.service;

import com.atguigu.hospital.model.Doctor;

/**
 * 医生账号
 */
public interface DoctorService {

    /**
     * 医生登录
     */
    Doctor login(String username, String password);

    /**
     * 更新医生本人可维护的资料。
     */
    Doctor updateProfile(Long doctorId, String title, String phone);

    /**
     * 校验原密码后修改登录密码。
     */
    void changePassword(Long doctorId, String currentPassword, String newPassword, String confirmPassword);
}
