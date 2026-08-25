package com.atguigu.hospital.controller;

import com.atguigu.hospital.service.HospSetService;
import com.atguigu.hospital.util.HttpRequestHelper;
import com.atguigu.hospital.util.Result;
import com.atguigu.hospital.util.YyghException;
import io.swagger.annotations.Api;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@Api(tags = "医院设置管理接口")
@RestController
public class HospSetController {

    @Autowired
    private HospSetService hospSetService;

    @Value("${yygh.hospital-manage.bootstrap-token:}")
    private String bootstrapToken;

    /**
     * 同步签名秘钥
     */
    @PostMapping("/hospSet/updateSignKey")
    public Result updateSignKey(HttpServletRequest request, HttpServletResponse response) {
        try {
            Map<String, Object> paramMap = HttpRequestHelper.switchMap(request.getParameterMap());
            validateBootstrapRequest(paramMap);

            hospSetService.updateSignKey(paramMap);
            return Result.ok();
        } catch (YyghException e) {
            return Result.fail().message(e.getMessage());
        }
    }

    private void validateBootstrapRequest(Map<String, Object> paramMap) {
        Object suppliedToken = paramMap.get("bootstrapToken");
        Object timestamp = paramMap.get("timestamp");
        boolean fresh;
        try {
            fresh = Math.abs(System.currentTimeMillis()
                    - Long.parseLong(String.valueOf(timestamp))) <= 5 * 60 * 1000L;
        } catch (NumberFormatException exception) {
            fresh = false;
        }
        boolean tokenMatches = bootstrapToken != null && !bootstrapToken.isBlank()
                && suppliedToken != null
                && MessageDigest.isEqual(bootstrapToken.getBytes(StandardCharsets.UTF_8),
                suppliedToken.toString().getBytes(StandardCharsets.UTF_8));
        // 功能完善：首次密钥同步必须同时具备引导令牌和有效时间戳。
        if (!fresh || !tokenMatches) {
            throw new YyghException("签名密钥同步请求无效或已过期", 300);
        }
    }
}
