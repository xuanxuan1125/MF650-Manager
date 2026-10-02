# MF650 Web RAM 修复进度报告

日期：2026-10-02。

**状态：原工程恢复、交接阅读和 Web 前端错误定位完成；设备部署尚未进行。不能标记“修复完成”。**

最新 FOTA/ADB 阶段：完成 4,551 文件 hash 复核、66 PID USB 矩阵及关键 ELF 逆向；一次已授权 tcpip 5555 返回 error: closed，5555 超时。restart_adbd.asp 与 adb usb 未满足用户执行条件，均未执行。当前没有实时 meminfo 或 Web 原文件访问，仍无 RAM patch/部署；详见 [FOTA_ANALYSIS.md](FOTA_ANALYSIS.md) 和 [ADB_ENABLE_TEST_REPORT.md](ADB_ENABLE_TEST_REPORT.md)。以下为此前 RAM/SYNC 阶段记录，不代表本次曾重复这些探测。

RAM 修复部署仍暂停。完整端口重新发现已经结束，本轮仅审计 7628 的标准 SYNC READ，没有重复扫描或 shell 测试。7689 为 ttyd/ShellCrash 菜单终端，直接 root shell 未验证；7628 为 ADB-compatible restricted transport，标准 sync: 在 OPEN 阶段返回 CLSE。详见 `PORT_REDISCOVERY_REPORT.md` 和 `ADB7628_SYNC_AUDIT.md`。

## 项目与文档

- 来源：已有 GitHub 仓库 `xuanxuan1125/MF650-Manager`。
- 当前分支：`main`；基线：`8b02a4f`，前一提交：`28ca821`。
- 已阅读：README、HANDOFF、CHANGELOG、全部现有 docs 文档及根目录构建/发布说明。
- 缺失：文档引用的 `MF650_AUDIT`、完整原始 Web 审计目录和 8081 后端源码；仓库没有其他远程分支，Release 只有 APK。
- 原 RAM 审计结论保持不变：约 90% 是统计口径误导，原审计未发现真实内存压力。

## 修改前与错误算法

`mf650.html` / `html/mf650.html` 的内联 JavaScript 直接显示 `GET /api/device/info` 返回的 `system_status.memory_usage`，没有 `MemAvailable` 数据。

旧算法：`used = Total - Free`。
目标算法：`effective_used = MemTotal - MemAvailable`。
目标百分比：`effective_used / MemTotal * 100`。

链路、字段、文件 hash 和 10 次只读样本详见 `current_ram_display_flow.md`。

## 计划修复方案（尚未实施）

1. 通过可用 root 入口核实后端程序、Web 文件位置、权限和持久化方式。
2. 先在 `backup/` 备份设备原文件、SHA-256、权限及属主，生成并检查 rollback 脚本。
3. 优先通过现有 Web 扩展提供只读 `/api/memory-status`；若后端闭源且无法扩展，再评估独立轻量服务。禁止修改 ELF 或基带程序。
4. API 只读固定 `/proc/meminfo`；不接受任意命令、Shell 参数、文件路径或任意文件读取请求。返回任务要求的 total/free/available/buffers/cached/slab/swap 和 usage_percent。
5. 缺失或异常 RAM 字段返回 null，页面显示 `--`；各 RAM 字段校验 `0 <= value <= MemTotal`。SwapFree/SwapUsed 按 SwapTotal 校验，不把有效的大容量 Swap 当作 RAM 异常。
6. 保留原页面样式，更新 RAM 圆环、有效使用/总量、可用内存及 MemAvailable/Cache/Swap 详情。移除 RAM 圆环的清理缓存动作，改为只读详情。

这些步骤需根据实际后端与扩展路径落地，当前没有猜测设备目录或安装新服务。

## 本次已修改文件

