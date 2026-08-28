package com.atguigu.hospital.mapper;

import com.atguigu.hospital.model.Patient;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 医院侧就诊人数据访问。
 *
 * <p>医院数据库中的就诊人是医院自己的患者档案，与平台用户服务中的
 * {@code patient} 表使用不同的主键。</p>
 */
@Mapper
public interface PatientMapper extends BaseMapper<Patient> {
}
