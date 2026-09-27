# MF650 Manager Android 原生应用架构设计 (ARCHITECTURE.md)

## 1. 架构总览与核心设计原则

`MF650 Manager` 是专门为阿乐卡（ALECA）MF650 5G 便携随身 Wi-Fi（高通骁龙 SDX55 平台）量身打造的原生 Android 管理客户端。
针对设备独特的双 Web 后台体系（Port 80 Padavan 嵌入式后台 + Port 8081 飞流定制高级后台 + Port 7689 ttyd 终端），本应用摒弃了低效低劣的套壳 WebView 架构，采用现代化 **纯原生 Android 技术栈**（Kotlin + Jetpack Compose + Material 3 + MVVM + Flow + Retrofit + EncryptedSharedPreferences）。

```
+---------------------------------------------------------------------------------+
|                                 UI / Presentation Layer                         |
|  [HomeScreen]    [CellularScreen]    [WifiScreen]    [MessagesScreen]   [More]  |
|  - SpeedCanvas   - Pro Mode / Lock   - 2.4G / 5G     - Inbox / Outbox   - 16项  |
|  - Hero Card     - SignalIndicator   - 15s Countdown - Send SMS Dialog  - AT/PMIC|
+---------------------------------------+-----------------------------------------+
                                        | UI State Flow & Event Triggers
+---------------------------------------v-----------------------------------------+
|                                ViewModel Layer                                  |
|   DashboardViewModel  |  CellularViewModel  |  WifiViewModel  |  SmsViewModel   |
|   - 2s Fast Polling   |  - Lock ARFCN/PCI   |  - Diff Engine  |  - Raw Parser   |
|   - Speed Queue (20)  |  - Band Filtering   |  - Restart Timer|  - CRUD Actions |
+---------------------------------------+-----------------------------------------+
                                        | Repository Failover Calls
+---------------------------------------v-----------------------------------------+
|                           Mf650Repository (双端口聚合层)                        |
|   - 动态路由切换 (Active IP)        - Port 8081 首选 -> Port 80 降级兜底         |
|   - 统一 AppState (Connectivity, Auth, Offline)                                 |
+-------------------+-----------------------------------+-------------------------+
                    |                                   |
         Retrofit (Port 80)                  Retrofit (Port 8081)
      BasicAuth & Allowlist Interceptor     HostAllowlist Interceptor
                    |                                   |
+-------------------v-------------------+   +-----------v-------------------------+
|     Port 80 Padavan ASP / CGI         |   |      Port 8081 飞流 Go / REST       |
|  - /system_status_data.asp (~6.89ms)  |   |  - /api/device/info (~1383ms)       |
|  - /sms_in.asp / /sms_out.asp         |   |  - /api/cell?ACTION=get_status      |
|  - /apply.cgi (FormUrlEncoded 完整表单)|   |  - /api/device-status (IP5332 PMIC) |
|  - /syslog.asp                        |   |  - /api/at-debug (SDX55 裸指令)     |
+---------------------------------------+   +-------------------------------------+
```

---

## 2. 分层架构细节

### 2.1 UI 表现层 (Jetpack Compose + Material 3)
- **声明式 UI**：无 XML 布局堆砌，全界面由 Jetpack Compose 组件构建。
- **动态主题与视觉语言**：以科技青（Cyan / Teal）为主基调，支持深色（Dark）、浅色（Light）模式，并在高危操作处引入阶梯色彩警戒系统（R0 绿色至 R4 血红色）。
- **零依赖原生 Canvas**：`SpeedCanvasChart` 利用 Compose `Canvas` 原生绘制平滑贝塞尔与折线速度曲线，彻底消除第三方图表库引入的 20MB+ 体积膨胀与掉帧风险。
- **高危防护对话框**：`RiskConfirmDialog` 针对 R3（重启）与 R4（改串、AT）操作强制执行 3 秒倒计时锁定，根绝误触。

### 2.2 ViewModel 状态管理与协程调度
- **状态单向流动 (UDF)**：所有界面状态均收敛至各自 ViewModel 的 `StateFlow<UiState>`，保证配置变更（如屏幕旋转）无缝恢复。
- **精细化轮询策略**：
  - Port 80 极速状态轮询（2秒一次，响应耗时 6.89ms，CPU 零压力）。
  - Port 8081 流量统计轮询（2秒一次）。
  - Port 8081 电池状态轮询（6秒一次）。
  - Port 8081 完整设备概览轮询（10秒一次，避开 1383ms 耗时接口引起的并发拥塞）。

### 2.3 Repository 双端口聚合与故障转移 (Failover)
- **主备容灾机制**：蜂窝小区与基站状态优先调用 Port 8081 高级 RESTful API；当 8081 发生服务超时或脱机时，透明降级至 Port 80 `system_status_data.asp` 正则解析，保障界面核心指标（RSRP、SINR、制式）永不空白。
- **动态网关支持**：默认绑定 `192.168.100.1`，同时支持切换至自定义子网网关。

### 2.4 安全通信与网络层
- **HostAllowlistInterceptor**：网络层拦截器强校验目标主机名，非白名单目标（仅允许路由器 LAN IP 与本地调试环境）立刻抛出 `SecurityException` 阻断。
- **动态 Basic Auth**：Port 80 自动注入 Base64 编码的凭据，拦截 401 认证挑战，防止密码泄露。
- **敏感数据脱敏**：日志与调试输出统一经过 `SensitiveDataRedactor`，IMEI、MAC、手机号自动掩码。
