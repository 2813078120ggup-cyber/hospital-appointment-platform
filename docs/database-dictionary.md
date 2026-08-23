# 数据库字典

> 本文档由 `tools/generate_reference_docs.py` 从初始化 SQL 静态生成。仓库中的 SQL 仅保留 DDL；源导出中的 INSERT 数据未复制，以避免提交用户、订单、支付或签名数据。

## 数据库概览

| 数据库 | 负责模块 | 表数量 | 源文件校验值（SHA-256 前 12 位） |
| --- | --- | ---: | --- |
| `yygh_cmn` | `service-cmn` | 1 | `37534013e6e8` |
| `yygh_hosp` | `service-hosp` | 1 | `2fc654b33483` |
| `yygh_manage` | `hospital-manage` | 3 | `e4bc3dae5b5b` |
| `yygh_order` | `service-orders` | 3 | `e3a13668a737` |
| `yygh_user` | `service-user` | 3 | `738cd69c579f` |

共 5 个数据库、11 张关系表、146 个字段。

## `yygh_cmn`

负责模块：`service-cmn`。

### `dict` — 组织架构表

| 字段 | 类型 | 可空 | 默认值 | 扩展 | 说明 |
| --- | --- | :---: | --- | --- | --- |
| `id` | `bigint` | 否 | `'0'` | — | id |
| `parent_id` | `bigint` | 否 | `'0'` | — | 上级id |
| `name` | `varchar(100)` | 否 | `''` | — | 名称 |
| `value` | `bigint` | 是 | `—` | — | 值 |
| `dict_code` | `varchar(20)` | 是 | `—` | — | 编码 |
| `create_time` | `timestamp` | 否 | `CURRENT_TIMESTAMP` | — | 创建时间 |
| `update_time` | `timestamp` | 否 | `CURRENT_TIMESTAMP` | ON UPDATE CURRENT_TIMESTAMP | 更新时间 |
| `is_deleted` | `tinyint` | 否 | `'0'` | — | 删除标记（0:不可用 1:可用） |

索引与约束：

- `PRIMARY KEY (`id`)`
- `KEY `idx_dict_code` (`dict_code`)`
- `KEY `idx_parent_id` (`parent_id`)`

## `yygh_hosp`

负责模块：`service-hosp`。

### `hospital_set` — 医院设置表

| 字段 | 类型 | 可空 | 默认值 | 扩展 | 说明 |
| --- | --- | :---: | --- | --- | --- |
| `id` | `bigint` | 否 | `—` | AUTO_INCREMENT | 编号 |
| `hosname` | `varchar(100)` | 是 | `—` | — | 医院名称 |
| `hoscode` | `varchar(30)` | 是 | `—` | — | 医院编号 |
| `api_url` | `varchar(100)` | 是 | `—` | — | api基础路径 |
| `sign_key` | `varchar(50)` | 是 | `—` | — | 签名秘钥 |
| `contacts_name` | `varchar(20)` | 是 | `—` | — | 联系人 |
| `contacts_phone` | `varchar(11)` | 是 | `—` | — | 联系人手机 |
| `status` | `tinyint` | 否 | `'0'` | — | 状态 |
| `create_time` | `timestamp` | 否 | `CURRENT_TIMESTAMP` | — | 创建时间 |
| `update_time` | `timestamp` | 否 | `CURRENT_TIMESTAMP` | ON UPDATE CURRENT_TIMESTAMP | 更新时间 |
| `is_deleted` | `tinyint` | 否 | `'0'` | — | 逻辑删除(1:已删除，0:未删除) |

索引与约束：

- `PRIMARY KEY (`id`)`
- `UNIQUE KEY `uk_hoscode` (`hoscode`)`

## `yygh_manage`

负责模块：`hospital-manage`。

### `hospital_set` — 医院设置表

| 字段 | 类型 | 可空 | 默认值 | 扩展 | 说明 |
| --- | --- | :---: | --- | --- | --- |
| `id` | `bigint` | 否 | `—` | AUTO_INCREMENT | 编号 |
| `hoscode` | `varchar(30)` | 是 | `—` | — | 医院编号 |
| `sign_key` | `varchar(50)` | 是 | `—` | — | 签名秘钥 |
| `api_url` | `varchar(100)` | 是 | `—` | — | 统一挂号平台api地址 |
| `create_time` | `timestamp` | 否 | `CURRENT_TIMESTAMP` | — | 创建时间 |
| `update_time` | `timestamp` | 否 | `CURRENT_TIMESTAMP` | ON UPDATE CURRENT_TIMESTAMP | 更新时间 |
| `is_deleted` | `tinyint` | 否 | `'0'` | — | 逻辑删除(1:已删除，0:未删除) |

索引与约束：

- `PRIMARY KEY (`id`)`

### `order_info` — 订单表

