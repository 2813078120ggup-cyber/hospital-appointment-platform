#!/usr/bin/env python3
"""Generate DDL-only database assets and static API/database references.

The source dumps can contain INSERT data. This generator intentionally exports
only CREATE TABLE statements so user, order, payment, and signing data never
become part of the repository.
"""

from __future__ import annotations

import argparse
import hashlib
import re
from dataclasses import dataclass
from pathlib import Path


SERVICE_PORTS = {
    "service-gateway": "8222",
    "service-hosp": "8201",
    "service-cmn": "8202",
    "service-msm": "8204",
    "service-oss": "8205",
    "service-orders": "8207",
    "service-task": "8208",
    "service-sta": "8260",
    "service-user": "8160",
    "hospital-manage": "9998",
}

DATABASE_OWNERS = {
    "yygh_cmn": "service-cmn",
    "yygh_hosp": "service-hosp",
    "yygh_manage": "hospital-manage",
    "yygh_order": "service-orders",
    "yygh_user": "service-user",
}


def read_text(path: Path) -> str:
    for encoding in ("utf-8-sig", "utf-8", "gb18030"):
        try:
            return path.read_text(encoding=encoding)
        except UnicodeDecodeError:
            continue
    raise UnicodeDecodeError("unknown", b"", 0, 1, f"Cannot decode {path}")


def md(value: str) -> str:
    return value.replace("|", "\\|").replace("\n", " ").strip()


def quoted_value(text: str) -> str:
    match = re.search(r'["\']([^"\']*)["\']', text)
    return match.group(1) if match else ""


def join_path(base: str, child: str) -> str:
    parts = [part.strip("/") for part in (base, child) if part.strip("/")]
    return "/" + "/".join(parts) if parts else "/"


@dataclass
class Column:
    name: str
    data_type: str
    nullable: str
    default: str
    extra: str
    comment: str


@dataclass
class Table:
    name: str
    comment: str
    columns: list[Column]
    indexes: list[str]
    ddl: str


def extract_tables(sql: str) -> list[Table]:
    tables: list[Table] = []
    pattern = re.compile(
        r"CREATE\s+TABLE\s+`(?P<name>[^`]+)`\s*\((?P<body>.*?)\)\s*"
        r"(?P<options>ENGINE=.*?);",
        re.IGNORECASE | re.DOTALL,
    )
    for match in pattern.finditer(sql):
        name = match.group("name")
        body = match.group("body")
        options = match.group("options")
        table_comment_match = re.search(r"COMMENT\s*=\s*'((?:\\'|[^'])*)'", options, re.I)
        table_comment = table_comment_match.group(1) if table_comment_match else ""
        columns: list[Column] = []
        indexes: list[str] = []
        for raw_line in body.splitlines():
            line = raw_line.strip().rstrip(",")
            if not line:
                continue
            column_match = re.match(r"`([^`]+)`\s+(.+)$", line, re.DOTALL)
            if not column_match:
                indexes.append(line)
                continue
            column_name, definition = column_match.groups()
            type_match = re.match(r"([A-Za-z]+(?:\([^)]*\))?(?:\s+unsigned)?)\s*(.*)", definition, re.I)
            data_type = type_match.group(1) if type_match else definition
            remainder = type_match.group(2) if type_match else ""
            comment_match = re.search(r"\sCOMMENT\s+'((?:\\'|[^'])*)'", remainder, re.I)
            comment = comment_match.group(1) if comment_match else ""
            default_match = re.search(
                r"\sDEFAULT\s+('(?:\\'|[^'])*'|NULL|CURRENT_TIMESTAMP(?:\(\))?|[-+]?\d+(?:\.\d+)?)",
                remainder,
                re.I,
            )
            default = default_match.group(1) if default_match else "—"
            nullable = "否" if re.search(r"\bNOT\s+NULL\b", remainder, re.I) else "是"
            extra_parts: list[str] = []
            if re.search(r"\bAUTO_INCREMENT\b", remainder, re.I):
                extra_parts.append("AUTO_INCREMENT")
            if re.search(r"\bON\s+UPDATE\s+CURRENT_TIMESTAMP\b", remainder, re.I):
                extra_parts.append("ON UPDATE CURRENT_TIMESTAMP")
            columns.append(
                Column(
                    name=column_name,
                    data_type=data_type,
                    nullable=nullable,
                    default=default,
                    extra=", ".join(extra_parts) or "—",
                    comment=comment or "—",
                )
            )
        ddl = match.group(0)
        tables.append(Table(name, table_comment, columns, indexes, ddl))
    return tables


