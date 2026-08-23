package com.atguigu.yygh.user.service.impl;

import com.atguigu.yygh.common.exception.YyghException;
import com.atguigu.yygh.common.utils.JwtHelper;
import com.atguigu.yygh.enums.AuthStatusEnum;
import com.atguigu.yygh.model.user.Patient;
import com.atguigu.yygh.model.user.UserInfo;
import com.atguigu.yygh.user.mapper.UserInfoMapper;
import com.atguigu.yygh.user.service.PatientService;
import com.atguigu.yygh.user.service.UserInfoService;
import com.atguigu.yygh.vo.user.LoginVo;
import com.atguigu.yygh.vo.user.UserAuthVo;
import com.atguigu.yygh.vo.user.UserInfoQueryVo;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 用户表 服务实现类
 */
@Service
public class UserInfoServiceImpl extends ServiceImpl<UserInfoMapper, UserInfo> implements UserInfoService {

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Autowired
    private PatientService patientService;

    //登录接口-手机验证码登录
    @Override
    public Map<String, Object> loginUser(LoginVo loginVo) {
        //1 获取手机号和验证码
        String phone = loginVo.getPhone();
        String code = loginVo.getCode();

        //2 手机号和验证码非空判断
        if (StringUtils.isEmpty(phone) || StringUtils.isEmpty(code)) {
            throw new YyghException(20001, "数据为空");
        }

        //3 验证码校验过程
        // 输入验证码 和 redis存储验证码比对
        String redisCode = "6666";//redisTemplate.opsForValue().get(phone);
        if (!code.equals(redisCode)) {
            throw new YyghException(20001, "验证码校验失败");
        }

        //判断手机验证码登录  还是 绑定手机号
        //获取openid
        String openid = loginVo.getOpenid();
        //如果openid不为空绑定手机号，否则手机验证码登录
        if (StringUtils.isEmpty(openid)) {  //手机验证码登录
            //4 判断手机号是否第一次登录，根据手机号查询数据库
            QueryWrapper<UserInfo> wrapper = new QueryWrapper<>();
            wrapper.eq("phone", phone);
            UserInfo userInfo = baseMapper.selectOne(wrapper);

            //5 如果是第一次使用手机号，进行注册，添加数据到用户表
            if (userInfo == null) { //添加数据到用户表
                userInfo = new UserInfo();
                userInfo.setName("");
                userInfo.setPhone(phone);
                userInfo.setStatus(1);
//            this.save(userInfo);
                baseMapper.insert(userInfo);
            }

            //6 校验用户是否被禁用
            if (userInfo.getStatus() == 0) {
                throw new YyghException(20001, "用户被禁用");
            }

            //7 返回登录相关信息
            Map<String, Object> map = this.get(userInfo);
            return map;
        } else {  //绑定手机号
            //1 根据绑定手机号查询数据库，手机号数据是否存在
            QueryWrapper<UserInfo> wrapper = new QueryWrapper<>();
            wrapper.eq("phone", phone);
            UserInfo userInfoPhone = baseMapper.selectOne(wrapper);

            //2 如果手机号记录不存在，直接更新手机号完成绑定
            if (userInfoPhone == null) {
                UserInfo wxuserInfo = this.getWxInfoByOpenid(openid);
                wxuserInfo.setPhone(phone);
                baseMapper.updateById(wxuserInfo);

                //7 返回登录相关信息
                Map<String, Object> map = this.get(wxuserInfo);
                return map;
            } else {
                //3 如果手机号记录存在，合并
                /// 微信记录合并到手机号
                //删除微信记录
                // userInfoPhone对象是手机号数据
                //根据openid查询微信
                UserInfo wxuserInfo = this.getWxInfoByOpenid(openid);
                //设置到手机号数据对象里面
                userInfoPhone.setOpenid(wxuserInfo.getOpenid());
                userInfoPhone.setNickName(wxuserInfo.getNickName());

                //更新
                baseMapper.updateById(userInfoPhone);

                //删除微信记录
                baseMapper.deleteById(wxuserInfo.getId());

                //7 返回登录相关信息
                Map<String, Object> map = this.get(userInfoPhone);
                return map;
            }
        }
    }