| 字段 | 类型 | 可空 | 默认值 | 扩展 | 说明 |
| --- | --- | :---: | --- | --- | --- |
| `id` | `bigint` | 否 | `—` | AUTO_INCREMENT | 编号 |
| `schedule_id` | `bigint` | 是 | `—` | — | 排班id |
| `patient_id` | `bigint` | 是 | `—` | — | 就诊人id |
| `number` | `int` | 是 | `—` | — | 预约号序 |
| `fetch_time` | `varchar(50)` | 是 | `—` | — | 建议取号时间 |
| `fetch_address` | `varchar(255)` | 是 | `—` | — | 取号地点 |
| `amount` | `decimal(10,0)` | 是 | `—` | — | 医事服务费 |
| `pay_time` | `datetime` | 是 | `—` | — | 支付时间 |
| `quit_time` | `datetime` | 是 | `—` | — | 退号时间 |
| `order_status` | `tinyint` | 是 | `—` | — | 订单状态 |
| `create_time` | `timestamp` | 否 | `CURRENT_TIMESTAMP` | — | 创建时间 |
| `update_time` | `timestamp` | 否 | `CURRENT_TIMESTAMP` | ON UPDATE CURRENT_TIMESTAMP | 更新时间 |
| `is_deleted` | `tinyint` | 否 | `'0'` | — | 逻辑删除(1:已删除，0:未删除) |

索引与约束：

- `PRIMARY KEY (`id`)`

### `schedule` — 医生日程安排表

| 字段 | 类型 | 可空 | 默认值 | 扩展 | 说明 |
| --- | --- | :---: | --- | --- | --- |
| `id` | `bigint` | 否 | `'0'` | — | 编号 |
| `hoscode` | `varchar(30)` | 是 | `—` | — | 医院编号 |
| `depcode` | `varchar(30)` | 是 | `—` | — | 科室编号 |
| `title` | `varchar(20)` | 是 | `—` | — | 职称 |
| `docname` | `varchar(20)` | 是 | `—` | — | 医生名称 |
| `skill` | `text` | 是 | `—` | — | — |
| `work_date` | `date` | 是 | `—` | — | 安排日期 |
| `work_time` | `tinyint` | 是 | `—` | — | 安排时间（0：上午 1：下午） |
| `reserved_number` | `int` | 是 | `—` | — | 可预约数 |
| `available_number` | `int` | 是 | `—` | — | 剩余预约数 |
| `amount` | `decimal(10,0)` | 是 | `—` | — | 挂号费 |
| `status` | `tinyint` | 是 | `—` | — | 排班状态（-1：停诊 0：停约 1：可约） |
| `create_time` | `timestamp` | 否 | `CURRENT_TIMESTAMP` | — | 创建时间 |
| `update_time` | `timestamp` | 否 | `CURRENT_TIMESTAMP` | ON UPDATE CURRENT_TIMESTAMP | 更新时间 |
| `is_deleted` | `tinyint` | 否 | `'0'` | — | 逻辑删除(1:已删除，0:未删除) |

索引与约束：

- `PRIMARY KEY (`id`)`

## `yygh_order`

负责模块：`service-orders`。

### `order_info` — 订单表

| 字段 | 类型 | 可空 | 默认值 | 扩展 | 说明 |
| --- | --- | :---: | --- | --- | --- |
| `id` | `bigint` | 否 | `—` | AUTO_INCREMENT | 编号 |
| `user_id` | `bigint` | 是 | `—` | — | — |
| `out_trade_no` | `varchar(300)` | 是 | `—` | — | 订单交易号 |
| `hoscode` | `varchar(30)` | 是 | `—` | — | 医院编号 |
| `hosname` | `varchar(100)` | 是 | `—` | — | 医院名称 |
| `depcode` | `varchar(30)` | 是 | `—` | — | 科室编号 |
| `depname` | `varchar(20)` | 是 | `—` | — | 科室名称 |
| `title` | `varchar(20)` | 是 | `—` | — | 医生职称 |
| `hos_schedule_id` | `varchar(50)` | 是 | `—` | — | 排班编号（医院自己的排班主键） |
| `reserve_date` | `date` | 是 | `—` | — | 安排日期 |
| `reserve_time` | `tinyint` | 是 | `—` | — | 安排时间（0：上午 1：下午） |
| `patient_id` | `bigint` | 是 | `—` | — | 就诊人id |
| `patient_name` | `varchar(20)` | 是 | `—` | — | 就诊人名称 |
| `patient_phone` | `varchar(11)` | 是 | `—` | — | 就诊人手机 |
| `hos_record_id` | `varchar(30)` | 是 | `—` | — | 预约记录唯一标识（医院预约记录主键） |
| `number` | `int` | 是 | `—` | — | 预约号序 |
| `fetch_time` | `varchar(50)` | 是 | `—` | — | 建议取号时间 |
| `fetch_address` | `varchar(255)` | 是 | `—` | — | 取号地点 |
| `amount` | `decimal(10,0)` | 是 | `—` | — | 医事服务费 |
| `quit_time` | `datetime` | 是 | `—` | — | 退号时间 |
| `order_status` | `tinyint` | 是 | `—` | — | 订单状态 |
| `create_time` | `timestamp` | 否 | `CURRENT_TIMESTAMP` | — | 创建时间 |
| `update_time` | `timestamp` | 否 | `CURRENT_TIMESTAMP` | ON UPDATE CURRENT_TIMESTAMP | 更新时间 |
| `is_deleted` | `tinyint` | 否 | `'0'` | — | 逻辑删除(1:已删除，0:未删除) |

