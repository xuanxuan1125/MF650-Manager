# Port 8081 飞流高级后台全量能力审计报告 (MF650 Manager v0.2.0)

## 1. 概述与设计宗旨

在第一与第二阶段的底层黑盒审计与协议逆向中，我们确认了阿乐卡 MF650（高通 SDX55）固件采用的双引擎微服务架构：
1. **Port 80 (Padavan 内核 httpd)**：管理基础路由、Wi-Fi 2.4G/5G 驱动、DHCP、防火墙、系统日志与基础短信息。通过 `/apply.cgi` 接收表单控制。
2. **Port 8081 (飞流定制 Python/Go 守护进程)**：直接挂接高通 QMI/AT 节点（如 `/dev/smd11` 或 QMI DMS/NAS），掌控硬件级充放电、基带锁频/锁小区、IMEI 写入、SIM 切换、定时 Cron 与自动化通知。
3. **Port 7689 (ttyd Web Terminal)**：直接暴露底层 Linux Shell。

在 v0.1.0 阶段，Port 8081 的诸多接口虽然在 Retrofit 中有所声明，但缺乏显性入口。在 v0.2.0 中，**所有 8081 接口 100% 显性化并赋予明确风险分级**。

---

## 2. 接口矩阵与 App 挂载全景图

| 模块类别 | 接口路径 | HTTP 方法 | 功能描述 | 风险等级 | App UI 对应入口 |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **设备总览** | `/api/device/info` | GET | CPU、内存、实时上下行速率、网络类型 | R0 | 首页 NetworkHeroCard & DeviceHealthCard |
| **电源控制** | `/api/device-status` | GET | 电池离散挡位 (0..6)、电压、充电状态 | R0 | 首页 Header 胶囊 & 更多-电池控制 |
| **电源控制** | `/api/control-charge` | POST | 直供电模式 (0: 停充 / 1: 充) | R1 | 首页快捷按钮 & 更多-电池控制 |
| **电源控制** | `/api/set-auto-charge` | POST | 开启/关闭自动过充保护与阈值停充 | R1 | 更多-供电与电池控制 |
| **电源控制** | `/api/battery-calibration` | POST | 触发 IP5332 库仑计重新标定 | R1 | 更多-供电与电池控制 |
| **基站与锁频** | `/api/cell?ACTION=get_status` | GET | 5G NR / 4G LTE 载波频段、PCI、RSRP、SINR | R0 | 基站页 Cellular Hero & 指标详情 |
| **基站与锁频** | `/api/cell` (`lock_lte`) | POST | 锁定 4G LTE 指定频点 (EARFCN) 与小区 (PCI) | R2 | 基站页 - 4G 小区锁定卡片 (带R2弹窗) |
| **基站与锁频** | `/api/cell` (`lock_nr`) | POST | 锁定 5G NR 指定频段、频点与 PCI | R2 | 基站页 - 5G 小区锁定卡片 (带R2弹窗) |
| **基站与锁频** | `/api/cell?ACTION=unlock_lte` | GET | 解除 4G 小区锁定恢复自动选网 | R2 | 基站页 - 解除锁定按钮 |
| **基站与锁频** | `/api/cell?ACTION=unlock_nr` | GET | 解除 5G 小区锁定恢复全自动搜网 | R2 | 基站页 - 解除锁定按钮 |
| **频段过滤** | `/api/nr5g-band` | GET/POST | 查询/启用/禁用 5G SA 与 NSA 载波频段 | R2 | 基站页 - 频段控制 FilterChips |
| **SIM 管理** | `/api/sim?ACTION=get_status` | GET | 当前卡槽状态、ICCID 与网络注册状态 | R0 | 更多-SIM 卡管理与切换 |
| **SIM 管理** | `/api/sim` (`switch_sim`) | POST | 切换卡槽 1 (物理 SIM) 与卡槽 2 (内置 eSIM) | R2 | 首页快捷按钮 & 更多-SIM 卡管理 |
| **流量与自动化** | `/api/liuliang?traffic-usage`| GET | 月度已用流量、剩余额度、限额设置 | R0 | 首页 CompactMetricsRow 流量卡 |
| **流量与自动化** | `/api/liuliang?feixing-kg` | POST | 切换飞行模式 (CFUN=0/1 射频发射) | R1 | 首页快捷按钮 & 更多-蜂窝设置 |
| **定时任务** | `/api/cron?ACTION=get` | GET | 读取设备系统 crontab 调度列表 | R0 | 更多-定时调度任务 (Cron) |
| **定时任务** | `/api/cron` | POST | 添加定时重启、定时飞行搜网调度任务 | R3 | 更多-定时调度任务 (Cron) |
| **短信转发** | `/api/forward?action=load` | GET | 读取 Bark / Telegram / Webhook 转发配置 | R0 | 短信页右上角直达 & 更多-短信转发 |
| **短信转发** | `/api/forward` | POST | 保存推送 Token 并启动/停用转发监听 | R2 | 更多-短信自动化转发 |
| **屏幕与壁纸** | `/api/photos/change-wallpaper`| POST | 切换 MF650 机身彩色 LCD 屏壁纸风格 | R1 | 首页快捷按钮 & 更多-屏幕设置 |
| **高通底层** | `/api/at-debug` | POST | 裸发 AT 指令交互（带安全过滤） | R4 | 更多-高通 AT 指令终端 (R4) |
| **高通底层** | `/api/imei?get-imei-info` | GET | 读取基带 NVRAM 真实 IMEI 与历史修改记录 | R0 | 更多-IMEI 维护 (R4) |
| **高通底层** | `/api/imei?modify-imei` | POST | 写入新 IMEI 串号至基带物理存储 | R4 | 更多-IMEI 维护 (带极高危免责弹窗) |
| **系统 Shell** | `http://<IP>:7689/` | WebSocket | 远程交互式终端会话 | R4 | 更多-ttyd 远程网页终端 |

---

## 3. 安全防护与降级熔断设计

1. **双后台独立健康探测**：
   - App 采用 `BackendHealthBadge` 分离探测 Port 80 与 Port 8081。
   - 当 Port 8081 服务崩溃或被关闭时，首页高级控制快捷网格自动置灰并展示“8081离线”状态标签，严禁抛出致命崩溃。
2. **敏感操作风险确认等级 (R0 ~ R4)**：
   - **R1**：轻微配置变更（如开启直供电、触发库仑计校准），提供明确反馈。
   - **R2**：网络短暂中断（如锁频、重启 Wi-Fi 驱动），提供 15 秒倒计时提示与保存确认弹窗。
   - **R3**：整机重启、定时 crontab 写入，提示终端将彻底断网约 30 秒。
   - **R4**：基带物理层操作（AT 指令、IMEI 改写），必须弹出猩红色高危对话框，包含“NVRAM 损坏不可逆风险”警告与合规免责声明，用户二次点击后方可下发。