    private Map<String, Object> get(UserInfo userInfo) {
        //返回页面显示名称
        Map<String, Object> map = new HashMap<>();
        String name = userInfo.getName();
        if (StringUtils.isEmpty(name)) {
            name = userInfo.getNickName();
        }
        if (StringUtils.isEmpty(name)) {
            name = userInfo.getPhone();
        }
        map.put("name", name);
        //根据userid和name生成token字符串
        String token = JwtHelper.createToken(userInfo.getId(), name);
        map.put("token", token);
        return map;
    }

    //根据微信openid查询数据
    @Override
    public UserInfo getWxInfoByOpenid(String openid) {
        LambdaQueryWrapper<UserInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserInfo::getOpenid, openid);
        //调用方法查询
        UserInfo userInfo = baseMapper.selectOne(wrapper);
        return userInfo;
    }

    //用户认证
    @Override
    public void userAuth(Long userId, UserAuthVo userAuthVo) {
        //根据userid查询
        UserInfo userInfo = baseMapper.selectById(userId);

        //设置修改值
        //认证人姓名
        userInfo.setName(userAuthVo.getName());
        //其他认证信息
        userInfo.setCertificatesType(userAuthVo.getCertificatesType());
        userInfo.setCertificatesNo(userAuthVo.getCertificatesNo());
        userInfo.setCertificatesUrl(userAuthVo.getCertificatesUrl());
        //认证状态
        userInfo.setAuthStatus(AuthStatusEnum.AUTH_RUN.getStatus());

        //调用方法更新
        baseMapper.updateById(userInfo);
    }

    //用户列表（条件分页查询）
    @Override
    public IPage<UserInfo> selectPage(Page<UserInfo> pageParam, UserInfoQueryVo userInfoQueryVo) {
        //UserInfoQueryVo获取条件值
        String name = userInfoQueryVo.getKeyword(); //用户名称
        Integer status = userInfoQueryVo.getStatus();//用户状态
        Integer authStatus = userInfoQueryVo.getAuthStatus(); //认证状态
        String createTimeBegin = userInfoQueryVo.getCreateTimeBegin(); //开始时间
        String createTimeEnd = userInfoQueryVo.getCreateTimeEnd(); //结束时间
        //判断条件是否为空，进行封装
        QueryWrapper<UserInfo> wrapper = new QueryWrapper<>();
        if (!StringUtils.isEmpty(name)) {
            wrapper.like("name", name);
        }
        if (!StringUtils.isEmpty(status)) {
            wrapper.eq("status", status);
        }
        if (!StringUtils.isEmpty(authStatus)) {
            wrapper.eq("auth_status", authStatus);
        }
        if (!StringUtils.isEmpty(createTimeBegin)) {
            // >=
            wrapper.ge("create_time", createTimeBegin);
        }
        if (!StringUtils.isEmpty(createTimeEnd)) {
            //<=
            wrapper.le("create_time", createTimeEnd);
        }
        //调用mapper方法
        IPage<UserInfo> pages = baseMapper.selectPage(pageParam, wrapper);

        //封装编号对应名称 ：用户状态  和  用户认证状态
        pages.getRecords().stream().forEach(item -> {
            this.packageUserInfo(item);
        });

        return pages;
    }

    //用户详情接口
    @Override
    public Map<String, Object> show(Long userId) {
        Map<String, Object> map = new HashMap<>();
        //根据userid查询用户信息
        UserInfo userInfo = this.packageUserInfo(baseMapper.selectById(userId));

        //根据userid查询就诊人列表
        List<Patient> patientList = patientService.findAllUserId(userId);

        //封装到map集合
        map.put("userInfo", userInfo);
        map.put("patientList", patientList);

        //返回
        return map;
    }

    //认证审批
    @Override
    public void approval(Long userId, Integer authStatus) {
        if (authStatus.intValue() == 2 || authStatus.intValue() == -1) {
            UserInfo userInfo = baseMapper.selectById(userId);
            userInfo.setAuthStatus(authStatus);
            baseMapper.updateById(userInfo);
        }
    }

    //编号变成对应值封装
    private UserInfo packageUserInfo(UserInfo userInfo) {
        //处理认证状态编码
        userInfo.getParam().put("authStatusString", AuthStatusEnum.getStatusNameByStatus(userInfo.getAuthStatus()));
        //处理用户状态 0  1
        String statusString = userInfo.getStatus().intValue() == 0 ? "锁定" : "正常";
        userInfo.getParam().put("statusString", statusString);
        return userInfo;
    }
}
