package com.atguigu.yygh.model.hosp;

import com.alibaba.fastjson.JSONArray;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.util.StringUtils;

import java.util.Date;
import java.util.List;

/**
 * <p>
 * RegisterRule
 * </p>
 *
 * @author qy
 */
@Data
@Schema(description = "预约规则")
@Document("BookingRule")
public class BookingRule {
	private Integer cycle;

	private String releaseTime;

	private String stopTime;

	private Integer quitDay;

	private String quitTime;

	private List<String> rule;

	/**
	 *
	 * @param rule
	 */
	public void setRule(String rule) {
		if(!StringUtils.isEmpty(rule)) {
			this.rule = JSONArray.parseArray(rule, String.class);
		}
	}

}

