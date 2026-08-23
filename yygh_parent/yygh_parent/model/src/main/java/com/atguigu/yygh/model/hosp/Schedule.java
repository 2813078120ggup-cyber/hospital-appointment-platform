package com.atguigu.yygh.model.hosp;

import com.atguigu.yygh.model.base.BaseEntity;
import com.atguigu.yygh.model.base.BaseMongoEntity;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.util.Date;

/**
 * <p>
 * Schedule
 * </p>
 *
 * @author qy
 */
@Data
@Document("Schedule")
public class Schedule extends BaseMongoEntity {
	
	private static final long serialVersionUID = 1L;

	@Indexed //普通索引
	private String hoscode;

	@Indexed //普通索引
	private String depcode;

	private String title;

	private String docname;

	private String skill;

	@JsonFormat(pattern = "yyyy-MM-dd")
	private Date workDate;

	private Integer workTime;

	private Integer reservedNumber;

	private Integer availableNumber;

	private BigDecimal amount;

	//@ApiModelProperty(value = "排班状态（-1：停诊 0：停约 1：可约）")
	private Integer status;

	//@ApiModelProperty(value = "排班编号（医院自己的排班主键）")
	@Indexed //普通索引
	private String hosScheduleId;

}

