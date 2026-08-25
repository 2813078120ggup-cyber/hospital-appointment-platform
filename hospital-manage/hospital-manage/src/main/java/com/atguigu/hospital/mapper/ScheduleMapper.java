package com.atguigu.hospital.mapper;

import com.atguigu.hospital.model.Schedule;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface ScheduleMapper extends BaseMapper<Schedule> {

    // 功能完善：下单事务内锁定排班行，防止并发超卖和重复预约号序。
    @Select("SELECT * FROM schedule WHERE id = #{id} FOR UPDATE")
    Schedule selectByIdForUpdate(@Param("id") Long id);
}
