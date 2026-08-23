package com.atguigu.yygh.model.acl;

import com.atguigu.yygh.model.base.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * <p>
 * 角色
 * </p>
 *
 * @author qy
 * @since 2019-11-08
 */
@Data
@Schema(description = "角色")
@TableName("acl_role")
public class Role extends BaseEntity {
	
	private static final long serialVersionUID = 1L;

	@TableField("role_name")
	private String roleName;

	@TableField("remark")
	private String remark;

}

