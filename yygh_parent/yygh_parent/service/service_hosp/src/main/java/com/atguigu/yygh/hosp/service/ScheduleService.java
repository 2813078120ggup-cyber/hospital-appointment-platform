package com.atguigu.yygh.hosp.service;

import com.atguigu.yygh.model.hosp.Schedule;
import com.atguigu.yygh.vo.hosp.ScheduleOrderVo;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.Map;

public interface ScheduleService {

    //上传排班
    void saveSchedule(Map<String, Object> newObjectMap);

    //获取排班分页列表
    Page<Schedule> selectPageSchedule(int page, int limit, String hoscode, String depcode);

    //删除
    void remove(String hoscode, String hosScheduleId);

    //根据医院编号 + 科室编号，查询可以预约日期数据，分页显示
    Map<String, Object> findScheduleRule(long page, long limit, String hoscode, String depcode);

    ////根据医院编号 + 科室编号 + 工作日期，查询科室里面医生排班详细信息
    List<Schedule> getScheduleDataDetail(String hoscode, String depcode, String workDate);

    //显示科室可以预约日期数据
    // 医院编号 +  科室编号  + 分页参数
    Map<String, Object> getBookingScheduleRule(Integer page, Integer limit, String hoscode, String depcode);

    //获取排班详情
    Schedule getScheduleId(String id);

    //下单辅助方法：根据排班主键查询排班订单信息
    // 根据排班id查询预约挂号相关数据
    ScheduleOrderVo getScheduleOrderVo(String scheduleId);

    //修改排班信息
    void update(Schedule schedule);

    //根据科室名称、日期、时间和可选医生查询是否有可预约号源
    boolean hasAvailableSchedule(String departmentName, String date, String time, String doctorName);

    // 功能完善：AI 正式下单前解析出具体排班主键，避免仅凭布尔结果创建模糊订单。
    Schedule findAvailableSchedule(String departmentName, String date, String time, String doctorName);
}
