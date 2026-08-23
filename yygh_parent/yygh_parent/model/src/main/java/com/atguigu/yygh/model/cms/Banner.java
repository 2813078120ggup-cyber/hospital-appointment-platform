package com.atguigu.yygh.model.cms;

import com.atguigu.yygh.model.base.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * <p>
 * 首页Banner实体
 * </p>
 *
 * @author qy
 * @since 2019-11-08
 */
@Data
@TableName("banner")
public class Banner extends BaseEntity {
	
	private static final long serialVersionUID = 1L;

	@TableField("title")
	private String title;

	@TableField("image_url")
	private String imageUrl;

	@TableField("link_url")
	private String linkUrl;

	@TableField("sort")
	private Integer sort;

}

