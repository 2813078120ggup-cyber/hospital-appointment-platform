package com.atguigu.yygh.model.hosp;

import com.atguigu.yygh.model.base.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 意见反馈
 */
@Data
@TableName("feedback")
public class Feedback extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @TableField("type")
    private Integer type;

    @TableField("user_id")
    private Long userId;

    @TableField("user_name")
    private String userName;

    @TableField("phone")
    private String phone;

    @TableField("hoscode")
    private String hoscode;

    @TableField("hosname")
    private String hosname;

    @TableField("content")
    private String content;

    @TableField("status")
    private Integer status;

    @TableField("reply")
    private String reply;
}