- `docs/current_ram_display_flow.md`：当前链路审计及修复前样本。
- `docs/RAM_FIX_REPORT.md`：此进度报告。
- `docs/TTYD_CONNECTION_CHECK.md`：用户补充连接方式后的完整复核记录。
- `docs/PORT_REDISCOVERY_REPORT.md`、`docs/ROOT_ACCESS_CANDIDATES.md`：前阶段端口发现与 root 入口证据，补充 SYNC 结论并脱敏。
- `docs/ADB7628_SYNC_AUDIT.md`、`docs/ADB_SYNC_FILESYSTEM_MAP.md`：本轮标准 SYNC 读取失败及文件系统定位状态。
- `tools/adb_sync_readonly.py`、`tools/tests/test_adb_sync_readonly.py`：标准只读客户端与 6 项离线测试。

未修改 Android App。未修改设备 Web 文件或后台程序。

## 测试结果

| 检查 | 结果 |
| --- | --- |
| Port80 `/system_status_data.asp` 连续读取 10 次 | PASS（修复前连通性及旧字段检查） |
| Port8081 `/api/device/info` 连续读取 10 次 | PASS（修复前连通性，旧占用率 89.35%） |
| RAM buffers 范围验证 | 10/10 超范围，不能用于新计算 |
| Chrome Desktop 页面入口 | 可打开，修复后显示尚未验证 |
| Chrome Android 真机 | 未验证，无可用 Android 测试设备 |
| 新 API 与 `/proc/meminfo` 误差 <1% | 未执行，缺少 root 读取入口及已部署的新 API |
| HTTP 页面内容/hash 的 PC 副本 | 已保存到 PC backup/20261002-predeployment |
| 设备原文件权限/属主备份 | 未完成，尚未获得设备文件访问，HTTP 副本不能替代 |
| rollback | 尚未生成，必须在确认原路径、文件和部署方式后生成 |

## 当前阻碍与继续条件

用户旧审计确认 7689 为 ttyd 菜单入口，启动链路是 `ttyd :7689 → sh -c sh /usr/bin/feiliu.sh → ShellCrash 菜单`；当前标题与此一致，没有证据表明启动方式变化，也未验证直接 root shell。该前阶段复核未再尝试其他端口或连接方式。
已用 Chrome 观察，并用 `wsproto` 核验 `/ws`、`tty`、空 AuthToken、Text 初始化、Binary `b'0\x03'` 与菜单后发送顺序。前阶段三次最小测试均初始化成功，但 Ctrl+C 后会话关闭，日志解析为关闭码 1006。无法继续输入 id，尚未取得 `uid=0(root)`，因此没有执行后续设备读命令或部署。
继续只读查看了 `8 其他工具`：目录信息为 `/home/root/ShellCrash`，仅显示测试、新手引导、日志及推送工具，没有系统 shell 入口。没有执行这些工具。完整输出、上游同版本源码分析和边界见 `TTYD_CONNECTION_CHECK.md`。
设备 `/usr/bin/feiliu.sh` 的内容仍未取得；不能把上游脚本等同于设备定制包装脚本。目前尚无经过核实的安全 shell 进入方式，需实际脚本内容或旧审计中的完整脚本输出。

本轮四个 stock pull（version、meminfo、mounts、passwd）都 connect failed: closed。原始 STAT/LIST/RECV 入口复核均在 OPEN sync: 收到 CLSE，未发送文件请求，pulled 目录无文件。没有得到实时 MemTotal/MemAvailable，未计算新百分比，也未采样新的 8081 RAM 值；不生成伪造内存 JSON 或公式验证结果。Web root、实际文件路径与元数据仍未知，未生成修改副本/patch，尚不具备部署条件。

## 设备影响、回滚与 Git

无重启、无基带修改、无 QMI/NV/Wi-Fi/SIM/网络配置修改，也未调用清理缓存 API。
当前没有设备更改需要回滚。正式修复必须先提供原文件备份与 rollback 脚本。
本轮只提交脱敏审计报告与只读分析工具，提交标题为 `docs: audit restricted ADB sync access on MF650`。不使用 `fix: correct MF650 web RAM usage calculation` 描述尚未实施的修复；不提交原始抓包、MAC、本机网络详情或设备文件。
