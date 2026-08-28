-- 医院端医生登录：医生账号表。账号由 yygh_manage.schedule 中已存在的医生自动生成。
USE `yygh_manage`;

CREATE TABLE IF NOT EXISTS `doctor` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
  `username` varchar(30) NOT NULL COMMENT '登录账号',
  `password` varchar(64) NOT NULL COMMENT '登录密码(MD5)',
  `docname` varchar(20) NOT NULL COMMENT '医生姓名（对应 schedule.docname）',
  `title` varchar(20) DEFAULT NULL COMMENT '职称',
  `phone` varchar(20) DEFAULT NULL COMMENT '联系电话',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态（1：启用 0：停用）',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除（0：正常，1：删除）',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_doctor_username` (`username`),
  KEY `idx_doctor_docname` (`docname`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='医生账号表';

-- 从 schedule 表中已存在的医生自动生成登录账号（登录名=医生姓名，默认密码 123456）
INSERT INTO `doctor` (`username`, `password`, `docname`, `title`, `status`)
SELECT s.docname, MD5('123456'), s.docname, s.title, 1
FROM `schedule` s
INNER JOIN (
    SELECT MIN(id) AS min_id FROM `schedule`
    WHERE docname IS NOT NULL AND docname <> '' AND is_deleted = 0
    GROUP BY docname
) t ON s.id = t.min_id
WHERE NOT EXISTS (
    SELECT 1 FROM `doctor` d WHERE d.docname = s.docname AND d.is_deleted = 0
);
