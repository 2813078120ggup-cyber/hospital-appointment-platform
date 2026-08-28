-- 帮助中心文章。管理端维护，用户端只读取已发布内容。
CREATE TABLE IF NOT EXISTS `platform_help_article` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
  `category_code` varchar(40) NOT NULL COMMENT '分类编码',
  `category_name` varchar(50) NOT NULL COMMENT '分类名称',
  `title` varchar(200) NOT NULL COMMENT '文章标题',
  `summary` varchar(500) DEFAULT NULL COMMENT '文章摘要',
  `content` text NOT NULL COMMENT '文章正文',
  `keywords` varchar(500) DEFAULT NULL COMMENT '搜索关键词',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '状态 0草稿 1已发布',
  `sort` int NOT NULL DEFAULT '0' COMMENT '展示排序',
  `publish_time` datetime DEFAULT NULL COMMENT '发布时间',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  KEY `idx_help_status_sort` (`status`, `sort`, `publish_time`),
  KEY `idx_help_category_status` (`category_code`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='平台帮助中心文章';

INSERT INTO `platform_help_article`
  (`category_code`, `category_name`, `title`, `summary`, `content`, `keywords`, `status`, `sort`, `publish_time`)
SELECT 'registration', '预约挂号', '如何预约挂号', '从选择医院到提交订单的完整预约流程。',
       '第一步：在首页选择医院，进入医院详情。\n第二步：选择科室、就诊日期和可预约的排班。\n第三步：确认医生、费用和放号信息，点击立即预约。\n第四步：选择就诊人并核对信息，提交挂号订单。\n第五步：按页面提示完成支付，支付成功后可在挂号订单中查看详情。',
       '预约 挂号 医院 科室 医生 排班 订单', 1, 100, NOW()
WHERE NOT EXISTS (
  SELECT 1 FROM `platform_help_article`
  WHERE `category_code` = 'registration' AND `title` = '如何预约挂号' AND `is_deleted` = 0
);

INSERT INTO `platform_help_article`
  (`category_code`, `category_name`, `title`, `summary`, `content`, `keywords`, `status`, `sort`, `publish_time`)
SELECT 'registration', '预约挂号', '如何选择医院和科室', '根据疾病症状和医院服务范围选择合适的就诊入口。',
       '可先通过首页的医院等级和地区筛选缩小范围，再进入医院查看科室。\n如果不确定科室，可根据主要症状选择相近的常见科室，或先咨询医院导诊。\n请同时关注医院预约须知、放号时间和停诊公告，避免错过预约。',
       '选择医院 科室 地区 等级 导诊 放号', 1, 90, NOW()
WHERE NOT EXISTS (
  SELECT 1 FROM `platform_help_article`
  WHERE `category_code` = 'registration' AND `title` = '如何选择医院和科室' AND `is_deleted` = 0
);

INSERT INTO `platform_help_article`
  (`category_code`, `category_name`, `title`, `summary`, `content`, `keywords`, `status`, `sort`, `publish_time`)
SELECT 'account', '账号与实名', '如何完成实名认证', '实名认证用于确认预约人与就诊信息。',
       '登录后打开右上角用户菜单，进入实名认证。\n按页面要求填写真实姓名、证件类型和证件号码，确认无误后提交。\n认证信息应与就诊证件保持一致。若审核未通过，请根据审核意见修改后重新提交。',
       '实名认证 姓名 身份证 审核 账号', 1, 80, NOW()
WHERE NOT EXISTS (
  SELECT 1 FROM `platform_help_article`
  WHERE `category_code` = 'account' AND `title` = '如何完成实名认证' AND `is_deleted` = 0
);

INSERT INTO `platform_help_article`
  (`category_code`, `category_name`, `title`, `summary`, `content`, `keywords`, `status`, `sort`, `publish_time`)
SELECT 'patient', '就诊人管理', '如何添加和管理就诊人', '维护本人或家庭成员的真实就诊资料。',
       '登录后从右上角用户菜单进入就诊人管理。\n点击添加就诊人，按证件填写姓名、证件号码、性别、出生日期和联系方式。\n提交前请仔细核对，挂号时需要选择其中一位就诊人。\n已有就诊人可在列表中查看或修改，请勿重复添加同一人。',
       '就诊人 添加 家庭成员 证件 联系方式', 1, 70, NOW()
WHERE NOT EXISTS (
  SELECT 1 FROM `platform_help_article`
  WHERE `category_code` = 'patient' AND `title` = '如何添加和管理就诊人' AND `is_deleted` = 0
);

INSERT INTO `platform_help_article`
  (`category_code`, `category_name`, `title`, `summary`, `content`, `keywords`, `status`, `sort`, `publish_time`)
SELECT 'payment', '支付与退款', '挂号费用如何支付', '提交订单后在有效时间内完成支付。',
       '提交挂号订单后，进入订单详情并点击支付。\n请在订单显示的支付有效期内完成付款，超时订单可能自动关闭。\n支付完成后不要重复付款，可刷新订单详情确认状态。\n本地演示环境使用模拟支付，不会产生真实资金交易。',
       '支付 微信 费用 订单 超时 模拟支付', 1, 60, NOW()
WHERE NOT EXISTS (
  SELECT 1 FROM `platform_help_article`
  WHERE `category_code` = 'payment' AND `title` = '挂号费用如何支付' AND `is_deleted` = 0
);

INSERT INTO `platform_help_article`
  (`category_code`, `category_name`, `title`, `summary`, `content`, `keywords`, `status`, `sort`, `publish_time`)
SELECT 'payment', '支付与退款', '退款什么时候到账', '退款时间取决于取消结果和原支付渠道。',
       '订单符合取消条件并取消成功后，平台会按原支付渠道发起退款。\n退款到账时间由支付渠道和银行处理速度决定，请以订单退款状态和支付渠道通知为准。\n如订单长时间没有更新，请记录订单号后联系平台客服核查。',
       '退款 到账 原路退回 支付渠道 取消', 1, 50, NOW()
WHERE NOT EXISTS (
  SELECT 1 FROM `platform_help_article`
  WHERE `category_code` = 'payment' AND `title` = '退款什么时候到账' AND `is_deleted` = 0
);

INSERT INTO `platform_help_article`
  (`category_code`, `category_name`, `title`, `summary`, `content`, `keywords`, `status`, `sort`, `publish_time`)
SELECT 'cancel', '取消预约', '如何取消预约', '在医院规定的截止时间前从订单详情申请取消。',
       '登录后进入挂号订单，打开需要取消的订单详情。\n确认订单仍在医院允许的取消时间内，然后点击取消预约并确认。\n平台会同步医院取消结果并更新订单状态。已支付订单在取消成功后进入退款流程。\n超过取消截止时间或已取号的订单，通常无法在线取消。',
       '取消预约 退号 截止时间 订单 退款', 1, 40, NOW()
WHERE NOT EXISTS (
  SELECT 1 FROM `platform_help_article`
  WHERE `category_code` = 'cancel' AND `title` = '如何取消预约' AND `is_deleted` = 0
);

INSERT INTO `platform_help_article`
  (`category_code`, `category_name`, `title`, `summary`, `content`, `keywords`, `status`, `sort`, `publish_time`)
SELECT 'visit', '就诊服务', '就诊当天需要准备什么', '提前准备证件、订单信息并按医院要求到院。',
       '请携带就诊人有效证件，以及医院要求的就诊卡或医保凭证。\n提前查看订单中的医院、院区、科室、日期和时段，并预留取号与候诊时间。\n到院后按医院指引取号和报到。若医院发送了临时通知，请以最新通知为准。',
       '就诊 当天 证件 取号 报到 医保', 1, 30, NOW()
WHERE NOT EXISTS (
  SELECT 1 FROM `platform_help_article`
  WHERE `category_code` = 'visit' AND `title` = '就诊当天需要准备什么' AND `is_deleted` = 0
);
