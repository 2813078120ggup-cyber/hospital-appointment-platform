package com.atguigu.yygh.model.cmn;

import com.atguigu.yygh.model.base.BaseEntity;
import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * <p>
 * Dict
 * </p>
 *
 * @author qy
 */
@Data
@Schema(description = "数据字典")
@TableName("dict")
public class Dict {

    private static final long serialVersionUID = 1L;

    private Long id;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField("create_time")
    private Date createTime;

    @TableField("update_time")
    private Date updateTime;

    @TableLogic
    @TableField("is_deleted")
    private Integer isDeleted;

    @TableField("parent_id")
    private Long parentId;

    @TableField("name")
    private String name;

    @TableField("value")
    private String value;

    @TableField("dict_code")
    private String dictCode;

    @TableField(exist = false)
    private Map<String,Object> param = new HashMap<>();

    @TableField(exist = false)
    private boolean hasChildren;

}