索引与约束：

- `PRIMARY KEY (`id`)`
- `UNIQUE KEY `uk_out_trade_no` (`out_trade_no`)`
- `KEY `idx_user_id` (`user_id`)`
- `KEY `idx_hoscode` (`hoscode`)`
- `KEY `idx_hos_schedule_id` (`hos_schedule_id`)`
- `KEY `idx_hos_record_id` (`hos_record_id`)`

### `payment_info` — 支付信息表

| 字段 | 类型 | 可空 | 默认值 | 扩展 | 说明 |
| --- | --- | :---: | --- | --- | --- |
| `id` | `int` | 否 | `—` | AUTO_INCREMENT | 编号 |
| `out_trade_no` | `varchar(30)` | 是 | `—` | — | 对外业务编号 |
| `order_id` | `bigint` | 是 | `—` | — | 订单id |
| `payment_type` | `tinyint(1)` | 是 | `—` | — | 支付类型（微信 支付宝） |
| `trade_no` | `varchar(50)` | 是 | `—` | — | 交易编号 |
| `total_amount` | `decimal(10,2)` | 是 | `—` | — | 支付金额 |
| `subject` | `varchar(200)` | 是 | `—` | — | 交易内容 |
| `payment_status` | `tinyint` | 是 | `—` | — | 支付状态 |
| `callback_time` | `datetime` | 是 | `—` | — | 回调时间 |
| `callback_content` | `varchar(1000)` | 是 | `—` | — | 回调信息 |
| `create_time` | `timestamp` | 否 | `CURRENT_TIMESTAMP` | — | 创建时间 |
| `update_time` | `timestamp` | 否 | `CURRENT_TIMESTAMP` | ON UPDATE CURRENT_TIMESTAMP | 更新时间 |
| `is_deleted` | `tinyint` | 否 | `'0'` | — | 逻辑删除(1:已删除，0:未删除) |

索引与约束：

- `PRIMARY KEY (`id`)`
- `KEY `idx_out_trade_no` (`out_trade_no`)`
- `KEY `idx_order_id` (`order_id`)`

### `refund_info` — 退款信息表

| 字段 | 类型 | 可空 | 默认值 | 扩展 | 说明 |
| --- | --- | :---: | --- | --- | --- |
| `id` | `int` | 否 | `—` | AUTO_INCREMENT | 编号 |
| `out_trade_no` | `varchar(50)` | 是 | `—` | — | 对外业务编号 |
| `order_id` | `bigint` | 是 | `—` | — | 订单编号 |
| `payment_type` | `tinyint` | 是 | `—` | — | 支付类型（微信 支付宝） |
| `trade_no` | `varchar(50)` | 是 | `—` | — | 交易编号 |
| `total_amount` | `decimal(10,2)` | 是 | `—` | — | 退款金额 |
| `subject` | `varchar(200)` | 是 | `—` | — | 交易内容 |
| `refund_status` | `tinyint` | 是 | `—` | — | 退款状态 |
| `callback_content` | `varchar(1000)` | 是 | `—` | — | 回调信息 |
| `callback_time` | `datetime` | 是 | `—` | — | — |
| `create_time` | `timestamp` | 否 | `CURRENT_TIMESTAMP` | — | 创建时间 |
| `update_time` | `timestamp` | 否 | `CURRENT_TIMESTAMP` | ON UPDATE CURRENT_TIMESTAMP | 更新时间 |
| `is_deleted` | `tinyint` | 否 | `'0'` | — | 逻辑删除(1:已删除，0:未删除) |

索引与约束：

- `PRIMARY KEY (`id`)`
- `KEY `idx_out_trade_no` (`out_trade_no`)`
- `KEY `idx_order_id` (`order_id`)`

## `yygh_user`

负责模块：`service-user`。

### `patient` — 就诊人表

