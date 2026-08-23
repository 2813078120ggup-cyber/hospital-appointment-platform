package com.atguigu.yygh.model.acl;

import com.atguigu.yygh.model.base.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * <p>
 * 用户角色
 * </p>
 *
 * @author qy
 * @since 2019-11-08
 */
@Data
@Schema(description = "用户角色")
@TableName("acl_user_role")
public class UserRole extends BaseEntity {
	
	private static final long serialVersionUID = 1L;

	@TableField("role_id")
	private Long roleId;

	@TableField("user_id")
	private Long userId;

}

