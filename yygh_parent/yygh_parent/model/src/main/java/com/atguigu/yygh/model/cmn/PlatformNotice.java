package com.atguigu.yygh.model.cmn;

import com.atguigu.yygh.model.base.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;

/**
 * 平台公告与停诊公告。
 */
@Data
@TableName("platform_notice")
public class PlatformNotice extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @TableField("notice_type")
    private Integer noticeType;

    @TableField("title")
    private String title;

    @TableField("summary")
    private String summary;

    @TableField("content")
    private String content;

    @TableField("hoscode")
    private String hoscode;

    @TableField("hosname")
    private String hosname;

    @TableField("status")
    private Integer status;

    @TableField("sort")
    private Integer sort;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField("publish_time")
    private Date publishTime;
}
