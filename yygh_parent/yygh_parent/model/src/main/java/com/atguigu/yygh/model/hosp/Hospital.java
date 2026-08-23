package com.atguigu.yygh.model.hosp;

import com.alibaba.fastjson.JSONObject;
import com.atguigu.yygh.model.base.BaseEntity;
import com.atguigu.yygh.model.base.BaseMongoEntity;
import lombok.Data;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * <p>
 * Hospital
 * </p>
 *
 * @author qy
 */
@Data

@Document("Hospital")
public class Hospital extends BaseMongoEntity {
	
	private static final long serialVersionUID = 1L;

	@Indexed(unique = true) //唯一索引
	private String hoscode;

	@Indexed //普通索引
	private String hosname;

	private String hostype;

	private String provinceCode;

	private String cityCode;

	private String districtCode;

	private String address;

	private String logoData;

	private String intro;

	private String route;

	private Integer status;

	//预约规则
	private BookingRule bookingRule;

	public void setBookingRule(String bookingRule) {
		this.bookingRule = JSONObject.parseObject(bookingRule, BookingRule.class);
	}

}

