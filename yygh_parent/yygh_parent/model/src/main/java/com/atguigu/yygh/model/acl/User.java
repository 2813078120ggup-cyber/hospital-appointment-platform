package com.atguigu.yygh.model.acl;

import com.atguigu.yygh.model.base.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * <p>
 * 用户
 * </p>
 *
 * @author qy
 * @since 2019-11-08
 */
@Data
@Schema(description = "用户")
@TableName("acl_user")
public class User extends BaseEntity {

	private static final long serialVersionUID = 1L;

	@TableField("username")
	private String username;

	@TableField("password")
	private String password;

	@TableField("nick_name")
	private String nickName;

	@TableField("salt")
	private String salt;

	@TableField("token")
	private String token;

}



