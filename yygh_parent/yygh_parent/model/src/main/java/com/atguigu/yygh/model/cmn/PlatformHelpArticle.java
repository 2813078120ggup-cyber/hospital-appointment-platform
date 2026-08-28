package com.atguigu.yygh.model.cmn;

import com.atguigu.yygh.model.base.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;

/**
 * 帮助中心文章。
 */
@Data
@TableName("platform_help_article")
public class PlatformHelpArticle extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @TableField("category_code")
    private String categoryCode;

    @TableField("category_name")
    private String categoryName;

    @TableField("title")
    private String title;

    @TableField("summary")
    private String summary;

    @TableField("content")
    private String content;

    @TableField("keywords")
    private String keywords;

    @TableField("status")
    private Integer status;

    @TableField("sort")
    private Integer sort;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField("publish_time")
    private Date publishTime;
}
