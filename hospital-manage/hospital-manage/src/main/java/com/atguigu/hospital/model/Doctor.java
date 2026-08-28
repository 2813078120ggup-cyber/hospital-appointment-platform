package com.atguigu.hospital.model;

import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 医生账号
 */
@Data
@ApiModel(description = "Doctor")
@TableName("doctor")
public class Doctor extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "登录账号")
    private String username;

    @ApiModelProperty(value = "登录密码(MD5)")
    private String password;

    @ApiModelProperty(value = "医生姓名（对应排班 docname）")
    private String docname;

    @ApiModelProperty(value = "职称")
    private String title;

    @ApiModelProperty(value = "联系电话")
    private String phone;

    @ApiModelProperty(value = "状态（1：启用 0：停用）")
    private Integer status;
}
