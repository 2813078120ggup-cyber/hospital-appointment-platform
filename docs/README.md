# 项目文档导航

| 文档 | 适用场景 |
| --- | --- |
| [系统架构](architecture.md) | 理解系统边界、服务职责、数据存储和调用关系 |
| [开发指南](development-guide.md) | 首次搭建环境、初始化数据库、启动服务与前端 |
| [配置说明](configuration.md) | 设置环境变量、端口、依赖地址和第三方凭据 |
| [接口文档](api-reference.md) | 查询 Controller 路由、参数摘要、返回类型和访问范围 |
| [数据库字典](database-dictionary.md) | 查询 5 个数据库、11 张表、字段、索引与约束 |
| [验证与排障](testing-and-troubleshooting.md) | 执行构建检查并定位常见启动和联调问题 |
| [数据库脚本说明](../database/README.md) | 了解 DDL 来源、导入顺序和数据安全边界 |
| [贡献规范](../CONTRIBUTING.md) | 分支、提交、代码检查和敏感信息要求 |

`api-reference.md`、`database-dictionary.md` 和 `database/schema/*.sql` 可通过以下命令重新生成：

```powershell
python tools\generate_reference_docs.py --sql-source "D:\XZYL\代码\01-数据库脚本"
```
