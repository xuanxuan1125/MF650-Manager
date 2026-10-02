# 当前 Web RAM 显示链路

检查日期：2026-10-02。原项目来源：`https://github.com/xuanxuan1125/MF650-Manager`，分支 `main`，恢复基线 `8b02a4f`。

## 已恢复上下文

已阅读仓库 README、HANDOFF、CHANGELOG、全部 docs 文档以及构建与发布交接说明。
沿用 `docs/RAM_AUDIT.md` 的结论：旧占用率把可回收缓存算入已用，原审计未发现真实内存压力。本次只处理 Web 显示。

当前仓库只有 Android 工程和交接文档。全部远程分支只有 `origin/main`；两条提交的树均未提供 `MF650_AUDIT/`、`11_pulled_web/`、8081 服务源码或 `WEB_RAM_FIX*`。文档中的旧电脑绝对路径不在当前电脑。

## 页面 → JavaScript → API

- 目标设备：`192.168.100.1`（与交接审计地址一致）。
- 当前页面：`http://192.168.100.1:8081/mf650.html`，标题“设备信息监控”。
- Port80 的 `feiliu.asp` 在设备信息 iframe 中加载 `/html/mf650.html`。实测该路径与 `/mf650.html` 内容一致，SHA-256 均为 `bc4a01ccbb817ee801c7a16c32e18385b9aa0dc34e75e8f53182c6361b2fbbc8`。
- JavaScript：页面内联脚本 `loadAllData()` → `updateDeviceInfo()` → `updateGauge()`。
- 当前 API：`GET /api/device/info`。
- 当前字段：`system_status.memory_usage`。
- `updateDeviceInfo()` 直接调用 `updateGauge(parseFloat(ss.memory_usage) || 0, false)`。
- `updateGauge()` 把数值限制在 0..100 后更新 `#memoryPercentage` 与 `#memoryGauge .gauge-progress`，没有读取 `MemAvailable`。
- RAM 圆环原来还绑定 `clearCache()`，发送 `POST /api/device/clear_cache`；本次未调用这个写接口。

## 后台程序 → 系统来源

原 RAM 审计确认旧算法是 `MemTotal - MemFree`。当前实机只读抓取再次观察到：

```text
Port80 /system_status_data.asp:
ram.total=227900 kB
ram.used=202568 kB
ram.free=25332 kB
ram.buffers=3068876780（异常，禁止用于新计算）
ram.cached=120

Port8081 /api/device/info:
system_status.memory_usage=88.99%
```

`202568 + 25332 = 227900`，同一统计口径仍存在。两次 HTTP 抓取并非同一时刻，因此不把百分比微小差异视为算法变化。

8081 实际后台程序位置、语言、扩展能力和 `/proc/meminfo` 原始值仍需 root 入口确认；交接文档对后台语言有 C、Go、Python/Go 等不同描述，不凭这些描述选择实现或修改二进制。

## 错误原因与正确目标

旧后端把 Linux 缓存算入“已用”；前端直接显示后端百分比，同时将缺失值当作 0%。现有 API 不提供 `MemAvailable`，前端无法从 `total/free/buffers/cached` 准确还原该值。

目标公式：`effective_used_kb = MemTotal - MemAvailable`；`usage_percent = effective_used_kb / MemTotal * 100`。
新只读 API 必须只读取固定的 `/proc/meminfo`，不接受命令、路径或 Shell 参数；异常字段返回 null，页面显示 `--`。Swap 使用量由 `SwapTotal - SwapFree` 得出，校验范围以 SwapTotal 为准。

## 当前设备访问条件

- Port80：HTTP 200，Padavan 状态接口可读取。
- Port8081：根路径 302 到 `/mf650.html`，页面及设备信息 API 可读取。
- 首次恢复阶段曾观察到 SSH 22、Telnet 23、ADB TCP 5555 拒绝连接；用户纠正后未再尝试这些方式，也未寻找另一个 root 端口。
- Port7689：用户旧审计与当前标题均支持 `ttyd → sh -c sh /usr/bin/feiliu.sh → ShellCrash` 启动链路。Text 初始化成功并完整读取菜单；Binary `b'0\x03'` 导致会话立即关闭，无法输入 id。最新最小复核同样如此，未执行后续设备命令。
- 只读查看 `8 其他工具` 后得到目录线索 `/home/root/ShellCrash`，该层没有系统 shell 入口。设备 feiliu.sh 内容未取得，尚无已核实的安全 shell 进入方式。完整输出与同版本上游分析见 `TTYD_CONNECTION_CHECK.md`；不据此断言设备不存在 root 能力或启动方式变化。
- 未重启设备，未改动 QMI、NV、Wi-Fi、SIM 或网络配置。

在确认 root 入口、原文件路径、hash、权限与持久化方式之前，不部署更改。需要先备份原文件并准备可核验的 rollback 脚本。
PC 已保存 HTTP 拉取的页面原内容、hash 与 ttyd 完整日志，设备权限与真实路径仍待 root 确认。此次补充复核未再尝试其他连接方式。

后续授权的端口发现与 7628 标准 SYNC 审计仍未恢复文件读取：Transport 可握手，shell 关闭，OPEN sync: 返回 CLSE。没有取得 meminfo、feiliu.sh 或 Web 文件 metadata，RAM 部署继续暂停，详情见 ADB7628_SYNC_AUDIT.md。

## 连续 10 次修复前只读检查

Port80 状态接口与 Port8081 设备信息接口均成功读取 10 次。每次 Port80 都满足 `used + free = total`；`buffers` 每次超出 `total`。Port8081 返回旧口径 `89.35%`。这里只证明当前链路及错误存在，不代表修复后通过验收。

| 次数 | Total kB | Used kB | Free kB | Buffers kB（无效） | 8081 旧百分比 |
| --- | ---: | ---: | ---: | ---: | ---: |
| 1 | 227900 | 203364 | 24536 | 3068876780 | 89.35% |
| 2 | 227900 | 203356 | 24544 | 3068876780 | 89.35% |
| 3 | 227900 | 203380 | 24520 | 3068876780 | 89.35% |
| 4 | 227900 | 203388 | 24512 | 3068876780 | 89.35% |
| 5 | 227900 | 203620 | 24280 | 3068876780 | 89.35% |
| 6 | 227900 | 203448 | 24452 | 3068876780 | 89.35% |
| 7 | 227900 | 203356 | 24544 | 3068876780 | 89.35% |
| 8 | 227900 | 203324 | 24576 | 3068876780 | 89.35% |
| 9 | 227900 | 203356 | 24544 | 3068876780 | 89.35% |
| 10 | 227900 | 203356 | 24544 | 3068876780 | 89.35% |
