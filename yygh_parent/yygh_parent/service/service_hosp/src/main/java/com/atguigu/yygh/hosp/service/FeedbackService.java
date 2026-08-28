package com.atguigu.yygh.hosp.service;

import com.atguigu.yygh.model.hosp.Feedback;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * 意见反馈
 */
public interface FeedbackService extends IService<Feedback> {

    //患者提交反馈
    void submit(Long userId, Feedback feedback);

    //医院端分页拉取本医院反馈
    IPage<Feedback> selectHospitalPage(int page, int limit, String hoscode);

    //医院端处理反馈
    void handleByHospital(Long id, String hoscode, Integer status, String reply);

    //管理端分页查询
    IPage<Feedback> selectAdminPage(Page<Feedback> pageParam, Integer type, Integer status, String keyword);

    //查询反馈详情
    Feedback getRequired(Long id);

    //管理端处理反馈
    void handleByAdmin(Long id, Integer status, String reply);
}
