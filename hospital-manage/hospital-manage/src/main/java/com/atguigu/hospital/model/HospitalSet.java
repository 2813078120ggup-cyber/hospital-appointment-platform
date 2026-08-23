package com.atguigu.hospital.model;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * 医院设置
 */
@Data
@ApiModel(description = "HospitalSet")
@TableName("hospital_set")
public class HospitalSet {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "id")
    @TableId(type = IdType.INPUT)
    private Long id;

    @ApiModelProperty(value = "创建时间")
    @TableField("create_time")
    private Date createTime;

    @ApiModelProperty(value = "更新时间")
    @TableField("update_time")
    private Date updateTime;

    @ApiModelProperty(value = "逻辑删除(1:已删除，0:未删除)")
    @TableLogic
    @TableField("is_deleted")
    private Integer isDeleted;

    @ApiModelProperty(value = "其他参数")
    @TableField(exist = false)
    private Map<String, Object> param = new HashMap<>();

    @ApiModelProperty(value = "医院编号")
    private String hoscode;

    @ApiModelProperty(value = "签名秘钥")
    private String signKey;

    @ApiModelProperty(value = "api基础路径")
    private String apiUrl;

}

