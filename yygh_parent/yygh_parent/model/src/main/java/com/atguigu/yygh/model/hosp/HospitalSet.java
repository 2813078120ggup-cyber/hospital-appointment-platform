package com.atguigu.yygh.model.hosp;

import com.atguigu.yygh.model.base.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * <p>
 * HospitalSet
 * </p>
 *
 * @author qy
 */
@Data
@TableName("hospital_set")
public class HospitalSet extends BaseEntity {
	
	private static final long serialVersionUID = 1L;

	@TableField("hosname")
	private String hosname;

	@TableField("hoscode")
	private String hoscode;

	@TableField("api_url")
	private String apiUrl;

	@TableField("sign_key")
	private String signKey;

	@TableField("contacts_name")
	private String contactsName;

	@TableField("contacts_phone")
	private String contactsPhone;

	@TableField("status")
	private Integer status;
}

