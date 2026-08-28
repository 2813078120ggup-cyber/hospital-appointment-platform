package com.atguigu.yygh.orders.controller;

import com.atguigu.yygh.common.exception.YyghException;
import com.atguigu.yygh.common.result.R;
import com.atguigu.yygh.common.utils.AuthContextHolder;
import com.atguigu.yygh.model.order.OrderCompensationTask;
import com.atguigu.yygh.orders.service.CompensationTaskService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 平台管理员查看和重新投递订单补偿任务。 */
@RestController
@RequestMapping("/admin/order/compensation")
public class AdminCompensationController {

    private final CompensationTaskService compensationTaskService;

    public AdminCompensationController(CompensationTaskService compensationTaskService) {
        this.compensationTaskService = compensationTaskService;
    }

    @GetMapping("/{page}/{limit}")
    public R list(@PathVariable Long page,
                  @PathVariable Long limit,
                  @RequestParam(required = false) Integer status,
                  @RequestParam(required = false) String taskType,
                  HttpServletRequest request) {
        requireAdmin(request);
        long current = page == null || page < 1 ? 1 : page;
        long size = limit == null || limit < 1 ? 10 : Math.min(limit, 100);
        IPage<OrderCompensationTask> pageModel = compensationTaskService.selectPage(
                new Page<>(current, size), status, taskType);
        return R.ok().data("pageModel", pageModel);
    }

    @PostMapping("/retry/{taskId}")
    public R retry(@PathVariable Long taskId, HttpServletRequest request) {
        requireAdmin(request);
        if (!compensationTaskService.retryDeadLetter(taskId)) {
            throw new YyghException(20001, "补偿任务不存在、未处于死信状态或正在处理中");
        }
        return R.ok().message("补偿任务已重新投递");
    }

    @GetMapping("/statusList")
    public R statusList(HttpServletRequest request) {
        requireAdmin(request);
        return R.ok().data("list", new Object[]{
                status(CompensationTaskService.STATUS_PENDING, "待处理"),
                status(CompensationTaskService.STATUS_PROCESSING, "处理中"),
                status(CompensationTaskService.STATUS_SUCCESS, "成功"),
                status(CompensationTaskService.STATUS_DEAD, "死信")
        });
    }

    private void requireAdmin(HttpServletRequest request) {
        Long userId = AuthContextHolder.getUserId(request);
        if (!Long.valueOf(0L).equals(userId)) {
            throw new YyghException(20001, "没有补偿任务运营权限");
        }
    }

    private java.util.Map<String, Object> status(int value, String label) {
        java.util.Map<String, Object> result = new java.util.LinkedHashMap<>();
        result.put("status", value);
        result.put("label", label);
        return result;
    }
}
