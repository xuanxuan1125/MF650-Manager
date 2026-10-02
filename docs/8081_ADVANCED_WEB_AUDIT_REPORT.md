# MF650 8081 高级后台审计

2026-10-02 17:25:30–17:50:59，Asia/Shanghai；项目基线 `7f43277568168ac5a1bf65a4afad23d7f2a54550`。**只读审计完成；RAM 数据接口存在，但科学 RAM 统计所需原始字段未取得。设备修改 NO，ADB Patch 保留、未安装。**

先读取用户指定的 FOTA_ANALYSIS、ADB_ENABLE_TEST_REPORT、ADB_PATCH_BUILD_REPORT、ADB_ENABLE_PATHS、BOOT_USB_COMP_WRITERS、ONLINE_SYSTEM_FACTS、WEB_ADB_INTERFACE_AUDIT、TCP2358_RPC_REVERSE。本轮不重复端口发现或已失败的 ADB/ttyd 操作。

## 已发现接口

根 `http://192.168.100.1:8081/` 返回 **302 → /mf650.html**，首页 200。沿此前真实 feiliu.asp 菜单源码引用刷新 11 个 8081 高级页面；没有本轮 Port80 请求。12 个成功 HTML URL（首页两个别名内容相同）全部为内联 JS/CSS，已提取 12 个 script、12 个 style 块；没有独立 JS/CSS/JSON 文件引用。首页还读取 ad.txt；壁纸页面的图片/音频为展示资源，不是代码或 API，本轮未下载。

词法扫描覆盖 fetch、XMLHttpRequest、API_URL、wrapper、模板字符串及参数分支；人工核对动态调用。发现 **36 条 API 路径、44 个方法/路径组合**，15 条路径有查询用途、29 条有写入/执行能力，8 条共享。数量按路径统计，包含源码中的未调用功能，不宣称已经恢复完整后端路由表。

| 功能组 | 路径数 | 当前源码中的接口 | 已确认用途 |
| --- | --- | --- | --- |
| 首页/电源状态 | 4 | `/api/device/info`、`/api/device/status`、`/api/device/clear_cache`、`/api/device-status` | CPU/RAM百分比、温度、网络、电池等；清缓存为写操作 |
| 电源控制 | 5 | control-charge、set-auto-charge、battery-calibration、low-voltage-shutdown、set-charge-levels（均在 `/api/` 下） | 充电、自动充电、校准、低压及阈值 |
| 小区/频段 | 2 | `/api/cell`、`/api/nr5g-band` | 状态、激活、锁定/解锁、扫描、SA/NSA频段 |
| IMEI/流量 | 2 | `/api/imei`、`/api/liuliang` | 信息查询及高风险修改/模式控制 |
| 定时/转发 | 3 | `/api/cron`、`/api/traffic-config`、`/api/forward` | 任务、推送配置、转发服务及日志 |
| 壁纸 | 6 | `/api/photos/` 下 upload、upload/quanping、upload/asus_logo、upload/shuping、device-model、change-wallpaper | 机型查询及图片上传/切换 |
| 系统状态 | 4 | `/api/system/` 下 info、bigscreen、check-update、update-log | 高级后台/屏幕状态、更新查询/变更日志 |
| 系统控制/激活 | 9 | `/api/system/` 下 apply-update、switch-sim、toggle-lcd-screen、toggle-sim、toggle、action、activate、activate-lcd、bigscreen-activate | 更新、SIM/屏幕/云控等、整机动作和功能激活 |
| AT Debug | 1 | `/api/at-debug` | POST atcmd，显示 output |

逐项方法、参数、读写及源码行号见 [8081_API_ENDPOINTS.md](8081_API_ENDPOINTS.md) 和 [endpoint-inventory.json](../analysis/web8081/reports/endpoint-inventory.json)。不是仅凭名称判断用途。

## RAM数据来源

当前实际 **GET `/api/device/info` → `system_status.memory_usage=89.97`**，CPU percentage=98.03；这是单次采样，不能推断长期负载或真实 RAM 压力。原始完整 JSON 已保存，不公开设备标识/激活码/推送凭据。

| 数据 | 是否在当前已采集 JSON 中存在 |
| --- | --- |
| RAM 百分比 | YES，memory_usage=89.97 |
| MemTotal | NO |
| MemFree | NO |
| MemAvailable | NO |
| 原始 meminfo / 缓存 / Swap 数量 | NO |

18 个只读 GET 变体、19 次请求均 200，覆盖 13 条 API 路径；完整字段名/类型见 [api-schemas.json](../analysis/web8081/reports/api-schemas.json)。system/info 提供高级后台版本 **v5.2.4** 和功能状态，没有 CPU/RAM 原始字段。device/status 的 web_ui.version 同为 v5.2.4，但 system_version 为 1.0.0；这些是不同接口的原样标签，未据此确定固件内核/adbd 身份。此前 Port80 的 MF650_V3.4 标签也不被当成当前 8081 版本。

NO 指当前响应/前端引用范围未提供字段，不声称内核没有 MemAvailable 或后台不存在未引用路由。未访问没有源码证据的 `/api/status`。

## RAM显示链路

`GET /api/device/info → system_status.memory_usage → updateDeviceInfo(): parseFloat(...) || 0 → updateGauge(..., false) → clamp 0..100 → toFixed(2) → #memoryPercentage/圆环`。首页每 2 秒刷新，没有前端 total/free/available 计算。

后台 Total-Free 口径有旧阶段证据，本轮没有当前后端源码/hash或 meminfo，仍属于待核实推断。新公式应为 `(MemTotal-MemAvailable)/MemTotal*100`，当前不可计算。详见 [RAM_WEB_DISPLAY_FLOW.md](RAM_WEB_DISPLAY_FLOW.md)；后续方案和回滚条件见 [RAM_FIX_PLAN.md](RAM_FIX_PLAN.md)。

