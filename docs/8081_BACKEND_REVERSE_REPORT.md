# 8081 后端逆向报告

2026-10-02。阶段：8081 高级后台后端逆向。**状态：BACKEND_ARTIFACT_MISSING / OLD_RAM_PIPELINE_CONFIRMED。当前 `/api/device/info` 的真正实现仍未找到，不能宣称已确认当前 RAM 公式。**

## 必填结果

| 项目 | 结论 |
| --- | --- |
| 监听 8081 程序 | **UNKNOWN**；没有在线 LISTEN PID、程序路径、argv 或当前文件 hash |
| 后端语言 | **UNKNOWN**；不能从前端格式、版本标签或旧 ELF 推断 |
| API 实现位置 | **UNKNOWN**；完整 FOTA 搜索没有 `/api/device/info`，当前服务文件未取得 |
| memory_usage 来源 | **UNKNOWN**；FOTA mobile_svr 的 NVRAM `mem_usage` 是已确认的候选上游，消费关系未验证 |
| 当前公式 | **UNKNOWN**；旧 FOTA 的数量/百分比路径均为 Total-Free 口径 |
| 是否读取 MemAvailable | **UNKNOWN（当前 8081）**；**NO（已复核的旧 httpd/mobile_svr RAM 路径）** |
| 能否增加 MemAvailable | 条件方案可设计；当前 route、读取权限和可扩展性未取得，实施适配性 **UNVERIFIED** |
| RAM 修复方案 | 取得当前 handler 后，按键读取 MemTotal/MemAvailable，仅修改该 RAM 字段公式；可选增加原始 Available 字段，详见公式报告 |
| ADB Patch | **PATCH_READY_OFFLINE，保留未安装** |
| 设备修改 | **NO** |

用户要求 YES/NO 的“是否读取 MemAvailable”不能在当前证据缺失时填 NO；API 未输出该字段与后台未读取该字段是两件事。

## 已得到的后端证据

1. 参考 ZIP 4,551/4,551 成员的 CRC、大小与 SHA-256 复核通过；全文件 116,633,721 bytes 做 ASCII/UTF-16 字面搜索。核心 REST 路由、memory_usage、v5.2.4 均为 0 命中。
2. 继续扫描 423 个解压 SELinux 成员及解压内核；仍无目标 REST/字段，实现证据不在可见参考内容中。内核存在 MemAvailable 输出字符串，不能误报“参考系统完全没有该字段”。
3. 旧 `/usr/bin/httpd`：`json_system_status` 分派表指向 `0xf090`，`0xefd0` 读取 meminfo 的 Total/Free/Buffers/Cached，`0xf1a4` 计算 Total-Free。启动 unit 是 `/usr/bin/httpd -d`、默认 80；不能填成当前 8081 程序。
4. 旧 `/usr/bin/mobile_svr`：`0x1350` 取 free 第二行 total/used，计算 `used*100/total`，写临时 NVRAM `mem_usage`，两位小数附 `%`。对应 BusyBox 的 used 是 sysinfo.totalram-freeram，缓存扣除结果在另一行，没有被此命令选用。
5. 核心指令以 GNU ARM Thumb 与 Capstone 交叉复核；19 条精选证据的字符串/原始指令字节、9,805 条原始成员关键词 offset 核对通过。未执行固件程序、popen 命令或 updater。
6. 142 个配置名称候选及全包关键词未找到 RAM 统计/API 字段开关；命中的 memory/ram 配置涉及系统资源限制和硬件策略。当前配置仍未知。
7. 复用上阶段 12 个成功 HTML URL/11 个 body hash，与 FOTA 全成员比较，没有完全相同的内容；这证明参考包与现有资源存在差异，不确定资源作者或安装来源。未重复前端分析。

## 数据链及尚缺的一段

```text
已确认，只对固定 FOTA hash 成立：
BusyBox free 第二行 total/used
  → mobile_svr @0x1350
  → NVRAM 临时字段 mem_usage = "xx.xx%"

候选关系，尚未验证：
NVRAM mem_usage
  → [当前 8081 handler？尚未取得]
  → /api/device/info.system_status.memory_usage

另一条已确认的旧路径：
/proc/meminfo
  → httpd @0xefd0 / @0xf090
  → json_system_status 的 ram.total/used/free 等数量
```

上阶段真实返回 memory_usage=89.97、版本标签 v5.2.4；本轮没有重新采样。该值与旧口径可能相容，但不是代码来源证明，也不能据此判断实际内存压力。

## 下一步需要的实际资料

需要当前 8081 的进程只读导出，或维护方提供当前后台二进制/源码及配置。用户补充确认目前没有可提供的资料路径/维护方链接。GitHub 范围的公开版本/路由关键词检索也未返回可用资料；不把检索无结果当作源码不存在的证明。

若获得已经验证的只读 shell，先 ss/netstat/ps 定位 8081，再读取 PID 对应 exe、cmdline、服务配置与 hash，最后追踪 route→数据读取→计算→JSON 序列化。未获得入口时不猜 root 端口、不重试 ttyd/ADB、不调用控制接口去获取文件。

没有当前实现时，不对旧 httpd/mobile_svr 打补丁，不修改共用 NVRAM，不制作冒名的 8081 后台替代程序。最小 RAM 修改只停留在可审查的条件方案；部署、回滚准备需以实际文件为基础。

## 本轮操作记录与产物

设备 HTTP 请求 **0**，设备 shell 命令 **0**，写接口 **0**，服务启停/重启 **0**，ADB Patch 安装 **0**，设备/NV/USB/分区修改 **0**。没有访问用户列出的禁止接口。此前 PATCH_READY_OFFLINE 产物保留未安装；RAM 部署继续暂停。

本轮新增公开内容仅为报告与脱敏证据元数据；原始 FOTA、ELF、上下文及设备原始 API 内容保留本地忽略目录。

- [8081_BACKEND_SEARCH.md](8081_BACKEND_SEARCH.md)：全包搜索、offset、启动项、配置与来源差异。
- [MEMORY_USAGE_FORMULA.md](MEMORY_USAGE_FORMULA.md)：两条旧路径的指令证据、MemAvailable 边界与最小条件方案。
- [BACKEND_SEARCH_RESULT.json](../analysis/web8081/reports/BACKEND_SEARCH_RESULT.json)：机器可读状态及验证摘要。
- [BACKEND_BYTE_EVIDENCE.csv](../analysis/web8081/reports/BACKEND_BYTE_EVIDENCE.csv)：文件 hash、offset、上下文与解释。

目标的完成边界：FOTA 后端资料已只读核对；**当前 8081 后端实现、语言、监听 PID 和公式仍待取得实际资料后确认。**
