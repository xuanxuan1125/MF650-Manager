# 更新日志 (CHANGELOG.md)

## [v0.2.0] - 2026-09-27

### 🚀 重点里程碑：全量 UI/UX 重构 + 数据正确性修复 + 高级后台能力显性化

#### 1. 致命缺陷修复与物理模型校准 (Zero Clamp Fix)
- **根除电池 150% 虚假电量**：
  - 深度追踪发现 Port 8081 `/api/device-status` 与 Port 80 `bat_value` 实为硬件 discrete scale (0..6 离散挡位)。
  - 废除错误的简单百分比乘法；严禁采用 `clamp(0, 100)` 掩盖真实硬件映射。
  - 构建 `BatteryNormalizer` 核心量化引擎，严格依据 official `charge.html` 映射表将 0..6 精准映射为 `[0, 10, 25, 50, 75, 90, 100]%`。
  - 支持 `3850mV` 自动除以 1000 换算为 `3.85V` 物理电压，支持数据合法性严格校验。
- **速率与流量单位大一统**：
  - 引入 `UnitNormalizer`，智能识别 8081 无量纲浮点速率（如 `"0.26"` MB/s）自动换算为标准 KB/s。
  - 统一全 App 信号 RSRP (-140..-40 dBm)、SINR (dB)、RSRQ (dB) 物理有效性范围校验与异常 `--` 兜底。

#### 2. 全新 Liquid Glass (液态玻璃) 设计系统
- **统一视觉语言**：
  - 告别单调扁平卡片与过度模糊，全面引入以深色半透明（PanelAlpha: 0.65~0.80）与物理渐变边缘高光（Gradient Border Highlight）为核心的 `LiquidGlassCard`。
  - 底层渲染柔和的径向微弱环境光晕 `AmbientBackground`，塑造深邃通透的未来科技感。
  - 卡片集成 0.985x 阻尼弹簧按压微动效与高光反射增强，触控反馈极其清晰。
  - 提供 `Light`、`Dark` 与 `AMOLED 纯黑` 三套调色板，并支持画质动态分级。
- **图标矢量化与符号体系升级**：
  - 彻底清理全 App 历史遗留的 Emoji 图标（🏠, 📡, 📶, 💬, ⚙️ 等）。
  - 100% 换装为规范标准的 Google Material Vector Symbols。

#### 3. 页面重构与 CPE++ 交互体验
- **首页 Dashboard 重构**：
  - 顶部集成 `NetworkHeroCard`（运营商、5G SA/NSA 状态、60秒平滑双向速率折线图、当前主频段）。
  - 紧凑型四宫格 `CompactMetricsRow`：信号格数、电量胶囊、核心温度、已用流量。
  - 设备健康卡片 `DeviceHealthCard`：CPU 负载、RAM 使用比例、系统连续运行时长。
  - 首页常驻【高级控制中心】8 大快捷入口，8081 服务离线时自动置灰并提示。
  - 头部集成 `BackendHealthBadge`，分离独立诊断 Port 80、Port 8081 与 5G 蜂窝健康状态。
- **基站管理页面 (CellularScreen)**：
  - 新增【简洁模式】与【专业模式】无缝切换。
  - 5G NR 与 LTE 支持频段 FilterChip 多选控制与恢复自动。
  - 小区锁定与频点锁定卡片集成 R2 级风险保存确认弹窗。
- **Wi-Fi 管理页面 (WifiScreen)**：
  - 2.4 GHz 与 5 GHz 双频 Liquid Glass 大卡片，带状态点亮指示。
  - 密码输入框支持一键眼睛图标显隐切换。
  - 结构化分节：基础设置、安全加密 (WPA2/WPA3)、射频参数。
  - 保存后动态展示 15 秒无线驱动重启倒计时进度条。
- **短信中心 (MessagesScreen)**：
  - 类原生现代化消息 App 设计，发件人头像圆环、未读蓝点、气泡对话框。
  - 右上角直达 SMS 自动化转发配置入口。
  - 写短信弹窗实时统计字数并提示 70 字单条限制。
- **高级功能矩阵 (MoreScreen)**：
  - 归整为 6 大玻璃分类卡片：硬件与电源、网络与通信、自动化与定时、系统监控与维护、高级玩家与底层协议、关于与版本。
  - 完整呈现 8081 隐藏高级能力：新增 `SimSubScreen` (物理卡槽/eSIM 硬件切换)、`CronSubScreen` (Crontab 定时调度)、`SmsForwardSubScreen` (Bark/Telegram/Webhook 转发)、`LcdWallpaperSubScreen` (机身彩屏息屏与壁纸选择)、`ImeiSubScreen` (带 R4 极高危猩红风险确认与合规声明)、`ChangelogScreen`。

#### 4. 代码质量与工程规范
- **40/40 单元测试全部通过 (100% PASS)**：涵盖电量转换、速率转换、脱敏安全、模型反序列化、主机白名单。
- **构建输出**：`build-artifacts/MF650_Manager_v0.2.0.apk` (Release 版，大小 12.17 MB，通过 apksigner 验证)。

---

## [v0.1.0] - 2026-09-27
- 初始版本发布，验证 Port 80 Padavan + Port 8081 飞流双端口双引擎通信可行性。
- 基础 Wi-Fi 配置与 15 秒倒计时。
- 基础基站查询与 ttyd 终端集成。
