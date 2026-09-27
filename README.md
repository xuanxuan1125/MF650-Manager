# MF650 Manager

[![Release](https://img.shields.io/badge/Release-v0.2.0-blue.svg)](https://github.com/xuanxuan1125/MF650-Manager/releases/tag/v0.2.0)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0.21-purple.svg)](https://kotlinlang.org)
[![Compose](https://img.shields.io/badge/Jetpack_Compose-2024.12.01-green.svg)](https://developer.android.com/jetpack/compose)
[![Tests](https://img.shields.io/badge/Unit_Tests-40%2F40_PASS-brightgreen.svg)]()
[![License](https://img.shields.io/badge/License-Apache_2.0-blue.svg)](LICENSE)

专为**阿乐卡 (ALECA) MF650** 随身 Wi-Fi（高通 SDX55 5G 平台）打造的高性能现代原生 Android 管理客户端。

旨在 100% 完整取代浏览器访问原版 Padavan 后台（Port 80）及飞流高级后台（Port 8081），带来类 **CPE++** 桌面级的极致移动管理体验。

---

## 🌟 v0.2.0 核心亮点

### 1. 根治“电池电量 150%”等数据顽疾 (Zero Clamp)
- 拒绝粗暴的 `clamp(0, 100)` 掩耳盗铃。
- 引入 `BatteryNormalizer`，精准还原硬件级 discrete scale (0..6 挡位) 至 `[0, 10, 25, 50, 75, 90, 100]%` 物理阶梯。
- 统一 `UnitNormalizer` 速率与流量换算引擎，精准识别 8081 无量纲浮点速率（自动转换 MB/s 与 KB/s）。

### 2. 独创 Liquid Glass (液态玻璃) 统一设计系统
- 全 App 告别粗糙纯色卡片与无度模糊，采用物理渐变边缘高光 (Gradient Border Highlight) 与深色微通透材质。
- 底层叠加环境微光光晕 (`AmbientBackground`)，配合 0.985x 触控按压阻尼微动效。
- 深度适配浅色 (Light)、深色 (Dark) 与 AMOLED 纯黑主题。
- 彻底肃清历史遗留的 Emoji 字符，全面矢量化为规范的 Google Material Vector Symbols。

### 3. Port 8081 飞流高级后台 100% 显性化
- **8 大首页快捷开关**：锁频、锁小区、SIM 卡槽切换、直供电模式、流量守护、自动化转发、定时任务、LCD 壁纸。
- **三合一状态诊断徽章 (`BackendHealthBadge`)**：独立解耦展示 Port 80、Port 8081 与 5G 蜂窝连通性。8081 离线自动降级与置灰，杜绝闪退。
- **高级功能矩阵 6 大分类卡片**：
  1. 硬件与电源：直供电 (Bypass Charge)、过充保护、库仑计校准、LCD 息屏与待机主题。
  2. 网络与通信：网络首选模式 (5G SA/NSA/LTE)、APN 管理、SIM1/eSIM 硬件切换、DHCP 地址池、SPI 防火墙。
  3. 自动化与定时：Crontab 定时调度（定时重启/重新搜网）、Bark / Telegram / Webhook 短信推送转发。
  4. 系统监控与维护：syslog 实时日志、Linux 内核热重启。
  5. 高级玩家与底层协议 (R4)：高通 SDX55 交互式 AT 终端、基带 NVRAM IMEI 串号读取与改写、Port 7689 ttyd 网页终端。
  6. 关于与版本：应用内更新日志（ChangelogScreen）、检查更新与源码仓库。

### 4. 严密的 R0 ~ R4 风险控制防线
- **R0**：只读监控，无感刷新。
- **R1**：轻微变更（直供电、壁纸切换），即时反馈。
- **R2**：网络短暂中断（Wi-Fi 重启 15 秒倒计时指示条、基站锁频），弹窗确认。
- **R3**：整机热重启、Crontab 定时注入，警示弹窗二次确认。
- **R4**：高通底层物理操作（AT 调试、IMEI 改写），强制弹出猩红色免责对话框，注明合规法规与变砖风险。

---

## 📱 核心界面一览

- **首页 (Dashboard)**：`NetworkHeroCard` 核心大卡（运营商、5G模式、60秒平滑上下行折线图）+ 紧凑四宫格 + 设备健康大卡 + 8大高级控制入口。
- **基站管理 (Cellular)**：简洁/专业模式一键切换、5G NR & LTE 支持频段 FilterChip 多选、4G/5G 小区精确锁定。
- **Wi-Fi 管理 (Wi-Fi)**：2.4G & 5G 双频 Liquid Glass 卡片、密码一键显隐切换、保存后 15 秒倒计时重启动画。
- **短信中心 (Messages)**：类原生聊天 App 设计、发件人头像圆环、未读蓝点、气泡对话、70 字实时统计、右上角直达 SMS 转发。
- **高级功能 (More)**：分类归整 13 个子功能页面，提供实时搜索过滤与平滑路由。

---

## 🛠️ 构建与测试

### 环境要求
- JDK 21 (推荐 Android Studio JBR 21)
- Android SDK 35 (compileSdk 35, minSdk 26)
- Gradle 8.13

### 执行单元测试
```powershell
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
.\gradlew.bat test
```
> **测试结果**：40/40 单元测试通过率 100%。

### 构建 Release APK
```powershell
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
.\gradlew.bat assembleRelease
```
产物路径：`build-artifacts/MF650_Manager_v0.2.0.apk`

---

## 📦 下载与安装

请前往 GitHub Releases 下载正式签名构建包：
👉 **[下载 MF650 Manager v0.2.0 APK](https://github.com/xuanxuan1125/MF650-Manager/releases/tag/v0.2.0)**

- **文件名称**：`MF650_Manager_v0.2.0.apk`
- **文件大小**：12.17 MB (12,762,700 字节)
- **SHA256 校验和**：`DBE38D238A05A01A746282032886018541D99CF8740AE6292BFF2D113B1ED819`

---

## 📄 开源许可证

本项目基于 [Apache License 2.0](LICENSE) 协议开源。仅供随身 Wi-Fi 设备个人合法研究与网络管理使用，严禁用于任何非法篡改入网许可或破坏通信网络行为。
