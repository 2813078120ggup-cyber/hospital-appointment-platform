package com.atguigu.yygh.user.controller.admin;

import com.atguigu.yygh.common.result.R;
import com.atguigu.yygh.model.user.UserInfo;
import com.atguigu.yygh.user.service.UserInfoService;
import com.atguigu.yygh.vo.user.UserInfoQueryVo;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/admin/user")
public class UserController {

    @Autowired
    private UserInfoService userInfoService;

    //认证审批
    @GetMapping("approval/{userId}/{authStatus}")
    public R approval(@PathVariable Long userId, @PathVariable Integer authStatus) {
        userInfoService.approval(userId, authStatus);
        return R.ok();
    }

    //用户详情接口
    @GetMapping("show/{userId}")
    public R showUserInfo(@PathVariable Long userId) {
        Map<String, Object> map = userInfoService.show(userId);
        return R.ok().data(map);
    }

    //用户列表（条件分页查询）
    @GetMapping("{page}/{limit}")
    public R list(@PathVariable Long page,
                  @PathVariable Long limit,
                  UserInfoQueryVo userInfoQueryVo) {
        //创建page对象，传递当前页和每页记录数
        Page<UserInfo> pageParam = new Page<>(page, limit);
        //调用service方法
        IPage<UserInfo> pageModel =
                userInfoService.selectPage(pageParam, userInfoQueryVo);
        return R.ok().data("pageModel", pageModel);
    }
}
