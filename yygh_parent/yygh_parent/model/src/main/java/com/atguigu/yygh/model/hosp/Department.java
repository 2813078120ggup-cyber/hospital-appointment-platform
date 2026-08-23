package com.atguigu.yygh.model.hosp;

import com.atguigu.yygh.model.base.BaseEntity;
import com.atguigu.yygh.model.base.BaseMongoEntity;
import lombok.Data;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

/**
 * <p>
 * Department
 * </p>
 *
 * @author qy
 */
@Data
@Document("Department")
public class Department extends BaseMongoEntity {
	
	private static final long serialVersionUID = 1L;

	@Indexed //普通索引
	private String hoscode;

	@Indexed(unique = true) //唯一索引
	private String depcode;

	private String depname;

	private String intro;

	private String bigcode;

	private String bigname;

}

