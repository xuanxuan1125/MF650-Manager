# 项目维护交接文档 (HANDOFF.md - v0.2.0)

## 2026-10-02 厂商附件离线取证与备份安全审计

**CURRENT_BACKEND_NOT_FOUND / RAM_NOT_READY。** 三个用户 ZIP 原件、嵌套 ZIP 和 372 个非目录文件已核对 hash/CRC，76 组跨包同 hash；当前三个指定页面均不在包内，全部缓存页面与包内内容无 hash 匹配。高级后台参考 ELF 的默认监听为 6391，修复包 main 为 8152 AT/锁频服务；8081 仅有旧 BusyBox CGI unit 声明，未找到当前 v5.2.4 `/api/device/info` 或 memory_usage handler。当前 PID/路径/hash/语言/公式/MemAvailable 使用仍 UNKNOWN。

备份 BAT 唯一设备动作是 `adb pull /www .\`（远端 R0），但原生客户端可信度、入口和当前后端覆盖未确认；整体安全性与完整导出能力 UNKNOWN。自身重实现只有 DRY_RUN，条件备份方案 NOT_READY/NOT_EXECUTED。修复包会覆盖 `/www`、改权限/服务，原生 main 有 AT/锁频写能力；高级后台安装器另含 adbd/USB/NV 改动。不运行附件，不以旧 UI 变体作为当前回滚原件。

设备请求/命令/修改均为 0；未执行任何附件，ADB Patch 保留未安装，9008 未使用。只提交自身脚本、hash/manifest 和脱敏报告，原始 ZIP/ELF/EXE/页面/设备备份不提交。详见 [VENDOR_ARTIFACTS_ANALYSIS.md](docs/VENDOR_ARTIFACTS_ANALYSIS.md)、[VENDOR_BACKUP_TOOL_AUDIT.md](docs/VENDOR_BACKUP_TOOL_AUDIT.md)、[VENDOR_BACKEND_REVERSE.md](docs/VENDOR_BACKEND_REVERSE.md)、[VENDOR_NETWORK_FIX_AUDIT.md](docs/VENDOR_NETWORK_FIX_AUDIT.md)、[BACKEND_BACKUP_EXECUTION_PLAN.md](docs/BACKEND_BACKUP_EXECUTION_PLAN.md)。

## 2026-10-02 8081 更新通道取证

**CASE C / METADATA_ONLY_BACKEND_ARTIFACT_MISSING。** 当前 system.html 与既有缓存 hash 一致；基于完整更新调用链，check-update 与 update-log 满足 SAFE_GET 条件，各只请求一次，均 HTTP 200。前者仅返回 success、local_version=v5.2.4 与已是最新版消息，后者仅一条 MF650L 屏幕功能说明；均无包 URL、服务器或仓库。公开检索只找到需登录的附件列表/早期功能介绍，未取得可验证后端。当前 PID/路径/语言/handler/RAM 公式及 MemAvailable 使用仍 UNKNOWN。

本阶段设备 GET 共 3，POST/控制请求 0；apply-update 未执行，设备未修改，ADB Patch 未安装，9008 未使用。原始响应本地忽略，提交脱敏元数据/文档/单次 GET 捕获脚本；三个 endpoint 已采集，不再重发。没有后端制品，未生成 handler 报告/diff/包 manifest，不修改 RAM_FIX_PLAN。下一步补齐同版本制品与当前进程身份及可恢复原件。详见 [8081_UPDATE_FLOW.md](docs/8081_UPDATE_FLOW.md)、[8081_UPDATE_CHANNEL_AUDIT.md](docs/8081_UPDATE_CHANNEL_AUDIT.md)、[8081_BACKEND_ARTIFACT_REPORT.md](docs/8081_BACKEND_ARTIFACT_REPORT.md)。

## 2026-10-02 8081 后端逆向

**BACKEND_ARTIFACT_MISSING / OLD_RAM_PIPELINE_CONFIRMED。** 全量 FOTA 4,551 个成员及 424 个解压派生单元未命中 `/api/device/info` / memory_usage 字面量；当前 8081 PID、程序、语言、route 与公式仍 UNKNOWN。已以字节/Thumb 指令确认旧 httpd 的 Total-Free 数量路径，以及 mobile_svr 取 BusyBox free 第二行后计算百分比写 NVRAM mem_usage；后者是否被当前 8081 消费尚未验证。参考解压内核有 MemAvailable 字符串，旧用户态 RAM 路径不读取它。

本轮设备请求/命令/写接口均为 0，不重复前端分析，设备未修改，ADB Patch 保留未安装。下一步需当前监听进程及后台文件/源码的只读导出，不以旧 FOTA 替代在线程序，不重试 ttyd/ADB 或猜端口。详见 [8081_BACKEND_SEARCH.md](docs/8081_BACKEND_SEARCH.md)、[MEMORY_USAGE_FORMULA.md](docs/MEMORY_USAGE_FORMULA.md)、[8081_BACKEND_REVERSE_REPORT.md](docs/8081_BACKEND_REVERSE_REPORT.md)。

## 2026-10-02 8081 高级后台全面只读审计

本轮 33 GET（19 API，18个变体），0 POST/控制请求；当前高级后台版本标签 v5.2.4。已完整分析实际菜单引用的 12 个 HTML URL及内联 JS/CSS，发现 36 API 路径/44 方法组合。RAM 只有 memory_usage=89.97%，19 个 JSON 没有 MemTotal/MemFree/MemAvailable；新口径仍不可计算。发现 AT Debug、短信转发服务启停和业务日志，未发现 ADB/adbd 或通用 shell 控制；GET cell unlock 是写动作，未调用。

详见 [8081_ADVANCED_WEB_AUDIT_REPORT.md](docs/8081_ADVANCED_WEB_AUDIT_REPORT.md)、[8081_API_ENDPOINTS.md](docs/8081_API_ENDPOINTS.md)、[8081_AUTH_MODEL.md](docs/8081_AUTH_MODEL.md)、[RAM_WEB_DISPLAY_FLOW.md](docs/RAM_WEB_DISPLAY_FLOW.md)、[RAM_FIX_PLAN.md](docs/RAM_FIX_PLAN.md)。下一步优先取得当前 8081 服务受支持的 MemAvailable 数据来源及可恢复 Web/后台资料；不猜维护 URL、不发送 AT/RPC、不优先刷补丁。设备未修改，ADB Patch 保留未安装，RAM 未部署。原始资源与含标识/凭据的 JSON 均忽略，只提交脱敏分析产物。

## 2026-10-02 ADB Patch 在线只读预检

**WEB_COMMAND_CONSOLE_UNAVAILABLE / PATCH_PRECHECK=FAIL / Recovery Installability=UNVERIFIED。** 本轮仅 9 次页面 GET 与本机 USB 枚举；在已取得系统/设置/日志页和相关 JS 中未发现正常命令提交链，已按停止条件结束设备访问。POST、id/系统命令、安装和持久修改操作均为 0；没有重试 ADB/SYNC/ttyd 或端口扫描。

当前 USB 为 `05C6:9057`，Web 标签为 `MF650_V3.4`。在线 adbd hash、boot token、systemrw override、挂载和 meminfo 均 UNKNOWN，不能用枚举 PID 或旧 FOTA 内容代替。补丁 2 bytes 差异和回滚原载荷已离线复核；八项硬门槛仍有五项 UNKNOWN，禁止安装，RAM 部署继续暂停。详见 [ADB_PATCH_ONLINE_PREFLIGHT.md](docs/ADB_PATCH_ONLINE_PREFLIGHT.md) 和 [ONLINE_SYSTEM_FACTS.md](docs/ONLINE_SYSTEM_FACTS.md)。原始页面含凭据，仅留本地忽略目录；后续先取得厂商支持的只读导出/维护证据，再评估实际适配性。

## 2026-10-02 ADB Patch 离线构建状态

用户后续明确授权制作、但不安装最小厂商 adbd gate patch。本轮已生成 Literal `90DB → 9057`，文件 offset `0x5E18`，覆盖 4 bytes、实际仅 2 bytes 不同；9059 许可保留，90DB 自动许可移除。patched SHA256 `efa63d205f045b66426f14e547050613fb9ea4f27a1536965e9f987d4208d5ae`。原 adbd/启动链/USB composition/TCP 7628 的其他字节未改。

Builder 和 9 项离线回归检查已完成；成对 Enable/Rollback ZIP 保存在本地 `patch-output/`，固件二进制/ZIP 全部忽略，不上传 Release。状态 **PATCH_READY_OFFLINE / INSTALLABILITY UNVERIFIED**；复用原 updater 和原厂 MD5 包尾，写前/后强制 SHA256，Rollback 严格接受 patched hash。本轮设备访问、安装、重启、USB/NV/分区操作均为 0。完整 EDL 恢复 **NOT READY**，不能承诺设备已可部署。

当前应读 [ADB_PATCH_DESIGN.md](docs/ADB_PATCH_DESIGN.md)、[ADB_PATCH_BUILD_REPORT.md](docs/ADB_PATCH_BUILD_REPORT.md)、[EDL_RECOVERY_READINESS.md](docs/EDL_RECOVERY_READINESS.md)。等待下一阶段明确安装授权，并先补齐在线 hash/boot token及 recovery 包接受/挂载/工具依赖条件；不得自动安装或激活。RAM 部署继续暂停。

## 前阶段 FOTA / ADB 维护记录

服务许可逆向阶段已完成：参考 adbd 的隐藏 BSS flag `0x8044` 有 2 条写入指令（`0x3588` boot 匹配、`0x38f8` 厂商序列号授权），3 条逻辑启用路径。授权是进程内许可及临时文件/MD5 流程，不需直接改 USB/boot/NV；未生成或尝试凭据。厂商合法客户端、发放/撤销方式及在线文件版本仍未知。全量 ASP/2358 与当前 Web 源码没有发现 ADB 专用开关。本轮只有 24 次已知页面 GET，控制请求 0。

稳定 serial/adb_devid 的正常重启 bookkeeping 可在 USB 已 active、启动链/override 已核实等条件下定为 R1；在线尚未确认这些条件，且仅重启解锁 UNLIKELY。restart_adbd 和 adb usb 均未执行。当前下一步以 [ADB_ENABLE_PATHS.md](docs/ADB_ENABLE_PATHS.md) 的厂商临时授权条件方案为准，证据见 [ADB_VENDOR_AUTH_REVERSE.md](docs/ADB_VENDOR_AUTH_REVERSE.md)、[WEB_ADB_INTERFACE_AUDIT.md](docs/WEB_ADB_INTERFACE_AUDIT.md)、[TCP2358_RPC_REVERSE.md](docs/TCP2358_RPC_REVERSE.md)。RAM 部署继续暂停。

以下保留前阶段 FOTA/TCPIP 验证记录：

FOTA 仅离线解包分析，没有刷机或 RAM 部署。FOTA adbd 默认 TCP 7628，服务有厂商许可检查，配套 property_set 为空实现；在线 7628 很可能是该类定制 adbd，但文件 hash/UID 未验证。唯一一次 tcpip 5555 返回 error: closed，5555 超时，当前 USB 仍 05C6:9057，RNDIS/PnP/IP/网关未变。

上阶段 restart_adbd.asp 未满足当时用户“无任何其他副作用”的执行条件；本轮已按正常 bookkeeping 重评风险。usb: 不会明确切 9059，也没有可靠恢复 9057 的渠道，二者均未执行。设备 shell/SYNC/meminfo 仍不可用。继续先读 [FOTA_ANALYSIS.md](docs/FOTA_ANALYSIS.md)、[ADB_ENABLE_RESEARCH.md](docs/ADB_ENABLE_RESEARCH.md)、[ADB_ENABLE_TEST_REPORT.md](docs/ADB_ENABLE_TEST_REPORT.md)，不要重复扫描、猜 root 端口或部署 RAM 修复。

## 1. 架构总览

MF650 Manager 是基于 Android 现代技术栈（Kotlin + Jetpack Compose + Coroutines + Flow + Retrofit）构建的随身 Wi-Fi 管理工具，严格采用 MVVM 架构：

```
app/src/main/java/com/mf650/manager/
├── data/
│   ├── api/
│   │   ├── PadavanApi.kt         # Port 80 Padavan 路由器原生后台 (Basic Auth)
│   │   └── AdvancedApi.kt        # Port 8081 飞流高级后台 (免认证 REST/CGI)
│   ├── auth/
│   │   └── PadavanAuthInterceptor.kt # 自动注入 Basic 认证头
│   ├── model/
│   │   ├── Models.kt             # 严密契合实机 8081/80 返回的数据实体
│   │   └── RiskLevel.kt          # R0..R4 操作风险评级模型
│   ├── parser/
│   │   ├── BatteryNormalizer.kt  # 核心：0..6 硬件挡位至百分比映射，解决 150% 错误
│   │   ├── UnitNormalizer.kt     # 核心：KB/s、MB/s、GB、dBm 单位清洗与换算
│   │   ├── PadavanFormBuilder.kt # Port 80 apply.cgi 表单构造器
│   │   ├── PadavanParsers.kt     # Port 80 ASP 页面响应提取
│   │   └── SignalQualityEvaluator.kt # 5G/4G 信号等级评定
│   ├── repository/
│   │   └── Mf650Repository.kt   # 双后台聚合、独立健康探测、无缝降级容灾
│   └── security/
│       ├── HostAllowlistInterceptor.kt # 局域网 Host 安全白名单
│       ├── SecureCredentialStorage.kt  # Keystore 硬件级凭据存储
│       └── SensitiveDataRedactor.kt    # 敏感数据日志脱敏
└── ui/
    ├── components/               # Liquid Glass 核心组件库
    │   ├── LiquidGlassCard.kt    # 带按压微动效与渐变高光的玻璃卡片
    │   ├── AmbientBackground.kt  # 环境光晕底板
    │   ├── BackendHealthBadge.kt # Port 80 / 8081 / Cell 独立状态指示
    │   ├── BatteryCapsule.kt     # 顶栏微型电量胶囊
    │   ├── AdvancedControlGrid.kt# 8大高级功能快捷入口
    │   ├── NetworkHeroCard.kt    # 顶部核心网络聚合大卡 (含60s折线图)
    │   ├── CompactMetricsRow.kt  # 信号、电量、温度、流量四宫格
    │   ├── DeviceHealthCard.kt   # CPU、RAM、运行时长
    │   └── RiskConfirmDialog.kt  # R1-R4 分级风险确认弹窗
    ├── navigation/
    │   └── NavGraph.kt           # 底部毛玻璃导航栏与全局路由分发
    ├── screens/
    │   ├── home/HomeScreen.kt    # 首页全量仪表盘
    │   ├── cellular/CellularScreen.kt # 基站管理与简洁/专业双模
    │   ├── wifi/WifiScreen.kt    # 双频 Wi-Fi 管理与 15s 倒计时
    │   ├── messages/MessagesScreen.kt # 类原生聊天短信息中心
    │   └── more/
    │       ├── MoreScreen.kt     # 6大玻璃分类卡片
    │       ├── MoreSubScreens.kt # 13个全量高级子功能页面
    │       └── ChangelogScreen.kt# 应用内静态版本演进日志
    ├── theme/
    │   ├── Color.kt              # 语义色彩与 Light/Dark/AMOLED 调色板
    │   ├── GlassTokens.kt        # 圆角、边框、透明度设计令牌
    │   └── Theme.kt              # 主题配置
    └── viewmodel/                # 业务逻辑与状态驱动层
        ├── DashboardViewModel.kt # 首页数据轮询与 60s 速率队列
        ├── CellularViewModel.kt  # 基站状态与锁频交互
        ├── WifiViewModel.kt      # Wi-Fi 配置与重启倒计时
        ├── SmsViewModel.kt       # 短信加载、发送、标记与删除
        └── MoreViewModel.kt      # 8081 全量高级能力调用层
