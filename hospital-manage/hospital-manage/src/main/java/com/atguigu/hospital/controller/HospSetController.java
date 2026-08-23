package com.atguigu.hospital.controller;

import com.atguigu.hospital.service.HospSetService;
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

@Api(tags = "医院设置管理接口")
@RestController
public class HospSetController {

    @Autowired
    private HospSetService hospSetService;

    /**
     * 同步签名秘钥
     */
    @PostMapping("/hospSet/updateSignKey")
    public Result updateSignKey(HttpServletRequest request, HttpServletResponse response) {
        try {
            Map<String, Object> paramMap = HttpRequestHelper.switchMap(request.getParameterMap());
//			if(!HttpRequestHelper.isSignEquals(paramMap, apiService.getSignKey())) {
//				throw new YyghException(ResultCodeEnum.SIGN_ERROR);
//			}

            hospSetService.updateSignKey(paramMap);
            return Result.ok();
        } catch (YyghException e) {
            return Result.fail().message(e.getMessage());
        }
    }
}