## ADB相关能力

**NO（已取得页面/JS/JSON范围）。** 未发现 ADB switch、restart adbd、enable debug/adbd gate、launch_adbd 或任意 shell API。未将功能激活码、cron COMMAND 或 AT output 当作 Linux shell 通道。

device/status 里有 `ssh.enabled=true`，但前端没有对应 SSH 控制 API；该状态字段不证明另一个可用 root 端口。本轮没有连接 SSH、扫描端口或更改既有 7628 transport/gate 结论。

## Debug能力

**YES：可见菜单中的 at_debug.html 与 POST `/api/at-debug`。** 输入 atcmd 后显示 output，浏览器拦截含 usbcfg 的输入。仅静态记录，**AT 请求 0**，不验证或绕过拦截。device/info 的 `_debug` 是解析诊断字段，不是系统命令控制台。没有发现另一套隐藏 Debug/maintenance 命令页面。

## Service控制

**YES，限定为具体业务服务。** forward 页 POST action=start/stop 控制短信转发服务；本轮只读 status 返回 `{"status":"success","running":true}`。电源接口也提供 auto_charge/controller_running、battery_calibration/running 等状态，cron 有定时业务任务。未发现通用 systemctl、进程 PID 列表或可重启任意服务的接口。

| 其他能力 | 判定及边界 |
| --- | --- |
| 日志 | YES，forward action=log 已读；system/update-log 是版本变更日志源码入口，未请求 |
| 文件系统/Flash/mount信息 | NO（已采集字段/引用范围）；服务器未公开实现仍 UNKNOWN |
| 进程/服务信息 | 有业务 running 标志；没有全系统 ps/PID 列表 |
| Modem/网络/电池/温度/CPU | YES，有对应 JSON 或页面字段 |
| 隐藏服务器页面 | NO（引用范围）；未猜路径或枚举 |
| 隐藏 UI 链接 | IMEI 区三击会打开外部说明页；不是 8081 隐藏维护页面或 root 入口，未打开外站 |
| 2358/7777/7628 关系 | JS 未引用这些端口、WebSocket或TCP连接；后端是否使用它们 UNKNOWN，RPC请求0 |

## 写接口

上述 29 条路径具备写入/执行能力，全部只记录。包括清缓存、充电、AT、锁频/小区、IMEI、流量、任务、转发、壁纸、SIM、屏幕、云控、升级/整机动作及激活。风险可能涉及配置、网络中断、NV或重启，不按 HTTP method 判断只读。

特别排除 **GET `/api/cell?ACTION=unlock_lte` / `unlock_nr`**：源码明确解除锁定，属于写动作。check-update 与 update-log 本轮也未请求；不触发更新相关流程。没有 POST、PUT、DELETE、上传、激活、服务启停、清日志/缓存或主动扫描。9008 写入口仅保留已有源码能力记录，没有继续 EDL 研究或测试。

## 权限模型

19 次 API GET 无 Authorization/Cookie/token 均返回 200；响应没有 Set-Cookie/WWW-Authenticate。前端无通用登录/session/JWT流程；推送 token 是业务参数。系统/小区/屏幕有功能激活及 403 DEVICE_LOCKED 的前端处理，当前 activation_status 为 activated_both。

写接口的服务器端登录/激活/来源检查 **UNKNOWN**，未用 POST 验证。不能从匿名 GET 推断所有写接口可匿名执行。详见 [8081_AUTH_MODEL.md](8081_AUTH_MODEL.md)。

## 当前最安全路线

优先继续使用已有 8081 设备信息服务，取得其官方源码、支持的只读内存导出或能提供 MemAvailable 的正式扩展说明，之后才准备最小 RAM 数据/显示修改及回滚。当前 percentage-only API 无法直接完成科学 RAM 修复；不虚构新路由、不绕过 AT限制、不通过 cron 执行系统命令，也不因此转向刷 ADB Patch。

## 设备修改

**NO。** 总计 33 GET（32个200、1个302），其中 API 19（18个变体，一次补采样）；POST/PUT/DELETE、未知 RPC、端口扫描、ADB/ttyd 操作、设备文件/boot/NV/systemrw/USB 修改及 Patch/更新/重启均为 0。服务器访问日志/状态缓存等内部 GET 行为未取实现，不宣称后端处理 GET 完全不产生临时状态。

原始 HTML/JS/CSS/API JSON 保存在 `analysis/web8081/{html,js,css,api}/` 与 `test-results/web8081/`，全部 gitignored；公开仅文档、离线分析脚本及脱敏索引/请求摘要。验证范围为源码和只读响应，不是写接口、安全攻击、RAM部署或 APK 测试。

保存校验曾发现 `/api/device/status` 与 `/api/device-status` 的文件名冲突。前者首个采样保留了完整解析 JSON及原响应摘要，但原始字节文件被覆盖；已单独补采一次原始响应并修正命名。manifest 明确区分 PARSED_JSON 和 ORIGINAL_BYTES，原响应 SHA256 与保存文件 SHA256分别列出，不把重序列化文件当成原始响应。其余原始响应逐一与摘要核对；补采仅 GET，没有扩大控制权限。

证据：[HTTP_GET_MANIFEST.csv](../analysis/web8081/reports/HTTP_GET_MANIFEST.csv)、[source-index.json](../analysis/web8081/reports/source-index.json)、[导航来源](../analysis/web8081/reports/navigation-source.json)。离线复核命令：`python tools/audit_web8081_sources.py analysis/web8081/html analysis/web8081`；脚本没有网络或执行 JS 的能力。
