# 开发指南

## 1. 环境要求

| 工具/依赖 | 建议版本 | 备注 |
| --- | --- | --- |
| JDK | 17 或 21 | 微服务父工程以 Java 17 为目标；医院模拟端源级别为 Java 8，并锁定兼容新版 JDK 的 Lombok |
| Maven | 3.9.x | 从 Maven Central 解析依赖 |
| Node.js / npm | Node 16、npm 8 | 与 Vue 2 / Nuxt 2 依赖年代更匹配 |
| MySQL | 8.0 | 需要 `yygh_cmn`、`yygh_hosp`、`yygh_manage`、`yygh_order`、`yygh_user` |
| MongoDB | 4.4+ | 医院、科室和排班文档 |
| Redis | 6+ | 验证码与缓存 |
| RabbitMQ | 3.9+ | 预约、短信和定时任务消息 |
| Nacos | 2.x | 服务注册与发现 |
| Sentinel Dashboard | 可选 | 订单热点参数限流观察 |

## 2. 初始化数据库

脚本只包含表结构，不包含源导出中的 INSERT 数据。在 PowerShell 中逐个导入：

```powershell
Get-Content database\schema\yygh_cmn.sql -Raw | mysql -u root -p
Get-Content database\schema\yygh_hosp.sql -Raw | mysql -u root -p
Get-Content database\schema\yygh_manage.sql -Raw | mysql -u root -p
Get-Content database\schema\yygh_order.sql -Raw | mysql -u root -p
Get-Content database\schema\yygh_user.sql -Raw | mysql -u root -p
Get-Content java-ai-langchain4j\database\create.sql -Raw | mysql -u root -p
```

基础建表完成后执行条件迁移；存量数据库不要重新执行含 `DROP TABLE` 的初始化脚本，只需先备份再执行同一迁移：

```powershell
Get-Content database\migrations\20260825_add_ai_formal_order_support.sql -Raw | mysql -u root -p
```

该脚本会先检查字段和索引是否存在，因此可重复执行。它为医院模拟端订单增加跨系统幂等号，并为 AI 预约记录增加平台就诊人、排班和正式订单关联字段。

完整功能需要字典等基础数据。请从可信来源筛选非敏感 INSERT，禁止导入或提交真实用户、证件、手机号、订单和支付数据。字段说明见 [数据库字典](database-dictionary.md)。

## 3. 配置运行环境

至少设置数据库密码和 JWT 密钥：

```powershell
$env:YYGH_DB_PASSWORD = '<本地 MySQL 密码>'
$env:YYGH_JWT_SECRET = '<至少 32 个 UTF-8 字节的随机字符串>'
$env:YYGH_ADMIN_PASSWORD = '<管理端密码>'
$env:YYGH_HOSPITAL_BOOTSTRAP_TOKEN = '<平台与医院模拟端共享的随机引导令牌>'
```

基础设施默认连接 `localhost`。如果服务运行在其他主机，继续按 [配置说明](configuration.md) 覆盖 Nacos、Redis、MongoDB、RabbitMQ 和各数据库 URL。第三方能力不用时可保持相应凭据为空，但调用该能力会明确失败。

## 4. 构建后端

```powershell
Set-Location yygh_parent\yygh_parent
mvn -DskipTests package

Set-Location ..\..\hospital-manage\hospital-manage
mvn -DskipTests package
```

## 5. 启动顺序

1. 启动 MySQL、MongoDB、Redis、RabbitMQ、Nacos；按需启动 Sentinel Dashboard。
2. 启动业务服务：`service-cmn`、`service-hosp`、`service-user`、`service-msm`、`service-oss`、`service-orders`、`service-task`、`service-statistics`。
3. 启动 `service-gateway`，确认端口 8222 可用。
4. 按需启动医院模拟端。
5. 按需启动 AI 服务；正式预约依赖网关、用户、医院、订单、Redis、RabbitMQ 和医院模拟端均可用。
6. 启动管理端和用户门户。

各 Spring Boot 服务可在 IDE 中运行对应 `*Application.java`，也可进入模块后使用 `mvn spring-boot:run`。

## 6. 启动前端

```powershell
Set-Location yygh-admin\yygh-admin
npm install
npm run dev

Set-Location ..\..\yygh-sitedemo
npm install
$env:NUXT_ENV_API_BASE_URL = 'http://localhost:8222'
npm run dev
```

管理端默认地址为 `http://localhost:9528`，用户门户默认地址为 `http://localhost:3000`。

## 7. 重新生成文档

当 Controller 或初始化 SQL 发生变化时执行：

```powershell
python tools\generate_reference_docs.py --sql-source "D:\XZYL\代码\01-数据库脚本"
```

生成后检查 `docs/api-reference.md`、`docs/database-dictionary.md` 和 `database/schema/` 的差异，再随代码一起提交。
