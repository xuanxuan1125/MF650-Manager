# 原版 Web 后台功能覆盖与替代度评估报告 (MF650 Manager v0.2.0)

## 1. 目标与评估原则

本项目旨在制作一个能**完全取代**浏览器访问：
- `http://192.168.100.1` (Padavan 原始后台)
- `http://192.168.100.1:8081` (飞流高级定制后台)
的原生 Android 现代化管理客户端。

本报告对第一阶段审计提取的所有 Web 页面 (`FINAL_REPORT/`) 与 `API_MATRIX.csv` 进行 1:1 对账，严密核验功能迁移完整度。

---

## 2. 页面与功能覆盖度对照矩阵

| 原始 Web 页面/模块 | 涉及端口 | 核心功能点 | App 替代实现路径 | 覆盖状态 |
| :--- | :--- | :--- | :--- | :--- |
| **system_status_data.asp** | Port 80 | CPU负载、内存使用、运行时长、Wi-Fi状态 | `HomeScreen` -> `DeviceHealthCard` & `CompactMetricsRow` | **100% 完整覆盖** |
| **status_internet.asp** | Port 80 | WAN 口连接、运营商、基站 ID、信号质量 | `HomeScreen` -> `NetworkHeroCard` | **100% 完整覆盖** |
| **mf650.html (首页)** | Port 8081 | 实时双向速率、高通 SDX55 模块详情 | `HomeScreen` -> `SpeedCanvasChart` (60s平滑滚动) | **100% 完整覆盖** |
| **Advanced_Wireless2g_Content.asp** | Port 80 | 2.4G 无线开关、SSID、WPA2密码、隐藏广播 | `WifiScreen` -> 2.4 GHz 配置卡片 | **100% 完整覆盖** |
| **Advanced_Wireless5g_Content.asp** | Port 80 | 5G 无线开关、SSID、密码显隐、信道与频宽 | `WifiScreen` -> 5 GHz 配置卡片 | **100% 完整覆盖** |
| **charge.html (电源管理)** | Port 8081 | 直供电停充、自动充断电保护、库仑计校准 | `MoreScreen` -> `BatterySubScreen` & 首页快捷开关 | **100% 完整覆盖** |
| **cell.html (基站与锁频)** | Port 8081 | LTE/NR 小区频点锁定、解除锁定、邻区列表 | `CellularScreen` -> 简洁/专业双模切换 & 锁卡片 | **100% 完整覆盖** |
| **nr5g_band.html (频段控制)**| Port 8081 | 5G SA (n1/28/41/78) 与 NSA 频段开关 | `CellularScreen` -> 频段 FilterChips 交互卡 | **100% 完整覆盖** |
| **liuliang.html (流量控制)** | Port 8081 | 流量额度统计、飞行模式切换、插件控制 | `HomeScreen` 四宫格 & `MoreScreen` -> `ModemSubScreen` | **100% 完整覆盖** |
| **forward.html (短信转发)** | Port 8081 | Telegram / Bark / Webhook 短信推送与Token | `MessagesScreen` 右上角直达 & `SmsForwardSubScreen` | **100% 完整覆盖** |
| **cron.html (定时任务)** | Port 8081 | Crontab 定时重启、定时搜网脚本 | `MoreScreen` -> `CronSubScreen` | **100% 完整覆盖** |
| **photos.html (屏幕与壁纸)** | Port 8081 | 机身彩屏息屏控制、三套待机主题壁纸 | `MoreScreen` -> `LcdWallpaperSubScreen` | **100% 完整覆盖** |
| **system.html (系统与SIM)** | Port 8081 | 物理卡槽 1 / 内置 eSIM 卡槽 2 硬件切换 | `MoreScreen` -> `SimSubScreen` & 首页快捷切换 | **100% 完整覆盖** |
| **at_debug.html (AT终端)** | Port 8081 | 高通 SDX55 交互式 AT 终端、回显显示 | `MoreScreen` -> `AtTerminalSubScreen` (带预设) | **100% 完整覆盖** |
| **imei.html (串号维护)** | Port 8081 | NVRAM IMEI 串号读取、改写、清除历史 | `MoreScreen` -> `ImeiSubScreen` (带 R4 极高危警示) | **100% 完整覆盖** |
| **log_content.asp (日志查看)**| Port 80 | syslog 内核与守护进程运行日志 | `MoreScreen` -> `LogViewerSubScreen` | **100% 完整覆盖** |
| **ttyd (Port 7689)** | Port 7689 | 浏览器 Web 终端 Shell 会话 | `MoreScreen` -> `TtydWebViewSubScreen` (内置 WebView) | **100% 完整覆盖** |
| **Advanced_LAN_Content.asp** | Port 80 | 局域网网关 IP、DHCP 地址池分配 | `MoreScreen` -> `LanDhcpSubScreen` | **100% 完整覆盖** |
| **Advanced_Firewall_Content.asp** | Port 80 | SPI 防火墙、Ping 响应、DMZ 主机设置 | `MoreScreen` -> `FirewallSubScreen` | **100% 完整覆盖** |
| **apply.cgi (系统重启)** | Port 80 | Linux 热重启软关机 | `MoreScreen` -> `MaintenanceSubScreen` (带 R3 确认) | **100% 完整覆盖** |

---

## 3. 超越原版 Web 后台的体验革新

1. **移动端首屏感知体验 (CPE++ 范式)**：
   - 原始 Web 后台信息散落、字体极小、手机浏览器缩放体验极差。
   - App 采用统一的 Liquid Glass 仪表盘，首屏即可一目了然获取关键数据（实时图表、电量胶囊、双频 Wi-Fi 状态、基站主频段与信号格数）。
2. **免登入与自动心跳重试**：
   - 自动在后台注入认证令牌，网络短暂中断自动静默重试，告别 Web 端频繁掉登录。
3. **数据清洗与物理模型矫正 (Zero Clamp)**：
   - 原版 Web 各页面混杂了字符串、离散挡位与浮点数。App 建立 `BatteryNormalizer` 与 `UnitNormalizer` 核心校验库，彻底根治“电量150%”等荒谬展示。
4. **高危操作防呆防误触**：
   - 原版 Web 许多破坏性操作缺乏警告或仅有微弱 JS confirm。App 建立 R0 ~ R4 级逐级风险确认机制，严密保障高通基带物理安全。
