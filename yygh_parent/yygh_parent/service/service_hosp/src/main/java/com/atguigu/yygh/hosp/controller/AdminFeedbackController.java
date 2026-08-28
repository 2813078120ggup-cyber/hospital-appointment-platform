package com.atguigu.yygh.hosp.controller;

import com.atguigu.yygh.common.result.R;
import com.atguigu.yygh.hosp.service.FeedbackService;
import com.atguigu.yygh.model.hosp.Feedback;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/admin/hosp/feedback")
public class AdminFeedbackController {

    @Autowired
    private FeedbackService feedbackService;

    @GetMapping("/{page}/{limit}")
    public R list(@PathVariable Long page,
                  @PathVariable Long limit,
                  @RequestParam(required = false) Integer type,
                  @RequestParam(required = false) Integer status,
                  @RequestParam(required = false) String keyword) {
        long current = page == null || page < 1 ? 1 : page;
        long size = limit == null || limit < 1 ? 10 : Math.min(limit, 100);
        IPage<Feedback> pageModel = feedbackService.selectAdminPage(
                new Page<>(current, size), type, status, keyword);
        return R.ok().data("pageModel", pageModel);
    }

    @GetMapping("/show/{id}")
    public R show(@PathVariable Long id) {
        return R.ok().data("feedback", feedbackService.getRequired(id));
    }

    @PutMapping("/handle/{id}")
    public R handle(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        Integer status = body.get("status") == null ? 1 : Integer.parseInt(body.get("status").toString());
        String reply = body.get("reply") == null ? null : body.get("reply").toString();
        feedbackService.handleByAdmin(id, status, reply);
        return R.ok();
    }
}
