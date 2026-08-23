package com.atguigu.yygh.model.acl;

import com.atguigu.yygh.model.base.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * <p>
 * 权限
 * </p>
 *
 * @author qy
 * @since 2019-11-08
 */
@Data
@Schema(description = "权限")
@TableName("acl_permission")
public class Permission extends BaseEntity {
	
	private static final long serialVersionUID = 1L;

	@Schema(description = "所属上级")
	@TableField("pid")
	private Long pid;

	@TableField("name")
	private String name;


	@TableField("type")
	private Integer type;


	@TableField("permission_value")
	private String permissionValue;


	@TableField("path")
	private String path;


	@TableField("component")
	private String component;


	@TableField("icon")
	private String icon;


	@TableField("status")
	private Integer status;


	@TableField(exist = false)
	private Integer level;


	@TableField(exist = false)
	private List<Permission> children;


	@TableField(exist = false)
	private boolean isSelect;

}