def generate_database_assets(repo: Path, sql_source: Path) -> tuple[int, int]:
    output_dir = repo / "database" / "schema"
    output_dir.mkdir(parents=True, exist_ok=True)
    docs: list[str] = [
        "# 数据库字典",
        "",
        "> 本文档由 `tools/generate_reference_docs.py` 从初始化 SQL 静态生成。仓库中的 SQL 仅保留 DDL；源导出中的 INSERT 数据未复制，以避免提交用户、订单、支付或签名数据。",
        "",
        "## 数据库概览",
        "",
        "| 数据库 | 负责模块 | 表数量 | 源文件校验值（SHA-256 前 12 位） |",
        "| --- | --- | ---: | --- |",
    ]
    parsed: list[tuple[str, str, str, list[Table]]] = []
    total_tables = 0
    total_columns = 0
    for sql_path in sorted(sql_source.glob("*.sql")):
        database = sql_path.stem
        sql = read_text(sql_path)
        tables = extract_tables(sql)
        digest = hashlib.sha256(sql_path.read_bytes()).hexdigest()[:12]
        owner = DATABASE_OWNERS.get(database, "待确认")
        parsed.append((database, owner, digest, tables))
        total_tables += len(tables)
        total_columns += sum(len(table.columns) for table in tables)
        docs.append(f"| `{database}` | `{owner}` | {len(tables)} | `{digest}` |")

        ddl_lines = [
            f"-- 医院预约挂号平台：{database} 纯结构初始化脚本",
            "-- 由 tools/generate_reference_docs.py 生成；不包含源文件中的 INSERT 数据。",
            f"CREATE DATABASE IF NOT EXISTS `{database}` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;",
            f"USE `{database}`;",
            "",
        ]
        for table in tables:
            ddl_lines.extend([f"DROP TABLE IF EXISTS `{table.name}`;", table.ddl, ""])
        (output_dir / sql_path.name).write_text("\n".join(ddl_lines).rstrip() + "\n", encoding="utf-8")

    docs.extend(["", f"共 {len(parsed)} 个数据库、{total_tables} 张关系表、{total_columns} 个字段。", ""])
    for database, owner, _digest, tables in parsed:
        docs.extend([f"## `{database}`", "", f"负责模块：`{owner}`。", ""])
        for table in tables:
            docs.extend(
                [
                    f"### `{table.name}` — {table.comment or '未提供表注释'}",
                    "",
                    "| 字段 | 类型 | 可空 | 默认值 | 扩展 | 说明 |",
                    "| --- | --- | :---: | --- | --- | --- |",
                ]
            )
            for column in table.columns:
                docs.append(
                    f"| `{md(column.name)}` | `{md(column.data_type)}` | {column.nullable} | "
                    f"`{md(column.default)}` | {md(column.extra)} | {md(column.comment)} |"
                )
            docs.extend(["", "索引与约束：", ""])
            if table.indexes:
                docs.extend(f"- `{md(index)}`" for index in table.indexes)
            else:
                docs.append("- 源 DDL 未声明索引或约束。")
            docs.append("")

    (repo / "docs" / "database-dictionary.md").write_text(
        "\n".join(docs).rstrip() + "\n", encoding="utf-8"
    )
    return total_tables, total_columns


def mapping_path(args: str) -> str:
    value_match = re.search(r'''(?:value|path)\s*=\s*["']([^"']*)["']''', args)
    if value_match:
        return value_match.group(1)
    return quoted_value(args)


