# 验证与排障

## 提交前检查

```powershell
# 后端编译
mvn -f yygh_parent\yygh_parent\pom.xml -DskipTests package
mvn -f hospital-manage\hospital-manage\pom.xml -DskipTests package

# 管理端
Set-Location yygh-admin\yygh-admin
npm run lint
npm run test:unit
npm run build:prod

# 用户门户
Set-Location ..\..\yygh-sitedemo
npm run lint
npm run build

# 文档生成器
Set-Location ..
python -m py_compile tools\generate_reference_docs.py
python tools\generate_reference_docs.py --sql-source "D:\XZYL\代码\01-数据库脚本"

# Git 文本检查
git diff --check
```

旧版前端依赖可能在较新的 Node.js 上出现 OpenSSL、node-sass 或 eslint 兼容问题。优先使用 Node 16；不要通过提交 `node_modules` 规避环境问题。

## 常见问题

### 服务无法注册到 Nacos

确认 `YYGH_NACOS_SERVER_ADDR`、Nacos 端口和网络可达性；再检查各服务控制台是否出现相同 `spring.application.name` 的实例。

### 网关返回 503

503 通常表示目标服务未注册或不健康。根据请求前缀确认对应服务已启动，并检查 Nacos 中实例端口是否与 [架构文档](architecture.md) 一致。

### 数据库认证失败

确认当前 PowerShell 会话已设置 `YYGH_DB_PASSWORD`，并验证各 JDBC URL 对应的数据库已由 `database/schema/` 初始化。

### 登录时提示 JWT 密钥缺失

设置至少 32 字节的 `YYGH_JWT_SECRET` 后重启 `service-user` 以及所有解析 JWT 的服务。更换密钥会使旧 token 失效，这是预期行为。

### 字典、医院或排班页面为空

仓库脚本只初始化表结构。字典数据和 MongoDB 医院/科室/排班数据需要从无敏感信息的开发数据集导入。

### 前端请求失败

确认管理端 `VUE_APP_BASE_API`、用户门户 `NUXT_ENV_API_BASE_URL` 都指向 8222 网关；再检查浏览器 Network 中的实际路径是否匹配 [接口文档](api-reference.md)。

### 第三方能力失败

OSS、微信登录和微信支付默认凭据为空。设置对应环境变量并重启目标服务；不要把调试凭据写入 `.properties`、`.yml`、`.env` 或提交历史。
