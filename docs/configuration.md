# 配置说明

Spring 配置为本地基础设施保留了开发默认地址，凭据和第三方密钥通过环境变量注入。生产环境不得依赖默认地址或把密钥写回源码。

## 核心环境变量

| 变量 | 必需性 | 用途 / 默认值 |
| --- | --- | --- |
| `YYGH_DB_USERNAME` | 必需 | MySQL 用户名，默认 `root` |
| `YYGH_DB_PASSWORD` | 必需 | MySQL 密码，默认空 |
| `YYGH_CMN_DATASOURCE_URL` | 按环境 | `yygh_cmn` JDBC URL |
| `YYGH_HOSP_DATASOURCE_URL` | 按环境 | `yygh_hosp` JDBC URL |
| `YYGH_MANAGE_DATASOURCE_URL` | 按环境 | `yygh_manage` JDBC URL |
| `YYGH_MANAGE_TEST_DATASOURCE_URL` | 测试环境 | 医院模拟端 test profile JDBC URL，默认 localhost |
| `YYGH_ORDER_DATASOURCE_URL` | 按环境 | `yygh_order` JDBC URL |
| `YYGH_USER_DATASOURCE_URL` | 按环境 | `yygh_user` JDBC URL |
| `YYGH_NACOS_SERVER_ADDR` | 必需 | Nacos 地址，默认 `localhost:8848` |
| `YYGH_MONGODB_URI` | 必需 | MongoDB URI，默认 `mongodb://localhost:27017/test` |
| `YYGH_REDIS_HOST` / `YYGH_REDIS_PORT` | 必需 | Redis 地址，默认 `localhost:6379` |
| `YYGH_RABBITMQ_HOST` / `YYGH_RABBITMQ_PORT` | 必需 | RabbitMQ 地址，默认 `localhost:5672` |
| `YYGH_RABBITMQ_USERNAME` / `YYGH_RABBITMQ_PASSWORD` | 必需 | RabbitMQ 凭据，开发默认 `guest/guest` |
| `YYGH_SENTINEL_DASHBOARD` | 可选 | Sentinel Dashboard，默认 `localhost:8058` |
| `YYGH_JWT_SECRET` | 必需 | JWT HMAC 密钥，至少 32 个 UTF-8 字节；未设置时网关拒绝启动 |
| `YYGH_ADMIN_USERNAME` / `YYGH_ADMIN_PASSWORD` | 必需 | 管理端账号；用户名默认 `admin`，密码无默认值 |
| `YYGH_HOSPITAL_BOOTSTRAP_TOKEN` | 必需 | 平台首次向医院模拟端同步签名密钥的独立引导令牌 |
| `YYGH_HOSPITAL_MANAGE_URL` | 本地联调 | 医院模拟端地址，默认 `http://localhost:9998` |
| `YYGH_PLATFORM_API_URL` | 本地联调 | 医院模拟端调用平台的地址，默认 `http://localhost:8201` |
| `YYGH_SERVICE_HOSP_URL` | AI 查号 | AI 调用公开查号接口，默认 `http://localhost:8201` |
| `YYGH_GATEWAY_URL` | AI 正式预约 | AI 转发用户登录令牌并创建正式订单，默认 `http://localhost:8222` |
| `XIAOZHI_DATASOURCE_URL` / `XIAOZHI_DB_USERNAME` / `XIAOZHI_DB_PASSWORD` | AI 服务 | AI 预约关联库，默认连接本机 `guiguxiaozhi` |
| `XIAOZHI_MONGODB_URI` | AI 服务 | AI 对话记忆 MongoDB URI，默认 `mongodb://localhost:27017/chat_memory_db` |

## 第三方能力

| 变量 | 使用模块 | 说明 |
| --- | --- | --- |
| `ALIYUN_OSS_ENDPOINT` | `service-oss` | OSS 地域 endpoint |
| `ALIYUN_OSS_KEY_ID` / `ALIYUN_OSS_KEY_SECRET` | `service-oss` | OSS 访问凭据 |
| `ALIYUN_OSS_BUCKET_NAME` | `service-oss` | 上传目标 Bucket |
| `WECHAT_OPEN_APP_ID` / `WECHAT_OPEN_APP_SECRET` | `service-user` | 微信开放平台扫码登录 |
| `WECHAT_OPEN_REDIRECT_URL` | `service-user` | OAuth 回调，默认本地 8160 地址 |
| `WECHAT_PAY_APP_ID` | `service-orders` | 微信支付应用 ID |
| `WECHAT_PAY_PARTNER_ID` / `WECHAT_PAY_PARTNER_KEY` | `service-orders` | 商户号与商户密钥 |
| `WECHAT_PAY_CERT_PATH` | `service-orders` | 本机证书绝对路径；证书文件不可提交 |
| `WECHAT_PAY_NOTIFY_URL` | `service-orders` | 微信支付回调地址；部署时必须是微信可访问的 HTTPS 地址 |
| `ALIYUN_SMS_APPCODE` | `service-msm` | 阿里云市场短信凭据，无默认值 |
| `ALIYUN_SMS_HOST` / `ALIYUN_SMS_PATH` / `ALIYUN_SMS_TEMPLATE_ID` | `service-msm` | 短信供应商地址、路径和模板 |

## 前端配置

- 管理端通过 `yygh-admin/yygh-admin/.env.development` 的 `VUE_APP_BASE_API` 指向网关。
- 用户门户使用 `NUXT_ENV_API_BASE_URL`，默认 `http://localhost:8222`。
- 部署时应把浏览器请求地址改为 HTTPS 公网网关，并同步配置 CORS、OAuth 回调和反向代理。
- 调用 AI `/xiaozhi/chat` 时，登录用户应使用 `token` 或 `X-Token` 请求头传递现有 JWT。令牌只在当前请求线程中转发给网关，不写入提示词、聊天记忆或预约表。

## 凭据轮换

初始化审计发现历史源码中曾出现 OSS、微信开放平台、微信支付和 JWT 固定密钥。即使仓库设为私有，也应在对应控制台撤销/轮换旧值，只向部署环境注入新值。
