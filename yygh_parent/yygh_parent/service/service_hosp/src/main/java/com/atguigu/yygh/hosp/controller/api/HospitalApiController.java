package com.atguigu.yygh.hosp.controller.api;

import com.atguigu.yygh.common.result.R;
import com.atguigu.yygh.hosp.service.DepartmentService;
import com.atguigu.yygh.hosp.service.HospitalService;
import com.atguigu.yygh.hosp.service.ScheduleService;
import com.atguigu.yygh.model.hosp.Hospital;
import com.atguigu.yygh.model.hosp.Schedule;
import com.atguigu.yygh.vo.hosp.DepartmentVo;
import com.atguigu.yygh.vo.hosp.HospitalQueryVo;
import com.atguigu.yygh.vo.hosp.ScheduleOrderVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@Schema(description = "医院显示接口")
@RestController
@RequestMapping("/api/hosp/hospital")
public class HospitalApiController {

    @Autowired
    private HospitalService hospitalService;

    @Autowired
    private DepartmentService departmentService;

    @Autowired
    private ScheduleService scheduleService;

    @Operation(description = "获取排班详情")
    @GetMapping("getSchedule/{id}")
    public R getScheduleList(
            @PathVariable String id) {
        Schedule schedule = scheduleService.getScheduleId(id);
        return R.ok().data("schedule",schedule);
    }

    //显示科室可以预约日期数据
    // 医院编号 +  科室编号  + 分页参数
    @Operation(description = "获取可预约排班数据")
    @GetMapping("auth/getBookingScheduleRule/{page}/{limit}/{hoscode}/{depcode}")
    public R getBookingSchedule(
            @PathVariable Integer page,
            @PathVariable Integer limit,
            @PathVariable String hoscode,
            @PathVariable String depcode) {
        Map<String,Object> map =
                scheduleService.getBookingScheduleRule(page,limit,hoscode,depcode);
        return R.ok().data(map);
    }

    @Operation(description = "获取排班数据")
    @GetMapping("auth/findScheduleList/{hoscode}/{depcode}/{workDate}")
    public R findScheduleList(
            @PathVariable String hoscode,
            @PathVariable String depcode,
            @PathVariable String workDate) {
        List<Schedule> scheduleList = scheduleService.getScheduleDataDetail(hoscode, depcode, workDate);
        return R.ok().data("scheduleList",scheduleList);
    }

    @Operation(description = "获取科室列表")
    @GetMapping("department/{hoscode}")
    public R index(@PathVariable String hoscode) {
        List<DepartmentVo> list = departmentService.getDeptTree(hoscode);
        return R.ok().data("list",list);
    }

    //条件查询医院列表
    @Operation(description = "获取分页列表")
    @GetMapping("{page}/{limit}")
    public R index(
            @PathVariable Integer page,
            @PathVariable Integer limit,
            HospitalQueryVo hospitalQueryVo) {
        //调用方法
        Page<Hospital> pageModel =
                hospitalService.selectPageHosp(page, limit, hospitalQueryVo);
        return R.ok().data("pages",pageModel);
    }

    //医院名称模糊查询
    @Operation(description = "根据医院名称获取医院列表")
    @GetMapping("findByHosname/{hosname}")
    public R findByHosname(@PathVariable String hosname) {
        List<Hospital> list = hospitalService.getHospLike(hosname);
        return R.ok().data("list",list);
    }

    //根据医院编号获取医院详情
    @Operation(description = "医院预约挂号详情")
    @GetMapping("{hoscode}")
    public R item(
            @PathVariable String hoscode) {
        Map<String,Object> map = hospitalService.selectHospByHoscode(hoscode);
        return R.ok().data(map);
    }


    //--------------------------------------------
    @GetMapping("inner/getScheduleOrderVo/{scheduleId}")
    public ScheduleOrderVo getScheduleOrderVo(@PathVariable("scheduleId") String scheduleId) {
        return scheduleService.getScheduleOrderVo(scheduleId);
    }
}
