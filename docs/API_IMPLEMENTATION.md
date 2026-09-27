# MF650 API 映射与实现指南 (API_IMPLEMENTATION.md)

## 1. 端口与服务分工对比

| 维度 | Port 80 (Padavan 嵌入式) | Port 8081 (飞流高级管理) | Port 7689 (ttyd 终端) |
| :--- | :--- | :--- | :--- |
| **底层核心** | Padavan RT-N56U / MIPS-ARM Linux | Go / Shell 轻量级 HTTP 守护进程 | C 原生 WebSocket 终端守护进程 |
| **主要定位** | 路由底层基础架构 (Wi-Fi, LAN, DHCP, 短信, 核心重启) | 蜂窝特有高级能力 (锁频, 锁小区, 电池硬件路径, 裸 AT) | 纯底层 Linux Shell 交互 |
| **典型接口延时**| **6.89 ms** (`/system_status_data.asp`) | **1383 ms** (`/api/device/info`) | 实时 WebSocket (~5ms) |
| **认证方式** | HTTP Basic Auth (`admin:admin`) | 无认证 (CORS `*`) | 无认证 |
| **App 实现方式** | 原生 Retrofit + 正则解析器 + 表单序列化器 | 原生 Retrofit + Gson 强类型反序列化 | 独立隔离 WebView 控件 |

---

## 2. 关键接口实现与映射表

### 2.1 状态监控与遥测 (Telemetry)
- **极速状态监控**: `GET /system_status_data.asp`
  - 提取项: `cpu_usage`, `mem_usage`, `battery_val`, `sim_status`, `unread_sms`, `net_type`, `rsrp_5g`, `sinr_5g`, `uptime`
  - 映射类: `PadavanParsers.parseSystemStatus()` -> `SystemStatus`
- **详细蜂窝与邻区**: `GET /api/cell?ACTION=get_status`
  - 提取项: `lte_lock`, `nr_lock`, `lte.earfcn`, `lte.pci`, `lte.rsrp`, `nr.nrarfcn`, `nr.pci`, `nr.rsrp`, `nr.sinr`, `lte_neighbors`
  - 映射类: `AdvancedApi.getCellStatus()` -> `CellularStatus`
- **电池库仑计与充放电**: `GET /api/device-status`
  - 提取项: `battery`, `usb_connection`, `charging`, `auto_charge`, `voltage`, `temperature`
  - 映射类: `AdvancedApi.getBatteryStatus()` -> `BatteryStatus`
- **实时流量计数**: `GET /api/liuliang?traffic-usage`
  - 提取项: `rx_bytes`, `tx_bytes`, `rx_speed`, `tx_speed`, `traffic_limit`
  - 映射类: `AdvancedApi.getTrafficUsage()` -> `TrafficStatus`

### 2.2 无线与网络写入 (apply.cgi 机制)
Padavan 固件要求写入时必须完整提交 ASP 页面内的所有隐藏字段，任何遗漏均会导致固件拒绝执行或静默失败。
- **2.4G Wi-Fi 保存**:
  - Endpoint: `POST /apply.cgi`
  - 关键隐藏字段: `current_page=Advanced_Wireless2g_Content.asp`, `action_mode= Apply `, `action_script=restart_wifis`, `sid_list=WLANConfig11b;`
  - 关键字段: `rt_radio_x`, `rt_ssid`, `rt_closed`, `rt_channel`, `rt_wpa_psk`
- **5G Wi-Fi 保存**:
  - Endpoint: `POST /apply.cgi`
  - 关键隐藏字段: `current_page=Advanced_Wireless_Content.asp`, `action_mode= Apply `, `action_script=restart_wifis`, `sid_list=WLANConfig11a;`
  - 关键字段: `wl_radio_x`, `wl_ssid`, `wl_closed`, `wl_channel`, `wl_wpa_psk`
- **短信收发与管理**:
  - 收件箱: `GET /sms_in.asp` -> `PadavanParsers.parseSmsArray(html, 0)`
  - 发送短信: `POST /apply.cgi` (`current_page=smsSend.asp`, `action_script=handle_sms`, `handle_sms_mode=SEND`, `number`, `content`)
  - 删除短信: `POST /apply.cgi` (`current_page=smsInbox.asp`, `action_script=handle_sms`, `handle_sms_mode=DELETE`, `box_type`, `sms_id`)

### 2.3 蜂窝高级控制 (Port 8081)
- **LTE 小区锁定**: `POST /api/cell` (`ACTION=lock_lte`, `LTE_ARFCN`, `LTE_PCI`)
- **5G NR 小区锁定**: `POST /api/cell` (`ACTION=lock_nr`, `NR_ARFCN`, `NR_PCI`)
- **解除锁定**: `GET /api/cell?ACTION=unlock_lte` / `GET /api/cell?ACTION=unlock_nr`
- **直供电硬件切断 (停充)**: `POST /api/control-charge` (`mode=0`)
- **恢复充电**: `POST /api/control-charge` (`mode=1`)
- **高通 SDX55 裸 AT 交互**: `POST /api/at-debug` (`atcmd=<command>`)
- **IMEI 串号改写**: `POST /api/imei?modify-imei` (`imei=<15位数字>`) (附带 R4 强制确认与倒计时保护)
