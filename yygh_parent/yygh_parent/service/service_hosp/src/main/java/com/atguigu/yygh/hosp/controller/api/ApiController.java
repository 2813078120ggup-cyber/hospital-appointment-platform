package com.atguigu.yygh.hosp.controller.api;

import com.atguigu.yygh.common.exception.YyghException;
import com.atguigu.yygh.common.result.R;
import com.atguigu.yygh.common.result.Result;
import com.atguigu.yygh.hosp.service.DepartmentService;
import com.atguigu.yygh.hosp.service.HospitalService;
import com.atguigu.yygh.hosp.service.HospitalSetService;
import com.atguigu.yygh.hosp.service.ScheduleService;
import com.atguigu.yygh.hosp.utils.HttpRequestHelper;
import com.atguigu.yygh.hosp.utils.MD5;
import com.atguigu.yygh.model.hosp.Department;
import com.atguigu.yygh.model.hosp.Hospital;
import com.atguigu.yygh.model.hosp.Schedule;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Schema(description = "医院管理数据API接口")
@RestController
@RequestMapping("/api/hosp")
public class ApiController {

    @Autowired
    private HospitalService hospitalService;

    @Autowired
    private HospitalSetService hospitalSetService;

    @Autowired
    private DepartmentService departmentService;

    @Autowired
    private ScheduleService scheduleService;

    @Operation(description = "删除排班")
    @PostMapping("schedule/remove")
    public Result removeSchedule(HttpServletRequest request) {
        Map<String, Object> paramMap = HttpRequestHelper.switchMap(request.getParameterMap());
        //必须参数校验 略
        String hoscode = (String)paramMap.get("hoscode");
        //必填
        String hosScheduleId = (String)paramMap.get("hosScheduleId");
        scheduleService.remove(hoscode, hosScheduleId);
        return Result.ok();
    }

    @Operation(description = "获取排班分页列表")
    @PostMapping("schedule/list")
    public Result schedule(HttpServletRequest request) {
        Map<String, Object> paramMap = HttpRequestHelper.switchMap(request.getParameterMap());

        String hoscode = (String)paramMap.get("hoscode");
        String depcode = (String)paramMap.get("depcode");
        int page = StringUtils.isEmpty(paramMap.get("page")) ? 1 : Integer.parseInt((String)paramMap.get("page"));
        int limit = StringUtils.isEmpty(paramMap.get("limit")) ? 10 : Integer.parseInt((String)paramMap.get("limit"));

        Page<Schedule> pageModel =
                scheduleService.selectPageSchedule(page,limit,hoscode,depcode);
        return Result.ok(pageModel);
    }

    @Operation(description = "上传排班")
    @PostMapping("saveSchedule")
    public Result saveSchedule(HttpServletRequest request) {
        Map<String, Object> newObjectMap = HttpRequestHelper.switchMap(request.getParameterMap());
        scheduleService.saveSchedule(newObjectMap);
        return Result.ok();
    }

    @Operation(description = "删除科室")
    @PostMapping("department/remove")
    public Result removeDepartment(HttpServletRequest request) {
        Map<String, Object> paramMap = HttpRequestHelper.switchMap(request.getParameterMap());
        //必须参数校验 略
        String hoscode = (String)paramMap.get("hoscode");
        String depcode = (String)paramMap.get("depcode");
        departmentService.remove(hoscode, depcode);
        return Result.ok();
    }

    @Operation(description = "科室获取分页列表")
    @PostMapping("department/list")
    public Result department(HttpServletRequest request) {
        //获取医院模拟系统传递数据
        Map<String, String[]> parameterMap = request.getParameterMap();
        Map<String, Object> newObjectMap = HttpRequestHelper.switchMap(parameterMap);

        //必须参数校验 略
        String hoscode = (String)newObjectMap.get("hoscode");
        //非必填
        String depcode = (String)newObjectMap.get("depcode");

        int page = StringUtils.isEmpty(newObjectMap.get("page")) ? 1 : Integer.parseInt((String)newObjectMap.get("page"));
        int limit = StringUtils.isEmpty(newObjectMap.get("limit")) ? 10 : Integer.parseInt((String)newObjectMap.get("limit"));

        //调用service方法
        Page<Department> pageModel =
                departmentService.selectPageDept(page,limit,hoscode,depcode);
        return Result.ok(pageModel);
    }

    @Operation(description = "上传科室")
    @PostMapping("saveDepartment")
    public Result saveDepartment(HttpServletRequest request) {
        //获取医院模拟系统传递数据
        Map<String, String[]> parameterMap = request.getParameterMap();
        Map<String, Object> newObjectMap = HttpRequestHelper.switchMap(parameterMap);
        //调用方法添加
        departmentService.saveDept(newObjectMap);
        return Result.ok();
    }

    @Operation(description = "获取医院信息")
    @PostMapping("hospital/show")
    public Result hospital(HttpServletRequest request) {
        Map<String, Object> newObjectMap =
                HttpRequestHelper.switchMap(request.getParameterMap());
        //获取医院编号
        String hoscode = (String)newObjectMap.get("hoscode");
        //调用service方法
        Hospital hospital = hospitalService.getHosp(hoscode);
        return Result.ok(hospital);
    }

    /**
     * 1、获取提交数据方式 request.getParameterMap()
     * 2、map集合遍历方式
     */
    //上传医院信息（添加医院）
    @Operation(description = "上传医院")
    @PostMapping("saveHospital")
    public Result saveHospital(HttpServletRequest request) {
        //String hoscode = request.getParameter("hoscode");
        //hoscode 1000
        //hosname 协和医院
        //1 获取提交参数，封装map集合里面
        Map<String,String[]> parameterMap = request.getParameterMap();

        //2 为了后面操作方便    Map<String,String[]> --  Map<String,Object>
        Map<String, Object> newObjectMap = HttpRequestHelper.switchMap(parameterMap);

        //添加签名校验
        //获取医院模拟系统传递sign,MD5加密
        String signHospital = (String)newObjectMap.get("sign");

        //查询当前医院在平台保存sign
        String hoscode = (String)newObjectMap.get("hoscode");
        String signYygh = hospitalSetService.getHospSignKey(hoscode);

        //比对两个sign是否一样
        //  signHospital    signYygh
        // 平台key加密 和 医院模拟系统值比对
        String md5SignYygh = MD5.encrypt(signYygh);
        if(!signHospital.equals(md5SignYygh)) {
            throw new YyghException(20001,"签名校验失败");
        }

        //传输过程中“+”转换为了“ ”，因此我们要转换回来
        String logoData = (String)newObjectMap.get("logoData");
        logoData = logoData.replaceAll(" ","+");
        newObjectMap.put("logoData",logoData);

        //3 调用service方法添加
        hospitalService.saveHosp(newObjectMap);
        return Result.ok();
    }


}
