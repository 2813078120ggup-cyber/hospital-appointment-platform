package com.atguigu.yygh.hosp.controller;


import com.atguigu.yygh.common.result.R;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 医院设置表 前端控制器
 */
@RestController
@RequestMapping("/user/hosp")
//@CrossOrigin  //解决跨域
public class LoginController {

    //login
    @PostMapping("login")
    public R login() {
        //{"code":20000,"data":{"token":"admin-token"}}
        return R.ok().data("token", "admin-token");
    }

    //info
    @GetMapping("info")
    public R info() {
        //{"code":20000,"data":{"roles":["admin"],
        // "introduction":"I am a super administrator",
        // "avatar":"https://wpimg.wallstcn.com/f778738c-e4f8-4870-b634-56703b4acafe.gif",
        // "name":"Super Admin"}}
        return R.ok().data("roles", "admin")
                .data("introduction", "I am a super administrator")
                .data("avatar", "https://wpimg.wallstcn.com/f778738c-e4f8-4870-b634-56703b4acafe.gif")
                .data("name", "Super Admin");
    }
}

