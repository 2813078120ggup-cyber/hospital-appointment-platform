package com.atguigu.yygh.hosp.controller.api;

import com.atguigu.yygh.common.exception.YyghException;
import com.atguigu.yygh.common.result.R;
import com.atguigu.yygh.common.result.Result;
import com.atguigu.yygh.hosp.service.DepartmentService;
import com.atguigu.yygh.hosp.service.HospitalService;
import com.atguigu.yygh.hosp.service.HospitalSetService;
import com.atguigu.yygh.hosp.service.ScheduleService;
import com.atguigu.yygh.hosp.utils.HttpRequestHelper;
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

    @Operation(description = "根据科室名称，日期，时间和医生查询是否有号源")
    @PostMapping("selectSchedule")
    public Result<Boolean> selectSchedule(HttpServletRequest request) {
        // 功能完善：该接口仅返回布尔型号源状态，不暴露医院签名密钥或患者数据，供 AI 服务调用。
        Map<String, Object> paramMap = HttpRequestHelper.switchMap(request.getParameterMap());
        String name = (String) paramMap.get("name");
        String doctorName = (String) paramMap.get("doctorName");
        String date = (String) paramMap.get("date");
        String time = (String) paramMap.get("time");

        if (!StringUtils.hasText(name) || !StringUtils.hasText(date) || !StringUtils.hasText(time)) {
            return Result.fail(false).message("科室名称、日期和时间不能为空");
        }

        try {
            boolean available = scheduleService.hasAvailableSchedule(name, date, time, doctorName);
            if (available) {
                return Result.ok(true).message("有可预约号源");
            }
            return Result.fail(false).message("暂无可预约号源");
        } catch (IllegalArgumentException exception) {
            return Result.fail(false).message(exception.getMessage());
        }
    }

    @Operation(description = "删除排班")
    @PostMapping("schedule/remove")
    public Result removeSchedule(HttpServletRequest request) {
        Map<String, Object> paramMap = HttpRequestHelper.switchMap(request.getParameterMap());
        validateSignedRequest(paramMap);
        String hoscode = requireText(paramMap, "hoscode", "医院编号");
        String hosScheduleId = requireText(paramMap, "hosScheduleId", "医院排班编号");
        scheduleService.remove(hoscode, hosScheduleId);
        return Result.ok();
    }

    @Operation(description = "获取排班分页列表")
    @PostMapping("schedule/list")
    public Result schedule(HttpServletRequest request) {
        Map<String, Object> paramMap = HttpRequestHelper.switchMap(request.getParameterMap());
        validateSignedRequest(paramMap);

        String hoscode = requireText(paramMap, "hoscode", "医院编号");
        String depcode = (String)paramMap.get("depcode");
        int page = parsePositiveInt(paramMap, "page", 1, 10000);
        int limit = parsePositiveInt(paramMap, "limit", 10, 100);

        Page<Schedule> pageModel =
                scheduleService.selectPageSchedule(page,limit,hoscode,depcode);
        return Result.ok(pageModel);
    }

    @Operation(description = "上传排班")
    @PostMapping("saveSchedule")
    public Result saveSchedule(HttpServletRequest request) {
        Map<String, Object> newObjectMap = HttpRequestHelper.switchMap(request.getParameterMap());
        validateSignedRequest(newObjectMap);
        scheduleService.saveSchedule(newObjectMap);
        return Result.ok();
    }

    @Operation(description = "删除科室")
    @PostMapping("department/remove")
    public Result removeDepartment(HttpServletRequest request) {
        Map<String, Object> paramMap = HttpRequestHelper.switchMap(request.getParameterMap());
        validateSignedRequest(paramMap);
        String hoscode = requireText(paramMap, "hoscode", "医院编号");
        String depcode = requireText(paramMap, "depcode", "科室编号");
        departmentService.remove(hoscode, depcode);
        return Result.ok();
    }

    @Operation(description = "科室获取分页列表")
    @PostMapping("department/list")
    public Result department(HttpServletRequest request) {
        //获取医院模拟系统传递数据
        Map<String, String[]> parameterMap = request.getParameterMap();
        Map<String, Object> newObjectMap = HttpRequestHelper.switchMap(parameterMap);
        validateSignedRequest(newObjectMap);

        String hoscode = requireText(newObjectMap, "hoscode", "医院编号");
        //非必填
        String depcode = (String)newObjectMap.get("depcode");

        int page = parsePositiveInt(newObjectMap, "page", 1, 10000);
        int limit = parsePositiveInt(newObjectMap, "limit", 10, 100);

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
        validateSignedRequest(newObjectMap);
        //调用方法添加
        departmentService.saveDept(newObjectMap);
        return Result.ok();
    }

    @Operation(description = "获取医院信息")
    @PostMapping("hospital/show")
    public Result hospital(HttpServletRequest request) {
        Map<String, Object> newObjectMap =
                HttpRequestHelper.switchMap(request.getParameterMap());
        validateSignedRequest(newObjectMap);
        //获取医院编号
        String hoscode = requireText(newObjectMap, "hoscode", "医院编号");
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

        validateSignedRequest(newObjectMap);

        //传输过程中“+”转换为了“ ”，因此我们要转换回来
        String logoData = (String)newObjectMap.get("logoData");
        if (logoData != null) {
            logoData = logoData.replace(" ", "+");
            newObjectMap.put("logoData",logoData);
        }

        //3 调用service方法添加
        hospitalService.saveHosp(newObjectMap);
        return Result.ok();
    }

    private void validateSignedRequest(Map<String, Object> paramMap) {
        String hoscode = (String) paramMap.get("hoscode");
        if (!StringUtils.hasText(hoscode)) {
            throw new YyghException(20001, "医院编号不能为空");
        }
        String signKey = hospitalSetService.getHospSignKey(hoscode);
        // 功能完善：所有医院数据交换接口统一校验签名及五分钟时间窗口。
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

    private int parsePositiveInt(Map<String, Object> paramMap,
                                 String key,
                                 int defaultValue,
                                 int maxValue) {
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
