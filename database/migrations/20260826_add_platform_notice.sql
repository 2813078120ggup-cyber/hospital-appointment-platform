-- 平台管理端公告运营中心：平台公告与停诊公告。
USE `yygh_cmn`;

CREATE TABLE IF NOT EXISTS `platform_notice` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
  `notice_type` tinyint NOT NULL COMMENT '公告类型（1：平台公告，2：停诊公告）',
  `title` varchar(200) NOT NULL COMMENT '公告标题',
  `summary` varchar(500) DEFAULT NULL COMMENT '公告摘要',
  `content` text NOT NULL COMMENT '公告内容',
  `hoscode` varchar(30) DEFAULT NULL COMMENT '医院编号',
  `hosname` varchar(100) DEFAULT NULL COMMENT '医院名称',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '状态（0：草稿，1：已发布）',
  `sort` int NOT NULL DEFAULT '0' COMMENT '排序值',
  `publish_time` datetime DEFAULT NULL COMMENT '发布时间',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除（0：正常，1：删除）',
  PRIMARY KEY (`id`),
  KEY `idx_notice_publish` (`notice_type`, `status`, `publish_time`),
  KEY `idx_notice_hoscode` (`hoscode`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='平台公告表';

INSERT INTO `platform_notice`
  (`notice_type`, `title`, `summary`, `content`, `status`, `sort`, `publish_time`)
SELECT 1, '预约挂号平台服务说明', '请按预约时间到院取号，并携带有效身份证件。',
       '请按预约时间到院取号，并携带有效身份证件。号源、出诊安排和退号截止时间以订单详情及医院最新通知为准。',
       1, 100, NOW()
WHERE NOT EXISTS (
  SELECT 1 FROM `platform_notice` WHERE `title` = '预约挂号平台服务说明' AND `is_deleted` = 0
);

INSERT INTO `platform_notice`
  (`notice_type`, `title`, `summary`, `content`, `status`, `sort`, `publish_time`)
SELECT 1, '就诊人实名信息填写提示', '就诊人信息须与证件及医院就诊档案保持一致。',
       '添加就诊人时，请确保姓名、证件号码和联系电话真实准确。信息不一致可能导致医院无法取号或退号。',
       1, 90, NOW()
WHERE NOT EXISTS (
  SELECT 1 FROM `platform_notice` WHERE `title` = '就诊人实名信息填写提示' AND `is_deleted` = 0
);

INSERT INTO `platform_notice`
  (`notice_type`, `title`, `summary`, `content`, `hoscode`, `hosname`, `status`, `sort`, `publish_time`)
SELECT 2, '北京协和医院门诊安排提醒', '部分科室出诊安排可能调整，请预约前查看最新排班。',
       '北京协和医院部分科室出诊安排可能临时调整。请在预约前查看最新排班，已预约用户请关注订单状态。',
       '10000', '北京协和医院', 1, 100, NOW()
WHERE NOT EXISTS (
  SELECT 1 FROM `platform_notice` WHERE `title` = '北京协和医院门诊安排提醒' AND `is_deleted` = 0
);
