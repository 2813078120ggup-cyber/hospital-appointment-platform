package com.atguigu.yygh.hosp.service.impl;

import com.atguigu.yygh.common.exception.YyghException;
import com.atguigu.yygh.hosp.mapper.FeedbackMapper;
import com.atguigu.yygh.hosp.service.FeedbackService;
import com.atguigu.yygh.model.hosp.Feedback;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Date;

@Service
public class FeedbackServiceImpl extends ServiceImpl<FeedbackMapper, Feedback> implements FeedbackService {

    @Override
    public void submit(Long userId, Feedback feedback) {
        if (feedback == null || feedback.getType() == null) {
            throw new YyghException(20001, "反馈类型不能为空");
        }
        int type = feedback.getType();
        if (type != 1 && type != 2) {
            throw new YyghException(20001, "反馈类型不正确");
        }
        if (!StringUtils.hasText(feedback.getContent())) {
            throw new YyghException(20001, "反馈内容不能为空");
        }
        if (type == 2 && !StringUtils.hasText(feedback.getHoscode())) {
            throw new YyghException(20001, "医院反馈必须选择医院");
        }
        if (type == 1) {
            feedback.setHoscode(null);
            feedback.setHosname(null);
        }
        feedback.setUserId(userId);
        feedback.setStatus(0);
        feedback.setReply(null);
        this.save(feedback);
    }

    @Override
    public IPage<Feedback> selectHospitalPage(int page, int limit, String hoscode) {
        QueryWrapper<Feedback> wrapper = new QueryWrapper<>();
        wrapper.eq("type", 2);
        wrapper.eq("hoscode", hoscode);
        wrapper.orderByDesc("create_time");
        return this.page(new Page<>(page, limit), wrapper);
    }

    @Override
    public void handleByHospital(Long id, String hoscode, Integer status, String reply) {
        if (id == null || (status == null || (status != 0 && status != 1))) {
            throw new YyghException(20001, "处理参数不正确");
        }
        Feedback feedback = this.getById(id);
        if (feedback == null || !Integer.valueOf(2).equals(feedback.getType())
                || !hoscode.equals(feedback.getHoscode())) {
            throw new YyghException(20001, "反馈不存在");
        }
        feedback.setStatus(status);
        feedback.setReply(StringUtils.hasText(reply) ? reply : null);
        feedback.setUpdateTime(new Date());
        this.updateById(feedback);
    }

    @Override
    public IPage<Feedback> selectAdminPage(Page<Feedback> pageParam, Integer type, Integer status, String keyword) {
        QueryWrapper<Feedback> wrapper = new QueryWrapper<>();
        if (type != null) {
            wrapper.eq("type", type);
        }
        if (status != null) {
            wrapper.eq("status", status);
        }
        if (StringUtils.hasText(keyword)) {
            wrapper.and(condition -> condition.like("user_name", keyword)
                    .or().like("phone", keyword)
                    .or().like("hosname", keyword)
                    .or().like("content", keyword));
        }
        wrapper.orderByDesc("create_time");
        return this.page(pageParam, wrapper);
    }

    @Override
    public Feedback getRequired(Long id) {
        Feedback feedback = this.getById(id);
        if (feedback == null) {
            throw new YyghException(20001, "反馈不存在");
        }
        return feedback;
    }

    @Override
    public void handleByAdmin(Long id, Integer status, String reply) {
        if (id == null || (status == null || (status != 0 && status != 1))) {
            throw new YyghException(20001, "处理参数不正确");
        }
        Feedback feedback = this.getRequired(id);
        feedback.setStatus(status);
        feedback.setReply(StringUtils.hasText(reply) ? reply : null);
        feedback.setUpdateTime(new Date());
        this.updateById(feedback);
    }
}
