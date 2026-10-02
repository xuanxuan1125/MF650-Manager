# 8081 后端制品获取结果

后续离线附件阶段见 [VENDOR_ARTIFACTS_ANALYSIS.md](VENDOR_ARTIFACTS_ANALYSIS.md)：已取得另一套后台的参考 binary 和脚本，仍未匹配当前 v5.2.4 `/api/device/info`。下文保留为此前更新渠道审计结果，不将参考包等同于当前后端。

2026-10-02。**CASE C / METADATA_ONLY_BACKEND_ARTIFACT_MISSING。** 自身更新渠道返回当前版本标签 v5.2.4 和一条功能日志，但没有后端文件、更新包 URL 或服务器。未取得可匹配当前版本或新版的后端制品，RAM 修复目前不可实施。

## 必须区分的证据

| 项目 | 状态 | 证据或限制 |
| --- | --- | --- |
| 当前后台版本标签 | v5.2.4 | 本次 check-update 的 local_version，与既有 system/info 标签相符；不能代替在线可执行文件 hash |
| check-update | SAFE_GET / PASS | 前端只读门槛通过；GET 一次，HTTP 200 |
| update-log | SAFE_GET / PASS | 前端读取日志；GET 一次，HTTP 200 |
| 更新服务器 | NO：未发现 | 两个响应和更新源码无相关域名，实际存在与否 UNKNOWN |
| 更新包 URL | NO：未发现 | 无前端拼接地址或二次下载 API，未猜路径 |
| 后端源码仓库 | NO：未发现 | 有界公开检索未取得可确认仓库；本项目客户端仓库不算后端源码 |
| 取得更新包 | NO | downloads 原件/工作副本/manifest 目录为空 |
| 包版本 | N/A | 无包 |
| 是否匹配当前 v5.2.4 | UNKNOWN | 无包及运行文件 hash，不能确认构建身份 |
| 找到当前 8081 后端制品 | NO：未取得 | 不表示设备没有后台，只表示没有取得其文件 |
| 后端文件 / 安装路径 / 语言 | UNKNOWN | 无程序或安装脚本 |
| 监听 8081 的 PID / 程序路径 | UNKNOWN | HTTP 200 证明该端口可响应，不能识别监听进程；未取得 ss/netstat/ps 输出 |
| API 路由文件 / device/info handler | NOT FOUND | 未取得后端代码，未能定位 handler |
| memory_usage 来源 / 当前公式 | UNKNOWN | 不以旧 FOTA RAM 路径或当前百分比替代当前 handler 证据 |
| 读取/使用 MemAvailable | UNKNOWN | API 不输出该字段不证明后端不读取它 |
| 原始 RAM 字段可扩展性 | UNKNOWN | 未见当前序列化模块/字段合同 |
| service/unit、启动参数、安装及回滚 | UNKNOWN | 未取得当前制品/安装脚本或进程导出 |
| RAM 修复可实施 | NO：当前证据不足 | 无可验证的修改位置与恢复入口 |

本阶段只获得 HTML/元数据；它们不是后端制品。论坛附件列表与早期功能介绍不能建立与当前 v5.2.4 的对应关系，也未取得可比较的新版，所以**不标记 CURRENT_BACKEND_CONFIRMED 或 REFERENCE_NEWER_VERSION**。

## 制品与后续分析状态

已建本地目录 `analysis/update-channel/downloads/original/`、`working-copy/`、`manifest/`、`analysis/update-channel/extracted/`，无下载文件、无解包文件。没有创建 UPDATE_PACKAGE_MANIFEST.csv，以免把 HTTP 元数据误报为包清单。

未执行候选后台 file/strings/readelf/objdump、全包路由/RAM 搜索或安装脚本分析，因为没有新增制品；未重复搜索旧 FOTA。既有旧用户态 RAM 证据继续保留在 [MEMORY_USAGE_FORMULA.md](MEMORY_USAGE_FORMULA.md)，与当前 8081 的关系仍未证实。

没有创建 `8081_MEMORY_HANDLER_REVERSE.md`、source diff 或 binary patch；没有修改 [RAM_FIX_PLAN.md](RAM_FIX_PLAN.md)。取得可行当前后端改法的前提尚未满足。

脱敏清单：[UPDATE_CHANNEL_RESULT.json](../analysis/update-channel/reports/UPDATE_CHANNEL_RESULT.json)、[HTTP_GET_MANIFEST.csv](../analysis/update-channel/reports/HTTP_GET_MANIFEST.csv)、[UPDATE_SOURCE_MAP.json](../analysis/update-channel/reports/UPDATE_SOURCE_MAP.json)、[UPDATE_FIELD_PRESENCE.csv](../analysis/update-channel/reports/UPDATE_FIELD_PRESENCE.csv)、[PUBLIC_SOURCE_SEARCH.json](../analysis/update-channel/reports/PUBLIC_SOURCE_SEARCH.json)。原始响应及页面留在本地忽略目录；没有提交包、闭源二进制或固件。

## 下一步的最小方案与回滚前提

**目前没有可部署 RAM 补丁。** 下一步只需补齐同版本后台制品及它与当前进程的身份对应证据，来源可为维护方直接提供的 v5.2.4 安装包/公开源码，或以后获得受支持只读 shell 时导出的监听 PID、实际程序路径、文件 SHA256、启动配置及完整 meminfo。不重发本阶段查询、不猜维护目录、不重试 ADB/ttyd、不安装备用 ADB Patch。

取得材料后按 router → device_info handler → system_status → memory_usage → RAM 数据源完成证据链；先判断当前是否已用 MemAvailable，再决定修改，避免重复或改错数据源。新版制品只能作 REFERENCE_NEWER_VERSION，不能直接证明当前实现。

若当前实现确为 Total-Free 且有源代码，最小兼容改法是只调整该 handler 的取数/计算：同次读取 `/proc/meminfo`，使用 `memory_usage = (MemTotal - MemAvailable) / MemTotal * 100`，保持 JSON 名 `system_status.memory_usage` 与数值类型/既有精度合同。可同时增加带 kB 单位的 `memory_total_kb`、`memory_free_kb`、`memory_available_kb`、`swap_total_kb`、`swap_free_kb`；字段/异常行为须由实际合同验证，不从 percentage 反推数据。MemTotal 非正、MemAvailable 缺失或越界时须明确报数据不可用，不夹取为健康数值。

这一方案为**条件设计**，不是当前程序已有行为或已验证 diff。若是 ELF，先完成函数/offset/字段/公式/序列化逆向报告，不直接做 binary patch。设备 meminfo 和字段存在性尚 UNKNOWN，不能承诺无需前端改动；只有后端维持原百分比合同后才可评估页面保持兼容。

回滚前必须在实际安装位置保存原文件、SHA256、属主/权限、配置与启动方式，确认可靠恢复入口；没有这些就不部署。以后获准的改动只触及 RAM handler，回滚恢复这份原文件/配置并核对 hash、版本及只读 API。安装/重启/恢复命令均待实际程序与授权确定，本阶段没有生成猜测命令。

离线验收应使用真实同次 meminfo/JSON，对比 Total-Free 与 Total-Available，覆盖缺失/越界字段、单位及舍入，并检查相邻状态字段不变。在线验收或部署仍需后续明确授权。

## 保留状态

设备修改：**NO**。apply-update：**NOT EXECUTED**。ADB Patch：**PATCH_READY_OFFLINE / NOT INSTALLED**。9008：**NOT USED**。本次止步于现有公开/明确只读路径的证据边界，不通过安装或写接口获取后台。
