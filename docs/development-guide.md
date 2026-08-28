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
Get-Content database\migrations\20260826_add_doctor.sql -Raw | mysql -u root -p
Get-Content database\migrations\20260826_add_feedback.sql -Raw | mysql -u root -p
Get-Content database\migrations\20260827_add_hospital_patient.sql -Raw | mysql -u root -p
```

迁移脚本均可重复执行。它们为医院模拟端订单增加跨系统幂等号，为 AI 预约记录增加平台就诊人、排班和正式订单关联字段，为医院端医生建立登录账号，为反馈建立数据表，并通过 `20260827_add_hospital_patient.sql` 建立医院侧独立的 `patient` 患者档案表。医院侧患者主键与平台用户服务的患者主键相互独立；医院订单提交时会按平台用户和证件信息复用档案，不再使用固定患者编号。

完整功能需要字典等基础数据。请从可信来源筛选非敏感 INSERT，禁止导入或提交真实用户、证件、手机号、订单和支付数据。字段说明见 [数据库字典](database-dictionary.md)。

## 3. 配置运行环境

至少设置数据库密码和 JWT 密钥：

```powershell
$env:YYGH_DB_PASSWORD = '<本地 MySQL 密码>'
$env:YYGH_JWT_SECRET = '<至少 32 个 UTF-8 字节的随机字符串>'
$env:YYGH_ADMIN_PASSWORD = '123456' # 仅限本地演示，生产环境必须改为强密码
$env:YYGH_HOSPITAL_BOOTSTRAP_TOKEN = '<平台与医院模拟端共享的随机引导令牌>'
$env:YYGH_NACOS_DISCOVERY_IP = '192.168.6.1'
```

后端基础设施默认连接 `192.168.6.101`。如果服务运行在其他主机，继续按 [配置说明](configuration.md) 覆盖 Nacos、Redis、MongoDB、RabbitMQ 和各数据库 URL。第三方能力不用时可保持相应凭据为空，但调用该能力会明确失败。

Windows 存在 VMware、WSL、代理等多块网卡时，所有微服务和网关通过 `YYGH_NACOS_DISCOVERY_IP` 显式指定注册 IP。本项目当前 VMnet8 地址为 `192.168.6.1`；不要让 Nacos 自动选择 `192.168.101.1`，否则虚拟机侧的服务消费者无法回调该实例。更换机器或网段时必须覆盖此变量。

## 4. 构建后端

```powershell
Set-Location yygh_parent\yygh_parent
mvn -DskipTests package

Set-Location ..\..\hospital-manage\hospital-manage
mvn -DskipTests package

Set-Location ..\..\java-ai-langchain4j
mvn clean test
mvn -DskipTests package
```

AI 模块的 Maven 编译必须保留 Java 参数名，LangChain4j 才能把模型生成的工具 JSON 绑定到挂号方法；不要移除 `maven-compiler-plugin` 的 `parameters` 配置。生成的 AI 与网关 JAR 均可直接通过 `java -jar` 启动。

AI 默认测试只运行无外部副作用的单元测试。原有模型、图片、MongoDB 和 MySQL 演示测试会访问真实服务或修改数据，需在准备好隔离数据和相应凭据后显式启用：

```powershell
$env:XIAOZHI_RUN_INTEGRATION_TESTS = 'true'
mvn test
```

## 5. 启动顺序

1. 启动 MySQL、MongoDB、Redis、RabbitMQ、Nacos；按需启动 Sentinel Dashboard。
2. 启动业务服务：`service-cmn`、`service-hosp`、`service-user`、`service-msm`、`service-oss`、`service-orders`、`service-task`、`service-statistics`。
3. 启动 `service-gateway`，确认端口 8222 可用。
4. 按需启动医院模拟端。
5. 按需启动 AI 服务；正式预约依赖网关、用户、医院、订单、Redis、RabbitMQ 和医院模拟端均可用。
6. 启动管理端和用户门户。

各 Spring Boot 服务可在 IDE 中运行对应 `*Application.java`，也可进入模块后使用 `mvn spring-boot:run`。

本地 AI 默认连接 Ollama。首次运行需准备模型：

```powershell
ollama pull qwen3:0.6b
ollama serve
```

使用远程 OpenAI 兼容服务时，应通过 `LANGCHAIN4J_OPENAI_BASE_URL`、`LANGCHAIN4J_OPENAI_API_KEY` 和 `LANGCHAIN4J_OPENAI_MODEL` 覆盖默认值，禁止把密钥提交到配置文件。

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