```

---

## 2. 关键业务约定与禁止事项

1. **绝对禁止 clamp 掩盖电量异常**：
   - 如果未来设备固件升级修改了电量格式，必须在 `BatteryNormalizer.kt` 中添加新的格式分支（如探测到直接输出 0..100 浮点数），绝不能直接 `level.coerceIn(0, 100)`！
2. **所有破坏性操作必须挂载 RiskConfirmDialog**：
   - R2：无线重启、锁频；
   - R3：整机重启、Crontab 定时注入；
   - R4：AT 指令、IMEI 更改。严禁绕过确认直接请求 API。
3. **8081 离线时的降级体验**：
   - `Mf650Repository` 会自动捕获 8081 的异常，将 `advancedOnline` 设为 `false`，但保持 Port 80 轮询畅通，并在 UI 上标示出来。不要在 ViewModel 强行抛出 8081 的网络异常。

---

## 3. 后续演进路线建议 (v0.3.0+)

1. **桌面小组件 (App Widget)**：利用 Glance 或 RemoteViews 编写桌面 4x2 实时流量与电量微件。
2. **多语言支持 (i18n)**：抽取硬编码中文字符串至 `strings.xml`，支持繁体中文与英文。
3. **高级频段聚合监控 (CA / Carrier Aggregation)**：从高通 AT 指令 `AT+QCAINFO` 解析 SCC 辅载波聚合状态并在专业模式呈现。
