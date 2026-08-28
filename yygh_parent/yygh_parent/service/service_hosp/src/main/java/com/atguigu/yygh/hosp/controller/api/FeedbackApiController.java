package com.atguigu.yygh.hosp.controller.api;

import com.atguigu.yygh.common.exception.YyghException;
import com.atguigu.yygh.common.result.R;
import com.atguigu.yygh.common.result.Result;
import com.atguigu.yygh.common.utils.AuthContextHolder;
import com.atguigu.yygh.hosp.service.FeedbackService;
import com.atguigu.yygh.hosp.service.HospitalSetService;
import com.atguigu.yygh.hosp.utils.HttpRequestHelper;
import com.atguigu.yygh.model.hosp.Feedback;
import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@Schema(description = "意见反馈API接口")
@RestController
@RequestMapping("/api/hosp/feedback")
public class FeedbackApiController {

    @Autowired
    private FeedbackService feedbackService;

    @Autowired
    private HospitalSetService hospitalSetService;

    @Operation(description = "患者提交反馈")
    @PostMapping("auth/save")
    public R save(@RequestBody Feedback feedback, HttpServletRequest request) {
        Long userId = AuthContextHolder.getUserId(request);
        feedbackService.submit(userId, feedback);
        return R.ok();
    }

    @Operation(description = "医院端拉取本医院反馈")
    @PostMapping("list")
    public Result list(HttpServletRequest request) {
        Map<String, Object> paramMap = HttpRequestHelper.switchMap(request.getParameterMap());
        validateSignedRequest(paramMap);
        String hoscode = requireText(paramMap, "hoscode", "医院编号");
        int page = parsePositiveInt(paramMap, "page", 1, 10000);
        int limit = parsePositiveInt(paramMap, "limit", 10, 100);
        IPage<Feedback> pageModel = feedbackService.selectHospitalPage(page, limit, hoscode);
        Map<String, Object> result = new HashMap<>();
        result.put("content", pageModel.getRecords());
        result.put("totalElements", pageModel.getTotal());
        return Result.ok(result);
    }

    @Operation(description = "医院端处理反馈")
    @PostMapping("handle")
    public Result handle(HttpServletRequest request) {
        Map<String, Object> paramMap = HttpRequestHelper.switchMap(request.getParameterMap());
        validateSignedRequest(paramMap);
        String hoscode = requireText(paramMap, "hoscode", "医院编号");
        Long id = parseLong(paramMap, "id", "反馈编号");
        Integer status = parseStatus(paramMap);
        String reply = (String) paramMap.get("reply");
        feedbackService.handleByHospital(id, hoscode, status, reply);
        return Result.ok();
    }

    private void validateSignedRequest(Map<String, Object> paramMap) {
        String hoscode = (String) paramMap.get("hoscode");
        if (!StringUtils.hasText(hoscode)) {
            throw new YyghException(20001, "医院编号不能为空");
        }
        String signKey = hospitalSetService.getHospSignKey(hoscode);
        if (!HttpRequestHelper.isSignEquals(paramMap, signKey)) {
            throw new YyghException(20001, "签名校验失败或请求已过期");
        }
    }

    private String requireText(Map<String, Object> paramMap, String key, String label) {
        Object value = paramMap.get(key);
        if (value == null || !StringUtils.hasText(value.toString())) {
            throw new YyghException(20001, label + "不能为空");
        }
        return value.toString().trim();
    }

    private Long parseLong(Map<String, Object> paramMap, String key, String label) {
        Object value = paramMap.get(key);
        if (value == null || !StringUtils.hasText(value.toString())) {
            throw new YyghException(20001, label + "不能为空");
        }
        try {
            return Long.parseLong(value.toString());
        } catch (NumberFormatException exception) {
            throw new YyghException(20001, label + "不正确");
        }
    }

    private Integer parseStatus(Map<String, Object> paramMap) {
        Object value = paramMap.get("status");
        if (value == null || !StringUtils.hasText(value.toString())) {
            throw new YyghException(20001, "处理状态不能为空");
        }
        try {
            int status = Integer.parseInt(value.toString());
            if (status != 0 && status != 1) {
                throw new NumberFormatException();
            }
            return status;
        } catch (NumberFormatException exception) {
            throw new YyghException(20001, "处理状态仅支持 0/1");
        }
    }

    private int parsePositiveInt(Map<String, Object> paramMap, String key, int defaultValue, int maxValue) {
        Object value = paramMap.get(key);
        if (value == null || !StringUtils.hasText(value.toString())) {
            return defaultValue;
        }
        try {
            int parsed = Integer.parseInt(value.toString());
            if (parsed < 1 || parsed > maxValue) {
                throw new NumberFormatException();
            }
            return parsed;
        } catch (NumberFormatException exception) {
            throw new YyghException(20001, key + " 参数不正确");
        }
    }
}
