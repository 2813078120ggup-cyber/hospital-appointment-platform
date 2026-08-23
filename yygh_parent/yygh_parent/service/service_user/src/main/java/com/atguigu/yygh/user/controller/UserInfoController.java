package com.atguigu.yygh.user.controller;


import com.atguigu.yygh.common.result.R;
import com.atguigu.yygh.common.utils.AuthContextHolder;
import com.atguigu.yygh.model.user.UserInfo;
import com.atguigu.yygh.user.service.UserInfoService;
import com.atguigu.yygh.vo.user.LoginVo;
import com.atguigu.yygh.vo.user.UserAuthVo;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 用户表 前端控制器
 */
@RestController
@RequestMapping("/api/user")
public class UserInfoController {

    @Autowired
    private UserInfoService userInfoService;

    //用户认证
    @PostMapping("auth/userAuth")
    public R userAuth(@RequestBody UserAuthVo userAuthVo, HttpServletRequest request) {
        //从请求头获取token字符串
//        String token = request.getHeader("token");
//        //下面这样写可能出现问题
////        if(StringUtils.isEmpty(token)) {
////            throw new YyghException()
////        }
//        //从token获取userid
//        Long userId = JwtHelper.getUserId(token);

        Long userId = AuthContextHolder.getUserId(request);
        //调用方法认证
        userInfoService.userAuth(userId, userAuthVo);
        return R.ok();
    }

    //获取用户id信息接口
    @GetMapping("auth/getUserInfo")
    public R getUserInfo(HttpServletRequest request) {
        Long userId = AuthContextHolder.getUserId(request);
        UserInfo userInfo = userInfoService.getById(userId);
        return R.ok().data("userInfo", userInfo);
    }

    //登录接口-手机验证码登录
    //@ApiOperation(value = "会员登录")
    @PostMapping("login")
    public R login(@RequestBody LoginVo loginVo) {
        Map<String, Object> map = userInfoService.loginUser(loginVo);
        return R.ok().data(map);
    }

}