def controller_title(prefix: str, class_name: str) -> str:
    candidates = []
    candidates.extend(re.findall(r'''@Schema\s*\(.*?description\s*=\s*["']([^"']+)''', prefix, re.S))
    candidates.extend(re.findall(r'''@Api\s*\(.*?tags\s*=\s*["']([^"']+)''', prefix, re.S))
    return candidates[-1] if candidates else class_name


def endpoint_description(context: str, method_name: str) -> str:
    matches = re.findall(
        r"@(?:Operation|ApiOperation)\s*\((.*?)\)", context, re.DOTALL
    )
    if matches:
        args = matches[-1]
        named = re.search(r'''(?:summary|description|value)\s*=\s*["']([^"']+)''', args)
        if named:
            return named.group(1)
        direct = quoted_value(args)
        if direct:
            return direct
    return method_name


def summarize_parameters(parameters: str) -> str:
    compact = re.sub(r"\s+", " ", parameters).strip()
    if not compact:
        return "—"
    compact = re.sub(r"@(PathVariable|RequestParam)\s*\([^)]*\)", r"@\1", compact)
    compact = re.sub(r"@(RequestBody|PathVariable|RequestParam|RequestHeader|Valid)\b", r"\1 ", compact)
    compact = compact.replace("final ", "")
    parts = [part.strip() for part in compact.split(",")]
    ignored = ("HttpServletRequest ", "HttpServletResponse ", "ModelMap ", "RedirectAttributes ", "BindingResult ")
    useful = [part for part in parts if part and not part.startswith(ignored)]
    return md("; ".join(useful) if useful else "—")


def detect_service(path: Path) -> str:
    normalized = str(path).replace("/", "\\")
    if "hospital-manage" in normalized:
        return "hospital-manage"
    match = re.search(r"\\service\\(service_[^\\]+)\\", normalized)
    if not match:
        return "service-gateway"
    return match.group(1).replace("_", "-").replace("service-orders", "service-orders").replace("service-statistics", "service-sta")


def extract_controller(repo: Path, path: Path) -> dict | None:
    java = read_text(path)
    class_match = re.search(r"\bclass\s+(\w+Controller)\b", java)
    if not class_match:
        return None
    class_name = class_match.group(1)
    prefix = java[: class_match.start()]
    class_mappings = list(re.finditer(r"@RequestMapping\s*(?:\((.*?)\))?", prefix, re.DOTALL))
    base = mapping_path(class_mappings[-1].group(1) or "") if class_mappings else ""
    title = controller_title(prefix, class_name)
    service = detect_service(path)
    endpoints: list[dict] = []
    mapping_re = re.compile(
        r"@(GetMapping|PostMapping|PutMapping|DeleteMapping|PatchMapping|RequestMapping)\s*(?:\((.*?)\))?",
        re.DOTALL,
    )
    matches = list(mapping_re.finditer(java, class_match.end()))
    for index, mapping in enumerate(matches):
        boundary = matches[index + 1].start() if index + 1 < len(matches) else len(java)
        segment = java[mapping.end() : boundary]
        method_match = re.search(
            r"\bpublic\s+(?:static\s+)?(?P<return>[\w<>?,.\[\]]+)\s+"
            r"(?P<name>\w+)\s*\((?P<params>.*?)\)\s*(?:throws\s+[^\{]+)?\{",
            segment,
            re.DOTALL,
        )
        if not method_match:
            continue
        annotation = mapping.group(1)
        args = mapping.group(2) or ""
        if annotation == "RequestMapping":
            verb_match = re.search(r"RequestMethod\.(GET|POST|PUT|DELETE|PATCH)", args)
            verb = verb_match.group(1) if verb_match else "ANY"
        else:
            verb = annotation.replace("Mapping", "").upper()
        child = mapping_path(args)
        endpoint_path = join_path(base, child)
        context = java[max(class_match.end(), mapping.start() - 500) : mapping.start()]
        method_name = method_match.group("name")
        access = "登录用户" if "/auth/" in endpoint_path else "内部调用" if "/inner/" in endpoint_path else "公开/按业务校验"
        if endpoint_path.startswith("/admin/"):
            access = "管理端"
        endpoints.append(
            {
                "verb": verb,
                "path": endpoint_path,
                "description": endpoint_description(context, method_name),
                "parameters": summarize_parameters(method_match.group("params")),
                "return": method_match.group("return"),
                "access": access,
            }
        )
    return {
        "service": service,
        "port": SERVICE_PORTS.get(service, "—"),
        "class": class_name,
        "title": title,
        "file": path.relative_to(repo).as_posix(),
        "endpoints": endpoints,
    }


