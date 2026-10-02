# 项目维护交接文档 (HANDOFF.md - v0.2.0)

## 2026-10-02 FOTA / ADB 维护状态

FOTA 仅离线解包分析，没有刷机或 RAM 部署。FOTA adbd 默认 TCP 7628，服务有厂商许可检查，配套 property_set 为空实现；在线 7628 很可能是该类定制 adbd，但文件 hash/UID 未验证。唯一一次 tcpip 5555 返回 error: closed，5555 超时，当前 USB 仍 05C6:9057，RNDIS/PnP/IP/网关未变。

restart_adbd.asp 的间接启动链存在序列号/可能持久文件写入，未满足用户“无其他副作用”条件；usb: 不会明确切 9059，也没有可靠恢复 9057 的渠道，二者均未执行。设备 shell/SYNC/meminfo 仍不可用。继续先读 [FOTA_ANALYSIS.md](docs/FOTA_ANALYSIS.md)、[ADB_ENABLE_RESEARCH.md](docs/ADB_ENABLE_RESEARCH.md)、[ADB_ENABLE_TEST_REPORT.md](docs/ADB_ENABLE_TEST_REPORT.md)，不要重复扫描、猜 root 端口或部署 RAM 修复。

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
