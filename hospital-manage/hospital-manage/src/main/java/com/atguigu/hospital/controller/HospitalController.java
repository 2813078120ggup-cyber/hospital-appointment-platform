package com.atguigu.hospital.controller;

import com.atguigu.hospital.service.ApiService;
import com.atguigu.hospital.service.HospitalService;
import com.atguigu.hospital.util.HttpRequestHelper;
import com.atguigu.hospital.util.Result;
import com.atguigu.hospital.util.YyghException;
import io.swagger.annotations.Api;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;

@Api(tags = "医院管理接口")
@RestController
public class HospitalController {

    @Autowired
    private HospitalService hospitalService;

    @Autowired
    private ApiService apiService;

    /**
     * 预约下单
     */
    @PostMapping("/order/submitOrder")
    public Result AgreeAccountLendProject(HttpServletRequest request, HttpServletResponse response) {
        try {
            Map<String, Object> paramMap = HttpRequestHelper.switchMap(request.getParameterMap());

//			if(!HttpRequestHelper.isSignEquals(paramMap, apiService.getSignKey())) {
//				throw new YyghException(ResultCodeEnum.SIGN_ERROR);
//			}

            Map<String, Object> resultMap = hospitalService.submitOrder(paramMap);
            return Result.ok(resultMap);
        } catch (YyghException e) {
            return Result.fail().message(e.getMessage());
        }
    }

    /**
     * 更新支付状态
     */
    @PostMapping("/order/updatePayStatus")
    public Result updatePayStatus(HttpServletRequest request, HttpServletResponse response) {
        try {
            Map<String, Object> paramMap = HttpRequestHelper.switchMap(request.getParameterMap());
//			if(!HttpRequestHelper.isSignEquals(paramMap, apiService.getSignKey())) {
//				throw new YyghException(ResultCodeEnum.SIGN_ERROR);
//			}

            hospitalService.updatePayStatus(paramMap);
            return Result.ok();
        } catch (YyghException e) {
            return Result.fail().message(e.getMessage());
        }
    }

    /**
     * 更新取消预约状态
     */
    @PostMapping("/order/updateCancelStatus")
    public Result updateCancelStatus(HttpServletRequest request, HttpServletResponse response) {
        try {
            Map<String, Object> paramMap = HttpRequestHelper.switchMap(request.getParameterMap());
//			if(!HttpRequestHelper.isSignEquals(paramMap, apiService.getSignKey())) {
//				throw new YyghException(ResultCodeEnum.SIGN_ERROR);
//			}

            hospitalService.updateCancelStatus(paramMap);
            return Result.ok();
        } catch (YyghException e) {
            return Result.fail().message(e.getMessage());
        }
    }
}