def generate_api_reference(repo: Path) -> tuple[int, int]:
    controller_paths = sorted(
        path
        for path in repo.rglob("*Controller.java")
        if "target" not in path.parts and "node_modules" not in path.parts
    )
    controllers = [item for path in controller_paths if (item := extract_controller(repo, path))]
    endpoint_count = sum(len(item["endpoints"]) for item in controllers)
    docs: list[str] = [
        "# 接口文档",
        "",
        "> 本文档由 `tools/generate_reference_docs.py` 对 Controller 注解进行静态提取。它描述代码中已声明的路由，不替代联调、权限测试或运行时 OpenAPI 文档。",
        "",
        "## 调用约定",
        "",
        "- 统一入口：`http://localhost:8222`（Spring Cloud Gateway）。",
        "- 业务服务通常返回 `R`，成功码为 `20000`；医院模拟端的页面控制器返回视图或重定向。",
        "- 路径包含 `/auth/` 的接口通过请求头 `token: <JWT>` 识别登录用户。",
        "- 路径包含 `/inner/` 的接口供 Feign 等内部调用，生产环境必须限制外部访问。",
        "- `ANY` 表示源码使用了未限定 method 的 `@RequestMapping`，实际可接受多个 HTTP 方法。",
        "",
        "## 运行时文档入口",
        "",
        "| 模块 | 端口 | 文档入口 |",
        "| --- | ---: | --- |",
        "| Spring Cloud 业务服务 | 各服务端口 | `http://localhost:<port>/doc.html`（Knife4j/OpenAPI，需服务启动） |",
        "| 医院模拟端 | 9998 | `http://localhost:9998/swagger-ui.html`（Springfox Swagger 2） |",
        "",
        f"静态扫描共发现 {len(controllers)} 个 Controller、{endpoint_count} 个映射方法。",
        "",
    ]
    for controller in sorted(controllers, key=lambda item: (item["service"], item["class"])):
        docs.extend(
            [
                f"## `{controller['service']}` / {controller['title']}",
                "",
                f"Controller：`{controller['class']}`  ·  端口：`{controller['port']}`  ·  源码：`{controller['file']}`",
                "",
                "| 方法 | 路径 | 用途 | 访问范围 | 参数摘要 | 返回类型 |",
                "| --- | --- | --- | --- | --- | --- |",
            ]
        )
        if controller["endpoints"]:
            for endpoint in controller["endpoints"]:
                docs.append(
                    f"| `{endpoint['verb']}` | `{md(endpoint['path'])}` | {md(endpoint['description'])} | "
                    f"{endpoint['access']} | {endpoint['parameters']} | `{endpoint['return']}` |"
                )
        else:
            docs.append("| — | — | 未发现可静态提取的映射方法 | — | — | — |")
        docs.append("")
    (repo / "docs" / "api-reference.md").write_text("\n".join(docs).rstrip() + "\n", encoding="utf-8")
    return len(controllers), endpoint_count


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--repo", type=Path, default=Path(__file__).resolve().parents[1])
    parser.add_argument("--sql-source", type=Path, required=True)
    args = parser.parse_args()
    repo = args.repo.resolve()
    sql_source = args.sql_source.resolve()
    if not sql_source.is_dir():
        raise SystemExit(f"SQL source directory does not exist: {sql_source}")
    (repo / "docs").mkdir(parents=True, exist_ok=True)
    tables, columns = generate_database_assets(repo, sql_source)
    controllers, endpoints = generate_api_reference(repo)
    print(
        f"Generated database dictionary ({tables} tables/{columns} columns) "
        f"and API reference ({controllers} controllers/{endpoints} endpoints)."
    )


if __name__ == "__main__":
    main()
