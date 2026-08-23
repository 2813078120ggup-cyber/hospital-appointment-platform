package com.atguigu.yygh.model.acl;

import com.atguigu.yygh.model.base.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * <p>
 * 角色权限
 * </p>
 *
 * @author qy
 * @since 2019-11-08
 */
@Data
@Schema(description = "角色权限")
@TableName("acl_role_permission")
public class RolePermission extends BaseEntity {
	
	private static final long serialVersionUID = 1L;

	@TableField("role_id")
	private Long roleId;

	@TableField("permission_id")
	private Long permissionId;

}