| 字段 | 类型 | 可空 | 默认值 | 扩展 | 说明 |
| --- | --- | :---: | --- | --- | --- |
| `id` | `bigint` | 否 | `—` | AUTO_INCREMENT | 编号 |
| `user_id` | `bigint` | 是 | `—` | — | 用户id |
| `name` | `varchar(20)` | 是 | `—` | — | 姓名 |
| `certificates_type` | `varchar(3)` | 是 | `—` | — | 证件类型 |
| `certificates_no` | `varchar(30)` | 是 | `—` | — | 证件编号 |
| `sex` | `tinyint` | 是 | `—` | — | 性别 |
| `birthdate` | `date` | 是 | `—` | — | 出生年月 |
| `phone` | `varchar(11)` | 是 | `—` | — | 手机 |
| `is_marry` | `tinyint` | 是 | `—` | — | 是否结婚 |
| `province_code` | `varchar(20)` | 是 | `—` | — | 省code |
| `city_code` | `varchar(20)` | 是 | `—` | — | 市code |
| `district_code` | `varchar(20)` | 是 | `—` | — | 区code |
| `address` | `varchar(100)` | 是 | `—` | — | 详情地址 |
| `contacts_name` | `varchar(20)` | 是 | `—` | — | 联系人姓名 |
| `contacts_certificates_type` | `varchar(3)` | 是 | `—` | — | 联系人证件类型 |
| `contacts_certificates_no` | `varchar(30)` | 是 | `—` | — | 联系人证件号 |
| `contacts_phone` | `varchar(11)` | 是 | `—` | — | 联系人手机 |
| `card_no` | `varchar(50)` | 是 | `—` | — | 就诊卡号 |
| `is_insure` | `tinyint` | 是 | `—` | — | 是否有医保 |
| `status` | `tinyint` | 否 | `'0'` | — | 状态（0：默认 1：已认证） |
| `create_time` | `timestamp` | 否 | `CURRENT_TIMESTAMP` | — | 创建时间 |
| `update_time` | `timestamp` | 否 | `CURRENT_TIMESTAMP` | ON UPDATE CURRENT_TIMESTAMP | 更新时间 |
| `is_deleted` | `tinyint` | 否 | `'0'` | — | 逻辑删除(1:已删除，0:未删除) |

索引与约束：

- `PRIMARY KEY (`id`)`
- `KEY `idx_user_id` (`user_id`)`

### `user_info` — 用户表

| 字段 | 类型 | 可空 | 默认值 | 扩展 | 说明 |
| --- | --- | :---: | --- | --- | --- |
| `id` | `bigint` | 否 | `—` | AUTO_INCREMENT | 编号 |
| `openid` | `varchar(100)` | 是 | `—` | — | 微信openid |
| `nick_name` | `varchar(20)` | 是 | `—` | — | 昵称 |
| `phone` | `varchar(11)` | 否 | `''` | — | 手机号 |
| `name` | `varchar(20)` | 是 | `—` | — | 用户姓名 |
| `certificates_type` | `varchar(3)` | 是 | `—` | — | 证件类型 |
| `certificates_no` | `varchar(30)` | 是 | `—` | — | 证件编号 |
| `certificates_url` | `varchar(200)` | 是 | `—` | — | 证件路径 |
| `auth_status` | `tinyint` | 否 | `'0'` | — | 认证状态（0：未认证 1：认证中 2：认证成功 -1：认证失败） |
| `status` | `tinyint` | 否 | `'1'` | — | 状态（0：锁定 1：正常） |
| `create_time` | `timestamp` | 否 | `CURRENT_TIMESTAMP` | — | 创建时间 |
| `update_time` | `timestamp` | 否 | `CURRENT_TIMESTAMP` | ON UPDATE CURRENT_TIMESTAMP | 更新时间 |
| `is_deleted` | `tinyint` | 否 | `'0'` | — | 逻辑删除(1:已删除，0:未删除) |

索引与约束：

- `PRIMARY KEY (`id`)`
- `KEY `uk_mobile` (`phone`)`

### `user_login_record` — 用户登录记录表

| 字段 | 类型 | 可空 | 默认值 | 扩展 | 说明 |
| --- | --- | :---: | --- | --- | --- |
| `id` | `bigint` | 否 | `—` | AUTO_INCREMENT | 编号 |
| `user_id` | `bigint` | 是 | `—` | — | 用户id |
| `ip` | `varchar(32)` | 是 | `—` | — | ip |
| `create_time` | `timestamp` | 否 | `CURRENT_TIMESTAMP` | — | 创建时间 |
| `update_time` | `timestamp` | 否 | `CURRENT_TIMESTAMP` | ON UPDATE CURRENT_TIMESTAMP | 更新时间 |
| `is_deleted` | `tinyint` | 否 | `'0'` | — | 逻辑删除(1:已删除，0:未删除) |

索引与约束：

- `PRIMARY KEY (`id`)`
- `KEY `idx_user_id` (`user_id`)`
